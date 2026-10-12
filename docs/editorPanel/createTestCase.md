[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-005

# UC-EDITOR-PANEL-005: Create a test case

**As a** tester, **I want** to add a test case to the test set I am looking at, **so that** an idea is written down
before I forget it.

This is how every test case in Testin begins.

`Ctrl+M`, or `Ctrl+Alt+Cmd+M` on a Mac.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, kept
  against the test set it is showing, and the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-028** — The dialog opens showing the description alone.
  Every other field appears when its key is pressed.
- **Rule-EDITOR-PANEL-029** — A field the tester left unopened writes nothing.
- **Rule-EDITOR-PANEL-030** — A new test case has no place in the order yet, so
  it sorts last.
- **Rule-EDITOR-PANEL-031** — A new test case starts at the lowest priority.
- **Rule-EDITOR-PANEL-032** — The description, the expected result, the module,
  the pre-conditions, the test data and each step have their spaces trimmed.
- **Rule-EDITOR-PANEL-033** — A blank step is dropped.
- **Rule-EDITOR-PANEL-034** — The dialog does not close when the tester clicks
  outside it, or when the IDE loses the focus.
- **Rule-EDITOR-PANEL-199** — The keys sit on one strip: the keys of the field
  the tester is in, then **Save** and **Cancel**. The shared two are always
  last, so a tester looks for Save in the same place whichever field they are
  in.
- **Rule-EDITOR-PANEL-200** — There is one strip, and it is never empty, because
  Save and Cancel are always on it. A field with no keys of its own adds
  nothing to it.
- **Rule-EDITOR-PANEL-218** — A key is stood down only while a popup is actually
  using it. An open suggestion list takes Enter and Escape; every other key
  reaches the field underneath, which is what lets CTRL+ENTER add a line while
  the list is up.
- **Rule-EDITOR-PANEL-247** — Priority is radio buttons, not a list, so a tester
  sees every choice at once and picks one in a single click. Each reads its
  word - Low, Medium, High - because a number alone says nothing about which
  end is urgent, and the lowest is chosen to start with.
- **Rule-EDITOR-PANEL-248** — A button that turns gray while the pointer is on
  it loses its hover look at once, so a control that has stopped working never
  looks ready to press.
- **Rule-EDITOR-PANEL-249** — A grid that held the keyboard when it was rebuilt
  holds it again afterward, so the next key lands in the grid rather than
  nowhere.
- **Rule-EDITOR-PANEL-250** — A grid row is drawn in its stripe, or in the
  selection color while it is selected, and in nothing else. The pointer passing
  over it changes nothing.
- **Rule-EDITOR-PANEL-251** — When the status bar is too narrow for everything,
  nothing on it gives way or is cut short: it scrolls sideways, as the toolbar
  does (Rule-EDITOR-PANEL-257), so the page size field can always be reached.
  Where there is room, the page arrows sit in the middle of the bar.
- **Rule-EDITOR-PANEL-252** — A badge's words are printed white or dark,
  whichever reads on its color, so a pale badge never carries white text.
- **Rule-EDITOR-PANEL-257** — An editor narrower than its toolbar or its status
  bar scrolls that bar sideways under the pointer, with the mouse wheel or a
  touchpad. No scrollbar is shown and the bar keeps its height, so nothing on it
  is cut off and the editor below does not move.
- **Rule-EDITOR-PANEL-271** — Reference opens with Ctrl+R, Cmd+R on a Mac, and
  carries R on its icon, the letter the update menu and the copy menu give it.
- **Rule-EDITOR-PANEL-293** — Ctrl+= adds one more box to the list field the
  cursor is in, Steps or Group, and puts the cursor in it. Ctrl++ and Ctrl with
  the numpad + do the same. A field's own key, Ctrl+S or Ctrl+G, only opens that field; pressed again it
  moves the cursor to the field and adds nothing. With the cursor anywhere else,
  Ctrl+= does nothing.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Case                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [D]  set description                                        │
│                                                              │
│  (a field appears here when its key is pressed)              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k] Alt+Enter Corrections   Tab / Shift+Tab Navigate   Ctr …│
└──────────────────────────────────────────────────────────────┘
```

1. **The description** — the only field there when the dialog opens. Its icon
   is the letter of its key, D, in a rounded frame.
2. **The strip** — one row. The keys of the field the tester is in come first
   and change as they move between fields; **Save** and **Cancel** are always
   last, so a tester looks for Save in the same place whichever field they are
   in. A field with no keys of its own adds nothing, and the strip still shows
   those two. Just before them, *Ctrl+Letter Add Field*, *⌘Letter* on a Mac,
   stands for the keys below: each field opens with that key and the letter
   on its icon. On a Mac, Module, Pre Conditions, Priority and Group take the
   keys in the table instead.

## The fields and the keys that open them

| Field           | Key                                 | The gray hint in the empty box                |
|-----------------|-------------------------------------|-----------------------------------------------|
| Description     | `Ctrl+D`, `Cmd+D` on a Mac          | *set description*                             |
| Expected Result | `Ctrl+E`, `Cmd+E` on a Mac          | *set expected result*                         |
| Module          | `Ctrl+M`, `Ctrl+Alt+Cmd+M` on a Mac | *set module*                                  |
| Steps           | `Ctrl+S`, `Cmd+S` on a Mac          | *set step*, then the number                   |
| Priority        | `Ctrl+P`, `Ctrl+Alt+P` on a Mac     | none, it is three radio buttons               |
| Group           | `Ctrl+G`, `Ctrl+Alt+G` on a Mac     | *set group*, completing what the project uses |
| Test Data       | `Ctrl+T`, `Cmd+T` on a Mac          | *set test data*                               |
| Pre Conditions  | `Ctrl+B`, `Ctrl+Alt+Cmd+B` on a Mac | *set preconditions*                           |
| Reference       | `Ctrl+R`, `Cmd+R` on a Mac          | *set reference*                               |

In Steps and Group, `Ctrl+=`, `Ctrl++` or Ctrl with the numpad `+` adds one more box; the field's own key only
opens it (Rule-EDITOR-PANEL-293).

Each field's icon is the letter of its key in a rounded frame, so Pre Conditions
shows B. The same icon marks the field's values in its completion list.

Description, Expected Result, Module, Steps and Group complete the values Testin
knows. [When that list changes](refreshEditor.md#what-completion-and-the-group-filter-offer)
is written on the refresh page.

**Expected Result, Test Data and Pre Conditions hold many lines.** They are the
three fields a tester writes sentences into, so each is the same growing box:
`Ctrl+Enter` adds a line, `Enter` still saves, and the dialog grows with the box
rather than scrolling inside it (Rule-INTERNAL-097).

## Main flow

1. The tester presses `Ctrl+M` in the editor.
2. The **Create Test Case** dialog opens with the cursor in the description.
3. The tester types a description.
4. The tester presses `Ctrl+E` and types the expected result.
5. The tester presses `Ctrl+S`, types the first step, and presses `Ctrl+=` for each next one.
6. The tester presses `Enter`.
7. Testin creates the test case at the end of the test set.
8. A message reads *Created*.
9. Testin writes the test method for it.
10. The new test case is selected.

## What Testin refuses

**If the description is empty** — the dialog stays open, the gray hint in the
box and the description's **D** turn red, and the cursor goes back to it. No
message is raised.

**If the test set is removed or renamed while the dialog is open** — by a sync,
a pull or the tree — nothing is saved. A message says the test set is no longer
there, and the dialog stays open with what was typed in it.

**If a Create Test Case dialog is already open** — it is brought forward rather
than a second one opened, so nothing typed into it is lost (Rule-INTERNAL-075).

**If the description cannot name a Java method** — nothing is saved. A message
titled **That description cannot name a test method** says what the method would
have been called. A description must begin with a letter. It also cannot be a
single word that Java keeps for itself.

**If another test case in this test set already names that method** — nothing is
saved. A message titled **Another test case already names that test method**
says what the method would have been called. Punctuation and capitals are
dropped on the way to a method name, so *Log in* and *Log-in!* are one method:
the description has to differ by a word.

**If a step is left blank** — it is dropped. The steps after it keep their own
numbers.

**If saving the test case fails** — a message reads *Could not save, so the
cards went back to what is on disk*, and the list is read again. The new card
goes with it: it was never written, and leaving it on screen would be a test
case that exists nowhere.

**In a test run editor** — **Create Test Case** is gray, and its reason reads *A
test run covers test cases that already exist - write a new one in its test set,
then edit the test run to include it.* The editor is not asked what class it is;
the node is asked whether it holds test cases, which is the question Delete
beside it already asks.

**While the test set is still being read** — the entry and `Ctrl+M` are gray,
reading *This test set is still being read - the new test case would be placed
against a list nobody has seen yet.* The toolbar button says the same when it is
pressed. Creating a test case sorts the list, and a sort makes a load that is
still on its way stale: the load then landed on nothing, the spinner never
stopped, and the new test case sat in the middle of the set at the next
refresh.

## Where the plugin breaks its own rules

**The empty editor names a key a Mac does not have.** A test set with no test
cases reads *Press Ctrl+M to add* on every machine, while the key itself is
`Ctrl+Alt+Cmd+M` on a Mac. That is difference 9.

---

[Documentation](../README.md) › [The editor panel](main.md)
