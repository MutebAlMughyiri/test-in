[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-009

# UC-EDITOR-PANEL-009: Move a test case by typing its number

**As a** tester, **I want** to say a test case is number three, **so that** I can put it where I want without dragging
it through 200 rows.

The tester types the position they want. Testin moves the test case there.

`O` on the selected card.

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
- **Rule-EDITOR-PANEL-054** — The number is the position in the whole test set,
  counting from one.
- **Rule-EDITOR-PANEL-055** — Positions are read with this test case taken out
  of the list, so typing three puts it third.
- **Rule-EDITOR-PANEL-056** — Only this test case's own file is written. The
  test cases around it are not touched.
- **Rule-EDITOR-PANEL-223** — The one exception to the rule above, and it
  happens at most once per test set: a set holding test cases that have never
  been placed - imported, brought by a sync, or written by hand - is given
  places first, the same way a drag gives them, and those files are written too.
  Without it a position between two unplaced test cases means nothing, and the
  test case lands where it already was.
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
- **Rule-EDITOR-PANEL-273** — In a test set editor, Enter or a double click on a
  grid Order cell opens the O dialog for that row, as pressing O does, and every
  row's Order then shows its new number. In a test run editor the Order column
  cannot be changed.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Update Order                                                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Log in with a valid user                                    │
│                                                              │
│  [   3 ]  / 12                                               │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Save       Escape Cancel                         │
└──────────────────────────────────────────────────────────────┘
```

1. **The box** — the test case's position now.
2. **of, then the number** — how many test cases the test set holds.

## Main flow

1. The tester selects a card and presses `O`.
2. The **Update Order** dialog opens with the current position in the box.
3. The tester types a new number and presses `Enter`.
4. Testin works out a new place for it, between the two test cases it now sits
   between.
5. Only this test case's file is written - unless the test set holds test
   cases that have never been placed, which are given places first, once.
6. A message reads *Re-sorted*, the same word dragging a card gives.
7. Every card is renumbered.

## What Testin refuses

**If the number is below one, or above how many test cases there are** — a
message says so, in the platform's own words, and nothing is saved.

**If the number is where the test case already is** — nothing is written at all.

**If several test cases are selected** — the **Order** row on the update menu is
gray. It reads *Order is set one test case at a time* in place of its key. The
refusal is read before it is triggered, not after. Pressing `O` on the cards,
which has no menu to gray, still answers with that message.

---

[Documentation](../README.md) › [The editor panel](main.md)
