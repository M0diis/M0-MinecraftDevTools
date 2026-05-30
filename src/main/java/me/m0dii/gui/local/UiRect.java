package me.m0dii.gui.local;

public record UiRect(int x, int y, int width, int height) {
    public UiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("width/height must be >= 0");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public UiRect inset(int inset) {
        return inset(inset, inset, inset, inset);
    }

    public UiRect inset(int left, int top, int right, int bottom) {
        int nx = x + left;
        int ny = y + top;
        int nw = Math.max(0, width - left - right);
        int nh = Math.max(0, height - top - bottom);
        return new UiRect(nx, ny, nw, nh);
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}

