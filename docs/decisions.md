[Documentation](README.md) › Standing decisions

# Standing decisions

> Fifteen decisions in Testin look wrong until you know why they were made. Each
> one has been proposed for reversal at least once, and each reversal would have
> broken something the decision exists to protect. They are written here so a
> contributor reads the reason before writing the fix.

|                           |                                                                                                                                                   |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**        | None. These are decisions about how the plugin is built, not about what a tester sees                                                             |
| **What the numbers mean** | `Decision-001` and up, in the order they were recorded. A number is never reused and never renumbered                                             |
| **Answers**               | Why a design that looks odd is deliberate, and what it costs to change                                                                            |
| **State**                 | Written                                                                                                                                           |
| **Checked against**       | `main` at `b056082c`, 23 September 2026                                                                                                           |
| **Written to**            | [The standard](standard.md), as far as it applies. A decision is not a use case, so it has context, a decision and consequences instead of a flow |

---

## How to read this

Every decision has the same four parts.

| Part                               | What it gives you                                            |
|------------------------------------|--------------------------------------------------------------|
| **Context**                        | What was true when the decision was made, and what forced it |
| **Decision**                       | The decision itself, in one sentence                         |
| **Consequences**                   | What it costs, and what it buys                              |
| **If you are about to reverse it** | The specific thing that breaks. Read this one first          |

**A decision is superseded, never edited into a different answer.** When one
stops being true, its section stays exactly as written and a new decision is
added below with a higher number, and the old one gains a *Superseded by* line.
That is the same rule the [rule numbers](standard.md) follow, for the same
reason: a decision nobody can find the history of gets made again every year.

---

## Decision-001 — The indexer owns every read and write of test data

**Context.** Test data is JSON on disk, and every panel wants it: the tree draws
nodes from it, both editors read test cases out of it, the view panel shows one
test case, the report writes a test run out. When each of them read the disk
for itself, "does this node exist" was a `Files.exists` in one place, a cache
lookup in another, and a stale `AbstractNode` in a third.

**Decision.** `org.testin.indexer` is the only package that touches test data.
Everything else holds `AbstractNode` and `TestCaseDto` objects the indexer
served, and asks the indexer to create, move, rename, copy or delete. Inside the
indexer, the VFS operation happens first and the cache is updated after it
succeeds.

**Consequences.** Every read is an in-memory lookup, so the tree and the panels
answer at once. A short list of packages may open a file directly, and what they
have in common is that none of them reads or writes test data: they handle
generated source, the automation repository's own `testin.yml`, the Git working
tree, files outside the tree, report output, the IDE settings path, the log, and
the temporary folder a bug report is sent from. The list itself is in
[ARCHITECTURE.md](ARCHITECTURE.md) and nowhere else, because
`ArchitectureTest` enforces exactly that list and a second copy of it here would
be the one that goes stale.

**If you are about to reverse it.** Updating the cache before the VFS operation
is the specific version of this that keeps getting written, because it reads as
"update the model, then persist". It creates phantom directories and *already exists in VFS* errors, because a marker
write creates directories on
its way to writing the marker.

---

## Decision-002 — What a tester types is stored byte for byte

**Context.** Testin capitalizes a description and closes it with a period so a
test set reads consistently. The grid and the editor fields are typed into.

**Decision.** Formatting is display-only. A surface may draw a value differently
and never saves the drawn form; an editable surface loads the **raw** value when
editing begins. `Rule-EDITOR-PANEL-005` states it for a tester, and
`Rule-VIEW-PANEL-026` names the four fields that are formatted at all.

**Consequences.** `TestSetEditorAttributes` answers both questions in one place:
`displayValue` for a reader and `gridValue` for a cell. Reference, module and
test data are never formatted. An identifier and a label are not sentences. Test
data is a value that gets used rather than read, so a character the display
decides to change is a value that no longer works.

**If you are about to reverse it.** The failure is silent and permanent. A cell
that loads the formatted value commits it back the first time a tester edits a
row they did not mean to change, and nothing in the JSON says a period was
Testin's rather than theirs.

---

