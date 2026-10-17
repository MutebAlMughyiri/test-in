#Requires -Version 7

<#
.SYNOPSIS
    Runs the IntelliJ inspections over this project without opening the IDE (#55).

.DESCRIPTION
    The findings the editor shows exist only inside the IDE - ./gradlew compileJava
    reports zero warnings, because everything the profile finds is an IntelliJ
    inspection, not a javac one. This produces the same list on the command line so
    it can be counted and driven to zero (#48).

    The inspector is the build the Gradle plugin downloaded, pinned by
    intellij.version in gradle.properties, so the result does not depend on which
    IDE happens to be installed on a given machine.

    Everything it writes - the XML, the two reports, and the throwaway config and
    system directories the inspector needs - goes under .inspection/, one folder
    to delete. Those last are isolated because a running IDE holds a lock on its
    own: without them the run fails while IntelliJ is open on the same project,
    and it must never touch the developer's real settings.

    Costs one full indexing pass, so expect 10-20 minutes. A deliberate sweep, not
    a per-commit gate - .github/workflows/inspect.yml runs it on every push to
    main. It was on a two-day schedule until 2026-09-12: a calendar runs it over
    code nobody touched and misses the push that mattered.

    Exits non-zero for any finding in the files this repository writes, the
    ones .idea/scopes/Inspected.xml names. A warning the IDE shows there is a
    warning the run fails on. The only exceptions are the ones qodana.yaml
    lists under exclude, each with its reason: Qodana reads that list in CI and
    this script reads the same one, so the two gates cannot disagree.

    Some rules are this script's own, because no IntelliJ inspection makes them
    or the headless run cannot be trusted with the one that does. CONTRIBUTING.md
    names them in one table, under The inspection gate, and UnlistedRule fails the
    run when that table and this file disagree.

    They read the source as text, so -Quick runs them, and PMD, in about a
    minute with no IDE. That is the check to run before handing a change over; the full run is
    CI's, on every push.

.EXAMPLE
    pwsh tools/inspect.ps1
#>

param(
# The one folder this script writes: the XML, the two reports, and the
# inspector's throwaway IDE directories. Deliberately not under build/:
# ./gradlew clean deletes that, and the findings list is what you work from
# for the next hour. Ignored by Git instead.
    [string] $OutputDir = '.inspection',

# Narrows a run to one place, and passing '' inspects the project root.
#
# No default, deliberately. It used to default to 'src/main', which is
# always truthy - so the branch that reads every production source root was
# unreachable, the content modules were never checked, and three comments
# said otherwise (#170). What "no override" means is now asked of
# PSBoundParameters, which can tell it from a value.
    [string] $Subdirectory,

# Reuse the XML already in $OutputDir and only rebuild the reports. The
# line numbers in it are the ones the inspector saw, so a source edited
# since then reports against lines that have moved - fine for re-reading a
# run, wrong for judging the tree as it is now.
    [switch] $ReportOnly,

# Only this script's own rules and PMD, and no IDE at all. A minute rather
# than twenty, because nothing is indexed: the rules below read the source
# as text, and PMD reads its syntax tree.
#
# This is what to run before handing a change over. The full run belongs to
# CI on every push, and the findings it alone can see - dead code, a
# deprecated call, a redundant cast - are read from that run. What it cannot
# see is exactly what this mode catches, and what kept arriving in Muteb's
# IDE one warning at a time.
    [switch] $Quick
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

# The spell check has no exceptions: not the Inspected scope, and not the All
# entries in qodana.yaml, which excuse the legal texts and the wrapper scripts
# from every other inspection. A word it cannot correct - an IntelliJ API, a
# command, a library - goes into .idea/dictionaries/project.xml instead.
$spelling = 'SpellCheckingInspection'

# Whether the caller narrowed this run. Asked of PSBoundParameters rather than
# of the value, because "-Subdirectory ''" is a real request - inspect the
# project root - and a value cannot tell that from having no value at all.
$narrowed = $PSBoundParameters.ContainsKey('Subdirectory')

# What the inspector analyses: the whole project unless narrowed, and what is
# kept of it: the files .idea/scopes/Inspected.xml names, which is everything
# this repository writes. The inspector reads everything because a whole-project
# check judged on part of the tree is wrong - run on 'src/main' alone, a throws
# clause testin-java needs was reported redundant, and the content modules, the
# tests and every Markdown file were never read at all. It cannot be told the
# scope instead: its -scope option and the idea.analyze.scope property both
# failed headless, the first matching nothing and the second nothing less than
# the whole repository. So Select-Inspected drops what lies outside - .sandbox,
# which holds an entire IDE and produced 74,803 spelling findings, this script's
# own scratch folder, and the sample data.
$analysisScope = if ($narrowed)
{
    $Subdirectory
}
else
{
    ''
}

function Resolve-Inspector
{
    $props = Get-Content (Join-Path $repo 'gradle.properties')
    $version = ($props | Select-String -Pattern '^intellij\.version=(.+)$').Matches[0].Groups[1].Value

    # Wherever Gradle keeps its caches. GitHub Actions points GRADLE_USER_HOME
    # at the runner's workspace rather than at the profile, so looking only in
    # the profile found nothing there and the scheduled run could not start.
    # $HOME is PowerShell's own and is the profile on every platform.
    $gradleHome = if ($env:GRADLE_USER_HOME)
    {
        $env:GRADLE_USER_HOME
    }
    else
    {
        Join-Path $HOME '.gradle'
    }

    # The platform is downloaded per operating system and the folder is named
    # after it, so the suffix is matched rather than assumed (#106).
    $osSuffix = if ($IsWindows)
    {
        'win'
    }
    elseif ($IsMacOS)
    {
        'mac'
    }
    else
    {
        'linux'
    }

    # The Gradle transform path carries a content hash, so it is matched by shape rather than stored.
    $ide = Get-ChildItem -Path (Join-Path $gradleHome "caches/*/transforms/*/transformed" "idea-$version-$osSuffix") `
        -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

    if (-not $ide)
    {
        throw "No downloaded IDE $version found under $gradleHome. Run './gradlew compileJava' first - it fetches the platform this script inspects with."
    }

    # inspect.bat on Windows, inspect.sh everywhere else. Same arguments, different name.
    $launcher = if ($IsWindows)
    {
        'inspect.bat'
    }
    else
    {
        'inspect.sh'
    }
    $inspect = Join-Path $ide.FullName 'bin' $launcher
    if (-not (Test-Path $inspect))
    {
        throw "No $launcher in $( $ide.FullName )"
    }

    # Plugin DevKit, which the IDE no longer bundles. It is what reads plugin.xml:
    # without it an icon path that resolves nowhere passes, and a field only
    # plugin.xml names - Icons.PLUGIN - is reported as never used (#390). The
    # Marketplace serves the build made for this IDE's exact build number. Kept
    # under Gradle's caches folder, which CI restores between runs.
    $product = Get-Content (Join-Path $ide.FullName 'product-info.json') -Raw | ConvertFrom-Json
    $build = "$( $product.productCode )-$( $product.buildNumber )"
    $devKit = Join-Path $gradleHome 'caches' 'testin-devkit' "devkit-$build.zip"
    if (-not (Test-Path $devKit))
    {
        New-Item -ItemType Directory -Force -Path (Split-Path $devKit) | Out-Null
        Invoke-WebRequest -Uri "https://plugins.jetbrains.com/pluginManager?action=download&id=DevKit&build=$build" -OutFile "$devKit.part"
        Move-Item -Path "$devKit.part" -Destination $devKit
    }

    $formatter = Join-Path $ide.FullName 'bin' ($launcher -replace '^inspect', 'format')

    Write-Host "Inspector: $inspect, with Plugin DevKit for $build"
    return [pscustomobject]@{ Launcher = $inspect; Formatter = $formatter; DevKit = $devKit }
}

function Invoke-Inspector([object] $inspector, [string] $outPath)
{
    # Outside the repository, and emptied before every run. Both halves matter.
    #
    # Empty, because reusing these caches is tempting - they hold the indexes
    # and make a second run far quicker - but the VFS snapshot in there can
    # survive a source edit and be analysed instead of the file on disk, which
    # reports findings that were already fixed. A slow run beats a stale one.
    #
    # Outside, because they used to live in .inspection, inside the very folder
    # being inspected: 741 MB of an IDE's own caches, indexed by the run that
    # wrote them. The inspector reads the project from disk, so the scratch has
    # no reason to sit in it.
    $scratch = Join-Path ([System.IO.Path]::GetTempPath()) 'testin-inspect'
    if (Test-Path $scratch)
    {
        Remove-Item -Path $scratch -Recurse -Force -ErrorAction SilentlyContinue
    }
    New-Item -ItemType Directory -Force -Path $scratch | Out-Null

    # Forward slashes: idea.properties is read as a Java properties file, where a
    # backslash escapes the next character.
    $s = $scratch -replace '\\', '/'
    $propsFile = Join-Path $scratch 'idea.properties'
    @"
idea.config.path=$s/config
idea.system.path=$s/system
idea.log.path=$s/log
idea.plugins.path=$s/plugins
"@ | Set-Content -Path $propsFile -Encoding utf8
    Expand-Archive -Path $inspector.DevKit -DestinationPath (Join-Path $scratch 'plugins')

    $profilePath = Join-Path $repo '.idea' 'inspectionProfiles' 'Testin.xml'
    $arguments = @($repo, $profilePath, $outPath, '-v1')
    if ($analysisScope)
    {
        $arguments += @('-d', (Join-Path $repo $analysisScope))
    }

    Write-Host "Inspecting $repo - one indexing pass, expect 10-20 minutes..."
    $env:IDEA_PROPERTIES = $propsFile
    try
    {
        & $inspector.Launcher @arguments
    }
    finally
    {
        Remove-Item Env:\IDEA_PROPERTIES -ErrorAction SilentlyContinue
    }
}

function Select-Inspected([object[]] $problems)
{
    <#
        The findings in files this repository writes, which is what the scope
        .idea/scopes/Inspected.xml names - the same scope Code | Inspect Code
        offers in the IDE, so the two lists agree. The pattern is a||b||...: a
        folder is written as file:folder//* and a single file as file:name.
    #>
    $pattern = ([xml](Get-Content (Join-Path $repo '.idea/scopes/Inspected.xml') -Raw)).component.scope.pattern
    $folders = @()
    $files = @()
    foreach ($part in ($pattern.Trim('(', ')') -split '\|\|'))
    {
        $target = $part -replace '^file:', ''
        if ( $target.EndsWith('//*'))
        {
            $folders += $target.Substring(0, $target.Length - 3) + '/'
        }
        else
        {
            $files += $target
        }
    }

    # The spell check reads every file Git tracks, not only the scope: a typo in
    # CHANGELOG.md or a gradle.properties comment is as public as one in src.
    # What it must not read is the same as for every rule - .sandbox and the
    # other folders nobody here writes - and those are not tracked.
    $tracked = [System.Collections.Generic.HashSet[string]]::new([string[]]@(git -C $repo ls-files))

    $problems | Where-Object {
        $path = $_.Path.TrimEnd('/')
        if ($_.Inspection -eq $spelling)
        {
            return $tracked.Contains($path)
        }
        ($files -contains $path) -or @($folders | Where-Object { $path.StartsWith($_) }).Count -gt 0
    }
}

function Read-Problems([string] $outPath)
{
    $files = Get-ChildItem -Path (Join-Path $outPath '*.xml') -ErrorAction SilentlyContinue
    if (-not $files)
    {
        throw "No XML in $outPath - the inspector produced nothing. Check its output above."
    }

    foreach ($file in $files)
    {
        $doc = [xml](Get-Content -Path $file.FullName -Raw)
        foreach ($problem in $doc.problems.problem)
        {
            if (-not $problem)
            {
                continue
            }
            [pscustomobject]@{
            # file:// $PROJECT_DIR$ / path - only the repo-relative tail is useful.
                Path = ($problem.file -replace '^file://\$PROJECT_DIR\$/', '')
                Line = [int]$problem.line
                Inspection = $problem.problem_class.id
                Severity = $problem.problem_class.severity
                Message = ($problem.description -replace '<[^>]+>', '' -replace '\s+', ' ').Trim()
            }
        }
    }
}

function Get-SourceRoots
{
    <#
        Every production source tree, not just the core one.

        The plugin is one module plus a content module per language, and the
        checks below have to see all of them. A wrapped signature in
        testin-java is exactly as unreadable as one in src, and it used to pass
        the gate because nothing looked at it.

        Only the ones that exist: a checkout mid-refactor, or a future module,
        should not fail the run.
    #>
    return @('src/main', 'testin-java/src/main', 'testin-testng/src/main', 'testin-apimodel/src/main') |
            ForEach-Object { Join-Path $repo $_ } |
            Where-Object { Test-Path $_ }
}

function Get-JavaSourceRoots
{
    <#
        Every source tree, tests included.

        Get-SourceRoots is the production ones, which is what the design checks
        want: a caption duplicated between two test harnesses is read by nobody.
        The copyright header is the other kind of rule - a test file is as
        readable in a fork, a decompiler or a paste as a main one, and a rule
        with an exception is a rule somebody has to remember (#303).

        Only the ones that exist, for the same reason as above.
    #>
    return @('src/main', 'src/test', 'testin-java/src', 'testin-testng/src', 'testin-apimodel/src') |
            ForEach-Object { Join-Path $repo $_ } |
            Where-Object { Test-Path $_ }
}

function Resolve-CrossModuleUsages([object[]] $problems)
{
    <#
        A method the inspector calls dead because the only calls to it are in a
        content module.

        The inspector is given src/main as its analysis scope - the whole
        project is indexed, but the reference graph it builds for "is this ever
        used" covers the scope only. So every method the core exposes for
        testin-java or testin-testng to call reads as unused: four of them
        today, all on TestNGExecution, all called from TestNGRunner.

        They are relabelled rather than dropped. Nothing should vanish out of a
        report silently - that is how the wildcard import in #61 turned 94
        findings into 93 false ones without anyone noticing - and a group of its
        own in the summary says the thing worth knowing: these are not dead, and
        the unused count beside them is now about code that really is.

        Matched by name, so a method sharing a name with one a module genuinely
        calls would be spared wrongly. That is the safe direction to be wrong
        in, and the finding is still in the list to read.
    #>
    $moduleRoots = @('testin-java/src/main', 'testin-testng/src/main', 'testin-apimodel/src/main') |
            ForEach-Object { Join-Path $repo $_ } |
            Where-Object { Test-Path $_ }

    if (-not $moduleRoots)
    {
        return $problems
    }

    $moduleText = (Get-ChildItem -Path $moduleRoots -Filter *.java -Recurse -File |
            ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"

    foreach ($problem in $problems)
    {
        if ($problem.Inspection -ne 'unused')
        {
            continue
        }

        # A parameter an implementation elsewhere uses. Its own name is no use
        # for the search below - a parameter called p appears in every file in
        # the plugin - so what is looked for is the type declaring it, which a
        # content module has to name to implement it at all. CodeNavigation's
        # two p parameters were reported unused on every run: the only
        # implementation in scope is NoCodeNavigation, which does nothing by
        # design, and the real one is CodeNavigator in testin-java (#170).
        if ($problem.Message -match '^Parameter .* is not used in any implementation')
        {
            $type = [System.IO.Path]::GetFileNameWithoutExtension($problem.Path)
            if ($moduleText -match ('\b' + [regex]::Escape($type) + '\b'))
            {
                Set-UsedFromContentModule $problem
            }
            continue
        }

        # A method, a whole class, or an enum constant. It caught only the first
        # to begin with, which left ExecutionPosition reported as a dead class
        # while two files in testin-java were calling it.
        if ($problem.Message -notmatch '^(Method|Class|Enum constant|Constructor) .* never used')
        {
            continue
        }

        $name = Get-DeclaredName $problem
        if (-not $name)
        {
            continue
        }

        # How a module would name it. A type is named outright, so a word match
        # is enough and anything tighter would miss an import or a static call.
        # A method or a constant is reached through something, so the dot or the
        # method reference has to be there - a bare word match on a name like
        # "run" would spare every finding in the report.
        $pattern = if ($problem.Message -match '^Class ')
        {
            '\b' + [regex]::Escape($name) + '\b'
        }
        else
        {
            '(\.|::)\s*' + [regex]::Escape($name) + '\s*[(:,)]'
        }

        if ($moduleText -notmatch $pattern)
        {
            continue
        }

        Set-UsedFromContentModule $problem
    }

    return $problems
}

<#
    Relabels one finding as reached from a content module.

    One place, because there are two ways in now - a declaration nothing in
    scope calls, and a parameter no implementation in scope uses - and the
    sentence a reader gets should not depend on which of them found it.
#>
function Set-UsedFromContentModule([object] $problem)
{
    $problem.Inspection = 'UsedFromContentModule'
    $problem.Message = "$( $problem.Message ) It is called from a content module, which the inspector cannot see: mark it @FromContentModule, the entry point .idea/misc.xml names."
}

function Get-DeclaredName([object] $problem)
{
    <#
        The method name a finding points at. Read from the source rather than
        parsed out of the message, which names the method only in some of its
        wordings.

        Scanned forward a few lines rather than read off the one line, because
        the line number can point just above the declaration - at the close of
        its javadoc - when the file has been edited since the inspector ran.
        The next declaration below a javadoc is the one that javadoc documents,
        so this is the right answer for what actually happens, and a
        few lines is not enough drift to reach a different method.

        Empty when nothing there looks like a declaration, which leaves the
        finding labelled as the inspector labelled it. Failing to recognize a
        cross-module call is a report that says too much; recognizing one that
        is not there would be a report that says too little.
    #>
    $file = Join-Path $repo $problem.Path
    if (-not (Test-Path $file))
    {
        return ''
    }

    $lines = [System.IO.File]::ReadAllLines($file)
    if ($problem.Line -lt 1 -or $problem.Line -gt $lines.Count)
    {
        return ''
    }

    $last = [Math]::Min($problem.Line + 4, $lines.Count)
    for ($n = $problem.Line; $n -le $last; $n++) {
        $text = $lines[$n - 1]
        $trimmed = $text.Trim()
        if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
        {
            continue
        }
        if ( $trimmed.EndsWith('*/'))
        {
            continue
        }

        # A type first: "public final class ExecutionPosition {" has no
        # parenthesis to find, and "record Moved(..)" has one that would give
        # back the record name anyway - but only by accident, and not for a
        # class or an interface.
        if ($text -match '\b(?:class|interface|enum|record)\s+(\w+)')
        {
            return $matches[1]
        }

        if ($text -match '\b(\w+)\s*\(')
        {
            return $matches[1]
        }
    }

    return ''
}


function Test-MachineString([string] $value)
{
    <#
        Whether a literal is machinery rather than something a tester reads.

        Deliberately generous about what it excludes. A check that cries wolf
        gets switched off, and the ones it lets through - a caption, a status, a
        button - are the ones worth arguing about.
    #>
    if ($value.Length -lt 2 -or $value.Length -gt 60)
    {
        return $true
    }
    if ($value -notmatch '[A-Za-z]')
    {
        return $true
    }
    # paths, ids, format strings, css, packages
    if ($value -match '[/\\{}<>%$#=;|\[\]()*+^~`]')
    {
        return $true
    }
    if ($value -match '\.' -and $value -notmatch ' ')
    {
        return $true
    }
    if ($value -ne $value.Trim())
    {
        return $true
    }
    if ($value -cmatch '^[a-z0-9_-]+$')
    {
        return $true
    }
    if ($value -cmatch '^[A-Z0-9_]+$')
    {
        return $true
    }
    # An identifier, not a sentence. A hump inside a single word is how a JSON
    # field, a config key or an inspection name is spelled - testinProject,
    # RepoUrl, UnstableApiUsage - and none of them is read by a tester. A word
    # they do read is a word, or words: Copy and Details have no hump, Git Error
    # has a space.
    if ($value -notmatch ' ' -and $value -cmatch '[a-z][A-Z]')
    {
        return $true
    }
    return $false
}



function Write-DisplayStringInventory([string[]] $scopes, [string] $outPath)
{
    <#
        Every string a tester might read, and where it is written.

        findings.txt lists the ones written in more than one file, because those
        are the ones to act on. This is the whole set - including the strings
        written once, which are the ones a new duplicate would be created from.
        Read it when deciding whether a word already exists before adding it.

        Grouped by the call the string is handed to, because the call says what
        kind of word it is: one passed to softShow is a confirmation, one passed
        to .column is a table heading, one passed to StatusBarShortcut.build is a
        dialog key. A group with no owning enum is a vocabulary waiting for one -
        which is how Done was found.
    #>
    $rows = @()

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $short = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
            $number = 0

            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
                {
                    continue
                }
                if ($text -match 'Logger\.(trace|debug|info|warn|error|fatal)')
                {
                    continue
                }

                foreach ($match in [regex]::Matches($text, '"((?:[^"\\]|\\.)*)"'))
                {
                    $value = $match.Groups[1].Value
                    if (Test-MachineString $value)
                    {
                        continue
                    }

                    # The nearest identifier followed by '(' before the string.
                    # Single-line calls only, which is nearly all of them; the
                    # rest land under (unknown) and are still listed.
                    $before = $text.Substring(0, $match.Index)
                    $calls = [regex]::Matches($before, '([\w.]+)\s*\(')
                    $call = if ($calls.Count -gt 0)
                    {
                        $calls[$calls.Count - 1].Groups[1].Value
                    }
                    else
                    {
                        '(unknown)'
                    }

                    $rows += [pscustomobject]@{ Value = $value; Call = $call; Where = "$short`:$number" }
                }
            }
        }
    }

    $inventory = Join-Path $outPath 'display-strings.txt'
    $lines = @(
        'Every string a tester might read, and where it is written.',
        '',
        'Grouped by the call it is handed to - the call says what kind of word it',
        'is, and a group with no owning enum is a vocabulary waiting for one.',
        '',
        "$( $rows.Count ) uses of $( @($rows | Select-Object -ExpandProperty Value -Unique).Count ) distinct strings.",
        '',
        'The ones written in more than one file are also in findings.txt, as',
        'DuplicatedDisplayString, which is what the ratchet counts.',
        '')

    foreach ($group in ($rows | Group-Object Call | Sort-Object Count -Descending))
    {
        $distinct = @($group.Group | Select-Object -ExpandProperty Value -Unique)
        $lines += ''
        $lines += ('=' * 78)
        $lines += ('{0}   -   {1} use(s), {2} distinct' -f $group.Name, $group.Count, $distinct.Count)
        $lines += ('=' * 78)

        foreach ($value in ($distinct | Sort-Object))
        {
            $places = @($group.Group | Where-Object { $_.Value -eq $value } | Select-Object -ExpandProperty Where)
            $lines += ('  "{0}"' -f $value)
            foreach ($place in ($places | Sort-Object))
            {
                $lines += ('        ' + $place)
            }
        }
    }

    $lines | Set-Content -Path $inventory -Encoding utf8
    Write-Host "Strings:   $inventory"
}

