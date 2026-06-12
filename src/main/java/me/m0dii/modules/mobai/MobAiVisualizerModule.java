package me.m0dii.modules.mobai;

import me.m0dii.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class MobAiVisualizerModule extends Module {

    public static final MobAiVisualizerModule INSTANCE = new MobAiVisualizerModule();

    private final MobAiVisualizerRenderer renderer = new MobAiVisualizerRenderer();

    private boolean useDebugSubscriptions = true;
    private boolean useClientFallback = true;
    private boolean showPathLines = true;
    private boolean showPathNodes = true;
    private boolean showBrainTargets = true;
    private boolean showLabels = true;
    private int radius = 48;

    private MobAiVisualizerModule() {
        super("mob_ai_visualizer", "Mob AI Visualizer", false);
    }

    @Override
    public void register() {
        renderer.register();
    }

    public boolean useDebugSubscriptions() {
        return useDebugSubscriptions;
    }

    public boolean useClientFallback() {
        return useClientFallback;
    }

    boolean showPathLines() {
        return showPathLines;
    }

    boolean showPathNodes() {
        return showPathNodes;
    }

    boolean showBrainTargets() {
        return showBrainTargets;
    }

    boolean showLabels() {
        return showLabels;
    }

    int getRadius() {
        return radius;
    }

    public boolean shouldRequestServerDebugData() {
        return (isEnabled() && useDebugSubscriptions) || MobAiDebugClientState.requiresServerDebugSubscriptions();
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Server Debug Data: " + (useDebugSubscriptions ? "ON" : "OFF"));
        settings.add("Client Fallback: " + (useClientFallback ? "ON" : "OFF"));
        settings.add("Path Lines: " + (showPathLines ? "ON" : "OFF"));
        settings.add("Path Nodes: " + (showPathNodes ? "ON" : "OFF"));
        settings.add("Brain Targets: " + (showBrainTargets ? "ON" : "OFF"));
        settings.add("Labels: " + (showLabels ? "ON" : "OFF"));
        settings.add("Radius: " + radius);
        settings.add("Radius (+)");
        settings.add("Radius (-)");
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> useDebugSubscriptions = !useDebugSubscriptions;
            case 1 -> useClientFallback = !useClientFallback;
            case 2 -> showPathLines = !showPathLines;
            case 3 -> showPathNodes = !showPathNodes;
            case 4 -> showBrainTargets = !showBrainTargets;
            case 5 -> showLabels = !showLabels;
            case 7 -> radius = Math.min(256, radius + 8);
            case 8 -> radius = Math.max(8, radius - 8);
            case 9 -> toggleEnabled();
            default -> {
                // No-op
            }
        }
    }
}
