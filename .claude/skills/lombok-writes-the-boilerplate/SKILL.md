---
name: lombok-writes-the-boilerplate
description: Lombok generates the constructors, getters and builders in Testin — never hand-write one. Use when adding a class with a private constructor, a getter, a value type, an enum with fields, or any accessor that Lombok already generates. Triggers on "private X() {}", "public String getY()", "add a field to this enum".
---

# Lombok writes the boilerplate

Testin has Lombok on `compileOnly` + `annotationProcessor` in every module. If
Lombok generates it, do not type it.

The rule is not about characters saved. Handwritten boilerplate is code a
reader has to check, and checking it means reading it against the annotation
that would have written it — which is strictly more work than there was before.

## The one that keeps coming back

A utility class needs a private constructor so nobody instantiates it:

```java
// no
public final class CardTitle {

    private CardTitle() {
    }
}

// yes
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
}
```

Two imports instead of three lines, and it reads as a *property of the class*
sitting with `final` and `public`, rather than as a member the reader has to
look at and dismiss. Every other holder in the codebase is already written this
way — `Fqcn`, `Notifier`, `Badges`, `BackgroundWork` — so a handwritten one is
also the odd file out.

## What Lombok owns here

| Instead of                                                           | Write                                                                         |
|----------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `private Foo() {}` on a holder                                       | `@NoArgsConstructor(access = AccessLevel.PRIVATE)`                            |
| A constructor assigning every field                                  | `@AllArgsConstructor`, or `@RequiredArgsConstructor` for the `final` ones     |
| `public X getY()`                                                    | `@Getter` on the field, or on the class                                       |
| `public void setY(X y)`                                              | `@Setter`, and only where a setter is genuinely wanted                        |
| A field-per-constant enum with a handwritten constructor and getters | `@Getter @AllArgsConstructor` on the enum — see `Done`, `Refused`, `Priority` |
| `equals`/`hashCode`/`toString` on a value                            | a `record`, first. `@EqualsAndHashCode` only when a record will not do        |

## What Lombok does not own

- **`@NonNull` goes on DTO and marker fields only**, where the generated runtime
  check is the point. Everywhere else nullability is carried by the jetbrains
  `@NotNull` / `@Nullable`, which the IDE instruments. Two annotations meaning
  the same thing in one signature is worse than either.
- **`@SneakyThrows` is not used.** A method handles its own failures — catch
  inside, log through `TestinLogger`, notify when a tester action triggered it. There
  are zero in the tree; keep it that way.
- **`@Data` is not used.** It bundles decisions nobody made — a setter for every
  field, `equals` on a mutable object. Say which ones you want.
- **`@Builder` only where a call site really cannot read positionally.** Most
  Testin constructors take two or three arguments and read fine.

## The three constructors Lombok cannot write

These are the whole list. Anything else handwritten is a miss.

|                              | Why it stays                                                                                                                    |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `util/Bundle`                | Calls `super(Bundle.class, BUNDLE)`. `@NoArgsConstructor` writes an empty body, so there is nowhere for the `super` call to go. |
| `open/OpenContextMenuAction` | Calls `super("Show Context Menu")`, then does four more lines of work.                                                          |
| `ui/Badges.BadgePill`        | Its body is real: `setOpaque`, a border, an icon gap. A constructor that configures is not boilerplate.                         |

Two of the three are the same rule: **a constructor that calls `super` with
arguments cannot be generated.** The third is the other rule: **a constructor
that does work is not boilerplate, however short it is.**

If a fourth appears, it needs one of those two reasons written beside it.

## Also delete, do not annotate

An **empty** no-arg constructor needs no annotation at all — Java writes a
public no-arg constructor for any class that declares none. `TestCaseHoverButtons` had

```java
public ActionIcons() {
}
```

which does exactly nothing that removing it would not do. There should be zero
of these; `@NoArgsConstructor` on such a class is a second way of writing the
same nothing.

`@NoArgsConstructor(access = AccessLevel.PRIVATE)` is different, and is the
right answer: it makes the constructor **private**, which is the point — it says
the class is not to be instantiated.

## `@NotNull` survives generation — it is TYPE_USE

Worth knowing before anyone "fixes" it. The JetBrains annotations are
`TYPE_USE`, so a field written

```java
private final @NotNull String outcome;
```

has `@NotNull String` as its *type*, and Lombok copies the type verbatim into
the getter's return and the constructor's parameter. Verified on the compiled
class:

