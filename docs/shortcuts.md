[Documentation](README.md) › Every shortcut

# Every shortcut

Every key Testin answers to, what it does, and where. One row per key per place,
so a key that means different things in two places appears twice - which is the
honest way to read this, because that is how a tester meets them.

`Ctrl` here is `Cmd` on a Mac for the keys the platform decides: copy, cut,
paste, undo, redo, find, and select every value. So are Testin's own keys whose
`Cmd` form nothing else takes: `Ctrl+Enter` for a new line, `Ctrl+=` for one more
box in a list, `Ctrl+D` for the details, and the create dialog's `Ctrl+D`, `Ctrl+E`,
`Ctrl+S` and `Ctrl+T`.

The rest keep `Ctrl` on a Mac and add `Alt`, because their `Cmd` form belongs to
macOS or to the IDE's own editor: `Ctrl+Alt+P` for a report and for the
priority, and `Ctrl+Alt+G` for a group. Where `Ctrl+Alt` is taken as well they
add `Cmd` too: `Ctrl+Alt+Cmd+M` to create a test case and for the module,
`Ctrl+Alt+Cmd+B` for the pre-conditions, and `Ctrl+Alt+Cmd+Right` and `Left` to
page, with `Shift` for the ends. Paging cannot stay on `Ctrl` and an arrow,
which move between desktops on a Mac. Each row below names its Mac key.

`Ctrl+Space` is `Ctrl+Space` on a Mac too: it is the IDE's own completion key
there as well, and Testin only names it. Search is `Cmd+Alt+F` on a Mac, and
writing the automation method is `Ctrl+Alt+Cmd+F12`.

Each module's own page carries the same keys in its **Every key, in one place**
table; this is all of them together.

## The tree panel

| Key                             | What it does                                                                                                                                                                                                                                                                                                             |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Enter`                         | Opens the selected test set or test run                                                                                                                                                                                                                                                                                  |
| `Context Menu`                  | Opens the menu on the selected node, without the mouse                                                                                                                                                                                                                                                                   |
| *no key*                        | **Create Testin Node** is a right-click entry. It shared `Ctrl+M` with Create Test Case and lost every press: creating a test case needs only an open editor, so that action was enabled here too and is declared first. Gray on a test project, a test set or a test run, which hold what they hold - the entry says so |
| `Shift+F6`                      | Renames the selected node                                                                                                                                                                                                                                                                                                |
| `Delete`                        | Removes the selected nodes                                                                                                                                                                                                                                                                                               |
| `Ctrl+X`                        | Cuts the selected nodes, to move them                                                                                                                                                                                                                                                                                    |
| `Ctrl+C`                        | Copies the selected nodes                                                                                                                                                                                                                                                                                                |
| `Ctrl+V`                        | Pastes into the selected node                                                                                                                                                                                                                                                                                            |
| `Escape`                        | Takes the gray off nodes the tester cut                                                                                                                                                                                                                                                                                  |
| `Ctrl+Z`                        | Undoes the last tree change                                                                                                                                                                                                                                                                                              |
| `Ctrl+Y`                        | Redoes it                                                                                                                                                                                                                                                                                                                |
| `1` `2` `3`                     | Inside the status popup: Assigned, Completed, Closed                                                                                                                                                                                                                                                                     |
| `Enter`                         | Inside the status popup, takes the highlighted one                                                                                                                                                                                                                                                                       |
| `Shift+Enter`                   | **Review Changes**, on the message about uncommitted work                                                                                                                                                                                                                                                                |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac | Generates a report on the selected test run                                                                                                                                                                                                                                                                              |

## The editor panel: both editors

| Key                                                     | What it does                                                                                                                                                                                                                                                                          |
|---------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Ctrl+F`                                                | Puts the cursor in the search box                                                                                                                                                                                                                                                     |
| `Ctrl+Right`, `Ctrl+Alt+Cmd+Right` on a Mac             | Forward: the next page                                                                                                                                                                                                                                                                |
| `Ctrl+Left`, `Ctrl+Alt+Cmd+Left` on a Mac               | Back: the previous page                                                                                                                                                                                                                                                               |
| `Ctrl+Shift+Right`, `Ctrl+Alt+Shift+Cmd+Right` on a Mac | All the way forward: the last page                                                                                                                                                                                                                                                    |
| `Ctrl+Shift+Left`, `Ctrl+Alt+Shift+Cmd+Left` on a Mac   | All the way back: the first page                                                                                                                                                                                                                                                      |
| `Enter`                                                 | Opens the details panel, or a grid cell                                                                                                                                                                                                                                               |
| `Escape`                                                | Steps back one step                                                                                                                                                                                                                                                                   |
| `Context Menu`                                          | Opens the menu on the selection                                                                                                                                                                                                                                                       |
| `Ctrl` and the wheel                                    | Changes the text size                                                                                                                                                                                                                                                                 |
| `Delete`                                                | Removes the selected test cases                                                                                                                                                                                                                                                       |
| `Ctrl+Z`                                                | Takes back the last change                                                                                                                                                                                                                                                            |
| `Ctrl+Y`                                                | Puts it back                                                                                                                                                                                                                                                                          |
| `Ctrl+Enter`                                            | A line break inside a grid cell or a long field                                                                                                                                                                                                                                       |
| `Ctrl+C`                                                | On cards, opens the copy menu; a letter copies one value. In the grid, copies the cells                                                                                                                                                                                               |
| `Ctrl+X`                                                | In the grid, copies the cells and empties the ones that can be typed into                                                                                                                                                                                                             |
| `Ctrl+V`                                                | In the grid, pastes text into the cells                                                                                                                                                                                                                                               |
| *no key*                                                | **Copy Test Case**, **Cut Test Case** and **Paste Test Case** are right-click menu entries. They had `Ctrl+Shift+C/X/V`, lost that fight to the IDE's own Copy Path and Paste from History, moved to `Alt+Shift`, and then carried no key at all: a tester who wants them clicks them |
| `F5`                                                    | Runs the selected test cases, or stops them                                                                                                                                                                                                                                           |
| `Shift+F5`                                              | Goes to the automation code                                                                                                                                                                                                                                                           |
| `F12`                                                   | Writes the method for a test case that has none                                                                                                                                                                                                                                       |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac                         | Generates a report on this test run                                                                                                                                                                                                                                                   |

