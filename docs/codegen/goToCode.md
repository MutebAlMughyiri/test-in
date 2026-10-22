[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-006

# UC-CODEGEN-006: Go to the code from a test case

**As a** tester, **I want** to open the method behind the test case I am looking
at, **so that** I can read or change what the automation really does.

This opens the Java method that runs the selected test case.

`Shift+F5` on the selected test case.

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
- **Rule-CODEGEN-027** — Without the Java plugin the class button on the card
  and on the view panel is drawn gray, and the menu entry stays, grayed, reading *(needs the Java plugin)*. Nothing is
  left out.
- **Rule-CODEGEN-119** — In a Robot Framework test project, Navigate to Test
  Method opens the test whose first tag is the test case UUID, at its name line.
  Testin looks in every `.robot` file of the code project, so a test a tester
  moved to another suite file by hand is still found.

## The three ways in

| The tester does this                | Where                                       |
|-------------------------------------|---------------------------------------------|
| Presses `Shift+F5`                  | The list of test cases in either editor     |
| Chooses **Navigate to Test Method** | The menu in either editor                   |
| Clicks the class button             | A card under the pointer, or the view panel |

## The screen

The Java file opens in the IDE's own editor, and the caret lands on the method
that runs this test case.

```
┌──────────────────────────────────────────────────────────────────────────┐
│     40      @Test(description = "Log in with a valid user",              │
│     41            testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",     │
│     42            priority = 3)                                          │
│     43      public void |logInWithAValidUser() {                         │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The caret** — drawn here as `|`. It lands on the method, not at the top of
   the file.
2. **The line numbers** — the file is scrolled to the method, so a long class
   does not have to be searched.

## Main flow

1. The tester selects a test case and presses `Shift+F5`.
2. Testin works out the class from the test case's place in the tree.
3. Testin finds the method carrying that test case's identity.
4. The Java file opens with the caret on that method.

## What Testin refuses

**If the test case has no method** — nothing opens, and Testin says *Not
automated*, the same message running it gives.

**If the IDE is still indexing** — a message reads *Waiting for indexing*, and
the jump happens when indexing finishes.

**If the class cannot be found** — nothing opens, and only the log says so.

**If the IDE has no Java plugin** — the menu entry is still there, grayed,
reading *(needs the Java plugin)*, and the class button is drawn gray. It does
not grow under the pointer, and it says the same sentence when it is hovered. If
the jump is reached anyway, a message titled **Java Plugin Not Available**
appears. It appears every time, not once.

**If testin.yml does not name the open test project** — the class button is
drawn gray, and hovering it says *testin.yml does not name this test project.
Save to testin.yml, in the Testin panel, turns code on.* The menu entry is not
grayed. It stays live, and pressing it says the same sentence and opens nothing.
That is difference 10 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
