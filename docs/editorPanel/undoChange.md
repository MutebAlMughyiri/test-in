[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-012

# UC-EDITOR-PANEL-012: Undo a change

**As a** tester, **I want** to take back what I just did, **so that** a wrong bulk edit across 30 test cases costs one
keystroke.

One press takes back the whole of the last change, however many test cases it
touched.

`Ctrl+Z`.

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
- **Rule-EDITOR-PANEL-067** — The menu entry says what the next press would take
  back.
- **Rule-EDITOR-PANEL-068** — A test case is put back exactly, including who
  last changed it and when.
- **Rule-EDITOR-PANEL-069** — A test case coming back from a removal gets its
  test method back as the tester left it, body included (Rule-CODEGEN-114).
- **Rule-EDITOR-PANEL-070** — A gesture that changed nothing is not on the
  history at all.
- **Rule-EDITOR-PANEL-228** — A test case moved from one test set to another is
  present only in the set it is in now. So a cut and paste taken back with
  Ctrl+Z comes back with Ctrl+Y, and only a change somebody else really made is
  refused as one.
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

This opens no screen. While the test cases are written back, a bar reads *Putting Back*; it cannot be canceled, because
a change half taken back is worse
than one not taken back at all. The test cases the change touched are then drawn
again as they were before it.

A small message then appears at the bottom of the IDE and fades. It reads *Undone*.

## Main flow

1. The tester changes the module on 30 test cases.
2. The tester presses `Ctrl+Z`.
3. A bar reads *Putting Back* while all 30 are written back exactly as they
   were.
4. Every editor open on that test set reloads, keeping its filters and its
   search.
5. A message reads *Undone*.

## What Testin refuses

**If there is nothing to take back** — the menu entry is gray and the key does
nothing.

**If something else has written those test cases since** — a message is shown,
titled **These test cases changed since**. It reads *Something else has written
them - a sync, a pull, or another IDE - so taking this back would write over
work that is not yours. Nothing was changed.*

**If a test case cannot be written back, or the recycle bin does not take its
file** — the others are still taken back. A message reads *unable to write
content:* and the reason, or a notification titled *Not Removed* names the file
(Rule-INTERNAL-143).
*Undone* is not shown, because not
everything was.

## What one press takes back

| The tester did                              | One press takes back |
|---------------------------------------------|----------------------|
| Changed one field on 30 test cases          | All 30               |
| Removed four test cases                     | All four             |
| Dragged three cards                         | All three            |
| Typed in one grid cell                      | That one cell        |
| Cut in one test set and pasted into another | Both halves          |

## What the history does not hold

The history belongs to this editor and this test set. A change made in the tree
is taken back in the tree. A change made in another editor is taken back there.
`Ctrl+Z` in the wrong place takes back the wrong thing, or nothing.

---

[Documentation](../README.md) › [The editor panel](main.md)
