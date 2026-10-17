[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-022

# UC-CODEGEN-022: Make the API model of a call

**As a** tester automating an API, **I want** the two Java types that hold a
call's request and response, **so that** I never type their fields by hand.

Right-click a package of the automation project, or press `Alt+Insert` there,
and choose **New → Testin API Model from JSON**. Give the call a name, paste the
request body, the response body or both, and press **Create**. Testin writes
one file for each into that package and opens them: the request as a Lombok
class to build, the response as a record to read.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — The body belongs to the tester. Testin writes the
  annotation, the declaration and one `// TODO` line, and never touches a body a
  tester has written. An agent writes the body only where that `// TODO` still
  stands, and only when the tester asked for it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-082** — Testin touches a test project's automation code only
  when `testin.yml` names that test project. Otherwise, nothing is generated,
  renamed, moved or removed, **Automate Test Case**, **Navigate to Test Method**
  and **Run Tests** are gray and say why, and no gutter icon or automated mark
  is shown. **Save to testin.yml**, in the Testin panel, turns code on.
- **Rule-CODEGEN-098** — **Testin API Model from JSON** is on the New menu of a
  Java package in a source folder of the automation project. On any other
  folder, and while `testin.yml` does not name the open test project (Rule-CODEGEN-082), it is gray and says why.
- **Rule-CODEGEN-099** — Nothing is checked until **Create** is pressed, or
  `Enter`. The Name field takes only English letters and digits, starting with a
  letter, and its first letter is made a capital, so `user` gives `UserRequest`.
  Nothing is written until a Name is given and at least one body is pasted.
  Every pasted body must be a JSON object, or a list of objects, with at least
  one key, and the package must hold neither file yet. A refusal says which, and
  the dialog stays open with everything typed. JSON copied from a script or a
  log is read as it is: comments, single quotes, unquoted keys and a trailing
  comma are accepted.
- **Rule-CODEGEN-100** — A request becomes a single class, `<Name>Request`, and
  a response a single record, `<Name>Response`. However deep the JSON nests,
  there are no other files: every object inside a body is a nested type inside
  the type whose field holds it.
- **Rule-CODEGEN-101** — The request class and every class inside it carry
  `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` and
  `@Accessors(chain = true)`, so a setter returns the object and calls chain. A
  class with no fields carries `@Data` and `@NoArgsConstructor` only. The
  response record carries no Lombok. It puts each component on a line of its
  own, with a blank line between each and an annotation on the line above its
  component. In both files, a field whose JSON key is not its Java name carries
  `@JsonProperty` with the key exactly as the JSON spells it: `first-name` for
  `firstName`, `class` for `classValue`. A field whose key is already its name
  carries nothing. The imports are written even when the module lacks Lombok or
  Jackson; the IDE marks them until it has them.
- **Rule-CODEGEN-102** — A field's type follows its value. Text is `String`. A
  whole number is `Integer`, `Long` past `Integer`'s range, and `BigInteger`
  past `Long`'s. A fraction is `Double`, and true or false is `Boolean`. An
  object becomes a nested type of its own, and a list is a `List` of what it
  holds. A list of objects has one nested type, `<Key>Item`, with every key any
  of its objects has, at every depth: an object or a list inside those objects
  merges the same way. Where values meet, nulls are left out and numbers widen;
  whole numbers and fractions together are `Double`. A null alone, an empty
  list, or values of different kinds are `Object`. Text stays `String` whatever
  it looks like, a date or an id included.
- **Rule-CODEGEN-103** — A field is its key in lowerCamelCase and a nested type
  its key in UpperCamelCase, made of English letters and digits only; any other
  character is dropped, and a key left with none becomes `field`. A Java
  keyword, or a method every Java object has - `toString`, `getClass`, `wait`
  and the rest - gains `Value`; a leading digit gains `_`. A nested type whose
  name the file already uses - `Data`, `Builder`, `String`, `List`,
  `Object` and the like - gains `Item`. Any other name already taken in the same
  type, or the name of a type around it, gains `2`, `3` and so on.
- **Rule-CODEGEN-104** — When a body is a list, its type describes one of the
  list's objects, with every key any of them has.
- **Rule-CODEGEN-105** — The files open in the editor, a balloon says *Created*,
  and one Undo removes them both. A file already in the package is never
  replaced.
- **Rule-CODEGEN-106** — Request and Response are stacked or side by side, and
  the button beside maximize, in the dialog's title bar, switches between the
  two. They start stacked, and the dialog opens the next time the way it was
  left. The dialog can be resized and maximized (Rule-INTERNAL-101). Request and
  Response each sit in a section that opens and closes (Rule-INTERNAL-133). Both
  are open each time the dialog opens, a closed one is still written on Create,
  and each section's line names the file it becomes once a Name is typed.
- **Rule-CODEGEN-107** — An empty Request or Response box is three lines high. A
  pasted body grows it until all of it shows, up to its share of the IDE frame:
  30% of the frame's height for each box when they are stacked, 60% side by
  side. Past that it scrolls, with a scrollbar drawn over the text that takes no
  width of its own. Spelling is not checked in them: a JSON key is not a word.
- **Rule-CODEGEN-108** — At the Debug or Trace log level, `testin.log` records each
  Create: each pasted body exactly as it was pasted, as Testin reads it, then
  every file exactly as it was written - or, when a body cannot be read, its
  refusal and the reason, and a body after it is not read. At Info, it records
  only which files were written, and above Info nothing. Like the agent's
  transcript (Rule-CODEGEN-090), the bodies are recorded as pasted, secrets
  included, so Debug is for tracing a problem, not for every day.
- **Rule-CODEGEN-109** — The request and response boxes are JSON editors: a body
  shows in the editor's JSON colors, and the Format JSON button in each
  section's header lays it out over lines with the tester's JSON code style.
  Only whitespace changes, so a formatted body still holds exactly what was
  pasted.

## The screen

```
┌─ Testin API Model from JSON ─────────────────────── [◫] [⤢] ─┐
│ PATH   com.example.api                                       │
│ NAME                                                         │
│ [ User                                                     ] │
│ ┌─ ▾ REQUEST   UserRequest.java ──────────── Collapse [≣] ─┐ │
│ │ { "first-name": "Muteb", "mobile": "0501234567", ... }  │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌─ ▾ RESPONSE  UserResponse.java ─────────── Collapse [≣] ─┐ │
│ │ { "id": 7, "first-name": "Muteb", "class": "gold", ... } │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                   [ Create ] │
│ Enter Create   Esc Cancel   Ctrl+V Paste   Tab Navigate      │
└──────────────────────────────────────────────────────────────┘
```

1. **The title bar** — the layout switch, stacked or side by side, and
   maximize.
2. **Path** — the package the files go into, or *Source root*.
3. **Name** — the call's name; `User` gives `UserRequest` and `UserResponse`.
4. **Request** and **Response** — a section each, three lines high while
   empty, growing with what is pasted, in the editor's JSON colors. The
   section's line names the file it becomes, and its **Format JSON** button
   lays the body out over lines.
5. **Create** — and the strip of keys under it.

## Main flow

1. The tester right-clicks `com.example.api` and chooses **New → Testin API
   Model from JSON**.
2. The tester types `User`; the sections read `UserRequest.java` and
   `UserResponse.java`.
3. The tester pastes the request body and the response body.
4. The tester presses **Create**.
5. `UserRequest.java` and `UserResponse.java` open in the editor, and the
   balloon reads **Created**.
6. `Ctrl+Z` once removes both.

```java
public record UserResponse(
        Integer id,

        @JsonProperty("first-name")
        String firstName,

        @JsonProperty("class")
        String classValue,

        List<CardsItem> cards) {

    public record CardsItem(
            String last4,

            Boolean frozen) {
    }
}
```

## What Testin refuses

Nothing is checked until **Create**. Then, with the dialog left open and
everything in it kept:

**If no Name is given** — the Name field shows its empty warning.

**If neither body is pasted** — *Paste a request, a response or both*.

**If a body is not JSON** — *The pasted request is not JSON*, or *response*.

**If a body is a number, a string or a list of them** — *The JSON must be an
object, or a list of objects*.

**If a body is `{}`, `[]` or `[{}]`** — *The pasted request has no keys*.

**If the package already holds the file** — *UserResponse.java Already Exists*.
Nothing is replaced.

## Why it works this way

A request is something the test builds and sends, so it is a class with a
builder and chained setters. A response is something the test reads, so it is
a record: nothing in it is set after it is read. `@JsonProperty` is written
only where Java cannot use a key as it is, because it names the key both when
reading and when writing; a camelCase key needs nothing.

One file per body, however deep the JSON, keeps a call's model in one place.
Two calls that share a shape each hold a nested type of their own, rather than one
class quietly taking the fields of both.

The idea of pasting a payload into a package's New menu is RoboPOJOGenerator's,
the IntelliJ plugin by Vadim Shchenev; the module's `README.md` says so. Its
code was read, not copied.
