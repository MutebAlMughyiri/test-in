[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-040

# UC-EDITOR-PANEL-040: Change the failure details on their own

**As a** tester, **I want** to add to what I wrote about a failure, **so that** I can paste the error in after I have
found it, without touching the
run item status.

This reopens the failure form on a test case that is already **Failed**. The
run item status is not touched.

`F2` on the failed test case.

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
- **Rule-EDITOR-PANEL-167** — The run item status is not touched. Only the four
  fields are written.
- **Rule-EDITOR-PANEL-168** — The entry works on exactly one test case, and only
  when its run item status is **Failed**.
- **Rule-EDITOR-PANEL-169** — The message comes after the test run is written,
  so an edit that was dropped never reports itself as saved.
- **Rule-EDITOR-PANEL-170** — The dialog is the same one `F` opens, filled in
  with what is there.
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
- **Rule-EDITOR-PANEL-286** — Changing the failure details changes the run
  item's default execution and adds none.

## The screen

The same dialog the `F` key opens, with what was written already in it.

```
┌────────────────────────────────────────────────────────────┐
│  Failed Test Case Details                                  │
├────────────────────────────────────────────────────────────┤
│  [D] Log in with a locked account                          │
│  [E] The account is refused.                               │
│                                                            │
│  ACTUAL RESULT                                             │
│  [ The session was dropped instead.                    ]   │
│                                                            │
│  BUG SEVERITY                                              │
│  ( ) Blocker  (x) Major  ( ) Minor  ( ) Enhancement        │
│                                                            │
│  BUG PRIORITY                                              │
│  (x) High  ( ) Medium  ( ) Low                             │
│                                                            │
│  STACKTRACE                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Error 500 returned by /api/session                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  SCREENSHOTS                                               │
│   ┌────────┐x                                              │
│   │ picture│                                               │
│   └────────┘                                               │
├────────────────────────────────────────────────────────────┤
│  [k]  Enter Save   Escape Cancel   Alt+Enter Corrections … │
└────────────────────────────────────────────────────────────┘
```

1. **Description** and **Expected** — what the test case says, each after its
   icon, **D** and **E**. Neither can be typed into.
2. **The first box** — what actually happened. It opens holding what was written
   before.
3. **Bug Severity** and **Bug Priority** — the choices made last time are the
   ones already selected, or Enhancement and Low where nobody chose.
4. **The big box** — for the error or the exception. This is usually what the
   tester came back to add.
5. **The run item status** — not on this dialog at all. It stays **Failed**.

## Main flow

1. A test case is recorded as **Failed**.
2. The tester finds the exception in the log and copies it.
3. The tester selects the test case and presses `F2`.
4. The **Failed Test Case Details** dialog opens with what was already written.
5. The tester pastes the error into the big box and presses `Enter`.
6. The four fields are written onto the default execution and the run item,
   and no execution is added. The run item status stays **Failed**.
7. The test run is written to disk.
8. A message reads *Details updated*.

## What Testin refuses

**If the test run is Committed** — the entry is gray, and its tooltip reads *This test run is Committed, so it records
nothing further.* The grid's Actual
Result cell cannot be edited either.

**If nothing is selected, or more than one thing is** — the entry is gray.

**If the selected test case is not failed** — the entry is gray. A passed test
case has nothing to explain.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the test run keeps what it recorded.*

**If the test run is being read again at that moment** — nothing is written, and
nothing is said. Only the log records it.

## Why the message comes last

The message is raised after the test run is written, not before. So an edit that
was dropped never says *Details updated*. Nothing at all is said, which is the
half of the trade worth keeping: a confirmation of a save that did not happen
costs more than a silence.

---

[Documentation](../README.md) › [The editor panel](main.md)
