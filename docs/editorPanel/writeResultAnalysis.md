[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-045

# UC-EDITOR-PANEL-045: Write the result analysis

**As a** tester, **I want** to say what the test run as a whole showed, **so
that** the report carries my judgment and not only the figures.

Four boxes, one for each run item status. What the tester writes goes into the
report.

There is no key for this. The button's tooltip reads **Result Analysis**.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, kept
  against the test set it is showing, and the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-189** — The analysis can be written only once the test run
  is **Completed**. A **Committed** test run shows it read-only: the same
  dialog, with nothing to save.
- **Rule-EDITOR-PANEL-190** — There are four sections, one for each run item
  status, each carrying its own count in the heading.
- **Rule-EDITOR-PANEL-191** — A section left blank is not saved, and does not
  appear in the report.
- **Rule-EDITOR-PANEL-192** — `Enter` in a box makes a new paragraph. A box
  takes text only: a screenshot on the clipboard pastes nothing. **Save** is a
  button.
- **Rule-EDITOR-PANEL-248** — A button that turns gray while the pointer is on
  it loses its hover look at once, so a control that has stopped working never
  looks ready to press.
- **Rule-EDITOR-PANEL-249** — A grid that held the keyboard when it was rebuilt
  holds it again afterward, so the next key lands in the grid rather than
  nowhere.
- **Rule-EDITOR-PANEL-250** — A grid row is drawn in its stripe, or in the
  selection color while it is selected, and in nothing else. The pointer passing
  over it changes nothing.
- **Rule-EDITOR-PANEL-251** — When the status bar is too narrow for everything,
  nothing on it gives way or is cut short: it scrolls sideways, as the toolbar
  does (Rule-EDITOR-PANEL-257), so the page size field can always be reached.
  Where there is room, the page arrows sit in the middle of the bar.
- **Rule-EDITOR-PANEL-252** — A badge's words are printed white or dark,
  whichever reads on its color, so a pale badge never carries white text.
- **Rule-EDITOR-PANEL-257** — An editor narrower than its toolbar or its status
  bar scrolls that bar sideways under the pointer, with the mouse wheel or a
  touchpad. No scrollbar is shown and the bar keeps its height, so nothing on it
  is cut off and the editor below does not move.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Result Analysis                                             │
├──────────────────────────────────────────────────────────────┤
│  Passed (10)                                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the passed test cases say about this test run     │  │
│  └────────────────────────────────────────────────────────┘  │
│  Failed (2)                                                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the failed test cases say about this test run     │  │
│  └────────────────────────────────────────────────────────┘  │
│  Blocked (0)                                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the blocked test cases say about this test run    │  │
│  └────────────────────────────────────────────────────────┘  │
│  Untested (0)                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the untested test cases say about this test run   │  │
│  └────────────────────────────────────────────────────────┘  │
│                                          [ Save ]            │
├──────────────────────────────────────────────────────────────┤
│  [k]  Tab Navigate       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **Each heading** — the run item status and how many test cases carry it.
2. **Each box** — three lines tall, with its own gray hint.
3. **Save** — a button, because `Enter` makes a paragraph here.

## Main flow

1. The tester finishes executing, and the test run becomes **Completed**.
2. The tester presses the **Result Analysis** button.
3. The dialog opens with four headings carrying the live counts.
4. The tester writes under **Failed**, explaining that both failures are the
   same defect.
5. The tester presses **Save**.
6. The test run is written to disk.
7. A message reads *Saved*.
8. The next report on this test run carries the section.

## What Testin refuses

**If the test run is neither Completed nor Committed** — the button is gray. Its
tooltip reads *Result Analysis is written once the test run is completed — it
is*, then the status. A **Closed** test run is refused as well.

**If the test run is Committed** — the dialog opens read-only, with no Save: the
analysis is part of the record (Rule-EDITOR-PANEL-189).

**If a section is left blank** — it is dropped, and the report leaves that
heading out.

---

[Documentation](../README.md) › [The editor panel](main.md)
