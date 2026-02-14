# Troubleshooting Guide

Common issues and solutions when working with the GraalPy scripting extension and Robot Framework runner for Operaton BPM.

---

## GraalPy Issues

### "No language and polyglot implementation was found on the module-path"

**Symptom:** Runtime error when creating a Python script engine or GraalPy context.

**Cause:** The GraalPy runtime JARs are not on the classpath.

**Solution:** Ensure both `python-community` (POM type) and `python-embedding` dependencies are declared:

```xml
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
```

### "Could not find Python home"

**Symptom:** GraalPy fails to initialize with a message about missing Python home.

**Cause:** The GraalPy Virtual File System (VFS) resources are not bundled in the JAR.

**Solution:** Ensure the `graalpy-maven-plugin` is configured in your module's `pom.xml`:

```xml
<plugin>
  <groupId>org.graalvm.python</groupId>
  <artifactId>graalpy-maven-plugin</artifactId>
  <version>${graalpy.version}</version>
  <executions>
    <execution>
      <configuration>
        <packages>
          <!-- List pip packages here if needed -->
        </packages>
      </configuration>
      <goals>
        <goal>process-graalpy-resources</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Run `mvn process-resources` to generate the VFS resources.

### "Multi-threaded access requested but is not allowed"

**Symptom:** Error when multiple threads try to use the same script engine instance.

**Cause:** GraalPy contexts are single-threaded by default.

**Solution:** Each `PythonScriptEngine` instance owns its own GraalPy context. Create a separate engine instance per thread:

```java
ScriptEngineManager manager = new ScriptEngineManager();
// Each call creates a new isolated engine
ScriptEngine engine = manager.getEngineByName("python");
```

Do not share a single `PythonScriptEngine` instance across threads.

### Slow first script execution

**Symptom:** The first Python script evaluation takes several seconds; subsequent evaluations are fast.

**Cause:** GraalPy performs JIT compilation and Python runtime initialization on first use.

**Solution:** This is expected behavior. Strategies to mitigate:

1. **Warm up in test setup** — Evaluate a trivial script (`"1+1"`) during initialization.
2. **Use GraalVM JDK** — GraalPy runs fastest on GraalVM's JIT compiler. Use `distribution: graalvm` in CI.
3. **Reuse engine instances** — Operaton's `ScriptEngineManager` caches engines; avoid creating unnecessary new instances.

### Python import errors

**Symptom:** `ModuleNotFoundError` when importing a Python module in a script task.

**Cause:** The module is not available in GraalPy's VFS or on the Python path.

**Solutions:**

- **Standard library modules** (`json`, `re`, `math`, etc.) should work out of the box.
- **Third-party packages** — Add them to the `graalpy-maven-plugin` configuration:
  ```xml
  <packages>
    <package>requests==2.31.0</package>
  </packages>
  ```
- **Custom modules** — Place them in `src/main/resources/script/env/python/` and they will be auto-loaded via the VFS.

---

## Robot Framework Issues

### "No keyword with name 'Setup Process Engine' found"

**Symptom:** Robot Framework cannot find ProcessEngine keywords.

**Cause:** The `ProcessEngine` library is not on the Python path.

**Solution:** Ensure your `.robot` file imports the library:

```robot
*** Settings ***
Library    ProcessEngine
```

The library is automatically available when running via the `Robot` CLI class, which adds the working directory and `lib/` to `sys.path`.

### "No engine" assertion error

**Symptom:** Keywords fail with `AssertionError: No engine`.

**Cause:** `Setup Process Engine` was not called before using other keywords.

**Solution:** Always call `Setup Process Engine` in test setup:

```robot
*** Test Cases ***
My Test
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    # ... test steps ...
```

### "Process instance not found or has ended"

**Symptom:** Assertion keywords (`Should Be Active`, `Should Have Task`) fail unexpectedly.

**Cause:** The process instance completed before the assertion was checked. This often happens with processes that have no wait states (user tasks, timers, message events).

**Solution:**

1. Ensure your BPMN process has a wait state (user task, receive task, timer event) before the assertion point.
2. Use `Should Be Ended` instead if the process is expected to complete.

### Robot test hangs or times out

**Symptom:** A Robot test does not complete.

**Cause:** A process instance is waiting for an event that never arrives (message, signal, timer).

**Solutions:**

1. **For timer events** — Use `Execute Timer Jobs` to fire pending timers:
   ```robot
   Execute Timer Jobs    ${instance}
   ```
2. **For message events** — Correlate the expected message:
   ```robot
   Correlate Message    myMessage    ${instance}
   ```
3. **For signal events** — Send the expected signal:
   ```robot
   Signal Event    mySignal
   ```

### "Decision definition not found"

**Symptom:** DMN keywords fail with a "decision not found" error.

**Cause:** The `.dmn` file was not deployed, or the decision key does not match.

**Solution:**

1. Deploy the DMN file with `Deploy Resources`:
   ```robot
   Deploy Resources    ${CURDIR}${/}my-decision.dmn
   ```
2. Verify the decision key matches the `id` attribute in the `<decision>` element of your `.dmn` file (not the `name` attribute).

### "Expected exactly 1 matched rule, but got 0"

**Symptom:** `Decision Single Result` or `Decision Single Entry` fails.

**Cause:** No rules matched the provided inputs.

**Solutions:**

1. **Add a default rule** — In FIRST-policy tables, add a catch-all rule with empty input entries.
2. **Check input types** — Ensure input values match the expected types. Use typed variable keywords:
   ```robot
   ${amount}=    Create Integer Variable    1000
   ${result}=    Evaluate Decision    discount    orderTotal=${amount}
   ```
3. **Verify input names** — The keyword argument names must exactly match the `<inputExpression>` text in the DMN file.

---

## Build Issues

### Maven build fails with "Compilation failure"

**Symptom:** `mvn package` fails during compilation.

**Cause:** Wrong JDK version.

**Solution:** Ensure JDK 17+ is installed and active:

```bash
java -version
# Should show 17 or higher
```

### JaCoCo coverage report is empty

**Symptom:** The coverage report shows 0% or is missing.

**Cause:** The JaCoCo agent was not attached during test execution.

**Solution:** Ensure JaCoCo is configured in the parent POM (it should be by default):

```bash
mvn verify
# Reports are in target/site/jacoco/
```

### "google-java-format: command not found"

**Symptom:** `make format` fails.

**Cause:** The `google-java-format` tool is not installed.

**Solution:** Install via your package manager or download directly:

```bash
# Using the devenv/nix setup (recommended)
devenv shell

# Or download manually
VERSION=1.25.2
curl -fsSL "https://github.com/google/google-java-format/releases/download/v${VERSION}/google-java-format-${VERSION}-all-deps.jar" \
  -o google-java-format.jar
alias google-java-format="java -jar $(pwd)/google-java-format.jar"
```

---

## Performance Tips

1. **Use GraalVM JDK 21** — Provides the best GraalPy performance via the Graal JIT compiler.
2. **Cache Maven dependencies** — The GraalPy dependencies are large (~200 MB). Always cache `~/.m2/repository` in CI.
3. **Minimize engine creation** — Each `PythonScriptEngine` creates a new GraalPy context. Reuse engines where possible.
4. **Use `Compilable`** — For scripts executed repeatedly, use `PythonCompiledScript` for faster re-evaluation.
5. **Profile with GraalVM tools** — Use `--engine.TraceCompilation` for JIT compilation insights.
