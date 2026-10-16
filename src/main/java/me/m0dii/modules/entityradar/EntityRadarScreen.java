package me.m0dii.modules.entityradar;



import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntityRadarScreen extends Screen implements ThemedScreen {
    private static final int ENTRY_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 40;

    private final Screen parent;
    private List<Entity> entities;
    private int scrollOffset = 0;

    public EntityRadarScreen(Screen parent) {
        super(Component.literal("Entity Radar"));
        this.parent = parent;
    }

    public static EntityRadarScreen create(Screen parent) {
        return new EntityRadarScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(parent);
            }
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());

        refreshEntities();
    }

    private void refreshEntities() {
        this.entities = EntityRadarModule.INSTANCE.getEntities();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractBackground(context, mouseX, mouseY, delta);
        drawContents(context, mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawContents(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int passiveCount = EntityRadarModule.INSTANCE.getPassiveCount();
        int hostileCount = EntityRadarModule.INSTANCE.getHostileCount();
        int neutralCount = EntityRadarModule.INSTANCE.getNeutralCount();
        int totalCount = entities.size();

        String countText = String.format("Total: %d | §aPassive: %d §7| Neutral: %d §c| Hostile: %d",
                totalCount, passiveCount, neutralCount, hostileCount);
        context.centeredText(this.font, Component.literal(countText), this.width / 2, 25, 0xFFFFFFFF);

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

            if (minecraft != null && minecraft.player != null) {
                String entityName = entity.getName().getString();
                float distance = entity.distanceTo(minecraft.player);
                String text = String.format("%s - %.2fm", entityName, distance);


                int color;
                if (entity instanceof Monster) {
                    color = 0xFFFF5555; // Red for hostile
                } else if (entity instanceof AgeableMob) {
                    color = 0xFF55FF55; // Green for passive
                } else {
                    color = 0xFFFFFF55; // Yellow for neutral
                }

                context.text(this.font, text, 15, entryY + 6, color);

                if (isHovered) {
                    context.text(this.font, "Click to teleport",
                            this.width - 150, entryY + 6, 0xFFFFFF00);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int button = click.button();
        double mouseY = click.y();
        double mouseX = click.x();

        if (button == InputConstants.MOUSE_BUTTON_LEFT) { // Left click
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

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, entities.size() * ENTRY_HEIGHT - (this.height - HEADER_HEIGHT - 40));
        int delta = (int) (verticalAmount * ENTRY_HEIGHT);
        scrollOffset = Math.clamp(scrollOffset - delta, 0, maxScroll);
        return true;
    }

    private void teleportToEntity(Entity entity) {
        if (minecraft != null && minecraft.player != null) {
            if (FreecamModule.INSTANCE.isEnabled()) {
                CameraEntity camera = CameraEntity.getCamera();
                if (camera != null) {
                    camera.setPos(entity.getX(), entity.getY(), entity.getZ());
                }
                return;
            }

            String command = String.format("tp @s %.2f %.2f %.2f", entity.getX(), entity.getY(), entity.getZ());
            minecraft.player.connection.sendCommand(command);
            minecraft.gui.setScreen(null);
        }
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
