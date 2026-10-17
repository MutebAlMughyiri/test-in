[Documentation](../README.md) › Reports

# Reports

A report is a document about one test run: how many test cases it held, how many
passed, and what the failures were. It is written to a file, so it can be sent
to somebody who does not have the IDE.

|                     |                                                                                                                                                                                                                                                               |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | Reports                                                                                                                                                                                                                                                       |
| **Answers**         | How a report is made, what it contains, and what each format holds                                                                                                                                                                                            |
| **Numbering**       | Use cases are `UC-REPORT-001` to `UC-REPORT-003`. Rules are `Rule-REPORT-001` to `Rule-REPORT-027`                                                                                                                                                            |
| **Retired**         | `Rule-REPORT-014` said what `Rule-REPORT-011` says - clicking the link makes the message go. Retired 4 October 2026 (#392); Copy the report's path now carries Rule-REPORT-011 itself. Read Rule-REPORT-011 instead. The number is not given to anything else |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                                                                                                   |
| **Checked against** | `main` at `1270e599`, 20 September 2026. Every page in this part was read against the code                                                                                                                                                                    |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                   |

---

## The use cases

|                   | What the tester does                                 |                                                      |
|-------------------|------------------------------------------------------|------------------------------------------------------|
| **UC-REPORT-001** | [Generate a report on a test run](generateReport.md) | Send a test run's results to someone who has no IDE. |
| **UC-REPORT-002** | [Open the report that was just made](openReport.md)  | Check the document before sending it.                |
| **UC-REPORT-003** | [Copy the report's path](copyReportPath.md)          | Paste the file's location into a ticket or a chat.   |

---

## What reports are for

A test run holds everything: which test cases were covered, what each one
recorded, who ran it and how long it took. None of that leaves the IDE on its
own.

A report is that test run written out as a document. It is the thing attached to
a ticket, mailed to a manager, or kept as the record that a release was tested.

**Two words, before the rules use them.**

- A **run item status** is what one test run recorded against one test case:
  **Passed**, **Failed**, **Blocked**, **Pending**, **Untested** or **Removed**.
- The **result analysis** is what the tester writes about the test run as a
  whole, once it is completed. It is written in the test run editor, and it
  appears in the report.

---

## Every key

| Key                             | What it does                                | The page that owns it              |
|---------------------------------|---------------------------------------------|------------------------------------|
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac | Generates a report on the selected test run | [UC-REPORT-001](generateReport.md) |

The key works on the tree, and on the list of test cases inside a test run
editor. It is not in the IDE's keymap, so it cannot be changed there.

---

## The four formats

| Format    | What it is for                                                                          |
|-----------|-----------------------------------------------------------------------------------------|
| **PDF**   | The one to attach to a ticket. The starting choice                                      |
| **Word**  | The one to edit before sending                                                          |
| **HTML**  | The one to open in a browser. It carries a light and dark switch                        |
| **Excel** | The one to work with in a spreadsheet. Its test cases are one table, to sort and filter |

The first three carry the same content. The spreadsheet carries the same
overview and analysis on a sheet of their own, and is described below.

## What a report contains

The PDF, the Word document and the web page each hold these, in this order.

1. **The heading** — the words **TEST SUMMARY REPORT**, the test project's name,
   the platform and component, the test run's own name, and a line reading *Confidential — QA Test Execution Summary*.
2. **Report Overview** — a table of two columns. It holds the project, the test
   run, the test type, the change log, the commit, the platform and component,
   the language, the browser and the device type. It also holds everyone who
   recorded a run item status, when execution started, when it ended, how long
   it took, and the test run status. A row nobody answered is left out. The
   commit is the one row that stays when nobody answered it, and reads *n/a*.
3. **Execution Summary** — one sentence naming the test run and saying how many
   test cases it holds, how many were executed and what share passed. All four
   formats open it with the same sentence. Under it a row of
   figures: **Total Test Cases**, **Passed**, **Failed**, **Blocked**, **Pending** or **Untested**, **Removed** and
   **Pass Rate**. **Removed** appears only when
   the test run has any. A test case deleted before it was judged is counted
   only under **Removed**, so it is left out of **Pass Rate**; one deleted after
   its run item status is counted under that run item status, as the test run
   recorded it. The test cases nobody has judged read **Pending** while the test
   run is open and **Untested** once it is signed off, in the figures, the
   analysis and the table alike, as in the test run editor (Rule-EDITOR-PANEL-176).
4. **Result Analysis** — what the tester wrote about the test run, under a
   heading for each run item status with its count. The whole section is left
   out when the tester wrote nothing.
5. **One section for each run item status that has test cases**, in this order:
   failed, passed, blocked, pending or untested, removed. Each is a numbered table of test
   cases. A run item status with no test cases gets no section.
6. **The footer** — one line saying the document was generated by Testin,
   with the Testin logo beside the name. It carries no date.

The failed table is the only one with more than two columns. It also carries the
bug priority and the bug severity, and under each test case the actual result.
The web page also prints the whole error under it, as text; the PDF and the Word
document print no error. No report shows a screenshot pasted with a failure: the
issue a failure was reported as holds them. (Rule-REPORT-017)

A test case reported as a bug shows its issue right after the actual result, as *(#12)*. Clicking it opens the issue. A
test case with no issue shows nothing
there. (UC-VIEW-PANEL-016)

## What the spreadsheet report contains

Two sheets. (Rule-REPORT-020)

**Overview** — the words **TEST SUMMARY REPORT**, then the same three sections
the other formats open with, as a caption and a value on each row: **Report
Overview**, with the configuration and when execution started, ended and how
long it took; **Execution Summary**, with its sentence and the same figures;
and **Result Analysis**, left out when the tester wrote nothing.

**Test Cases** — a header row and one row for each test case, nothing above it,
so the sheet sorts and filters as one list. Each row is filled with the color
that heads the table for its run item status in the other formats: red for
failed, green for passed, amber for blocked, gray for untested and removed.

The last column, **Bug Issue**, holds the issue a test case was reported as, as *#12*. Clicking it opens the issue. It
is empty for a test case with no issue.
There is no column for the error, and the test cases are one table rather than
one per run item status.

---

## Where the plugin breaks its own rules

|                  | The rule it breaks                                    | What a tester sees                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|------------------|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 8** | Rule-REPORT-002 — every format reads the same figures | The PDF drops any text outside Latin-1. An Arabic test case description comes out of the PDF as an empty cell, while the HTML, Word and Excel reports print it. It is no longer silent: a message says what was left out and which formats keep it (Rule-REPORT-018). The three fonts the PDF is built from are the standard Helvetica faces, which carry Western encoding only, so the fix is a Unicode font shipped inside the plugin — a decision about what Testin distributes, not a line to change. [#326](https://github.com/MutebAlMughyiri/test-in/issues/326), *The PDF report prints Latin only, and its library is AGPL inside an Apache 2.0 plugin*. |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone              | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 1**  | An empty folder, file name or format moved the cursor and said nothing, so the dialog read as a button that does not work. The empty box's own hint turns red now and takes the cursor — *Choose a folder*, *Name the file*. The format cannot be empty any more: it is radios that open on one (Rule-INTERNAL-107). Fixed 9 September 2026, [#251](https://github.com/MutebAlMughyiri/test-in/issues/251)                                                              |
| **Difference 2**  | `Enter` did nothing in the report dialog, so the tester had to press **Generate**. `Enter` generates the report now, and the status bar says so. Fixed 9 September 2026, [#252](https://github.com/MutebAlMughyiri/test-in/issues/252)                                                                                                                                                                                                                                  |
| **Difference 3**  | The report button was offered on a test run still being written, so the document described a state that had already moved on. Every way of asking is gray while a test run is **In Progress**, with the reason in the tooltip (Rule-REPORT-016). The row also overstated the defect: the button could never be unavailable, because it is only ever built with a test run editor. Fixed 9 September 2026, [#253](https://github.com/MutebAlMughyiri/test-in/issues/253) |
| **Difference 4**  | A test run nobody executed printed **Execution Started** and **Execution Ended** as empty rows, because three rows went round the helper that drops a blank. Fixed 9 September 2026, [#254](https://github.com/MutebAlMughyiri/test-in/issues/254)                                                                                                                                                                                                                      |
| **Difference 5**  | The message named the format in capitals — *WORD Report Generated*. Formats read as words now, in the message and in the dialog alike. Fixed 9 September 2026, [#255](https://github.com/MutebAlMughyiri/test-in/issues/255)                                                                                                                                                                                                                                            |
| **Difference 6**  | A web page opened in whatever application claimed the file when it was a report, and in the browser when it was an export. Every web page goes to the browser now. Fixed 9 September 2026, [#256](https://github.com/MutebAlMughyiri/test-in/issues/256)                                                                                                                                                                                                                |
| **Difference 7**  | The report, export and import bars could not be canceled. All three can be. Fixed 9 September 2026, [#257](https://github.com/MutebAlMughyiri/test-in/issues/257)                                                                                                                                                                                                                                                                                                       |
| **Difference 9**  | The web page ran a test case description written on several lines together onto one, while the PDF, the Word document and the spreadsheet kept its line breaks (Rule-REPORT-023). It keeps them now. Fixed 2 October 2026, [#325](https://github.com/MutebAlMughyiri/test-in/issues/325)                                                                                                                                                                                |
| **Difference 10** | The spreadsheet headed the test case column **Description**, where the web page, the PDF and the Word document say **Test Case** (Rule-REPORT-019). It says **Test Case** now. Fixed 2 October 2026, [#325](https://github.com/MutebAlMughyiri/test-in/issues/325)                                                                                                                                                                                                      |

---

## Not decided

**Question 2** — A report can only be made on a test run. Nobody has decided
whether a report on a test set, or on a whole test project, is wanted.

---

[Documentation](../README.md) › **Reports**