function Read-DriftedCaptions([string[]] $enums)
{
    <#
        One concept, two words, in front of the same tester.

        The enums that name a test case's fields declare the same constants -
        EXPECTED_RESULT, STEPS, MODULE - and each used to spell its own caption.
        Two of them said "Expected Results" while the grid, the test run editor
        and the details panel said "Expected Result", and it stayed that way for
        as long as each was written out separately.

        Read-DuplicatedDisplayStrings cannot see this. It matches literals that
        are the same, and a caption that has already drifted is two different
        strings - the check that exists to stop drift is blind to drift that has
        already happened. So this one compares by constant name instead of by
        text.

        A constant whose caption comes from another enum is skipped: asking an
        owner is the fix, not the finding.
    #>
    $captions = @{ }

    foreach ($file in $enums)
    {
        # Loud, not skipped. A path that is not there used to be passed over in
        # silence, so this check shrank from four enums to two on the day #111
        # moved the other two - and printed "Gate clear" either way for
        # everything it had stopped looking at (#66, finding 98).
        if (-not (Test-Path $file))
        {
            throw "Read-DriftedCaptions was given a file that is not there: $file"
        }

        $lines = [System.IO.File]::ReadAllLines($file)
        $short = (Split-Path $file -Leaf) -replace '\.java$', ''

        for ($i = 0; $i -lt $lines.Count - 1; $i++) {
            if ($lines[$i] -notmatch '^    ([A-Z][A-Z0-9_]+)\($')
            {
                continue
            }
            $constant = $matches[1]

            # The caption is the first argument, on the line below: a literal, or
            # the bundle key it is looked up under since the captions were
            # translated. Either form is the one thing two enums naming the same
            # constant have to agree on, and reading only the literal meant this
            # saw nothing at all once every caption became a key.
            $argument = $lines[$i + 1].Trim()
            if ($argument -notmatch '^(?:Bundle\.message\()?"((?:[^"\\]|\\.)*)"')
            {
                continue
            }

            $value = $matches[1]
            if (-not $captions.ContainsKey($constant))
            {
                $captions[$constant] = @()
            }
            $captions[$constant] += [pscustomobject]@{
                Value = $value
                Where = $short
                Path = $file.Substring($repo.Length + 1) -replace '\\', '/'
                Line = $i + 2
            }
        }
    }

    foreach ($constant in $captions.Keys)
    {
        $places = $captions[$constant]
        $distinct = @($places | Select-Object -ExpandProperty Value -Unique)
        if ($distinct.Count -lt 2)
        {
            continue
        }

        $names = ($distinct | ForEach-Object { '"' + $_ + '"' }) -join ' and '

        foreach ($place in $places)
        {
            [pscustomobject]@{
                Path = $place.Path
                Line = $place.Line
                Inspection = 'DriftedCaption'
                Severity = 'ERROR'
                Message = "$constant is called $names in different places. One concept, one word - ask the enum that owns it."
            }
        }
    }
}

