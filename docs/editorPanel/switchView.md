[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-002

# UC-EDITOR-PANEL-002: Switch between cards and a grid

**As a** tester, **I want** the same test cases as a table, **so that** I can compare many at once and correct them in
columns.

The grid is the same test cases drawn as a table. One row is one test case.

There is no key for this. The button is on the toolbar.

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
- **Rule-EDITOR-PANEL-016** — Only one of the two buttons is on the toolbar at a
  time. It is always the view the tester is not in.
- **Rule-EDITOR-PANEL-018** — The two views show the same rows, and the
  selection follows from one to the other.
- **Rule-EDITOR-PANEL-019** — Anything half typed in a grid cell is saved, not
  thrown away, before the grid is rebuilt.
- **Rule-EDITOR-PANEL-020** — The grid has one column for each field the tester
  chose to show, in a fixed order.
- **Rule-EDITOR-PANEL-229** — A grid that cannot be built is never shown in
  place of the page asked for. The cards are shown instead, holding that page,
  and a message says the grid could not be drawn and why.
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
┌────────────────────────────────────────────────────────────────────────────┐
│  #  | Description                | Expected Result       | Priority | Group│
├────────────────────────────────────────────────────────────────────────────┤
│  1  | Log in with a valid user   | The dashboard opens.  | High     | Smoke│
│  2  | Log in with a locked accou.| The account is refus. | Medium   |      │
│  3  | Log in with the wrong pass.| The password is refu. | High     | Smoke│
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The first column** — the test case's number in the test set. Clicking it
   selects the whole row.
2. **Every other column** — one field. Some can be typed into, some cannot.
3. **The rows** — as tall as their tallest wrapped cell. Text wraps rather than
   being cut off.

## Main flow

1. The tester presses the grid button on the toolbar.
2. Testin builds the table from the test cases on this page.
3. Each column is made as wide as its text needs, up to a limit.
4. The selection the cards had is carried across.
5. The keyboard moves into the table.
6. The button on the toolbar becomes the one that goes back to cards.

## What Testin refuses

**If the grid cannot be built** — the cards are shown instead, holding the
page asked for, and a message says the grid could not be drawn and why (Rule-EDITOR-PANEL-229).

## What is different in each view

|                       | Cards                            | Grid                                       |
|-----------------------|----------------------------------|--------------------------------------------|
| Reading one test case | Every field, one under the other | One row, cut to the column width           |
| Correcting a value    | `F2` or the field's letter       | Type straight into the cell                |
| Dragging to reorder   | Yes                              | No                                         |
| `Ctrl+C`              | Copies the details as text       | Copies the cells                           |
| Selecting             | One or more cards                | Cells, or whole rows from the first column |

---

[Documentation](../README.md) › [The editor panel](main.md)
