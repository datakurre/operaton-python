# DMN FEEL Expression Reference for Operaton

This document covers the FEEL (Friendly Enough Expression Language) expressions supported by Operaton's DMN engine in decision table input and output entries.

---

## Overview

FEEL is the expression language defined by the DMN specification. In Operaton, FEEL expressions are used in:

- **Input entries** — conditions that determine whether a rule matches
- **Output entries** — values returned when a rule matches
- **Input expressions** — variable references for input columns

Operaton uses the FEEL expression language as the default for DMN decision tables.

---

## Literal Values

### Strings

```
"gold"
"hello world"
""
```

String literals are enclosed in double quotes. In DMN XML, double quotes inside `<text>` elements may need XML escaping, but the FEEL engine handles standard string literals.

### Numbers

```
42
-7
3.14
0
```

Numbers can be integers or decimals. No distinction is made between integer and floating-point in FEEL syntax — the type is determined by the `typeRef` on the input/output.

### Booleans

```
true
false
```

### Dates

```
date("2025-01-15")
date and time("2025-01-15T10:30:00")
time("10:30:00")
duration("P1D")
duration("PT2H30M")
```

---

## Comparison Operators

Used in input entries to match against the input value:

| Expression | Matches When |
|---|---|
| `< 100` | Input is less than 100 |
| `<= 100` | Input is less than or equal to 100 |
| `> 100` | Input is greater than 100 |
| `>= 100` | Input is greater than or equal to 100 |
| `42` | Input equals 42 (implicit equality) |
| `"gold"` | Input equals "gold" |

### Examples in DMN XML

```xml
<inputEntry id="IE_1">
  <text>&gt;= 1000</text>     <!-- >= 1000 (XML-escaped) -->
</inputEntry>

<inputEntry id="IE_2">
  <text>&lt; 500</text>       <!-- < 500 (XML-escaped) -->
</inputEntry>
```

> **Note:** In XML, `<` must be escaped as `&lt;` and `>` as `&gt;` inside `<text>` elements.

---

## Ranges (Intervals)

FEEL supports inclusive and exclusive range notation:

| Expression | Matches When |
|---|---|
| `[1..10]` | Input is between 1 and 10 (inclusive) |
| `(1..10)` | Input is between 1 and 10 (exclusive) |
| `[1..10)` | Input is >= 1 and < 10 |
| `(1..10]` | Input is > 1 and <= 10 |

### Examples

```xml
<inputEntry id="IE_1">
  <text>[100..200]</text>    <!-- 100 <= input <= 200 -->
</inputEntry>

<inputEntry id="IE_2">
  <text>(0..1000)</text>     <!-- 0 < input < 1000 -->
</inputEntry>
```

---

## Disjunction (Multiple Values)

Match against multiple discrete values:

```
"gold", "platinum"
1, 2, 3
```

This matches if the input equals any of the listed values.

### Example

```xml
<inputEntry id="IE_1">
  <text>"gold", "platinum"</text>    <!-- matches gold OR platinum -->
</inputEntry>
```

---

## Negation

Negate any expression with `not()`:

| Expression | Matches When |
|---|---|
| `not("gold")` | Input is not "gold" |
| `not(1, 2, 3)` | Input is not 1, 2, or 3 |
| `not([1..10])` | Input is not in range [1..10] |

### Example

```xml
<inputEntry id="IE_1">
  <text>not("bronze")</text>    <!-- matches anything except bronze -->
</inputEntry>
```

---

## Wildcard (Empty / Any)

An empty input entry matches any value:

```xml
<inputEntry id="IE_1">
  <text></text>    <!-- matches any input value -->
</inputEntry>
```

This is used for default/catch-all rules, typically at the bottom of a FIRST-policy table.

---

## Null Handling

| Expression | Matches When |
|---|---|
| `null` | Input is null |
| `not(null)` | Input is not null |

### Example

```xml
<inputEntry id="IE_1">
  <text>null</text>    <!-- matches when input is null/missing -->
</inputEntry>
```

---

## Complete Input Entry Reference

| Category | Expression | Description |
|---|---|---|
| Equality | `"gold"` | Equals string "gold" |
| Equality | `42` | Equals number 42 |
| Equality | `true` | Equals boolean true |
| Comparison | `< 100` | Less than 100 |
| Comparison | `<= 100` | Less than or equal to 100 |
| Comparison | `> 100` | Greater than 100 |
| Comparison | `>= 100` | Greater than or equal to 100 |
| Range | `[1..10]` | Between 1 and 10 inclusive |
| Range | `(1..10)` | Between 1 and 10 exclusive |
| Range | `[1..10)` | >= 1 and < 10 |
| Range | `(1..10]` | > 1 and <= 10 |
| Disjunction | `"a", "b", "c"` | Equals any of the values |
| Negation | `not("gold")` | Not equal to "gold" |
| Negation | `not(1, 2)` | Not equal to 1 or 2 |
| Negation | `not([1..10])` | Not in range |
| Wildcard | *(empty)* | Matches any value |
| Null | `null` | Input is null |
| Date | `date("2025-01-15")` | Equals date |
| Date range | `[date("2025-01-01")..date("2025-12-31")]` | Date in range |

---

## Output Entry Expressions

Output entries are typically literal values:

```xml
<outputEntry id="OE_1"><text>15</text></outputEntry>
<outputEntry id="OE_2"><text>"express"</text></outputEntry>
<outputEntry id="OE_3"><text>true</text></outputEntry>
```

---

## Type Reference Summary

Map DMN types to FEEL literals and Java types:

| DMN `typeRef` | FEEL Literal | Java Type | Robot Keyword |
|---|---|---|---|
| `string` | `"text"` | `java.lang.String` | *(default)* |
| `integer` | `42` | `java.lang.Integer` | `Create Integer Variable` |
| `long` | `1000000` | `java.lang.Long` | — |
| `double` | `3.14` | `java.lang.Double` | `Create Double Variable` |
| `boolean` | `true` / `false` | `java.lang.Boolean` | `Create Boolean Variable` |
| `date` | `date("2025-01-15")` | `java.util.Date` | `Create Date Variable` |

When passing variables from Robot Framework to DMN evaluation, use the typed variable keywords to ensure proper type matching:

```robot
${amount}=    Create Integer Variable    1000
${result}=    Evaluate Decision    discount    orderTotal=${amount}
```

---

## Practical Examples

### Customer Discount Table (FIRST policy)

| Customer Type | Order Total | → Discount % |
|---|---|---|
| `"gold"` | `>= 1000` | `20` |
| `"gold"` | *(any)* | `15` |
| `"silver"` | *(any)* | `10` |
| *(any)* | *(any)* | `0` |

### Shipping Priority (ranges)

| Order Total | → Priority |
|---|---|
| `>= 1000` | `"high"` |
| `[500..1000)` | `"medium"` |
| `< 500` | `"low"` |

### Access Control (negation + disjunction)

| Role | Department | → Access |
|---|---|---|
| `"admin"` | *(any)* | `"full"` |
| `not("guest")` | `"engineering", "product"` | `"standard"` |
| *(any)* | *(any)* | `"read-only"` |
