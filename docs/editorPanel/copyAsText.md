[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-014

# UC-EDITOR-PANEL-014: Copy a test case's details as text

**As a** tester, **I want** a test case as plain text on my clipboard, **so that** I can paste it into a chat message or
a ticket.

This copies words a person can read. It does not copy the test case itself.

`Ctrl+C` on the cards opens a menu of what can be copied. A letter picks one.

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
- **Rule-EDITOR-PANEL-073** — Each test case is written as the field name, a
  colon, then the value.
- **Rule-EDITOR-PANEL-074** — Several test cases are separated by a blank line.
- **Rule-EDITOR-PANEL-207** — **All Details** copies every field the tester
  wrote on the test case: the description, the expected result, the steps, the
  priority, the reference, the test data, the pre-conditions, the group, the
  module and the status. A field left empty is not a line.
- **Rule-EDITOR-PANEL-208** — Any one of those can be copied on its own, and so
  can the class name, the identity and the path — which the tester never writes
  and often needs. A single value is copied bare, with no caption in front of
  it.
- **Rule-EDITOR-PANEL-209** — The menu's letters are the update menu's letters
  wherever the field is the same, so `D` is the description in both.
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
┌──────────────────────────────────────────────┐
│  Copy                                        │
├──────────────────────────────────────────────┤
│  > [A] All Details                       A   │
│    [D] Description                       D   │
│    [E] Expected Result                   E   │
│    [S] Steps                             S   │
│    [B] Pre Conditions                    B   │
│    [T] Test Data                         T   │
│    [P] Priority                          P   │
│    [M] Module                            M   │
│    [G] Group                             G   │
│    [U] Status                            U   │
│    [R] Reference                         R   │
│    [F] FQCN                              F   │
│    [I] ID                                I   │
│    [H] Path                              H   │
└──────────────────────────────────────────────┘
```

1. **All Details first, and already selected** — so `Ctrl+C` then `Enter` copies
   the whole test case, which is what `Ctrl+C` did on its own before the menu.
2. **A letter on each row** — pressing it copies that value and closes the menu.
   `Ctrl+C` then `D` is as quick as one keystroke for a tester who knows it.
   Each row's icon is that letter in a frame. A field Create Test Case also has,
   such as the description, shows the same icon there.
3. **The last three** — the class name, the identity and the path. Testin writes
   those; a tester pastes them into a stack trace, a ticket or a search.

Nothing on the list changes, because nothing was written. A small message appears
at the bottom of the IDE and fades: *Description copied*, with a count after it
when more than one card was selected.

## Main flow

1. The tester selects two cards and presses `Ctrl+C`.
2. The **Copy** menu opens, with **All Details** selected.
3. The tester presses `D`.
4. Both descriptions go on the clipboard, one block each.
5. A message reads *Description copied 2*.

## What Testin refuses

**If nothing is selected** — **Copy** is gray and the key does nothing.

**If the grid is showing** — `Ctrl+C` belongs to the grid there, and copies the
selected cells instead. The menu does not open. That is
[UC-EDITOR-PANEL-018](gridClipboard.md).

**If the tester presses `Escape`** — the menu closes and nothing is copied.

**If the value is empty** — it is copied as nothing, and the message still says
so. The menu offers every value the test case can hold, not only the ones this
test case has.

A tester who wants who changed the test case and when should export the test
set instead. That is [UC-SHARE-001](../share/exportTestSet.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
