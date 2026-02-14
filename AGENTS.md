# AGENTS.md — Project State Analysis

## Project Identity

**Name:** `operaton-bpm-extension-python`
**Purpose:** GraalPy (Python) scripting integration for Operaton BPM via JSR-223
**Last Activity:** November 29, 2024 (2 WIP commits)
**Status:** Abandoned incomplete prototype

---

## Git History

| Date | Commit | Message |
|---|---|---|
| 2024-11-12 | `15026af` | First commit |
| 2024-11-12 | `653e527` | Cleanup experiments |
| 2024-11-29 | `7b94a0a` | Add robotframework support |
| 2024-11-29 | `c186a91` | WIP |
| 2024-11-29 | `16c39c9` | WIP |

The project was developed over two sessions (~2 days of work) and left in a WIP state.

---

## Architecture

### How It Works

1. `GraalPyScriptEngineFactory` is registered via `META-INF/services/javax.script.ScriptEngineFactory` (Java SPI)
2. Operaton's `ScriptEngineManager` discovers it and requests engines for `scriptFormat="python"`
3. The factory creates `GraalPyScriptEngine` instances that delegate to a shared GraalPy `Context`
4. Python scripts in BPMN script tasks are evaluated via GraalPy's polyglot API
5. Process variables are exposed through `Bindings` backed by GraalPy's polyglot bindings
6. A `spin.py` script environment auto-imports Operaton Spin's `S()` function

### Spring Boot Application

The `Application.java` bootstraps a Spring Boot app with:
- Operaton BPM REST API (`operaton-bpm-spring-boot-starter-rest`)
- Operaton Cockpit/Tasklist web UI (`operaton-bpm-spring-boot-starter-webapp`)
- Operaton Spin for JSON/XML handling
- H2 in-memory database
- Demo user (`demo`/`demo`) with admin privileges

---

## Implementation Status by Class

### ✅ Fully Implemented

| Class | Notes |
|---|---|
| `Application.java` | Spring Boot entry point with demo user setup |
| `GraalPyContextBuilder.java` | Context configuration (has one TODO about deprecated constant GR-54915) |
| `GraalPyCompiledScript.java` | Pre-parsed script evaluation (but ignores ScriptContext parameter) |

### ⚠️ Partially Implemented

| Class | Issues |
|---|---|
| `GraalPyScriptEngine.java` | `close()` prints to stdout instead of cleaning up; `eval(String, ScriptContext)` and `eval(Reader, ScriptContext)` ignore the ScriptContext parameter; `invokeMethod` and `invokeFunction` throw `UnsupportedOperationException`; `getInterface()` methods return `null`; `createBindings()` returns `null` |
| `GraalPyScriptEngineFactory.java` | `getMethodCallSyntax()`, `getOutputStatement()`, `getProgram()` are stubbed (return `null`) |
| `GraalPyBindings.java` | `putAll()` is empty (no-op) |
| `GraalPyScriptContext.java` | `setBindings()` is empty; `getScopes()` always creates a new list; no real scope differentiation between ENGINE_SCOPE and GLOBAL_SCOPE |

### ❌ Not Implemented

| Feature | Notes |
|---|---|
| Tests | No `src/test/` directory exists; zero test coverage |
| Robot Framework integration | `robot.py` is an empty file; `robotframework==7.1.1` is bundled but unused |
| Native image support | Maven profile exists but `native-image/` config dir has only `.gitkeep` |
| Proper resource cleanup | `close()` does not release GraalPy Context |
| Thread safety | Single shared Context across all engine instances |

---

## Critical Architectural Concerns

### 1. Shared GraalPy Context (High Risk)

`GraalPyScriptEngineFactory` creates **one** `Context` at construction time and shares it across all `GraalPyScriptEngine` instances. This causes:
- **State leakage** between script executions (Python globals persist)
- **Thread-safety issues** — GraalPy Contexts are not thread-safe by default
- Operaton executes scripts from multiple threads concurrently

**Recommendation:** Create a new Context per engine instance, or use Context pooling with proper synchronization. The `allowAllAccess(true)` flag is set but does not make Contexts thread-safe.

### 2. ScriptContext Ignored (High Risk)

The `eval(String, ScriptContext)` and `eval(Reader, ScriptContext)` methods in `GraalPyScriptEngine` **ignore the ScriptContext parameter** and call `eval(String)` / `eval(Reader)` directly. Operaton passes process variables (execution, task, etc.) through the ScriptContext's bindings. This means:
- Process variables may not be accessible from Python scripts
- The integration may silently fail or produce incorrect results

**Recommendation:** Extract bindings from the provided ScriptContext and inject them into the GraalPy evaluation context before script execution.

### 3. No Scope Differentiation (Medium Risk)

`GraalPyScriptContext` maps both `ENGINE_SCOPE` and `GLOBAL_SCOPE` to the same underlying Python bindings. The JSR-223 spec expects these to be separate namespaces.

### 4. Resource Leaks (Medium Risk)

`GraalPyScriptEngine.close()` prints `"GraalPyScriptEngine.close()"` to stdout but does not:
- Close the GraalPy Context
- Release I/O streams
- Clean up any allocated resources

---

## Dev Environment

### Nix/devenv Setup

- `devenv.yaml` — Defines inputs (nixpkgs 25.11, custom mvn2nix/devcontainer modules)
- `devenv.nix` — JDK 21, formatting tools, devcontainer profile
- `devenv.local.nix.example` — VS Code extensions and Podman/GPG tweaks
- `flake.nix` — Legacy Nix flake (references `.nix/` directory that doesn't exist in repo)

### Makefile

The Makefile references `org.operaton.bpm.extension.robot` package paths which don't match the actual source (`org.operaton.bpm.extension.python`). This is a leftover from a rename/refactor.

---

## `py/` Subdirectory

The `py/python/` directory contains an **unrelated standalone GraalPy example** from Oracle's GraalPy documentation (Apache-licensed reference code). It has its own `pom.xml` with `org.example` groupId and is not part of the main build. This is reference material, not production code.

---

## File Inventory

### Source Files (7 Java classes)
- `src/main/java/org/operaton/bpm/extension/python/Application.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyBindings.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyCompiledScript.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyContextBuilder.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyScriptContext.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyScriptEngine.java`
- `src/main/java/org/operaton/bpm/extension/python/GraalPyScriptEngineFactory.java`

### Resource Files
- `src/main/resources/META-INF/services/javax.script.ScriptEngineFactory`
- `src/main/resources/org.graalvm.python.vfs/src/robot.py` (empty)
- `src/main/resources/script/env/python/spin.py`

### BPMN Examples
- `example-python-script-task.bpmn`
- `example-js-script-task.bpmn` (misleading name — actually uses Python)

### Build/Dev Config
- `pom.xml`, `Makefile`, `devenv.yaml`, `devenv.nix`, `flake.nix`
