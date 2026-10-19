[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-017

# UC-CODEGEN-017: Rename or move a package

**As a** tester, **I want** the Java package folder to follow a package I rename
or move, **so that** every class beneath it still declares the package it is
really in.

Rename or move a package, and every Java file under it is put right.

There is no key for this. It happens when a package is renamed or moved.

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
- **Rule-CODEGEN-056** — Renaming a package renames its folder, then rewrites
  the package line in every Java file beneath it.
- **Rule-CODEGEN-057** — Moving a package does the same, after moving the
  folder.
- **Rule-CODEGEN-058** — A package dropped into itself is not a move, and
  nothing is rewritten.
- **Rule-CODEGEN-080** — A rename is refused when the automation code already
  has the package the new name makes, beside the one being renamed. It is
  refused before anything moves, so neither the folder nor the code changes.
- **Rule-CODEGEN-081** — A rename that moves automation code - a test project,
  a package or a test set - waits for the IDE to finish indexing. It is refused
  while the IDE indexes, so the code and the tree never end up with two names.

## What this covers

| The tester does this       | What happens to the code                                         |
|----------------------------|------------------------------------------------------------------|
| Renames a test set package | The folder is renamed, every file below declares the new package |
| Moves a test set package   | The folder is moved, every file below declares the new package   |
| Renames the test project   | The folder at the top is renamed, and every file below follows   |
| Removes the test project   | The folder at the top is deleted, with everything under it       |

Test run packages and test runs have no code, so nothing happens for them.

## What the tester sees

The node takes its new name, and a message reads *Renamed*. Nothing on screen
mentions the code. In the Project tool window, the folder has the new name, and
the `package` line at the top of every Java file beneath it has been rewritten.

## Main flow

1. The tester renames a package from **Accounts** to **Identity**.
2. Testin renames the matching folder under the test source folder.
3. Testin walks every Java file beneath it.
4. Each file's package line is rewritten from where the file now sits.
5. Testin then renames the package itself in the tree.

## What Testin refuses

**If the automation code already has the package the new name makes** — for
example a test project renamed to `Tests` in a code project that has its own
`tests` package - nothing is renamed. *Package tests Already Exists* is shown
in red (Rule-CODEGEN-080). A package whose old folder is gone - a
colleague's rename already pulled - is not in the way: there is nothing left to
move, and the tree follows.

**If the IDE is indexing** — nothing is renamed, and *Rename needs the IDE to
finish indexing first* is shown in red (Rule-CODEGEN-081).

**If the folder cannot be found** — nothing is renamed, and only the log says
so.

**If the move goes to a place Testin has not read** — the folder is left where
it is. A notification that stays appears, titled *The automation code did not
move with* and the package's name. It names the package and says what to do.

**If the code project has no Java test source folder** — nothing happens, and
nothing is said.

**If the IDE has no Java plugin** — nothing happens.

## Why every file below is rewritten

The package a Java file declares must match the folder it is in. Moving one
folder changes that for every file underneath it, however deep. Testin works
each one out from where the file now sits. It does not edit the old text. So a
file that was already wrong is put right too.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