## Decision-003 — Absence is an empty value, not a null

**Context.** A nullable field spreads its check across every reader. A sweep
through the plugin found the same `if (x != null)` written at thirty-six call
sites for one enum constant whose field was null.

**Decision.** A field that is "not set yet" holds an empty value of its own
type — `""`, `Duration.ZERO`, an empty list, an `EMPTY` enum constant, an epoch
timestamp — or an empty `Optional` where the thing genuinely arrives later. DTO
and marker fields carry Lombok's `@NonNull`, which generates the runtime check;
everything else carries the JetBrains `@NotNull` / `@Nullable` so the annotation
is the contract.

**Consequences.** `Duration.ZERO` and `Config.NOT_EXECUTED` on a run item are
the pattern to copy. `BugSeverity.EMPTY` and `BugPriority.EMPTY` were, until a
severity nobody chose became Enhancement and a priority Low. `Optional` is used
as a field type on purpose — a popup built on first show, a service the
application has not started, a test run an editor has not loaded.

**If you are about to reverse it.** The `OptionalUsedAsFieldOrParameterType`
inspection is switched off in `.idea/inspectionProfiles/Testin.xml`, and it is
switched off deliberately. What it argues for is a nullable field, which is the
thing the sweep removed. Turning it on is reversing this decision, not tidying
up.

---

## Decision-004 — SFTP is the maintained JSch fork, not JSch and not MINA SSHD

*Superseded by Decision-012, 19 September 2026: there is no SFTP sync
anymore. Left as it was written.*

**Context.** The server sync needs an SFTP client inside the plugin.
`com.jcraft:jsch` last shipped in 2018 and cannot negotiate `rsa-sha2` against a
current `sshd`. Apache MINA SSHD is maintained and is the obvious alternative.

**Decision.** `com.github.mwiede:jsch` — the maintained fork — is the SFTP
client. MINA SSHD was ruled out on its default: `ClientBuilder` installs
`AcceptAllServerKeyVerifier.INSTANCE`, so a client built the documented way
trusts any host that answers.

**Consequences.** No transitive dependencies reach the distribution. A real SFTP
server runs inside the test JVM (`sshd-sftp`, test scope only), so the transport
is exercised against a real server on every build rather than against a machine
somebody set up.

**If you are about to reverse it.** Any replacement has to refuse an unknown
host key by default, not merely be able to. See Decision-007.

---

## Decision-005 — A Testin editor tab is a light virtual file on a deprecated file system

**Context.** A test set is a directory of JSON files, and the editor that opens
it is not a text editor. The platform's editor manager opens a `VirtualFile`,
so the node needs one.

**Decision.** `UnifiedVirtualFile` is a `LightVirtualFile` on the `testin://`
protocol, served by `TestinFileSystem`, which extends
`DeprecatedVirtualFileSystem` and implements `NonPhysicalFileSystem`.

**Consequences.** `TestinFileSystem.findFileByPath` returns nothing, always, and
cannot do otherwise: a file system is application level, so there is no project
to ask, and a node's kind lives in a marker file that only the indexer may read (Decision-001). Testin therefore closes
its own tabs as the project closes, so
the IDE's tab list holds nothing of ours and nobody asks.

