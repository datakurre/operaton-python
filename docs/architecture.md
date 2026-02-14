# Architecture

This document describes the architecture of the Operaton GraalPy extension, showing how the components interact to provide Python scripting in BPMN processes and Robot Framework testing.

---

## High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Operaton BPM Engine                          │
│                                                                     │
│  ┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ BPMN Process │    │ Business Rule    │    │ Script Task      │  │
│  │ Instance     │───▶│ Task (DMN)       │    │ (scriptFormat=   │  │
│  │              │    │                  │    │  "python")       │  │
│  └──────────────┘    └──────────────────┘    └────────┬─────────┘  │
│                                                       │            │
│                       ┌───────────────────────────────┘            │
│                       ▼                                            │
│              ┌─────────────────┐                                   │
│              │ ScriptEngine    │                                   │
│              │ Manager (SPI)   │                                   │
│              └────────┬────────┘                                   │
└───────────────────────┼────────────────────────────────────────────┘
                        │  discovers via META-INF/services
                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│              operaton-bpm-extension-python (JSR-223)                │
│                                                                     │
│  ┌─────────────────────────┐    ┌──────────────────────────────┐   │
│  │ PythonScriptEngine      │    │ PythonScriptEngineFactory    │   │
│  │ Factory                 │───▶│ (creates engine instances)   │   │
│  │ (SPI entry point)       │    │                              │   │
│  └─────────────────────────┘    └──────────────┬───────────────┘   │
│                                                 │                   │
│  ┌──────────────────────────────────────────────┼───────────────┐  │
│  │ PythonScriptEngine                           ▼               │  │
│  │                                                              │  │
│  │  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐  │  │
│  │  │ PythonScript     │  │ PythonBindings   │  │ Python     │  │  │
│  │  │ Context          │  │ (wraps polyglot  │  │ Resources  │  │  │
│  │  │ (ENGINE_SCOPE/   │  │  bindings)       │  │ (VFS,      │  │  │
│  │  │  GLOBAL_SCOPE)   │  │                  │  │  config)   │  │  │
│  │  └─────────────────┘  └──────────────────┘  └────────────┘  │  │
│  │                                                              │  │
│  │  Implements: ScriptEngine, Compilable, Invocable,            │  │
│  │              AutoCloseable                                   │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                 │                                   │
└─────────────────────────────────┼───────────────────────────────────┘
                                  │  owns isolated
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GraalPy Runtime (Polyglot)                       │
│                                                                     │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ Python       │  │ Virtual File     │  │ Host Interop         │  │
│  │ Context      │  │ System (VFS)     │  │ (Java ↔ Python)      │  │
│  │ (isolated)   │  │ (bundled libs)   │  │                      │  │
│  └──────────────┘  └──────────────────┘  └──────────────────────┘  │
│                                                                     │
│  Python standard library + pip packages bundled in JAR              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Component Interaction Flow

### Script Task Execution

```
BPMN Process Instance
        │
        │  encounters <scriptTask scriptFormat="python">
        ▼
Operaton ScriptEngine Manager
        │
        │  looks up "python" engine via SPI
        │  (META-INF/services/javax.script.ScriptEngineFactory)
        ▼
PythonScriptEngineFactory
        │
        │  creates new PythonScriptEngine
        │  (each instance gets its own GraalPy Context)
        ▼
PythonScriptEngine.eval(script, context)
        │
        │  1. Extracts process variables from ScriptContext bindings
        │  2. Injects variables into GraalPy polyglot bindings
        │  3. Evaluates Python source via GraalPy Context
        │  4. Returns result (stored in camunda:resultVariable)
        ▼
GraalPy Context
        │
        │  executes Python code with access to:
        │  - Process variables (as Python globals)
        │  - Java classes (via polyglot interop)
        │  - Spin S() function (via spin.py auto-import)
        │  - Python standard library
        ▼
Result → stored as process variable
```

### Robot Framework Test Execution

```
Robot CLI (Java main class)
        │
        │  creates GraalPy Context via GraalPyResources
        │  injects cwd and args into Python bindings
        ▼
GraalPy Context
        │
        │  executes: from robot.run import run_cli; run_cli()
        │  (Robot Framework 7.1.1 is bundled in VFS)
        ▼
Robot Framework
        │
        │  loads ProcessEngine.py keyword library
        │  parses .robot test suite files
        ▼
ProcessEngine Keywords
        │
        │  uses GraalPy Java interop to call Operaton APIs:
        │  - ProcessEngineConfiguration (standalone in-memory)
        │  - RuntimeService, TaskService, DecisionService, etc.
        │  - BpmnAwareTests assertions
        ▼
Operaton BPM Engine (in-memory)
        │
        │  deploys BPMN/DMN, starts instances,
        │  evaluates decisions, manages tasks
        ▼
Test Results → Robot output.xml, log.html, report.html
```

---

## Module Dependencies

```
operaton-bpm-extension-python
├── GraalPy (python-community, python-embedding)
├── Operaton Engine (provided)
└── Operaton Spin (optional, for S() function)

operaton-bpm-extension-robot
├── operaton-bpm-extension-python
├── GraalPy (python-community, python-embedding)
├── graalpy-maven-plugin (bundles Robot Framework 7.1.1 in VFS)
└── Operaton Engine

operaton-bpm-extension-example
├── operaton-bpm-extension-python
├── Spring Boot (REST + Webapp starters)
├── Operaton Spring Boot starters
├── Operaton Spin
└── H2 Database
```

---

## Key Design Decisions

### Engine Isolation

Each `PythonScriptEngine` instance owns its own GraalPy `Context`:

```
Engine A ──owns──▶ Context A (independent Python namespace)
Engine B ──owns──▶ Context B (independent Python namespace)
Engine C ──owns──▶ Context C (independent Python namespace)
```

This ensures:
- **No state leakage** between concurrent process instances
- **Thread safety** without synchronization
- **Clean globals** for each script evaluation

### SPI Discovery

The engine is registered via standard Java `ServiceLoader`:

```
META-INF/services/javax.script.ScriptEngineFactory
└── org.operaton.bpm.extension.python.PythonScriptEngineFactory
```

Operaton's `ScriptEngineManager` automatically discovers the factory at startup. No configuration is needed.

### VFS Bundling

GraalPy's Virtual File System bundles the Python runtime and packages inside the JAR:

```
JAR
├── org/graalvm/python/vfs/
│   ├── home/           (Python standard library)
│   └── proj/           (project packages)
│       └── robot/      (Robot Framework 7.1.1)
└── script/env/python/
    └── spin.py         (Spin S() auto-import)
```

This makes the extension fully self-contained — no external Python installation required.

### ScriptContext Bridge

Process variables flow through a layered binding system:

```
Operaton Process Variables
        ↓
ScriptContext (ENGINE_SCOPE bindings)
        ↓
PythonBindings (wraps GraalPy polyglot bindings)
        ↓
GraalPy Context polyglot bindings
        ↓
Python globals (accessible in script)
```

The `PythonScriptContext` maintains proper scope differentiation (ENGINE_SCOPE vs GLOBAL_SCOPE), following the JSR-223 specification.
