[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-010

# UC-CODEGEN-010: Change a test case's description

**As a** tester, **I want** the generated method to follow when I reword a test
case, **so that** the code says the same thing the test case says.

Reword a test case, and Testin rewrites the method to match.

There is no key for this. It happens when the description is changed, which is
[UC-EDITOR-PANEL-006](../editorPanel/changeOneField.md).

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
- **Rule-CODEGEN-021** — A description cleared back to nothing leaves the method
  under the name it already has, and records the empty description.
- **Rule-CODEGEN-040** — Both the annotation's description and the method's name
  are rewritten.
- **Rule-CODEGEN-079** — A description that cannot name a Java method, or that
  names the same method as another test case in the class, leaves the method
  under the name it already has. The description is still saved, and a message
  says the method kept its name and why. Renaming it anyway threw an internal
  error for the first, and wrote two methods with one name for the second, so
  the whole class stopped compiling.

## What the tester sees

The dialog closes and a message reads *Updated*. Nothing on screen mentions the
code. Two things changed in the class file: the words after `description`, and
the name of the method. The identity in `testName` did not change, and neither
did anything the tester wrote inside the method.

```java
@Test(description = "Sign in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      priority = 3)
public void signInWithAValidUser() {
    // whatever the tester wrote here is left exactly as it was
}
```

## Main flow

1. The tester changes a test case's description.
2. Testin finds the method by the test case's identity.
3. Testin rewrites the description in the annotation.
4. Testin renames the method to match the new description.
5. Anything that called the method is updated too, by the IDE's own rename.

## What Testin refuses

**If the test case has no method yet** — the method is written instead. That is
[UC-CODEGEN-003](getMissingMethod.md).

**If the description cannot name a Java method** — the update dialog and the
bulk editor refuse it before anything is written. Typed into a grid cell, or put
back by an undo, the description is saved and the method keeps the name it had;
a message titled **The test method kept its name** says what it would have been
called and why it was not (Rule-CODEGEN-079).

**If another test case in the class already has that method** — the same: the
dialogs refuse it, and anywhere else the description is saved, the method keeps
its name, and the message says which name was taken.

**If the method has no annotation** — nothing is rewritten, and only the log
says so.

**If the test case's place in the tree is too shallow** — nothing happens at
all, and nothing is written anywhere, not even the log.

## What is not rewritten

The body of the method. Testin owns the annotation and the name. Everything
between the braces is the tester's.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
