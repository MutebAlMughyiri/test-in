[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-004

# UC-INTERNAL-004: Give a test case its number

**As a** tester, **I want** every test case to carry the same number wherever I
read it, **so that** the number in my bug report means one test case, not a row
in whatever list I was looking at.

The number is the test case's place in its test set, counting from one.

There is no key for this. Dragging a test case into place is on the editor
panel.

## Rules

- **Rule-INTERNAL-025** — A test case's number is its position in its test set,
  counting from one.
- **Rule-INTERNAL-026** — The number is worked out when it is drawn, and stored
  nowhere.
- **Rule-INTERNAL-027** — One thing works the number out, so the card, the row
  and the generated test method always agree.
- **Rule-INTERNAL-028** — A filter never renumbers. Hiding rows does not change
  the number on the rows that remain.
- **Rule-INTERNAL-029** — Each test case carries its own place in the order.
  Testin keeps it inside that test case's own file.
- **Rule-INTERNAL-030** — A test case with no place yet sorts after every test
  case that has one. Among those, the oldest comes first.
- **Rule-INTERNAL-031** — Moving one test case writes only the test cases that
  actually moved. Dropping one into a test set of 200 writes one file. A test
  case the test set has never held is always written, whether its place moved or
  not. Putting a test set in order never takes a test case out of it: one the new
  order leaves out stays in the test set.
- **Rule-INTERNAL-032** — A place between any two places always exists, so a
  test case can always be dropped between two others.
- **Rule-INTERNAL-033** — A save that would leave the file exactly as it is
  writes nothing. Testin does not record the tester as having edited it.
- **Rule-INTERNAL-034** — A test case is known by its file name. Testin decides
  whether a save is a new test case or a change by whether it already knows that
  name.
- **Rule-INTERNAL-035** — An import, an undo, a move and a removed test case put
  back from the review write the file without stamping it. An import keeps the
  audit the file brought. An undo puts back the audit the test case had before.
  A move keeps who created the test case. A test case put back from the review
  keeps the audit it was committed with.

## The screens

The test set before the tester drags anything.

```
┌──────────────────────────────────────────────────────────────┐
│  Payment                                                     │
│                                                              │
│   1   Card is declined                                       │
│   2   Card has expired          the tester drags this to top │
│   3   Card is accepted                                       │
└──────────────────────────────────────────────────────────────┘
```

The same test set afterward.

```
┌──────────────────────────────────────────────────────────────┐
│  Payment                                                     │
│                                                              │
│   1   Card has expired            only this file was written │
│   2   Card is declined                                       │
│   3   Card is accepted                                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The numbers** — every one of them changed. None of them was saved.
2. **The file that was written** — one, the test case that moved.

## Main flow

1. The tester drags a test case to a new place in its test set.
2. Testin works out a place for it, between the two test cases it landed
   between.
3. Testin writes that one test case's file.
4. Testin lists the test set again. Test cases with a place come first, in that
   order. Test cases without one come last, oldest first.
5. Every number on screen is worked out again from the new list.
6. The card, the grid row and the generated test method all read the same
   number.

## What Testin refuses

**If the tester opens a field, reads it and presses `Enter`** — nothing is
written. Testin compares the file it would write with the file already on disk.
They are the same, so it stops. The tester is not recorded as having edited the
test case. The test set is not recorded as having changed either.

**If the file cannot be written** — a read-only or locked file, a full disk —
Testin says so, and treats the save as one that did not happen. The index keeps
the test case as it was on disk, and the test set is not recorded as changed. No
undo is recorded, and the generated method is not rewritten, so nothing goes on
claiming an edit the file does not hold (Rule-INTERNAL-033).

**If a test case has no place in the order** — it sorts last, with the other
test cases that have none, oldest first. Something that has just arrived lands
at the end, where a tester looks for it.

**If a test case's place is lost** — the same. Nothing becomes unsorted. What is
in the folder is on the screen, in the order the places give.

**If a tester copies a test case file by hand under a new UUID** — it becomes a
second, separate test case. The file name is the identity, so both are read and
both get a number.

**If a test case file is not named by a UUID** — `login.testcase`, written by
hand — it is not read, and a warning names it (Rule-INTERNAL-012). Testin does
not make up a UUID for it: a new one at every Refresh would make it a new test
case each time.

**If two testers add a test case at the same time** — both are kept. There is
no shared counter for them to fight over, and adding one never rewrites its
neighbor. If the two land in the same place, the older one is drawn first.

## Why it works this way

Three separate places used to work the number out. A filter renumbered the
cards from one, so a test set of 40 test cases read as a test set of 12. One
place owns the number now, and Rule-INTERNAL-027 is the whole reason it
exists.

---

[Documentation](../README.md) › [Inside Testin](main.md)
