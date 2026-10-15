[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-017

# UC-SHARE-017: Resolve the conflicts a pull stopped on

**As a** tester, **I want** to settle a file two of us changed, **so that** the pull can finish and neither of us loses
work.

A conflict is a file two people changed since they last agreed. The pull stops
until somebody says which change wins.

There is no key for this. The offer appears on the message.

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
- **Rule-SHARE-074** — A merged test case Git will not stage is reported to the
  tester, with what to do about it. The sync cannot go on without it.

- **Rule-SHARE-075** — A pull that stops leaves the repository part way through,
  and Testin says so rather than leaving the tester to find out.
- **Rule-SHARE-076** — Three answers are offered: let Testin settle the test
  cases field by field and then carry on, carry on without merging anything, or
  roll the whole pull back.
- **Rule-SHARE-077** — Rolling back keeps everything that was here before the
  pull.
- **Rule-SHARE-078** — Where both sides changed different fields of one test
  case, Testin merges them and asks only about the fields that really disagree.
- **Rule-SHARE-109** — Two fields are settled rather than asked about, and the
  dialog says so. Who changed the test case last, and when, take the more
  recent of the two edits; the order takes the remote's. Neither is a question
  a tester can usefully answer about a merge, so the answer is given and named
  rather than asked for.
- **Rule-SHARE-079** — A pull that will not move on is said again: the **Git
  Conflicts** message comes back, naming the files still in the way.
- **Rule-SHARE-127** — Every Git step that fails says what Git said and offers
  Show Git log. It opens the IDE's Git tool window, where Git's own output is
  kept. That holds for reading the changes, committing, choosing a branch,
  putting a folder under Git, setting the remote or the identity, Sync, the
  push, and every step of resolving conflicts.

## What the tester sees

No window opens first. A warning message appears at the bottom right of the
IDE, titled **Git Conflicts**, naming the files both sides changed. It waits in
the IDE's notification list, and three links sit under it: **Resolve**, **Continue rebase** and **Abort rebase**.

## Main flow

1. The tester syncs, and the pull stops on a conflict.
2. A message titled **Git Conflicts** reads *Both sides changed*, names the
   files, then *Resolve the conflict, then continue - or abort to undo the pull
   and keep what is here.*
3. The tester chooses to carry on.
4. Testin merges each conflicting test case, one field at a time.
5. For any field both sides rewrote, the tester is asked which one wins. That is
   [UC-SHARE-018](answerMergeQuestions.md).
6. The merged files are given back to Git and the pull carries on.
7. The sync ends the way one that never stopped does: a message titled **Synced** reads *Pushed* and the count, or *Up
   to date with the remote*.

A push from **Pending Changes** that stops on a conflict ends differently, with a
message titled **Rebase continued** reading *Changes pushed to the remote*.

## What Testin refuses

**If the tester chooses to roll back** — a message titled **Rebase aborted**
reads *The pull was rolled back*, or *Nothing was pushed*. Everything that was
here before the pull is still here.

**If rolling back fails** — a message titled **Git Conflict Operation Failed**
reads *Could not abort the rebase.*

**If carrying on fails** — the same title, reading *Could not continue the
rebase.*

**If conflicts remain after the merge** — the conflict offer opens again, naming
what is still in the way, with the same three links: Resolve, Continue and
Abort. It used to name them under a plain warning when the tester had come from
Pending Commits, and offer the three links only when they had come from Sync.
That was the same situation answered two ways, and the way out on screen only
once.

**A run item two testers executed** — both executions are kept. Each
execution is one tester's account of one attempt: the status, when they gave
it, what they saw, the stacktrace, the screenshots, the bug severity and
priority, and the issue it was reported as. Those travel together, so the two
lists are joined in `executedAt` order, an execution both sides hold is kept
once with the issue either side reported it as, and the default chosen comes
from the side that executed last. Nothing is asked. Nothing is said either: the
line naming what was settled belongs to the merge window, and that window opens
only when a question is left. That is difference 20. Two testers judging **different** test cases of one test run never
conflict at all: their run item statuses are
in different files.

**A test run two testers executed** — merged by rule, with nothing to answer:
the test run started when the earlier of the two says it started, ended when the
later says it ended, its status is the one further along, and its audit block
takes the later edit. What they each wrote - the configuration, the result
analysis - merges key by key, and only a key both of them changed differently is
a question.

**If the conflicted file is none of those** — a folder's marker other than a
test run's, or anything Testin did not write, is named in that same message and
left as it is. The tester settles those by hand, in files the IDE may not draw
as conflicted, because a Testin folder is not a version control root.

**If the repository has no remote** — the rebase is carried to the end and the
push is refused, reading *This repository has no remote, so there is nothing to
push to*.

**If a merged file cannot be written** — a message titled **Merge Failed** reads *Could not write*, the path, then the
reason.

**If a merged file Git will not take** — a message titled **Merge Not
Accepted** names the file and says what to do: resolve that file in Git, then
sync again. The sync cannot go on without it.

## Some things are settled without asking, and the dialog says which

The order of a test case takes the remote's value. Who changed it last, and
when, take the more recent of the two edits. Neither is a question a tester
can usefully answer about a merge - a position is not something either of them
chose, and who edited last is already in the two timestamps.

So they are not asked, and the dialog lists what was decided, one line and one
finished sentence for each:

> **Both changed these, and Testin settled them without asking:**
> Who changed it last and when, taken from the later edit
> The position in the test set, taken from the remote

Each entry carries its own reason, because the reasons differ: a test run's
marker settles its status, its execution stamps and every key only one tester
wrote, and none of those is settled by a rule about positions. The dialog adds
nothing to them - it used to end the sentence with one explanation for the lot,
which was true of a test case and of nothing else.

Only when it happened. A field one side never touched is settled by the ordinary
three-way rule, which is nobody's decision, and nothing is said about it.

Only when the window opens, too. A file the merge settles whole leaves nothing
to answer, so no window opens and the list is never shown. That is
difference 20.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
