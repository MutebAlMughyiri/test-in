---
name: Story
about: A feature, a refactor or a chore
title: ""
labels: feature
---

<!--
  Two labels before this is ready to pick up: priority: (critical|high|medium|low)
  and cost: (nocost|minor|major|expensive). See CONTRIBUTING.md.
-->

# Story

As a <role>, I want <capability>, so that <benefit>.

## Problem

What is wrong today, concretely. Skip it for a pure new feature.

## Scope

What this covers, and what it deliberately excludes. Being explicit about the
exclusions is half the value.

## Statistics

**Measured against `main`, not estimated.** Count the thing itself, per file;
count the blast radius — how many call sites, in how many files, and how many
already handle it correctly; and give a baseline to compare against.

Then say what the numbers mean for the plan, in a sentence. Counting routinely
changes the plan, and twice it has changed what the story was about.

Verify the arithmetic. A breakdown whose parts do not sum to the total is worse
than no breakdown, because the section exists to be trusted.

## Implementation notes

Mechanism, gotchas, threading, ordering. A code snippet where a specific API is
the point.

## Touches

`package/Class`, `package/OtherClass` — real paths, checked to exist.

## Acceptance criteria

- [ ] One observable outcome per line, checkable without reading the diff.
- [ ] One line for what must **not** change.
