package org.operaton.bpm.extension.robotframework;

import org.operaton.bpm.extension.python.PythonScriptEngineFactory;

import javax.script.ScriptEngine;
import java.util.List;

public class RobotFrameworkScriptEngineFactory extends PythonScriptEngineFactory {

    public RobotFrameworkScriptEngineFactory() {
        super();
    }
    @Override
    public String getEngineName() {
        return "RobotFramework";
    }

    @Override
    public List<String> getExtensions() {
        return List.of("robot");
    }

    @Override
    public List<String> getMimeTypes() {
        return List.of("text/x-robotframework");
    }

    @Override
    public List<String> getNames() {
        return List.of("robot", "robotframework");
    }

    @Override
    public String getLanguageName() {
        return "Robot Framework";
    }

    @Override
    public String getLanguageVersion() {
        return "7.1.1";
    }

    @Override
    public Object getParameter(final String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.NAME:
                return "robotframework";
        }
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new RobotFrameworkScriptEngine(this);
    }
}
