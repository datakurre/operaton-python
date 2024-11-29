package org.operaton.bpm.extension.robotframework;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.operaton.bpm.extension.python.GraalPyBindings;
import org.operaton.bpm.extension.python.GraalPyCompiledScript;
import org.operaton.bpm.extension.python.PythonScriptEngine;
import org.operaton.bpm.extension.python.PythonScriptEngineFactory;

import javax.script.*;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public final class RobotFrameworkScriptEngine extends PythonScriptEngine {

    private CompiledScript runner;

    public RobotFrameworkScriptEngine(PythonScriptEngineFactory factory) {
        super(factory);
        try {
            runner = super.compile("""
            from io import StringIO
            from robot.api import TestSuite
            from robot.api.parsing import get_model
            model = get_model(StringIO(locals()[".robot"]))
            suite = TestSuite.from_model(model)
            suite.run(output="NONE", log="NONE", report="NONE")
            "Hello World"  # TODO ... listen to suite variables
            """);
        } catch (ScriptException e) {
            runner = null;
        }
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        put(".robot", script);
        return runner;
    }

    @Override
    public Bindings createBindings() {
        GraalPyBindings bindings = (GraalPyBindings) defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
        Set<String> keep = new HashSet<>();
        keep.add(".robot");
        bindings.clear(keep);
        return new SimpleBindings();
    }

    @Override
    public CompiledScript compile(Reader script) throws ScriptException {
        throw new ScriptException("Not implemented.");
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        throw new ScriptException("Not implemented.");
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        throw new ScriptException("Not implemented.");
    }

    @Override
    public Object eval(String script) throws ScriptException {
        throw new ScriptException("Not implemented.");
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        throw new ScriptException("Not implemented.");
    }
}
