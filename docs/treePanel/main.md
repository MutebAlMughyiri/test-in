[Documentation](../README.md) › The tree panel

# The tree panel

The tree panel is the tree on the left of the IDE. It holds every test project,
test set and test run. Everything in Testin starts here.

|                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The tree panel                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Answers**         | What the panel is for, what a tester can do in it, exactly what happens step by step, and what every screen looks like                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Numbering**       | Use cases are `UC-TREE-PANEL-001` to `UC-TREE-PANEL-029`, apart from 024, which went to [UC-INTERNAL-001](../internal/globalSearch.md) with the search. Rules are `Rule-TREE-PANEL-001` to `Rule-TREE-PANEL-142`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Retired**         | `Rule-TREE-PANEL-018` and `Rule-TREE-PANEL-019` said cloning needed the code project to name the test project first, and named the folder by `testin.yml`, never by the address; read Rule-TREE-PANEL-107 instead. `Rule-TREE-PANEL-020` and `Rule-TREE-PANEL-021` said the choice was written into the code project, and said so when it could not be; read Rule-TREE-PANEL-106 instead. `Rule-TREE-PANEL-084` showed the branch box only when `testin.yml` said the project was shared through Git; read Rule-TREE-PANEL-108 instead. All five retired 19 September 2026, when Testin stopped writing `testin.yml` and stopped needing it (#301). `Rule-TREE-PANEL-035` said the test project and the two containers could not be renamed from the tree; retired the same day, when a test project became renamable (#331) - the containers are Rule-TREE-PANEL-002's. `Rule-TREE-PANEL-109` said a test project shared through an SFTP server keeps its name; written for #331 and retired unused the same day, before it shipped, when the SFTP sync was removed (#334). `Rule-TREE-PANEL-089` said only **Select Test Project** and **New Test Project** are ever gray on the toolbar; retired the same day, when **Save to testin.yml** became a third that can be (#335) - read Rule-TREE-PANEL-115 instead. `Rule-TREE-PANEL-075` said an edit of a test run could be undone as one step; retired 22 September 2026, when saving an edit became final - read Rule-TREE-PANEL-060 instead. The numbers are not given to anything else |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule, key, label and message read from the code. On 14 September 2026, at `e6277ddf`, the messages, names and keys of [UC-TREE-PANEL-004](chooseTestProject.md) were read from the code again. On 18 September 2026, at `d427cde7`, the pages the #328 work changed were read against the code again. On 20 September 2026, at `0b00da21`, [UC-TREE-PANEL-001](reachTheTree.md) was read against the code again for the six panel screens and the two new rules (#301, pieces 2 and 3).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |

---

## The use cases

Each one is a page of its own: the story, its rules, its screens and its steps.

|                       | What the tester does                                                     | Why a tester would use it                                                            |
|-----------------------|--------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
|                       | **Getting to the tree**                                                  |                                                                                      |
| **UC-TREE-PANEL-001** | [Open the panel and reach the tree](reachTheTree.md)                     | See the test project, or learn what to set up first.                                 |
| **UC-TREE-PANEL-002** | [Create a test project](createTestProject.md)                            | Give a new product under test a home.                                                |
| **UC-TREE-PANEL-003** | [Import a test project that already exists](importTestProject.md)        | Copy a test project from Git onto this machine.                                      |
| **UC-TREE-PANEL-004** | [Choose which test project this code project uses](chooseTestProject.md) | Point this code project at a different test project.                                 |
|                       | **Nodes**                                                                |                                                                                      |
| **UC-TREE-PANEL-005** | [Open a test set](openTestSet.md)                                        | Read and write the test cases inside a test set.                                     |
| **UC-TREE-PANEL-006** | [Open a test run](openTestRun.md)                                        | Record a run item status for each test case in a test run.                           |
| **UC-TREE-PANEL-007** | [Create a test set](createTestSet.md)                                    | Make a place for the test cases about to be written.                                 |
| **UC-TREE-PANEL-008** | [Create a test set package](createTestSetPackage.md)                     | Group test sets so a big tree stays readable.                                        |
| **UC-TREE-PANEL-009** | [Create a test run](createTestRun.md)                                    | Record one round of testing over chosen test cases.                                  |
| **UC-TREE-PANEL-010** | [Create a test run package](createTestRunPackage.md)                     | Group test runs so a year of cycles stays tidy.                                      |
| **UC-TREE-PANEL-011** | [Rename a node](renameNode.md)                                           | Give a node the name it is called now.                                               |
| **UC-TREE-PANEL-012** | [Remove a node](removeNode.md)                                           | Take out work that is no longer needed.                                              |
|                       | **Arranging the tree**                                                   |                                                                                      |
| **UC-TREE-PANEL-013** | [Move nodes](moveNodes.md)                                               | Put a node into a different folder.                                                  |
| **UC-TREE-PANEL-014** | [Copy nodes](copyNodes.md)                                               | Start from work that already exists.                                                 |
| **UC-TREE-PANEL-015** | [Order nodes among their siblings](orderNodes.md)                        | Fix where a node sits among its siblings.                                            |
| **UC-TREE-PANEL-016** | [Undo a change to the tree](undoChange.md)                               | Take back a wrong move, rename or removal.                                           |
| **UC-TREE-PANEL-017** | [Redo a change to the tree](redoChange.md)                               | Put back a change that was just undone.                                              |
|                       | **Statuses**                                                             |                                                                                      |
| **UC-TREE-PANEL-018** | [Retire a test project, a test set or a package](retireNode.md)          | Keep old work for its history, out of the way.                                       |
| **UC-TREE-PANEL-019** | [Bring a retired node back](reactivateNode.md)                           | Pick up work that was put aside.                                                     |
| **UC-TREE-PANEL-020** | [Set a test run's status](setTestRunStatus.md)                           | Show how far a test run has got.                                                     |
| **UC-TREE-PANEL-021** | [Re-create a test run](reCreateTestRun.md)                               | Start the next cycle from a finished test run.                                       |
| **UC-TREE-PANEL-022** | [Edit a test run](editTestRun.md)                                        | Correct a test run without building it again.                                        |
|                       | **Working from the tree**                                                |                                                                                      |
| **UC-TREE-PANEL-023** | [Run the automation for everything a node holds](runTests.md)            | Run a whole area of automation in one step.                                          |
| **UC-TREE-PANEL-025** | [Refresh the tree from disk](refreshTree.md)                             | See changes that happened outside the IDE.                                           |
| **UC-TREE-PANEL-026** | [Switch the Git branch of the test project](switchBranch.md)             | Follow the Git branch under test.                                                    |
| **UC-TREE-PANEL-027** | [See what a node holds](nodeDetails.md)                                  | See counts, dates and results without opening anything.                              |
| **UC-TREE-PANEL-028** | [Use the buttons at the top of the panel](panelToolbar.md)               | Reach the things that act on the whole panel.                                        |
| **UC-TREE-PANEL-029** | [Save the test project to testin.yml](saveTestinYml.md)                  | Turn the automation code on, and let a colleague's clone find the same test project. |

---

## Where the panel sits in the IDE

Testin puts two tool windows in IntelliJ IDEA. The tree panel is docked on
the left, beside the places the IDE keeps its own trees, and its stripe reads **Testin Tree**. The view panel is docked
on the right, and its stripe reads **Testin View**. Both keep the plugin's name, so neither reads as a different
plugin, and each says which of the two it is.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin  -  IntelliJ IDEA                                       - □ ×      │
├──────────────────────────────────┬──────────────────────┬──────────────────┤
│  Testin                     (1)  │                 (2)  │             (3)  │
│                                  │                      │                  │
│  [ main                 v ]      │                      │                  │
│                                  │                      │                  │
│  v Demo                          │                      │                  │
│    v Test Cases                  │                      │                  │
│      v Accounts                  │                      │                  │
│          Login                   │                      │                  │
│          Registration            │    Editor panel      │    View panel    │
│        Checkout                  │                      │                  │
│        Legacy sign-in            │                      │                  │
│    v Test Runs                   │                      │                  │
│      v Sprint 7                  │                      │                  │
│          + cycle-1  Created      │                      │                  │
│          @ cycle-2  In Progress  │                      │                  │
│          * cycle-3  Completed    │                      │                  │
└──────────────────────────────────┴──────────────────────┴──────────────────┘
```

1. **The tree panel** — everything on this page is about it. It is drawn wide
   here so the tree can be read. In the IDE the tester sets its width.
2. **Editor panel** — where a test set or a test run opens when the tester opens
   one from the tree. The tree panel itself never shows a test case.
3. **View panel** — details and history, bugs included, for the test case the tester
   has selected. It is a part of Testin of its own, and its pages are
   [here](../viewPanel/main.md).

---

## What the panel is for

A tester's work is arranged like a tree:

- a test project holds test sets
- a test set holds test cases
- a test run records one pass through some of those test cases

The panel is that tree. It sits on the left, where the IDE keeps every other
tree. It is always one click away.

**Four words, before the rules use them.**

- A **node** is anything the tree shows: a test project, a folder, a test set or
  a test run. A test case is not a node, because the tree does not show one.
- The **Testin folder** is the one folder that holds every test project. The
  settings page and Testin's own messages call it that too.
- **Bound** means this code project is set to use one test project. The
  choice is kept on this machine; a `testin.yml` in the code project can name
  one for everyone, and **Save to testin.yml** writes it there (Rule-TREE-PANEL-106, Rule-TREE-PANEL-112).
- **Signed off** means a test run is **Completed**, **Closed** or **Committed**. Its test cases and settings can no
  longer change, though the
  tree can still remove it. Its run item statuses can, until it is **Committed**: the last status, which only Testin
  sets when the test run is
  committed (Rule-TREE-PANEL-135).
- A **container** is **Test Cases** or **Test Runs**. There are always exactly
  two, and they come with the test project.
- A **run item status** is what a tester records against one test case in one
  test run: **Passed**, **Failed** or **Blocked**. **Pending** means the test
  run has not reached that test case yet. **Untested** means it never will,
  because the test run was signed off first.

The panel's job is small and strict. It shows one test project, the one this
code project is bound to. It lets the tester shape that test project: create,
name, group, order, retire and remove the things in it.

Reading and executing test cases happens in editors. The panel opens those
editors. The panel itself never shows a test case.

---

## How to read these pages

Each use case page has the same shape. **Rules** are the things that must always
be true. Then comes the screen, drawn with its parts numbered. A use case that
opens no screen of its own has **What the tester sees** instead. **Main flow**
is what happens, step by step, when nothing goes wrong. **What Testin refuses**
is every way it can go wrong, and what the tester sees each time.

Words like *\<name\>* stand for whatever the tester is working on. Testin puts
the real name there.

**In every drawing:**

Each screen is drawn with text, so it can live beside the words that explain
it. In every drawing:

- `v` is the little triangle that opens and closes a row
- `+` `@` `*` are the status icons a test run shows
- `[set]`, `[dir]`, `[edit]`, `[find]` and the like stand for icons
- `(1)` `(2)` `(3)` point at the numbered notes under the drawing

---

## Every key the tree answers to

A key works only while the tester is clicked into the tree. The search key is
the exception. It works anywhere in the IDE. A dialog's own keys are drawn on
its status bar, at the foot of the dialog.

| Key                                   | Does                                                   | Page                                           |
|---------------------------------------|--------------------------------------------------------|------------------------------------------------|
| `Enter`                               | Opens the selected test set or test run                | [UC-TREE-PANEL-005](openTestSet.md)            |
| `Context Menu`                        | Opens the menu on the selected node, without the mouse | [The menu](#the-menu)                          |
| *no key*                              | Creating a node is a right-click entry, **Create**     | [UC-TREE-PANEL-007](createTestSet.md)          |
| `Shift+F6`                            | Renames the selected node                              | [UC-TREE-PANEL-011](renameNode.md)             |
| `Delete`                              | Removes the selected nodes                             | [UC-TREE-PANEL-012](removeNode.md)             |
| `Ctrl+X`                              | Cuts the selected nodes, to move them                  | [UC-TREE-PANEL-013](moveNodes.md)              |
| `Ctrl+C`                              | Copies the selected nodes                              | [UC-TREE-PANEL-014](copyNodes.md)              |
| `Ctrl+V`                              | Pastes into the selected node                          | [UC-TREE-PANEL-013](moveNodes.md)              |
| `Escape`                              | Drops nodes cut or copied, and takes the gray off      | [UC-TREE-PANEL-013](moveNodes.md)              |
| `Ctrl+Z`                              | Undoes the last tree change                            | [UC-TREE-PANEL-016](undoChange.md)             |
| `Ctrl+Y`                              | Redoes it                                              | [UC-TREE-PANEL-017](redoChange.md)             |
| `1` `2` `3`                           | Inside the status popup: Assigned, Completed, Closed   | [UC-TREE-PANEL-020](setTestRunStatus.md)       |
| `Ctrl+Alt+F`, `Cmd+Alt+F` on a Mac    | Opens search, from anywhere in the IDE                 | [UC-INTERNAL-001](../internal/globalSearch.md) |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac       | Generates a report on the selected test run            | [UC-REPORT-001](../report/generateReport.md)   |
| The menu key, beside the right `Ctrl` | Opens the node menu                                    | Below, under **What the tree shows**           |

Six things a tester might expect have **no key** on the tree. **Order**, the
statuses, **Re-create**, **Edit Test Run**, **Run Tests** and **Details** are
menu items only. Each one is a decision the tester thinks about, not a reflex, and
none is used often enough to need a key.

The panel's own eight buttons have no keys either, except search. They are
[UC-TREE-PANEL-028](panelToolbar.md). `F2` renames in the editors, not here.

---

## Every message and the use case it belongs to

**This is the exit criteria.** A tester knows an action finished by the message
Testin shows, and learns from the refusal that it did not. Every message the
panel can
show is here, quoted as the plugin writes it. A word in angle brackets is
filled in with whatever the tester is working on, and *N* with a count.

### When it worked

A success message fades on the status bar and leaves no trace. It is one word
in the past tense wherever one word will do. (Rule-TREE-PANEL-007)

| Message                                        | Means                                                                                                                | Use case                                                                                                                                                         |
|------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *Created*                                      | A node was created: a test project, a test set, a package or a test run                                              | [UC-TREE-PANEL-002](createTestProject.md), [UC-TREE-PANEL-007](createTestSet.md), [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-021](reCreateTestRun.md) |
| *Cloned*                                       | A test project was brought down from Git                                                                             | [UC-TREE-PANEL-003](importTestProject.md)                                                                                                                        |
| *Bound*                                        | This code project now uses the test project that was chosen                                                          | [UC-TREE-PANEL-004](chooseTestProject.md)                                                                                                                        |
| *Renamed*                                      | A node was renamed                                                                                                   | [UC-TREE-PANEL-011](renameNode.md)                                                                                                                               |
| *Removed*, *Removed N*                         | One or more nodes went to the recycle bin                                                                            | [UC-TREE-PANEL-012](removeNode.md)                                                                                                                               |
| *Copied*, *Copied N*                           | Nodes are on the clipboard, to copy                                                                                  | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                |
| *Cut*, *Cut N*                                 | Nodes are on the clipboard, to move                                                                                  | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                |
| *Moved*, *Moved N*                             | The cut nodes have moved                                                                                             | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                |
| *Pasted*, *Pasted N*                           | The copied nodes have been duplicated                                                                                | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                |
| *Ordered*                                      | A node's number was set or cleared                                                                                   | [UC-TREE-PANEL-015](orderNodes.md)                                                                                                                               |
| *Undone*                                       | The last change was taken back                                                                                       | [UC-TREE-PANEL-016](undoChange.md)                                                                                                                               |
| *Redone*                                       | The undone change was put back                                                                                       | [UC-TREE-PANEL-016](undoChange.md)                                                                                                                               |
| *Active*, *Inactive*, *Archived*, *Deprecated* | The new status of a test project (Active, Inactive), a package (Active, Archived) or a test set (Active, Deprecated) | [UC-TREE-PANEL-018](retireNode.md)                                                                                                                               |
| *Assigned*, *Completed*, *Closed*              | The new status of a test run                                                                                         | [UC-TREE-PANEL-020](setTestRunStatus.md)                                                                                                                         |
| *Updated*                                      | A test run's name, test cases or settings were saved                                                                 | [UC-TREE-PANEL-022](editTestRun.md)                                                                                                                              |
| *Running*, *Running N*                         | The automation started                                                                                               | [UC-TREE-PANEL-023](runTests.md)                                                                                                                                 |
| *Refreshed*                                    | The tree was read again from disk                                                                                    | [UC-TREE-PANEL-025](refreshTree.md)                                                                                                                              |
| *Switched to \<branch\>*                       | The test project is on another branch                                                                                | [UC-TREE-PANEL-026](switchBranch.md)                                                                                                                             |

### When it refused

A refusal fades too. It says what stopped the action, and nothing was changed.

| Message                                                                              | Means                                                                        | Use case                                                                                                                                                                                                                                  |
|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *\<name\> Already Exists*                                                            | The name is taken where it was going                                         | [UC-TREE-PANEL-002](createTestProject.md), [UC-TREE-PANEL-007](createTestSet.md), [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-011](renameNode.md), [UC-TREE-PANEL-021](reCreateTestRun.md), [UC-TREE-PANEL-022](editTestRun.md) |
| *Select a folder*                                                                    | The selected place cannot take what is on the clipboard                      | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                                                                                         |
| *'\<name\>' already exists in '\<folder\>'*, *N nodes already exist in '\<folder\>'* | The destination already holds that name. The rest of the paste still moves   | [UC-TREE-PANEL-013](moveNodes.md)                                                                                                                                                                                                         |
| *A test run needs a name*                                                            | The name box was emptied                                                     | [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-021](reCreateTestRun.md), [UC-TREE-PANEL-022](editTestRun.md)                                                                                                                       |
| *'\<parent\>' no longer exists - test run not created*                               | The folder went away while the dialog was open                               | [UC-TREE-PANEL-009](createTestRun.md)                                                                                                                                                                                                     |
| *'\<test run\>' no longer exists - nothing saved*                                    | The test run went away while the dialog was open                             | [UC-TREE-PANEL-022](editTestRun.md)                                                                                                                                                                                                       |
| *'\<test run\>' was \<status\> while this was open - nothing saved*                  | Someone signed the test run off while the dialog was open                    | [UC-TREE-PANEL-022](editTestRun.md)                                                                                                                                                                                                       |
| *\<name\> has no test cases to run*                                                  | Nothing under the node can be run                                            | [UC-TREE-PANEL-023](runTests.md)                                                                                                                                                                                                          |
| *No Test Projects*                                                                   | There is nothing to choose. It adds *Create one in the Testin folder first*  | [UC-TREE-PANEL-004](chooseTestProject.md)                                                                                                                                                                                                 |
| *Java Test Source Not Found*                                                         | The IDE project has no Java test folder, so no automation code is written    | [UC-TREE-PANEL-007](createTestSet.md)                                                                                                                                                                                                     |
| *'\<name\>' cannot name a Java package*                                              | The name is a word Java keeps for itself, on a folder that becomes a package | [UC-TREE-PANEL-002](createTestProject.md), [UC-TREE-PANEL-008](createTestSetPackage.md), [UC-TREE-PANEL-011](renameNode.md)                                                                                                               |

### When something failed

These stay in the IDE's notification log, because they are real failures rather
than feedback on what the tester just typed.

| Message                                 | Means                                                                  | Use case                                  |
|-----------------------------------------|------------------------------------------------------------------------|-------------------------------------------|
| *Clone Failed*                          | The repository could not be cloned. The reason follows                 | [UC-TREE-PANEL-003](importTestProject.md) |
| *Clone Error*                           | Something needed for the clone was missing                             | [UC-TREE-PANEL-003](importTestProject.md) |
| *Rename Failed*                         | The folder could not be renamed on disk. The reason follows            | [UC-TREE-PANEL-011](renameNode.md)        |
| *Undo Incomplete*                       | Some removed nodes could not be put back. It says how many of how many | [UC-TREE-PANEL-016](undoChange.md)        |
| *Unable to update status to \<status\>* | A test project's status could not be written                           | [UC-TREE-PANEL-018](retireNode.md)        |
| *Unable to mark test set \<status\>*    | A test set's status could not be written                               | [UC-TREE-PANEL-018](retireNode.md)        |
| *Unable to mark package \<status\>*     | A package's status could not be written                                | [UC-TREE-PANEL-018](retireNode.md)        |
| *Git Error*                             | The branches could not be read. The reason follows                     | [UC-TREE-PANEL-026](switchBranch.md)      |
| *Git Fetch Warning*                     | The branch list may be out of date                                     | [UC-TREE-PANEL-026](switchBranch.md)      |

### Shown in the tree itself, not as a message

| What the tester sees                                                    | Means                                                                                                           |
|-------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| *Could not load '\<folder name\>'*, in red, as a child row              | That folder's contents could not be read                                                                        |
| *testin.yml names \<name\>, which is not in the Testin folder*, in red  | The code project names a test project the Testin folder does not hold. See [UC-TREE-PANEL-001](reachTheTree.md) |
| *testin.yml names \<name\>, which could not be read*, in red            | The test project's folder is there, but Testin could not read it                                                |
| *\<name\>, chosen on this machine, is not in the Testin folder*, in red | The test project chosen for this code project on this machine is gone from the Testin folder                    |
| *\<name\>, chosen on this machine, could not be read*, in red           | The chosen test project's folder is there, but Testin could not read it                                         |

---

## The panel

The panel is the Testin window that sits on the left of the IDE. The buttons
along its top are the IDE's own. The branch box and the tree are Testin's.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin              [1] [2] [3] [4] [5] [6] [7] [8]                       │
├────────────────────────────────────────────────────────────────────────────┤
│  [ main            v ]                                             (9)     │
├────────────────────────────────────────────────────────────────────────────┤
│  v Demo                                                           (10)     │
│    v Test Cases                                                            │
│      v Accounts                                                   (11)     │
│          Login                                                             │
│          Registration                                                      │
│        Checkout                                                            │
│        Legacy sign-in                            (deprecated: gray) (13)   │
│    v Test Runs                                                             │
│      v Sprint 7                                                            │
│          + cycle-1  Created                                       (12)     │
│          @ cycle-2  In Progress                                            │
│          * cycle-3  Completed                                              │
│        2025                                        (archived: gray)        │
└────────────────────────────────────────────────────────────────────────────┘
```

Parts 1 to 8 are the toolbar's eight buttons, left to right, and all of them
are [UC-TREE-PANEL-028](panelToolbar.md).

1. **Search Test Project**
2. **Settings**
3. **Expand All**
4. **Collapse All**
5. **Refresh**
6. **Select Test Project**
7. **New Test Project**
8. **Save to testin.yml**
9. **The branch box** — shown only for a test project shared through Git. Its
   placeholder reads *Loading branches...* until Git answers.
10. **The test project row** — drawn bold, with the IDE's project icon. The two
    folders under it are bold too, and there are always exactly two.
11. **A package** — a folder icon. **A test set** — the icon the IDE uses for a
    changelist. Neither is bold.
12. **A test run** — draws its **status** as its icon. The status word follows
    the name in gray. So the tester reads the status of every test run without
    opening one. A test run never draws a kind icon.
13. **Gray text** — means retired, or cut and not yet pasted. Both look the
    same on purpose. Both mean the same thing to a tester: not part of the work
    in front of them.

Retired nodes sit at the bottom of their folder. Their number does not change
that.

## What the tree shows

**A row.** The test project is the top row, drawn bold, with the IDE's project
icon. Under it sit exactly two bold rows, **Test Cases** and **Test Runs**, each
with a bookmark icon. A package shows a folder icon. A test set shows the icon
the IDE uses for a changelist. A test run shows the icon of its status instead
of an icon for what it is, and after its name comes its status word in gray:
*Created*, *In Progress*, *Assigned*, *Completed* or *Closed*.

**Gray text.** A **Deprecated** test set, an **Archived** package, or a node cut
and not yet pasted, is drawn in gray. A retired node sits after every live
sibling. (Rule-TREE-PANEL-008)

**The order of siblings.** Live nodes come first, and retired ones last. Inside
each of those two groups, nodes with a number come first, smallest first. Then
come the nodes without a number, by the date they were created. Then by name. (Rule-TREE-PANEL-010)

**A test project that is not Active** shows no children at all. (Rule-TREE-PANEL-063)

**Right-click.** Right-clicking a row that is not selected makes it the only
selection, and opens the menu at the pointer. Right-clicking inside a
multi-selection keeps the selection. Right-clicking below the last row, or on
a row that could not be read, opens no menu at all.

**What the menu does with several rows selected.** Three different things, and
two of them are quiet:

- **Open**, **Remove**, **Copy** and **Cut** act on all of them.
- **Order**, **Paste**, **Create** and **Details** stay black and act on the
  first row alone, saying nothing about the rest.
- **Rename**, **Re-create**, **Edit Test Run**, **Set Status**, **Export**, **Import** and every status entry go gray
  instead.

**The menu key**, the one beside the right `Ctrl`, opens the node menu over the
selected row.

**A folder that cannot be read** shows one child row with the error icon,
reading, in red, *Could not load '\<folder name\>'*.

**What stays open.** After a paste, a rename or a refresh of the same test
project, the rows that were expanded stay expanded, and the selection stays. The
first time a different test project is shown, the whole tree is expanded, except
retired branches.

---

## The menu

The tester right-clicks any row, or presses the menu key beside the right
`Ctrl`. The menu opens. Which items are gray depends on the row, and a gray item
says why when the pointer rests on it (Rule-TREE-PANEL-104). Which items appear
at all depends on which plugins are installed.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Open                     Enter  (1)   ┌ Actions ───────────────────┐      │
│  Create                                │  Activate                  │      │
│  ──────────────────────────────        │  Deactivate                │      │
│  Actions                     >   (2)   │  Archive                   │      │
│  ──────────────────────────────        │  Undo Move 'Login'  Ctrl+Z │      │
│  Run Tests                       (3)   │  Redo               Ctrl+Y │      │
│  ──────────────────────────────        │  Re-create                 │      │
│  Export                          (4)   │  Remove             Delete │      │
│  Import                                │  Rename           Shift+F6 │      │
│  ──────────────────────────────        │  Order                     │      │
│  Sync with Remote                (5)   │  Copy               Ctrl+C │      │
│  View Pending Commits                  │  Cut                Ctrl+X │      │
│  ──────────────────────────────        │  Paste              Ctrl+V │      │
│  Edit Run                        (6)   └────────────────────────────┘      │
│  Set Status                                                                │
│  ──────────────────────────────                                            │
│  Generate Report          Ctrl+P (7)                                       │
│  Details                                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Open** and **Create** — first, because they are the two things done most.
   A dividing line follows them.
2. **Actions** — a submenu. It holds everything that changes a node in place:
    - its status
    - undo and redo
    - re-create
    - remove
    - rename
    - order
    - the clipboard

   The submenu is one unbroken list. Status entries appear only for the kind
   selected, and the other kinds' entries are not there at all. The three rows
   shown are a test project's.
3. **Run Tests** — only when the TestNG plugin is present.
4. **Export**, **Import** — these belong to reports, export, import and sync.
5. **Sync with Remote**, **View Pending Commits** — only when the Git plugin is
   present. Otherwise, the whole section disappears, dividing line included.
6. **Edit Test Run** and **Set Status** — a test run's own two entries. Grayed
   once the test run is signed off.
7. **Generate Report** and **Details** — last.

Every entry that changes something confirms itself once. The confirmation is
one past-tense word. Every entry that only shows something confirms nothing.

---

## Also on this menu

Five items on the tree's menu belong to another part of Testin. They are
documented there:

| Item                                           | Belongs to                       |
|------------------------------------------------|----------------------------------|
| **Export**, **Import**                         | Reports, export, import and sync |
| **Generate Report** (`Ctrl+P`)                 | The same                         |
| **Sync with Remote**, **View Pending Commits** | The same, the Git half           |

---

## Why the panel is built this way

### The tree shows one test project, not a list of them

The panel shows the one test project this code project is set to use, and no
other. With a list of test projects in the tree, three things could disagree
about which one is open:

- the tree
- the reports
- the test runs

The choice is kept on this machine. A code project that wants every colleague
on the same test project with no setup says so in its `testin.yml`, which
Testin writes only when the tester presses **Save to testin.yml**
(Rule-TREE-PANEL-106, Rule-TREE-PANEL-112).

### A test run shows its status instead of a plain icon

Every other row shows what it *is*. A test run shows how far it has *got*: its
status icon, and the status word. That is the one thing a tester scanning the
tree wants to know about a test run. Without it, they would open each one to
find out.

### Retired and cut look the same

Both are gray. These three all mean "not part of current work right now":

- a **Deprecated** test set
- an **Archived** package
- a node cut but not yet pasted

One color for one meaning is easier to learn than three.

### Every removal, move and copy asks first

The confirmation names what will happen, as in *Remove 'Login'?* or *Move
'Login' into 'Smoke'?*. It shows where from and where to. The key that
confirms is named for the verb. Nothing in the tree changes until the tester presses
`Enter`.

### The menu is short at the top and deep in Actions

The top level holds what is done often: open and create. It also holds what
belongs to other parts of Testin. Everything that changes a node where it sits
is one level down, under **Actions**. So the first thing the tester sees is
short, and anything that removes or moves something takes a second, deliberate
step.

### Order is a dialog, not a drag

Dropping between rows to reorder is not offered. A number is better in three
ways:

- it is written down
- it stays right when Testin reads the folder again
- it is the same number the automation uses to decide the running order

Dragging would be quicker, but it would leave nothing written down.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is a real gap, found by reading the code this
documentation describes. None of them has a bug report yet.

|                   | The rule it breaks                                              | What a tester sees                                                                                                                                                                                                                                                        |
|-------------------|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 1**  | Rule-TREE-PANEL-007 — one past-tense word                       | Fixed. Creating a test run or a test project says *Created*, and cloning a test project says *Cloned*: one past-tense word, as every other creation says.                                                                                                                 |
| **Difference 2**  | Rule-TREE-PANEL-044 — nothing lands in a test run               | Fixed. **Paste** asks the same two questions a drop asks — is this a place things land in, and can anything on the clipboard land here — so it is gray on a test run, as it always was on a test set. The *Select a folder* refusal is no longer reachable from the menu. |
| **Difference 11** | Rule-TREE-PANEL-006 — nothing changes until the tester confirms | Fixed. All five ask `singleSelectedDirectory`, the question eight other tree entries already asked, so each is gray with more than one row selected instead of acting on the first. **Details** had no `update()` at all and now has one.                                 |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone              | Was                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 3**  | A signed-off test run could still be renamed, moved, reordered and removed from the tree. Fixed 7 September 2026, [#184](https://github.com/MutebAlMughyiri/test-in/issues/184)                                                                                                                                                                                                                                                         |
| **Difference 7**  | Three of the four ways to bind a test project reported success without checking the write. Fixed 7 September 2026, [#188](https://github.com/MutebAlMughyiri/test-in/issues/188)                                                                                                                                                                                                                                                        |
| **Difference 8**  | **New Test Project** stayed gray for the rest of the session once it had been drawn without a Testin folder. Fixed 7 September 2026, [#189](https://github.com/MutebAlMughyiri/test-in/issues/189)                                                                                                                                                                                                                                      |
| **Difference 15** | A removal whose copy could not be kept aside was not undoable, said nothing, and left CTRL+Z pointing at an unrelated change. Fixed 7 September 2026, [#196](https://github.com/MutebAlMughyiri/test-in/issues/196)                                                                                                                                                                                                                     |
| **Difference 9**  | An edit deleted the run item statuses of test cases removed from their test set. Fixed 7 September 2026, [#190](https://github.com/MutebAlMughyiri/test-in/issues/190)                                                                                                                                                                                                                                                                  |
| **Difference 4**  | Neither create dialog named its two kinds, so both rows showed only their hints. Fixed 8 September 2026, [#185](https://github.com/MutebAlMughyiri/test-in/issues/185)                                                                                                                                                                                                                                                                  |
| **Difference 6**  | Neither canceling a cut nor pasting it emptied the clipboard, so the same move was offered again. Fixed 8 September 2026, [#187](https://github.com/MutebAlMughyiri/test-in/issues/187)                                                                                                                                                                                                                                                 |
| **Difference 12** | A position too large to hold silently cleared the order and still said *Ordered*. Fixed 7 September 2026 in `b28e9af7`, and the document did not follow                                                                                                                                                                                                                                                                                 |
| **Difference 13** | The **Select Test Project** dialog's first column had no heading. Fixed 8 September 2026, [#194](https://github.com/MutebAlMughyiri/test-in/issues/194)                                                                                                                                                                                                                                                                                 |
| **Difference 14** | The **Uncommitted Changes** dialog read *1 change ... are not committed*. Fixed 9 September 2026, [#195](https://github.com/MutebAlMughyiri/test-in/issues/195)                                                                                                                                                                                                                                                                         |
| **Difference 5**  | The popup offered all five statuses, including the two that are the test run's own record of itself, and let a test run go from **Assigned** back to **Created**. It offers what the test run can be moved to and nothing else: the three a tester sets, and only those ahead of where it is now (Rule-TREE-PANEL-092). That settles question 1. Fixed 10 September 2026, [#186](https://github.com/MutebAlMughyiri/test-in/issues/186) |
| **Difference 10** | Setting a test run's status from the tree did not tell that test run's open editor, which kept the old status and the rows that had just become **Untested**. The tree was doing the work itself instead of asking the one place that owns a status change; now both go through it, and the editor and the tree each hear about the other. Fixed 9 September 2026, [#191](https://github.com/MutebAlMughyiri/test-in/issues/191)        |

---

## Not decided

|                | Question                                                                                                                   | Why it is open                                                                                                                                                                                                    |
|----------------|----------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Question 3** | What should the paste refusal say?                                                                                         | *Select a folder* is shown for six different reasons. Four of them are: the wrong side of the tree, the node itself, another test project, and a test run. Choosing a different folder only fixes one of the six. |
| **Question 4** | On a Mac, should cut, paste, undo and redo in the tree use the Mac's own key for those actions, the way copy already does? | Today copy does. The other four do not. The keys are named in the key table above.                                                                                                                                |

**Settled.** Question 1 asked whether a test run may go backwards, so that **Created** follows **Assigned** or **In
Progress**. It may not:
the popup offers only the statuses ahead of where the test run is now, so there
is no way to ask (Rule-TREE-PANEL-092, [#186](https://github.com/MutebAlMughyiri/test-in/issues/186)).

Question 2 asked whether a signed-off test run should be locked in the tree as
well as in its editor. It is. A **Completed** or **Closed** test run cannot be
renamed, moved, reordered or removed from the tree ([#184](https://github.com/MutebAlMughyiri/test-in/issues/184)).
The same question for the whole product is [question 1 in the product's own
document](../product.md#9-undecided).

---

[Documentation](../README.md) › **The tree panel**
