[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-007

# UC-EDITOR-PANEL-007: Change one field on many test cases at once

**As a** tester, **I want** to correct the same field on 30 test cases in one
go, **so that** a renamed module does not cost me half an hour of typing.

The bulk editor shows one field of every selected test case as a list. The
tester edits the list, and one save writes them all.

Select several test cases, then `F2` or the field's own letter.

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
- **Rule-EDITOR-PANEL-040** — The tester edits the values in place, in a list,
  with the original beside it.
- **Rule-EDITOR-PANEL-041** — A row the tester did not touch is not written, so
  it is not trimmed and its file is not changed.
- **Rule-EDITOR-PANEL-042** — A row the tester did edit has its spaces trimmed.
- **Rule-EDITOR-PANEL-043** — An edited value takes a green background, so it is
  clear what will be written.
- **Rule-EDITOR-PANEL-044** — A line break inside a value is shown as two
  characters and read back as a line break.
- **Rule-EDITOR-PANEL-045** — A test case with nothing in the field still gets a
  line to type into.
- **Rule-EDITOR-PANEL-206** — A value Testin cannot read is refused. What the
  test case already had stays, and the tester is told: once for a cell, and once
  with a count for a sheet or a bulk edit. A refused test case is not counted
  among the ones the change touched. Blank is not unreadable, and is never
  counted as unreadable — it clears a date and the groups, and it leaves the
  status alone. A blank priority in a bulk edit sets the default priority,
  Low; in a cell it leaves the priority alone.
- **Rule-EDITOR-PANEL-046** — The whole gesture is one entry on the undo
  history.
- **Rule-EDITOR-PANEL-224** — A description is refused here for the two reasons
  the update dialog refuses one: it cannot name a Java method, or it names the
  same method as another test case in the test set - one edited in this same
  dialog or one left as it was. A refused row is left as it was, and one
  message says why, in the words the update dialog uses.
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
┌────────────────────────────────────────────────────────────────────────────┐
│  Bulk Edit Descriptions                                                    │
├──────────────────────────────────┬─────────────────────────────────────────┤
│ 1  [                             │ 1  [                                    │
│ 2    {                           │ 2    {                                  │
│ 3      "id": "3f2a...",          │ 3      "id": "3f2a...",                 │
│ 4      "description": "Log in"   │ 4      "description": "Sign in"         │
│ 5    },                          │ 5    },                                 │
│ 6    {                           │ 6    {                                  │
│ 7      "id": "8b44...",          │ 7      "id": "8b44...",                 │
│ 8      "description": "Log out"  │ 8      "description": "Log out"         │
│ 9    }                           │ 9    }                                  │
│10  ]                             │10  ]                                    │
├──────────────────────────────────┴─────────────────────────────────────────┤
│  [k] Enter Save   Shift+Enter Save   Tab Next   Shift+Tab Previous         │
│      Down Next   Up Previous   Ctrl+Shift+A All Carets                     │
│      Ctrl+Click Multi-Caret   Escape Cancel                                │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The left side** — the values as they are now. It cannot be typed into.
2. **The right side** — the same values, and only the values can be typed into.
   Everything around them is locked.
3. **A changed value** — takes a green background.
4. **The line facing the cursor** — highlighted on the left, so the two sides
   line up.

## The nine bulk editors

**Bulk Edit Descriptions**, **Bulk Edit Expected Results**, **Bulk Edit
Modules**, **Bulk Edit Test Data**, **Bulk Edit Pre-Conditions**, **Bulk Edit
Steps**, **Bulk Edit Priorities**, **Bulk Edit Statuses**, **Bulk Edit Groups**.

**Bulk Edit Steps** and **Bulk Edit Groups** hold lists, so their strip carries
two keys more: `Ctrl+Enter` **Add** to add an item and `Shift+Delete` **Remove**
to drop one.

## Main flow

1. The tester selects 30 test cases and presses `M`.
2. **Bulk Edit Modules** opens with the 30 values on each side.
3. The tester presses `Ctrl+Shift+A` to put a cursor at the end of every value.
4. The tester types the new module once, and every value changes.
5. Each changed value turns green.
6. The tester presses `Enter`.
7. Only the rows that changed are written.
8. A message reads *Updated 30*, counting the test cases written.

## What Testin refuses

**If the tester chooses Order** — a message reads *Order is set one test case at
a time*. There is no bulk editor for it.

**If a description is edited to nothing** — that row is left as it was. A
blank is not a value Testin could not read, so it is not counted as one (Rule-EDITOR-PANEL-206).

**If a description cannot name a Java method, or names the same method as
another test case in the test set** — that row is left as it was, and a message
says why in the update dialog's own words: *That description cannot name a test
method*, or *Another test case already names that test method*. Two rows given
one description clash with each other; two rows that swap their descriptions do
not (Rule-EDITOR-PANEL-224).

**If a priority is edited to nothing** — it is set to the default priority,
Low, and counted among the test cases the edit changed.

**If a value is not one Testin can read** — a priority or a status it does not
know — that test case is left exactly as it was and is not counted
among the ones the edit changed. One message says how many:
*Could not read 3 values, so what was there stayed* (Rule-EDITOR-PANEL-206).

**If the cursor is put on the locked text around a value** — it moves to the
nearest place the tester can type. No message is shown.

**If a key would change the locked text** — nothing happens. The platform's own
warning is hidden.

**`Shift+Enter` also saves**, and the strip names it. It is there because the
gesture that normally inserts a line break must not put a newline inside a value
the shape says is one line.

---

[Documentation](../README.md) › [The editor panel](main.md)
