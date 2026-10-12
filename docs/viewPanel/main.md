[Documentation](../README.md) › The view panel

# The view panel

The view panel is the panel on the right of the IDE. It shows one test case at a
time: everything the test case says, and everything one test run recorded about
it.

|                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The view panel                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Answers**         | What the panel shows, how a test case gets into it, and what a tester can do from it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Numbering**       | Use cases are `UC-VIEW-PANEL-001` to `UC-VIEW-PANEL-017`. Rules are `Rule-VIEW-PANEL-001` to `Rule-VIEW-PANEL-125`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Retired**         | `Rule-VIEW-PANEL-037` said the History tab is not built and shows one line saying so. Retired when #150 built the tab: read Rule-VIEW-PANEL-096 instead. `Rule-VIEW-PANEL-090` said the Details tab shows only the fields the editor's Fields list shows. Retired on 5 October 2026, when the panel went back to the whole test case: read Rule-VIEW-PANEL-103 instead. Neither number is given to anything else. `UC-VIEW-PANEL-008` listed a test case's bugs from memory in an Open Bugs tab, with `Rule-VIEW-PANEL-038`, `064` and `065`. Retired when #397 moved the bugs into History from Git: read UC-VIEW-PANEL-007 and Rule-VIEW-PANEL-105 to 109 instead. None of these numbers is given to anything else |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Checked against** | `main` at `ee867bf7`, 5 October 2026. Every page in this part was read against the code.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

---

## The use cases

