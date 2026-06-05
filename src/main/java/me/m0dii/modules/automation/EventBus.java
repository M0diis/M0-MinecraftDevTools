package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
    public interface Listener {
        void onAutomationEvent(@NotNull AutomationEvent event);
    }

    private static final EventBus INSTANCE = new EventBus();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void register(@NotNull Listener listener) {
        listeners.add(listener);
    }

    public void unregister(@NotNull Listener listener) {
        listeners.remove(listener);
    }

    public void post(@NotNull AutomationEvent event) {
        for (Listener listener : listeners) {
            listener.onAutomationEvent(event);
        }
    }
}
