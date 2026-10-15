[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-016

# UC-SHARE-016: Send my changes and take the team's

**As a** tester, **I want** one gesture that brings the team's test cases in and
sends mine out, **so that** I do not have to remember two.

A sync takes the team's changes first, then sends the tester's. One press does
both, so nothing is left behind.

There is no key for this. The menu entry is **Sync with Remote**.

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
- **Rule-SHARE-068** — The branch that is synced is the one that is checked out.
- **Rule-SHARE-069** — A sync pulls and then pushes, so a sync that succeeded
  never leaves the tester's commits only here.
- **Rule-SHARE-070** — The push is skipped when nothing is waiting, so a sync
  pressed out of habit costs no network.
- **Rule-SHARE-071** — The pull rebases, and stashes anything uncommitted first.
- **Rule-SHARE-072** — The progress bar cannot be canceled. A sync pulls with a
  rebase and then pushes, and neither can be stopped halfway without leaving the
  repository somewhere nobody asked for — which is Rule-SHARE-005 above, and
  this rule used to say the opposite of it.
- **Rule-SHARE-073** — Afterward the working folder, Testin's own reading of
  it, and the tree are all read again. That read has a progress bar of its
  own, so the Git step's bar ends, and says it is done, as soon as Git is.
- **Rule-SHARE-120** — A remote that has no branch of that name yet is pushed to
  without a pull first. There is nothing to pull, and a pull would fail the sync
  before the push was tried.
- **Rule-SHARE-122** — Where the repository has several remotes, the one named
  `origin` is used, and with no `origin` the first one Git lists. **Commit &
  Push** chooses the same way.
- **Rule-SHARE-125** — A remote that cannot be asked whether it has the branch
  stops the sync before anything is pulled or pushed, and the message gives the
  reason Git gave. Only a remote that answered, and answered that it has no
  branch of that name, is pushed to without a pull.
- **Rule-SHARE-127** — Every Git step that fails says what Git said and offers
  Show Git log. It opens the IDE's Git tool window, where Git's own output is
  kept. That holds for reading the changes, committing, choosing a branch,
  putting a folder under Git, setting the remote or the identity, Sync, the
  push, and every step of resolving conflicts.
- **Rule-SHARE-132** — While the project is in safe mode, Testin runs no Git
  command. Each Git step says so in one line, naming IntelliJ's Trust Project as
  the way to turn Git on, rather than failing with Git's own words.

## What the tester sees

No window opens. A progress bar named *Syncing with remote* runs in the status
bar, and its line changes as the sync works. At the end a message titled **Synced** appears at the bottom right and
stays in the IDE's notification
list.

## Main flow

1. The tester selects the test project and chooses **Sync with Remote**.
2. A bar titled *Syncing with remote* opens. It cannot be canceled: what
   follows writes to the repository and to the remote.
3. The bar reads *Checking remote configuration...*.
4. The bar reads *Pulling latest changes from*, then the branch.
5. If anything is waiting, the bar reads *Pushing what is committed here...*.
6. The bar reads *Refreshing files...*.
7. A message titled **Synced** reads *Up to date with the remote*, or *Pushed*
   and the count. A branch that had never been pushed reads *Pushed, and this
   branch now tracks the remote*.

## What Testin refuses

**If nothing that is a test project is selected** — the menu entry is gray.

**If the folder is not a Git repository** — a short message titled **Nothing
to Sync** names the folder, then reads *is not under Git yet. Open Pending
Commits to create the repository.* Testin Help holds **Initialize Git (git
init)** until the repository exists.

**If no remote address is set** — a short message titled **Sync Aborted** reads *No remote URL is configured for this
project. Push a commit first to configure
the remote.* Testin Help holds the same until the first push sets a remote.

**If Git cannot name a branch** — the sync fails, and the message says so.

**If the pull stops on a conflict** — the conflict offer appears instead of
anything else. That is [UC-SHARE-017](resolveConflicts.md).

**If the remote cannot be asked for its branches** — nothing is pulled or
pushed. A message titled **Sync Failed** reads *Could not ask* the remote *whether it has the branch*, the branch, *so
nothing was pulled or pushed:*,
and Git's reason on the next line.

**If anything else fails** — a message titled **Sync Failed** gives the reason.

**If the IDE has no Git plugin** — the menu entry is still there, grayed,
reading *(needs the Git plugin)*.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
