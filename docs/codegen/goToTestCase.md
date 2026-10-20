[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-007

# UC-CODEGEN-007: See the test case from the code

**As a** tester, **I want** to see the test case behind a method I am looking at, **so that** I can read what the
automation is supposed to prove without leaving
the code I am reading.

The view panel opens on that test case, showing its details. Nothing else moves:
the tree stays where it was, no editor opens, and the caret stays on the method.

Going to the test case is one more click, and it is
[the third icon in that panel](../viewPanel/goToTestCase.md).

There is no key for this. The mark is in the gutter beside the method.

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
- **Rule-CODEGEN-028** — The mark is drawn beside the identity, on the right
  of the gutter: inside a Java method's annotation, or on a Robot test's
  `[Tags]` line.
- **Rule-CODEGEN-029** — The mark is drawn only for a test whose identity
  Testin recognizes: a TestNG test's `testName`, or the first tag of a Robot
  test (Rule-CODEGEN-118).
- **Rule-CODEGEN-030** — The mark is drawn while the IDE is still indexing, and
  the jump waits for indexing to finish.
- **Rule-CODEGEN-070** — The mark opens the view panel on the test case and
  stops there. It moves nothing else: not the tree, not the editor tabs, not the
  caret. A tester reading a method asked what it proves, and taking them out of
  the code to answer is a bigger answer than the question.
- **Rule-CODEGEN-069** — Clicking the mark of a method whose test case is gone
  says so, naming the method. Generated code outlives the test case it was
  written from, so this is an ordinary answer rather than a failure.
- **Rule-CODEGEN-118** — In a `.robot` file the mark is drawn on a test's
  `[Tags]` when its first tag is a test case UUID, whether Testin wrote the tag
  or a tester did. It needs no Robot plugin: it is drawn whether the file is
  opened by RobotCode, by another Robot plugin, or as plain text.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│     40      @Test(description = "Log in with a valid user",                │
│  [*] 41           testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",       │
│     42           priority = 3)                                             │
│     43      public void logInWithAValidUser() {                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The mark** — on the line holding the identity, not the line holding the
   method name. Its tooltip reads **View Test Case Details**.

In a Robot Framework suite file the mark sits on the test's `[Tags]` line, when
its first tag is a test case UUID:

```
┌────────────────────────────────────────────────────────────────────────────┐
│      2   Pay with a saved card                                             │
│  [*] 3       [Tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b    smoke        │
│      4       # TODO: Auto-generated test steps for Pay with a saved card   │
└────────────────────────────────────────────────────────────────────────────┘
```

2. **The Robot mark** — on the `[Tags]` line, with the same tooltip and the same
   click. A test the tester tagged by hand gets it too (Rule-CODEGEN-118).

## Main flow

1. The tester is reading a generated method.
2. The tester clicks the mark beside the identity.
3. Testin waits for indexing, then looks the test case up by its identity.
4. The view panel opens on that test case, with the Details tab in front.
5. Nothing else moves. The tester is still in the code, on the same line.

To go to the test case itself, click its identity in the panel that just
opened — its test set opens in an editor with the row selected, and the tree
still does not move (Rule-VIEW-PANEL-063).

## What Testin refuses

**If the identity is not one Testin recognizes** — no mark is drawn at all.

**If the method is not a TestNG test** — no mark is drawn.

**If a Robot test's first tag is not a test case UUID** — no mark is drawn on
its `[Tags]` (Rule-CODEGEN-118).

**If the test case behind the identity no longer exists** — nothing opens, and
the message names the method: *logInWithAValidUser was generated from a test
case that is gone*. It fades (Rule-CODEGEN-069).

**If anything else fails** — a message titled **Test Case Not Opened** reads *Could not find test case:* and then the
reason.

**If the IDE has no Java plugin** — no mark is drawn in Java code.

**If testin.yml does not name the open test project** — no mark is drawn (Rule-CODEGEN-082). The marks come back when
Testin reads the file again: after **Save to testin.yml**, after **Refresh** in the panel's title bar, and after
choosing or renaming the test project the panel shows.

## Where the plugin breaks its own rules

**A tester who edits the identity loses the mark**, with nothing saying why.
That is question 3.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
