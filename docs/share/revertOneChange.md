[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-011

# UC-SHARE-011: Put one changed field back

**As a** tester, **I want** to undo one field I changed by mistake, **so that** I do not have to throw away a morning's
work to lose one typo.

Each row of the review is one changed field. This puts that one field back to
what was committed, and leaves the rest of the test case alone.

Right-click the row in the review.

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
- **Rule-SHARE-051** — Only a change to a test case can be put back: one changed
  field, a new test case, or a removed one.
- **Rule-SHARE-052** — Only that one field is put back. Everything else on the
  test case stays as it is. When it was the test case's last change, who created
  and changed it goes back to what was committed too, so the test case leaves the
  review.
- **Rule-SHARE-053** — The change is written to disk at once, not on the commit.
- **Rule-SHARE-118** — Right-clicking a row that is already selected leaves the
  selection as it is, so what Commit will send does not change by looking at the
  menu. The menu's entry acts on the row that was right-clicked.
- **Rule-SHARE-119** — A revert can be taken back. It goes on the test set's own
  history like every other change to a test case, so Ctrl+Z in that test set's
  editor puts back what the revert threw away: the value that was typed, the new
  test case it removed, or the removal it undid. It is the only way back,
  because the revert writes to disk at once.

## The screen

```
┌────────────────────────────────────────────────────────────────────┐
│[x]  | Change Type      | Test Set | Name             | Before      │
│[x]  | Description      | Login    | Log in with a..  | Log in      │
│[x]  | Expected Result  | Login    | Log in with a..  | It opens    │
│[x]  | New Test Case    | Login    | Log out          |             │
└────────────────────────────────────────────────────────────────────┘
                ┌──────────────────────┐
                │  Revert this change  │
                └──────────────────────┘
```

1. **The rows** — the changed fields. [UC-SHARE-010](reviewChanges.md) draws
   the whole dialog around them.
2. **The menu** — right-clicking a row opens it. It holds one entry.
3. **Revert this change** — puts that one field back, then takes the row away.
   `Ctrl+Z` in that test set's editor takes the revert back (Rule-SHARE-119).

## Main flow

1. The tester is reading the **Pending Changes** table.
2. One row shows a description they did not mean to change.
3. The tester right-clicks that row.
4. A menu offers **Revert this change**.
5. The tester chooses it.
6. The field goes back to what it was before.
7. A message reads *Reverted*.
8. The row leaves the table.

## What Testin refuses

**If the row is not a change to a test case** — a message reads *Only a test
case change can be reverted*.

**If that kind of change cannot be put back** — a message reads *A change to*,
then the kind, then *cannot be reverted*. A test case that changed with no field
different - a **Change File** row reading *No field changed* - is the one
of those.

**If the test case is no longer in the test project** — a message reads *That
test case is no longer in the project*.

**If the file cannot be written, or the recycle bin does not take it** — the
row stays, and nothing reads *Reverted*. A message reads *unable to write
content:* and the reason, a notification titled *Not Removed* names the file
(Rule-INTERNAL-143), or one titled **Revert Failed** carries it.

## A new or removed test case

Reverting a **Create Test Case** row removes that new test case, and the test
method Testin generated for it (Rule-CODEGEN-049). Reverting a **Remove Test Case** row puts the removed test case back
as it was committed,
with who created it and when.

## What cannot be put back this way

A reordering or a restamp of a test case with no field changed. A change to a
test run, a result, a marker, or anything Testin did not read as a test case.
A result is a record of work rather than an edit, so putting one back would say
a test case was never executed. For those,
use Git's own tools.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
