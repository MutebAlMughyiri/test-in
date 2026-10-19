[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-002

# UC-INTERNAL-002: Read a test project from disk

**As a** tester, **I want** Testin to read my test projects into memory before
it draws anything, **so that** every panel answers at once instead of going to
disk for each row.

This is the loading step. It is why the tree opens at once.

There is no key for this. It starts on its own.

## Rules

- **Rule-INTERNAL-003** — A folder is a test project only if it holds a file
  named `.testproject`.
- **Rule-INTERNAL-091** — A test project says which format its files are in, in
  its `.testproject`. This build reads format 5 and nothing else: a project with a
  higher number is refused as written by a newer Testin, and one without the
  number or with a lower one as written by an older Testin. A refused project is
  not read and not written: no file inside it is opened. A folder written in
  format 4 or earlier holds no `.testproject`, so it is not a test project
  (Rule-INTERNAL-003) and is not shown.
- **Rule-INTERNAL-004** — Only the folders directly inside the Testin folder are
  looked at. A test project one level deeper is not found.
- **Rule-INTERNAL-005** — A test project that is not **Active** is read,
  and nothing inside it is.
- **Rule-INTERNAL-006** — When a test project is named for this code project -
  by `testin.yml`, or chosen on this machine - only that one is read.
- **Rule-INTERNAL-007** — Inside a test project, Testin reads two folders. They
  are named `Test Cases` and `Test Runs`. Nothing else is read.
- **Rule-INTERNAL-008** — A folder inside `Test Cases` is read only if it holds
  a file named `.testset` or a file named `.testsetpackage`. The first makes it a test set. The
  second makes it a test set package.
- **Rule-INTERNAL-009** — A folder holding both is read as a test set.
- **Rule-INTERNAL-010** — A folder inside `Test Runs` is read the same way, from
  a file named `.testrun` or a file named `.testrunpackage`.
- **Rule-INTERNAL-011** — A file's name says what it is: every file ending in
  `.testcase` directly inside a test set is a test case, and every file ending in `.runitem`
  directly inside a test run is one run item. Nothing looks inside a
  file to decide what it is.
- **Rule-INTERNAL-012** — A test case is known by its file name,
  `<test case UUID>.testcase`, and the file does not write its UUID. A `.testcase`
  whose name is not a UUID is not read, and a warning names it; named by a new
  UUID, it is read at the next Refresh. Two files named by one UUID are both kept,
  and which one is the test case is for the tester to decide.
- **Rule-INTERNAL-013** — Each test project is read by its own background job.
  The tester can cancel it, and the IDE stays usable while it runs.
- **Rule-INTERNAL-014** — One thing that cannot be read never stops the rest.
  Testin skips it and carries on, and says which ones it could not read. A node
  whose marker will not parse is still drawn, and the scan names it by its place
  in the tree: the test project first, then every folder down to it, joined with
  ` > `. A folder name on its own is in every test project and says nothing about
  which node the tester should go and look at.
- **Rule-INTERNAL-015** — A folder skipped for having no marker is reported when
  it holds test cases, and named by its place in the tree as a damaged node is.
  The tester then knows which folder to go to rather than which name to look for.
  A folder holding none is skipped in silence, because a folder that is
  deliberately not a test set is the ordinary situation.
- **Rule-INTERNAL-062** — Reading a test case costs no more than 40 microseconds
  and holds no more than 4 kilobytes. Ten thousand test cases is the size the
  budget is stated at.
- **Rule-INTERNAL-070** — A timestamp is stored in the zone it happened in, on
  every machine. Testin never converts one into the zone of whoever is reading,
  so the same test case is the same bytes wherever it is saved and a colleague
  elsewhere does not rewrite a file by opening it.
- **Rule-INTERNAL-082** — Two test case files claiming one identity are reported
  rather than merged. A test case is identified by its file name, and the index
  holds one test case per identity, so the second file read goes over the first
  and neither test set can reach its own anymore. The read cannot choose which
  of the pair keeps the identity, so it names the files and leaves that to the
  tester.
- **Rule-INTERNAL-083** — Who created a node, and when, is stamped once: the
  first time its marker is written. After that the creation stays as it was,
  even while no tester name is set. A marker file that is there but will not
  parse is never written over, so it is still there to repair.
- **Rule-INTERNAL-089** — `testin.yml` is read and written by one class, and
  written only by a tester's own gesture: **Save to testin.yml**
  (Rule-TREE-PANEL-112), or **Apply** in the bugRepoUrl hint (Rule-VIEW-PANEL-104). Testin never needs it: when a code
  project has none, or
  it leaves a value out, nothing refuses, stalls or asks the tester to create it;
  only the automation code stays off (Rule-CODEGEN-082). When it is there, what
  it says is read in one place, so a missing value means the same thing
  everywhere.
- **Rule-INTERNAL-090** — Every folder carries a UUID of its own in its marker:
  stamped the first time Testin writes that marker, never changed afterward,
  and fresh on a copied folder. Nothing in Testin reads it; it names a test
  project, a test set or a test run for a tool outside the IDE.
- **Rule-INTERNAL-093** — A run item file Testin could not read is never written
  over and never removed. The scan reports it and leaves it out of the test run,
  so the test run covers the test cases it could read; a later write touches
  only the run items the test run holds, and a removal takes only the file whose
  test case a change stopped covering. A run item status nobody can read is
  still a run item status somebody recorded, and the tester repairs the file and
  presses Refresh.
- **Rule-INTERNAL-094** — A run item is known by its file name,
  `<test case UUID>.runitem`, as a test case is (Rule-INTERNAL-012), and the file
  does not write the UUID. A run item file whose name is not a test case UUID is not
  read, and a warning names it; renamed to its test case's UUID, it is read again
  at the next Refresh. Nothing writes a second file for it.
- **Rule-INTERNAL-111** — Waiting for the index is refused under a read lock. It
  logs an error and returns at once, because a read action that waits holds up
  every write in the IDE, the one that finishes indexing included.
- **Rule-INTERNAL-112** — A node is moved, renamed or removed on disk, and the
  file system refreshed, before Testin's own reading of it changes, so that
  reading never names something the disk does not have yet.
- **Rule-INTERNAL-114** — Every change Testin records to a node is announced
  once, after it is recorded. Each surface redraws itself from that: the tree
  for a node, the panel for a test project, and the open editors for a project
  read again from disk.
- **Rule-INTERNAL-115** — A code project bound to a test project reads it when
  the project opens. One with no test project bound reads nothing until
  something needs Testin's index, such as opening the Testin panel or following
  the gutter's link to a test case.
- **Rule-INTERNAL-116** — A folder is known by its path. Two readings of the
  same folder are the same node, however much of it each one read, and two kinds
  of node at one path are not.
- **Rule-INTERNAL-117** — Only Testin's index changes a test case, a test run or
  a marker it holds. A test case is edited as a copy, and the index writes the
  copy into the one it holds once the file is written; a test run or a marker is
  changed by asking the index, which saves it. Whatever shows one of them keeps
  showing the same one and sees the change, and nothing else can change it under
  a reader.
- **Rule-INTERNAL-123** — A change that cannot be written does not stay in
  memory. A test set's or a node's marker keeps its old values. A test run's
  marker that was not written is read again from disk, and a test run whose run
  item or screenshot was not written is read again with the test project that
  holds it. Either way the tester is told it was not saved.
- **Rule-INTERNAL-124** — A test project whose read fails part-way is never
  counted as read. Testin says it could not read it and why, keeps none of it as
  current, and reads it again on the next refresh.
- **Rule-INTERNAL-135** — A stored file holds only the values that differ from a
  fresh one, and a key it leaves out reads back as a fresh one holds it. So an
  empty field is not written, and a passed run item stores no failure detail: no
  actual result, stacktrace, screenshots, bug severity, bug priority or bug
  issue. When a test case or a node was created is always written, because a
  fresh one holds the moment it was made. A run item's `default` and
  `executions` and each execution's `status` are always written too, so a run
  item says what it is in every case (#432).
- **Rule-INTERNAL-141** — A test project is read in two halves on its one
  background job: its test cases first, then its test runs. Each half replaces
  only what it reads, so a read again after a Git step or an outside change
  keeps the old test runs until the new ones are in. The time its test cases
  took is written to the log, beside the time the whole read took.

## The budget

A test project of ten thousand test cases, and the run items of its test runs,
measured rather than estimated.

|                                   | Measured | Budget |
|-----------------------------------|----------|--------|
| **Reading one test case**         | 21 µs    | 40 µs  |
| **Reading ten thousand**          | 214 ms   | 400 ms |
| **Held in memory, per test case** | 1.5 KB   | 4 KB   |
| **Held in memory, ten thousand**  | 14.6 MB  | 40 MB  |
| **Reading one run item**          | 8.8 µs   | 20 µs  |
| **Reading four thousand**         | 35 ms    | 80 ms  |

The run items are their own line because they are their own files: since #305 a
test run of two thousand test cases is two thousand `.runitem` files rather than one
`run.json`, so the question "what does a big cycle cost to read" is a question
about four thousand small documents - one cycle of two thousand test cases and
fifty of forty, which is the shape of a real Testin folder. Two thousand of them
parse in about 18 milliseconds.

Measured on 9 September 2026 (the test cases) and 20 September 2026 (the
run items), Windows 11 with JBR 25, by `IndexerBudgetTest`. CI asserts the budget
on every push, on a runner doing nothing else, so a change that doubles the
cost fails rather than ships. It is its own run - `./gradlew test -Pbudget` -
and not part of the ordinary one, because a clock on a machine that is also
compiling measures the machine.

**What the budget covers is the reading, not the disk.** The same ten thousand
test cases written out as files and walked took **3.5 seconds warm and 150
seconds cold** on the same machine — a forty-fold spread on identical work,
because a cold read of ten thousand freshly written files is the virus
scanner's number rather than the plugin's. Those two figures are measured and
reported by the same test with `-Dtestin.budget.testCases=10000`, and deliberately
not asserted on: a budget that fails on a loaded runner and passes through a
real regression on a fast disk is worse than none.

So the honest statement is two sentences. The part Testin controls is 214
milliseconds for ten thousand test cases. The part the machine controls is
everything else, and it is why the scan runs in the background with a progress
bar the tester can cancel (Rule-INTERNAL-013).

## What starts a read

| The tester does this                              | What Testin reads                                                                 |
|---------------------------------------------------|-----------------------------------------------------------------------------------|
| Opens a code project that has a Testin folder set | Every test project                                                                |
| Presses **Refresh** on the panel toolbar          | Every test project, from nothing                                                  |
| Changes a file outside the IDE                    | The one test project that holds it, see [UC-INTERNAL-003](noticeOutsideChange.md) |
| Finishes a sync with Git                          | The one test project that was synced                                              |
| Switches branch                                   | The one test project in that repository                                           |
| Clones a test project                             | The one that was cloned                                                           |

## The screen

Testin shows one progress bar for each test project it is reading. It sits at
the bottom of the IDE, beside the other background jobs.

```
┌──────────────────────────────────────────────────────────────┐
│  Testin indexing - Checkout                            [ X ] │
│  ████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  40% │
│  Test set: Payment (12 test cases)                           │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always *Testin indexing*, then the test project's name.
2. **The cancel button** — stops this test project. What was read is kept.
3. **The line underneath** — the test set or test run being read right now.

## Main flow

1. The tester opens a code project, or presses **Refresh**.
2. Testin reads the Testin folder from the settings.
3. Testin lists the folders directly inside it, and keeps those holding a `.testproject`
   file.
4. Testin starts one background job for each of them.
5. The bar reads *Indexing*, then the test project's name.
6. The bar reads the test project's name, then *test sets...*.
7. Testin walks `Test Cases`. For each test set, it reads every `.testcase` file
   at the same time. It does not read them one after another.
8. After each test set the bar reads *Test set:*, then its name, then how many
   test cases it holds.
9. The bar reads the test project's name, then *test runs...*.
10. Testin walks `Test Runs`. Each test run reads its own facts from its
    `.testrun` - its status, how it was configured, what the tester wrote about the
    run item statuses, when it was executed. It also reads one file per run item,
    `<test case UUID>.runitem`, in the order their test cases sit in their test sets.
11. The bar reads *Done -*, then the test project's name, and closes.
12. The tree draws itself from memory. Every editor that was open when the IDE
    closed opens again.

## What Testin refuses

**If no Testin folder is set** — nothing is read, and nothing is said. The panel
shows its empty state instead.

**If a folder under the Testin folder holds no `.testproject` file** — it is not a test
project, and it is skipped without a word. This is the ordinary situation. The
Testin folder usually holds other things.

**If Testin itself fails while reading a test project** — the test project is
not counted as read. The tester is told it could not be read, and why (Rule-INTERNAL-124). This is a fault in Testin,
not in the files. A damaged file
is never reported this way. A damaged `.testproject` or other marker is read as defaults.
An unreadable test case or run item is left out, as described below.

**If a marker file is missing** — nothing is said. A node just created has no
marker yet, so Testin uses defaults and the node appears normally.

**If a marker file is there but damaged** — the node still appears, with default
values, and once the read has finished one message names every node whose marker
would not parse. Testin never writes over that file, whatever writes it: a test
case saved into the test set, and a test run's status changed from the tree,
both leave the marker exactly as it is. So the number, the status, who made it,
how the test run was configured and what the tester wrote about the run item
statuses are all still there to repair (Rule-INTERNAL-083).

**If a folder under `Test Cases` holds no `.testset` and no `.testsetpackage` file** — the
folder is skipped. Everything inside it is skipped too. When it holds test
cases, Testin says so once the read has finished. One message names all such
folders. That message waits in the notification list instead of fading, because
a read finishes on its own time and the tester may be looking elsewhere.

**If such a folder holds no test cases** — it is skipped without a word. Most
folders that are not test sets are nothing, and reporting each one would be
noise.

**If one test case file cannot be read** — that one test case is left out of its
test set. The others are read. The set is drawn one row shorter, and nothing
says which row is missing.

**If a test case file is not named by a UUID** — `login.testcase`, written by
hand — it is not read, and once the read has finished one message names all of
them, titled **Test cases not read in \<project\>**. Testin does not make up a
UUID for it; named by a new UUID, it is read at the next **Refresh**
(Rule-INTERNAL-012).

**If one run item file cannot be read** — the test run is drawn without that
run item, and once the read has finished one message names all of them. It is
titled **Run items not read in \<project\>** and it says the file names, up to
five of them, then how many more there are. Nothing writes over a run item Testin
cannot read and nothing removes it, so the repair is to fix the file and press **Refresh**.

**If two test case files claim the same identity** — the second one read goes
over the first, and a notification titled **Test cases sharing an identity in
\<project\>** names the files. Neither test set can reach its own test case
while that is true: both resolve the identity to whichever file landed last, so
an edit in one showed in the other. The file name is the identity, so the
repair is to rename one of them - which of the two keeps it is the tester's to
decide, not the plugin's.

**If the tester presses cancel** — the read stops between one test set and the
next. What was already read stays in memory. The rest of that test project is
missing until the next **Refresh**.

**If the whole read fails** — Testin does not mark itself as read, and tries
again the next time something asks it to.

---

[Documentation](../README.md) › [Inside Testin](main.md)