function Read-DuplicatedDisplayStrings([string[]] $scopes)
{
    <#
        A user-facing string written in more than one file has no owner.

        This is the check the rest of the design rests on. A caption, a status
        name, a button - each belongs to the type that owns the concept, asked
        for as RunItemStatus.getLabel() or TestRunConfiguration.getDisplayName().
        A second file spelling it out is a divergence that nothing fails over:
        both read correctly alone, and they stop agreeing the day one is
        renamed.

        Reported once per extra file rather than once per literal, so the count
        is "how many places should be asking an owner instead" - which is the
        number that has to come down.

        Log lines are skipped. What a log says is not read by a tester and is
        not worth centralizing.
    #>
    $found = @{ }

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
                {
                    continue
                }
                if ($text -match 'Logger\.(trace|debug|info|warn|error|fatal)')
                {
                    continue
                }

                foreach ($match in [regex]::Matches($text, '"((?:[^"\\]|\\.)*)"'))
                {
                    $value = $match.Groups[1].Value
                    if (Test-MachineString $value)
                    {
                        continue
                    }

                    if (-not $found.ContainsKey($value))
                    {
                        $found[$value] = @()
                    }
                    $found[$value] += [pscustomobject]@{
                        Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                        Line = $number
                    }
                }
            }
        }
    }

    foreach ($value in $found.Keys)
    {
        $places = $found[$value]
        $files = @($places | Select-Object -ExpandProperty Path -Unique)
        if ($files.Count -lt 2)
        {
            continue
        }

        # Every place after the first: the first one is allowed to be where it
        # lives, and the rest are the ones that should be asking it.
        foreach ($place in ($places | Select-Object -Skip 1))
        {
            [pscustomobject]@{
                Path = $place.Path
                Line = $place.Line
                Inspection = 'DuplicatedDisplayString'
                Severity = 'WARNING'
                Message = "`"$value`" is written in $( $files.Count ) files. A string a tester reads belongs to the type that owns the concept - ask it instead."
            }
        }
    }
}

function Read-ModelStatics([string[]] $scopes)
{
    <#
        Static mutable state, in the packages that model the data.

        Never legitimate there. A static that is not final is one slot for the
        whole IDE holding a value that belongs to something - Config kept the
        cached Java test source root that way, so opening a second project
        overwrote the first and generation could write into the wrong source
        tree without saying anything.

        A per-project value goes on a @Service(Service.Level.PROJECT), which the
        caller gets and then reads or sets.

        Only non-final statics. A static final is a constant or an empty value
        of its own type, which is the pattern this codebase is built on.
    #>
    foreach ($scope in $scopes)
    {
        if (-not (Test-Path $scope))
        {
            continue
        }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
                {
                    continue
                }

                # An import declares nothing, so it holds nothing. An
                # 'import static a.b.C.took;' has no parentheses and no
                # 'final', which is every test below - so it read as a mutable
                # static and failed the gate over a line that is not a member.
                if ( $trimmed.StartsWith('import '))
                {
                    continue
                }

                if ($text -notmatch '\bstatic\b')
                {
                    continue
                }
                if ($text -match '\bfinal\b')
                {
                    continue
                }
                if ($text -match '\bstatic\s+(final\s+)?(class|interface|enum|record)\b')
                {
                    continue
                }
                # a method, not a field
                if ($text -match '\(')
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line = $number
                    Inspection = 'StaticMutableState'
                    Severity = 'ERROR'
                    Message = "One slot for the whole IDE: $trimmed - a value that belongs to a project goes on a project service the caller gets and sets."
                }
            }
        }
    }
}

function Read-HandWrittenPrivateConstructors([string[]] $scopes)
{
    <#
        An empty private constructor, where the Lombok annotation says it better.

        @NoArgsConstructor(access = AccessLevel.PRIVATE) is what this codebase
        uses to say "not instantiable", and it says it on the class rather than
        four lines down.

        Only empty ones. A constructor with a real body - a super(..) call,
        Swing setup - is a different thing entirely, and Lombok cannot express
        it.
    #>
    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            for ($i = 0; $i -lt $lines.Count; $i++) {
                $text = $lines[$i]
                if ($text -notmatch '^\s*private\s+[A-Z]\w*\s*\(\s*\)\s*\{')
                {
                    continue
                }

                # Empty when the brace closes on this line, or the next line is
                # only the closing brace.
                $empty = $text -match '\{\s*\}\s*$'
                if (-not $empty -and $i + 1 -lt $lines.Count)
                {
                    $empty = $lines[$i + 1].Trim() -eq '}'
                }
                if (-not $empty)
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line = $i + 1
                    Inspection = 'HandWrittenPrivateConstructor'
                    Severity = 'ERROR'
                    Message = 'An empty private constructor: use @NoArgsConstructor(access = AccessLevel.PRIVATE) instead.'
                }
            }
        }
    }
}

function Hide-StringsAndComments([string] $source)
{
    <#
        The same file with every string literal, char literal, text block and
        comment blanked to spaces, offsets unchanged.

        Written because the first sweep for unused lambda parameters read
        Logger.debug("[details] selectedDetails changed -> ") as a lambda and
        renamed the word inside the message. Any rule that matches on Java
        punctuation has to see the code alone.
    #>
    $out = [System.Text.StringBuilder]::new($source)
    $i = 0
    $n = $source.Length

    while ($i -lt $n)
    {
        $c = $source[$i]

        if ($c -eq '"' -and $i + 3 -le $n -and $source.Substring($i, 3) -eq '"""')
        {
            $j = $source.IndexOf('"""', $i + 3)
            $j = if ($j -lt 0)
            {
                $n
            }
            else
            {
                $j + 3
            }
            for ($k = $i; $k -lt $j; $k++) {
                if ($source[$k] -ne "`n")
                {
                    $out[$k] = ' '
                }
            }
            $i = $j
            continue
        }

        if ($c -eq '"' -or $c -eq "'")
        {
            $j = $i + 1
            while ($j -lt $n)
            {
                if ($source[$j] -eq '\')
                {
                    $j += 2; continue
                }
                if ($source[$j] -eq $c)
                {
                    $j++; break
                }
                $j++
            }
            for ($k = $i; $k -lt [Math]::Min($j, $n); $k++) {
                $out[$k] = ' '
            }
            $i = $j
            continue
        }

        if ($c -eq '/' -and $i + 1 -lt $n -and $source[$i + 1] -eq '/')
        {
            $j = $source.IndexOf("`n", $i)
            if ($j -lt 0)
            {
                $j = $n
            }
            for ($k = $i; $k -lt $j; $k++) {
                $out[$k] = ' '
            }
            $i = $j
            continue
        }

        if ($c -eq '/' -and $i + 1 -lt $n -and $source[$i + 1] -eq '*')
        {
            $j = $source.IndexOf('*/', $i + 2)
            $j = if ($j -lt 0)
            {
                $n
            }
            else
            {
                $j + 2
            }
            for ($k = $i; $k -lt $j; $k++) {
                if ($source[$k] -ne "`n")
                {
                    $out[$k] = ' '
                }
            }
            $i = $j
            continue
        }

        $i++
    }

    return $out.ToString()
}

