package me.m0dii.nbteditor.screens;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.integrations.NBTAutocompleteIntegration;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.nbtfolder.NBTFolder;
import me.m0dii.nbteditor.screens.nbtfolder.StringNBTFolder;
import me.m0dii.nbteditor.screens.util.FancyConfirmScreen;
import me.m0dii.nbteditor.screens.util.TextAreaScreen;
import me.m0dii.nbteditor.screens.widgets.*;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.NbtFormatter;
import me.m0dii.nbteditor.util.TextUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;


public class NBTEditorScreen<L extends LocalNBT> extends LocalEditorScreen<L> {

    private static String copiedKey;
    private static NbtElement copiedValue;

    private final NBTFolder<NbtCompound> baseFolder;

    private NamedTextFieldWidget type;
    private NamedTextFieldWidget count;

    private NamedTextFieldWidget path;
    private SuggestingTextFieldWidget value;
    private List2D editor;
    private final Map<String, Integer> scrollPerFolder;

    private final List<String> realPath;
    private NBTFolder<?> currentFolder;
    private NBTValue upValue;
    private NBTValue selectedValue;
    private boolean json;

    @SuppressWarnings({"serial", "deprecation"})
    public NBTEditorScreen(NBTReference<L> ref) {
        super(TextInst.of("NBT Editor"), ItemReference.toItemPartsRef(ref));

        scrollPerFolder = new HashMap<>();

        realPath = new ArrayList<>() {
            @Override
            public String toString() {
                return String.join("/", this);
            }
        };
        baseFolder = NBTFolder.get(NbtCompound.class, localNBT::getOrCreateNBT, localNBT::setNBT);
    }

