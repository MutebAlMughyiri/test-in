[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-021

# UC-CODEGEN-021: Let an agent write the method body

**As a** tester, **I want** the method Testin writes to arrive with the steps in
it, **so that** I do not read the test case in one window and type it in another.

Testin writes a method and leaves a `// TODO` in it. Connect a command-line
agent you already run, and **Automate Test Case** hands it the test cases in the
IDE's own Terminal, where the agent writes each body over that `// TODO` and asks
you as it goes.

`F12`, or the menu entry **Automate Test Case** — the same gesture as before.
Connecting an agent changes what that gesture produces, not where it lives.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — The body belongs to the tester. Testin writes the
  annotation, the declaration and one `// TODO` line, and never touches a body a
  tester has written. A body is the tester's as soon as it is anything but that
  one line: a statement, a comment, or the TODO deleted. An agent is asked to
  write the body only where it is still that line, and only when the tester
  asked for it: the request tells it to replace the TODO line and change
  nothing else.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-082** — Testin touches a test project's automation code only
  when `testin.yml` names that test project. Otherwise, nothing is generated,
  renamed, moved or removed, **Automate Test Case**, **Navigate to Test Method**
  and **Run Tests** are gray and say why, and no gutter icon or automated mark
  is shown. **Save to testin.yml**, in the Testin panel, turns code on.
- **Rule-CODEGEN-083** — An agent is connected by naming the command that runs
  it and the arguments it takes, and by nothing else. There is no list of agents
  to choose from: the two fields are the whole connection, and Testin runs them
  as they are, in the IDE's Terminal. An agent published tomorrow is connected
  by typing it, not by waiting for a release, and no second place can disagree
  with what is in the fields. An empty command means no agent, which changes
  nothing about what Automate Test Case already did.
- **Rule-CODEGEN-084** — The agent is asked once per Automate Test Case, never
  while a method is being written, in the agent's tab of the IDE's Terminal
  tool window, named after its command: claude, pi. The request is pasted into
  the tab the way the Terminal pastes Ctrl+V, so every agent keeps its lines
  together, then sent with one Enter: typed line by line, an Enter would send a
  request of several lines at its first line. A new tab gets it once its
  connection is ready; a running one at once.
- **Rule-CODEGEN-085** — Check runs the command with `--version` and prints the
  first line it answered. Every agent answers that one, so it is not a field a
  tester can get wrong. A command nothing on PATH answers to is refused by name,
  and the refusal says what was looked for: `pi.exe, pi.cmd, pi.bat`. A tester
  connecting an agent Testin has never heard of needs to know which spelling was
  tried. It waits twenty seconds and can be canceled, because a settings page
  that hangs is worse than one that says nothing.
- **Rule-CODEGEN-086** — What is sent is the test cases asked for, each filled
  into a prompt the tester can read and change: its description, expected
  result, steps, test data, pre-conditions and module, where it sits in the
  tree, and the class and file its method is written into. Nothing else ever
  leaves: not another test case, not the test data root. An empty prompt means
  the one Testin ships, so a tester who never edited it gets the better wording
  a later release brings.
- **Rule-CODEGEN-087** — A key is never Testin's to hold, to read, or even to
  ask about. An agent signs itself in, with its own account or with a key it
  reads from the environment the IDE was started in. Testin has no field for one,
  writes none to its settings file, and puts none on a command line. A
  question about a key it never uses is a field that can only be wrong. It once
  named `ANTHROPIC_API_KEY` beside a Claude Code signed in through a Claude
  account, which reads no such variable, and reported a fault that was not there.
- **Rule-CODEGEN-089** — Automate Test Case says Sent once its request is
  handed over. What the agent writes is the agent's: it shows it in its tab and
  asks before each edit unless the tester told it not to, and Local History
  keeps every body it replaces.
- **Rule-CODEGEN-090** — Every exchange is shown as it happens, by the agent
  itself, in its tab: the same on Windows, macOS and Linux. The
  tester talks to it there as in any terminal, a second and a third time about
  the same method. The request goes to the log at debug level.
- **Rule-CODEGEN-091** — A test case whose method already holds a body of its
  own is not sent to the agent, and the execution says so before it starts. One
  dialog names how many they are and offers Write over them or Leave them as
  they are. Escape leaves them. Nothing is replaced without that answer, the
  request tells the agent which bodies it may replace, and Local History keeps
  the tester's own.
- **Rule-CODEGEN-129** — The agent's tab is the agent itself, started in the
  IDE's Terminal from the command and arguments in settings, and named after
  the command: claude, pi. While it runs, every Automate Test Case hands its
  request to it; once it has ended or its tab is closed, the next opens a new
  one, and another command opens a tab of its own. Without the Terminal plugin,
  Automate Test Case writes the methods and says the Terminal plugin is not
  available. Check, on the settings page, opens no tab.

## The screen

Two pages, under **Settings › Tools › Testin**. The first is how Testin reaches
the agent; the second is what it asks for.

```
┌─ Settings › Tools › Testin › Automation Agent ──────────────────┐
│                                                                     │
│  Command      [ claude                                  ]           │
│               The command that runs the agent, found on PATH        │
│                                                                     │
│  Arguments    [                                         ]           │
│               What follows the command when the agent starts,       │
│               such as --model sonnet. The request is typed into     │
│               its tab.                                              │
│                                                                     │
│  Keys live in your system environment. Testin never reads one.      │
│  2.1.308 (Claude Code)                                              │
│                                                     [ Check ]       │
└──────────────────────────────────────────────────────────────────┘

┌─ Settings › Tools › Testin › Automation Agent › Prompt ─────────┐
│                                                                     │
│  The test cases asked for: the description, expected result,        │
│  steps, test data, pre-conditions and module of each, where it      │
│  sits in the tree, and the class and file its method is written     │
│  into. Nothing else ever leaves.                                    │
│                                                                     │
│  Prompt       ┌──────────────────────────────────────────┐          │
│               │ Write the body of {method}.              │          │
│               │ Test case: {description}                 │          │
│               │ Expected result: {expectedResult}        │          │
│               └──────────────────────────────────────────┘          │
│               {description} {expectedResult} {steps} {testData}     │
│               {preConditions} {module} {testSetPath}                │
│               {class} {method} {file}                [ Reset ]      │
└──────────────────────────────────────────────────────────────────┘
```

While the agent works, the Terminal tool window holds its tab:

```
┌─ Terminal ──────────────────────────────────────────────────────┐
│ Local │ claude │                                                 │
├──────────────────────────────────────────────────────────────────┤
│ > Write the body of the Java TestNG method signInWithAValidUser  │
│   in the class nafath.LoginPageTest, in the file ...             │
│   Test case: Sign in with a valid user                           │
│   ...                                                            │
│   Replace the // TODO line of the method with the Java ...       │
│                                                                  │
│   Update(src/test/java/nafath/LoginPageTest.java)                │
│     driver.get(baseUrl + "/login");                              │
│                                                                  │
│   Do you want to make this edit to LoginPageTest.java?           │
│   > 1. Yes                                                       │
│     2. No, and tell Claude what to do differently                │
└──────────────────────────────────────────────────────────────────┘
```

What to type for the agents in use today, none of which Testin needs to know
about: `claude`, `codex`, `gemini` and `pi`, each with nothing. Each opens its
own chat in the tab, takes the request as a message, and edits the files itself.
An agent run with `-p`, print, answers once and ends, which leaves no chat to
talk to and nothing to type the next request into.

## Main flow

1. The tester selects one test case or several, and presses **Automate Test
   Case**.
2. Testin writes the method for every selected test case that has none — the
   annotation, the declaration and the `// TODO`, exactly as before.
3. Testin fills the tester's prompt from each test case asked for, in order,
   into one request.
4. If the Terminal tool window holds a running tab named after the command,
   such as **claude**, Testin types the request into it. Otherwise it opens one
   whose process is the agent, started from the command and arguments, and
   types the request once it is ready.
5. The balloon reads **Sent**.
6. The agent writes each body over its `// TODO`, and asks the tester before
   each edit.
7. The tester types to the agent in the same tab to change a body, as many
   times as needed. Local History keeps every body it replaced.

## What Testin refuses

**If no agent is connected** — nothing is asked and nothing is said. The methods
are written with their `// TODO`, which is what happened before any of this.

**If the Terminal plugin is not there** — the methods are written with their
`// TODO`, and Testin says the Terminal plugin is not available.

**If the command cannot be started** — the agent's tab says so, in the
Terminal's own words, and the methods keep their `// TODO`.

**If the tester has written the body already** — that test case is not sent, and
the execution asks first. One dialog says how many such test cases there are,
and offers **Write over them** or **Leave them as they are**. `Enter` writes
over them; `Escape` leaves them, and the test cases still holding their
`// TODO` are sent all the same. Nothing is replaced without that answer, and
Local History keeps the tester's own body.

**If the agent has ended, or its tab was closed** — the next Automate Test Case
opens a new tab.

## Why it works this way

The tester's own agent runs the request, under the tester's own account, so
Testin holds no API key and no credential of any kind — the same bargain **Report Bug** makes with `gh`. What an agent
needs to sign in lives in the
system environment, where Testin neither reads it nor writes it.

The agent is never asked while the method is being written. Writing a method is
a PSI write action; an agent takes seconds to minutes, and a write action that
waits on one freezes the IDE. So the request goes once the methods exist, naming
each one and the file it is in.

The agent works in the IDE's own Terminal because that is what the Terminal is
for, and the way Anthropic's Claude Code plugin does it: a Terminal shows the
agent's own screen, so the agent edits the file itself rather than Testin
reading its answer back. A tester with that plugin installed sees Claude's edits
in the IDE's diff viewer, with nothing from Testin.

---

[← Automate Test Case](automateTestCase.md) · [Automation code and the gutter](main.md)
