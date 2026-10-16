package me.m0dii.gui.theme;

/**
 * Design tokens for the mod's flat, dark UI. Colors are ARGB.
 */
public final class Theme {
    // Surfaces, darkest to lightest.
    public static final int SCRIM = 0xE60B0D11;
    public static final int SIDEBAR = 0xFF111419;
    public static final int SURFACE = 0xFF171B22;
    public static final int SURFACE_RAISED = 0xFF1E232C;
    public static final int SURFACE_HOVER = 0xFF262C37;
    public static final int SURFACE_PRESSED = 0xFF2E3542;
    public static final int INPUT = 0xFF0F1216;

    public static final int BORDER = 0xFF2A303B;
    public static final int BORDER_STRONG = 0xFF3A4250;
    public static final int DIVIDER = 0xFF232832;

    public static final int TEXT = 0xFFE7EAF0;
    public static final int TEXT_MUTED = 0xFF98A1B3;
    public static final int TEXT_DIM = 0xFF5F6879;

    public static final int ACCENT = 0xFF4C8BF5;
    public static final int ACCENT_HOVER = 0xFF6A9FF7;
    public static final int ACCENT_PRESSED = 0xFF3B76DB;
    public static final int ACCENT_SOFT = 0x334C8BF5;
    public static final int ACCENT_TEXT = 0xFF9CC1FF;

    public static final int DANGER = 0xFFE5534B;
    public static final int DANGER_SOFT = 0x33E5534B;
    public static final int SUCCESS = 0xFF3FB950;
    public static final int WARNING = 0xFFD29922;

    public static final int SWITCH_OFF = 0xFF3A4250;
    public static final int KNOB = 0xFFF2F4F8;

    // Spacing scale (GUI pixels).
    public static final int GAP_XS = 2;
    public static final int GAP_S = 4;
    public static final int GAP_M = 8;
    public static final int GAP_L = 12;
    public static final int GAP_XL = 16;

    public static final int CONTROL_H = 18;
    public static final int ROW_H = 20;

    private Theme() {
    }
}
