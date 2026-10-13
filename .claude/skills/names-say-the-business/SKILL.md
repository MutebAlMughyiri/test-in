---
name: names-say-the-business
description: Every class, method, field, variable and heading is named with the business word, in full - a test case is a test case, never a "case". Use when adding or renaming anything in Testin, when reading a name that shortens a domain word, and when writing docs or an issue about one. Triggers on "case", "item", "run", "set", "node" standing alone in a name.
---

# Names say the business word, in full

Testin's vocabulary is fixed, and `docs/standard.md` writes it down: a **test
case**, a **test set**, a **test run**, a **run item**, a **test project**. Every
name in the code, the docs and the issues uses the whole word.

> ## `CaseDetails` is not a thing. `TestCaseDetails` is.

Muteb, 22 September 2026: *"[I] found class called CaseDetails, it is
TestCaseDetails not CaseDetails... this is not acceptable, as it belongs to test
case. correct name will allow me and contributors to understand easily and any
name should match our correct names in business."*

## The rule

**Name it after the thing it is about, with the business word spelled out.**

| Not this                    | This                                         | Why                                                                                           |
|-----------------------------|----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `CaseDetails`               | `TestCaseDetails`                            | A case is a court case. The plugin has test cases                                             |
| `caseId`, `caseIds`         | `testCaseId`, `testCaseIds`                  | The id of what?                                                                               |
| `cases`                     | `testCases`                                  | Even where the type says `List<TestCase>`: the reader is reading the name                     |
| `shownCase`, `liveCase`     | `shownTestCase`, `liveTestCase`              | Same word, same thing, everywhere                                                             |
| `item` on its own           | `runItem`                                    | A run item is a test case's row in a test run                                                 |
| `A_CASE`, `JUDGED_CASE`     | `A_TEST_CASE`, `JUDGED_TEST_CASE`            | Constants are names too                                                                       |
| `aCaseThatArrivesIsPending` | `aTestCaseThatArrivesIsPending`              | A test's name is a sentence about the business                                                |
| `RunEditor`, `runPath`      | `TestRunEditor`, `testRunPath`               | Run alone is a verb. The thing is a test run                                                  |
| `TestStatus`, `verdict`     | `RunItemStatus`, `runItemStatus`             | Passed, Failed, Blocked is what a test run records for a run item                             |
| `status` of one execution   | `runItemExecutionStatus`                     | A run item has many executions, each with its own status. `testNGExecutionStatus` is TestNG's |
| `RunStatus`, `RunRegistry`  | `TestNGExecutionStatus`, `ExecutionRegistry` | Executing TestNG methods is an execution, not a test run                                      |
| `TestEditor`, `TestToolbar` | `TestSetEditor`, `TestSetToolbar`            | The editor of a test set's test cases                                                         |

It holds for the documentation and for issues as well: *test case id*, never *case id*; *run item*, never *item*.

**And for every sentence written to Muteb** - chat, a design, an `AskUserQuestion`
brief and its options. A question that says "the run" or "run the tests" was sent
back unanswered on 10 October 2026: *"resend this question with selection by use our
names based on code and business, don't say run, say test run or run item"*.

