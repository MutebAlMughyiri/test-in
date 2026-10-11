[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-022

# UC-EDITOR-PANEL-022: Page through the test cases

**As a** tester, **I want** to move through a long test set a page at a time, **so that** a test set of 2,770 test cases
opens as fast as one of ten.

Testin never draws the whole test set at once. It draws one page.

`Ctrl+Right` and `Ctrl+Left` for one page, `Ctrl+Alt+Cmd+Right` and `Left` on a
Mac. Add `Shift` for the ends of the
set.

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
- **Rule-EDITOR-PANEL-101** — The whole test set is paged through after the
  filters and the search have narrowed it.
- **Rule-EDITOR-PANEL-102** — An arrow with nowhere to go is gray.
- **Rule-EDITOR-PANEL-103** — Paging says nothing.
- **Rule-EDITOR-PANEL-104** — Reloading lands on whichever page holds the test
  case that was selected.
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

The five controls sit in the middle of the status bar.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  1 of 12 test cases            |<   <   2 of 3   >   >|            [ 50 ]  │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **First page** — `Ctrl+Shift+Left`, `Ctrl+Alt+Shift+Cmd+Left` on a Mac.
2. **Previous page** — `Ctrl+Left`, `Ctrl+Alt+Cmd+Left` on a Mac.
3. **The label** — which page this is, of how many.
4. **Next page** — `Ctrl+Right`, `Ctrl+Alt+Cmd+Right` on a Mac.
5. **Last page** — `Ctrl+Shift+Right`, `Ctrl+Alt+Shift+Cmd+Right` on a Mac.

Every arrow prints its own key in its tooltip, because the key and the tooltip
come from the same place.

## Main flow

1. The tester presses `Ctrl+Right`.
2. The next page of test cases is drawn.
3. The label reads the new page number.
4. Nothing is said.

## What Testin refuses

**If this is the first page** — the two arrows on the left are gray and drawn
faded. `Ctrl+Left` and `Ctrl+Shift+Left` do nothing.

**If this is the last page** — the two arrows on the right are the same, and so
are `Ctrl+Right` and `Ctrl+Shift+Right`.

## The keys are the editor's, not the menu's

The four keys work on the cards and in the grid, and they are on no menu. Paging
moves the view and changes nothing about the test case under the pointer, so a
menu about that test case is the wrong place to offer it. The four arrows in the
status bar are already there, each printing its own key.

In the grid they are refused while a cell is open for editing, like every other
key that would act on the row: a page turned under a half-typed Actual Result
loses it.

## Why Shift means the end

Shift adds reach to a direction wherever a keyboard offers both, so a tester who
knows `Ctrl+Left` has nothing new to learn. It also keeps the four keys in one
shape: the same two arrow keys, told apart by one modifier, rather than a third
pair chosen from somewhere else.

## The same key in the view panel

`Ctrl+Right` and `Ctrl+Left` mean forward and back in whichever surface has the
keyboard. Here that is a page of test cases. In the view panel it is one test
case, because the panel shows one at a time and has nothing else forward could
mean there.

---

[Documentation](../README.md) › [The editor panel](main.md)
