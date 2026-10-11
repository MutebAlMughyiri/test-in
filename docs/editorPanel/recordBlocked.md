[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-033

# UC-EDITOR-PANEL-033: Record that a test case is blocked

**As a** tester, **I want** to say a test case could not be tried at all, **so that** it is not counted as a failure of
the product.

**Blocked** means the tester could not try it. It is not the product's fault.

`B`.

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
- **Rule-EDITOR-PANEL-141** — **Blocked** means the test case was attempted and
  could not finish, usually because of the environment or the data.
- **Rule-EDITOR-PANEL-142** — Recording blocked clears nothing. Anything already
  written about the test case stays.
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

This opens no screen. The card's run item status badge turns to **Blocked**, and
the figures in the status bar move. Nothing already written about the test case
is cleared.

A small message appears at the bottom of the IDE and fades. It reads *Blocked*.

## Main flow

1. The walk has selected a test case and is timing it.
2. The tester finds the environment will not let them try it.
3. The tester presses `B`.
4. Testin records **Blocked**, the tester's name, the time and the duration.
5. A message reads *Blocked*.
6. The walk moves to the next test case.

## What Testin refuses

**If the test run is Committed** — `B` is gray, and its tooltip reads *This test run is Committed, so it records nothing
further.* Nothing is
written. A Completed or Closed test run still records it (Rule-TREE-PANEL-009).

**If nothing is selected** — nothing happens.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the test run keeps what it recorded.*

**If a grid cell is open for editing** — the key belongs to the cell.

## Blocked against failed

**Blocked** is the environment's fault. **Failed** is the product's. A report
counts them apart. Its blocked section says the test cases *could not complete,
typically because of an environment or data dependency*.

Blocked asks for no explanation. A tester who wants to write down why can type
into the **Actual Result** column afterward. That is
[UC-EDITOR-PANEL-041](typeActualResult.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
