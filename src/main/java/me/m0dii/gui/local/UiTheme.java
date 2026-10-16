package me.m0dii.gui.local;

import me.m0dii.gui.theme.Theme;

/** Legacy aliases kept for older screens; new code should use {@link Theme} directly. */
public final class UiTheme {
    public static final int BG = Theme.SCRIM;
    public static final int PANEL = Theme.SURFACE;
    public static final int PANEL_BORDER = Theme.BORDER;
    public static final int TEXT = Theme.TEXT;
    public static final int TEXT_MUTED = Theme.TEXT_MUTED;
    public static final int TEXT_ACCENT = Theme.ACCENT_TEXT;

    private UiTheme() {
    }
}

