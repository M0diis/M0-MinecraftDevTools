package me.m0dii.modules.itemdata;

import net.minecraft.registry.RegistryWrapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ItemComponentMetadata {
    private static final Spec[] SPECS = {
            spec("minecraft:custom_data", "ComponentType<NbtComponent>"),
            spec("minecraft:max_stack_size", "ComponentType<Integer>"),
            spec("minecraft:max_damage", "ComponentType<Integer>"),
            spec("minecraft:damage", "ComponentType<Integer>"),
            spec("minecraft:unbreakable", "ComponentType<Unit>"),
            spec("minecraft:use_effects", "ComponentType<UseEffectsComponent>"),
            spec("minecraft:custom_name", "ComponentType<Text>"),
            spec("minecraft:minimum_attack_charge", "ComponentType<Float>"),
            spec("minecraft:damage_type", "ComponentType<LazyRegistryEntryReference<DamageType>>"),
            spec("minecraft:item_name", "ComponentType<Text>"),
            spec("minecraft:item_model", "ComponentType<Identifier>"),
            spec("minecraft:lore", "ComponentType<LoreComponent>"),
            spec("minecraft:rarity", "ComponentType<Rarity>"),
            spec("minecraft:enchantments", "ComponentType<ItemEnchantmentsComponent>"),
            spec("minecraft:can_place_on", "ComponentType<BlockPredicatesComponent>"),
            spec("minecraft:can_break", "ComponentType<BlockPredicatesComponent>"),
            spec("minecraft:attribute_modifiers", "ComponentType<AttributeModifiersComponent>"),
            spec("minecraft:custom_model_data", "ComponentType<CustomModelDataComponent>"),
            spec("minecraft:tooltip_display", "ComponentType<TooltipDisplayComponent>"),
            spec("minecraft:repair_cost", "ComponentType<Integer>"),
            spec("minecraft:creative_slot_lock", "ComponentType<Unit>"),
            spec("minecraft:enchantment_glint_override", "ComponentType<Boolean>"),
            spec("minecraft:intangible_projectile", "ComponentType<Unit>"),
            spec("minecraft:food", "ComponentType<FoodComponent>"),
            spec("minecraft:consumable", "ComponentType<ConsumableComponent>"),
            spec("minecraft:use_remainder", "ComponentType<UseRemainderComponent>"),
            spec("minecraft:use_cooldown", "ComponentType<UseCooldownComponent>"),
            spec("minecraft:damage_resistant", "ComponentType<DamageResistantComponent>"),
            spec("minecraft:tool", "ComponentType<ToolComponent>"),
            spec("minecraft:weapon", "ComponentType<WeaponComponent>"),
            spec("minecraft:attack_range", "ComponentType<AttackRangeComponent>"),
            spec("minecraft:enchantable", "ComponentType<EnchantableComponent>"),
            spec("minecraft:equippable", "ComponentType<EquippableComponent>"),
            spec("minecraft:repairable", "ComponentType<RepairableComponent>"),
            spec("minecraft:glider", "ComponentType<Unit>"),
            spec("minecraft:tooltip_style", "ComponentType<Identifier>"),
            spec("minecraft:death_protection", "ComponentType<DeathProtectionComponent>"),
            spec("minecraft:blocks_attacks", "ComponentType<BlocksAttacksComponent>"),
            spec("minecraft:piercing_weapon", "ComponentType<PiercingWeaponComponent>"),
            spec("minecraft:kinetic_weapon", "ComponentType<KineticWeaponComponent>"),
            spec("minecraft:swing_animation", "ComponentType<SwingAnimationComponent>"),
            spec("minecraft:stored_enchantments", "ComponentType<ItemEnchantmentsComponent>"),
            spec("minecraft:dyed_color", "ComponentType<DyedColorComponent>"),
            spec("minecraft:map_color", "ComponentType<MapColorComponent>"),
            spec("minecraft:map_id", "ComponentType<MapIdComponent>"),
            spec("minecraft:map_decorations", "ComponentType<MapDecorationsComponent>"),
            spec("minecraft:map_post_processing", "ComponentType<MapPostProcessingComponent>"),
            spec("minecraft:charged_projectiles", "ComponentType<ChargedProjectilesComponent>"),
            spec("minecraft:bundle_contents", "ComponentType<BundleContentsComponent>"),
            spec("minecraft:potion_contents", "ComponentType<PotionContentsComponent>"),
            spec("minecraft:potion_duration_scale", "ComponentType<Float>"),
            spec("minecraft:suspicious_stew_effects", "ComponentType<SuspiciousStewEffectsComponent>"),
            spec("minecraft:writable_book_content", "ComponentType<WritableBookContentComponent>"),
            spec("minecraft:written_book_content", "ComponentType<WrittenBookContentComponent>"),
            spec("minecraft:trim", "ComponentType<ArmorTrim>"),
            spec("minecraft:debug_stick_state", "ComponentType<DebugStickStateComponent>"),
            spec("minecraft:entity_data", "ComponentType<TypedEntityData<EntityType<?>>>"),
            spec("minecraft:bucket_entity_data", "ComponentType<NbtComponent>"),
            spec("minecraft:block_entity_data", "ComponentType<TypedEntityData<BlockEntityType<?>>>"),
            spec("minecraft:instrument", "ComponentType<InstrumentComponent>"),
            spec("minecraft:provides_trim_material", "ComponentType<ProvidesTrimMaterialComponent>"),
            spec("minecraft:ominous_bottle_amplifier", "ComponentType<OminousBottleAmplifierComponent>"),
            spec("minecraft:jukebox_playable", "ComponentType<JukeboxPlayableComponent>"),
            spec("minecraft:provides_banner_patterns", "ComponentType<TagKey<BannerPattern>>"),
            spec("minecraft:recipes", "ComponentType<List<RegistryKey<Recipe<?>>>>"),
            spec("minecraft:lodestone_tracker", "ComponentType<LodestoneTrackerComponent>"),
            spec("minecraft:firework_explosion", "ComponentType<FireworkExplosionComponent>"),
            spec("minecraft:fireworks", "ComponentType<FireworksComponent>"),
            spec("minecraft:profile", "ComponentType<ProfileComponent>"),
            spec("minecraft:note_block_sound", "ComponentType<Identifier>"),
            spec("minecraft:banner_patterns", "ComponentType<BannerPatternsComponent>"),
            spec("minecraft:base_color", "ComponentType<DyeColor>"),
            spec("minecraft:pot_decorations", "ComponentType<Sherds>"),
            spec("minecraft:container", "ComponentType<ContainerComponent>"),
            spec("minecraft:block_state", "ComponentType<BlockStateComponent>"),
            spec("minecraft:bees", "ComponentType<BeesComponent>"),
            spec("minecraft:lock", "ComponentType<ContainerLock>"),
            spec("minecraft:container_loot", "ComponentType<ContainerLootComponent>"),
            spec("minecraft:break_sound", "ComponentType<RegistryEntry<SoundEvent>>"),
            spec("minecraft:villager_variant", "ComponentType<RegistryEntry<VillagerType>>"),
            spec("minecraft:wolf_variant", "ComponentType<RegistryEntry<WolfVariant>>"),
            spec("minecraft:wolf_sound_variant", "ComponentType<RegistryEntry<WolfSoundVariant>>"),
            spec("minecraft:wolf_collar", "ComponentType<DyeColor>"),
            spec("minecraft:fox_variant", "ComponentType<FoxEntity.Variant>"),
            spec("minecraft:salmon_size", "ComponentType<SalmonEntity.Variant>"),
            spec("minecraft:parrot_variant", "ComponentType<ParrotEntity.Variant>"),
            spec("minecraft:tropical_fish_pattern", "ComponentType<TropicalFishEntity.Pattern>"),
            spec("minecraft:tropical_fish_base_color", "ComponentType<DyeColor>"),
            spec("minecraft:tropical_fish_pattern_color", "ComponentType<DyeColor>"),
            spec("minecraft:mooshroom_variant", "ComponentType<MooshroomEntity.Variant>"),
            spec("minecraft:rabbit_variant", "ComponentType<RabbitEntity.Variant>"),
            spec("minecraft:pig_variant", "ComponentType<RegistryEntry<PigVariant>>"),
            spec("minecraft:cow_variant", "ComponentType<RegistryEntry<CowVariant>>"),
            spec("minecraft:chicken_variant", "ComponentType<LazyRegistryEntryReference<ChickenVariant>>"),
            spec("minecraft:zombie_nautilus_variant", "ComponentType<LazyRegistryEntryReference<ZombieNautilusVariant>>"),
            spec("minecraft:frog_variant", "ComponentType<RegistryEntry<FrogVariant>>"),
            spec("minecraft:horse_variant", "ComponentType<HorseColor>"),
            spec("minecraft:painting_variant", "ComponentType<RegistryEntry<PaintingVariant>>"),
            spec("minecraft:llama_variant", "ComponentType<LlamaEntity.Variant>"),
            spec("minecraft:axolotl_variant", "ComponentType<AxolotlEntity.Variant>"),
            spec("minecraft:cat_variant", "ComponentType<RegistryEntry<CatVariant>>"),
            spec("minecraft:cat_collar", "ComponentType<DyeColor>"),
            spec("minecraft:sheep_color", "ComponentType<DyeColor>"),
            spec("minecraft:shulker_color", "ComponentType<DyeColor>")
    };

    private static final Map<String, Entry> ENTRIES = createEntries();

    private ItemComponentMetadata() {
    }

    public static Entry entry(String componentId) {
        if (componentId == null || componentId.isBlank()) {
            return fallback("minecraft:component");
        }
        return ENTRIES.getOrDefault(componentId, fallback(componentId));
    }

    public static String label(String componentId) {
        return entry(componentId).label();
    }

    public static String typeHint(String componentId) {
        return entry(componentId).typeHint();
    }

    public static String description(String componentId) {
        return entry(componentId).description();
    }

    public static String starterTemplate(String componentId, RegistryWrapper.WrapperLookup registryLookup) {
        return entry(componentId).starterTemplate().apply(registryLookup);
    }

    public static String example(String componentId, RegistryWrapper.WrapperLookup registryLookup) {
        return entry(componentId).example().apply(registryLookup);
    }

    public static int priority(String componentId) {
        return entry(componentId).priority();
    }

    private static Map<String, Entry> createEntries() {
        Map<String, Entry> entries = new HashMap<>();
        for (Spec spec : SPECS) {
            Entry entry = genericEntry(spec);
            entries.put(entry.id(), entry);
        }

        replace(entries, "minecraft:custom_name", "Custom Name",
                "Sets the display name players see for this stack. Friendly input accepts legacy codes like `&c` and hex tags like `<#ff0000>`.",
                0,
                constant("&eRenamed Item"),
                constant("&cOP Sword"));
        replace(entries, "minecraft:item_name", "Item Name",
                "Overrides the base translated item name without adding an anvil-style custom rename. Friendly `&` and `<#rrggbb>` formatting is accepted.",
                1,
                constant("&bDebug Wrench"),
                constant("<#55ffff>Developer Tool"));
        replace(entries, "minecraft:lore", "Lore",
                "Adds tooltip lines under the item name. In easy editing you can enter one line per lore row with legacy or hex formatting.",
                2,
                constant("&7Line 1"),
                constant("&7Line 1\n<#ff5555>Danger"));
        replace(entries, "minecraft:enchantments", "Enchantments",
                "Stores the live enchantments applied to the item stack.",
                3,
                constant("{levels:{\"minecraft:sharpness\":1}}"),
                constant("{levels:{\"minecraft:sharpness\":5,\"minecraft:unbreaking\":3},show_in_tooltip:true}"));
        replace(entries, "minecraft:stored_enchantments", "Stored Enchantments",
                "Stores enchantments inside enchanted books and other carrier items.",
                4,
                constant("{levels:{\"minecraft:mending\":1}}"),
                constant("{levels:{\"minecraft:mending\":1,\"minecraft:unbreaking\":3}}"));
        replace(entries, "minecraft:damage", "Damage",
                "Sets how much durability has already been consumed. `0` means brand new.",
                5,
                constant("0"),
                constant("42"));
        replace(entries, "minecraft:max_damage", "Max Damage",
                "Overrides the durability cap used by damageable items.",
                6,
                constant("250"),
                constant("1561"));
        replace(entries, "minecraft:max_stack_size", "Max Stack Size",
                "Overrides how many copies of this item can stack together.",
                7,
                constant("64"),
                constant("16"));
        replace(entries, "minecraft:unbreakable", "Unbreakable",
                "Flag component. When present, the item stops losing durability.",
                8,
                constant("{}"),
                constant("{show_in_tooltip:false}"));
        replace(entries, "minecraft:custom_model_data", "Custom Model Data",
                "Extra model selectors for resource packs. Modern versions support structured values.",
                9,
                constant("{floats:[1.0f]}"),
                constant("{floats:[12.0f],strings:[\"ruby\"],flags:[true]}"));
        replace(entries, "minecraft:item_model", "Item Model",
                "Overrides the baked item model identifier.",
                10,
                constant("\"minecraft:diamond_sword\""),
                constant("\"minecraft:netherite_sword\""));
        replace(entries, "minecraft:tooltip_display", "Tooltip Display",
                "Controls which tooltip sections are hidden or shown for this stack.",
                11,
                constant("{hidden_components:[]}"),
                constant("{hidden_components:[\"minecraft:attribute_modifiers\",\"minecraft:unbreakable\"]}"));
        replace(entries, "minecraft:dyed_color", "Dyed Color",
                "Sets the RGB dye color used by leather armor and other dyeable items.",
                12,
                constant("{rgb:11743532}"),
                constant("{rgb:16711680,show_in_tooltip:true}"));
        replace(entries, "minecraft:map_color", "Map Color",
                "Overrides the display color used by the filled map item itself.",
                13,
                constant("{rgb:5635925}"),
                constant("{rgb:16755200}"));
        replace(entries, "minecraft:attribute_modifiers", "Attribute Modifiers",
                "Applies attack, armor, speed, and other stat modifiers while the item is equipped.",
                14,
                constant("{modifiers:[{type:\"minecraft:generic.attack_damage\",id:\"minecraft:bonus\",slot:\"mainhand\",amount:1.0d,operation:\"add_value\"}]}"),
                constant("{modifiers:[{type:\"minecraft:generic.attack_damage\",id:\"minecraft:bonus\",slot:\"mainhand\",amount:6.0d,operation:\"add_value\"},{type:\"minecraft:generic.attack_speed\",id:\"minecraft:tempo\",slot:\"mainhand\",amount:0.4d,operation:\"add_value\"}],show_in_tooltip:true}"));
        replace(entries, "minecraft:can_place_on", "Can Place On",
                "Adventure-mode whitelist for blocks this item may be placed on.",
                15,
                constant("{predicates:[{blocks:\"minecraft:stone\"}]}"),
                constant("{predicates:[{blocks:[\"minecraft:stone\",\"minecraft:deepslate\"]}],show_in_tooltip:true}"));
        replace(entries, "minecraft:can_break", "Can Break",
                "Adventure-mode whitelist for blocks this item may break.",
                16,
                constant("{predicates:[{blocks:\"minecraft:stone\"}]}"),
                constant("{predicates:[{blocks:[\"minecraft:obsidian\",\"minecraft:crying_obsidian\"]}],show_in_tooltip:true}"));
        replace(entries, "minecraft:container", "Container",
                "Stored item stacks for container-like items such as bundles, shulker boxes, or filled containers.",
                17,
                constant("[{slot:0,item:{id:\"minecraft:diamond\",count:1}}]"),
                constant("[{slot:0,item:{id:\"minecraft:golden_apple\",count:3}},{slot:1,item:{id:\"minecraft:ender_pearl\",count:16}}]"));
        replace(entries, "minecraft:bundle_contents", "Bundle Contents",
                "Items stored inside a bundle-style stack.",
                18,
                constant("[{id:\"minecraft:diamond\",count:1}]"),
                constant("[{id:\"minecraft:torch\",count:16},{id:\"minecraft:bread\",count:3}]"));
        replace(entries, "minecraft:repair_cost", "Repair Cost",
                "The anvil prior-work penalty stored on the stack.",
                19,
                constant("0"),
                constant("5"));
        replace(entries, "minecraft:trim", "Trim",
                "Armor trim material and pattern selection.",
                20,
                constant("{material:\"minecraft:iron\",pattern:\"minecraft:sentry\"}"),
                constant("{material:\"minecraft:amethyst\",pattern:\"minecraft:spire\",show_in_tooltip:true}"));
        replace(entries, "minecraft:food", "Food",
                "Defines hunger, saturation, and edible behavior for food-like items.",
                21,
                constant("{nutrition:4,saturation:0.3f,can_always_eat:false}"),
                constant("{nutrition:8,saturation:0.8f,can_always_eat:true}"));
        replace(entries, "minecraft:consumable", "Consumable",
                "Controls the use animation, consume sound, and timing when the stack is consumed.",
                22,
                constant("{consume_seconds:1.6f,animation:\"eat\"}"),
                constant("{consume_seconds:0.8f,animation:\"drink\",sound:\"minecraft:entity.generic.drink\"}"));
        replace(entries, "minecraft:use_remainder", "Use Remainder",
                "Determines which stack remains after the item is fully used.",
                23,
                constant("{convert_into:{id:\"minecraft:glass_bottle\",count:1}}"),
                constant("{convert_into:{id:\"minecraft:bucket\",count:1}}"));
        replace(entries, "minecraft:use_cooldown", "Use Cooldown",
                "Applies a reusable cooldown after the item is used.",
                24,
                constant("{seconds:1.0f}"),
                constant("{seconds:5.0f,cooldown_group:\"minecraft:ender_pearl\"}"));
        replace(entries, "minecraft:tool", "Tool",
                "Defines mining rules, speed, and drop-correction behavior for tools.",
                25,
                constant("{rules:[],default_mining_speed:1.0f,damage_per_block:1}"),
                constant("{rules:[{blocks:\"minecraft:obsidian\",speed:12.0f,correct_for_drops:true}],default_mining_speed:1.0f,damage_per_block:1}"));
        replace(entries, "minecraft:weapon", "Weapon",
                "Defines weapon-style damage application behavior.",
                26,
                constant("{item_damage_per_attack:1,disable_blocking_for_seconds:0.0f}"),
                constant("{item_damage_per_attack:1,disable_blocking_for_seconds:5.0f}"));
        replace(entries, "minecraft:attack_range", "Attack Range",
                "Adjusts the melee reach granted by this item.",
                27,
                constant("{base:3.0d}"),
                constant("{base:5.0d}"));
        replace(entries, "minecraft:enchantable", "Enchantable",
                "Controls enchantability and related enchanting behavior.",
                28,
                constant("{value:10}"),
                constant("{value:25}"));
        replace(entries, "minecraft:equippable", "Equippable",
                "Controls equipment slot, equip sound, and related wearable behavior.",
                29,
                constant("{slot:\"head\"}"),
                constant("{slot:\"chest\",equip_sound:\"minecraft:item.armor.equip_netherite\"}"));
        replace(entries, "minecraft:repairable", "Repairable",
                "Lists which ingredients can repair this item in an anvil or grindstone-like flow.",
                30,
                constant("{items:\"minecraft:diamond\"}"),
                constant("{items:[\"minecraft:diamond\",\"minecraft:netherite_ingot\"]}"));
        replace(entries, "minecraft:tooltip_style", "Tooltip Style",
                "Overrides the tooltip style identifier used when rendering the stack.",
                31,
                constant("\"minecraft:default\""),
                constant("\"minecraft:bundle\""));
        replace(entries, "minecraft:death_protection", "Death Protection",
                "Defines totem-style rescue effects when the holder would die.",
                32,
                constant("{death_effects:[{type:\"minecraft:apply_effects\",effects:[{id:\"minecraft:regeneration\",duration:900,amplifier:1}]}]}"),
                constant("{death_effects:[{type:\"minecraft:apply_effects\",effects:[{id:\"minecraft:absorption\",duration:100,amplifier:1},{id:\"minecraft:regeneration\",duration:900,amplifier:1}]}]}"));
        replace(entries, "minecraft:blocks_attacks", "Blocks Attacks",
                "Shield-like settings for blocking incoming attacks.",
                33,
                constant("{block_delay_seconds:0.25f,disable_cooldown_scale:1.0f}"),
                constant("{block_delay_seconds:0.25f,disable_cooldown_scale:1.0f,item_damage:{base:1.0f}}"));
        replace(entries, "minecraft:charged_projectiles", "Charged Projectiles",
                "Projectiles preloaded into crossbows and similar launchers.",
                35,
                constant("[{id:\"minecraft:arrow\",count:1}]"),
                constant("[{id:\"minecraft:firework_rocket\",count:1}]"));
        replace(entries, "minecraft:potion_contents", "Potion Contents",
                "Base potion plus any custom color or custom effects stored on the item.",
                36,
                constant("{potion:\"minecraft:healing\"}"),
                constant("{potion:\"minecraft:strength\",custom_color:16711680,custom_effects:[{id:\"minecraft:speed\",duration:200,amplifier:1}]}"));
        replace(entries, "minecraft:suspicious_stew_effects", "Suspicious Stew Effects",
                "Effects granted by suspicious stew when consumed.",
                37,
                constant("[{id:\"minecraft:night_vision\",duration:160}]"),
                constant("[{id:\"minecraft:regeneration\",duration:160},{id:\"minecraft:fire_resistance\",duration:100}]"));
        replace(entries, "minecraft:writable_book_content", "Writable Book Content",
                "Pages stored in a writable book.",
                38,
                lookup -> "{pages:[" + ItemDataCodec.encodeTextLiteral("Draft page", lookup) + "]}",
                lookup -> "{pages:[" + ItemDataCodec.encodeTextLiteral("Draft page 1", lookup) + "," + ItemDataCodec.encodeTextLiteral("Draft page 2", lookup) + "]}");
        replace(entries, "minecraft:written_book_content", "Written Book Content",
                "Author, title, and finalized pages stored in a written book.",
                39,
                lookup -> "{title:" + ItemDataCodec.encodeTextLiteral("Guide", lookup) + ",author:\"Player\",pages:[" + ItemDataCodec.encodeTextLiteral("Page 1", lookup) + "]}",
                lookup -> "{title:" + ItemDataCodec.encodeTextLiteral("Field Notes", lookup) + ",author:\"ModeS\",pages:[" + ItemDataCodec.encodeTextLiteral("Page 1", lookup) + "," + ItemDataCodec.encodeTextLiteral("Page 2", lookup) + "]}");
        replace(entries, "minecraft:entity_data", "Entity Data",
                "Entity NBT/data-components applied when the item spawns or carries an entity.",
                40,
                constant("{id:\"minecraft:zombie\"}"),
                constant("{id:\"minecraft:armor_stand\",Invisible:1b,NoGravity:1b}"));
        replace(entries, "minecraft:bucket_entity_data", "Bucket Entity Data",
                "Raw entity data carried by fish buckets and similar bucketed mobs.",
                41,
                constant("{}"),
                constant("{NoAI:1b,CustomName:'\"Bucket Friend\"'}"));
        replace(entries, "minecraft:block_entity_data", "Block Entity Data",
                "Block-entity data applied when the item places a block with stored state.",
                42,
                constant("{id:\"minecraft:chest\"}"),
                constant("{id:\"minecraft:beehive\",Bees:[]}"));
        replace(entries, "minecraft:instrument", "Instrument",
                "Goat horn or similar playable instrument identifier and tuning data.",
                43,
                constant("\"minecraft:ponder_goat_horn\""),
                constant("\"minecraft:dream_goat_horn\""));
        replace(entries, "minecraft:jukebox_playable", "Jukebox Playable",
                "Marks a stack as playable in a jukebox and points to the sound event it should use.",
                44,
                constant("{song:\"minecraft:13\"}"),
                constant("{song:\"minecraft:pigstep\",show_in_tooltip:true}"));
        replace(entries, "minecraft:recipes", "Recipes",
                "Recipes unlocked or referenced by the item.",
                45,
                constant("[\"minecraft:crafting_table\"]"),
                constant("[\"minecraft:crafting_table\",\"minecraft:furnace\"]"));
        replace(entries, "minecraft:lodestone_tracker", "Lodestone Tracker",
                "Compass-style target tracking for lodestones and dimensions.",
                46,
                constant("{target:{pos:[I;0,64,0],dimension:\"minecraft:overworld\"},tracked:true}"),
                constant("{target:{pos:[I;128,80,-32],dimension:\"minecraft:the_nether\"},tracked:false}"));
        replace(entries, "minecraft:firework_explosion", "Firework Explosion",
                "Single explosion definition for firework stars.",
                47,
                constant("{shape:\"star\",colors:[16711680],fade_colors:[16777215],trail:true,twinkle:true}"),
                constant("{shape:\"burst\",colors:[5635925,16755200],trail:true}"));
        replace(entries, "minecraft:fireworks", "Fireworks",
                "Full firework rocket payload including flight and explosion list.",
                48,
                constant("{flight_duration:1,explosions:[{shape:\"small_ball\",colors:[16711680]}]}"),
                constant("{flight_duration:3,explosions:[{shape:\"star\",colors:[16711680],fade_colors:[16776960],trail:true}]}"));
        replace(entries, "minecraft:profile", "Profile",
                "Player-profile information used by heads and other profile-backed items.",
                49,
                constant("{name:\"Player\"}"),
                constant("{name:\"Notch\"}"));
        replace(entries, "minecraft:banner_patterns", "Banner Patterns",
                "Layered banner pattern data stored on banners and shields.",
                50,
                constant("[{pattern:\"minecraft:stripe_downright\",color:\"white\"}]"),
                constant("[{pattern:\"minecraft:creeper\",color:\"green\"},{pattern:\"minecraft:border\",color:\"black\"}]"));
        replace(entries, "minecraft:pot_decorations", "Pot Decorations",
                "The four sherds or brick decorations placed on a decorated pot item.",
                51,
                constant("[\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\"]"),
                constant("[\"minecraft:archer_pottery_sherd\",\"minecraft:prize_pottery_sherd\",\"minecraft:skull_pottery_sherd\",\"minecraft:miner_pottery_sherd\"]"));
        replace(entries, "minecraft:block_state", "Block State",
                "Block-state properties stored by a placeable block item.",
                52,
                constant("{facing:\"north\"}"),
                constant("{facing:\"north\",powered:\"false\"}"));
        replace(entries, "minecraft:bees", "Bees",
                "Bee occupants stored in bee nests and beehives.",
                53,
                constant("[]"),
                constant("[{entity_data:{id:\"minecraft:bee\"},ticks_in_hive:1200,min_ticks_in_hive:600}]"));
        replace(entries, "minecraft:lock", "Lock",
                "Lock key used by container blocks that require a matching item name.",
                54,
                constant("\"Secret Key\""),
                constant("\"Admin Chest\""));
        replace(entries, "minecraft:container_loot", "Container Loot",
                "Unresolved loot table and seed stored by a container item.",
                55,
                constant("{loot_table:\"minecraft:chests/simple_dungeon\"}"),
                constant("{loot_table:\"minecraft:chests/end_city_treasure\",seed:123456789l}"));
        replace(entries, "minecraft:break_sound", "Break Sound",
                "Sound event played when this item breaks.",
                56,
                constant("\"minecraft:entity.item.break\""),
                constant("\"minecraft:block.glass.break\""));

        return Map.copyOf(entries);
    }

    private static Entry genericEntry(Spec spec) {
        String typeHint = simplifyTypeHint(spec.typeHint());
        String label = humanize(spec.id());
        int priority = genericPriority(spec.id(), typeHint);
        return new Entry(
                spec.id(),
                label,
                typeHint,
                genericDescription(spec.id(), label, typeHint),
                genericStarter(spec.id(), typeHint),
                genericExample(spec.id(), typeHint),
                priority
        );
    }

    private static void replace(Map<String, Entry> entries,
                                String id,
                                String label,
                                String description,
                                int priority,
                                Function<RegistryWrapper.WrapperLookup, String> starterTemplate,
                                Function<RegistryWrapper.WrapperLookup, String> example) {
        String typeHint = entries.containsKey(id) ? entries.get(id).typeHint() : "Unknown";
        entries.put(id, new Entry(id, label, typeHint, description, starterTemplate, example, priority));
    }

    private static Function<RegistryWrapper.WrapperLookup, String> genericStarter(String componentId, String typeHint) {
        if (componentId.endsWith("_variant")) {
            return constant("\"minecraft:default\"");
        }
        if (componentId.endsWith("_color") || componentId.endsWith("_collar")) {
            return constant("\"white\"");
        }

        return switch (typeHint) {
            case "Integer" -> constant(integerStarter(componentId));
            case "Float" -> constant(floatStarter(componentId));
            case "Boolean" -> constant("true");
            case "Unit" -> constant("{}");
            case "Text" -> textLiteral("Example");
            case "Identifier" -> constant(identifierStarter(componentId));
            case "Rarity" -> constant("\"rare\"");
            case "DyeColor" -> constant("\"white\"");
            case "NbtComponent" -> constant("{}");
            case "LoreComponent" -> lookup -> "[" + ItemDataCodec.encodeTextLiteral("Line 1", lookup) + "]";
            case "ItemEnchantmentsComponent" -> constant("{levels:{\"minecraft:sharpness\":1}}");
            case "BlockPredicatesComponent" -> constant("{predicates:[]}");
            case "AttributeModifiersComponent" -> constant("{modifiers:[]}");
            case "CustomModelDataComponent" -> constant("{floats:[1.0f]}");
            case "TooltipDisplayComponent" -> constant("{hidden_components:[]}");
            case "FoodComponent" -> constant("{nutrition:4,saturation:0.3f}");
            case "ConsumableComponent" -> constant("{consume_seconds:1.6f,animation:\"eat\"}");
            case "UseRemainderComponent" -> constant("{convert_into:{id:\"minecraft:air\",count:1}}");
            case "UseCooldownComponent" -> constant("{seconds:1.0f}");
            case "DamageResistantComponent" -> constant("{types:\"minecraft:in_fire\"}");
            case "ToolComponent" -> constant("{rules:[],default_mining_speed:1.0f,damage_per_block:1}");
            case "WeaponComponent" -> constant("{item_damage_per_attack:1,disable_blocking_for_seconds:0.0f}");
            case "AttackRangeComponent" -> constant("{base:3.0d}");
            case "EnchantableComponent" -> constant("{value:10}");
            case "EquippableComponent" -> constant("{slot:\"head\"}");
            case "RepairableComponent" -> constant("{items:\"minecraft:iron_ingot\"}");
            case "DyedColorComponent", "MapColorComponent" -> constant("{rgb:16711680}");
            case "MapIdComponent" -> constant("0");
            case "MapDecorationsComponent" -> constant("{}");
            case "MapPostProcessingComponent" -> constant("\"scale\"");
            case "ChargedProjectilesComponent" -> constant("[{id:\"minecraft:arrow\",count:1}]");
            case "BundleContentsComponent" -> constant("[{id:\"minecraft:diamond\",count:1}]");
            case "PotionContentsComponent" -> constant("{potion:\"minecraft:healing\"}");
            case "SuspiciousStewEffectsComponent" -> constant("[{id:\"minecraft:night_vision\",duration:160}]");
            case "WritableBookContentComponent" -> lookup -> "{pages:[" + ItemDataCodec.encodeTextLiteral("Page 1", lookup) + "]}";
            case "WrittenBookContentComponent" -> lookup -> "{title:" + ItemDataCodec.encodeTextLiteral("Book", lookup) + ",author:\"Player\",pages:[" + ItemDataCodec.encodeTextLiteral("Page 1", lookup) + "]}";
            case "ArmorTrim" -> constant("{material:\"minecraft:iron\",pattern:\"minecraft:sentry\"}");
            case "TypedEntityData<EntityType<?>>", "TypedEntityData<BlockEntityType<?>>" -> constant("{}");
            case "InstrumentComponent" -> constant("\"minecraft:ponder_goat_horn\"");
            case "ProvidesTrimMaterialComponent" -> constant("{}");
            case "OminousBottleAmplifierComponent" -> constant("1");
            case "JukeboxPlayableComponent" -> constant("{song:\"minecraft:13\"}");
            case "TagKey<BannerPattern>" -> constant("\"minecraft:no_item_required\"");
            case "List<RegistryKey<Recipe<?>>>" -> constant("[\"minecraft:crafting_table\"]");
            case "LodestoneTrackerComponent" -> constant("{target:{pos:[I;0,64,0],dimension:\"minecraft:overworld\"},tracked:true}");
            case "FireworkExplosionComponent" -> constant("{shape:\"small_ball\",colors:[16711680]}");
            case "FireworksComponent" -> constant("{flight_duration:1,explosions:[{shape:\"small_ball\",colors:[16711680]}]}");
            case "ProfileComponent" -> constant("{name:\"Player\"}");
            case "BannerPatternsComponent" -> constant("[{pattern:\"minecraft:stripe_downright\",color:\"white\"}]");
            case "Sherds" -> constant("[\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\"]");
            case "ContainerComponent" -> constant("[{slot:0,item:{id:\"minecraft:diamond\",count:1}}]");
            case "BlockStateComponent" -> constant("{facing:\"north\"}");
            case "BeesComponent" -> constant("[]");
            case "ContainerLock" -> constant("\"Secret Key\"");
            case "ContainerLootComponent" -> constant("{loot_table:\"minecraft:chests/simple_dungeon\"}");
            default -> constant(fallbackStarter(typeHint));
        };
    }

    private static Function<RegistryWrapper.WrapperLookup, String> genericExample(String componentId, String typeHint) {
        if (componentId.endsWith("_variant")) {
            return constant("\"minecraft:default\"");
        }
        if (componentId.endsWith("_color") || componentId.endsWith("_collar")) {
            return constant("\"orange\"");
        }

        return switch (typeHint) {
            case "Text" -> textLiteral("Example Name");
            case "Integer" -> constant(integerExample(componentId));
            case "Float" -> constant(floatExample(componentId));
            case "Boolean" -> constant("false");
            case "Identifier" -> constant(identifierExample(componentId));
            case "DyeColor" -> constant("\"orange\"");
            default -> genericStarter(componentId, typeHint);
        };
    }

    private static Entry fallback(String componentId) {
        String label = humanize(componentId);
        return new Entry(
                componentId,
                label,
                "Unknown",
                "No hand-written metadata is available for this component yet. Use the starter value, then Save to let the real codec validate it.",
                constant("{}"),
                constant("{}"),
                1000
        );
    }

    private static int genericPriority(String componentId, String typeHint) {
        if ("Text".equals(typeHint) || "Integer".equals(typeHint) || "Boolean".equals(typeHint)) {
            return 120;
        }
        if (componentId.contains("variant") || componentId.contains("color")) {
            return 340;
        }
        return 220;
    }

    private static String genericDescription(String componentId, String label, String typeHint) {
        if (componentId.endsWith("_variant")) {
            return "Selects the stored " + label.toLowerCase(Locale.ROOT) + " used by spawn eggs, buckets, paintings, or other entity-backed items.";
        }
        if (componentId.endsWith("_color")) {
            return "Sets the stored color used by this item or the entity/block it represents.";
        }
        if (componentId.endsWith("_collar")) {
            return "Sets the collar color applied by this entity-related item.";
        }
        return switch (typeHint) {
            case "Integer" -> "Stores an integer value used by this item.";
            case "Float" -> "Stores a floating-point value used by this item.";
            case "Boolean" -> "Boolean toggle for this item behavior.";
            case "Unit" -> "Presence-only flag. `{}` enables this behavior.";
            case "Text" -> "Stores a text component rendered by the item.";
            case "Identifier" -> "Stores a resource identifier used by the item.";
            case "Rarity" -> "Controls the rarity tier shown in the tooltip.";
            case "DyeColor" -> "Stores a dye color value.";
            case "NbtComponent" -> "Stores raw structured NBT-like data for the item.";
            default -> {
                if (typeHint.startsWith("RegistryEntry<") || typeHint.startsWith("LazyRegistryEntryReference<")) {
                    yield "Stores a registry entry reference used by the item.";
                }
                if (typeHint.startsWith("List<")) {
                    yield "Stores a list of values used by the item.";
                }
                if (typeHint.endsWith("Component")) {
                    yield "Stores structured " + humanizeType(typeHint.substring(0, typeHint.length() - "Component".length())).toLowerCase(Locale.ROOT) + " data for the item.";
                }
                yield "Stores " + humanizeType(typeHint).toLowerCase(Locale.ROOT) + " data for the item.";
            }
        };
    }

    private static String simplifyTypeHint(String rawTypeHint) {
        if (rawTypeHint == null || rawTypeHint.isBlank()) {
            return "Unknown";
        }
        String prefix = "ComponentType<";
        if (rawTypeHint.startsWith(prefix) && rawTypeHint.endsWith(">")) {
            return rawTypeHint.substring(prefix.length(), rawTypeHint.length() - 1);
        }
        return rawTypeHint;
    }

    private static String integerStarter(String componentId) {
        return switch (componentId) {
            case "minecraft:max_stack_size" -> "64";
            case "minecraft:max_damage" -> "250";
            case "minecraft:damage" -> "0";
            case "minecraft:repair_cost" -> "0";
            case "minecraft:map_id" -> "0";
            default -> "1";
        };
    }

    private static String integerExample(String componentId) {
        return switch (componentId) {
            case "minecraft:max_stack_size" -> "16";
            case "minecraft:max_damage" -> "1561";
            case "minecraft:damage" -> "42";
            case "minecraft:repair_cost" -> "5";
            case "minecraft:map_id" -> "12";
            default -> "2";
        };
    }

    private static String floatStarter(String componentId) {
        return switch (componentId) {
            case "minecraft:minimum_attack_charge" -> "0.0f";
            default -> "1.0f";
        };
    }

    private static String floatExample(String componentId) {
        return switch (componentId) {
            case "minecraft:minimum_attack_charge" -> "0.85f";
            case "minecraft:potion_duration_scale" -> "0.5f";
            default -> "2.0f";
        };
    }

    private static String identifierStarter(String componentId) {
        return switch (componentId) {
            case "minecraft:item_model" -> "\"minecraft:diamond_sword\"";
            case "minecraft:tooltip_style" -> "\"minecraft:default\"";
            case "minecraft:note_block_sound" -> "\"minecraft:block.note_block.harp\"";
            default -> "\"minecraft:example\"";
        };
    }

    private static String identifierExample(String componentId) {
        return switch (componentId) {
            case "minecraft:item_model" -> "\"minecraft:netherite_sword\"";
            case "minecraft:tooltip_style" -> "\"minecraft:bundle\"";
            case "minecraft:note_block_sound" -> "\"minecraft:block.note_block.bell\"";
            default -> "\"minecraft:example_path\"";
        };
    }

    private static String fallbackStarter(String typeHint) {
        if (typeHint.startsWith("List<")) {
            return "[]";
        }
        if (typeHint.startsWith("RegistryEntry<") || typeHint.startsWith("LazyRegistryEntryReference<")) {
            return "\"minecraft:default\"";
        }
        return "{}";
    }

    private static Function<RegistryWrapper.WrapperLookup, String> constant(String value) {
        return lookup -> value;
    }

    private static Function<RegistryWrapper.WrapperLookup, String> textLiteral(String value) {
        return lookup -> ItemDataCodec.encodeTextLiteral(value, lookup);
    }

    private static String humanize(String componentId) {
        String path = componentId;
        int colon = path.indexOf(':');
        if (colon >= 0 && colon + 1 < path.length()) {
            path = path.substring(colon + 1);
        }
        return humanizeType(path);
    }

    private static String humanizeType(String raw) {
        String normalized = raw.replace('<', ' ')
                .replace('>', ' ')
                .replace('?', ' ')
                .replace('.', ' ')
                .replace(',', ' ')
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('/', ' ')
                .replace('-', ' ');
        String[] parts = normalized.split("[_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Unknown" : builder.toString();
    }

    private static Spec spec(String id, String typeHint) {
        return new Spec(id, typeHint);
    }

    public record Entry(String id,
                        String label,
                        String typeHint,
                        String description,
                        Function<RegistryWrapper.WrapperLookup, String> starterTemplate,
                        Function<RegistryWrapper.WrapperLookup, String> example,
                        int priority) {
    }

    private record Spec(String id, String typeHint) {
    }

}
