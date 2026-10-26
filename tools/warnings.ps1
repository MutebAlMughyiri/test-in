<#
.SYNOPSIS
    Every warning the plugin produces, from every source, in one report.

.DESCRIPTION
    Six commands answered "what are all the warnings right now", one of them
    twenty minutes long and two only ever seen in a CI log. This gathers them.

    By default it reads what is already there: the compiler (seconds), the
    reports the last local run left on disk, and what the last CI run of each
    gate concluded. -Full runs the slow ones as well.

    Sources, in the order they are printed - the gated ones first, because they
    are the ones that fail a build:

      Inspections      tools/inspect.ps1, gated, runs in CI on every push
      Plugin Verifier  gradlew verifyPlugin, gated: any problem fails it
      Qodana           the free Community linter, run on this machine when asked
      Compiler         gradlew compileJava, -Xlint:deprecation,removal,cast
      Gradle           --warning-mode all

.PARAMETER Full
    Run the slow sources instead of reading their last result: the IDE
    inspection (about twenty minutes) and the Plugin Verifier (downloads six
    IDEs). Without it, nothing here takes longer than a compile.

.PARAMETER NoCi
    Skip everything that asks GitHub. Use it offline, or when gh is not
    authenticated.
#>
[CmdletBinding()]
param(
    [switch] $Full,
    [switch] $NoCi
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$gh = if (Get-Command gh -ErrorAction SilentlyContinue)
{
    'gh'
}
else
{
    'C:\Program Files\GitHub CLI\gh.exe'
}
$rows = [System.Collections.Generic.List[object]]::new()

function Add-Row
{
    param([string] $Source, [string] $Count, [string] $Gated, [string] $Where)
    $rows.Add([pscustomobject]@{ Source = $Source; Count = $Count; Gated = $Gated; Where = $Where })
}

function Get-LastRun
{
    param([string] $Workflow)
    if ($NoCi)
    {
        return $null
    }
    try
    {
        $json = & $gh run list --repo MutebAlMughyiri/test-in --workflow $Workflow --limit 1 --json databaseId,conclusion,status,headSha 2> $null
        if (-not $json)
        {
            return $null
        }
        return ($json | ConvertFrom-Json)[0]
    }
    catch
    {
        return $null
    }
}

Write-Host ''
Write-Host 'Gathering warnings. Nothing here changes a file.' -ForegroundColor Cyan
Write-Host ''

# --- Inspections -----------------------------------------------------------
if ($Full)
{
    Write-Host '  inspections: running the IDE, this takes about twenty minutes...' -ForegroundColor DarkGray
    & pwsh -NoProfile -File (Join-Path $PSScriptRoot 'inspect.ps1') | Out-Host
}

$findings = Join-Path $repoRoot '.inspection\findings.txt'
if (Test-Path $findings)
{
    $lines = @(Get-Content $findings | Where-Object { $_ -match '\S' })
    Add-Row 'Inspections' "$( $lines.Count )" 'yes' '.inspection/findings.txt'
}
else
{
    $run = Get-LastRun 'inspect.yml'
    $said = if ($run)
    {
        "last CI run $( $run.databaseId ): $( $run.conclusion )"
    }
    else
    {
        'never run here; no CI answer'
    }
    Add-Row 'Inspections' '-' 'yes' $said
}

# --- Plugin Verifier -------------------------------------------------------
if ($Full)
{
    Write-Host '  verifier: downloading IDEs and verifying...' -ForegroundColor DarkGray
    & ./gradlew verifyPlugin --console=plain | Out-Host
}

$run = Get-LastRun 'verify.yml'
$said = if ($run)
{
    "last CI run $( $run.databaseId ): $( $run.conclusion )"
}
else
{
    'CI only; no answer without gh'
}
Add-Row 'Plugin Verifier' '-' 'yes' $said

# --- Qodana ----------------------------------------------------------------
$sarif = Join-Path $repoRoot 'build/qodana/qodana.sarif.json'
if (Test-Path $sarif)
{
    $found = @((Get-Content $sarif -Raw | ConvertFrom-Json).runs[0].results).Count
    Add-Row 'Qodana' "$found" 'no' "build/qodana, the local run of $( (Get-Item $sarif).LastWriteTime.ToString('yyyy-MM-dd HH:mm') )"
}
else
{
    Add-Row 'Qodana' '-' 'no' 'not run on this machine yet: see CONTRIBUTING.md'
}

# --- Compiler --------------------------------------------------------------
$compile = & ./gradlew compileJava --rerun-tasks --console=plain 2>&1
$compilerWarnings = @($compile | Where-Object { $_ -match 'warning:|^Note:' })
Add-Row 'Compiler' "$( $compilerWarnings.Count )" 'no' 'gradlew compileJava, -Xlint:deprecation,removal,cast'

# --- Gradle ----------------------------------------------------------------
$gradle = & ./gradlew help --warning-mode all --console=plain 2>&1
$gradleWarnings = @($gradle | Where-Object { $_ -match 'Deprecated Gradle features|has been deprecated' })
Add-Row 'Gradle' "$( $gradleWarnings.Count )" 'no' 'gradlew --warning-mode all'

# --- The report ------------------------------------------------------------
Write-Host ''
$rows | Format-Table -AutoSize Source, Count, Gated, Where | Out-Host

if ($compilerWarnings.Count -gt 0)
{
    Write-Host 'Compiler:' -ForegroundColor Yellow
    $compilerWarnings | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
}

if ($gradleWarnings.Count -gt 0)
{
    Write-Host 'Gradle:' -ForegroundColor Yellow
    $gradleWarnings | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
}

if (-not $Full)
{
    Write-Host 'Read, not run: the inspection and the verifier report their last result.' -ForegroundColor DarkGray
    Write-Host 'Run them here with -Full.' -ForegroundColor DarkGray
    Write-Host ''
}
