[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-023

# UC-CODEGEN-023: Automate a test project in Robot Framework

**As a** tester on a team that writes Robot Framework, **I want** Testin to
write and keep a Robot test for every test case, **so that** our suites follow
the test project the way a Java team's classes do.

Testin reads which language a test project's code is written in from the code
project's interpreter: a Python one means Robot Framework. From then on every
code change on this part's pages writes Robot instead of Java: a test set is a
suite file, a test case is a test in it, and its first tag is the test case
UUID. Navigate to Test Method opens the test (UC-CODEGEN-006,
Rule-CODEGEN-119), Run Test Method runs it (UC-CODEGEN-008, Rule-CODEGEN-120),
and the mark in the gutter leads back (UC-CODEGEN-007, Rule-CODEGEN-118).

Nothing starts this, and nothing is written to say so. The interpreter a code
project already has decides.

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
- **Rule-CODEGEN-116** — A test project is automated in the language of the
  code project's interpreter: Robot Framework when it is a Python interpreter,
  Java when it is a JDK, another interpreter or none. Nothing is written to say
  so, and a tester never chooses it.
- **Rule-CODEGEN-117** — Each kind of work on code asks the language the test
  project is automated in. Work that language has no module for is gray on the
  action and says so, *Testin API Model from JSON (not available for Robot
  Framework yet)*, and the message reads *API Model is not available for Robot
  Framework yet.* A Robot test project needs no Java plugin, and a connected
  agent is not asked to write a Robot test's body, because its prompt writes
  Java.
- **Rule-CODEGEN-121** — A Robot test project's code lives in the code project's
  test source folder, as a Java one's does (Rule-CODEGEN-064). Each folder above
  the test set is a folder there, and the test set is a suite file, `<test
  set>.robot`, holding a `*** Test Cases ***` section. A space in a name becomes
  an underscore, so Robot shows the name the tree shows; a trailing
  `-test-cases` is dropped (Rule-CODEGEN-008); and a character a file name
  cannot hold is left out. *NAFATH › Checkout › Payment flow* is
  `NAFATH/Checkout/Payment_flow.robot`.
- **Rule-CODEGEN-122** — Each test case is one test in its suite file, named by
  its description. A run of spaces becomes one space, because two spaces end a
  cell in Robot, and a leading `#`, `.` or `[` is left out, because Robot reads
  a line starting with one as a comment, a continuation or a setting. A
  description that comes to nothing gets no test (Rule-CODEGEN-002), and
  clearing a description keeps the test's name (Rule-CODEGEN-021).
- **Rule-CODEGEN-123** — A Robot test's `[Tags]` hold the test case UUID first,
  then the test case's groups, one tag each (Rule-CODEGEN-045). A test the
  tester wrote with the test case's name and no UUID is adopted: the UUID is put
  first in its tags, and its steps stay (Rule-CODEGEN-095).
- **Rule-CODEGEN-124** — A new test holds two lines: `# TODO: Auto-generated
  test steps for <name>` and `No Operation`. Robot fails a test that holds only
  a comment, so the second line makes a new test pass until someone writes it,
  as a new Java method does. Those two lines are the line Testin wrote for
  Rule-CODEGEN-003: the body is the tester's as soon as it is anything else.
- **Rule-CODEGEN-125** — A disabled test case's test carries the tag
  `robot:exclude`, so Robot neither runs it nor reports it, as TestNG does with
  `enabled = false` (Rule-CODEGEN-047). Enabling the test case takes the tag
  away.
- **Rule-CODEGEN-126** — Robot runs a suite's tests in the order they are
  written, so a test set's order is the order of its tests in the suite file
  (Rule-CODEGEN-042). Reordering the test set moves Testin's tests and no
  others; no number is written.
- **Rule-CODEGEN-127** — Testin writes into a suite file only what it owns: a
  test's name line, its `[Tags]` line, and a new test's two lines. Every other
  line, setting, keyword and section is the tester's and stays as typed. A suite
  file written in Robot's pipe-separated form is left as it is, and the log
  names it.
- **Rule-CODEGEN-128** — Wherever a rule of this part says class, a Robot test
  project has the test set's suite file; package means its folder; method means
  its test; and `testName` means its first tag.

## The screen

```
NAFATH/Checkout/Payment_flow.robot

*** Test Cases ***
Pay with a saved card
    [Tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b    smoke
    # TODO: Auto-generated test steps for Pay with a saved card
    No Operation

Pay by bank transfer
    [Tags]    0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44    robot:exclude
    # TODO: Auto-generated test steps for Pay by bank transfer
    No Operation
```

1. **The suite file** — the test set *NAFATH › Checkout › Payment flow*, under
   the test source folder (Rule-CODEGEN-121).
2. **The name** — the test case's description (Rule-CODEGEN-122).
3. **The first tag** — the test case UUID, which is how Testin finds the test;
   the groups follow it (Rule-CODEGEN-123).
4. **The two lines** — what a new test holds until someone writes it
   (Rule-CODEGEN-124).
5. **`robot:exclude`** — the test case is disabled (Rule-CODEGEN-125).

## Main flow

1. A tester opens a code project whose interpreter is Python, in PyCharm or in
   any IDE with a Python interpreter set.
2. Creating a test set writes its suite file, holding `*** Test Cases ***`.
3. Creating a test case writes its test, at its place in the set.
4. Changing, disabling, ordering, moving, copying or removing the test case
   changes the test as this part's pages say for a method (Rule-CODEGEN-128).
   Renaming, moving or removing a test set or a package does the same to its
   suite file or folder.

## What Testin refuses

**A Java test project** — nothing changes. A code project whose interpreter is
a JDK, or that has none, is automated in Java, as before (Rule-CODEGEN-116).

**If the code project has no test source folder** — nothing is written, and
the Testin Help hint says how to mark one, as for Java (UC-CODEGEN-020).

**If testin.yml does not name the test project** — nothing is written
(Rule-CODEGEN-082).

**If a description comes to nothing** — no test is written (Rule-CODEGEN-122).

**If a suite file is written in Robot's pipe-separated form** — Testin writes
nothing into it, and the log names the file (Rule-CODEGEN-127).

**Testin API Model from JSON** — gray, reading *(not available for Robot
Framework yet)* (Rule-CODEGEN-117).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
