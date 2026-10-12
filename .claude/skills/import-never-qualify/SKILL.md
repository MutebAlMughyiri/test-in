---
name: import-never-qualify
description: In Testin's Java, a class is named by its simple name with an import — never written out in full as java.util.Optional or org.testin.codegen.GenType in the code. Use when writing or editing any Java class or test, especially one-line fixes and test helpers where a fully qualified name is the quick way to avoid adding an import. Triggers on "java.util.", "org.testin." or any package path in the middle of a line of code.
---

# Import, never qualify

A class is written by its simple name, and the import says where it comes from:

```java
// no
void pending(final org.testin.model.status.RunItemStatus passed) {
    final RunItem runItem = RunItem.builder().build().showing(java.util.Optional.of(passed));
    assertEquals(runItem.getStatus(), org.testin.model.status.RunItemStatus.PENDING);
}
```

```java
// yes
import java.util.Optional;
import org.testin.model.status.RunItemStatus;

void pending(final RunItemStatus passed) {
    final RunItem runItem = RunItem.builder().build().showing(Optional.of(passed));
    assertEquals(runItem.getStatus(), RunItemStatus.PENDING);
}
```

Muteb, 22 September 2026: *"why did you write java.util.Optional, just write
Optional, make sure our classes not have like this."*

A qualified name in the middle of a line is a package path the reader has to
skip to find the word that matters. It also hides a dependency the import list
would have shown at the top of the file. It is almost always the quick way out
of adding an import during a small edit, which is exactly when it slips in.

## The one exception: two classes with one simple name

Qualify only when an import is impossible because the simple name already means
something else in that file. There is one such place in `src`:

| File                    | Qualified                                | Why an import cannot be used                                                              |
|-------------------------|------------------------------------------|-------------------------------------------------------------------------------------------|
| `logger/LogWriter.java` | `com.intellij.openapi.diagnostic.Logger` | Testin's own `TestinLogger` sits in the same package, so the simple name already means it |

Anything else is not an exception. A wildcard import that also offers the name,
`com.itextpdf.layout.element.*` beside `java.util.List`, is resolved by a
single-type import: `import java.util.List;` wins over the wildcard. Check the
file's other uses of the name still mean what they meant before.

## Checking

Package paths in code, outside the import and package lines:

```bash
grep -rnE "(^|[^.[:alnum:]_])(java|javax|org|com|kotlin|lombok)\.([a-z_][[:alnum:]_]*\.)*[A-Z][[:alnum:]_]*" src --include=*.java \
  | grep -vE ":[0-9]+:[[:space:]]*(import|package) " | grep -v "LogWriter.java"
```

It also matches comments and strings - the class names `ArchitectureTest`
passes as text, a configurable's id - and those stay: only a hit in code is a
finding.

## Where it came from

The sweep of 22 September 2026 found 44 qualified names in 24 files, 10 in
`src/main` and 14 in `src/test`, and replaced 43 of them with imports. The one
left is the `LogWriter` row above.

## The gate enforces it

`tools/inspect.ps1` reports a package path written in the middle of a line as
`QualifiedClassName` and exits non-zero for it, beside `WrappedMethodDeclaration`
and the other rules no IntelliJ inspection has. `-Quick` runs it in seconds, so a
qualified name never reaches a review.

Three things are not code, and the rule skips them:

- a string, because a persisted key reads like a package path and is one:
  `@State(name = "testin.settings.AppSettingsState")`;
- a text block, because the sample stack traces the bug and details tests are
  built from are full of `java.lang.AssertionError` and none of it is a
  reference;
- a statement under `//noinspection`, which is how the one exception below is
  written down.

## The one exception, and how it is marked

A simple name that already means another class in that file. `org.testin.logger`
has Testin's own `TestinLogger`, so the platform's has to be written out - Java has no
import alias. That line carries the marker, written here as text rather than as
Java, because it is an example of a suppression and a suppression is reported:

```text
//noinspection QualifiedClassName
private static final com.intellij.openapi.diagnostic.@NotNull Logger IDE_LOG =
        com.intellij.openapi.diagnostic.Logger.getInstance(LogWriter.class);
```

The marker covers the statement it stands over, to its semicolon. It is a comment
a machine reads, which is one of the two kinds this codebase keeps.
