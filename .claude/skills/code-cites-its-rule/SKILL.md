---
name: code-cites-its-rule
description: Every code change updates the documentation in the same commit, and every method a tester can reach names the rule or use case it implements. Use when writing or changing any Java in Testin, when reviewing a diff, and when a method has no comment saying which documented behavior it carries. Triggers on "write this method", "fix this", "change what this does", "review this change".
---

# Code cites its rule

Testin's documentation is not a description written after the fact. It is the
specification: 151 use cases and 610 rules under `docs/`, each one numbered so
it can be pointed at.

Two things follow, and they are not optional.

> ## The code says which rule it carries, and the rule changes when the code does.

## The two halves

**1. Every method a tester can reach names its rule or its use case.**

```java
// UC-TREE-PANEL-012, Rule-TREE-PANEL-042
public void refuseRemove(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
}
```

**2. Every change to what a tester sees changes the document in the same
commit.** A commit that changes what a key does, and does not change the step
that names that key, is incomplete. The review should say so.

This is the whole reason the documentation lives in this repository rather than
beside it.

## How to write the marker

The marker is one line, above the signature. Never on the signature line: a
method declaration is one line, however long, and a marker must not be the thing
that wraps it.

| What the method carries               | The marker                                    |
|---------------------------------------|-----------------------------------------------|
| One use case                          | `// UC-TREE-PANEL-012`                        |
| A rule inside one use case            | `// UC-TREE-PANEL-012, Rule-TREE-PANEL-038`   |
| A rule that holds across a whole part | `// Rule-TREE-PANEL-007`                      |
| Several use cases                     | `// UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033` |

**A rule name carries its part.** `Rule-TREE-PANEL-045` says where it lives, so
the marker needs nothing else to be findable. It was not always so: rules were
bare numbers until 7 September 2026, and there were eight different Rule44s. A
marker still reading `Rule44` is from before that and names nothing.

**A rule that holds for a whole part is written on every page in it**, first in
the list and in number order, then that page's own. No page sends the reader
elsewhere for half its rules. Reword such a rule and every copy has to be
reworded - `RuleNumbersTest` fails when one number is written two ways.

**`tools/add-rule.ps1` writes it, on every page it governs.** It takes the next
number, moves the Numbering row on, and puts a part-wide rule after the shared
block on each page. Typing 46 copies by hand is how they come to disagree.

**A new rule takes the next number, and nothing else moves.** Read the **Numbering** row of that part's `main.md`, take
the number after the end of its
rule range, write the rule where it belongs on its page, and move the row on. No
existing rule is ever renumbered to make room: a number that changes is one that
was already quoted in an issue, a commit and a code marker.

That means a page's rules need not be consecutive - 47, 48, 49 and then 194 is a
page that was added to later, which is the numbering working. The page is the
order to read them in; the number is only how to find one.

**There is no Javadoc to put it in.** Since 20 September 2026 the marker is the
only comment a method carries, so it is always the `//` line above the signature:

```java
// UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-144
public void recordRunItemStatus(final @NotNull TestCase testCase) {
}
```

The sentence that used to sit under it now goes in the commit message: *"Escape
records nothing at all, neither the detail nor the run item status, because a
half-written failure is worse than no failure"*. The behavior itself is in
`Rule-EDITOR-PANEL-144` on its page. Read the comment rule in `CLAUDE.md`.

## The eight prefixes

