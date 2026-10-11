[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-025

# UC-EDITOR-PANEL-025: Open the details panel

**As a** tester, **I want** the whole of one test case beside the list, **so that** I can read its steps while the list
stays where it is.

The panel opens beside the list, not on top of it. The list stays where it was.

`Enter` on the selection.

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
- **Rule-EDITOR-PANEL-112** — Opening the details panel says nothing.
- **Rule-EDITOR-PANEL-113** — Once the panel is open, moving the selection fills
  it again.
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

This opens no screen of its own. The view panel opens on the right of the IDE,
with the **Details** tab in front, and it shows the whole of the selected test
case.

The list is not moved and nothing is written. No message appears, because
nothing changed.

## The four ways in

| The tester does this                                    | Where       |
|---------------------------------------------------------|-------------|
| Presses `Enter`                                         | The cards   |
| Double-clicks a card                                    | The cards   |
| Chooses **View Test Case Details**                      | Either menu |
| Presses `Enter`, or double-clicks, on the number column | The grid    |

## Main flow

1. The tester selects a card and presses `Enter`.
2. The view panel opens on the right, if it was closed.
3. It shows the whole test case, with the Details tab in front.
4. The tester moves down the cards, and the panel follows.

The panel itself is a part of Testin of its own, and is
[the view panel](../viewPanel/main.md).

## What Testin refuses

**If nothing is selected** — **View Test Case Details** is gray, reading *Select
a test case first.*, and `Enter` does nothing.

**If `Enter` is pressed on any grid column but the number** — the cell opens for
editing instead, or nothing happens.

---

[Documentation](../README.md) › [The editor panel](main.md)
