[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-005

# UC-CODEGEN-005: Ask for the method a test case never got

**As a** tester, **I want** to ask Testin for the test method a test case has not
got, **so that** test cases that arrived from somebody else are as runnable as
the ones I wrote here.

A test case written in the create dialog gets its method as it is saved. A test
case that arrives any other way does not, because nothing was asked for: a Git
pull, an imported sheet, a branch switch, a data folder
edited by hand. This is how to ask.

`F12`, or the menu entry **Automate Test Case**.

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
- **Rule-CODEGEN-071** — The entry is live where there is a method to write and
  gray with the reason where there is not, never left off the menu — a tester
  who cannot see it cannot learn it is there.
- **Rule-CODEGEN-025** — This writes the same method creating a test case
  writes: the annotation, the declaration and an empty body, in the class the
  test set belongs to, with that class and its folders written if they are not
  there. Whether a test case has a method is the question, not whether anything
  is written in it: a test case whose method is still the empty one gets nothing
  and is told so. Filling in what the method does is a different thing and is not
  this.

## What the tester sees

This opens no screen. Nothing on the list changes — the method is in the class
file, and the card's automation mark says so at the next redraw.

A small message appears at the bottom of the IDE and fades. It reads *Automated*, with a count after it for more than
one test case. The count is of
the methods that were written, not of the test cases asked for: a test case the
generator could not write a method for is left out of it, and one message from
the generator says why. When none was written, no *Automated* message appears at
all.

## Main flow

1. The tester selects the test cases.
2. The tester chooses **Automate Test Case**, or presses `F12`.
3. Testin passes over the test cases that already have a method.
4. Testin writes the class for any test set that has not got one, and every
   folder on its path with it.
5. Testin writes each remaining method into the class its test case belongs to.
6. A message reads *Automated 4*.

## What Testin refuses

**If nothing is selected** — the entry is gray, and says *Select a test case
first.* (Rule-EDITOR-PANEL-230).

**If every selected test case already has its method** — the entry is gray, and
says so: *Every one of these test cases already has its method. Automate writes
the method for a test case that has none.* Nothing is written, and nothing
claims to have been. A method the tester has not filled in yet still counts as
having one: the card's **Navigate to Test Method** icon is hollow because an
empty method is not automation, and there is still nothing here to write.

**If a test case has no description** — it is passed over, because a description
is what names a method (Rule-CODEGEN-002). Where that is true of every selected
test case the entry is gray and says so: *A test method is named after the test
case, so a test case needs a description before it can have one.* The method
appears when the description is filled in, which is
[UC-CODEGEN-003](getMissingMethod.md).

**If a description cannot name a Java method** — a message appears, titled *A
test case has no automation method*, or *\<how many\> test cases have no
automation method*. It names up to three of them and says to reword and
generate again.

**If another test case already answers to that name** — the second gets no
method. The same title appears, and the message says the name is taken and to
reword the description by a word.

**If the code project has no test source folder** — the Testin Help hint says so, and nothing is written.

**If the IDE has no Java plugin** — the entry is still on the menu, grayed,
reading *(needs the Java plugin)*.

**If testin.yml does not name the open test project** — the entry is still on
the menu, grayed, reading *Automate Test Case (Needs testin.yml)*. Hovering it
says *testin.yml does not name this test project. Save to testin.yml, in the
Testin panel, turns code on.* (Rule-CODEGEN-082)

**If the IDE is indexing** — nothing is written, and the tester is told once for
the whole gesture rather than once per test case.

## What this does not do

It does not write the steps. The body is one comment, exactly as it is for a
test case created here, and filling it in is the tester's — or a later feature's,
which is [#3](https://github.com/MutebAlMughyiri/test-in/issues/3). What this gives the
tester is the method their test case should have had, in the class it belongs
in, ready to be filled.

## The other way a missing method appears

Filling in a test case's description writes its method by itself. That is
[UC-CODEGEN-003](getMissingMethod.md), and it is the way for a test case being
written here. This entry is the way for a test case that arrived with its
description already set, and so never had an edit for that to hang on.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
