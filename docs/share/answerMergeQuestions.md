[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-018

# UC-SHARE-018: Answer which side wins for a field both changed

**As a** tester, **I want** to choose between my wording and a colleague's, **so that** a merge does not silently throw
away one of them.

When two people change the same field, only one value can be kept. This window
shows both and lets the tester pick.

**Two kinds of file raise a question, and one never does.** A test case asks
about a field both sides rewrote. A test run's own `.testrun` asks about a key both
sides wrote differently into its configuration or its result analysis. The row
is named as the tester already knows that key - **Platform**, or the heading the
analysis was written under. A run item is
never asked about at all: both sides' executions are joined in time order,
because each execution is one account of one attempt - its status, actual
result, stacktrace and screenshots travel together - and neither side's
attempt is wrong ([UC-SHARE-017](resolveConflicts.md)).

There is no key that opens this. It opens during a merge.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones can
  be canceled: none of them leaves anything the tester cannot see.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-080** — Only what both sides really changed is asked about, and
  only where an answer is the tester's to give. Everything else is merged
  without a question: a field one side left alone, a test run's start and end
  and status, and a run item's executions.
- **Rule-SHARE-081** — One window for each conflicted file, holding one question
  for each thing that disagrees: a field of a test case, or a key both sides
  wrote differently into a test run's own marker. A run item raises no
  question: both sides' executions are joined in time order, an execution both
  hold is kept once, and the default chosen and the bug fields come from the
  side that executed last.
- **Rule-SHARE-082** — The tester's own value is chosen to start with.
- **Rule-SHARE-083** — A value is shown on one line, cut at 70 characters. An
  empty one says that it is empty rather than showing nothing.
- **Rule-SHARE-084** — `Escape` answers nothing for this file and moves on to
  the next. The file is left as the other side has it and is named at the end
  with everything else that was not resolved. Answers already given are kept.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Both Changed Log in with a valid user                                     │
├────────────────────────────────────────────────────────────────────────────┤
│  DESCRIPTION                                                               │
│  (x) Mine: Log in with a valid user                                        │
│  ( ) Remote: Sign in with a valid account                                  │
│                                                                            │
│  STEPS                                                                     │
│  (x) Mine: ["open the app", "sign in"]                                     │
│  ( ) Remote: (empty)                                                       │
│                                                                            │
│                                              [ Keep Selected ]             │
├────────────────────────────────────────────────────────────────────────────┤
│  [k]  Enter Keep Selected       Escape Skip This One                       │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The title** — the words **Both Changed**, then what the tester calls the
   thing: a test case's description, and for a test run's marker the test run
   itself, which is the folder it sits in. Neither is ever the file's name:
   every test run holds a marker called `.testrun`, so three test run conflicts in a
   row would all be titled the same.
2. **Each row** — the field, then the two values. **Mine** is chosen to start
   with. A test run's key is named as the tester already knows it: the question
   they answered when the test run was created, or the heading they wrote their
   analysis under.
3. **Keep Selected** — writes the answers for this file.

## Main flow

1. The merge finds a test case, or a test run's own marker, that both sides
   changed.
2. The window opens with one row for each field or key that disagrees.
3. The tester reads both values and picks one for each row.
4. The tester presses `Enter`.
5. The merged file is written, and the merge moves to the next one.

## What Testin refuses

**If the tester presses `Escape`** — nothing is written for this file. The sync
moves on to the next question. Every answer already given is kept, and this file
is named at the end with everything else left unresolved.

**If a value is too long to show** — it is cut at 70 characters on screen. The
whole value is still what gets written.

**If the conflicted file is a result** — no window opens. The side whose
`executedAt` is later takes the file whole, and nothing is asked. Nothing is
said either, because the settled list is carried by this window and the window
never opens. That is difference 20. Two testers judging *different* test cases
of one test run never conflict at all: their run item statuses are in different
files.

**If two testers only moved a test run along** — no window opens either. The
start, the end, the status and the audit block are settled by rule, and a
question comes only from a configuration or result analysis key both of them
wrote differently.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
