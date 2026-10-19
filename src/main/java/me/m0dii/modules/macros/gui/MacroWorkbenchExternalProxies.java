package me.m0dii.modules.macros.gui;

import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;

/**
 * Canvas proxies for HUD elements owned by other modules (secondary chat, NBT inspector, macro keybinds, pickup notifier):
 * their ids, default layouts and the conversion between module settings and canvas elements.
 */
public final class MacroWorkbenchExternalProxies {
    private MacroWorkbenchExternalProxies() {
    }

    public static final String EXTERNAL_NBT_INSPECTOR_ID = "__ext_nbt_inspector";

    public static final String EXTERNAL_SECONDARY_CHAT_ID = "__ext_secondary_chat";

    public static final String EXTERNAL_MACRO_KEYBINDS_ID = "__ext_macro_keybinds";

    public static final String EXTERNAL_PICKUP_NOTIFIER_ID = "__ext_pickup_notifier";

    public enum ExternalProxyRenderState {
        ACTIVE,
        MODULE_DISABLED
    }

    public static boolean isSecondaryChatProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_SECONDARY_CHAT_ID.equals(element.id);
    }

    public static boolean isNbtInspectorProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_NBT_INSPECTOR_ID.equals(element.id);
    }

    public static boolean isMacroKeybindProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_MACRO_KEYBINDS_ID.equals(element.id);
    }

    public static boolean isPickupNotifierProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_PICKUP_NOTIFIER_ID.equals(element.id);
    }

    public static HudCanvasDataHandler.HudCanvasElement defaultSecondaryChatCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 40;
        e.width = 260;
        e.height = 120;
        e.fontScale = 0.85f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
    }

    public static HudCanvasDataHandler.HudCanvasElement defaultNbtInspectorCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 8;
        e.width = 260;
        e.height = 120;
        e.fontScale = 1.0f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_LEFT;
        return e;
    }

    public static HudCanvasDataHandler.HudCanvasElement defaultMacroKeybindsCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 12;
        e.y = 12;
        e.width = 240;
        e.height = 120;
        e.fontScale = 0.85f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_RIGHT;
        return e;
    }

    public static HudCanvasDataHandler.HudCanvasElement defaultPickupNotifierCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 12;
        e.y = 12;
        e.width = 220;
        e.height = 110;
        e.fontScale = 1.0f;
        e.lineHeight = 18;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFFFFFFF;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_RIGHT;
        e.horizontalAlign = HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        e.verticalAlign = HudCanvasDataHandler.HudCanvasElement.VerticalAlign.BOTTOM;
        return e;
    }

    public static void applyExternalCanvasToProxy(HudCanvasDataHandler.HudCanvasElement external, MacroHudDataHandler.HudElement proxy) {
        proxy.visible = external.visible;
        proxy.x = external.x;
        proxy.y = external.y;
        proxy.width = external.width;
        proxy.height = external.height;
        proxy.lineHeight = external.lineHeight;
        proxy.fontScale = external.fontScale;
        proxy.zIndex = external.zIndex;
        proxy.backgroundColor = external.backgroundColor;
        proxy.textColor = external.textColor;
        proxy.borderColor = external.borderColor;
        proxy.drawBackground = external.drawBackground;
        proxy.drawBorder = external.drawBorder;
        proxy.anchor = switch (external.anchor) {
            case TOP_CENTER -> MacroHudDataHandler.Anchor.TOP_CENTER;
            case TOP_RIGHT -> MacroHudDataHandler.Anchor.TOP_RIGHT;
            case MIDDLE_LEFT -> MacroHudDataHandler.Anchor.MIDDLE_LEFT;
            case MIDDLE_CENTER -> MacroHudDataHandler.Anchor.MIDDLE_CENTER;
            case MIDDLE_RIGHT -> MacroHudDataHandler.Anchor.MIDDLE_RIGHT;
            case BOTTOM_LEFT -> MacroHudDataHandler.Anchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> MacroHudDataHandler.Anchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> MacroHudDataHandler.Anchor.BOTTOM_RIGHT;
            default -> MacroHudDataHandler.Anchor.TOP_LEFT;
        };
        proxy.horizontalAlign = switch (external.horizontalAlign) {
            case CENTER -> MacroHudDataHandler.HorizontalAlign.CENTER;
            case RIGHT -> MacroHudDataHandler.HorizontalAlign.RIGHT;
            default -> MacroHudDataHandler.HorizontalAlign.LEFT;
        };
        proxy.verticalAlign = switch (external.verticalAlign) {
            case CENTER -> MacroHudDataHandler.VerticalAlign.CENTER;
            case BOTTOM -> MacroHudDataHandler.VerticalAlign.BOTTOM;
            default -> MacroHudDataHandler.VerticalAlign.TOP;
        };
    }

    public static void applyProxyToExternalCanvas(MacroHudDataHandler.HudElement proxy, HudCanvasDataHandler.HudCanvasElement external) {
        external.x = proxy.x;
        external.y = proxy.y;
        external.width = proxy.width;
        external.height = proxy.height;
        external.lineHeight = proxy.lineHeight;
        external.fontScale = proxy.fontScale;
        external.zIndex = proxy.zIndex;
        external.backgroundColor = proxy.backgroundColor;
        external.textColor = proxy.textColor;
        external.borderColor = proxy.borderColor;
        external.drawBackground = proxy.drawBackground;
        external.drawBorder = proxy.drawBorder;
        external.visible = proxy.visible;
        external.anchor = switch (proxy.anchor) {
            case TOP_CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_CENTER;
            case TOP_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_RIGHT;
            case MIDDLE_LEFT -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_LEFT;
            case MIDDLE_CENTER, CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_CENTER;
            case MIDDLE_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_RIGHT;
            case BOTTOM_LEFT -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_RIGHT;
            default -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_LEFT;
        };
        external.horizontalAlign = switch (proxy.horizontalAlign) {
            case CENTER -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.CENTER;
            case RIGHT -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.RIGHT;
            default -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        };
        external.verticalAlign = switch (proxy.verticalAlign) {
            case CENTER -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.CENTER;
            case BOTTOM -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.BOTTOM;
            default -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.TOP;
        };
    }
}
