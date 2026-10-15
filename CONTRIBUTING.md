# Contributing to Testin

Everything between a clone and a reviewed change. If something here is wrong or
missing, that is a bug in this page — say so.

## What you need

|                           |                                                                                                                                                                                                        |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JDK 25**                | The toolchain is pinned to it (`build.gradle.kts`). Gradle fetches one if your machine has none. IntelliJ 2026.2 runs on JetBrains Runtime 25, and its class files cannot be read by an older compiler |
| **IntelliJ IDEA**         | Any recent build. The sandbox the plugin runs in is downloaded by Gradle, not by you.                                                                                                                  |
| **PowerShell 7** (`pwsh`) | Only for `./gradlew inspect`. It is cross-platform, and the script asks for version 7.                                                                                                                 |

Nothing else. There is no local database, no service to start, no account.

## From clone to a running plugin

```bash
git clone https://github.com/MutebAlMughyiri/test-in.git
cd test-in
./gradlew compileJava test      # must be green
./gradlew runIde                # a sandbox IDE with Testin installed
```

`runIde` opens a second IDE with the plugin loaded. It keeps its own settings and
its own logs under `.sandbox/`, so nothing it does touches the IDE you work in.

**If `prepareSandbox` fails with *"cannot be performed on a file with a
user-mapped section open"***, a sandbox IDE is still running and holding the
jars. Close it; nothing else clears it.

### Where the sandbox keeps things

```
.sandbox/Testin/IU-<version>/log/
    idea.log      the IDE
    testin.log    the plugin
```

Both together, deliberately: `testin.log` is written to `PathManager.getLogPath()`
so *Help → Collect Logs and Diagnostic Data* bundles it with `idea.log`.

The plugin's log level defaults to **INFO**, and most of what is worth tracing is
`TestinLogger.debug` and `TestinLogger.trace`. Set **Settings → Testin → Log Level → TRACE**
in the sandbox before a session that matters, or the log will be nearly empty for
the paths you care about.

### The run configurations are committed

Open the project in IntelliJ IDEA and four are already there, for the same reason
the inspection profile and the code style are committed — nobody should have to
retype them:

|                             |                                                                                                            |
|-----------------------------|------------------------------------------------------------------------------------------------------------|
| **Run IDE with Testin**     | `runIde`. The one you want.                                                                                |
| **Run PyCharm with Testin** | `runPyCharm`. Proves the plugin still loads where there is no Java; downloads a second IDE the first time. |
| **Tests**                   | `compileJava test`                                                                                         |
| **Inspection gate**         | `inspect`                                                                                                  |

## Two branches at once means two working trees

A clone is one working tree, and `git checkout` moves **all** of it. So two
pieces of work on two branches cannot share one directory: switching branches
under work in progress takes the files out from under it, and nothing warns you.

```bash
git worktree add ../testin-<what> <branch>     # its own directory, same repository
git worktree remove ../testin-<what>           # when the work is merged or abandoned
```

Each worktree gets its own `build/`, so the first Gradle run in a new one is a
full build. The downloaded IDE and the dependency cache are shared through
`~/.gradle`, so it is minutes rather than a fresh setup.

This is not hypothetical: it was written down after a branch was checked out in a
directory that already had work running in it, which would have destroyed that
work had it not been noticed immediately.

## The checks

