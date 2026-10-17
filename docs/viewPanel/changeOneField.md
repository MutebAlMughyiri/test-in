[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-011

# UC-VIEW-PANEL-011: Change one field without leaving the panel

**As a** tester, **I want** to correct a field I have just noticed is wrong, **so that** I do not have to go back to the
editor and find the test case
again.

One field, one small dialog. The rest of the test case is left alone.

`F2` opens the menu of fields.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has two tabs, Details and History, and
  both are drawn every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-044** — `F2` works only once a test case has been drawn. It
  does nothing while the panel is empty.
- **Rule-VIEW-PANEL-045** — The panel always changes exactly one test case,
  because it only ever shows one.
- **Rule-VIEW-PANEL-046** — An edit is written to the test set that holds the
  test case, wherever the view panel was opened from, a search result included.
- **Rule-VIEW-PANEL-047** — A save that would leave the file as it is writes
  nothing, and raises no message.
- **Rule-VIEW-PANEL-048** — A saved change is one entry on the undo history.
- **Rule-VIEW-PANEL-049** — Saving a change rewrites the automation code for
  that field, where the field has any.
- **Rule-VIEW-PANEL-110** — `F2` changes nothing from a run item of a Committed
  test run, or from a test case that was deleted: a short message says why, and
  to change the test case in its test set.

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
│  [U] Status              U             │
└────────────────────────────────────────┘
```

1. **Each row** — the field's icon, its name, then the letter that opens it.
   The icon is that letter in a rounded frame, the same icon the field has
   wherever it is offered.
2. **The first row** — selected when the menu opens.
3. **Status** — the last row, with the letter `Z` as on the copy menu. **Reference** has the letter `R`.

Choosing a row opens that field's own small dialog. Those dialogs belong to the
editor panel. They are drawn on
[UC-EDITOR-PANEL-006](../editorPanel/changeOneField.md).

## Main flow

1. The panel is showing a test case, and the keyboard is in the panel.
2. The tester presses `F2`.
3. The **Update Test Case** menu opens, one row for each field.
4. The tester presses the field's letter, or picks the row.
5. That field's dialog opens with the current value in it.
6. The tester types and presses `Enter`.
7. Testin writes the test case and shows *Updated*, or *Re-sorted* when the
   field was **Order**, the same words the editor gives.
8. The panel redraws with the new value.

## What Testin refuses

**If the panel is showing no test case** — `F2` does nothing.

**If the panel has never drawn a test case** — `F2` does nothing at all. The key
starts working the first time a test case is drawn.

**If the save would change nothing** — nothing is written, no message is raised,
and nothing goes on the undo history.

**If the test case was deleted** — `F2` opens nothing and says *This test case
was deleted, so it cannot be changed.* (Rule-VIEW-PANEL-110)

**If the test run is Committed** — `F2` opens nothing and says the test run
keeps the test case as it was. (Rule-VIEW-PANEL-110)

## Closing the panel afterward

`F2` needs the keyboard to be in the panel, and it leaves it there. `Escape`
works from there too: it closes the panel from inside a tab exactly as it does
from the editor (Rule-VIEW-PANEL-058). That is
[UC-VIEW-PANEL-015](closePanel.md).

---

[Documentation](../README.md) › [The view panel](main.md)