**If you are about to reverse it.** "Deprecated" here names the base class, not
a scheduled removal, and the obvious improvement — making `findFileByPath`
answer — was measured and refused in
[#160](https://github.com/MutebAlMughyiri/test-in/issues/160). The IDE asks for the file
about twenty seconds before the index exists.

---

## Decision-006 — Testin writes its own log, beside the IDE's

**Context.** The plugin traces indexing, code generation, Git and the sync at a
level a tester can turn up to TRACE. Writing that through the platform's own
logger puts it in `idea.log` at whatever level the IDE is set to.

**Decision.** `LogWriter` is an application service holding a bounded queue
and one daemon thread named `Testin-Async-TestinLogger`, writing to
`PathManager.getLogPath()/testin.log` and rolling at 5 MB. `TestinLogger` is the
static front door and reads the caller's class name from a `StackWalker`.

**Consequences.** *Help → Show Log in Explorer* finds it and *Collect Logs and
Diagnostic Data* bundles it, because it sits beside `idea.log`. The location
never depends on an open project. Nothing on a caller's thread waits for the
log to be written.

**If you are about to reverse it.** `LogLevel.FATAL` and `TestinLogger.fatal` have no
callers and are kept on purpose: the settings combo is built from
`LogLevel.values()` and stores the choice by name, so deleting the constant makes a
stored setting fail to parse on the next start.

---

## Decision-007 — An unknown SSH host is refused, and trusting one is the tester's step

*Superseded by Decision-012, 19 September 2026: there is no SFTP sync
anymore. Left as it was written.*

**Context.** The first sync against a new server always fails with a host-key
error, and the fix that removes the error in one line is to accept any key.

**Decision.** `SftpTransport` opens the connection with `StrictHostKeyChecking`
left at its default, which refuses a host this machine has not seen. Trusting a
new host is done once by the tester, in their own
`~/.ssh/known_hosts`, and never by a default in this code.

**Consequences.** A tester syncing to a new server has one manual step. Testin
never holds an SSH credential of its own — the same rule the Git integration
follows by handing every operation to `git4idea`.

**If you are about to reverse it.** The refusal is the feature. A client that
connects anyway connects to anything claiming to be the server, and the sync
would then upload a whole test project to it.

## Decision-008 — The source is Apache 2.0, and a contribution needs an agreement

**Context.** `LICENSE.md` was JetBrains' developer EULA for paid plugins —
Subscriptions, Fallback Versions, Paid Plugins — while the repository was being
opened up: public, with an issue tracker, contributor documents and starter
work. The two pull against each other. A proprietary EULA gives an outside
contributor no clear right to submit or reuse anything, so the documents invited
people to a door that was shut.

The plugin was also not being sold. `plugin.xml` carries no `product-descriptor`,
which is the tag that makes a Marketplace listing paid, so Testin was *licensed*
as a paid plugin and distributed as a free one. Nothing was earning, so nothing
was at risk in the decision itself.

The question that decided it was not "open or closed" but **"does this stop
Testin being sold later"** — to JetBrains, to a test-management vendor, to
anyone.

**Decision.** The source is licensed under the **Apache License 2.0**. The
EULA is kept as `EULA.md` for the distributed plugin, if and when it is offered
as paid or freemium. Contributions are accepted under Apache 2.0 **and** a
contributor license agreement.

**Consequences.**

A license is not ownership, and that is the whole of why this is safe. Apache
2.0 says what others may do with the code; the copyright stays with the author.
Testin can be relicensed going forward, offered under a separate commercial
license, or sold. What cannot be undone is a version already published: whoever
received it keeps that grant for that code, forever.

The contributor license agreement is the part that protects the rest. A patch
sent under Apache 2.0 leaves its author owning it, and Testin holding a license
— which is enough to ship and not enough to relicense or to give a buyer clear
title. Collected at the start it is a sentence; collected afterward it is every
past contributor, one at a time.

Going paid is unaffected and starts elsewhere. The Marketplace product code
"must be agreed with JetBrains in advance", so the paid route begins with a
conversation rather than a commit. The freemium shape, a free plugin with paid
optional features, is compatible with an Apache-licensed source.

**If you are about to reverse it.** Check who owns the code first. Every
contribution merged without an agreement narrows what can be done with the whole
of it, and the narrowing is not visible in the repository — it looks exactly
like a merged pull request.

---

## Decision-009 — A test run's configuration answers are English, and the option lists are not translated

**Context.** A test run is created by answering eight questions: Test Type,
Platform, Component, Language, Browser, Device Type and two more. Each offers a
list to pick from, and what the tester picks is written into the test run's
JSON word for word. `TestRunConfiguration.is(PLATFORM, Answer.WEB)` then reads that
stored string back to decide whether the Browser question is asked at all, and
all four report generators print it.

So one string is three things at once: a caption on a form, a value on disk, and
half of a rule about which questions follow which answers. The field **names**
are translated. The options are not.

`RunItemStatus` solved the same problem the other way round. The constant name is
what is stored, the label is what is shown, and an import matches the constant
whatever language the file was written in. The test run configuration has no
such split: its option list is a `String[]` and the stored value is the display
string.

**Decision.** The eight option lists stay English. They are values, not labels,
and Testin does not translate values.

**Consequences.** A test run created in a French IDE reads the same as one
created in an English IDE, in the file and in every report. Nothing that was
saved before today stops matching the cross-field rule. What it costs is that a
French tester picks *Web* from a list of English words under a French caption,
which reads as an oversight until somebody finds this page.

**If you are about to reverse it.** Translating an option changes what a saved
test run says. Every test run saved before the change keeps the English word, so
the rule that shows the Browser question stops matching them, and a test run
created in French reads differently in a report generated in English. The way
out is the `RunItemStatus` shape - a small enum per field, the constant stored
and the label shown, plus a read that maps the old stored strings onto the
constants. It is that read, not the enums, that is the work.

---

## Decision-010 — Testin's own translations are reached by the IDE's locale, not by a language plugin

*Superseded by Decision-016, 27 September 2026: Testin ships in English only, so
there is no translation to reach. Left as it was written.*

**Context.** `Bundle` extends `DynamicBundle`, which picks its locale from the
IDE's language. JetBrains ships localization plugins for Chinese, Japanese and
Korean only, so Settings offers no French entry and no Hindi entry: no amount of
clicking selects either of Testin's bundles.

They do resolve. `DynamicBundle` falls back to the JVM's default locale, so an
IDE started with `-Duser.language=fr`, or with `hi`, reads `messages_fr` or
`messages_hi`. That is how the sandbox has to be run to see any of the
translation work at all.

**Decision.** Testin does not register a `DynamicBundle.LanguageBundleEP`. The
translations ship, and the way to reach one is the IDE's own locale.

**Consequences.** Nobody can switch Testin's language on its own, and nobody
sees a Testin language setting that the rest of the IDE would not follow. A
sandbox pass that starts the IDE normally reads English, which is correct and
is not evidence that the translation is broken - this line is here so that
finding is not made twice.

**If you are about to reverse it.** Registering the extension point is a
decision about what the plugin ships rather than about the strings: it puts
Testin's language in a list beside the IDE's own, so a tester can have a French
Testin inside an English IDE. Every dialog then has to read correctly with two
languages on screen at once, which is a thing to design rather than a flag to
set.

---

## Decision-011 — Testin reads `testin.yml` and never writes it

*Superseded by Decision-013, 19 September 2026: one button writes the file, and
the automation code needs it. Left as it was written, apart from grammar.*

**Context.** Seven places wrote `testin.yml` - choosing a project, creating one,
cloning one, drawing the tree, the push prompt - and each created the file when
it was absent, so a repository ended up carrying a committed file nobody chose
to add. A code project without one could not clone a test project at all. And
the file's raw values were handed to eight classes, each deciding on its own
what a missing one meant (#301).

**Decision.** One class, `config/TestinYml`, reads `testin.yml` when it is
there, and nothing writes it. Which test project a tester chooses for a code
project is kept in the IDE's own storage for that project on this machine, and
never committed. It wins over the project the file names until the file names a
different one.

**Consequences.** Every flow works without the file: a clone is named after its
repository, and the branch box follows the folder's Git. A team that wants
every colleague on the same test project with no setup writes the file by hand.
The file's values and parser are package-private, and `ArchitectureTest` keeps
the YAML parser in the one class. The choice is the second value kept per
project on this machine, beside the editors that were open (`LastOpenEditors`), and, like them, it is a choice, not a
setting.

**If you are about to reverse it.** Writing the file back - to remember a pick,
a remote, a name - commits one machine's choice into a file the whole team
shares, which is what Muteb refused twice on 19 September 2026. Reading a value
of the file anywhere but `TestinYml` brings back a second answer to what a
missing value means.

---

## Decision-012 — Git is the only way a test project is shared

**Context.** A test project could be shared two ways: through Git, or by syncing
it with an SFTP server (#94). The server sync was the larger of the two to
carry. It had its own SSH client, a field-by-field merge, a lock so one machine
synced at a time, and a record of what each machine had agreed with the server.
It kept a password in the IDE's password store and needed two settings. While a
sync ran, every test run write was held back. That came to 5,183 lines in the
`sftp` package and its tests, for teams without a Git repository. Muteb, 19
September 2026: *"we have git only now"*.

**Decision.** A test project is shared through Git, or not at all. Export and
import still hand test cases to somebody without an IDE. The SFTP sync, its
settings, its dependency and its local test server are removed (#334).

**Consequences.** No SSH client ships in the plugin, and Testin keeps no
password of its own. A test run change is written the moment it is made, with
nothing held for a sync. `testin.yml` loses the `connection` key, which only
ever told Git from SFTP: `location: remote` with a `repoUrl` is a Git project.
A file that still has `connection` or the server keys has them skipped as
unknown keys, each named in the log. So an old Git file still clones, an old
SFTP file is not shared, and the test project either one names is still read. A
password kept for a server stays in the IDE's password store, where Testin no
longer looks.
Decision-004 and Decision-007 are superseded.

**If you are about to reverse it.** A second sync is a second answer to how a
test project reaches the team. Its merge would have to agree with the Git
merge on every field, and its SSH client would have to refuse an unknown host
the way Decision-004 and Decision-007 describe.

---

## Decision-013 — Code needs `testin.yml`, and one button writes it

**Context.** Decision-011 made `testin.yml` something Testin only reads, and
nothing needed it. Two things followed that Muteb did not want. Code was
generated into any code project, for whichever test project was open, even
when that code project was not about it. And the only way to make the file name
a project was to edit it by hand. Muteb, 19 September 2026: *"the codegen
operation must only work and only if [YAML exists] and project name match the
test project name"*, and *"[YAML] file can be created or updated by use new button
shown in explorer toolbar"* (#335).

**Decision.** Testin touches a test project's automation code - generating it,
Automate Test Case, Navigate to Test Method, Run Tests, the gutter, the automated
marks - only when `testin.yml` names that test project (`codegen/CodeOn`,
Rule-CODEGEN-082). One button, **Save to testin.yml**, writes the file: the open
test project and where it is cloned from, three lines, after showing them (Rule-TREE-PANEL-112 to Rule-TREE-PANEL-114).
Nothing else writes it. The test
project a tester chooses is still kept on this machine and wins until the file
names another (Rule-TREE-PANEL-106).

**Consequences.** Without the file, Testin works fully - open, clone, commit,
push - and leaves the code alone; the first write it skips says so once, with
the button. A rename or a move made while code is off leaves the code under its
old name. The writer lives beside the reader in `config/TestinYml`, so the file
still has one class (Rule-INTERNAL-089). Decision-011 is superseded.

**Amended 5 October 2026.** A second explicit gesture writes it: **Apply** in
the bugRepoUrl hint of Testin Help writes that one line and opens the file (Rule-VIEW-PANEL-104, #307). Both writers are
a tester's own click, and
`ArchitectureTest` names them; a silent write is still refused.

**If you are about to reverse it.** Writing the file anywhere but the button -
on a pick, a clone, a rename - commits one machine's choice into the team's
file without anyone deciding to, which is what Decision-011 was written
against. Generating code without the file puts one test project's methods into
a code project that is about another.

---

## Decision-014 — The tab foreground hook is experimental, and Testin uses it

*Superseded by Decision-017, 1 October 2026: the platform already gives a
Testin tab the label color, so the hook went. Left as it was written.*

**Context.** A Testin editor opens a file that is not on disk, and the IDE
paints the tab title of such a file in the "unknown file" color — the brown a
file with no status gets. Beside an ordinary Java tab it reads as a warning
about the tab rather than a name, and `7c7688fa` ("Grid view: multi-line steps,
ESC, details on ENTER") fixed it by overriding
`EditorTabColorProvider.getEditorTabForegroundColor`. That method is a default
method marked `@ApiStatus.Experimental`, so every Marketplace verification
report says *1 usage of experimental API* beside the Compatible result. The
stable way to reach a tab's foreground is the file's status, and the hook for
that lives in the VCS module.

**Decision.** `TestinTabColorProvider` keeps the override, returning
`TESTIN_TAB_FOREGROUND` for Testin's own files and nothing for every other
file, and a release carries the experimental-API line on its plugin page.
Decided 22 September 2026.

**Consequences.** Each verification report lists one experimental usage, which
is a warning and not a compatibility problem — the result stays Compatible and
the release publishes. If the platform changes the method or takes it away,
nothing throws: an override nobody calls leaves the tabs the color they had
before, so the cost of being wrong is a tab color rather than a failure.

**If you are about to reverse it.** Removing the override puts every Testin tab
back in the "unknown file" color, in every IDE and both themes. Replacing it
with a `FileStatusProvider` adds `com.intellij.modules.vcs` to a plugin whose
only required dependency is `com.intellij.modules.platform` — the reason it
runs in PyCharm, Rider, GoLand and WebStorm at all.

---

## Decision-015 — Escape closes at once, and never asks

**Context.** Rule-INTERNAL-059 says Escape closes a dialog at once and saves
nothing, and every dialog in the plugin does. A review counted what that throws
away: Create Test Case, the nine bulk editors and Result Analysis lose
everything typed, and the create and edit test run form, Update Test Case and
the commit message lose less (#66, finding 296). Two dialogs had already decided
not to ask - the failure form, where a half-written failure is worse than none (Rule-EDITOR-PANEL-144), and Report Bug
(Rule-VIEW-PANEL-070). The framework is
one class, so a question on Escape would have been written once and inherited
everywhere.

**Decision.** Escape stays immediate in every dialog, and the failure form's
reasoning is the plugin's: a dialog is escaped on purpose. Decided 23 September
2026; finding 296 was closed as decided rather than fixed.

**Consequences.** A tester who escapes a full Create Test Case dialog loses
what they typed, with no second chance. In exchange, Escape means one thing
everywhere and is answered by habit rather than read: a question that appears
on most Escapes is dismissed without being read, which costs the typing anyway
and costs a keystroke every other time. Nothing in the framework has to
remember which dialogs ask.

**If you are about to reverse it.** Do it in the framework, not in a dialog:
one dialog that asks while its neighbors do not is the worst of both, because
Escape then means two things. And keep the failure form and Report Bug out of
it, which is what Rule-EDITOR-PANEL-144 and Rule-VIEW-PANEL-070 already say.

---

## Decision-016 — Testin ships in English only

**Context.** Testin carried French and Hindi bundles beside the English one,
1,061 keys each (#11). Nothing could say whether a translated value meant what
its English original meant (#349). The platform downloads spelling dictionaries
for Russian, German and Ukrainian only, so neither language could be
spell-checked. Grazie checks French grammar and not Hindi, and only on the
machine that downloaded the model, so CI could check neither. And no tester saw
them: Decision-010 put them behind a JVM option nobody passes. In the 16 days
after the Hindi bundle landed, 116 of the 119 commits that changed the English
bundle also changed the French one, and 372 English lines became 1,101 across
the three.

**Decision.** Both translated bundles are deleted, and `messages.properties` is
the only bundle. Decided 27 September 2026.

**Consequences.** A bundle change is one file again, and every word Testin shows
can be read and checked by the gates it runs. A tester whose IDE runs in
another language reads Testin in English, which is what nearly every tester
already saw. The Translations scope, the Qodana exclusion for the bundles and
the `-Ptestin.language` sandbox property went with them, because each existed
only for the translations.

**If you are about to reverse it.** Bring a reviewer first, not a file. The
13,789 translated words are in git history at the commit before this decision,
and one revert brings them back. Every English key added since has to be
translated before they build, and `BundleKeysTest` has to assert again that
every translation answers the same keys. A translation nobody who reads the language has checked
is what this decision removed.

---

## Decision-017 — A Testin tab takes the platform's own title color

**Context.** Decision-014 kept an override of the experimental
`EditorTabColorProvider.getEditorTabForegroundColor`, so every Marketplace
verification carried *1 usage of experimental API* and the verifier gate had to
leave `EXPERIMENTAL_API_USAGES` off (#324). Read in the 2026.2.1 jars, the
platform colors a tab title from `FileStatusManager.getStatus(file)`, and a file
that is not in the local file system and not special is `NOT_CHANGED`, which has
no color, so the title gets `UIUtil.getLabelForeground()`. A Testin file is a
`LightVirtualFile` on the platform's light file system, a
`NonPhysicalFileSystem`: it is neither. The override returned that same label color.

**Decision.** `TestinTabColorProvider` is deleted, and `EXPERIMENTAL_API_USAGES`
is back on the verifier's failure level. Decided 1 October 2026, after a sandbox
run showed the tabs in the label color in both themes.

**Consequences.** The plugin makes no experimental API call, the Marketplace
page carries no warning line, and a new experimental call fails `Build` instead
of reaching it. Nothing paints a Testin tab title anymore; the platform does.

**If you are about to reverse it.** Check first why the title turned brown. The
brown is `FileStatus.UNKNOWN`, which only a file in the local file system can
get. So the cause is a Testin file that starts answering `isInLocalFileSystem()`
with true, or moves to the local file system. Putting the override back would
hide that behind an experimental call.

---

## Decision-018 — A Testin tab is a light file on the platform's light file system

**Context.** Decision-005 registered a `testin://` file system,
`TestinFileSystem`, through the `virtualFileSystem` extension point. Qodana
reports that registration as a critical: the extension point's bean,
`VirtualFileManagerImpl$VirtualFileSystemBean`, is internal to the platform,
and Marketplace does not publish a version that uses internal API. The file
system answered nothing: finding a file by path returned null by design, and
refreshing did nothing. Its one caller, `UnifiedVirtualFile.getFileSystem`,
already fell back to the platform's own light file system when ours was absent.

**Decision.** `UnifiedVirtualFile` is a `LightVirtualFile` on the platform's
light file system, and Testin registers no file system of its own. Decided 2
October 2026 (#120).

**Consequences.** The plugin uses no internal extension point. A tab keeps its
`testin:///path` URL, so two test sets with the same name in two packages stay
two tabs: the light file system's own URL is `mock:///` plus the name, and
would give both the same one. Asking the platform for a file at a `testin://`
URL still finds nothing, now because no file system answers that protocol. The
title color is unchanged, since the light file system is non-physical too (Decision-017). Tabs are restored by Testin's
own `LastOpenEditors`, and
`SaveOnProjectClose` closes them before the IDE saves, so the IDE never stores
a `testin://` URL it cannot resolve.

**If you are about to reverse it.** A file system of Testin's own cannot be
registered without the internal extension point, so a version that does it
cannot be published. Answer whatever needed it from `UnifiedVirtualFile` or
from `LastOpenEditors` instead.

---

## Superseded decisions

Each is listed here with the number that replaced it, and its section above is
left exactly as it was written.

| Decision                                                             | Superseded by | When              |
|----------------------------------------------------------------------|---------------|-------------------|
| Decision-004 — SFTP is the maintained JSch fork                      | Decision-012  | 19 September 2026 |
| Decision-007 — An unknown SSH host is refused                        | Decision-012  | 19 September 2026 |
| Decision-011 — Testin reads `testin.yml` and never writes it         | Decision-013  | 19 September 2026 |
| Decision-010 — Testin's translations are reached by the IDE's locale | Decision-016  | 27 September 2026 |
| Decision-014 — The tab foreground hook is experimental               | Decision-017  | 1 October 2026    |
| Decision-005 — A Testin editor tab is on a deprecated file system    | Decision-018  | 2 October 2026    |

---

## Adding one

A decision belongs here when reversing it would break something that is not
obvious from the code at the point where the reversal is written — that is the
whole test. A naming convention is not a decision, unless a persisted key
depends on it.

Take the next number, write the four parts, and link it from
[the home page](README.md).

---
