# The sample test project

A small, complete Testin project, committed so that `./gradlew runIde` opens onto
data instead of onto nothing, and so that every file format has one example a
person can read (#107).

It is **documentation as much as data**. Everything in it is invented — a
fictional `Demo` project, a `Testin Sample` author, one fixed date. Nothing here
came from a real test project, and nothing here should.

## What is in it

    automation/          the project the sandbox IDE opens
      testin.yml         says which test project this repository drives
      pom.xml            a real Java project, so generated methods have a home
      src/test/java/     where Generate Code writes

    testin-root/         what the rootTestinPath setting points at
      Demo/                    .testproject       the test project, format 5
        Test Cases/            .testcasesfolder
          Login/               .testset           3 test cases, one .testcase each
          Checkout/            .testsetpackage    a test set package
            Payment/           .testset           3 test cases, one .testcase each
        Test Runs/             .testrunsfolder
          Cycle-1/             .testrun           completed; six run items, one .runitem each
          Regression/          .testrunpackage    a test run package
            Cycle-2/           .testrun           in progress; two run items

Seven directories, seven marker formats. `.testsetpackage` and `.testrunpackage`
had no committed example anywhere before this — the format documentation (#100) takes its
examples from here.

## Running the sandbox on it

    ./gradlew runIde

On a **fresh** sandbox the task points it at `samples/testin-root` and opens
`samples/automation`. On a sandbox that already has Testin settings it does
neither: it opens whatever that sandbox had, pointed wherever its owner pointed
it.

Both halves move together on purpose. Opening the sample against somebody else's
Testin root does not resolve `testinProject: Demo`, so the tester is asked to
choose a project. Testin no longer writes that choice into `testin.yml` (it did,
twice, before Decision-011), and `SampleTestProjectTest` still asserts the file
names `Demo`.

To get the sample back, delete `.sandbox` and run again, or set the Testin root
to this folder in **Settings → Testin**.

## Changing it

`SampleTestProjectTest` reads every file here through the same model the plugin
reads it with, and asserts that:

- all seven markers are present;
- each test case's file name is its id;
- each test case carries a rank;
- a test run holds one run item file per test case and no run items file of its own;
- every run item names a test case that exists.

So this sample cannot rot quietly. If a format changes, that test fails and names
the file — which is the entire reason it is worth committing sample data rather
than writing it by hand each time.

Keep it small. It is meant to be read in one sitting; a set with forty test
cases in it would demonstrate nothing the six here do not.
