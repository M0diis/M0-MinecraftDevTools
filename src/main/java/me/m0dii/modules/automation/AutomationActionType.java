package me.m0dii.modules.automation;

public enum AutomationActionType {
    RUN_MACRO,
    RUN_SCRIPT,
    SEND_CLIENT_COMMAND,
    TOGGLE_MODULE,
    /** Shows text only to you (chat or action bar), optionally with a ping sound. */
    SHOW_MESSAGE
}
