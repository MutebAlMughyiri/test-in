[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-028

# UC-EDITOR-PANEL-028: See the test set's own details

**As a** tester, **I want** to know what this test set holds and who made it, **so that** I can answer a question about
it without leaving the editor.

This is about the test set or the test run, not about one test case.

There is no key for this. The button is at the far right of the toolbar.

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
- **Rule-EDITOR-PANEL-121** — The dialog is about the node the editor is
  showing, not about the test case that is selected.
- **Rule-EDITOR-PANEL-122** — The counts are worked out as the dialog opens and
  stored nowhere.
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
- **Rule-EDITOR-PANEL-281** — The Details dialog closes on Escape or on a click
  anywhere outside it: it only reads, so there is nothing to lose.

## The screen

```
┌────────────────────────────────────────────────────────────┐
│  Details                                                   │
├────────────────────────────────────────────────────────────┤
│  NAME                                                      │
│  Login                                                     │
│  PATH                                                      │
│  C:\...\Demo\Test Cases\Login                              │
│  CREATED BY                                                │
│  mtb                                                       │
│  CREATED AT                                                │
│  12 Aug 2026 09:14                                         │
│  UPDATED BY                                                │
│  mtb                                                       │
│  UPDATED AT                                                │
│  03 Sep 2026 16:02                                         │
│  STATUS                                                    │
│  Active                                                    │
│  TEST CASES                                                │
│  12                                                        │
│                                                            │
│           ( a ring chart of what this node holds )         │
├────────────────────────────────────────────────────────────┤
│  [k]  Escape Close                                         │
└────────────────────────────────────────────────────────────┘
```

1. **Name** and **Path** — what the node is called, and where it sits on disk.
2. **The four middle rows** — who made it and when, and who changed it last and
   when.
3. **Status** — the node's own status. A node with no status of its own has no
   row here.
4. **The counts** — how much the node holds. A test set counts its test cases.
5. **The ring** — a chart under the rows. In a test set it shows what the node
   holds. In a test run it shows one slice for each run item status.
6. **The strip at the bottom** — the only key the dialog answers.

A test run shows more rows: when it started, when it ended, how long it took,
and the answers the tester gave when it was created. The same rows the tree's
own dialog draws, which is [UC-TREE-PANEL-027](../treePanel/nodeDetails.md).

## Main flow

1. The tester presses the last button on the toolbar.
2. The **Details** dialog opens on the test set the editor is showing.
3. It says the name, the path, who made it and when, its status, and how many
   test cases it holds.
4. `Escape` closes it.

The same dialog opens from the tree, and is drawn on
[UC-TREE-PANEL-027](../treePanel/nodeDetails.md). What the counts mean is on
[UC-INTERNAL-006](../internal/countNodeContents.md).

## What Testin refuses

Nothing.

## What else is on the toolbar

The tooltip of this button reads **Details**, and the tooltip of the fields
button beside it reads **Fields**. They used to share the one word.

---

[Documentation](../README.md) › [The editor panel](main.md)
