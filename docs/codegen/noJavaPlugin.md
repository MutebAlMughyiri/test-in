[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-019

# UC-CODEGEN-019: Work in an IDE with no Java plugin

**As a** tester, **I want** Testin to work in PyCharm or GoLand, **so that** I can manage test cases where my team
writes its code, even where
Testin cannot generate anything.

Test management works in full. Only the Java code is missing.

Nothing starts this. It is the state of the IDE.

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
- **Rule-CODEGEN-061** — Test management works in full without the Java plugin.
  Only the code generation and the jumps into code are missing.
- **Rule-CODEGEN-062** — What is missing is shown and refuses, naming the plugin
  it waits for. A menu entry reads *<entry> (needs the Java plugin)* and is gray; a
  card's or a panel's icon is drawn gray, does not grow under the pointer, and
  says the same sentence when it is hovered or pressed. Nothing is left out: an
  entry that is absent teaches nobody the feature exists.
- **Rule-CODEGEN-063** — A plugin that is installed but switched off counts as
  missing, and switching it on needs the IDE restarted before Testin notices.

## The screen

Nothing is drawn until something asks for code that cannot be written. Then one
small red message appears near the bottom right of the IDE.

```
┌──────────────────────────────────────────────────────────────┐
│  Java Plugin Not Available                                   │
│  Automation code generation and navigation require the Java  │
│  plugin, which is not available in this IDE.                 │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always these four words.
2. **The line under it** — always the same sentence.
3. **The color** — red, because nothing was written.
4. **How long it stays** — about five seconds, then it fades. It is not kept in
   the IDE's notification list.

## What is missing

| Missing                      | What the tester sees                                                                                                                                                                                                                                                                                                                                                                          |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Automate Test Case**       | On the menu, gray, reading *Automate Test Case (needs the Java plugin)*                                                                                                                                                                                                                                                                                                                       |
| **Navigate to Test Method**  | The same on the menu, but under a second name: the grayed entry reads *Navigate to Test Method (needs the Java plugin)*. That is difference 11 on [the automation code page](main.md#where-the-plugin-breaks-its-own-rules). Its icon on a card and on the view panel is drawn gray, does not grow under the pointer, and says the Java plugin is what it needs when it is hovered or pressed |
| **Run Test Method**          | On the menu, gray, reading *Run Test Method (needs the Java plugin)*. Its run icon on a card and on the view panel is drawn gray, the same way                                                                                                                                                                                                                                                |
| **Run Tests**                | On the tree's menu, gray, reading *Run Tests (needs the Java plugin)*                                                                                                                                                                                                                                                                                                                         |
| The gutter marks             | Not drawn in any editor                                                                                                                                                                                                                                                                                                                                                                       |
| Every class and every method | Nothing is written, ever                                                                                                                                                                                                                                                                                                                                                                      |

## What still works

The tree, both editors, the view panel, light mode, executing a test run by
hand, reports, export, import, and both kinds of sync. Everything except the
Java code.

## What Testin refuses

**If a generator is reached anyway** — nothing appears. The Testin Help mark
turns red, and its hint reads *Automation code generation and navigation require
the Java plugin, which is not available in this IDE.*, with the guide *How to
generate and run automation code*.

**If navigation is reached anyway** — a short message titled **Java Plugin Not
Available** says the same, every time, and the hint waits beside it.

**If the IDE is still building its index** — the change is refused rather than
waited for, and the message names the test case: *Log in with a valid user needs
the IDE to finish indexing first*. No class can be found by name until the index
is there, and deferring would be wrong for a rename or a move: those run while
the old name still finds the code (Rule-CODEGEN-004), so one that waited would
look for a class that has already been renamed.

## Where the plugin breaks its own rules

**The grayed entry is not called what the live one is called.** In an IDE with
the Java plugin the menu reads **Navigate to Test Method**. In an IDE without it
the same entry reads *Navigate to Test Method (needs the Java plugin)*, so one entry
has two names. That is difference 11 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