## The editor panel: writing test cases

On the cards of a test set editor.

| Key                                         | What it does                                    |
|---------------------------------------------|-------------------------------------------------|
| `Ctrl+M`, `Ctrl+Alt+Cmd+M` on a Mac         | Creates a test case                             |
| `F2`                                        | Opens the menu of fields to change              |
| `D` `E` `M` `T` `B` `S` `P` `G` `O` `R` `U` | Opens that one field straight away              |
| `Enter`                                     | Inside the F2 menu, takes the highlighted field |

## The editor panel: executing a test run

On the cards of a test run editor.

| Key         | What it does                                                                              |
|-------------|-------------------------------------------------------------------------------------------|
| `P`         | Records **Passed**                                                                        |
| `F`         | Records **Failed**, and asks why                                                          |
| `B`         | Records **Blocked**                                                                       |
| `F2`        | Changes the failure details without changing the run item status                          |
| `Alt+Enter` | In the failure details, offers the corrections for a misspelled word in the actual result |

`P` and `B` mean a field in the test set editor and a run item status in the
test run editor. They are different keys on different screens, not one key with
two jobs.

## Light mode

The always on top window, which has its own keys.

| Key             | What it does                                                                           |
|-----------------|----------------------------------------------------------------------------------------|
| `P` `F` `B`     | The three run item statuses, as on the cards                                           |
| `Enter`         | Saves the failure detail and moves on                                                  |
| `Escape`        | Leaves the failure form, or closes the window                                          |
| `Alt+Enter`     | In the failure form, offers the corrections for a misspelled word in the actual result |
| `Ctrl+D`        | Shows the details, or hides them when they are shown                                   |
| The wheel alone | Changes the text size - no `Ctrl` here, deliberately                                   |

## The view panel

