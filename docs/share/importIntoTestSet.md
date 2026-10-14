[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-005

# UC-SHARE-005: Import into a test set

**As a** tester, **I want** to bring in a sheet of test cases somebody wrote
elsewhere, **so that** work done in a spreadsheet does not have to be typed
again.

To import is to read test cases out of a file and add them to a test set. The
test cases already in the test set are not touched.

There is no key for this. The menu entry is **Import**.

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
- **Rule-SHARE-024** — Every imported test case is new, with a new identity. An
  import never overwrites a test case that is already there.
- **Rule-SHARE-025** — Imported test cases land after everything already in the
  test set. Nothing already there is touched.
- **Rule-SHARE-026** — An imported test case keeps the audit the file carried,
  rather than being stamped as written now.
- **Rule-SHARE-027** — Every sheet in the file goes into this one test set.
- **Rule-SHARE-028** — The file is read as soon as the box holds a path Testin
  recognizes.
- **Rule-SHARE-029** — Thirteen columns are read. **Order**, **UUID**, **FQCN**, **Path** and **Status** are not.
- **Rule-SHARE-030** — Importing the same file twice makes two copies of
  everything.
- **Rule-SHARE-110** — A column heading is matched by the name Testin shows for
  that column and by the column's name in the code. Underscores and capitals
  do not matter, so a heading typed as *created at* finds **Created At**.
- **Rule-SHARE-121** — Created At and Updated At are read in the shape Testin
  exports them and in Excel's own date shape, so a sheet exported and imported
  unchanged keeps its dates. An empty cell leaves the date empty, and any other
  shape is refused and counted in the one message.
- **Rule-SHARE-124** — A CSV file is read the way a spreadsheet writes it. A
  quote opens a quoted value only at the start of a value; a quote anywhere else
  is kept as typed. A byte-order mark at the start is ignored, and a line with
  no value in it is skipped.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Import Test Cases                                           │
├──────────────────────────────────────────────────────────────┤
│  SOURCE                                                      │
│  [ C:\Users\mtb\Downloads\Login.xlsx            ] [ ... ]    │
│  Your file should hold these columns: Description,           │
│  Expected Result, Steps, Priority, ...                       │
│                                                              │
│  | Login |                                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │[x]| #  | Description       | Expected Result | Priority│  │
│  │[x]| 1  | Log in with a va. | The dashboard.. | High    │  │
│  │[x]| 2  | Log in with a lo. | The account i.. | Medium  │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│                                          [ Import ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Import        Escape Cancel                      │
└──────────────────────────────────────────────────────────────┘
```

1. **Source** — the file to read. A file chooser opens on its own as soon as
   the dialog opens, on the folder the settings page names.
2. **The gray lines** — the chosen format's note. It names the columns Testin
   reads.
3. **The tabs** — one for each sheet in the file. All of them go into this one
   test set.
4. **The table** — the test cases of the tab in front, every one ticked.
   [UC-SHARE-007](chooseWhatToImport.md) draws it.
5. **Import** — writes the test cases. So does `Enter`.

## Main flow

1. The tester selects a test set and chooses **Import**.
2. The **Import Test Cases** dialog opens, and a file chooser opens with it.
3. The tester picks a workbook and confirms.
4. Testin reads it at once, with no progress bar.
5. The preview fills with one tab for each sheet, every test case ticked.
6. The tester unticks what they do not want and corrects a few values.
7. The tester presses **Import**.
8. A bar reads *Importing*, then the count, then *test cases into*, then the
   test set.
9. Each test case is written, and the bar names it as it goes.
10. Test methods are generated in batches, and the bar counts them.
11. The test set's editor is closed and opened again, so the new test cases are
    in front of the tester.
12. A message reads *Imported 24*.

## What Testin refuses

**If more than one node is selected, or the node cannot hold test cases** — **Import** is gray.

**If nothing can be read out of the file** — a message titled **No Data** reads *No test cases found in the selected
file.*

**If the file cannot be read at all** — a message titled for the format, such
as **CSV Parse Error**, carries the reason. The preview stays empty.

**If the tester presses Import with nothing loaded** — a message titled **Import
Empty** reads *No data loaded from the selected file.*

**If the tester unticks every test case** — a message titled **Import Empty**
reads *Select at least one test case to import.*

**If the file is not one Testin can import** — nothing is read, and a message
titled **Cannot Be Imported** names the file and the kinds Testin reads:
*report.pdf is not a kind of file Testin can read. It reads .xls, .xlsx, .json,
.csv.*

**If the import fails part way** — a message titled **Import Failed** says how
many test cases were written before it stopped, and that they are still there.
It says "at least", because the test set being written when it stopped may have
got part of the way through.

**If one test case cannot be written** — a message reads *unable to write
content:* and the reason. The rest are imported. That one is not counted in the
message that says how many were, and no test method is written for it.

**If the IDE has no Java plugin** — the test cases are imported and no test
methods are generated. A message says so once for the whole code project.

## What is done with a row Testin cannot read

| The row                                     | What happens                                                                                                           |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| A column is missing, or the row stops early | Those fields are blank                                                                                                 |
| Every value is blank                        | The row is skipped without a word                                                                                      |
| A value cannot be read                      | It is refused, and the test case keeps what it had. One message counts them. See [UC-SHARE-007](chooseWhatToImport.md) |
| The sheet has no header row                 | The sheet contributes nothing                                                                                          |

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
