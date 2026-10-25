[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-003

# UC-INTERNAL-003: Pick up a change made outside the IDE

**As a** tester, **I want** Testin to notice when my test data changes on disk
without me, **so that** what I am reading is what the files say and not what
they said when the IDE started.

Testin watches the files. When one changes, it reads that test project again.

There is no key for this. It starts on its own. **Refresh** at the top of the
panel does the same job by hand, and is on
[UC-TREE-PANEL-025](../treePanel/refreshTree.md). It has no key either.

## Rules

- **Rule-INTERNAL-016** — Testin watches every file the IDE watches. A change
  counts only if the file is inside a test project Testin reads: a folder under
  the Testin folder with a `.testproject` marker, and only the one named for this code
  project when one is named. A changed folder that is not one is dropped from what Testin
  shows.
- **Rule-INTERNAL-017** — Anything inside a `.git` folder is ignored, however
  deep it is.
- **Rule-INTERNAL-018** — The Testin folder itself is not watched. It holds test
  projects, and is not one.
- **Rule-INTERNAL-019** — Testin ignores its own writes for five seconds after
  making them.
- **Rule-INTERNAL-064** — It ignores them because the file still says what
  Testin wrote, not because five seconds have passed. A file somebody changed by
  hand is read again whenever the change arrives, including inside those five
  seconds and including the file Testin has just saved.
- **Rule-INTERNAL-020** — Testin waits four tenths of a second after the last
  change before it reads. A pull that brings 40 files costs one read, not 40.
- **Rule-INTERNAL-021** — The whole test project is read again, never the one
  file that changed.
- **Rule-INTERNAL-022** — A change arriving while a read is running books the
  next read. Nothing is missed and nothing is read twice at once.
- **Rule-INTERNAL-023** — A code project whose Testin panel was never opened is
  left alone.
- **Rule-INTERNAL-024** — `testin.yml` is not watched. Only **Refresh**, and
  Report Bug before it sends, read it again.
- **Rule-INTERNAL-081** — A test project stays readable while it is read again.
  The pass reads into a copy and is put in when it is finished, so everything
  still on disk answers throughout, and a pass that is canceled or that fails
  changes nothing at all. What the pass did not find is dropped when it lands,
  which is how the read forgets what was deleted.
- **Rule-INTERNAL-113** — A file Testin writes or deletes is claimed as its own
  before the change starts, because the file system can report the change while
  it is still running.
- **Rule-INTERNAL-125** — A file Testin writes is replaced whole or not at all.
  It is written beside itself first and moved into place, so a write cut off
  part-way leaves the file as it was, and the file written beside it is claimed
  as Testin's own as well.
- **Rule-INTERNAL-145** — A test case, a test set, a package or a test run that
  Testin creates while a read of its test project is running is kept when the
  read lands, although the read never saw it, and a test case stays in its test
  set. The read forgets only what it did not find and Testin did not create
  after it began.

## What is picked up, and what is not

| The change                                                    | Picked up            |
|---------------------------------------------------------------|----------------------|
| A test case, a test set or a marker file edited by hand       | Yes                  |
| Files a pull brought in                                       | Yes                  |
| Files a branch switch changed                                 | Yes                  |
| Forty files at once                                           | Yes, as one read     |
| Anything inside a `.git` folder                               | No                   |
| The Testin folder itself                                      | No                   |
| Anything outside the Testin folder                            | No                   |
| Anything at all, when no Testin folder is set                 | No                   |
| What Testin itself just wrote, still saying what Testin wrote | No, for five seconds |
| A file Testin just wrote, edited by hand since                | Yes                  |
| A code project whose panel was never opened                   | No                   |
| `testin.yml`                                                  | No                   |

## The screen

One progress bar, whatever changed and however many test projects it touched.

```
┌──────────────────────────────────────────────────────────────┐
│  Reading test data that changed on disk                [ X ] │
│  ██████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  25% │
│  Checkout - test sets...                                     │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the same words every time. It does not name the file.
2. **The cancel button** — stops the read. The tree is still redrawn.
3. **The line underneath** — the same lines UC-INTERNAL-002 shows.

## Main flow

1. Something changes a file on disk. A pull, a branch switch, an editor outside
   the IDE, or another program.
2. Testin checks the path. It keeps the test project the file belongs to.
3. Testin waits four tenths of a second, for more changes that may be coming.
4. The progress bar opens with *Reading test data that changed on disk*.
5. Each changed test project is read again, exactly as UC-INTERNAL-002 reads it.
6. The tree redraws. Every open editor reloads.
7. The bar closes. Nothing else is said.

## What Testin refuses

**If the change is Testin's own write** — it is ignored for five seconds, as
long as the file still says what Testin wrote. A tester who edits the same file
by hand inside those five seconds changes what it says, so that change is read
again like any other (Rule-INTERNAL-064).

**If the change arrives while Testin's write is still running** — it is ignored
until the five seconds are up. There is nothing on disk yet to compare, so the
five seconds are the whole answer. The same holds for a file Testin deleted,
renamed or moved. **Refresh** recovers anything missed that way.

**If the tester cancels** — the read stops. The tree and the editors are still
redrawn. What was read is on screen. What was not read is still the old
reading, and nothing shows which rows are which.

**If the code project's panel was never opened** — nothing happens at all. The
change is on disk and Testin has not read it. It is picked up the first time the
panel is opened.

**If `testin.yml` changed** — nothing happens. It lives in the code repository,
outside the Testin folder, so Testin never sees it change. **Refresh** reads it
again.

## Why it works this way

A tester was told a sync had worked. The panel still showed the old files.
Nobody presses **Refresh** after being told the work is done. So Testin watches
the files instead.

Testin has to ignore its own writes. Without that, saving one test case would
rebuild the whole tree under the tester who saved it. That would be correct.
It would also be unusable.

---

[Documentation](../README.md) › [Inside Testin](main.md)
