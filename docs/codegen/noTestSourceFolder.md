[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-020

# UC-CODEGEN-020: Work in a project with no test source folder

**As a** tester, **I want** to know why no code is appearing, **so that** I can mark a folder as holding tests and have
it start working.

No folder in the code project is marked as holding tests, so no code is
written. Everything else works: the tree, both editors, the view panel, reports,
import and export all read and write test data without one.

Nothing starts this. It is the state of the code project, and Testin says
nothing about it until something would have written code.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — The body belongs to the tester. Testin writes the
  annotation, the declaration and one `// TODO` line, and never touches a body a
  tester has written. A body is the tester's as soon as it is anything but that
  one line: a statement, a comment, or the TODO deleted. An agent writes the
  body only where it is still that line, and only when the tester asked for it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-082** — Testin touches a test project's automation code only
  when `testin.yml` names that test project. Otherwise, nothing is generated,
  renamed, moved or removed, **Automate Test Case**, **Navigate to Test Method**
  and **Run Tests** are gray and say why, and no gutter icon or automated mark
  is shown. **Save to testin.yml**, in the Testin panel, turns code on.
- **Rule-CODEGEN-064** — Testin looks for the test source folder once when the
  code project opens, and remembers what it found.
- **Rule-CODEGEN-065** — Anything that would create code says so. Anything that
  only tidies up after a removal stays silent.
- **Rule-CODEGEN-066** — The first test source folder of the first module that
  has one is the one Testin uses.
- **Rule-CODEGEN-072** — Opening a code project says nothing about the test
  source folder. The first thing that would have written code puts a hint in
  Testin Help naming what was skipped; the hint waits, saying nothing more,
  until a test source folder is found.
- **Rule-CODEGEN-093** — A remembered test source folder that has since been
  deleted is forgotten, and the next thing that needs one looks again as though
  none had been found.

## The screen

The message the tester sees the first time something would have written code. It
stays in the Notifications log, because the work it interrupted was theirs.

```
┌──────────────────────────────────────────────────────────────┐
│  This project has no test source folder, so LoginTest.java   │
│  was not created. Test cases and test runs are read and      │
│  written without one - only the automation code needs it.    │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always these four words.
2. **What was skipped** — named, so the message is about the thing the tester
   just did rather than about the project in general.
3. **The second sentence** — what still works, which is everything a tester came
   for.
4. **Once for the project** — fifty saved test cases are one absent source
   folder, not fifty messages.

## Main flow

1. The tester opens a code project with no folder marked as holding Java tests.
2. **Nothing is said.** Testin does not look, and does not ask.
3. The tester writes test cases, reads test runs, exports, imports and reports.
   None of it needs the folder.
4. The tester saves a test case whose description would name a method.
5. Testin looks for the folder, finds none, and puts a hint in Testin Help
   naming what was skipped. Nothing else is said.
6. The tester marks a folder as a test source folder in the IDE's own project
   settings.
7. Testin looks again the next time it needs the folder, finds it, and the hint
   clears.

## What Testin refuses

**The first creation** — the Testin Help mark turns red. Its hint reads *This
project has no test source folder, so* the file *was not created*, with the
guide *How to generate and run automation code*. The test set or test case is
still created.

**Every creation after it** — the hint names the latest class skipped, and
nothing else is said.

**Every removal, rename and move** — nothing at all is said. There was no code
to tidy up, and reporting that would be alarming for something that does not
matter.

## Why a missing folder is not a failure

Test management is the point of Testin. The generated code is a convenience on
top of it. A team that writes its automation somewhere else, or has not started
yet, should still be able to use every other part of the plugin. So a missing
folder is a skip with a message, never an error.

**And nothing to hear about until it matters.** Project open used to raise this
message before anything had been asked of the folder. So a tester who opened the
IDE to read test cases was told about automation they were not doing, on every
open, forever. Nothing about it was fixable from where they were standing.
The check bought nothing either: reading test data is gated on the Testin folder,
not on this one, and every generator already skips for itself ([#286](https://github.com/MutebAlMughyiri/test-in/issues/286)).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
