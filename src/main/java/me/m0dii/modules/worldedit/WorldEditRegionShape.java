package me.m0dii.modules.worldedit;

enum WorldEditRegionShape {
    BOX,
    CIRCLE,
    CYLINDER,
    SPHERE;

    static WorldEditRegionShape fromName(String name) {
        if (name == null || name.isBlank()) {
            return BOX;
        }
        try {
            return WorldEditRegionShape.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BOX;
        }
    }
}