| Key                                         | What it does                                                                       |
|---------------------------------------------|------------------------------------------------------------------------------------|
| `Enter`                                     | Opens the panel on the selected test cases                                         |
| `F2`                                        | Opens the menu that changes one field                                              |
| `Ctrl+Right`, `Ctrl+Alt+Cmd+Right` on a Mac | Forward: the next test case                                                        |
| `Ctrl+Left`, `Ctrl+Alt+Cmd+Left` on a Mac   | Back: the previous test case                                                       |
| `F5`                                        | Runs the test case on display                                                      |
| `Shift+F5`                                  | Opens its generated test method                                                    |
| `Escape`                                    | Closes the panel, pressed in the editor or inside the panel                        |
| `Tab`                                       | Brings the next tab to the front: **Details**, **History**, then **Details** again |
| `Shift+Tab`                                 | Brings the previous tab to the front                                               |
| `Ctrl` and the wheel                        | Changes the text size                                                              |

## Automation code

| Key        | What it does                                    |
|------------|-------------------------------------------------|
| `F5`       | Runs the selected test cases, or stops them     |
| `Shift+F5` | Goes to the generated method                    |
| `F12`      | Writes the method for a test case that has none |

## Reports

| Key                             | What it does                                |
|---------------------------------|---------------------------------------------|
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac | Generates a report on the selected test run |

## Sharing work with the team

| Key           | What it does                                                               |
|---------------|----------------------------------------------------------------------------|
| `Enter`       | Confirms the group picker, the Git identity, a merge answer, and a removal |
| `Escape`      | Cancels the dialog, except the merge question below                        |
| `Escape`      | On a merge question, skips that test case and goes on to the next          |
| `Ctrl+Click`  | Adds a group in the group picker                                           |
| `Right click` | Puts one change back, in the review                                        |

## Inside Testin

| Key          | What it does                                                 |
|--------------|--------------------------------------------------------------|
| `Ctrl+Alt+F` | Opens search, from anywhere in the IDE. `Cmd+Alt+F` on a Mac |
| `Enter`      | In the search dialog, goes to the result                     |
| `Up` `Down`  | Moves through the results                                    |
| `Escape`     | Closes the search                                            |

## The settings page

The settings page has no keys of its own. Changing the text size is `Ctrl` and
the mouse wheel over the editor panel or the view panel, and the wheel alone in
light mode. The tree does not zoom.

## Inside the test case dialogs

While the create or update dialog is open.

| Key                                                  | What it does                                      |
|------------------------------------------------------|---------------------------------------------------|
| `Ctrl+D`, `Cmd+D` on a Mac                           | Opens the description field                       |
| `Ctrl+E`, `Cmd+E` on a Mac                           | Opens the expected result                         |
| `Ctrl+M`, `Ctrl+Alt+Cmd+M` on a Mac                  | Opens the module                                  |
| `Ctrl+S`, `Cmd+S` on a Mac                           | Opens the steps                                   |
| `Ctrl+T`, `Cmd+T` on a Mac                           | Opens the test data                               |
| `Ctrl+B`, `Ctrl+Alt+Cmd+B` on a Mac                  | Opens the pre conditions                          |
| `Ctrl+R`, `Cmd+R` on a Mac                           | Opens the reference                               |
| `Ctrl+G`, `Ctrl+Alt+G` on a Mac                      | Opens the group                                   |
| `Ctrl+=`, `Ctrl++`, `Ctrl+Numpad+`, `Cmd+=` on a Mac | Adds a box to Steps or Group, where the cursor is |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac                      | Opens the priority                                |
| `Alt+Enter`                                          | Offers the corrections for what was typed         |
| `Ctrl+Space`                                         | Offers what has been typed before, on a Mac too   |
| `Tab` `Shift+Tab`                                    | Moves between the choices in a picker             |
| `Up` `Down`                                          | Moves through a list of choices                   |
| `Space`                                              | Selects the group under the cursor                |
| `Enter`                                              | Saves                                             |
| `Escape`                                             | Cancels                                           |

## Inside the bulk editors

| Key                                                  | What it does                        |
|------------------------------------------------------|-------------------------------------|
| `Ctrl+=`, `Ctrl++`, `Ctrl+Numpad+`, `Cmd+=` on a Mac | Adds an item to a list being edited |
| `Shift+Delete`                                       | Removes one                         |
| `Ctrl+Shift+A`                                       | Puts a caret on every value at once |
| `Ctrl+Click`                                         | Puts a caret where clicked          |
| `Tab` `Shift+Tab` `Up` `Down`                        | Move between the values             |
| `Enter`                                              | Saves                               |
| `Escape`                                             | Cancels                             |

## In any Testin text field