|                       | What the tester does                                            | Why they do it                                                   |
|-----------------------|-----------------------------------------------------------------|------------------------------------------------------------------|
|                       | **Getting a test case into the panel**                          |                                                                  |
| **UC-VIEW-PANEL-001** | [Open a test case's details](openDetails.md)                    | See the whole test case, not only its title.                     |
| **UC-VIEW-PANEL-002** | [Let the panel follow the selection](followSelection.md)        | Read one test case after another without asking each time.       |
| **UC-VIEW-PANEL-003** | [Page through several test cases](pageThroughTestCases.md)      | Walk the selected test cases with two keys.                      |
|                       | **Reading**                                                     |                                                                  |
| **UC-VIEW-PANEL-004** | [Read what a test case says](readTestCase.md)                   | Follow the steps and check the expected result.                  |
| **UC-VIEW-PANEL-005** | [Read what a test run recorded](readRunItem.md)                 | See the run item status and what went wrong last time.           |
| **UC-VIEW-PANEL-006** | [Read the stacktrace behind a failure](readStacktrace.md)       | Copy the whole error into a bug report.                          |
| **UC-VIEW-PANEL-007** | [Read a test case's history](readHistory.md)                    | See what changed on the test case, and when.                     |
| **UC-VIEW-PANEL-017** | [Move between the panel's tabs](moveBetweenTabs.md)             | Reach History without the mouse.                                 |
|                       | **Working from the panel**                                      |                                                                  |
| **UC-VIEW-PANEL-009** | [Go to the test case](goToTestCase.md)                          | Open the test case's own editor from wherever it is being read.  |
| **UC-VIEW-PANEL-010** | [Go to the test set the test case lives in](goToTestSet.md)     | Open the test set and see the test cases around it.              |
| **UC-VIEW-PANEL-011** | [Change one field without leaving the panel](changeOneField.md) | Fix a wrong field without going back to the editor.              |
| **UC-VIEW-PANEL-012** | [Run a test case from the panel](runFromPanel.md)               | Try the test case again while reading it.                        |
| **UC-VIEW-PANEL-013** | [Stop a test case from the panel](stopFromPanel.md)             | Stop an execution that is taking too long.                       |
| **UC-VIEW-PANEL-014** | [Go to the automation code](goToCode.md)                        | Read or change the code behind the test case.                    |
| **UC-VIEW-PANEL-015** | [Close the panel](closePanel.md)                                | Give the editor the whole width of the screen.                   |
| **UC-VIEW-PANEL-016** | [Report a failed test case as a bug](reportBug.md)              | File the failure on GitHub without retyping it, and never twice. |

---

## What the panel is for

A test case is too big for a card and too big for a grid row. A card shows a
title. A grid row shows the fields that fit. The view panel shows all of it,
beside whatever the tester is working in.

It is built for one moment above all. A tester is executing a test run, has just
recorded a failure, and needs the expected result, the steps and the test data
in front of them while they write down what actually happened.

**Three words, before the rules use them.**

- A **field** is one thing a test case carries, such as its expected result, its
  steps or its module.
- A **run item status** is what one test run recorded against this test case:
  **Passed**, **Failed** or **Blocked**.
- To **follow the selection** is what the panel does once it is open. The tester
  moves to another test case, and the panel moves with them.

---

## Every key the panel answers to

| Key                                         | What it does                                                | The page that owns it                                                       |
|---------------------------------------------|-------------------------------------------------------------|-----------------------------------------------------------------------------|
| `Enter`                                     | Opens the panel on the selected test cases                  | [UC-VIEW-PANEL-001](openDetails.md)                                         |
| `F2`                                        | Opens the menu that changes one field                       | [UC-VIEW-PANEL-011](changeOneField.md)                                      |
| `Ctrl+Right`, `Ctrl+Alt+Cmd+Right` on a Mac | Moves to the next test case                                 | [UC-VIEW-PANEL-003](pageThroughTestCases.md)                                |
| `Ctrl+Left`, `Ctrl+Alt+Cmd+Left` on a Mac   | Moves to the previous test case                             | [UC-VIEW-PANEL-003](pageThroughTestCases.md)                                |
| `Escape`                                    | Closes the panel, pressed in the editor or inside the panel | [UC-VIEW-PANEL-015](closePanel.md)                                          |
| `Ctrl` and the mouse wheel                  | Makes every Testin text bigger or smaller                   | [UC-SETTING-011](../setting/changeTextSize.md)                              |
| `F5`                                        | Runs the test case on display, or stops it while it runs    | [UC-VIEW-PANEL-012](runFromPanel.md), [UC-VIEW-PANEL-013](stopFromPanel.md) |
| `Shift+F5`                                  | Opens its generated test method                             | [UC-VIEW-PANEL-014](goToCode.md)                                            |
| `Tab`                                       | Brings the next tab to the front, with the keyboard in it   | [UC-VIEW-PANEL-017](moveBetweenTabs.md)                                     |
| `Shift+Tab`                                 | Brings the previous tab to the front                        | [UC-VIEW-PANEL-017](moveBetweenTabs.md)                                     |

---

## The panel

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin View                                                ( < )  ( > )   │
├────────────────────────────────────────────────────────────────────────────┤
│   Details      History                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   Demo > Test Cases > Accounts > Login                                     │
│                                                                            │
│   Log in with a valid user                                                 │
│                                                                            │
│   ( High ) ( Smoke )          [ go to code ] [ run ] [ tc ]                │
│                                                                            │
│   EXECUTION RESULT ──────────────────────────────────────────────────────  │
│   ( Failed ) ( 02:14 ) ( Blocker / High )  #123                            │
│   ACTUAL RESULT                                                            │
│   The session was dropped.                                                 │
│   Stacktrace                                                               │
│   EXECUTED BY                                                              │
│   muteb on 7 January 10:05                                                 │
│                                                                            │
│   ▸ TEST CASE ───────────────────────────────────────────────────────────  │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The two arrows** — move to the previous and the next test case. They are
   gray when the panel was handed only one.
2. **The two tabs** — **Details** and **History**. **History**
   shows the test case's Git history, newest first.
3. **The breadcrumb** — one step for each folder above the test case. Only the
   last step opens anything.
4. **The title** — the test case's description, the largest words on the panel.
5. **The badges and the actions** — the priority and one badge for each group,
   then the three icons: go to the automation code, run the test case, and open
   the test case's own editor. All three are always drawn; one that cannot work
   here is gray, does not grow under the pointer, and says what it is waiting for (Rule-VIEW-PANEL-085).
6. **Execution result** — what one test run recorded. The whole band, its name
   included, is drawn only when the panel was opened from a test run (Rule-VIEW-PANEL-085). Its run item status, its
   duration and its bug are one
   line under the band's name, the duration is framed rather than filled, and
   who ran it and when close the band as a row (Rule-VIEW-PANEL-086). That is
   [UC-VIEW-PANEL-005](readRunItem.md).
7. **Test case** — folded until a tester clicks its name, and then every field
   the test case has, with every empty one left out. The state is remembered for
   the IDE rather than for one test case. In the test set editor there is no
   test run above it, so the fields are drawn with no heading and no fold (Rule-VIEW-PANEL-087).
8. **The captions** — each on a line of its own above its value, in the
   caption font: JetBrains Mono, smaller than the value, in capitals, in gray.
   The value has the whole width of the panel (Rule-VIEW-PANEL-082).

---

## Why the panel is built this way

**It refuses to reappear.** A tester who closes the panel has said they want the
screen. Following the selection would open it again on the next click, so
following happens only while it is already open.

**It reads again instead of remembering.** The panel is handed a test case, and
then never draws that copy. It asks Testin's memory again every time. A tester
can change a field in the grid and watch the panel change with it.

**One panel for each code project.** Two code projects open at once used to
share one panel, so a test case from the first appeared in the second.

---

## Where the plugin breaks its own rules

Stated, not hidden, and there are none today. Every difference this part has
listed is repaired. Each one is in the table below, with the date it went and
the issue it was fixed under.

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone              | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 2**  | `Escape` closed the panel from the editor and did nothing inside it, so a tester who pressed `F2` — which is what puts the keyboard in the panel — could not close the panel with the key that closes everything else. `Escape` is registered on all three tabs now, and takes the same step back as everywhere else: a pending cut first, then the panel. Fixed 9 September 2026, [#226](https://github.com/MutebAlMughyiri/test-in/issues/226)         |
| **Difference 11** | Every result a running test reported redrew the whole panel, whichever test case reported it. A test run of 200 test cases rebuilt the panel 400 times, each rebuild walking every test run in the project for Open Bugs. The report goes through the same filtered refresh every other writer uses now, so the panel redraws only for the test case it is showing. Fixed 18 September 2026, [#66](https://github.com/MutebAlMughyiri/test-in/issues/66) |
| **Difference 10** | An F2 edit that Testin could find no place to write was dropped in silence. Fixed 7 September 2026, [#234](https://github.com/MutebAlMughyiri/test-in/issues/234)                                                                                                                                                                                                                                                                                        |
| **Difference 3**  | The last step of the path looked for a test set where a test run was, and stopped with an internal error. Fixed 8 September 2026, [#227](https://github.com/MutebAlMughyiri/test-in/issues/227)                                                                                                                                                                                                                                                          |
| **Difference 8**  | Test run captions had no colon and test case captions did, in one column. The colon was part of the caption; it belongs to the one surface that needs it, which is copied text. Fixed 9 September 2026, [#232](https://github.com/MutebAlMughyiri/test-in/issues/232)                                                                                                                                                                                    |
| **Difference 4**  | Every step took a hand pointer and underlined itself and only the last one did anything, so clicking **Test Cases** to go up a level did nothing and said nothing. Every step goes where it says now, through the same `GoTo` the global search uses - the tree comes up and expands to it, and a step with an editor opens that too. Fixed 10 September 2026, [#228](https://github.com/MutebAlMughyiri/test-in/issues/228)                             |
| **Difference 7**  | Both stripes read **Testin**, so the side a panel was docked on was the only thing that told them apart - and no document could say "open the Testin tool window" without meaning either. They read **Testin Tree** and **Testin View** now: both keep the plugin's name, and each says which of the two it is. Fixed 10 September 2026, [#231](https://github.com/MutebAlMughyiri/test-in/issues/231)                                                   |
| **Difference 1**  | The run button's tooltip said `F5` and the go to code button's said `Shift+F5`, and neither key did anything while the focus was in the panel. Both are bound there now, to the same `TestCaseQuickAction` that printed them into the tooltip - so the key and the words cannot drift apart. Fixed 10 September 2026, [#225](https://github.com/MutebAlMughyiri/test-in/issues/225)                                                                      |
| **Difference 9**  | It emptied the panel whichever editor closed, so a tester reading a test case from the first editor who closed the second watched it go blank. The panel already records which node it is showing, so it is asked. Fixed 9 September 2026, [#233](https://github.com/MutebAlMughyiri/test-in/issues/233)                                                                                                                                                 |
| **Difference 12** | Three doors start Testin and any of them could raise *Testin Setup Required*, so opening the panel to read a test case handed the tester a settings notification. The prompt is about a project opening, so it hangs off the one door that means a project opened. Fixed 9 September 2026, [#236](https://github.com/MutebAlMughyiri/test-in/issues/236)                                                                                                 |

---

## Not decided

**Question 3** — Should the panel show a test case at all when it was opened
from a test run that does not hold it? It shows the test case with no execution
result band today, which reads as a test case nobody has run.

---

[Documentation](../README.md) › **The view panel**
