[Documentation](README.md) › The product

# The product — business requirements

> Testin is a test case manager that lives inside the IDE instead of inside a
> browser tab. A tester can execute a whole test run without touching the
> mouse. That is the product, not a feature of it.

|                           |                                                                                                                                                      |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**        | All of it. These rules hold everywhere. Each part also has a document of its own, listed on [the home page](README.md)                               |
| **What the numbers mean** | These rules belong to no single part of Testin, so `PRODUCT` is their part. Nine rules are still missing their text, which is explained in section 7 |
| **Numbering**             | Rules are `Rule-PRODUCT-001` to `Rule-PRODUCT-024`. This document names no use cases of its own; every one belongs to a part                         |
| **Answers**               | Who uses Testin, what they work with, every status, the rules for the whole product, and what is undecided                                           |
| **State**                 | **Draft 1** — moved from Notion, never re-checked, and nothing owns correcting it. Read the eight parts for what is current                          |
| **Checked against**       | `main` at `0becc8b2`, 29 August 2026                                                                                                                 |
| **Written to**            | Written before [the standard](standard.md). It names keys and has no user stories                                                                    |

Every rule in the main body was true of the product on that commit. Where a rule
is genuinely undecided it is listed as undecided rather than invented.

---

> ### The state of this draft
>
> Moved here from Notion on 6 September 2026. Its facts are unchanged. Only the
> wording was made plain, to [the standard](standard.md). **It has not been
> re-checked against the product since it was written.** By 20 September 2026
> `main` had moved **866 commits**: 1,028 files, 90,147 insertions, 19,873
> deletions.
>
> What is known to have changed, and is therefore not reflected below:
>
> - **Light mode does not appear in this document at all.** It was built after
>   this draft, 29 commits later. It is a whole execution capability. It is a
>   standalone always-on-top window, showing one test case at a time, with `P`,
>   `F` and `B` on it.
>   Section 6.2 is incomplete without it. See
>   [the light mode document](editorPanel/lightMode.md).
> - **#74 is closed.** Section 8 names the grid view's missing keyboard path as a
>   live gap tracked by it. That needs re-checking.
> - **#68 is closed.** Question 2 still calls it open.
> - **#71 is closed.** Question 3 still calls it open.
> - **Section 6.1's keys are wrong in three places.** Creating a node has no key
>   of its own anymore; `Ctrl+M` creates a test case and nothing else. Renaming
>   is `Shift+F6`, not `F2`, and it is one action rather than two. `Ctrl+F` puts
>   the cursor in an editor's own search box; searching the whole test project is
>   `Ctrl+Alt+F`. [Every shortcut](shortcuts.md) is the current list.
> - **The count of bound keys is from 29 August 2026.** So is every number in
>   section 2 that rests on it.
> - **Rule-PRODUCT-020 is no longer the whole condition.** Automation code needs
>   the Java plugin *and* a `testin.yml` naming the open test project. Without
>   the file, nothing is generated and Testin says so once. See Decision-013 on
>   [the decisions page](decisions.md).
> - **A test run's run items are one file per test case now**, named by the test
>   case, beside the test run's own file. Section 4's tree is right about what
>   exists and says nothing about the files. [The formats on disk](formats.md) is
>   the contract.
> - **Rule-PRODUCT-018 is not what the product does.** One keystroke carries
>   three actions today, and it is deliberate: `F2` changes the thing in front of
>   the tester, and which thing that is depends on where they are standing. The
>   rule that describes it now is Rule-INTERNAL-071 on
>   [the search page](internal/globalSearch.md).
>
> Nothing above has been corrected in the text below, deliberately: this is
> Draft 1 as written. The issue that owned correcting it,
> [#72](https://github.com/MutebAlMughyiri/test-in/issues/72), was closed as not planned
> on 11 September 2026. The documentation is being written as the eight parts
> listed on [the home page](README.md) rather than as a separate business
> specification. So those parts are where a current answer lives. Nothing owns
> this page now. The document's own rule is that a rule which quietly stops
> being true is worse than no rule, so the staleness is stated instead of
> patched over.
>
> **Two databases are not fully moved.** Section 7 has 21 of its 33 business
> rules, and none of their numbers. Section 6.4, the use cases, has none
> of its rows. Both were Notion databases, and a copy of the page does not carry
> those.

---

## 1. Purpose and scope

Testin is a test case manager that lives inside the IDE instead of inside a
browser tab.

This document states what the product **promises**. It says who uses Testin,
what they work with, what they can do, and the rules that govern all of it.

**In scope:**

- the actors
- the things they work with
- the capabilities they can trigger
- the numbered rules
- every status, and its transitions
- the expectations that are already promises

**Out of scope: how any of it is built.** Class names, packages and
architecture are deliberately absent. A reader does not need them, and they
date the document the first time the code is refactored. This document should
survive a rewrite of the product.

---

## 2. The idea: the keyboard is the product

> This is the section to read if only one section is read.

Testin's competitors are web applications: Jira, Azure DevOps and TestRail.
That is not a small detail of how they are built. It is a limit on how fast
anyone can work in them.

|                            | A browser-based tool                                                                                                                         | Testin                                             |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| **Who owns the keyboard**  | The browser. `Ctrl+N`, `Ctrl+W`, `Ctrl+T`, `F5`, `F6` and `F12` are already taken, and what is left works only while no text field has focus | The IDE, which hands its key map to the plugin     |
| **What one change costs**  | A trip to the server. Click the dropdown, wait, click the option, wait for the save                                                          | A write on this machine, straight away             |
| **Whether work stacks up** | It does not. There is no way to reach a run item status from the keyboard, so eight rows cannot be judged in one go                          | Select the rows, press one key. Any number of rows |

So a tester executing a test run of 200 test cases in a browser tool does it
with the mouse, one test case at a time. The tool can never be faster than the
browser it is trapped in.

**A tester using Testin can execute a whole test run without touching the
mouse.** Move to the test case, read it, press one key for the run item
status, move on. Every other capability is a reason to be in Testin: the tree,
the grid, the reports, the Git integration. The keyboard is the reason the work is faster
once the tester is there.

That idea is written down here as a capability, with rules behind it - **Rule-PRODUCT-015** to **Rule-PRODUCT-018**, and
**Rule-PRODUCT-024**. It is
not left to fall out of whichever keys happen to be bound.

**62 keys are bound**, counted against the product at `0becc8b2`. Of those, 40
are shared across screens, so the same gesture means the same thing
everywhere. The other 22 belong to a single capability. Standard text editing
inside dialogs is restored on top of those, and is not counted. That is the
platform's behavior, not Testin's.

---

## 3. Actors

> **There is exactly one actor today: the Tester.**

The product stores a tester's **name** and a tester's **role**. The name is
real. It is stamped on every run item status, and on every record the tester
touches, so a test run always says who judged it. **The role is read by
nothing at all today.** It is typed into the settings page, written to disk,
and never used again. The HTML report once printed it above the footer. That
line was removed, and nothing replaced it.

It is **reserved rather than abandoned**. A planned feature will read it to
decide who may approve a test case, and who may remove a test project. Until
that ships, three things are true. No permission depends on either field.
Nothing is assigned by them. No capability is withheld because of them. So
describing a "Lead" as an actor today would describe behavior that does not
exist. See **Appendix A** and **Question 4**.

Planned actors are in **Appendix A**, kept separate so the main body stays
checkable against the product.

> ℹ The use case diagram (`testin-use-cases.png`) has not been moved. It was an
> image in the Notion page and did not come across with the text.

---

## 4. The things a tester works with

Testin stores everything as files in a folder the tester chooses. The structure
is a tree, and the tree is the product's vocabulary.

```
Test Project
├── Test Cases                (fixed container)
│   ├── Test Set Package      (can hold another package)
│   └── Test Set
│       └── Test Case
└── Test Runs                 (fixed container)
    ├── Test Run Package      (can hold another package)
    └── Test Run
        └── Run Item          (one per test case in the run)
```

| Thing                | What it is                                                                                                                                    | Notes                                                       |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| **Test Project**     | The top of one tree, and one folder on disk                                                                                                   | Carries a status. Removing it removes everything beneath it |
| **Test Cases**       | The fixed folder holding everything there is to test                                                                                          | Cannot be created, renamed, moved or removed                |
| **Test Runs**        | The fixed folder holding every test run                                                                                                       | Cannot be created, renamed, moved or removed                |
| **Test Set Package** | A folder that groups test sets. One package can hold another                                                                                  | Carries a status                                            |
| **Test Set**         | A named group of test cases. A test run is built from one                                                                                     | Carries a status                                            |
| **Test Case**        | One thing to test: description, preconditions, steps, expected result, test data, module, group, priority                                     | The only thing in the tree a tester writes                  |
| **Test Run Package** | A folder that groups test runs. One package can hold another                                                                                  | Carries a status                                            |
| **Test Run**         | One pass through a chosen set of test cases, at one moment                                                                                    | Carries a status. Holds one result per test case            |
| **Run Item**         | What happened to one test case in one test run: run item status, who recorded it, when, how long it took, and the failure detail if it failed | Belongs to the test run, not to the test case               |

> **The difference that matters most.** A test case is the *question*. A
> run item is one *answer*, at one moment, by one person. A test case can be
> in many test runs, and carry a different run item in each. This is why deleting a test case does
> not erase history. See **Rule-PRODUCT-007**.

---

## 5. Statuses and transitions

There are seven kinds of status, and **26 values** in all. The seven do not
affect each other, and that is on purpose. Where a test run has got to is one
question. The run item status a test case got is another. What the team thinks
of the test case itself is a third.

> Issue [#72](https://github.com/MutebAlMughyiri/test-in/issues/72) counts 22 values
> across 6 kinds of status. Measured against the product, **it is 26 across
> 7**. Two corrections. First, "Removed" moved. It used to sit with a test
> project's status, and it belongs with the run item status a test case gets. A
> test project has no removed state, because removing one deletes it. A test
> case deleted out from under a test run leaves a row the test run still owns.
> Second, a **seventh kind of status was missed entirely**. A test case carries
> its own status, separate from any run item status. It is section 5.7, and it
> is the one worth reading.

### 5.1 The run item status a test case gets in one test run — six values

| Run Item Status | Meaning                                                                    | Who sets it                      | Key |
|-----------------|----------------------------------------------------------------------------|----------------------------------|-----|
| **Pending**     | Waiting. This test run holds the test case and has not reached it          | The test run, when it is created | —   |
| **Passed**      | The test case behaved as expected                                          | Tester                           | `P` |
| **Failed**      | It did not. The failure detail is collected in the same gesture            | Tester                           | `F` |
| **Blocked**     | The test case could not be judged. Something outside it prevented the test | Tester                           | `B` |
| **Untested**    | The test run ended without ever reaching this test case                    | The test run, when it ends       | —   |
| **Removed**     | The test case itself has been deleted. The row is history now              | The test run, when it notices    | —   |

**Three run item statuses are a tester's to give, and exactly those three carry
a key.**
The other three are the test run's record of its own state. They have no key on
purpose, because there is nothing for a person to apply.

```mermaid
stateDiagram-v2
    Pending --> Passed: P
    Pending --> Failed: F
    Pending --> Blocked: B
    Passed --> Failed: F
    Failed --> Passed: P
    Blocked --> Passed: P
    Blocked --> Failed: F
    Pending --> Untested: run completed or closed
    Pending --> Removed: test case deleted
    Passed --> Removed: test case deleted
    Failed --> Removed: test case deleted
```

### 5.2 Where a test run has got to — six values

| Status          | Meaning                                     | Key | Signed off |
|-----------------|---------------------------------------------|-----|------------|
| **Created**     | Set up, never started                       | —   | no         |
| **In Progress** | At least one test case has been run         | —   | no         |
| **Assigned**    | Handed to someone to run                    | `1` | no         |
| **Completed**   | Finished and signed off                     | `2` | **yes**    |
| **Closed**      | Ended without being finished                | `3` | **yes**    |
| **Committed**   | Completed, and committed to Git: the record | —   | **yes**    |

Created, In Progress and Committed carry no key, because the product sets them
itself. A
test run is Created when it is made. It moves to In Progress the moment
anything in it is executed, however it was started.

**A signed-off test run cannot be started or edited.** A Completed or Closed one
still takes run item statuses and corrections. **A Committed test run records
nothing further** (Rule-PRODUCT-011): it becomes Committed when a commit takes it
while it is Completed, and from then on each run item shows its test case as
that commit holds it. Any run item arriving from anywhere else is refused. The one
thing it still takes is the bug issue a failed test case is reported as. That
changes no run item status. (UC-VIEW-PANEL-016)

```mermaid
stateDiagram-v2
    Created --> Assigned: 1
    Created --> InProgress: first test case executed
    Assigned --> InProgress: first test case executed
    InProgress --> Completed: 2 or every test case judged
    InProgress --> Closed: 3
    Created --> Completed: 2
    Created --> Closed: 3
    Assigned --> Completed: 2
    Assigned --> Closed: 3
    Completed --> Committed: a commit in View Pending Commits
```

> ** Undecided. See section 9.** The diagram shows what the product *allows*.
> It currently allows every move, including Completed back to In Progress.
> **Nothing stops a test run moving anywhere at all.** Whether a signed-off test
> run may be reopened is **Question 1**.

### 5.3 What a card shows while tests are running — four values

These four are never saved to disk. They are what a card shows while tests are
actually running: *idle*, *running*, *passed*, *failed*. They last only as long
as the IDE is open. The test run's own record is the one that is kept.

### 5.4 A test project's status — two values

**Active** and **Inactive**. One word for "not now", because a tester should
not have to tell two kinds of it apart. There is no removed state, because
removing a test project deletes it.

### 5.5 A test set's status — two values

**Active** and **Deprecated**. Deprecated is not deleted.
See **Rule-PRODUCT-022**.

### 5.6 A package's status — two values

**Active** and **Archived**. Shared by test set packages and test run packages,
because it means the same thing in both. See **Rule-PRODUCT-023**.

### 5.7 A test case's own status — four values

This is separate from the run item status, and easy to confuse with it. A **run item status** says what happened to the
test case in one test run. **This** says what the team thinks of the test case itself, across every test
run it is ever in.

| Status            | What it means                                                    |
|-------------------|------------------------------------------------------------------|
| **Pending**       | The default. Written, and nobody has said whether it is any good |
| **Reviewed**      | Someone has checked the test case is correct                     |
| **Disabled**      | The test case exists but is not to be used                       |
| **To Be Updated** | The test case is known to be out of date                         |

> ** Nothing in the product uses this today.** It can be set in exactly one
> place. The tester switches on the **Status** column in the grid view, which is
> hidden by default, and types the label. There is no menu entry, and no key. It
> is written into an export and **not read back by an import**, so a test case
> exported as Reviewed comes back Pending. No filter, badge, report, count or
> rule reads it, and no capability is withheld because of it.
>
> That matters because **Reviewed is the "approved" state the planned role
> permissions describe.** The state already exists on every test case. Nothing
> decides who may set it, and nothing at all changes once it is set. See
> **Question 5**.

---

## 6. Capabilities

Every capability below is reachable from the project tree's context menu, an
editor toolbar, or a key. **A capability with no key says so, and says why.**

### 6.1 Authoring

| Capability                                                  | Key                             | Notes                                                                                                     |
|-------------------------------------------------------------|---------------------------------|-----------------------------------------------------------------------------------------------------------|
| Create a node: test project, package, test set or test case | `Ctrl+M`                        | The dialog offers only what is legal in the selected place                                                |
| Open the selected node                                      | `Enter`                         |                                                                                                           |
| Rename                                                      | `F2`                            |                                                                                                           |
| Rename, carrying the automation code with it                | `Shift+F6`                      | Renames the generated method too, so the test case stays runnable                                         |
| Reorder within the parent                                   | —                               | Drag, or the menu. Order carries meaning: it is the execution order                                       |
| Copy / Cut / Paste a node                                   | `Ctrl+C` `Ctrl+X` `Ctrl+V`      |                                                                                                           |
| Copy / Cut / Paste a test case                              | *the right-click menu*          | No key: these are menu entries a tester clicks. `Ctrl+C` in an editor is the grid's, and stays the grid's |
| Undo / Redo the last tree change                            | `Ctrl+Z` / `Ctrl+Y`             |                                                                                                           |
| Delete                                                      | `Delete`                        | Refused on the two fixed containers                                                                       |
| Edit one field of a test case directly                      | `D` `E` `M` `T` `B` `S` `P` `G` | Description, expected result, module, test data, preconditions, steps, priority, group                    |
| Bulk edit in a grid                                         | —                               | The grid takes Excel gestures: `Ctrl+C`, `Ctrl+X`, `Ctrl+V`                                               |
| Search                                                      | `Ctrl+F`                        |                                                                                                           |

### 6.2 Execution — the flow the product exists for

| Capability                                         | Key                 | Notes                                                               |
|----------------------------------------------------|---------------------|---------------------------------------------------------------------|
| Start execution                                    | —                   | Toolbar. Begins at the first test case the test run has not reached |
| Run the selected test cases through the automation | `F5`                |                                                                     |
| Run everything a node holds                        | —                   | Context menu, on a test set or a test run                           |
| Stop                                               | —                   | Toolbar. Puts every test case it started back                       |
| **Record Passed**                                  | **`P`**             |                                                                     |
| **Record Failed**                                  | **`F`**             | Opens the failure detail dialog in the same gesture                 |
| **Record Blocked**                                 | **`B`**             |                                                                     |
| Move to the next / previous test case              | `Ctrl+→` / `Ctrl+←` |                                                                     |
| Set the test run's status                          | `1` `2` `3`         | Assigned, Completed, Closed                                         |
| Jump to the generated automation method            | `Shift+F5`          |                                                                     |
| Generate the automation method                     | `F12`               |                                                                     |

> **ℹ Light mode is missing from this table.** It was built after this draft: a
> standalone always-on-top window showing one test case at a time, so a tester
> can work with the IDE minimized and still record a run item status. The keys
> are `P`, `F` and `B`, with `Ctrl+D` for the rest of the test case and `Escape`
> to close. It is the clearest example of section 2's idea, and it is not
> described here.
> See
> [the light mode document](editorPanel/lightMode.md).

### 6.3 Evidence and exchange

| Capability        | Key      | Formats                                                            |
|-------------------|----------|--------------------------------------------------------------------|
| Generate a report | `Ctrl+P` | HTML, PDF, Word, Excel                                             |
| Export            | —        | CSV, Excel, HTML, JSON                                             |
| Import            | —        | CSV, Excel, JSON                                                   |
| Show node details | —        | Counts for a container, a run item status breakdown for a test run |
| Sync with Git     | —        | Needs the Git plugin                                               |

### 6.4 Use cases

> ** Not moved yet.** This was a Notion database. It held one row per
> capability a tester triggers, numbered `UC-nn`. Each row was in the usual form
> (who does it, what must be true first, the main flow, the alternatives, and
> what is true afterward). Each one named the key that starts it.
>
> **Mouse needed** is the column to scan. Anything in the execution flow that is
> not **No** breaks **Rule-PRODUCT-024**. That is not a detail.

---

## 7. Business rules

Numbered so an issue or a commit can cite one.

> ** Partially moved: 21 of 33 rules, and without their numbers.** This was a
> Notion database. The text of 21 rules came across. The column holding their
> numbers did not.
> The numbers are the point of the database, because an issue cannot cite a rule
> that has none. So they are left blank rather than invented.
>
> Twelve rules are missing entirely. Among them were the three this document
> pointed at by number and never carried: that a deprecated test set is not
> deleted, that Active and Archived mean the same for both kinds of package, and
> that every step of the execution flow can be reached from the keyboard. Each is
> quoted somewhere in the text above, so each is written into the table below in
> its own words rather than invented - **Rule-PRODUCT-022**,
> **Rule-PRODUCT-023** and **Rule-PRODUCT-024**. They carry the numbers after the
> last, not the numbers their sections would suggest, because a rule keeps the
> number it is given.
>
> **Every number in this document now names a rule.** The six that pointed at
> nothing - Rule11, Rule12, Rule13, Rule31, Rule40 and Rule50 - are written as
> `Rule-PRODUCT-nnn` and point at the rule they always meant. The nine rules
> still missing from the table are missing text, not missing numbers. Nothing
> owns restoring them: the issue that did was closed with this page unfinished.
>
> **Rules the product broke were written down, not hidden.** Two were broken by the
> product at `0becc8b2`, and each was cited to the issue that tracks it. Those
> citations are also not in what arrived.

### Data and storage

| BR                   | Rule                                                                                                                                                   |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-001** | All data is files on the tester's own disk, in a folder they chose.                                                                                    |
| **Rule-PRODUCT-002** | A test project is one folder. Everything belonging to it lives beneath that folder and nowhere else.                                                   |
| **Rule-PRODUCT-003** | What is stored is exactly what the tester typed, character for character. Showing a value on screen may tidy it up. Saving never does.                 |
| **Rule-PRODUCT-004** | Secrets are never written to a file the repository carries, and Testin holds none: a Git remote's credentials are kept by Git's own credential helper. |

### Structure

| BR                   | Rule                                                                                                                                                                    |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-005** | A node may only be moved into a place that can legally hold it. A test set cannot be dropped among test runs, and a test run cannot be dropped inside another test run. |
| **Rule-PRODUCT-006** | Test Cases and Test Runs are fixed containers. They cannot be created, renamed, moved or removed.                                                                       |
| **Rule-PRODUCT-022** | A deprecated test set is not deleted. It is kept, and drawn as deprecated.                                                                                              |
| **Rule-PRODUCT-023** | Active and Archived mean the same thing for a test set package as for a test run package.                                                                               |

### Test runs and history

| BR                   | Rule                                                                                                                                                                                                                                                                                     |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-007** | A test run is a record of an execution at a point in time, not a live view of the test set. Changing a test case after a test run has judged it does not change what the test run recorded.                                                                                              |
| **Rule-PRODUCT-008** | A run item is written into the test run the tester started, and no other. The same test case running in another test run does not affect this one.                                                                                                                                       |
| **Rule-PRODUCT-009** | A test case may belong to any number of test runs and carry a different run item status in each. The run item status belongs to the test run.                                                                                                                                            |
| **Rule-PRODUCT-010** | Every run item status records who gave it and when, whether a person typed it or the automation reported it.                                                                                                                                                                             |
| **Rule-PRODUCT-011** | A Committed test run records nothing further: no run item status is given or corrected on it and no run item arriving from anywhere is written into it. The one exception is the bug issue link a failed test case is reported as, which changes no run item status (UC-VIEW-PANEL-016). |

### Run item statuses

| BR                   | Rule                                                                                                                                                                |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-012** | A tester may record exactly three run item statuses: Passed, Failed, Blocked.                                                                                       |
| **Rule-PRODUCT-013** | Pending and Untested are the test run's own record, and a person cannot apply them. Pending means not reached yet. Untested means never reached, and never will be. |

### Feedback

| BR                   | Rule                                                                                                                                                        |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-014** | Every action that changes something confirms itself once, in the past tense. An action on several things confirms once, with a count, never once per thing. |

### The keyboard

| BR                   | Rule                                                                                                                                       |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-015** | A published binding is a promise. Changing one costs every tester their muscle memory, so a binding changes only deliberately and visibly. |
| **Rule-PRODUCT-016** | A key is shown wherever the capability it triggers is shown, so it can be learned by using the menu once.                                  |
| **Rule-PRODUCT-017** | A capability with no key says so rather than being silently unreachable.                                                                   |
| **Rule-PRODUCT-018** | One key means one thing. The same keystroke does not do different jobs in different places.                                                |
| **Rule-PRODUCT-024** | Every step of the execution flow can be reached from the keyboard. A capability in that flow that needs the mouse breaks this.             |

### When a plugin is missing

| BR                   | Rule                                                                                                                                                             |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Rule-PRODUCT-019** | Testin installs and runs in any JetBrains IDE. Where an optional plugin is missing, the features that need it are withheld, and Testin says why. It never fails. |
| **Rule-PRODUCT-020** | Without Java: no automation code is written, and no jumping to it. Everything else works.                                                                        |
| **Rule-PRODUCT-021** | Without Git: no sync and no cloning. The data is still on disk, and still usable.                                                                                |

---

## 8. Promises about the product as a whole

These are promises already kept, not hopes.

| Promise                                                 | What it means                                                                                                                                                           |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **The data stays on your machine**                      | Everything is files under a folder the tester chose. Nothing is uploaded                                                                                                |
| **Nothing is sent anywhere**                            | Testin makes no network calls of its own. The only traffic is the tester's own Git sync, which they set up and start                                                    |
| **What you typed is what is stored**                    | Stored data matches what was typed, character for character — **Rule-PRODUCT-003**                                                                                      |
| **A missing plugin removes a feature, not the product** | Testin withholds the feature and says why. It never shows an error — **Rule-PRODUCT-019**                                                                               |
| **The keyboard is enough**                              | A tester can run a whole test run without the mouse. **True of the card view. The grid view was a gap, tracked as #74, which is now closed, so this needs re-checking** |
| **It runs in every JetBrains IDE**                      | IntelliJ IDEA, PyCharm, GoLand, WebStorm and the rest of the family                                                                                                     |

---

## 9. Undecided

Listed rather than guessed. Each is a real question the product has not
answered.

|                | Question                                                                 | Why it is open                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|----------------|--------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Question 1** | May a Completed or Closed test run be reopened?                          | **Nothing stops a test run moving anywhere at all today.** Any status can be set from any other. Section 5.2 describes what is *allowed*, which is everything. The table that once said which moves were legal was deleted, and never replaced. Tracked in [#10](https://github.com/MutebAlMughyiri/test-in/issues/10)                                                                                                                                                                                                                                      |
| **Question 2** | Is a deprecated test set hidden, or just not offered?                    | Today it is shown, drawn gray, and not offered. Whether it should disappear from the tree entirely is not settled. Tracked in [#68](https://github.com/MutebAlMughyiri/test-in/issues/68), **now closed, so re-check**                                                                                                                                                                                                                                                                                                                                      |
| **Question 3** | What happens to a test run when the whole test set behind it is deleted? | The answer for one test case is **Rule-PRODUCT-007**. The answer for a whole test set has never been stated. Tracked in [#71](https://github.com/MutebAlMughyiri/test-in/issues/71), **now closed, so re-check**                                                                                                                                                                                                                                                                                                                                            |
| **Question 4** | What are the roles, and what does each one permit?                       | The field is reserved, not dead: role-based permissions are planned, and will read it to decide **who may approve a test case** and **who may remove a test project**. What is undecided is the list of roles and the permission each carries — and free text cannot answer "may this person approve", so the field will need a fixed set behind it. It is also application-level today, meaning one role per IDE installation rather than one per team member. Tracked in [#14](https://github.com/MutebAlMughyiri/test-in/issues/14)                      |
| **Question 5** | What does a test case's own status do?                                   | Every test case has four states: Pending, Reviewed, Disabled and To Be Updated. **Not one of them changes anything.** A Disabled test case is still offered to a test run. A To Be Updated test case is still run. A Reviewed test case is treated exactly like an unreviewed one. The state can be set only in a hidden grid column, and an import drops it. Deciding what each state *does* is the same decision as **Question 4**. Reviewed means approved, and approval means nothing until something is withheld from a test case that is not approved |

---

## Appendix A — Planned actors

> ** Nothing in this appendix exists.** It is kept separate so the main body stays
> true and checkable against the product.

| Planned actor | What they would do                                                                                                                                                                                                              | Tracked in                                                                                                                 |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| **Lead**      | Assign a test run to a tester and watch its progress without executing it                                                                                                                                                       | [#14](https://github.com/MutebAlMughyiri/test-in/issues/14)                                                                |
| **Lead**      | Be notified when a test run completes or a test case fails, without opening the IDE                                                                                                                                             | [#15](https://github.com/MutebAlMughyiri/test-in/issues/15), [#159](https://github.com/MutebAlMughyiri/test-in/issues/159) |
| **Any role**  | Gate capabilities by role: who may approve a test case, and who may remove a test project. This is what the stored `Tester role` is reserved for — it is the reason the field stays on the settings page while nothing reads it | [#14](https://github.com/MutebAlMughyiri/test-in/issues/14)                                                                |

---

## Appendix B — Glossary

| Term                | Definition                                                                                      |
|---------------------|-------------------------------------------------------------------------------------------------|
| **Test Project**    | The top of one tree. One folder, holding everything below it                                    |
| **Test Set**        | A named group of test cases. A test run is built from one                                       |
| **Test Case**       | One testable thing, with its steps and expected result. The question                            |
| **Test Run**        | One execution of a chosen set of test cases at a point in time                                  |
| **Run Item**        | What happened to one test case in one test run. The answer                                      |
| **Run Item Status** | Passed, Failed or Blocked — the three a tester can give                                         |
| **Pending**         | This test run holds the test case and has not reached it yet                                    |
| **Untested**        | The test run ended without ever reaching the test case                                          |
| **Removed**         | The test case has been deleted. The test run keeps what it recorded                             |
| **Signed off**      | Completed or Closed. A signed-off test run records nothing further, apart from a bug issue link |
| **Deprecated**      | A test set kept for its history but no longer offered for new test runs                         |
| **Archived**        | A package kept but moved out of the way                                                         |
| **Automation**      | The generated test method that executes a test case without a person                            |

---

> **How to keep this true.** Every rule above can be checked against the product.
> When the plugin changes, change the rule in the same breath, and put its number
> in the commit message. A rule that quietly stops being true is worse than no
> rule, because someone will plan against it.

---

[Documentation](README.md) › **The product** — the parts have documents of their own, starting
with [the tree panel](treePanel/main.md)
