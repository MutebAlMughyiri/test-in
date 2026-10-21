---
name: use-what-the-ide-has
description: Before building any surface or service of a feature - a window, a console, a terminal, a merge or diff view, a chooser, a timer, a log, a popup, a progress - find the IntelliJ tool that already does it and use it through public API. Build Testin's own only when the IDE has none, or its API is Internal or Experimental (the verifier fails the build on both), and say so with the evidence. Use when designing a feature, when choosing between "open X" and "build X", and when reviewing code that draws, schedules or runs something itself.
---

# Use what the IDE has

> *"this is what i mean. use exist idea tools is better that customize new
> thing."* - Muteb, 22 October 2026

A tester already knows the IDE: its Run window, its notifications, its file
chooser, its diff. A feature that opens the IDE's own tool is learned the moment
it appears, looks right in every theme, and is maintained by JetBrains. A
feature that builds its own copy is a second thing to learn and a part Testin
maintains forever.

> ## The IDE's tool first, through public API. Our own only with evidence that there is none.

This is the `deliver-smallest-change` rule "The component supports it, or we say
so", one level up: not a tooltip or a header, but a whole window, console or
service.

## The order of choice

1. **An IDE tool, through public API** - a tool window, a service, a dialog the
   platform already shows. Called through Testin's owner of it where one exists.
2. **A platform building block** - a component the IDE ships (`JBPopupFactory`,
   `SearchTextField`, `FileChooser`), through the house owner that wraps it.
3. **Testin's own** - only when 1 and 2 cannot do it. Say which tool was checked,
   and why it cannot, before writing code. Not after.

Never offer 3 as a peer of 1. "We could build our own" is not an option while
the IDE has the tool; it is the fallback when it does not.

## Public means public: read the SDK, not the docs

A tool the IDE uses is not automatically one a plugin may use. `build.gradle.kts`
sets the verifier's `failureLevel` to fail on `INTERNAL_API_USAGES`,
`EXPERIMENTAL_API_USAGES`, `DEPRECATED_API_USAGES` and
`SCHEDULED_FOR_REMOVAL_API_USAGES`, so the build refuses all four. Check the
class in the Gradle cache before proposing it:

```bash
IDEA=$(ls -d ~/.gradle/caches/*/transforms/*/transformed/idea-2026.*-win | head -1)
unzip -o -q "$IDEA/plugins/terminal/lib/terminal.jar" 'org/jetbrains/plugins/terminal/TerminalToolWindowManager.class'
javap -v org/jetbrains/plugins/terminal/TerminalToolWindowManager.class | grep -E "ApiStatus|Deprecated"
```

A class marked at the top applies to every member; a method can carry its own
mark too. If the only route is marked, the tool is not available to Testin,
however well it fits. Say that, with the class name and the mark.

## What it cost to learn

Muteb asked for the AI agent's prompt and answer to be visible while it works
(#490). The first idea was a real command window. Measured against the SDK:

| Route                                                | What the SDK says                                                                                                                                                           | Verdict                                                         |
|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| A cmd window on Windows, Terminal on macOS and Linux | The platform's one call for it, `ExecUtil.getTerminalCommand`, is `@ApiStatus.Internal`; Linux has no single terminal (gnome-terminal, konsole, xterm)                      | Three launch commands of Testin's own                           |
| The IDE's Terminal tool window                       | `TerminalToolWindowTabsManager`, `TerminalView`, `TerminalSendTextBuilder` are `@ApiStatus.Experimental`; `TerminalToolWindowManager` is partly `Internal` and `Deprecated` | The build fails                                                 |
| The Run tool window console                          | Public                                                                                                                                                                      | **Chosen**: the same on every OS, Testin still reads the answer |

The IDE tool that fit was not the first one named, and the one that looked
closest was not public. Both are found only by reading the SDK.

## Tools Testin already uses, and their owners

Reach for these; do not build beside them.

| Need                                            | IDE tool                                                        | Testin's owner                                                   |
|-------------------------------------------------|-----------------------------------------------------------------|------------------------------------------------------------------|
| Tell the tester something happened              | Notifications, status bar balloons (`NotificationGroupManager`) | `notifications/Notifier` with `Done` and `Refused`               |
| Work off the EDT with a progress bar and Cancel | `Task.Backgroundable`, `ProgressManager`                        | `services/BackgroundWork`                                        |
| Choose a file or a folder                       | `FileChooser`, `FileChooserDescriptor`                          | called where needed                                              |
| A list to pick from                             | `JBPopupFactory`                                                | called where needed                                              |
| Run a command                                   | `GeneralCommandLine`, `OSProcessHandler`                        | `codegen/agent/AgentCli` for the agent                           |
| Compare two versions                            | `DiffManager`                                                   | `view/history/HistoryCardPanel`                                  |
| The clipboard                                   | `CopyPasteManager`                                              | called where needed (9 files); `util/ClipboardContents` reads it |
| Show a run's output                             | Run tool window console                                         | #490, for the agent                                              |

Dialogs are the deliberate exception: Testin's dialogs are built on its own
`ui.framework`, which is the house design system and a decision of its own. Do
not route a dialog around it to reach a platform one.

## What this does not mean

- **Not "use Internal API because the IDE does".** A tool behind an Internal or
  Experimental mark is not available, and suppressing the verifier is not a way
  in (`never-suppress-a-warning`).
- **Not "never write our own".** Test data is Testin's own domain: a field-level
  merge of a test case, a run item status, a report. When the IDE has no tool
  for the domain, the code is Testin's, and that is correct.
- **Not a reason to replace a house owner.** Where Testin already wraps an IDE
  tool in one owner (`Notifier`, `BackgroundWork`), a feature calls the owner,
  not the IDE directly, so every feature gets the owner's improvements.

## Checks before building a surface or a service

1. Which IDE tool window, service or dialog already does this? Name it.
2. Is its route public in the SDK this build targets? Read the marks.
3. Does Testin already wrap it? Call the owner.
4. If none of the above works, write down which tool was checked and why it
   cannot, in the design or the issue, before the first line of code.
