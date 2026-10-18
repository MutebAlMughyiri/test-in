---
name: a-button-is-a-class
description: Every button a tester clicks is a class of its own, shaped like the editor toolbar's - it extends AbstractIconButton, takes what it needs in its constructor, and owns its words, its picture, its click and its updateState. Buttons with the same or a similar purpose (Select All / Unselect All, Collapse All / Expand All) are one button, so one class that swaps its own word and picture. Use before adding a button, a toggle or an icon control to any surface - a toolbar, a dialog, a section header, a panel - and whenever you are about to write AbstractIconButton.of(...) or a flip/show pair of methods inside the class that holds the button.
---

# A button is a class

> *"[I] notice you did not create new class for all of new actions. also, select
> all is not in separate class. every class should [have a unique]
> responsibility. [I] think it is better to make each button in separate class"*
> and *"look at our editor toolbar, its structure is each button in separate
> class. [I] design[ed] it like that by my hands"*
>
> - Muteb, 18 October 2026

A button is not a few lines inside the thing it sits on. It is one job - a word,
a picture, what a click does, and what it shows in each state - and a job gets a
class. The surface that shows the button lists it, and does nothing else for it.

## The shape to copy: the editor toolbar

`editor/toolbar` is the model, designed by hand. Every button there is its own
class, and the toolbar only lists them:

```java
public @NotNull List<ToolbarItem> getCustomComponents() {
    return List.of(
            new StartExecutionButton(editor, getCallbacks()::onStartExecutionClicked),
            new LightModeButton(editor),
            new RefreshButton(getCallbacks()::onToolBarRefreshButtonClicked));
}
```

```java
public class RefreshButton extends AbstractIconButton implements ToolbarItem {
    // UC-EDITOR-PANEL-027
    public RefreshButton(final @NotNull Runnable onToolBarRefreshClicked) {
        super(Bundle.message("toolbar.refresh"), AllIcons.Actions.Refresh);
        addActionListener(_ -> onToolBarRefreshClicked.run());
    }
}
```

A button that changes with its surface has an `updateState()` that says which
word and picture it shows now - `LightModeButton.updateState()` is the model.

## The rule

1. **One button, one class**, named for what it does and ending `Button`,
   extending `AbstractIconButton`, in the package of the surface it belongs to.
2. **Its constructor takes what it needs** and nothing more: a `Runnable` for
   the click, the editor, the tree. `super(...)` gives it its first word and
   picture, and the constructor wires its own click.
3. **Buttons with the same or a similar purpose are one button, so one
   class.** *"still button is a class[,] but we have multi buttons do the same
   or similar purpose so it will be [dealt with] as one button"* - Muteb, on 18
   October 2026. Select All and Unselect All are one purpose, choosing what is
   ticked: `SelectUnselectAllButton`. Collapse All and Expand All are one,
   choosing what is open: `CollapseExpandButton`. The class is named for the
   purpose, one `updateState()` with one `if/else` picks the word and picture,
   and the click does the one that is showing - so it can never show both, or
   neither.
4. **A fact its owner holds comes in through the constructor, not by reaching
   in.** `SelectUnselectAllButton(this::everyShownTicked, this::flipTicks)`:
   what is ticked is `SelectionTree`'s fact (Disabled test cases, hidden ones),
   so the tree keeps it and hands the button the question and the click. The
   owner calls `updateState()` when that fact changes.
5. **A fact only the component holds, the button follows itself.**
   `CollapseExpandButton(tree)` lists the folders, listens to the tree's
   expansion and model changes, and updates itself; `SelectionTree` only adds
   it.
6. **The surface decides membership, never the call site.** `SelectionTree`
   puts both buttons in its own header, so every dialog built on it has them.
   Only a member that belongs to one caller comes in from outside, the way the
   test run form passes in its `FilterPopupButton`. Read
   `one-surface-many-states` beside this.
7. **Tests find a button by its class**, not by its position:
   `filter(CollapseExpandButton.class::isInstance)`. A test that takes "the
   first icon button" breaks the day another is added before it.

## What it cost to learn

#441 added Collapse All to the Create Test Run dialog's test case list. The first
build put it inside `SelectionTree`, beside Select All, which was already there
the same way: two `AbstractIconButton.of(...)` fields, a
`flipFolds`/`showFolds`/`everyFolderOpen`/`foldersOf` set next to
`flipTicks`/`showTickAll`, a folder list and an expansion listener. It worked
and passed its tests, and `SelectionTree` had three jobs: keeping ticks across
filters, Select All, and Collapse All.

The next try gave every action a class: `CollapseAllButton`, `ExpandAllButton`,
and a `TreeFolders` so the two could share "is every folder open", with Select
All to follow as two more. It measured about 236 lines in five classes, and the
show-or-hide condition was written twice - `setVisible(x)` in one class,
`setVisible(!x)` in the other - so changing one and not the other shows both
buttons or neither. One class per pair came to about 115 lines in two classes,
with one `if/else` per pair, and that is what was built. `SelectionTree` ended
shorter than before the feature began.

## The signals

- `AbstractIconButton.of(...)` in new code. It exists for the few inline buttons
  that predate this rule; a new button never uses it.
- An anonymous `new AbstractIconButton(...) { }` or `new JButton(...)` in a
  surface class.
- A pair of methods in a surface named for a button - `flipX()` and `showX()`,
  `toggleX()` and `updateX()` - or a field holding one button's state.
- A surface calling `button.show(...)` with words it chose.
- Two classes for one purpose, each hiding itself when the other shows.
- A test helper that finds a button with `findFirst()` over every icon button.

## When a surface already holds a button inline

Move it out before adding another beside it: the words, picture, click and
`updateState()` go to the new class; the fact the button asks about stays with
its owner and comes in through the constructor. Behavior does not change, so the
tests that already cover the button are the proof, once their helpers find it by
its class.

Known on 18 October 2026, built before this rule, and a finding rather than
work for an unrelated change:

- Inline buttons, each through `AbstractIconButton.of(...)` in `ui/framework`:
  `FieldPair` (the layout switch), `Maximized`, `MultiLineField` (the format
  button) and `ScreenshotStrip` (remove a screenshot).
- Buttons of one purpose kept as two classes in the editor toolbar:
  `CardViewButton` and `GridViewButton` (choosing the view; the toolbar shows
  one at a time) and `StartExecutionButton` and `StopExecutionButton` (running
  a manual execution).

## Checks before adding a button

1. Is there already a class for this button, or one shaped like it? Copy it.
2. Does another button already serve the same or a similar purpose? Then it is
   that button's new state, not a new class.
3. What does it need to know, and who owns that fact? The owner's fact comes in
   through the constructor; the component's fact the button follows itself.
4. Which surface lists it, and would every user of that surface want it? Then
   the surface adds it, not the caller.
5. Does a test find it by its class?
