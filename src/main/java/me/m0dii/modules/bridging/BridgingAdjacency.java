package me.m0dii.modules.bridging;

public enum BridgingAdjacency {
    DISABLED(false, false),
    FACES(true, false),
    EDGES(true, true),
    CORNERS(true, true);

    private final boolean supportsFaces;
    private final boolean supportsEdges;

    BridgingAdjacency(boolean supportsFaces, boolean supportsEdges) {
        this.supportsFaces = supportsFaces;
        this.supportsEdges = supportsEdges;
    }

    public boolean supportsFaces() {
        return this.supportsFaces;
    }

    public boolean supportsEdges() {
        return this.supportsEdges;
    }

    public BridgingAdjacency next() {
        BridgingAdjacency[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
