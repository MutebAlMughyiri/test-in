[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-034

# UC-EDITOR-PANEL-034: Record that a test case failed, and say why

**As a** tester, **I want** to write down what actually happened at the moment I
see it, **so that** the bug report writes itself later.

**Failed** is the one run item status that asks a question first. Testin opens
a small form before it records anything.

`F`.

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
- **Rule-EDITOR-PANEL-143** — Failing is the one run item status that asks for
  detail. The dialog opens before the run item status is recorded.
- **Rule-EDITOR-PANEL-144** — `Escape` in the dialog records nothing at all,
  neither the detail nor the run item status. The dialog closes at once, without
  asking, even when something was typed.
- **Rule-EDITOR-PANEL-145** — Nothing is written as the tester types. Only
  saving writes.
- **Rule-EDITOR-PANEL-146** — The dialog opens for one test case. Several at
  once are failed with no detail collected.
- **Rule-EDITOR-PANEL-147** — A bug severity nobody chose is **Enhancement**, and
  a bug priority nobody chose is **Low**. The dialog starts there, and so does
  an automated failure.
- **Rule-EDITOR-PANEL-148** — The five fields - what happened, the bug severity,
  the bug priority, the stacktrace and the screenshots - are the same five the
  failure form in light mode uses, and each is drawn by a section of its own.
- **Rule-EDITOR-PANEL-219** — A picture on the clipboard is a screenshot wherever
  it is pasted in the failure form, and never letters; anything else on the
  clipboard pastes as text into whichever box has the cursor. The pictures are a **Screenshots** row of their own, which
  is there only while there is a picture
  to show. Each is kept as a picture file beside the test run, in the order it
  was pasted, and the button on its picture is the one way to take it out.
- **Rule-EDITOR-PANEL-221** — What actually happened is spell checked as the
  tester types, in the failure dialog and in light mode's form. A misspelled
  word is underlined, Alt+Enter offers the corrections, and nothing is changed
  unless the tester picks one.
- **Rule-EDITOR-PANEL-225** — The test case is recorded Failed only once what
  was typed is written. It cannot be written when the test run is no longer
  there, or no longer covers the test case. Then nothing is recorded, a
  message says why, and in light mode the form stays open with everything
  typed still in it.
- **Rule-EDITOR-PANEL-246** — What actually happened is typed into the same box
  the test case form gives the expected result and the test data, because a
  tester writing why a test case failed writes sentences: it grows as lines are
  added, up to six of them, and the dialog grows with it (Rule-INTERNAL-097,
  Rule-INTERNAL-102). Enter records the
  failure, and Ctrl+Enter adds a line, the same bargain a grid cell makes (Rule-EDITOR-PANEL-048).
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
- **Rule-EDITOR-PANEL-285** — A failure is recorded in one step: the Failed
  execution carries the actual result, the error and the screenshots the form
  collected, and the bug severity and priority go on the run item. Nothing is
  written before the form is saved, and nothing an earlier execution recorded
  changes.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Failed Test Case Details                                    │
├──────────────────────────────────────────────────────────────┤
│  [D] Log in with a valid user                                │
│  [E] The dashboard opens.                                    │
│                                                              │
│  ACTUAL RESULT                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ set actual result…                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  BUG SEVERITY                                                │
│  ( ) Blocker  ( ) Major  ( ) Minor  (x) Enhancement          │
│                                                              │
│  BUG PRIORITY                                                │
│  ( ) High  ( ) Medium  (x) Low                               │
│                                                              │
│  STACKTRACE                                                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ java.lang.AssertionError: expected [true]              │  │
│  │   at org.testin.demo.LoginTest.valid(LoginTest:41)     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  SCREENSHOTS                                                 │
│   ┌────────┐x  ┌────────┐x                                   │
│   │ picture│   │ picture│                                    │
│   └────────┘   └────────┘                                    │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Save   Escape Cancel   Alt+Enter Corrections …   │
└──────────────────────────────────────────────────────────────┘
```

1. **Description** and **Expected** — what the test case says, each after its
   icon, **D** and **E**, as the test case form draws them. They show the tester
   what should have happened. Neither can be typed into.
2. **The first box** — what actually happened, in the same box the test case
   form gives the expected result and the test data: one line to start, growing
   a line at a time as the tester writes, up to six lines and then scrolling,
   with the dialog growing with it (Rule-EDITOR-PANEL-246, Rule-INTERNAL-102). Its caption above it
   reads **Actual Result**, and its
   gray hint is inside it and stays there while the box has the cursor. A
   misspelled word in it is underlined, and `Alt+Enter` offers the corrections.
   `Ctrl+Enter` adds a line; `Enter` still saves; `Tab` leaves the box.
3. **Bug Severity** — four choices, with the least serious chosen.
4. **Bug Priority** — three choices, with the lowest chosen.
5. **The stacktrace box** — for the error or the exception, in the same box the
   others use: one line to start, growing as the error is pasted in, six lines at
   most and scrolling after that. Its caption above it reads **Stacktrace**, and
   its gray hint reads *paste error or exception or screenshot…*. `Ctrl+Enter`
   adds a line; `Enter` still saves.
6. **Screenshots** — one small picture for each screenshot pasted with `Ctrl+V`,
   in the order they were pasted: the screenshot itself, 48 pixels high. Its **x** takes that screenshot out, and the
   row disappears with the last of them.
   The row is not there at all until the first picture arrives, so a failure
   without one shows no caption and no gap. `Ctrl+V` adds a picture wherever the
   cursor is, because the clipboard decides: a picture is a screenshot and
   anything else is text (Rule-EDITOR-PANEL-219).
7. There is no button. `Enter` saves and `Escape` cancels. The status bar names
   them, then *Alt+Enter Corrections* and *Ctrl+V Paste Screenshot*.

## Main flow

1. The walk has selected a test case, and it does not work.
2. The tester presses `F`.
3. The **Failed Test Case Details** dialog opens, with the cursor in the first
   box.
4. The tester types what happened, picks a severity and a priority, and pastes
   the error.
5. The tester presses `Enter`.
6. **Failed** is recorded as one more execution, carrying the actual result,
   the error and the screenshots, with the tester's name, the time and the
   duration. The bug severity and priority go on the run item.
7. A message reads *Failed*.
8. The walk moves to the next test case.

## What Testin refuses

**If the test run is Committed** — `F` is gray, and its tooltip reads *This test run is Committed, so it records nothing
further.* Nothing is
written. A Completed or Closed test run still records it (Rule-TREE-PANEL-009).

**If the tester presses `Escape`** — the dialog closes at once, without asking.
Nothing at all is recorded, what was typed is gone, and the test case keeps
whatever run item status it had.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the test run keeps what it recorded.*

**If the test run is no longer there, or no longer covers the test case** — a
message is shown. It reads *This test run is no longer here, so nothing was recorded*, or *This test run does not cover
this test case, so nothing was recorded*. The
test case is not recorded Failed. In light mode the failure form stays open
with everything typed still in it (Rule-EDITOR-PANEL-225).

**If the test case is gone but the dialog is reached anyway** — the description
row reads *No longer in the test set*, and the expected row is not drawn.

**If several test cases are selected** — the dialog does not open at all. All of
them are failed with no detail. The detail can be filled in afterward, one test
case at a time, with `F2`.

---

[Documentation](../README.md) › [The editor panel](main.md)
