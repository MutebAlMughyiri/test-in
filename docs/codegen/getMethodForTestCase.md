[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-002

# UC-CODEGEN-002: Get a method when I create a test case

**As a** tester, **I want** a test method to appear when I write a test case, **so that** I can fill in what it does and
run it, without writing the
declaration myself.

A test case becomes a Java test method. The tester fills in what it does.

There is no key for this. It happens when a test case is created, which is
[UC-EDITOR-PANEL-005](../editorPanel/createTestCase.md).

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
- **Rule-CODEGEN-012** — The method name is the description with everything but
  letters and digits removed, the first word lowercase and later words
  capitalized.
- **Rule-CODEGEN-013** — `testName` carries the test case's identity. That is
  what finds the method afterward.
- **Rule-CODEGEN-014** — `priority` carries the test case's position in its test
  set, counting from one. TestNG runs methods in that order. It is not the test
  case's own High, Medium or Low, which is a different thing and writes nothing
  into the code.
- **Rule-CODEGEN-016** — Two descriptions that differ only in punctuation or
  capitals are one method.
- **Rule-CODEGEN-018** — A whole sheet of test cases is one undo. Its code is
  written in steps of 200 test cases, so the IDE stays responsive, and Ctrl+Z
  takes the whole sheet's code back at once.
- **Rule-CODEGEN-068** — Undoing a change to a test case writes the test
  case's code again, in every part Testin owns: the description, the method
  name, the groups, whether it is enabled, and where it sits. A test case with
  no method is passed over, and none is created.
- **Rule-CODEGEN-077** — A test case cut from one test set and pasted into
  another takes its method with it, body and all: the method is carried into the
  destination class as it is written, where the classes its body uses still
  resolve, and removed from the one it came from, and the methods of both test
  sets are numbered again (Rule-CODEGEN-014). Not
  written fresh - what makes the move worth making is the automation the tester
  wrote. A destination test set with no class gets one, written with every
  package folder on its path; a class that could not be written leaves the method
  where it is, because a method in the wrong class can be found and one that was
  deleted cannot.
- **Rule-CODEGEN-078** — A test case copied into a test set gets a method of its
  own, carrying the body of the method it was copied from. Only the body comes
  across: the copy's method has the copy's own UUID, name and attributes, because
  it is a different test case. A copy of a test case that has no method gets
  the empty one a new test case gets.
- **Rule-CODEGEN-095** — A test case with no method of its own adopts a @Test
  the tester wrote by hand when that method carries no test case identity and
  its description is the test case's description. It is tagged with the identity
  and kept as written, rather than a stub being written beside it.

## What is written

The method is described on
[the automation code page](main.md#what-testin-writes). The body holds one
comment and nothing else.

## What the tester sees

The create dialog closes and a message reads *Created*. Nothing on screen
mentions the code. The method is simply in the class file, beside the ones
already there. A test case described as *Log in with a valid user* becomes this.

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      groups = {"Regression"},
      priority = 1)
public void logInWithAValidUser() {
    // TODO: Auto-generated test steps for logInWithAValidUser
}
```

## Main flow

1. The tester creates a test case with a description.
2. Testin groups the new test cases by the class they belong to.
3. Testin finds the class, or writes it first.
4. Testin adds the TestNG import if the class has not got it.
5. Testin writes every method in one change, just before the closing brace,
   or at the end of the class while a half-typed class has none.
6. Testin formats what it wrote, and nothing else.

## What Testin refuses

**If the description is empty** — no method is written. Nothing is said, and
only the log records it. The method appears later, when the description is
filled in. That is [UC-CODEGEN-003](getMissingMethod.md).

**If the description cannot name a Java method** — the create dialog refuses
it before anything is written. The field turns red. A message titled **That
description cannot name a test method** says what the method would have been
called.

**If the class already holds a method with that name** — no method is written
for the second test case. A message titled *A test case has no automation
method* names it and says it would be named after a method the class already
has. Typing such a description is refused before the test case is created, and
so is editing one into it, so what reaches here comes from an import, a paste,
a Git merge or a sync.

**If the code project has no Java test source folder** — a message titled **No
Java Test Source Root** appears, and the test case is still created.

**If the IDE has no Java plugin** — nothing is generated, and a message appears
once for the whole code project.

## Where the plugin breaks its own rules

**Two test cases can still arrive sharing one method.** Two descriptions that
differ only in punctuation give one method name, and the second test case is
left with no method of its own — it cannot be run and cannot be jumped to.
Creating one is refused, by the dialog, and so is editing one into it, before
anything is stored. An import, a paste, a Git merge and a sync are not checked,
and generating then says which test case got no method and why. That is
difference 3 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
