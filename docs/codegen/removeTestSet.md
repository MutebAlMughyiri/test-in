[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-018

# UC-CODEGEN-018: Remove a test set or a package

**As a** tester, **I want** the class or the folder to go with the node I
removed, **so that** the code does not keep tests for a test set nobody has
anymore.

Remove a test set or a package, and its code goes to the recycle bin with it.

There is no key for this. It happens when the node is removed, which is
[UC-TREE-PANEL-012](../treePanel/removeNode.md).

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — The body belongs to the tester. Testin writes the
  annotation, the declaration and one `// TODO` line, and never touches a body a
  tester has written. An agent writes the body only where that `// TODO` still
  stands, and only when the tester asked for it.
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
- **Rule-CODEGEN-059** — Removing a test set sends its class file to the recycle
  bin. Removing a package sends its folder, with everything under it.
- **Rule-CODEGEN-060** — A removal that finds no code to delete says nothing at
  all.
- **Rule-CODEGEN-113** — Undoing the removal puts the code back as it was,
  method bodies included. Testin keeps the class file or the package folder
  while it removes it, and Ctrl+Z writes each file back where it stood. A file
  already standing there is left as it is.

## What the tester sees

The node goes from the tree, and a message reads *Removed*, with a count when
more than one node went. Nothing on screen mentions the code. In the Project
tool window, the class file is gone; for a package, the whole folder is gone
with everything under it.

## Main flow

1. The tester selects a test set and presses `Delete`.
2. The confirmation says what the test set holds.
3. The tester confirms.
4. Testin sends the class file to the recycle bin.
5. Testin removes the test set.

## What Testin refuses

**If the code project has no Java test source folder** — nothing is deleted, and
nothing is said. A removal that finds nothing to tidy up is not a failure.

**If the recycle bin does not take the class file or the folder** — it stays
where it is, and a notification titled *Not Removed* names it
(Rule-INTERNAL-143).

**If the IDE has no Java plugin** — nothing is deleted.

## What the tester should expect

Everything the tester wrote inside those methods goes with the class, into the
recycle bin. The confirmation before the removal counts test sets, test cases
and test runs. It does not mention the code.

Undoing the removal puts the test data back and the code as it was, method
bodies included (Rule-CODEGEN-113). The recycle bin keeps its own copy as well.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
