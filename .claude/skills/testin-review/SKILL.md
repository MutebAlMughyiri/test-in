---
name: testin-review
description: What "testin review" means - Muteb's own review of a change before it is committed. Read it whenever he says "testin review", "review", "our review", "enhance as we do before commit" or "review then commit", and before committing any change to Testin.
---

# Testin review

> *"testin review means our review that I told you many times. simple code,
> clean, well-structured, no customization and every thing based on business.
> no redundant as possible unless it required to make code simpler. rules and
> use cases added to each method and class."*
>
> - Muteb, 12 October 2026

A Testin review is not a bug hunt. It takes a change that already works and
makes it **simpler without changing what it does**, then commits it. Behavior
stays exactly as tested in the sandbox; only the shape of the code moves.

## The seven questions

Ask each of them of every file in the diff, in this order.

| #   | Question                                                                                                                    | What a "no" looks like                                                                                                                                 | Read                        |
| --- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------- |
| 1   | **Is it simple?** The direct form, fewest parts, fewest lines for the same behavior.                                        | A helper, flag, parameter or class that exists only to carry something the caller already knows. A longer version of something shorter.                | `deliver-smallest-change`   |
| 2   | **Is it clean?** Names, `final @NotNull`, one-line signatures, no null, no `throws`, no dead code, no comment but a marker. | A `@Nullable` meaning "not set", an `if (x != null)` at a call site, an unused parameter or import, a narrating comment.                               | CLAUDE.md, Code conventions |
| 3   | **Is it well structured?** Each class has one job its name says; each method one job; logic at the level that owns it.      | A grab bag of static helpers, a method doing two jobs, a class named for one job doing another.                                                        | `a-class-does-one-job`      |
| 4   | **Does every shared fact have one owner?** A look, a rule, a word, a lookup, a key.                                         | The same lookup, string, color or decision written in two places; two paths that draw the same thing.                                                  | `centralize-shared-design`  |
| 5   | **No customization?** One surface holding every member, each disabling itself with its reason.                              | A per-screen variant of a menu, toolbar or dialog; a call site choosing members.                                                                       | `one-surface-many-states`   |
| 6   | **Is everything named for the business?** Test case, test set, test run, run item, run item status, test set editor, card.  | `Case`, `Result`, `Run` alone, `TestEditor`, a near-twin class name, a name that claims the wrong job.                                                 | `names-say-the-business`    |
| 7   | **Does every method and class a tester can reach name its rule or use case?**                                               | A behavior a tester could describe with no `UC-` or `Rule-` marker, a marker naming nothing in `docs/`, a rule that no longer says what the code does. | `code-cites-its-rule`       |

**No redundancy, unless it makes the code simpler.** A second copy is removed
by giving the fact one owner. Keep a repetition only when removing it would
need more code or a less readable shape than the two copies, and say so.

## What a review may and may not do

- **May:** rename, move a method to its owner, inline a one-use helper, delete
  a parameter, a branch or a class, merge two copies into one owner, add a
  missing marker, reword a rule the code no longer matches.
- **May not:** change behavior a tester sees, add a feature, or fix an
  unrelated defect inside the review. Those are findings: report them in a
  table, and file each new one as an issue with `priority:` and `cost:` labels
  (the `report-findings` skill).
- **If a feature is what makes the code complicated,** say so with its cost and
  ask whether to keep it, simplify it or remove it. Removing it is a fair answer.
- **Fewer lines or the same** for the same behavior. A review that grows the
  diff explains each added line.

## Checks before the commit

Run in this order; never two Gradle builds at once.

1. The IDE formatter on every changed Java file.
2. `./gradlew compileJava test` (the documentation checks run here too).
3. The IDE tests the change reaches, by class (`--tests` filters only the share
   it follows).
4. `pwsh tools/inspect.ps1 -Quick` (text rules and PMD).

Never `./gradlew inspect` by hand: CI runs it on the push.

## Reporting it

One short table of what the review changed and why, then the checks and their
results, then any finding left as a finding. Commit only after Muteb has tested
in the sandbox and asked for it, with the why in the commit body (the code
carries no comments). Push only when he says push.
