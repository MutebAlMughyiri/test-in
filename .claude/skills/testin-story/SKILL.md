---
name: testin-story
description: Write or update a Testin backlog item as a GitHub issue on MutebAlMughyiri/test-in in the house format, grounded in the code and carrying measured statistics. Use when the user describes a feature, bug, refactor or chore for the Testin plugin and wants it captured, or asks to rewrite/tidy an existing issue. Triggers on "make this a story", "open an issue for this", "add to the backlog", "write this up".
allowed-tools: Bash(gh issue create:*), Bash(gh issue edit:*), Bash(gh issue view:*), Bash(gh issue list:*), Bash(gh search issues:*), Read, Grep, Glob, Write
---

# Testin story format

Backlog items for Testin live as GitHub issues on `MutebAlMughyiri/test-in`. Never write
them to a local file — Muteb works across several machines and a local file does
not travel.

Pass `--repo MutebAlMughyiri/test-in` explicitly. `gh` can infer it from the remote, but
these commands are run from other directories often enough that being explicit
is cheaper than being wrong.

## Before writing

1. `gh issue list --repo MutebAlMughyiri/test-in --state open --limit 100` — check whether
   this already exists. If it does, edit that issue rather than opening a second.
2. Ground what you write in the code. Read the classes involved and name real
   paths; an issue that names a class that does not exist costs more time than it
   saves.
   If the backlog refers to a class by an old name, say so and give the real one.
3. If recent commits may already have fixed it, add a **Check first** section
   listing the commit hashes and say to close the issue rather than implement it
   if the work is already done.

## Structure

Feature, refactor or chore:

```markdown
# Story

As a <role>, I want <capability>, so that <benefit>.

## Problem

What is wrong today, concretely. Skip for pure new features.

## Scope

What this issue covers, as prose or a short numbered list. Be explicit about
what is deliberately excluded.

## Statistics

Counts measured against `main`, not estimated. See the section below.

## Implementation notes

Mechanism, gotchas, threading, ordering. Include a code snippet when a specific
API or pattern is the point.

## Touches

`package/Class`, `package/OtherClass` — the real paths.

## Acceptance criteria

- [ ] One observable outcome per line.
- [ ] Written so it can be checked without reading the diff.
```

Bug:

```markdown
# Bug

One-paragraph statement of the defect.

## Observed

- What happens, and where.
- Reproduction conditions: version, IDE build, theme, platform.

## Where to look

`Class:line` references with what each does, plus ranked suspects for the cause.

## Statistics

How many places show the defect, how many callers are affected, how far it
reaches. Measured, not estimated.

## Acceptance criteria

- [ ] ...
```

## Every story carries statistics

> from now, any new feature should have statistics. to know the impact and
> enhance the implement plan and testin business.

A story without numbers is a guess about how big it is. Count it, in the
codebase, before writing the plan — the numbers routinely change the plan, and
twice now they have changed what the story was even about.

**Measure, never estimate.** Run the greps and paste the real figures. "Several
call sites" is not a statistic; "18 call sites across 11 files" is, and it is the
difference between an afternoon and a week.

What to count depends on the story, but the useful ones are almost always:

- **The thing itself** — how many occurrences of whatever is being changed, per
  file. `grep -c` per file beats one total, because it shows whether the work is
  concentrated or spread.
- **Blast radius** — how many call sites, in how many files, and how many of them
  already handle it correctly. This is the number that sizes the risk.
- **A baseline to compare against** — the same count across `src/main`, so
  "twenty" becomes "twenty, which is below the project average". Without a
  denominator a count reads as bad news whatever it says.
- **What is genuinely in scope** after the count is broken down. This is where the
  numbers earn their place.

Then say what the numbers mean for the plan, in a sentence. The table is not the
point; the conclusion drawn from it is.

Two live examples of the numbers changing the work:

- #71 was written as "remove the 20 `@Nullable`s from the indexer". Counting them
  split them into three groups and the real scope was **9**, with the file holding
  the joint-highest count entirely out of scope. Counting also showed all 18
  callers already handle null, which turned it from a bug hunt into a contract
  change.
- #61 was written as "triage 55 unused-code findings". Regenerating the report
  gave 94, and checking them found 93 were false — one wildcard import was
  poisoning a whole-project inspection. The count was the finding.

**Verify the arithmetic before publishing.** A breakdown whose parts do not sum
to the total is worse than no breakdown, because the whole section exists to be
trusted. Add the groups up.

## Rules

- Acceptance criteria are observable outcomes, not implementation steps. "Rows
  fit their content in every theme" — not "change `setRowHeight`".
- Include a criterion for what must **not** change. Regressions in the other
  editor, the other theme, or the other platform are the usual failure.
- When a real design decision is unresolved, add an **Open question** or **Design decision required** section stating
  the options and a recommendation.
  Do not resolve it silently in the issue text.
- Respect the architecture rules in `CLAUDE.md` — indexer-only file access,
  Swing on the EDT, display-only formatting. An issue that proposes violating
  one of them should say why the exception is justified.
- Keep the body in Markdown, plain prose, no emoji.

## Writing it

Stage the body in a scratch file, then:

```bash
gh issue create --repo MutebAlMughyiri/test-in --title "<title>" --body-file <path>
gh issue edit <n> --repo MutebAlMughyiri/test-in --body-file <path>
```

Titles are a specific claim, not a category: "Grid rows render oversized in all
themes except Darcula", not "Grid bug". Report the issue number and URL back.
