[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-049

# UC-EDITOR-PANEL-049: Sort the test cases

**As a** tester, **I want** to arrange the test cases I am looking at by one field, **so that** I can work through them
in the order the job in front of me needs. The order the test set is run in does not change.

A sort arranges the screen and writes nothing. Each card keeps it's number, the test case's place in its set, so a
sorted list reads its numbers out of step. To keep an arrangement, change the order itself with **O** or by dragging.

There is no key for this. **Sort** is its own button on the editor's toolbar, beside **Filter**.

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
- **Rule-EDITOR-PANEL-274** — The Sort button on the editor's toolbar arranges
  the shown test cases by one field, Ascending or Descending: Order,
  Description, Priority, Status, Module, Created At, Updated At, Run Item
  Status, Executed At and Duration. Order, Ascending is the sort to start with,
  the button lights up while any other is on, and every card keeps its number.
- **Rule-EDITOR-PANEL-275** — Test cases that tie keep their order, and one with
  nothing in the sorted field comes last in either direction. Priority puts High
  first when Descending; Run Item Status follows the order the Run Item Status
  filter lists them in.
- **Rule-EDITOR-PANEL-276** — In a test set editor, Run Item Status, Executed
  At and Duration are gray; hovering one says only a test run has run items.
- **Rule-EDITOR-PANEL-277** — Sorting writes nothing: no test case, no marker,
  no change for Git. To keep an arrangement, change the order with O or by
  dragging.
- **Rule-EDITOR-PANEL-278** — A sort belongs to the open editor, as a filter
  does: opening it again starts on Order with no filter.
- **Rule-EDITOR-PANEL-279** — The Sort menu's first entry, Reset Sort, puts the
  test cases back in their order: Order, Ascending. With nothing sorted it is
  gray, and hovering it says nothing is sorted.
- **Rule-EDITOR-PANEL-280** — While a sort other than Order, Ascending is on,
  the status bar says so after the count of test cases: 12 test cases, then
  Sorted by Priority, Descending.

## What the tester sees

**Sort** sits on the toolbar beside **Filter**. Pressing it opens **Reset Sort**, then the list of fields, one of them
ticked, and below a line the two directions, one ticked:

```
  Reset Sort
  ─────────────
  (•) Order
  ( ) Description
  ( ) Priority
  ( ) Status
  ( ) Module
  ( ) Created At
  ( ) Updated At
  ( ) Run Item Status
  ( ) Executed At
  ( ) Duration
  ─────────────
  (•) Ascending
  ( ) Descending
```

Choosing a field or a direction draws the list again from the first page. While the sort is anything but Order,
Ascending, the button lights up, its tooltip names the sort, and the status bar says it after the count:
*12 test cases · Sorted by Priority, Descending*.

## Main flow

1. The tester presses **Sort**.
2. The tester chooses a field.
3. The list is drawn again in that field's order, from the first page.
4. The tester chooses **Descending** to turn it round.
5. **Reset Sort**, or **Refresh**, puts it back to Order, Ascending.

## What Testin refuses

**A run field in a test set editor** — Run Item Status, Executed At and Duration are gray in a test set editor, and
hovering one says only a test run has run items.

**Reset Sort with nothing sorted** — the entry is gray, and hovering it says *Nothing is sorted*.

---

[Documentation](../README.md) › [The editor panel](main.md)
