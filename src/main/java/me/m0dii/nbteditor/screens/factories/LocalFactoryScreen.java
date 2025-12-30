package me.m0dii.nbteditor.screens.factories;

import me.m0dii.nbteditor.commands.OpenCommand;
import me.m0dii.nbteditor.commands.factories.AttributesCommand;
import me.m0dii.nbteditor.commands.factories.BlockStatesCommand;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.LocalEditorScreen;
import me.m0dii.nbteditor.screens.NBTEditorScreen;
import me.m0dii.nbteditor.screens.configurable.ConfigButton;
import me.m0dii.nbteditor.screens.configurable.ConfigCategory;
import me.m0dii.nbteditor.screens.configurable.ConfigPanel;
import me.m0dii.nbteditor.screens.containers.ContainerScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class LocalFactoryScreen<L extends LocalNBT> extends LocalEditorScreen<L> {

    public static final Identifier FACTORY_ICON = IdentifierInst.of("m0-dev-tools", "textures/factory.png");
    public static final List<LocalFactoryReference> BASIC_FACTORIES = new ArrayList<>();

    static {
        BASIC_FACTORIES.add(new LocalFactoryReference(TextInst.translatable("nbteditor.container"),
                OpenCommand.CONTAINER_FILTER, ContainerScreen::show));

        addFactory("m0-dev-tools", NBTEditorScreen::new);
        addFactory("nbteditor.display", DisplayScreen::new);
        addFactory("nbteditor.enchantments", EnchantmentsScreen::new, ItemReference.class);
        addFactory("nbteditor.attributes", AttributesCommand.ATTRIBUTES_FILTER, AttributesScreen::new);
        addFactory("nbteditor.block_states", BlockStatesCommand.BLOCK_FILTER, BlockStatesScreen::new);
    }

    private final ConfigCategory config;
    private ConfigPanel panel;

    public LocalFactoryScreen(NBTReference<L> ref) {
        super(TextInst.of("Factories"), ref);
        this.config = new ConfigCategory();

        for (LocalFactoryReference factory : BASIC_FACTORIES) {
            if (factory.supported().test(ref)) {
                ConfigButton button = new ConfigButton(150, factory.buttonText(),
                        btn -> factory.factory().accept(ref));

                this.config.setConfigurable(factory.buttonText().getString(), button);
            }
        }
    }

    private static void addFactory(String key, Predicate<NBTReference<?>> supported, Function<NBTReference<?>, Screen> screen) {
        BASIC_FACTORIES.add(new LocalFactoryReference(TextInst.translatable(key), supported,
                ref -> MiscUtil.client.setScreen(screen.apply(ref))));
    }

    private static <T extends NBTReference<?>> void addFactory(String key, Predicate<T> supported, Function<T, Screen> screen, Class<T> clazz) {
        addFactory(key, ref -> clazz.isInstance(ref) && supported.test(clazz.cast(ref)), ref -> screen.apply(clazz.cast(ref)));
    }

    private static void addFactory(String key, Function<NBTReference<?>, Screen> screen) {
        addFactory(key, ref -> true, screen);
    }

    private static <T extends NBTReference<?>> void addFactory(String key, Function<T, Screen> screen, Class<T> clazz) {
        addFactory(key, ref -> true, screen, clazz);
    }

    @Override
    protected boolean isSaveRequired() {
        return false;
    }

    @Override
    protected FactoryLink<L> getFactoryLink() {
        return null;
    }

    @Override
    protected void initEditor() {
        ConfigPanel newPanel = addDrawableChild(new ConfigPanel(16, 64, width - 32, height - 80, config));

        if (panel != null) {
            newPanel.setScroll(panel.getScroll());
        }

        panel = newPanel;
    }

    public record LocalFactoryReference(Text buttonText,
                                        Predicate<NBTReference<?>> supported,
                                        Consumer<NBTReference<?>> factory) {
    }

}