| Key                        | What it does                 |
|----------------------------|------------------------------|
| `Ctrl+C` `Ctrl+X` `Ctrl+V` | Copy, cut and paste the text |
| `Ctrl+A`                   | Selects all of it            |

## Which keys the IDE knows about, and which stay on their surface

Every key above works. They do not all reach the tester the same way, and the
difference is worth knowing before rebinding one.

**A key in the Keymap.** Testin declares the action to the IDE, so it appears in **Find Action** under its own name and
in **Settings › Keymap › Plug-ins ›
Testin**, where any key can be put on it. Thirty-one entries are declared: 29
actions, and two more that each stand for a list and become one entry per
status. Nine of them carry a default key, on seven keys:

| Key                                 | The action                                                | Where it works                                                                                                                                 |
|-------------------------------------|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `Ctrl+Alt+F`                        | Search Test Project                                       | Anywhere in the IDE                                                                                                                            |
| `Ctrl+M`, `Ctrl+Alt+Cmd+M` on a Mac | Create Test Case                                          | A test set editor. Create Testin Node is declared with no key - it is in Find Action and the Keymap page, so a tester who wants one can set it |
| `Shift+F6`                          | Rename Testin Node                                        | The tree                                                                                                                                       |
| `F2`                                | Update Test Case, Failed Test Case Details, Edit Test Run | Both editors, the view panel, and the tree                                                                                                     |
| *none by default*                   | Copy, Cut and Paste Test Case                             | Both editors. Declared with no key, so Find Action offers them and a tester who wants one can set it                                           |
| `F5`                                | Run Test Method                                           | Both editors and the view panel                                                                                                                |
| `Shift+F5`                          | Navigate to Test Method                                   | Both editors and the view panel                                                                                                                |
| `F12`, `Ctrl+Alt+Cmd+F12` on a Mac  | Automate Test Case                                        | Both editors                                                                                                                                   |

Rebinding one of these moves it everywhere it works at once, including the
tooltips and status bars that print it — those ask the keymap rather than
remembering our default.

**`Ctrl+Alt+F` is also IntelliJ's Refactor › Introduce Field**, and `Cmd+Alt+F`
on a Mac. Two actions on one keystroke, and the IDE settles it by where the
cursor is: in a Java file the refactoring answers, in the Testin panel the search
does. It is kept, because it is the right key for the feature and a tester who
does not refactor Java never meets the other one. `Ctrl+Alt+Shift+F` is free in
the default keymap if it ever bites, and either action can be rebound in **Settings › Keymap**.

**`F12` is also IntelliJ's Jump to Last Window.** It is kept for the same
reason, and for one more: `Ctrl+F12` is worse, because the IDE's File Structure
popup owns that, and `Cmd+F12` on a Mac. Automate Test Case takes
`Ctrl+Alt+Cmd+F12` on a Mac, which nothing else uses, so that generating code is
reachable by key there at all.

These two are the only Testin keys whose IDE action can fire in the same place
as Testin's, and each was decided rather than inherited. Rule-INTERNAL-069 asks
for a note beside each binding in the plugin descriptor, saying which and why.

**A key on its surface.** The action is still declared, so Find Action offers it
and the Keymap lists it — with no default key, because the key belongs to the
component. A declared shortcut is dispatched before a component's own bindings,
so a Keymap entry would silently take the key away from the grid or the tree:

| Key                        | The action                                            | Why it is not in the Keymap                                                                                |
|----------------------------|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `Enter`                    | Open Testin Node, View Test Case Details              | Enter is what a tree and a list do; a global Enter would fire in every editor in the IDE                   |
| `Delete`                   | Remove Testin Node, Delete Test Case                  | Three surfaces answer it — the tree, the card list and the grid — and one entry would answer for all three |
| `Ctrl+C` `Ctrl+X` `Ctrl+V` | Copy, Cut and Paste Testin Node, Copy Test Case Value | The grid keeps these for its own cells, and Excel expects them there                                       |
| `P` `F` `B`                | Passed, Failed, Blocked                               | Bare letters. In the Keymap they would answer everywhere, including while somebody is typing               |

**A key nothing declares.** These are bound by the surface that draws them, and
they are not in Find Action or the Keymap. Each one is here for a reason, and
the reason is the same shape every time: the key means something only while a
particular thing is on screen.

