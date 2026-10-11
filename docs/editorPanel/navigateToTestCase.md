[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-048

# UC-EDITOR-PANEL-048: Go from a test run to a test case

**As a** tester executing a test run, **I want** to open the test case behind a
row in its own test set, **so that** I can read it or fix it where it lives.
I do not have to hunt for it in the tree.

The test case's own test set opens in the test set editor with the test case
selected, as clicking its identity in the view panel does. The test run editor
stays open behind it.

No key. The card's last button, or the right-click menu.

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
- **Rule-EDITOR-PANEL-233** — Navigate to Test Case opens the test case's own
  test set in the test set editor and selects it there, as clicking its
  identity in the view panel does. The tree does not move.
- **Rule-EDITOR-PANEL-234** — It is on a test run's cards and in the right-click
  menu of the test run editor. It is not offered in a test set, on a card or in
  the menu: those are already in the test set.
- **Rule-EDITOR-PANEL-235** — On a card it is drawn only while the pointer is on
  that card, as the card's other buttons are. It is the last of them, after Run,
  so the two buttons testers already use keep their places. Its icon is the
  letter frame the Create Test Case dialog uses, reading tc, frame and letters in
  one green, the same in a light theme and a dark one. It sits in the same slot as
  every other button on the card, and grows under the pointer like the others.
- **Rule-EDITOR-PANEL-236** — A test case that is in no test set is refused with
  a message, and nothing opens.
- **Rule-EDITOR-PANEL-237** — One gesture has one name wherever it is offered.
  The menu entry, the card's button and the view panel's identity all say
  Navigate to Test Case; the method's entries and buttons say Navigate to Test
  Method and Run Test Method, because the method is what they reach and run.
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
┌─────────────────────────────────────────────────────────────────────────┐
│  Sign in with a correct username and password         [m]  [▶]  [tc]    │
│  Pending                                                        (1)     │
└─────────────────────────────────────────────────────────────────────────┘
```

1. **The buttons** — drawn only while the pointer is on the card: Navigate to
   Test Method, Run, then **tc**, Navigate to Test Case. The one under the
   pointer grows and names itself.

The right-click menu of the test run editor offers **Navigate to Test Case**
beside **View Test Case Details**. It is not offered in a test set, on a card or
in the menu.

## Main flow

1. The tester is working through a test run.
2. The tester rests the pointer on a row's card, and its buttons appear, the
   last one reading **tc**.
3. The tester clicks it, or right-clicks the row and chooses **Navigate to Test
   Case**.
4. The test case's own test set opens in the test set editor, or comes forward
   if it is already open, with the test case selected.
5. The tree stays where it was. The test run editor stays open behind.

## What Testin refuses

**If the test case is in no test set** — deleted since the test run recorded it
— the button is gray, and a message reads *There is no test set to open this
test case in.*, and nothing opens.

---

[Documentation](../README.md) › [The editor panel](main.md)
