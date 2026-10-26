[Documentation](../README.md) › Inside Testin

# Inside Testin

Two things in Testin belong to no panel. The search opens from anywhere in the
IDE. The one part that reads and writes the files on disk sits under every
panel, so everything a tester reads has come through it.

A tester meets both without going looking for them. That is why they are written
up here rather than left to the code.

|                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The parts that belong to no panel                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **Answers**         | What the search finds, and what Testin does with the files under the Testin folder                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Numbering**       | Use cases are `UC-INTERNAL-001` to `UC-INTERNAL-009`. Rules are `Rule-INTERNAL-001` to `Rule-INTERNAL-146`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **Retired**         | `UC-INTERNAL-008` said a test project in an older file format is converted once, before it is read, and `Rule-INTERNAL-092` said the conversion reports what it did. Both retired 23 September 2026 (#333), when 2.14.0-alpha stopped carrying the code that knows any format before 2.13.0-alpha: an older project is refused and told which release converts it, which is Rule-INTERNAL-091 on [UC-INTERNAL-002](readTestProject.md). The numbers are not given to anything else. `Rule-INTERNAL-086` said the captions down the left of one dialog share one width, widened to the longest caption so none is cut off. Retired 18 September 2026, when each caption moved onto its own line above its field (#328) and there was no column left to widen; read Rule-INTERNAL-087 instead. `Rule-INTERNAL-088` said `testin.yml` is read by one class and written by none. Retired 19 September 2026, when **Save to testin.yml** became its one writer (#335); read Rule-INTERNAL-089 instead. `Rule-INTERNAL-084` said a test case read from a file named by hand is filed under its UUID the first time it is saved. Retired 14 October 2026 (#432), when a test case file stopped writing its UUID, so a file named by hand has no UUID to be filed under; read Rule-INTERNAL-012 instead. The numbers are not given to anything else |
| **State**           | **Written**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Checked against** | `main` at `31ad4b86a`, 8 October 2026, for this page's use case table, which no longer copies each page's rules. `main` at `1270e599`, 20 September 2026, for the file-touching parts, difference 6, [UC-INTERNAL-002](readTestProject.md), [UC-INTERNAL-003](noticeOutsideChange.md) and [UC-INTERNAL-007](dialogFramework.md). [UC-INTERNAL-001](globalSearch.md), [UC-INTERNAL-004](numberTestCase.md), [UC-INTERNAL-005](keepRemovedNode.md) and [UC-INTERNAL-006](countNodeContents.md) were last read at `e6277ddf`, 14 September 2026                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

---

## The use cases

|                     | What the tester does                                            | Why a tester uses it                              |
|---------------------|-----------------------------------------------------------------|---------------------------------------------------|
| **UC-INTERNAL-001** | [Find anything in the test project](globalSearch.md)            | Reach one test case without scrolling a big tree. |
| **UC-INTERNAL-002** | [Read a test project from disk](readTestProject.md)             | Every panel answers at once, with no wait.        |
| **UC-INTERNAL-003** | [Pick up a change made outside the IDE](noticeOutsideChange.md) | See what a pull or a teammate changed.            |
| **UC-INTERNAL-004** | [Give a test case its number](numberTestCase.md)                | Quote one number that means the same everywhere.  |
| **UC-INTERNAL-005** | [Keep a removed node so it can come back](keepRemovedNode.md)   | Get back what a wrong Delete took away.           |
| **UC-INTERNAL-006** | [Count what a node holds](countNodeContents.md)                 | See what a node holds before removing it.         |
| **UC-INTERNAL-007** | [Answer any Testin dialog](dialogFramework.md)                  | Learn one dialog and know them all.               |
| **UC-INTERNAL-009** | [Get help where Testin needs it](testinHelp.md)                 | Fix what is missing without asking anyone.        |

Beside the use cases, one reference page: [What Testin draws itself](customizations.md) - every color, icon, border,
painting, popup and size the plugin builds instead of taking the platform's, and which of them the platform already
ships.


---

## What this part is for

Testin keeps a whole test project in memory. Every panel, every dialog and every
report is a question answered from memory, not a trip to disk. That is why a
tree of 2,770 test cases opens at once.

Memory that is wrong is worse than disk that is slow. So most of this part is
about keeping the two the same: reading the files once at the start, noticing
when something changes them, and making sure only one thing is ever writing.

## The words it uses

- **The Testin folder** is the one folder on this machine that holds test
  projects. It is set once, on the settings page.
- **A marker file** is a small file Testin writes beside a node, saying what the
  node is. A test project holds one named `.testproject`. A test set holds one named
  `.testset`. Testin uses seven of them.
- **To read** a test project is to walk its folders once and hold everything it
  holds in memory.
- **A kept copy** is the copy Testin puts aside when it removes something, so
  `Ctrl+Z` has something to put back.

## Every key, in one place

| Key                                | What it does                           | The page that owns it              |
|------------------------------------|----------------------------------------|------------------------------------|
| `Ctrl+Alt+F`, `Cmd+Alt+F` on a Mac | Opens search, from anywhere in the IDE | [UC-INTERNAL-001](globalSearch.md) |

Nothing else in this part has a key of its own. It runs when a panel asks it to.

---

## Why it is built this way

**One place owns every file.** No other part of Testin reads or writes test
data. Everything else holds what that one place gave it. The reason is that a
second writer cannot be kept honest: two of them once wrote the same test run,
the older write landed second, and the tester found their failure analysis gone
after the next reload.

Eight other parts of Testin do touch files, and none of them touches test data.
They write generated source code, the code repository's own settings file, the
Git working folder, files chosen outside the Testin folder, generated reports,
the settings this machine keeps, the folder a bug report is sent from, and the
log.

**A file is claimed before it is written, not after.** Testin tells itself which
file it is about to write, then writes it. The other order looks the same and is
not: the change can be noticed while the file is still being written, and a file
claimed a moment too late looks like somebody else's edit.

**Nothing empty is ever written.** Writing nothing over a marker file empties
it, and an empty marker takes its node's history with it. Six markers in a real
test project were left at zero bytes that way before the rule was added.

**A node is forgotten only after the file is actually gone.** A file the IDE
refused to delete was once dropped from memory anyway, and the tree stopped
showing a node that was still on disk.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

|                  | The rule it breaks                                                                                                                                                                                                                                                                                                                                                                                                                                              | What a tester sees                                                                                                                                                                                                                                                                                                                                                                     |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 1** | Fixed. Both counts were right and answered different questions - a container is the sum of everything beneath it, and a new test run leaves retired branches out - so the count now names the other one when they differ: *40 (31 for a new test run)*. The second number comes from the very method the test run form walks, not from a rule about retirement written twice (Rule-INTERNAL-065). [#274](https://github.com/MutebAlMughyiri/test-in/issues/274) |                                                                                                                                                                                                                                                                                                                                                                                        |
| **Difference 2** | Rule-INTERNAL-040 — one press puts back what one removal took                                                                                                                                                                                                                                                                                                                                                                                                   | Fixed. The undo answers whether the whole of it came back, and the press is confirmed only when it did — so a `Ctrl+Z` that could not restore a node says that and nothing else (Rule-INTERNAL-063).                                                                                                                                                                                   |
| **Difference 5** | Rule-INTERNAL-019 — Testin ignores its own writes for five seconds                                                                                                                                                                                                                                                                                                                                                                                              | Fixed. The five seconds still bound how long a claim is kept, but what decides is whether the file still says what Testin wrote (Rule-INTERNAL-064). A hand edit changes the content, so it is read again whenever it arrives. A write still in flight, and a delete or a rename, have no content to compare and keep the window as their whole answer.                                |
| **Difference 6** | Rule-INTERNAL-053 — every dialog has the same three parts                                                                                                                                                                                                                                                                                                                                                                                                       | Thirty-four dialogs are built on the shell and three surfaces are not: light mode's zoom indicator, the button that picks which details a view shows, and the shortcut menu. Each behaves the same way by hand, so each is a copy that can drift. [#69](https://github.com/MutebAlMughyiri/test-in/issues/69)                                                                          |
| **Difference 7** | Rule-INTERNAL-069 — a default key the IDE also uses is noted beside its binding where both actions can fire in one place                                                                                                                                                                                                                                                                                                                                        | Fixed. The rule asked for no shared key at all while two were kept on purpose, so it now asks for a note beside the binding only where the IDE's action and Testin's can fire in the same place. `F12` (Jump to Last Window) and `Ctrl+Alt+F` (Introduce Field, in any Java file) are the two, and each carries one. [Every shortcut](../shortcuts.md) says what each competes against |
| **Difference 8** | Rule-INTERNAL-126 — the mark is gray while nothing waits                                                                                                                                                                                                                                                                                                                                                                                                        | The mark is the IDE's own question mark, `AllIcons.General.QuestionDialog`, and the IDE draws it blue. So while nothing waits it is the one colored entry on the right of the status bar, where the rule asks for a quiet gray. It still turns red while a hint waits (Rule-INTERNAL-127). Found 13 October 2026 (#425)                                                                |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone             | Was                                                                                                                                                                            |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 4** | A damaged marker file left its node drawn with default values, and only the log said so. Fixed 7 September 2026, [#277](https://github.com/MutebAlMughyiri/test-in/issues/277) |
| **Difference 3** | Test cases in an unmarked folder were invisible everywhere, and nothing said so. Fixed 7 September 2026, [#276](https://github.com/MutebAlMughyiri/test-in/issues/276)         |

---

## Not decided

**Question 1** — Should Testin say when it skips an unmarked folder holding test
cases? It is silent today, so test cases can exist and be invisible. Saying so
on every read would be noise in a folder that is deliberately not a test set.

**Question 2** — Should a damaged marker file be shown to the tester? The node
appears with default values today, and only the log says why.

**Question 3** — Five seconds is how long Testin ignores its own writes. Nobody
has measured whether it is the right number, and nothing shows it to the tester.

---

[Documentation](../README.md) › **Inside Testin**
