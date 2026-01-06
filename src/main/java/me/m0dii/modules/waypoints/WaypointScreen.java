package me.m0dii.modules.waypoints;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class WaypointScreen extends Screen {
    private static final int ENTRY_HEIGHT = 60;
    private static final int HEADER_HEIGHT = 60;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 18;

    private final Screen parent;
    private int scrollOffset = 0;
    private List<WaypointHandler.Waypoint> waypoints;

    // Edit mode variables
    private WaypointHandler.Waypoint editingWaypoint = null;
    private TextFieldWidget nameField;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;

    public WaypointScreen(Screen parent) {
        super(Text.literal("Waypoint Manager"));
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(this.width / 2 - 155, this.height - 30, 100, 20)
                .build());

        // Add waypoint button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Waypoint"), button -> {
            WaypointHandler.addWaypoint();
            refreshWaypoints();
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // Refresh button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⟳ Refresh"), button -> {
            refreshWaypoints();
            editingWaypoint = null;
            clearEditFields();
        }).dimensions(this.width / 2 + 55, this.height - 30, 100, 20).build());

        initEditFields();
    }

    private void initEditFields() {
        if (editingWaypoint != null) {
            int editPanelX = this.width - 220;
            int editPanelY = 80;

            // Name field
            nameField = new TextFieldWidget(this.textRenderer, editPanelX + 10, editPanelY + 25, 190, 18, Text.literal("Name"));
            nameField.setMaxLength(50);
            nameField.setText(editingWaypoint.name);
            this.addDrawableChild(nameField);

            // X coordinate field
            xField = new TextFieldWidget(this.textRenderer, editPanelX + 10, editPanelY + 60, 190, 18, Text.literal("X"));
            xField.setMaxLength(20);
            xField.setText(String.format("%.2f", editingWaypoint.position.x));
            this.addDrawableChild(xField);

            // Y coordinate field
            yField = new TextFieldWidget(this.textRenderer, editPanelX + 10, editPanelY + 85, 190, 18, Text.literal("Y"));
            yField.setMaxLength(20);
            yField.setText(String.format("%.2f", editingWaypoint.position.y));
            this.addDrawableChild(yField);

            // Z coordinate field
            zField = new TextFieldWidget(this.textRenderer, editPanelX + 10, editPanelY + 110, 190, 18, Text.literal("Z"));
            zField.setMaxLength(20);
            zField.setText(String.format("%.2f", editingWaypoint.position.z));
            this.addDrawableChild(zField);

            // Save button
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
                saveEditedWaypoint();
            }).dimensions(editPanelX + 10, editPanelY + 140, 90, 20).build());

            // Cancel button
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
                editingWaypoint = null;
                clearEditFields();
                init(client, width, height);
            }).dimensions(editPanelX + 110, editPanelY + 140, 90, 20).build());
        }
    }

    private void clearEditFields() {
        if (nameField != null) {
            this.remove(nameField);
            nameField = null;
        }
        if (xField != null) {
            this.remove(xField);
            xField = null;
        }
        if (yField != null) {
            this.remove(yField);
            yField = null;
        }
        if (zField != null) {
            this.remove(zField);
            zField = null;
        }
    }

    private void saveEditedWaypoint() {
        if (editingWaypoint == null) return;

        try {
            String newName = nameField.getText().trim();
            double newX = Double.parseDouble(xField.getText());
            double newY = Double.parseDouble(yField.getText());
            double newZ = Double.parseDouble(zField.getText());

            if (!newName.isEmpty()) {
                editingWaypoint.name = newName;
            }
            editingWaypoint.position = new Vec3d(newX, newY, newZ);

            WaypointHandler.saveWaypoints();

            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal("✓ Saved waypoint ").formatted(Formatting.GREEN)
                                .append(Text.literal(editingWaypoint.name).formatted(Formatting.AQUA)),
                        false
                );
            }

            editingWaypoint = null;
            clearEditFields();
            refreshWaypoints();
            init(client, width, height);
        } catch (NumberFormatException e) {
            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal("❌ Invalid coordinates! Please enter valid numbers.").formatted(Formatting.RED),
                        false
                );
            }
        }
    }

    private void refreshWaypoints() {
        this.waypoints = WaypointHandler.getWaypoints();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        drawContents(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawContents(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        String countText = String.format("Total Waypoints: %d", waypoints.size());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(countText).formatted(Formatting.GRAY),
                this.width / 2, 25, 0xAAAAAA);

        if (waypoints.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("No waypoints yet. Click '+ Add Waypoint' to create one!").formatted(Formatting.YELLOW),
                    this.width / 2, this.height / 2, 0xFFFF55);
            return;
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Click a waypoint to teleport • Use buttons to manage").formatted(Formatting.DARK_GRAY),
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
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(scrollText).formatted(Formatting.DARK_GRAY),
                    this.width / 2, this.height - 50, 0x666666);
        }
    }

    private void drawWaypointEntry(DrawContext context, WaypointHandler.Waypoint wp, int x, int y, int width, int mouseX, int mouseY) {
        int buttonAreaWidth = 150;
        int contentWidth = width - buttonAreaWidth;

        boolean isHovered = mouseX >= x && mouseX <= x + contentWidth
                && mouseY >= y && mouseY <= y + ENTRY_HEIGHT;
        boolean isEditing = editingWaypoint != null && editingWaypoint.id.equals(wp.id);

        int bgColor = isEditing ? 0x80FFA500 : (isHovered ? 0x60FFFFFF : 0x40000000);
        context.fill(x, y, x + contentWidth, y + ENTRY_HEIGHT, bgColor);

        int borderColor = isEditing ? 0xFFFFAA00 : 0x80808080;
        context.drawBorder(x, y, contentWidth, ENTRY_HEIGHT, borderColor);

        String displayName = wp.name;
        int maxNameWidth = contentWidth - 120;
        if (this.textRenderer.getWidth(displayName) > maxNameWidth) {
            while (this.textRenderer.getWidth(displayName + "...") > maxNameWidth && displayName.length() > 3) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        context.drawTextWithShadow(this.textRenderer,
                Text.literal(displayName).formatted(Formatting.BOLD, Formatting.AQUA),
                x + 5, y + 5, 0xFFFFFF);

        String dimName = wp.dimension.substring(wp.dimension.lastIndexOf(":") + 1);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("[" + dimName + "]").formatted(Formatting.DARK_GRAY),
                x + 5 + this.textRenderer.getWidth(displayName) + 5, y + 5, 0x666666);

        String coords = String.format("X: %.0f  Y: %.0f  Z: %.0f", wp.position.x, wp.position.y, wp.position.z);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(coords).formatted(Formatting.WHITE),
                x + 5, y + 20, 0xFFFFFF);

        if (client != null && client.player != null) {
            double distance = client.player.getPos().distanceTo(wp.position);
            String distText = String.format("Distance: %.0fm", distance);
            int distColor = distance < 100 ? 0x55FF55 : (distance < 1000 ? 0xFFFF55 : 0xFF5555);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(distText),
                    x + 5, y + 35, distColor);
        }

        int buttonX = x + contentWidth + 10;
        int buttonY = y + 5;

        boolean editHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
                editHovered ? 0xFF666600 : 0xFF333300);
        context.drawCenteredTextWithShadow(this.textRenderer, "Edit", buttonX + BUTTON_WIDTH / 2, buttonY + 5, 0xFFFF00);

        buttonY += BUTTON_HEIGHT + 3;
        boolean delHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
                delHovered ? 0xFF660000 : 0xFF330000);
        context.drawCenteredTextWithShadow(this.textRenderer, "Delete", buttonX + BUTTON_WIDTH / 2, buttonY + 5, 0xFF5555);

        if (isHovered && !editHovered && !delHovered) {
            int hintY = y + ENTRY_HEIGHT - 15;
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("Click to teleport!").formatted(Formatting.YELLOW),
                    x + 5, hintY, 0xFFFF00);
        }
    }

    private void drawEditPanel(DrawContext context) {
        int panelX = this.width - 220;
        int panelY = 80;
        int panelWidth = 210;
        int panelHeight = 180;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0202020);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFFFFAA00);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Edit Waypoint").formatted(Formatting.GOLD, Formatting.BOLD),
                panelX + panelWidth / 2, panelY + 8, 0xFFAA00);

        context.drawTextWithShadow(this.textRenderer, "Name:", panelX + 10, panelY + 15, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "X:", panelX + 10, panelY + 50, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Y:", panelX + 10, panelY + 75, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Z:", panelX + 10, panelY + 100, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
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
                    init(client, width, height);
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
                        init(client, width, height);
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, waypoints.size() * ENTRY_HEIGHT - (this.height - HEADER_HEIGHT - 40));
        int delta = (int) (verticalAmount * 20);
        scrollOffset = Math.clamp(scrollOffset - delta, 0, maxScroll);
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

