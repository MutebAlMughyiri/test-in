[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-003

# UC-CODEGEN-003: Get a method for a test case that had none

**As a** tester, **I want** the method to appear once I have given a test case
its description, **so that** a test case I sketched without a name is not left
without automation forever.

Give a test case a description, and Testin writes the method it never had.

There is no button for this. It happens when the description is filled in.

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
- **Rule-CODEGEN-019** — Filling in a description on a test case that has no
  method writes the method, rather than reporting that there is nothing to
  change.
- **Rule-CODEGEN-020** — Changing a description on a test case that already has
  a method rewrites the method instead. That is
  [UC-CODEGEN-010](renameTestCase.md).
- **Rule-CODEGEN-021** — A description cleared back to nothing leaves the method
  under the name it already has, and records the empty description.

## What the tester sees

The dialog closes and a message reads *Updated*. That message is about the test
case, not about the code, and nothing says a method was written. The method is
in the class file, and it looks like this.

```java
@Test(description = "Card is declined",
      testName = "b81c0d2e-4a77-41f0-9a35-2d8e5f6c7a10",
      priority = 2)
public void cardIsDeclined() {
    // TODO: Auto-generated test steps for cardIsDeclined
}
```

## Main flow

1. A test case exists with no description, so it has no method.
2. The tester selects it, presses `F2`, then presses `D` for **Description**.
3. The tester types a description and presses `Enter`.
4. Testin looks for a method carrying this test case's identity.
5. There is none, so instead of stopping, Testin writes it.
6. The method appears with the description, the identity, the groups and the
   position.

## What Testin refuses

**If the description cannot name a Java method** — the dialog refuses it, and
the field turns red. The message names what the method would have been called.

**If the test case's place in the tree is too shallow to name a class** —
nothing happens at all, and nothing is written anywhere, not even the log.

**If the IDE has no Java plugin** — nothing is generated.

## Why this is worth knowing

There are two ways a missing method gets written. Filling in the description is
this one, and it happens by itself the moment the description is saved. Asking
for it outright is **Automate Test Case**, [UC-CODEGEN-005](automateTestCase.md).
That is the way for a test case that arrived with its description already set -
from a Git pull, a sync, an imported sheet or a branch switch. Such a test case
never had a description edit for this to hang on.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
