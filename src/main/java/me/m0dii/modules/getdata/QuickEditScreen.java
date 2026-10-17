package me.m0dii.modules.getdata;

import me.m0dii.gui.local.UiTheme;
import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.utils.NbtEditorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Friendly editor for the common fields: an entity's name, health and flags, or a block's state properties and
 * container name. Shows the exact commands before sending them, and needs permission for {@code /data} and
 * {@code /setblock} on the server.
 */
public class QuickEditScreen extends Screen implements GetDataTargetView, ThemedScreen {
    private static final int ROW_H = 22;

    private final Screen parent;
    private final String targetName;
    private final String targetToken;
    private CompoundTag payload;
    private boolean serverSynced;

    // entity
    private @Nullable Entity entity;
    private QuickEditCommands.EntityValues entityBefore;
    private final Map<String, Boolean> flags = new LinkedHashMap<>();
    private EditBox nameBox;
    private EditBox healthBox;

    // block
    private @Nullable BlockPos blockPos;
    private final Map<String, List<String>> propertyValues = new LinkedHashMap<>();
    private final Map<String, String> propertiesBefore = new LinkedHashMap<>();
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final List<Button> propertyButtons = new ArrayList<>();
    private String blockNameBefore = "";
    private boolean hasBlockEntity;
    private boolean confirmRisky;

    private List<String> pendingCommands = List.of();
    private String error;
    private String status = "";
    private Button applyButton;

    private QuickEditScreen(Screen parent, String targetName, String targetToken, CompoundTag payload) {
        super(Component.literal("Quick Edit"));
        this.parent = parent;
        this.targetName = targetName;
        this.targetToken = targetToken;
        this.payload = payload == null ? new CompoundTag() : payload;
    }

    public static Screen create(Screen parent, String targetName, String targetToken, String payload) {
        CompoundTag parsed = NbtEditorUtils.parseCompoundOrNull(payload);
        return new QuickEditScreen(parent, targetName, targetToken, parsed);
    }

    private boolean isBlock() {
        return this.targetToken.startsWith("block ");
    }