| Keys                                                                    | Where                                          | Why they stay                                                                                                                                                                       |
|-------------------------------------------------------------------------|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `D` `E` `M` `T` `B` `S` `P` `G` `O` `R` `U`                             | The update menu, and a selected card           | Bare letters that stand for a field. They mean nothing outside a test case, and in the Keymap they would answer while a tester types                                                |
| `A` `D` `E` `S` `B` `T` `P` `M` `G` `U` `R` `F` `I` `H`                 | The copy menu                                  | The same, for the fourteen values a copy can take                                                                                                                                   |
| `1` `2` `3`                                                             | The test run status popup                      | Numbers standing for the three test run statuses, live only while that popup is open                                                                                                |
| `Ctrl+D` `Ctrl+E` `Ctrl+M` `Ctrl+S` `Ctrl+T` `Ctrl+B` `Ctrl+G` `Ctrl+P` | The create and update test case dialogs        | Each opens one field of the dialog in front of the tester. Outside it there is no field to open. The top of this page names their Mac keys                                          |
| `Tab` `Shift+Tab` `Up` `Down` `Space`                                   | Any dialog                                     | Moving between fields and choices is the platform's own gesture, not a command                                                                                                      |
| `Tab` `Shift+Tab`                                                       | The view panel                                 | Moving between its two tabs, which exist only there                                                                                                                                 |
| `Enter` `Escape`                                                        | Any dialog, and the grid                       | Confirm and cancel. Every dialog has them, so they belong to the dialog framework rather than to any one action ([#11](https://github.com/MutebAlMughyiri/test-in/issues/11))       |
| `Ctrl+Space`                                                            | Any dialog field that completes                | The platform's own completion gesture, on a Mac too                                                                                                                                 |
| `Ctrl+Enter`                                                            | A grid cell, a long field                      | A line break where `Enter` saves                                                                                                                                                    |
| `Ctrl+Shift+A`, `Shift+Delete`                                          | The bulk editors                               | Caret on every value, and remove an item — both about the editor on screen                                                                                                          |
| `Ctrl+Z` `Ctrl+Y`                                                       | The tree, and each editor                      | Undo and redo are per surface: each keeps its own history, so one keymap entry could not say whose                                                                                  |
| `Ctrl+Left` `Ctrl+Right`, with `Shift`                                  | Both editors, and the view panel               | Paging in an editor and stepping through test cases in the panel — the same gesture over different things. `Ctrl+Alt+Cmd` on a Mac, where `Ctrl` and an arrow move between desktops |
| `Ctrl+F`                                                                | Both editors                                   | Puts the cursor in that editor's own search box                                                                                                                                     |
| `Ctrl+P`, `Ctrl+Alt+P` on a Mac                                         | A test run                                     | Generate a report. Its action is not declared yet — see below                                                                                                                       |
| `Context Menu`                                                          | The tree and both lists                        | Opens the menu on the selection, without the mouse                                                                                                                                  |
| `Escape`                                                                | The tree, the lists, the grid, the details tab | Steps back one step, and what a step is depends on the surface                                                                                                                      |
| `Ctrl+D`, the wheel alone                                               | Light mode                                     | A window with its own keys, always on top                                                                                                                                           |

**Still to declare.** Four actions a tester can reach are not declared yet, so
they are not in Find Action and their keys cannot be rebound: **Generate Report**
(`Ctrl+P`), **Undo** (`Ctrl+Z`), **Redo** (`Ctrl+Y`), and **Set Status** on a
test run. Each of them is one action shown on several surfaces with different
things behind it, which is the part of [#119](https://github.com/MutebAlMughyiri/test-in/issues/119)
that is not finished.

## Every key on this page is bound

There is no list of declared-and-unbound keys anymore, and there should never be
one again.

`H` `M` `L` were drawn beside the priorities and `E` beside the empty bug
priority, and nothing answered to any of them. They were removed rather than
bound: at the time the letters matched no word a tester could see, and `E` named
a choice the failure dialog does not offer at all. Choosing a priority is the mouse or the arrow keys, which is what the hint
beside it now says. [#283](https://github.com/MutebAlMughyiri/test-in/issues/283).

**A key is drawn only where pressing it does that thing.** Both directions of
that matter: a capability shown without its key is
[Rule-PRODUCT-016](product.md), and a key shown for a capability that has none
is this.

---

[Documentation](README.md)
