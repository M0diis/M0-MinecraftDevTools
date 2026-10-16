package me.m0dii.modules.mousetweaks;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;

@Getter
public enum MouseTweaksButton {
    // Container-click protocol button ids (unchanged by the 26.x input renumbering).
    LEFT(0),
    RIGHT(1);

    private final int id;

    MouseTweaksButton(int id) {
        this.id = id;
    }

    public static MouseTweaksButton fromButton(int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> LEFT;
            case InputConstants.MOUSE_BUTTON_RIGHT -> RIGHT;
            default -> null;
        };
    }
}
