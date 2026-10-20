# Testin — instructions for AI assistants

## Architecture rules

**[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) is the authority on all four.**
It carries the layer map, the reasoning, the exempt list and two operations
traced class by class. Read it before a change that touches any of them; what is
below is the rule itself, stated once, so this file is not a second copy of it
going stale (#99).

1. **All test data file access goes through the indexer.** `org.testin.indexer`
   is the single owner. No other package reads, writes or performs a VFS
   operation on test data - it asks the indexer, whose cache is authoritative.
   The packages exempt from it are the ones that never touch test data; the
   list is in ARCHITECTURE.md and nowhere else, and adding to it is a decision,
   not a convenience.
2. **The VFS operation succeeds first, then the cache is updated.** Never the
   other way round: the cache update persists markers, marker writes create
   directories, and the reverse order produces phantom directories and "already
   exists in VFS" errors. One deliberate exception: a test run's run items are
   put in the cache first and written by `RunItemWriter`'s background queue, so
   recording a run item status never waits on the disk.
3. **Swing is read and written only on the EDT.** Short work with no UI of its
   own goes to `executeOnPooledThread` and finishes with `invokeLater`; long
   work the tester should be able to cancel goes to `Task.Backgroundable`.
   Actions declare `ActionUpdateThread.EDT` when their `update()` reads Swing
   state.
4. **Formatting is display-only.** Rendering may reformat a value; saving never
   does. The stored JSON is byte-identical to what the tester typed, so an
   editable surface loads the raw value and a read-only one loads the formatted
   value - `gridValue` and `displayValue` on `TestSetEditorFields`.

The fifth is not in that document, because it is about where a value is kept
rather than how the plugin is shaped:

### A setting is application level; `testin.yml` is written only on the tester's click

Where a value lives is decided by who it belongs to, not by what it is about.

- **The machine's settings are application level.** `AppSettingsState` is
  `@Service(Service.Level.APP)` over one `testinSettings.xml` in the IDE's own
  config directory, so every open project sees the same Testin root, tester name
  and log level. These are facts about this machine and this person — a root
  folder, a download folder, an account — and they are exactly the things that
  must never be committed.
- **The repository's config is `testin.yml`, and it is the team's.** Which test
  project this repository drives, and how it is shared. It is committed, so a
  clone needs no setup, and it names no machine and no person. **Testin writes
  it only on the tester's own click**: Save to testin.yml in the panel's title
  bar, or Apply in the bugRepoUrl hint of Testin Help (Decision-013),
  and everything works without it - except the automation code, which stays off
  until the file names the open test project (Rule-CODEGEN-082, `codegen/CodeOn`).
  One class reads and writes it, `config/TestinYml`; its values are
  package-private and `ArchitectureTest` keeps the YAML parser there (Rule-INTERNAL-089, Decision-013).
- **The test project a tester chose is kept on this machine.** One value in the
  project's `PropertiesComponent` (the workspace, never committed), owned by
  `BoundTestProject`. It wins over the name `testin.yml` gives until the file
  names a different one (Rule-TREE-PANEL-106).

**Do not add a project-level `PersistentStateComponent`.** A value the team
agrees on belongs in `testin.yml`, written by hand or by Save to testin.yml; a value that differs per
machine belongs in the application settings, where it cannot be committed by
accident. The chosen test project is the one exception, and it is a choice, not
a setting. Another store would be a second answer to "where is this
configured", and the ones that exist would start disagreeing with it.

This has been tried and undone. `testin_example/.idea/` was found carrying both
`testinSettings.xml` and `testinProjectSettings.xml`, each holding a
`rootTestinPath`, and **neither was read by anything** — the first because the
service is application level, the second because
`testin.settings.ProjectSettingsState` does not exist in the source or anywhere
in the git history. Both were deleted on 2026-09-02. A stale config file that
silently has no effect costs more than the setting it was meant to hold.

## Code conventions

- The `Project` object is always named `project`: `final @NotNull Project project`.
- Abstract parent classes are named `Abstract*`; non-abstract parents `Base*`.
- **Packages are all-lowercase and named by feature, never by kind.** One word
  where one will do: `editor`, `explorer`, `report`, `creator`, `model`. A
  sub-package does not repeat its parent — `editor/run`, not `editor/runeditor`.
  There is no `enums`, `listeners` or `mappers` package: a type lives with the
  feature that owns it, and shared domain vocabulary lives in `model` beside the
  DTOs and markers that carry it (#53). A package past 20 classes splits along
  its feature's parts, or names its reason in ARCHITECTURE.md's Package sizes;
  `PackageSizeTest` holds the line (#394).
- **No `I` prefix on interfaces**, and no `Impl` suffix unless an interface of
  that exact name exists. A class is named for what it does — `VfsExecutor`, not
  `TreeUtilImpl`; `SaveOnProjectClose`, not `ProjectCloseListenerImpl`. Where the
  plain noun would collide with a platform type, the plugin prefixes with
  `Testin`: `TestinEditor` against `com.intellij.openapi.editor.Editor`.
- **A `*Service`, `*Handler`, `*Provider` or `*Manager` suffix has to earn its
  place.** The suffix names no job, so the default answer is to name the job
  instead - #291 renamed seven that way. Four reasons keep one, and there are no
  others: the class implements a platform or Swing interface of that name (`UnifiedEditorProvider` for
  `FileEditorProvider`, `TreeTransferHandler` for
  `TransferHandler`); it is a `@FunctionalInterface` where the suffix *is* the
  job (`RemoveHandler`, `ImportHandler`); the plain noun would collide with a
  platform type (`GitRepositoryService` against `git4idea`'s `GitRepository`); or
  it distinguishes the reader from the value it serves. A fifth `*Manager` is
  not a reason, it is a class nobody has read yet.

- **Renaming anything is a three-step check, not a find-and-replace.** Persisted
  keys read like code — `@State(name = "testin.settings.AppSettingsState")`,
  `"testin.pageSize"` — and log lines contain ordinary words like `Setting`, so
  rewrite the qualified form rather than the bare word, then diff every string
  literal in `src` before and after. On Windows a package rename that changes
  only capitals also needs `--no-build-cache`: Gradle restores the pre-rename
  capitals and javac compares classpath entries capital for capital, which
  surfaces as a package-private class being invisible to a test in its own
  package.
- **A method declaration is one line.** No matter how many parameters it has or
  how long the annotations make it, the signature does not wrap. A signature is
  one thing to read, and split over four lines it is four things to reassemble
  before the first question about the method can be asked. No IntelliJ
  inspection says this, so `tools/inspect.ps1` does: it reports a wrapped
  signature as `WrappedMethodDeclaration` and exits non-zero for one, alongside
  `DataFlowIssue` and `ReturnNull`. `.github/workflows/inspect.yml` runs it on
  every push.
- `final` on parameters and locals wherever possible.
- Nullability: org.jetbrains `@NotNull`/`@Nullable` everywhere; Lombok
  `@NonNull` only on DTO/marker fields (it generates runtime checks there).
  Never jspecify. Locals carry `final @NotNull` too, wherever the value they
  are assigned is one.
- **`Optional` is how a field says "not set yet", and a parameter says
  "possibly nothing".** A popup built on first show, a service the application
  has not started, a test run an editor has not loaded: each holds an empty
  Optional rather than a null, so no reader has to test for one. The
  `OptionalUsedAsFieldOrParameterType` inspection is switched off in
  `.idea/inspectionProfiles/Testin.xml` for exactly this reason — what it argues
  for instead is a nullable field, which is the thing the codebase spent a sweep
  removing. Turning it back on means reversing that decision, not tidying up.
- Node behavior is declared on the node: capability flags on `AbstractNode`
  (`isRenamable`, `isTransferable`, `acceptsTransferred`, ...) instead of
  instanceof chains at call sites. Enums carry their own presentation and
  actions (see `RunItemStatus`, `TestRunStatus`).
- Dialogs are built on the declarative framework (`org.testin.ui.framework`):
  a dialog assigns `title`, `components`, `shortcuts` in its constructor and
  implements `submit()`. Never hand-build popup layouts.

- **Lombok writes the boilerplate; never hand-write what it generates.** A
  holder's private constructor is `@NoArgsConstructor(access =
  AccessLevel.PRIVATE)`, not three typed lines — it belongs with `final` and
  `public` as a property of the class, not as a member a reader has to look at
  and dismiss. The same for getters, all-args constructors and enum fields.
  `Bundle` is the one exception in `src/main`, because its constructor calls
  `super(...)` and Lombok writes an empty body. Read the
  `lombok-writes-the-boilerplate` skill under `.claude/skills/`.
- **Names say the business word, in full.** A test case is a `TestCase`, never
  a `Case`: `TestCaseDetails`, `testCaseUuid`, `testCases`, `A_TEST_CASE`. The
  same holds for the documentation and for issues - *test case UUID*, never
  *case id*. A test case's, a run item's or a folder's identity is a UUID and is
  named `uuid`, never `id` (#434); `id` stays only in IntelliJ's own names, such
  as an action id. Nothing Testin names is abbreviated: `testCase`, never `tc`;
  `testProject`, never `tp`; `button`, never `btn`. **Abbreviations** below
  lists the Java idioms and known acronyms that stay. No business word stands
  bare either (#431): `testProject`, never `project`, unless it is IntelliJ's
  `Project`; `runItem`, never `item`; `testRun`, never `run` as a noun; and a
  status says which one - `testCaseStatus`, `runItemStatus`, `testRunStatus`,
  `runItemExecutionStatus` (one of a run item's executions),
  `automationExecutionStatus`, `nodeStatus`. A model field keeps its plain name
  (`TestCase.status`), because its class already says whose it is and its name
  is the key on disk. `NamesSayTheBusinessWordTest` holds the type names to it. Read the
  `names-say-the-business` skill under `.claude/skills/` before naming or
  renaming anything.
- **A class is named by its import, never by its package path.** `Optional`,
  not `java.util.Optional`, in code and in tests alike; the only exception is
  a simple name that already means another class in that file, and it carries
  `//noinspection QualifiedClassName` over the statement that needs it. No
  IntelliJ inspection says this either, so `tools/inspect.ps1` does: it reports
  a package path written mid-line as `QualifiedClassName` and exits non-zero
  for one. Read the `import-never-qualify` skill under `.claude/skills/`.
- **A class does one job, and its name says which.** Before adding a method to
  an existing class, say the class's job in one sentence, then the method's. If
  the second is not the first, the method belongs elsewhere — even when the
  class is where every caller already looks. `Notifier` delivers messages and
  had grown six methods that wrote them; they are `Refused` constants now, the
  twin of the `Done` enum that already owned the success words. Read the
  `a-class-does-one-job` skill under `.claude/skills/` before adding to a
  shared class.
- **A button is a class.** Every button a tester clicks extends
  `AbstractIconButton` in a class of its own, as the editor toolbar's do: it
  takes what it needs in its constructor and owns its words, its picture, its
  click and its `updateState()`, and the surface only lists it. Buttons with
  the same or a similar purpose are one button, so one class that swaps its own
  word and picture: `SelectUnselectAllButton`, `CollapseExpandButton`. #441 first put
  both inside `SelectionTree`, which then did three jobs. Read the
  `a-button-is-a-class` skill under `.claude/skills/` before adding a button
  anywhere.

- **Every state-changing action confirms itself** with one soft notification at
  the point it succeeded: `Services.getInstance(project, Notifier.class).softShow(project,
  Done.COPIED)`. The words live in `Done` when it worked and `Refused` when it
  did not; `Notifier` only delivers them. The message is the **outcome in the
  past tense** and nothing
  else — `Copied`, `Pasted`, `Renamed`, `Removed`, `Re-sorted`, `Passed`. One
  word wherever one will do, no trailing dots, and **no noun**: the tester
  pressed the key on the thing in front of them, so naming it back is a word
  they read every time and needed once. A bulk operation notifies once with a
  count — `Removed 4`, never four balloons.

  Not around `actionPerformed`: an action that returns early, opens a
  confirmation, or hands off to a background task would report a success that has
  not happened. Put the call after the work, inside whatever `try` could fail.
  Actions that only move the view — paging, escape, opening a details panel —
  stay silent, or the tester learns to ignore all of them (#62).

  **Which kind: can it finish while the tester is not looking?** `softShow` is a
  balloon on the status bar that fades and leaves no trace; `info` is a real IDE
  notification that stays in the Notifications log. Work that happens under the
  tester's hand takes the balloon — that is almost everything. Work that runs in
  the background and completes on its own time takes `info`, so a sync or a push
  that lands while they are reading a bug report is still there afterward. Those
  keep a short title and one line of detail — `Synced` / "Up to date with the
  remote" — never a sentence and never an exclamation mark.
- **The code carries no comments but its markers.** A `UC-` or `Rule-` marker
  above the signature, and nothing else: no Javadoc, no paragraph saying why, no
  line narrating the next one. Muteb, 20 September 2026: *"[I] want to make my
  classes clear and simple, no more comments accept rule and use case numbers."*

  The tests are held to it too. 27,483 comment lines came out of
  `src/main/java` in one commit - 34% of every line in it, against 35,130 lines
  of code - and 4,256 more came out of `src/test` on 22 September 2026, when a
  Javadoc block was found back in a test class. `NonMarkerComment` gates both
  trees now, so the next one fails the run rather than waiting to be spotted.

  What replaced them is not nothing: **the marker says which documented
  behavior this is, `docs/` says what that behavior is, and the commit message
  says why the code is shaped this way.** A reason belongs in the commit body,
  where `git blame` reaches it and where it cannot drift from the code, rather
  than in a paragraph above the method.

  So write the commit message as if it were the comment you did not write: what
  was wrong, what was tried and refused, which trap the shape avoids. That is
  where the next reader looks, and it is the one record that is still true years
  later. Every comment this policy removed is in `af5f3013` and readable with
  `git show af5f3013:` and the file's path, forever - for example
  `git show af5f3013:src/main/java/org/testin/editor/run/RunEditor.java`.

  Two exceptions, and only two. A comment a machine reads stays -
  `//noinspection`, `// @formatter:off` - because it is part of the build rather
  than prose. And the Apache notice at the top of every file stays, untouched
  (`MissingCopyright` gates it). A `//` inside a text block or a string is not a
  comment, and nothing touches it.
- **American English**, in comments and in text a tester reads. The platform API
  this is written against is American (`Color`, `EditorColors`, `normalize`), so
  British spellings put two dialects in one sentence — a comment about "the caret
  row color" directly above `EditorColors.CARET_ROW_COLOR`. It is a convention
  rather than a cleanup because it does not stay fixed otherwise: #48 normalized
  28 of them, and the next few comments written put six back.
- A method handles its own failures: no `throws` on the signature. Catch inside,
  log through `TestinLogger`, and notify when a tester action triggered it — a catch
  that does neither is worse than the `throws` it replaced. **This applies to
  `src/test` too.** A test that cannot run wraps its body and throws an
  `AssertionError` carrying the cause; a test that skips itself does that check
  *outside* the try, because a skip is a `RuntimeException` and a broad catch
  swallows it into a failure on every machine that legitimately cannot run it.
- **The rule is about methods, and the whole tree obeys it.** The `throws`
  that remain are declarations rather than work, and this list is the record of
  why - not a comment above each one, which the no-comment rule forbids and the
  September sweep removed. Counted on 19 October 2026: four in `src/main`, none
  in `src/test`, `testin-java` or `testin-testng`, and every one of them is
  below. `DeclaredContractsTest` scans the main and test trees for them, holds
  the same four with their reasons and fails on a fifth, so this list can no
  longer drift from the code unnoticed. Do not sweep them again:
  - `TreeTransferable.getTransferData`, and the one `TestCaseTransferHandler` builds
    for a drag of test cases — AWT's `Transferable` contract is that an
    unsupported flavor throws. Catching it hands the platform a wrong object
    instead of "I do not have that".
  - `SettingsConfigurable.apply` — `Configurable.apply` declares
    `ConfigurationException`, and the settings dialog is the one owner that
    catches it: throwing is how the platform is told to stay open and print the
    message under the field. Catching it and notifying instead would hand the
    dialog a success it did not have, and close it over a refused value. The
    check behind it, `refusalOfTheRoot`, returns the refusal rather than
    throwing it, so `apply` is the only method that declares it.
  - `AgentPromptConfigurable.apply` — the same contract, for a timeout that is
    not a whole number of seconds above 0 (Rule-CODEGEN-115). Muteb, 19 October
    2026, chose to refuse it on Apply exactly as the main page refuses a bad
    folder, so every settings page refuses the one way. `refusalOfTheTimeout`
    returns the refusal and `apply` throws it.

  Every `*IdeTest` extends `AbstractIdeTest`, or `AbstractOpenEditorsIdeTest`
  when it opens editors. Each saves what the test alone ran, which `ruleTrace`
  reads (#425). One that needs a folder on disk extends `AbstractTempRootIdeTest`,
  which owns `root`, creates it before the fixture and deletes it after. Those
  bases are where `BasePlatformTestCase`'s `throws Exception` turns into an
  `AssertionError`. Their subclasses override `setUp` and `tearDown` without
  `throws`, and catch their own checked calls the same way.

  Adding another is a decision, not a shortcut: it needs the same shape (a
  declaration, one owner catching above it), an entry in
  `DeclaredContractsTest.MAY_THROW` naming the contract, and a line here. The
  list said five while seven stood (#66, finding 268), which is why the test now
  owns the count. Its twin, `MAY_RETURN_NULL`, does the same for a `@Nullable`
  return: every one is a platform override whose contract uses null, six in
  `src/main` and the rest in two test fixtures standing in for the platform.
  Testin's own last three went to `Optional` in #352.

### Abbreviations

No name in the plugin abbreviates a word, in production code or tests (#426).
Muteb, 13 October 2026: *"no abbreviations in our plugin at all. except java
and all known like exception → ex"*, and *"don't use `p`"*. The `Project` is
`project`. A business word is never shortened: `testCase`, `testSet`,
`testRun`, `runItem`, `testProject`, `node`, `testCaseUuid`, `field`. Nor is
any other word: `button`, `label`, `column`, `index`, `text`, `background`,
`component`, `message`, `directory`. What stays is in this table, and only
here.

| Abbreviation                                                                                     | Stands for                                                                                                                               | Where it stands                                                                               |
|--------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `e`                                                                                              | an event                                                                                                                                 | A parameter holding an `AnActionEvent`, a `MouseEvent` or any other event                     |
| `ex`                                                                                             | an exception                                                                                                                             | A caught exception or throwable                                                               |
| `i`, `j`                                                                                         | an index                                                                                                                                 | A loop counter                                                                                |
| `x`, `y`                                                                                         | a coordinate                                                                                                                             | A horizontal or vertical position                                                             |
| `g`, `g2`                                                                                        | `Graphics`, `Graphics2D`                                                                                                                 | A painting method's graphics                                                                  |
| `sb`                                                                                             | `StringBuilder`                                                                                                                          | A local building a string                                                                     |
| `args`                                                                                           | arguments                                                                                                                                | A varargs parameter                                                                           |
| `Id`, `UUID`, `Url`, `Http`, `Ssh`, `Api`, `Cli`, `Os`, `Iso`, `Rgb`, `Hex`, `Lf`, `Nul`, `Todo` | the standards, characters and words of those names                                                                                       | Inside a longer name: `testCaseUuid`, `bugRepoUrl`, `isoTime`, `nulSeparated`                 |
| `Json`, `Yaml`, `Yml`, `Xml`, `Html`, `Css`, `Pdf`, `Csv`, `Tsv`, `Xls`, `Xlsx`, `Png`           | the file formats of those names                                                                                                          | `Yml` only for `testin.yml`, the file's own name                                              |
| `Enum`, `Bi`, `Max`, `Min`, `Temp`, `Info`, `NoOp`                                               | enumeration, two, maximum, minimum, temporary, information, no operation                                                                 | As Java writes them: `BiConsumer`, `Math.max`, `createTempFile`, the `INFO` log level         |
| `Ide`, `Edt`, `Psi`, `Vfs`, `Ui`, `App`                                                          | integrated development environment, event dispatch thread, program structure interface, virtual file system, user interface, application | As IntelliJ's API writes them: `PsiMethod`, `Service.Level.APP`, `*IdeTest`                   |
| `Git`, `Gh`, `TestNg`, `Mac`, `Ctrl`, `Alt`, `Fqcn`                                              | the products, keys and terms of those names                                                                                              | `Gh` is the GitHub CLI; `Fqcn` the FQCN attribute, as Java names a fully qualified class name |
| `UC`                                                                                             | use case                                                                                                                                 | The `UC-` markers and the use case ids of `docs/standard.md`                                  |

A library's own names are not Testin's to change: Apache POI's `XWPFRun` and
`getTcPr`, Swing's `GridBagConstraints`. A variable holding one is spelled out:
`textRun`, `cellProperties`, `constraints`.

## Process

- **Code cites its rule, and the document changes with the code.** Every method
  a tester can reach carries a marker naming the use case or rule it
  implements — `// UC-TREE-PANEL-012, Rule-TREE-PANEL-038` — and every change to
  what a tester sees changes the document in the same commit. The format, what to do
  when no rule exists yet, and when a method needs no marker are in the
  `code-cites-its-rule` skill under `.claude/skills/`. That skill is the
  authority; do not restate it here.
- **GitHub is the source of truth**: stories/bugs live in `MutebAlMughyiri/test-in`
  issues, not local files. Muteb works across several machines.
- Write new stories straight to an issue with `gh`; update an existing one with
  `gh issue edit <n> --repo MutebAlMughyiri/test-in --body-file ...` instead of opening a
  duplicate. Read current state with `gh issue view` before assuming anything.
- **A feature a tester will see is designed before it is built**, with the
  `feature-design` plugin's `/feature-design:design`, and implemented only after
  Muteb approves the design. The plugin is installed on each machine, not kept in
  this repository.
- Tests live under `src/test` only; the plugin distribution must never
  contain test classes or compile-time-only dependencies (Lombok is
  `compileOnly` + `annotationProcessor`, never `implementation`).

- **The commands, the checks and the gate are in
  [CONTRIBUTING.md](CONTRIBUTING.md)** — the toolchain, `compileJava test`,
  `runIde`, the inspection gate and what it exits non-zero for, where the
  sandbox logs are, the compatibility policy, the issue format and the label
  scheme. One authority, and this file is not it: it was carried here as well
  until #102, and two copies of a command is one of them going stale.

  Read that page before running anything. What follows is only what changes
  how *this* file's instructions are carried out.

- **Verify with `./gradlew compileJava test` before presenting changes**, and do
  not commit until Muteb has sandbox-tested (`./gradlew runIde`) and approved.

- **Two branches at once means two working trees.** `git checkout` moves the
  whole directory, so a second piece of work never shares a checkout with the
  first - `git worktree add ../testin-<what> <branch>`, and remove it when the
  work is merged or abandoned. Switching a branch under work in progress
  destroys it silently, and there is nothing to notice afterward.

- **A hundred compile errors are usually one.** An error in an annotation stops
  Lombok's processing, so every member it would have generated is absent and
  javac reports one error per use. Read the first error rather than the file the
  hundredth blames. CONTRIBUTING.md has the worked example.

- **A green build is not evidence of a working plugin.** NullAway now makes
  `@NotNull` a compile-time contract in the production code and the tests
  (#377, #389): a null passed to a `@NotNull` parameter, or returned where the
  type says it cannot be, fails `compileJava` and `compileTestJava`. Nothing
  checks what a platform callback hands over at run time. So a change a tester
  can see is not finished until it has been run in a sandbox, whatever the
  build says.

- **Never run `./gradlew inspect` by hand.** It runs in CI on every push, on
  every branch, and the result is read from the run afterward — twenty minutes
  of indexing is not something to spend between one edit and the next, and a
  gate somebody waits on is a gate that gets skipped. `compileJava test` is the
  check to run while working; `ideTest` when the change reaches the indexer or
  the tree. Muteb, 22 September 2026: *"no need to run inspector here anymore,
  let us make it every push."*

- **Run `pwsh tools/inspect.ps1 -Quick` before handing a change over.** It is
  the same script with the IDE left out: the rules that read the source as
  text, then `pmdMain`, about a minute over every source root, and it exits
  non-zero on a breach. It was built on 23 September 2026 because warnings kept reaching
  Muteb's IDE one paste at a time — *"many times I told you, still face inspect
  issues"* — and the worst of them, a lambda parameter nothing reads, is one that
  the headless inspector never reports at all: 113 were sitting in the tree.
  `compileJava test` says the code runs; this says the IDE will not complain
  about it.

  A sweep across many files used to be the exception, and it is not one
  anymore: push the branch and read the run. The IDE shows the same findings
  while you type, because `.idea/inspectionProfiles/Testin.xml` is the project
  profile, and **Code | Inspect Code** over the *Inspected* scope produces the
  list the gate reads.

  The bar is the same wherever it is read: zero findings in the files the
  repository writes, apart from the few rules a headless run cannot be trusted
  with. CONTRIBUTING.md names those, and this file does not keep a second list
  that would fall behind.
