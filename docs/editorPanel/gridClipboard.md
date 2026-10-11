[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-018

# UC-EDITOR-PANEL-018: Copy, cut and paste the grid's cells

**As a** tester, **I want** the grid to behave like a spreadsheet, **so that** I can move a column of values between
Testin and Excel.

Inside the grid these three keys act on cells, not on whole test cases.

`Ctrl+C`, `Ctrl+X` and `Ctrl+V`, inside the grid.

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
- **Rule-EDITOR-PANEL-086** — Cells are copied as text with a tab between
  columns and a line break between rows, which is what a spreadsheet reads.
- **Rule-EDITOR-PANEL-087** — A value holding a tab, a line break or a quote is
  wrapped in quotes.
- **Rule-EDITOR-PANEL-088** — One value on the clipboard fills every selected
  cell. A block is laid from the top left cell.
- **Rule-EDITOR-PANEL-089** — A cell that cannot be typed into is skipped, for
  both cut and paste.
- **Rule-EDITOR-PANEL-226** — A cut or a paste over cells is one change, however
  many cells it touches. Each test case it changed is saved once, one message
  counts those test cases, and Ctrl+Z takes the whole of it back in one press.
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
- **Rule-EDITOR-PANEL-254** — Pasted text is read the way a spreadsheet writes
  it. A quote opens a quoted value only at the start of a value; a quote
  anywhere else is kept as typed. A byte-order mark at the start is ignored. An
  empty line is a row of empty values, so the rows under it land in the same
  cells they held in the spreadsheet.

## What the tester sees

This opens no screen. A copy changes nothing on screen at all. A cut empties the
cells it took, and a paste draws them with their new values.

A cut or a paste raises one small message at the bottom of the IDE, which fades.
It reads *Updated*, with the number of test cases the gesture changed after it - *Updated 3* for a block across three
rows, *Updated* for a whole row of one test
case. It is one step to undo: `Ctrl+Z` puts back every cell it changed (Rule-EDITOR-PANEL-226).

## Main flow

1. The tester drags across a block of cells in the grid.
2. The tester presses `Ctrl+C`.
3. The block goes on the clipboard as tab separated text.
4. The tester pastes it into a spreadsheet, and it lands as a table.
5. The tester copies a column back from the spreadsheet.
6. The tester selects the top cell in Testin and presses `Ctrl+V`.
7. The values are laid down the column, stopping at the last row.

## What Testin refuses

**If nothing is selected** — nothing happens.

**If the clipboard holds no text** — nothing happens.

**If a cell cannot be typed into** — it is skipped without a word, for a cut and
for a paste.

**If the block on the clipboard is bigger than the grid** — it is laid down as
far as the last row and the last column. The rest is dropped.

## These keys are the grid's

`Ctrl+C`, `Ctrl+X` and `Ctrl+V` mean cells in the grid, and test cases on the
cards. Copying a test case itself is the right-click menu - **Copy Test Case**, **Cut Test Case**, **Paste Test Case** -
and those entries mean the same thing in
both views. They carry no key, so `Ctrl+C` in a grid is always about cells.

---

[Documentation](../README.md) › [The editor panel](main.md)
