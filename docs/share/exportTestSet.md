[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-001

# UC-SHARE-001: Export one test set

**As a** tester, **I want** the test cases of a test set as a file, **so that** somebody without the IDE can read them
or review them.

To export is to write test cases out to a file. The file is a spreadsheet, or
plain text. No test case is changed.

There is no key for this. The menu entry is **Export**.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones can
  be canceled: none of them leaves anything the tester cannot see.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-007** — The rows are written in the order the editor shows them.
- **Rule-SHARE-008** — Only what the tester ticks is written.
- **Rule-SHARE-009** — Only the test cases inside the test set are read - the
  files ending in `.testcase` - so nothing else in the folder is exported.
- **Rule-SHARE-010** — The file is written where the tester chose, never under
  the Testin folder.
- **Rule-SHARE-011** — Exporting changes no test case.
- **Rule-SHARE-012** — A test set becomes one sheet, named after itself.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Export Test Cases                                           │
├──────────────────────────────────────────────────────────────┤
│  | Login |                                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │[x]| # | Description        | Expected Result | Priority│  │
│  │[x]| 1 | Log in with a val..| The dashboard.. | High    │  │
│  │[x]| 2 | Log in with a loc..| The account is..| Medium  │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  DESTINATION                                                 │
│  [ C:\Users\mtb\Downloads                         ] [ ... ]  │
│  FILE NAME                                                   │
│  [ Login                                          ]          │
│  FORMAT                                                      │
│  [ XLSX                                          v]          │
│                                                              │
│                                          [ Export ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Export        Escape Cancel                      │
└──────────────────────────────────────────────────────────────┘
```

1. **The tab** — named after the test set.
2. **The tick column** — every test case arrives ticked. The box in the heading
   ticks or unticks the whole tab.
3. **The other columns** — the 17 fields. Every column but the number can be
   typed into. Typing changes the file, and never the test case.
4. **Export** — writes the file. So does `Enter`.

## Main flow

1. The tester selects a test set and chooses **Export**.
2. A progress bar reads *Reading test cases in*, then the test set's name.
3. The **Export Test Cases** dialog opens, every test case ticked.
4. The tester unticks two, and corrects a typo in a third.
5. The tester picks a folder and a name, then presses **Export**. The format is
   four radios - Excel, JSON, CSV and HTML - and Excel is already picked.
6. The dialog closes. A bar reads *Exporting*, then the count, then *test
   cases to*, then the file name.
7. One message, titled *Exported 8*, names the file and carries **Open file**
   and **Copy path**. It stays in the notification log.

## What Testin refuses

**If more than one node is selected** — **Export** is gray.

**If the node cannot hold test cases** — **Export** is gray. Only a test set, a
test set package and the **Test Cases** folder can be exported.

**If the folder holds no test cases** — a message titled **Export Empty** reads *No test cases found.*

**If the tester unticks every test case** — a message titled **Export Empty**
reads *Select at least one test case to export.* The dialog stays open.

**If the file name is empty** — its gray hint turns red and reads *Name the
file*, and the box takes the cursor. Nothing is written and the dialog stays
open. The file name is checked first, so it is the box that speaks when more
than one is empty.

**If the folder is empty** — the same, reading *Choose a folder*.

**If the file cannot be written** — a message titled **Export Failed** carries
the reason.

**If one test case file cannot be read** — the file is named before anything is
written, and the tester chooses whether to export anyway.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
