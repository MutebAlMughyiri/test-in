[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-021

# UC-CODEGEN-021: Let an agent write the method body

**As a** tester, **I want** the method Testin writes to arrive with the steps in
it, **so that** I do not read the test case in one window and type it in another.

Testin writes a method and leaves a `// TODO` in it. Connect a command-line
agent you already run, and **Automate Test Case** hands it the test case and
puts what comes back where that `// TODO` was.

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
  one line: a statement, a comment, or the TODO deleted. An agent writes the
  body only where it is still that line, and only when the tester asked for it.
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
  to choose from: the two fields are the whole connection. An agent published
  tomorrow is connected by typing it, not by waiting for a release, and no second
  place can disagree with what is in the fields. An empty command means no agent,
  which changes nothing about what Automate Test Case already did.
- **Rule-CODEGEN-084** — The agent is asked once per test case, on a background
  task the tester can cancel, and never while the method is being written.
  Writing a method is a write action, and an agent takes seconds to minutes: a
  write action that waits on one freezes the IDE. The prompt is sent on standard
  input, because a prompt of several lines handed to a command as an argument is
  cut at the first newline by a `.cmd` shim and nothing says so. An agent that
  must have it as an argument says `{prompt}` in its arguments, and then it goes
  exactly there.
- **Rule-CODEGEN-085** — Check runs the command with `--version` and prints the
  first line it answered. Every agent answers that one, so it is not a field a
  tester can get wrong. A command nothing on PATH answers to is refused by name,
  and the refusal says what was looked for: `pi.exe, pi.cmd, pi.bat`. A tester
  connecting an agent Testin has never heard of needs to know which spelling was
  tried. It waits twenty seconds and can be canceled, because a settings page
  that hangs is worse than one that says nothing.
- **Rule-CODEGEN-086** — What is sent is one test case, filled into a prompt the
  tester can read and change: its description, expected result, steps, test
  data, pre-conditions and module. The prompt also says where the test case sits
  in the tree and names the class its method is written into. Nothing else ever leaves: not
  the class's code, not another test case, not the test data root. An empty
  prompt means the one Testin ships, so a tester who never edited it gets the
  better wording a later release brings.
- **Rule-CODEGEN-087** — A key is never Testin's to hold, to read, or even to
  ask about. An agent signs itself in, with its own account or with a key it
  reads from the environment the IDE was started in. Testin has no field for one,
  writes none to its settings file, and puts none on a command line. A
  question about a key it never uses is a field that can only be wrong. It once
  named `ANTHROPIC_API_KEY` beside a Claude Code signed in through a Claude
  account, which reads no such variable, and reported a fault that was not there.
- **Rule-CODEGEN-088** — What the agent printed is read from standard output,
  and where it fenced a block the largest one is taken. An answer that does not
  end as Java statements do is dropped: the TODO stays, the log says what came
  back, and the tester is shown nothing.
- **Rule-CODEGEN-089** — An execution reports the bodies that landed, once, with
  a count. Canceling keeps every body already written and leaves the rest with
  their TODO. Each body is one named write command, so Ctrl+Z takes one back.
- **Rule-CODEGEN-090** — Every exchange is kept while the execution lasts and
  offered once it ends. The notification says how many of how many test cases
  got a body, and carries Show what the agent said. That opens a read-only
  window holding what was asked and what came back for each test case, in order,
  with the ones that were dropped marked. Nothing opens on its own: a tester who
  only wanted the body reads one line and closes it. The same pair goes to the
  log at debug level, so an execution nobody watched can still be read
  afterward.
- **Rule-CODEGEN-091** — A test case whose method already holds a body of its
  own is not sent to the agent, and the execution says so before it starts. One
  dialog names how many they are and offers Write over them or Leave them as
  they are. Escape leaves them. Nothing is replaced without that answer,
  and what replaces it is one named write command, so Ctrl+Z takes the tester's
  own body back.
- **Rule-CODEGEN-115** — The timeout is a whole number of seconds above 0.
  Anything else is refused when the page is applied: the page stays open, the
  reason is shown under the box, and the timeout saved before is kept.

## The screen

Two pages, under **Settings › Tools › Testin**. The first is how Testin reaches
the agent; the second is what it asks for.

```
┌─ Settings › Tools › Testin › Automation Agent ──────────────────┐
│                                                                     │
│  Command      [ pi                                      ]           │
│               The command that runs the agent, found on PATH        │
│                                                                     │
│  Arguments    [ -p --no-tools --no-session --no-context-files ]      │
│               {prompt} marks where the test case goes. Without it   │
│               the test case is sent on standard input.              │
│                                                                     │
│  Keys live in your system environment. Testin never reads one.      │
│  pi 0.85.1                                                          │
│                                                     [ Check ]       │
└──────────────────────────────────────────────────────────────────┘

┌─ Settings › Tools › Testin › Automation Agent › Prompt ─────────┐
│                                                                     │
│  One test case: its description, expected result, steps, test       │
│  data, pre-conditions and module, where it sits in the tree and     │
│  the class its method is written into. Nothing else ever leaves.    │
│                                                                     │
│  Prompt       ┌──────────────────────────────────────────┐          │
│               │ Write the body of {method}.              │          │
│               │ Test case: {description}                 │          │
│               │ Expected result: {expectedResult}        │          │
│               └──────────────────────────────────────────┘          │
│               {description} {expectedResult} {steps} {testData}     │
│               {preConditions} {module} {testSetPath}                │
│               {class} {method}                       [ Reset ]      │
│                                                                     │
│  Timeout      [ 180 ]  seconds one test case may take               │
└──────────────────────────────────────────────────────────────────┘
```

What to type for the agents in use today, none of which Testin needs to know
about: `pi` with `-p --no-tools --no-session --no-context-files`, `claude` with
`-p`, `codex` with `exec`, `gemini` with `-p`. `-p` is *print*: answer once and
exit, rather than opening the agent's own window.

## Main flow

1. The tester selects one test case or several, and presses **Automate Test
   Case**.
2. Testin writes the method for every selected test case that has none — the
   annotation, the declaration and the `// TODO`, exactly as before.
3. The status bar shows **Writing test method bodies**, with a Cancel.
4. For each test case in turn, Testin fills the tester's prompt from that test
   case and runs the agent's command with it.
5. What the agent printed is read, and the statements in it replace the `// TODO`
   in that test case's method.
6. The balloon reads **Written 3**, counting the bodies that landed.
7. `Ctrl+Z` in the file takes one body back.

## What Testin refuses

**If no agent is connected** — nothing is asked and nothing is said. The methods
are written with their `// TODO`, which is what happened before any of this.

**If the command cannot be started** — the `// TODO` stays, the log says which
command and why, and the tester is not shown a stack trace (Rule-CODEGEN-006).

**If the agent answers something that is not Java** — the `// TODO` stays. A
half-written body is never left behind.

**If the tester has written the body already** — that test case is not sent, and
the execution asks first. One dialog says how many such test cases there are,
and offers **Write over them** or **Leave them as they are**. `Enter` writes
over them; `Escape` leaves them, and the test cases still holding their
`// TODO` are sent all the same. Nothing is replaced without that answer, and
`Ctrl+Z` takes the tester's own body back.

**If the execution is canceled** — the bodies already written stay, the rest
keep their `// TODO`, and the count reports what landed.

## Why it works this way

The tester's own agent runs the request, under the tester's own account, so
Testin holds no API key and no credential of any kind — the same bargain **Report Bug** makes with `gh`. What an agent
needs to sign in lives in the
system environment, where Testin neither reads it nor writes it.

The agent is never asked while the method is being written. Writing a method is
a PSI write action; an agent takes seconds to minutes, and a write action that
waits on one freezes the IDE. So the body is a second edit, made after the answer
arrives, against the method found again by its identity.

---

[← Automate Test Case](automateTestCase.md) · [Automation code and the gutter](main.md)
