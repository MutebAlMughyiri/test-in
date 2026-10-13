[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-007

# UC-VIEW-PANEL-007: Read a test case's history

**As a** tester, **I want** to see what changed on a test case, who changed it
and when, **so that** I can tell whether a failure follows a change somebody
made.

The History tab reads the test case's history from Git. Each commit that changed
the test case's file is one entry. Testin stores no history of its own.

The test case's bugs are read from Git too. A bug is what a run item records
about a failure. Each time a test run recorded, changed or cleared a bug on
this test case, History shows a bug card of its own beside the test case's
entries. The bug card opens with what happened and in which test run, and the
test run's name opens that test run with the run item selected.

There is no key for this. The tab is called **History**.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has two tabs, Details and History, and
  both are drawn every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-091** — Beside a filed bug's link stands its state on
  GitHub: the Status column of the Project board it sits on, or else Open, Fixed
  or Not planned. A link whose state has not been read stands alone.
- **Rule-VIEW-PANEL-092** — Testin asks GitHub when a test run opens and on
  Refresh, for every bug the test project holds, one request per repository, in
  the background. The panel never waits for the answer, and the answer is never
  written to the test run.
- **Rule-VIEW-PANEL-093** — When GitHub cannot be asked - gh missing, signed out
  or offline - every link still shows and opens, and Testin Help holds one hint
  saying why until a read succeeds.
- **Rule-VIEW-PANEL-094** — A board's Status needs gh's read:project permission.
  Without it the issue's own state shows, and Testin Help holds one hint naming
  the command that grants it. Every read asks for the board again, so Refresh
  shows it once the permission is granted.
- **Rule-VIEW-PANEL-095** — A bug GitHub calls closed stays in the tab with its
  pill, so the tester sees the fix landed and knows to test it again.
- **Rule-VIEW-PANEL-096** — The History tab shows the test case's Git history on
  the current branch, newest first: one entry for each commit that changed its
  file. Each entry names when, who and the commit's message, then each field
  from what it was to what it became.
- **Rule-VIEW-PANEL-097** — A committed entry shows its commit's short hash, seven
  characters, and the full hash when the pointer rests on it.
- **Rule-VIEW-PANEL-098** — Edits saved but not committed head the list as one
  entry. It names Testin's last editor and time, and each field that differs
  from the last commit. It has no hash.
- **Rule-VIEW-PANEL-099** — The history is read from Git each time the tab shows
  a test case, and Testin stores none of it. It appears as soon as it is read,
  newest first. Only a read longer than 0.3 seconds shows a Reading line, and
  one test case's history is never drawn over another's.
- **Rule-VIEW-PANEL-100** — A test project that is not under Git has no history.
  The tab lists the bugs its test runs hold now, each as a bug card with no
  hash. Then it says in one line that it has no history, and names Initialize
  Git (git init) in View Pending Commits as the way to start one.
- **Rule-VIEW-PANEL-101** — The tab shows every commit that changed the test
  case. Git is read away from the screen, and the entries are drawn in groups,
  so the IDE never waits for a long history.
- **Rule-VIEW-PANEL-105** — A bug is what a run item records about a failure: a
  Failed run item status, or the link of the issue it was filed as. Each bug
  event in any test run is a bug card of its own, apart from the test case
  cards: recorded, changed, cleared, or its run item removed. It opens with the
  event, its test run, when, who and the commit, then shows the bug's severity
  and priority when its run item failed, and its filed issue when there is one.
  Severity and priority belong to the bug card, never to the test case card.
- **Rule-VIEW-PANEL-106** — The bugs are read from Git beside the test case's
  own changes, from its run item in every test run. A run item change that
  touches no bug is not shown.
- **Rule-VIEW-PANEL-107** — Bugs not committed yet are bug cards with no hash,
  at the top: each test run's run item as this machine holds it now,
  against the last commit.
- **Rule-VIEW-PANEL-108** — A cleared bug says why: its run item is no longer
  Failed and has no link, or its run item was removed from the test run.
- **Rule-VIEW-PANEL-109** — A bug card names its test run, and the name opens
  that test run with the run item selected. When the test run is no longer in
  the test project, its name is gray and its tooltip says so.
- **Rule-VIEW-PANEL-111** — A change on a card names its field by the field's
  icon, the one the update menu shows, with the field's name when the pointer
  rests on it. A field with no icon is named in words.
- **Rule-VIEW-PANEL-112** — The History tab reads the test case as it is now,
  whether the view panel was opened from a test set or from a test run. A
  Committed test run's run item shows its commit's text on the Details tab, and
  that text never makes a card that is not committed.
- **Rule-VIEW-PANEL-113** — The history of a deleted test case is read from Git
  like any other's. Its newest card is Removed: with no hash until the
  deletion is committed, then with the hash of the commit that deleted it.
- **Rule-VIEW-PANEL-115** — Every History card opens with a badge saying what
  happened: Created, Updated or Removed for a test case, Bug for a bug. A
  committed card adds a second badge, the commit's short hash; a card not
  committed yet has none.
- **Rule-VIEW-PANEL-116** — A test case's card is three lines at most: its two
  badges, then up to two of the fields that changed, one to a line: what it was,
  an arrow, then what it became. A line too long for the panel ends in an
  ellipsis, shows more as the panel widens, and shows its whole value on hover.
  When more fields changed, the last line ends with how many more. The cards
  alternate the editors' two row colors.
- **Rule-VIEW-PANEL-117** — Clicking a test case's card opens the IDE's diff
  window: the whole test case as it was beside it as it became, written as Copy
  as Text writes All Details. A bug card opens nothing; its test run's name is
  its link.
- **Rule-VIEW-PANEL-118** — A Steps change names only the steps that changed,
  each with its number. A step inserted or removed does not make the steps after
  it read as changed.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Details    | History |                                                   │
├────────────────────────────────────────────────────────────────────────────┤
│  [Updated]  Muteb  05-10-2026                                         (1)  │
│     Priority          Medium → High                                   (4)  │
│ ┃[Bug]  cycle 4  Muteb  05-10-2026                                    (5)  │
│ ┃   Bug Severity      Major                                                │
│ ┃   Bug Priority      High                                                 │
│ ┃   Bug Issue         #123  (In progress)                                  │
│  [Updated]  [4f1c9e2]  Sara  02-10-2026  Cycle 4 review               (2)  │
│     Expected Result   The dashboard opens → The dashboard opens within …   │
│     Steps             3 steps → 4 steps                                    │
│ ┃[Bug]  cycle 2  [4f1c9e2]  Sara  02-10-2026                          (6)  │
│ ┃   Bug Severity      Minor → Major                                        │
│ ┃[Bug]  cycle 1  [5e6594a]  mtb  01-10-2026                           (7)  │
│ ┃   Because           run item now Passed                                  │
│  [Created]  [a83d07e]  Muteb  24-09-2026  UC-10                       (3)  │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The badges** — every card opens with what happened: **Created**,
   **Updated** or **Removed** for a test case, **Bug** for a bug
   (Rule-VIEW-PANEL-115). A committed card then shows the commit's short hash;
   edits saved but not committed head the list with no hash.
2. **A committed entry** — the badges, who, when, and the commit's message. The
   full hash shows when the pointer rests on the short one. A commit whose
   version cannot be read, or that changed none of the fields, is Updated and
   says so in a line of its own.
3. **Created** — the commit that first added the test case.
4. **A field row** — the field, then what it was, an arrow and what it became,
   on one line. A test case's card shows at most two; when
   more changed, the last ends with how many more, and a value too long for
   the panel ends in an ellipsis, whole on hover (Rule-VIEW-PANEL-116). A
   Steps row names only the steps that changed (Rule-VIEW-PANEL-118). The field
   is its icon, the letter the update menu shows (**D** Description, **E**
   Expected Result, **S** Steps, **P** Priority, **G** Group, **M** Module,
   **T** Test Data, **B** Pre Conditions, **R** Reference, **U** Status), with its
   name on hover; the run item's fields have no icon and are named in words
   (Rule-VIEW-PANEL-111). A commit that changed none of the fields reads
   *No field changed*. Clicking a test case's card opens the IDE's diff
   window with every change (Rule-VIEW-PANEL-117).
5. **A bug card** — a card of its own, marked by a bar in the bug's severity
   color, or in red when its run item did not fail and only has a filed issue. It opens with **Bug** and the
   test run, then the commit's hash when it is committed, then who and when. Then the
   bug's severity and priority when its run item failed, and its filed issue
   with its state on GitHub when there is one. Clicking the test run's name opens that test run
   with the run item selected; a test run no longer in the test project is gray,
   and its tooltip says so.
6. **A changed bug** — each of the bug's own fields, from what it was to
   what it became. A run item change that touches no bug is not shown.
7. **A cleared bug** — the bar is green, and the card says why: the run item is
   no longer Failed and has no link, or it was removed from its test run.

A commit that changed the test case and a bug gives two cards, the test case's
first.

## Main flow

1. The tester shows a test case in the view panel and clicks **History**.
2. If the read takes longer than 0.3 seconds, the tab reads *Reading the
   history from Git...*.
3. Testin asks Git for the commits that changed the test case's file, newest
   first, and reads each version. Beside it, Testin asks Git for the commits
   that changed the test case's run item in any test run, and compares each
   version with the one before it.
4. The tab shows the entries. Paging to another test case reads its history the
   same way.

## What Testin refuses

**The test project is not under Git.** The tab shows a bug card with no hash
for each bug the test runs hold now, then one line: *This test project is
not under Git, so it has no history. Choose View Pending Commits, then
Initialize Git (git init), to start one.*

**A run item version cannot be read.** Its bug card is left out, and the rest of
the commit's cards show.

**Git cannot read the bugs.** The test case's own entries still show, and one
line under them says *The bugs could not be read from Git*, with Git's reason.

**A run item removed and not committed yet.** Its bug card names nobody and no
time, because Testin does not record who removed it.

**Git cannot be read.** The tab shows one line with Git's reason. The rest of
the panel works as before.

**A version cannot be read.** Its entry still shows when, who, the hash and the
message, and says *This version could not be read*. The entry after it has
nothing to compare with, so it says *What changed is not known, because the
version before it could not be read*.

**A long history.** Every commit is shown. The first entries appear at once,
and the rest follow while the tester reads.

The Details tab's **Updated** row still says who last changed the test case,
and when.

---

[Documentation](../README.md) › [The view panel](main.md)
