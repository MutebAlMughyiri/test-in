# Testin

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/31514-testin?label=marketplace)](https://plugins.jetbrains.com/plugin/31514-testin)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/31514-testin)](https://plugins.jetbrains.com/plugin/31514-testin)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/31514-testin)](https://plugins.jetbrains.com/plugin/31514-testin)

Test case management inside the IDE, next to the automation it drives.

Testin adds a tool window where you organize test projects, test sets and test
runs, write test cases, execute them, and generate reports — without leaving
IntelliJ IDEA and without a second tool to keep in sync. Test cases are stored
as JSON on disk in a Git repository of their own, so they are reviewed, branched
and merged like the code they test — without mixing into the automation project
they exercise.

## What it does

- **Organize** — test projects hold test sets and test runs, in a tree beside
  your code. Create, rename, move, copy and delete nodes with the keyboard.
- **Write** — a test case has a description, expected result, module,
  pre-conditions, steps, test data, priority and groups. The editor offers list
  and grid views, paging, filtering, search and bulk edits, with completion
  drawn from what you have already written.
- **Generate** — Testin writes and maintains the TestNG classes and `@Test`
  methods for your test cases, and keeps them in step when you rename or remove
  one.
- **Execute** — run a test case, a test set, or a whole test run from the
  gutter, the tree or the editor. Results are recorded per test case with
  status, duration and failure details.
- **Report** — export a test run to PDF, Word, Excel or HTML. Test cases export
  as XLSX, CSV, JSON or HTML, and import from XLSX, XLS, CSV or JSON.
- **Version** — commit and sync test case changes from inside the panel, with a
  diff of what changed in each test case.

## How the data is stored

Everything Testin owns is plain files under the Testin folder you configure, and
a folder is a node because of the marker inside it:

| Marker          | Node                                    |
|-----------------|-----------------------------------------|
| `.tp`           | Test project                            |
| `.tcf` / `.trf` | The Test Cases and Test Runs containers |
| `.tsp` / `.ts`  | Test set package, test set              |
| `.trp` / `.tr`  | Test run package, test run              |

Inside those folders, each thing a tester works on is a record of its own:

| Record                     | What it is                            |
|----------------------------|---------------------------------------|
| `<uuid>.testcase`          | A test case, in its test set's folder |
| `<test case uuid>.runitem` | A run item, in its test run's folder  |

The records are JSON too, and **each one is named by what it is about**: a test
case by its own UUID, a run item by the UUID of the test case it records. So
renaming or moving anything leaves every file still valid, two testers recording
run item statuses on different test cases of the same test run never touch the
same file, and what a test run recorded about itself — its status, when it ran, how it was configured — lives in the test
run's own `.tr`.

**Stored values are byte-identical to what you typed** — Testin formats for
display only, never on save — so a diff shows the change you made and nothing
else.

## Requirements

- IntelliJ IDEA **2026.2** or later (build 262+)
- **Java 25** toolchain
- **TestNG** for execution, and the Java plugin for code generation

The Java, TestNG and Git integrations are optional dependencies: Testin loads
without them and disables the features that need them rather than failing.

## Install

From the IDE: **Settings → Plugins → Marketplace**, search for *Testin*, install
and restart.

Or install it from the listing:
**[Testin on the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31514-testin)**.

Then set the Testin folder in **Settings → Tools → Testin** — the folder where
your test cases will live — and create your first test project from the panel.
Each test project in that folder is its own Git repository, kept separate from
the automation project it tests, so test data and code have their own histories.
Testin's settings are per-IDE, not per-project: the root you choose is the one
every open project uses.

**New to it?** [First run](docs/guides/firstRun.md) takes ten minutes and ends at a
recorded run item status and a report. On the way: install, a test case you wrote, the Java
method Testin wrote for it, and the document you send to someone who has no IDE.

## Building from source

```bash
./gradlew build          # compile, run tests, build the plugin
./gradlew runIde         # launch a sandbox IDE with the plugin installed
pwsh tools/inspect.ps1   # run the IntelliJ inspections headlessly
```

The sandbox writes to `.sandbox/`; the inspection tooling writes to
`.inspection/`, deliberately outside `build/` so `./gradlew clean` does not
delete the findings list. Neither is committed.

## License and privacy

Testin is built and maintained by [Muteb AlMughyiri](https://github.com/MutebAlMughyiri).
The source is licensed under the **Apache License 2.0** — see
[LICENSE](LICENSE). [EULA.md](EULA.md) is the end user agreement for the plugin
as distributed through JetBrains Marketplace, and applies only if and when it is
offered there as a paid or freemium plugin.

If you want to contribute, the terms are in
[CONTRIBUTING.md](CONTRIBUTING.md).

It runs entirely on your machine: test cases, steps and JSON files stay on your
disk or in your own version control, and the plugin embeds no telemetry, no
analytics, and never uploads your source, test data or credentials anywhere. The
full statement is in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Documentation

Read it on the web at **[mutebalmughyiri.github.io/test-in](https://mutebalmughyiri.github.io/test-in/)**,
or in this repository under [`docs/`](docs/README.md).

New to the idea rather than the reference? **[Testin - New IntelliJ plugin to
manage your test cases and test runs](https://medium.com/@mtb550/testin-new-intellij-plugin-to-mange-you-test-cases-and-test-runs-75422b0610dc)**
walks one test case from written to reported, in pictures.

Testin has eight parts. Each part gets a folder, and every thing a tester does
in it gets a page of its own: the story, its rules, its screens, what happens
step by step, and every way it can be refused. 153 use cases and 954 rules, each
checked against the code it describes.

| Document                                                   | Covers                                             |
|------------------------------------------------------------|----------------------------------------------------|
| **[The tree panel](docs/treePanel/main.md)**               | The tree on the left                               |
| **[The editor panel](docs/editorPanel/main.md)**           | Writing test cases, and executing a test run       |
| **[The view panel](docs/viewPanel/main.md)**               | One test case and its fields, on the right         |
| **[The settings page](docs/setting/main.md)**              | Everything set once per machine                    |
| **[Automation code and the gutter](docs/codegen/main.md)** | The test methods Testin writes                     |
| **[Reports](docs/report/main.md)**                         | A test run written out as a document               |
| **[Sharing work with the team](docs/share/main.md)**       | Export, import and Git                             |
| **[Inside Testin](docs/internal/main.md)**                 | The search, and the one thing that owns every file |

Seven pages sit outside the eight parts: [First run](docs/guides/firstRun.md), [every
shortcut](docs/shortcuts.md), [the formats on disk](docs/formats.md), [the
standing decisions](docs/decisions.md), [how Testin is put
together](docs/ARCHITECTURE.md), [the product](docs/product.md) and [how a
document is written](docs/standard.md).

Questions, ideas and feedback are welcome in
[Discussions](https://github.com/MutebAlMughyiri/test-in/discussions).

## Contributing

Everything between a clone and a reviewed change is in
[CONTRIBUTING.md](CONTRIBUTING.md): what you need installed, the checks, the
inspection gate, and the terms a contribution is accepted under.

The four architecture rules — all test data file access through the indexer, the
VFS before the cache, Swing on the EDT, formatting display-only — are in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), with the layer map and two
operations traced class by class. Issues and plans live in
[GitHub issues](https://github.com/MutebAlMughyiri/test-in/issues).
