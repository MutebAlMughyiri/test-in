[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-047

# UC-EDITOR-PANEL-047: See which test cases are automated

> There is no key for this. It is drawn on every card.

**As a** tester, **I want** to see which of my test cases have automation behind
them, **so that** I can tell what an execution will actually cover without
opening the code.

The icon that jumps to the generated method now also says whether there is any
automation to jump to. The filter beside it is how a tester sees a whole test
set at once, and the status bar says the same thing as a number.

## Rules

- **Rule-EDITOR-PANEL-195** — A test case is in one of three states. Its
  generated method has something in it, or it names a method and has none, or it
  is not automated. A method is filled in as soon as its body is anything but
  the one `// TODO` line Testin wrote: a statement, a comment, or the TODO
  deleted. Until then, it is not automation: every generated method starts as
  that one line.
- **Rule-EDITOR-PANEL-196** — The state is read away from the screen. A test set
  opens at the speed it opened before, and the cards say what they know as the
  answers arrive. A test case nobody has read yet is never reported as not
  automated.
- **Rule-EDITOR-PANEL-197** — Nothing about this is saved. The state is read
  from the code again whenever the code has changed since it was last read, so a
  method written or deleted shows the next time the tester looks.
- **Rule-EDITOR-PANEL-198** — The filter offers the three states and behaves
  like the four filters beside it. Choosing none of them shows everything. It is
  offered in both editors, because a test run holds test cases too.
- **Rule-EDITOR-PANEL-210** — The test set editor's status bar says how many of
  the test set's test cases have a generated test method behind them.
- **Rule-EDITOR-PANEL-211** — That count says nothing at all until an answer is
  in, and nothing in an IDE that cannot read one. Nobody has looked yet and none
  are automated are the same arithmetic and opposite statements, so the count is
  written from how many test cases the answer covers rather than from how many
  there are.
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

## What the tester sees

```
┌──────────────────────────────────────────────────────────────┐
│▌ 1  Sign in with a correct username and password   [C] [>]   │
│     Regression                                               │
└──────────────────────────────────────────────────────────────┘
```

The two icons appear while the pointer is over the card, as they always have.
The first is the one that jumps to the code, and its shape says what is there:

|                                 |                                                                                           |
|---------------------------------|-------------------------------------------------------------------------------------------|
| a class icon                    | the generated method has something in it                                                  |
| a class icon with an error mark | the test case names a method and there is none — the automation was written and is gone   |
| a hollow class icon             | not automated. Either there is no method, or Testin wrote one and nobody has filled it in |

The same icon and the same three shapes are used in the view panel's details.

## The filter

**Automation** joins **Priority**, **Group**, **Module** and **Status** in the
filter popup, with the same three icons beside the three states. Picking one
narrows the list to it, picking two shows both, and picking none shows
everything. **Reset Filters** clears it with the rest.

This is how a tester reads a whole test set at once. The icon answers for the
card under the pointer; the filter answers for all of them.

It is offered in the test set editor and in the test run editor. A test run is
where the question matters most: a test run whose test cases have no methods
will not execute much, and the filter is how a tester sees that before starting
it.

## The count in the status bar

```
┌────────────────────────────────────────────────────────────────────────────┐
│  1 of 15 test cases     |<  <  1 of 1  >  >|   Automated 10 of 15  [ 50 ]  │
└────────────────────────────────────────────────────────────────────────────┘
```

**Automated 10 of 15** — ten of this test set's fifteen test cases have a
generated method with something in it. It counts the whole test set, not the
page in front of the tester and not what the filter has left: automation is a
property of the set, and a number that changed when a filter was switched on
would be answering a different question every time it was read.

The corner it sits in is the test run editor's run item status counts, which are
blank in a test set editor — so the two bars differ by exactly the figures
their editor has. A test run editor shows the run item statuses and no
automation count.

It is written twice, and that is deliberate: blank when the editor draws, filled
in when the reading lands a moment later. Rule-EDITOR-PANEL-196 is why — nothing
waits for the code to be read, so the bar says nothing until it has an answer
rather than claiming a set is un-automated while nobody has looked.

## Main flow

1. The tester opens a test set.
2. The cards are drawn at once.
3. Testin reads the generated code for every test case in the set, away from
   the screen. That is one class read once, whether the set holds fifty test
   cases or a thousand.
4. The status bar's count fills in.
5. Hovering a card shows its state on the navigate icon.
6. To see which test cases they are, the tester opens the filter and picks a
   state.

## What Testin refuses

**If the IDE has no Java plugin** — the icon is drawn as it always was and no
test case is reported as un-automated. Testin cannot read Java there, and a
tester who cannot generate automation has not failed to write it. The filter is
not offered, and the status bar's corner is empty rather than reading **Automated 0 of 15**.

**If a method is there but empty** — the test case is not automated. Testin
writes every method as a name and a TODO comment, so a method on its own means
somebody created the test case, not that somebody wrote the automation.

**If the IDE is still indexing** — the reading waits until it has finished.
Asking sooner answers wrongly.

**If the code cannot be read** — the icon is drawn as it always was and the
reason goes to the log. Nothing on screen claims a test case has no automation
because the reading failed, and the count stays away for the same reason.

---

[Documentation](../README.md) › [The editor panel](main.md)
