# DMN Modeling Guide for Operaton

This guide explains how to create `.dmn` files compatible with the Operaton BPM engine.

---

## What Is DMN?

[Decision Model and Notation (DMN)](https://www.omg.org/dmn/) is an OMG standard for modeling business decisions. Operaton supports DMN 1.3 with the FEEL (Friendly Enough Expression Language) expression language.

A `.dmn` file is an XML document that defines:

- **Decision tables** — tabular rules with inputs, outputs, and hit policies
- **Decision Requirements Graphs (DRG)** — chains of decisions where one decision's output feeds into another

---

## File Structure

A minimal DMN file looks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
             xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/"
             id="Definitions_1"
             name="My Decisions"
             namespace="http://operaton.org/schema/1.0/dmn">

  <decision id="myDecision" name="My Decision">
    <decisionTable id="DecisionTable_1" hitPolicy="FIRST">
      <!-- inputs and outputs here -->
    </decisionTable>
  </decision>

</definitions>
```

### Key Elements

| Element | Description |
|---|---|
| `<definitions>` | Root element. Must include the DMN namespace. |
| `<decision>` | A named decision with a unique `id` (the decision key used in the API). |
| `<decisionTable>` | Contains the rules as a table. Specifies the `hitPolicy`. |
| `<input>` | Defines an input column (variable name and type). |
| `<output>` | Defines an output column (variable name and type). |
| `<rule>` | A single row in the decision table with input entries and output entries. |

---

## Decision Table Anatomy

```xml
<decisionTable id="DecisionTable_1" hitPolicy="FIRST">

  <!-- Input columns -->
  <input id="Input_1" label="Customer Type">
    <inputExpression id="InputExpression_1" typeRef="string">
      <text>customerType</text>
    </inputExpression>
  </input>

  <input id="Input_2" label="Order Total">
    <inputExpression id="InputExpression_2" typeRef="integer">
      <text>orderTotal</text>
    </inputExpression>
  </input>

  <!-- Output columns -->
  <output id="Output_1" label="Discount" name="discountPercent" typeRef="integer" />

  <!-- Rules -->
  <rule id="Rule_1">
    <inputEntry id="IE_1"><text>"gold"</text></inputEntry>
    <inputEntry id="IE_2"><text>&gt;= 1000</text></inputEntry>
    <outputEntry id="OE_1"><text>20</text></outputEntry>
  </rule>

  <rule id="Rule_2">
    <inputEntry id="IE_3"><text>"gold"</text></inputEntry>
    <inputEntry id="IE_4"><text></text></inputEntry>
    <outputEntry id="OE_2"><text>15</text></outputEntry>
  </rule>

  <rule id="Rule_default">
    <inputEntry id="IE_5"><text></text></inputEntry>
    <inputEntry id="IE_6"><text></text></inputEntry>
    <outputEntry id="OE_3"><text>0</text></outputEntry>
  </rule>
</decisionTable>
```

### Input Expressions

- The `<text>` inside `<inputExpression>` is the **variable name** that the engine looks up from the provided inputs.
- `typeRef` specifies the data type: `string`, `integer`, `long`, `double`, `boolean`, `date`.

### Input Entries (Conditions)

- Each `<inputEntry>` contains a FEEL expression to match against the input value.
- An **empty** `<text></text>` matches any value (wildcard).
- String literals are quoted: `"gold"`.
- Numeric comparisons: `>= 1000`, `< 500`, `[100..200]`.
- See the [DMN FEEL Reference](dmn-feel-reference.md) for the full expression syntax.

### Output Entries (Results)

- Each `<outputEntry>` contains the value to return when the rule matches.
- The `name` attribute on `<output>` becomes the key in the result map.

---

## Hit Policies

The `hitPolicy` attribute on `<decisionTable>` determines how multiple matching rules are handled:

| Policy | Behavior |
|---|---|
| `UNIQUE` | Exactly one rule must match. Error if multiple rules match. |
| `FIRST` | Returns the first matching rule (rules evaluated top-to-bottom). |
| `ANY` | Multiple rules may match, but all must produce the same output. |
| `COLLECT` | Returns all matching rules as a list. |
| `RULE ORDER` | Returns all matching rules, in the order they appear in the table. |

### When to Use Each Policy

- **UNIQUE** — Use when inputs are mutually exclusive (e.g., letter grades).
- **FIRST** — Use for priority-based rules with a catch-all default at the bottom.
- **ANY** — Use when overlapping rules are expected but must agree on the output.
- **COLLECT** — Use when you need all applicable results (e.g., all matching benefits).
- **RULE ORDER** — Like COLLECT, but guarantees the order matches the table definition.

---

## Supported Data Types

| Type | DMN `typeRef` | FEEL Literal Examples |
|---|---|---|
| String | `string` | `"gold"`, `"active"` |
| Integer | `integer` | `42`, `0`, `-1` |
| Long | `long` | `1000000` |
| Double | `double` | `3.14`, `99.9` |
| Boolean | `boolean` | `true`, `false` |
| Date | `date` | `date("2025-01-15")` |

---

## Multi-Output Decision Tables

A decision table can have multiple output columns:

```xml
<output id="Out_1" label="Discount" name="discountPercent" typeRef="integer" />
<output id="Out_2" label="Shipping" name="shippingMethod" typeRef="string" />

<rule id="Rule_1">
  <inputEntry id="IE_1"><text>"gold"</text></inputEntry>
  <outputEntry id="OE_1"><text>15</text></outputEntry>
  <outputEntry id="OE_2"><text>"express"</text></outputEntry>
</rule>
```

Each matched rule returns a map with all output columns. From the Robot Framework test:

```robot
${result}=    Evaluate Decision    shipping    customerType=gold    orderTotal=${total}
Decision Result Should Contain    ${result}    discountPercent    15
Decision Result Should Contain    ${result}    shippingMethod    express
```

---

## Decision Requirements Graphs (DRG)

A DRG connects multiple decisions where one feeds into another:

```xml
<decision id="customerTier" name="Customer Tier">
  <decisionTable id="DT_tier" hitPolicy="FIRST">
    <input id="I_type">
      <inputExpression id="IE_type" typeRef="string"><text>customerType</text></inputExpression>
    </input>
    <output id="O_tier" name="tier" typeRef="string" />
    <rule id="R_gold">
      <inputEntry id="IE_gold"><text>"gold"</text></inputEntry>
      <outputEntry id="OE_premium"><text>"premium"</text></outputEntry>
    </rule>
  </decisionTable>
</decision>

<decision id="tierDiscount" name="Tier Discount">
  <!-- This decision depends on customerTier -->
  <informationRequirement id="IR_1">
    <requiredDecision href="#customerTier" />
  </informationRequirement>
  <decisionTable id="DT_discount" hitPolicy="FIRST">
    <input id="I_tier">
      <inputExpression id="IE_tier" typeRef="string"><text>tier</text></inputExpression>
    </input>
    <output id="O_discount" name="discountPercent" typeRef="integer" />
    <rule id="R_premium">
      <inputEntry id="IE_prem"><text>"premium"</text></inputEntry>
      <outputEntry id="OE_20"><text>20</text></outputEntry>
    </rule>
  </decisionTable>
</decision>
```

When you evaluate `tierDiscount`, Operaton automatically evaluates `customerTier` first and passes its output (`tier`) as input to the downstream decision.

---

## Using DMN in BPMN Processes

### Business Rule Task

Embed a DMN decision in a BPMN process using a Business Rule Task:

```xml
<bpmn:businessRuleTask id="calcDiscount"
    name="Calculate Discount"
    camunda:decisionRef="discount"
    camunda:mapDecisionResult="singleEntry"
    camunda:resultVariable="discountPercent">
</bpmn:businessRuleTask>
```

| Attribute | Description |
|---|---|
| `camunda:decisionRef` | The `id` of the decision in the `.dmn` file. |
| `camunda:mapDecisionResult` | How to map the result: `singleEntry`, `singleResult`, `collectEntries`, `resultList`. |
| `camunda:resultVariable` | The process variable to store the result in. |

### Result Mapping Options

| Mapping | Use Case | Result Type |
|---|---|---|
| `singleEntry` | One rule, one output column | Single value |
| `singleResult` | One rule, multiple output columns | Map |
| `collectEntries` | Multiple rules, one output column | List of values |
| `resultList` | Multiple rules, multiple output columns | List of maps |

---

## Modeling Tools

- [**Operaton Modeler**](https://operaton.org/) — Desktop application for BPMN and DMN modeling
- [**bpmn.io DMN Editor**](https://demo.bpmn.io/dmn) — Free web-based DMN editor
- Any text editor — DMN files are XML and can be hand-edited

---

## Best Practices

1. **Use descriptive decision IDs** — The `id` attribute is the key used in the API (`Evaluate Decision    discount`).
2. **Always include a default rule** — Add a catch-all rule with empty input entries at the bottom of FIRST-policy tables.
3. **Choose the right hit policy** — Use UNIQUE for mutually exclusive rules, FIRST for priority-based rules.
4. **Type your inputs and outputs** — Set `typeRef` on inputs and outputs to catch type mismatches early.
5. **Keep decisions small** — Split complex logic into multiple decisions connected via a DRG.
6. **Test decisions independently** — Use Robot Framework's DMN keywords to test decisions in isolation before embedding them in BPMN processes.
