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
 * Integration tests for the Spin {@code S()} function used from Python script tasks.
 *
 * <p>Verifies that the {@code spin.py} script environment correctly makes the Spin {@code S()}
 * function available to Python scripts, enabling JSON/XML creation and manipulation.
 */
class PythonSpinIntegrationTest {

  @RegisterExtension
  static ProcessEngineExtension processEngine =
      ProcessEngineExtension.builder().configurationResource("operaton-spin.cfg.xml").build();

  @Test
  @Deployment(resources = "bpmn/python-spin-script-task.bpmn")
  void spinSFunctionCreatesJsonObject() {
    RuntimeService runtimeService = processEngine.getRuntimeService();
    HistoryService historyService = processEngine.getHistoryService();

    ProcessInstance instance =
        runtimeService
            .createProcessInstanceByKey("python-spin-test")
            .setVariable("name", "Operaton")
            .execute();

    HistoricVariableInstance variable =
        historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(instance.getId())
            .variableName("data")
            .singleResult();
    assertThat(variable).isNotNull();
    // Spin wraps the JSON — the serialized value should contain the greeting
    assertThat(variable.getValue().toString()).contains("Hello Operaton");
  }
}