function Get-LambdaBody([string] $masked, [int] $after)
{
    $k = $after
    while ($k -lt $masked.Length -and [char]::IsWhiteSpace($masked[$k]))
    {
        $k++
    }
    if ($k -ge $masked.Length)
    {
        return ''
    }

    $depth = 0

    if ($masked[$k] -eq '{')
    {
        for ($j = $k; $j -lt $masked.Length; $j++) {
            if ($masked[$j] -eq '{')
            {
                $depth++
            }
            elseif ($masked[$j] -eq '}')
            {
                $depth--; if ($depth -eq 0)
                {
                    break
                }
            }
        }
        return $masked.Substring($k + 1,[Math]::Max(0, [Math]::Min($j, $masked.Length) - $k - 1))
    }

    for ($j = $k; $j -lt $masked.Length; $j++) {
        $ch = $masked[$j]
        if ( '([{'.Contains($ch))
        {
            $depth++
        }
        elseif (')]}'.Contains($ch))
        {
            if ($depth -eq 0)
            {
                break
            }; $depth--
        }
        elseif (($ch -eq ';' -or $ch -eq ',') -and $depth -eq 0)
        {
            break
        }
    }

    return $masked.Substring($k, [Math]::Min($j, $masked.Length) - $k)
}

function Read-UnusedLambdaParameters([string[]] $scopes)
{
    <#
        A lambda parameter nothing in the body reads. Java has a name for one
        since 21 - the unnamed variable, _ - so there is a fix that needs no
        suppression, and the IDE offers it.

        The headless inspector cannot be asked for these: the global "unused"
        inspection under-reports without Lombok's generated code and the content
        modules' callers, and parameters never appear in its list at all. They
        reached Muteb's IDE one at a time instead, which is the whole reason
        this rule exists: 106 of them were sitting in the tree when it was
        written.

        Read off the masked source, so a log message holding an arrow is code to
        nobody. Switch arms - case 'n' -> ... - are skipped: the literal is
        masked away and what is left looks exactly like a lambda.
    #>
    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $source = [System.IO.File]::ReadAllText($file.FullName)
            $masked = Hide-StringsAndComments $source
            $path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'

            $unused = [System.Collections.Generic.List[object]]::new()

            foreach ($match in [regex]::Matches($masked, '\(([^()]*)\)\s*->'))
            {
                $inner = $match.Groups[1].Value
                if ($inner -notmatch '^\s*\w+(\s*,\s*\w+)*\s*$')
                {
                    continue
                }

                $body = Get-LambdaBody $masked ($match.Index + $match.Length)
                foreach ($name in [regex]::Matches($inner, '\w+'))
                {
                    if ($name.Value -eq '_')
                    {
                        continue
                    }
                    if ($body -match ('\b' + [regex]::Escape($name.Value) + '\b'))
                    {
                        continue
                    }
                    $unused.Add(@{ At = $match.Groups[1].Index + $name.Index; Name = $name.Value })
                }
            }

            foreach ($match in [regex]::Matches($masked, '(?<![\w.)])(\w+)\s*->'))
            {
                $name = $match.Groups[1].Value
                if ($name -in @('_', 'default', 'case'))
                {
                    continue
                }

                $lineStart = $masked.LastIndexOf("`n",[Math]::Max(0, $match.Index - 1)) + 1
                if ($masked.Substring($lineStart, $match.Index - $lineStart) -match '\b(case)\b')
                {
                    continue
                }

                $body = Get-LambdaBody $masked ($match.Index + $match.Length)
                if ($body -match ('\b' + [regex]::Escape($name) + '\b'))
                {
                    continue
                }
                $unused.Add(@{ At = $match.Groups[1].Index; Name = $name })
            }

            foreach ($found in $unused)
            {
                [pscustomobject]@{
                    Path = $path
                    Line = ($masked.Substring(0, $found.At) -split "`n").Count
                    Inspection = 'UnusedLambdaParameter'
                    Severity = 'ERROR'
                    Message = "Parameter '$( $found.Name )' is never used. Java 21 named it: write _ instead"
                }
            }
        }
    }
}

