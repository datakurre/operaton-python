package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * Thread safety tests for the Python ScriptEngine.
 *
 * <p>Verifies that concurrent script executions on separate engine instances do not interfere with
 * each other.
 */
class PythonScriptEngineThreadSafetyTest {

  @Test
  void concurrentExecutionsDoNotInterfere() throws Exception {
    PythonScriptEngineFactory factory = new PythonScriptEngineFactory();
    int threadCount = 4;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      futures.add(
          executor.submit(
              () -> {
                PythonScriptEngine engine = (PythonScriptEngine) factory.getScriptEngine();
                try {
                  latch.countDown();
                  latch.await(10, TimeUnit.SECONDS);

                  engine.put("thread_id", index);
                  // Small sleep to increase chance of interleaving
                  Thread.sleep(10);
                  Object result = engine.eval("f'thread-{thread_id}'");
                  return result.toString();
                } finally {
                  engine.close();
                }
              }));
    }

    executor.shutdown();
    assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

    for (int i = 0; i < threadCount; i++) {
      assertThat(futures.get(i).get()).isEqualTo("thread-" + i);
    }
  }
}
