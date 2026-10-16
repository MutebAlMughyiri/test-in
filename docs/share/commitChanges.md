[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-012

# UC-SHARE-012: Commit without pushing

**As a** tester, **I want** to record my work locally, **so that** I can decide about the team's copy later.

A commit records the work in Git on this machine. Nothing is sent to the team
until it is pushed.

There is no key for this. **Commit** is behind the arrow of the split button.

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
- **Rule-SHARE-054** — The marker files above every committed test case are
  committed too, whether they were ticked or not.
- **Rule-SHARE-055** — Only a path that is really on disk is added. A path that
  is gone is committed as a removal.
- **Rule-SHARE-056** — No Git command Testin runs may open an editor.
- **Rule-SHARE-057** — A password inside a remote address is never written to
  the log or shown.
- **Rule-SHARE-058** — The list of paths reaches Git in a file, not on the
  command line, so a very large commit does not fail for length. Git does not
  check each file's line endings either, so a commit of thousands of files
  prints no warning for each one and no line-ending setting refuses it.
- **Rule-SHARE-112** — A test run's screenshots travel with it. The review lists
  none of them on its own, and committing the test run commits the screenshots its
  folder gained or lost.
- **Rule-SHARE-130** — A commit makes every Completed test run of the test
  project Committed whose own files and whose test cases' changes are all in it:
  Testin writes the commit into the test run and commits it straight after,
  as Record test run <name> as committed. A Completed test run with a change left
  out of the commit stays Completed, and a later commit records it. With Commit &
  Push both commits go out in one push.

## The screen

The button sits at the bottom right of the review dialog.
[UC-SHARE-010](reviewChanges.md) draws the whole dialog.

```
  [ Commit & Push   v ]
  ┌────────────────────┐
  │  Commit            │
  └────────────────────┘
```

1. **Commit & Push** — the face of the button. A click runs it.
2. **The arrow** — opens the one other answer.
3. **Commit** — records the work on this machine, and sends nothing.

## Main flow

1. The tester unticks what they are not ready to send.
2. The tester types a message.
3. The tester opens the arrow beside **Commit & Push** and chooses **Commit**.
4. The dialog closes.
5. A bar reads *Preparing the branch*. A second bar reads *Committing to
   local Git*.
6. Testin adds the ticked paths and their markers, and commits.
7. A message titled **Committed** names the commit.

## What Testin refuses

**If no row is ticked** — the button is gray. Hovering over it reads *Choose at least one change*.

**If the message is blank** — the gray hint turns red and the box takes the
cursor. Nothing is committed. The dialog stays open.

**If Git does not know who the tester is** — the identity dialog opens instead,
and the commit follows it. That is
[UC-SHARE-008](setGitIdentity.md).

**If the commit is in but recording the Completed test runs fails** — a
message titled **Test Runs Not Recorded** says the tester's commit is in and the
next commit takes them. Recording fails when Git refuses the second commit, or
when a test run's marker was not written in time. With Commit & Push, the push
still goes (Rule-SHARE-130).

**If the commit fails for any other reason** — a message titled **Commit
Failed** reads *Failed to commit changes:* and then the reason.

## Why the markers go too

A test set carries a small marker file that says what it is. Now picture a test
case committed without that marker. In a colleague's copy it lands in a folder
Testin does not know, so the test case is invisible there. Committing the
markers is therefore not optional, and Testin does it whether they were ticked
or not.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
