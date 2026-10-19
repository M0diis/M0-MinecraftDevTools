package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.UiRect;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Small value types used by the workbench screen: canvas and preview modes, toolbar entries, keyboard layout cells
 * and move snapshots.
 */
public final class MacroWorkbenchModels {
    private MacroWorkbenchModels() {
    }

    public enum CanvasMode {
        CUSTOM,
        VANILLA
    }

    public enum VanillaPreviewMode {
        VIRTUAL,
        ACTUAL
    }

    public record KeyCell(int keyCode, String label, int x, int y, int w, int h) {
    }

    public record ElementSnapshot(String id, int x, int y, int width, int height) {
    }

    public enum ModalDragSelectionField {
        NONE,
        ADVANCED_TEXT,
        ADVANCED_ACTION,
        ADVANCED_BG,
        ADVANCED_BORDER,
        KB_COMMANDS
    }

    public record ToolbarLabel(String text, int x, int y, int width) {
    }

    public record ToolbarItem(AbstractWidget widget, int width, String label) {
        static ToolbarItem of(AbstractWidget widget, int width) {
            return new ToolbarItem(widget, width, null);
        }

        static ToolbarItem text(String label, int width) {
            return new ToolbarItem(null, width, label);
        }
    }

    /** Geometry of the keyboard tab: keyboard area plus an inspector card (right when wide, below when narrow). */
    public record KbLayout(boolean wide, int kbX, int kbY, int kbW, int kbH,
                           int cardX, int cardY, int cardW, int cardH, int listTop, int listBottom,
                           UiRect game, UiRect name, UiRect commands, UiRect edit, UiRect delay,
                           UiRect save, UiRect add, UiRect delete) {
    }
}
