[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-012

# UC-CODEGEN-012: Change a test case's groups

**As a** tester, **I want** the groups on the test method to follow the groups
on the test case, **so that** an execution of the smoke group runs what the tree
calls smoke.

The groups on the test case become the groups on the method.

There is no key for this. It happens when the groups are changed, which is
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
- **Rule-CODEGEN-045** — The groups are written into the annotation as a list,
  each exactly as the tester typed it.
- **Rule-CODEGEN-046** — A test case belonging to no group has no `groups` in
  its method at all.

## What the tester sees

The dialog closes and a message reads *Updated*. Nothing on screen mentions the
code. In the class file, the whole list after `groups` is written again, so a
group taken away goes as well as a group added.

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      groups = {"Regression", "Smoke"},
      priority = 1)
public void logInWithAValidUser() {
}
```

## Main flow

1. The tester adds the smoke group to a test case.
2. Testin finds the method by the test case's identity.
3. Testin rewrites the list of groups in the annotation.

## What Testin refuses

**If the test case has no method** — a message reads *Not automated*. Nothing
is written.

**If the method has no annotation** — nothing is written, and only the log says
so.

**If the IDE has no Java plugin** — nothing is written.

## What the tester sees elsewhere

In the code the groups are written as they read everywhere else in Testin, as
`"Regression"` and `"Smoke"`. TestNG compares group names exactly, so an
execution selecting `Regression` finds them and one selecting `REGRESSION` does
not.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
