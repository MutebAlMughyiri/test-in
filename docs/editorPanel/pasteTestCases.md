[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-017

# UC-EDITOR-PANEL-017: Paste test cases

**As a** tester, **I want** to drop the test cases I copied or cut into this
test set, **so that** they end up where I am working.

This is the second half of a copy or a cut. It is the press that really moves
them.

the **Paste Test Case** entry on the right-click menu. It has no key of its
own.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, kept
  against the test set it is showing, and the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-081** — A pasted copy is a new test case with a new
  identity, and its description gains the word `(Copy)`.
- **Rule-EDITOR-PANEL-082** — A pasted cut keeps its identity and who created it,
  because it is the same test case in a new place. The tester who pasted it is
  recorded as the one who changed it.
- **Rule-EDITOR-PANEL-083** — Pasted test cases land under the test case the
  tester has selected, in the order they were pasted, so a paste arrives where
  the tester is looking rather than somewhere they have to scroll to. With
  nothing selected they land at the end.
- **Rule-EDITOR-PANEL-084** — A cut and its paste are one entry on the undo
  history.
- **Rule-EDITOR-PANEL-085** — The clipboard is read as test cases. Anything else
  is turned away.
- **Rule-EDITOR-PANEL-215** — Undoing a move puts the test cases back in the set
  they came from and nowhere else. A cut keeps each test case's identity, so
  the undo takes them out of where they landed before putting them back where
  they started - never the other way round, which would delete them from both.
- **Rule-EDITOR-PANEL-248** — A button that turns gray while the pointer is on
  it loses its hover look at once, so a control that has stopped working never
  looks ready to press.
- **Rule-EDITOR-PANEL-249** — A grid that held the keyboard when it was rebuilt
  holds it again afterward, so the next key lands in the grid rather than
  nowhere.
- **Rule-EDITOR-PANEL-250** — A grid row is drawn in its stripe, or in the
  selection color while it is selected, and in nothing else. The pointer passing
  over it changes nothing.
- **Rule-EDITOR-PANEL-251** — When the status bar is too narrow for everything,
  nothing on it gives way or is cut short: it scrolls sideways, as the toolbar
  does (Rule-EDITOR-PANEL-257), so the page size field can always be reached.
  Where there is room, the page arrows sit in the middle of the bar.
- **Rule-EDITOR-PANEL-252** — A badge's words are printed white or dark,
  whichever reads on its color, so a pale badge never carries white text.
- **Rule-EDITOR-PANEL-257** — An editor narrower than its toolbar or its status
  bar scrolls that bar sideways under the pointer, with the mouse wheel or a
  touchpad. No scrollbar is shown and the bar keeps its height, so nothing on it
  is cut off and the editor below does not move.
- **Rule-EDITOR-PANEL-255** — An undo of a move whose test set they came from is
  gone, or renamed since, is refused. The test cases stay where they landed and
  nothing is deleted from either set.

## What the tester sees

This opens no screen. The pasted test cases appear at the end of the list. On a
cut they also disappear from the test set they came from.

A small message appears at the bottom of the IDE and fades. It reads *Pasted*,
with a count after it for more than one test case.

## Main flow

1. The tester has copied or cut test cases.
2. The tester opens the test set they want them in.
3. The tester chooses **Paste Test Case** from the right-click menu.
4. On a cut, the test cases are taken out of the test set they came from first.
5. Each test case is written into this test set, under the selected one and in
   the order they were pasted (Rule-EDITOR-PANEL-083).
6. The test set's order is worked out again and saved.
7. On a cut, each test case takes its generated method with it: the method is
   carried into this test set's class exactly as it is written, body and all,
   and removed from the class it came from. It then takes its place in this test
   set's order, so an execution runs it where its card sits (Rule-CODEGEN-014).
8. On a copy, each test case gets a method of its own in this test set's class,
   because a copy is a new test case. That method starts with the body of the
   one it was copied from, which stays where it is. Only the body comes across;
   the identity, the name and the attributes are the copy's own.
9. A message reads *Pasted*, with a count for more than one.

## What Testin refuses

**If nothing is selected in the test set** — the test cases land at the end.
There is nothing to land under, and the end is where a tester looks for what
just arrived.

**If the clipboard does not hold test cases** — **Paste Test Case** is gray.

**If the clipboard holds text that is not test cases** — it is turned away
without being read, and nothing is said.

**If the clipboard holds test cases that will not read** — the entry stays gray,
and only the log says why.

**If the destination test set has no class** — it gets one. Writing a class
writes every folder on its path with it, so there is nothing to make first, and a
cut and a copy into that test set behave the same. Only a class that could not be
written leaves a cut method where it was, and only the log says so: a method in
the wrong class is something a tester can find and move by hand; one that was
deleted is not.

**If the copied test case has no method, or is no longer there** — the copy gets
the empty method a new test case gets, and only the log says why. There is
nothing to carry, and the copy is still a test case.

**If writing the pasted test cases fails** — a message reads *Could not save, so
the cards went back to what is on disk*, and the list is read again, without
them.

**If a cut test case cannot be written where it goes** — it stays in the test set
it was cut from and is not pasted. A message reads *unable to write content:* and
the reason. The rest of the cut still moves. When none of it could be written,
the cut keeps waiting, and the next paste tries again.

## Pasting into the same test set

Pasting a copy into the test set it came from is allowed. The result is a second
test case. Its description ends in `(Copy)`. It has its own identity and its own
test method, holding the same body as the one it was copied from - two methods
in one class doing the same thing, which is the start of varying one of them.

---

[Documentation](../README.md) › [The editor panel](main.md)
