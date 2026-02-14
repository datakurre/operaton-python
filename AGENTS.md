# AGENTS.md — Project State Analysis

## Project Identity

**Name:** `operaton-bpm-extension-graalpy`
**Purpose:** GraalPy (Python) scripting integration for Operaton BPM via JSR-223, plus a Robot Framework test runner for BPM process/decision acceptance testing
**Repository:** https://github.com/operaton/operaton-python
**License:** Apache 2.0
**Status:** Active development (AI-assisted)

---

## Architecture

### How It Works

1. `PythonScriptEngineFactory` is registered via `META-INF/services/javax.script.ScriptEngineFactory` (Java SPI)
2. Operaton's `ScriptEngineManager` discovers it and requests engines for `scriptFormat="python"`
3. The factory creates `PythonScriptEngine` instances, each with its own isolated GraalPy `Context`
4. Python scripts in BPMN script tasks are evaluated via GraalPy's polyglot API
5. Process variables are injected through `Bindings` backed by GraalPy's polyglot bindings, via a proper `ScriptContext` implementation
6. A `spin.py` script environment auto-imports Operaton Spin's `S()` function

### Robot Framework Runner

The `operaton-bpm-extension-robot` module provides:
1. `Robot.java` — CLI entry point that creates a GraalPy `Context` and runs Robot Framework's `run_cli()`
2. `ProcessEngine.py` — A Robot Framework keyword library (scope: GLOBAL) that wraps Operaton's Java API for BPM process testing
3. `robot_runner.py` — A variable bridge that injects/captures Robot variables for programmatic execution
4. Robot Framework 7.1.1 is bundled via GraalPy's VFS (virtual file system) through `graalpy-maven-plugin`

### Spring Boot Example Application

`Application.java` bootstraps a Spring Boot app with:
- Operaton BPM REST API (`operaton-bpm-spring-boot-starter-rest`)
- Operaton Cockpit/Tasklist web UI (`operaton-bpm-spring-boot-starter-webapp`)
- Operaton Spin for JSON/XML handling
- H2 in-memory database
- Demo user (`demo`/`demo`) with admin privileges

---

## Module Structure

### operaton-bpm-extension-python (Core JSR-223 Engine)

| Class | Status | Notes |
|---|---|---|
| `PythonScriptEngineFactory` | ✅ Complete | JSR-223 SPI factory; creates isolated engine instances |
| `PythonScriptEngine` | ✅ Complete | Implements `ScriptEngine`, `Compilable`, `Invocable`, `AutoCloseable`; each instance owns its own GraalPy `Context` |
| `PythonCompiledScript` | ✅ Complete | Pre-parsed Python source for repeated evaluation |
| `PythonScriptContext` | ✅ Complete | Proper ENGINE_SCOPE / GLOBAL_SCOPE differentiation |
| `PythonBindings` | ✅ Complete | `Bindings` wrapping GraalPy polyglot bindings |
| `PythonResources` | ✅ Complete | Configures GraalPy Context with VFS, Python home, interpreter options |

### operaton-bpm-extension-robot (Robot Framework Runner)

| File | Status | Notes |
|---|---|---|
| `Robot.java` | ✅ Complete | CLI entry point for running `.robot` suites |
| `ProcessEngine.py` | ✅ BPMN + DMN | Keywords for BPMN process and DMN decision testing |
| `robot_runner.py` | ✅ Complete | Variable bridge for programmatic Robot execution |

### operaton-bpm-extension-example (Spring Boot Demo)

| File | Status | Notes |
|---|---|---|
| `Application.java` | ✅ Complete | Spring Boot entry with demo user setup |

---

## Test Coverage

Test coverage is measured with JaCoCo (0.8.12). Current instruction coverage for the Python module is **~93%**.

### operaton-bpm-extension-python (8 test classes, ~75 test methods)

