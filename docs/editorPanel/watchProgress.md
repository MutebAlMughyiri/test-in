[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-042

# UC-EDITOR-PANEL-042: Watch how the test run is going

**As a** tester, **I want** to see how many have passed and how long I have
been at it, **so that** I can say when the test run will be finished.

The status bar reports. Nothing here changes a test case.

There is no key for this. The figures are in the status bar.

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
- **Rule-EDITOR-PANEL-175** — A run item status no test case carries is not
  drawn at all.
- **Rule-EDITOR-PANEL-176** — The untouched test cases read **Pending** while
  the test run is open, and **Untested** once it is signed off.
- **Rule-EDITOR-PANEL-177** — The figures are worked out from what the test run
  holds now, not from disk.
- **Rule-EDITOR-PANEL-178** — A test run that has measured nothing shows a blank
  clock, not a row of zeros.
- **Rule-EDITOR-PANEL-179** — The three test run labels are hidden, not blank,
  when there is nothing to say. A test set editor never shows them.
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
- **Rule-EDITOR-PANEL-265** — The test run status in the status bar is its icon
  alone, with the status named when the pointer rests on it. Beside the icon it
  shows the commit a Committed test run was recorded in; any other status shows
  the icon only. An Assigned test run names no one, because Testin does not yet
  keep who a test run is assigned to, and this machine's tester is not that
  person on anyone else's machine.
- **Rule-EDITOR-PANEL-288** — The clock adds up every execution of every run
  item, retries and re-runs included, and what the walk is counting on the test
  case it is on, so it says what the test run really cost. A card and the grid
  show the default execution's own duration.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  4 of 12 test cases   |< < 1 of 1 > >|   In Progress                       │
│                       Passed 6 · Failed 2 · Pending 4     00:14:22  [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The test run status** — the same icon the tree draws, alone. Its tooltip
   names the status: *Completed. A committed test run records no more run item
   statuses*. Beside the icon: the tester's name when the test run is **Assigned**, the commit's short hash when it is
   **Committed**, nothing otherwise (Rule-EDITOR-PANEL-265).
2. **The figures** — one for each run item status any test case carries, each
   in that run item status's own color, separated by a dot. Their tooltip reads *How this test run is going*.
3. **The clock** — how long this test run has been executing: every execution
   of every run item, retries and re-runs included (Rule-EDITOR-PANEL-288). It
   ticks once a second while a test case is being timed. Its tooltip reads *Time spent
   executing this test run*.
4. **The page size** — how many test cases a page holds.

## Main flow

1. The tester starts executing.
2. The status becomes **In Progress**, and the clock starts.
3. Each run item status recorded moves one figure up and another down.
4. A run item status nobody has recorded yet is not drawn.
5. When the walk finishes, the status becomes **Completed** and the clock stops.

## What Testin refuses

Nothing. The status bar only reports.

---

[Documentation](../README.md) › [The editor panel](main.md)