| Part                           | Its use cases and rules                                  | Its folder          |
|--------------------------------|----------------------------------------------------------|---------------------|
| The tree panel                 | `UC-TREE-PANEL-001` and `Rule-TREE-PANEL-001` and up     | `docs/treePanel/`   |
| The editor panel               | `UC-EDITOR-PANEL-001` and `Rule-EDITOR-PANEL-001` and up | `docs/editorPanel/` |
| The view panel                 | `UC-VIEW-PANEL-001` and `Rule-VIEW-PANEL-001` and up     | `docs/viewPanel/`   |
| The settings page              | `UC-SETTING-001` and `Rule-SETTING-001` and up           | `docs/setting/`     |
| Automation code and the gutter | `UC-CODEGEN-001` and `Rule-CODEGEN-001` and up           | `docs/codegen/`     |
| Reports                        | `UC-REPORT-001` and `Rule-REPORT-001` and up             | `docs/report/`      |
| Sharing work with the team     | `UC-SHARE-001` and `Rule-SHARE-001` and up               | `docs/share/`       |
| Inside Testin                  | `UC-INTERNAL-001` and `Rule-INTERNAL-001` and up         | `docs/internal/`    |

The numbering is defined once, in `docs/standard.md`. Read it before inventing
anything.

## The identifier has to exist

A marker naming a use case or a rule that is not in `docs/` is worse than no
marker, because the next reader trusts it and goes looking. Two ways to be sure:

```bash
grep -rn "UC-TREE-PANEL-012" docs/          # the use case page and everything pointing at it
grep -rn "\*\*Rule-TREE-PANEL-038\*\*" docs/   # the rule, on the page that owns it
```

## When there is no rule yet

**Write the rule first.** `docs/standard.md` says it in one line: *if a use case
needs a rule nobody has written, the rule is written first.*

That is not ceremony. Writing the rule is where the decision gets made, and a
rule that cannot be written in one plain sentence is usually a sign the design
is wrong. Three of the six critical defects found in September 2026 were code
that contradicted a rule the codebase had already written down somewhere else.

If the behavior is genuinely new, add the rule to the use case page it governs
and give it the next number after that part's last - the **Numbering** row of its `main.md`
says what that is. Move the row on in the same commit, and cite the rule from
the code. Never renumber a rule that already exists.

## When a method carries no rule

Not every method is a rule. A method carries no marker when:

- it is a helper nobody outside its own class calls, and it implements no
  behavior a tester can name;
- it is framework plumbing: a constructor, a getter, a Swing listener that only
  forwards;
- it is a test.

The test is simple. **Could a tester describe what this method does, without
being shown the code?** If yes, it has a rule, and the marker names it. If no,
it is plumbing, and a marker would be noise.

## When a change needs no document change

A refactor that changes nothing a tester can see changes no document. That is
not an exception to the rule, it is the test of whether it really was a
refactor.

So the question at the end of a change is always: **what would a tester
notice?**

- Nothing → no document change. Say so in the commit body.
- Something → the document changes in this commit, not the next one.
- Something, and no document covers it → write the rule first, then the code.

## What a complete change looks like

A key that used to do one thing and now does another:

1. The step in the use case page that names the key, rewritten.
2. The rule, if the rule itself changed.
3. The part's key table, if the key moved or is new.
4. The `Checked against` line in that part's `main.md`, moved to this commit.
5. The code, carrying the marker.
6. One commit.

## Two things this is not

**It is not a license to write any other comment.** The marker says *which
documented behavior this is*. It does not describe what the lines do, and nothing
else is added beside it - not a paragraph saying why, not a line narrating the
next one. The reason goes in the commit message; `CLAUDE.md` says why, and what the
two exceptions are.

**It is not a reason to weaken the documentation to match the code.** When the
code and the document disagree, one of them is wrong and the disagreement is
worth stating. Testin's eight `main.md` pages each end with the places the
plugin breaks its own rules, and 97 of those are open issues. Adding to that
list is an honest outcome. Quietly rewriting a rule so today's code satisfies it
is not.

## Checking a diff

Before offering a change, ask three questions in this order:

1. **Which rule or use case does each new or changed method carry?** If a method
   a tester can reach has no marker, add one, or say why it needs none.
2. **Does the marker name something that is really in `docs/`?** Grep it.
3. **Would a tester notice this change?** If yes, is the document in this diff?

A change that answers all three is complete. A change that cannot answer the
first is usually a change nobody asked for.
