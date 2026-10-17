[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-004

# UC-VIEW-PANEL-004: Read what a test case says

**As a** tester, **I want** every field of the test case in one place, **so that** I can follow its steps and judge it
against its expected result.

This is what a tester reads while running a test.

There is no key for this. It is what the **Details** tab shows.

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
- **Rule-VIEW-PANEL-023** — The rows are drawn in one fixed order, whatever the
  test case holds.
- **Rule-VIEW-PANEL-024** — A row with nothing in it is not drawn, and its
  caption goes with it.
- **Rule-VIEW-PANEL-025** — A test case with no description shows a dash for its
  title.
- **Rule-VIEW-PANEL-026** — The rows a tester writes as sentences are
  capitalized, and a period is added unless the text already closes itself:
  **Description**, **Expected Result**, **Steps** and **Pre Conditions**. **Reference**, **Module** and **Test Data**
  are left exactly as the tester
  typed them — a reference is an identifier and a module is a label, and a
  period after either reads as a typo. The same four rows are the ones the card
  and light mode capitalize, because all three ask the same question.
- **Rule-VIEW-PANEL-027** — A blank step is skipped, and its number is not given
  to the step after it. A test case with a blank third step reads one, two,
  four.
- **Rule-VIEW-PANEL-028** — No value in the panel can be typed into, and none of
  them takes the keyboard: the tab holds it, so Escape and the arrows always
  mean what they mean in the panel rather than inside whichever value was last
  clicked. A value is copied from where it is written - the test case in its
  editor, the stacktrace in the window that opens on it. Selecting text in the
  panel was asked for and refused on 2026-09-23: it costs every value its
  focus, and the panel is for reading.
- **Rule-VIEW-PANEL-061** — Who did something and when are one row, not two. **Created** reads the name, then *on*, then
  the date.
- **Rule-VIEW-PANEL-062** — **Order** is where the test case sits in its test
  set, read from the set rather than from the test case. A test case its set
  no longer holds - a deleted one, read in a test run - has no Order.
- **Rule-VIEW-PANEL-082** — Every field in the Details tab has its caption on a
  line of its own above its value, in the caption font, so the value has the
  whole width of the panel at any width.
- **Rule-VIEW-PANEL-085** — The panel is three bands, always in this order: who
  this is - the breadcrumb, the title, the badges and the action icons; **Execution
  result**, what one test run recorded; and **Test case**, what the test
  case says. A band's name carries a hairline to the panel's edge, level with the
  middle of the words. A band with nothing to show is not drawn at all, and
  where only one band has anything to show its name is not drawn either: a
  heading over the whole panel names nothing.
- **Rule-VIEW-PANEL-086** — What a test run recorded is one line per execution
  under the band's name, the oldest first: the run item status in its own color
  and how long it took, then, when the automation ran the method more than once,
  what it was - *Invocation n*, or *↳ Retry n* under the attempt it retried; an
  attempt TestNG retried is dimmed. The bug's severity and
  priority chip, the issue and the link that raises one stay on the default's
  line, and hovering the chip names each half as a test run's card does. The
  duration carries a clock and a plain frame, so it reads as a measurement
  rather than a run item status. Who ran the execution shown below and when
  close the band as a row of their own, in the words **Created** already uses (Rule-VIEW-PANEL-061).
- **Rule-VIEW-PANEL-087** — The test case's band folds only where a test run
  stands above it, and there it is folded when a tester first meets it: its name
  carries the arrow that opens it, and opening it shows every field the test
  case has. In the test set editor there is no test run, so the fields are
  drawn straight under the badges with nothing to open. The state is remembered
  for the IDE rather than for one test case, so a tester who opens it keeps it
  open for the next test case they read.
- **Rule-VIEW-PANEL-114** — The priority badge says High or Medium, and hovering
  it says Priority: High or Priority: Medium, so it is not read as the bug's
  priority, which uses the same words.
- **Rule-VIEW-PANEL-119** — One execution is the run item's default: its status
  is the run item status on the card, in the grid, in the totals and in the
  reports, and it carries a ★. A tester's new recording becomes the default, so
  does the one the automation picked (Rule-CODEGEN-111), and so does the one a
  tester chooses with ☆.
- **Rule-VIEW-PANEL-120** — Hovering an execution's line shows ☆ and × over its
  right end, as a card shows its hover actions, taking no room of their own. ☆
  makes that execution the default, and the run item status follows at once.
- **Rule-VIEW-PANEL-121** — × removes that execution at once, asks nothing, and
  says *Removed*. If it was the default, the last one left becomes the default.
  ☆ and × are gray, and say why, once the test run is Committed (Rule-TREE-PANEL-009), and while the automation is still
  running that test
  case, because its attempts are still being written (Rule-CODEGEN-111).
- **Rule-VIEW-PANEL-122** — Clicking an execution's line shows that execution's
  actual result, error, screenshots and who ran it in the rows below, and
  changes nothing else. The panel opens on the default.
- **Rule-VIEW-PANEL-123** — A run item executed once reads as it did before
  executions were kept: one line, no ★, nothing to hover.
- **Rule-VIEW-PANEL-124** — Everything on the Details tab wraps to the panel's
  width: a value onto more lines, and a row of badges, chips and links onto a
  second row. Making the panel narrower never cuts anything off at the right or
  makes it scroll sideways. The History tab keeps each card's lines whole
  instead, and scrolls sideways when they are wider than the panel.
- **Rule-VIEW-PANEL-125** — While the test case shown is executing, by hand or
  by automation, a dot blinks right after its title, as on its card (Rule-EDITOR-PANEL-291).

## The screen

```
┌──────────────────────────────────────────────────────────────────────────┐
│   Demo > Test Cases > Accounts > Login                                   │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
│   ( High ) ( Smoke )        [ go to code ] [ run ] [ tc ]                │
│                                                                          │
│   EXPECTED RESULT                                                        │
│   The dashboard opens.                                                   │
│   STEPS                                                                  │
│   1- Open the login page.                                                │
│   2- Type the credentials.                                               │
│   3- Press Sign in.                                                      │
│   PRE CONDITIONS                                                         │
│   An account exists.                                                     │
│   TEST DATA                                                              │
│   user=admin                                                             │
│   MODULE                                                                 │
│   Accounts                                                               │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The top part** — the breadcrumb, then the title, then one line carrying the
   badges and the three icons (Rule-VIEW-PANEL-085). While the test case
   executes, a dot blinks right after the title (Rule-VIEW-PANEL-125). Each one is numbered on
   [the view panel page](main.md#the-panel).
2. **No heading, and nothing to open** — in the test set editor the fields are
   the only thing on the panel, so they are drawn straight under the identity
   line (Rule-VIEW-PANEL-087). Under a test run they fold behind **Test case
   details** instead.
3. **The captions** — the name of the field, on a line of its own above its
   value, in the caption font: JetBrains Mono, smaller than the value, in
   capitals, in gray (Rule-VIEW-PANEL-082).
4. **The values** — each under its caption, across the whole panel, so a narrow
   panel still gives a value all its width. Each one can be selected and
   copied. None of them can be typed into.
5. **The rows that are missing** — this test case has nothing in its **Reference** field, so that row is not drawn and
   no gap is left for it.

## The rows, in order

| Caption             | What it holds                                 |
|---------------------|-----------------------------------------------|
| **Expected Result** | What should happen                            |
| **Steps**           | One numbered line for each step               |
| **Pre Conditions**  | What must be true before the test case starts |
| **Test Data**       | The data the test case uses, exactly as typed |
| **Reference**       | A link or a ticket number                     |
| **Module**          | The part of the product this test case covers |
| **Order**           | Where the test case sits in its test set      |
| **Created**         | Who made it, and when                         |
| **Updated**         | Who last changed it, and when                 |

Above the rows sit the breadcrumb, the title and the
badges. They are numbered on the panel drawing on
[the view panel page](main.md#the-panel).

## Main flow

1. The panel is filled with a test case.
2. Testin reads the test case again from memory, not from the copy it was
   handed.
3. The breadcrumb is drawn, one step for each folder above the test case.
4. The description is drawn as the title.
5. The badges are drawn - the priority, then one for each group - and the three
   icons beside them.
6. Each row that has something in it is drawn, in the order above.

## What Testin refuses

**If a field is empty** — the row is not drawn at all. The panel never shows a
caption with nothing under it.

**If the description is empty** — the title reads a single dash.

**If a date was never set** — the row says the name alone rather than showing a
date in 1970. A row with neither a name nor a date is not drawn at all.

**If the priority is the lowest** — no priority badge is drawn. Only the two
higher priorities get a badge.

**If the test case has no steps, or every step is blank** — there is no **Steps** row.

---

[Documentation](../README.md) › [The view panel](main.md)
