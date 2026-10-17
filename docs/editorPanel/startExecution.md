[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-031

# UC-EDITOR-PANEL-031: Start executing by hand

**As a** tester, **I want** Testin to walk me through the test run one test case
at a time, **so that** I judge each one in turn and each one is timed.

The walk picks the next test case for the tester and times it. The tester only
has to press `P`, `F` or `B`.

There is no key for this. The button's tooltip reads **Start Manual Execution**.

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
- **Rule-EDITOR-PANEL-130** — The walk only ever lands on a test case waiting
  for a run item status. It starts at the first one and moves to the next,
  passing over any that has been judged, whoever judged it.
- **Rule-EDITOR-PANEL-131** — Starting marks the test run **In Progress**, and
  stamps when execution began. That stamp is set once and never overwritten.
- **Rule-EDITOR-PANEL-132** — The clock starts on the test case the walk lands
  on, and ticks once a second. It stops the moment that test case has a run item
  status, and what it counted is the duration of the run item status the tester
  records. A run item status the automation records keeps the framework's own
  times instead (Rule-EDITOR-PANEL-183).
- **Rule-EDITOR-PANEL-133** — The button becomes **Stop Execution** while the
  walk is going.
- **Rule-EDITOR-PANEL-134** — Reaching the end of the list ends the walk, and
  nothing more. The test run is marked **Completed** only when every test case
  in it has been judged. That question is asked of the test run rather than of
  the walk, and it is asked wherever a run item status is recorded, however the
  last one arrived.
- **Rule-EDITOR-PANEL-135** — Start is offered only when there is something to
  walk. Three things leave nothing to walk: a test run holding no test cases, a
  filter that matches nothing, and a list where every test case has been judged.
  All three gray the button. A press that reaches Testin anyway is refused, and
  says so.
- **Rule-EDITOR-PANEL-227** — The walk follows the test case being executed, not
  its place in the list, so narrowing the list with the filter never moves a run
  item status onto another test case. While the filter hides the test case being
  executed, a run item status key records nothing and says so, and clearing the
  filter brings it back.
- **Rule-EDITOR-PANEL-238** — A run item status records who gave it and when; it
  keeps no copy of the test case. The text it was given against is read from Git
  once its test run is Committed.
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
- **Rule-EDITOR-PANEL-264** — When the test case the walk is on has been
  deleted, a run item status key records nothing: a short message says so, and
  the walk moves on to the next Pending test case.
- **Rule-EDITOR-PANEL-283** — The execution walk and light mode follow the test
  cases as shown, the sort included. Under a sort by Run Item Status, Executed
  At or Duration a judged test case moves to where the sort places it, and the
  walk goes on to the next one waiting.
- **Rule-EDITOR-PANEL-290** — A test run executes in one mode at a time: the
  walk Start Manual Execution begins, or the automation Run Tests begins. While one is
  on, the other is gray and says why, and Stop Execution ends whichever is on. A
  single test case's ▶ is neither: it runs alongside either and leaves both
  available.

## What the tester sees

This opens no screen. The toolbar button becomes **Stop Execution**. The editor
turns to the page holding the first test case with no run item status, and
selects its row.

The status bar reads **In Progress**, and the clock starts ticking once a
second. A small message appears at the bottom of the IDE and fades, reading *In Progress*.

## Main flow

1. The tester presses **Start Manual Execution**.
2. The test run becomes **In Progress**, and a message says so.
3. Testin finds the first test case with no run item status.
4. The editor turns to the page holding it and selects its row.
5. The clock starts, and the card and the test run clock redraw once a second.
6. The tester judges it with `P`, `F` or `B`.
7. The walk moves to the next test case waiting for a run item status,
   passing over any that already has one, and times that one.
8. When the walk runs out of test cases it stops, and the test run is written.
9. If every test case in the test run now has a run item status, the test run is
   marked **Completed**.

## What Testin refuses

**If Run Tests is executing** — the button is gray beside **Stop Execution**,
and its tooltip reads *Run Tests is executing; stop it first*
(Rule-EDITOR-PANEL-290). A single test case's run button grays nothing.

**If the test run is completed or closed** — the button is gray, and its tooltip
reads *Execution disabled — test run status is*, then the status.

**If nothing is waiting for a run item status** — the button is gray, and its
tooltip reads *Nothing to execute — no test case is waiting for a run item
status*. Three things lead to this: a test run holding no test cases, a filter
that matches nothing, and a list whose test cases have all been judged. [Light
mode](lightMode.md) grays no button, so its start refuses instead, in a message
that fades.

---

[Documentation](../README.md) › [The editor panel](main.md)
