
# Operaton BPM Extension: GraalPy (Python Scripting)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![GraalPy](https://img.shields.io/badge/GraalPy-25.0.2-green.svg)](https://www.graalvm.org/python/)
[![Operaton](https://img.shields.io/badge/Operaton-1.0.3-purple.svg)](https://operaton.org/)
[![Coverage](https://img.shields.io/badge/coverage-93%25-brightgreen.svg)](#test-coverage)

> **⚠️ This project is developed with the assistance of AI coding agents.** All code, tests, and documentation may have been authored or co-authored by automated agents. Please review carefully before production use.

An extension for [Operaton BPM](https://operaton.org/) that integrates [GraalPy](https://www.graalvm.org/python/) (GraalVM's Python implementation) as a JSR-223 script engine, enabling Python script tasks in BPMN process definitions. Also includes a GraalPy-based [Robot Framework](https://robotframework.org/) test runner for BPM process and DMN decision testing.

---

## Table of Contents

- [Overview](#overview)
- [Modules](#modules)
- [Usage Guide](#usage-guide)
- [Script Environment](#script-environment)
- [Configuration Options](#configuration-options)
- [Robot Framework](#robot-framework)
  - [BPMN Process Keywords](#processengine-keyword-library-reference)
  - [DMN Decision Keywords](#dmn-decision-keywords)
- [Building](#building)
- [Project Structure](#project-structure)
- [Test Coverage](#test-coverage)
- [License](#license)

---

## Overview

This monorepo provides three Maven modules for integrating Python scripting into Operaton BPM:

1. **`operaton-bpm-extension-python`** — JSR-223 `ScriptEngineFactory` that bridges Operaton's script task execution with GraalPy's polyglot Python runtime. Allows BPMN processes to use `scriptFormat="python"` in script tasks.
2. **`operaton-bpm-extension-robot`** — GraalPy-based Robot Framework runner with an Operaton keyword library (`ProcessEngine`) for writing BPM process and DMN decision acceptance tests in `.robot` files.
3. **`operaton-bpm-extension-example`** — Spring Boot demo application showcasing Python script tasks with the Operaton web UI (Cockpit/Tasklist).

### Key Components

| Component | Description |
|---|---|
| `PythonScriptEngineFactory` | JSR-223 SPI entry point; creates isolated `PythonScriptEngine` instances |
| `PythonScriptEngine` | Core engine implementing `ScriptEngine`, `Compilable`, `Invocable`, `AutoCloseable` |
| `PythonCompiledScript` | Pre-parsed Python source for repeated evaluation |
| `PythonScriptContext` | Script context with proper ENGINE_SCOPE / GLOBAL_SCOPE differentiation |
| `PythonBindings` | `Bindings` implementation wrapping GraalPy's polyglot bindings |
| `PythonResources` | Configures the GraalPy `Context` with VFS, Python home, and interpreter options |
| `Robot` | CLI entry point for running Robot Framework test suites via GraalPy |
| `ProcessEngine` | Robot Framework keyword library for Operaton BPM process and DMN decision testing |

---

## Modules

### operaton-bpm-extension-python

The core Python scripting extension. Add this to your Operaton deployment to enable `scriptFormat="python"` in BPMN script tasks.

**Maven coordinates:**

```xml
<dependency>
  <groupId>org.operaton.bpm.extension.python</groupId>
  <artifactId>operaton-bpm-extension-python</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### operaton-bpm-extension-robot

Robot Framework test runner with Operaton keyword library. Use this for BPM process and DMN decision acceptance testing.

**Maven coordinates:**

```xml
<dependency>
  <groupId>org.operaton.bpm.extension.robot</groupId>
  <artifactId>operaton-bpm-extension-robot</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### operaton-bpm-extension-example

Spring Boot demo application with Operaton REST API, Cockpit/Tasklist web UI, Spin JSON/XML support, and demo user (`demo`/`demo`).

---

## Usage Guide

### Adding Python Scripting to an Existing Operaton Deployment

#### 1. Add the Maven dependency

Add the `operaton-bpm-extension-python` artifact and the GraalPy runtime to your project:

```xml
<properties>
  <graalpy.version>25.0.2</graalpy.version>
</properties>

<dependencies>
  <!-- Python ScriptEngine -->
  <dependency>
    <groupId>org.operaton.bpm.extension.python</groupId>
    <artifactId>operaton-bpm-extension-python</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>

  <!-- GraalPy runtime (required) -->
  <dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>python-community</artifactId>
    <version>${graalpy.version}</version>
    <type>pom</type>
  </dependency>
  <dependency>
    <groupId>org.graalvm.python</groupId>
    <artifactId>python-embedding</artifactId>
    <version>${graalpy.version}</version>
  </dependency>
</dependencies>
```

#### 2. Use Python in BPMN script tasks

The extension is auto-discovered via Java's `ServiceLoader` (SPI). No additional configuration is required. Simply set `scriptFormat="python"` on your BPMN script tasks:

```xml
<bpmn:scriptTask id="greet" scriptFormat="python" camunda:resultVariable="greeting">
  <bpmn:script>f"Hello {name}"</bpmn:script>
</bpmn:scriptTask>
```

#### 3. (Optional) Enable Spin JSON/XML support

If you use Operaton Spin, the `S()` function is automatically available in Python scripts via the bundled `spin.py` script environment. Make sure the Spin plugin is configured in your process engine:

```xml
<property name="processEnginePlugins">
  <list>
    <bean class="org.operaton.spin.plugin.impl.SpinProcessEnginePlugin" />
  </list>
</property>
```

Then use Spin from Python:

```python
import json
S(json.dumps({"key": "value"}), "application/json")
```

---

## Script Environment

### Process Variables

Operaton process variables are automatically injected into the Python execution scope via the JSR-223 `ScriptContext` bindings. All variables from the current execution are accessible as Python globals:

```python
# Access process variables directly
f"Hello {name}"

# Use execution object for advanced operations
execution.setVariable("result", "computed value")
```

The `camunda:resultVariable` attribute on the script task stores the script's return value (the last expression evaluated) back into the process as a variable.

### Spin S() Function

When the Operaton Spin plugin is active, the `S()` function is auto-imported from `org.operaton.spin.Spin` and available globally in Python scripts. This is handled by the bundled `spin.py` script environment at `script/env/python/spin.py`.

```python
import json

# Create a Spin JSON object from a Python dict
data = S(json.dumps({"greeting": "Hello " + name}), "application/json")

# Parse existing JSON
parsed = S('{"key": "value"}')
```

### Python Standard Library

GraalPy provides a compatible Python 3.x standard library. Common modules such as `json`, `os`, `sys`, `re`, `math`, `datetime`, and `collections` are available for use in script tasks.

### Engine Isolation

Each script engine instance owns its own GraalPy `Context`, providing full isolation:
- **No state leakage** between script executions across different process instances
- **Thread safety** — concurrent script executions on separate engine instances do not interfere
- **Clean globals** — each engine starts with a fresh Python namespace

---

## Configuration Options

### GraalPy Context Settings

The GraalPy context is configured in `PythonResources.contextBuilder()` with the following defaults:

| Option | Default | Description |
|---|---|---|
| `python.PosixModuleBackend` | `java` | Uses Java-based POSIX emulation (no native POSIX) |
| `python.DontWriteBytecodeFlag` | `true` | Disables `.pyc` bytecode file generation |
| `python.VerboseFlag` | `false` | Set `PYTHONVERBOSE=1` env var to enable verbose import logging |
| `python.WarnOptions` | (empty) | Set `PYTHONWARNINGS` env var to configure Python warnings |
| `python.AlwaysRunExcepthook` | `true` | Ensures Python exception hooks are always invoked |
| `python.ForceImportSite` | `true` | Forces `site.py` import on startup |
| `python.CheckHashPycsMode` | `never` | Disables hash-based `.pyc` validation |

Additional context permissions:
- `allowHostAccess(HostAccess.ALL)` — full Java interop from Python
- `allowCreateThread(true)` — Python code can create threads
- `allowNativeAccess(true)` — access to native modules
- `allowPolyglotAccess(PolyglotAccess.ALL)` — cross-language interop

### Environment Variables

| Variable | Effect |
|---|---|
| `PYTHONVERBOSE` | When set, enables verbose GraalPy import logging (`python.VerboseFlag=true`, log level `FINE`) |
| `PYTHONWARNINGS` | Configures Python warning filters (passed to `python.WarnOptions`) |

### Virtual File System (VFS)

GraalPy uses a Virtual File System to bundle Python packages and resources within the JAR. The VFS is configured via the `graalpy-maven-plugin` in `pom.xml`:

```xml
<plugin>
  <groupId>org.graalvm.python</groupId>
  <artifactId>graalpy-maven-plugin</artifactId>
  <version>${graalpy.version}</version>
  <executions>
    <execution>
      <configuration>
        <packages>
          <!-- Add pip packages here, e.g.: -->
          <!-- <package>requests==2.31.0</package> -->
        </packages>
        <pythonHome>
          <includes>
            <include>.*</include>
          </includes>
        </pythonHome>
      </configuration>
      <goals>
        <goal>process-graalpy-resources</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Python packages listed in `<packages>` are downloaded at build time and embedded into the JAR's VFS, making them available for import in script tasks without any runtime installation.

---

## Robot Framework

### Overview

The `operaton-bpm-extension-robot` module provides a GraalPy-based Robot Framework runner with a built-in `ProcessEngine` keyword library for testing Operaton BPM processes. Robot Framework 7.1.1 is bundled via the GraalPy VFS.

### ProcessEngine Keyword Library Reference

The `ProcessEngine` library (scope: GLOBAL) provides the following keywords for BPM process testing:

| Keyword | Arguments | Description |
|---|---|---|
| **Setup Process Engine** | — | Creates a standalone in-memory Operaton process engine. Call in test setup. |
| **Teardown Process Engine** | — | Closes and releases the process engine. Call in test teardown. |
| **Deploy Resources** | `*paths`, `name=Test Deployment` | Deploys one or more BPMN files to the engine. Accepts file paths as positional args. |
| **Start Instance** | `process_definition_key` | Starts a process instance by key. Returns the process instance ID. Asserts the instance started. |
| **Should Have Task** | `process_instance_id`, `task_definition_key` | Asserts that the process instance has an active user task with the given definition key. |
| **Complete Task** | `process_instance_id`, `task_definition_key=""`, `variables=None` | Completes a user task. Optionally filters by task definition key and passes variables. |
| **Get Variable** | `process_instance_id`, `variable_name` | Returns the value of a process variable. |
| **Set Variable** | `process_instance_id`, `variable_name`, `variable_value` | Sets a process variable on a running instance. |
| **Get Tasks** | `process_instance_id` | Returns a list of all active tasks for the process instance. |

#### DMN (Decision) Keywords

| Keyword | Arguments | Description |
|---|---|---|
| **Evaluate Decision** | `decision_key`, `**variables` | Evaluates a deployed DMN decision by key. Variables are passed as keyword args. Returns a list of dicts (one per matched rule). |
| **Evaluate Decision Table** | `decision_key`, `**variables` | Evaluates a deployed DMN decision table by key. Like Evaluate Decision but specifically for decision tables. |
| **Decision Result Should Contain** | `result`, `output_name`, `expected_value` | Asserts that at least one matched rule contains the expected output value. |
| **Decision Single Result** | `result` | Returns the single matched rule from the result. Fails if not exactly one rule matched. |
| **Decision Single Entry** | `result` | Returns the single output value from a result with one matched rule and one output column. |

### Writing Robot Framework Test Suites

#### Basic test structure

```robot
*** Settings ***
Library    ProcessEngine

*** Test Cases ***

My Process Test
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    Deploy Resources    ${CURDIR}${/}my-process.bpmn
    ${instance}=    Start Instance    my-process-id
    Should Have Task    ${instance}    my-user-task
    Complete Task    ${instance}    my-user-task
```

#### Testing with process variables

```robot
*** Settings ***
Library    ProcessEngine

*** Test Cases ***

Variable Round-Trip
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    Deploy Resources    ${CURDIR}${/}process.bpmn
    ${instance}=    Start Instance    my-process
    Set Variable    ${instance}    myVar    hello-robot
    ${value}=    Get Variable    ${instance}    myVar
    Should Be Equal    ${value}    hello-robot
```

#### Testing DMN decision tables

```robot
*** Settings ***
Library    ProcessEngine

*** Test Cases ***

Evaluate Gold Customer Discount
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    Deploy Resources    ${CURDIR}${/}discount.dmn
    ${result}=    Evaluate Decision    discount    customerType=gold
    ${entry}=    Decision Single Entry    ${result}
    Should Be Equal As Integers    ${entry}    15

Verify Decision Result Contains Expected Output
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    Deploy Resources    ${CURDIR}${/}discount.dmn
    ${result}=    Evaluate Decision    discount    customerType=silver
    Decision Result Should Contain    ${result}    discountPercent    10
```

The DMN keywords use Operaton's `DecisionService` API under the hood. Deploy `.dmn` files with `Deploy Resources` (same as BPMN), then evaluate decisions by key with input variables passed as keyword arguments. Results are returned as Python lists of dictionaries, with one dict per matched rule.

#### Running test suites

Run Robot Framework suites via the `Robot` CLI class:

```bash
java -jar operaton-bpm-extension-robot.jar src/test/resources/example/Example.robot
```

Or from Maven with the main class configured:

```bash
mvn exec:java -pl operaton-bpm-extension-robot -Dexec.args="path/to/suite.robot"
```

The `ProcessEngine` library must be on the Python path. Place your `.robot` files alongside your BPMN process definitions, and use `${CURDIR}${/}` to reference relative paths.

#### Organizing test suites

```
tests/
├── resources/
│   ├── my-process.bpmn
│   └── another-process.bpmn
├── MyProcessTest.robot
└── AnotherProcessTest.robot
```

Each test case should use `[Setup]` and `[Teardown]` to manage the process engine lifecycle, ensuring clean isolation between tests.

---

## Building

### Prerequisites

The development environment uses [devenv](https://devenv.sh/) with Nix for reproducible tooling. Alternatively, ensure you have:

- JDK 17+ (GraalVM JDK 21 recommended)
- Apache Maven 3.9+

### Compile

```bash
mvn package -DskipTests
```

### Run tests

```bash
mvn test
```

### Full verification (compile + test + integration checks)

```bash
mvn verify
```

### Format, check, and test (via Makefile)

```bash
make format check test
```

### Run the example application

```bash
mvn -pl operaton-bpm-extension-example spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080) and log in with `demo` / `demo`.

### Native Image (experimental)

```bash
mvn package -Pnative
```

Native image configuration (reflection, resources) is provided in `META-INF/native-image/`.

---

## Project Structure

```
operaton-bpm-extension-python/          # Core Python ScriptEngine
  src/main/java/.../python/
    PythonScriptEngineFactory.java        # JSR-223 SPI factory
    PythonScriptEngine.java               # Core script engine
    PythonCompiledScript.java             # Compiled script support
    PythonScriptContext.java              # ScriptContext implementation
    PythonBindings.java                   # Bindings implementation
    PythonResources.java                  # GraalPy Context configuration
  src/main/resources/
    META-INF/services/                    # SPI registration
    script/env/python/spin.py             # Spin S() auto-import

operaton-bpm-extension-robot/           # Robot Framework Runner
  src/main/java/.../robot/
    Robot.java                            # CLI entry point
  src/main/resources/.../
    ProcessEngine.py                      # Robot keyword library (BPMN + DMN)
    robot_runner.py                       # Robot execution bridge

operaton-bpm-extension-example/         # Spring Boot Demo
  src/main/java/.../example/
    Application.java                      # Spring Boot entry point
  src/main/resources/
    example-python-script-task.bpmn       # Python f-string demo
    example-python-spin-script-task.bpmn  # Spin S() demo
```

---

## Test Coverage

Test coverage is measured with JaCoCo (0.8.12) and reported during the `test` phase. Current instruction coverage for the core Python module is **~93%**.

| Class | Instructions | Branches | Lines |
|---|---|---|---|
| `PythonScriptContext` | 100% | 93% | 100% |
| `PythonCompiledScript` | 100% | 79% | 100% |
| `PythonScriptEngineFactory` | 96% | 83% | 94% |
| `PythonScriptEngine` | 90% | 95% | 86% |
| `PythonBindings` | 89% | 86% | 91% |
| `PythonResources` | 86% | 50% | 100% |

---

## Example BPMN Processes

Two example process definitions are included in the example module:

- **`example-python-script-task.bpmn`** — Demonstrates Python f-string interpolation accessing process variables (`f"Hello {word}"`)
- **`example-python-spin-script-task.bpmn`** — Demonstrates using Operaton Spin's `S()` function from Python to create JSON objects

---

## License

[Apache License 2.0](LICENSE)
