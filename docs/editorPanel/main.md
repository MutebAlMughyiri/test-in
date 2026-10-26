[Documentation](../README.md) › The editor panel

# The editor panel

The editor panel is the middle of the IDE, where a test set or a test run opens
in a tab. It is where test cases are written, and where a test run is executed.

|                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The editor panel                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **Answers**         | What a tester can do to test cases and to a test run, exactly what happens step by step, and what every screen looks like                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Numbering**       | Use cases are `UC-EDITOR-PANEL-001` to `UC-EDITOR-PANEL-049`. Rules are `Rule-EDITOR-PANEL-001` to `Rule-EDITOR-PANEL-301`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **Retired**         | `Rule-EDITOR-PANEL-193` said the analysis appears in the PDF, Word and web reports and that the spreadsheet leaves it out. Retired 21 September 2026: `c97c44f6` gave the spreadsheet an Overview sheet carrying the overview, the execution summary and the result analysis, and [Rule-REPORT-020](../report/generateReport.md) states that. The two rules contradicted each other for three days and this one lost. Read Rule-REPORT-020 instead. `Rule-EDITOR-PANEL-231` said each open field in Create Test Case and Update Test Case is named by a caption above it. Retired 18 September 2026, when the captions were taken off those two dialogs again (#328): a field is named by its icon and its gray hint, as it was before. The number is not given to anything else. `Rule-EDITOR-PANEL-205` was never written: the number was taken while the part was being numbered and the rule it was meant for never arrived, so no page carries it and none ever did. It is not given to anything else either (#66, finding 305). Retired 4 October 2026 as copies of rules every page of this part carries (#392): `Rule-EDITOR-PANEL-037` said what `Rule-EDITOR-PANEL-006` says - a save that changes nothing writes nothing and says nothing; `Rule-EDITOR-PANEL-012` and `Rule-EDITOR-PANEL-017` said what `Rule-EDITOR-PANEL-002` says - the editor opens on cards and builds the grid when first asked; `Rule-EDITOR-PANEL-066` said what `Rule-EDITOR-PANEL-007` says, which now carries its one extra clause, that the history is kept against the test set the editor shows; `Rule-EDITOR-PANEL-105` and `Rule-EDITOR-PANEL-106` said what `Rule-EDITOR-PANEL-004` says - a page holds 50 test cases and at most 1000. Read the kept rule instead. None of these numbers is given to anything else. Retired 11 October 2026, when a run item began keeping every execution (#415): `Rule-EDITOR-PANEL-138` said recording a pass clears the actual result, the error, the screenshots and the bug issue link, and resets severity and priority; `Rule-EDITOR-PANEL-161` said changing a failed test case to passed asks first, because it clears the failure; `Rule-EDITOR-PANEL-162` said only passing clears anything; `Rule-EDITOR-PANEL-220` said a failure from the automation clears what the last failure said before it writes its own. Nothing is cleared any more: read Rule-EDITOR-PANEL-284, and Rule-CODEGEN-111 for the automation. None of these numbers is given to anything else |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **Checked against** | `main` at `1270e599`, 20 September 2026. Every page of this part was read against the code that day: every message quoted, every menu entry, every key, every status bar strip and every drawn screen.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |

---

## The use cases

Two editors share this panel. A test set opens in one, a test run in the other.
They share the toolbar, the status bar, the two views and every key that is not
about a run item status.

|                         | What the tester does                                                  | Why a tester would use it                                  |
|-------------------------|-----------------------------------------------------------------------|------------------------------------------------------------|
|                         | **Opening and reading**                                               |                                                            |
| **UC-EDITOR-PANEL-001** | [Open a test set and see its test cases](openTestSet.md)              | Read and work on the test cases in a test set.             |
| **UC-EDITOR-PANEL-002** | [Switch between cards and a grid](switchView.md)                      | Compare many test cases at once in a table.                |
| **UC-EDITOR-PANEL-003** | [Choose which fields are shown](chooseFields.md)                      | Hide the fields you do not need, so rows stay short.       |
| **UC-EDITOR-PANEL-004** | [Change a grid column's width](changeColumnWidth.md)                  | Widen a column so long text is not cut off.                |
|                         | **Writing test cases**                                                |                                                            |
| **UC-EDITOR-PANEL-005** | [Create a test case](createTestCase.md)                               | Write down a new test case before you forget it.           |
| **UC-EDITOR-PANEL-006** | [Change one field of one test case](changeOneField.md)                | Fix one field, such as a typo, in two keys.                |
| **UC-EDITOR-PANEL-007** | [Change one field on many test cases at once](bulkEdit.md)            | Correct the same field on many test cases in one go.       |
| **UC-EDITOR-PANEL-008** | [Type straight into a grid cell](editGridCell.md)                     | Correct a value in the table where you can see it.         |
| **UC-EDITOR-PANEL-009** | [Move a test case by typing its number](setOrderByNumber.md)          | Put a test case in the right place without dragging.       |
| **UC-EDITOR-PANEL-010** | [Reorder test cases by dragging](dragToReorder.md)                    | Drag test cases into the order somebody would run them.    |
| **UC-EDITOR-PANEL-011** | [Remove test cases](removeTestCases.md)                               | Delete test cases nobody wants any more.                   |
| **UC-EDITOR-PANEL-012** | [Undo a change](undoChange.md)                                        | Take back the last change with one key.                    |
| **UC-EDITOR-PANEL-013** | [Redo a change](redoChange.md)                                        | Put back a change you took back by mistake.                |
|                         | **The clipboard**                                                     |                                                            |
| **UC-EDITOR-PANEL-014** | [Copy a test case's details as text](copyAsText.md)                   | Get a test case as text for a chat or a ticket.            |
| **UC-EDITOR-PANEL-015** | [Copy test cases](copyTestCases.md)                                   | Copy test cases to start another test set from them.       |
| **UC-EDITOR-PANEL-016** | [Cut test cases](cutTestCases.md)                                     | Move test cases into the test set they belong in.          |
| **UC-EDITOR-PANEL-017** | [Paste test cases](pasteTestCases.md)                                 | Drop the test cases you copied or cut into this set.       |
| **UC-EDITOR-PANEL-018** | [Copy, cut and paste the grid's cells](gridClipboard.md)              | Move values between the grid and a spreadsheet.            |
|                         | **Finding what I want**                                               |                                                            |
| **UC-EDITOR-PANEL-019** | [Search the test cases](searchTestCases.md)                           | Find a test case by a word inside it.                      |
| **UC-EDITOR-PANEL-020** | [Filter the test cases](filterTestCases.md)                           | Show only the test cases you want to work on.              |
| **UC-EDITOR-PANEL-021** | [Clear the filters](clearFilters.md)                                  | Get the whole test set back in one click.                  |
| **UC-EDITOR-PANEL-022** | [Page through the test cases](pageThrough.md)                         | Move through a long test set one page at a time.           |
| **UC-EDITOR-PANEL-023** | [Change how many a page holds](changePageSize.md)                     | See more test cases at once, without turning pages.        |
| **UC-EDITOR-PANEL-024** | [Select test cases](selectTestCases.md)                               | Pick several test cases so one gesture changes them all.   |
|                         | **Working from the editor**                                           |                                                            |
| **UC-EDITOR-PANEL-025** | [Open the details panel](openDetailsPanel.md)                         | Read the whole of one test case beside the list.           |
| **UC-EDITOR-PANEL-026** | [Step back](stepBack.md)                                              | One key to get back to a plain list.                       |
| **UC-EDITOR-PANEL-027** | [Refresh the editor from disk](refreshEditor.md)                      | See the changes a colleague's sync brought in.             |
| **UC-EDITOR-PANEL-028** | [See the test set's own details](nodeDetails.md)                      | Check who made this test set and what it holds.            |
| **UC-EDITOR-PANEL-029** | [Open the menu from the keyboard](keyboardMenu.md)                    | Reach every menu entry without touching the mouse.         |
|                         | **The test run editor**                                               |                                                            |
| **UC-EDITOR-PANEL-030** | [Open a test run and see what it covers](openTestRun.md)              | See what a test run covers and what it recorded.           |
| **UC-EDITOR-PANEL-031** | [Start executing by hand](startExecution.md)                          | Walk the test run one test case at a time.                 |
| **UC-EDITOR-PANEL-032** | [Record that a test case passed](recordPassed.md)                     | Say a test case worked, with one key.                      |
| **UC-EDITOR-PANEL-033** | [Record that a test case is blocked](recordBlocked.md)                | Say a test case could not be tried at all.                 |
| **UC-EDITOR-PANEL-034** | [Record that a test case failed, and say why](recordFailed.md)        | Say what really happened while you can still see it.       |
| **UC-EDITOR-PANEL-035** | [Stop executing](stopExecution.md)                                    | Stop the walk and the clock part way through.              |
| **UC-EDITOR-PANEL-036** | [Resume a test run I stopped](resumeExecution.md)                     | Pick the test run up where you left it.                    |
| **UC-EDITOR-PANEL-037** | [Record a run item status out of order](recordOutOfOrder.md)          | Judge a test case the walk is not on.                      |
| **UC-EDITOR-PANEL-038** | [Correct a run item status I got wrong](correctRunItemStatus.md)      | Change a run item status you recorded by mistake.          |
| **UC-EDITOR-PANEL-039** | [Record one run item status on many test cases](bulkRunItemStatus.md) | Mark many test cases blocked, or passed, at once.          |
| **UC-EDITOR-PANEL-040** | [Change the failure details on their own](editFailureDetail.md)       | Add the error later, without touching the run item status. |
| **UC-EDITOR-PANEL-041** | [Type an actual result into the grid](typeActualResult.md)            | Note what happened straight into the table.                |
| **UC-EDITOR-PANEL-042** | [Watch how the test run is going](watchProgress.md)                   | See how many passed and how long it is taking.             |
| **UC-EDITOR-PANEL-043** | [Run one test case's automation](runOneTestCase.md)                   | Let the machine judge one test case for you.               |
| **UC-EDITOR-PANEL-044** | [Run every automated test case](runWholeTestRun.md)                   | Set the whole test run going and come back later.          |
| **UC-EDITOR-PANEL-045** | [Write the result analysis](writeResultAnalysis.md)                   | Say what the test run as a whole showed.                   |
| **UC-EDITOR-PANEL-046** | [Work in light mode](lightMode.md)                                    | Judge test cases in a small window above your app.         |
| **UC-EDITOR-PANEL-047** | [See which test cases are automated](seeWhatIsAutomated.md)           | Tell what a test run will cover without opening the code.  |
| **UC-EDITOR-PANEL-048** | [Go from a test run to a test case](navigateToTestCase.md)            | Open the test case behind a row, in its own test set.      |
| **UC-EDITOR-PANEL-049** | [Sort the test cases](sortTestCases.md)                               | Arrange the screen by one field; the order stays.          |

---

## What the panel is for

The tree says what exists. The editor is where the work happens.

A test set opens as a list of its test cases, and the tester writes them there.
A test run opens as the same list with a run item status beside each row, and
the tester walks it, judging each test case in turn.

Everything else in the panel exists to make those two things fast: two views of
the same rows, a filter, a search, and a key for every gesture.

**Five words, before the rules use them.**

- A **card** is one test case drawn as a block, with its description on top and
  its fields under it.
- The **grid** is the same test cases drawn as a table, one row each.
- A **field** is one thing a test case carries, such as its expected result.
- A **run item status** is what a test run records against one test case: **Passed**, **Failed** or **Blocked**.
- To **execute** a test run is to walk it test case by test case, recording a
  run item status for each.

---

## Every key the panel answers to

**Reading and moving**

| Key                                                     | What it does                            | The page that owns it                          |
|---------------------------------------------------------|-----------------------------------------|------------------------------------------------|
| `Ctrl+F`                                                | Puts the cursor in the search box       | [UC-EDITOR-PANEL-019](searchTestCases.md)      |
| `Ctrl+Right`, `Ctrl+Alt+Cmd+Right` on a Mac             | The next page                           | [UC-EDITOR-PANEL-022](pageThrough.md)          |
| `Ctrl+Left`, `Ctrl+Alt+Cmd+Left` on a Mac               | The previous page                       | [UC-EDITOR-PANEL-022](pageThrough.md)          |
| `Ctrl+Shift+Right`, `Ctrl+Alt+Shift+Cmd+Right` on a Mac | The last page                           | [UC-EDITOR-PANEL-022](pageThrough.md)          |
| `Ctrl+Shift+Left`, `Ctrl+Alt+Shift+Cmd+Left` on a Mac   | The first page                          | [UC-EDITOR-PANEL-022](pageThrough.md)          |
| `Enter`                                                 | Opens the details panel, or a grid cell | [UC-EDITOR-PANEL-025](openDetailsPanel.md)     |
| `Escape`                                                | Steps back one step                     | [UC-EDITOR-PANEL-026](stepBack.md)             |
| `Context Menu`                                          | Opens the menu on the selection         | [UC-EDITOR-PANEL-029](keyboardMenu.md)         |
| `Ctrl` and the wheel                                    | Changes the text size                   | [UC-SETTING-011](../setting/changeTextSize.md) |

**Writing test cases**

| Key                                     | What it does                                    | The page that owns it                     |
|-----------------------------------------|-------------------------------------------------|-------------------------------------------|
| `Ctrl+M`, `Ctrl+Alt+Cmd+M` on a Mac     | Creates a test case                             | [UC-EDITOR-PANEL-005](createTestCase.md)  |
| `F2`                                    | Opens the menu of fields to change              | [UC-EDITOR-PANEL-006](changeOneField.md)  |
| `D` `E` `M` `T` `B` `S` `P` `G` `O` `R` | Opens that one field straight away              | [UC-EDITOR-PANEL-006](changeOneField.md)  |
| `Delete`                                | Removes the selected test cases                 | [UC-EDITOR-PANEL-011](removeTestCases.md) |
| `Ctrl+Z`                                | Takes back the last change                      | [UC-EDITOR-PANEL-012](undoChange.md)      |
| `Ctrl+Y`                                | Puts it back                                    | [UC-EDITOR-PANEL-013](redoChange.md)      |
| `Ctrl+Enter`                            | A line break inside a grid cell or a long field | [UC-EDITOR-PANEL-008](editGridCell.md)    |

**The clipboard**

| Key      | What it does                                                                            | The page that owns it                    |
|----------|-----------------------------------------------------------------------------------------|------------------------------------------|
| `Ctrl+C` | On cards, opens the copy menu; a letter copies one value. In the grid, copies the cells | [UC-EDITOR-PANEL-014](copyAsText.md)     |
| `Ctrl+X` | In the grid, copies the cells and empties the ones that can be typed into               | [UC-EDITOR-PANEL-018](gridClipboard.md)  |
| `Ctrl+V` | In the grid, pastes text into the cells                                                 | [UC-EDITOR-PANEL-018](gridClipboard.md)  |
| *no key* | **Copy Test Case** on the right-click menu                                              | [UC-EDITOR-PANEL-015](copyTestCases.md)  |
| *no key* | **Cut Test Case** on the right-click menu                                               | [UC-EDITOR-PANEL-016](cutTestCases.md)   |
| *no key* | **Paste Test Case** on the right-click menu                                             | [UC-EDITOR-PANEL-017](pasteTestCases.md) |

**Executing a test run**

| Key                                | What it does                                                     | The page that owns it                            |
|------------------------------------|------------------------------------------------------------------|--------------------------------------------------|
| `P`                                | Records **Passed**                                               | [UC-EDITOR-PANEL-032](recordPassed.md)           |
| `F`                                | Records **Failed**, and asks why                                 | [UC-EDITOR-PANEL-034](recordFailed.md)           |
| `B`                                | Records **Blocked**                                              | [UC-EDITOR-PANEL-033](recordBlocked.md)          |
| `F2`                               | Changes the failure details without changing the run item status | [UC-EDITOR-PANEL-040](editFailureDetail.md)      |
| `F5`                               | Runs the selected test cases, or stops them                      | [UC-EDITOR-PANEL-043](runOneTestCase.md)         |
| `Shift+F5`                         | Goes to the automation code                                      | [UC-CODEGEN-006](../codegen/goToCode.md)         |
| `F12`, `Ctrl+Alt+Cmd+F12` on a Mac | Writes the method for a test case that has none                  | [UC-CODEGEN-005](../codegen/automateTestCase.md) |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac    | Generates a report on this test run                              | [UC-REPORT-001](../report/generateReport.md)     |

**Nothing has a key** for: **Refresh**, **Grid View**, **List View**, the **Fields** button, the **Filter** button, the
**Details** button, **Start Manual
Execution**, **Stop Execution**, **Result Analysis** and light mode.

---

## The editor

```
┌────────────────────────────────────────────────────────────────────────────┐
│ (+) (refresh) (fields) (filter) (grid) (details) [ search................ ]│
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│ ▌ 1. Log in with a valid user.                              ( Regression ) │
│      (E) The dashboard opens.                                              │
│                                                                            │
│ ▌ 2. Log in with a locked account.                          ( Smoke )      │
│      (E) The account is refused.                                           │
│                                                                            │
│ ▌ 3. Log in with the wrong password.                                       │
│      (E) The password is refused.                                          │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  1 of 12 test cases (filtered from 120)   |< < 1 of 3 > >|          [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The toolbar** — create, refresh, choose the fields shown, filter, switch
   view, the test set's own details, and the search field. The search field is
   last, and every button sits to its left in one unbroken row.
2. **A card** — the test case's number in the test set, then its description,
   with a short bar beside it for a High or Medium priority, then a badge for
   each group (Rule-EDITOR-PANEL-267).
3. **The detail lines** — one for each field the tester chose to show, led by
   the field's icon (Rule-EDITOR-PANEL-269).
4. **The status bar, left** — how many test cases are selected, of how many, and
   how many the filter hid.
5. **The status bar, middle** — first page, previous, which page of how many,
   next, last.
6. **The status bar, right** — how many test cases a page holds.

## The test run editor

The same shape, with three things added.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ (start) (light) (report) (refresh) (fields) (filter) (grid) (analysis)     │
│ (details) [ search.......................................................] │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│ ▌ 1. Log in with a valid user.                    ( Passed ) ( 00:42 )     │
│      Run Item Status: Passed                                               │
│                                                                            │
│ ▌ 2. Log in with a locked account.                          ( Failed )     │
│      Run Item Status: Failed                                               │
│      Actual Result: The session was dropped.                               │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  2 of 12 test cases    |< < 1 of 1 > >|   In Progress                      │
│                        Passed 1 · Failed 1 · Pending 10   00:01:14  [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Start Manual Execution** — the first button. It becomes **Stop Execution**
   while an execution is going.
2. **Light mode** — the always on top window, on [its own page](lightMode.md).
3. **Result Analysis** — the last button before the test run's own details and
   the search field.
4. **The test run status** — on the right of the status bar, with the tree's own
   icon.
5. **The figures** — one for each run item status any test case carries,
   separated by a dot. A run item status nobody recorded is not drawn.
6. **The clock** — how long this test run has been executing. It ticks once a
   second.

---

## Why the panel is built this way

**Two views of one list.** Cards are for reading one test case properly. The
grid is for comparing many and correcting them quickly. Both draw the same
rows, and the selection follows from one to the other.

**The grid is built late.** A test set of 2,770 test cases would cost a table
nobody asked for. The grid is built the first time the tester presses the
button.

**A key for everything a tester does often.** Executing a test run is `P`, `F`
and `B` and nothing else. The gestures a tester does twice a project, such as
switching view or refreshing, are buttons.

**One message per gesture.** Twenty test cases removed is *Removed 20*, not
twenty messages. Two places break this and they are named below.

---

## Where the plugin breaks its own rules, writing test cases

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

|                  | The rule it breaks                                                   | What a tester sees                                                                                                                                                                   |
|------------------|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 7** | Rule-EDITOR-PANEL-005 — what the tester typed is stored              | A description typed into a grid cell loses characters Testin will not keep. If nothing else changed, nothing is saved and nothing is said, and the tester watches their text change. |
| **Difference 9** | Rule-INTERNAL-068 — a key printed on screen is read from the binding | The empty editor's second line reads *Press Ctrl+M to add* whatever machine it is on. The key itself is `Ctrl+Alt+Cmd+M` on a Mac, so it names a key that does nothing there.        |

## Where the plugin breaks its own rules, executing a test run

None is open.

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone              | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 10** | Selecting a test case the filter was hiding threw every filter away, silently. Fixed 7 September 2026, [#205](https://github.com/MutebAlMughyiri/test-in/issues/205)                                                                                                                                                                                                                                                                                                                                                 |
| **Difference 13** | **Refresh** threw away every filter and the search text, and said only *Refreshed*. Fixed 7 September 2026, [#208](https://github.com/MutebAlMughyiri/test-in/issues/208)                                                                                                                                                                                                                                                                                                                                            |
| **Difference 14** | A card dragged between two visible cards landed after whatever the filter was hiding between them. Fixed 7 September 2026, [#209](https://github.com/MutebAlMughyiri/test-in/issues/209)                                                                                                                                                                                                                                                                                                                             |
| **Difference 20** | A filtered walk completed the whole test run and turned every other pending test case untested. Fixed 7 September 2026, [#214](https://github.com/MutebAlMughyiri/test-in/issues/214)                                                                                                                                                                                                                                                                                                                                |
| **Difference 21** | **Start Manual Execution** was live on a test run holding no test cases and on a filter matching nothing, and pressing it marked the test run **In Progress**. Fixed 7 September 2026, [#215](https://github.com/MutebAlMughyiri/test-in/issues/215)                                                                                                                                                                                                                                                                 |
| **Difference 22** | The walk landed on test cases that already had a run item status, timed them again, and re-stamped who judged them and when. Fixed 7 September 2026                                                                                                                                                                                                                                                                                                                                                                  |
| **Difference 23** | A test run whose every test case was judged from the menu or the keyboard stayed **In Progress**. Only an automated run item status completed it. Fixed 7 September 2026, [#217](https://github.com/MutebAlMughyiri/test-in/issues/217)                                                                                                                                                                                                                                                                              |
| **Difference 24** | **Refresh** stopped an execution and said only *Refreshed*. Fixed 7 September 2026, [#218](https://github.com/MutebAlMughyiri/test-in/issues/218)                                                                                                                                                                                                                                                                                                                                                                    |
| **Difference 27** | Stopping a test case marked the test run **In Progress** first, and so did clicking the icon that only navigates to the test method. Fixed 7 September 2026, [#221](https://github.com/MutebAlMughyiri/test-in/issues/221)                                                                                                                                                                                                                                                                                           |
| **Difference 28** | Closing the tab left the automation running and its run item statuses homeless. Fixed 7 September 2026, [#222](https://github.com/MutebAlMughyiri/test-in/issues/222)                                                                                                                                                                                                                                                                                                                                                |
| **Difference 29** | `Escape` in the failure dialog threw away everything typed with no confirmation. Fixed 7 September 2026, [#223](https://github.com/MutebAlMughyiri/test-in/issues/223). Reversed 14 September 2026: the question never showed, because the IDE closes a Testin dialog on `Escape` before Testin sees the key, and by decision no Testin dialog asks before `Escape` any more ([#66](https://github.com/MutebAlMughyiri/test-in/issues/66), finding 154)                                                              |
| **Difference 26** | An automated pass cleared the actual result, the error, the screenshots and the bug issue link and reset the bug severity and priority, and an automated failure cleared what the last failure said. Fixed 11 October 2026, #415: every run item status is an execution of its own, and recording one never erases another (Rule-EDITOR-PANEL-284)                                                                                                                                                                   |
| **Difference 2**  | **Test Data** and **Pre Conditions** were drawn in the create dialog with no key that opened either. Fixed 8 September 2026, [#198](https://github.com/MutebAlMughyiri/test-in/issues/198)                                                                                                                                                                                                                                                                                                                           |
| **Difference 15** | Dragging cards said *Re-sorted* and moving a test case by typing its number said *Updated* - the same act, two words. Reordering says *Re-sorted* whichever way it was done. Fixed 9 September 2026, [#210](https://github.com/MutebAlMughyiri/test-in/issues/210)                                                                                                                                                                                                                                                   |
| **Difference 16** | `Shift+Enter` saved a bulk edit and the strip named only `Enter`. Fixed 9 September 2026, [#211](https://github.com/MutebAlMughyiri/test-in/issues/211)                                                                                                                                                                                                                                                                                                                                                              |
| **Difference 3**  | The group tick boxes read **REGRESSION** where every other surface reads **Regression**, because the box's own text was the group's identity. Fixed 9 September 2026, [#199](https://github.com/MutebAlMughyiri/test-in/issues/199)                                                                                                                                                                                                                                                                                  |
| **Difference 5**  | Two buttons on one toolbar both had the tooltip **Details**. The one that picks what a card shows is **Fields**, which is what this document already called it. Fixed 9 September 2026, [#201](https://github.com/MutebAlMughyiri/test-in/issues/201)                                                                                                                                                                                                                                                                |
| **Difference 11** | The page size box answered a different number without a word. A number outside the range now says what the range is; a blank box or letters asked for nothing and still say nothing. Fixed 9 September 2026, [#206](https://github.com/MutebAlMughyiri/test-in/issues/206)                                                                                                                                                                                                                                           |
| **Difference 18** | The status bar read *0 of 12 test cases* with nothing selected, putting a position where a tester reads a count. It says the count alone now. Fixed 9 September 2026, [#213](https://github.com/MutebAlMughyiri/test-in/issues/213)                                                                                                                                                                                                                                                                                  |
| **Difference 1**  | `Ctrl+C` said *Details copied* and copied one line, the description, because `Can.COPY` was declared on that one field of eighteen. It copies the ten fields the tester writes now, and drops the ones they left empty (Rule-EDITOR-PANEL-207). Fixed 10 September 2026, [#197](https://github.com/MutebAlMughyiri/test-in/issues/197)                                                                                                                                                                               |
| **Difference 4**  | The filter offered eight groups and the create dialog three, so Security, UI, Functional and Validation could be filtered on and never typed - while an import, a paste, a Git merge and a bulk edit all put them on a test case perfectly well. The dialog offers every group now, and the `active` flag that held four of them back is gone, along with an `assignable` flag nothing ever read. Fixed 10 September 2026, [#200](https://github.com/MutebAlMughyiri/test-in/issues/200)                             |
| **Difference 8**  | An unreadable priority became the lowest and an unreadable status kept the old value - two columns, two answers to a typo, both silent. All four parsed columns refuse now and keep what was there, and the message names the text and the column (Rule-EDITOR-PANEL-206). Fixed 9 September 2026, [#204](https://github.com/MutebAlMughyiri/test-in/issues/204)                                                                                                                                                     |
| **Difference 30** | `Ctrl+Right` turned the page here and moved to the next test case in the view panel, and the constant behind both was called `NextTestCase` while the editor used it for pages - so the code disagreed with itself and the key reference listed the key twice with two meanings. The key is one gesture, forward in whichever surface has the keyboard, and the constant is called `Next`. Fixed 10 September 2026, [#224](https://github.com/MutebAlMughyiri/test-in/issues/224)                                    |
| **Difference 12** | Unticking **Order** stopped three gestures in the grid: clicking a row to select it, `Enter` to open the details panel, and the double-click. **Order** is locked on now, the way **Description** already was - it is the grid's row header and the target of the two gestures that are not edits, not a field a tester chooses, and `FieldDefault.LOCKED_CHECKED` named it in its own text while the constant said otherwise. Fixed 9 September 2026, [#207](https://github.com/MutebAlMughyiri/test-in/issues/207) |
| **Difference 17** | The search read the description, the identity, the expected result and the steps, and knew nothing of the module, the group, the test data or the pre-conditions - each of which has its own column, and three of which have their own filter. It reads every field the tester writes now, the reference included (Rule-EDITOR-PANEL-091). Fixed 9 September 2026, [#212](https://github.com/MutebAlMughyiri/test-in/issues/212)                                                                                     |
| **Difference 6**  | Cutting or pasting a block of grid cells raised one message per cell, so a row cut across its columns read *Updated 17* for one test case. The writes of one gesture all land in one event now, so they are counted together, and saved and undone together (Rule-EDITOR-PANEL-226). Fixed 18 September 2026, [#66](https://github.com/MutebAlMughyiri/test-in/issues/66) finding 171; retired from this list 20 September 2026                                                                                      |
| **Difference 25** | An automated execution raised one message per test case, so 50 test cases raised 50 balloons. It says nothing per test case now, and one line when it has nothing left to report — *Passed 42, Failed 8*. The words are the status bar's own, so the balloon and the bar cannot count one test run differently. Fixed 9 September 2026, [#219](https://github.com/MutebAlMughyiri/test-in/issues/219); retired from this list 20 September 2026                                                                      |
| **Difference 19** | `P`, `F` and `B` still recorded a run item status on a Completed or Closed test run, though the status bar's tooltip said it records no more. They are gray on a signed off test run now, with the reason, and so are the failure details and the grid's Actual Result. Fixed 2 October 2026, [#84](https://github.com/MutebAlMughyiri/test-in/issues/84)                                                                                                                                                            |

---

## Not decided

**Question 1** — Should **Start Manual Execution** walk the whole test run or
the filtered list? It walks the filtered list today, and finishing it completes
the whole test run. Either the walk should ignore the filter, or completing
should only count what was walked.

**Question 2** — Should a run item status be recordable on a completed or
closed test run? The keyboard allows it, the automation refuses it, and the
tooltip says it is refused.

**Question 3** — Should a test case that already has a run item status be
offered again by the walk? It is offered today, and judging it again re-stamps
who and when.

**Question 4** — Four groups can be filtered on and never assigned. Either the
create dialog should offer them or the filter should not.

---

[Documentation](../README.md) › **The editor panel**
