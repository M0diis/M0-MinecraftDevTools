package me.m0dii.modules.macros.hud;

public final class HudElementUtils {
    private HudElementUtils() {
    }

    public static MacroHudDataHandler.HudElement cloneElement(MacroHudDataHandler.HudElement element) {
        MacroHudDataHandler.HudElement cloned = new MacroHudDataHandler.HudElement();
        cloned.id = element.id;
        cloned.type = element.type;
        cloned.label = element.label;
        cloned.text = element.text;
        cloned.macroId = element.macroId;
        cloned.buttonAction = element.buttonAction;
        cloned.buttonExecutionMode = element.buttonExecutionMode;
        cloned.x = element.x;
        cloned.y = element.y;
        cloned.anchor = element.anchor;
        cloned.width = element.width;
        cloned.height = element.height;
        cloned.lineHeight = element.lineHeight;
        cloned.fontScale = element.fontScale;
        cloned.backgroundColor = element.backgroundColor;
        cloned.borderColor = element.borderColor;
        cloned.textColor = element.textColor;
        cloned.drawBackground = element.drawBackground;
        cloned.drawBorder = element.drawBorder;
        cloned.horizontalAlign = element.horizontalAlign;
        cloned.verticalAlign = element.verticalAlign;
        cloned.visibilityMode = element.visibilityMode;
        cloned.visibilityScreenType = element.visibilityScreenType;
        cloned.visible = element.visible;
        cloned.sourceToken = element.sourceToken;
        cloned.sourceTokenMax = element.sourceTokenMax;
        cloned.prefix = element.prefix;
        cloned.suffix = element.suffix;
        cloned.minValue = element.minValue;
        cloned.maxValue = element.maxValue;
        cloned.colorStart = element.colorStart;
        cloned.colorEnd = element.colorEnd;
        cloned.colorWarn = element.colorWarn;
        cloned.colorCrit = element.colorCrit;
        cloned.warnThreshold = element.warnThreshold;
        cloned.critThreshold = element.critThreshold;
        cloned.segmented = element.segmented;
        cloned.segments = element.segments;
        cloned.maxLines = element.maxLines;
        cloned.listScroll = element.listScroll;
        cloned.iconKind = element.iconKind;
        cloned.iconId = element.iconId;
        cloned.iconShowCount = element.iconShowCount;
        cloned.iconShowDurability = element.iconShowDurability;
        cloned.iconShowCooldown = element.iconShowCooldown;
        cloned.modelZoom = element.modelZoom;
        cloned.modelYaw = element.modelYaw;
        cloned.modelPitch = element.modelPitch;
        cloned.modelOffsetX = element.modelOffsetX;
        cloned.modelOffsetY = element.modelOffsetY;
        cloned.modelAutoFit = element.modelAutoFit;
        cloned.modelFollowLook = element.modelFollowLook;
        cloned.shapeType = element.shapeType;
        cloned.shapeFilled = element.shapeFilled;
        cloned.shapeRadius = element.shapeRadius;
        cloned.shapeThickness = element.shapeThickness;
        cloned.stateOnText = element.stateOnText;
        cloned.stateOffText = element.stateOffText;
        cloned.stateShowValue = element.stateShowValue;
        return cloned;
    }
}
