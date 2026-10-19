[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-011

# UC-CODEGEN-011: Reorder the test cases in a test set

**As a** tester, **I want** the automation to run in the order I put the test
cases in, **so that** a test case that signs in comes before the one that signs
out.

The order in the editor becomes the order the automation runs in.

There is no key for this. It happens when test cases are reordered, which is
[UC-EDITOR-PANEL-010](../editorPanel/dragToReorder.md).

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
- **Rule-CODEGEN-042** — Reordering rewrites the position on **every** test case
  in the test set, not only the one that moved.
- **Rule-CODEGEN-043** — The position counts from one, and is written into the
  annotation's `priority`.
- **Rule-CODEGEN-044** — A test case with no method is skipped without a word,
  because the sweep touches every test case in the set.
- **Rule-CODEGEN-067** — The generated methods are put in the same order as the
  test cases, so a class read top to bottom is the test set read top to bottom.
  Only the methods Testin wrote are moved, and one already in place is left
  where it is.

## What the tester sees

The cards move, and the numbers beside them change. A message reads *Re-sorted*,
with a count when more than one card moved. Nothing on screen mentions the code.
In the class file, every method's `priority` is written again, and the methods
are put in the new order.

```java
@Test(description = "Card has expired",
      testName = "7c2e91af-33b0-4d81-88ec-5f0a2b6c4d19",
      priority = 1)
public void cardHasExpired() {
}

@Test(description = "Card is declined",
      testName = "b81c0d2e-4a77-41f0-9a35-2d8e5f6c7a10",
      priority = 2)
public void cardIsDeclined() {
}
```

## Main flow

1. The tester drags a test case to the top of its test set.
2. Testin rewrites the order of the test cases themselves.
3. Testin then walks every test case in that test set, in one pass.
4. For each one that has a method, Testin writes its new position into
   `priority` and moves the method after the one before it.
5. A test run of that whole test set now executes in the tester's order, and the
   class reads in it.

## What Testin refuses

**If a test case has no method** — it is skipped, and nothing is said. This is
on purpose. The sweep touches every test case in the set. Reporting each one
without a method would mean a message for every test case nobody has written
yet.

**If a method has no annotation** — it is skipped, and only the log says so.

**If the IDE has no Java plugin** — nothing is rewritten.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
