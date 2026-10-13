[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-002

# UC-SETTING-002: Set the Testin folder

**As a** tester, **I want** to tell Testin which folder on this machine holds my
test projects, **so that** the tree has something to show.

Testin shows nothing at all until this folder is set. It is the first thing to
do on a new machine.

There is no key for this. It is the first row of the page.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. Testin asks for none:
  a Git remote's credentials are kept by Git's own credential helper.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-010** — Testin reads exactly one folder. Every test project
  lives directly inside it.
- **Rule-SETTING-011** — An empty box means not set. A box of nothing but spaces
  means the same.
- **Rule-SETTING-012** — Changing the folder makes every code project that has
  opened the Testin panel read the disk again.
- **Rule-SETTING-013** — The folder is given as a full path, from the drive or
  the root. A partial one is refused: it would be read against wherever the IDE
  was started, so the same few letters would mean a different folder depending
  on how the IDE was launched.
- **Rule-SETTING-014** — With no folder set, Testin reads nothing and shows
  nothing. It does not fail.
- **Rule-SETTING-045** — A folder that is itself a test project, one that holds
  a .testproject, is refused, and the refusal names the folder that holds it: the Testin
  folder holds test projects and is never one of them.
- **Rule-SETTING-046** — A stored folder that is not a full path, or not a path
  at all, is read as not set, whatever put it there. Testin then asks for the
  folder, as Rule-SETTING-014 says, rather than reading one inside the open
  project.

## The screen

The row is the first one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Testin folder:       [ Example: C:\Users\...\Testin   ] [...] [Open]    │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The box** — the one folder that holds every test project.
2. **The gray example** — shown only while the box is empty. It reads *Example: C:\Users\{username}\Documents\Testin*.
3. **The browse button** — opens a folder chooser.
4. **Open** — opens the folder in the file manager. It is
   [UC-SETTING-003](openTestinFolder.md).

The folder chooser is titled **Select Testin Folder**. The line under that title
reads *Choose the directory where your test projects are stored*.

The whole page is drawn on [the settings page](main.md#the-page).

## Main flow

1. The tester opens the settings page.
2. The tester presses the browse button on the first row.
3. The folder chooser opens.
4. The tester picks the folder holding their test projects and confirms.
5. The whole path appears in the box.
6. The tester presses **Apply**.
7. Every code project with a Testin panel open reads the folder again.
8. The tree fills with the test project that code project is bound to.

## What Testin refuses

Every refusal keeps the window open with the message under the box, so the value
that cannot work is never stored.

**If the folder is not there** — *There is no folder at \<path\>.*

**If the path names a file** — *\<path\> is a file. The Testin folder has to be
a folder, because test projects are folders inside it.*

**If the path is a partial one** — *\<path\> is a partial path. Testin needs the
whole one, from the drive or the root, because a partial path is read against
wherever the IDE was started and that is not where you are looking.*

**If the path holds a character the system forbids** — the same message as a
folder that is not there. No folder can be at such a path, so it is refused as
one that is missing rather than with a sentence about path syntax.

**An empty box is not refused.** It is how a tester says they have not chosen
yet, and the panel has an empty state for exactly that. A box of nothing but
spaces is the same answer.

## What happens when no folder is set

**When a code project opens** — the Testin Help mark on the status bar turns
red. Its hint reads *Please set the Testin folder to enable test management
features.*, with an **Open Settings** link and the guide *How to set up Testin
on this machine*. Setting a folder clears it in every open code project.

**In the tree panel** — the empty state is shown, with a link reading **Configure Testin settings**.

**On the toolbar** — **Select Test Project** is gray.

**On disk** — nothing is read at all.

**If the path has spaces around it** — they are removed when it is stored, so
the value in the file is the value Testin uses. It used to keep them and trim
them at the next project open, which changed a stored setting nobody had edited.

---

[Documentation](../README.md) › [The settings page](main.md)
