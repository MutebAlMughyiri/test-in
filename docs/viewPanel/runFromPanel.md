[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-012

# UC-VIEW-PANEL-012: Run a test case from the panel

**As a** tester, **I want** to start the test case I am reading, **so that** I can try it again without going back to
the card it came from.

The button starts this one test case, on its own.

`F5` does it, which is the key the button's tooltip names and the same key that
runs a card in the editor.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has two tabs, Details and History, and
  both are drawn every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-050** — The run button is always drawn. Where it cannot work
  it is gray, it does not grow under the pointer, the pointer stays an arrow,
  and it says what it is waiting for. Nothing is left out (Rule-CODEGEN-062).
- **Rule-VIEW-PANEL-051** — The same place shows the run button or the stop
  button, never both.
- **Rule-VIEW-PANEL-052** — The button grows under the pointer, so it is clear
  it can be pressed.

## The screen

The three buttons sit on the line under the title, beside the badges.

```
┌────────────────────────────────────────────────────────────────────────────┐
│   ( High ) ( Smoke )        [ go to code ]  [ run ]  [ tc ]                │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The first button** — goes to the automation code. It is
   [UC-VIEW-PANEL-014](goToCode.md).
2. **The second button** — runs the test case. Its tooltip reads **Run Test
   Method** and names `F5`. Where it cannot work the tooltip says that instead.

## Main flow

1. The panel is showing a test case.
2. The tester moves the pointer over the run button, and it grows.
3. The tester clicks it.
4. Testin starts the test case as an execution of one.
5. A message reads *Running*.
6. When the result comes back, the whole panel redraws with the new run item
   status.

## What Testin refuses

**If the IDE has no TestNG plugin** — the run button is gray, it does not grow
under the pointer, and its tooltip reads *Run Test Method (needs the TestNG
plugin)*. Clicking it says the same thing. Running a test case needs the Java
plugin as well, so an IDE with neither names the Java one first.

**If `testin.yml` does not name the open test project** — the run button is
gray the same way, and says why. Running a test case reaches the automation
code, and the code is off until the file names the project (Rule-CODEGEN-082).

**If the test case is already running** — the place shows the stop button
instead. Stopping is [UC-VIEW-PANEL-013](stopFromPanel.md).

**If the test case has no automation code** — a message reads *Not automated*.

---

[Documentation](../README.md) › [The view panel](main.md)
