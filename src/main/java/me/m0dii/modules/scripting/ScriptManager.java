package me.m0dii.modules.scripting;

import java.util.Map;

public interface ScriptManager {
    Object runScript(String script, Map<String, Object> context);
}

