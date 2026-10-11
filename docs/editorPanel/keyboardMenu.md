[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-029

# UC-EDITOR-PANEL-029: Open the menu from the keyboard

**As a** tester, **I want** the menu without reaching for the mouse, **so that** a whole test run can be walked with two
hands on the keyboard.

It is the same menu the right button opens. The key just opens it from the
keyboard.

The `Context Menu` key.

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
- **Rule-EDITOR-PANEL-123** — The menu opens on whatever is selected, in both
  views and in the tree.
- **Rule-EDITOR-PANEL-124** — With nothing selected, nothing opens.
- **Rule-EDITOR-PANEL-213** — The clipboard entries and the two history entries
  are gathered under one Actions entry that opens onto them, so the top level of
  the menu is what a tester opens it for. Every key those entries carry still
  works, on the cards and in the grid.
- **Rule-EDITOR-PANEL-214** — Actions holds the same seven entries in both
  editors. Copy Test Case, Cut Test Case, Paste Test Case and Delete Test Case
  are gray in a test run editor, with the reason on the entry. That is because a
  test run's test cases were chosen when it was created, and it keeps what it
  recorded. They are shown and refused rather than left out, so a tester who
  learns the gesture in one editor can find it in the other.
- **Rule-EDITOR-PANEL-230** — A menu entry that cannot work on what is selected
  is gray, and says why when the pointer rests on it. Nothing selected, a test
  run where only a test set will do, and a clipboard with no test cases each
  have their own reason.
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

This is the menu in a test set editor.

```
┌──────────────────────────────────┐
│  Create Test Case                │
│  View Test Case Details          │
│  ──────────────────────────────  │
│  Update Test Case                │
│  Actions                       > │
│  ──────────────────────────────  │
│  Automate Test Case              │
│  Run Test Method                 │
│  Navigate to Test Method         │
└──────────────────────────────────┘
```

1. **Where it opens** — a quarter of the way across the selected card, or on the
   selected cell in the grid.
2. **The separator lines** — they group entries that belong together.
3. **The last group of three** — gray in an IDE without the Java or TestNG
   plugin, and each names what it needs, as *Run Test Method (needs the TestNG
   plugin)*. A test run editor has two of them: **Automate Test Case** is not
   offered there.
4. **Moving in it** — the arrow keys move down the entries, and `Enter` chooses
   one.

## Main flow

1. The tester selects a card.
2. The tester presses the `Context Menu` key.
3. The menu opens a quarter of the way across the selected card.
4. The tester moves down it with the arrow keys and presses `Enter`.

In the grid the menu opens on the selected cell instead.

## What Testin refuses

**If nothing is selected** — nothing opens, and nothing is said.

## What the menu holds

In a test set editor, in this order: **Create Test Case**, **View Test Case
Details**, **Update Test Case**, **Actions**, **Automate Test Case**, **Run Test
Method**, **Navigate to Test Method**.

**Actions** opens onto the seven that act on the test case rather than on the
automation: **Copy Test Case Value**, **Copy Test Case**, **Cut Test Case**, **Paste Test Case**, **Delete Test Case**,
then **Undo** and **Redo**. They are
one level down because they all have keys, and none of them is what the menu is
opened for. At the top level, they pushed Automate Test Case, Run Test Method
and Navigate to Test Method off the end of a list nobody read that far down.

**The same seven are in the test run editor's menu**, in the same place and
under the same word. Four of them are gray there, each saying why on the entry:

| Entry                | Why it is gray in a test run                                                                                       |
|----------------------|--------------------------------------------------------------------------------------------------------------------|
| **Copy Test Case**   | *A test run records what happened to a test case, not the test case itself. Copy it in its test set.*              |
| **Cut Test Case**    | *A test run's test cases were chosen when it was created. Cut the test case in its test set.*                      |
| **Paste Test Case**  | *A test run's test cases were chosen when it was created. Paste into a test set.*                                  |
| **Delete Test Case** | *A test run keeps what it recorded, including for a test case that is gone. Delete the test case in its test set.* |

Shown and refused rather than left out. A menu that changes shape between
editors teaches a tester nothing, and they cannot learn that the gesture exists
or where it does work.

**Copy Test Case Value** is not one of the four. It copies the text of what is
selected, which a test run reads out as well as a test set does. **Copy Test
Case** is the one that reaches for the test case itself.

Each entry decides this for itself, from the node its editor is open on — the
same flag Import and Export read to find out whether a node can hold test
cases.

Paging is not on it. It moves the view and does nothing to the test case the
tester right-clicked, and the status bar already draws four arrows that each
print their own key.

The last group of three is gray in an IDE without the Java plugin or the
TestNG plugin, and each entry says which plugin it is waiting for.

In a test run editor, the menu holds the three run item statuses first: **Passed**, **Failed** and **Blocked**. Then
come **Failed Test Case Details**, then **View Test Case Details** and **Navigate to Test Case**, then **Actions**, then
**Run Test Method** and **Navigate to Test Method**. **Create Test Case**, **Update Test Case** and **Automate Test
Case** are not on it. A test run records run item statuses
rather than the test case, and it covers test cases that already exist.

---

[Documentation](../README.md) › [The editor panel](main.md)
