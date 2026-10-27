[Documentation](../README.md) › The settings page

# The settings page

The settings page is where Testin learns about this machine and this person:
where the test data lives, and who is using it.

|                     |                                                                                                        |
|---------------------|--------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The settings page                                                                                      |
| **Answers**         | What every setting does, where each one is kept, and what happens when one is wrong                    |
| **Numbering**       | Use cases are `UC-SETTING-001` to `UC-SETTING-012`. Rules are `Rule-SETTING-001` to `Rule-SETTING-047` |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                            |
| **Checked against** | `main` at `1270e599`, 20 September 2026. Every page in this part was read against the code             |
| **Written to**      | [How a document is written](../standard.md)                                                            |

---

## The use cases

|                    | What the tester does                                           | Why they do it                                            |
|--------------------|----------------------------------------------------------------|-----------------------------------------------------------|
| **UC-SETTING-001** | [Open the settings page](openSettings.md)                      | Reach every Testin setting in one place.                  |
| **UC-SETTING-002** | [Set the Testin folder](setTestinFolder.md)                    | Tell Testin where the test data lives, so the tree fills. |
| **UC-SETTING-003** | [Open the Testin folder on this machine](openTestinFolder.md)  | Look at the test files on disk.                           |
| **UC-SETTING-004** | [Give my name](setTesterName.md)                               | Put the tester's name on the work they do.                |
| **UC-SETTING-005** | [Give my role](setTesterRole.md)                               | Record the tester's job, though nothing reads it yet.     |
| **UC-SETTING-006** | [Set the folder that files are saved to](setDownloadFolder.md) | Save reports and exports to the same place every time.    |
| **UC-SETTING-007** | [Choose how much Testin writes to its log](setLogLevel.md)     | Turn the log up when something goes wrong.                |
| **UC-SETTING-008** | [Turn the shortcut hints off](hideShortcutHints.md)            | Make dialogs shorter once the keys are known.             |
| **UC-SETTING-011** | [Change the size of Testin's text](changeTextSize.md)          | Make Testin's text bigger or smaller.                     |

Choosing which test project a code project uses is not on this page. It is
written into a file the whole team shares, and it is
[UC-TREE-PANEL-004](../treePanel/chooseTestProject.md).

---

## What the page is for

Two things have to be true before Testin can show anything. It has to know which
folder on this machine holds the test projects, and each code project has to say
which test project it is about.

The two are kept apart on purpose, and the reason is who they belong to.

- **A setting belongs to this machine and this person.** The Testin folder, the
  tester's name, the download folder. None of them is committed, and each one is
  the same in every code project open in this IDE.
- **The choice of test project belongs to the team.** It is written into a file
  in the code repository, so a colleague who clones the repository gets it too.
  That file names no machine and no person.

**Two words, before the rules use them.**

- The **Testin folder** is the one folder on this machine that holds test
  projects. The page calls it that too.
- **This machine's settings** are one set of values shared by every code project
  open in this IDE.

---

## The page

The page is at **Settings**, then **Tools**, then **Testin**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin                                                                    │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   Testin folder:           [ Example: C:\Users\...\Testin   ] [...] [Open] │
│                                                                            │
│   Log level:               [ INFO                                     v ]  │
│                                                                            │
│   Tester name:             [                                            ]  │
│                                                                            │
│   Tester role:             [                                            ]  │
│                                                                            │
│   Default download folder: [                                     ] [...]   │
│                                                                            │
│   [x] Show keyboard shortcuts in dialogs                                   │
│                                                                            │
│   Everything here belongs to this machine and this person, and is never     │
│   committed. Which test project a repository is about is chosen in the      │
│   Testin tool window and kept on this machine. A repository's testin.yml,   │
│   when it has one, can name it and say how it is shared - Testin writes     │
│   it only when you press Save to testin.yml in the Testin panel.       (9)  │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Testin folder** — the folder holding every test project. The only
   field with a gray example in it.
2. **The browse button** — opens a folder chooser. Two fields have one.
3. **Open** — opens the Testin folder in the file manager. It is gray until the
   box names a folder that is really there.
4. **Log level** — how much Testin writes to its own log.
5. **Tester name** — stamped on everything this machine writes.
6. **Tester role** — stored, and read by nothing. That is difference 2 below.
7. **Default download folder** — where saving a report, an export or an import
   starts.
8. **Show keyboard shortcuts in dialogs** — the strip of key hints along the
   bottom of every Testin dialog.
9. **The gray note** — where a value belongs. It is the
   table below, said where a tester is looking for a setting rather than only
   in this document.

**OK**, **Apply** and **Cancel** belong to the IDE's own settings window, not to
this page.

**The page answers the IDE's settings search.** Typing a row's name in the
search box at the top of the settings window opens this page with that row
highlighted.

---

## Where each setting is kept

| Setting                                     | Where it lives                                                                                    | Committed                                 |
|---------------------------------------------|---------------------------------------------------------------------------------------------------|-------------------------------------------|
| Testin folder                               | This machine's settings                                                                           | No                                        |
| Log level                                   | This machine's settings                                                                           | No                                        |
| Tester name                                 | This machine's settings                                                                           | No                                        |
| Tester role                                 | This machine's settings                                                                           | No                                        |
| Default download folder                     | This machine's settings                                                                           | No                                        |
| Show keyboard shortcuts                     | This machine's settings                                                                           | No                                        |
| Which test project this repository is about | Chosen in the Testin tool window and kept on this machine; `testin.yml` can name one for everyone | No; the file is, when the team writes one |
| Where the test project is cloned from       | `testin.yml`, in the code repository, written by hand or by **Save to testin.yml**                | **Yes**                                   |

Nothing on this page is ever committed. That is the reason the page exists
rather than putting these values in `testin.yml`.

---

## Why it is built this way

**Where a value lives is decided by whose it is.** A value that differs
between machines goes here, so it cannot be committed by accident. What the
team agrees on goes in `testin.yml`, so a clone needs no setting up - written
by hand, or by **Save to testin.yml** in the Testin panel, and by nothing else.
The one thing in between, the test project a tester chose for a repository, is
kept on their machine and wins over the file until the file names a different
one (Decision-013).

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

|                  | The rule it breaks                                               | What a tester sees                                                                                                                                                                                                                                                                                                         |
|------------------|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 1** | Rule-SETTING-002 — nothing is checked, so nothing warns          | The Testin folder is checked now: refused when it is not a folder (Rule-SETTING-042), and when it is a partial path (Rule-SETTING-013). The rest of the page still is not: a tester name and a download folder are both stored exactly as typed. None of them can make the tree empty, which is why the folder went first. |
| **Difference 2** | Rule-SETTING-020 — a value this page stores is read by something | **Tester role** is stored and read by nothing at all. It is on no marker, no report and no message. It is reserved rather than dead: [#14](https://github.com/MutebAlMughyiri/test-in/issues/14) is what will read it.                                                                                                     |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone             | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 3** | The Testin folder was stored exactly as typed and trimmed later by the reader, so the stored value changed on its own at the next project open. Every field on the page is trimmed when it is stored now. Fixed 9 September 2026, [#239](https://github.com/MutebAlMughyiri/test-in/issues/239)                                                                                                                                                                                                                                                               |
| **Difference 4** | The export, report and import dialogs each carried a **Set as default folder** tick box that wrote this page's value. The box is gone and this page is the one writer, which retires UC-SHARE-023. Fixed 9 September 2026, [#240](https://github.com/MutebAlMughyiri/test-in/issues/240)                                                                                                                                                                                                                                                                      |
| **Difference 5** | It read Rule-SETTING-004 as a promise that every open code project re-reads the disk. That rule is about which *setting* causes a re-read, not which projects; which projects is Rule-SETTING-012, and it says the ones with a panel open. A project without one has read nothing to correct, and the folder is read where it is used, so it answers the new one the first time it asks. Not a defect, closed 9 September 2026, [#241](https://github.com/MutebAlMughyiri/test-in/issues/241) and [#77](https://github.com/MutebAlMughyiri/test-in/issues/77) |

**Retired.** A use case or a rule that is gone keeps its number forever, so an
issue that quotes one still leads somewhere and nothing is ever renumbered onto
it.

| Gone                 | Was                                                                                                                                                                                                                                                                                                                                                                                        | Read instead                                             |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| **Rule-SETTING-002** | *Nothing on this page is checked. A folder that does not exist is stored exactly as typed.* Retired 21 September 2026: `SettingsConfigurable.refusalOfTheRoot` refuses four kinds of impossible root, and every field is trimmed on the way in, so neither half was true                                                                                                                   | [Rule-SETTING-042](openSettings.md) and Rule-SETTING-013 |
| **Rule-SETTING-003** | *Nothing on this page raises a message when it is saved.* Retired 21 September 2026: the page refuses a folder that does not exist by throwing `ConfigurationException`, which is how the platform is told to hold the dialog open and print the message. [UC-SETTING-001](openSettings.md) stated this rule and then stated Rule-SETTING-042 eighteen lines below it, saying the opposite | [Rule-SETTING-042](openSettings.md)                      |
| **UC-SETTING-009**   | *Name my account on the team's server* - the account an SFTP sync connected as. Removed 19 September 2026, when Git became the only way a test project is shared ([#334](https://github.com/MutebAlMughyiri/test-in/issues/334))                                                                                                                                                           | —                                                        |
| **UC-SETTING-010**   | *Name the key file this machine offers* - the SSH key an SFTP sync proved this machine with                                                                                                                                                                                                                                                                                                | —                                                        |
| **Rule-SETTING-031** | *The account belongs to this machine and this person. It is never written into the file the team shares.*                                                                                                                                                                                                                                                                                  | —                                                        |
| **Rule-SETTING-032** | *An empty account means the tester has not said. The sync then asks.*                                                                                                                                                                                                                                                                                                                      | —                                                        |
| **Rule-SETTING-033** | *The sync can write this row too, so a tester who answers the sync's question never has to visit this page.*                                                                                                                                                                                                                                                                               | —                                                        |
| **Rule-SETTING-034** | *A key file named here is the way this machine proves who it is.*                                                                                                                                                                                                                                                                                                                          | —                                                        |
| **Rule-SETTING-035** | *An agent already holding identities is tried before the key file itself.*                                                                                                                                                                                                                                                                                                                 | —                                                        |
| **Rule-SETTING-036** | *Testin never asks for a key file's passphrase.*                                                                                                                                                                                                                                                                                                                                           | —                                                        |
| **UC-SETTING-012**   | *Put my company's logo on reports* - a **Company logo** row whose picture the web page, PDF and Word reports printed above their title. Removed 3 October 2026, the day it was added                                                                                                                                                                                                       | —                                                        |
| **Rule-SETTING-043** | *The web page, PDF and Word reports print the logo in their top-left corner, above the title, 30 points tall and in its own proportions. The spreadsheet carries none.*                                                                                                                                                                                                                    | —                                                        |
| **Rule-SETTING-044** | *With no file set, a missing file or one that is not a picture, a report has no logo and says nothing about it. The report is never refused for the logo's sake.*                                                                                                                                                                                                                          | —                                                        |

---

## Not decided

**Question 2** — Should the Testin folder be checked when it is typed? Every
other refusal in Testin is stated at the moment it happens.

**Settled.** Question 3 asked how to clear the default download folder from the
import or export dialogs. They no longer set it, so there is nothing to clear
from them: the box is gone and this page is the only writer.

**Settled.** Whether **Tester role** should exist was question 1 here. It is
answered on [the product page](../product.md): the field is reserved for
role-based permissions, which is what decides who may approve a test case and
who may remove a test project. It stays, unread, until
[#14](https://github.com/MutebAlMughyiri/test-in/issues/14) reads it. What is still open
there is the list of roles, and that free text cannot answer *may this person
approve*.

---

[Documentation](../README.md) › **The settings page**
