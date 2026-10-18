[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-011

# UC-EDITOR-PANEL-011: Remove test cases

**As a** tester, **I want** to delete test cases that are no longer wanted, **so that** the test set is what somebody
would actually run.

Testin asks first. The test case's file and its generated test method both go.

`Delete` on the selection.

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
- **Rule-EDITOR-PANEL-062** — The confirmation names the test case, or counts
  them, and says which test set they are in.
- **Rule-EDITOR-PANEL-063** — A removal can be taken back with `Ctrl+Z`.
- **Rule-EDITOR-PANEL-064** — Nothing is renumbered. The removed test case
  simply leaves a gap in the order, and the numbers on screen close up on their
  own.
- **Rule-EDITOR-PANEL-065** — A test case that is waiting to be pasted is asked
  about like any other, and removing it calls off the waiting cut.
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

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Remove these 4 test cases?                       (1)  │  │
│  │                                                        │  │
│  │  FROM                                             (2)  │  │
│  │  Demo › Test Cases › Login                             │  │
│  └────────────────────────────────────────────────────────┘  │
│                                            [ Remove ]   (3)  │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Remove       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — names the one test case, or counts several. It sits on a
   card (Rule-INTERNAL-099).
2. **From** — the test set they are in, said as its place in the test project
   rather than as a path on disk (Rule-INTERNAL-108).
3. **The button** — colored, and named for what it does (Rule-INTERNAL-105).

## Main flow

1. The tester selects four cards and presses `Delete`.
2. The **Confirm Removing** dialog opens.
3. The tester presses `Enter`.
4. The four test cases leave the editor, the test project and the automation
   code.
5. A message reads *Removed 4*.

## What Testin refuses

**If nothing is selected** — **Delete Test Case** is gray, reading *Select a
test case first.*

**In a test run editor** — **Delete Test Case** is gray, reading *A test run
keeps what it recorded, including for a test case that is gone. Delete the test
case in its test set.*

**If the tester presses `Escape`** — nothing is removed and nothing is said.

**If the recycle bin does not take a test case's file** — because the desktop
has none, or the file is open in another program — that test case stays, and
the editor reads its test set again with it there. A notification titled *Not
Removed* names the file (Rule-INTERNAL-143). *Removed* counts only the test
cases that went, and says nothing when none did.

## What goes with them

The test case's file, which goes to the recycle bin, and its generated test
method. Anything the tester wrote
inside that method goes with it, and the confirmation does not mention the code.

Undoing the removal brings the test cases back, and their methods with them, as
the tester left them (Rule-CODEGEN-114).

---

[Documentation](../README.md) › [The editor panel](main.md)
