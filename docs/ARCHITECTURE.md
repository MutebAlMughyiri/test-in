[Documentation](README.md) › How Testin is put together

# How Testin is put together

The plugin is 651 classes in 32 top-level packages. This page is the map: which
packages are layers and which are side modules, the four rules the whole thing
is built on, and two operations traced class by class — because everything else
is a variation on one of them.

Read this before your first change. It replaces reading 32 packages to find out
where anything lives. It does **not** describe what Testin does for a tester —
that is [the documentation](README.md) — and it does not describe any one
package in detail.

---

## The shape

```
                       the tester
                            |
   +------------------------+------------------------+
   |            |           |          |             |
explorer          editor           view          lightmode      the surfaces
  (tree)         (test/run)       (details)       (read-only)
   |            |           |          |             |
   +------------------------+------------------------+
                            |
              actions, ui.framework                             how a surface
              creator, clipboard, undo, search,                 asks for things
              navigate, open, order, rename, remove
                            |
              testcase  testproject  testrun  bug               the operations
                            |
                        services                                who to ask
             (Services, Notifier, settings, testin.yml)
                            |
                        indexer                                 the only door
        ProjectIndexer   TestCases   TestRuns   Nodes           to test data
             -> IndexerDataStore -> TestCaseSequenceStore
             -> TestDataFiles
                            |
                       VFS / disk

   model  ..... the vocabulary every layer above speaks
   logger ..... written to by all of them, imports none of them

   side modules, each on the indexer and three pairs on each other:
   codegen   git   report   importexport   runner
```