| Command                                                  | What it settles                                                                                                            | When                                                                                                                                                                                                                  |
|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew compileJava test`                             | It compiles, and the unit tests and the documentation guards pass                                                          | Every change, before you offer it                                                                                                                                                                                     |
| `./gradlew runIde`                                       | It actually works                                                                                                          | Anything a tester can see — see below                                                                                                                                                                                 |
| `./gradlew inspect`                                      | Every finding in the Inspected scope, apart from the exceptions `qodana.yaml` lists                                        | Never by hand. CI runs it on every push, on every branch, and the run is where it is read                                                                                                                             |
| `pwsh tools/inspect.ps1 -Quick`                          | The rules that read the source as text, and `pmdMain`; not what an IDE indexes                                             | Every change, before you hand it over. About a minute, no IDE: the text rules, then fifteen seconds of PMD                                                                                                            |
| `./gradlew check`                                        | The unit tests, the IDE tests, the complexity gate and the coverage gate, with the coverage report                         | Before you hand over a change that reaches the indexer or the tree. About four minutes: the IDE tests run as three shares at once                                                                                     |
| `./gradlew ideTestGitAndCode --tests "org.testin.git.*"` | The IDE tests of one package, in the share that holds it                                                                   | While working on a change that needs the IDE. `ideTest` runs three shares at once, each in its own sandbox, and `--tests` filters only the share it follows; `build.gradle.kts` lists which packages each share holds |
| `./gradlew pmdMain`                                      | No production method is over the cognitive complexity or nesting limit                                                     | Run by `-Quick`, and part of `check` and of `build.yml`; fifteen seconds, and it does not wait for a compile                                                                                                          |
| `git worktree add ../testin-<what> <branch>`             | A second branch, checked out at once                                                                                       | Whenever two pieces of work run at the same time — see below                                                                                                                                                          |
| `./gradlew verifyDistribution`                           | No test classes and no compile-only dependencies reached the jar                                                           | Runs in CI; run it if you touched packaging                                                                                                                                                                           |
| `pwsh tools/warnings.ps1`                                | Every warning from every source in one table, the gated ones first: inspections, Plugin Verifier, Qodana, compiler, Gradle | Whenever you want the whole picture. It prints to the console and names where each source's full list lives. It reads last results; `-Full` runs the slow ones, about twenty minutes                                  |

### One build at a time in one checkout

Two Gradle invocations sharing a project directory corrupt each other's outputs,
and the errors they produce read exactly like real ones:

- a class reported as `cannot find symbol` from inside its own package;
- `package org.testin.util does not exist` in a submodule, while `:compileJava`
  and `:jar` both report UP-TO-DATE;
- `Unable to delete directory 'build\classes\java\main' - a process is still
  writing to the target directory`;
- a `NoSuchFileException` for `build/test-results/test/binary/in-progress-results-generic.bin`;
- a task reported as FAILED after every one of its tests has logged PASSED.

None of those is a defect in the code. `./gradlew --stop`, then `clean`, then one
run clears it, and `--no-build-cache` is the escape if it comes back.

This is the other half of the worktree rule above: a second working tree gives
the other piece of work its own `build/` as well as its own branch. Running two
builds in one checkout cost three separate diagnoses of failures that did not
exist, on 20 September 2026.

### A hundred compile errors are usually one

Lombok writes most of this codebase's boilerplate, and it writes it during
annotation processing. **An error in an annotation stops that processing**, so
every getter, every all-args constructor and every enum field it would have
generated is simply absent — and javac then reports one error per use of them.

What you see is a hundred `cannot find symbol` errors blaming `TestCase` and
half the model. What is wrong is one line somewhere else entirely. A duplicated
`@NotNull` produced exactly this: *"NotNull is not a repeatable annotation
interface"* at the top of the list, and ninety-nine consequences under it.

**Read the first error, not the last, and not the loudest.** If the list is long
and blames generated members, look for an annotation error above it before you
open any of the files it names.

### A green build is not evidence of a working plugin

This is the single most useful thing to know about this codebase.

`@NotNull` is half a compile-time contract. javac ignores it, and the IDE's
instrumenter rewrites it into a throw that exists only inside a running IDE.
NullAway, run by Error Prone inside `compileJava` and `compileTestJava`, is the
half that is checked: a null passed to a `@NotNull` parameter, returned from a
method not marked `@Nullable`, or dereferenced where it may be null fails the
build, in the production code and the tests of all three modules (#377, #389).
A test's fields are filled in JUnit 3's `setUp` or TestNG's `@BeforeMethod`,
and both are named as initializers. Error Prone's own checks run beside it at
their default severity: an error fails compilation, and a warning is fixed
rather than left standing. `build.gradle.kts` names the platform and JDK types
an enum may hold that Testin cannot mark `@Immutable`, and the one check a crash
in Error Prone keeps off until a release fixes it (#66, finding 372). What
NullAway cannot see still throws in
front of a tester: a null from platform or library code it has no model for,
and a field Jackson or reflection left empty.

When it reports a map lookup, a `Path.getParent()` or an `AtomicReference.get()`
that cannot be null here, say so in the code rather than suppressing it: a
harmless default (`getOrDefault(key, List.of())`) where there is one, and
`Objects.requireNonNull(value, whatWasMissing)` where there is not. A
`@SuppressWarnings("NullAway")` fails the inspection gate like any other
suppression.

Guarded blocks, caret gestures, tab colors, dialog layout, action registration
and anything the platform calls back into are reached by **no** unit test. If
your change touches one of those, it has not been tested until it has been run.

### The complexity gate

How hard a method is to read is measured, not argued over (#378). PMD scores
every method in the three modules' production code with two rules, and
`pmdMain` fails on either:

| Rule                       | Limit                                                                                                         |
|----------------------------|---------------------------------------------------------------------------------------------------------------|
| `CognitiveComplexity`      | Fails at 15. Each `if`, loop, `catch`, ternary and run of boolean operators costs one, plus its nesting depth |
| `AvoidDeeplyNestedIfStmts` | Fails on a third `if` nested inside two others                                                                |

A count of `if`s would punish the guard clauses that keep a method flat;
cognitive complexity charges for nesting, which is what makes a method hard to
follow.

`.github/complexity-rules.xml` holds both limits, and no method is exempt from
either. The methods that were already over a limit when the gate was written
were brought under it in #382, so the file lists none, and none is added: a new
method over the limit is split, not listed. The report is in
`build/reports/pmd/main.html` of each module, and the console names the method
and its score.

### Which rules a test proves

A rule is proven when a test method carries its marker, the same marker the
code carries: `// Rule-TREE-PANEL-038` on the line above the test method, or
`// UC-TREE-PANEL-012, Rule-TREE-PANEL-038`. `RuleCoverageTest` reads every
test root and does two things with them (#325):

| Check                          | What happens                                                                                                                                                                              |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `everyRuleATestNamesExists`    | Fails when a test marker names a rule no document writes, or a retired rule, as `RuleNumbersTest` does for the production code                                                            |
| `everyLiveRuleIsProvenByATest` | Fails when a rule that is not retired has no test carrying its marker, and writes `build/reports/rule-coverage/unproven.md`: proven against total per part, then each rule no test proves |

Every live rule has had a test since 13 October 2026 (#425), so a new rule
lands with the test that proves it, in the same commit. CI keeps the report
with the other test reports.

### Which code carries a rule

The other direction is `RuleNumbersTest`'s. It reads the production code of the
four modules, the root, `testin-java`, `testin-testng` and `testin-apimodel`:
the Java under each `src/main/java`, and the XML under each `src/main/resources`,
where `plugin.xml` carries the rules no Java declaration holds (#425):

| Check                             | What happens                                                                                                   |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------|
| `everyRuleTheCodeCitesExists`     | Fails when a marker names a rule no document writes, or a retired rule                                         |
| `everyUseCaseTheCodeCitesExists`  | Fails when a marker names a use case no page writes                                                            |
| `everyLiveRuleIsCarriedByTheCode` | Fails when a live rule has no marker in the production code: the declaration that makes the rule true names it |
| `everyUseCaseIsCarriedByTheCode`  | Fails when a use case has no marker in the production code                                                     |

So a rule is linked both ways: a test proves it and the code that carries it
names it. A new rule lands with both markers, in the same commit.

### Whether the test runs the code

A marker at both ends does not show that the test reaches the code. `ruleTrace`
checks that it does (#425):

```bash
./gradlew test ideTest ruleTrace   # it reads what the tests left, so run them first
```

After every test method, `TestCoverage` asks the JaCoCo agent what that test
alone ran, over JMX, and saves it under `build/jacoco/per-test/<task>`.
`TestCoverageListener` does it for the TestNG tests of all four modules: TestNG
finds it through `META-INF/services/org.testng.ITestNGListener` in the root's
test resources. For the IDE tests it is `AbstractIdeTest` and
`AbstractOpenEditorsIdeTest`, which is why every IDE test extends one of them.
The budget tests run alone after `ruleTrace`, so they are not traced. `RuleTraceTest` then reads every marker above a test method.
A marker is traced when that test ran at least one line of a declaration that
the production code marks the same way.

It writes `build/reports/rule-trace/untraced.md`, listing:
- each marker whose test runs none of its code;
- each marked test that saved nothing it ran.

CI runs it after the tests and keeps the report. For now, it only reports. It
fails the build once the markers it lists are fixed. A static field or an enum
constant runs once, when its class loads, so it counts as reached when the test
runs any line of its file.

### The coverage gate

JaCoCo measures the lines and branches both test tasks run, `test` and
`ideTest`, across the three modules, and reports them per package in one report (#325).
It measures only the `org.testin` classes. The report reads the per-test files
too: each save resets the agent, so the files left at the end hold almost
nothing on their own.

```bash
./gradlew check                            # both test tasks, every gate, and the report
./gradlew jacocoTestCoverageVerification   # the coverage gate alone, with the tests it needs
```

|            |                                                                                                                                                 |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Report** | `build/reports/jacoco/test/html/index.html`, one row per package, and `jacocoTestReport.xml` beside it. CI keeps it with the other test reports |
| **Gate**   | `jacocoTestCoverageVerification`, part of `check`. Fails under 86% of lines or 53% of branches, from 86.13% and 53.73% on 13 October 2026       |

The gate stops coverage falling and asks for nothing more. **Raise a minimum in
the commit that lifts its figure past the next whole percent**, so the floor
follows every gain and never trails it by more than a point. New tests go first
where the report shows error paths and state changes unrun, not to getters. A
line reached is not a rule proven, which is why the rule count above is kept
beside it.

Coverage has this one gate. Qodana's `JvmCoverageInspection` is off in the
profile: it judges each method against its own 50% from whatever coverage the
machine running it last recorded, so the same code passes in CI and fails on a
laptop. `qodana.yaml` turns it on in CI only, at Information level, where it
lists the methods no test reaches and fails nothing.

### The inspection gate

**It runs in CI on every push, on every branch, and that is where to read it.**
`inspect.yml` does the work and keeps the full list as an artifact; the summary
is on the run.

```bash
./gradlew inspect
```

Locally it is for the one job CI cannot do: checking a change before it is
pushed at all, usually because it touched nullability or annotations across many
files. It costs one indexing pass — ten to twenty minutes — so it is a sweep
gate rather than a per-commit one, and a gate that takes twenty minutes by hand
is a gate that gets skipped. Run it after the last edit, on a still tree:
editing a file while the inspector is reading it produces findings about a
version that no longer exists, which reads exactly like a real defect.

It exits non-zero for any finding in the files the repository writes: the scope
`.idea/scopes/Inspected.xml` names, which **Code | Inspect Code** offers in the
IDE as *Inspected*. A warning the IDE shows there is a warning CI fails on.

**The only exceptions are the ones `qodana.yaml` lists under `exclude`, each
with its reason.** Qodana reads that list in CI and `tools/inspect.ps1` reads the
same entries, so the two gates cannot disagree, and there is no second list to
fall behind. Each entry names the narrowest path it can:

| Exception                                       | Why nothing better is possible                                                                                            |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Files whose bytes something else owns           | The Gradle wrapper, JetBrains' agreements                                                                                 |
| Four CSS inspections in `style.scss`            | The docs site's stylesheet is a Jekyll file: front matter, Liquid and `_sass` imports, and the theme's own classes        |
| `JSUnresolvedLibraryURL` in `head-custom.html`  | The docs site loads Mermaid from its CDN, and the inspector keeps no local copy of a library behind a URL                 |
| `UndefinedParamsPresent` in `.github/workflows` | An action's inputs are declared in its `action.yml` online, which an offline inspector cannot fetch                       |
| `UndefinedAction` in `.github/workflows`        | It looks each action up through GitHub's API without signing in, and a shared runner runs out of the anonymous rate limit |

A core method that only a content module calls is marked `@FromContentModule`,
which `.idea/misc.xml` names an entry point, so neither the IDE nor the
inspector calls it unused. One the script finds unmarked is reported as
`UsedFromContentModule`, and the gate fails on it until it is marked.

`unused`, `SameReturnValue`, `RedundantThrows` and `UnusedReturnValue` are the
global ones, and the headless run under-reports them all: it sees neither
Lombok's generated code nor the content modules' callers. `UnusedReturnValue` is gated and reported zero on 22 September
2026,
while the IDE found `FormRows.wideRow`. For these four, **Code | Inspect Code**
in the IDE is the honest list.

A new `@SuppressWarnings` or `//noinspection` fails the gate too, through the
`SuppressionAnnotation` inspection. The profile allows `UnstableApiUsage` only,
for the one platform call `build.gradle.kts` names.

The spell check is part of the gate as well. A misspelled word is corrected in
the text. A word that cannot be corrected - an IntelliJ API, a command such as
`xvfb` or `ulimit`, a library's name - goes into
`.idea/dictionaries/project.xml`, the project dictionary every clone shares. It
is never renamed into something wrong to quiet the check, and never suppressed.

The script's own rules, because no IntelliJ inspection makes them. This table is the one list of them: `UnlistedRule`
fails the gate when it leaves out a rule the script reports, or names one it no longer does.

| Rule                            | What it forbids                                                                                                                                                                                                                                                           |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WrappedMethodDeclaration`      | A method declaration written over more than one line                                                                                                                                                                                                                      |
| `StaticMutableState`            | A static that is not final in the packages that model the data                                                                                                                                                                                                            |
| `HandWrittenPrivateConstructor` | An empty private constructor where `@NoArgsConstructor(access = PRIVATE)` says it                                                                                                                                                                                         |
| `NonMarkerComment`              | A comment in a `.java` file that is not a `UC-` or `Rule-` marker, in the tests as well as in `src/main`. The copyright header and the comments a machine reads, such as `//noinspection`, stay                                                                           |
| `DriftedCaption`                | One concept spelled two ways in front of the same tester                                                                                                                                                                                                                  |
| `OrphanedJavadoc`               | A doc block followed by a second one, which javac throws away                                                                                                                                                                                                             |
| `MissingCopyright`              | A `.java` file that does not open with the Apache 2.0 notice                                                                                                                                                                                                              |
| `HtmlParagraphInMarkdown`       | A bare `<p>` in a Markdown file, which turns what follows into raw HTML                                                                                                                                                                                                   |
| `MisalignedMarkdownTable`       | A Markdown table whose column borders do not line up row under row, which reads as a wall of pipes in a diff or a terminal                                                                                                                                                |
| `HelperNamedLikeTest`           | A helper in a JUnit 3 test whose name starts with `test`, which the IDE reads as a broken test                                                                                                                                                                            |
| `AbbreviatedName`               | A name that abbreviates a word the sweep of #426 spelled out - `tc`, `dto`, `dir`, `btn`, `gbc`, a single letter other than the idioms `e`, `i`, `j`, `x`, `y`, `g` - outside strings and comments, and not after a dot. CLAUDE.md's Abbreviations table lists what stays |
| `UnusedLambdaParameter`         | A lambda parameter nothing reads and not written as `_`, which the headless inspector never reports                                                                                                                                                                       |
| `QualifiedClassName`            | A class written by its package path mid-line instead of imported                                                                                                                                                                                                          |
| `UnicodeEscape`                 | A `\u` escape in a `.java` file, for any character: write the character itself, or `Character.toString(0x1E)` for one that cannot be seen                                                                                                                                 |
| `ColorCode`                     | A color written as RGB or hex in a `.java` file. Use IntelliJ's own (`JBColor` constants, `JBUI.CurrentTheme`) or a house owner (`Icons`, `FixedColors`)                                                                                                                  |
| `UnusedImport`                  | An import nothing in the `.java` file names, or one of a class in the file's own package or in `java.lang`                                                                                                                                                                |
| `UnformattedJava`               | A `.java` file IntelliJ's Reformat Code would change, by the IDE's own formatter with the project's code style. The full run only: it needs the IDE                                                                                                                       |
| `DuplicatedDisplayString`       | A string a tester reads that another file also writes, so it has no owner                                                                                                                                                                                                 |
| `UsedFromContentModule`         | A declaration the inspector calls unused that a content module calls, until it is marked `@FromContentModule`                                                                                                                                                             |
| `PmdDidNotRun`                  | A quick run whose `pmdMain` failed without naming a method, so PMD said nothing it can be trusted on. A method PMD names is reported under PMD's own rule                                                                                                                 |
| `UnlistedRule`                  | A rule this script reports that this table does not name, or a row naming one it no longer reports                                                                                                                                                                        |

The inspector's rules are named one by one in
`.idea/inspectionProfiles/Testin.xml`, so the gate does not depend on what a
default profile happens to switch on. `Convert2MethodRef` was not run from the
command line until it was named there: eight such lambdas sat on `main` while
the IDE flagged them and CI reported none.

The Gradle scripts have a gate of their own, and it is not this one.
`gradle.properties` sets `org.gradle.kotlin.dsl.allWarningsAsErrors=true`, so a
deprecated call in a `.gradle.kts` file fails every build, locally and in CI.
The inspector cannot report those warnings: run headless, it never loads the
scripts' Gradle classpath. `ConvertToStringTemplate` is the one Kotlin rule it
does see, because it needs no classpath.

`MissingCopyright` is the one that reads the test sources as well: every `.java`
file opens with the Apache 2.0 notice from `LICENSE`'s own appendix, above
`package` and one blank line before it. So a file read in a jar, a decompiler or
a fork still names its owner and its terms (#303). The year and
the owner are read from `LICENSE` rather than written into the script, so there
is one place to change them. A new file created in the IDE gets the header from
the copyright profile in `.idea/copyright/`, which is committed for that reason.

The report lands in `.inspection/`, deliberately outside `build/` so
`./gradlew clean` does not delete the list you are working from. Start with
`summary.txt` for the counts and `findings.txt` for the lines.

**An "unused" result is evidence, not a fact.** Two runs minutes apart over the
same tree returned 74 findings and 13. The 74 included a whole cascade — three
classes and twenty-eight unused imports — that the second run did not reproduce
and that reading the code disproved: a global unused check depends on how far the
index had got. So confirm one by finding the call site before deleting anything,
and re-run before reporting a count.

**A string a tester reads has one owner.** `DuplicatedDisplayString` reports a
user-facing string written in a second file, and the gate fails on it like any
other finding.

## What CI runs

| Workflow           | When                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `build.yml`        | Every push to `main` and every pull request. Compiles, runs the unit tests, the complexity gate, the IDE tests and the coverage gate, and verifies against **IntelliJ IDEA** - the one result that turns a pull request red Then **Qodana**, as a second job: the whole project on `main` and only the changed files on a pull request (with a comment on it), the build's test coverage included, against the profile and the exceptions `qodana.yaml` lists. Any finding above Information fails it, and every finding shows in Qodana Cloud and in GitHub's Code scanning |
| `verify.yml`       | Every push to `main`, plus every second day and on demand. The same verifier against **all six targets** - IntelliJ IDEA, PyCharm and Rider at both ends of the 262 branch - and any problem on any of them fails the run                                                                                                                                                                                                                                                                                                                                                    |
| `inspect.yml`      | Every push, on every branch, and on demand                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `dependencies.yml` | Every push to `main`. Submits the libraries the plugin ships - its `runtimeClasspath`, transitive ones included - to GitHub's dependency graph, so an advisory against any of them raises a Dependabot alert in the Security tab the day it is published                                                                                                                                                                                                                                                                                                                     |

No workflow publishes a release. It is published from a maintainer's machine
with `./gradlew publishPlugin`, which reads the Marketplace token from
`JETBRAINS_TOKEN` and signs with `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and
`PRIVATE_KEY_PASSWORD`. It uploads to the alpha channel, and a release is
promoted to the default channel from the Marketplace page rather than uploaded
again.

**Release notes are written in [`CHANGELOG.md`](CHANGELOG.md), and nowhere
else.** A change a tester will notice adds its line under Unreleased in the
same commit. The build renders the section named after `version` in
`build.gradle.kts` into plugin.xml's change notes, and Unreleased while no
section has that name. A release sets `version`, runs `./gradlew
patchChangelog` to put that version's heading over what Unreleased holds, and
then publishes.

**A release that moves the format number on has to say so first.** The format
in a `.tp` (Rule-INTERNAL-091) is the only promise about old test data, and
nothing converts it: a project in an older format is refused, and the tester
removes it and creates it again. The Unreleased section of `CHANGELOG.md` opens
with **Before you update** whenever the number moved, naming the keys that
changed. JetBrains cannot make one plugin version require an earlier one; the
notes are the only place that can say it.

## Compatibility: `since-build`, never `until-build`

`plugin.xml` declares `sinceBuild 262` and **no** `untilBuild`, and that is a
policy rather than an oversight. It was 261 until 22 September 2026, when the
plugin moved to IntelliJ 2026.2: that branch runs on JetBrains Runtime 25 and
ships Java 25 class files, which a 2026.1 IDE cannot load and a Java 21
compiler cannot read.

An `untilBuild` makes the plugin stop loading the day the IDE crosses it,
regardless of whether anything actually broke — so every user is blocked by a
date rather than by a defect. Without one the plugin keeps loading, and a real
incompatibility is found by the verifier and fixed. Raise `sinceBuild` when the
plugin starts using an API that needs it; do not add an upper bound.

The Gradle `platformVersion` is the other end of the same range and is what the
Marketplace checks.

**Two products are left out.** JetBrains Gateway is remote development's
launcher, and JetBrains Client its thin front end, which is also Code With Me's
guest. Neither holds a project, and neither ships the Git4Idea classes Testin's
Git code calls, so the verifier found the Git code unresolved in both. In remote
development Testin runs in the host IDE, beside the project (#419). `plugin.xml`
declares Testin `<incompatible-with>` JetBrains Client
(`com.intellij.jetbrains.client`), and leaves Gateway out by requiring
`com.intellij.modules.lang`, which every IDE has and Gateway does not. It cannot
name Gateway's id, `com.jetbrains.gateway`: IntelliJ IDEA bundles the Remote
Development Gateway plugin under that id, so Testin would not load in IDEA
either. `PluginDependenciesTest` holds these lines, and Verify does not run
against either product.

## Conventions

The rules this project holds itself to live in the repository, beside the code
they govern, so they travel with a clone:

|                                                |                                                                                                                 |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | **Start here.** The layer map, the four rules the plugin is built on, and two operations traced class by class  |
| [`CLAUDE.md`](CLAUDE.md)                       | The code conventions a change is reviewed against — naming, nullability, threading, what belongs in which class |
| [`docs/`](docs/README.md)                      | What Testin does, one use case at a time, with every rule numbered                                              |
| [`docs/standard.md`](docs/standard.md)         | How those documents are written, and what a machine checks about them                                           |
| [`docs/decisions.md`](docs/decisions.md)       | Decisions that look wrong until you know why, and what each costs to reverse                                    |

Three that catch people out:

- **Every method a tester can reach cites its rule** — `// UC-TREE-PANEL-012,
  Rule-TREE-PANEL-038`. A test fails if it cites one no document writes.
- **Behavior and its document change in the same commit.** Not afterward.
- **Lombok writes the boilerplate.** It is `compileOnly` plus
  `annotationProcessor` and never `implementation`, so it cannot reach the
  distribution. `@NonNull` goes on DTO and marker fields only, where it generates
  a runtime check; nullability everywhere else is
  `org.jetbrains.annotations`.

## Issues

Work lives in [GitHub issues](https://github.com/MutebAlMughyiri/test-in/issues), never in
a local file — this is developed across more than one machine.

**Every issue carries statistics, measured rather than estimated.** "Several call
sites" is not a statistic; "18 call sites across 11 files" is, and it is the
difference between an afternoon and a week. Counting routinely changes the plan:
one issue written as "remove the 20 nullable annotations from the indexer" turned
out to be 9 once counted, with the file holding the joint-highest count entirely
out of scope.

Two labels on every issue:

| Label       | Values                                  |
|-------------|-----------------------------------------|
| `priority:` | `critical`, `high`, `medium`, `low`     |
| `cost:`     | `nocost`, `minor`, `major`, `expensive` |

`cost:` is regression scope in the words testers already use — `minor` is
contained and the mechanism exists, `major` reaches several surfaces, `expensive`
changes many classes and refactors them, `nocost` is text or a list.

`out of scope` means parked: still open, still scored, not being worked on.

The issue templates produce the house format. Use them.

## Commits

The subject says what changed for a tester, not which files moved. The body says
what was wrong, what it is now, and why the fix is that one — a reader six months
later has the diff and needs the reasoning.

Reference the issue in the subject: `Say so when the gutter mark's test case is
gone (#245)`.

## The license and the terms your contribution is accepted under

Testin's source is licensed under the **Apache License 2.0** — see
[LICENSE](LICENSE).

[EULA.md](EULA.md) is a separate document: the end user agreement for the plugin
as distributed through JetBrains Marketplace, which applies only if and when
Testin is offered there as paid or freemium. It grants nothing over the source.

Two terms, and the second one is the one people do not expect:

1. **Your contribution is licensed under Apache 2.0**, the same as the rest.
2. **A contributor license agreement is required before an outside contribution
   is merged.** Open the pull request, and it will be discussed there; nothing is
   lost by starting work first.

### Why the second one

Because a license is not ownership, and the difference decides what can happen to
Testin later.

Apache 2.0 says what *you* may do with the code. It does not move the copyright:
a patch sent under it leaves **you** owning your patch, and Testin holding a
license to it. That is fine for using the code and awkward for everything else —
relicensing a future version, offering a commercial license, any arrangement with
a company that wants clear title. Each of those would otherwise need every past
contributor to agree, one at a time, forever.

A contributor license agreement settles that once, at the start, in writing. It
is what almost every company-backed open source project uses, and it is not a
trap: the code stays Apache 2.0, published, and yours to use like anyone else's.

There are no outside contributors yet, so this is written down before it is
needed rather than after. See
[Decision-008](docs/decisions.md) for the whole reasoning.
