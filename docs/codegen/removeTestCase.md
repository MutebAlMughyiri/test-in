[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-014

# UC-CODEGEN-014: Remove a test case

**As a** tester, **I want** the generated method to go when I remove a test
case, **so that** the code does not fill up with tests for things that no longer
exist.

Remove a test case, and its method is deleted from the class.

There is no key for this. It happens when a test case is removed, which is
[UC-EDITOR-PANEL-011](../editorPanel/removeTestCases.md).

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
- **Rule-CODEGEN-049** — Removing a test case deletes its method from the class,
  and the methods of the test cases left are numbered again (Rule-CODEGEN-014).
  The class itself stays.
- **Rule-CODEGEN-050** — A test case with no method is skipped without a word.
- **Rule-CODEGEN-096** — A removal that deletes automation code - a test case, a
  test set, a package or a test project - waits for the IDE to finish indexing.
  It is refused while the IDE indexes, so no test case is removed with its
  method left behind.
- **Rule-CODEGEN-114** — Undoing the removal puts the method back as the tester
  left it, body included. Testin keeps the method's text while it deletes it,
  and Ctrl+Z writes that text back into the class; what Testin owns in the
  method is then written again (Rule-CODEGEN-068). Each removal keeps it again,
  so a redo and a second undo bring it back too. The kept text lasts until the
  project closes.

## What the tester sees

The card goes, and a message reads *Removed*, with a count when more than one
went. Nothing on screen mentions the code. The method is simply gone from the
class file, and the class stays with one fewer method in it.

## Main flow

1. The tester selects a test case and presses `Delete`.
2. The tester confirms.
3. Testin removes the test case.
4. Testin finds the method by that test case's identity and deletes it.
5. The class stays, with one fewer method.

## What Testin refuses

**If the test case has no method** — nothing is deleted and nothing is said.

**If the class cannot be found** — nothing is deleted, and only the log says so.

**If the IDE has no Java plugin** — nothing is deleted.

## What the tester should expect

Anything the tester wrote inside the method goes with it. Testin owns the
declaration, but deleting a method deletes the body too. Nothing warns about
that. The confirmation before the removal names the test case, not the code.

Undoing the removal puts the method back as the tester left it, body included.
Once the project is closed the removal can no longer be undone, but the method
is in the desktop recycle bin as a file of its own, named after its class and
itself, for example *LoginTest.logInWithAValidUser.java* (Rule-INTERNAL-143).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
