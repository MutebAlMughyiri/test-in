[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-013

# UC-SHARE-013: Commit and push

**As a** tester, **I want** my work recorded and sent in one press, **so that** the team has it without a second gesture
I might forget.

A commit records the work here. A push sends it to the team's copy, which Git
calls the remote. This does both in one press.

There is no key for this. **Commit & Push** is the face of the split button.

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
- **Rule-SHARE-059** — The push happens only after the commit succeeded.
- **Rule-SHARE-060** — A repository with no remote takes the address
  `testin.yml` gives, when the file names this test project, says it is shared
  and gives a repository address (Rule-TREE-PANEL-117), and otherwise asks for
  one, once.
- **Rule-SHARE-061** — The message about a push stays in the IDE's notification
  list, because a push finishes on its own time.
- **Rule-SHARE-062** — Any password inside the remote address is taken out of
  anything Testin shows or logs.
- **Rule-SHARE-123** — **Commit & Push** pulls before it pushes, rebasing and
  stashing the way a sync does, so the tester's commits land on top of what the
  remote already holds. A remote with no branch of that name yet is pushed to
  without the pull; a remote that cannot be asked is not taken for one, and
  nothing is pushed.
- **Rule-SHARE-127** — Every Git step that fails says what Git said and offers
  Show Git log. It opens the IDE's Git tool window, where Git's own output is
  kept. That holds for reading the changes, committing, choosing a branch,
  putting a folder under Git, setting the remote or the identity, Sync, the
  push, and every step of resolving conflicts.

## What the tester sees

The button opens no window of its own. The dialog closes and a progress bar
named *Committing and pushing* runs in the status bar. When it ends, a message
titled **Pushed** appears at the bottom right. It waits in the IDE's
notification list, because a push can finish while the tester is reading
something else.

## Main flow

1. The tester ticks what to send, types a message, and presses **Commit &
   Push**.
2. Testin commits, exactly as
   [UC-SHARE-012](commitChanges.md) describes.
3. Testin reads the remote address.
4. Testin pushes the branch.
5. A message titled **Pushed** reads *Commit*, then the short identifier, then *is on*, then the remote and the branch.

## What Testin refuses

**If no remote is set** — when `testin.yml` names this test project, says
`location: remote` and gives a `repoUrl` that is a repository address, that
address becomes the remote and the push goes on. For any other test project, or
a `repoUrl` that is not an address, a window titled **Configure Remote** asks
for one. It
says the repository has nowhere to push to yet, names the remote the address
will be added as, and shows an example address in the empty box.

**If the box is left empty** — the window stays open and the box says so.

**If what was typed is not a repository address** — the window stays open and a
message reads the text, then *is not a repository address*. A repository's web
page pasted in place of its clone address is the usual way to meet this.

**If the tester closes that window** — nothing is said. They shut the window on
the question, and the push not happening is the answer to it. The commit was
already made.

**If the address cannot be added** — a message titled **Git Error** reads *Failed to add remote:* and then the reason.

**If the remote cannot be read** — a message titled **Git Error** reads *Could
not read the Git remote:* and then the reason.

**If the remote cannot be asked for its branches** — nothing is pulled or
pushed. A message titled **Push Failed** reads *Could not ask* the remote *whether it has the branch*, the branch, *so
nothing was pulled or pushed:*,
and Git's reason on the next line.

**If the push fails** — a message titled **Push Failed** carries the reason, with
a **Try Again** link and a **Show Git log** link (Rule-SHARE-127).

## Where the plugin breaks its own rules

**A commit made and not pushed leaves work on this machine.** If the push
fails, the commit still stands. The work is recorded here and not sent. The
next review offers to push it. That is [UC-SHARE-015](pushOldCommit.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
