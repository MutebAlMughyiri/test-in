[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-046

# UC-EDITOR-PANEL-046: Work in light mode

**As a** tester, **I want** the test case I am judging to stay on top of the
application I am testing, **so that** I can record a run item status without
hunting for the IDE.

Light mode is a separate window that stays above every other window. It shows **one test case at a time**. The tester
can work in the application under test
with IntelliJ minimized, and still record a run item status without switching
windows.

|                     |                                                                                                                                                                                                                  |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Part of Testin**  | The editor panel. Light mode is one of its windows                                                                                                                                                               |
| **Answers for**     | This one window. The rest of the editor panel is [on its own pages](main.md)                                                                                                                                     |
| **Answers**         | Why this window is shaped the way it is, and what every part of it does                                                                                                                                          |
| **State**           | **Written.** Built and shipped — [#13](https://github.com/MutebAlMughyiri/test-in/issues/13), closed                                                                                                             |
| **Checked against** | `main` at `1270e599`, 20 September 2026 — every message, key, strip and drawn part read against the code again                                                                                                   |
| **Numbering**       | `UC-EDITOR-PANEL-046`. Rules `Rule-EDITOR-PANEL-201` to `Rule-EDITOR-PANEL-204` say how the window moves; the rest of this page was written before the part was numbered and its statements are not numbered yet |

This document describes the window as it was built.

---

## What it is for

A tester running a test case by hand is not looking at the IDE. They are in a
browser, or an app, or on a phone, doing what the test case says. The test case
is behind the window they are working in. Every run item status costs them a
window switch. They find IntelliJ, find the test run, find the row, click, and
come back.

Light mode removes that switch. The window stays above the application under
test. The three run item statuses are one keystroke away.

**It is a window of its own, not a panel inside the IDE.** That is the whole
requirement. A panel disappears when the IDE is minimized. This window does
not, and staying visible is the reason it exists.

---

## Where it opens from

One button on the test run editor toolbar. It sits beside the Start and Stop
buttons it belongs with. Its icon is a sun, borrowed from the IDE's own set of
icons. The icon names the mode, not the action.

The design asked for a picture of a window holding a smaller window in its
corner. The IDE has no ready-made icon for that. The two that came closest were
both wrong. One points its arrow the wrong way. The other is drawn so faintly
that it would have looked switched off. So the icon is borrowed rather than
drawn. That is what [#123](https://github.com/MutebAlMughyiri/test-in/issues/123)
tracks.

The button is a **toggle**. The tester presses it to open light mode. They press
it again to close the window and carry on in the editor. It stays pressed for as
long as the window is there. It works only while the test run is open. It turns
gray the moment the test run reaches Completed or Closed, and its tooltip then
says why — *Light Mode records run item statuses, and this test run is
Completed*. The same question grays out Start, and the same question closes this
window if it is open.

---

## The window

The sketches below are at the real size. The content is the sample project's
Login test case, mid-way through Cycle 2. The window is about 420 pixels wide to
start with.

### Before the tester presses Start

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ >]  [pin]  [view]            Cycle-2                      6 cases       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│            Press the play button to start test execution                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

There is no test case at all. The window shows three things: the test run name,
how many test cases it holds, and the prompt. There is no description, there are
no run item status buttons, and there are no clocks.

The window would contradict itself if it showed a test case and offered the
three run item statuses while telling the tester to press Start. The tester
would be right to ignore the prompt and start judging. This is **stricter than
the test run editor**. The test run editor records a run item status whether
execution is running or not. The difference is deliberate. A test case judged
before Start carries a zero duration. Here that cannot happen, because there is
nothing to judge until the test run is going.

### While tests are running, showing the test case alone

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  (LOGIN)                                                   [m]  [>]  [tc]  │
│                                                                            │
│  [D] Sign in with a correct username and password                          │
│  [E] The dashboard opens and the account name is shown in the header.      │
├────────────────────────────────────────────────────────────────────────────┤
│      P Passed              F Failed              B Blocked                 │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Ctrl+D Show/Hide Details  Escape Close  P Passed  F Failed  B Blocked     │
└────────────────────────────────────────────────────────────────────────────┘
```

### While tests are running, with the details open

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  (LOGIN)                                                   [m]  [>]  [tc]  │
│                                                                            │
│  [D] Sign in with a correct username and password                          │
│  [E] The dashboard opens and the account name is shown in the header.      │
│                                                                            │
│      [ High ]  [ Accounts ]                                                │
│      1. Open the sign-in page.                                             │
│  [S] 2. Type the username.                                                 │
│      3. Type the password.                                                 │
│      4. Press Sign in.                                                     │
│  [T] sample.user@example.com / correct-horse                               │
│  [B] The account exists and is not locked.                                 │
├────────────────────────────────────────────────────────────────────────────┤
│      P Passed              F Failed              B Blocked                 │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Ctrl+D Show/Hide Details  Escape Close  P Passed  F Failed  B Blocked     │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## What each part is

Numbered left to right along the title bar, then down the window.

### 1. The pin button, pressed while the window stays on top

It is on by default, because that is the point of the window.

It is the IDE's own pin icon, on the rounded gray patch the IDE draws behind
any pressed toolbar button.

The window is a real top-level frame, not a tool window. So it survives IntelliJ
being minimized.

### 2. Start, then Stop

One button that swaps between Start and Stop. It uses the test run editor
toolbar's own two icons. They are read from one place, so the toolbar and this
window can never end up different. A tester who presses Start in the toolbar and
Stop in this window is pressing the same two buttons, and should be looking at
them.

The icon is not the IDE's arrow for running *code*, because a tester running a
test case by hand runs no code. Stop is a pause symbol rather than a square.

### 3. The menu that chooses what the window shows

Seven checkboxes decide what the window shows:

- the test set name
- the durations
- the run item status buttons
- the status bar
- Navigate to Test Method
- Run Test Method
- Navigate to Test Case

It carries the toolbar's own Details icon, a tick box with a tick in it. It is
the same picture in the IDE and in the window, and it is a picture of exactly
what it opens.

### 4. Test run and counter

This says which test run is being executed, and how far through it the tester
is. `3 / 6` counts the test cases the editor is showing. With a filter on, it
counts only what the filter left. The window reads the same list of test cases
the grid shows, so the two can never disagree about which test case is next.

### 5. The description, large

This is the one thing that cannot be turned off. It is drawn at a size the
tester can read from across a desk. Its icon, **D**, the test case form's own,
sits before it in the middle of its height.

### 6. The test set name, in small capitals above the description

It sits inside a rounded frame, with no icon, the name and the frame in the gray
of the **D** and **E** letter frames below it (Rule-EDITOR-PANEL-232), and a
line's space under it. It is
on by default, because a description alone can be ambiguous. "Sign in with
a correct username and password" could sit under a Login test set. It could also
sit under a Checkout test set testing guest sign-in.

It can be hidden. A tester working through a single test set reads the same
word on every test case, and a word that never changes is noise. It helps when
a test run covers several test sets. It does not when the test run covers one.

**The test case's own buttons sit at the right end of the same line**
(Rule-EDITOR-PANEL-243): **Navigate to Test Method**, **Run Test Method** and **Navigate to Test Case** - the three the
test run editor's card offers, in the
card's order, with the card's icons. Each grows under the pointer and does what
the card's does, and Run is **Stop Test Method** while the test case's
automation runs. A button that cannot work - no generated method, the Java or
TestNG plugin missing, a test case with no test set - is gray and says why in
its tooltip; it is never left out. The tooltip shows only while the pointer is
on the button: the button's name and its key, or the reason it is gray.

They belong to the test case, so they slide in with it, and the description
keeps the whole width of a narrow window. Each is an entry of its own in the
view menu (Rule-EDITOR-PANEL-244). The line shows while the name or any button
is on it. The buttons go while the failure form is open, as the run item status
buttons do.

Run claims the test case for this test run first, as the card's Run does (Rule-EDITOR-PANEL-180), so its run item status
comes back here. When it does,
the walk moves on exactly as after the tester's own run item status (Rule-EDITOR-PANEL-242).
A result TestNG may follow with another is not one: a skipped attempt may be
retried, and a method with an invocation count runs several times, so the window
stays on the test case until TestNG moves on or the execution ends
(Rule-CODEGEN-110).

### 7. The two clocks

The test case reads `00:41`. The test run reads `00:12:41`. They sit in the
bottom strip, small, one at each end, with no labels. The test case duration
is on the left. The test run duration is on the right.

They are small and gray, never large and never in the highlight color. A large
clock is a stopwatch, and a tester watching a number climb hurries.

**They carry no labels.** The right-hand figure is always the larger of the two.
That tells the tester which is which faster than a word would. A label that
never changes is noise. Both figures come from the execution timer
[#27](https://github.com/MutebAlMughyiri/test-in/issues/27) already built. Both are
recorded whether they are shown or not, so nothing is lost by showing them
softly.

**The test run clock carries its hours. The test case clock does not.** That is
what tells them apart. An earlier try showed the test run as hours and minutes,
and broke that rule. At five minutes it read `00:05`, beside a test case at four
and a half minutes reading `04:30`. The smaller number was the longer time. So
the test run clock keeps its seconds and always carries its hours. It is then
always the wider and larger of the two, whatever either of them shows.

### 8. The expected result, under the description

It is not behind the details toggle, because it is not a detail. The description
says what to do. The expected result says what should happen. A tester who
cannot see the second has no way to judge the first.

They are one thought, so they read as one. **Neither carries a label**, only
the test case form's icon: **D** before the description and **E** before the
expected result, each in the middle of its text's height. The
description is large and dark. The expected result is smaller, lighter and
gray, directly beneath it. A tester reading two lines in that order does not
need to be told which is which. The word *Expected* would cost a line, in a
window that is only as tall as what it shows.

It cannot be turned off. It was taken off the view menu while the window was
being built, for the same reason the description was never on it.

### 9. Details open and close on the keyboard

`Ctrl+D` shows the rest of the test case: the priority badge and a badge for
each group, then the steps, the test data and the pre-conditions. `Ctrl+D` again
hides them. There is no button. A control that sits on
screen permanently, to be pressed twice a session, is wasted space. This window
is built to have none.

**One key, both ways.** It was two - `Ctrl+D` to show and `Ctrl+H` to hide - so
that a key always did what it said whatever the window was showing. Muteb chose
one key on 18 September 2026: a tester learns one key rather than two, and the
status bar names it as *Show/Hide Details*.

### 10. The status bar, which names every key

`Ctrl+D` Show/Hide Details, `Escape` Close, `P` Passed, `F` Failed, `B`
Blocked, `Shift+F5` Navigate to Test Method, `F5` Run Test Method - **Stop Test
Method** while the test case's automation runs.

The last two are the keys the IDE's keymap gives those two actions, so a tester
who remaps them reads their own keys here and in the buttons' tooltips, and the
window answers them (Rule-EDITOR-PANEL-245). They go while the failure form is
open, as the buttons do.

**This is where `Ctrl+D` is taught.** Removing the details button left a
shortcut that nothing on screen named. That was the one real cost of
the change, and this row covers it. The row is not a new idea. Every Testin
dialog already carries a row like this one. That is where the wording and the
spacing come from.

**Its background is the title bar's, not the body's.** The window uses two
shades, and they mean something. The working area is the pale one. That is the
test case, the run item status buttons and the clocks. The surrounding frame is
the gray one. That is the title bar and this row. So the run item status row and
the status bar look like the two separate things they are, rather than one band
split by a hairline.

**It can be turned off in two places.** It is a tick box in the view menu, so a
tester who has learned the keys can win the row back in this window. It is also
a setting: *Settings → Testin → "Show keyboard shortcuts in dialogs"*. That
setting turns the strip off in every Testin dialog at once. Knowing the keys is
a fact about the tester, not about 28 separate dialogs. One piece of code draws
this row everywhere, so one setting reaches every dialog.

The failure form is the one exception. Its **Save** and **Cancel** buttons are
gone, so this row
is the only place `Enter` and `Escape` are written down. The window forces the
row back for that one state.

**One line, always.** It never wraps and never scrolls. Whatever fits is shown,
and the rest is simply not there. So the order was chosen, not left to chance.
The two keys with no button to teach them come first. The ones that fall off
a narrow window are exactly the ones with buttons sitting above them - the two
button keys first, at the window's starting width, then the three run item
statuses. Making the window wider brings them back. So the edge of the window
has a second job.

### 11. The run item status buttons

Three buttons, each printing its own key, all in one color. `P`, `F` and `B` are
not new keys. The three run item statuses already answer to them everywhere else
in Testin. That is why a fourth run item status will appear here without this
window changing. The fourth is Out Of Scope, once
[#10](https://github.com/MutebAlMughyiri/test-in/issues/10) is done. The words tell the
three apart. Color would only say "button".

---

## Choosing what shows

Seven checkboxes on the title bar, remembered per machine rather than per
project.

- **Test set name**
- **Duration** — both clocks, one entry
- **Run Item Status buttons**
- **Status bar**
- **Navigate to Test Method**
- **Run Test Method** — Run and Stop are one button, so one entry
- **Navigate to Test Case**

**Seven toggles, not a settings screen.** The window has one job. Every one of
these is a thing on the window that a tester can already see. So the list cannot
grow past what the window holds. That is why it is a menu on the title bar
rather than a page in Settings.

**Duration is one entry, not two**, because the two clocks are one line. Hiding
one and keeping the other would leave a lopsided row. It would also leave a
toggle nobody would reach for twice.

**The run item status buttons and the clocks are one strip, with two halves
that switch on and off.** Turn the buttons off, and the clocks stay where they
are. Turn the clocks off, and the buttons close up over them. Turn both off, and
the whole strip is gone, leaving the body sitting directly on the status bar.
Nothing is left behind as an empty band, because the window's height is its
content.

**Hiding the run item status buttons costs nothing, because the status bar
still names the keys.** That is what makes the smallest window usable, rather
than merely small. The buttons go. The row that says `P` Passed is still there.
Nobody hides the buttons and thinks the window stopped working. This is also the one case
where the tail of that row matters, because no button is left to teach those
three keys.

**The status bar itself stays on the list.** A tester who has learned the keys
does not need a strip repeating them. In a window this small, a row they never
read is worth reclaiming. Turning everything off is theirs to choose. The failure
form is the one state where it is not. With no buttons on it, that row is the
only place `Enter` and `Escape` are written down. So the window forces the row
back for that state, rather than taking the choice away everywhere.

**Neither the description nor the expected result turns off.** The description
says what to do. The expected result says what should happen. They are one
thought, and a tester who can see only the first has no way to judge it. A
window showing half a test case is not a smaller window. It is a broken one. The
expected result was the second checkbox on this list until the build. Taking it
off is the same argument that kept the description off from the start.

**Remembered with the position.** They are remembered per machine, not per
project. The same tester on the same screen wants the same window, whichever
project they open.

**The failure form overrides five of the seven.** Pressing `F` shows the actual
result, the severity, the priority and the error box, whatever is hidden. If a
run item status needs detail, the window must show that detail, whatever is
switched off. The run item status buttons and the three test case buttons go
while the form is open. The test case is already judged, and the form is the
only thing left to do. The status bar comes back, because it is what says how
the form is finished.

---

## What happens when a test case fails

Passed and Blocked are one keystroke. The run item status is recorded, and the
window moves to the next test case. Failed is the one run item status that asks
for something back. A failure nobody described is a failure nobody can act on.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  [set] LOGIN · Failed                                                      │
│                                                                            │
│  [D] Sign in with a correct username and password                          │
│  [E] The dashboard opens and the account name is shown in the header.      │
│                                                                            │
│  ACTUAL RESULT                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ set actual result…                                                   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  BUG SEVERITY                                                              │
│  ( ) Blocker   ( ) Major   ( ) Minor   (•) Enhancement                     │
│                                                                            │
│  BUG PRIORITY                                                              │
│  ( ) High   ( ) Medium   (•) Low                                           │
│                                                                            │
│  STACKTRACE                                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ paste error or exception or screenshot…                              │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  SCREENSHOTS                                                               │
│   ┌────────┐x                                                              │
│   │ picture│                                                               │
│   └────────┘                                                               │
│                                                                            │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Enter Save & next   Escape Cancel   Alt+Enter Corrections   Ctrl+V Paste …│
└────────────────────────────────────────────────────────────────────────────┘
```

### 12. The test case stays above the form

The description and the expected result do not move or shrink. Writing down what
actually happened is a comparison against what should have happened. Hiding
either half while the tester types is the one thing this form must not do.
`Ctrl+D` does not apply here. A form cannot be collapsed while it is waiting to
be filled in.

**The form zooms with it.** The wheel exists so the window can be read from
where the tester is sitting. A form they then had to lean in to type into would
have moved the problem, not solved it. Each part remembers the size it was built
at, and zoom multiplies that starting size. Multiplying the size on screen
instead would make every turn of the wheel grow on top of the last.

### 13. The same four fields as the test run editor's failure dialog

The four fields are the actual result, the bug severity, the bug priority and
the error box. They are exactly the fields the test run editor's own failure
dialog asks for, with the same starting values and the same wording. A failure
recorded here and one recorded in the test run editor are the same record. The
actual result underlines a misspelled word here as it does there, and
`Alt+Enter` offers the corrections. It is the same growing box as well: it
starts one line tall, grows a line at a time as the tester writes, and the
window grows with it (Rule-INTERNAL-097).

### 14. Inside the window, in a box it already has

There is no second dialog. `Ctrl+D` fills the details box with the badges, the
steps, the test data and the pre-conditions. While a failure is being written,
that same box holds these four fields instead. Nothing is added to the window,
and nothing is taken away. One box swaps what it is showing, and so does the run
item status strip below it.

That is also the decision the `F` key forces. The test run editor's own failure
dialog belongs to the IDE window, and blocks it until it is closed. Opening it
would bring IntelliJ to the front. That is exactly what this window exists to
avoid. The feature would defeat itself on its most common run item status.

### 15. Real radio buttons, the same ones the test run editor uses

Real radio buttons, not something smaller. The four fields are written down
once and shared with the test run editor's own dialog, so the same four fields
can never end up looking different in the two places. The choices come from one
list each, so a new severity or priority appears here on its own.

### 16. Ctrl+V reads the clipboard, not the cursor

The form answers one paste gesture and the clipboard decides what it means: a
picture becomes a screenshot, and anything else is text in whichever box has the
cursor. The error box still says so in its gray hint text, *"paste error or
exception or screenshot…"*, because that is where a tester is already looking,
and the status bar names it as *Ctrl+V Paste Screenshot*.
The gesture is not tied to that box, so a screenshot arrives wherever they happen
to be typing. A pasted screenshot shows as a small picture in a **Screenshots** row, never as letters, and its **x**
takes it out. The row is
there only while it holds a picture.

This removes a way to lose work. Passing a test case clears everything recorded
about a failure on it. So pasting evidence onto a test case and then pressing
`P` would have destroyed that evidence. The test run editor asks first. A window
where one keystroke records a run item status cannot ask. So evidence only ever
lives inside a form that `Enter` saves and `Escape` throws away.

### 17. The run item status buttons disappear while the form is open

There is no **Cancel** button and no **Save & next** button. `Enter` and
`Escape` already do the same, and two buttons repeating them would cost a row in
a window whose whole argument is that it is small. The clocks keep running,
because the tester is still on this test case while they write the failure up.
The status bar names the two keys that work here.

That is the one thing the removal had to pay for. With no buttons, the status
bar is the only place `Enter` and `Escape` are written down. So while a form is
open, that row is shown whatever the view menu says.

### 18. Escape puts the test case back to no run item status

A tester who presses `F` by mistake gets the test case back unjudged. They do
not get a Failed with an empty actual result. Only Save records anything.

---

## Zoom

The scroll wheel over the window makes the text bigger and smaller, **without
holding `Ctrl` or any other key**. It changes this window and nothing else. Not
the grid behind it. Not the details panel. Not the IDE's editors.

**The text changes size. The window's furniture does not.** Four things change
size: the test set name, the description, the expected result and the detail
fields. The title bar, the run item status buttons and the clocks stay the same
at every zoom. Zoom exists so the
test case can be read from where the tester is sitting. Making the icons and the
clocks bigger would only cost the screen space they were trying to free.

**It is this window's own size, and nothing else's.** That is a deliberate
departure from how the rest of the plugin zooms. The reason is the whole point
of the feature. The tester is away from the IDE. They are reading a small window
on the far side of a screen, while working in another application. How large
that window needs to be has nothing to do with how large a grid should be on a
monitor they are not looking at.

**So it must not reuse the plugin's shared zoom.** That is the obvious thing to
reach for, and it is the wrong one. It would put a font size on every open
editor in the IDE. This is written down because a later reader will find two
pieces of wheel-zoom code and try to merge them.

**It opens at the size the tester is used to, then follows its own zoom.** That
happens every time, not just the first. The size is read from the IDE's editor
font as the window opens. So a tester who changes the IDE font gets a window
that agrees with it. The zoom on top of that is the tester's own. It is
remembered per machine, alongside the position, the width and the seven view
toggles. It used to go back to the normal size every time the window opened, so
the tester had to set it again each time.

The size is asked for each time, not fixed once. The editor font is a setting,
and a tester can change it while the IDE is running. A fixed size would freeze
whichever size happened to be set when the window first opened.

**Everything in the window is that size, plus or minus a little.** Call it the
base size.

- the description is three points larger, and bold
- the expected result and every detail are the base size
- the test set name is two points smaller. The details are named by their
  icons, **S**, **T** and **B**, as the test case form names them, each in the
  middle of its row's height. The badges come first with no icon, since a badge
  says what it is. The captions on the failure form are set in the caption
  font, JetBrains Mono in capitals, as every caption in Testin is (Rule-INTERNAL-087). The icons keep their size when
  the window zooms

No text ever goes below 8 points, which is where the rest of the plugin stops
too. One number moves, and everything moves together. So the layout cannot come
apart at a size nobody tested. The expected result and the detail values share a
size on purpose. They are the same kind of thing being read, and setting them
apart said they were not.

**No other key held down.** Everywhere else in the IDE, and on every other
Testin screen, zooming takes `Ctrl` and the wheel. Here it is the wheel alone,
for three reasons. The window has nothing else a wheel could mean. Its height is
fixed to what it shows, so there is nothing to scroll. Anything inside it that
does scroll would take the wheel first anyway. So this is the one window in the
plugin where zooming needs no other key held down.

---

## How it moves

The window used to change instantly. A run item status was recorded, and the
next test case was simply there — different words in the same place, and the
window a different height. On a window that sits above the application under
test and is looked at rather than worked in, that reads as a redraw rather than
as progress.

- **Rule-EDITOR-PANEL-201** — A new test case comes in from above while the one
  it replaces goes down and out. The first test case does not arrive this way,
  because none left to make room for it.
- **Rule-EDITOR-PANEL-202** — The window eases to its new height rather than
  jumping to it. That covers every height change: the details opening, the
  failure form taking their place, a screenshot pasted into the form or taken
  out of it, a longer test case arriving and every zoom step.
- **Rule-EDITOR-PANEL-203** — Everything moves for the same 200 milliseconds.
- **Rule-EDITOR-PANEL-204** — A key pressed while the window is still moving is
  taken at once. The movement in progress is dropped where it is and the next
  one starts from there; nothing waits for an animation to finish.
- **Rule-EDITOR-PANEL-216** — A tester who turned Animate windows off in the IDE
  sees no movement. Light mode floats above every other application, which is
  the worst place to move when somebody has asked for stillness, and the switch
  is the IDE's own rather than a second one of ours that could disagree with it.
- **Rule-EDITOR-PANEL-217** — Light mode never grows past the display it is on.
  A test case longer than the screen stops at the edge and says so on its last
  row. This is because the run item status buttons and the status bar finish a
  test case, and a window taller than the screen puts them where nobody can
  reach them.
- **Rule-EDITOR-PANEL-232** — The test set's name sits in a rounded frame with
  no icon. The name and the frame are in the gray of the letter frames beside
  the description, so it reads as a label above the test case rather than as
  part of it. The frame stays quieter than the description, and it goes with the
  name when the name is turned off.
- **Rule-EDITOR-PANEL-243** — Light mode offers the test case on screen the
  buttons the test run editor's card offers it: Navigate to Test Method, Run
  Test Method and Navigate to Test Case. They sit at the right end of the test
  set line, always visible. Each does exactly what the card's does, and Run
  claims the test case for this test run first. A button that cannot work is
  gray with its reason in its tooltip, never hidden.
- **Rule-EDITOR-PANEL-244** — Each of the three buttons is an entry of its own
  in light mode's View menu, named as the button is, and on until the tester
  turns it off. Run and Stop are one button and one entry.
- **Rule-EDITOR-PANEL-245** — Light mode answers the keys the IDE's keymap gives
  Navigate to Test Method and Run Test Method, Shift+F5 and F5 by default, and
  F5 stops the test case while its automation runs. The tooltips name the keys,
  and the status bar lists them after the run item status keys. The buttons and
  their keys go while the failure form is open, as the run item status buttons
  do.
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
- **Rule-EDITOR-PANEL-256** — Saving the failure form writes the actual result,
  the bug severity and priority, the error and the screenshots onto the test run
  as the index holds it, never onto the copy the window was drawn from. A
  failure saved while the test run is read again, or while a sync is bringing it
  in, is kept.
- **Rule-EDITOR-PANEL-268** — The priority badge says High or Medium, and
  hovering it says Priority: High or Priority: Medium, so it is not read as the
  bug's priority, which uses the same words.

**Why 200 milliseconds.** `P` is pressed once per test case, so a test run of a
hundred test cases is a hundred of these, and the tester is watching the
application under test rather than this window. The movement has to be seen
without being waited for.

**Not everything moves.** Dragging the window's width tracks the pointer exactly —
that is the tester's hand on the edge, not a state change.

---

## What each key and button does

| Gesture                                        | What happens                                                                                                                                                                                                                                                                                                                                                                                                                 |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Start**                                      | Sets the test run to In Progress. Begins timing. Shows the first test case that has no run item status. It does exactly what the toolbar's Start button does. Opening the window does not start the test run, because opening a window should not change a test run's status.                                                                                                                                                |
| **Stop**                                       | Ends the execution flow. The test run keeps every run item status already recorded. Only the clock stops.                                                                                                                                                                                                                                                                                                                    |
| **`P` / `B`**                                  | Records the run item status on the current test case. Advances to the next test case that has not been judged.                                                                                                                                                                                                                                                                                                               |
| **`F`**                                        | Opens the failure capture in place. `Enter` saves and advances. `Escape` returns, with the test case still unjudged.                                                                                                                                                                                                                                                                                                         |
| **`Ctrl+V`**                                   | On the failure form: a picture on the clipboard is added as a screenshot, wherever the cursor is, and anything else is pasted as text into the box that has it. It does nothing when no failure form is open, so evidence cannot be attached to a test case that is about to be passed and cleared.                                                                                                                          |
| **`Escape`**                                   | Closes the window. The test run is untouched. Reopening returns to the first unjudged test case.                                                                                                                                                                                                                                                                                                                             |
| **`Ctrl+D`**                                   | Shows the detail fields, or hides them when they are shown.                                                                                                                                                                                                                                                                                                                                                                  |
| **`F5`**, or **Run Test Method**               | Runs the test case on screen's generated method, claiming it for this test run first. While it runs, the button is **Stop Test Method** and `F5` stops it. The last attempt's status is the run item status, and the walk moves on; an attempt TestNG retries keeps the window here. The key is the one the IDE's keymap gives Run Test Method.                                                                              |
| **`Shift+F5`**, or **Navigate to Test Method** | Opens the test case's generated test method in the IDE, behind the window. The key is the one the IDE's keymap gives Navigate to Test Method.                                                                                                                                                                                                                                                                                |
| **Navigate to Test Case**                      | Opens the test case in the test set editor of its own test set, behind the window. It has no key.                                                                                                                                                                                                                                                                                                                            |
| **Wheel**                                      | Zooms the test case and the failure form, inside this window only, with no modifier.                                                                                                                                                                                                                                                                                                                                         |
| **Drag an edge**                               | Width only. The left and right edges resize. The top and bottom do not. Height is whatever the content needs.                                                                                                                                                                                                                                                                                                                |
| **Show details, or a view toggle**             | The window grows or shrinks to fit. That is the only way its height changes. It changes immediately, rather than leaving a gap or a scrollbar.                                                                                                                                                                                                                                                                               |
| **Drag the bar**                               | Moves the window. Its position, width, zoom level and seven view toggles are remembered per machine, not per project.                                                                                                                                                                                                                                                                                                        |
| **Light Mode pressed again**                   | The window closes. The editor carries on from wherever the test run got to. Nothing is saved or discarded on the way out, because every run item status was written as it was recorded.                                                                                                                                                                                                                                      |
| **Window closed any other way**                | The button pops back out. That covers `Escape`, the project closing, and the test run editor's tab closing. The button shows whether the window is open, rather than remembering that someone opened it.                                                                                                                                                                                                                     |
| **Run item status set in either view**         | The other view follows it. Both read the same test run, and neither keeps a copy. So a test case judged in light mode is already judged in the grid behind it, and one judged in the grid moves the window on.                                                                                                                                                                                                               |
| **Last test case judged**                      | The test run finishes itself. Testin marks it **Completed** the moment nothing is left waiting. So this is not something the window decides. The window has already closed, so nothing is left to announce it.                                                                                                                                                                                                               |
| **Wrong run item status recorded**             | Press Light Mode to return to the editor. Correct the test case there. Press it again to carry on. The window advances only forward. The grid is where a test run is edited.                                                                                                                                                                                                                                                 |
| **Test run reaches Completed or Closed**       | The window closes, and the Light Mode button goes gray. Both ask the same question Start already asks. So a signed-off test run cannot be reopened in light mode, and cannot be started.                                                                                                                                                                                                                                     |
| **Project or IDE closes**                      | The window closes with the project. Nothing is left running behind it.                                                                                                                                                                                                                                                                                                                                                       |
| **A clock is read**                            | The whole plugin uses two formats, not four. A test run always shows hours, minutes and seconds, even at zero, so the number never suddenly gets wider. A test case shows minutes and seconds, here, in the grid and in an exported sheet. Milliseconds are measured and saved, and never shown. So a fast automated test case reads `00:00` in the grid where it used to read `84ms`. The real figure is still in the file. |
| **A test case nobody timed**                   | Blank, in the grid and the sheet. A run item status set from the menu, or set on many test cases at once, was never timed, and `00:00` would claim a measurement nobody took. A test case sitting in front of a tester at zero is the opposite: it has only just started. So light mode's clock shows `00:00`, rather than going blank for its first second.                                                                 |

---

## Why the window is built this way

Each is a choice, not a default.

### It is a window in Testin, not a second plugin

One download, one license. The window reads the test run through the same code
the rest of Testin uses. A separate plugin would need its own way to find a test
run on disk, and having two ways is exactly what Testin's file rule exists to
prevent. It can still be split out later, once its edges are clear.

### It needs a test run open

The window executes a test run that already exists. Exploratory testing creates
test cases as it goes. That is a different feature, with a different shape.
Folding it in here would make both worse.

### The toolbar button stays pressed while the window is open

The button is a toggle, not an opener. It is pressed for exactly as long as the
window is there. It checks whether the window is open, rather than remembering
that someone opened it. So it pops back out on its own however the window
closes: by `Escape`, by the test run being signed off, by the editor's tab
closing, or by the project closing. Nobody had to write four separate pieces of
code for that. Start and Stop already work the same way. A second copy of an
answer is always the one that ends up wrong.

### The window asks the test run whether it is running

Whether Start or Stop shows is asked of the test run each time. The window never
remembers it. A remembered answer is a second thing to keep in step, and the one
that fell out of step would leave a Stop button on a finished test run. Light
mode is a second view of one test run, so it asks the same question the editor
asks, and the two can never disagree.

### It only goes forward

Recording a run item status moves on to the next test case that has none. There
is no way back to the last one. That is the window's whole shape: one test case,
one decision, on to the next. The test run editor's own menu goes both ways,
because that is where a test run is edited rather than run.

Correcting a run item status is an editing gesture, so it happens where editing
lives. The toolbar toggle makes that cheap. The tester presses Light Mode to drop
back into the grid, fixes the test case, and presses it again to carry on. A
Back button here would turn this into a second, smaller editor, inside a window
built to be no such thing.

### Closing it abandons nothing

Every run item status is written as it is recorded. So there is no session to
resume. Reopening returns to the first test case with no run item status. That
is why `Escape` needs no confirmation.

### There is no finished state, because the window is gone

A test run that has judged every test case finishes itself. Testin marks it **Completed** as soon as nothing is left
waiting. Completed is the end, and
three things already ask whether a test run has reached it:

- Start refuses
- the Light Mode button grays out
- the window closes

One question, three answers. There is no fourth state to draw, and no "well
done" screen to write. A window left open after its test run had ended would
offer run item statuses that nothing could take.

### The view menu chooses parts, not fields

[#13](https://github.com/MutebAlMughyiri/test-in/issues/13) asks for a control over which
test case fields appear. The menu chooses between the things the window itself
holds instead. What sits behind Details stays fixed: the badges, the steps, the
test data and the pre-conditions. Switches over parts of a window cannot grow
past what the window holds. A list of 18 switches over test case fields is a
settings screen, inside a window built to have none. If the wrong four sit
behind Details, the fix is to change those four.

### Details open with a key, and that costs something

Removing the button removes a permanent row. The window's whole argument is that
it holds one test case and nothing else. One key shows and hides the details,
`Ctrl+D`, which does the opposite of whatever the window is showing.

The status bar teaches it. It is the row that answered this. Before it,
removing the button left a shortcut that nothing on screen mentioned. A
tester who did not read the documentation would never have found it.

**One warning.** `Ctrl+D` already means something else in the create-test-case
dialog. It is a different window, so the two never clash, but they do mean
different things. The keyboard reference should say so, rather than leave a
tester to find out.

### The tester sets the width. The content sets the height

Only the side edges resize. Height is never dragged. It is exactly what the
window is holding. It changes when the tester opens details, or turns a part
off. Those are the gestures that actually change how much there is to show.

That removes two problems, not one. A window taller than what it shows has a
strip of empty gray under the run item status buttons, and looks broken. A
window shorter than what it shows needs a scrollbar, and a scrollbar in a window
showing one test case means scrolling to read the thing you came for. Neither
happens, because the tester cannot set the height.

**Nothing stops the window growing past the screen.** It grows to fit what it
holds, so a very long description at a large size makes a window taller than
the screen. Stopping it would cut the test case off, which is worse. Letting it
scroll puts a scrollbar in the one window built so nothing has to be scrolled.
So it is written down rather than fixed, as finding 54 in
[#66](https://github.com/MutebAlMughyiri/test-in/issues/66).

### This window keeps its own zoom

Everywhere else in Testin, zooming follows one number: the IDE's editor font
size. It changes the font everywhere in the IDE, so the grid, the details panel
and every editor move together. This window does not join them.

The difference is the feature. A tester using light mode is not looking at the
IDE. That is why the window exists. One size makes a test case readable across a
desk, over the top of a browser. Another makes a grid readable on a monitor
nobody is facing. The two have nothing to do with each other. If they moved
together, every zoom here would quietly rearrange the work waiting behind it.

### The clocks never compete with the test case

The time is recorded for the report. It is not shown to hurry anybody. It stays
small and gray at the bottom. It never takes the highlight color, which is kept
for the test case being worked on. A window that puts a climbing number in front
of a tester is asking them to go faster. A tester going faster is the failure
this window was built to avoid.

### A run item status color means a run item status was given

Green, red and amber belong to a test case that has been judged. That is what
they mean everywhere else in the plugin: in the grid, in the tree and in every
report. The three buttons at the bottom of this window are offers, not outcomes.
Painting them permanently in all three colors would make color mean "button"
instead. A tester scanning for color would learn nothing from it. They might
read the strip as the test case's status.

So the run item status buttons are one color, and the words tell the three
apart. Color returns on the failure form, where **Failed** has actually been
chosen. The window never shows a test case that already carries a run item
status, so there is nothing else for color to say.

---

## How it was tested

Almost nothing in this window can be tested automatically. It is a window of
its own, with keys, resizing and a layout all wired by hand. It was tested by
hand instead, in a trial copy of the IDE.

**The first pass found six defects.** No test could have caught any of them.
Three of the six:

- the test case was drawn in the IDE's menu font, not the editor's font
- Start and Stop drew the IDE's arrow for running code
- the window stayed open after a test run was signed off

**Reading the code later found two more.** A right click on a run item status
button recorded that run item status. With one test run open in two editors,
the toolbar button updated the wrong one.

---

*The colors are Testin's own green, red and amber, the same three the run item
statuses use everywhere else, and the same blue the reports use.*

---

[Documentation](../README.md) › [The editor panel](main.md)
