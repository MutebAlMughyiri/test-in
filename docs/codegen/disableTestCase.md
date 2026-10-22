[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-013

# UC-CODEGEN-013: Turn a test case off

**As a** tester, **I want** a test case's method to stop running, **so that** a
test I know is broken does not fail every execution until I fix it.

Turn a test case off, and TestNG skips its method from then on.

There is no key for this. It happens when the test case's status is set to
disabled.

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
- **Rule-CODEGEN-047** — Turning a test case off writes `enabled = false` into
  its method's annotation.
- **Rule-CODEGEN-048** — Turning it back on takes the attribute off again,
  rather than writing `enabled = true`.

## What the tester sees

The card shows the new status and a message reads *Updated*. Nothing on screen
mentions the code. In the class file, one attribute has been added to the
annotation.

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      priority = 1,
      enabled = false)
public void logInWithAValidUser() {
}
```

## Main flow

1. The tester gives a test case the disabled status.
2. Testin finds the method by the test case's identity.
3. Testin writes `enabled = false` into the annotation.
4. TestNG skips the method from then on.
5. The tester sets the status back.
6. Testin takes the attribute off, leaving the annotation as it was.

## What Testin refuses

**If the test case has no method** — a message reads *Not automated*.

**If the method has no annotation** — nothing is written, and only the log says
so.

**If the IDE has no Java plugin** — nothing is written.

## Why the attribute is removed rather than set

An annotation carrying `enabled = true` says the same thing as an annotation
with no `enabled` at all. Leaving it out keeps the generated code short. It also
means a tester only ever sees the attribute when it matters.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
