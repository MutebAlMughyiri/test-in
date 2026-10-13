[Documentation](../README.md) › Task guides

# First run — ten minutes, install to a run item status

> Nine steps, from a freshly installed plugin to a test case you wrote, a Java
> method Testin wrote for it, a recorded run item status and a report you can
> send.
> Nothing here needs the rest of the documentation.

|                     |                                                                                                   |
|---------------------|---------------------------------------------------------------------------------------------------|
| **Part of Testin**  | All of it, once, in the shortest line through                                                     |
| **Answers**         | What the plugin is for, shown rather than described                                               |
| **State**           | Written                                                                                           |
| **Checked against** | `main` at `6b9a554a`, 17 September 2026                                                           |
| **You need**        | IntelliJ IDEA 2026.2 or later, the Java plugin enabled, and a Java project with a `src/test/java` |

The screens below are drawn rather than photographed, the same way every other
page in this documentation draws them. They show what is on the screen, not what
it looks like.

---

## Before you start

**Two folders, and they are not the same thing.**

|                            | What it holds                                                | Who owns it                                           |
|----------------------------|--------------------------------------------------------------|-------------------------------------------------------|
| **The Testin folder**      | Your test projects: sets, test cases, test runs, all as JSON | You. It is a setting on this machine                  |
| **The automation project** | The Java code — the test methods Testin writes               | Your team. It is the repository you already have open |

Testin keeps them apart on purpose: test data changes when a tester writes a
test case, code changes when a developer commits, and putting them in one
folder makes every test case edit a diff in the code repository.

Pick any empty folder for the first one — `Documents/Testin` will do.

---

## 1. Open the panel

Click **Testin** on the tool window bar, at the edge of the IDE. There is no
key for this; it is the IDE's own button.

With no Testin folder set yet, the panel says so rather than showing an empty
tree:

```
┌────────────────────────────────────────────────────────────┐
│  Welcome to Testin                                         │
│                                                            │
│  The new awesome test management tool                      │
│                                                            │
│  By                                                        │
│  Muteb AlMughyiri                                          │
│                                                            │
│  ⚙ Configure Testin settings                               │
└────────────────────────────────────────────────────────────┘
```

The last line is a link. What it offers is the one step out of wherever you
are — settings when there is no folder yet, a first test project once there is.

## 2. Point Testin at a folder

Press **Configure Testin settings**, or go to **Settings → Tools → Testin**.

Set two things and leave the rest:

| Setting           | What to put                                                                        |
|-------------------|------------------------------------------------------------------------------------|
| **Testin folder** | The empty folder you picked                                                        |
| **Tester name**   | Your name. It is written into every test case and every run item status you record |

Everything on this page belongs to this machine and this person. Nothing here is
committed — [the settings page](../setting/main.md) says where each value lives and
why.

## 3. Create a test project

A test project is one product under test. Press **New Test Project** on the
panel's toolbar, or the **Create your first test project** link the empty panel
now offers. There is no key for it.

Call it `Demo`. Testin makes the folder and the two fixed folders inside it:

```
Demo
├── Test Cases
└── Test Runs
```

Those two names are fixed. Test cases go in one, records of testing go in the
other, and nothing else at that level is read.

**Now press Save to testin.yml**, the eighth button on the same toolbar. It
writes three lines into your repository saying that this code project is about
`Demo`. Testin works fully without it, with one exception: the automation code
in step 6 stays off until the file names the open test project. Press it once
and the rest of this page works.
[UC-TREE-PANEL-029](../treePanel/saveTestinYml.md) draws what it shows first.

## 4. Create a test set

A test set is a group of test cases that belong together — usually one feature.

Right-click **Test Cases**, choose **Create**, then **Test Set**, and call it
`Login`.

Double-click it, or press `Enter`, and it opens in an editor.

## 5. Write a test case

In the test set editor, press `Ctrl+M`.

Fill in the description — *Log in with a valid user* — and the expected result — *The dashboard opens*. Save.

The card appears:

