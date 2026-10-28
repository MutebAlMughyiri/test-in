# Changelog

What each version of Testin changed, for a tester deciding whether to update. The build turns the section of the version
it builds into the plugin's change notes, so this file is the only place they are written. The format
is [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Unreleased

## 2.16.0-alpha - 2026-10-28

Your test data carries over: this build reads and writes format 5, as 2.15.0-alpha does.

### Added

- **Robot Framework test projects:** a code project whose interpreter is Python is automated in Robot Framework. Each
  test set is a suite file and each test case a test in it, its first tag the test case UUID. Testin keeps the suites
  in step as test cases and test sets are created, edited, moved and removed, and leaves the steps a tester wrote as
  they are. Navigate to Test Method opens the test. Run Test Method and Run Tests run Robot Framework with the
  project's interpreter, and each run item is recorded from Robot's output. The mark beside a test in the gutter leads
  back to its test case. It works in PyCharm too (#2).
- **The agent works in the Terminal:** Automate Test Case hands the test cases to your command-line agent in a tab of
  IntelliJ's Terminal named after its command. The agent writes each body over the TODO itself and asks you as it goes.
  Leave print mode such as `-p` out of the agent's Arguments; the hint under the field says so (#490).

### Changed

- **A test run's dialogs show retired test sets gray:** in Create Test Run and Edit Test Run, a Deprecated test set and
  everything under an Archived package is listed gray with its status beside its name, and its boxes cannot be changed.
  A new test run never covers it, Re-create leaves out a test case there, and an edit keeps what the test run already
  covers. An empty test set is still left out.
- **A card draws a line break as a space:** an Expected Result typed on two lines no longer reads with its lines run
  together on the card. The value is stored as typed.
- **A Disabled test case's Run is gray and says why,** and the method Testin writes for a test case that is already off
  carries `enabled = false` from the start.
- **A test case with no automation says Not automated,** and several say how many.
- **An Assigned test run shows its icon alone:** the name beside it was this machine's tester, wrong on every other
  machine.
- **A comma separates groups everywhere:** the Create and Update dialogs kept "smoke, login" as one group while the
  grid, bulk edit and import made it two. Every surface makes it two now, and the Filter finds every group and module
  its lists offer.
- **Bulk edit names the key a value is stored under,** its arrows wrap like Tab, and it follows the IDE's editor font.
- **Create Test Case's strip names the field keys once,** as Ctrl+Letter Add Field.
- **A merge question about a field is headed by the field's own name.**
- **Undo and redo say Undone only once the step has worked,** and one Order change is one undo step over every test
  case it ranked, so Ctrl+Z puts the whole set back.
- **A cloned test project is named in capitals, without -test-cases.**
- **A removed test method goes to the recycle bin as a file of its own,** so its body can be put back after the project
  closes.
- **Safe mode:** in a project the IDE does not trust, every Git step says Choose Trust Project once and runs no Git.
- **The four reports draw one outline,** and the Excel report is written with Apache POI.

### Fixed

- **A step keeps its numbers and dashes:** a Steps value split wherever a number came after a space, so "Enter the
  2-digit code" became two steps, and pressing Enter on an untouched cell rewrote it and said Updated. A step is one
  line now, in the grid and in an import.
- **A sync or a Commit & Push with no branch checked out is refused** instead of pushing `main`. A pull that stopped
  earlier is offered back before the sync asks which branch, and a pull resolved from View Pending Commits pushes to its
  own branch.
- **The page keys turn the page in the grid,** as they do on the cards.
- **Going to code while the IDE indexes says it waits.**
- **A test run editor says when the test case asked for is hidden by the filter,** instead of saying nothing.
- **Automate Test Case is gray with its reason when nothing is selected.**
- **A blank row in an imported workbook is passed over** rather than coming in as an empty test case.
- **What Testin creates, removes or moves while a test project is being read stays so** when the read lands.
- **A test run name that is a path is refused** in Create, Re-create and Edit.
- **Creating or moving a node no longer lands on a sibling whose name differs only in capitals.**
- **Refresh keeps the editors answering,** and a save made during it keeps its audit.
- **A cut test case is kept when its editor closes** before the paste.
- **Jackson 2.22.4,** since 2.22.3 has CVE-2026-110879.

## 2.15.0-alpha - 2026-10-17

**Before you update:** wipe your test data. This build reads format 5 and converts nothing. Every file Testin stores is
now named in full words - a test project holds `.testproject`, a test set `.testset`, a test run `.testrun`, a test case
is `<uuid>.testcase` and a run item `<uuid>.runitem` - so a test project an earlier build wrote is not shown at all:
delete its folder and create it again. Neither file holds its UUID inside anymore, the folder files write `"uuid"`
where they wrote `"id"`, and a test case file not named by a UUID is not read; a warning names it. A test run's
configuration keys are camelCase too: `testType`, `platform`, `deviceType`. Format 5 also keeps every execution of a
run item (#415). A team sharing a test project should update together.

### Added

- **A run item keeps every execution:** recording a run item status no longer writes over the one before, so Passed
  after Failed keeps the failure's actual result, error and screenshots, and the warning that they would be erased is
  gone. The view panel's Execution Result lists one line per execution: its status, duration and Invocation n or
  Retry n, a retried attempt dimmed. Hovering a line offers Set as default and Remove this execution, and clicking it
  shows that execution's details. The default, starred, gives the run item its status: the one chosen, or
  else the last. A card shows a dot for each of the last eight executions, the default ringed. The test run clock adds
  every execution, the reports show the default with a line naming the other failed or blocked ones, and a Git merge
  joins both sides' executions (#415).
- **Sort:** a new **Sort** button beside **Filter** arranges the test cases by one field - Order, Description, Priority,
  Status, Module, Created At, Updated At, Run Item Status, Executed At or Duration - Ascending or Descending. Ties keep
  their order and empty values come last either way. Sorting writes nothing and every card keeps its number; the button
  lights up and the status bar names the sort while one is on; Reset Sort puts it back. In a test set editor the three
  run fields are gray. O and dragging still change the order under a sort, and the execution walk and light mode follow
  the sorted list.
- **A test case's status shows in its color:** on both editors' cards, in light mode, in the view panel and in the
  Create Test Run form, a Reviewed test case's description is white with a little blue (dark blue in a light theme)
  and a Disabled one's gray. In a test set editor a To Be Updated test case carries a To Be Updated badge whatever the
  Fields list shows: a plain frame like a run item's duration, its words white with a little yellow (dark yellow in a
  light theme).
- **A green dot marks the test case executing now:** it blinks after the description of the test case TestNG is
  executing, or the one the walk is on, on its card in every open editor and in the view panel, in place of the orange
  Running badge. While Run Tests executes, the test run editor's selection moves to each test case as TestNG starts
  it; the view panel follows it only while that editor is in front.
- **Select All in the test run form:** a button beside the filter ticks every test case the filter shows, or unticks
  them once all are ticked. A Disabled test case is gray, starts unticked and cannot be ticked, so a new or re-created
  test run leaves it out, and Edit keeps one the test run already covers.
- **Integration is a platform:** the test run form's Platform offers Web, Mobile and Integration. Picking Integration
  makes Component Backend and grays Frontend out, and resting the pointer on Frontend says an Integration test run
  covers the backend only. Picking Web or Mobile again lets Frontend be picked.
- **Testin API Model from JSON:** right-click a package of the automation project, choose New, Testin API Model from
  JSON, type a name such as User, paste a call's request and response, and press Create: `UserRequest` arrives as a
  Lombok class with chained setters and `UserResponse` as a record, one file each however deep the JSON nests, with
  `@JsonProperty` only where a key is not already camelCase. The boxes start three lines high, grow with the paste and
  show it in JSON colors, and each has a Format JSON button that lays the body out without changing it. The title bar
  switches them between stacked and side by side, and nothing already in the package is replaced. Designed on the idea
  of RoboPOJOGenerator by Vadim Shchenev.

### Changed

- **A test run executes one way at a time:** while Start Manual Execution is on, Run Tests in the tree is gray and says
  "Start Manual Execution is on; stop it first"; while Run Tests executes, Start Manual Execution, in the toolbar and in
  light mode, is gray and says "Run Tests is executing; stop it first", and Stop Execution ends it. A single method's
  run button no longer grays Start Manual Execution.
- **ID is UUID, and Status has the letter Z:** the column and the Fields list say UUID, the Excel report's caption says
  Test Case UUID, and the bulk edit window shows uuid. On the copy menu U copies the UUID, where I copied the ID. Status
  is Z on the copy menu and the update menu, so every field on the update menu has a letter: Z opens it after F2, or
  straight from a selected card as D does, and Status's detail line on a card and in History leads with its Z icon. A
  saved choice to show the ID column starts hidden once.
- **Every field has one letter, wherever it shows:** a card's UUID, FQCN and Path lines lead with U, F and H, the
  letters the copy menu shows, instead of their names.
- **One add key for steps and groups:** Ctrl+=, Ctrl++ or Ctrl with the numpad + adds a Steps or Group box where the
  cursor is, in Create, Update and bulk edit, and the strip says Add. Ctrl+S and Ctrl+G only open their field, adding
  a box only when it has none, and Ctrl+Enter no longer adds an item in bulk edit.
- **Bulk edit saves with Enter alone:** Shift+Enter no longer saves, and the strip names Save once.
- **Review Changes is a button:** the Uncommitted Changes dialog shows it beside its main button, and Shift+Enter no
  longer opens it. Write Over These Bodies? shows Leave them as they are as a button too, still on Escape.
- **Pending until a test run is signed off:** the Details ring, the report tiles, the report tables and the analysis
  headings call a test case nobody judged Pending while the test run is open, and Untested once it is Completed or
  Closed, as the editor's status bar already did. The report no longer says such test cases carry forward to the next
  test run.
- **Removing never deletes for good:** a removed node, test case, run item, screenshot, and a test set's class or
  package go to the recycle bin. When the bin refuses one, a Not Removed notification names it, and it stays where it
  is; before, a refused folder was deleted outright, and generated code was always deleted. On Linux, where Java has
  no recycle bin, nothing can be removed yet: every removal says Not Removed and leaves the item where it is.
- **The test sets show before the test runs are read:** on a first read or a Refresh, the tree and a test set editor
  are ready once the test cases are in, and the Test Runs folder says Reading test runs until its test runs follow,
  or Not read if the read is canceled first. A cancel now stops within a test run rather than after it.
- **A Git step ends when Git does:** a push, a sync and an aborted rebase report as soon as Git has finished, and the
  test project is read again on its own Testin indexing bar, which can be canceled, instead of leaving Pushing to Remote
  on screen for minutes.
- **The grid's Actual Result waits for a run item status:** on a Pending or Untested row it refuses and says to record
  a run item status first, since an actual result describes what was recorded.
- **Testin needs an IDE with language support:** every JetBrains IDE has it, so nothing changes there, but JetBrains
  Gateway and JetBrains Client, Code With Me's guest, no longer offer Testin: neither has a project for it to work on,
  and a Git step there would have failed.
- **Screens say test project and node:** the new test project dialog is titled Create Test Project, the report
  overview names the Test Project and the Test Run Status (it called the test run's status Run Item Status), and
  copying, moving or removing several nodes says nodes, not items.
- **The grid numbers its rows:** its first column, **#**, counts the rows as shown, 1 to n across pages, like a
  spreadsheet, in IntelliJ's own font as the tree is drawn rather than the editor font of the cells. It cannot be hidden
  and the Fields list leaves it out; Enter or a double click on it opens the details panel, as the number column did.
- **Order can be switched off:** the Order box in the Fields list is on to start with, and unticking it now hides the
  number on each card and the grid's Order column. In a test set editor, Enter or a double click on an Order cell opens
  the O dialog for that row.
- **The Details dialog closes on a click outside it:** the dialog the toolbar's Details button and the tree open about
  a node only reads, so a click anywhere else closes it, as Escape does.
- **A gray menu entry says why on hover:** every entry Testin grays out - in menus and now in the Filter list too -
  shows its reason when the pointer rests on it. The Filter menu is the same in every editor: Run Item Status is listed
  in a test set editor too, gray, and Reset Filters no longer writes its reason into its label.
- **Refresh clears the filters and the sort:** it puts the list back to Order with no filter, as a freshly opened editor
  shows it, and keeps the search text.
- **The Test Set filter works in a test run:** it lists the run's test sets in the tree's order, named by their place
  under the test cases folder as the Create Test Run dialog names them, and narrows the run to the chosen ones. In a
  test set editor it stays gray and its reason says why; the reason is no longer written into the menu label.
- **A test run is drawn test set by test set:** its test cases come in the order the Create Test Run form lists them,
  each set in its own order, and each card is numbered by its place in its own set, as the details panel and the
  generated test method already count it. The four reports list run items in the same order. A test case that was
  deleted comes last, with no number.
- **Generated packages are all lowercase:** a folder named *Home Screen* becomes the package `homescreen`, as Java names
  packages, no longer `homeScreen`. A package generated by an earlier build keeps its name until it is generated again.
- **A bug belongs to the failure it describes:** a failure's bug severity, bug priority and the issue it was reported as
  are kept in that execution, beside what happened and the screenshots, instead of on the run item. A pass or a block
  carries no bug, so the summary line, the grid and the reports show none after one; choosing the failed execution as
  the default brings its bug back, and a new failure can be reported on its own. Removing an execution removes its bug,
  and a Git merge keeps one copy of an execution both sides hold, with the issue either side reported it as (#435).

### Fixed

- **A retried or repeated TestNG method is judged on every attempt:** light mode moved on at the first attempt TestNG
  was about to retry, recorded it as Failed "Skipped/Terminated", and lost a retry that passed. Each attempt is now its
  own execution, light mode stays until TestNG moves on to the next test case, and with invocationCount any failed
  invocation fails the run item.
- **A test method's body stays as the tester typed it:** a new description, groups or Enabled, a reorder, a move, a
  copy and the undo of an edit format only the annotations Testin writes, never the body or the whole class. Undoing a
  description edit on a large class takes a fraction of a second instead of several.
- **A test set's methods count from one again after a cut or a remove:** the methods left in the test set kept their
  old priority, so removing the first of three left 2 and 3; they are numbered 1 and 2 again. TestNG ran them in the
  same order either way.
- **Undoing the removal of a test set brings its class back:** Ctrl+Z restored the test data but not the automation
  class; the class now comes back as it was, method bodies included.
- **Commit & Push of thousands of files no longer stalls:** Git's line-ending warning for each file reached the IDE one
  line at a time, so adding 27,700 new files took about 90 seconds; it now takes a few. A repository set to
  core.safecrlf=true no longer refuses Testin's commit over a line ending.
- **A slow disk no longer fails a commit that went in:** when the test runs could not be recorded in time, Commit
  Failed was shown although the commit was in, and Commit & Push skipped the push. Testin now waits up to a minute; if
  that runs out, Test Runs Not Recorded says the commit is in, and the push still goes.
- **A confirmation closes before its work:** a second Enter on a Remove confirmation still on screen ran the removal
  twice. The dialog now closes first, and a node selected together with the folder it is in goes with the folder and
  is not counted on its own.
- **Stop works while the IDE is still building:** a Stop pressed before the automation's process started let it run
  to the end; the process is now ended as it starts.
- **Closed stops a running execution:** only Completed did, so a test run closed mid-execution kept taking run item
  statuses from TestNG.
- **A status key on a test case deleted under the walk moves the walk on:** the test run editor said the test case was
  removed and stayed on it; it now moves to the next Pending test case, as light mode did.
- **Reports and exports refuse the Testin folder:** a report or an export could be saved inside it; the dialog now
  refuses and stays open.
- **The view panel fits its width:** narrowing it re-wraps its text, a row wraps instead of hiding what does not fit,
  the History tab scrolls sideways, and the panel no longer opens scrolled down. A run item status set by key, menu or
  grid shows in its Details at once.
- **Cards keep their lines when the title wraps:** a card whose title wrapped as the editor narrowed lost its Run Item
  Status and Actual Result lines, and a slightly long title left an empty line under it in a wide editor.
- **The duration badge's frame is whole under display scaling:** its top and bottom edges were clipped.
- **The editor's status bar scrolls instead of hiding:** when it was too narrow, the page size field could disappear
  with no way to reach it; the bar now scrolls sideways like the toolbar.
- **Resizable dialogs open centered:** they widened to the right of their left edge and opened off-center.
- **Testin starts completely in PhpStorm and RubyMine:** its startup loaded a test-runner class those IDEs do not ship
  and stopped part way, before its clipboard watch started.
- **Run Tests on a test run not yet open no longer raises an IDE error:** IntelliJ's build before launch reported
  "Write-unsafe context!" twice.
- **The plugin page's documentation link works again:** the documentation site moved with the GitHub account to
  `mutebalmughyiri.github.io`, and the old address is gone.
- **Automate Test Case and Run Tests are gray while code is off:** while `testin.yml` did not name the open test
  project, both entries said they needed it but could still be clicked. They are gray with the reason, as every code
  action is.
- **Reordering a test case in the grid no longer overflows the stack:** after a reorder, the list and the grid could
  each select the other's row by its old index and correct each other until the IDE reported a stack overflow. The
  grid now follows the selected test case, not its row number.
- **A test project's own folder is refused as the Testin folder:** choosing `...\Testin\test-02` instead of the folder
  that holds it left the Testin panel empty with no reason; Settings now refuses it on Apply and names the folder to
  choose.
- **A damaged Testin folder setting asks for the folder:** a stored value that is not a full path was read as a folder
  inside the open project, and the panel came up empty; it now counts as not set, and Testin Help asks for the folder.
- **A deleted test case has no Order in the details panel:** a run item whose test case was deleted showed Order 1;
  the row is now left out, as the run's card already leaves out its number.

## 2.14.1-alpha - 2026-10-10

### Fixed

- **Git works in PyCharm and every IDE where the Testin folder sits outside the project:** reading a committed test
  run's test cases no longer fails with Git's untrusted-project error. In a project still in safe mode, every Git step
  says so in one line and names Trust Project, and run items show as always.

## 2.14.0-alpha - 2026-10-09

**Before you update:** wipe your test data. This build reads format 3 and converts nothing, so every test project an
earlier build wrote is refused as written by an older Testin: remove it and create it again. Format 3 renames four keys
so each says the business word - a folder marker's `modifiedBy` and `modifiedAt` are now `updatedBy` and `updatedAt`,
and a test case's `group` is now `groups`, and a test run's configuration answer `COMMIT_ID` is now `COMMIT` - and
renames two marker files: the Test Cases folder's `.tcd` is now `.tcf` and the Test Runs folder's `.trd` is now `.trf`.
`testin.yml` spells its remote `repoUrl` instead of `RepoUrl`: edit that one line in a committed `testin.yml`. A team
sharing a test project should update together.

### Added

- **Create Test Case offers Reference:** `Ctrl+R`, `Cmd+R` on a Mac, opens it like every other field, with R on its icon
  as the update and copy menus show it. IntelliJ's Find and Replace bar no longer opens inside a field of any Testin
  dialog, where `Ctrl+R` used to open Replace.
- **A committed test run is the record:** committing in View Pending Commits makes every Completed test run of the test
  project **Committed**, and Testin commits its `.tr` with that commit straight after, in the same push. From then on
  its run items show each test case as that commit holds it, edited or deleted since, and say *Changed since this test
  run was committed* where it differs today. A Committed test run takes nothing more; F2 on one of its run items says to
  change the test case in its test set.
- **A completed test run asks to be committed:** setting a test run Completed shows a notification with **View Pending
  Commits** on it, because the commit is what makes it the record.
- **Edit Test Run lists deleted test cases:** a run item whose test case was deleted sits under *Deleted test cases*,
  ticked; untick it and save to remove it.
- **An agent writes the test method:** Automate Test Case still writes the **@Test** method and a TODO, and now hands
  the test case to a command-line agent you already run — Claude Code, Codex, Gemini CLI, pi or any other — and puts its
  answer where the TODO was. The agent is two fields in the settings, a command and its arguments. Testin sends that one
  test case, where it sits in the tree and the class its method goes into, and nothing else, and holds no key. A method
  whose body you wrote is asked about before it is written over, **Ctrl+Z** takes a written body back, and **Show what
  the agent said** opens what was asked and what came back.
- **Filter the Create Test Run dialog:** the editor's Filter menu narrows the test cases shown — every Smoke test case,
  every High priority — and a test case stays ticked while the filter hides it. **Status**, the test case's own status,
  joins the Filter menu in both editors, and **Run Item Status** joins it in the test run editor.
- **A test case id finds the test runs that ran it:** the search returns the test case and one row for each test run
  that covers it, and that row opens the test run on the result it recorded.
- **A screen reader can follow Testin:** every field, icon button, card, grid cell and view panel tab says its name, the
  branch box takes the keyboard without switching on each arrow, and moving through search results says the one reached.
- **Reference is on the update menu,** with a section and a bulk edit of its own, so every field the grid edits can be
  edited from **F2** as well.

### Changed

- **Hovering a bug badge names each half:** *Bug Severity: Major, Bug Priority: High*, on a test run's card and
  in the view panel, so `Major / High` never has to be decoded.
- **Every History card opens with what it is:** Created, Updated or Removed for a test case, Bug for a bug, which
  no longer says Bug in. A committed card adds the commit's short hash; one not committed yet shows none.
  A commit that could not be read or changed no field is Updated, with a line saying which.
- **A History card is three lines, and a click shows every change:** a test case's card shows at most two changed
  fields, what each was, an arrow and what it became, then how many more. Clicking it opens
  IntelliJ's diff window with the whole test case as it was and as it became. A Steps change names only the steps
  that changed, in History and in View Pending Commits.
- **Test data files hold only what was set:** a value equal to a fresh one is not written, so an empty field, a
  default bug severity or priority, and a passed run item's cleared failure detail no longer fill the files and
  their Git diffs. A key left out reads back as it always did.
- **A section that opens and closes says so the same way everywhere:** a blue chevron, the word Collapse or Expand at
  the end of its line, and Click to collapse or Click to expand when hovered. The Test Case band of the details panel
  now shows all three, as Create Test Run's sections do.
- **A date shows its day:** the cards and the grid of both editors, the details panel, History and a node's details
  show a date as 17-09-2026. Hover it for the full date and time, as Thursday 17-09-2026 At 06:10:00 [Asia/Riyadh].
  History's Git dates now name the IDE's time zone, not an offset such as [+03:00]. Reports, bug issues and exported
  files keep the full date and time.
- **Reference has its letter:** the update menu opens Reference with **R**, as the copy menu already copies it, and a
  card line or a History row for Reference leads with its **R** icon, as every other field does. Status, the one field
  with no letter, now sits last on the update menu.
- **A run item's duration is a badge on its card,** the same clock badge the view panel draws, beside the run item
  status, instead of a Duration line. Hovering it says Duration, on the card and in the view panel.
- **A card's detail lines lead with the field's icon, not its name:** E for Expected Result, S for Steps, T for Test
  Data and so on, the letters the update menu and History show. Hovering the icon names the field, and a screen reader
  still hears the name. A field with no icon, such as Run Item Status, keeps its name.
- **The tree is drawn in the IDE's own interface font again,** like IntelliJ's own trees, rather than at the code
  editor's size, and Ctrl and the mouse wheel over it no longer change the text size. The gesture still works over the
  editor panel, the view panel and light mode.
- **On an editor's card, a test case's priority is a bar down its left edge, not a badge:** red for High, blue for
  Medium, nothing for Low, while the Priority field is shown, in both editors, as wide as a bug card's bar in History.
  No text moves for it, and a screen reader still hears the priority. Light mode and the view panel keep the priority
  badge.
- **A priority is named High, Medium or Low everywhere:** the badge, the Priority column, the
  import and export columns, the Filter menu, the test case dialogs, the history and the HTML report. An import cell
  reading anything else is refused like any other value Testin cannot read. Hovering the bar or the
  priority badge says *Priority: High*, so it is not read as the bug's priority, which uses the same words.
- **A committed test run stays read-only everywhere:** Run on its test cases is gray and says why, and Result Analysis
  opens read-only. Completing a test run offers the commit only when its test project is under Git.
- **A test case opened from a search result can be edited:** F2 writes it to the test set that holds it.
- **A deleted test case keeps its text and its history:** its run item shows the last version Git holds instead of a
  placeholder, reads **Removed** while the test run is not committed, and cannot be changed. Its History tab shows every
  card it had, the newest one **Removed**.
- **The test run status in the status bar is its icon:** the status is in its tooltip, and beside the icon is the
  tester an Assigned test run is assigned to, or the commit a Committed one was recorded in.
- **History names a changed field by its icon:** the letter the update menu shows, with the field's name on hover, so
  a long name no longer cuts off. A field without an icon is still named in words.
- **A test run not committed shows every edit:** a run item shows its test case as it is now, judged or not. A
  Completed or Closed test run still takes run item statuses and corrections; only a Committed one refuses them.
- **Every marker in View Pending Commits is ticked and stays ticked:** a node's own file always goes with the commit.
- **The walk moves past a deleted test case:** the run item status key records nothing on it, says so, and goes on.
- **Two more guides in Testin Help:** *How to get started*, the ten-minute first run, offered on a new machine, and *How
  to collect Testin's logs*, offered when Testin shows an error or the log level changes.
- **Testin Help covers Git and automation code too:** a missing Java, TestNG or Git plugin, a missing test source
  folder, a test project not under Git and a missing remote wait on the ? as hints; **Initialize Git** is in its hint.
  A key you press still answers with a short message. Three new guides: sharing test projects over Git, resolving Git
  conflicts, and generating and running automation code. Sync and View Pending Commits offer the Git guide; Select Test
  Project and Save to testin.yml offer the linking guide.
- **Testin Help says what is not set up, instead of messages:** the ? at the end of the status bar shows in every
  project.
  It turns red, with the fix and its guide, when the Testin folder is not set, when testin.yml does not name the test
  project, when bug states cannot be read, or when board columns need gh's read:project permission; the four messages
  that said so are gone. Guides are offered where you meet their part: Sync, a failed run item, a test run opened, which
  now has its own shortcuts guide. Refresh asks for board columns again once the permission is granted.
- **The Details tab is three bands:** who the test case is, what this test run recorded, and what the test case says.
  The run item status, the duration and who executed it are pills on one line, and bug severity and bug priority are one
  chip, **Major / High**. Reference, module, order, created and updated fold behind one line that stays the way you left
  it. The exception is a link that opens its own window.
- **Escape steps back one place at a time:** in the view panel it gives the keyboard back to the editor rather than
  closing the panel.
- **Escape drops test cases you copied,** as it drops the ones you cut, so nothing is left waiting to be pasted. The
  tree already did this.
- **The report footer no longer prints a date,** in the web page, the PDF and the Word document.
- **The Details tab shows the fields the editor's Fields list shows,** from a test set or a test run, and changes as
  soon as Fields does. A field the list does not offer is always shown.
- **A test case the filter hides is named in a red refusal,** not in a blue notice that looked like the success just
  before it.
- **A choice from a fixed list is radio buttons:** the six questions of Create Test Run, Priority — Low, Medium,
  High — and Status in the test case dialogs, and the format in Generate Report and Export. A group in
  the import and export preview is a checkbox list under its cell.
- **Dialogs read as one card:** every confirmation has a button naming what it does — Remove, Move, Rename — and a move
  is one row from where a node is to where it lands. Rename says what it is renaming and where, and a place is named
  inside the test project rather than as a path on disk. A dialog you can resize opens at six tenths of the IDE window
  and scrolls rather than pushing its buttons out of sight, and a folder or file field has its browse button inside it.
- **A pasted test case lands under the selected one,** rather than beside the test case it was copied from.
- **A failure nobody triaged reads Enhancement / Low** in the grid, the Details tab, History, the Excel report and a bug
  report, and every failure is a bug card on History.
- **A narrow editor scrolls its toolbar and status bar sideways** instead of cutting an icon in half or wrapping the
  test case count onto three lines.
- **The status bars name every key that works:** **Alt+Enter** for spelling corrections, **Ctrl+V** for a screenshot,
  and **Ctrl+Letter** to add a field in Create Test Case. On a Mac, **Ctrl+Enter**, **Ctrl+D**, **Ctrl+E**, **Ctrl+S**
  and **Ctrl+T** are Cmd keys, and the hint reads **Cmd+Letter**.
- **The IDE draws what it already knows how to:** the selection, the striped rows, the bulk editor's diff tint and the
  toolbar buttons follow your theme.
- **A gray dialog button says why when you hover over it:** the reason is its tooltip in every dialog, and the line
  beside it only counts what pressing it would act on.
- **A hint or a path sits right after the name** in search results and picker lists alike, instead of in a column.
- **One name for Navigate to Test Method:** the view panel's first button reads **Navigate to Test Method**, as the menu
  and the card do. Its icon still says what automation is there.
- **A blank priority in a bulk edit is Low,** the default priority, and a blank value in a bulk edit is never
  counted among the values Testin could not read.
- **History tells a test case's bugs from Git:** each time a test run recorded, changed or cleared a bug on the test
  case, History shows a bug card of its own beside the test case's entries, with its bug severity, bug priority and
  filed issue. The test run's name opens that test run with the run item selected.

### Removed

- **The Open Bugs tab:** the view panel has two tabs, Details and History, and History now shows every bug any test run
  recorded, read from Git rather than from this machine's memory.
- **English only:** the French and Hindi translations are gone. Nobody who reads either language had checked them, and
  no tool could, so Testin now shows only words that have been read.

### Fixed

- **Saving no longer fails when the IDE or a virus scanner is reading the file at that moment:** on Windows a file
  being read cannot be replaced, so Testin waits a moment and tries again rather than reporting the save as failed.
- **Every test run recorded in one commit shows its own test cases from it,** not only the first one read. A Committed
  test run whose commit is not read yet keeps the run item status of a deleted test case instead of reading Removed.
  A commit that Git failed to read once is read again rather than remembered as empty.
- **A Committed test run is read from the right commit after a rebase:** when a pull rewrote the commit it names, as
  Commit & Push and Sync do once a teammate pushed first, it is read from the commit the rebase made, on every clone.
- **A record commit that fails says your commit is in,** instead of reporting the whole commit as failed.
- **A test run becomes Committed only with all of it in the commit:** its own files and its test cases' changes. One
  with a change left unticked stays Completed until a later commit holds it.
- **Result Analysis saves nothing onto a test run that became Committed** while the dialog was open, and completing a
  test run in a test project not under Git says Completed.
- **The ? keeps a hint until its own step is done:** a bug repository read fine no longer clears the hint another
  could not be read for, no bug link left clears it, a Git hint about one test project is not cleared by another,
  Sync clears the no-repository hint once the folder is a repository, and choosing the test project testin.yml names
  clears the code-off hint. A hint with a link wraps to the hint's own width.
- **A read the IDE canceled is no longer logged as an error.**
- **The Medium introduction links to the First run guide where it is now.**
- History opened from a committed test run no longer shows an uncommitted card made of the commit's own text:
  the History tab always reads the test case as it is now.
- **An imported test case is new:** a JSON file no longer brings its status and its order into the test set that imports
  it.
- **Only what you remove goes to the recycle bin:** a cut and paste, or a file Testin replaced, no longer leaves test
  case files in the desktop's bin.
- **G** and **S** in the test case dialogs put the caret in Group and Steps, and Group shows the hint *set group*.
- A second IDE window on the same test project sees what the first one writes. Undo and redo of a rename still work
  after a Refresh, and a test project rename that fails on disk leaves the automation package where it was. A token
  typed into a clone address no longer stays in the cloned repository, and Save to testin.yml previews the text it is
  going to write.
- A failure with no message, or a bug report that cannot be written, no longer shows the word *null*.
- **Large test projects:** an Excel export and an Excel import take far less memory, Cancel stops a report within a row,
  and the tree redraws only the folders that changed.
- **Ready for the next IDE:** Report Bug no longer calls a platform method that is scheduled for removal, and Testin no
  longer registers a file system through an internal platform API.
- On a Mac, **Cmd+D** in light mode shows and hides the details every time, not only the first.
- **Every Testin key reaches Testin on a Mac:** Create Test Case is **Ctrl+Alt+Cmd+M**, where **Cmd+M** minimized the
  window, and paging is **Ctrl+Alt+Cmd+Right** and **Left**, with **Shift** for the ends, because **Ctrl** and an arrow
  switch desktops. Generate Report is **Ctrl+Alt+P**. In the test case dialogs, Module is **Ctrl+Alt+Cmd+M**, Pre
  Conditions **Ctrl+Alt+Cmd+B**, Priority **Ctrl+Alt+P** and Group **Ctrl+Alt+G**. Automate Test Case is
  **Ctrl+Alt+Cmd+F12**, where **Cmd+F12** opened the file structure. Neither macOS nor the IDE uses these keys. The
  IntelliJ IDEA Classic keymap gets the same Mac keys as the macOS one. Windows and Linux keep their keys.
- The web page report keeps the line breaks of a test case description, and the Excel report heads its test case column
  **Test Case**, as the web page, the PDF and the Word document do.
- **A committed test run keeps what it recorded:** on a Committed test run, **P**, **F** and **B**, the failure details
  and the grid's Actual Result are gray and say why, as the status bar claims.
- Renaming a test set, a test set package or a test project renames its automation class or package again, and so does
  undo the rename: the code was looked up under the new name after the tree already carried it, so it was never found.
- A search or filter that matches nothing in a test run reads *No test cases match the search*, as it does in a test
  set, instead of *Loading...*.
- **A skipped or terminated test says so:** a TestNG test skipped by a failed dependency or an excluded group, or ended
  by a stop, is recorded Failed with *Skipped/Terminated* as its actual result, rather than with none.
- **Filling in the description of several test cases at once writes the method** of each that had none, as filling in
  one already did.
- **Testin runs only its own run configurations:** an execution is named *Testin:* and then the class and method, so
  Testin no longer reuses or changes a run configuration you made, and its Stop no longer ends a run you started from
  the gutter.
- **Escape on a merge question skips that file and asks about the next,** as its status bar says, instead of ending the
  questions with the files after it unasked.
- A drag whose new order cannot be written to a test case file says *Could not save* and puts the cards back as they are
  on disk. It used to say **Re-sorted** and keep an undo entry for an order no file holds.
- A marker that will not parse is named under the test project that holds it, not under whichever test project finished
  reading next.
- **A failed Git step says what to do next:** when Sync, the push after resolving conflicts, or reading the branches
  fails, the message offers **Show Git log**, which opens the IDE's Git tool window with Git's own output.
- **A test set's marker file is read as JSON,** not as TypeScript: *.ts* and the other marker files no longer show red
  errors when opened in the IDE.
- **Import keeps a test you wrote by hand:** when a test case's description matches a handwritten **@Test** in its
  class, Testin tags that method with the test case. It no longer writes an empty method beside it.
- **Removing a test case, test set or package waits for indexing to finish** when it has automation code. It used to
  remove the test case and leave its **@Test** method behind.
- **A rename whose folder cannot be renamed changes nothing:** the automation code takes its old name back and the test
  project keeps its name. It used to leave the code renamed and the folder not.
- **A test run's details that could not be saved do not linger:** the test run shows what is on disk again and says it
  was not saved.
- **A move the tree refuses leaves the automation code where it was:** the class goes back to the package of the test
  set it belongs to, rather than staying in the package the test set was never moved to.
- **An imported sheet's test set is named without its special characters:** a sheet called Log/in: Flow is
  imported as **Login Flow**, not Log_in_ Flow.
- **A test case or result the review cannot read still gets its own kind of row,** decided by the file's name.
- **Clearing a test case's description records the empty description in its @Test** and keeps the method's name.
- **The mark beside a test method is drawn while the IDE is still indexing,** rather than failing until it finishes.
- **Ctrl and the mouse wheel change Testin's text size over the tree** as they do over the editor and the view panel.
- **A sheet named only with special characters imports as Imported sheet,** rather than as a test set with no name.
- **The search draws a test run with its status icon,** the icon the tree draws for it.
- **Dialogs keep to their own measures:** captions two points below the label font with a hairline to the card's edge,
  the confirmation message and the dialog button in the dialog fonts, a half-height dialog at half the frame, a table
  eight rows tall, every gap one of the five spacing steps, and a shortcut key with no room left out rather than cut.
- **Refresh lands on the selected test case:** reloading or refreshing a test set opens the page that holds the test
  case you had selected, and keeps it selected.
- **Copy Test Case works in the grid,** as it does on a card.
- **A gray menu entry says why:** View Test Case Details, Copy Test Case, Cut Test Case and Navigate to Test Case say to
  select a test case first.
- **A narrow editor's toolbar and status bar scroll sideways with the mouse wheel,** not only with a sideways swipe.
- **A run item status records how long the test case took,** to the millisecond rather than in whole seconds: from
  the moment it is selected until its run item status is set, by hand or by the automation.
- **Two test projects with one name open the one under your Testin root,** rather than whichever was read first.
- **A dialog on a very small IDE window stays inside it,** rather than growing past its edges.
- **A test project that cannot be read all the way says so and is read again on the next refresh,** instead of
  being treated as read with part of it missing.
- **A save cut off part-way leaves the file as it was:** Testin writes beside the file and moves the result into
  place, so a crash or a full disk can no longer leave a test case or a marker empty.
- **Test cases whose methods live in two modules no longer run on one module's classpath:** nothing starts, and one
  message names the modules, so each module's test cases are run on their own.
- **The screenshots pasted with a failure show in the view panel again,** even when Fields hides the Stacktrace
  link. Only the link follows that field.

## 2.13.0-alpha - 2026-09-22

- **Before you update, two things:** this build needs **IntelliJ 2026.2 or later** — it runs on Java 25, which a 2026.1
  IDE cannot load — and it converts your test data once, the first time each test project is opened. Test cases and
  markers are brought forward and **every test run is removed**, because a test run written by an older build names its
  results in a way the new one cannot read. A message says how many test cases were converted and how many test runs
  went. A team sharing a test project updates together, and a project nobody converted is refused from 2.14.0-alpha on,
  naming the release that can still convert it.
- **Sharing over SFTP is gone:** Git is the one way a test project is shared. A project that was shared over SFTP keeps
  its files — put them in a Git repository and set **RepoUrl** in **testin.yml**. The settings page, the panel and the
  dialogs no longer offer a host, a port or a key file.
- **One file per thing on disk:** a test case is **\<id\>.tc**, a test run's own facts live in its **.tr**, and a test
  run's results are one file per test case rather than one file for the whole test run. Renaming a test run keeps its
  results, because nothing is named after the folder anymore. Two testers judging the same cycle no longer overwrite
  each other: the results merge file by file, and where both judged one test case the run item recorded later wins
  whole — its run item status, its actual result and its stacktrace together.
- **testin.yml is read, and written by one button:** **Save to testin.yml** in the panel's title bar is the only thing
  that writes it, so a repository never gains a committed file nobody chose to add. The file names the test project a
  repository drives, and the automation code stays off until it does — the entries that need it say so and gray
  themselves. Everything else works without it.
- **Rename a test project from the tree:** the folder on disk and the automation package follow the new name, and
  **Ctrl+Z** brings both back. A rename that cannot finish says which part refused and changes nothing.
- **Light mode, as a test case on its own:** the card's buttons for the test case on screen are on the window, each one
  also an entry in the **View** menu with its key. Fields are named by their icons rather than captions, **Ctrl+D**
  shows and hides the details, **F** opens the failure form, and the test set's name sits in a rounded frame.
- **Every field says which key opens it:** in the create dialog, the update dialog and the copy menu, each field carries
  its key's letter in a frame beside its caption, and each caption sits above its field. An empty description is refused
  with its hint and its letter turned red.
- **The Details tab reads like a form:** every caption sits above its value, a pasted screenshot shows as a thumbnail
  that opens at its real size, and the bug row carries what was reported.
- **Navigate to Test Case, drawn in green:** every card in a test run shows it, and so does the test run editor
  right-click menu. A test set does not need it, because its test cases are already on screen.
- **A run item status stays with the test case it was given against:** a judged row shows its run item status whatever
  later happened to the test case — renamed, moved or deleted — and a row whose test case is gone says so by name rather
  than disappearing.
- **A revert can be taken back:** reverting a change in Pending Changes is itself undoable, and reverting a test case
  that was never committed removes the test method generated for it.
- **The gutter follows the test project you chose**, not only the one **testin.yml** names, so the run icons appear
  beside the generated methods as soon as a project is bound.
- **The tree reads a folder set after the IDE started:** setting the Testin folder and then opening the panel used to
  leave it reading forever, until a Refresh. It reads on the first open now.
- **Words a tester reads:** a test case is called a test case everywhere — the report's **Total Test Cases** tile, the
  confirmation for a run item status set in bulk, light mode's counter. The failure form's hints end in an ellipsis, the
  preconditions hint reads *set preconditions*, the folder example reads *Example: C:\Users\\...*, and a pull that stops
  on a conflict offers to undo the pull.
- Tested by **Mohammed AlZamil**.

## 2.12.0-alpha - 2026-09-16

- **Before you update:** three things an older build wrote are not read. A test run recorded with 2.10.0-alpha or
  earlier kept its results in a file named after its folder, which is why renaming a test run lost them; results live in
  **run.json** now, so such a test run opens with no results. A test project is Active or Inactive — Archived is gone —
  so a project still marked Archived is reported as damaged. And a screenshot pasted with 2.11.0-alpha was written
  inside **run.json** as letters; screenshots are picture files beside it now, so those test runs show none. Remove such
  a test run rather than repairing it, and a team sharing a test project should update together.
- **A screenshot of the failure, kept as a picture:** **Ctrl+V** in the error box of the failure form pastes whatever is
  on the clipboard as a small picture under the box, and the **x** on it takes it out. Each one is a PNG file beside the
  test run, named by five characters of its own. So the test run's file stays small however many are pasted, and Git and
  a server sync carry them as the files they are. The Details tab links each one and opens it at its real size, no
  report prints one, and Report Bug attaches them to the GitHub issue.
- **The actual result is spell checked:** a misspelled word in what actually happened is underlined as the IDE
  underlines one anywhere else, and **Alt+Enter** offers the corrections and Save to dictionary — in the failure dialog
  and in light mode's form. Nothing is corrected unless you pick a correction: what is saved is what you typed.
- **The page size is remembered:** the size typed into the box at the right of the status bar is kept, and every test
  case editor and test run editor opened afterward starts with it, after restarting the IDE too. An editor already open
  keeps its own.
- **The view panel takes the keyboard:** **Tab** moves between its three tabs, a click anywhere in the panel brings the
  keyboard to it, and the mouse wheel scrolls Details, History and Open Bugs. Each page of the panel says what the panel
  is called and where **Escape** closes it.
- **Fifty-two fixes from reading every package.** The ones a tester meets: a drag says **Re-sorted** only once the new
  order is written, and a drop that moved nothing stays silent; copying no longer loses a test case waiting to be cut,
  and a paste no longer moves the cut test cases instead of the copied ones; undoing a cut and paste after the source
  test set was renamed no longer deletes the moved test cases; a half-written failure in light mode survives another
  test case reporting its result; a branch named **feature/login** is checked out under that name rather than as
  **login**; a sync with two conflicts asks about both; correcting a cell in the export preview no longer edits the test
  case behind it, and Cancel leaves it as it was; the browse button beside the SFTP key file opens a chooser; a bulk
  update says how many test cases it wrote; and a name Windows forbids is refused with a message rather than an IDE
  error report.
- **Quieter about what it did:** a canceled sync no longer reports itself as failed, a failed push is no longer followed
  by "up to date with the remote", and a rebase that cannot continue for another reason is no longer offered as a
  conflict with no file in it.
- **Steadier underneath:** a Refresh while a project is still being read no longer redraws over half of it, a clone is
  no longer scanned on the thread that draws the IDE, and the Testin folder setting is no longer written back on every
  project open.
- **How Testin works, written down:** [mutebalmughyiri.github.io/test-in](https://mutebalmughyiri.github.io/test-in/)
  has a page for each
  thing a tester does — 154 of them, each with the rules it keeps — a page of every shortcut, the formats on disk, and
  ten minutes from installing Testin to sending a report.
- Tested by **Mohammed AlZamil**.

## 2.11.0-alpha - 2026-09-14

- **Before you update:** two things an older build wrote are not read. A test run recorded with 2.10.0-alpha or earlier
  kept its results in a file named after its folder, which is why renaming a test run lost them; results live in
  **run.json** now, so such a test run opens with no results. And a test project is Active or Inactive — Archived is
  gone — so a project still marked Archived is reported as damaged. A team sharing a test project should update
  together.
- **Report a failure as a GitHub issue:** a failed test case in a test run has **Report Bug** in the Bug Issue row of
  its Details tab. Testin writes the bug in one template from the test case, its test run and what the test run
  recorded, and opens it to be read and edited. **Send** files it with GitHub's **gh** command line tool, with the
  screenshots pasted into the error attached. The issue stays linked beside the result, and a test case already reported
  cannot be reported twice. **bugRepoUrl** in **testin.yml** names the repository; Testin stores no password and no
  token.
- **Every report links the bug:** the PDF, HTML and Word reports show a reported failure's issue as **(#12)** right
  after its actual result, and the spreadsheet gives it a Bug Issue column of its own. Clicking it opens the issue.
- **Light mode:** one button on the test run editor's toolbar, drawn as a sun, opens a small window that stays above
  every other window and shows one test case at a time — so a tester working in the application under test records a run
  item status without going back to the IDE. It carries the run item status buttons, Start and Stop, the test case and
  test run clocks and the failure form; **Ctrl+D** shows the rest of the test case and the wheel changes the text size.
  It moves between test cases instead of snapping, and holds still when the IDE's own Animate windows setting is off.
- **Every action is in Find Action and the Keymap:** one of sixty-six actions was declared to the IDE, so the rest could
  not be searched for or given a key. All of them are declared now, and a menu prints whatever key the keymap holds,
  including one you rebound. On macOS the tree cuts and pastes with Cmd, as it already copied.
- **Change a test run after it has started:** Edit Test Run, on an open test run, adds and removes test cases, renames
  the test run and changes its configuration, keeping the run item statuses of the test cases it still covers. Renaming
  a test run no longer loses its results, and a test run can be removed whatever its status.
- **A group is a word you type,** completed from the groups the project already uses, instead of eight tick boxes.
- **See what is automated:** the navigate icon on a test case says whether it has test code, the filter asks it of a
  whole test set, and a test set's details say how much of it is automated.
- **The grid answers the keyboard:** Enter edits a cell or says why it cannot be edited, Ctrl+C copies the cells you
  picked, the Context Menu key opens the menu, and pasting a block says so once, with a count.
- **A test run keeps the work done in it:** judging every test case a filter showed no longer turned the hidden ones
  untested and completed the test run; Start on an empty filter no longer completes it; closing the tab stops the
  automation instead of leaving it recording into nothing; a repeated automated execution that clears failure notes
  written by hand says what it cleared. Escape closes the failure form without asking, and what was typed is gone.
- **Nothing lost around it:** undoing a cut and paste no longer deletes the test cases from both test sets; Refresh and
  selecting a hidden test case keep the filters and the search; Escape no longer empties the system clipboard; an import
  that fails part way says what it already wrote; exporting a package reaches every level under it; copying a test set
  whose files were named by hand gives each copy an id of its own; and test cases in a folder with no marker are named
  in a warning instead of silently skipped.
- **Syncing keeps what you are doing:** a Git sync refreshes the editors it changed, and a file changed outside Testin
  is read when it arrives, including a hand edit made seconds after Testin saved. A run item status, a grid edit or a
  completed test run recorded while an SFTP sync brings that test run in lands on the test run that arrived. Cancel
  stops an SFTP sync and an import, Keep on a file the server deleted keeps it, and the SFTP password box shows dots.
- **Undo is one gesture:** Ctrl+Z takes back a drag together with the test code it moved, removing forty test cases is
  one entry, and an undo that is refused does not spend the press.
- **Generated code:** two descriptions that differ only in punctuation no longer share one method, a folder named for a
  Java keyword no longer generates a class that will not compile, a drag no longer moves Testin's methods above a
  **@Test** you wrote, and a method's name no longer depends on the machine's language.
- **Testin says what happened:** places that refused or acted in silence now say so, once; an automated execution
  reports one line at the end rather than one per test case; every action carries an icon; the two Testin tool windows
  have different names; and the settings page answers the search and refuses a Testin folder that does not exist.
- **How Testin works, written down:** [mutebalmughyiri.github.io/test-in](https://mutebalmughyiri.github.io/test-in/)
  has a page for each
  thing a tester does — 153 of them, each with the rules it keeps — a page of every shortcut, and ten minutes from
  installing Testin to sending a report.
- **French and Hindi:** every message Testin shows is translated. The IDE offers neither language in its settings, so
  they show when the IDE runs with the JVM option **-Duser.language** set to French or Hindi.
- The source is licensed under the Apache License 2.0.
- Tested by **Mohammed AlZamil**.

## 2.10.0-alpha - 2026-09-02

- **Order is the rank a test case carries, and nothing else:** test cases used to store it as a chain across their
  files, and 2.8 and 2.9 rewrote that chain as a rank on each test case the first time they indexed a set. The converter
  is gone, and with it the only place indexing wrote test data — a scan is a pure read again. A test case that arrives
  with no rank, whether from an old project, an import or a file copied in by hand, sorts after the ranked ones, oldest
  first. That is what an unranked test case has always done. Nothing is hidden and nothing is refused.

## 2.9.3-alpha - 2026-09-02

- **A badge looks like what it is, instead of saying so:** a pill spent half its width naming the field its color
  already meant — "Priority: High" beside "Bug Priority: High" and "Bug Severity: Major". Priority is the value now, and
  it is **P1** and **P2** rather than High and Medium, so it cannot be read as the bug's. P3 draws nothing at all,
  because P3 is what a test case is unless somebody said otherwise. Groups are ribbons rather than pills, since eight
  identical pills on one row was the hardest row in the plugin to read. Severity and bug priority are one badge carrying
  the IDE's own bug mark, and each half leaves on its own when its column is unticked.
- **The next cycle starts from the one before it:** Re-create, on a test run in the tree, opens the create dialog
  holding the previous cycle's test cases and its configuration with the next name suggested — cycle-1 offers cycle-2.
  Building cycle 2 no longer means re-ticking every test case and retyping every answer. No run item status, duration,
  actual result, stacktrace or bug field is carried across.
- **A disabled test case stops running:** Disabled is the one status that says whether a test case should run, and it
  said it to Testin alone — the card showed it and the suite ran the test case anyway. The generated **@Test** carries
  **enabled = false** now, and leaving Disabled takes the attribute off rather than writing the word that says nothing.
- **A test case you only looked at is not a test case you edited:** opening a field in the update dialog and pressing
  Enter without changing anything recorded the test case as modified — Modified By became whoever pressed Enter, and the
  file was rewritten. The stamp is now decided by whether the save would leave the file different, in the one place
  every save passes through, so the update dialog, a grid cell, the details panel and a paste are all covered.
- **A description nobody wrote is empty, and names no method:** a blank description was stored as the literal
  **EMPTY_DESCRIPTION**, which the tester then read as the test case's name on the card, in the grid, in every report
  and in the execution log, with a generated method called emptyDescription. It is stored empty and drawn blank. Writing
  a description on such a test case creates its method then, and the second unfinished test case in a set no longer
  collides with the first.
- **The search has a button:** the keystroke reaches the search from anywhere, which is also why nothing on screen said
  the search existed. The project tree's toolbar carries it, under the magnifier the dialog uses, with a tooltip naming
  whatever key the keymap currently holds — including one the tester rebound.
- Editing a grid cell re-measured every row on the page, and dragging a column divider did it again on every pixel of
  the drag — nine hundred cell layouts on a fifty-row page. The edit measures the rows that changed, the column gestures
  coalesce, and the cell borders are constants instead of two thousand seven hundred objects per pass.

## 2.9.2-alpha - 2026-09-01

- **Ctrl+Z reaches the test case, on the surface it is pressed on:** undo reached a node move and a node rename and
  nothing else, so a Remove wrote the deletion straight through and a bulk edit rewrote forty files in one gesture that
  could not be taken back. All of it is undoable now, and every test case editor keeps its own history beside the
  tree's — a tester with two editors open takes back what they did here, not whichever change happened to be last.
  Removing a node is undoable at last. A restore never overwrites work it did not remove, and never stamps the tester as
  having modified a test case at the moment they un-modified it.
- **Deleted test data goes to the recycle bin:** a removed test case was a file erased and a removed test set a folder
  erased with everything under it, with the confirmation dialog the only thing between a misclick and typing the work in
  again. Both go to the desktop's own bin now, found in Explorer and restored the way every other file is, without
  needing this plugin to be working.
- **A long card title wraps instead of running off the card,** in both the test case editor and the test run editor. The
  title is a wrapping text area rather than a label, so a description holding a **\<** is the tester's own sentence
  rather than markup that swallows the rest of the line.
- **Test data is shown as it was typed, commas and all:** a rendering rule written for comma-separated lists turned
  every ", " into a line break on the way to the screen. Test data is a username, a query or a payload — values that are
  used rather than read — so each comma vanished between the save and the details panel while the file on disk had kept
  it all along.
- **A bulk edit stops flattening the value it saves:** a multi-line expected result or test data edited in a bulk dialog
  was written back as one line, silently, with nothing to undo it. A line break is now written as the two characters
  that stand for it, so the value still sits on one editor line and comes back whole.
- **Generated code follows the test case in and out:** undoing a removal used to put the JSON back and leave the method
  deleted, and redoing a creation left the method standing with no test case. An import over a class automated by hand
  stopped appending stubs beside the real tests. The generator matched method names exactly, so "verify NEXT db value"
  did not recognize verifyNEXTDbValue, and 84 of one class's 150 descriptions got an empty duplicate.
- **Refresh says it refreshed** when the test cases are back on screen, rather than when the button went down. The read
  waits for indexing and finishes on another thread, so a balloon at click time announced a refresh that had not
  happened.
- **Double-clicking a card leaves the focus on the details tab,** so F2 opens the test case for editing straight away
  instead of doing nothing until the tester clicks into the panel first.
- FQCN and Path start visible on the editor toolbar, so the editor shows where a test case's automation lives and where
  the test case sits in the tree without being asked.
- **Stability:** the plugin's one private interface method compiled to a call the JVM is entitled to reject — an error
  waiting for a JVM that checks, which the Plugin Verifier reported against all four IDEs. It is inlined, and the result
  is Compatible everywhere with no compatibility problems.

## 2.9.1-alpha - 2026-08-30

- **The stop no longer depends on internal API:** finding the process behind an execution went through a method the
  platform marks internal, which can change or disappear in any release with no warning - and it was the load-bearing
  call in the only feature that can stop an execution. The execution now keeps the handle the IDE offers it as each
  process starts, so stopping uses what it was given rather than asking the platform to find it again.
- The grid stopped detaching a hover listener it no longer needed: the cell colors are already set after the platform
  has had its turn, so the tint it was there to prevent could not appear anyway.
- Every compatibility check the IntelliJ Plugin Verifier offers now fails the build, rather than the two it had been
  narrowed to.

## 2.9.0-alpha - 2026-08-30

- **Zero compatibility problems, on every IDE:** the Java and TestNG code moved into content modules the IDE loads only
  where they apply. The plugin page reported 159 problems on PyCharm, GoLand and WebStorm — 53 apiece — because the
  verifier could not see past a runtime guard. It now reports none, on all four.
- **A test run records what happened, not just that it happened:** run item statuses are given from the keyboard in the
  grid, the tester types the actual result beside them, and a failure keeps its stacktrace whole. Each test case is
  timed — including one that passes inside a second — and the timings survive stopping, resuming and reopening. The
  details panel shows what the test run recorded next to what the test case says.
- **Start a test run from the tree:** a test set or a whole package starts a test run and the run item statuses land in
  it. One runner for one test case and for a hundred, so a selection compiles once and executes in the order the tester
  arranged.
- **A stop that really stops:** the process is killed rather than asked to end, a test case that never started is no
  longer claimed by the test run that asked for it, and a test run is over when every test case has a run item status,
  whoever gave it.
- **Execution order belongs to the tester, and the code follows it:** a test case carries its own position instead of
  naming its neighbors, so dragging one test case writes one file rather than renumbering the set. The generated
  **@Test** carries that position — priority is a Testin field again, shown, filtered and reported, and no concern of
  the automation. Order also joins the update menu, so a test case can be moved to a place by number.
- **A filter no longer renumbers the set:** cards and grid rows show a test case's place in its test set, which stays
  the same with a filter on. Dragging while a filter was on used to move test cases the tester never touched, and save
  them.
- **A test project can live on a server, with no Git at all:** **testin.yml** says where. A sync claims the project,
  merges, and gives the lock back even if it dies mid-claim; a test case one side deleted and the other edited is left
  to the tester instead of stopping the whole sync.
- **Git that survives a conflict:** a conflicted test case is merged field by field and usually asks nothing. A failed
  push leaves a way forward and names the files; switching branch asks before carrying uncommitted work across, and open
  editors follow the index rather than the branch they were opened on. A rename is committed as a move rather than as a
  new file beside the old one.
- **One key finds anything in the test project** — a test case, a set, a test run, a node — and goes there.
- **The test case dialog, field by field:** Expected Result is multi-line, wears the same frame and focus ring as the
  surrounding fields, and grows as lines are added. Every field is set in one size. From a selected card, each field's
  letter opens that field's editor with no menu in between. A bulk edit writes the rows the tester actually edited and
  leaves the rest byte-identical.
- **Imports and copies stop freezing the IDE:** an import that took 49 seconds with the processor idle now waits for the
  index once and says where the time went, and a set of test methods is generated as a set in one edit instead of one
  method at a time. Copying a test set runs under a progress bar.
- **Reports that agree with each other:** the three formats of one test run now say the same thing, a report says what
  it is from its file name, a test run named "Sprint 7 R&D" no longer breaks its own report, and what the tester writes
  about a result is kept with the test run and rendered into every format.
- **Generated code that compiles:** a description with a comma in it, two descriptions that differ only in punctuation,
  and a quoted phrase each used to produce a class that would not build. Go To Code opens the method that carries the
  test case rather than one named like it, and a description that cannot name a test method is now refused where it is
  typed.
- **Credentials stay out of the log:** an HTTPS remote can carry a token in its URL, and Git echoes that URL when it
  fails. It no longer reaches **idea.log** or the balloon on screen.
- **Stability:** the tree no longer waits for the index while holding the read lock, which could take the IDE down with
  it. Two open projects no longer share one view panel or one editor zoom. Typing in the Status cell no longer throws,
  and a corrupt manifest no longer reads as though the tester had deleted everything.
- **One word for one thing:** a refusal is red and an outcome is blue everywhere, and every status is captioned by the
  enum that owns it — so renaming one reaches the reports, the tree, the grid and the details panel together.
- Tested by **Mohammed AlZamil**.

## 2.8.0-alpha - 2026-08-19

- **Every change is reviewable, and the review commits and pushes:** the pending-changes dialog now understands test
  runs and markers as well as test cases, so an executed test run no longer shows as a nameless row and a test run
  edited after its first commit no longer disappears from the list entirely. A Test Set column says where each change
  lives, and the button is **Commit & Push** with Commit alone behind it — the push is chosen with the changes still on
  screen, and the notification names the commit it sent.
- **Removed:** a test run keeps the row for a test case deleted under it, marked Removed, carrying the run item status
  and timings it recorded. The row takes no new run item status — a test case that is gone cannot be run — execution
  steps over it, and the reports count it in a table of its own.
- **Removing a test project works again** from the tree, behind a confirmation that says how much goes with it: "Holds 3
  test sets, 42 test cases and 5 test runs".
- **Dates read the way they are written:** exporting a sheet and importing it back kept every date, instead of turning
  each one into today. An imported test case now keeps the author and dates its file carried rather than being stamped
  with whoever ran the import.
- **Audit that tells the truth:** all seven node markers share one audit block, every one of them reads a file written
  before the fields were renamed, and a node nobody has modified reports when it was made instead of the moment it was
  read.
- **Lighter lists:** a card reuses its badges instead of rebuilding them for every row, so sorting, filtering and paging
  a full page of test cases costs a fraction of what it did.
- **Quieter, clearer messages:** import, export, report and sync outcomes appear as status-bar balloons instead of
  entries in the notification log, which is left for the failures worth finding again. Syncing a project that is not
  under Git says so plainly and points at where a repository is created.
- **Shown, not broken:** in IDEs without the Java or TestNG plugin, Run Test Case, Run Tests, Automate Test Case and
  Navigate To Code stay on the menu, grayed, with the plugin each needs written into its name, rather than appearing and
  refusing.
- The Git identity and group-selection dialogs moved onto the shared dialog framework, so every key they bind is shown;
  an empty list says why it is empty; and a marker that has not been written yet is no longer logged as an error.
- **Git on a repository of its own:** test cases no longer have to live inside the automation project. The pending
  review reads Git directly, so a newly written test case shows up straight away, the markers that make a test set a
  test set travel with the commit, and a first push to an empty repository succeeds.
- **A real review dialog:** pending changes open in a table with a commit message field, a status bar showing the keys,
  and right-click to revert a single changed field.
- **Execution timing:** a test run records when it started and ended, per-test-case durations survive stopping and
  resuming, and all three report formats show a real execution date instead of the day the test run was created.
- **Bug severity and priority as badges** on the test run card, each captioned so it cannot be mistaken for the test
  case's own priority, and readable in both light and dark themes.
- Tested by **Mohammed AlZamil**.

## 2.7

- **Multi-IDE Support:** Java, TestNG, and Git are now optional dependencies — Testin installs and runs on PyCharm,
  GoLand, WebStorm, and other IntelliJ-based IDEs. Features that need a missing plugin degrade gracefully with a clear
  notification instead of failing.
- **Accurate Metadata:** Plugin metadata is declared once in the build; the compatibility statement is verified against
  each supported IDE by the JetBrains Plugin Verifier.

## 2.6 - 2026-07-05

- **Excel-Style Grid:** Row selection from the sequence column, multi-row and multi-cell selection, Excel-compatible
  copy/cut/paste, and automatic row-height fitting after paste.
- **Layout Memory:** User column widths survive refreshes; the list view opens by default and loads pages
  asynchronously.
- **Safer Persistence:** Run item status writes go through a single sequential writer, eliminating corrupted JSON under
  rapid status changes.
- **Zero-Setup Automation Path:** The Java test source root is detected automatically at startup — no manual
  configuration.
- Many stability fixes: dialog lifecycle cleanups, unified keyboard shortcuts, and grid editing at its edges.

## 2.5 - 2026-06-27

- **Grid View:** Content-based automatic column sizing with word wrap, uniform cell padding, and inline editing.
- **Test Run Creation Redesign:** Collapsible sections, clearer layout, and wider input fields in the test run dialog.
- **Font Sync:** Editor zoom is mirrored across the view panel for a consistent scale.
- Drag & drop fixes in the project tree; newly created test cases are selected and scrolled into view.

## 2.4.1 - 2026-06-17

- **Instant HTML Reporting:** Added a one-click action to instantly generate and view clean, formatted HTML execution
  reports right from the toolbar.
- **Native Event Migration:** Migrated background watchers to the verifier-safe postprocessor API to ensure full
  architectural compatibility with IntelliJ 2025.1+ and 2026 EAP editions.
- **Performance Refactoring:** Optimized list rendering cycles by caching custom row boundaries and styling components,
  cutting down memory allocation overhead.

## 2.0.0

- **Initial Architecture Release:** Introduced the core Test Project Tree sidebar navigation and dynamic, in-memory
  collection handlers.
- Added the interactive Test Case Editor with pagination, background async loaders, and text query filtering.
- Introduced the Test Run Manager tracking live run item statuses, manual status overrides, and runtime duration
  counters.
- Integrated global application setting configurations under the IDE Tools menu group.
