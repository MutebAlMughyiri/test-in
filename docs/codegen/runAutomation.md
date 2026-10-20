[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-008

# UC-CODEGEN-008: Run a test case's automation

**As a** tester, **I want** to run the generated method for one or more test
cases, **so that** the run item status is recorded without me judging it by
hand.

The code runs, and Testin writes down whether each test case passed.

`F5` on the selected test cases.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — The body belongs to the tester. Testin writes the
  annotation, the declaration and one `// TODO` line, and never touches a body a
  tester has written. A body is the tester's as soon as it is anything but that
  one line: a statement, a comment, or the TODO deleted. An agent writes the
  body only where it is still that line, and only when the tester asked for it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-082** — Testin touches a test project's automation code only
  when `testin.yml` names that test project. Otherwise, nothing is generated,
  renamed, moved or removed, **Automate Test Case**, **Navigate to Test Method**
  and **Run Tests** are gray and say why, and no gutter icon or automated mark
  is shown. **Save to testin.yml**, in the Testin panel, turns code on.
- **Rule-CODEGEN-031** — Whatever the tester selected is one execution, not one
  execution for each test case.
- **Rule-CODEGEN-033** — One message with a count, however many test cases
  started, and it counts the ones that actually started. It appears once Testin
  has found their methods, not when the key was pressed.
- **Rule-CODEGEN-034** — A card's run button becomes its stop button the moment
  the tester presses the key, before the process exists.
- **Rule-CODEGEN-035** — The order the methods run in is the order of the test
  cases in their test set.
- **Rule-CODEGEN-074** — A test case that cannot run is reported once. One says
  its description; several say how many, because the descriptions are on the
  cards in front of the tester.
- **Rule-CODEGEN-075** — A test the framework does not run to a run item status
  is recorded as Failed, with Skipped/Terminated as its actual result. TestNG
  skips a test whose dependency failed or whose group is excluded, and stops the
  rest when the execution is terminated: all of them come back as a defect with
  no run item status, and an execution that quietly left them Pending would
  report a cycle as finished when part of it never ran. A disabled method is
  different: TestNG never reports it at all, so it is taken out of Running with
  nothing recorded (Rule-CODEGEN-092), and a Disabled test case cannot be added
  to a test run (Rule-TREE-PANEL-138).
- **Rule-CODEGEN-076** — A second execution started while the first is still
  going gets a name of its own - the same name with a number after it, such as
  Testin: LoginTest (2). The two executions are then separate everywhere: each has its
  own process, Stop reaches one without touching the other, and each test case
  reports its run item status under the execution it belongs to.
- **Rule-CODEGEN-092** — An execution that ends without reporting a test case,
  whether its build failed, its JVM crashed or the IDE's own Stop ended it,
  takes that test case out of Running with no run item status recorded. Nothing
  is left running after the execution has ended.
- **Rule-CODEGEN-097** — A selection whose test methods live in more than one
  module does not start, because one execution runs on one module's classpath.
  Testin names the modules, so the tester runs each module's test cases on their
  own.
- **Rule-CODEGEN-110** — A test case's result is decided when TestNG moves on
  to another test case, or when the execution ends - not at its first result.
  TestNG may run one method several times in a row: an attempt it skips may be
  retried, exactly as it skips a test whose dependency failed, and a method with
  an invocation count runs that many times. The run item takes the worst of
  them: Failed if any attempt failed, with that failure; Passed if they all
  passed; and, when TestNG only skipped it, Failed with Skipped/Terminated, as
  Rule-CODEGEN-075 says. A skipped attempt that a retry follows does not count.
  Until then the test case stays running, and light mode and the walk stay on
  its run item. So a test that fails and passes on a retry is recorded Passed,
  and one of four invocations failing records Failed.
- **Rule-CODEGEN-111** — Every attempt TestNG reports for a test case is
  recorded as an execution the moment it is reported: each retry, each
  invocation and each data provider row, so the card and the view panel grow one
  by one while the method runs. Until TestNG moves on, the last attempt so far
  is the default, and an attempt's label may change when the next one arrives.
  Once its result is decided (Rule-CODEGEN-110), the one that decides is made
  the default: the last failed invocation or row if one failed, otherwise the
  last attempt, and never an attempt TestNG retried.
- **Rule-CODEGEN-112** — An automated execution says what it was, read from the
  order the IDE reported it. An attempt reported as skipped that its test case
  starts again after was retried, and the start after it is *Retry 1*, *Retry
  2*... under the same invocation. A start after a passed or failed result is
  the next invocation, numbered *Invocation 1*, *Invocation 2*... only when the
  method ran more than once; a data provider row is an invocation like any other.
- **Rule-CODEGEN-120** — In a Robot Framework test project, Run Test Method and
  Run Tests run Robot Framework with the code project's interpreter, choosing
  each test by its test case UUID tag, and show it in the Run window. Each run
  item is recorded from Robot's output when the run ends: PASS is Passed, FAIL
  is Failed with Robot's message, and a skipped test is recorded as
  Rule-CODEGEN-075 says. No Robot plugin is needed.

## The four ways in

| The tester does this                  | Where                                                                     |
|---------------------------------------|---------------------------------------------------------------------------|
| Presses `F5`                          | The list of test cases in either editor                                   |
| Chooses **Run Test Method**           | The menu in either editor                                                 |
| Clicks the run button                 | A card under the pointer, or the view panel                               |
| Presses `F5` or clicks the run button | [Light mode](../editorPanel/lightMode.md), on the test case it is showing |

Running every automated test case in a test run is different, and is
[UC-EDITOR-PANEL-044](../editorPanel/runWholeTestRun.md).

## What the tester sees

Every selected card's run button becomes a stop button at once, and a dot blinks
after the description of the one TestNG is executing. That is the answer to the key; the message comes a moment later, once
Testin has found the methods. It appears near the bottom right of the IDE,
reading *Running* and then how many actually started. The IDE's own run window
opens underneath and shows what TestNG is doing.

## Main flow

1. The tester selects three test cases and presses `F5`.
2. Every card's run button becomes a stop button at once.
3. Testin finds the method for each test case by its identity.
4. The three are handed to TestNG as one configuration.
5. A message reads *Running 3* — the number that started, said once Testin
   knows it.
6. While TestNG executes a test case, a dot blinks after its description
   (Rule-EDITOR-PANEL-291).
7. Each result comes back and is recorded against the test case, as one more
   execution of its run item.

## What Testin refuses

**If nothing is selected** — the entry is gray and the key does nothing.

**If a test case has no method** — that test case is dropped. Its card goes
back to how it was, and the other test cases still run. One message says so: for
a single test case it reads its description and then *has no generated code
yet*; for several it reads how many *test cases have no generated code yet*.
There is never one message per test case.

**If none of them has a method** — nothing starts, and no running message
appears. The message saying how many have no generated code still does.

**If every selected test case is already running** — nothing starts and nothing
is said.

**If an execution of that name is already going** - the second one is named
after it with a number, such as *Testin: LoginTest (2)*, and runs alongside. Each has
its own process: stopping one leaves the other running, and each test case
records its run item status under the execution it actually belongs to.

**If the framework skips a test** - the test case is recorded as **Failed**, and
its actual result reads *Skipped/Terminated*. A dependency that failed, an
excluded group and a terminated execution all arrive the same
way: the framework says the test did not pass and gives no run item status.
A skip is recorded once TestNG has moved on, because TestNG skips an attempt
it is about to retry the same way: a retry that passes is recorded **Passed**.
Every attempt is kept as an execution of its own - the retried one too, as a
failure marked *retried* - and the one that decided is the default
(Rule-CODEGEN-111). Leaving it Pending would report the cycle as finished when
part of it never ran.

**If the IDE is indexing** — every test case is put back and a message reads *Tests cannot run while the IDE is
indexing. Wait a moment and run them again.*

**If indexing starts part way through** — every test case is put back and a
message reads *Indexing interrupted the execution. Run it again.*

**If the IDE has no TestNG or no Java plugin** — the menu entry is still there,
grayed, naming the first one missing: *(needs the Java plugin)* or *(needs the
TestNG plugin)*. The run button is drawn gray. It does not grow under the
pointer, and it says the same sentence when it is hovered.

**If testin.yml does not name the open test project** — the run button is drawn
gray, and hovering it says *testin.yml does not name this test project. Save to
testin.yml, in the Testin panel, turns code on.* The menu entry is not grayed.
It stays live, and pressing it says the same sentence and starts nothing. That
is difference 10 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

## What the execution is called

The name in the IDE's run widget starts with *Testin:*, then depends on the
selection. The IDE's own gutter icon names a configuration the way the table
does, without *Testin:*, so a configuration the tester made is never reused,
changed or stopped by Testin (Rule-CODEGEN-094).

| The tester selected      | The name                                             |
|--------------------------|------------------------------------------------------|
| One test case            | The class name, a dot, then the method name          |
| Several in one test set  | The class name                                       |
| Several across test sets | The first class name, then *and*, then how many more |

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
