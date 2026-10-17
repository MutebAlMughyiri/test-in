[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-001

# UC-CODEGEN-001: Get a class when I create a test set

**As a** tester, **I want** an empty Java class to appear when I make a test
set, **so that** the test cases I write next have somewhere to be written.

A test set becomes a Java class. The class is where its test methods go.

There is no key for this. It happens when a test set is created, which is
[UC-TREE-PANEL-007](../treePanel/createTestSet.md).

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
- **Rule-CODEGEN-007** — The class is named after the test set, with everything
  but letters and digits removed, and always ends in `Test`.
- **Rule-CODEGEN-008** — Each folder above the test set becomes a package in
  Java's own style: every word in lowercase, joined with nothing between, so
  *Checkout* is `checkout` and *Home Screen* is `homescreen`.
- **Rule-CODEGEN-009** — A class that is already there is never written over.
- **Rule-CODEGEN-010** — The class is written empty. It holds no blank line
  inside its braces.
- **Rule-CODEGEN-011** — A test set whose name comes to nothing when the illegal
  characters are removed is still named after what it was called. So two of them
  never share a class, and each one is in the same class every time. A test set
  with no name at all — which the tree does not let a tester make — becomes
  `DefaultTest`.
- **Rule-CODEGEN-073** — A folder whose name is a word Java keeps for itself -
  New, Class, Import - still becomes a package, named after what it was called,
  so nothing generated under it lands in a file that will not compile.

## What is written

```java
package demo.accounts;

public class LoginTest {
}
```

1. **The package line** — built from the folders above the test set.
2. **The class name** — the test set's name, cleaned, with `Test` on the end.

## What the tester sees

No screen opens, and Testin says nothing at all. The new Java file appears in
the Project tool window, inside the folder that holds the Java tests. Only a
problem speaks. A missing Java test source folder is a notification that stays
in the IDE's Notifications log, said once for the code project. A missing Java
plugin is a small red message near the bottom right that fades after about five
seconds. Everything else goes to the log alone.

## Main flow

1. The tester creates a test set named **Login** under a package named **Accounts**.
2. Testin works out the class name and the package from the tree path.
3. Testin makes the package folder under the test source folder.
4. Testin writes the class file.

## What Testin refuses

**If the code project has no Java test source folder** — a message titled **No
Java Test Source Root** reads *This project has no Java test source folder, so
creating test class was skipped. Test cases and test runs are read and written
without one - only the automation code needs it.* It is said once for the code
project. The test set is still created.

**If there is no name to build a class from** — nothing is written, and only the
log says so.

**If the class file is already there** — nothing is written and nothing is said.
The tester's own code is never overwritten.

**If the package folder cannot be made** — nothing is written, and only the log
says so.

**If the file cannot be written** — the same. Only the log says so.

**If the IDE has no Java plugin** — a message titled **Java Plugin Not
Available** appears once for the whole code project. Nothing is generated after
that, and nothing more is said.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
