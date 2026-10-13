[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-007

Every color, border and painting this framework does itself, and what the platform
ships instead, is listed in [What Testin draws itself](customizations.md).

# UC-INTERNAL-007: Answer any Testin dialog

> There is no key for this. It is what happens in every dialog Testin opens.

**As a** tester, **I want** every Testin dialog to work the same way, **so that**
I learn it once and never wonder what a new one will do.

Testin opens a lot of dialogs — creating a node, renaming one, ordering it,
choosing what to export, answering a conflict, writing a failure. They are one
dialog with different contents. A dialog says what it is called, what it holds
and which keys it answers, and the shell builds the rest.

## Rules

- **Rule-INTERNAL-053** — Every Testin dialog has the same three parts: a title
  at the top, its fields stacked down the middle, and a strip along the bottom
  naming the keys that work right now.
- **Rule-INTERNAL-054** — The strip and the keys come from one declaration. A
  key named on the strip works, and a key that works is named on the strip.
  Neither can exist without the other.
- **Rule-INTERNAL-055** — A key on the strip works wherever the cursor is inside
  the dialog, not only in the field that has it.
- **Rule-INTERNAL-056** — One key does one thing. A dialog that gave two
  meanings to one key does not open at all.
- **Rule-INTERNAL-057** — The first field that can take the cursor has it when
  the dialog opens, so the tester can type straight away.
- **Rule-INTERNAL-058** — `Tab` moves to the next field in the order the fields
  are drawn, and `Shift+Tab` back to the one before.
- **Rule-INTERNAL-059** — `Escape` closes the dialog at once and saves nothing.
  Whether a dialog asks first is decided for that dialog: the failure form never
  asks (Rule-EDITOR-PANEL-144), and no other dialog asks yet.
- **Rule-INTERNAL-060** — A field's own way of saying yes does what the dialog's
  confirming key does. Clicking a row in a list, or pressing the dialog's
  button, is the same as pressing `Enter`.
- **Rule-INTERNAL-061** — A dialog opens in the middle of the window at the size
  its contents need. Two things make one movable and resizable: asking for a size
  in pixels, or saying it is resizable and keeping the size its contents give. The
  second is for a dialog whose rows decide how tall it is — a details popup grows
  with what it lists, and a fixed height would squeeze its chart out below the
  rows.
- **Rule-INTERNAL-067** — A dialog says why it will not take what was typed and
  stays open with the value still in the field. An empty field is marked as the
  one holding the dialog open; a value the dialog refuses for any other reason
  is refused in one sentence naming the value.
- **Rule-INTERNAL-075** — A dialog of a kind already on screen is brought
  forward rather than opened again. Testin dialogs do not close when they lose
  the focus, so nothing else would have stopped a second one. A dialog that holds
  nothing the tester typed - a confirmation, a node's details, a screenshot - is
  replaced by the newer one instead, closed unanswered. So an older question can
  never be answered by the key meant for a newer one.
- **Rule-INTERNAL-076** — A dialog says whether clicking away closes it. Almost
  none do - one holding what the tester typed must not lose it to a stray click,
  and Escape is what cancels. The search does, because it holds a question
  rather than an answer, and so does the Details dialog, because it only reads.
- **Rule-INTERNAL-077** — Every icon a framework surface draws is gray. A stock
  platform icon may ship colored, and one colored glyph among gray ones is the
  loudest thing on the row. A color that means something - an error, a run item
  status - is not an icon that names a thing and is drawn as it is.
- **Rule-INTERNAL-078** — The shortcut strip is one row. A dialog narrower than
  its own hints shortens the strip rather than folding it onto a second line and
  growing a line taller to hold it.
- **Rule-INTERNAL-079** — A surface has one shortcut strip. It cannot be built
  as half of a pair, so no dialog is two tinted rows tall to say six words.
- **Rule-INTERNAL-080** — A button a dialog will not act on yet is drawn
  disabled, and the reason is the gray button's tooltip: hovering over it says
  why, and nothing is printed beside it. The line beside the button carries one
  thing, what pressing it would act on - *5 test cases in 2 test sets* - and it
  stays the same whether the button is ready or gray. Every dialog button takes
  this from the one shared button, so no dialog prints its own reason.
- **Rule-INTERNAL-085** — In a box that offers a list and can also be typed
  into, Enter picks the value under it while the list is open. While the list is
  closed, Enter is the dialog's own key, as it is in every other field.
- **Rule-INTERNAL-087** — A caption sits on its own line above the field it
  names, in the caption font: JetBrains Mono, two points below the dialog's
  label font, in capitals, in the muted caption gray. There is no caption
  column to line up: a caption starts where its field's frame starts, so one
  edge runs down everything in a card. A card's own caption carries a hairline
  from the end of its text to the card's far edge, and that is the only line
  inside a dialog.
- **Rule-INTERNAL-095** — Every font a tester reads comes from one owner,
  `org.testin.util.Fonts`. It names each role - title, strong, body, label,
  badge, panel caption, field, placeholder, value, choice, option, caption, row,
  small, small strong, hint, keycap, figure and icon letter. It derives them all
  from the same two sizes: the editor's, which the panels zoom with, and the
  IDE's label font, which the dialogs follow. A surface asks for the role it is
  showing and never derives a font of its own, so a title is the same size in
  every panel and a placeholder the same in every dialog. The documents Testin
  writes are in it too. `ReportFont` holds the point sizes a PDF and a Word file
  are set in, the pixel sizes an HTML report uses, and the families all three
  are written in. So a size changes in one place, or it disagrees with itself in
  three.
- **Rule-INTERNAL-096** — Every typing surface in a dialog is drawn in the same
  frame, whether it holds one line or many: a text area sits in the frame a text
  field has, not in a borderless well. A value the dialog shows read-only is set
  in the size a field would show it in, so a test case's description above the
  fields is not smaller than the answer being typed under it. A cell inside a
  table is not a typing surface of the dialog and does not take this. It takes
  the table's own font, colors and row height, whether it is the tick box in a
  header, the choice box that opens on a Priority cell or the button that opens
  the group picker. A field's font is six points above the IDE's label font, and
  a cell set in it would jump size the moment a tester clicked into it and stand
  taller than the row that holds it.
- **Rule-INTERNAL-097** — A box that holds many lines grows as lines are added,
  and whatever draws it grows with it rather than letting it scroll inside a
  fixed box. Every such box is the same box: the same frame, the same font, Tab
  leaves it, and Ctrl+Enter adds a line. Enter belongs to whoever draws the box.
  A dialog saves with it, and light mode's window saves and moves to the next
  test case. So a box asks its host to bind Enter rather than binding it itself,
  and a host with other plans for the key keeps it.
- **Rule-INTERNAL-099** — A section inside a dialog is a card: rounded, with the
  dialog's ground showing around it and between it and the next card, and a
  hairline at its edge. A theme names two surfaces, its panel and its content
  color. The card takes the content one - the darker of the two in a dark theme,
  the lighter in a light one - and the ground takes whichever is left. The card
  then reads as content in every theme without either color being invented here.
  Which of the two is darker is the theme's business, and it differs: the Islands
  themes paint their panel darker than their content, every other bundled theme
  the other way. The edge is what tells a card from the ground rather than the
  fill, because a theme may paint both surfaces nearly the same and Islands
  light does. One place decides the surfaces, the edge, the corner and the gap,
  for every dialog and every theme.
- **Rule-INTERNAL-100** — A dialog the tester can resize is six tenths of the
  IDE frame wide, every one of them, because a width chosen per dialog is a
  number nobody chose with the others. What a dialog names is how tall it is:
  half the frame for a form with a list under it, or seven tenths for an image
  or a document read top to bottom. A dialog that names nothing is as tall as
  its content needs, and grows as the tester opens more of it. The share is
  clamped: never narrower or shorter than the dialog needs to show its content,
  never past the frame less a margin. The frame wins when both cannot hold. A
  dialog whose content is larger than the frame is the frame's size, not its
  content's. A dialog the tester cannot resize names no size and is as big as
  its content, so a confirmation holding one sentence stays the size of that
  sentence.
- **Rule-INTERNAL-101** — A dialog whose size the tester can change can be maximized, from a
  button in its title bar. Maximize fills the IDE frame and the button pressed
  again returns the dialog to the size and place it opened at, with everything
  typed and checked still in it. A dialog that grows as it is typed into stops
  growing while it is maximized, because a dialog filling the frame is the size
  the tester asked for. No dialog can be minimized: a dialog is a popup
  and has no taskbar entry of its own to minimize into.
- **Rule-INTERNAL-102** — A dialog made smaller than its content scrolls rather
  than clipping it: a vertical scrollbar appears and the status bar stays where
  it is. Only a dialog whose size the tester owns can be made smaller than its
  content, so only that dialog scrolls. One that opens at the height its content
  needs and cannot be resized has nothing to scroll, and a scrollbar there is a
  stray. While the dialog has room, the content takes the whole height and the
  tree or table inside it scrolls on its own. A tree or table asks for eight
  rows, so a dialog opens at the height its form needs rather than the height
  its rows would take. A box that takes several lines shows six of them and
  scrolls past that, unless the dialog tells it otherwise (Rule-INTERNAL-136),
  so one long value cannot push the rest of the dialog out of sight.
- **Rule-INTERNAL-103** — A box that spell-checks what is typed into it
  underlines a misspelled word and shows nothing else. No bulb, no icon and no
  button offers the corrections: the underline says there is something to
  correct and Alt+Enter offers it, the same key that offers a correction
  anywhere in the IDE.
- **Rule-INTERNAL-104** — The keys along the bottom of a dialog never make it
  wider. They stay on one line and a key with no room is simply not drawn,
  because a dialog is sized by what the tester fills in, not by how many keys it
  can be answered with. Widening the dialog shows the rest.
- **Rule-INTERNAL-105** — Color marks what acts, and nothing else. A dialog has
  one accent. It goes on the things a tester touches: the button that confirms
  it, the field holding the keyboard, the answer picked, the row selected. Every
  surface stays neutral. A dialog that colors its
  furniture has to explain itself with hints; one that colors only what acts
  tells the tester where to click and what to fill without a word.
- **Rule-INTERNAL-106** — A row a tester picks out of a list is marked by a
  rounded band inside the card that holds it, never a bar running edge to edge.
  The band is the thing being chosen; the card is not. What a row says about
  itself - a hint in a picker list, a path in a search row - sits in gray right
  against the name it belongs to, whatever length the name is. Every list draws
  its rows the same way, so there is no column to line it up in anywhere (Rule-INTERNAL-074).
- **Rule-INTERNAL-107** — A closed set of answers is shown as radios with the
  ordinary answer already picked, never a combo box a tester has to open to
  learn what the answers are. The set is closed when the code names it: a report
  of four formats, an export of four, a test run of platforms and browsers. A
  tester then reads every answer at once and clicks nothing for the ordinary one.
  Because an answer is always picked, there is no refusal for not picking.
- **Rule-INTERNAL-108** — A dialog names a node by its place in the test
  project, not by its path on disk: the segments below the Testin root, a
  chevron between them. The root itself is its folder name, and anything
  outside it is shown in full.
- **Rule-INTERNAL-109** — Where something is going is one row: where it is, an
  arrow, then where it lands, with the segment it gains in the ordinary text
  color while the rest stays gray.
- **Rule-INTERNAL-110** — A field that takes one shape of value, such as a
  position that is a whole number from 1, refuses any keystroke that would break
  the shape. What it holds is never wrong, so it is never refused afterward.
- **Rule-INTERNAL-118** — A dialog builds no input of its own. A field that
  holds a folder or a file is the framework's text field with a browse button
  inside it, at the field's right edge. The button opens the platform's chooser
  at the path the field holds, and puts the chosen path in the field.
- **Rule-INTERNAL-119** — Every icon button Testin draws is the framework's one
  icon button, in a toolbar, a status bar, a dialog's title bar or beside a
  screenshot. It shows the icon alone at rest, a gray rounded fill under it
  while the pointer is on it, and the fill a shade darker while it is on. A new
  button takes this button rather than drawing its own.
- **Rule-INTERNAL-120** — Every file in the dialog framework has a row in its
  register. The row says whether the file is kept because two or more screens
  ask for it, kept on purpose for the one screen that does, or internal to the
  framework. A file added or removed changes the register in the same change.
- **Rule-INTERNAL-121** — Every gap and padding in a dialog is one of five
  steps, named once in Spacing: XS 4, S 6, M 8, L 10 and XL 12. A screen asks
  for a step by name and never writes a number, so changing a step changes every
  dialog at once.
- **Rule-INTERNAL-122** — Everything a tester can reach with the keyboard tells
  a screen reader its name, in the words it already draws. A field is named by
  its caption, as it is written rather than in the capitals it is drawn in, or
  by its hint when it has no caption. A table with neither is named by its
  column headings, an icon button by its tooltip and a view panel tab by its
  title. A card says its title, then its badges and the details it shows, so a
  run item status shown on a card is heard with it. A grid cell says its value,
  and the box a cell opens for typing says its column. The card list and the
  grid are both named Test cases, and the explorer tree by its tool window.
  Moving through a search's results with the arrow keys says the result
  reached, because the keyboard stays in the search box.
- **Rule-INTERNAL-132** — A date on a screen shows only its day, as 17-09-2026,
  and hovering it shows the full date and time it was stored with, as Thursday
  17-09-2026 At 06:10:00 [Asia/Riyadh]. That holds on the cards and in the grid
  of both editors, in the details panel, in History and in a node's details. A
  Git commit's time is shown in the IDE's own time zone, the zone Testin stores
  its own dates in. Reports, bug issues and exported files keep the full text,
  because nothing can be hovered there.
- **Rule-INTERNAL-133** — A section that opens and closes shows its chevron in
  the accent color, pointing down while it is open and right while it is
  closed, and ends its line with the word *Collapse* or *Expand*, saying what a
  click does, and hovering anywhere on that line says it too: *Click to
  collapse* or *Click to expand*. That holds wherever Testin draws one, in a
  dialog and in the view panel alike. The chevron is what a tester clicks, so
  it carries the color, and the caption and the word beside it stay neutral.
- **Rule-INTERNAL-134** — A text field in a Testin dialog never opens IntelliJ's
  Find or Replace bar. A key IntelliJ would give to it either belongs to the
  dialog, as Ctrl+R opens the reference in the test case dialogs, or does
  nothing.
- **Rule-INTERNAL-136** — A box that holds many lines can be told the fewest
  lines it shows, the most it shows, and whether its spelling is checked. Told
  nothing, it starts at its content, stops at six lines with its scrollbar, and
  checks spelling, as every box did before. A box with no upper limit grows with
  what is typed or pasted until the dialog fills the IDE frame. Past that it
  scrolls, under a scrollbar drawn over the text that takes no width of its own.
- **Rule-INTERNAL-137** — Two boxes can be paired, stacked or side by side, and
  a button in the dialog's title bar switches between the two. The button shows
  the layout it switches to: side by side while the boxes are stacked, stacked
  while they are side by side. Paired boxes start stacked, and open the next
  time the way the tester left them. Each box sits in a section that opens and
  closes, both open to start. A box that grows never pushes the dialog past the
  frame: it stops at the height the frame has left and scrolls there.
- **Rule-INTERNAL-138** — A dialog can add buttons to its title bar, beside the
  maximize button. A dialog that adds none shows maximize alone, as before.
- **Rule-INTERNAL-139** — A box that holds code is an editor for that code's
  language, JSON for an API body, and shows it in the colors the IDE's editor
  gives that language. Each section of a field pair carries a Format button in
  its header, beside Collapse, named for its box's language. It lays the text
  out with the IDE's own Reformat Code and the tester's code style for that
  language, so only whitespace changes.
- **Rule-INTERNAL-140** — A dialog's buttons sit in one row at its foot: its
  other actions first, then the one that confirms, drawn as the primary and
  answering Enter. Each other button does its own action and closes the dialog.
  An action with a key is also on the strip; one without is a button only.

These rules are about the shell every dialog is built on. What each dialog
holds, and what its keys mean, is on the page for that dialog.

## The register

Every file in `org.testin.ui.framework`, and why it is there (Rule-INTERNAL-120).
A screen asks the framework for what it needs by name; a part nobody else will
ask for lives with its one caller. Counted on 2 October 2026 by how many classes
outside the framework name each file.

| Standing                                           | Files                                                                                                                                                                                                                                                                                                                                                                                                                             | Why                                                                                              |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| **Kept** - two or more screens ask for it          | `DialogPart`, `StatusBarShortcut`, `AbstractFrameworkDialog`, `AbstractIconButton`, `TextInput`, `DialogSize`, `DialogComponent`, `ConfirmDialog`, `RadioSelection`, `Prose`, `SelectionTree`, `DialogDetailRow`, `MultiLineField`, `Answer`, `TextFieldWithSelections`, `RowStripe`, `DialogHost`, `DialogButton`, `ShortcutMenuPopup`, `PlainTextArea`, `ShortcutStrip`, `SelectionTable`, `Alternative`, `Spacing`, `CardEdge` | The system's parts                                                                               |
| **Kept on purpose** - one screen asks for it today | `Selection` (Global Search), `DialogSplitButton` and `ChoiceInput` (Pending Commits), `Screenshots` (the failure form), `Picture` (a stack trace line), `ScreenshotImages` (a stack trace line's thumbnail), `Keycap` (light mode's keys), `DialogPlace` (the rename card), `DialogKeys` (the letter menus), `TextValue` (the import preview), `HtmlPage` (a Testin Help guide), `FieldPair` (the API model dialog)               | Each is the one way to ask its question; folded into its caller, the second caller would copy it |
| **Internal** - reached through a builder           | `ButtonFooter`, `TextInputField`, `ScreenshotStrip`, `ShortcutMenuRenderer`, `SelectionSource`, `DialogsOnScreen`, `EmptyWarning`, `DialogDto`, `DialogDetails`, `DialogMessage`, `ConfirmCard`, `Option`, `Maximized`, `TitleButtons`                                                                                                                                                                                            | Parts of the parts                                                                               |

### The kinds of dialog

Four kinds, written down so a new dialog is shaped like the others of its kind.
They are a catalog, not code: no dialog declares its kind.

| Kind        | What it asks                              | What the kind fixes                                                                                   | Dialogs                                                                                                                                                                                                                                                          |
|:------------|:------------------------------------------|:------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Form**    | Values to type or choose, then one action | Fields stacked; one blue main button; `Enter` runs it; as tall as its content unless it names a size  | Create Project, Create Test Run, Create Test, Create Test Case, Update Test Case and its nine bulk-section editors, Rename, Order, Git Identity, Remote URL, Test Run Configuration, Result Analysis, Report Bug, Failed Result, Import, Export, Generate Report |
| **Confirm** | Yes or no about a stated change           | The message, the change from and to, one named action; `Escape` cancels unless a second answer has it | `ConfirmDialog`, from nine places                                                                                                                                                                                                                                |
| **Picker**  | One or more things chosen from a list     | A search or a list fills the space; `Enter` picks; a share of the window                              | Global Search, Bind Test Project, Pending Commits, Resolve Conflict                                                                                                                                                                                              |
| **Viewer**  | Nothing; it shows something to read       | Read-only; no main button; `Escape` closes; can be maximized                                          | Screenshot, Stack Trace, Marker Details, Agent Said                                                                                                                                                                                                              |

### Outside the framework, on purpose

| Surface                                  | Owner                     | Why it stays outside                                                                                                              |
|:-----------------------------------------|:--------------------------|:----------------------------------------------------------------------------------------------------------------------------------|
| The Fields popup on the editor toolbar   | `AbstractFieldsPopupBtn`  | A toolbar control, not a question                                                                                                 |
| The Filter popup                         | `FilterPopupBtn`          | The platform's own action-group popup. One menu for the two editors and the Create Test Run dialog, each answering `FilterSource` |
| The zoom indicator in light mode         | `ZoomIndicator`           | An indicator that asks nothing                                                                                                    |
| The group picker in an import table cell | `GroupMultiSelectEditor`  | A table cell editor, which takes the table's look                                                                                 |
| Balloons                                 | `Notifier`                | The platform's notifications                                                                                                      |

## What the tester sees

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Node                                       (1)  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [set]  set name...                                     (2)  │
│                                                              │
│  [set]  Test Set          Holds test cases              (3)  │
│  [pkg]  Test Set Package  Groups test sets                   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    ↑ ↓ Select    Escape Cancel           (4)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — what this dialog is for, in a few words.
2. **The first field** — it has the cursor the moment the dialog opens.
3. **The rest of the fields**, stacked below it in the order they were
   declared. One of them takes any space left over; the others keep the height
   they need.
4. **The strip** — every key that works right now, and what each one does. Some
   entries are only a hint: `↑ ↓ Select` names keys the list answers itself,
   and the strip shows them so the tester knows they are there.

## Main flow

1. The tester opens a dialog, from a key or a menu.
2. It opens in the middle of the window, with the cursor in the first field.
3. The tester types, and moves between fields with `Tab`.
4. The tester presses a key from the strip, or clicks a row.
5. The dialog does that one thing and closes.

## What Testin refuses

**If the tester presses `Escape`** — the dialog does what its strip names for
`Escape`, and closes at once. Almost everywhere that is Cancel, and nothing is
saved. No dialog asks first, even when something was typed, and what was typed
is gone: Decision-015, after the failure form (Rule-EDITOR-PANEL-144) and Report
Bug (Rule-VIEW-PANEL-070). Two dialogs give `Escape` an answer of its own, and
the strip says which: the merge question skips the file (Rule-SHARE-084), and
the question about bodies the tester wrote leaves them as they are (Rule-CODEGEN-091). The platform closes a popup on
`Escape` before any key the
dialog binds is reached, so the shell answers `Escape` first, with what the
dialog declared (Rule-INTERNAL-054).

**If a dialog is written with two meanings for one key** — it does not open, and
the plugin says which key. A key that silently replaced another is the failure
this prevents, and it cannot be seen from the screen.

**If a dialog is written with no fields at all** — it does not open. There would
be nothing to put the cursor in.

## Where the plugin breaks its own rules

**Three surfaces are not built on the shell**, against thirty-four that are:
light mode's zoom indicator, the button that picks which details a view shows,
and the shortcut menu. Rule-INTERNAL-053 to Rule-INTERNAL-061 do not reach them.
They behave the same way by hand, which is the problem: each is a copy that can
drift. That is difference 6 on
[the Inside Testin page](main.md#where-the-plugin-breaks-its-own-rules), and
[#69](https://github.com/MutebAlMughyiri/test-in/issues/69).

**The test case create and update dialogs are built on the shell**, like every
other. The shell owns the popup, the title, the strip, the sizing and the
one-at-a-time rule. Each dialog owns its own sections and the keys that reach
past the editors inside them.

**The shortcut menu is the exception that keeps the promise.** It is still
hand-built — a menu is rows and nothing else, and each row carries and prints
its own letter, so the shell would give it nothing. What it does not do anymore
is answer a key it never mentions: its `Enter`, `Escape` and arrows come from
one declaration that both binds the keys and draws the strip, the same two
halves the shell uses. Rule-INTERNAL-067.

---

[Documentation](../README.md) › [Inside Testin](main.md)
