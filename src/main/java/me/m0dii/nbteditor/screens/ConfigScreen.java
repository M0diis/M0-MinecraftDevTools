package me.m0dii.nbteditor.screens;

import com.google.gson.*;
import lombok.Getter;
import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.screens.configurable.*;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConfigScreen extends TickableSupportingScreen {

    public static final List<Consumer<ConfigCategory>> ADDED_OPTIONS = new ArrayList<>();
    @Getter
    private static EnchantLevelMax enchantLevelMax;
    @Getter
    private static boolean enchantNumberTypeArabic;
    @Getter
    private static double keyTextSize;
    @Getter
    private static boolean keybindsHidden;
    private static boolean lockSlots; // Not shown in screen
    @Getter
    private static boolean chatLimitExtended;
    @Getter
    private static boolean singleQuotesAllowed;
    @Getter
    private static double scrollSpeed;
    @Getter
    private static boolean airEditable;
    @Getter
    private static boolean jsonText;
    @Getter
    private static List<String> shortcuts;
    @Getter
    private static CheckUpdatesLevel checkUpdates;
    @Getter
    private static boolean screenshotOptions;
    @Getter
    private static boolean tooltipOverflowFix;
    @Getter
    private static boolean noSlotRestrictions;
    @Getter
    private static boolean hideFormatButtons;
    @Getter
    private static boolean specialNumbers;
    @Getter
    private static List<Alias> aliases;
    @Getter
    private static ItemSizeFormat itemSizeFormat;
    @Getter
    private static boolean invertedPageKeybinds;
    @Getter
    private static boolean triggerBlockUpdates;
    @Getter
    private static boolean warnIncompatibleProtocol;
    @Getter
    private static boolean enchantGlintFix;
    @Getter
    private static boolean recreateBlocksAndEntities;

    private final Screen parent;
    private final ConfigCategory config;
    private ConfigPanel panel;

    public ConfigScreen(Screen parent) {
        super(TextInst.translatable("nbteditor.config"));

        this.parent = parent;
        this.config = new ConfigCategory(TextInst.translatable("nbteditor.config"));

        ConfigCategory mc = new ConfigCategory(TextInst.translatable("nbteditor.config.category.mc"));
        ConfigCategory guis = new ConfigCategory(TextInst.translatable("nbteditor.config.category.guis"));
        ConfigCategory functional = new ConfigCategory(TextInst.translatable("nbteditor.config.category.functional"));

        this.config.setConfigurable("mc", mc);
        this.config.setConfigurable("guis", guis);
        this.config.setConfigurable("functional", functional);

        // ---------- MC ----------

        mc.setConfigurable("extendChatLimit", new ConfigItem<>(TextInst.translatable("nbteditor.config.chat_limit"),
                new ConfigValueBoolean(chatLimitExtended, false, 100, TextInst.translatable("nbteditor.config.chat_limit.extended"), TextInst.translatable("nbteditor.config.chat_limit.normal"))
                        .addValueListener(value -> chatLimitExtended = value.getValidValue()))
                .setTooltip("nbteditor.config.chat_limit.desc"));

        mc.setConfigurable("tooltipOverflowFix", new ConfigItem<>(TextInst.translatable("nbteditor.config.tooltip_overflow_fix"),
                new ConfigValueBoolean(tooltipOverflowFix, true, 100, TextInst.translatable("nbteditor.config.tooltip_overflow_fix.enabled"), TextInst.translatable("nbteditor.config.tooltip_overflow_fix.disabled"))
                        .addValueListener(value -> tooltipOverflowFix = value.getValidValue()))
                .setTooltip("nbteditor.config.tooltip_overflow_fix.desc"));

        mc.setConfigurable("maxEnchantLevelDisplay", new ConfigItem<>(TextInst.translatable("nbteditor.config.enchant_level_max"),
                ConfigValueDropdown.forEnum(enchantLevelMax, EnchantLevelMax.NEVER, EnchantLevelMax.class)
                        .addValueListener(value -> enchantLevelMax = value.getValidValue()))
                .setTooltip("nbteditor.config.enchant_level_max.desc"));

        mc.setConfigurable("useArabicEnchantLevels", new ConfigItem<>(TextInst.translatable("nbteditor.config.enchant_number_type"),
                new ConfigValueBoolean(enchantNumberTypeArabic, false, 100, TextInst.translatable("nbteditor.config.enchant_number_type.arabic"),
                        TextInst.translatable("nbteditor.config.enchant_number_type.roman"), new MVTooltip(TextInst.translatable("nbteditor.config.enchant_number_type.desc2")))
                        .addValueListener(value -> enchantNumberTypeArabic = value.getValidValue()))
                .setTooltip("nbteditor.config.enchant_number_type.desc"));

        mc.setConfigurable("noSlotRestrictions", new ConfigItem<>(TextInst.translatable("nbteditor.config.no_slot_restrictions"),
                new ConfigValueBoolean(noSlotRestrictions, false, 100, TextInst.translatable("nbteditor.config.no_slot_restrictions.enabled"), TextInst.translatable("nbteditor.config.no_slot_restrictions.disabled"))
                        .addValueListener(value -> noSlotRestrictions = value.getValidValue()))
                .setTooltip("nbteditor.config.no_slot_restrictions.desc"));

        mc.setConfigurable("screenshotOptions", new ConfigItem<>(TextInst.translatable("nbteditor.config.screenshot_options"),
                new ConfigValueBoolean(screenshotOptions, true, 100, TextInst.translatable("nbteditor.config.screenshot_options.enabled"), TextInst.translatable("nbteditor.config.screenshot_options.disabled"))
                        .addValueListener(value -> screenshotOptions = value.getValidValue()))
                .setTooltip(new MVTooltip(TextInst.translatable("nbteditor.config.screenshot_options.desc", TextInst.translatable("nbteditor.file_options.show"), TextInst.translatable("nbteditor.file_options.delete")))));

        mc.setConfigurable("enchantGlintFix", new ConfigItem<>(TextInst.translatable("nbteditor.config.enchant_glint_fix"),
                new ConfigValueBoolean(enchantGlintFix, false, 100, TextInst.translatable("nbteditor.config.enchant_glint_fix.enabled"), TextInst.translatable("nbteditor.config.enchant_glint_fix.disabled"))
                        .addValueListener(value -> enchantGlintFix = value.getValidValue()))
                .setTooltip("nbteditor.config.enchant_glint_fix.desc"));

        // ---------- GUIs ----------

        guis.setConfigurable("scrollSpeed", new ConfigItem<>(TextInst.translatable("nbteditor.config.scroll_speed"),
                ConfigValueSlider.forDouble(100, scrollSpeed, 5, 0.5, 10, 0.05, value -> TextInst.literal(String.format("%.2f", value)))
                        .addValueListener(value -> scrollSpeed = value.getValidValue()))
                .setTooltip("nbteditor.config.scroll_speed.desc"));

        guis.setConfigurable("hideFormatButtons", new ConfigItem<>(TextInst.translatable("nbteditor.config.hide_format_buttons"),
                new ConfigValueBoolean(hideFormatButtons, false, 100, TextInst.translatable("nbteditor.config.hide_format_buttons.enabled"), TextInst.translatable("nbteditor.config.hide_format_buttons.disabled"))
                        .addValueListener(value -> hideFormatButtons = value.getValidValue()))
                .setTooltip("nbteditor.config.hide_format_buttons.desc"));

        guis.setConfigurable("hideKeybinds", new ConfigItem<>(TextInst.translatable("nbteditor.config.keybinds"),
                new ConfigValueBoolean(keybindsHidden, false, 100, TextInst.translatable("nbteditor.config.keybinds.hidden"), TextInst.translatable("nbteditor.config.keybinds.shown"),
                        new MVTooltip("nbteditor.keybind.edit", "nbteditor.keybind.factory", "nbteditor.keybind.container", "nbteditor.keybind.enchant", "nbteditor.keybind.delete"))
                        .addValueListener(value -> keybindsHidden = value.getValidValue()))
                .setTooltip("nbteditor.config.keybinds.desc"));

        guis.setConfigurable("invertedPageKeybinds", new ConfigItem<>(TextInst.translatable("nbteditor.config.page_keybinds"),
                new ConfigValueBoolean(invertedPageKeybinds, false, 100, TextInst.translatable("nbteditor.config.page_keybinds.inverted"), TextInst.translatable("nbteditor.config.page_keybinds.normal"))
                        .addValueListener(value -> invertedPageKeybinds = value.getValidValue()))
                .setTooltip("nbteditor.config.page_keybinds.desc"));

        guis.setConfigurable("itemSize", new ConfigItem<>(TextInst.translatable("nbteditor.config.item_size"),
                ConfigValueDropdown.forEnum(itemSizeFormat, ItemSizeFormat.HIDDEN, ItemSizeFormat.class)
                        .addValueListener(value -> itemSizeFormat = value.getValidValue()))
                .setTooltip("nbteditor.config.item_size.desc"));

        guis.setConfigurable("keyTextSize", new ConfigItem<>(TextInst.translatable("nbteditor.config.key_text_size"),
                ConfigValueSlider.forDouble(100, keyTextSize, 0.5, 0.5, 1, 0.05, value -> TextInst.literal(String.format("%.2f", value)))
                        .addValueListener(value -> keyTextSize = value.getValidValue()))
                .setTooltip("nbteditor.config.key_text_size.desc"));

        guis.setConfigurable("checkUpdates", new ConfigItem<>(TextInst.translatable("nbteditor.config.check_updates"),
                ConfigValueDropdown.forEnum(checkUpdates, CheckUpdatesLevel.MINOR, CheckUpdatesLevel.class)
                        .addValueListener(value -> checkUpdates = value.getValidValue()))
                .setTooltip("nbteditor.config.check_updates.desc"));

        guis.setConfigurable("warnIncompatibleProtocol", new ConfigItem<>(TextInst.translatable("nbteditor.config.warn_incompatible_protocol"),
                new ConfigValueBoolean(warnIncompatibleProtocol, true, 100, TextInst.translatable("nbteditor.config.warn_incompatible_protocol.enabled"), TextInst.translatable("nbteditor.config.warn_incompatible_protocol.disabled"))
                        .addValueListener(value -> warnIncompatibleProtocol = value.getValidValue()))
                .setTooltip("nbteditor.config.warn_incompatible_protocol.desc"));

        // ---------- FUNCTIONAL ----------

        functional.setConfigurable("aliases", new ConfigButton(100, TextInst.translatable("nbteditor.config.aliases"),
                btn -> client.setScreen(new AliasesScreen(this)), new MVTooltip("nbteditor.config.aliases.desc")));

        functional.setConfigurable("shortcuts", new ConfigButton(100, TextInst.translatable("nbteditor.config.shortcuts"),
                btn -> client.setScreen(new ShortcutsScreen(this)), new MVTooltip("nbteditor.config.shortcuts.desc")));

        functional.setConfigurable("recreateBlocksAndEntities", new ConfigItem<>(TextInst.translatable("nbteditor.config.recreate_blocks_and_entities"),
                new ConfigValueBoolean(recreateBlocksAndEntities, false, 100, TextInst.translatable("nbteditor.config.recreate_blocks_and_entities.enabled"), TextInst.translatable("nbteditor.config.recreate_blocks_and_entities.disabled"))
                        .addValueListener(value -> recreateBlocksAndEntities = value.getValidValue()))
                .setTooltip("nbteditor.config.recreate_blocks_and_entities.desc"));

        functional.setConfigurable("airEditable", new ConfigItem<>(TextInst.translatable("nbteditor.config.air_editable"),
                new ConfigValueBoolean(airEditable, false, 100, TextInst.translatable("nbteditor.config.air_editable.yes"), TextInst.translatable("nbteditor.config.air_editable.no"))
                        .addValueListener(value -> airEditable = value.getValidValue()))
                .setTooltip("nbteditor.config.air_editable.desc"));

        functional.setConfigurable("specialNumbers", new ConfigItem<>(TextInst.translatable("nbteditor.config.special_numbers"),
                new ConfigValueBoolean(specialNumbers, true, 100, TextInst.translatable("nbteditor.config.special_numbers.enabled"), TextInst.translatable("nbteditor.config.special_numbers.disabled"))
                        .addValueListener(value -> specialNumbers = value.getValidValue()))
                .setTooltip("nbteditor.config.special_numbers.desc"));

        functional.setConfigurable("triggerBlockUpdates", new ConfigItem<>(TextInst.translatable("nbteditor.config.trigger_block_updates"),
                new ConfigValueBoolean(triggerBlockUpdates, true, 100, TextInst.translatable("nbteditor.config.trigger_block_updates.yes"), TextInst.translatable("nbteditor.config.trigger_block_updates.no"))
                        .addValueListener(value -> triggerBlockUpdates = value.getValidValue()))
                .setTooltip("nbteditor.config.trigger_block_updates.desc"));

        functional.setConfigurable("jsonText", new ConfigItem<>(TextInst.translatable("nbteditor.config.json_text"),
                new ConfigValueBoolean(jsonText, false, 100, TextInst.translatable("nbteditor.config.json_text.yes"), TextInst.translatable("nbteditor.config.json_text.no"))
                        .addValueListener(value -> jsonText = value.getValidValue()))
                .setTooltip("nbteditor.config.json_text.desc"));

        functional.setConfigurable("allowSingleQuotes", new ConfigItem<>(TextInst.translatable("nbteditor.config.single_quotes"),
                new ConfigValueBoolean(singleQuotesAllowed, false, 100, TextInst.translatable("nbteditor.config.single_quotes.allowed"),
                        TextInst.translatable("nbteditor.config.single_quotes.not_allowed"), new MVTooltip("nbteditor.config.single_quotes.example"))
                        .addValueListener(value -> singleQuotesAllowed = value.getValidValue()))
                .setTooltip("nbteditor.config.single_quotes.desc"));

        ADDED_OPTIONS.forEach(option -> option.accept(config));
    }

    public static void loadSettings() {
        enchantLevelMax = EnchantLevelMax.NEVER;
        enchantNumberTypeArabic = false;
        keyTextSize = 0.5;
        keybindsHidden = false;
        chatLimitExtended = false;
        singleQuotesAllowed = false;
        scrollSpeed = 5;
        airEditable = false;
        jsonText = false;
        shortcuts = new ArrayList<>();
        checkUpdates = CheckUpdatesLevel.MINOR;
        screenshotOptions = true;
        tooltipOverflowFix = true;
        noSlotRestrictions = false;
        hideFormatButtons = false;
        specialNumbers = true;
        aliases = new ArrayList<>(List.of(
                new Alias("m0-dev-tools", "nbt"),
                new Alias("factory signature", "sign")));
        itemSizeFormat = ItemSizeFormat.HIDDEN;
        invertedPageKeybinds = false;
        triggerBlockUpdates = true;
        warnIncompatibleProtocol = true;
        enchantGlintFix = false;
        recreateBlocksAndEntities = false;

        try {
            // Many config options use the old names
            // To avoid converting the config types, the old names are still used
            JsonObject settings = new Gson().fromJson(new String(Files.readAllBytes(new File(M0DevToolsClient.SETTINGS_FOLDER, "settings.json").toPath())), JsonObject.class);
            enchantLevelMax = EnchantLevelMax.valueOf(settings.get("maxEnchantLevelDisplay").getAsString());
            enchantNumberTypeArabic = settings.get("useArabicEnchantLevels").getAsBoolean();
            keyTextSize = settings.get("keyTextSize").getAsDouble();
            keybindsHidden = settings.get("hideKeybinds").getAsBoolean();
            lockSlots = settings.get("lockSlots").getAsBoolean();
            chatLimitExtended = settings.get("extendChatLimit").getAsBoolean();
            singleQuotesAllowed = settings.get("allowSingleQuotes").getAsBoolean();
            scrollSpeed = settings.get("scrollSpeed").getAsDouble();
            airEditable = settings.get("airEditable").getAsBoolean();
            jsonText = settings.get("jsonText").getAsBoolean();
            shortcuts = getStream(settings.get("shortcuts").getAsJsonArray())
                    .map(cmd -> cmd.getAsString()).collect(Collectors.toList());
            JsonPrimitive checkUpdatesLegacy = settings.get("checkUpdates").getAsJsonPrimitive();
            checkUpdates = checkUpdatesLegacy.isBoolean() ?
                    (checkUpdatesLegacy.getAsBoolean() ? CheckUpdatesLevel.MINOR : CheckUpdatesLevel.NONE)
                    : CheckUpdatesLevel.valueOf(checkUpdatesLegacy.getAsString());
            screenshotOptions = settings.get("screenshotOptions").getAsBoolean();
            tooltipOverflowFix = settings.get("tooltipOverflowFix").getAsBoolean();
            noSlotRestrictions = settings.get("noArmorRestriction").getAsBoolean();
            hideFormatButtons = settings.get("hideFormatButtons").getAsBoolean();
            specialNumbers = settings.get("specialNumbers").getAsBoolean();
            aliases = getStream(settings.get("aliases").getAsJsonArray())
                    .map(alias -> new Alias(alias.getAsJsonObject().get("original").getAsString(),
                            alias.getAsJsonObject().get("alias").getAsString())).collect(Collectors.toList());
            itemSizeFormat = ItemSizeFormat.valueOf(settings.get("itemSize").getAsString());
            invertedPageKeybinds = settings.get("invertedPageKeybinds").getAsBoolean();
            triggerBlockUpdates = settings.get("triggerBlockUpdates").getAsBoolean();
            warnIncompatibleProtocol = settings.get("warnIncompatibleProtocol").getAsBoolean();
            enchantGlintFix = settings.get("enchantGlintFix").getAsBoolean();
            recreateBlocksAndEntities = settings.get("recreateBlocksAndEntities").getAsBoolean();
        } catch (NoSuchFileException | ClassCastException | NullPointerException e) {
            M0DevTools.LOGGER.info("Missing some settings from settings.json, fixing ...");
            saveSettings();
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error while loading settings", e);
        }
    }

    private static void saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("maxEnchantLevelDisplay", enchantLevelMax.name());
        settings.addProperty("useArabicEnchantLevels", enchantNumberTypeArabic);
        settings.addProperty("keyTextSize", keyTextSize);
        settings.addProperty("hideKeybinds", keybindsHidden);
        settings.addProperty("lockSlots", lockSlots);
        settings.addProperty("extendChatLimit", chatLimitExtended);
        settings.addProperty("allowSingleQuotes", singleQuotesAllowed);
        settings.addProperty("scrollSpeed", scrollSpeed);
        settings.addProperty("airEditable", airEditable);
        settings.addProperty("jsonText", jsonText);
        settings.add("shortcuts", shortcuts.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        settings.addProperty("checkUpdates", checkUpdates.name());
        settings.addProperty("screenshotOptions", screenshotOptions);
        settings.addProperty("tooltipOverflowFix", tooltipOverflowFix);
        settings.addProperty("noArmorRestriction", noSlotRestrictions);
        settings.addProperty("hideFormatButtons", hideFormatButtons);
        settings.addProperty("specialNumbers", specialNumbers);
        settings.add("aliases", aliases.stream().map(alias -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("original", alias.original);
            obj.addProperty("alias", alias.alias);
            return obj;
        }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        settings.addProperty("itemSize", itemSizeFormat.name());
        settings.addProperty("invertedPageKeybinds", invertedPageKeybinds);
        settings.addProperty("triggerBlockUpdates", triggerBlockUpdates);
        settings.addProperty("warnIncompatibleProtocol", warnIncompatibleProtocol);
        settings.addProperty("enchantGlintFix", enchantGlintFix);
        settings.addProperty("recreateBlocksAndEntities", recreateBlocksAndEntities);

        try {
            Files.write(new File(M0DevToolsClient.SETTINGS_FOLDER, "settings.json").toPath(), new Gson().toJson(settings).getBytes());
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Error while saving settings", e);
        }
    }

    // jsonArray.asList().stream() doesn't exist in 1.17
    private static Stream<JsonElement> getStream(JsonArray jsonArray) {
        return StreamSupport.stream(jsonArray.spliterator(), false);
    }

    public static boolean isLockSlots() {
        return lockSlots || isLockSlotsRequired();
    }

    public static void setLockSlots(boolean lockSlots) {
        ConfigScreen.lockSlots = lockSlots;
        saveSettings();
    }

    public static boolean isLockSlotsRequired() {
        return MiscUtil.client.interactionManager != null && !M0DevToolsClient.SERVER_CONN.isEditingAllowed();
    }

    private static EditableText getEnchantName(Enchantment enchant, int level) {
        EditableText output = TextInst.copy(MVEnchantments.getEnchantmentName(enchant));
        if (level != 1 || enchant.getMaxLevel() != 1 || enchantLevelMax == EnchantLevelMax.ALWAYS) {
            output.append(" ");
            if (isEnchantNumberTypeArabic()) {
                output.append("" + level);
            } else {
                output.append(TextInst.translatable("enchantment.level." + level));
            }
        }
        return output;
    }

    public static Text getEnchantNameWithMax(Enchantment enchant, int level, EnchantLevelMax display) {
        EditableText text = getEnchantName(enchant, level);
        if (display.shouldShowMax(level, enchant.getMaxLevel())) {
            text = text.append("/").append(
                    ConfigScreen.isEnchantNumberTypeArabic() ?
                            TextInst.of("" + enchant.getMaxLevel()) :
                            TextInst.translatable("enchantment.level." + enchant.getMaxLevel()));
        }
        return text.getInternalValue(); // Allows Enchantment Descriptions to detect the enchantments
    }

    public static Text getEnchantNameWithMax(Enchantment enchant, int level) {
        return getEnchantNameWithMax(enchant, level, enchantLevelMax);
    }

    @Override
    protected void init() {
        ConfigPanel newPanel = addDrawableChild(new ConfigPanel(16, 16, width - 32, height - 32, config));
        if (panel != null) {
            newPanel.setScroll(panel.getScroll());
        }
        panel = newPanel;

        this.addDrawableChild(MVMisc.newButton(this.width - 134, this.height - 36, 100, 20, ScreenTexts.DONE, btn -> close()));
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public void close() {
        client.setScreen(this.parent);
    }

    @Override
    public void removed() {
        saveSettings();
    }

    public enum EnchantLevelMax implements ConfigTooltipSupplier {
        NEVER("nbteditor.config.enchant_level_max.never", (level, maxLevel) -> false),
        NOT_MAXED_EXACT("nbteditor.config.enchant_level_max.not_exact", (level, maxLevel) -> level != maxLevel),
        NOT_MAXED("nbteditor.config.enchant_level_max.not_max", (level, maxLevel) -> level < maxLevel),
        ALWAYS("nbteditor.config.enchant_level_max.always", (level, maxLevel) -> true);

        private final Text label;
        private final BiFunction<Integer, Integer, Boolean> showMax;

        EnchantLevelMax(String key, BiFunction<Integer, Integer, Boolean> showMax) {
            this.label = TextInst.translatable(key);
            this.showMax = showMax;
        }

        public boolean shouldShowMax(int level, int maxLevel) {
            return showMax.apply(level, maxLevel);
        }

        @Override
        public String toString() {
            return label.getString();
        }

        @Override
        public MVTooltip getTooltip() {
            List<Text> output = new ArrayList<>();

            for (int lvl = 1; lvl <= 3; lvl++) {
                output.add(getEnchantNameWithMax(MVEnchantments.FIRE_ASPECT, lvl, this));
            }

            return new MVTooltip(output);
        }
    }

    public enum CheckUpdatesLevel implements ConfigTooltipSupplier {
        MINOR("nbteditor.config.check_updates.minor", 1),
        PATCH("nbteditor.config.check_updates.patch", 2),
        NONE("nbteditor.config.check_updates.none", -1);

        private final Text label;
        private final Text desc;
        @Getter
        private final int level;

        CheckUpdatesLevel(String key, int level) {
            this.label = TextInst.translatable(key);
            this.desc = TextInst.translatable(key + ".desc");
            this.level = level;
        }

        @Override
        public String toString() {
            return label.getString();
        }

        @Override
        public MVTooltip getTooltip() {
            return new MVTooltip(desc);
        }
    }

    public enum ItemSizeFormat {
        HIDDEN("nbteditor.config.item_size.hidden", -1, false),
        AUTO("nbteditor.config.item_size.auto", 0, false),
        AUTO_COMPRESSED("nbteditor.config.item_size.auto_compressed", 0, true),
        BYTE("nbteditor.config.item_size.byte", 1, false),
        KILOBYTE("nbteditor.config.item_size.kilobyte", 1000, false),
        MEGABYTE("nbteditor.config.item_size.megabyte", 1000000, false),
        GIGABYTE("nbteditor.config.item_size.gigabyte", 1000000000, false),
        BYTE_COMPRESSED("nbteditor.config.item_size.byte_compressed", 1, true),
        KILOBYTE_COMPRESSED("nbteditor.config.item_size.kilobyte_compressed", 1000, true),
        MEGABYTE_COMPRESSED("nbteditor.config.item_size.megabyte_compressed", 1000000, true),
        GIGABYTE_COMPRESSED("nbteditor.config.item_size.gigabyte_compressed", 1000000000, true);

        private final Text label;
        @Getter
        private final int magnitude;
        @Getter
        private final boolean compressed;

        ItemSizeFormat(String key, int magnitude, boolean compressed) {
            this.label = TextInst.translatable(key);
            this.magnitude = magnitude;
            this.compressed = compressed;
        }

        @Override
        public String toString() {
            return label.getString();
        }
    }

    public record Alias(String original, String alias) {
    }

}
