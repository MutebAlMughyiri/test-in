[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-016

# UC-EDITOR-PANEL-016: Cut test cases

**As a** tester, **I want** to move test cases into another test set, **so that** a test case written in the wrong place
ends up in the right one.

A cut on its own changes nothing. The test cases move when the tester pastes.

the **Cut Test Case** entry on the right-click menu. It has no key of its own.

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
- **Rule-EDITOR-PANEL-078** — A cut test case is drawn faded, so the tester can
  see what is waiting to move.
- **Rule-EDITOR-PANEL-079** — Nothing is removed until the paste. A cut on its
  own changes nothing.
- **Rule-EDITOR-PANEL-080** — A cut is called off by a paste, by `Escape`, by
  removing test cases, and by anything at all being written to the clipboard.
  There is one clipboard, so a copy in the tree, a copied grid selection, a
  copied id, or a copy made in another application all take the cut with them:
  what the paste would put down is no longer the test cases that are faded.
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

This opens no screen. The cut cards stay where they are and are drawn faded, so
the tester can see what is waiting to move.

A small message appears at the bottom of the IDE and fades. It reads *Cut*, with
a count after it for more than one test case.

## Main flow

1. The tester selects two cards.
2. The tester chooses **Cut Test Case** from the right-click menu.
3. The two cards are drawn faded.
4. A message reads *Cut 2*.
5. The tester opens the other test set and chooses **Paste Test Case**.
6. The two test cases leave the first test set and appear in the second.

## What Testin refuses

**If nothing is selected** — **Cut Test Case** is gray.

**If writing to the clipboard fails** — nothing is said, and only the log
records it.

**If the tester never pastes** — nothing happens. The test cases stay where they
are, drawn faded until the cut is called off.

## What a moved test case keeps

It keeps its identity, so the run item statuses in every test run still point
at it. It keeps its own test method too. Only its place changes.

## Undoing a move

The cut and the paste are one entry on the undo history, named as a move. One
press of `Ctrl+Z` puts the test cases back where they came from.

---

[Documentation](../README.md) › [The editor panel](main.md)
