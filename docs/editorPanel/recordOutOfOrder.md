[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-037

# UC-EDITOR-PANEL-037: Record a run item status out of order

**As a** tester, **I want** to judge a test case that is not the one the walk is
on, **so that** I can record something I happened to try while doing something
else.

A run item status can go on any test case at any time. The walk is not in
charge of that.

`P`, `F` or `B` on any selected test case.

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
- **Rule-EDITOR-PANEL-156** — A run item status can be recorded on any test case
  at any time. The walk does not have to be going.
- **Rule-EDITOR-PANEL-157** — A run item status recorded away from the walk is
  not timed. The test case keeps whatever duration it had.
- **Rule-EDITOR-PANEL-158** — Recording away from the walk does not stop the
  walk, and does not move it.
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

This opens no screen. The run item status badge on the test case the tester
clicked changes, and the figures in the status bar move. Its duration column
stays blank.

The walk does not move. It stays on the test case it was timing, and the clock
keeps running there.

## Main flow

1. The walk is on test case 12.
2. The tester notices test case 40 is worth judging now.
3. The tester clicks test case 40 and presses `P`.
4. **Passed** is recorded against test case 40, with no duration.
5. A message reads *Passed*.
6. The walk is still on test case 12, still timing it.

## What Testin refuses

**If nothing is selected** — nothing happens.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the test run keeps what it recorded.*

**If the test run does not cover this test case** — nothing is recorded, and
only the log says so.

## Why the duration is blank

The clock times the test case the walk is on. A run item status recorded
anywhere else was never timed. Nothing is written to the duration, so the column
stays blank.
A blank duration means nothing was measured. It does not mean the test case took
no time.

---

[Documentation](../README.md) › [The editor panel](main.md)
