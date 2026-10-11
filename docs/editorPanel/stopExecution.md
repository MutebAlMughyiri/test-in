[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-035

# UC-EDITOR-PANEL-035: Stop executing

**As a** tester, **I want** to stop the walk part way, **so that** the clock stops when I go to a meeting and the test
run is written
as it stands.

Stopping changes no run item status. It only ends the walk and stops the clock.

There is no key for this. The button's tooltip reads **Stop Execution**.

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
- **Rule-EDITOR-PANEL-149** — Stopping writes the test run to disk as it stands.
- **Rule-EDITOR-PANEL-150** — Stopping changes no run item status already
  recorded, and no status either. Only a gesture that starts something stamps
  when execution began or moves a test run to **In Progress**.
- **Rule-EDITOR-PANEL-151** — Stopping stamps when execution ended. That stamp
  is written again by every stop.
- **Rule-EDITOR-PANEL-152** — The tester's own stop ends any automation this
  editor started, and closing the tab ends it too. Nothing else does.
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

This opens no screen. The clock stops, and the toolbar button becomes **Start
Manual Execution** again. Every run item status already recorded stays where it
is.

A small message appears at the bottom of the IDE and fades. It reads *Stopped*.

## Main flow

1. A walk is going, and the clock is ticking.
2. The tester presses **Stop Execution**.
3. Any automation this editor started is ended first.
4. Testin stamps when execution ended.
5. The clock stops, writing the last stretch onto the test case it was timing.
6. The button becomes **Start Manual Execution** again.
7. The test run is written to disk.
8. A message reads *Stopped*.

## What Testin refuses

Nothing. The button is never gray while it is on the toolbar.

## What the tester should expect

**Stopping one test case stops every test case running with it.** One execution
started as one gesture is one process, so ending it ends all of them.

**Stopping something that had already finished says *Stopped* anyway.** The
button has no gray state. A stop that reached nothing still reports itself.

## Two other things stop the walk

**Refresh** stops it, and says *Refreshed, and the execution stopped*. The time
spent so far on the test case being executed is written with the test run. The
test run is not stamped as ended.

**Closing the tab** stops the walk and the automation both, writes the test run,
and asks nothing. It is the same thing as pressing **Stop Execution**.

---

[Documentation](../README.md) › [The editor panel](main.md)
