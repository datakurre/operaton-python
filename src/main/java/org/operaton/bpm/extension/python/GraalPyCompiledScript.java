package org.operaton.bpm.extension.python;

import org.graalvm.polyglot.Source;

import javax.script.*;

public final class GraalPyCompiledScript extends CompiledScript {
    private final Source source;
    private final GraalPyScriptEngine engine;

    public GraalPyCompiledScript(Source src, GraalPyScriptEngine engine) {
        this.source = src;
        this.engine = engine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        if (context instanceof SimpleScriptContext) {
            GraalPyContext ctx = new GraalPyContext(engine.getFactory());

            Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
            if (engineBindings != null) {
                ctx.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
            }

            Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
            if (globalBindings != null) {
                ctx.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
            }

            return this.engine.getFactory().getContext(globalBindings).eval(source).as(Object.class);
        }
        if (context instanceof GraalPyContext) {
            return ((GraalPyContext) context).getContext().eval(source).as(Object.class);
        }
        throw new UnsupportedOperationException(
                "Polyglot CompiledScript instances can only be evaluated in Polyglot.");
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }
}