Three content modules sit outside this entirely, loaded only where their
platform plugin is: `testin-java` writes and reconciles the generated Java,
`testin-testng` starts a TestNG execution, and `testin-apimodel` turns an API
call's JSON into a request class and a response record. The core declares
extension points and never learns whether anything answered — see [The content
modules](#the-content-modules).

### The layers and what each is allowed to do

| Layer        | Packages                                                                                                   | May touch test data files                                           |
|--------------|------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Surfaces     | `explorer`, `editor`, `view`, `lightmode`                                                                  | No                                                                  |
| Gestures     | `actions`, `ui`, `creator`, `clipboard`, `undo`, `search`, `navigate`, `open`, `order`, `rename`, `remove` | No                                                                  |
| Operations   | `testcase`, `testproject`, `testrun`, `bug`                                                                | `bug` only, and only the temporary folder a bug report is sent from |
| Services     | `services`, `notifications`, `setting`, `config`                                                           | `config` and `setting` only, and neither touches test data          |
| Data         | `indexer`, `model`                                                                                         | `indexer` only                                                      |
| Side modules | `codegen`, `git`, `report`, `importexport`, `runner`                                                       | See the exempt list below                                           |
| Leaves       | `logger`, `util`                                                                                           | `logger` only, and only its own log                                 |

**Four names left this table on 11 September 2026, and the root package emptied.**
`dialogs` held one class whose only caller is in `ui`, one letter away from
`ui.dialogs` - two names that near each other are a coin toss rather than a
choice. `automate` held one action, and automating a test case *is* generating
its code, which is what `codegen` is. Neither move crossed a layer: each went to
a package in the row it was already in.

`EscapeAction` and `ShowNodeDetailsAction` were in `org.testin` itself, which is
in no row of this table at all. They are in `actions` and `view.marker` now -
where the first already extends `AbstractProjectAction`, and the second opens
`MarkerDetailsViewDialog` and does nothing else.

`testset` held one action and the group that built it. Both were a copy of the
package pair two directories away and of the test project pair one directory
further. That made three classes setting a status, differing in the name of a
DTO and in nothing else. There is one now, in `explorer.tree` where the menu
that offers it lives: a marker says which statuses its node has and applies the
one it is given, so the tester sees the statuses of whatever they right-clicked
and a fourth status anywhere appears with nothing else to change.

`run` merged into `runner`, which is the documented pair #110 asked for. They
were one concern split over two names - `run` started an execution and `runner`
watched it - and a side module owning the actions that invoke it is what `git`
and `importexport` already do. What is left is a pair that cannot be confused:
`runner` executes test cases, `testrun` is the test run a tester creates, names
and records run item statuses into. Merging those two would lose the
distinction, which is why the triple folded to two rather than to one.

`statusbar` left the table on the same day, and it was never the surface this
picture drew. Nothing in Testin adds a widget to the IDE's status bar; what the
package held was the shortcut-hint strip along the bottom of a dialog, plus the
two interfaces a row on one implements. The strip is `ui/framework/StatusBarBase`
now, beside `StatusBarShortcut`, which is what fills it; the interfaces are in
`model`, beside the four enums that implement them (#111).

**The packages that are small and staying that way** are small because this table
says so. `open`, `order` and `remove` hold two files each; they are
Gestures, and the feature each acts on is a Surface. Merging a gesture into the
surface it acts on is this table inverted, and a short package is a smaller price
than a layer that is drawn here and not in the tree (#110).

`logger` imports nothing from the plugin and is imported by 30 packages. `util`
imports only `logger` and `model`, so reaching for a helper can never drag an
editor into the classpath (#112). Keep both that way: a helper that needs
`editor`, `view`, `ui`, `services` or `notifications` is feature glue, and it
belongs beside the feature.

### Where the graph is not a tree

Not every import points down that picture. Three shapes account for almost all
the ones that do not, and all three are deliberate, so they are described here
rather than listed one by one:

- **An action holds the surface it acts on**, and a feature owns the actions
  that invoke it. `git/SyncActionAction` reaching `explorer` is a gesture
  reaching its surface, filed under `git` because that is where sharing lives.
- **A dialog reaches the dialog framework.** `ui/framework` is drawn in the
  gestures row and is infrastructure: everything that opens a dialog reaches it,
  which is what it is for.
- **`testcase` and `testrun` hold vocabulary as well as operations.** #111 moved
  `TestSetEditorAttributes` and `TestRunEditorAttributes` out of `model` into the
  packages that own those fields, so a feature asking what a test case's columns
  are now reaches the operations row to ask. `importexport` does it 30 times,
  `report` 8, `git` twice. That is what the move cost, and it is not a package
  reaching up to *do* anything.

What is left is below. **There is no count here on purpose.** The sentence that
stood here said eleven, defined as "a package importing one strictly above it in
the table" - and by that definition the real number is about ninety, because the
three shapes above are everywhere. A number nothing measures is a number that
goes stale the same week (#66, findings 61 and 77).

| From                                                                  | To                                  | Why                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-----------------------------------------------------------------------|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `indexer/ProjectIndexer`                                              | `editor/open/LastOpenEditors`       | Indexing finishes, and the editors the tester had open are reopened.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `indexer/Rescan`                                                      | `explorer/TreePanel`                | Only to ask `Services.isNotCreated(p, TreePanel.class)` before scanning: a project that never opened the Testin tool window must not be indexed behind the tester's back (#77). It no longer tells the surfaces anything - the indexer announces on `IndexChanged` (#361).                                                                                                                                                                                                                                                                                                                                                                                                               |
| `setting/SettingsConfigurable`                                        | `explorer/TreePanel`                | Applying the settings page rebuilds the tree, because the Testin folder it names is what the tree is built from. Not an action, so it is not the first shape above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `codegen/AutomationState`                                             | `navigate/CodeNavigation`           | Whether a test case has automation behind it is answered by resolving the generated method, and resolving is what `navigate` knows how to do.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `codegen/ExecutionPosition`                                           | `testcase/TestCaseOrder`            | The number a generated method carries is the test case's place in its set, and the set's order is `testcase`'s answer. The third shape above, in one import.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `actions/TestinData`, `actions/Declared`                              | `editor`, `model`, `util`, `logger` | Deliberate, and new with #119. A declared action is built by the platform with a no-arg constructor, so it asks the surface that has the keyboard what is selected - and a data key has to name the type it answers with. `actions` was a leaf until then, and typing the keys as `Object` to keep it one would be worse than the edge.                                                                                                                                                                                                                                                                                                                                                  |
| `testcase/TestSetEditorAttributes`, `testrun/TestRunEditorAttributes` | `ui/Badges`                         | Deliberate. An enum carries its own presentation and its own action rather than being read by an `instanceof` chain at every call site — see the conventions in [CLAUDE.md](https://github.com/MutebAlMughyiri/test-in/blob/main/CLAUDE.md). What is new is where it points *from*: these two were in `model` until 11 September 2026, so the vocabulary every layer speaks pulled the badge painter in behind it (#111). A field of a test case is a fact about `testcase`. What is left is one import each, for the badge a card draws; the other four - `codegen`, `importexport`, `notifications`, `indexer` - are a feature calling a side module and a service, which points down. |

**Side modules are not quite "none on each other".** The drawing above says they
are, and three pairs say otherwise:

|                             |                                                                                                                                           |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `report` -> `importexport`  | The report dialog offers four formats and `importexport/FileTypes` is where a format's extension lives.                                   |
| `importexport` -> `report`  | And back: `FileTypes` names the four report generators, one per format. The pair is a cycle, and it is the only one between side modules. |
| `importexport` -> `codegen` | An import creates test cases, and a created test case gets its generated method like any other.                                           |

**How the surfaces learn of a change.** The indexer announces each change on
`IndexChanged`, the same shape as `TestCaseExecutionListener`: a node changed, a
test project changed, or a project was read again from disk. It announces after
the change is recorded and after the caller's own callback has run, so a rename
has moved the binding before the panel redraws. The tree panel subscribes when it
is created, so a project that never opened the tool window has no listener. The
open editors follow through a project listener in `plugin.xml`. `Rescan` still
asks `Services.isNotCreated(p, TreePanel.class)` before it scans, because that
question decides whether the project is scanned at all (#77). Startup reads only
a bound test project; anything else waits for something to need the index (#364, Rule-INTERNAL-115). Nothing outside
`explorer` and `editor`
refreshes the tree or reloads the open editors itself, and `ArchitectureTest`
fails if something does (#361, Rule-INTERNAL-114).

**Who changes a value the index holds.** The index holds one instance of every
test case, test run and marker, and the EDT and pooled threads read the same
one. Only `model` and `indexer` call a setter on them, and `ArchitectureTest`
fails if anything else does (#376, Rule-INTERNAL-117). A test case is edited as
a copy - `TestCaseDto.edit()` - and handed to `TestCases`. It writes the file
first, then writes the copy into the instance it holds. So every surface showing
that test case sees the change without being handed a new object. A run item, a
test run or a marker is changed inside the lambda `TestRuns` runs before it
saves, through a method named for the change - `linkBug`, `recordFailure`,
`configure` - and a node's order and status through `Nodes`.

What is **not** there: **`model` imports nothing above it at all** - not
`editor`, not `explorer`, not `view`, and since 11 September not `indexer`,
`codegen`, `creator`, `services`, `notifications`, `importexport`, `ui` or
`statusbar` either. The leaf rule is at zero and `ArchitectureTest` no longer
carries a single exception to it (#111).

Four of the six that had to move were tables: per node kind, what some feature
does for it. Each lives in the package that knows the answer. Three of them -
`creator/NodeCreators`, `codegen/JavaCode`, `remove/Removals` - answer through
one `of(NodeType)` whose body is a `switch` over every kind with no
`default`, so a kind added without an entry fails `compileJava`, and no call
site asks what it is holding (#350). The fourth, `indexer/Gathered`, is
still an enum with one constant per way of counting, bridged by
`valueOf(...name())`, and `NodeKindTablesTest` guarantees by name that the two
agree.

---

## Package sizes

A package holds one part of one feature, and its file list should read as a
description of that part. Past 20 classes it splits along the feature's parts,
or it is named here with the reason it cannot. `PackageSizeTest` reads this
table: it fails for a package over 20 that has no row, and for a row whose
package has dropped to 20 or fewer. A new package under 4 classes is refused in
review, because #110 merged those. The line and the two rows were decided with
Muteb on 5 October 2026 (#394).

| Package        | Why it stays whole                                                                                                                                  |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `ui.framework` | One framework with one register. `ComponentDialogBase` refers to 30 of its classes, and a split would make nine of its internals public.            |
| `indexer`      | Rule 1: the indexer is the only owner of file access. Its file layer is package-private so nothing else can reach it; a split would make 15 public. |

---

## The four rules

### 1. All test data file access goes through the indexer

`org.testin.indexer` is the single owner of reading and writing test data. Its
cache is therefore authoritative, and every read a surface makes is an in-memory
lookup rather than a disk hit.

The package is one owner with four doors, one per area, and a caller asks the
one for what it holds:

| Service          | Answers for                                                                              |
|------------------|------------------------------------------------------------------------------------------|
| `ProjectIndexer` | Scanning and the index's lifecycle: the first read, a rescan, whether it is indexed yet  |
| `TestCases`      | Test cases: find, save, move, remove, order, and the file each one lives in              |
| `TestRuns`       | Test runs: find, change a test run, a result or its marker, and a test run's screenshots |
| `Nodes`          | The tree's folders: find, add, rename, move, copy, remove and restore, and their markers |

All four share one store, one test run writer and one announcer, which
`ProjectIndexer` builds and hands out inside the package only, so there is still
one cache and one `IndexChanged` announcement per change.

- Need to know whether a node exists? Ask the indexer's cache — never
  `Files.exists`.
- Need to create, move, rename, copy or delete? Call the indexer. It performs the
  VFS operation and updates the cache, in that order.
- UI code holds `Node` and `TestCaseDto` objects the indexer served, and
  never touches disk.

Test runs in particular are saved and read only through the indexer —
`TestRuns.putRunItems` to create one, `changeRunItems` and `saveRunItems` to change
its run items, `changeTestRunMarker`, and `Nodes.addTestRunNode` for its folder. The
sequential run item writer lives inside the package. It writes one file per run item - `<test case id>.ri` - so recording
a run item status writes that one file, and two testers judging different test cases of a test run never touch the
same one. A test run's screenshots are its files too:
`TestRuns.storeScreenshots` writes them, `screenshot` reads one, and the test
run writer removes those no result names.

The rule is enforced by the compiler rather than by review: `TestDataFiles` and
`VfsExecutor` are package-private and live in `indexer`, so nothing outside the
package can reach the writer at all.

**The exempt packages**, which may open files directly: `codegen`, `config`,
`git`, `importexport`, `report`, `setting`, `logger`, `bug`. `codegen` includes
the Java module's `org.testin.java.codegen`, which writes the generated classes.
`ArchitectureTest` reads all three modules and allows one class besides:
`ui.framework.TextInput`, which asks the VFS which folder a file chooser opens
at, and that is never test data.

What they have in common is that none of them read or write **test data**. They
handle generated source, the automation repository's own `testin.yml`, the Git
working tree, files outside the tree, generated report output, the IDE settings
path, the log, and the temporary folder a bug report is sent from. `config` in
particular reads a file that lives in the automation repository rather than
under the Testin folder. It runs before the indexer exists. The name it reads is
one of the two `BoundTestProject` weighs to tell the indexer which project to
index.

`bug` joined the list with #28. It writes a bug report's body and screenshots
into a fresh temporary folder, runs `gh` from there, and deletes the folder
afterward. The test case's own file is asked of the indexer (`TestCases.testCaseFile`), never built. Its other
edges point down: `git`
for the test project's remote and branch, `report` for `ReportText.joined`, and
`config` for `bugRepoUrl`.

### 2. The VFS operation succeeds first, then the cache is updated

Never the other way round. The cache update may persist markers, and a marker
write creates directories — so a cache update that runs first produces phantom
directories and "already exists in VFS" errors.

`Nodes.moveNode` is the shape to copy: `VfsExecutor.executeVfsAction`
performs the move and takes two callbacks, and `store.renameNode` is called from
the success one.

### 3. Swing is read and written only on the EDT

Anything else that runs during a UI action moves off it.

| Work                                                                                | How                                                                                                           | Gets an indicator                        |
|-------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------------------------|
| Short, no UI of its own — badge recomputes, filtering, sorting                      | `ApplicationManager.getApplication().executeOnPooledThread(...)`, finishing with `invokeLater` to touch Swing | No                                       |
| Long, and the tester should be able to cancel it — indexing, Git, report generation | `Task.Backgroundable`                                                                                         | Yes, and it participates in cancellation |

If a pooled recompute is slow enough to want a progress bar, cache the result
instead of backgrounding it harder. Actions declare `ActionUpdateThread.EDT` when
their `update()` reads Swing state.

### 4. Formatting is display-only

Rendering may reformat a value. Saving never does. The stored JSON is always
byte-identical to what the tester typed.

Two methods on `testcase/TestSetEditorAttributes` are the whole rule:

- `gridValue(tc)` — the raw value, for anything typed into.
- `displayValue(tc)` — the same value with a sentence made of it where the tester
  wrote prose, for anything only read.

A grid cell and an editor field load `gridValue`, so a period `displayValue`
would have added is never committed back into the JSON the first time a tester
opens a cell they had not changed. That is #22, and it is why the two methods are
not one.

---

## Walkthrough one: a test case is saved

A tester edits a cell in the grid and presses Enter. Ten steps later the JSON on
disk is either changed or byte-identical, and which one it is decides whether
anything else happens at all.

| #   | Where                                                    | What happens                                                                                                                                 |
|-----|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | `editor/grid/TestSetGridEditListener`                    | Reads what was typed, parses it for the column's type, and compares it to what the test case already held. **Unchanged, and it stops here.** |
| 2   | `editor/grid/TestSetGridEditListener.persistAndGenerate` | Moves off the EDT with `executeOnPooledThread`, because the codegen in step 10 schedules its own write commands.                             |
| 3   | `indexer/TestCases.putTestCase`                          | The public door. Returns a boolean: did this have anything to save.                                                                          |
| 4   | `indexer/IndexerDataStore.putTestCase`                   | Delegates the write, then stamps the **set's** marker as modified — but only if the write happened.                                          |
| 5   | `indexer/TestCaseSequenceStore.put`                      | The funnel every save arrives at: the update dialog, a grid cell, the details panel, a paste.                                                |
| 6   | `indexer/TestDataFiles.alreadyHolds`                     | Serializes the test case and compares the bytes to the file. **Identical, and nothing below runs.**                                          |
| 7   | `indexer/TestCaseSequenceStore.put`                      | Stamps the audit — `touch` if the index already knows this id, `stampCreated` if it does not.                                                |
| 8   | `indexer/TestCaseSequenceStore.store`                    | Updates the two maps, then writes.                                                                                                           |
| 9   | `indexer/TestDataFiles.write`                            | Refuses a zero-byte write, claims the path in `OwnWrites` **before** `Files.write`, writes, then records what landed.                        |
| 10  | back in `TestSetGridEditListener`                        | The attribute's `GenType` regenerates the test method, and `TestCaseSnapshot.record` files the undo entry.                                   |

**Why the bytes are identical.** Step 6 puts the rule's question in the rule's
own terms: would this write leave the file the same. It has to be asked as
bytes, and it has to be asked *before* step 7, because the stamp is itself a
change. `touch()` writes a new `updatedAt`, and anything compared after it
differs by the one field the check exists to avoid writing. It also cannot be
asked in memory: the index hands out its own objects and the dialogs edit them in
place, so by the time a save arrives the indexed test case and the test case
being saved are the same object, and the file is the only record of what it
looked like before. Opening a field to read it and pressing Enter used to
record the tester as having edited the test case (#164).

Two things follow from step 6 answering true. Nothing is written, so a tester's
next commit does not contain a file they never changed; and step 4 does not stamp
the set either, because a save that changed nothing did not modify the set, and
stamping it would move the lie one level up.

**Step 9's ordering is a bug fix, not a preference.** The VFS event can arrive
while the thread is still inside `Files.write`, and a file claimed a moment too
late looks to the watcher like somebody else's edit (#20). Claim first, record
what landed afterward, so an edit the tester makes inside that window is told
from this write rather than swallowed with it (#278).

There is one deliberate bypass. `putTestCaseVerbatim` stores without stamping: an
import writes the audit the imported file carries, and an undo writes the audit
the test case had before the change being taken back — stamping either would
record the tester as having modified a test case at the moment they un-modified
it (#164, #165).

## Walkthrough two: a test set is run

The tester right-clicks a test set and picks Run Tests. The plugin does not know
how to run anything; a content module does.

| #   | Where                                                                            | What happens                                                                                                                                                                                                                                                         |
|-----|----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | `runner/ExecuteTestsAction`                                                      | Asks the selected node which gesture this is. A node that *holds* test cases hands them straight to the runner; a **test run** is opened in its editor first, because a run item status reaches a named test run only through the editor that claimed the test case. |
| 2   | `runner/ExecuteTestCases.run`                                                    | Refuses if the TestNG plugin is absent, drops the test cases already running, and starts what is left **as one execution** — one compile and one JVM, not twelve. Notifies once, with a count.                                                                       |
| 3   | `runner/TestRunner.available()`                                                  | The extension point `org.testin.testRunners`. Empty in an IDE where nothing can run — an answer, not a missing one.                                                                                                                                                  |
| 4   | `testin-testng/TestNGRunner.run`                                                 | Finds each test case's generated method as a `PsiClass`, reports the ones with no generated code, and builds a `TestNGConfiguration` whose pattern set names class and method.                                                                                       |
| 5   | `runner/TestNGExecution.launch`                                                  | Hands the configuration to the platform and remembers which test cases this environment covers, so a stop knows what to put back.                                                                                                                                    |
| 6   | `runner/TestNGExecution.starting`                                                | Broadcasts each test case as running before the process exists, so the cards change at the click rather than at the first report.                                                                                                                                    |
| 7   | the platform                                                                     | Compiles, starts a JVM, runs TestNG.                                                                                                                                                                                                                                 |
| 8   | `testin-testng/TestNGEventRelay`                                                 | Declared in the TestNG module's descriptor as a listener on `SMTRunnerEventsListener`, so it exists only where TestNG does. Turns each started and finished event into a `TestCaseExecutionListener.broadcast`.                                                      |
| 9   | `runner/TestCaseExecutionSubscriber.record`                                      | On the EDT. The test name **is** the test case's id, because that is what Testin named the generated method — so there is nothing to look up. A name that is not an id is nobody's, and is logged.                                                                   |
| 10  | `runner/TestCaseExecutionSubscriber.report`                                      | Decides what the report means: a test case the tester stopped reports itself failed, and that is not a failure. Records the run item status against the **id**, then tells the surfaces.                                                                             |
| 11  | `editor/testset/TestSetEditor`, `editor/testrun/TestRunEditor`, `view/ViewPanel` | Repaint.                                                                                                                                                                                                                                                             |

**One recorder per project, not one per surface.** Step 10 runs even when
nothing is open. Each surface used to subscribe and record for itself, so the
model was updated once per open surface and not at all when none was open.
Running a test set from the tree in a session where no editor had been opened
stored no run item status at all. Every card afterward showed no result for an
execution that had passed (#66).

The order in step 10 is guaranteed rather than hoped for. A surface asked to
redraw reads what is running from `TestNGExecution`, so it must not be told before
`TestNGExecution` has been. One subscriber that records and then calls the
surfaces gives that; two independent subscriptions would have run in whichever
order the message bus chose.

**The run item status is recorded against the id, not the object.** The
`TestCaseDto` in hand is replaced by the next rescan, and a run item status that
lived on it went with it — which is how a test case that had just passed lost
its badge at the tester's next keystroke (#116).

---

## The content modules

The core plugin runs in every IDE. Anything that needs another plugin's classes
lives in a content module, which the platform loads only where that plugin is
present.

| Module            | Needs             | Contributes                                                                                                                                                                                                                                                 |
|-------------------|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `testin-java`     | the Java plugin   | Writing, moving, renaming and reconciling the generated test classes and methods, and the gutter mark beside a generated method                                                                                                                             |
| `testin-testng`   | the TestNG plugin | `TestNGRunner`, the one implementation of `runner/TestRunner`                                                                                                                                                                                               |
| `testin-apimodel` | the Java plugin   | **New → Testin API Model from JSON** on a package: the request as a Lombok class and the response as a record (UC-CODEGEN-022). It answers `codegen/ApiModelMaker`, and reaches the core otherwise only through its public classes and the dialog framework |

`testin-apimodel` answers `ApiModelMaker`, whose action `plugin.xml` declares with
every other action. With no Java plugin the action is gray and says why, as
every code action is (Rule-CODEGEN-082).

The core declares the extension point and never learns whether anything answered:
`TestRunner.available()` returns a runner that logs and starts nothing when the
list is empty. A module running a different framework on a different IDE
contributes to the same point, and nothing in the core changes.

---

## What this page does not cover

|                                                                      |                                                                                         |
|----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| What Testin does for a tester                                        | [the documentation](README.md) — 150 use cases, every rule numbered                     |
| Why a design that looks wrong is that way                            | [Standing decisions](decisions.md)                                                      |
| Every file Testin writes, field by field                             | [The formats on disk](formats.md)                                                       |
| Setup, the checks, and how to contribute                             | [CONTRIBUTING.md](https://github.com/MutebAlMughyiri/test-in/blob/main/CONTRIBUTING.md) |
| Naming, nullability and the conventions a change is reviewed against | [CLAUDE.md](https://github.com/MutebAlMughyiri/test-in/blob/main/CLAUDE.md)             |
| What one package does                                                | its own classes — the javadoc is the authority there                                    |
