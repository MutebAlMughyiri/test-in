[Documentation](README.md) › The formats on disk

# The formats on disk

> Everything Testin stores is a plain file you can open. There are ten formats,
> all of them JSON except one YAML and the screenshots, which are PNG. This page
> is the contract for every one — what a field means, which are required, and
> what a reader must do with a field it does not recognize.

|                     |                                                                                                                                                                                                                                                                                                                                                                       |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | None. This is the shape of the data, not a screen                                                                                                                                                                                                                                                                                                                     |
| **Answers**         | What is on disk, what may change, and what a version bump promises                                                                                                                                                                                                                                                                                                    |
| **State**           | Written                                                                                                                                                                                                                                                                                                                                                               |
| **Checked against** | `main` at `1270e599`, 20 September 2026 — every marker, the test case, the run item and `testin.yml` read field by field against the new file structure (#305), and the committed sample under `samples/testin-root` read against this page. The real tree at `Testin/NAFATH` was read at `f369f1d6`, 9 September 2026, and is still in the format this page replaced |
| **Written to**      | [The standard](standard.md), as far as it applies. A format is not a use case, so it has a shape and an example instead of a flow                                                                                                                                                                                                                                     |

---

## The tree

```
<Testin folder>/
└── NAFATH/                       a test project
    ├── .tp                       the test project marker
    ├── Test Cases/
    │   ├── .tcf                  the Test Cases directory marker
    │   ├── Login/                a test set package
    │   │   └── .tsp
    │   └── ts2/                  a test set
    │       ├── .ts
    │       └── 4fd2a19b-….tc     one test case, named by its id
    └── Test Runs/
        ├── .trf                  the Test Runs directory marker
        ├── Cycles/               a test run package
        │   └── .trp
        └── cycle31/              a test run
            ├── .tr
            ├── 4fd2a19b-….ri     one run item, named by the test case it is about
            └── k3f9a.png         a screenshot a failure names
```

**A marker is a file whose whole name is the extension.** `.ts`, not `foo.ts`.
A directory is a test set because it holds a file called `.ts`, and that is the
only thing that makes it one — the folder name is the node's name and carries no
meaning beyond it.

**The names `Test Cases` and `Test Runs` are fixed.** They are the folder
names of `NodeType.TCF` and `NodeType.TRF` and a test project has
exactly one of each.

---

## What is true of every JSON file

|                      |                                                                                                                                                |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **Encoding**         | UTF-8, pretty-printed with two spaces, and a space either side of the colon — Jackson's default pretty printer, unchanged                      |
| **Unknown fields**   | Ignored, never an error. Every type carries `@JsonIgnoreProperties(ignoreUnknown = true)`                                                      |
| **Missing fields**   | Take the field's default, which is always an empty value of its own type — never null                                                          |
| **Dates**            | `EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'` in `en_US`, for example `Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]`. The zone is part of the value  |
| **"Never happened"** | The Unix epoch in UTC, `Thursday 01-01-1970 At 00:00:00 [Z]` — `Config.NOT_EXECUTED`. Not a missing field, and not null                        |
| **Enums**            | Written by constant name in capitals. A name this build does not know is a read failure for that file, not a silent default — see *Versioning* |

---

## The seven markers

Every marker shares the same six fields, from `AbstractMarker`, and most add a
`status` of their own.

### Shared by all seven

| Field       | Type        | Required                    | Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|-------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`        | UUID string | no, **omitted while empty** | The folder's own id. Stamped the first time Testin writes the marker and never changed after — not by a rename, a move, a status or an order change, or a merge — and fresh on a copied folder, because a copy is another folder. A marker written before ids has none and is stamped the next time anything writes it. One marker writer stamps them, and it refuses to write over a marker file that is there and will not parse, so a damaged marker is left for the tester to repair rather than replaced by defaults with an id on top (Rule-INTERNAL-083). Nothing in the plugin reads the id: it names a test project, a test set or a test run for a tool outside the IDE (Rule-INTERNAL-090) |
| `order`     | integer     | no                          | Where the node sits among its siblings. **Omitted entirely when the node is unordered**, which is `Integer.MAX_VALUE` in memory — an absent `order` is the normal case, not a defect                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `createdBy` | string      | no, defaults `""`           | The tester's name as the settings hold it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `createdAt` | date        | no, defaults to now         | When the directory was made                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `updatedBy` | string      | no, defaults `""`           | Who last changed it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `updatedAt` | date        | no, defaults to the epoch   | When it last changed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |

**A blank `updatedBy` reads back as `createdBy`, and a blank `updatedAt` as
`createdAt`** — a node that was never modified was last touched when it was
made. The answer is given once, in `AbstractMarker`, so nothing downstream tests
for it, and what is written back is that answer rather than an invented one.

### The seven and what each adds

| File   | Node                       | Adds                                                                                                                                      | Values                                                              |
|--------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| `.tp`  | Test project               | `status`, and `format` - which format this project's files are in, 3 for the one described here                                           | `ACTIVE` `INACTIVE`                                                 |
| `.tcf` | The `Test Cases` directory | —                                                                                                                                         |                                                                     |
| `.trf` | The `Test Runs` directory  | —                                                                                                                                         |                                                                     |
| `.tsp` | Test set package           | `status`                                                                                                                                  | `ACTIVE` `ARCHIVED`                                                 |
| `.ts`  | Test set                   | `status`                                                                                                                                  | `ACTIVE` `DEPRECATED`                                               |
| `.trp` | Test run package           | `status`                                                                                                                                  | `ACTIVE` `ARCHIVED`                                                 |
| `.tr`  | Test run                   | `status`, and the test run's own facts: `configuration`, `resultAnalysis`, `executionStartedAt`, `executionEndedAt`, `commit` — see below | `CREATED` `IN_PROGRESS` `COMPLETED` `ASSIGNED` `CLOSED` `COMMITTED` |

**A directory carrying two markers of one family is read as the more specific
one.** Under `Test Cases` the order is `.ts` then `.tsp`; under `Test Runs` it is
`.tr` then `.trp` — a set is what holds test cases and a package is what holds
sets. `NodeType.UNDER_TEST_CASES` and `UNDER_TEST_RUNS` are that
precedence, and nothing else may ask in a different order.

```json
{
  "id" : "8c1f0f2e-6a44-4a31-9f0b-5b2a7c4e91d3",
  "createdBy" : "Mohammed AlZamil",
  "createdAt" : "Wednesday 02-09-2026 At 23:29:16 [Asia/Riyadh]",
  "updatedBy" : "Mohammed AlZamil",
  "updatedAt" : "Wednesday 09-09-2026 At 05:28:11 [Asia/Riyadh]",
  "status" : "ACTIVE"
}
```

**The test run marker adds five more**, because they are facts about the test
run rather than about any one run item. They are what it was executed against,
what the tester wrote about the run item statuses afterward, when execution
started and last stopped, and the commit that made it a record. They are the test run's, so they are in the test run's
own file:

| Field                | Type   | Required                   | Meaning                                                                                                                                                                                  |
|----------------------|--------|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `configuration`      | map    | no, **omitted when empty** | What the test run was executed against. Keys are `TEST_TYPE` `CHANGE_LOG` `COMMIT` `PLATFORM` `COMPONENT` `LANGUAGE` `BROWSER` `DEVICE_TYPE`; values are free text                       |
| `resultAnalysis`     | map    | no, **omitted when empty** | What the tester wrote about each group of run item statuses. Keys are `PASSED` `FAILED` `BLOCKED` `UNTESTED`; values are free text                                                       |
| `executionStartedAt` | date   | no, defaults to the epoch  | When Start Execution was first pressed. Kept: a test run resumed next week still started when it started                                                                                 |
| `executionEndedAt`   | date   | no, defaults to the epoch  | When execution last stopped                                                                                                                                                              |
| `commit`             | string | no, **omitted when empty** | The commit that made the test run Committed, written by Testin straight after it (Rule-SHARE-130). Its run items show their test cases as that commit holds them (Rule-EDITOR-PANEL-239) |

```json
{
  "id" : "b7d3a5c1-2e48-4f6a-8d09-1c5b7e2a4f80",
  "createdBy" : "Mohammed AlZamil",
  "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
  "updatedBy" : "Mohammed AlZamil",
  "updatedAt" : "Monday 14-09-2026 At 10:22:05 [Asia/Riyadh]",
  "status" : "IN_PROGRESS",
  "configuration" : { "TEST_TYPE" : "Regression", "PLATFORM" : "Web", "BROWSER" : "Chrome" },
  "executionStartedAt" : "Monday 14-09-2026 At 10:00:12 [Asia/Riyadh]",
  "executionEndedAt" : "Thursday 01-01-1970 At 00:00:00 [Z]"
}
```

---

## A test case — `<uuid>.tc`

One file per test case, inside its test set. **Any `.tc` file directly inside
a test set is a test case** (Rule-INTERNAL-011), so `login.tc` written by hand
is read as one and a copy of it is given an id of its own; a `.tc` sitting in
`Test Cases` rather than in a set is not a test case at all. The file holds
JSON; the name says what the JSON is, so nothing has to look inside to find
out.

Testin writes the file as `<id>.tc`, and a hand-named one is filed under its
id the next time anything writes it, the hand-named file going once that write
has landed (Rule-INTERNAL-084). The two names answer different questions:
the **file name** is what the tree shows (Rule-INTERNAL-012 — a `name` field
inside the file would not decide it), and the **`id` field** is what a test run's
recorded run item points at.

| Field                     | Type             | Required               | Meaning                                                                                     |
|---------------------------|------------------|------------------------|---------------------------------------------------------------------------------------------|
| `id`                      | UUID string      | yes in practice        | Identity. A fresh random UUID when absent, which makes the test case a new one              |
| `order`                   | string           | no, defaults `""`      | The rank that places the test case in its set — see below                                   |
| `description`             | string           | no                     | What the test case is. The card title                                                       |
| `expectedResult`          | string           | no                     |                                                                                             |
| `steps`                   | array of strings | no                     | One step per element. A blank element is skipped when drawn and keeps its place in the file |
| `status`                  | enum             | no, defaults `PENDING` | `REVIEWED` `PENDING` `DISABLED` `TO_BE_UPDATED`                                             |
| `priority`                | enum             | no, defaults `LOW`     | `HIGH` `MEDIUM` `LOW`                                                                       |
| `groups`                  | array of strings | no                     | The groups the test case is in, each as the tester typed it. Empty for none                 |
| `reference`               | string           | no                     | A ticket, a requirement — an identifier, never formatted for display                        |
| `module`                  | string           | no                     |                                                                                             |
| `testData`                | string           | no                     | Used rather than read: never reformatted, on any surface                                    |
| `preConditions`           | string           | no                     |                                                                                             |
| `createdBy` / `updatedBy` | string           | no                     |                                                                                             |
| `createdAt` / `updatedAt` | date             | no                     |                                                                                             |

**`order` is a rank, not a number.** `"zo"` is a valid order and sorts as text.
A test case carries where it sits, not who its neighbors are; the earlier
design gave each test case a `previous` and a `next`, so every insertion,
deletion and reorder rewrote a file the tester had not touched — which is what
made two people working in parallel conflict on a third person's test case, and
what let one lost pointer leave a whole set unordered.

**An empty `order` means "not placed yet"** — imported, copied in, or arrived
from a merge. Those sort after the placed test cases, oldest first, and are
given a rank the next time anything writes them. It is not an error and must
not be repaired on read.

There is **no separate sequence file.** `TestCaseSequenceStore` is an in-memory
index the plugin builds while scanning; the order on disk is the `order` field
on each test case and nothing else.

```json
{
  "order" : "zo",
  "id" : "4fd2a19b-59c7-44df-8cc4-ec5d293b18e9",
  "description" : "Log in with a valid user",
  "expectedResult" : "The dashboard opens",
  "status" : "REVIEWED",
  "steps" : [ "Open the login page", "Enter a valid user", "Submit" ],
  "priority" : "HIGH",
  "reference" : "",
  "groups" : [ "SMOKE" ],
  "createdBy" : "Mohammed AlZamil",
  "updatedBy" : "Mohammed AlZamil",
  "createdAt" : "Wednesday 02-09-2026 At 23:29:28 [Asia/Riyadh]",
  "updatedAt" : "Monday 07-09-2026 At 06:49:39 [Asia/Riyadh]",
  "module" : "",
  "testData" : "",
  "preConditions" : ""
}
```

---

## A run item — `<test case id>.ri`

One file per test case the test run covers, inside the test run's folder, beside
its `.tr`. A test case appears in a test run once, so the test case's id is the
file's name and two testers adding the same test case offline write the same
file rather than two.

It records what was executed, not what exists: a test case removed from the test
set keeps its run item here. What the test run itself is - how it was configured,
what the tester wrote about the run item statuses, when it was executed - is in
its `.tr`, with its status.

The run items are read in the order their test cases sit in their test sets, and
a run item whose test case the project no longer holds comes last. So a report
prints them in the order a tester reads the tree, never in the order a folder
listing happens to give.

**A run item's file goes only when the test run stops covering its test case.**
Unticking a test case in Edit Test Run removes that one `.ri`, and nothing else
in the folder is touched. A `.ri` the test run in memory does not cover is left
exactly where it is: it can be one a pull brought a moment ago, or one a failed
write could not produce, and removing either would lose a run item status nobody
asked to lose. Writing a run item status writes the one file it is about, so two
testers judging different test cases of one test run never touch the same file.

**A run item is known by its file name** (Rule-INTERNAL-094). A `.ri` whose name
is not a test case id, `login-result.ri` renamed by hand for one, is not read:
the scan names it in a warning, and nothing writes a second file for it.
Renamed to its test case's id, it is read again at the next Refresh.

The screenshots a failure names sit in the same folder, one PNG each, named by
five random lowercase letters and digits that no run item of the test run already
holds - `k3f9a.png`. Testin writes a screenshot before the run item that names it,
and moves one that no run item of the test run names anymore to the recycle bin
after the next write. Any PNG in a test run folder named that way is taken for a
screenshot, so one put there by hand under such a name goes too.

| Field                    | Type             | Meaning                                                                                                                                                                                   |
|--------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                     | UUID string      | The test case this run item is about - the same id the file is named by, which is what decides it (Rule-INTERNAL-012). Written so the file says what it is about on its own               |
| `bugSeverity`            | enum             | `BLOCKER` `MAJOR` `MINOR` `ENHANCEMENT`. `ENHANCEMENT` until the tester chooses another (Rule-EDITOR-PANEL-147). A later execution never resets it (Rule-EDITOR-PANEL-284)                |
| `bugPriority`            | enum             | `HIGH` `MEDIUM` `LOW`. `LOW` until the tester chooses another (Rule-EDITOR-PANEL-147). A later execution never resets it                                                                  |
| `bugIssueUrl`            | string           | The GitHub issue the failure was reported as, written by [Report Bug](viewPanel/reportBug.md). Empty until then, and kept by every later execution                                        |
| `untested`               | boolean          | `true` for a test case still pending when the test run completed or closed (Rule-TREE-PANEL-067). Left out otherwise                                                                      |
| `clock`                  | number, seconds  | What the walk's clock counted on this test case before a run item status was recorded, so a walk that pauses picks it up again (Rule-EDITOR-PANEL-132). Left out when nothing was counted |
| `unrecordedActualResult` | string           | An actual result typed before the test case was executed. The next execution takes it (Rule-EDITOR-PANEL-172). Left out when there is none                                                |
| `executions`             | array of objects | Every execution of this test case in this test run, the oldest first (Rule-EDITOR-PANEL-136). Left out until the first; with none, the run item is Pending. Each is in the table below    |

An execution:

| Field          | Type             | Meaning                                                                                                                                                                                                                      |
|----------------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `status`       | enum             | `PASSED` `FAILED` `BLOCKED`, as a tester or the automation judged it                                                                                                                                                         |
| `duration`     | number, seconds  | Nanosecond precision, written as a decimal. Left out when nothing was timed                                                                                                                                                  |
| `executedBy`   | string           |                                                                                                                                                                                                                              |
| `executedAt`   | date             |                                                                                                                                                                                                                              |
| `actualResult` | string           | What happened. Left out when nothing was written                                                                                                                                                                             |
| `stacktrace`   | string           | The error. Text only: a pasted screenshot is never in it                                                                                                                                                                     |
| `screenshots`  | array of strings | The file names of the screenshots pasted with the failure, beside this file, in the order they were pasted. Left out when there are none; their files go with the next write after the last execution naming them is removed |
| `invocation`   | number           | Which invocation of the method this was, from 1, when the automation ran it more than once (Rule-CODEGEN-112). Left out otherwise                                                                                            |
| `retry`        | number           | Which retry of its invocation this was, from 1. Left out for the first attempt                                                                                                                                               |
| `retried`      | boolean          | `true` for an attempt TestNG skipped and ran again. It is recorded as Failed and never decides (Rule-CODEGEN-111)                                                                                                            |
| `parameters`   | string           | The data provider row this attempt ran, such as `[1234]`. Left out for a method with no data provider                                                                                                                        |
| `default`      | boolean          | `true` on the execution a tester chose with ☆ (Rule-VIEW-PANEL-119). Written only when that is not the last; with none flagged, the last is the default                                                                      |

The run item status is the default execution's status. A run item with no
executions is Pending, or Untested when `untested` says so; one whose test case
was deleted since is shown as Removed while the test run is not Committed, and
with the status its commit recorded once it is (Rule-EDITOR-PANEL-126).

A run item keeps no copy of its test case (Rule-EDITOR-PANEL-238): a test run
that is not Committed shows each test case as it is now, and a Committed one reads
it from its `commit`. A `testCase` object an earlier version wrote is not read.

A severity and a priority are never missing. A run item nobody triaged carries
`"bugSeverity" : "ENHANCEMENT"`, and only a failure shows it. A file that still
holds `"EMPTY"`, which earlier versions wrote, cannot be read.

---

## `testin.yml` — the automation repository's own file

The one file that lives in the code repository rather than under the Testin
folder, and the only one that is **committed**. It is the team's: Testin reads
it when it is there, and **writes it only when the tester presses Save to
testin.yml** (Rule-TREE-PANEL-112). A code project without one works fully,
except that its automation code stays off until the file names the open test
project (Rule-INTERNAL-089, Rule-CODEGEN-082, Decision-013). What it adds is agreement - a
colleague who clones the repository lands on the same test project with no
setup. `testin.yaml` is read too.

| Key             | Type               | Required             | Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------|--------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `location`      | `local` / `remote` | no, defaults `local` | Whether the test data is on this machine or cloned from Git, the only way a test project is shared                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `repoUrl`       | string             | only when remote     | **Kept as it is written.** Whether the text is a repository address at all is decided where something is about to clone it (Rule-TREE-PANEL-117), so a value Testin does not recognize stays in the file to be seen and corrected rather than being dropped as it is read. Any account and token are dropped as it is read: on an `https://` or `http://` address every account goes, because there it is the secret; on an `ssh://` address only `account:secret` goes, so the conventional `git@` survives (Rule-SHARE-004) |
| `testinProject` | string             | no                   | Which test project, for everyone who opens the code project. Without it each tester chooses one, kept on their own machine; with it, a tester's own choice still wins until the file names a different project (Rule-TREE-PANEL-106)                                                                                                                                                                                                                                                                                          |
| `bugRepoUrl`    | string             | only to report bugs  | The GitHub repository [Report Bug](viewPanel/reportBug.md) files issues in, as its address: `https://github.com/owner/repo`, `https://host/owner/repo.git`, `ssh://git@host:2222/owner/repo` or `git@host:owner/repo`. Any account and token in it are dropped as it is read, by the same rule as `RepoUrl`. An address that does not name exactly a host, an owner and a repository - one ending in `/issues`, a file or a local path - is kept, and Report Bug refuses to send with the reason                              |

**No machine or person appears here.** The Testin folder, the tester's
name and the log level are application settings, and the test
project a tester chose is kept on their machine — see `CLAUDE.md`, and
Decision-011 on [the decisions page](decisions.md). An unknown key is logged
and skipped, never fatal - which is what happens to `connection` and the
`sftp` keys a file written before Decision-012 still carries.

```yaml
location: local
testinProject: NAFATH
```

---

## Versioning

**A reader ignores what it does not know.** Every JSON type carries
`@JsonIgnoreProperties(ignoreUnknown = true)` and the YAML reader logs an
unknown key and skips it. Adding a field is therefore always safe: an older
build reading a newer file drops the field, and writes the file back without it.
That is the trade — forward compatibility is a *read*, not a round trip.

**A file holds only what differs from a fresh one** (Rule-INTERNAL-135). Every
stored type carries `@JsonInclude(NON_DEFAULT)`, so a value equal to what a
newly made test case, run item or marker holds is left out, and a missing key
reads back as exactly that value: an empty field, a default bug severity, a
execution's empty failure detail. `createdAt` is the one exception and
is always written, because a fresh object holds the moment it was made. The
price is that **changing a field's default is a format change**: every file
that left the key out would silently take the new default.

**A renamed field is not read under its old name.** No `@JsonAlias`, no
converter: a rename moves the format number on, and a project in an older
format is refused (Rule-INTERNAL-091). Format 3 renamed three keys so each says
the business word: a marker's `modifiedBy` and `modifiedAt` became `updatedBy`
and `updatedAt`, as a test case and every screen say, and a test case's `group`
became `groups`, because it holds a list. The two fixed folders' markers became
`.tcf` and `.trf` - Test Cases Folder and Test Runs Folder - instead of `.tcd`
and `.trd`.

Format 4 keeps every execution of a run item: the top-level `status`,
`duration`, `executedBy`, `executedAt`, `actualResult`, `stacktrace` and
`screenshots` moved into `executions`, and the run item gained `untested`,
`clock` and `unrecordedActualResult` (#415).

**The project says which format its files are in**, in its `.tp`: `format`, and
4 is the format this page describes. A project without the number, or with a
lower one, is not read at all: it is shown refused, and the tester removes it
and creates it again, because nothing converts it ([UC-INTERNAL-002](internal/readTestProject.md), Rule-INTERNAL-091).
A project
with a **higher** number is not read at all: a format this build does not know is
refused rather than guessed at, because reading it as format 4 would delete what
this build cannot see.

**A file whose shape changed structurally is not read at all.** A test run
written before 3 September 2026 kept its run items in `<folder>.json`, and one
written before 20 September 2026 kept them all in a `run.json`. Neither is read:
such a test run shows no run items, and the conversion removed the test run rather
than carrying it forward. That is a decision, not an oversight, and
`FormatConversionIdeTest.testAnOldProjectBecomesThisFormat` asserts it so it
cannot be mistaken for one.

**An unknown enum constant fails that file's read.** It is not defaulted,
because a status quietly becoming `PENDING` loses a tester's decision without
saying so. This is the one incompatibility that needs a conversion story before
it ships. [#91 and #92](https://github.com/MutebAlMughyiri/test-in/issues/91) exist
because a format changed and the rule for old data had to be decided after the
fact. That is the thing this page is here to prevent.

**Old data gets wiped, not migrated.** When a change cannot be made compatible,
the answer is to delete the old data and start again rather than to ship a
converter. The trees are small, and a converter is a second reader of a format
nobody writes anymore.

**The one exception was the conversion to format 2**, which 2.13.0-alpha carried
because the plugin is public. Test cases and markers were brought forward, and
only the test runs were removed (#305, D4). The conversion code was deleted in
2.14.0-alpha. Formats 3 and 4 have no conversion at all: a project in an older
format is refused and created again.
