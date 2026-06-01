package me.m0dii.modules.macros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class MacroPlaceholderCatalog {
    public static final String DEFAULT_BAR_SOURCE = "hp";
    public static final String DEFAULT_BAR_MAX_SOURCE = "max_hp";
    public static final String DEFAULT_LIST_SOURCE = "players.nearby.5.with_distance";
    public static final String DEFAULT_STATE_SOURCE = "player.sprinting";
    public static final String DEFAULT_ICON_KIND = "item";
    public static final String DEFAULT_ICON_ID = "minecraft:stone";
    public static final String DEFAULT_ENTITY_MODEL_ICON_ID = "minecraft:player";
    public static final String DEFAULT_SHAPE_TYPE = "rounded_rect";

    public static final String[] BAR_VALUE_SOURCE_PRESETS = {
            "hp", "max_hp", "food", "saturation", "xp", "level", "client.fps", "players.count", "players.nearby.count"
    };

    public static final String[] BAR_MAX_SOURCE_PRESETS = {"", "max_hp", "food", "players.count"};

    public static final String[] LIST_SOURCE_PRESETS = {
            "players.nearby.5.with_distance",
            "players.nearby.8.with_distance.with_direction.nl",
            "players.nearby.8.with_distance.with_direction_arrow.nl",
            "players.nearby.16.r96.with_distance.sort=distance.nl",
            "players.list.other",
            "players.list.other.nl",
            "entities.nearby.6.with_distance",
            "entities.nearby.10.with_distance.with_direction_arrow.nl",
            "entities.nearby.20.r96.unique.with_distance.with_direction.sort=name",
            "players.nearby.10.nl"
    };

    public static final String[] STATE_SOURCE_PRESETS = {
            "player.sprinting", "player.sneaking", "player.swimming", "player.on_ground", "world.is_day", "client.server.singleplayer"
    };

    public static final String[] ICON_KIND_PRESETS = {"item", "block", "entity", "entity_model"};
    public static final String[] SHAPE_TYPE_PRESETS = {"rounded_rect", "rect", "circle", "line", "triangle", "cross", "diamond"};

    private static final List<String> SUPPLEMENTAL_PLACEHOLDER_TOKENS = buildSupplementalPlaceholderTokens();

    private MacroPlaceholderCatalog() {
    }

    public static List<String> supplementalPlaceholderTokens() {
        return SUPPLEMENTAL_PLACEHOLDER_TOKENS;
    }

    private static List<String> buildSupplementalPlaceholderTokens() {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        tokens.add("armor.helmet.item");
        tokens.add("armor.helmet.id");
        tokens.add("armor.helmet.count");
        tokens.add("armor.helmet.damage");
        tokens.add("armor.helmet.max_damage");
        tokens.add("armor.helmet.durability");
        tokens.add("armor.chestplate.item");
        tokens.add("armor.chestplate.id");
        tokens.add("armor.chestplate.count");
        tokens.add("armor.chestplate.damage");
        tokens.add("armor.chestplate.max_damage");
        tokens.add("armor.chestplate.durability");
        tokens.add("armor.leggings.item");
        tokens.add("armor.leggings.id");
        tokens.add("armor.leggings.count");
        tokens.add("armor.leggings.damage");
        tokens.add("armor.leggings.max_damage");
        tokens.add("armor.leggings.durability");
        tokens.add("armor.boots.item");
        tokens.add("armor.boots.id");
        tokens.add("armor.boots.count");
        tokens.add("armor.boots.damage");
        tokens.add("armor.boots.max_damage");
        tokens.add("armor.boots.durability");

        tokens.add("inventory.count:<item>");
        tokens.add("container.count:<item>");
        tokens.add("rand.int(<min>,<max>)");
        tokens.add("rand.int(1,10)");

        tokens.add("key.pressed.<key>");
        tokens.add("key.held.<key>");
        tokens.add("key.pressed.w");
        tokens.add("key.held.space");

        tokens.add("players.count");
        tokens.add("players.count.other");
        tokens.add("players.nearby.count");
        tokens.add("players.list");
        tokens.add("players.list.csv");
        tokens.add("players.list.other");
        tokens.add("players.list.other.csv");
        tokens.add("players.list.nl");
        tokens.add("players.list.other.nl");
        tokens.add("players.nearby.<limit>");
        tokens.add("players.nearby.<limit>.nl");
        tokens.add("players.nearby.<limit>.r<radius>");
        tokens.add("players.nearby.<limit>.r<radius>.nl");
        tokens.add("players.nearby.<limit>.with_distance");
        tokens.add("players.nearby.<limit>.with_direction");
        tokens.add("players.nearby.<limit>.with_direction_arrow");
        tokens.add("players.nearby.<limit>.unique");
        tokens.add("players.nearby.<limit>.sort=name");
        tokens.add("players.nearby.<limit>.sort=distance");

        tokens.add("entities.nearby.count");
        tokens.add("entities.nearby.<limit>");
        tokens.add("entities.nearby.<limit>.nl");
        tokens.add("entities.nearby.<limit>.r<radius>");
        tokens.add("entities.nearby.<limit>.r<radius>.nl");
        tokens.add("entities.nearby.<limit>.with_distance");
        tokens.add("entities.nearby.<limit>.with_direction");
        tokens.add("entities.nearby.<limit>.with_direction_arrow");
        tokens.add("entities.nearby.<limit>.unique");
        tokens.add("entities.nearby.<limit>.sort=name");
        tokens.add("entities.nearby.<limit>.sort=distance");

        tokens.add("player.name|lower");
        tokens.add("pos.biome|basename|title");

        tokens.addAll(Arrays.asList(LIST_SOURCE_PRESETS));
        return List.copyOf(new ArrayList<>(tokens));
    }
}
