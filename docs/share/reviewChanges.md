[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-010

# UC-SHARE-010: See what I have not committed

**As a** tester, **I want** to see every change I have made after my last
commit, **so that** I can decide what to send and what to put back.

This lists everything the tester changed since the last commit, one row for
each changed field. It is also where a commit is written and sent.

There is no key on the menu. The **Review Changes** button on the message about
uncommitted work reaches it.

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
- **Rule-SHARE-044** — The review reads Git directly, so a repository the IDE
  does not track and a test case Git has never seen both appear.
- **Rule-SHARE-045** — One row for each changed **field**, not one for each
  changed file.
- **Rule-SHARE-046** — A file Git says has changed gets a row even when Testin
  can see no difference inside it, so the tester can still commit it.
- **Rule-SHARE-047** — Testin decides what a file is by its name, not by reading
  it: a `.tc` is a test case, a `<test case id>.ri` is one test case's result,
  and the seven fixed marker names are markers. A record is identified by the
  name it is written under, so a file the review cannot parse still gets the
  right kind of row.
- **Rule-SHARE-048** — A rename is two rows, the removal first and the addition
  second.
- **Rule-SHARE-049** — Every row arrives selected.
- **Rule-SHARE-105** — Without the Git plugin, **Sync with Remote** and **View
  Pending Commits** are still in the menu, grayed, and each says what it needs:
  *(needs the Git plugin)*. They are not left out. The menu then has the same
  shape in every IDE, and a tester can see the feature exists and learn what to
  install.
- **Rule-SHARE-050** — Reading Git happens off the main thread, so the IDE stays
  usable.
- **Rule-SHARE-112** — A test run's screenshots travel with it. The review lists
  none of them on its own, and committing the test run commits the screenshots its
  folder gained or lost.
- **Rule-SHARE-127** — Every Git step that fails says what Git said and offers
  Show Git log. It opens the IDE's Git tool window, where Git's own output is
  kept. That holds for reading the changes, committing, choosing a branch,
  putting a folder under Git, setting the remote or the identity, Sync, the
  push, and every step of resolving conflicts.
- **Rule-SHARE-129** — Every marker in View Pending Commits is ticked and cannot
  be unticked, because a node's own file always goes with the commit. Its row is
  gray and says so when pointed at.
- **Rule-SHARE-131** — A Steps change is one row, naming only the steps that
  changed, each with its number. History names the same steps.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Pending Changes                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│ [x]| Change Type      | Test Set | Name          | Before   | After        │
│ [x]| Description      | Login    | Log in with.. | Log in   | Sign in      │
│ [x]| Expected Result  | Login    | Log in with.. | It opens | It opens now │
│ [x]| New Test Case    | Login    | Log out       |          | Log out      │
├────────────────────────────────────────────────────────────────────────────┤
│  BRANCH                                                                    │
│  [ main                                                                v]  │
│                                                                            │
│  [ what changed, in a line...                                            ] │
│                                                                            │
│                                        [ Commit & Push  v ]                │
├────────────────────────────────────────────────────────────────────────────┤
│  [k]  Enter Commit & Push   Right click Revert a change   Escape Cancel    │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The tick column** — every row arrives ticked.
2. **Change Type** — what kind of change this row is.
3. **Test Set** — filled for a test case, and for a result: the set its test
   case sits in. Blank for a test run, a marker, or any other file.
4. **Name** — the test case's description; for a result, the description of the
   test case it is about; for a marker, the folder it sits in; otherwise the
   file's name.
5. **Before** and **After** — the two values of the field that changed.
6. **Branch** — the branch the commit goes onto. It can be typed into, and a
   name that is not a branch yet starts one.
7. **The message box** — no label. It shows a gray hint instead.
8. **Commit & Push** — a split button. Its arrow offers **Commit** alone.
   `Enter` presses whichever of the two is chosen, and the bottom line names
   it.

## Main flow

1. The tester selects a test project and chooses **View Pending Commits**.
2. A progress bar reads *Scanning for changes*, and can be canceled.
3. Testin asks Git what has changed. It then reads each changed file.
4. The **Pending Changes** dialog opens with one row for each changed field.
5. The tester unticks the rows they are not ready to send.
6. The tester types a message and presses **Commit & Push**.

## What Testin refuses

**If nothing that is a test project is selected** — the menu entry is gray.

**If the folder is not a Git repository** — the message offering to make one.
That is [UC-SHARE-009](putUnderGit.md).

**If a pull stopped on a conflict earlier** — the conflict offer appears instead
of the review. That is [UC-SHARE-017](resolveConflicts.md).

**If nothing has changed and nothing is unpushed** — a message reads *No
changes*.

**If nothing has changed but a commit never reached the remote** — that
message is shown instead. That is [UC-SHARE-015](pushOldCommit.md).

**If one file cannot be read** — the row is still listed. It carries only what
Git said about the file.

**If Git listed a new file that is already gone** — the row is dropped. Only
the log says so.

**If the changed file is a screenshot inside a test run** — it gets no row of
its own (Rule-SHARE-112). A screenshot only arrives or goes because a result
started or stopped naming it, and committing that result carries it. Inside a
test run only: a picture a tester keeps in a test set gets a row like any other
file, so it can still be committed.

**If the IDE has no Git plugin** — the menu entry is still there, grayed,
reading *(needs the Git plugin)*, as Rule-SHARE-105 says.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
