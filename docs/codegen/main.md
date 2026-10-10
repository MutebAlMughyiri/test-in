[Documentation](../README.md) › Automation code and the gutter

# Automation code and the gutter

Testin writes a Java test method for every test case, and keeps it in step as
the tree changes. The marks in the gutter beside those methods lead back to the
test case they came from.

|                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | Automation code and the gutter                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| **Answers**         | What Testin writes into the code, when it rewrites it, and what a tester sees when it cannot                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| **Numbering**       | Use cases are `UC-CODEGEN-001` to `UC-CODEGEN-022`. Rules are `Rule-CODEGEN-001` to `Rule-CODEGEN-109`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **Retired**         | `Rule-CODEGEN-015` said what `Rule-CODEGEN-046` says — the groups attribute is written only when the test case belongs to one. Retired 8 September 2026; read Rule-CODEGEN-046 instead. The number is not given to anything else. Retired 4 October 2026 as copies (#392): `Rule-CODEGEN-017` said what `Rule-CODEGEN-002` says - a test case with no description gets no method; `Rule-CODEGEN-026`, `Rule-CODEGEN-032` and `Rule-CODEGEN-039` said what `Rule-CODEGEN-001` says - a method is found by the identity in `testName`, never by its name; `Rule-CODEGEN-041` said what `Rule-CODEGEN-021` says, which now carries its one extra clause, that the empty description is recorded. Read the kept rule instead. None of these numbers is given to anything else |
| **State**           | **Written** — [#181](https://github.com/MutebAlMughyiri/test-in/issues/181)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Checked against** | `main` at `1270e599`, 20 September 2026                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Written to**      | [How a document is written](../standard.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

---

## The use cases

|                    | What the tester does                                                |                                                                      |
|--------------------|---------------------------------------------------------------------|----------------------------------------------------------------------|
|                    | **Getting code written**                                            |                                                                      |
| **UC-CODEGEN-001** | [Get a class when I create a test set](getClassForTestSet.md)       | New test sets get a Java class ready for their code.                 |
| **UC-CODEGEN-002** | [Get a method when I create a test case](getMethodForTestCase.md)   | New test cases get a test method to fill in.                         |
| **UC-CODEGEN-003** | [Get a method for a test case that had none](getMissingMethod.md)   | Give a test case a description and its method appears.               |
| **UC-CODEGEN-004** | [Get the whole subtree's code when I copy](copySubtreeCode.md)      | A copied package is as runnable as the original.                     |
| **UC-CODEGEN-005** | [Ask for the method a test case never got](automateTestCase.md)     | A test case that arrived as data gets the method it should have had. |
|                    | **Moving between the two**                                          |                                                                      |
| **UC-CODEGEN-006** | [Go to the code from a test case](goToCode.md)                      | Read or change what a test case really does.                         |
| **UC-CODEGEN-007** | [See the test case from the code](goToTestCase.md)                  | See what a method is meant to prove.                                 |
|                    | **Running it**                                                      |                                                                      |
| **UC-CODEGEN-008** | [Run a test case's automation](runAutomation.md)                    | Run the code and have the run item status recorded.                  |
| **UC-CODEGEN-009** | [Stop a running test case](stopAutomation.md)                       | End an execution so something can be changed and tried again.        |
|                    | **Keeping the code in step**                                        |                                                                      |
| **UC-CODEGEN-010** | [Change a test case's description](renameTestCase.md)               | Reword a test case and the code says the same.                       |
| **UC-CODEGEN-011** | [Reorder the test cases in a test set](reorderTestCases.md)         | Set the order the automation runs in.                                |
| **UC-CODEGEN-012** | [Change a test case's groups](changeGroups.md)                      | Run only the group a test case belongs to.                           |
| **UC-CODEGEN-013** | [Turn a test case off](disableTestCase.md)                          | Stop a broken test failing every execution.                          |
| **UC-CODEGEN-014** | [Remove a test case](removeTestCase.md)                             | Remove a test case and its dead code goes too.                       |
| **UC-CODEGEN-015** | [Rename a test set](renameTestSet.md)                               | Rename a test set and the class name follows.                        |
| **UC-CODEGEN-016** | [Move a test set](moveTestSet.md)                                   | Move a test set and its class moves with it.                         |
| **UC-CODEGEN-017** | [Rename or move a package](renamePackage.md)                        | Rename a package and every file below stays correct.                 |
| **UC-CODEGEN-018** | [Remove a test set or a package](removeTestSet.md)                  | Delete a test set and its code goes with it.                         |
|                    | **When it cannot work**                                             |                                                                      |
| **UC-CODEGEN-019** | [Work in an IDE with no Java plugin](noJavaPlugin.md)               | Manage test cases in PyCharm or GoLand.                              |
| **UC-CODEGEN-020** | [Work in a project with no Java test folder](noTestSourceFolder.md) | Find out why no code is appearing.                                   |
| **UC-CODEGEN-021** | [Let an agent write the method body](agentWritesTheBody.md)         | Have the steps written into the method rather than a TODO.           |
| **UC-CODEGEN-022** | [Make the API model of a call](apiModelFromJson.md)                 | Turn an API call's JSON into a request class and a response record.  |

---

## What this part is for

A test case says what to do. A test method does it. Keeping the two in step by
hand is the work that makes teams give up on automation: a test case is renamed
and the method keeps the old name, a test case is removed and the method stays
forever.

So Testin writes the method, and rewrites it whenever the tree changes. The
tester never edits the parts Testin owns.

**Four words, before the rules use them.**

- The **test source folder** is the folder in the code project marked as holding
  Java tests. Testin writes everything under it.
- A **generated method** is the Java method Testin wrote for one test case.
- The **gutter** is the narrow strip down the left of a code editor, where the
  IDE draws its own marks.
- To be **in step** is for the code to say what the tree says. Testin's job here
  is keeping it that way.

---

## What Testin writes

For a test case, one method:

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      groups = {"Regression", "Smoke"},
      priority = 3)
public void logInWithAValidUser() {
    // TODO: Auto-generated test steps for logInWithAValidUser
}
```

1. **description** — the test case's description, exactly.
2. **testName** — the test case's identity. This is what ties the method to the
   test case, and it is the only part that must never be edited.
3. **groups** — the test case's groups. Written only when it has any.
4. **priority** — the test case's **position in its test set**, counting from
   one. It is the method's priority, which is what TestNG runs methods in the
   order of. The test case's own High, Medium or Low is a different thing and
   writes nothing here (Rule-CODEGEN-014).
5. **The method name** — the description with everything but letters and digits
   removed, the first word lowercase and later words capitalized.
6. **The body** — one comment, and nothing else. The tester writes the rest.

For a test set, one class holding those methods. For a package, a folder.

---

## Every key

| Key                                | What it does                                    | The page that owns it                 |
|------------------------------------|-------------------------------------------------|---------------------------------------|
| `Shift+F5`                         | Goes to the generated method                    | [UC-CODEGEN-006](goToCode.md)         |
| `F5`                               | Runs the selected test cases, or stops them     | [UC-CODEGEN-008](runAutomation.md)    |
| `F12`, `Ctrl+Alt+Cmd+F12` on a Mac | Writes the method for a test case that has none | [UC-CODEGEN-005](automateTestCase.md) |

The gutter mark has no key. Stopping has no key of its own.

---

## Where the code lands

```
testin_example/
└── src/test/java/                     the test source folder
    └── demo/                          the test project
        └── accounts/                  a test set package
            └── LoginTest.java         a test set
                logInWithAValidUser()  a test case
```

1. **The test source folder** — found once per code project, and remembered.
2. **Each folder above the test set** — becomes a package.
3. **The test set** — becomes a class, whose name always ends in `Test`.
4. **Each test case** — becomes a method in that class.

The two fixed folders, test run packages and test runs generate nothing at all.

A package is never written on its own. The folders are made on the way to a
class, so creating a package and stopping there puts nothing on disk, and the
folder appears when the first test set is created inside it. Java has no empty
packages, and an empty folder is invisible to the thing that looks a class up
by name.

---

## When Testin will not generate

Writing code touches two things the plugin does not own: the IDE's Java support,
and the IDE's index. A third is the team's own answer to which test project this
repository drives.

**Until `testin.yml` names the open test project**, the code is off. Nothing is
generated, renamed, moved or removed, and no gutter mark is drawn. Testin Help
holds a hint saying why, with **Save to testin.yml** beside it; that button,
also in the Testin panel's title bar, turns code on (Rule-CODEGEN-082). Testin reads the
file again on **Refresh** as well, so a
file a colleague changed takes effect without restarting the IDE.

**Without the Java plugin**, nothing is generated at all. A message says so once
for the whole code project, and every later operation is a silent skip. That is
[UC-CODEGEN-019](noJavaPlugin.md).

**While the IDE is indexing**, Testin refuses and says so - once for each time
the IDE indexes, however many operations it refuses meanwhile, and again the
next time it indexes. Every generated file is found by the name of the class it
belongs to, and looking a class up by name is a question the index answers - so
until the index is built there is no answer to give.

The refusal is deliberate, rather than waiting for the index and doing the work
afterward. Waiting would be right for creating something and wrong for renaming
or moving it: **Rule-CODEGEN-004** has a rename happen while the old name still
finds the code, and a rename that waited would run after the tree had changed
and look for a class that no longer answers to that name. So Testin says it
cannot, and the tester's next attempt works.

Both answers are given in one place, so a fifteenth operation gets them without
asking. Everything Testin writes runs inside one of the IDE's write commands,
which is also what makes each operation a single entry on the undo history.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

|                   | The rule it breaks                                                          | What a tester sees                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-------------------|-----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 3**  | Rule-CODEGEN-001 — one test case, one method                                | Two test cases whose descriptions differ only in punctuation share one method, and the second gets none. It can no longer be typed: the create dialog and the update dialog both refuse such a description and say which method it would have named. It can still arrive by the doors that cannot refuse — an import, a paste, a Git merge, a sync, and the bulk description editor — and a test set that already held a clash before the refusal existed still holds it. Those no longer pass in silence: generating says which test cases got no method and why, so the tester learns it then rather than at the first `F5`. |
| **Difference 6**  | Rule-CODEGEN-004 — the tree and the code agree                              | Moving a test set to a place Testin has not read still leaves the class where it was — guessing a destination the tree has not read is what once moved a whole package into the default package and lost it. The tester is told now, in a notification that stays, naming the class and what to do about it. The tree and the code disagree until they act.                                                                                                                                                                                                                                                                    |
| **Difference 10** | Rule-CODEGEN-082 — code off means the entries are gray and say why          | Two of the four gray themselves and two do not. **Automate Test Case** and **Run Tests** go gray, reading *(Needs testin.yml)*. **Navigate to Test Method** and **Run Test Method** stay live: pressing either says *testin.yml does not name this test project* and does nothing. The buttons beside them on a card are gray, so the same gesture answers one way from the card and another from the menu.                                                                                                                                                                                                                    |
| **Difference 11** | Rule-CODEGEN-062 — a grayed entry reads *\<entry\> (needs the Java plugin)* | The entry it names is not its own. In an IDE with no Java plugin, **Navigate to Test Method** reads *Navigate to Test Method (needs the Java plugin)*, because the name comes from the card button's tooltip rather than from the entry. One entry with two names, and the one the tester reads is not the one Find Action lists it under.                                                                                                                                                                                                                                                                                     |

**Settled since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone             | Was                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Difference 2** | **Automate Test Case** and `F12` were live on every selected test case and always answered *Not built yet*, so the one entry named after generating code was the one that did not. It was grayed with the reason in its own name first, which is what the no-hidden-buttons rule asks for, and then given the work: it writes the method a test case has not got. Filling in what that method does is still [#3](https://github.com/MutebAlMughyiri/test-in/issues/3). Grayed 10 September 2026, [#243](https://github.com/MutebAlMughyiri/test-in/issues/243); built 17 September 2026                             |
| **Difference 5** | One state said two ways: *has no generated code yet* when run, **Nothing to open** when jumped to. Both go through the one owner now. Fixed 9 September 2026, [#246](https://github.com/MutebAlMughyiri/test-in/issues/246)                                                                                                                                                                                                                                                                                                                                                                                         |
| **Difference 1** | `priority` in the generated method carries the position, not the test case's priority. Not a difference: a test method's priority and a test case's priority are different things, and the test case's own writes nothing into the code on purpose. Decided 7 September 2026, [#242](https://github.com/MutebAlMughyiri/test-in/issues/242)                                                                                                                                                                                                                                                                         |
| **Difference 8** | A removal, a move and a rename all read *Class Name Unknown* from the one place that builds a class name, because only creating one made that news. It goes to the log now, which is what Rule-CODEGEN-006 said all along. Fixed 9 September 2026, [#249](https://github.com/MutebAlMughyiri/test-in/issues/249)                                                                                                                                                                                                                                                                                                    |
| **Difference 4** | Clicking the gutter mark of a removed test case did nothing at all, and generated code outlives its test case, so that was the ordinary case rather than a rare one. It says so now, naming the method (Rule-CODEGEN-069). Rule-CODEGEN-006 is not what it broke: that rule is about what goes wrong while Testin writes code, and this is a tester's click getting no answer. Fixed 9 September 2026, [#245](https://github.com/MutebAlMughyiri/test-in/issues/245)                                                                                                                                                |
| **Difference 7** | In an IDE with TestNG and no Java, **Run Test Method** was still offered, no test case found a method, and nothing said the Java plugin was the reason. It declares both plugins it needs now — TestNG starts the execution, Java finds the method it starts — so it is gray in an IDE with only one of them. The entries are shown in every IDE rather than left out, so a menu does not change shape between IDEs. Fixed under [#248](https://github.com/MutebAlMughyiri/test-in/issues/248); retired from this list 20 September 2026                                                                            |
| **Difference 9** | Two test sets whose names came to nothing once the illegal characters were removed both wrote into one class called `DefaultTest`, so the second set's methods landed in the first set's file. Each is named after what it was called now, and the package fallback no longer carries a timestamp, so a node names the same package on the call that writes its code and the call that comes looking for it (Rule-CODEGEN-011). The row cited Rule-CODEGEN-003, which is about the method body. Fixed under [#250](https://github.com/MutebAlMughyiri/test-in/issues/250); retired from this list 20 September 2026 |

---

## Not decided

**Question 1** — Should `priority` in the generated method carry the position or
the test case's priority? It carries the position on purpose, so a test run
executes in the tester's order. The attribute's name says otherwise.

**Question 2** — Should a test case whose method was skipped for a name clash be
told so at the moment it is created? Today the tester finds out at the first
`F5`.

**Question 3** — The gutter mark is drawn only for TestNG methods Testin wrote.
A tester who edits the identity loses the mark with nothing saying why.

---

[Documentation](../README.md) › **Automation code and the gutter**
