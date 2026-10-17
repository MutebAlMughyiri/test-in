[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-006

# UC-EDITOR-PANEL-006: Change one field of one test case

**As a** tester, **I want** to correct one field without opening a form with
twenty boxes on it, **so that** fixing a typo takes two keys.

One field, one small dialog. The rest of the test case is left alone.

`F2` opens the menu of fields. Each field also has a letter of its own.

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
- **Rule-EDITOR-PANEL-035** — Only the field the tester opened may be written
  back. Every other field on the dialog is gray.
- **Rule-EDITOR-PANEL-036** — The dialog always shows the description, and shows
  the expected result when it is not empty, so the tester can see what they are
  changing.
- **Rule-EDITOR-PANEL-038** — One gesture is one entry on the undo history,
  however many test cases it changed.
- **Rule-EDITOR-PANEL-039** — Undo puts the test case back exactly, including
  who last changed it and when.
- **Rule-EDITOR-PANEL-194** — Reference is on the update menu with the letter
  `R` and its icon, after Order. Status sits last with the letter `Z` and its
  icon, the same as on the copy menu, so every field on the menu has a letter;
  `Z` on a selected card opens Status straight away, as `D` opens Description.
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
- **Rule-EDITOR-PANEL-258** — Status is radio buttons, not a list, so a tester
  sees all four - Reviewed, Pending, Disabled, To Be Updated - and picks one in
  a single click. A new test case starts Pending; an update starts on the test
  case's own status.
- **Rule-EDITOR-PANEL-259** — The update menu holds every field a tester can
  edit in the grid. A field that can be edited there and is missing from the
  menu fails the build.
- **Rule-EDITOR-PANEL-293** — Ctrl+= adds one more box to the list field the
  cursor is in, Steps or Group, and puts the cursor in it. Ctrl++ and Ctrl with
  the numpad + do the same. A field's own key, Ctrl+S or Ctrl+G, only opens that field; pressed again it
  moves the cursor to the field and adds nothing. With the cursor anywhere else,
  Ctrl+= does nothing.

## The screen

```
┌────────────────────────────────────────┐
│  Update Test Case                      │
├────────────────────────────────────────┤
│  [D] Description         D             │
│  [E] Expected Result     E             │
│  [M] Module              M             │
│  [T] Test Data           T             │
│  [B] Pre Conditions      B             │
│  [S] Steps               S             │
│  [P] Priority            P             │
│  [G] Group               G             │
│  [O] Order               O             │
│  [R] Reference           R             │
│      Status                            │
└────────────────────────────────────────┘
```

1. **Each row** — the field's icon, the field, then the letter that opens it.
   The icon is that letter in a rounded frame, the same icon the field has
   wherever it is offered.
2. **The first row** — selected when the menu opens.
3. **Reference** — `R`, one line of text. **Status** is the one row with no
   letter and no icon, reached with the arrow keys. It is four radio buttons, as **Priority** is, and `Enter` saves the
   dialog from either.

Pressing the letter on the card skips this menu and opens the field straight
away.

## Main flow

1. The tester selects a card.
2. The tester presses `E`, or presses `F2` and picks **Expected Result**.
3. The **Update Expected Result** dialog opens with the current value in it.
4. Every other field on the dialog is gray.
5. The tester types and presses `Enter`.
6. Testin writes the test case.
7. A message reads *Updated*.
8. The list is sorted again and redrawn, and the automation code is rewritten.

## What Testin refuses

**If nothing is selected** — **Update Test Case** is gray, reading *Select a
test case first.*, and `F2` does nothing.

**If the save changed nothing** — the dialog closes and nothing at all happens.

**If the description is emptied** — the field turns red and the dialog stays
open.

**If an update dialog is already open** — it is brought forward rather than a
second one opened, so nothing typed into it is lost (Rule-INTERNAL-075).

**If the description cannot name a Java method** — the same message the create
dialog shows.

**If another test case in this test set already names that method** — nothing is
saved, and the same message the create dialog shows. The test case being edited
is not compared against itself, so a description the tester left unchanged is
never refused.

**If several test cases are selected** — the menu title becomes **Update**, then
the count, then **Test Cases**. Picking a field opens the bulk editor instead.
That is [UC-EDITOR-PANEL-007](bulkEdit.md).

**In a test run editor** — **Update Test Case** is gray, reading *A test run
records run item statuses, not the test case. Change the test case in its test
set.*

## The dialog cannot be moved

The update dialog cannot be moved or resized. The create dialog can. Nothing
explains the difference.

---

[Documentation](../README.md) › [The editor panel](main.md)
