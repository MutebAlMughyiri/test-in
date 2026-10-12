---
name: a-class-does-one-job
description: Before adding a method to an existing class, check that it is the same job the class already does. Use when a shared helper is growing, when a class's name and its method list have drifted apart, or when adding a convenience method to a service, notifier, util or manager.
---

# A class does one job, and its name says which

A class earns its name by doing one job. The failure this catches is not a bad
class — it is a good class that quietly took on a second job, one convenient
method at a time, until its name stopped describing it.

Nothing breaks when this happens. That is why it survives review.

## The case it is made of

`Notifier` delivers a message. It knows about balloons and notifications,
colors, titles, fade times, actions, and the status bar it anchors to. One job:
**hand a message to the platform.**

It also held six methods like this:

```java
public void softRefuseNothingToRun(final @NotNull Project p, final @NotNull String name) {
    softRefuse(p, name + " has no test cases to run");
}
```

Every one of them was added for a good reason — two call sites were phrasing the
same refusal differently, so the sentence was given one owner. Correct
instinct, wrong owner. The sentence needs *an* owner; it does not need *this*
owner. What it got instead:

- The one place to look for how Testin phrases a refusal was a list of balloon
  plumbing with sentences scattered through it.
- The one place to look for balloon plumbing was a list of sentences.
- Three refusals that answer nearly the same gesture sat several screens apart,
  each with a comment explaining how it differs from "the one above" — which was
  no longer above it.

The fix was not to inline them back at the call sites. It was a `Refused` enum
beside the `Done` enum that already owned the *success* vocabulary. Notifier
went back to one job, the sentences got a home where they can be read against
each other, and no tester-facing text changed.

## The rule

**Before adding a method to an existing class, say the class's job in one
sentence — then say the new method's job. If the second sentence is not the
first sentence, the method goes somewhere else.**

Both halves matter. "It's related" is not the test; "it is the same job" is.
A message and the delivery of a message are related and are not the same job.

## What this is not

It is not an argument against shared owners. Centralizing is right — see
`centralize-shared-design`. The two rules run together:

|                            |                                                        |
|----------------------------|--------------------------------------------------------|
| `centralize-shared-design` | This thing needs **an** owner                          |
| this skill                 | And that owner is **not** whatever class is convenient |

Nor is it an argument for many tiny classes. A class with one job may be large.
The question is never how many methods it has; it is whether one sentence
covers all of them.

## The signals

Watch for these while writing, not afterward:

- **You are about to add a method that composes a value, to a class that
  transports values.** Or formats, to a class that stores. Or decides, to a
  class that executes.
- **The new method's name repeats a domain noun the class's name does not
  contain.** `Notifier.softRefuseNothingToRun` — a notifier that knows what an *execution* is has learned something
  about
  running.
- **A Javadoc on the new method explains a rule from another part.**
  `Rule-TREE-PANEL-004` on a method in `notifications` is the rule telling you
  where it belongs.
- **You have to scroll past unrelated methods to compare two related ones.**
- **The class name would need an "and" to be accurate.** "Notifier delivers
  messages *and* writes refusals."
- **Names ending in `Manager`, `Helper`, `Util`, `Service`, `Handler`.** They
  describe no job at all, so nothing a method does can contradict them, and they
  accumulate. `util` is already tracked as this problem in #112.

## Where the method goes instead

In order. Stop at the first that fits.

1. **A type that already owns this vocabulary.** `Done` owned the past-tense
   outcomes; `Refused` was the missing twin, not a new idea.
2. **The model the value is about.** A node's behavior goes on `AbstractNode`,
   an enum's presentation goes on the enum. This is already the house style.
3. **A new type named for the job.** Cheap and honest. An enum of sentences is
   five lines plus its constants, and it makes the rule enforceable: a
   present-tense word cannot be passed to `softShow(Project, Done)` at all.
4. **The single caller.** If exactly one place uses it, there is nothing to
   diverge — put it there, with the comment saying why. "Hidden by the filter"
   went back to the editor that shows it.

Do not stop at "the class that already imports what I need".

## What to do when you find one

Finding one in code you were not asked to change is a **finding**, not work —
record it and put it in the table. See `report-findings`.

Finding one in the class you are editing is different: you are already there,
and the next method will make it worse. Say what you found in one sentence,
move the methods, keep every tester-facing string byte-identical, and let the
tests prove nothing else moved.
