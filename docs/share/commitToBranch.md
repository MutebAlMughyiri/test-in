[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-014

# UC-SHARE-014: Commit onto a different branch

**As a** tester, **I want** to put these changes on another branch, **so that** work for the next release does not land
on the one being tested now.

A branch is one line of work in Git. This picks the branch the commit goes
onto, without leaving the review dialog.

There is no key for this. The **Branch** box is in the review.

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
- **Rule-SHARE-063** — The box lists the branches on this machine, with the one
  checked out chosen.
- **Rule-SHARE-064** — The box can also be typed into. A name that is not a
  branch yet starts one.
- **Rule-SHARE-065** — If the branch cannot be checked out, nothing at all is
  committed.
- **Rule-SHARE-126** — Choosing a branch in the box checks out the branch the
  box names. A local branch whose name holds a slash, such as feature/login, is
  that local branch, never a remote one, so nothing is checked out under a
  shortened name.
- **Rule-SHARE-127** — Every Git step that fails says what Git said and offers
  Show Git log. It opens the IDE's Git tool window, where Git's own output is
  kept. That holds for reading the changes, committing, choosing a branch,
  putting a folder under Git, setting the remote or the identity, Sync, the
  push, and every step of resolving conflicts.
- **Rule-SHARE-135** — A Branch box left empty commits on the branch that is
  checked out.

## The screen

The box sits under the table in the review dialog.
[UC-SHARE-010](reviewChanges.md) draws the whole dialog.

```
┌──────────────────────────────────────────────────────────────┐
│  BRANCH                                                      │
│  [ main                                                   v] │
└──────────────────────────────────────────────────────────────┘
  ┌──────────────────────────────────────────────────────────┐
  │  main                                                    │
  │  release-2.4                                             │
  └──────────────────────────────────────────────────────────┘
```

1. **The box** — the branch the commit goes onto. It opens on the branch that
   is checked out.
2. **The list** — the branches on this machine (Rule-SHARE-063).
3. **Typing** — the box can be typed into. A name that is not on the list
   starts a new branch.

## Main flow

1. The tester opens the **Pending Changes** dialog.
2. The **Branch** box shows the branch they are on.
3. The tester types a name that does not exist yet.
4. The tester types a message and presses **Commit & Push**, or `Enter` from
   any field, the **Branch** box included once its list is closed (Rule-INTERNAL-085).
5. A bar reads *Preparing the branch*.
6. Testin makes the branch and checks it out.
7. The commit is made on it, and pushed.

## What Testin refuses

**If the branch cannot be checked out or started** — a message titled **Branch
Not Switched** reads the branch's name, then *could not be checked out, so
nothing changed: the changes are still here and still yours. Git said:*, then
what Git said. Nothing is committed (Rule-SHARE-065). Two links sit under it:
**Review Changes**, which opens the dialog again, and **Show Git log**.

**If the branch cannot be prepared for any other reason** — a message titled **Git Error** reads *Could not prepare*,
the branch, then the reason.

## Why nothing is committed on a failure

A commit on the wrong branch would be worse than no commit. So the branch is
prepared first, and everything stops if it cannot be. The tester's changes are
untouched, and the message says so in those words.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
