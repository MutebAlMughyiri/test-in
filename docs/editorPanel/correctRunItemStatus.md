[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-038

# UC-EDITOR-PANEL-038: Correct a run item status I got wrong

**As a** tester, **I want** to change a run item status I recorded by mistake, **so that** the test run says what really
happened.

There is no special gesture. The tester records the right run item status. It
is added as one more execution and becomes the run item status, and the wrong
one stays in the list until × removes it in the view panel.

Press the key for the right run item status on the test case.

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
- **Rule-EDITOR-PANEL-159** — A run item status is never written over. Recording
  another one adds an execution, which becomes the run item status. There is no
  separate gesture for correcting one, and a mistaken execution is removed with
  × in the view panel (Rule-VIEW-PANEL-121).
- **Rule-EDITOR-PANEL-160** — Recording again keeps the earlier execution in the
  list, with the tester who recorded it, the time and what it said.
- **Rule-EDITOR-PANEL-240** — Executions can be added, chosen as the default and
  removed while the test run takes run item statuses (Rule-TREE-PANEL-009).
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
- **Rule-EDITOR-PANEL-284** — Recording a run item status never erases what an
  earlier execution recorded. Its actual result, error, screenshots and duration
  stay with it, so passing a failed test case asks nothing first. The bug
  severity, priority and issue link stay on the run item.

## Main flow

1. A test case is recorded as **Failed**, with an actual result and an error.
2. The tester realizes it really passed.
3. The tester selects it and presses `P`.
4. **Passed** is recorded as one more execution and becomes the run item
   status. The failed execution keeps its actual result and its error.
5. A message reads *Passed*.
6. To drop the mistaken execution, the tester hovers its line under
   **Execution result** in the view panel and clicks × (Rule-VIEW-PANEL-121).

## What Testin refuses

**If several test cases are selected** — each of them gets one more execution,
and nothing is asked.

**If the test run is Committed** — the key is gray, and ☆
and × in the view panel are gray and say why (Rule-TREE-PANEL-009).

## What cannot be undone

There is no undo for a run item status inside the test run editor. `Ctrl+Z`
there belongs to the test cases, not to the test run. An execution removed with
× is gone.

**There is no way to clear a run item status back to nothing.** **Pending**, **Untested** and **Removed** have no key
and are on
no menu.

---

[Documentation](../README.md) › [The editor panel](main.md)
