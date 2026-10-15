[Documentation](../README.md) › Sharing work with the team

# Sharing work with the team

Test data is files on one machine. This part is every way those files get to
somebody else and back: a spreadsheet handed over, or a Git repository the
whole team pulls.

|                     |                                                                                                                                                                              |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | Sharing work with the team                                                                                                                                                   |
| **Answers**         | How test cases leave Testin and come back, and how a test project is kept in step with a team                                                                                |
| **Numbering**       | Use cases are `UC-SHARE-001` to `UC-SHARE-023`. Rules are `Rule-SHARE-001` to `Rule-SHARE-133`                                                                               |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                  |
| **Checked against** | `main` at `1270e599`, 20 September 2026. Every page in this part was read against the code: the messages, the keys, the dialog titles, the button labels and the file names. |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                  |

---

## The use cases

|                  | What the tester does                                                       | Why the tester would                                      |
|------------------|----------------------------------------------------------------------------|-----------------------------------------------------------|
|                  | **Sending test cases out**                                                 |                                                           |
| **UC-SHARE-001** | [Export one test set](exportTestSet.md)                                    | Send test cases to somebody who has no IDE.               |
| **UC-SHARE-002** | [Export a package, one sheet for each test set](exportPackage.md)          | Send many test sets to a reviewer as one file.            |
| **UC-SHARE-003** | [Choose what goes into the export, and correct it](chooseWhatToExport.md)  | Drop rows and fix typos before the file is written.       |
| **UC-SHARE-004** | [Open the exported file, or copy its path](openExportedFile.md)            | Check the new file right away.                            |
|                  | **Bringing test cases in**                                                 |                                                           |
| **UC-SHARE-005** | [Import into a test set](importIntoTestSet.md)                             | Bring a spreadsheet of test cases into one test set.      |
| **UC-SHARE-006** | [Import into a package, one test set for each sheet](importIntoPackage.md) | Turn a whole workbook into many test sets.                |
| **UC-SHARE-007** | [Choose what is imported, and correct it](chooseWhatToImport.md)           | Fix what the file got wrong before it becomes test cases. |
|                  | **Working with Git**                                                       |                                                           |
| **UC-SHARE-008** | [Tell Git who I am](setGitIdentity.md)                                     | Let Git put a name on the tester's commits.               |
| **UC-SHARE-009** | [Put the test project under Git](putUnderGit.md)                           | Start versioning a test project Git does not know yet.    |
| **UC-SHARE-010** | [See what I have not committed](reviewChanges.md)                          | See every change made since the last commit.              |
| **UC-SHARE-011** | [Put one changed field back](revertOneChange.md)                           | Undo one field that was changed by mistake.               |
| **UC-SHARE-012** | [Commit without pushing](commitChanges.md)                                 | Record the work on this machine only, for now.            |
| **UC-SHARE-013** | [Commit and push](commitAndPush.md)                                        | Record the work and send it to the team at once.          |
| **UC-SHARE-014** | [Commit onto a different branch](commitToBranch.md)                        | Keep these changes off the branch being tested now.       |
| **UC-SHARE-015** | [Push a commit that never left this machine](pushOldCommit.md)             | Find work the tester thinks the team already has.         |
| **UC-SHARE-016** | [Send my changes and take the team's](syncWithGit.md)                      | Give and take work in one gesture.                        |
| **UC-SHARE-017** | [Resolve the conflicts a pull stopped on](resolveConflicts.md)             | Finish a pull that two people's changes stopped.          |
| **UC-SHARE-018** | [Answer which side wins for a field both changed](answerMergeQuestions.md) | Choose between the tester's wording and a colleague's.    |

Cloning a test project from a repository is how a test project first arrives,
and it is [UC-TREE-PANEL-003](../treePanel/importTestProject.md).

---

## What this part is for

A test project is a folder of files. That is deliberate, and it is what makes
all of this possible: the files can be committed, synced, exported and read by
anything.

There are two ways to share them.

| Way                   | What it suits                                                                                       |
|-----------------------|-----------------------------------------------------------------------------------------------------|
| **Export and import** | Handing a set of test cases to somebody who has no IDE, or bringing in a spreadsheet somebody wrote |
| **Git**               | A team that already uses Git, and wants the test data reviewed and versioned like code              |

`testin.yml`, in the code repository, can say that this test project is shared
through Git, and where it is cloned from.

**Four words, before the rules use them.**

- To **export** is to write test cases out to a file.
- To **import** is to read test cases from a file into a test set.
- To **sync** is to send what is here and take what is there, in one gesture.
- A **conflict** is a file two people changed since they last agreed.

---

## Every key

| Key           | What it does                                                                                                                                                                                               | The page that owns it                 |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `Enter`       | Presses the dialog's own button: **Export**, **Import**, **Commit & Push**, **Keep Selected** or **Export anyway**. The group picker and the Git identity have no button, and their line reads **Confirm** | The page that opens each              |
| `Escape`      | Closes every dialog in this part. On a merge question it answers nothing and moves to the next file                                                                                                        | Everywhere                            |
| `Ctrl+Click`  | Adds a group in the group picker                                                                                                                                                                           | [UC-SHARE-003](chooseWhatToExport.md) |
| Right click   | Puts one change back, in the review                                                                                                                                                                        | [UC-SHARE-011](revertOneChange.md)    |

**Nothing has a key** for the menu entries: **Export**, **Import**, **Sync With
Remote** and **View Pending Commits**.

Every dialog in this part names its keys along the bottom, so the tester reads
which button `Enter` presses before they press it.

---

## Which formats do what

| Format   | A report | An export | An import |
|----------|----------|-----------|-----------|
| **XLSX** | Yes      | Yes       | Yes       |
| **XLS**  | No       | No        | **Yes**   |
| **CSV**  | No       | Yes       | Yes       |
| **JSON** | No       | Yes       | Yes       |
| **HTML** | Yes      | Yes       | No        |
| **PDF**  | Yes      | No        | No        |
| **Word** | Yes      | No        | No        |

The older spreadsheet format can be imported and not exported, on purpose.
Choosing it once produced a file in the newer format under the older name.

## What one exported row holds

Seventeen columns, in this order: **Description**, **UUID**, **Expected Result**, **Steps**, **Priority**, **FQCN**,
**Reference**, **Test Data**, **Pre
Conditions**, **Group**, **Path**, **Module**, **Status**, **Created By**, **Updated By**, **Created At**, **Updated
At**.

The steps are joined by a comma, and so are the groups. A date is written in
full, with the time zone.

The JSON export is different. It writes every field of the test case as Testin
stores it, not the 17 columns.

## What an import reads

Thirteen columns: **Description**, **Expected Result**, **Steps**, **Priority**, **Reference**, **Test Data**, **Pre
Conditions**, **Group**, **Module**, **Created By**, **Updated By**, **Created At**, **Updated At**.

**Order**, **UUID**, **FQCN**, **Path** and **Status** are never imported. A file
carrying a status column has it ignored.

Headings are matched whatever their capitals, and spaces around them are
ignored. A heading Testin does not know is left alone.

---

## What needs the Git plugin

| Feature                                                                    | Needs it |
|----------------------------------------------------------------------------|----------|
| Export and import                                                          | No       |
| Reports                                                                    | No       |
| **Sync with Remote**, and everything reached from **View Pending Commits** | **Yes**  |

Without the Git plugin those two menu entries are still in the menu, grayed, and
each says what it needs: *(needs the Git plugin)*. The menu has the same shape
in every IDE, and the reason is on the entry (Rule-SHARE-105). A plugin that is
installed but switched off counts as missing, and switching it on needs the IDE
restarted before Testin notices.

The Java plugin is needed for one thing here: an import generates test methods
for what it brought in. Without it the test cases still import, and a message
says once that the code was not generated.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet. The ones that were repaired are in the table under the retired
rules, with the issue that repaired them.

|                   | The rule it breaks                                 | What a tester sees                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------------|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 12** | Rule-SHARE-005 — long work can be canceled         | The import bar stops between test cases now, and still says how many were written. The report bar offers **Cancel** and nothing inside asks: the four generators are handed the document to write and never the bar, so the press takes effect only once the document is finished. An export has nothing long left to stop, because it reads the tree from memory and writes one file at the end.                                                                                                                                                                |
| **Difference 16** | Rule-SHARE-003 — the tester is told                | A message can still be shown to nobody: with no status bar on the code project's window, the balloon is dropped. It is written to `testin.log` now rather than lost, so an export that finished with nobody told can be told from one that did not finish. Nothing is raised in its place — a notification about a failed notification is noise.                                                                                                                                                                                                                 |
| **Difference 20** | Rule-SHARE-109 — the dialog says what was settled  | A merge that settles a whole file says nothing at all. The window is what carries the settled list, and it opens only when a question is left, so a result taken whole from the later run item status, a test run's marker merged by rule, and a test case whose only clash was its stamps and its position are all written with no word to the tester. The tester hears what was settled only when something else in the same file still has to be answered.                                                                                                    |
| **Difference 21** | Rule-SHARE-132 — safe mode runs no Git and says so | In a project in safe mode the Git runner refuses every command and words it as "Choose Trust Project to turn Git on", but most steps lose that line: `GitRepositoryService.run` turns the refusal into an empty answer, so Sync says no remote is configured, Commit asks for an identity, choosing a branch says it could not be checked out and the review of changes looks empty. Clone builds its Git command on the default project, which is never in safe mode, so `git clone` runs anyway. [#429](https://github.com/MutebAlMughyiri/test-in/issues/429) |

**Retired.** A use case or a rule that is gone keeps its number forever, so an
issue that quotes one still leads somewhere and nothing is ever renumbered onto
it.

| Gone               | Was                                                                                                                                                                                                                                                                                                                                                                                                                                   | Read instead                                        |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| **UC-SHARE-023**   | *Remember the folder I use* — a **Set as default folder** tick box on the export, report and import dialogs wrote the machine's download folder. It was drawn only while no folder was set, so once one was there those dialogs could not change it back, and it overwrote a value the tester had set on the settings page without saying so. Removed 9 September 2026, [#240](https://github.com/MutebAlMughyiri/test-in/issues/240) | [UC-SETTING-006](../setting/setDownloadFolder.md)   |
| **Rule-SHARE-102** | *The tick box is drawn only while no folder has been set yet.*                                                                                                                                                                                                                                                                                                                                                                        | —                                                   |
| **Rule-SHARE-103** | *One folder is remembered, and every dialog uses it.* Said twice; the settings page owns the folder                                                                                                                                                                                                                                                                                                                                   | [Rule-SETTING-021](../setting/setDownloadFolder.md) |
| **Rule-SHARE-104** | *The export dialog remembers the folder in its box. The import dialog remembers the folder holding the file that was chosen.* Two dialogs storing two different things under one name was half the defect                                                                                                                                                                                                                             | —                                                   |
| **UC-SHARE-019**   | *Sync with the team's server* - send a test project to an SFTP server and take what is there, in one gesture. Removed 19 September 2026, when Git became the only way a test project is shared ([#334](https://github.com/MutebAlMughyiri/test-in/issues/334))                                                                                                                                                                        | [UC-SHARE-016](syncWithGit.md)                      |
| **UC-SHARE-020**   | *Have my password kept for next time* - the server password, kept in the IDE's password store                                                                                                                                                                                                                                                                                                                                         | —                                                   |
| **UC-SHARE-021**   | *Answer the conflicts the server sync could not settle*                                                                                                                                                                                                                                                                                                                                                                               | [UC-SHARE-018](answerMergeQuestions.md)             |
| **UC-SHARE-022**   | *Agree to remove files the server no longer holds*                                                                                                                                                                                                                                                                                                                                                                                    | —                                                   |
| **Rule-SHARE-085** | *The server's address comes from `testin.yml`, which the team shares. The account comes from this machine's settings.*                                                                                                                                                                                                                                                                                                                | —                                                   |
| **Rule-SHARE-086** | *An account is never written into the shared file. If one is found there it is ignored, and the log says so.*                                                                                                                                                                                                                                                                                                                         | —                                                   |
| **Rule-SHARE-087** | *Only one machine syncs a test project at a time. A second is told who is syncing.*                                                                                                                                                                                                                                                                                                                                                   | —                                                   |
| **Rule-SHARE-088** | *The server must already be known to this machine. One that is not is refused rather than trusted.*                                                                                                                                                                                                                                                                                                                                   | —                                                   |
| **Rule-SHARE-089** | *The sync sends and takes in one gesture.*                                                                                                                                                                                                                                                                                                                                                                                            | Rule-SHARE-003                                      |
| **Rule-SHARE-090** | *A test case both sides changed is merged field by field. What the merge cannot settle is kept as it is here, and nothing is sent for it.*                                                                                                                                                                                                                                                                                            | —                                                   |
| **Rule-SHARE-091** | *Testin reads the test project again itself after a sync, because it ignores its own writes.*                                                                                                                                                                                                                                                                                                                                         | —                                                   |
| **Rule-SHARE-092** | *A password is kept in the IDE's own password store. Never in `testin.yml`, never on a marker file, never in the log.*                                                                                                                                                                                                                                                                                                                | —                                                   |
| **Rule-SHARE-093** | *It is kept for one server and one account, so two servers, or two accounts on one, do not overwrite each other.*                                                                                                                                                                                                                                                                                                                     | —                                                   |
| **Rule-SHARE-094** | *The name it is kept under says which server and which account, and holds no part of the secret.*                                                                                                                                                                                                                                                                                                                                     | —                                                   |
| **Rule-SHARE-095** | *A password the tester has just typed is preferred over one kept from before.*                                                                                                                                                                                                                                                                                                                                                        | —                                                   |
| **Rule-SHARE-096** | *What the field merge cannot settle is kept as it is here, and nothing is sent for it until the tester answers.*                                                                                                                                                                                                                                                                                                                      | —                                                   |
| **Rule-SHARE-097** | *The tester is asked only about the fields that really disagree.*                                                                                                                                                                                                                                                                                                                                                                     | —                                                   |
| **Rule-SHARE-098** | *The answers are sent on the same sync, if nobody else has taken the lock in the meantime.*                                                                                                                                                                                                                                                                                                                                           | —                                                   |
| **Rule-SHARE-099** | *A sync never deletes anything on this machine on its own. It asks.*                                                                                                                                                                                                                                                                                                                                                                  | —                                                   |
| **Rule-SHARE-100** | *Only files this machine has not touched since are offered for removal.*                                                                                                                                                                                                                                                                                                                                                              | —                                                   |
| **Rule-SHARE-101** | *Keeping them sends them back to the server on the next sync, so the question is asked once.*                                                                                                                                                                                                                                                                                                                                         | —                                                   |
| **Rule-SHARE-111** | *The password box shows dots rather than the characters typed into it.*                                                                                                                                                                                                                                                                                                                                                               | —                                                   |
| **Rule-SHARE-113** | *A secret the server refuses is forgotten, and the account window opens again.*                                                                                                                                                                                                                                                                                                                                                       | —                                                   |
| **Rule-SHARE-114** | *An answer is about the version of the test case the tester was shown.*                                                                                                                                                                                                                                                                                                                                                               | —                                                   |
| **Rule-SHARE-115** | *A file fetched from the server is recorded as agreed only once it is on this machine.*                                                                                                                                                                                                                                                                                                                                               | —                                                   |
| **Rule-SHARE-116** | *When this machine cannot save its record of what it agrees with the server, a notification that stays says so.*                                                                                                                                                                                                                                                                                                                      | —                                                   |
| **Rule-SHARE-117** | *What moved is recorded even when the sync stops part way.*                                                                                                                                                                                                                                                                                                                                                                           | —                                                   |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone              | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 1**  | An empty folder, file name or format moved the cursor and said nothing, so the export dialog read as a button that does not work. The empty box's own hint turns red now and takes the cursor, the way every dialog on the framework already did. Fixed 9 September 2026, [#251](https://github.com/MutebAlMughyiri/test-in/issues/251)                                                                                                                                          |
| **Difference 19** | Both Git entries were left out of the menu when the Git plugin was missing, so the menu had a different shape in two IDEs and nothing said why. They are in the menu in every IDE now, grayed and reading *(needs the Git plugin)*, so the reason is on the entry (Rule-SHARE-105). Fixed 9 September 2026, [#273](https://github.com/MutebAlMughyiri/test-in/issues/273)                                                                                                        |
| **Difference 3**  | Escape on a merge question ended the whole sync and threw away every answer already given. Fixed 7 September 2026, [#258](https://github.com/MutebAlMughyiri/test-in/issues/258)                                                                                                                                                                                                                                                                                                 |
| **Difference 4**  | A merged file Git would not stage was reported only to the log. Fixed 7 September 2026, [#259](https://github.com/MutebAlMughyiri/test-in/issues/259)                                                                                                                                                                                                                                                                                                                            |
| **Difference 5**  | An import that failed part way left what it had written and said nothing about how much. Fixed 7 September 2026, [#260](https://github.com/MutebAlMughyiri/test-in/issues/260)                                                                                                                                                                                                                                                                                                   |
| **Difference 7**  | Exporting a package walked only one level down, so nested test sets contributed nothing. Fixed 7 September 2026, [#262](https://github.com/MutebAlMughyiri/test-in/issues/262)                                                                                                                                                                                                                                                                                                   |
| **Difference 8**  | A test case file that would not read was dropped from an export in silence. Fixed 7 September 2026, [#263](https://github.com/MutebAlMughyiri/test-in/issues/263)                                                                                                                                                                                                                                                                                                                |
| **Difference 10** | **No Group** was offered in the picker and thrown away on the way back in, because the label reached `Group.valueOf` and was read as an unknown group. Fixed 9 September 2026, [#265](https://github.com/MutebAlMughyiri/test-in/issues/265)                                                                                                                                                                                                                                     |
| **Difference 15** | A message about none read *Exported 0*. A count of none says nothing now. Fixed 9 September 2026, [#269](https://github.com/MutebAlMughyiri/test-in/issues/269)                                                                                                                                                                                                                                                                                                                  |
| **Difference 13** | Choosing a file no format could read did nothing and said nothing. It names the file and the kinds Testin reads. Fixed 9 September 2026, [#267](https://github.com/MutebAlMughyiri/test-in/issues/267)                                                                                                                                                                                                                                                                           |
| **Difference 17** | A refusal naming which nodes can be imported into, on nodes where the menu entry was already gray. The refusal is gone; the gray entry says it. Fixed 9 September 2026, [#271](https://github.com/MutebAlMughyiri/test-in/issues/271)                                                                                                                                                                                                                                            |
| **Difference 14** | A Git sync faded, a push stayed, a server sync faded. All three stay: a sync lands on its own time, so it is the message a tester comes back to. Fixed 9 September 2026, [#268](https://github.com/MutebAlMughyiri/test-in/issues/268)                                                                                                                                                                                                                                           |
| **Difference 2**  | `Enter` did nothing in the export, import and review dialogs, which declared only `Escape`. All three answer it now, and the report dialog with them. Fixed 9 September 2026, [#252](https://github.com/MutebAlMughyiri/test-in/issues/252)                                                                                                                                                                                                                                      |
| **Difference 6**  | The order and who changed the test case last were settled without asking and without saying so, so a tester did not know a choice had been made for them. They are still settled - neither is a question anybody can usefully answer about a merge - and the conflict dialog now names them and says how (Rule-SHARE-109). Fixed 10 September 2026, [#261](https://github.com/MutebAlMughyiri/test-in/issues/261)                                                                |
| **Difference 18** | The remote address had already been fixed under [#83](https://github.com/MutebAlMughyiri/test-in/issues/83); the email address had not, so Git recorded whatever it was handed and a name typed into that box was not found out until somebody read a commit and wondered who wrote it. Its shape is checked now, in the same dialog and with the same kind of sentence (Rule-SHARE-108). Fixed 10 September 2026, [#272](https://github.com/MutebAlMughyiri/test-in/issues/272) |
| **Difference 11** | Every keystroke in the source box parsed the whole file, and nothing said it was happening - so a large workbook left the dialog looking frozen, and a tester could not tell that from a file Testin could not read. It waits three tenths of a second after the last keystroke now, and the form says which file it is reading (Rule-SHARE-107). Fixed 10 September 2026, [#266](https://github.com/MutebAlMughyiri/test-in/issues/266)                                         |
| **Difference 9**  | An unreadable value was quietly replaced four different ways - a priority became the lowest, a group was dropped from the list, a date became blank, a status kept whatever the row had - so 200 rows whose priority column read High, Medium and Low all arrived at the lowest priority in silence. One answer now, and it is said once with a count (Rule-SHARE-106). Fixed 9 September 2026, [#264](https://github.com/MutebAlMughyiri/test-in/issues/264)                    |

---

## Not decided

**Question 2** — Should an import be able to update a test case that is already
there, rather than always adding a new one?

Question 1 asked whether an export should walk the whole subtree. It does (Rule-SHARE-013). Question 3 asked whether the
tester should be told which
fields were settled for them in a merge. They are, in the merge window (Rule-SHARE-109). Neither number is given to
anything else.

---

[Documentation](../README.md) › **Sharing work with the team**
