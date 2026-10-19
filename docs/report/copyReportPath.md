[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-003

# UC-REPORT-003: Copy the report's path

**As a** tester, **I want** the full path of the report on my clipboard, **so that** I can paste it into a ticket or a
chat message.

This copies the full location of the report Testin has just written.

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
- **Rule-REPORT-011** — Clicking the link makes the message go.
- **Rule-REPORT-013** — The whole path is copied, not the file name.
- **Rule-REPORT-015** — The same link is offered on every message about a file
  Testin wrote, so the gesture is the same for a report and for an export.

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
2. **The line under it** — *Saved successfully:*, then the file name. It names
   the file, not the folder.
3. **Copy path** — this use case. Clicking it copies the whole path.
4. **Open report** — opens the file. That is
   [UC-REPORT-002](openReport.md).
5. **The message** — it waits in the IDE's notification list. It does not fade.

## Main flow

1. Testin writes the report and shows the message.
2. The tester clicks **Copy path**.
3. The whole path goes on the clipboard.
4. The message goes.

## What Testin refuses

Nothing. Copying a path cannot fail. Testin shows no confirmation for it
either.

## If the message has already gone

The message waits in the IDE's notification list, so the tester can find it
again there. If the list has been cleared, the path can still be built by hand.
The report dialog named the folder, and the message named the file.

---

[Documentation](../README.md) › [Reports](main.md)