function Read-AbbreviatedNames([string[]] $scopes)
{
    <#
        A name that abbreviates a word (#426). Muteb, 13 October 2026: "no
        abbreviations in our plugin at all. except java and all known like
        exception -> ex". CLAUDE.md's Abbreviations table is the one list of
        what may stay; this rule holds back the ones the sweep removed, so a
        tp, a dto or a btn does not come back one paste at a time.

        Read off the masked source. A name after a dot is a member of
        someone's API - FileUtil.copyDir, cell.getTcPr - and is not Testin's
        to rename, so only a bare name counts: a declaration, a parameter, a
        lambda parameter or a use of one.
    #>
    $whole = '^(p|tc|tcs|tp|ts|tr|tsp|trp|dto|dir|dirs|pm|pc|vf|uvf|cm|gbc|fm|btn|lbl|idx|str|bg|fg|col|cols|attr|gt|baos|fos|os|wb|ws|tw|env|sp|doc|para|msg|src|desc|pos|fmt|sel|seq|pref|obj|evt|num|pri|sev|pkg|[a-d]|f|h|[k-w]|z)$'
    $part = '(?<=[a-z0-9])(Tc|Tp|Dir|Btn|Txt|Lbl|Col|Idx|Str|Bg|Fg|Pt|Attr|Pkg|Vf|Dto)(?=[A-Z0-9]|$)'
    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $masked = Hide-StringsAndComments ([System.IO.File]::ReadAllText($file.FullName))
            $path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
            $seen = @{ }

            foreach ($match in [regex]::Matches($masked, '(?<![\w$.@])([a-z][\w$]*)(?![\w$])'))
            {
                $name = $match.Groups[1].Value
                if ($seen.ContainsKey($name) -or $name -cnotmatch $whole -and $name -cnotmatch $part)
                {
                    continue
                }
                $before = $masked.Substring(0, $match.Index).TrimEnd()
                if ($before.EndsWith('::') -or $before -match '\b(package|import)\s+[\w.]*$')
                {
                    continue
                }
                $seen[$name] = $true

                [pscustomobject]@{
                    Path = $path
                    Line = ($masked.Substring(0, $match.Index) -split "`n").Count
                    Inspection = 'AbbreviatedName'
                    Severity = 'ERROR'
                    Message = "'$name' abbreviates a word. Spell it out; CLAUDE.md's Abbreviations table lists the Java idioms that stay"
                }
            }
        }
    }
}

function Read-WrappedDeclarations([string] $scope)
{
    <#
        A method declaration is one line. A signature is one thing to read, and
        split over four lines it is four things to put back together before the
        first question about the method can be asked.

        No IntelliJ inspection says this. The platform has code style settings
        for how to wrap a signature and none for refusing to, so it is checked
        here and reported beside the inspector's own findings - one list to
        read, one gate to pass.

        Matched on the modifiers rather than on the parenthesis, which is what
        keeps a wrapped call - stream() on one line and .filter(..) on the next
        - from being read as a declaration. A record header is matched with or
        without them, because "record Name(" is never a call: six package-private
        records sat wrapped while the gate read clear.
    #>
    $declaration = '^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)+[^;=()]*?\b\w+\s*\('
    $record = '^\s*(?:\w+\s+)*record\s+\w+(?:<[^>]*>)?\s*\('

    foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
    {
        $number = 0
        foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
        {
            $number++

            $trimmed = $text.Trim()
            if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
            {
                continue
            }
            if ($text -notmatch $declaration -and $text -notmatch $record)
            {
                continue
            }

            # Still open at the end of the line, so the signature carries on to
            # the next one. A declaration that closes on its own line is fine,
            # however long it is.
            if ([regex]::Matches($text, '\(').Count -le [regex]::Matches($text, '\)').Count)
            {
                continue
            }

            [pscustomobject]@{
                Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                Line = $number
                Inspection = 'WrappedMethodDeclaration'
                Severity = 'ERROR'
                Message = "A method declaration belongs on one line: $trimmed"
            }
        }
    }
}

function Read-HelpersNamedLikeTests([string[]] $scopes)
{
    <#
        A helper in a JUnit 3 test whose name starts with "test". JUnit 3 runs
        every public void test*() by name, so the IDE's JUnitMalformedDeclaration
        reads any other method named that way as a broken test: testRuns()
        returning a service, testCase(description) building one. Three reached
        Muteb's IDE one paste at a time, and sixteen more stood in the tree.

        A JUnit 3 class is one that extends an *IdeTest base or a platform
        TestCase; the TestNG tests name their methods freely.
    #>
    $junit3 = 'class\s+\w+(?:<[^>]*>)?\s+extends\s+\w*(?:IdeTest|TestCase)\b'
    $helper = '^\s*(?:(?:public|protected|private|static|final|abstract|synchronized)\s+)+[^;=()]*?\b(test\w*)\s*\('
    $test = '^\s*public\s+void\s+test\w*\s*\(\s*\)'

    foreach ($scope in $scopes)
    {
        if (-not (Test-Path $scope))
        {
            continue
        }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)
            if (-not ($lines -match $junit3))
            {
                continue
            }

            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i] -notmatch $helper -or $lines[$i] -match $test)
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line = $i + 1
                    Inspection = 'HelperNamedLikeTest'
                    Severity = 'ERROR'
                    Message = "JUnit 3 reads $( $Matches[1] ) as a test because of its name. A helper's name does not start with 'test': aTestCase, theTestRuns."
                }
            }
        }
    }
}

function Test-DocComment([string[]] $lines, [int] $close)
{
    <#
        Whether the block comment that ends at $close is a doc comment.

        Only /** is documentation. Every file opens with the copyright notice in
        a plain /* block, and javac never held that as documentation - so the
        package javadoc below it in a package-info.java is still attached to the
        package, and nothing is thrown away. Asking only whether a line ends in
        "*/" read those three files as orphaned the day the headers landed
        (#303).

        Walks back to the line that opened the block, which is the line itself
        for a one-line /** .. */.
    #>
    for ($i = $close; $i -ge 0; $i--) {
        if (-not $lines[$i].Trim().StartsWith('/*'))
        {
            continue
        }
        return $lines[$i].Trim().StartsWith('/**')
    }

    return $false
}

function Read-OrphanedJavadoc([string[]] $scopes)
{
    <#
        A javadoc block the compiler throws away.

        Java keeps only the last doc comment before a declaration, so a block
        followed by another block documents nothing, and the member the first
        one was written for is left bare. Twenty were found on 12 September
        2026 and eight of them carried a rule marker - so a grep from the
        documentation landed on the wrong method, and the method that carries
        the rule cited nothing (#66, finding 76).

        Mechanically checkable, which is what makes it worth a rule rather than
        a sweep: the shape is exact. A block whose next line that is neither
        blank nor an annotation of its own opens another block.

        "Of its own" is the whole subtlety. An annotation on a line by itself
        is skipped and one carrying a declaration after it is not - "@NotNull
        String cardTitle(..)" is the member, and reading that as an annotation
        made every documented method of every interface in the tree look
        orphaned.

        One file is frozen by name, the way ArchitectureTest freezes a known
        violation: RunItems's block separates a @JsonIgnore from the method
        it was written for, which is a data bug rather than a documentation
        one - the derived field reaches every test run file a tester commits. It
        is #73's, and deleting that name is how that story closes.
    #>
    $frozen = @('src/main/java/org/testin/model/testrun/RunItems.java')

    foreach ($scope in $scopes)
    {
        if (-not (Test-Path $scope))
        {
            continue
        }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
            if ($frozen -contains $path)
            {
                continue
            }

            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i].Trim() -notmatch '\*/$')
                {
                    continue
                }
                if (-not (Test-DocComment $lines $i))
                {
                    continue
                }

                $j = $i + 1
                while ($j -lt $lines.Count -and ($lines[$j].Trim() -eq '' -or $lines[$j].Trim() -match '^@[A-Za-z_][\w.]*(\(.*\))?$'))
                {
                    $j++
                }

                if ($j -ge $lines.Count -or $lines[$j].Trim() -notmatch '^/\*\*')
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $path
                    Line = $i + 1
                    Inspection = 'OrphanedJavadoc'
                    Severity = 'ERROR'
                    Message = "A second block opens at line $( $j + 1 ), so javac keeps that one and this documents nothing."
                }
            }
        }
    }
}

function Read-HtmlParagraphInMarkdown
{
    <#
        A paragraph break written as HTML in a Markdown file.

        A line holding only <p> opens a raw HTML block that runs to the next
        blank line. Inside it backticks stop marking code, so a placeholder
        such as git show af5f3013:<path> is read as an element that is never
        closed, and the IDE says so. CLAUDE.md carried seven of them until
        22 September 2026. A blank line is the Markdown for the same break,
        indented under a list item when the paragraph belongs to one.

        Every tracked Markdown file is read, the repository's root included,
        which is where CLAUDE.md sits and where the inspector does not look.
    #>
    foreach ($relative in @(git -C $repo ls-files '*.md'))
    {
        # A page deleted in the working tree is still tracked until the delete is staged,
        # and a gate that throws on it reports nothing at all.
        $path = Join-Path $repo $relative
        if (-not (Test-Path $path))
        {
            continue
        }

        $lines = [System.IO.File]::ReadAllLines($path)

        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim() -ne '<p>')
            {
                continue
            }

            [pscustomobject]@{
                Path = $relative
                Line = $i + 1
                Inspection = 'HtmlParagraphInMarkdown'
                Severity = 'ERROR'
                Message = 'A bare <p> opens a raw HTML block, where backticks stop being code. Separate the paragraphs with a blank line.'
            }
        }
    }
}

