[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-027

# UC-EDITOR-PANEL-027: Refresh the editor from disk

**As a** tester, **I want** to read the test set again from disk, **so that** I see what a colleague's sync brought in.

This reads the test set from disk again. It writes nothing.

There is no key for this. The button's tooltip reads **Refresh**.

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
- **Rule-EDITOR-PANEL-117** — Refresh clears every filter, as Reset Filters
  does, puts the sort back to Order, Ascending, and keeps the search text. It
  reads the data again, rebuilds in the background the values the completion
  fields offer, and changes nothing else about the view.
- **Rule-EDITOR-PANEL-118** — Refresh remembers which test case was selected,
  and lands on the page holding it.
- **Rule-EDITOR-PANEL-119** — The tester's own refresh always reloads, even with
  a grid cell open. A refresh Testin starts on its own leaves a busy editor
  alone.
- **Rule-EDITOR-PANEL-120** — In a test run editor, refresh also stops the
  execution, and the message says so. Refresh reads the test run again from disk
  and the walk goes with the copy it replaces.
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

This opens no screen. The list empties and reads *Refreshing...* while Testin
reads the test set from disk again. Then the page holding the selected test case
is drawn.

A small message appears at the bottom of the IDE and fades. It reads *Refreshed*.

## Main flow

1. The tester presses the refresh button.
2. The selected test case is remembered.
3. The list empties and reads *Refreshing...*.
4. Testin reads the test set from disk again.
5. Every filter is cleared and the sort goes back to Order; the search text stays.
6. The page holding the remembered test case is drawn.
7. A message reads *Refreshed*. In a test run editor where an execution was
   running, it reads *Refreshed, and the execution stopped*.
8. In the background, Testin rebuilds the values the completion fields offer
   from every test case. A value no test case uses anymore, such as a group
   renamed with Update Test Case, is no longer offered.

## What Testin refuses

Nothing.

## In a test run editor

Refresh also stops the execution. The clock stops, the walk ends, and the
toolbar button turns back into **Start Manual Execution**. The message says *Refreshed, and the execution stopped*, so
the tester is not left wondering why
the button changed.

## What completion offers

The fields that complete what has been typed before - Description, Expected
Result, Module, Steps and Group - all read one list of the values Testin knows.
That list changes only at these moments. The **Group** list in the filter does
not read it: it is built from the test cases in front of the filter, as the
module list is (Rule-EDITOR-PANEL-095).

| What the tester does                                                                                                               | What happens to the list                                                                                                                                |
|------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Opens a test set or a test run                                                                                                     | The values of the test cases it shows are added                                                                                                         |
| Creates a test case                                                                                                                | Its values are added at once                                                                                                                            |
| Changes a test case: Update Test Case, a grid cell, the details panel or a bulk edit                                               | Nothing. A value it no longer has is still offered, and a new value is offered only once an editor reloads or the tester presses Refresh                |
| Copies test cases into a test set                                                                                                  | Nothing, until an editor reloads or the tester presses Refresh                                                                                          |
| Imports test cases                                                                                                                 | The test set opens again in the test set editor, and their values are added                                                                             |
| Removes a test case: Delete, cutting it into another test set, undoing its creation, or reverting it in the pending changes review | The list is rebuilt in the background from the test cases left. A value only that test case used is no longer offered                                   |
| Undoes or redoes a change                                                                                                          | The open editors of the test sets it changed reload, and their values are added. A test case the undo takes out rebuilds the list, as removing one does |
| Presses Refresh on the tree, syncs with Git, or changes a file outside Testin                                                      | The open editors reload, and their values are added. Nothing is taken out                                                                               |
| Presses **Refresh** on the editor toolbar                                                                                          | The list is rebuilt in the background from every test case. A value no test case uses any more is no longer offered                                     |
| Starts the IDE again                                                                                                               | The list starts empty, and fills as editors open                                                                                                        |

So a group renamed from *Smoke* to *Sanity* is still offered as *Smoke* until
the tester presses Refresh. Creating and removing a test case are the only two
changes that update the list on their own.

## Refreshing on its own

Testin also reads a test project again when something changes it on disk, with
no button pressed. That is
[UC-INTERNAL-003](../internal/noticeOutsideChange.md), and it does not clear the
filters.

---

[Documentation](../README.md) › [The editor panel](main.md)
