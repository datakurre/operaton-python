package org.operaton.bpm.extension.python;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/**
 * Configuration utility for creating GraalPy {@link Context.Builder} instances.
 *
 * <p>Sets up the GraalPy context with the virtual filesystem (VFS) for bundled Python packages,
 * Python home, POSIX backend, bytecode settings, and other interpreter options.
 */
public class PythonResources {

  private PythonResources() {}

  /**
   * Creates a GraalPy {@link Context.Builder} pre-configured with the given virtual filesystem.
   *
   * <p>Uses {@link GraalPyResources#contextBuilder(VirtualFileSystem)} to set up VFS paths, Python
   * home, executable, and filesystem, then applies additional interpreter options.
   *
   * @param vfs the virtual filesystem containing bundled Python packages and resources, or {@code
   *     null} to use the default context builder without a VFS
   * @return a configured context builder ready for further customization and building
   */
  public static Context.Builder contextBuilder(VirtualFileSystem vfs) {
    Context.Builder builder =
        vfs != null ? GraalPyResources.contextBuilder(vfs) : Context.newBuilder("python");
    return builder
        .allowHostAccess(HostAccess.ALL)
        .allowCreateThread(true)
        .allowNativeAccess(true)
        .allowPolyglotAccess(PolyglotAccess.ALL)
        .option("python.PosixModuleBackend", "java")
        .option("python.DontWriteBytecodeFlag", "true")
        .option("python.VerboseFlag", System.getenv("PYTHONVERBOSE") != null ? "true" : "false")
        .option("log.python.level", System.getenv("PYTHONVERBOSE") != null ? "FINE" : "SEVERE")
        .option(
            "python.WarnOptions",
            System.getenv("PYTHONWARNINGS") == null ? "" : System.getenv("PYTHONWARNINGS"))
        .option("python.AlwaysRunExcepthook", "true")
        .option("python.ForceImportSite", "true")
        .option("python.CheckHashPycsMode", "never");
  }
}
