[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-002

# UC-REPORT-002: Open the report that was just made

**As a** tester, **I want** to see the document straight away, **so that** I can check it before sending it, without
hunting through a folder.

This opens the report Testin has just written, from the message about it.

There is no key for this. The link is on the message.

## Rules

- **Rule-REPORT-001** — A report is about exactly one test run.
- **Rule-REPORT-002** — Every format reads the same figures, so two reports on
  one test run cannot disagree.
- **Rule-REPORT-003** — A report is written after the dialog closes, under a
  progress bar the tester can cancel. Stopping it leaves no file behind: the
  report is written in one go at the end.
- **Rule-REPORT-004** — A report is written where the tester chose. It never
  lands under the Testin folder.
- **Rule-REPORT-005** — The project named in a report is the test project, not
  the code project the IDE has open.
- **Rule-REPORT-019** — Every format takes its words from one place. The
  headings, the column names and the Actual result label read the same in the
  web page, the PDF, the Word document and the spreadsheet, so no two formats
  name one thing two ways. A value that is not there reads — in every format,
  and the failed test cases' columns stand in one order, bug priority then bug
  severity.
- **Rule-REPORT-010** — The link is on the message that says the report was
  written, and the message stays in the IDE's notification list.
- **Rule-REPORT-011** — Clicking the link makes the message go.
- **Rule-REPORT-012** — The file is handed to whatever application on this
  machine claims it.

## The screen

There is no dialog. The whole of this use case is one link on the message
Testin shows when a report has been written.

```
┌──────────────────────────────────────────────────────────────┐
│  PDF Report Generated                                  [ X ] │
│                                                              │
│  Saved successfully: TestRun_Demo_cycle2_07-09-2026.pdf      │
│                                                              │
│  Open report    Copy path                                    │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the format's name, then *Report Generated*.
2. **The line under it** — *Saved successfully:*, then the file name.
3. **Open report** — this use case. Clicking it opens the file.
4. **Copy path** — puts the whole path on the clipboard. That is
   [UC-REPORT-003](copyReportPath.md).
5. **The message** — it waits in the IDE's notification list. It does not fade.

## Main flow

1. Testin writes the report and shows the message.
2. The tester clicks **Open report**.
3. The message goes.
4. The computer opens the file. It uses the program that owns that kind of
   file, such as a PDF reader for a PDF.

## What Testin refuses

**If the file is no longer there** — a message titled **Open Error** reads *The
file does not exist.*

**If this machine cannot open files that way** — a message titled **System
Error** reads *Opening a file is not supported on this system.*

**If the open fails** — a message titled **Execution Error** reads *Failed to
open the file:* and then the reason.

**If the report is a web page** — it opens in the browser, which is where a web
page is read. That holds whether the file was made as a report or as an export:
one kind of file, one answer.

---

[Documentation](../README.md) › [Reports](main.md)
