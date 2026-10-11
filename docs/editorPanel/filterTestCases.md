[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-020

# UC-EDITOR-PANEL-020: Filter the test cases

**As a** tester, **I want** to show only the test cases in one group or one
module, **so that** I can work through the smoke tests without the rest in the
way.

A filter hides rows. It never changes or deletes a test case.

There is no key for this. The button's tooltip reads **Filter**.

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
- **Rule-EDITOR-PANEL-094** — The Filter menu is the same in the test set
  editor, the test run editor and the Create Test Run dialog: the priority, the
  automation, the group, the module, the status, the run item status and the
  test set. An entry that cannot work there is gray, and hovering it says why.
- **Rule-EDITOR-PANEL-095** — The module list is built from the test cases in
  this test set, so it only ever offers modules that exist.
- **Rule-EDITOR-PANEL-096** — Choosing nothing in a filter means every test case
  matches it.
- **Rule-EDITOR-PANEL-097** — Filtering or sorting goes back to the first page.
- **Rule-EDITOR-PANEL-098** — The button says how many filters are on.
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
- **Rule-EDITOR-PANEL-260** — Test Set filters a test run editor and the
  Create Test Run dialog to the chosen test sets, listed in the tree's order and
  named by their place under the test cases' folder. In a test set editor it is
  gray, and its reason says a test set editor shows one test set.
- **Rule-EDITOR-PANEL-261** — Status filters on the test case's own status -
  Reviewed, Pending, Disabled, To Be Updated - and works in both editors and the
  Create Test Run dialog. Run Item Status is a different thing: what a test run
  recorded for each run item, such as Passed or Failed. It is on every menu, and
  gray in a test set editor, where nothing has a run item; hovering it says so.

## The screen

```
┌──────────────────────────────────────┐
│  Reset Filters                       │
│  ──────────────────────────────────  │
│  Priority                          > │
│  Automation                        > │
│  Group                             > │
│  Module                            > │
│  Status                            > │
│  Run Item Status                   > │
│  Test Set                            │
└──────────────────────────────────────┘
```

1. **Reset Filters** — always the first entry, and gray while no filter is on. It
   is [UC-EDITOR-PANEL-021](clearFilters.md).
2. **Priority** — **High**, **Medium** and **Low**, each with its own color.
3. **Automation** — the three automation states, which is
   [UC-EDITOR-PANEL-047](seeWhatIsAutomated.md).
4. **Group** — every group Testin knows, with **No Group** first.
   [When that list changes](refreshEditor.md#what-completion-and-the-group-filter-offer)
   is written on the refresh page.
5. **Module** — the modules the test cases in this test set actually carry.
6. **Status** — the test case's own status: **Reviewed**, **Pending**, **Disabled** and **To Be Updated**. It works in
   both editors and in the
   Create Test Run dialog.
7. **Run Item Status** — what the test run recorded for each run item, such as **Passed** or **Failed**. Gray in a test
   set editor, where hovering it says only a test run has run items.
8. **Test Set** — the test sets a test run holds, in the tree's order, named like **Payments / Cards**. Gray in a test
   set editor, where hovering it says a test set editor shows one test set. The same menu opens in the
   [Create Test Run](../treePanel/createTestRun.md) dialog, where it offers the test sets the dialog holds.

While a filter is on, the button shows the count in brackets. Its tooltip
becomes **Filter**, then the count, then **active**.

## Main flow

1. The tester presses the filter button.
2. The menu opens under it.
3. The tester opens **Group** and ticks **Smoke**.
4. The list narrows at once, and goes back to the first page.
5. The button reads **(1)**.
6. The status bar says how many test cases are showing, and how many were
   filtered out.

## What Testin refuses

**If nothing matches** — the middle reads *No test cases match the search*.

**If the module has never been used** — it is not offered. The list is built
from what is there.

**In a test set editor** — **Run Item Status** and **Test Set** are gray, and
hovering either says why: only a test run holds run items, and a test set editor
shows one test set. The label stays plain; the reason is never written beside it.

---

[Documentation](../README.md) › [The editor panel](main.md)