| Test Class | Scope |
|---|---|
| `PythonScriptEngineTest` | 39 tests — eval, ScriptContext handling, bindings, compilation, invocation, isolation, close |
| `PythonScriptEngineFactoryTest` | 19 tests — factory metadata, engine names, MIME types, language info |
| `PythonBindingsTest` | 17 tests — size/empty, contains, get, put, remove, collection views |
| `PythonScriptContextTest` | 22 tests — bindings management, attributes, I/O streams, scopes |
| `PythonProcessEngineTest` | 2 tests — Operaton engine integration with Python scripts |
| `PythonSpinIntegrationTest` | 1 test — Spin `S()` function from Python |
| `PythonResourcesTest` | 5 tests — GraalPy context configuration |
| `PythonConcurrencyTest` | 1 test — 4-thread concurrent execution |

### operaton-bpm-extension-robot (4 test classes, ~6 test methods)

| Test Class | Scope |
|---|---|
| `RobotCliTest` | 2 tests — basic Robot CLI invocation, suite execution |
| `ProcessEngineKeywordTest` | 2 tests — BPMN keyword end-to-end, task completion |
| `VariableBridgeTest` | 1 test — variable round-trip through Robot Framework |
| `DecisionTableTest` | 1 test — DMN decision table evaluation via Robot keywords |

---

## Key Design Decisions

### Engine Isolation (Resolved)

Each `PythonScriptEngine` instance owns its own GraalPy `Context`. This provides:
- **No state leakage** between script executions
- **Thread safety** — concurrent executions on separate engine instances don't interfere
- **Clean globals** — each engine starts with a fresh Python namespace

### ScriptContext Handling (Resolved)

The `eval(String, ScriptContext)` methods properly extract bindings from the provided `ScriptContext` and inject them into the GraalPy evaluation context. Operaton process variables are correctly accessible from Python scripts.

### Scope Differentiation (Resolved)

`PythonScriptContext` provides proper ENGINE_SCOPE / GLOBAL_SCOPE differentiation with separate underlying bindings.

---

## What's Missing

### DMN (Decision Model and Notation) Enhancements

Basic DMN support is implemented with `Evaluate Decision`, `Evaluate Decision Table`, `Decision Result Should Contain`, `Decision Single Result`, and `Decision Single Entry` keywords. Remaining work:
- Multi-output decision table testing
- DRG (Decision Requirements Graph) evaluation keywords
- Testing decisions embedded in BPMN processes (Business Rule Tasks)
- Typed variable input support (integer, double, boolean, date)
- `Collect Entries` keyword for extracting all values of a specific output column

### Additional Robot Framework Keywords

The `ProcessEngine` library could be extended with:
- Message correlation keywords
- Signal event keywords
- Timer manipulation keywords
- History query keywords
- Multi-instance/subprocess assertion keywords

---

## Dev Environment

### Nix/devenv Setup

- `devenv.yaml` — Defines inputs (nixpkgs, custom modules)
- `devenv.nix` — JDK 21, formatting tools, devcontainer profile
- `devenv.local.nix.example` — VS Code extensions and Podman/GPG tweaks

### Makefile

```
build:    mvn package -DskipTests
test:     mvn test
check:    mvn verify
format:   google-java-format -i $(JAVA_FILES)
clean:    mvn clean
run:      mvn -pl operaton-bpm-extension-example spring-boot:run
start:    mvn install -DskipTests && mvn -pl operaton-bpm-extension-example spring-boot:run
```

---

## File Inventory

### Source Files
- **8 Java main classes** — 6 (python) + 1 (robot) + 1 (example)
- **12 Java test classes** — 8 (python) + 4 (robot)
- **3 Python files** — `spin.py`, `robot_runner.py`, `ProcessEngine.py`
- **4 Robot files** — `Example.robot`, `CompleteTask.robot`, `VariableBridge.robot`, `DecisionTable.robot`
- **6 BPMN files** — 3 (python tests) + 2 (example) + 1 (robot tests)
- **1 DMN file** — `discount.dmn` (robot tests)
- **4 POM files** — parent + 3 modules
- **3 Native image configs** — `reflect-config.json`, `native-image.properties`, `resource-config.json`
- **2 Engine configs** — `operaton.cfg.xml`, `operaton-spin.cfg.xml`
- **1 SPI file** — `javax.script.ScriptEngineFactory`

### Key Properties
- Java 17+, GraalPy 25.0.2, Operaton 1.0.3, Spring Boot 3.3.3
- Robot Framework 7.1.1 (bundled via GraalPy VFS)
- JaCoCo 0.8.12 for coverage
