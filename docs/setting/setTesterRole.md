[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-005

# UC-SETTING-005: Give my role

**As a** tester, **I want** to record what I do, **so that** a report or a test case can say it was written by a test
engineer.

The row takes a value and stores it. Nothing in Testin reads it yet, and the
row is kept for the thing that will: role-based permissions,
[#14](https://github.com/MutebAlMughyiri/test-in/issues/14).

There is no key for this. It is the **Tester role** row.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. Testin asks for none:
  a Git remote's credentials are kept by Git's own credential helper.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-020** — A value this page stores is read by something. The
  role is stored and read by nothing, which is why it is a difference below.

## The screen

The row is the fourth one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Tester role:  [ Test Engineer                               ]           │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The caption** — **Tester role**.
2. **The box** — a plain text box, the same as the one above it.

The whole page is drawn on [the settings page](main.md#the-page).

## Main flow

1. The tester types a role into the **Tester role** row.
2. The tester presses **Apply**.
3. The value is stored.
4. Nothing else happens.

## What Testin refuses

Nothing.

## Where the plugin breaks its own rules

**The role is read by nothing at all.** It is on no marker. It is in no report,
no message and no log line. The row takes a value, and the value is never used
again. That is difference 2 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

A tester who fills this row in expects to see the role somewhere. It is
nowhere. The row is kept anyway, because role-based permissions will read it to
decide who may approve a test case and who may remove a test project. That is
answered on [the product page](../product.md) and tracked in
[#14](https://github.com/MutebAlMughyiri/test-in/issues/14). What is still open is the
list of roles: free text cannot answer *may this person approve*.

---

[Documentation](../README.md) › [The settings page](main.md)
