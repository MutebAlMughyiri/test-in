[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-015

# UC-EDITOR-PANEL-015: Copy test cases

**As a** tester, **I want** to copy test cases into another test set, **so that** a set of login tests can be the start
of a set of sign-up tests.

This copies the test cases themselves, ready to be pasted. It does not copy
words a person can read.

This is the **Copy Test Case** entry on the right-click menu. It has no key of
its own.

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
- **Rule-EDITOR-PANEL-075** — Copying puts the test cases themselves on the
  clipboard, not their text.
- **Rule-EDITOR-PANEL-076** — Copying calls off any cut that was waiting, and
  the faded cards come back.
- **Rule-EDITOR-PANEL-077** — The entry works in both views, on a card and in
  the grid.
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

This opens no screen. Nothing on the list changes, because nothing has moved
yet.

A small message appears at the bottom of the IDE and fades. It reads *Copied*,
with a count after it for more than one test case.

## Main flow

1. The tester selects three cards.
2. The tester chooses **Copy Test Case** from the right-click menu.
3. The three test cases go on the clipboard.
4. A message reads *Copied 3*.
5. The tester opens another test set and chooses **Paste Test Case**.

## What Testin refuses

**If nothing is selected** — **Copy Test Case** is gray, reading *Select a test
case first.*

**In a test run editor** — **Copy Test Case** is gray, reading *A test run records
what happened to a test case, not the test case itself. Copy it in its test
set.* **Cut Test Case** and **Paste Test Case** refuse there for the same reason
([Rule-EDITOR-PANEL-214](keyboardMenu.md)).

**If writing to the clipboard fails** — nothing is said, and only the log
records it.

## What a pasted copy becomes

A copy is a new test case with a new identity, and its description gains the
word `(Copy)`. It gets a test method of its own, so the two do not collide. That
method starts with the body of the one it was copied from: the automation comes
with the test case, which is what a copy is for. Only the body: the copy's
method carries the copy's own identity, name and attributes, because it is a
different test case. Pasting is [UC-EDITOR-PANEL-017](pasteTestCases.md).

If the body calls something the destination class does not have, it does not
compile there and the IDE says so on the line, exactly as it would if you had
pasted the method by hand. Testin does not rewrite what you wrote.

## Two different copies

`Ctrl+C` and **Copy Test Case** are not the same. The first copies text a person can
read. The second copies test cases Testin can paste. They are two different
things, so the second has no key of its own: give it one in the Keymap if you
want one.

---

[Documentation](../README.md) › [The editor panel](main.md)
