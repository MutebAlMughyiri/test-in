# What Testin draws itself

Every place the plugin builds its own color, icon, border, painting, popup or
size instead of taking the platform's. Read it when deciding whether to add one.

Measured on `af6b14a3`, over `src/main`. `testin-java` and `testin-testng` draw
nothing.

The rows are grouped by the only question that matters: **does the platform
already ship this?** Only the first group is a candidate for removal.

---

## The platform ships this

Nothing left. The six rows found on `af6b14a3` now take the platform's own
part (#348, sandbox-checked 1 October 2026):

| What                                      | Now                                                                                                     |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------|
| The grid and card selection color         | `UIUtil.getListSelectionBackground(true)`, with the selection foreground on a selected grid cell        |
| The editor row stripe                     | `UIUtil.getListBackground()` and `UIUtil.getDecoratedRowColor()`                                        |
| The bulk editor's diff tint               | The scheme's `DiffColors.DIFF_INSERTED`, so it follows the tester's diff colors                         |
| The bulk editor's caret-row fallback      | The editor scheme's default background                                                                  |
| The grid's table UI                       | `JBTable`'s own; `BasicTableUI` is gone, and a hovered row still paints nothing (Rule-EDITOR-PANEL-250) |
| A toolbar button's hover and pressed fill | `ActionButtonLook.SYSTEM_LOOK.paintBackground`                                                          |

## The platform does not ship this

| What                                            | Where                                                                                         | Why nothing fits                                                                                                                                                                                                        | Rule in docs/                                                    |
|-------------------------------------------------|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| A letter drawn into an icon                     | `util/Icons.java` `fieldLetter`, `LetterIcon` (`fieldLetter`, `LetterIcon`)                   | `com.intellij.ui.TextIcon` draws a framed letter but places it by the font's line box, so a capital sits high, and it sizes the frame from the text rather than a fixed 16 × 16 box. Tried and reverted, 1 October 2026 | Rule-EDITOR-PANEL-209, Rule-EDITOR-PANEL-232 (the letter frames) |
| The dialog card: a rounded fill plus a hairline | `ui/dialogs/SectionFill.java` `paintBorder`                                                   | `RoundedLineBorderWithBackground` is exactly this and is `@ApiStatus.Internal`. Verified in the SDK, 2026-09-27                                                                                                         | Rule-INTERNAL-099                                                |
| The unfocused field frame                       | `ui/dialogs/FieldFrame.java` `FieldFrame`                                                     | It **extends** `DarculaTextBorder` and only adds the unfocused stroke, which is the intended way to specialise a platform border                                                                                        | Rule-INTERNAL-096                                                |
| A chosen row as a rounded band                  | `ui/framework/TextFieldWithSelections.java` `SelectionRenderer.paintComponent`                | A list selection is a rectangle everywhere in Swing. Rule-INTERNAL-106                                                                                                                                                  | Rule-INTERNAL-106                                                |
| Badge pills                                     | `ui/Badges.java` `fillPill`, `ui/Tag.java` `paint`                                            | No platform pill. The shape carries severity and priority together                                                                                                                                                      | Rule-EDITOR-PANEL-252, Rule-EDITOR-PANEL-253                     |
| The run item status donut                       | `view/marker/RunItemStatusDonut.java` `paintComponent`, `view/marker/Swatch.java` `paintIcon` | No platform chart of any kind                                                                                                                                                                                           | None                                                             |
| The grid's selected-cell edge                   | `editor/grid/SelectionCellBorder.java` `paintBorder`                                          | A table cell border is the caller's in Swing                                                                                                                                                                            | None                                                             |
| Light mode's key button                         | `lightmode/KeyBtn.java` `paintComponent`                                                      | Related: `ui/framework/Keycap` draws the same idea for dialogs, so **these two are each other's duplicate**, not the platform's                                                                                         | None                                                             |

## Decided, not drift

These are recorded decisions. They are listed so nobody "cleans" them.

| What                                                          | Where                                    | The decision                                                               | Rule in docs/     |
|---------------------------------------------------------------|------------------------------------------|----------------------------------------------------------------------------|-------------------|
| `GRAY`, `RED`, `GREEN` are a `JBColor` whose halves are equal | `util/Icons.java` `GRAY`, `RED`, `GREEN` | Testin's icons are one color in both themes, on purpose - the pair says so | Rule-INTERNAL-077 |
| The dialog surfaces                                           | `ui/dialogs/DialogStyle.java` `CONTENT`  | Two named theme colors, chosen per theme by luminance. Rule-INTERNAL-099   | Rule-INTERNAL-099 |

## Not customizations at all

Counted during the sweep and dismissed, so the next sweep does not re-raise them.

| What                         | Count | Why it is not                                                                                                                                                                                                                         |
|------------------------------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Cell renderers               | 5     | Every one extends a platform renderer (`ColoredListCellRenderer`, `ColoredTreeCellRenderer`, `TableCellRenderer`). That is the platform's own extension point                                                                         |
| Popups                       | 6     | `DialogStyle.createPopupBuilder` is the framework's one owner; `FilterPopupBtn` uses the stock `createActionGroupPopup`; the other four hand-build because they attach under a component, which `AbstractFrameworkDialog` does not do |
| Components in `ui.framework` | 35    | The thing that makes every dialog agree                                                                                                                                                                                               |
| Font roles in `util.Fonts`   | 22    | One owner for every font a tester reads. Rule-INTERNAL-095                                                                                                                                                                            |
| Rules in `tools/inspect.ps1` | 12    | They exist because no IntelliJ inspection covers them                                                                                                                                                                                 |
| Fixed sizes                  | 5     | `EditorStatusBar:188`, `AbstractToolbarPanel:92`, `LightModeScreen:665`, `TestCaseForm:74`, `StatusBarBase:46` — each pins one dimension so a row cannot grow with its content. Worth re-reading, but they are layout, not drawing    |

---

## The two questions to ask before adding one

1. **Has the platform got it?** `AllIcons` for icons, `JBUI.CurrentTheme` and the
   theme's named keys for colors, `JBUI.Borders` for borders,
   `ColoredListCellRenderer` and friends for renderers, `JBPopupFactory` for
   popups. Search before concluding it has not: a collapse triangle was
   hand-drawn here on 27 September 2026 because `AllIcons.General.ArrowDown` was
   assumed to be a chevron. It is a `<polygon>`, and the platform ships 106
   filled-triangle icons.
2. **Does it survive every bundled theme?** Six ship: New UI Dark and Light,
   Darcula, IntelliJ Light, Island Dark and Island Light. A color that is right
   in one and invisible in another is a defect, not a customization.
