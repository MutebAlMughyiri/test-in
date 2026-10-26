[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-043

# UC-EDITOR-PANEL-043: Run one test case's automation

**As a** tester, **I want** to run a test case's generated method and have its run item status land in this test run,
**so that** the machine judges what it can.

The machine runs the test and writes the run item status. The tester does not
press `P` or `F`.

`F5` on the selection.

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
- **Rule-EDITOR-PANEL-180** — This editor claims each test case first, so the
  run item status comes back to this test run and not to another editor showing
  the same test case.
- **Rule-EDITOR-PANEL-181** — Claiming marks the test run **In Progress** if it
  is not already.
- **Rule-EDITOR-PANEL-182** — A run item status from the automation is written
  the same way a run item status set from the keyboard is.
- **Rule-EDITOR-PANEL-183** — A run item status the automation records keeps the
  framework's own time for every attempt, whether the walk's clock was counting
  or not. The clock stops, and what it counted is not kept, so no second is
  counted twice in the test run's clock (Rule-EDITOR-PANEL-288).
- **Rule-EDITOR-PANEL-241** — A run item can be executed again while its test
  run takes run item statuses. Each attempt is one more execution, after the
  ones before it, which stay as they were recorded.
- **Rule-EDITOR-PANEL-242** — A status from the automation for the test case the
  walk is on moves the walk to the next test case waiting for a run item status. The tester's own run item status does
  exactly the same, and execution goes on. Until
  the status comes, the walk stays on that test case.
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
- **Rule-EDITOR-PANEL-266** — Run on a test case of a Committed test run is gray
  and says why, on its card, in the context menu and in the view panel: a
  Committed test run records no result.
- **Rule-EDITOR-PANEL-301** — Run on a Disabled test case is gray and says why,
  on its card, in the context menu and in the view panel. Its test method is
  switched off (Rule-CODEGEN-047), so the framework would run nothing and no run
  item status would come back.

## What the tester sees

This opens no screen of Testin's own. The selected cards turn to running at
once, and the IDE's own run window opens below the editor.

Each result comes back on its own and its card takes the new run item status.
Nothing is said per test case. When the last one is in, one small message
appears at the bottom of the IDE and fades. It reads a figure for each run item
status the test run now carries, such as *Passed 42, Failed 8*. Those are the
status bar's own words, so the message and the bar cannot count one test run
differently.

## Main flow

1. The tester selects three test cases in the test run and presses `F5`.
2. Testin claims all three for this editor.
3. The cards turn to running at once.
4. The three are handed to TestNG as one configuration.
5. A message reads *Running 3*.
6. Each result comes back and is written into the test run, silently.
7. When the last one is in, one message reads the whole test run's figures, such
   as *Passed 42, Failed 8*.

**During a walk** ([UC-EDITOR-PANEL-031](startExecution.md)), running the test
case the walk is on keeps the walk there, its clock counting, until the status
comes back. The status then moves the walk to the next test case waiting for a
run item status, as the tester's own run item status does, and execution goes on (Rule-EDITOR-PANEL-242). The time
recorded is the framework's own (Rule-EDITOR-PANEL-183).

Everything about how the execution is built and named is on
[UC-CODEGEN-008](../codegen/runAutomation.md).

## What is recorded

| Written                         | From                                           |
|---------------------------------|------------------------------------------------|
| The run item status             | Whether the framework said it passed or failed |
| The duration                    | The framework's own timing                     |
| The actual result and the error | The framework's message and stacktrace         |
| Who ran it, and when            | The name on the settings page, and now         |

Each result is one more execution of the run item, carrying its own actual
result and error, so nothing an earlier execution recorded is cleared: what a
tester wrote about a failure stays with that failure. A method TestNG retried or
ran more than once records every attempt (Rule-CODEGEN-111). The bug severity,
the bug priority and the reported issue belong to the failure they describe
(Rule-EDITOR-PANEL-296), so an automated failure starts at Enhancement and Low,
with no issue, until a tester triages it.

## What Testin refuses

**If the test run is Committed** — the result is ignored. A Completed or Closed
test run still records it (Rule-TREE-PANEL-009).

**If the test case is not covered by this test run** — the result is ignored.

**If a test case has no generated method** — it is dropped and a message says
so. The others still run.

**If the test case is Disabled** — Run is gray and reads *This test case is
Disabled, so its test method never runs.* Pressing it says the same and starts
nothing (Rule-EDITOR-PANEL-301).

## Where the plugin breaks its own rules

**Closing the tab stops the automation this editor started.** It is the same
gesture as pressing **Stop Execution**, so the results of tests still running
are lost, exactly as a stop loses them.

---

[Documentation](../README.md) › [The editor panel](main.md)
