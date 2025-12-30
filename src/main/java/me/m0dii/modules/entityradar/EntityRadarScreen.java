package me.m0dii.modules.entityradar;

import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.text.Text;

import java.util.List;

public class EntityRadarScreen extends Screen {
    private static final int ENTRY_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 40;

    private final Screen parent;
    private List<Entity> entities;
    private int scrollOffset = 0;

    public EntityRadarScreen(Screen parent) {
        super(Text.literal("Entity Radar"));
        this.parent = parent;
    }

    public static EntityRadarScreen create(Screen parent) {
        return new EntityRadarScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        refreshEntities();
    }

    private void refreshEntities() {
        this.entities = EntityRadarModule.INSTANCE.getEntities();
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

        int passiveCount = EntityRadarModule.INSTANCE.getPassiveCount();
        int hostileCount = EntityRadarModule.INSTANCE.getHostileCount();
        int neutralCount = EntityRadarModule.INSTANCE.getNeutralCount();
        int totalCount = entities.size();

        String countText = String.format("Total: %d | §aPassive: %d §7| Neutral: %d §c| Hostile: %d",
                totalCount, passiveCount, neutralCount, hostileCount);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(countText), this.width / 2, 25, 0xFFFFFF);

        int startIndex = scrollOffset / ENTRY_HEIGHT;
        int visibleEntries = (this.height - HEADER_HEIGHT - 40) / ENTRY_HEIGHT;

        for (int i = startIndex; i < Math.min(entities.size(), startIndex + visibleEntries + 1); i++) {
            Entity entity = entities.get(i);
            int entryY = HEADER_HEIGHT + (i * ENTRY_HEIGHT) - scrollOffset;

            if (entryY < HEADER_HEIGHT || entryY > this.height - 60) {
                continue;
            }

            // Check if mouse is hovering over this entry
            boolean isHovered = mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

            int backgroundColor = isHovered ? 0x80FFFFFF : 0x40000000;
            context.fill(10, entryY, this.width - 10, entryY + ENTRY_HEIGHT, backgroundColor);

            if (client != null && client.player != null) {
                String entityName = entity.getName().getString();
                float distance = entity.distanceTo(client.player);
                String text = String.format("%s - %.2fm", entityName, distance);


                int color;
                if (entity instanceof HostileEntity) {
                    color = 0xFF5555; // Red for hostile
                } else if (entity instanceof PassiveEntity) {
                    color = 0x55FF55; // Green for passive
                } else {
                    color = 0xFFFF55; // Yellow for neutral
                }

                context.drawTextWithShadow(this.textRenderer, text, 15, entryY + 6, color);

                if (isHovered) {
                    context.drawTextWithShadow(this.textRenderer, "Click to teleport",
                            this.width - 150, entryY + 6, 0xFFFF00);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int startIndex = scrollOffset / ENTRY_HEIGHT;
            int visibleEntries = (this.height - HEADER_HEIGHT - 40) / ENTRY_HEIGHT;

            for (int i = startIndex; i < Math.min(entities.size(), startIndex + visibleEntries + 1); i++) {
                Entity entity = entities.get(i);
                int entryY = HEADER_HEIGHT + (i * ENTRY_HEIGHT) - scrollOffset;

                if (entryY < HEADER_HEIGHT || entryY > this.height - 40) {
                    continue;
                }

                if (mouseX >= 10 && mouseX <= this.width - 10
                        && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                    teleportToEntity(entity);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, entities.size() * ENTRY_HEIGHT - (this.height - HEADER_HEIGHT - 40));
        int delta = (int) (verticalAmount * ENTRY_HEIGHT);
        scrollOffset = Math.clamp(scrollOffset - delta, 0, maxScroll);
        return true;
    }

    private void teleportToEntity(Entity entity) {
        if (client != null && client.player != null) {
            if (FreecamModule.INSTANCE.isEnabled()) {
                CameraEntity camera = CameraEntity.getCamera();
                if (camera != null) {
                    camera.setPosition(entity.getX(), entity.getY(), entity.getZ());
                }
                return;
            }

            String command = String.format("tp @s %.2f %.2f %.2f",
                    entity.getX(), entity.getY(), entity.getZ());
            client.player.networkHandler.sendChatCommand(command);
            client.setScreen(null);
        }
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