function Read-UnlistedRules
{
    <#
        A rule this script reports that CONTRIBUTING.md's table of its rules
        does not name, or a row in that table naming a rule it no longer
        reports.

        The rules were listed in three places - this header, inspect.yml and
        that table - and by 4 October 2026 the three said eleven, ten and
        nine, with two rules in none of them. The table is the one list now,
        and this keeps it true: the names are read from the Inspection
        values in this file, so a rule cannot be added without its row.
    #>
    $contributing = Join-Path $repo 'CONTRIBUTING.md'
    $lines = [System.IO.File]::ReadAllLines($contributing)
    $intro = [array]::FindIndex($lines, [Predicate[string]]{ param($line) $line.StartsWith("The script's own rules") })

    $listed = @()
    for ($i = $intro + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\|\s*`(\w+)`')
        {
            $listed += $Matches[1]
        }
        elseif ($listed -and -not $lines[$i].StartsWith('|'))
        {
            break
        }
    }

    $reported = @([regex]::Matches([System.IO.File]::ReadAllText($PSCommandPath), "Inspection\s*=\s*'(\w+)'") |
            ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)

    foreach ($rule in $reported | Where-Object { $_ -notin $listed })
    {
        [pscustomobject]@{
            Path = 'CONTRIBUTING.md'
            Line = $intro + 1
            Inspection = 'UnlistedRule'
            Severity = 'ERROR'
            Message = "tools/inspect.ps1 reports $rule, and the table of the script's own rules does not name it."
        }
    }
    foreach ($rule in $listed | Where-Object { $_ -notin $reported })
    {
        [pscustomobject]@{
            Path = 'CONTRIBUTING.md'
            Line = $intro + 1
            Inspection = 'UnlistedRule'
            Severity = 'ERROR'
            Message = "The table of the script's own rules names $rule, and tools/inspect.ps1 no longer reports it."
        }
    }
}

function Get-CellBorders([string] $line)
{
    $borders = [System.Collections.Generic.List[int]]::new()
    $inCode = $false
    for ($i = 0; $i -lt $line.Length; $i++) {
        $c = $line[$i]
        if ($c -eq '\')
        {
            $i++; continue
        }
        if ($c -eq '`')
        {
            $inCode = -not $inCode
        }
        elseif ($c -eq '|' -and -not $inCode)
        {
            $borders.Add($i)
        }
    }
    $borders -join ','
}

function Read-MisalignedMarkdownTable
{
    <#
        A Markdown table whose column borders do not line up, row under row.

        The rendered page does not care, but the file is read as text far
        more often than rendered - in a diff, in a terminal, in the IDE - and
        there a table that is not padded is a wall of pipes. A row added
        without re-padding the rest is how it happens: CONTRIBUTING.md's
        table of checks took one on 4 October 2026. The IDE's Markdown
        formatter pads every cell to its column's width, and a table it has
        formatted passes.

        A border is a | outside a code span and not escaped, so a pipe inside
        backticks is cell text. Code fences are skipped.
    #>
    foreach ($relative in @(git -C $repo ls-files '*.md'))
    {
        $path = Join-Path $repo $relative
        if (-not (Test-Path $path))
        {
            continue
        }

        $lines = [System.IO.File]::ReadAllLines($path)
        $fenced = $false
        $header = $null

        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            if ( $line.TrimStart().StartsWith('```'))
            {
                $fenced = -not $fenced
            }
            if ($fenced -or -not $line.StartsWith('|'))
            {
                $header = $null; continue
            }

            $borders = Get-CellBorders $line
            if ($null -eq $header)
            {
                $header = $borders; continue
            }
            if ($borders -eq $header)
            {
                continue
            }

            [pscustomobject]@{
                Path = $relative
                Line = $i + 1
                Inspection = 'MisalignedMarkdownTable'
                Severity = 'ERROR'
                Message = "This row's column borders are not under the header's. Pad every cell to its column's width, as the IDE's Reformat Code does."
            }
        }
    }
}

function Find-CommentStart([string] $line)
{
    <#
        Where a comment starts on a line of code, or -1. A // inside a string -
        an address, a regular expression - is not a comment, so the quotes are
        followed rather than the slashes.
    #>
    $quote = ''

    for ($j = 0; $j -lt $line.Length; $j++) {
        $c = $line[$j]

        if ($quote)
        {
            if ($c -eq '\')
            {
                $j++; continue
            }
            if ($c -eq $quote)
            {
                $quote = ''
            }
            continue
        }

        if ($c -eq '"' -or $c -eq "'")
        {
            $quote = $c; continue
        }
        if ($c -eq '/' -and $j + 1 -lt $line.Length -and ($line[$j + 1] -eq '/' -or $line[$j + 1] -eq '*'))
        {
            return $j
        }
    }

    return -1
}

function Read-QualifiedClassNames([string[]] $scopes)
{
    <#
        A class is named by its import, never by its package path.

        No IntelliJ inspection says this. The platform offers to add the import
        and never refuses the long form, so a one-line fix that writes
        new java.awt.Color(...) rather than importing Color compiles, reviews
        clean, and reads as a different house style from every line around it.

        Three things are not code and are skipped. A string, because a persisted
        key reads like a package path and is one - @State(name =
        "testin.settings.AppSettingsState"). A text block, because the sample
        stack traces the bug and details tests are built from are full of
        java.lang.AssertionError and none of it is a reference. And a line under
        //noinspection, up to the semicolon that ends its statement, which is
        how the one exception the skill allows is written down: a simple name
        that already means another class in that file, as Testin's own Logger
        does in org.testin.logger.
    #>
    $qualified = '(?<![\w."])(?:[a-z][a-z0-9_]*\.){2,}[A-Z]\w*'

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $number = 0
            $inTextBlock = $false
            $excused = $false

            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++
                $trimmed = $text.Trim()

                $fences = [regex]::Matches($text, '"""').Count
                if ($inTextBlock)
                {
                    if ($fences % 2 -eq 1)
                    {
                        $inTextBlock = $false
                    }
                    continue
                }
                if ($fences % 2 -eq 1)
                {
                    $inTextBlock = $true; continue
                }

                if ( $trimmed.StartsWith('//noinspection'))
                {
                    $excused = $true; continue
                }
                if ($excused)
                {
                    if ( $trimmed.EndsWith(';'))
                    {
                        $excused = $false
                    }
                    continue
                }

                if ($trimmed.StartsWith('import ') -or $trimmed.StartsWith('package '))
                {
                    continue
                }
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*'))
                {
                    continue
                }

                $bare = [regex]::Replace($text, '"[^"]*"', '""')
                $match = [regex]::Match($bare, $qualified)
                if (-not $match.Success)
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1).Replace('\', '/')
                    Line = $number
                    Inspection = 'QualifiedClassName'
                    Severity = 'ERROR'
                    Message = "A class is named by its import, never by its package path: $( $match.Value )"
                }
            }
        }
    }
}

function Read-ColorCodes([string[]] $scopes)
{
    <#
        A color is IntelliJ's own, never a number.

        #348 took every color literal out and docs/internal/customizations.md
        records that nothing is left. A color written as RGB or hex is right in
        one theme and wrong in another, and it is a second owner for a color the
        platform or the house already names: JBColor's constants,
        JBUI.CurrentTheme, util/Icons for icon colors, util/FixedColors. A color
        built from another named color, new JBColor(Color.WHITE, Color.WHITE), is
        not a number and passes.
    #>
    $code = 'new\s+(JB)?Color\s*\(\s*(0x|\d)|new\s+JBColor\s*\([^;]*,\s*(0x|\d)|Color\.decode\s*\(|ColorUtil\.fromHex\s*\('

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++
                $match = [regex]::Match($text, $code)
                if (-not $match.Success)
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1).Replace('\', '/')
                    Line = $number
                    Inspection = 'ColorCode'
                    Severity = 'ERROR'
                    Message = "A color is IntelliJ's own or a house owner's (Icons, FixedColors), never RGB or hex: $( $match.Value )"
                }
            }
        }
    }
}

function Read-UnicodeEscapes([string[]] $scopes)
{
    <#
        A character is written as itself, never as a unicode escape.

        IntelliJ's UnnecessaryUnicodeEscape reports only a character the file
        could hold as it is, and its warning still reaches the editor. An escape
        is unreadable for every other character too: a control character is
        Character.toString(0x1E), which says what it is without one. Java turns
        an escape into its character before it reads the line, so a comment, a
        string and a text block are all checked. An escaped backslash before
        the u is not an escape and is skipped.
    #>
    $escape = '(?<!\\)\\u+[0-9a-fA-F]{4}'

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName))
            {
                $number++
                $match = [regex]::Match($text, $escape)
                if (-not $match.Success)
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1).Replace('\', '/')
                    Line = $number
                    Inspection = 'UnicodeEscape'
                    Severity = 'ERROR'
                    Message = "Write the character itself, or Character.toString(0x..) for one that cannot be seen: $( $match.Value )"
                }
            }
        }
    }
}