    @Override
    protected void init() {
        super.init();
        this.propertyButtons.clear();
        if (isBlock()) {
            initBlock();
        } else {
            initEntity();
        }
        int bottom = this.height - 30;
        this.applyButton = this.addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
                .bounds(12, bottom, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("NBT browser"), b -> openBrowser())
                .bounds(86, bottom, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 72, bottom, 60, 20).build());
        recompute();
    }

    // ---- entity ------------------------------------------------------------------------------------------

    private void initEntity() {
        this.entity = findEntity();
        String name = this.entity != null && this.entity.getCustomName() != null
                ? this.entity.getCustomName().getString()
                : tagText(this.payload.get("CustomName"));
        String health = null;
        if (this.payload.contains("Health")) {
            health = this.payload.getFloat("Health").map(QuickEditScreen::shortFloat).orElse(null);
        }
        if (this.flags.isEmpty()) {
            for (String flag : QuickEditCommands.ENTITY_FLAGS) {
                boolean value = this.payload.getBoolean(flag).orElse(false);
                if ("Glowing".equals(flag) && this.entity != null) {
                    value = value || this.entity.hasGlowingTag();
                }
                this.flags.put(flag, value);
            }
            this.entityBefore = new QuickEditCommands.EntityValues(name, health, this.flags);
        }

        int y = 44;
        this.nameBox = new EditBox(this.font, 90, y, 220, 18, Component.literal("Name"));
        this.nameBox.setMaxLength(200);
        this.nameBox.setValue(this.entityBefore.name());
        this.nameBox.setHint(Component.literal("No custom name"));
        this.nameBox.setResponder(v -> recompute());
        this.addRenderableWidget(this.nameBox);
        y += ROW_H;
        this.healthBox = null;
        if (this.entityBefore.health() != null) {
            this.healthBox = new EditBox(this.font, 90, y, 70, 18, Component.literal("Health"));
            this.healthBox.setValue(this.entityBefore.health());
            this.healthBox.setResponder(v -> recompute());
            this.addRenderableWidget(this.healthBox);
            y += ROW_H;
        }
        y += 4;
        int x = 12;
        for (String flag : QuickEditCommands.ENTITY_FLAGS) {
            Button button = Button.builder(Component.literal(""), b -> {
                        this.flags.put(flag, !Boolean.TRUE.equals(this.flags.get(flag)));
                        recompute();
                    })
                    .bounds(x, y, 140, 20).build();
            this.addRenderableWidget(button);
            this.propertyButtons.add(button);
            x += 146;
            if (x + 140 > this.width - 12) {
                x = 12;
                y += ROW_H;
            }
        }
    }

    private @Nullable Entity findEntity() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        String selector = this.targetToken.substring("entity ".length()).trim();
        for (Entity candidate : client.level.entitiesForRendering()) {
            if (candidate.getStringUUID().equals(selector)) {
                return candidate;
            }
        }
        return null;
    }

    // ---- block -------------------------------------------------------------------------------------------

    private void initBlock() {
        Minecraft client = Minecraft.getInstance();
        this.blockPos = parseBlockPos();
        if (client.level == null || this.blockPos == null) {
            this.error = "Block is not loaded.";
            return;
        }
        BlockState state = client.level.getBlockState(this.blockPos);
        this.hasBlockEntity = client.level.getBlockEntity(this.blockPos) != null;
        if (this.propertiesBefore.isEmpty()) {
            for (Property<?> property : state.getProperties()) {
                this.propertyValues.put(property.getName(), valueNames(property));
                this.propertiesBefore.put(property.getName(), currentValue(state, property));
            }
            this.properties.putAll(this.propertiesBefore);
            this.blockNameBefore = tagText(this.payload.get("CustomName"));
        }

        int y = 44;
        if (this.hasBlockEntity) {
            this.nameBox = new EditBox(this.font, 90, y, 220, 18, Component.literal("Name"));
            this.nameBox.setMaxLength(200);
            this.nameBox.setValue(this.blockNameBefore);
            this.nameBox.setHint(Component.literal("Default name"));
            this.nameBox.setResponder(v -> recompute());
            this.addRenderableWidget(this.nameBox);
            y += ROW_H + 4;
        }
        int x = 12;
        for (String key : this.propertyValues.keySet()) {
            Button button = Button.builder(Component.literal(""), b -> {
                        this.properties.put(key, QuickEditCommands.cycle(this.propertyValues.get(key), this.properties.get(key), 1));
                        recompute();
                    })
                    .bounds(x, y, 180, 20).build();
            this.addRenderableWidget(button);
            this.propertyButtons.add(button);
            x += 186;
            if (x + 180 > this.width - 12) {
                x = 12;
                y += ROW_H;
            }
        }
    }

    private static <T extends Comparable<T>> List<String> valueNames(Property<T> property) {
        List<String> names = new ArrayList<>();
        for (T value : property.getPossibleValues()) {
            names.add(property.getName(value));
        }
        return names;
    }

    private static <T extends Comparable<T>> String currentValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private @Nullable BlockPos parseBlockPos() {
        String[] parts = this.targetToken.substring("block ".length()).trim().split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- commands ----------------------------------------------------------------------------------------

    private void recompute() {
        this.error = isBlock() && this.blockPos == null ? "Block is not loaded." : null;
        this.confirmRisky = false;
        try {
            this.pendingCommands = isBlock() ? blockCommands() : entityCommands();
        } catch (IllegalArgumentException e) {
            this.pendingCommands = List.of();
            this.error = e.getMessage();
        }
        updateLabels();
    }

    private List<String> entityCommands() {
        if (this.entityBefore == null) {
            return List.of();
        }
        QuickEditCommands.EntityValues after = new QuickEditCommands.EntityValues(
                this.nameBox == null ? this.entityBefore.name() : this.nameBox.getValue(),
                this.healthBox == null ? null : this.healthBox.getValue(),
                this.flags);
        return QuickEditCommands.entity(this.targetToken, this.entityBefore, after);
    }

    private List<String> blockCommands() {
        if (this.blockPos == null) {
            return List.of();
        }
        List<String> commands = new ArrayList<>();
        String coords = this.blockPos.getX() + " " + this.blockPos.getY() + " " + this.blockPos.getZ();
        String newName = this.nameBox == null ? this.blockNameBefore : this.nameBox.getValue().strip();
        boolean nameChanged = !newName.equals(this.blockNameBefore.strip());
        boolean stateChanged = !this.properties.equals(this.propertiesBefore);

        CompoundTag merge = new CompoundTag();
        if (stateChanged) {
            Minecraft client = Minecraft.getInstance();
            String id = client.level == null ? "" : String.valueOf(BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(this.blockPos).getBlock()));
            commands.add("setblock " + coords + " " + id + QuickEditCommands.stateSuffix(this.properties));
            if (this.hasBlockEntity) {
                // setblock recreates the block entity, so put its data back.
                for (String key : this.payload.keySet()) {
                    if (!List.of("id", "x", "y", "z", "Properties", "CustomName").contains(key)) {
                        Tag value = this.payload.get(key);
                        if (value != null) {
                            merge.put(key, value.copy());
                        }
                    }
                }
                if (!nameChanged && this.payload.get("CustomName") != null) {
                    merge.put("CustomName", this.payload.get("CustomName").copy());
                }
            }
        }
        if (nameChanged && !newName.isEmpty()) {
            merge.put("CustomName", net.minecraft.nbt.StringTag.valueOf(newName));
        }
        if (!merge.isEmpty()) {
            commands.add("data merge block " + coords + " " + merge);
        }
        // After a setblock the recreated block entity has no name anyway.
        if (nameChanged && newName.isEmpty() && !(stateChanged && this.hasBlockEntity)) {
            commands.add("data remove block " + coords + " CustomName");
        }
        return commands;
    }

    private void apply() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || this.pendingCommands.isEmpty() || this.error != null) {
            return;
        }
        boolean risky = isBlock() && this.hasBlockEntity && !this.serverSynced && !this.properties.equals(this.propertiesBefore);
        if (risky && !this.confirmRisky) {
            this.confirmRisky = true;
            this.status = "Changing the state recreates this block's stored data from what your client knows, which may"
                    + " be incomplete (e.g. chest contents). Press 'Apply anyway' to continue.";
            updateLabels();
            return;
        }
        for (String command : this.pendingCommands) {
            client.player.connection.sendCommand(command);
        }
        int sent = this.pendingCommands.size();
        // The sent state becomes the new baseline.
        if (isBlock()) {
            this.propertiesBefore.clear();
            this.propertiesBefore.putAll(this.properties);
            this.blockNameBefore = this.nameBox == null ? this.blockNameBefore : this.nameBox.getValue().strip();
        } else {
            this.entityBefore = new QuickEditCommands.EntityValues(
                    this.nameBox == null ? "" : this.nameBox.getValue(),
                    this.healthBox == null ? null : this.healthBox.getValue(),
                    this.flags);
        }
        recompute();
        this.status = "Sent " + sent + " command(s). Reopen to see the result from the server.";
    }

    private void updateLabels() {
        if (isBlock()) {
            int i = 0;
            for (String key : this.propertyValues.keySet()) {
                if (i < this.propertyButtons.size()) {
                    boolean changed = !this.properties.get(key).equals(this.propertiesBefore.get(key));
                    this.propertyButtons.get(i).setMessage(Component.literal((changed ? "• " : "") + key + ": " + this.properties.get(key)));
                }
                i++;
            }
        } else {
            int i = 0;
            for (String flag : QuickEditCommands.ENTITY_FLAGS) {
                if (i < this.propertyButtons.size()) {
                    boolean on = Boolean.TRUE.equals(this.flags.get(flag));
                    this.propertyButtons.get(i).setMessage(Component.literal((on ? "✔ " : "✘ ") + flag));
                }
                i++;
            }
        }
        if (this.applyButton != null) {
            this.applyButton.active = !this.pendingCommands.isEmpty() && this.error == null;
            this.applyButton.setMessage(Component.literal(this.confirmRisky ? "Apply anyway" : "Apply"));
        }
    }

    private void openBrowser() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(GetDataBrowserScreen.create(this.parent, this.targetName, this.targetToken, this.payload.toString()));
        }
    }

    // ---- rendering ---------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, UiTheme.BG);
        context.text(this.font, "Quick Edit · " + this.targetName + (this.serverSynced ? "  (server data)" : ""), 12, 12, UiTheme.TEXT);
        context.text(this.font, isBlock()
                        ? "Click a property to cycle its value. Needs permission for /setblock and /data."
                        : "Values are what your client knows; server-only ones (Invulnerable, PersistenceRequired) may show off.",
                12, 26, UiTheme.TEXT_MUTED);
        if (this.nameBox != null) {
            context.text(this.font, isBlock() ? "Container name" : "Name", 12, this.nameBox.getY() + 5, UiTheme.TEXT);
        }
        if (this.healthBox != null) {
            context.text(this.font, "Health", 12, this.healthBox.getY() + 5, UiTheme.TEXT);
        }

        int previewTop = this.height - 44 - Math.max(1, this.pendingCommands.size()) * 10 - 12;
        if (this.error != null) {
            context.text(this.font, this.error, 12, previewTop, 0xFFFF7070);
        } else if (this.pendingCommands.isEmpty()) {
            context.text(this.font, "No changes yet.", 12, previewTop, UiTheme.TEXT_MUTED);
        } else {
            context.text(this.font, "Will send:", 12, previewTop, UiTheme.TEXT_MUTED);
            for (int i = 0; i < this.pendingCommands.size(); i++) {
                String line = this.font.plainSubstrByWidth("/" + this.pendingCommands.get(i), this.width - 30);
                context.text(this.font, line, 18, previewTop + 12 + i * 10, 0xFFFFE08A);
            }
        }
        if (!this.status.isEmpty()) {
            context.text(this.font, this.font.plainSubstrByWidth(this.status, this.width - 200), 180, this.height - 24, 0xFF9AD0FF);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        // Right-click cycles a block property backwards.
        if (click.button() == 1 && isBlock()) {
            int i = 0;
            for (String key : this.propertyValues.keySet()) {
                if (i < this.propertyButtons.size() && this.propertyButtons.get(i).isMouseOver(click.x(), click.y())) {
                    this.properties.put(key, QuickEditCommands.cycle(this.propertyValues.get(key), this.properties.get(key), -1));
                    recompute();
                    return true;
                }
                i++;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean matchesTarget(String token) {
        return this.targetToken.equals(token);
    }

    @Override
    public void applySyncedPayload(String syncedPayload) {
        CompoundTag parsed = NbtEditorUtils.parseCompoundOrNull(syncedPayload);
        if (parsed != null) {
            this.payload = parsed;
            this.serverSynced = true;
            if (isBlock() && this.nameBox != null && this.nameBox.getValue().equals(this.blockNameBefore)) {
                this.blockNameBefore = tagText(parsed.get("CustomName"));
                this.nameBox.setValue(this.blockNameBefore);
            }
            recompute();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------------

    /** Plain text of a name tag: a string, or a text component's "text" field. */
    private static String tagText(@Nullable Tag tag) {
        if (tag == null) {
            return "";
        }
        if (tag instanceof CompoundTag compound) {
            return compound.getString("text").orElse("");
        }
        return tag.asString().orElse("");
    }

    private static String shortFloat(float value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
    }
}
