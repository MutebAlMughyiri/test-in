[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-013

# UC-EDITOR-PANEL-013: Redo a change

**As a** tester, **I want** to put back something I just took back, **so that** an undo pressed by mistake costs
nothing.

It only works right after an undo. Any new change clears the list.

`Ctrl+Y`.

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
- **Rule-EDITOR-PANEL-071** — The menu entry says what the next press would put
  back.
- **Rule-EDITOR-PANEL-072** — Making any new change clears everything waiting to
  be put back.
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

## What the tester sees

This opens no screen. The change is made again, and the test cases it touched
are drawn again with the new values.

A small message then appears at the bottom of the IDE and fades. It reads *Redone*.

## Main flow

1. The tester presses `Ctrl+Z` and the change is taken back.
2. The tester presses `Ctrl+Y`.
3. The change is made again.
4. A message reads *Redone*.

## What Testin refuses

**If there is nothing to put back** — the menu entry is gray and the key does
nothing.

**If the tester has made any change since undoing** — there is nothing to put
back. Making a new change clears the list.

**If something else has written those test cases since** — the same message the
undo shows, and nothing is changed.

Everything else about the history is on
[UC-EDITOR-PANEL-012](undoChange.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
