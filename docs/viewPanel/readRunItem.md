[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-005

# UC-VIEW-PANEL-005: Read what a test run recorded

**As a** tester, **I want** the run item status and everything behind it beside
the test case itself, **so that** I can see what happened last time without
opening the test run.

This band comes from one test run. The band under it comes from the test case.

There is no key for this. The band appears when the panel was opened from a
test run.

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
- **Rule-VIEW-PANEL-029** — The test run band is drawn only when the panel was
  opened from a test run that holds this test case.
- **Rule-VIEW-PANEL-030** — The band comes from the test run itself, so the
  panel and a report on the same test run can never disagree.
- **Rule-VIEW-PANEL-031** — A test run value with nothing in it is not drawn.
- **Rule-VIEW-PANEL-033** — A test case the test run has not reached yet reads **Pending**.
- **Rule-VIEW-PANEL-091** — Beside a filed bug's link stands its state on
  GitHub: the Status column of the Project board it sits on, or else Open, Fixed
  or Not planned. A link whose state has not been read stands alone.
- **Rule-VIEW-PANEL-092** — Testin asks GitHub when a test run opens and on
  Refresh, for every bug the test project holds, one request per repository, in
  the background. The panel never waits for the answer, and the answer is never
  written to the test run.
- **Rule-VIEW-PANEL-093** — When GitHub cannot be asked - gh missing, signed out
  or offline - every link still shows and opens, and Testin Help holds one hint
  saying why until a read succeeds.
- **Rule-VIEW-PANEL-094** — A board's Status needs gh's read:project permission.
  Without it the issue's own state shows, and Testin Help holds one hint naming
  the command that grants it. Every read asks for the board again, so Refresh
  shows it once the permission is granted.

## The screen

What this test run recorded is **Execution result**, a band of its own under the
badges and above **Test case** (Rule-VIEW-PANEL-085).

```
┌──────────────────────────────────────────────────────────────────────────┐
│   Demo > Test Cases > Accounts > Login                                   │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
│   ( High ) ( Smoke )        [ go to code ] [ run ] [ tc ]                │
│                                                                          │
│   EXECUTION RESULT ────────────────────────────────────────────────────  │
│   ( Failed ) ( 02:14 ) ( Blocker / High )  #123                          │
│   ACTUAL RESULT                                                          │
│   The session was dropped.                                               │
│   Stacktrace                                                             │
│   EXECUTED BY                                                            │
│   muteb on 7 January 10:05                                               │
│                                                                          │
│   ▸ TEST CASE ─────────────────────────────────────────────────────────  │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The band's name** — **Execution result**, in the caption font, with a
   hairline running from it to the panel's edge, level with the middle of the
   words (Rule-VIEW-PANEL-085).
2. **The summary line** — everything the test run recorded that fits in a word
   or two: the run item status in its own color, the duration, the bug, and the
   links that open the issue or raise one (Rule-VIEW-PANEL-086).
3. **The duration** — a clock and the time, in a plain frame with nothing
   filled in. It is a measurement, not a run item status, and the frame says so.
4. **The bug** — one chip carrying the severity and the priority together, in
   the severity's color, then the issue as `#123` - the whole address is on
   hover - or **Report a bug** where there is no issue yet. Only a failed
   result has the chip, and a failure nobody triaged reads `Enhancement / Low`.
5. **Actual result** — the one sentence the band holds, its caption on a line of
   its own above it (Rule-VIEW-PANEL-082).
6. **Stacktrace** — the application's own error, which the panel never shows.
   The link opens it in a window that repeats the test case and the actual result
   above it, read only. A thumbnail of each screenshot sits beside the link on
   the panel, not in the window. That is
   [UC-VIEW-PANEL-006](readStacktrace.md) and Rule-VIEW-PANEL-034.
7. **Executed by** — last in the band, because who ran it is read after what
   happened. One row, in the words **Created** uses on the test case's own band (Rule-VIEW-PANEL-061).
8. **Test case** — folded, because a tester reading a failure is
   reading the failure. Clicking its name opens every field the test case has (Rule-VIEW-PANEL-087).

## When it was executed more than once

Each execution is a line of its own, the oldest first (Rule-VIEW-PANEL-086).

```
│   EXECUTION RESULT ────────────────────────────────────────────────────  │
│   ( Blocked )                                                            │
│   ( Passed ) ( 00:01 )  Invocation 1                                     │
│   ( Failed ) ( 00:01 )  Invocation 2 retried                             │
│     ( Passed ) ( 00:00 )  ↳ Retry 1  ★                         ☆  ×      │
│   ACTUAL RESULT                                                          │
```

1. **★** — the default: its status is the run item status everywhere, and the
   bug chip sits on its line (Rule-VIEW-PANEL-119). It is the last execution
   unless a tester chose another.
2. **☆ and ×** — shown only while the pointer is on a line, over its right
   end. ☆ makes that execution the default; × removes it at once and says
   *Removed* (Rule-VIEW-PANEL-120, Rule-VIEW-PANEL-121). A Committed test run
   draws both gray, with the reason.
3. **Clicking a line** — the rows below show that execution's actual result,
   stacktrace and who ran it. The band opens on the default
   (Rule-VIEW-PANEL-122).
4. **Executed once** — one line, no ★ and nothing to hover, as above
   (Rule-VIEW-PANEL-123).

## The band, in order

| Where it is       | What it holds                                                                                                                                                                                                            |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The first badge   | The run item status: **Passed**, **Failed**, **Blocked**, **Pending**, **Untested** or **Removed**, in its own color                                                                                                     |
| The second badge  | How long the test case took, with a clock, in a plain frame with nothing filled in                                                                                                                                       |
| The bug chip      | The severity and the priority in one chip, as `Blocker / High`, in the severity's color, on a failed result only                                                                                                         |
| After the chip    | The GitHub issue the failure was reported as, as `#123` with the address on hover — or, where nothing has been reported, the **Report a bug** link that files one, never both. That is [UC-VIEW-PANEL-016](reportBug.md) |
| **Actual Result** | What the tester says actually happened                                                                                                                                                                                   |
| **Stacktrace**    | A link, never the error itself, then a thumbnail of each screenshot pasted with the failure, drawn even when Fields hides the link. That is [UC-VIEW-PANEL-006](readStacktrace.md)                                       |
| **Executed By**   | Who ran it and when, as `muteb on 7 January 10:05`, last in the band. One row, not two — the date has no caption of its own (Rule-VIEW-PANEL-061)                                                                        |

## Main flow

1. The tester opens a test run and selects a test case in it.
2. The tester presses `Enter`.
3. Testin looks for that test case in that test run's recorded results.
4. **Execution result** is drawn above what the test case says.
5. Below it, the test case's own band is drawn as usual.

## What Testin refuses

**If the panel was opened from a test set rather than a test run** — the test
run band is not drawn at all, not even its name. There is no test run to read
from.

**If the test run does not hold this test case** — the test run band is not
drawn.
The test case is drawn on its own. It then looks like a test case nobody has run.
That is question 3 on [the view panel page](main.md#not-decided).

**If the test case passed** — a pass draws no bug chip, and has no actual
result or stacktrace of its own. What an earlier failed execution recorded is
still there: clicking its line shows it (Rule-EDITOR-PANEL-284).

**If nothing was timed** — the duration badge disappears and what is beside it
closes the gap. A run item status recorded from the menu, or on several test
cases at once, is never timed.

**If nobody is named** — the **Executed By** row holds the date on its own, and
with neither a name nor a date the row is not drawn at all (Rule-VIEW-PANEL-031).

## The same bug, on the History tab

The **Bug Severity** and **Bug Priority** rows say what *this* test run
recorded. The [History tab](readHistory.md) shows every bug any test run
recorded on this test case, each as a bug card of its own. A test case that has
failed in more than one cycle shows one bug here and all of them there.

---

[Documentation](../README.md) › [The view panel](main.md)
