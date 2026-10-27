[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-007

# UC-SETTING-007: Choose how much Testin writes to its log

**As a** tester, **I want** to turn Testin's own log up, **so that** I can send a useful log when something goes wrong,
and turn it back
down afterward.

The level decides how much Testin writes to its own log file.

There is no key for this. It is the **Log level** row.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. Testin asks for none:
  a Git remote's credentials are kept by Git's own credential helper.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-024** — The level takes effect the moment **Apply** is pressed.
  The IDE does not have to restart.
- **Rule-SETTING-025** — Testin starts at **INFO** until the tester chooses
  otherwise. The chosen level is in force from the moment the settings are read,
  which is before any project opens. So what happens at startup is written at
  the level that was asked for, rather than being dropped until a project is
  there.
- **Rule-SETTING-026** — The log sits beside the IDE's own log, so collecting
  the IDE's logs collects Testin's too.
- **Rule-SETTING-047** — The log holds up to 5 MB. A full log moves to
  `testin.log.1`, replacing the one there, and a new `testin.log` begins. A log
  that cannot be moved stops where it is, and the IDE's own log says so.

## The screen

The row is the second one on the page. It is the only drop-down there.

```
┌──────────────────────────────────────────────────────────────┐
│  Log level:  [ INFO                                      v ] │
├──────────────────────────────────────────────────────────────┤
│                DISABLED                                      │
│                TRACE                                         │
│                DEBUG                                         │
│                INFO                                          │
│                WARN                                          │
│                ERROR                                         │
│                FATAL                                         │
└──────────────────────────────────────────────────────────────┘
```

1. **The box** — the level in use now.
2. **The list** — the seven levels, in this order. It opens under the box.

Nothing can be typed into the box. The tester picks one of the seven.

The whole page is drawn on [the settings page](main.md#the-page).

## The choices

The drop-down offers seven, in this order.

| Level        | What is written                                        |
|--------------|--------------------------------------------------------|
| **DISABLED** | Nothing at all                                         |
| **TRACE**    | Everything, including every step of a read             |
| **DEBUG**    | Nearly everything                                      |
| **INFO**     | What happened, without the detail. The starting choice |
| **WARN**     | Only what went oddly                                   |
| **ERROR**    | Only what failed                                       |
| **FATAL**    | Only what stopped Testin                               |

## Main flow

1. The tester opens the **Log level** drop-down.
2. The tester picks **TRACE**.
3. The tester presses **Apply**.
4. Testin starts writing every step at once.
5. The tester reproduces the problem.
6. The tester sets the level back to **INFO**.

## What Testin refuses

Nothing. The drop-down cannot hold anything but one of the seven.

## Why it matters

Much of what Testin does is only ever written to the log. A folder skipped for
having no marker. A test case that would not read. An edit dropped because
there was nowhere to write it. None of those raises a message. At **INFO** most
of them are not written either. A tester looking for one of them needs **TRACE**.

---

[Documentation](../README.md) › [The settings page](main.md)
