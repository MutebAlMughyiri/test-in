[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-014

# UC-VIEW-PANEL-014: Go to the automation code

**As a** tester, **I want** to open the test method Testin wrote for this test
case, **so that** I can read or change what the automation actually does.

Testin writes a test method for each test case. This opens that method.

`Shift+F5` does it, which is the key the button's tooltip names and the same key
that opens the code from a card in the editor.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has two tabs, Details and History, and
  both are drawn every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-056** — The button is always drawn. Where it cannot work it
  is gray, it does not grow under the pointer, the pointer stays an arrow, and
  it says what it is waiting for. Nothing is left out (Rule-CODEGEN-062).
- **Rule-VIEW-PANEL-057** — The button is the first of the three, before the
  run button and the one that opens the test case.

## The screen

The three buttons sit on the line under the title, beside the badges.

```
┌──────────────────────────────────────────────────────────────────────────┐
│   ( High ) ( Smoke )        [ go to code ]  [ run ]  [ tc ]              │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The first button** — this one. Its tooltip reads **Navigate to Test
   Method** and names `Shift+F5`, and its shape says what automation is there.
   The three shapes are below.
2. **The second button** — runs the test case. That is
   [UC-VIEW-PANEL-012](runFromPanel.md).
3. **Either button** — grows under the pointer, and the pointer becomes a hand.
   A button that cannot work here does neither.

## What the button says

The same icon and the same words as the card in the editor, which is
[UC-EDITOR-PANEL-047](../editorPanel/seeWhatIsAutomated.md). The tooltip always
reads **Navigate to Test Method**, the name the gesture has everywhere (Rule-EDITOR-PANEL-237); where the button cannot
work, it reads the reason
instead.

| The button's shape              | What it means                                                                      |
|---------------------------------|------------------------------------------------------------------------------------|
| a class icon                    | The generated test method has something in it, or Testin has not read the code yet |
| a class icon with an error mark | The test case names a method and there is none                                     |
| a hollow class icon             | There is no method, or Testin wrote one and nobody filled it in                    |

## Main flow

1. The panel is showing a test case that has automation code.
2. The tester clicks the first button, drawn as a class icon.
3. The Java file opens with the caret on the test method for this test case.

## What Testin refuses

**If the IDE has no Java plugin** — the button is gray, it does not grow under
the pointer, and its tooltip reads *Navigate to Test Method (needs the Java plugin)*.
Clicking it says the same thing.

**If `testin.yml` does not name the open test project** — the button is gray
the same way, and says why. **Save to testin.yml**, in the Testin panel, turns
the code on. That is [UC-CODEGEN-019](../codegen/noJavaPlugin.md) and
Rule-CODEGEN-082.

**If the action is reached from the menu without the Java plugin** — a message
titled **Java Plugin Not Available** reads *Automation code generation and
navigation require the Java plugin, which is not available in this IDE.*

**If the test case has no automation code** — a message reads *Not automated*.
It is the same message running the test case gives, because it is the same
state.

**If the IDE is still indexing** — a message reads *Waiting for indexing*, and
the code opens once the IDE has finished.

## One action, two pictures

On a menu this action is drawn as an arrow. Here, and on a card, it is drawn as
the icon for a Java class, because here the picture also has to say what
automation is there. No rule says an action has one picture, so this is worth
knowing rather than a difference.

---

[Documentation](../README.md) › [The view panel](main.md)
