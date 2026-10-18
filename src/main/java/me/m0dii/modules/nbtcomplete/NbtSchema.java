package me.m0dii.modules.nbtcomplete;

import java.util.*;

/**
 * A small hand-written schema of the NBT keys you type most often in {@code /summon} and {@code /data}
 * (26.x names: {@code equipment}, {@code drop_chances}, {@code active_effects}, ...). It is a typing aid,
 * not a validator: unknown keys are still accepted by the game.
 */
public final class NbtSchema {
    /** Value kinds; {@code child} on a field names the compound used for COMPOUND and LIST_COMPOUND values. */
    public enum Kind {
        BOOL, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, TEXT, VEC3D, VEC2F, STRING_LIST, FLOAT_LIST,
        COMPOUND, LIST_COMPOUND, ANY,
        /** A namespaced id; {@code child} names the registry ("item", "mob_effect", "attribute", "entity_type", "block"). */
        ID
    }

    public record Field(String name, Kind kind, String child) {
    }

    private static final Map<String, Map<String, Field>> NODES = new HashMap<>();
    private static final Map<String, List<String>> EXTENDS = new HashMap<>();

    /** Entity types that are not living entities (no Health, effects, equipment...). */
    private static final Set<String> NON_LIVING = Set.of(
            "item", "experience_orb", "falling_block", "tnt", "arrow", "spectral_arrow", "trident", "snowball", "egg",
            "ender_pearl", "fireball", "small_fireball", "dragon_fireball", "wither_skull", "firework_rocket",
            "area_effect_cloud", "marker", "item_display", "block_display", "text_display", "interaction", "minecart",
            "chest_minecart", "hopper_minecart", "tnt_minecart", "furnace_minecart", "command_block_minecart",
            "spawner_minecart", "end_crystal", "painting", "item_frame", "glow_item_frame", "leash_knot",
            "lightning_bolt", "eye_of_ender", "evoker_fangs", "splash_potion", "lingering_potion", "experience_bottle",
            "llama_spit", "shulker_bullet", "wind_charge", "breeze_wind_charge", "fishing_bobber",
            "ominous_item_spawner", "oak_boat", "spruce_boat", "birch_boat", "jungle_boat", "acacia_boat",
            "cherry_boat", "dark_oak_boat", "pale_oak_boat", "mangrove_boat", "bamboo_raft", "oak_chest_boat");

    static {
        node("entity_base", null,
                "Air:short CustomName:text CustomNameVisible:bool Fire:short Glowing:bool Invulnerable:bool "
                        + "Motion:vec3d NoGravity:bool OnGround:bool Passengers:list(passenger) PortalCooldown:int "
                        + "Pos:vec3d Rotation:vec2f Silent:bool Tags:strings TicksFrozen:int fall_distance:double "
                        + "HasVisualFire:bool");
        node("living", "entity_base",
                "Health:float AbsorptionAmount:float active_effects:list(effect) attributes:list(attribute) "
                        + "equipment:compound(equipment) drop_chances:compound(drop_chances) DeathTime:short HurtTime:short "
                        + "FallFlying:bool LeftHanded:bool NoAI:bool PersistenceRequired:bool CanPickUpLoot:bool "
                        + "Team:string DeathLootTable:string Brain:any Age:int InLove:int Owner:any Sitting:bool "
                        + "IsBaby:bool Variant:any");
        node("passenger", "living", "id:id(entity_type)");
        node("effect", null,
                "id:id(mob_effect) amplifier:byte duration:int ambient:bool show_particles:bool show_icon:bool");
        node("attribute", null, "id:id(attribute) base:double modifiers:list(modifier)");
        node("modifier", null, "id:string amount:double operation:string");
        node("equipment", null,
                "head:compound(item) chest:compound(item) legs:compound(item) feet:compound(item) "
                        + "mainhand:compound(item) offhand:compound(item) body:compound(item) saddle:compound(item)");
        node("drop_chances", null, "head:float chest:float legs:float feet:float mainhand:float offhand:float body:float");
        node("item", null, "id:id(item) count:int components:any");
        node("slot_item", "item", "Slot:byte");
        node("block_state", null, "Name:id(block) Properties:any");
        node("transformation", null, "translation:float_list left_rotation:float_list scale:float_list right_rotation:float_list");

        // Entity types with extra keys.
        node("entity:zombie", "living", "CanBreakDoors:bool DrownedConversionTime:int");
        node("entity:villager", "living", "VillagerData:compound(villager_data) Offers:any Xp:int");
        node("villager_data", null, "profession:id(villager_profession) level:int type:id(villager_type)");
        node("entity:armor_stand", "living",
                "Invisible:bool Marker:bool NoBasePlate:bool ShowArms:bool Small:bool DisabledSlots:int Pose:compound(pose)");
        node("pose", null, "Head:float_list Body:float_list LeftArm:float_list RightArm:float_list LeftLeg:float_list RightLeg:float_list");
        node("entity:creeper", "living", "Fuse:short ExplosionRadius:byte powered:bool ignited:bool");
        node("entity:slime", "living", "Size:int");
        node("entity:sheep", "living", "Color:byte Sheared:bool");
        node("entity:wolf", "living", "CollarColor:byte angry_at:any");
        node("entity:item", "entity_base", "Item:compound(item) PickupDelay:short Age:short Owner:any Thrower:any");
        node("entity:experience_orb", "entity_base", "Value:short Count:int Age:short");
        node("entity:falling_block", "entity_base", "BlockState:compound(block_state) Time:int DropItem:bool HurtEntities:bool");
        node("entity:tnt", "entity_base", "fuse:short explosion_power:float block_state:compound(block_state)");
        node("display", "entity_base",
                "transformation:compound(transformation) billboard:string brightness:any view_range:float "
                        + "shadow_radius:float shadow_strength:float glow_color_override:int width:float height:float "
                        + "interpolation_duration:int teleport_duration:int");
        node("entity:item_display", "display", "item:compound(item) item_display:string");
        node("entity:block_display", "display", "block_state:compound(block_state)");
        node("entity:text_display", "display",
                "text:text background:int line_width:int text_opacity:byte alignment:string shadow:bool see_through:bool default_background:bool");
        node("entity:interaction", "entity_base", "width:float height:float response:bool");

        // Block entities.
        node("block_base", null, "CustomName:text components:any");
        node("container", "block_base", "Items:list(slot_item) LootTable:string LootTableSeed:long lock:any");
        node("block:sign", "block_base", "front_text:compound(sign_text) back_text:compound(sign_text) is_waxed:bool");
        node("sign_text", null, "messages:strings color:string has_glowing_text:bool");
        node("block:spawner", "block_base",
                "SpawnData:compound(spawn_data) SpawnPotentials:any Delay:short MinSpawnDelay:short MaxSpawnDelay:short "
                        + "SpawnCount:short MaxNearbyEntities:short RequiredPlayerRange:short SpawnRange:short");
        node("spawn_data", null, "entity:compound(passenger)");
        node("block:beacon", "block_base", "primary_effect:id(mob_effect) secondary_effect:id(mob_effect) Levels:int");
        node("block:command_block", "block_base", "Command:string auto:bool powered:bool TrackOutput:bool conditionMet:bool");
        node("block:furnace", "container", "cooking_time_spent:short cooking_total_time:short lit_time_remaining:short lit_total_time:short");
    }

