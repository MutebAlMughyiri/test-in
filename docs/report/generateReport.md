[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-001

# UC-REPORT-001: Generate a report on a test run

**As a** tester, **I want** one test run written out as a document, **so that** I can attach it to a ticket or send it
to someone with no IDE.

This turns one test run into a file: a PDF, a Word file, a web page or a
spreadsheet.

A test run can be reported on in four of its five statuses: **Created**, **Assigned**, **Completed** and **Closed**.
**In Progress** is the one that
cannot.

`Ctrl+P` on the selected test run, `Ctrl+Alt+P` on a Mac.

## Rules

- **Rule-REPORT-001** — A report is about exactly one test run.
- **Rule-REPORT-002** — Every format reads the same figures, so two reports on
  one test run cannot disagree.
- **Rule-REPORT-003** — A report is written after the dialog closes, under a
  progress bar the tester can cancel. Stopping it leaves no file behind: the
  report is written in one go at the end.
- **Rule-REPORT-004** — A report is written where the tester chose. It never
  lands under the Testin folder.
- **Rule-REPORT-016** — While a test run is **In Progress**, every way of asking
  for a report on it is gray: the toolbar button, the tree entry and `Ctrl+P`.
  The button is still there, with the reason in its tooltip — Testin does not
  hide a control that cannot work just now.
- **Rule-REPORT-005** — The project named in a report is the test project, not
  the code project the IDE has open.
- **Rule-REPORT-019** — Every format takes its words from one place. The
  headings, the column names and the Actual result label read the same in the
  web page, the PDF, the Word document and the spreadsheet, so no two formats
  name one thing two ways.
- **Rule-REPORT-006** — The file name is filled in already. It names the test
  project, the test run, the date and the time.
- **Rule-REPORT-007** — Spaces and special characters are taken out of the
  project and test run names in the file name.
- **Rule-REPORT-008** — A part of the name that is empty is left out, rather
  than leaving a gap.
- **Rule-REPORT-009** — The dialog closes before the work starts.
- **Rule-REPORT-017** — No report shows a screenshot pasted with a failure. The
  web page prints the error's text; the PDF, the Word document and the
  spreadsheet print no error at all.
- **Rule-REPORT-018** — The PDF says what it cannot print. Its font holds Latin
  letters only, so Arabic, Hindi, Cyrillic or Chinese text is left out of it;
  when that happens a message says how much text lost characters, shows one of
  them, and says the Word and HTML reports keep them.
- **Rule-REPORT-020** — The spreadsheet has two sheets. Overview holds what the
  other formats print before their tables: the overview, the execution summary
  and the result analysis. Test Cases holds the column names and one row for
  each test case, and nothing else, each row filled with the color of its run
  item status.
- **Rule-REPORT-021** — Every row names its test case the way the test run shows
  it, in every format. A test case deleted since the test run was made is named
  as the test run names it, Deleted test case and its id; no row is left blank
  or printed as a dash.
- **Rule-REPORT-022** — Words printed on a run item status's color, in every
  format, are white or near-black, whichever contrasts more with that color.
- **Rule-REPORT-023** — A line break in what the tester wrote stays a line break
  in every format, so nothing is run together onto one line.
- **Rule-REPORT-024** — In the web page and the PDF, a test case table's columns
  are as wide as what they hold: the number and the run item status take only
  their own width, and the description gets the rest.
- **Rule-REPORT-025** — A run item status has one color in every report and
  every format. Failed is the same red on its count tile, its table heading, its
  result analysis line and a bug priority or severity that alarms; passed,
  blocked and untested the same. The color is the run item status's own, so
  changing it once changes every report.
- **Rule-REPORT-026** — A report lists a test run's run items in the test run's
  order, the order its editor draws them in.
- **Rule-REPORT-027** — A run item is reported by its default execution, as
  before. When another of its executions failed or was blocked, one line under
  it says so - *Failed in another execution*, *Blocked in 2 other executions* -
  in the same words in every format.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Generate Report                                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Save to      [ C:\Users\mtb\Downloads              [...] ]  │
│                                                              │
│  File name    [ TestRun_Demo_cycle2_07-09-2026_02-14-33PM ]  │
│                                                              │
│  Format       [ PDF                                      v ] │
│                                                              │
│                                        [ Generate ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Generate   Escape Cancel                         │
└──────────────────────────────────────────────────────────────┘
```

1. **Save to** — where the file goes. It opens on the folder the settings page
   names.
2. **File name** — filled in already, and can be changed.
3. **Format** — **Excel**, **HTML**, **PDF** or **Word**. **PDF** is chosen.
4. **Generate** — writes the file. So does `Enter`.

The folder this dialog opens on is set on the settings page, and only there. It
is [UC-SETTING-006](../setting/setDownloadFolder.md).

## Main flow

1. The tester selects a test run in the tree and presses `Ctrl+P`.
2. The **Generate Report** dialog opens with the name already filled in.
3. The tester picks a folder. The format is four radios - Excel, HTML, PDF and
   Word - and PDF is already picked.
4. The tester presses **Generate**, or `Enter`.
5. The dialog closes.
6. A progress bar reads *Generating the*, the format, then *report for*, then
   the test run's name.
7. Testin reads the test run and every test case it names. Then it writes the
   file.
8. A message appears. Its title is the format and the words *Report Generated*.
   It reads *Saved successfully:* and then the file name.
9. The message carries two links, **Open report** and **Copy path**.

## The three ways in

| The tester does this        | Where                                                                             |
|-----------------------------|-----------------------------------------------------------------------------------|
| Presses `Ctrl+P`            | On a selected test run in the tree, or in a test run editor                       |
| Chooses **Generate Report** | The tree's menu, or the test run editor's menu                                    |
| Presses the report button   | The test run editor's toolbar. Its tooltip reads **Generate Test Summary Report** |

## What Testin refuses

**If the PDF cannot print some of the text** — Arabic, Hindi, Cyrillic or
Chinese, which its font does not hold — the PDF is still written, without
those characters. A notification titled *The PDF left some text out* says how
many texts lost characters and shows one. It says the Word and HTML reports
keep them (Rule-REPORT-018).

**If the test run is still In Progress** — every way of asking is gray: the
toolbar button, the tree entry and `Ctrl+P`. The button's tooltip reads *A
report is written once the test run has stopped — it is In Progress*. A report
is about what a test run recorded, and a test run still going is still recording (Rule-REPORT-016).

**If the selection is not a test run** — **Generate Report** is gray in the tree
menu, and its tooltip reads *Select a test run to report on.* A report is about
one test run, and nothing else.

**If the file name is empty** — nothing is written. The file name box turns red
and reads *Name the file*, the cursor moves into it, and the dialog stays open.

**If the folder is empty** — the same, with the folder box reading *Choose a
folder*.

**If the file cannot be written** — a message titled *Could not generate the*,
the format, *report* gives the reason under it.

## Where the plugin breaks its own rules

Nothing on this dialog. The one left in this part is the PDF's font. That is
difference 8 on
[the reports page](main.md#where-the-plugin-breaks-its-own-rules), and it is
under *What Testin refuses* above.

---

[Documentation](../README.md) › [Reports](main.md)