    @Override
    protected void initEditor() {
        if (realPath.isEmpty() && baseFolder.hasEmptyKey()) {
            client.setScreen(new FancyConfirmScreen(val -> {
                if (val) {
                    baseFolder.removeKey("");
                    save();
                    client.setScreen(this);
                } else {
                    close();
                }
            }, TextInst.translatable("nbteditor.nbt.empty_key.title"), TextInst.translatable("nbteditor.nbt.empty_key.desc"),
                    TextInst.translatable("nbteditor.nbt.empty_key.yes"), TextInst.translatable("nbteditor.nbt.empty_key.no"))
                    .setParent(null));

            return;
        }

        name.setChangedListener(str -> {
            if (str.equals(localNBT.getDefaultName())) {
                localNBT.setName(null);
            } else {
                localNBT.setName(TextInst.of(str));
            }

            genEditor();
        });

        addDrawableChild(MVMisc.newButton(16, height - 16 * 2, 20, 20, TextInst.translatable("nbteditor.nbt.add"), btn -> {
            add();
        }));
        addDrawableChild(MVMisc.newButton(16 + 16 + 8, height - 16 * 2, 20, 20, TextInst.translatable("nbteditor.nbt.remove"), btn -> {
            remove();
        }));
        addDrawableChild(MVMisc.newButton(16 + (16 + 8) * 2, height - 16 * 2, 48, 20, TextInst.translatable("nbteditor.nbt.copy"), btn -> {
            copy();
        }));
        addDrawableChild(MVMisc.newButton(16 + (16 + 8) * 2 + (48 + 4), height - 16 * 2, 48, 20, TextInst.translatable("nbteditor.nbt.cut"), btn -> {
            cut();
        }));
        addDrawableChild(MVMisc.newButton(16 + (16 + 8) * 2 + (48 + 4) * 2, height - 16 * 2, 48, 20, TextInst.translatable("nbteditor.nbt.paste"), btn -> {
            paste();
        }));
        addDrawableChild(MVMisc.newButton(16 + (16 + 8) * 2 + (48 + 4) * 3, height - 16 * 2, 48, 20, TextInst.translatable("nbteditor.nbt.rename"), btn -> {
            rename();
        }));


        Set<Identifier> allTypes = localNBT.getIdOptions();
        type = new NamedTextFieldWidget(16 + (32 + 8) * 2, 16 + 8 + 32, 208, 16).name(TextInst.translatable("nbteditor.nbt.identifier"));
        type.setMaxLength(Integer.MAX_VALUE);
        type.setText(localNBT.getId().toString());
        if (allTypes == null) {
            type.setEditable(false);
        } else {
            type.setChangedListener(str -> {
                Identifier id;
                try {
                    id = IdentifierInst.of(str);
                } catch (InvalidIdentifierException e) {
                    return;
                }

                if (!allTypes.contains(id)) {
                    return;
                }

                if (!ConfigScreen.isAirEditable() && localNBT.isEmpty(id)) {
                    return;
                }

                localNBT.setId(id);
                if (localNBT instanceof LocalItem item && item.getCount() == 0) {
                    item.setCount(count.getText().isEmpty() || count.getText().equals("+") ? 1 : Integer.parseInt(count.getText()));
                }

                genEditor();
            });
        }
        addDrawableChild(type);

        count = new NamedTextFieldWidget(16, 16 + 8 + 32, 72, 16).name(TextInst.translatable("nbteditor.nbt.count"));
        count.setMaxLength(Integer.MAX_VALUE);
        if (localNBT instanceof LocalItem item) {
            count.setText((ConfigScreen.isAirEditable() ? Math.max(1, item.getCount()) : item.getCount()) + "");
            count.setChangedListener(str -> {
                if (str.isEmpty() || str.equals("+")) {
                    return;
                }

                item.setCount(Integer.parseInt(str));
                checkSave();
            });
            count.setTextPredicate(MiscUtil.intPredicate(1, Integer.MAX_VALUE, true));
        } else {
            count.setText("1");
            count.setEditable(false);
        }
        addDrawableChild(count);

        path = new NamedTextFieldWidget(16, 16 + 8 + 32 + 16 + 8, 288, 16).name(TextInst.translatable("nbteditor.nbt.path"));
        path.setMaxLength(Integer.MAX_VALUE);
        path.setText(realPath.toString());
        path.setChangedListener(str -> {
            String[] parts = str.split("/");
            NBTFolder<?> folder = this.baseFolder;
            for (String part : parts) {
                folder = folder.getSubFolder(part);
                if (folder == null) {
                    return;
                }
            }
            realPath.clear();
            realPath.addAll(Arrays.asList(parts));
            genEditor();
        });
        addDrawableChild(path);

        value = new SuggestingTextFieldWidget(this, 16, 16 + 8 + 32 + (16 + 8) * 2, 288, 16).name(TextInst.translatable("nbteditor.nbt.value"));
        value.setRenderTextProvider((str, index) -> {
            return TextUtil.substring(NbtFormatter.FORMATTER.formatSafely(value.getText()).text(), index, index + str.length()).asOrderedText();
        });
        value.setMaxLength(Integer.MAX_VALUE);
        value.setText("");
        value.setEditable(false);
        value.setChangedListener(str -> {
            if (selectedValue != null) {
                selectedValue.setUnsafe(!NbtFormatter.FORMATTER.formatSafely(value.getText()).isSuccess());
                if (selectedValue.isUnsafe()) {
                    return;
                }
                selectedValue.valueChanged(str, nbt -> {
                    currentFolder.setValue(selectedValue.getKey(), nbt);
                    updateName();
                });
                if (realPath.isEmpty()) {
                    for (List2D.List2DValue element : editor.getElements()) {
                        if (element instanceof NBTValue val) {
                            val.updateInvalidComponent(localNBT, null);
                        }
                    }
                } else {
                    upValue.updateInvalidComponent(localNBT, realPath.getFirst());
                }
                checkSave();
            }
        });
        value.suggest((str, cursor) -> NBTAutocompleteIntegration.INSTANCE
                .filter(ac -> selectedValue != null)
                .map(ac -> ac.getSuggestions(localNBT, realPath, selectedValue.getKey(), str, cursor))
                .orElseGet(() -> new SuggestionsBuilder("", 0).buildFuture()));

        addDrawableChild(value);

        addDrawableChild(MVMisc.newButton(16 + 288 + 10, 16 + 8 + 32 + (16 + 8) * 2 - 2, 75, 20, TextInst.translatable("nbteditor.nbt.value_expand"), btn -> {
            if (selectedValue == null) {
                client.setScreen(new TextAreaScreen(this, currentFolder.getNBT().toString(), NbtFormatter.FORMATTER, false, str -> {
                    try {
                        NbtElement nbt = MixinLink.parseSpecialElement(new StringReader(str));
                        if (realPath.isEmpty()) {
                            if (!(nbt instanceof NbtCompound)) {
                                NbtCompound temp = new NbtCompound();
                                temp.put("value", nbt);
                                nbt = temp;
                            }
                            baseFolder.setNBT((NbtCompound) nbt);
                        } else {
                            String lastPathPart = realPath.remove(realPath.size() - 1);
                            genEditor();
                            currentFolder.setValue(lastPathPart, nbt);
                            realPath.add(lastPathPart);
                        }
                    } catch (CommandSyntaxException e) {
                        M0DevTools.LOGGER.error("Error parsing nbt from Expand", e);
                    }
                }).suggest((str, cursor) -> NBTAutocompleteIntegration.INSTANCE
                        .map(ac -> ac.getSuggestions(localNBT, realPath, null, str, cursor))
                        .orElseGet(() -> new SuggestionsBuilder("", 0).buildFuture())));
            } else {
                client.setScreen(new TextAreaScreen(this, selectedValue.getValueText(json), NbtFormatter.FORMATTER,
                        false, str -> value.setText(str)).suggest((str, cursor) -> NBTAutocompleteIntegration.INSTANCE
                        .map(ac -> ac.getSuggestions(localNBT, realPath, selectedValue.getKey(), str, cursor))
                        .orElseGet(() -> new SuggestionsBuilder("", 0).buildFuture())));
            }
        }));

        final int editorY = 16 + 8 + 32 + (16 + 8) * 3;
        editor = new List2D(16, editorY, width - 16 * 2, height - editorY - 16 * 2 - 8, 4, 32, 32, 8)
                .setFinalEventHandler(new MVElement() {
                    @Override
                    public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        selectedValue = null;
                        value.setText("");
                        value.setEditable(false);
                        return true;
                    }
                });
        genEditor();
        addSelectableChild(editor);
    }

    private void genEditor() {
        checkSave();

        selectedValue = null;
        value.setText("");
        value.setEditable(false);

        updateName();

        editor.clearElements();

        json = false;
        currentFolder = baseFolder;
        Iterator<String> keys = realPath.iterator();
        boolean removing = false;
        while (keys.hasNext()) {
            String key = keys.next();
            if (removing) {
                keys.remove();
                continue;
            }
            NBTFolder<?> folder = currentFolder.getSubFolder(key);
            if (folder != null) {
                currentFolder = folder;
                if (currentFolder instanceof StringNBTFolder) {
                    json = true;
                }
            } else {
                keys.remove();
                removing = true;
            }
        }

        if (removing) {
            MiscUtil.setTextFieldValueSilently(path, realPath.toString(), true);
        }

        if (realPath.isEmpty()) {
            upValue = null;
        } else {
            upValue = new NBTValue(this, null, null);
            upValue.updateInvalidComponent(localNBT, realPath.getFirst());
            editor.addElement(upValue);
        }

        List<NBTValue> elements = currentFolder.getEntries(this);
        if (elements == null) {
            selectNbt(null, true);
            return;
        } else {
            if (realPath.isEmpty()) {
                elements.forEach(element -> element.updateInvalidComponent(localNBT, null));
            }
            elements.sort((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()));
            elements.forEach(editor::addElement);
        }

        editor.setScroll(Math.max(editor.getMaxScroll(), scrollPerFolder.computeIfAbsent(realPath.toString(), key -> 0)));
    }

    private void updateName() {
        String newName = localNBT.getName().getString();

        if (!name.text.equals(newName)) {
            MiscUtil.setTextFieldValueSilently(name, newName, false);
        }
    }

    @Override
    protected boolean isNameEditable() {
        return true;
    }

    void selectNbt(NBTValue key, boolean isFolder) {
        //  true when clicking the back button and entering a "folder"
        if (isFolder) {
            if (key == null) {
                realPath.removeLast();
            } else {
                realPath.add(key.getKey());
            }

            selectedValue = null;
            value.setText("");
            value.setEditable(false);
            MiscUtil.setTextFieldValueSilently(path, realPath.toString(), true);
            genEditor();
        } else {
            // We are in a "folder" and we selected the key, for ex. "minecraft:block_state" or "facing"
            selectedValue = key;
            value.setText(key.getValueText(json));
            value.setEditable(true);
        }
    }

    @Override
    protected void preRenderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MVTooltip.setOneTooltip(true, false);
        editor.render(matrices, mouseX, mouseY, delta); // So the tab completion renders on top correctly
        MVTooltip.renderOneTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (NBTAutocompleteIntegration.INSTANCE.isEmpty()) {
            renderTip(matrices, "nbteditor.nbt_ac.tip");
        }
    }

    @Override
    protected boolean save() {
        if (localNBT.isEmpty() && localNBT.getNBT() != null && !localNBT.getNBT().isEmpty()) {
            MiscUtil.client.setScreen(new FancyConfirmScreen(val -> {
                if (val) {
                    super.save();
                }

                MiscUtil.client.setScreen(this);
            }, TextInst.translatable("nbteditor.nbt.saving_air.title"), TextInst.translatable("nbteditor.nbt.saving_air.desc"),
                    TextInst.translatable("nbteditor.nbt.saving_air.yes"), TextInst.translatable("nbteditor.nbt.saving_air.no"))
                    .setParent(this));
            return false;
        }

        if (localNBT instanceof LocalItem && localNBT.getNBT() != null) {
            List<NBTValue> elements = baseFolder.getEntries(this);
            elements.forEach(element -> element.updateInvalidComponent(localNBT, null));
            if (elements.stream().anyMatch(NBTValue::isInvalidComponent)) {
                MiscUtil.client.setScreen(new FancyConfirmScreen(val -> {
                    if (val) {
                        super.save();
                    }

                    MiscUtil.client.setScreen(this);
                }, TextInst.translatable("nbteditor.nbt.saving_invalid_components.title"), TextInst.translatable("nbteditor.nbt.saving_invalid_components.desc"),
                        TextInst.translatable("nbteditor.nbt.saving_invalid_components.yes"), TextInst.translatable("nbteditor.nbt.saving_invalid_components.no"))
                        .setParent(this));

                return false;
            }
        }

        return super.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getOverlay() != null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return type.keyPressed(keyCode, scanCode, modifiers) || type.isActive() ||
                count.keyPressed(keyCode, scanCode, modifiers) || count.isActive() ||
                path.keyPressed(keyCode, scanCode, modifiers) || path.isActive() ||
                value.keyPressed(keyCode, scanCode, modifiers) || value.isActive() || keyPressed2(keyCode, scanCode, modifiers);
    }

    private boolean keyPressed2(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            remove();
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (!realPath.isEmpty()) {
                selectNbt(null, true);
            }
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_C -> copy();
                case GLFW.GLFW_KEY_X -> cut();
                case GLFW.GLFW_KEY_V -> paste();
                case GLFW.GLFW_KEY_R -> rename();
                case GLFW.GLFW_KEY_N -> add();
                default -> {
                    // No-op
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
        boolean output = super.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
        scrollPerFolder.put(realPath.toString(), editor.getScroll());
        return output;
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        if (!(currentFolder.getNBT() instanceof NbtCompound)) {
            return;
        }

        for (Path currPath : paths) {
            File file = currPath.toFile();
            if (file.isFile() && file.getName().endsWith(".nbt")) {
                try (FileInputStream in = new FileInputStream(file)) {
                    NbtCompound nbt = MiscUtil.readNBT(in);

                    for (String key : nbt.getKeys()) {
                        currentFolder.setValue(key, nbt.get(key));
                    }

                    genEditor();
                } catch (Exception e) {
                    M0DevTools.LOGGER.error("Error while importing a .nbt file", e);
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private void add() {
        getNextKey(Optional.empty(), key -> {
            currentFolder.addKey(key);
            genEditor();
        }, false);
    }

    private void remove() {
        if (selectedValue != null) {
            currentFolder.removeKey(selectedValue.getKey());
            genEditor();
        }
    }

    private void copy() {
        if (selectedValue != null) {
            copiedKey = selectedValue.getKey();
            copiedValue = currentFolder.getValue(selectedValue.getKey()).copy();
        }
    }

    private void cut() {
        if (selectedValue != null) {
            copiedKey = selectedValue.getKey();
            copiedValue = currentFolder.getValue(selectedValue.getKey()).copy();

            currentFolder.removeKey(selectedValue.getKey());
            genEditor();
        }
    }

    private void paste() {
        if (copiedKey != null) {
            getNextKey(Optional.of(copiedKey), key -> {
                currentFolder.addKey(key);
                currentFolder.setValue(key, copiedValue.copy());
                genEditor();
            }, false);
        }
    }

    private void rename() {
        if (selectedValue != null) {
            String selectedKey = selectedValue.getKey();
            NbtElement currValue = currentFolder.getValue(selectedKey);

            getKey(selectedKey, key -> promptForDuplicateKey(key, key2 -> {
                currentFolder.removeKey(selectedKey);
                currentFolder.addKey(key2);
                currentFolder.setValue(key2, currValue);
                genEditor();
            }), true);
        }
    }

    private void getKey(String defaultValue, Consumer<String> keyConsumer, boolean renaming) {
        InputOverlay.show(
                TextInst.translatable("nbteditor.nbt.key"),
                StringInput.builder()
                        .withDefault(defaultValue)
                        .withValidator(str -> !str.isEmpty() && currentFolder.getKeyValidator(renaming).test(str))
                        .withSuggestions((str, cursor) -> NBTAutocompleteIntegration.INSTANCE
                                .map(autoComplete -> autoComplete.getSuggestions(localNBT, realPath, str, null, cursor,
                                        currentFolder.getEntries(this).stream()
                                                .map(NBTValue::getKey)
                                                .toList()))
                                .orElseGet(() -> new SuggestionsBuilder("", 0).buildFuture()))
                        .build(),
                keyConsumer);
    }

    private void getKey(Consumer<String> keyConsumer, boolean renaming) {
        getKey(null, keyConsumer, renaming);
    }

    private void promptForDuplicateKey(String key, Consumer<String> keyConsumer) {
        if (currentFolder.handlesDuplicateKeys() || currentFolder.getValue(key) == null) {
            keyConsumer.accept(key);
            return;
        }

        client.setScreen(new FancyConfirmScreen(val -> {
            if (val) {
                keyConsumer.accept(key);
            }

            client.setScreen(this);
        }, TextInst.translatable("nbteditor.nbt.overwrite.title"), TextInst.translatable("nbteditor.nbt.overwrite.desc"),
                TextInst.translatable("nbteditor.nbt.overwrite.yes"), TextInst.translatable("nbteditor.nbt.overwrite.no")));
    }

    private void getNextKey(Optional<String> pastingKey, Consumer<String> keyConsumer, boolean renaming) {
        currentFolder.getNextKey(pastingKey).ifPresentOrElse(
                key -> promptForDuplicateKey(key, keyConsumer),
                () -> getKey(key -> promptForDuplicateKey(key, keyConsumer), renaming));
    }
}