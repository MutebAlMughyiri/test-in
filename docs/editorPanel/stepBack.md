[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-026

# UC-EDITOR-PANEL-026: Step back

**As a** tester, **I want** one key that undoes whatever state I am in, **so that** I can get back to a plain list
without thinking about how I got
here.

One key, one step back. The tester does not have to remember which state they
are in.

`Escape`.

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
- **Rule-EDITOR-PANEL-114** — One press does one step. The steps are always in
  the same order.
- **Rule-EDITOR-PANEL-115** — In the grid, an open cell takes the press first,
  and only cancels the edit.
- **Rule-EDITOR-PANEL-116** — In the search box, the press returns the keyboard
  to the list and leaves the text.
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
- **Rule-EDITOR-PANEL-262** — Test cases waiting on the clipboard, cut or
  copied, are one step. The press drops them and empties the clipboard, so
  nothing is left waiting to be pasted, as in the tree.

## What the tester sees

This opens no screen, and it never closes the editor tab. One press undoes one
thing on screen: an open cell, test cases cut or copied and waiting to be
pasted, the details panel, or the selection.

Nothing is said, and no message appears. A press with nothing left to undo does
nothing at all.

## The order of the steps

| Press                      | What it does                       |
|----------------------------|------------------------------------|
| A cell is open for editing | Cancels the edit                   |
| A cut or a copy is waiting | Drops it and empties the clipboard |
| The details panel is open  | Closes it                          |
| Anything else              | Clears the selection               |

## Main flow

1. The tester cuts three test cases, opens the details panel, and changes their
   mind.
2. The tester presses `Escape`. The cut is dropped and the cards stop being
   faded.
3. The tester presses `Escape`. The details panel closes.
4. The tester presses `Escape`. The selection is cleared.

## What Testin refuses

Nothing. Every press does the next step, and a press with nothing left to do
does nothing.

## What it does not close

The editor tab. `Escape` never closes an editor, only the things inside it.

---

[Documentation](../README.md) › [The editor panel](main.md)