function Read-UnformattedJava([object] $inspector, [string[]] $scopes)
{
    <#
        A Java file IntelliJ's Reformat Code would change.

        Muteb reformats in the IDE before committing, and asked CI to say when a
        file was pushed without it (7 October 2026). This runs the IDE's own
        command-line formatter in dry-run mode with the project's code style, so
        the answer is the IDE's. Java only: two reformats in a row changed no
        Java file, but moved a Markdown table's separator row three spaces right
        each time and flipped a space in PowerShell back and forth, and the dry
        run says build.gradle.kts needs reformatting when formatting it changes
        nothing. A check on any of those would fail every push.

        The formatter exits 0 either way, so its verdict is read from what it
        prints. It cannot share the inspector's IDE directories while the
        inspector runs, and is given its own.
    #>
    $scratch = Join-Path ([System.IO.Path]::GetTempPath()) 'testin-format'
    if (Test-Path $scratch)
    {
        Remove-Item -Path $scratch -Recurse -Force -ErrorAction SilentlyContinue
    }
    New-Item -ItemType Directory -Force -Path $scratch | Out-Null

    $s = $scratch -replace '\\', '/'
    $propsFile = Join-Path $scratch 'idea.properties'
    @"
idea.config.path=$s/config
idea.system.path=$s/system
idea.log.path=$s/log
idea.plugins.path=$s/plugins
"@ | Set-Content -Path $propsFile -Encoding utf8

    $codeStyle = Join-Path $repo '.idea' 'codeStyles' 'Project.xml'
    $env:IDEA_PROPERTIES = $propsFile
    try
    {
        $said = & $inspector.Formatter -dry -r -s $codeStyle -m '*.java' @scopes 2>&1
    }
    finally
    {
        Remove-Item Env:\IDEA_PROPERTIES -ErrorAction SilentlyContinue
    }

    foreach ($line in $said)
    {
        $match = [regex]::Match("$line", '^Checking (.+)\.\.\.Needs reformatting$')
        if (-not $match.Success)
        {
            continue
        }

        [pscustomobject]@{
            Path = ([System.IO.Path]::GetFullPath($match.Groups[1].Value)).Substring($repo.Length + 1).Replace('\', '/')
            Line = 1
            Inspection = 'UnformattedJava'
            Severity = 'ERROR'
            Message = 'Reformat Code in the IDE changes this file. Reformat it before committing.'
        }
    }
}

function Read-UnusedImports([string[]] $scopes)
{
    <#
        An import the file does not need: nothing in its code names the class,
        or the class is in the file's own package or in java.lang, where the
        compiler finds it without one.

        The profile turned UNUSED_IMPORT on in db8617b0 and dropped it in
        920a865b without a word, and javac says nothing, so three were left
        behind on 7 October 2026 when ArrayList became List, beside 25 that
        the #394 package moves stranded in their new package. An IDE reformat
        found them, one paste from Muteb's screen. A use inside a string or a
        comment is not a use, so both are blanked before the names are read.
    #>
    $declared = '(?m)^import\s+(static\s+)?([\w.]+)\s*;'

    foreach ($scope in $scopes)
    {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File)
        {
            $source = [System.IO.File]::ReadAllText($file.FullName)
            $package = [regex]::Match($source, '(?m)^package\s+([\w.]+)\s*;').Groups[1].Value
            $code = [regex]::Replace((Hide-StringsAndComments $source), '(?m)^(import|package)\s[^;]*;', '')

            foreach ($import in [regex]::Matches($source, $declared))
            {
                $name = $import.Groups[2].Value
                $owner = $name.Substring(0,$name.LastIndexOf('.'))
                $simple = $name.Substring($name.LastIndexOf('.') + 1)
                $isStatic = $import.Groups[1].Success

                $why = if (-not $isStatic -and ($owner -eq $package -or $owner -eq 'java.lang'))
                {
                    "$simple is in $owner, which needs no import"
                }
                elseif (-not [regex]::IsMatch($code, "\b$([regex]::Escape($simple) )\b"))
                {
                    "nothing in the file names $simple"
                }
                else
                {
                    continue
                }

                [pscustomobject]@{
                    Path = $file.FullName.Substring($repo.Length + 1).Replace('\', '/')
                    Line = ($source.Substring(0, $import.Index) -split "`n").Count
                    Inspection = 'UnusedImport'
                    Severity = 'ERROR'
                    Message = "Remove the import of ${name}: $why"
                }
            }
        }
    }
}

function Read-ComplexMethods
{
    <#
        What pmdMain reports: a production method over the cognitive complexity
        or if-nesting limit (#378), one finding per method, under the rule
        name PMD gives it.

        CI's Build runs pmdMain before the IDE tests, and a quick run did not,
        so fa0c95529 pushed a breach nothing local reported. Build stayed red
        for three pushes and skipped the IDE tests on each (#66, finding 388).
        PMD needs no compile, so this costs Gradle's start and about fifteen
        seconds. A pmdMain that fails without naming a method is one finding,
        so a broken PMD run never reads as a clear gate.
    #>
    $gradlew = Join-Path $repo $( if ($IsWindows)
    {
        'gradlew.bat'
    }
    else
    {
        'gradlew'
    } )
    $output = @(& $gradlew -p $repo pmdMain --console=plain 2>&1 | ForEach-Object { "$_" })
    $failed = $LASTEXITCODE -ne 0
    $found = 0

    foreach ($line in $output)
    {
        if ($line -notmatch '^(.+\.java):(\d+):\s+(\w+):\s+(.+)$')
        {
            continue
        }
        $found++
        [pscustomobject]@{
            Path = [System.IO.Path]::GetFullPath($Matches[1]).Substring($repo.Length + 1).Replace('\', '/')
            Line = [int]$Matches[2]
            Inspection = $Matches[3]
            Severity = 'ERROR'
            Message = $Matches[4]
        }
    }

    if ($failed -and -not $found)
    {
        [pscustomobject]@{
            Path = 'build.gradle.kts'
            Line = 1
            Inspection = 'PmdDidNotRun'
            Severity = 'ERROR'
            Message = "pmdMain failed without naming a method: $( ($output | Select-Object -Last 5) -join ' / ' )"
        }
    }
}

function Read-NonMarkerComments([string[]] $scopes)
{
    <#
        A comment that is not a marker.

        The code carries a UC- or Rule- marker and nothing else. The marker says
        which documented behavior this is, docs/ says what that behavior is, and
        the commit message says why the code is shaped this way - where git
        blame reaches it and where it cannot drift from the code.

        27,483 comment lines came out of src/main in af5f3013, and 4,256 came
        out of the tests on 22 September 2026. Nothing stopped the next one
        until this rule, and a javadoc block was back in src/test the same week.

        Three kinds stay. The Apache notice every file opens with, which
        MissingCopyright checks instead. The ones a machine reads - //noinspection,
        // @formatter:off - because they are part of the build rather than prose.
        And the markers themselves. A // inside a text block or a string is not
        a comment and is left alone.
    #>
    $keep = '^//\s*(UC-|Rule-|noinspection|@formatter)'

    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)
            $inHeader = $lines.Count -gt 0 -and $lines[0].StartsWith('/*')
            $inBlock = $false
            $inTextBlock = $false

            for ($i = 0; $i -lt $lines.Count; $i++) {
                $text = $lines[$i]
                $trimmed = $text.Trim()

                if ($inHeader) {
                    if ($trimmed.EndsWith('*/')) { $inHeader = $false }
                    continue
                }

                if ($inBlock) {
                    if ($trimmed.Contains('*/')) { $inBlock = $false }
                    continue
                }

                if ($inTextBlock) {
                    if ((($text.Split('"""', [System.StringSplitOptions]::None).Count - 1) % 2) -eq 1) { $inTextBlock = $false }
                    continue
                }

                $at = -1

                if ($trimmed.StartsWith('//')) {
                    if ($trimmed -match $keep) { continue }
                    $at = $text.IndexOf('//')
                } elseif ($trimmed.StartsWith('/*')) {
                    $at = $text.IndexOf('/*')
                    if (-not $trimmed.Contains('*/')) { $inBlock = $true }
                } else {
                    $at = Find-CommentStart $text
                    if ($at -lt 0) {
                        if ((($text.Split('"""', [System.StringSplitOptions]::None).Count - 1) % 2) -eq 1) { $inTextBlock = $true }
                        continue
                    }
                    if ($text.Substring($at).StartsWith('/*') -and -not $text.Substring($at).Contains('*/')) { $inBlock = $true }
                }

                [pscustomobject]@{
                    Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line       = $i + 1
                    Inspection = 'NonMarkerComment'
                    Severity   = 'ERROR'
                    Message    = "The code carries a UC- or Rule- marker and nothing else: $trimmed"
                }
            }
        }
    }
}

function Read-MissingCopyright([string[]] $scopes) {
    <#
        A file that does not say who owns it or on what terms.

        The license used to live in LICENSE at the repository root and nowhere
        else, so a file that traveled out of the repository - into a jar, a
        decompiler, a search result, a fork - traveled without it. All 656 got
        the Apache 2.0 notice from LICENSE's own appendix in #303, and this is
        what keeps the 657th from being the one that has none: a header nothing
        checks is the header missing from the next file somebody adds.

        The expected first two lines are read from LICENSE rather than written
        out here, so the year and the owner have one owner. Changing the year
        there is meant to be loud: every file disagreeing with it is reported.

        The first non-blank line, not the first line, so a stray blank at the
        top is a header in the wrong place rather than no header at all - the
        finding says the same thing either way.
    #>
    $appendix = [System.IO.File]::ReadAllLines((Join-Path $repo 'LICENSE')) |
        Where-Object { $_ -cmatch '^\s*Copyright \d{4} \S' } |
        Select-Object -First 1

    if (-not $appendix) {
        throw "No 'Copyright <year> <owner>' line in LICENSE, so there is nothing to check the file headers against."
    }

    $expected = '* ' + $appendix.Trim()

    foreach ($scope in $scopes) {
        if (-not (Test-Path $scope)) { continue }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            $first = 0
            while ($first -lt $lines.Count -and $lines[$first].Trim() -eq '') { $first++ }

            $opens = ($first -lt ($lines.Count - 1)) -and ($lines[$first].Trim() -eq '/*')
            if ($opens -and $lines[$first + 1].Trim() -ceq $expected) { continue }

            [pscustomobject]@{
                Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                Line       = 1
                Inspection = 'MissingCopyright'
                Severity   = 'ERROR'
                Message    = "No copyright header. A file opens with the Apache 2.0 notice from LICENSE - '/*' and then '$expected' - above everything else, one blank line before package."
            }
        }
    }
}