| Not this, in any sentence                    | This                                                                                  |
|----------------------------------------------|---------------------------------------------------------------------------------------|
| the run, a run, open a run, complete the run | the test run, open a test run, set the test run Completed                             |
| run the tests, runs TestNG                   | execute the automation, executes the TestNG methods                                   |
| results, test results                        | run items, run item statuses (TestNG's own file keeps its name, `testng-results.xml`) |
| the cases                                    | the test cases                                                                        |

Before sending any message about test runs, read it once for the bare word *run*.

## Name the container, and name the thing

> *"test set has many test case, test run has many run items, so we can not say
> TestCaseEditor and TestRunEditor, we must say TestSetEditor and TestRunEditor.
> if you are not specific in names of methods and classes you will be confused and
> may implement something wrong, a contributor may be confused, and the business
> will not be clear."* - Muteb, 9 October 2026

Two halves, and a pair of names must sit on the same half:

- **What opens a container is named for the container.** An editor, its
  toolbar, its context menu, its grid listener and its package: `TestSetEditor`
  beside `TestRunEditor`, `TestSetToolbar` beside `TestRunToolbar`,
  `editor/testset` beside `editor/testrun`.
- **What shows one thing is named for that thing.** A card and its renderer:
  `TestCaseCard` beside `RunItemCard`, never `TestRunCard`.
- **A view is named for what it draws.** `CardView` beside `GridView`; the
  button a tester presses says **Card View**.
- **One thing is singular, a list is plural.** `RunItem` for one run item, never
  `TestRunItems`; `groups` for a test case's list of groups, never `group`.
- **A run item is never a result.** `getRunItems()`, `runItemOf(id)`,
  `RunItemsInTestCaseOrderTest`, and the words a tester reads: Record Run Item,
  Run items not read. "Result" stays only where the business says it: Actual
  Result, Expected Result, Result Analysis.
- **Where a thing lives is its path**, as `getPath()`, `testRunPath` and
  `testSetPath` already say. Which run item, by test run and test case, is a
  `RunItemPath` - not an address, a key or a reference.

| Not this                               | This                                         |
|----------------------------------------|----------------------------------------------|
| `TestCaseEditor`, `TestCaseToolbar`    | `TestSetEditor`, `TestSetToolbar`            |
| `TestRunCard`, `TestRunListRenderer`   | `RunItemCard`, `RunItemCardRenderer`         |
| `ListView`, `ListViewBtn`, `LIST_VIEW` | `CardView`, `CardViewButton`, `CARD_VIEW`    |
| `TestRunItems`, `getResults()`         | `RunItem`, `getRunItems()`                   |
| `bug.RunItem` (a test run and an id)   | `RunItemPath`                                |
| `FailedResultDialog`                   | `FailureDetailDialog`                        |
| `EditorKind.TEST`, `EditorKind.RUN`    | `EditorKind.TEST_SET`, `EditorKind.TEST_RUN` |

**The stored keys say the same word as the code and the screen.** A marker's
`modifiedBy` became `updatedBy`, as a test case and every screen say; a test
case's `group` became `groups`; `testin.yml`'s `RepoUrl` became `repoUrl`. A
stored key is renamed on purpose, never by a sweep: it moves the format number
on (Rule-INTERNAL-091), it gets no `@JsonAlias`, and old data is wiped rather
than converted. A key that is persisted but not seen as a word - a settings key
such as `testin.selectedDetails.test.v4` - keeps its string when the surrounding
constant is renamed.

## No short names

The `Project` is `project` and one `TestCase` is `testCase`. Muteb retired
the last two short names, `p` and `tc`, on 13 October 2026 (#426): *"[I] don't
want to have abbreviations for my business"*, then *"don't use `p`"*. What may
still be shortened is Java's own idioms and the known acronyms, and the
**Abbreviations** table in CLAUDE.md is the one list of them.

Anything else gets its full name. A local called `dto`, `obj`, `data` or `list`
says nothing that the line above it did not already say.

## Why the whole word

A name is read far more often than it is written, and by people who did not
write it. **The reader cannot look up what "case" means here** - they have to
find the declaration, read the type, and carry the answer back. A shortened
domain word also collides: `case` is a Java keyword, letter case, a use case and
a court case, so every reader pays for the ambiguity.

The other half is search. `grep -rn "TestCase"` finds every place the concept
lives. Every name that says `Case` instead is a place the search misses.

## What a rename costs, and how to do it

The sweep on 22 September 2026 renamed 10 types and about 600 identifiers across
137 files - `CaseDetails`, `CaseList`, `CaseFont`, `CopiedCase`, `MovedCase`,
`EditShownCase`, `ShownCaseAction` and the rest. The compiler and 767 tests
verified it, and no string, bundle key or stored value changed.

Renaming is a three-step check, not a find-and-replace. CLAUDE.md states it:

1. **Never touch a string by accident.** A bundle key, a marker file name, a stored JSON
   field and a `@State` name all read like code and are none of it. Rename one
   only on purpose, as the section above says, and diff every literal before and after.
2. **Check the new name is free in that file.** `cases` renamed to `testCases`
   where a `testCases` already exists is two variables with one name.
3. **Compile and run the tests.** A rename that compiles and passes is done; a
   rename that needs a comment explaining it is not.

## Checks before you name something

1. Which business word is this? Write it out in full.
2. Would a contributor who has never read this file know what the name holds?
3. Does the name repeat what the type already says - `dto`, `list`, `obj`? Say
   the business word instead.
4. Is it shortened at all? Then it is in CLAUDE.md's **Abbreviations** table,
   or it is spelled out.
