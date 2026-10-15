[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-002

# UC-VIEW-PANEL-002: Let the panel follow the selection

**As a** tester, **I want** the panel to keep up as I move down the test cases, **so that** I can read each one without
asking for it every time.

This is what the panel does by itself. There is nothing to turn on.

There is no key for this. It happens once the panel is open.

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
- **Rule-VIEW-PANEL-015** — Moving the selection in an editor fills the panel
  again, but only while the panel is on screen and that editor is in front. An
  editor behind another one, such as a test run whose automation moves its
  selection (Rule-EDITOR-PANEL-289), leaves the panel as it is.
- **Rule-VIEW-PANEL-016** — Following never opens the panel. A panel the tester
  closed does not come back on the next click.
- **Rule-VIEW-PANEL-017** — Moving to a Testin editor with nothing selected
  empties the panel.
- **Rule-VIEW-PANEL-018** — Every fill starts again at the first test case, so
  the paging position is never carried over.
- **Rule-VIEW-PANEL-102** — Following keeps the tab in front. A tester reading
  History stays on History as the selection moves; only opening the panel starts
  on Details.

## What the tester sees

This use case opens no screen of its own. The panel is already open, and what
is drawn inside it changes.

The test case in the panel is replaced by the newly selected one. Nothing is
said, and no message appears. When the tester moves to a Testin editor with
nothing selected, the panel is emptied and reads *Select a test case to view
details* in gray, at the top left of the **Details** tab.

## Main flow

1. The panel is open on a test case.
2. The tester presses the down arrow, or clicks another card.
3. The panel draws the newly selected test case, on the tab that was in front.
4. The tester moves to another Testin editor tab.
5. The panel draws whatever that editor has selected.

## What Testin refuses

**If the panel is closed** — nothing happens. Moving the selection does not open
it. A tester who closed the panel asked for the screen, and keeps it.

**If the new selection is empty** — the panel keeps the test case it was
showing.

**If the tester moves to a Testin editor with nothing selected** — the panel is
emptied, and reads *Select a test case to view details*.

**If the tester closes the Testin editor the panel's test case came from** — the
panel is emptied and goes with it. Closing any other editor leaves it alone.

---

[Documentation](../README.md) › [The view panel](main.md)