function Write-Reports([object[]] $problems, [string] $outPath) {
    # A project model that did not resolve floods the output with unresolved
    # symbols and every other count becomes meaningless. Say so rather than
    # leaving it to the reader to notice.
    $unresolved = @($problems | Where-Object { $_.Message -match 'Cannot resolve|cannot be resolved' }).Count
    if ($unresolved -gt 20) {
        Write-Warning "$unresolved unresolved-symbol findings. The Gradle project model probably did not load, or the bundled Lombok plugin was inactive - treat the counts below as unusable."
    }

    $summary = Join-Path $outPath 'summary.txt'
    $findings = Join-Path $outPath 'findings.txt'

    $lines = @("$($problems.Count) findings", '', 'By inspection:', '')
    $lines += $problems | Group-Object Inspection | Sort-Object Count -Descending |
        ForEach-Object { '{0,6}  {1}' -f $_.Count, $_.Name }

    $lines += @('', 'By package:', '')
    $lines += $problems |
        Group-Object { ($_.Path -replace '^src/(main|test)/java/', '' -replace '/[^/]+$', '') } |
        Sort-Object Count -Descending |
        ForEach-Object { '{0,6}  {1}' -f $_.Count, $_.Name }

    $lines | Set-Content -Path $summary -Encoding utf8

    $problems | Sort-Object Path, Line |
        ForEach-Object { '{0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message } |
        Set-Content -Path $findings -Encoding utf8

    Write-Host ''
    Get-Content $summary | Select-Object -First 25
    Write-Host ''
    Write-Host "Full list: $findings"
    Write-Host "Summary:   $summary"
}

$outPath = Join-Path $repo $OutputDir

if (-not $ReportOnly -and -not $Quick) {
    # The single cleanup: a stale XML file would be counted as part of this run,
    # and a stale index would be analysed in place of the source. Both live here.
    # The contents, not the directory: gradle.properties sets org.gradle.vfs.watch,
    # so the daemon holds a handle on it and removing the directory itself fails.
    # A cache file an earlier inspector still holds open is allowed to survive -
    # it costs a slower run - but a surviving report is counted twice, so say so.
    if (Test-Path $outPath) { Get-ChildItem -Path $outPath -Recurse -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue }
    if (Get-ChildItem -Path (Join-Path $outPath '*.xml') -ErrorAction SilentlyContinue) {
        throw "XML from an earlier run is still in $outPath and could not be deleted. Close whatever holds it open and run again."
    }
    Invoke-Inspector (Resolve-Inspector) $outPath
}

# The inspector's findings and this script's own, as one list. Write-Reports
# groups by inspection name and knows nothing about where a finding came from,
# which is why the wrapped-declaration check produces the same shape.
# Every production source root, so the one-line-signature gate covers the
# content modules too. An explicit -Subdirectory still wins, for the caller
# narrowing a run to one place.
$scopes = if ($narrowed) { @(Join-Path $repo $Subdirectory) } else { Get-SourceRoots }

$problems = if ($Quick) { @() } else { @(Read-Problems $outPath) }
if (-not $narrowed -and -not $Quick) { $problems = @(Select-Inspected $problems) }

# Two kinds of rule, and they read different trees.
#
# How code is written - one-line signatures, Lombok for the boilerplate, a doc
# block javac keeps, the licence header - holds for every Java file, tests
# included: CLAUDE.md states them for the whole tree, and a rule that looks at
# only part of it is a rule the rest can quietly break. Until #66 finding 106
# only the header read the tests, so a hand-written private constructor sat in
# src/test with nothing to see it.
#
# What a tester reads - a caption written in two places - is production alone.
# A string duplicated between two test harnesses is read by nobody.
#
# An explicit -Subdirectory still wins over both, for the caller narrowing a run.
$everyTree = if ($narrowed) { $scopes } else { Get-JavaSourceRoots }

foreach ($scope in $everyTree) { $problems += @(Read-WrappedDeclarations $scope) }
$problems += @(Read-HandWrittenPrivateConstructors $everyTree)
$problems += @(Read-OrphanedJavadoc $everyTree)
$problems += @(Read-MissingCopyright $everyTree)
$problems += @(Read-NonMarkerComments $everyTree)
$problems += @(Read-UnusedLambdaParameters $everyTree)
$problems += @(Read-QualifiedClassNames $everyTree)
$problems += @(Read-AbbreviatedNames $everyTree)
$problems += @(Read-UnicodeEscapes $everyTree)
$problems += @(Read-UnusedImports $everyTree)
if ($Quick)
{
    $problems += @(Read-ComplexMethods)
}
if (-not $Quick -and -not $ReportOnly)
{
    $problems += @(Read-UnformattedJava (Resolve-Inspector) $everyTree)
}
$problems += @(Read-ColorCodes $everyTree)
$problems += @(Read-HelpersNamedLikeTests $everyTree)
$problems += @(Read-HtmlParagraphInMarkdown)
$problems += @(Read-MisalignedMarkdownTable)
$problems += @(Read-UnlistedRules)

$problems += @(Read-DuplicatedDisplayStrings $scopes)
$problems += @(Read-ModelStatics @((Join-Path $repo 'src/main/java/org/testin/model')))

# The enums that name a test case's fields, compared against each other. Same
# constant, same concept, so the caption is the same question.
# A path that no longer exists is skipped in silence, so two of these four went
# on being listed after #111 moved them and the check quietly shrank to comparing
# one pair - printing "Gate clear" either way (#66, finding 98).
$problems += @(Read-DriftedCaptions @(
        (Join-Path $repo 'src/main/java/org/testin/testcase/TestSetEditorFields.java'),
        (Join-Path $repo 'src/main/java/org/testin/testrun/TestRunEditorFields.java'),
        (Join-Path $repo 'src/main/java/org/testin/testcase/CreateTestCaseFields.java'),
        (Join-Path $repo 'src/main/java/org/testin/testcase/UpdateTestCaseFields.java')))

# A quick run writes nothing: its list is partial by design, and findings.txt
# is what the last full run left to work from. Resolving a content module's
# callers and taking the vocabulary both read that full list, so both wait for
# it too.
if (-not $Quick) {
    $problems = Resolve-CrossModuleUsages $problems

    Write-Reports $problems $outPath

    # The whole vocabulary, beside the findings. findings.txt says what to act on;
    # this says what already exists, which is what stops the next duplicate.
    Write-DisplayStringInventory $scopes $outPath
}
else {
    Write-Host ''
    Write-Host "Quick run: this script's own rules only, over $($everyTree.Count) source root(s). The IDE's own findings come from CI." -ForegroundColor Cyan
    $problems | Sort-Object Path, Line | ForEach-Object {
        Write-Host ('  {0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message)
    }
}

function Read-Exceptions {
    <#
        The exceptions qodana.yaml lists under exclude: an inspection name, or
        All, and the paths where it is not reported - none meaning everywhere.
        Qodana reads the same list in CI, which is why it lives there rather
        than here.
    #>
    $exceptions = @()
    $inExclude = $false
    foreach ($line in [System.IO.File]::ReadAllLines((Join-Path $repo 'qodana.yaml'))) {
        if ($line -match '^[^\s#]') { $inExclude = $line -match '^exclude:'; continue }
        if (-not $inExclude) { continue }
        if ($line -match '^\s*- name:\s*(\S+)') {
            $exceptions += [pscustomobject]@{ Name = $Matches[1]; Paths = [System.Collections.Generic.List[string]]::new() }
        }
        elseif ($line -match '^\s*- "?([^"]+?)"?\s*$' -and $exceptions) {
            $exceptions[-1].Paths.Add($Matches[1].TrimEnd('/'))
        }
    }
    $exceptions
}

$exceptions = @(Read-Exceptions)

function Test-Gated([object] $problem) {
    $excused = $exceptions | Where-Object {
        (($_.Name -eq 'All' -and $problem.Inspection -ne $spelling) -or $_.Name -eq $problem.Inspection) -and
            (-not $_.Paths.Count -or @($_.Paths | Where-Object { $problem.Path -eq $_ -or $problem.Path.StartsWith("$_/") }).Count)
    }
    return -not @($excused).Count
}

$breaches = @($problems | Where-Object { Test-Gated $_ })

if ($breaches) {
    Write-Host ''
    Write-Host "$($breaches.Count) finding(s) the gate does not allow:" -ForegroundColor Red
    $breaches | Sort-Object Path, Line | ForEach-Object {
        Write-Host ('  {0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message)
    }

    # The run page, so a red main names its cause without opening the log (#323).
    $told = @("**$($breaches.Count) finding(s) the gate does not allow:**", '') +
        ($breaches | Sort-Object Path, Line | ForEach-Object { '- `{0}:{1}` [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message })
    if ($env:GITHUB_STEP_SUMMARY) { $told | Add-Content -Path $env:GITHUB_STEP_SUMMARY -Encoding utf8 }
    exit 1
}

Write-Host ''
Write-Host "Gate clear: no finding outside the $($exceptions.Count) exceptions qodana.yaml lists." -ForegroundColor Green
