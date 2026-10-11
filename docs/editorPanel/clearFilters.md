[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-021

# UC-EDITOR-PANEL-021: Clear the filters

**As a** tester, **I want** every test case back, **so that** I can see the whole test set again without undoing four
filters one
at a time.

One click clears every filter. It does not clear the search box or the sort.

There is no key for this. The entry is at the top of the filter menu.

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
- **Rule-EDITOR-PANEL-099** — **Reset Filters** clears every filter - the
  priority, the automation, the group, the module, the status, the run item
  status and the test set. With nothing to clear it is gray, and hovering it
  says nothing is filtered. It leaves the sort alone.
- **Rule-EDITOR-PANEL-100** — It does not clear the search text.
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

This opens no screen of its own. **Reset Filters** is the first entry of the
filter menu, above a separator line. It is always there, and gray while no
filter is on.

Choosing it draws the whole test set again, from the first page. The count
disappears from the filter button. Nothing is said.

## Main flow

1. The tester presses the filter button.
2. **Reset Filters** is the first entry.
3. The tester chooses it.
4. Every filter is cleared and the whole test set is drawn again.
5. The view goes back to the first page.
6. The count disappears from the button.

## What Testin refuses

**If no filter is on** — the entry is gray, and hovering it says *Nothing is
filtered*. It is always there: a popup that changes shape is one a tester
cannot learn, and the entry is also how they find out filters live here at all.

## What is left behind

The search text stays. A tester who cannot see a test case after clearing the
filters should check the search box, which is still narrowing the list.

**Refresh** clears every filter and puts the sort back to Order, and keeps the
search text (Rule-EDITOR-PANEL-117 on [UC-EDITOR-PANEL-027](refreshEditor.md)).

---

[Documentation](../README.md) › [The editor panel](main.md)
