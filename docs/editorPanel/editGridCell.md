[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-008

# UC-EDITOR-PANEL-008: Type straight into a grid cell

**As a** tester, **I want** to correct a value where I can see it, **so that** fixing five expected results does not
need five dialogs.

The cell turns into a box the tester can type in, right where it sits in the
table.

`Enter` on the cell, or a double click.

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
- **Rule-EDITOR-PANEL-047** — Only some columns can be typed into. The rest
  never open.
- **Rule-EDITOR-PANEL-048** — `Ctrl+Enter` puts a line break in. `Enter` saves.
- **Rule-EDITOR-PANEL-049** — Clicking away from an open cell saves it.
- **Rule-EDITOR-PANEL-050** — Testin stores the value it made of what the tester
  typed, and redraws the cell to match.
- **Rule-EDITOR-PANEL-051** — A cell that no longer shows what was typed into it
  says so, whether anything was saved or not.
- **Rule-EDITOR-PANEL-052** — A cell that ends up the same as it started writes
  nothing and says nothing.
- **Rule-EDITOR-PANEL-053** — Every cell saved is one entry on the undo history,
  named after the test case.
- **Rule-EDITOR-PANEL-206** — A value Testin cannot read is refused. What the
  test case already had stays, and the tester is told: once for a cell, and once
  with a count for a sheet or a bulk edit. A refused test case is not counted
  among the ones the change touched. Blank is not unreadable, and is never
  counted as unreadable — it clears a date and the groups, and it leaves the
  status alone. A blank priority in a bulk edit sets the default priority,
  Low; in a cell it leaves the priority alone.
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

No dialog opens. The cell itself becomes a box.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  #  | Description                 | Expected Result       | Priority     │
├──────────────────────────────────────────────────────────────────────────┤
│  1  | Log in with a valid user    | The dashboard opens.  | High         │
│  2  | Log in with a locked accou. |[The account is refu ] | Medium       │
│  3  | Log in with the wrong pass. | The password is ref.  | High         │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The open cell** — a box with a blue outline, with the cursor in it. Only
   one cell is open at a time.
2. **The row** — grows taller when the tester adds a line with `Ctrl+Enter`.
3. **Every other cell** — left as it was.
4. **The keyboard** — belongs to the open cell. `Enter` saves it. `Escape`
   throws the edit away.

## Which columns can be typed into

| Can be typed into                                                                                          | Cannot                                                                |
|------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| Description, Expected Result, Steps, Priority, Reference, Test Data, Pre Conditions, Group, Module, Status | Order, ID, FQCN, Path, Created By, Updated By, Created At, Updated At |

In a test run editor only **Actual Result** can be typed into.

## Main flow

1. The tester puts the cursor on a cell and presses `Enter`.
2. The cell opens as a box with a blue outline and the cursor in it.
3. The tester types. `Ctrl+Enter` adds a line, and the row grows to fit.
4. The tester presses `Enter`.
5. Testin reads the text, keeps what it can use, and writes it into the test
   case.
6. The cell is redrawn with the stored value.
7. A message reads *Updated*.
8. The cards behind the grid, and the details panel, both catch up.

## What Testin refuses

**If the column cannot be typed into** — the cell does not open. `Enter` there
does nothing at all, and nothing is said.

**If the value did not really change** — nothing is saved and nothing is said.

**If `Escape` is pressed while the cell is open** — the edit is thrown away and
nothing else happens.

**If any menu key is pressed while the cell is open** — it is refused. The cell
owns the keyboard until it is closed.

**If the editor has no test set to write to** — nothing is written, and only the
log says so.

**If Testin cannot read what was typed** — nothing is written, the cell redraws
with the old value, and the message names both the text and the column:
*Could not read "Urgent" as a Priority, so what was there stayed*
(Rule-EDITOR-PANEL-206).

## What Testin makes of what is typed

| The tester types                                   | What is stored                                               |
|----------------------------------------------------|--------------------------------------------------------------|
| Steps, one to a line                               | One step for each line                                       |
| A priority Testin does not know                    | Refused. The priority it had already, and Testin says so     |
| A status Testin does not know                      | Refused. The status it had already, and Testin says so       |
| A group Testin does not know                       | Refused whole. The groups it had already, and Testin says so |
| A description with characters Testin will not keep | Those characters removed                                     |

## Where the plugin breaks its own rules

**A description can change on screen with nothing saved and nothing said.** The
characters Testin will not keep are taken out and the cell is redrawn. If
nothing else changed, no save happens and no message appears, so the tester
watches their text change for no stated reason. That is difference 7 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
