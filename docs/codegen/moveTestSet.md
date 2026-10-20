[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-016

# UC-CODEGEN-016: Move a test set

**As a** tester, **I want** the Java class to move with the test set, **so that** the package the class is in still
matches where the test set sits.

Move a test set, and its Java class moves to the matching folder.

There is no key for this. It happens when a test set is moved, which is
[UC-TREE-PANEL-013](../treePanel/moveNodes.md).

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
- **Rule-CODEGEN-053** — The class file is moved, and its package line is
  rewritten to match where it landed.
- **Rule-CODEGEN-054** — A test set dropped where it already is, is not a move,
  and nothing is rewritten.
- **Rule-CODEGEN-055** — A move into a place Testin has not read leaves the
  class exactly where it is, and says so. The notification names the class and
  stays, because the tree and the code disagree until the tester does something
  about it.

## What the tester sees

The node lands in its new place, and a message reads *Moved*, with a count when
more than one node moved. Nothing on screen mentions the code. In the Project
tool window, the Java file now sits under the folder that matches the new place,
and the `package` line at the top of the file has been rewritten.

## Main flow

1. The tester drags a test set into another package.
2. Testin moves the class file into the matching package folder, before the tree
   changes.
3. Testin rewrites the package line at the top of the file.
4. Testin then moves the test set itself.

## What Testin refuses

**If the place it is moving to has not been read by Testin** — the class is left
where it is. The test set moves, the code does not, and a notification that
stays, titled *The automation code did not move with* and the test set's name,
names the class and says what to do (Rule-CODEGEN-055).

**If the class cannot be found** — nothing is moved, and only the log says so.

**If the code project has no test source folder** — nothing is moved, and
nothing is said.

**If the IDE has no Java plugin** — nothing is moved.

## Where the plugin breaks its own rules

**A move Testin declines leaves the tree and the code disagreeing.** The test
set is in one place. Its class is in another. Nothing on screen says so. That is
difference 6 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