```
┌──────────────────────────────────────────────────────────────────────────┐
│▌ 1. Log in with a valid user.                           ( Regression )   │
│     (E) The dashboard opens.                                             │
└──────────────────────────────────────────────────────────────────────────┘
```

Write two more, so the test run in step 8 has something to move through.

> **The period is not in your data.** Testin capitalizes and closes a
> description when it draws it, and stores exactly what you typed. Open the
> `.testcase` file if you want to check — it is yours, in plain text.

Press `F2` on a card to change one field, or any of `D` `E` `M` `T` `B` `S` `P`
`G` `O` `R` to open that field straight away.

## 6. Testin has already written the Java

There is nothing to press. Because `testin.yml` names `Demo`, saving the test
case in step 5 wrote the method — Testin writes a TestNG class into your
project's `src/test/java`, one `@Test` method per test case:

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      priority = 1)
public void logInWithAValidUser() {
    // TODO: Auto-generated test steps for logInWithAValidUser
}
```

`testName` is the test case's identity, and it is the one part never to edit: it
is how Testin finds this method again after you rename the test case.
`priority` is the test case's position in its test set, which is the order
TestNG runs methods in — not the test case's own High, Medium or Low.

The body is empty, and it is meant to be: Testin writes the annotation and the
declaration, and the rest is yours. It keeps the method in step with the tree —
renaming the test case renames the method, and removing the test case removes
the method from the class. The class itself stays.

Press `Shift+F5` on a card to jump from a test case to its method.

> **No method, and no class?** `testin.yml` is not naming this test project.
> Press **Save to testin.yml** on the panel toolbar, then save the test case
> again. Testin touches automation code only where that file names the test
> project in front of you, so one test project's methods never land in a code
> project that is about another.

## 7. Create a test run

A test run is one round of testing over the test cases you choose.

Right-click **Test Runs**, choose **Create**, then **Test Run**, call it
`Cycle-1`, and pick the `Login` set.

## 8. Record a run item status

Open the test run. Each test case is a card, and three keys record what
happened:

| Key | Run Item Status                      |
|-----|--------------------------------------|
| `P` | Passed                               |
| `F` | Failed — and it asks what went wrong |
| `B` | Blocked                              |

Press `P` on the first card. The cursor moves to the next one on its own, so a
whole test run is `P P P` without touching the mouse.

Press `F` on one of them and fill in the actual result, so the report in step 9
has a failure in it.

> **Try light mode.** It is a small always-on-top window showing the test case
> being executed, so the application under test can have the whole screen. The
> same three keys work in it.

## 9. Send the result to someone with no IDE

Select the test run and press `Ctrl+P`.

Choose a format:

| Format    | What it is for                                                    |
|-----------|-------------------------------------------------------------------|
| **PDF**   | The one to attach to a ticket                                     |
| **Word**  | The one to edit before sending                                    |
| **HTML**  | The one to open in a browser — it carries a light and dark switch |
| **Excel** | The one to filter and sort                                        |

The document holds what the test run recorded: the totals, the test cases that
passed, and every failure with what you wrote about it.

---

## That is the loop

Write test cases → Testin writes the code → run them → record run item statuses
→ send the report. Everything else in Testin is a shorter or wider version of those
five things.

## Where to go next

| If you want to                    | Read                                           |
|-----------------------------------|------------------------------------------------|
| Know every key                    | [Every shortcut](../shortcuts.md)              |
| Understand the tree               | [The tree panel](../treePanel/main.md)         |
| Write and edit test cases in bulk | [The editor panel](../editorPanel/main.md)     |
| Share test data with your team    | [Sharing work with the team](../share/main.md) |
| See what the files look like      | [The formats on disk](../formats.md)           |

## If you cloned this repository

You do not need steps 1 to 5. `./gradlew runIde` opens a sandbox IDE pointed at
`samples/testin-root`, which already holds a `Demo` test project with two test
sets, six test cases and two test runs — one completed, one in progress. Start
at step 7, or open the test run that is still in progress and go straight to
step 8.
