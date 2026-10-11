[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-024

# UC-EDITOR-PANEL-024: Select test cases

**As a** tester, **I want** to pick several test cases at once, **so that** one gesture changes all of them.

Almost every other page here starts with a selection. This is how one is made.

There is no key that starts this. Click, `Ctrl`-click and `Shift`-click.

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
- **Rule-EDITOR-PANEL-108** — Several test cases can be selected, in as many
  separate runs as the tester likes.
- **Rule-EDITOR-PANEL-109** — Clicking outside every card clears the selection.
- **Rule-EDITOR-PANEL-110** — Right-clicking outside the selection moves the
  selection to what was clicked first.
- **Rule-EDITOR-PANEL-111** — The grid's selection and the cards' selection are
  always the same. Changing one changes the other.
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

This opens no screen. The selected cards, or the selected cells, are drawn with
a highlight behind them.

The left of the status bar changes to say what is selected. Nothing else
happens, and no message appears.

## How to select

| In the cards                   | In the grid                                             |
|--------------------------------|---------------------------------------------------------|
| Click a card                   | Click a cell                                            |
| `Ctrl`-click to add one        | Drag across cells                                       |
| `Shift`-click to take a range  | Click the number column to take a whole row             |
| Click the empty space to clear | `Ctrl`-click the number column to add a row             |
|                                | `Shift`-click the number column to take a range of rows |

## What the status bar says

| The selection              | What it reads                               |
|----------------------------|---------------------------------------------|
| Nothing                    | *12 test cases*, the count alone            |
| One test case              | Its position, then *of 12 test cases*       |
| Several                    | The count, then *selected of 12 test cases* |
| Anything, with a filter on | The same, then *(filtered from 120)*        |

## What Testin refuses

**If the tester right-clicks inside the selection** — the menu acts on the whole
selection, not on the card under the pointer.

**If the tester right-clicks outside the selection** — the selection moves to
that card first, then the menu opens.

**With nothing selected** — the status bar reads the count alone, *12 test
cases*. It used to read *0 of 12 test cases*, putting a position where a tester
reads a count.

---

[Documentation](../README.md) › [The editor panel](main.md)