```
public java.lang.String getOutcome();
  RuntimeInvisibleTypeAnnotations:
    0: org.jetbrains.annotations.NotNull(): METHOD_RETURN
```

So there is **no need for a `lombok.config` with `copyableAnnotations`**, and
the project deliberately has no `lombok.config` at all. A generated member
carries the null contract already.

## What is deliberately not used

Named so nobody spends an afternoon rediscovering why.

|                                 | Why not                                                                                                                                                                                                                                                        |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@UtilityClass`                 | Makes every member implicitly `static`. The `static` keyword stops appearing in the source, so a reader cannot tell a static method from an instance one by looking at it. `@NoArgsConstructor(access = PRIVATE)` says the same thing without hiding anything. |
| `@Data`                         | Bundles decisions nobody made: a setter for every field, `equals` on a mutable object. Name the two or three you actually want.                                                                                                                                |
| `@Value`                        | A `record` does it, in the language, with no annotation. There are 51 of them.                                                                                                                                                                                 |
| `val` / `var` from Lombok       | The convention is `final @NotNull` on locals, which is a stated null contract. An inferred type states nothing.                                                                                                                                                |
| `@SneakyThrows`                 | A method handles its own failures. Zero in the tree.                                                                                                                                                                                                           |
| `@ExtensionMethod`, `@Delegate` | Both make a call site resolve somewhere the reader cannot see from the line in front of them.                                                                                                                                                                  |
| `@Synchronized`                 | It moves the lock to a hidden field, which is exactly the thing the 16 `synchronized` uses here are being explicit about.                                                                                                                                      |
| `@StandardException`            | No custom exception types exist.                                                                                                                                                                                                                               |
| `@Builder.Default` sweeps       | Only meaningful where a `@Builder` field has an initializer; check the specific class rather than applying it broadly.                                                                                                                                         |

## What the audit found, 9 September 2026

Measured, not estimated, across `src/main` and `testin-java/src/main`:

|                                                | Count                                   |
|------------------------------------------------|-----------------------------------------|
| `@Getter`                                      | 135                                     |
| `@NoArgsConstructor`                           | 94                                      |
| `@AllArgsConstructor`                          | 73                                      |
| `@Builder` / `@SuperBuilder`                   | 44 / 10                                 |
| `records`                                      | 51                                      |
| **Handwritten getters**                        | **0**                                   |
| Handwritten `equals` / `hashCode` / `toString` | 2                                       |
| Handwritten setters that only assign a field   | 1                                       |
| Pure field-assignment constructors             | 22, of which 8 were exactly replaceable |

The conclusion is the useful part: **Lombok was already doing nearly all of it.**
Eight constructors became `@RequiredArgsConstructor`; everything else on the list
either does real work or would be made worse by an annotation. If a future pass
turns up a big number here, check the measurement before writing code.

## The trap in `@RequiredArgsConstructor`

It generates parameters from the **final fields in declaration order**. Two
fields of the same type in a different order compiles clean and is wrong. Before
applying it, check three things:

1. The constructor body is nothing but `this.x = x;` lines.
2. The parameter order matches the field declaration order exactly.
3. No other `final` field lacks an initializer — Lombok would add it as a
   parameter nobody passes.

And it cannot generate a **varargs** parameter: `TestCaseDialogKey(String name,
Shortcuts... keys)` stays handwritten, because the annotation would produce
`Shortcuts[]` and every enum constant would stop compiling.

## Checking it

A grep for `private X() {` finds only the no-arg ones at one indent, which is
how nine handwritten constructors hid behind an answer of "there is one". Scan
for the shape instead — any visibility, any arity, any indent:

```python
# constructors whose body is nothing but this.x = x, or nothing at all
import io, re, glob
for f in glob.glob('src/**/*.java', recursive=True):
    s = io.open(f, encoding='utf-8').read()
    types = set(re.findall(r'(?:class|enum|record|interface)\s+(\w+)', s))
    for m in re.finditer(r'^([ 	]+)(?:public |private |protected )?(\w+)\([^)]*\)\s*\{
((?:.*?
)*?)\}', s, re.M):
        if m.group(2) not in types:
            continue
        body = [l.strip() for l in m.group(3).split('
') if l.strip() and not l.strip().startswith('//')]
        if all(re.fullmatch(r'this\.(\w+) = ;', l) for l in body):
            print(f, m.group(2), len(body))
```

Run against `src` and `testin-java`, it should print only the three above.
