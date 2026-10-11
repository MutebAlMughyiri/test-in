[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-019

# UC-EDITOR-PANEL-019: Search the test cases

**As a** tester, **I want** to find a test case by a word in it, **so that** I do not page through 200 of them looking
for one.

The search narrows the list. It hides the test cases that do not hold the word.

`Ctrl+F`.

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
- **Rule-EDITOR-PANEL-090** — The list narrows three tenths of a second after
  the last keystroke, not on every letter.
- **Rule-EDITOR-PANEL-091** — The search reads every field a test case carries,
  which is every field the Fields popup lists. It is the same question the
  global search asks, answered in one place, so a field readable in one is
  readable in the other.
- **Rule-EDITOR-PANEL-092** — Searching goes back to the first page.
- **Rule-EDITOR-PANEL-093** — `Escape` in the box returns the keyboard to the
  list and leaves the text where it is.
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

This opens no screen. The cursor moves into the search box on the toolbar, and
whatever is in the box is selected.

Three tenths of a second after the last key, the list narrows to the test cases
holding the word. The status bar says how many are left. No message appears.

## Main flow

1. The tester presses `Ctrl+F`.
2. The cursor moves to the search box and what is in it is selected.
3. The tester types a word.
4. Three tenths of a second later the list narrows to the test cases holding it.
5. The status bar says how many are left, of how many the test set holds.
6. The tester presses `Escape`, and the keyboard goes back to the list.

## What Testin refuses

**If nothing matches** — the middle reads *No test cases match the search*.

**If the tester clears the filters** — the search text stays. Clearing the
filters and clearing the search are two different things.

---

[Documentation](../README.md) › [The editor panel](main.md)
