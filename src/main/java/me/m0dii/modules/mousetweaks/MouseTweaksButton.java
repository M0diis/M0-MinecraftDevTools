package me.m0dii.modules.mousetweaks;

import lombok.Getter;

@Getter
public enum MouseTweaksButton {
    LEFT(0),
    RIGHT(1);

    private final int id;

    MouseTweaksButton(int id) {
        this.id = id;
    }

    public static MouseTweaksButton fromButton(int button) {
        return switch (button) {
            case 0 -> LEFT;
            case 1 -> RIGHT;
            default -> null;
        };
    }
}
