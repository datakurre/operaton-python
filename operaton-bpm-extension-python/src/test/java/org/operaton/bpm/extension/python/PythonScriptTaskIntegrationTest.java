package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * Integration tests for Python script task execution in Operaton.
 *
 * <p>Deploys BPMN processes with Python script tasks and verifies that the GraalPy ScriptEngine
 * evaluates them correctly within the Operaton process engine.
 */
class PythonScriptTaskIntegrationTest {

  @RegisterExtension
  static ProcessEngineExtension processEngine = ProcessEngineExtension.builder().build();

  @Test
  @Deployment(resources = "bpmn/python-script-task.bpmn")
  void scriptTaskSetsProcessVariable() {
    RuntimeService runtimeService = processEngine.getRuntimeService();
    HistoryService historyService = processEngine.getHistoryService();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("python-script-task-test");

    // Process completes synchronously, so query history for the variable
    HistoricVariableInstance variable =
        historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(instance.getId())
            .variableName("result")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue().toString()).isEqualTo("Hello from Python");
  }

  @Test
  @Deployment(resources = "bpmn/python-script-task-with-input.bpmn")
  void scriptTaskAccessesInputVariable() {
    RuntimeService runtimeService = processEngine.getRuntimeService();
    HistoryService historyService = processEngine.getHistoryService();

    ProcessInstance instance =
        runtimeService
            .createProcessInstanceByKey("python-script-task-input-test")
            .setVariable("inputValue", "World")
            .execute();

    HistoricVariableInstance variable =
        historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(instance.getId())
            .variableName("greeting")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue().toString()).isEqualTo("Hello World");
  }
}
