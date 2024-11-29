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
from robot.api.interfaces import ListenerV3
from robot import running
from robot import result
from robot.libraries import BuiltIn
from typing import Set

import json

# Update Output Variable
#     [Arguments]    ${name}    ${value}    ${scope}=global
#     VAR    ${${name}}    ${value}    scope=${scope}

WORK_ITEM = {}

OUTPUT_MAPPING = {}

if "variables" in locals():
    WORK_ITEM.update({
        json.loads(variables.toString())
    })


class InputVariablesListener(ListenerV3):
    initial_global_variables: Set[str]

    def start_suite(self, data: running.TestSuite, result: result.TestSuite):
        builtin = BuiltIn.BuiltIn()
        self.initial_global_variables = set(builtin.get_variables().keys())
        for k, v in WORK_ITEM.items():
            builtin.set_global_variable(f"${{{k}}}", v)

    def end_suite(self, data: running.TestSuite, result: result.TestSuite):
        builtin = BuiltIn.BuiltIn()
        all_variables = builtin.get_variables()
        if result.passed:
            for k in set(all_variables) - self.initial_global_variables:
                OUTPUT_MAPPING[k] = all_variables[k]


model = get_model(StringIO(locals()[".robot"]))
suite = TestSuite.from_model(model)
suite.run(output=None, listener=InputVariablesListener())


#S(json.dumps(OUTPUT_MAPPING), "application/json")
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
        // GraalPyBindings bindings = (GraalPyBindings) defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
        // Set<String> keep = new HashSet<>();
        // keep.add(".robot");
        // bindings.clear(keep);
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