    private NbtSchema() {
    }

    private static void node(String name, String parent, String spec) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (String part : spec.trim().split("\\s+")) {
            int colon = part.indexOf(':');
            String key = part.substring(0, colon);
            String type = part.substring(colon + 1);
            String child = null;
            int paren = type.indexOf('(');
            if (paren >= 0) {
                child = type.substring(paren + 1, type.length() - 1);
                type = type.substring(0, paren);
            }
            Kind kind = switch (type) {
                case "bool" -> Kind.BOOL;
                case "byte" -> Kind.BYTE;
                case "short" -> Kind.SHORT;
                case "int" -> Kind.INT;
                case "long" -> Kind.LONG;
                case "float" -> Kind.FLOAT;
                case "double" -> Kind.DOUBLE;
                case "string" -> Kind.STRING;
                case "text" -> Kind.TEXT;
                case "vec3d" -> Kind.VEC3D;
                case "vec2f" -> Kind.VEC2F;
                case "strings" -> Kind.STRING_LIST;
                case "float_list" -> Kind.FLOAT_LIST;
                case "compound" -> Kind.COMPOUND;
                case "list" -> Kind.LIST_COMPOUND;
                case "id" -> Kind.ID;
                default -> Kind.ANY;
            };
            fields.put(key, new Field(key, kind, child));
        }
        NODES.put(name, fields);
        EXTENDS.put(name, parent == null ? List.of() : List.of(parent));
    }

    /** All fields of a node including inherited ones (own fields first). Unknown nodes have no fields. */
    public static List<Field> fields(String node) {
        if (node == null || !NODES.containsKey(node)) {
            return List.of();
        }
        Map<String, Field> all = new LinkedHashMap<>();
        String current = node;
        while (current != null) {
            for (Field field : NODES.getOrDefault(current, Map.of()).values()) {
                all.putIfAbsent(field.name(), field);
            }
            List<String> parents = EXTENDS.getOrDefault(current, List.of());
            current = parents.isEmpty() ? null : parents.getFirst();
        }
        return Collections.unmodifiableList(new ArrayList<>(all.values()));
    }

    public static Field field(String node, String key) {
        for (Field field : fields(node)) {
            if (field.name().equals(key)) {
                return field;
            }
        }
        return null;
    }

    /** Root node for an entity type id such as "minecraft:zombie" or "husk" (null/blank → a generic mob). */
    public static String entityRoot(String typeId) {
        String path = stripNamespace(typeId);
        if (path.isEmpty()) {
            return "living";
        }
        String alias = switch (path) {
            case "husk", "drowned", "zombie_villager", "zombified_piglin" -> "zombie";
            case "magma_cube" -> "slime";
            default -> path;
        };
        if (NODES.containsKey("entity:" + alias)) {
            return "entity:" + alias;
        }
        return NON_LIVING.contains(path) || path.endsWith("_boat") || path.endsWith("_raft") ? "entity_base" : "living";
    }

    /** Root node for a block id such as "minecraft:chest" (unknown blocks get the shared block-entity keys). */
    public static String blockRoot(String blockId) {
        String path = stripNamespace(blockId);
        if (path.endsWith("_sign") || path.endsWith("_hanging_sign")) {
            return "block:sign";
        }
        if (path.endsWith("command_block")) {
            return "block:command_block";
        }
        if (path.equals("furnace") || path.equals("blast_furnace") || path.equals("smoker")) {
            return "block:furnace";
        }
        if (path.equals("spawner") || path.equals("beacon")) {
            return "block:" + path;
        }
        if (path.contains("chest") || path.endsWith("shulker_box") || path.equals("barrel") || path.equals("hopper")
                || path.equals("dispenser") || path.equals("dropper") || path.equals("crafter")
                || path.equals("brewing_stand") || path.equals("chiseled_bookshelf") || path.equals("decorated_pot")) {
            return "container";
        }
        return "block_base";
    }

    static String stripNamespace(String id) {
        if (id == null) {
            return "";
        }
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        int colon = trimmed.indexOf(':');
        return colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
    }
}
