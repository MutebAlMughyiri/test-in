[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-044

# UC-EDITOR-PANEL-044: Run every automated test case

**As a** tester, **I want** to set the whole test run's automation going and come back later, **so that** a regression
test run of 200 test cases executes while I do something else. Stopped half way and run again, it carries on from the
first test case still waiting, as the walk does by hand.

Testin runs every test case still waiting for a run item status that has a generated method, in one go.

There is no key and no button in the editor. It starts from the tree, with **Run Tests** on the test run.

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
- **Rule-EDITOR-PANEL-184** — Every test case still waiting for a run item
  status that has a generated method is run, the ones the walk lands on by hand (Rule-EDITOR-PANEL-130). A test case
  already judged is passed over; its own
  run button runs it again, as one more execution (Rule-EDITOR-PANEL-136). A
  test case without a method stays as it is.
- **Rule-EDITOR-PANEL-185** — The whole set is one configuration and one
  process.
- **Rule-EDITOR-PANEL-186** — A test run in which every test case has been
  judged is marked **Completed** on its own.
- **Rule-EDITOR-PANEL-187** — The order the methods run in is the order of the
  test cases in their test set.
- **Rule-EDITOR-PANEL-188** — A test case with no generated method is dropped,
  and the rest still run.
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
- **Rule-EDITOR-PANEL-289** — While Run Tests executes, the selection moves to
  each run item as TestNG starts executing it, so the tester watches the test
  run go by. A click or a key in the list does not stop it: the next run item
  TestNG starts takes the selection again, until Run Tests ends.

## What the tester sees

This opens no screen of Testin's own. The test run's editor opens or comes to
the front, a dot blinks after the description of the test case TestNG is
executing, and the IDE's own run window opens below it. Start Manual Execution turns gray and says
Run Tests is executing.

Results land one at a time, and nothing is said per test case. The selection
moves to each run item as TestNG starts executing it, whatever the tester clicks
in between. When the last one is in, one small message appears at the bottom of the IDE and fades. It reads a
figure for each run item status the test run now carries, such as *Passed 42,
Failed 8*.

## Main flow

1. The tester right-clicks the test run in the tree and chooses **Run Tests**.
2. The test run's editor opens, or comes to the front.
3. Testin gathers every test case in the test run still waiting for a run item status, and passes over the judged ones.
4. All of them are claimed by this editor.
5. They are handed to TestNG as one configuration; a test case with no generated method is dropped.
6. Results come back one at a time and are written into the test run, silently, each as one more execution. The
   selection moves to each run item as TestNG starts it.
7. When the last one lands, one message reads the test run's figures, such as *Passed 42, Failed 8*, and the test run is
   marked **Completed**.

## What Testin refuses

**If the test run is completed or closed** — the tree entry is gray.

**If Run Tests is already executing** — the tree entry is gray, and says *Run
Tests is executing; stop it first*.

**If the tester is executing it by hand** — the tree entry is gray, and says *Start Manual Execution is on; stop it
first*.

**If nothing is left to run** — a message reads the test run's name, then *has
no test cases to run*.

**If the IDE has no TestNG plugin** — nothing is offered.

---

[Documentation](../README.md) › [The editor panel](main.md)
