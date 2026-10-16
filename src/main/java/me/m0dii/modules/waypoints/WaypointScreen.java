package me.m0dii.modules.waypoints;



import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaypointScreen extends Screen implements ThemedScreen {
    private static final int ENTRY_HEIGHT = 60;
    private static final int HEADER_HEIGHT = 60;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 18;

    private final Screen parent;
    private int scrollOffset = 0;
    private List<WaypointHandler.Waypoint> waypoints;

    // Edit mode variables
    private WaypointHandler.Waypoint editingWaypoint = null;
    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public WaypointScreen(Screen parent) {
        super(Component.literal("Waypoint Manager"));
        this.parent = parent;
    }

    public static WaypointScreen create(Screen parent) {
        return new WaypointScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        refreshWaypoints();

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(this.width / 2 - 155, this.height - 30, 100, 20)
                .build());

        // Add waypoint button
        this.addRenderableWidget(Button.builder(Component.literal("+ Add Waypoint"), button -> {
            WaypointHandler.addWaypoint();
            refreshWaypoints();
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // Refresh button
        this.addRenderableWidget(Button.builder(Component.literal("⟳ Refresh"), button -> {
            refreshWaypoints();
            editingWaypoint = null;
            clearEditFields();
        }).bounds(this.width / 2 + 55, this.height - 30, 100, 20).build());

        initEditFields();
    }

    private void initEditFields() {
        if (editingWaypoint != null) {
            int editPanelX = this.width - 220;
            int editPanelY = 80;

            // Name field
            nameField = new EditBox(this.font, editPanelX + 10, editPanelY + 25, 190, 18, Component.literal("Name"));
            nameField.setMaxLength(50);
            nameField.setValue(editingWaypoint.name);
            this.addRenderableWidget(nameField);

            // X coordinate field
            xField = new EditBox(this.font, editPanelX + 10, editPanelY + 60, 190, 18, Component.literal("X"));
            xField.setMaxLength(20);
            xField.setValue(String.format("%.2f", editingWaypoint.position.x));
            this.addRenderableWidget(xField);

            // Y coordinate field
            yField = new EditBox(this.font, editPanelX + 10, editPanelY + 85, 190, 18, Component.literal("Y"));
            yField.setMaxLength(20);
            yField.setValue(String.format("%.2f", editingWaypoint.position.y));
            this.addRenderableWidget(yField);

            // Z coordinate field
            zField = new EditBox(this.font, editPanelX + 10, editPanelY + 110, 190, 18, Component.literal("Z"));
            zField.setMaxLength(20);
            zField.setValue(String.format("%.2f", editingWaypoint.position.z));
            this.addRenderableWidget(zField);

            // Save button
            this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
                saveEditedWaypoint();
            }).bounds(editPanelX + 10, editPanelY + 140, 90, 20).build());

            // Cancel button
            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                editingWaypoint = null;
                clearEditFields();
                init();
            }).bounds(editPanelX + 110, editPanelY + 140, 90, 20).build());
        }
    }

    private void clearEditFields() {
        if (nameField != null) {
            this.removeWidget(nameField);
            nameField = null;
        }
        if (xField != null) {
            this.removeWidget(xField);
            xField = null;
        }
        if (yField != null) {
            this.removeWidget(yField);
            yField = null;
        }
        if (zField != null) {
            this.removeWidget(zField);
            zField = null;
        }
    }

    private void saveEditedWaypoint() {
        if (editingWaypoint == null) {
            return;
        }

        try {
            String newName = nameField.getValue().trim();
            double newX = Double.parseDouble(xField.getValue());
            double newY = Double.parseDouble(yField.getValue());
            double newZ = Double.parseDouble(zField.getValue());

            if (!newName.isEmpty()) {
                editingWaypoint.name = newName;
            }
            editingWaypoint.position = new Vec3(newX, newY, newZ);

            WaypointHandler.saveWaypoints();

            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.literal("✓ Saved waypoint ").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(editingWaypoint.name).withStyle(ChatFormatting.AQUA)));
            }

            editingWaypoint = null;
            clearEditFields();
            refreshWaypoints();
            init();
        } catch (NumberFormatException e) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.literal("❌ Invalid coordinates! Please enter valid numbers.").withStyle(ChatFormatting.RED));
            }
        }
    }

    private void refreshWaypoints() {
        this.waypoints = WaypointHandler.getWaypoints();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractBackground(context, mouseX, mouseY, delta);
        drawContents(context, mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawContents(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        String countText = String.format("Total Waypoints: %d", waypoints.size());
        context.centeredText(this.font,
                Component.literal(countText).withStyle(ChatFormatting.GRAY),
                this.width / 2, 25, 0xAAAAAA);

        if (waypoints.isEmpty()) {
            context.centeredText(this.font,
                    Component.literal("No waypoints yet. Click '+ Add Waypoint' to create one!").withStyle(ChatFormatting.YELLOW),
                    this.width / 2, this.height / 2, 0xFFFF55);
            return;
        }

        context.centeredText(this.font,
                Component.literal("Click a waypoint to teleport • Use buttons to manage").withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, 40, 0x888888);

        int listWidth = editingWaypoint != null ? this.width - 240 : this.width - 40;
        int startX = 20;
        int startIndex = scrollOffset / ENTRY_HEIGHT;
        int visibleEntries = (this.height - HEADER_HEIGHT - 40) / ENTRY_HEIGHT;

        for (int i = startIndex; i < Math.min(waypoints.size(), startIndex + visibleEntries + 1); i++) {
            WaypointHandler.Waypoint wp = waypoints.get(i);
            int entryY = HEADER_HEIGHT + (i * ENTRY_HEIGHT) - scrollOffset;

            if (entryY < HEADER_HEIGHT || entryY > this.height - 60) {
                continue;
            }

            drawWaypointEntry(context, wp, startX, entryY, listWidth, mouseX, mouseY);
        }

        if (editingWaypoint != null) {
            drawEditPanel(context);
        }

        if (waypoints.size() * ENTRY_HEIGHT > (this.height - HEADER_HEIGHT - 40)) {
            int totalPages = (waypoints.size() * ENTRY_HEIGHT + (this.height - HEADER_HEIGHT - 40) - 1) / (this.height - HEADER_HEIGHT - 40);
            int currentPage = (scrollOffset / (this.height - HEADER_HEIGHT - 40)) + 1;
            String scrollText = "Scroll for more (Page " + currentPage + "/" + totalPages + ")";
            context.centeredText(this.font,
                    Component.literal(scrollText).withStyle(ChatFormatting.DARK_GRAY),
                    this.width / 2, this.height - 50, 0x666666);
        }
    }

    private void drawWaypointEntry(GuiGraphicsExtractor context, WaypointHandler.Waypoint wp, int x, int y, int width, int mouseX, int mouseY) {
        int buttonAreaWidth = 150;
        int contentWidth = width - buttonAreaWidth;

        boolean isHovered = mouseX >= x && mouseX <= x + contentWidth
                && mouseY >= y && mouseY <= y + ENTRY_HEIGHT;
        boolean isEditing = editingWaypoint != null && editingWaypoint.id.equals(wp.id);

        int bgColor = isEditing ? 0x80FFA500 : (isHovered ? 0x60FFFFFF : 0x40000000);
        context.fill(x, y, x + contentWidth, y + ENTRY_HEIGHT, bgColor);

        int borderColor = isEditing ? 0xFFFFAA00 : 0x80808080;
        context.fill(x, y, x + contentWidth, y + 1, borderColor);
        context.fill(x, y + ENTRY_HEIGHT - 1, x + contentWidth, y + ENTRY_HEIGHT, borderColor);
        context.fill(x, y, x + 1, y + ENTRY_HEIGHT, borderColor);
        context.fill(x + contentWidth - 1, y, x + contentWidth, y + ENTRY_HEIGHT, borderColor);

        String displayName = wp.name;
        int maxNameWidth = contentWidth - 120;
        if (this.font.width(displayName) > maxNameWidth) {
            while (this.font.width(displayName + "...") > maxNameWidth && displayName.length() > 3) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        context.text(this.font,
                Component.literal(displayName).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA),
                x + 5, y + 5, 0xFFFFFF);

        String dimName = wp.dimension.substring(wp.dimension.lastIndexOf(":") + 1);
        context.text(this.font,
                Component.literal("[" + dimName + "]").withStyle(ChatFormatting.DARK_GRAY),
                x + 5 + this.font.width(displayName) + 5, y + 5, 0x666666);

        String coords = String.format("X: %.0f  Y: %.0f  Z: %.0f", wp.position.x, wp.position.y, wp.position.z);
        context.text(this.font,
                Component.literal(coords).withStyle(ChatFormatting.WHITE),
                x + 5, y + 20, 0xFFFFFF);

        if (minecraft != null && minecraft.player != null) {
            double distance = minecraft.player.position().distanceTo(wp.position);
            String distText = String.format("Distance: %.0fm", distance);
            int distColor = distance < 100 ? 0x55FF55 : (distance < 1000 ? 0xFFFF55 : 0xFF5555);
            context.text(this.font,
                    Component.literal(distText),
                    x + 5, y + 35, distColor);
        }

        int buttonX = x + contentWidth + 10;
        int buttonY = y + 5;

        boolean editHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
                editHovered ? 0xFF666600 : 0xFF333300);
        context.centeredText(this.font, "Edit", buttonX + BUTTON_WIDTH / 2, buttonY + 5, 0xFFFF00);

        buttonY += BUTTON_HEIGHT + 3;
        boolean delHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
                delHovered ? 0xFF660000 : 0xFF330000);
        context.centeredText(this.font, "Delete", buttonX + BUTTON_WIDTH / 2, buttonY + 5, 0xFF5555);

        if (isHovered && !editHovered && !delHovered) {
            int hintY = y + ENTRY_HEIGHT - 15;
            context.text(this.font,
                    Component.literal("Click to teleport!").withStyle(ChatFormatting.YELLOW),
                    x + 5, hintY, 0xFFFF00);
        }
    }

    private void drawEditPanel(GuiGraphicsExtractor context) {
        int panelX = this.width - 220;
        int panelY = 80;
        int panelWidth = 210;
        int panelHeight = 180;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0202020);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFFFAA00);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFFFFAA00);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFFFFAA00);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFFFAA00);

        context.centeredText(this.font,
                Component.literal("Edit Waypoint").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                panelX + panelWidth / 2, panelY + 8, 0xFFAA00);

        context.text(this.font, "Name:", panelX + 10, panelY + 15, 0xFFFFFF);
        context.text(this.font, "X:", panelX + 10, panelY + 50, 0xFFFFFF);
        context.text(this.font, "Y:", panelX + 10, panelY + 75, 0xFFFFFF);
        context.text(this.font, "Z:", panelX + 10, panelY + 100, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int button = click.input();
        double mouseX = click.x();
        double mouseY = click.y();

        if (button == InputConstants.MOUSE_BUTTON_LEFT) { // Left click
            int listWidth = editingWaypoint != null ? this.width - 240 : this.width - 40;
            int startX = 20;
            int startIndex = scrollOffset / ENTRY_HEIGHT;
            int visibleEntries = (this.height - HEADER_HEIGHT - 40) / ENTRY_HEIGHT;

            for (int i = startIndex; i < Math.min(waypoints.size(), startIndex + visibleEntries + 1); i++) {
                WaypointHandler.Waypoint wp = waypoints.get(i);
                int entryY = HEADER_HEIGHT + (i * ENTRY_HEIGHT) - scrollOffset;

                if (entryY < HEADER_HEIGHT || entryY > this.height - 40) {
                    continue;
                }

                int buttonAreaWidth = 150;
                int contentWidth = listWidth - buttonAreaWidth;
                int buttonX = startX + contentWidth + 10;
                int buttonY = entryY + 5;

                if (mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                        && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                    editingWaypoint = wp;
                    clearEditFields();
                    init();
                    return true;
                }

                buttonY += BUTTON_HEIGHT + 3;
                if (mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                        && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                    WaypointHandler.deleteWaypoint(wp.id);
                    refreshWaypoints();
                    if (editingWaypoint != null && editingWaypoint.id.equals(wp.id)) {
                        editingWaypoint = null;
                        clearEditFields();
                        init();
                    }
                    return true;
                }

                if (mouseX >= startX && mouseX <= startX + contentWidth
                        && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                    WaypointHandler.teleportToWaypoint(wp.id);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, waypoints.size() * ENTRY_HEIGHT - (this.height - HEADER_HEIGHT - 40));
        int delta = (int) (verticalAmount * 20);
        scrollOffset = Math.clamp(scrollOffset - delta, 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

