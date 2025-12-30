package me.m0dii.nbteditor.screens.factories;

import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.LocalEditorScreen;
import me.m0dii.nbteditor.screens.configurable.*;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnchantmentsScreen extends LocalEditorScreen<LocalItem> {

    private final ConfigList config;
    private ConfigPanel panel;

    public EnchantmentsScreen(@NotNull ItemReference ref) {
        super(TextInst.of("Enchantments"), ref);

        MVRegistry<Enchantment> registry = MVRegistry.getEnchantmentRegistry();
        Map<String, Enchantment> allEnchantments = registry.getEntrySet().stream()
                .map(enchant -> Map.entry(enchant.getKey().toString(), enchant.getValue()))
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        ItemStack inputItem = ref.getItem();
        ConfigCategory entry = new ConfigCategory();
        List<String> orderedEnchants = allEnchantments.entrySet().stream()
                .map(enchant -> Map.entry(enchant.getKey(), enchant.getValue().isAcceptableItem(inputItem)))
                .sorted((a, b) -> {
                    if (Boolean.TRUE.equals(a.getValue())) {
                        if (Boolean.FALSE.equals(b.getValue())) {
                            return -1;
                        }
                    } else {
                        if (Boolean.TRUE.equals(b.getValue())) {
                            return 1;
                        }
                    }
                    return a.getKey().compareToIgnoreCase(b.getKey());
                })
                .map(Map.Entry::getKey)
                .toList();

        String firstEnchant = orderedEnchants.getFirst();
        entry.setConfigurable("enchantment", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.enchantment"),
                ConfigValueDropdown.forList(firstEnchant, firstEnchant, orderedEnchants,
                        allEnchantments.entrySet().stream().filter(enchant -> enchant.getValue().isAcceptableItem(inputItem)).map(Map.Entry::getKey).toList())));
        entry.setConfigurable("level", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.level"),
                ConfigValueNumber.forInt(1, 1, 1, 255)));
        config = new ConfigList(TextInst.translatable("nbteditor.enchantments"), false, entry);

        ItemTagReferences.ENCHANTMENTS.get(localNBT.getEditableItem()).enchants().forEach(enchant -> {
            ConfigCategory enchantConfig = entry.clone(true);
            getConfigEnchantment(enchantConfig).setValue(registry.getId(enchant.enchant()).toString());
            getConfigLevel(enchantConfig).setValue(enchant.level());
            config.addConfigurable(enchantConfig);
        });

        config.addValueListener(source -> {
            List<Enchants.EnchantWithLevel> newEnchants = new ArrayList<>();
            for (ConfigPath path : config.getConfigurables().values()) {
                ConfigCategory enchant = (ConfigCategory) path;
                newEnchants.add(new Enchants.EnchantWithLevel(
                        allEnchantments.get(getConfigEnchantment(enchant).getValidValue()),
                        getConfigLevel(enchant).getValidValue()));
            }
            ItemTagReferences.ENCHANTMENTS.set(localNBT.getEditableItem(), new Enchants(newEnchants));
            checkSave();
        });
    }

    @SuppressWarnings("unchecked")
    private static ConfigValueDropdown<String> getConfigEnchantment(ConfigCategory enchant) {
        return ((ConfigItem<ConfigValueDropdown<String>>) enchant.getConfigurable("enchantment")).getValue();
    }

    @SuppressWarnings("unchecked")
    private static ConfigValueNumber<Integer> getConfigLevel(ConfigCategory enchant) {
        return ((ConfigItem<ConfigValueNumber<Integer>>) enchant.getConfigurable("level")).getValue();
    }

    @Override
    protected void initEditor() {
        ConfigPanel newPanel = addDrawableChild(new ConfigPanel(16, 64, width - 32, height - 80, config));

        if (panel != null) {
            newPanel.setScroll(panel.getScroll());
        }

        panel = newPanel;
    }

    @Override
    protected void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderTip(matrices, "nbteditor.enchantments.tip");
    }

}
