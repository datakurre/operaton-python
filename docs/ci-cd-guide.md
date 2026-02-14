# Running Robot Framework Suites from CI/CD Pipelines

This guide explains how to integrate Operaton Robot Framework test suites into continuous integration and delivery pipelines.

---

## Overview

The `operaton-bpm-extension-robot` module provides a self-contained Robot Framework runner that bundles:

- GraalPy (Python runtime on the JVM)
- Robot Framework 7.1.1
- The `ProcessEngine` keyword library for Operaton BPM/DMN testing

No Python installation is required on the CI server — everything runs on the JVM.

---

## Prerequisites

- **JDK 17+** (GraalVM JDK 21 recommended for best performance)
- **Apache Maven 3.9+**
- Your `.robot` test suites and associated `.bpmn`/`.dmn` resources

---

## Project Setup

### Maven Dependency

Add the Robot extension to your test module:

```xml
<dependency>
  <groupId>org.operaton.bpm.extension.robot</groupId>
  <artifactId>operaton-bpm-extension-robot</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

### Test Structure

Organize your test suites alongside the resources they test:

```
src/test/resources/
├── robot/
│   ├── ProcessTest.robot
│   ├── DecisionTest.robot
│   ├── my-process.bpmn
│   └── my-decision.dmn
```

In your `.robot` files, reference resources relative to the test file:

```robot
*** Settings ***
Library    ProcessEngine

*** Test Cases ***
My Test
    [Setup]    Setup Process Engine
    [Teardown]    Teardown Process Engine
    Deploy Resources    ${CURDIR}${/}my-process.bpmn
    ${instance}=    Start Instance    my-process
    Should Be Ended    ${instance}
```

---

## Running from Maven

### Using maven-exec-plugin

Add the exec plugin to run Robot suites during the `integration-test` phase:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <id>robot-tests</id>
      <phase>integration-test</phase>
      <goals>
        <goal>java</goal>
      </goals>
      <configuration>
        <mainClass>org.operaton.bpm.extension.robot.Robot</mainClass>
        <arguments>
          <argument>--outputdir</argument>
          <argument>${project.build.directory}/robot-reports</argument>
          <argument>src/test/resources/robot/</argument>
        </arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Then run:

```bash
mvn verify
```

### Using the JAR directly

Build the Robot runner JAR and execute it:

```bash
mvn package -DskipTests
java -jar operaton-bpm-extension-robot/target/operaton-bpm-extension-robot-*.jar \
  --outputdir target/robot-reports \
  src/test/resources/robot/
```

---

## GitHub Actions

```yaml
name: Robot Framework Tests

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  robot-tests:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: graalvm

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Build project
        run: mvn package -DskipTests --batch-mode --no-transfer-progress

      - name: Run Robot Framework suites
        run: mvn verify --batch-mode --no-transfer-progress

      - name: Upload Robot reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: robot-reports
          path: |
            **/target/robot-reports/
            **/output.xml
            **/log.html
            **/report.html
          retention-days: 14
```

---

## GitLab CI

```yaml
robot-tests:
  image: ghcr.io/graalvm/jdk-community:21
  stage: test
  script:
    - mvn verify --batch-mode --no-transfer-progress
  artifacts:
    when: always
    paths:
      - "**/target/robot-reports/"
      - "**/output.xml"
      - "**/log.html"
      - "**/report.html"
    expire_in: 14 days
  cache:
    key: maven
    paths:
      - .m2/repository
  variables:
    MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
```

---

## Jenkins

```groovy
pipeline {
    agent any

    tools {
        jdk 'graalvm-21'
        maven 'maven-3.9'
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn package -DskipTests --batch-mode'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn verify --batch-mode'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/robot-reports/**', allowEmptyArchive: true
            junit '**/target/surefire-reports/*.xml'
        }
    }
}
```

---

## Robot Framework CLI Options

The `Robot` runner accepts all standard Robot Framework CLI arguments:

| Option | Description |
|---|---|
| `--outputdir DIR` | Directory for output files (default: current directory) |
| `--include TAG` | Run only tests with the given tag |
| `--exclude TAG` | Skip tests with the given tag |
| `--suite NAME` | Run only the named suite |
| `--test NAME` | Run only the named test |
| `--loglevel LEVEL` | Set log level: `TRACE`, `DEBUG`, `INFO`, `WARN` |
| `--variable NAME:VALUE` | Set a global variable |
| `--dryrun` | Validate tests without executing them |

Example with tags:

```bash
java -jar operaton-bpm-extension-robot.jar \
  --include smoke \
  --outputdir target/robot-reports \
  src/test/resources/robot/
```

---

## Handling Test Results

### Exit Codes

Robot Framework returns these exit codes:

| Code | Meaning |
|---|---|
| 0 | All tests passed |
| 1 | One or more tests failed |
| 252 | No tests found |
| 253 | Invalid test data or configuration |

CI pipelines will automatically detect failures via the non-zero exit code.

### Report Artifacts

Robot Framework generates three output files:

- **output.xml** — Machine-readable test results (for further processing)
- **log.html** — Detailed execution log with keyword-level details
- **report.html** — Summary report with pass/fail statistics

Upload these as CI artifacts for post-run analysis.

---

## Best Practices

1. **Use tags for test categorization** — Tag tests as `smoke`, `regression`, `dmn`, etc., to run subsets in different pipeline stages.
2. **Set explicit output directories** — Always use `--outputdir` to keep reports in a predictable location.
3. **Run in parallel where possible** — Each Robot test creates its own in-memory process engine, so tests are naturally isolated.
4. **Cache Maven dependencies** — GraalPy and its dependencies are large; caching `.m2/repository` significantly speeds up builds.
5. **Use GraalVM JDK** — GraalPy runs best on GraalVM. Use the `graalvm` distribution in `setup-java`.
6. **Upload reports always** — Use `if: always()` (GitHub Actions) or `when: always` (GitLab) to capture reports even on failure.
