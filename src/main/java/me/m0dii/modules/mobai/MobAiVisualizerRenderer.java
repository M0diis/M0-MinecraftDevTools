package me.m0dii.modules.mobai;

import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.TargetPathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.BrainDebugData;
import org.joml.Matrix4f;

import java.util.*;

final class MobAiVisualizerRenderer {
    private static final float WIDTH_MULTIPLIER = 1.55f;
    private static final Map<String, Integer> TRACKER_HOSTILE_RANGES = Map.ofEntries(
            Map.entry("all", 15),
            Map.entry("drowned", 8),
            Map.entry("evoker", 12),
            Map.entry("husk", 8),
            Map.entry("illusioner", 12),
            Map.entry("pillager", 15),
            Map.entry("ravager", 12),
            Map.entry("vex", 8),
            Map.entry("vindicator", 10),
            Map.entry("zoglin", 10),
            Map.entry("zombie", 8),
            Map.entry("zombie_villager", 8)
    );
    private static final Map<String, Integer> VILLAGER_FOOD_POINTS = Map.of(
            "bread", 4,
            "potato", 1,
            "carrot", 1,
            "beetroot", 1
    );

    private boolean registered;

    void register() {
        if (registered) {
            return;
        }
        registered = true;
        WorldRenderEvents.AFTER_ENTITIES.register(this::onAfterEntities);
    }

    private void onAfterEntities(WorldRenderContext context) {
        MobAiVisualizerModule module = MobAiVisualizerModule.INSTANCE;
        boolean hasCommandData = MobAiDebugClientState.hasActiveData();
        MobAiDebugClientState.TrackerConfig trackerConfig = MobAiDebugClientState.getTrackerConfig();
        boolean hasTrackerData = trackerConfig != null && trackerConfig.isActive();
        if (!module.isEnabled() && !hasCommandData && !hasTrackerData) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            consumers = client.getBufferBuilders().getEntityVertexConsumers();
        }

        VertexConsumer visible = consumers.getBuffer(RenderLayers.LINES);
        VertexConsumer occluded = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        double radiusSq = (double) module.getRadius() * (double) module.getRadius();

        Map<Integer, EntityOverlayInfo> overlays = new LinkedHashMap<>();
        Set<Integer> debugPathEntities = new LinkedHashSet<>();
        Set<Integer> debugBrainEntities = new LinkedHashSet<>();

        DebugDataStore debugDataStore = handler.getDebugDataStore();
        if (module.isEnabled() && debugDataStore != null) {
            renderDebugPathData(client, debugDataStore, cameraPos, tickDelta, radiusSq, visible, occluded, overlays, debugPathEntities);
            renderDebugBrainData(client, debugDataStore, cameraPos, tickDelta, radiusSq, visible, occluded, overlays, debugBrainEntities);
        }

        if (module.isEnabled() && module.useClientFallback()) {
            renderClientFallbackData(client, cameraPos, tickDelta, radiusSq, visible, occluded, overlays, debugPathEntities, debugBrainEntities);
        }

        renderTrackerData(client, debugDataStore, trackerConfig, cameraPos, tickDelta, visible, occluded, overlays);
        renderCommandDebugData(client, cameraPos, tickDelta, visible, occluded, overlays);

        if (module.showLabels() || hasCommandData || hasTrackerData) {
            renderLabels(context, consumers, cameraPos, tickDelta, overlays.values());
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayers.LINES);
            immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
        }
    }

    private void renderDebugPathData(MinecraftClient client,
                                     DebugDataStore debugDataStore,
                                     Vec3d cameraPos,
                                     float tickDelta,
                                     double radiusSq,
                                     VertexConsumer visible,
                                     VertexConsumer occluded,
                                     Map<Integer, EntityOverlayInfo> overlays,
                                     Set<Integer> debugPathEntities) {
        debugDataStore.forEachEntityData(DebugSubscriptionTypes.ENTITY_PATHS, (entity, debugData) -> {
            if (!(entity instanceof MobEntity mob) || mob.squaredDistanceTo(client.player) > radiusSq) {
                return;
            }

            debugPathEntities.add(entity.getId());
            drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta, 1.0f, 0.58f, 0.14f, 0.85f, 0.45f, 1.55f, 0.04);

            if (MobAiVisualizerModule.INSTANCE.showPathLines()) {
                double nodeHalfSize = clamp(debugData.maxNodeDistance() * 0.5f, 0.18, 0.55);
                drawPath(visible, occluded, debugData.path(), cameraPos, nodeHalfSize, MobAiVisualizerModule.INSTANCE.showPathNodes());
            }

            EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
            overlay.focused = entity == client.targetedEntity;
            overlay.pathDebug = true;
            addPathLine(overlay, "Path*", debugData.path(), 0xFFE6A24A);
        });
    }

    private void renderDebugBrainData(MinecraftClient client,
                                      DebugDataStore debugDataStore,
                                      Vec3d cameraPos,
                                      float tickDelta,
                                      double radiusSq,
                                      VertexConsumer visible,
                                      VertexConsumer occluded,
                                      Map<Integer, EntityOverlayInfo> overlays,
                                      Set<Integer> debugBrainEntities) {
        debugDataStore.forEachEntityData(DebugSubscriptionTypes.BRAINS, (entity, debugData) -> {
            if (!(entity instanceof LivingEntity living) || living.squaredDistanceTo(client.player) > radiusSq) {
                return;
            }

            debugBrainEntities.add(entity.getId());
            drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta, 0.26f, 0.88f, 1.0f, 0.75f, 0.35f, 1.35f, 0.08);

            if (MobAiVisualizerModule.INSTANCE.showBrainTargets()) {
                drawBrainDebugTargets(visible, occluded, entity, debugData, cameraPos, tickDelta);
            }

            EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
            overlay.focused = entity == client.targetedEntity;
            overlay.brainDebug = true;
            addDebugBrainLines(debugData, overlay);
            addCompactListLine(overlay, "Act", debugData.activities(), 2, 0xFF8FE9FF);
            addCompactListLine(overlay, "Task", debugData.behaviors(), 2, 0xFFBDEFFF);
            addCompactListLine(overlay, "Mem", debugData.memories(), 1, 0xFF8AD6E2);
        });
    }

    private void renderClientFallbackData(MinecraftClient client,
                                          Vec3d cameraPos,
                                          float tickDelta,
                                          double radiusSq,
                                          VertexConsumer visible,
                                          VertexConsumer occluded,
                                          Map<Integer, EntityOverlayInfo> overlays,
                                          Set<Integer> debugPathEntities,
                                          Set<Integer> debugBrainEntities) {
        List<Entity> nearbyEntities = client.world.getOtherEntities(client.player, client.player.getBoundingBox().expand(MobAiVisualizerModule.INSTANCE.getRadius()), entity -> entity instanceof MobEntity);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof MobEntity mob) || mob.squaredDistanceTo(client.player) > radiusSq) {
                continue;
            }

            EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
            overlay.focused = entity == client.targetedEntity;
            boolean hasClientPath = false;
            boolean hasClientTargets = false;

            if (!debugPathEntities.contains(entity.getId()) && MobAiVisualizerModule.INSTANCE.showPathLines()) {
                Path path = mob.getNavigation().getCurrentPath();
                if (path != null && !path.isFinished()) {
                    drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta, 0.98f, 0.74f, 0.29f, 0.70f, 0.30f, 1.15f, 0.0);
                    drawPath(visible, occluded, path, cameraPos, 0.22, false);
                    hasClientPath = true;
                    overlay.clientPath = true;
                    addPathLine(overlay, "Path", path, 0xFFF0BE66);
                }
            }

            if (!debugBrainEntities.contains(entity.getId()) && MobAiVisualizerModule.INSTANCE.showBrainTargets()) {
                if (drawClientBrainTargets(client, mob, visible, occluded, cameraPos, tickDelta)) {
                    hasClientTargets = true;
                    overlay.clientBrain = true;
                }
            }

            if (!debugBrainEntities.contains(entity.getId()) && MobAiVisualizerModule.INSTANCE.showLabels()) {
                addClientBrainLines(mob, overlay, hasClientPath, hasClientTargets);
            }

            if (!overlay.hasVisualMarker()) {
                drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta, 0.90f, 0.92f, 0.98f, 0.42f, 0.20f, 0.95f, 0.02);
                overlay.idleVisual = true;
            }
        }
    }

    private void renderCommandDebugData(MinecraftClient client,
                                        Vec3d cameraPos,
                                        float tickDelta,
                                        VertexConsumer visible,
                                        VertexConsumer occluded,
                                        Map<Integer, EntityOverlayInfo> overlays) {
        if (client.world == null) {
            return;
        }

        MobAiDebugClientState.PathPreviewState previewState = MobAiDebugClientState.getPathPreviewState();
        if (previewState != null) {
            Entity entity = client.world.getEntityById(previewState.entityId());
            if (entity instanceof MobEntity mob) {
                EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
                overlay.focused = overlay.focused || entity == client.targetedEntity;
                overlay.previewPath = true;

                drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta,
                        previewState.reachesTarget() ? 0.48f : 1.0f,
                        0.94f,
                        previewState.reachesTarget() ? 0.36f : 0.32f,
                        0.90f,
                        0.45f,
                        1.55f,
                        0.05);
                drawPreviewPath(visible, occluded, mob, previewState, cameraPos, tickDelta);

                for (MobAiDebugPayloads.DebugLine line : previewState.lines()) {
                    overlay.addLine(line.text(), line.color());
                }
            }
        }

        MobAiDebugClientState.InspectState inspectState = MobAiDebugClientState.getInspectState();
        if (inspectState != null) {
            Entity entity = client.world.getEntityById(inspectState.entityId());
            if (entity != null) {
                EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
                overlay.focused = true;
                overlay.inspected = true;

                if (!overlay.hasVisualMarker()) {
                    drawEntityOutline(visible, occluded, entity, cameraPos, tickDelta,
                            0.68f, 0.82f, 1.0f, 0.88f, 0.35f, 1.3f, 0.03);
                }

                for (MobAiDebugPayloads.DebugLine line : inspectState.lines()) {
                    overlay.addLine(line.text(), line.color());
                }
            }
        }
    }

    private void renderTrackerData(MinecraftClient client,
                                   DebugDataStore debugDataStore,
                                   MobAiDebugClientState.TrackerConfig trackerConfig,
                                   Vec3d cameraPos,
                                   float tickDelta,
                                   VertexConsumer visible,
                                   VertexConsumer occluded,
                                   Map<Integer, EntityOverlayInfo> overlays) {
        if (trackerConfig == null || !trackerConfig.isActive() || client.player == null || client.world == null) {
            return;
        }

        double radiusSq = (double) trackerConfig.radius() * (double) trackerConfig.radius();
        List<Entity> nearbyEntities = client.world.getOtherEntities(client.player,
                client.player.getBoundingBox().expand(trackerConfig.radius()),
                entity -> entity instanceof LivingEntity);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living) || entity.squaredDistanceTo(client.player) > radiusSq) {
                continue;
            }

            EntityOverlayInfo overlay = overlays.computeIfAbsent(entity.getId(), id -> new EntityOverlayInfo(entity));
            overlay.focused = overlay.focused || entity == client.targetedEntity;

            boolean tracked = false;
            if (trackerConfig.hasDisplay("health")) {
                tracked |= applyHealthTracker(living, overlay);
            }
            if (trackerConfig.hasDisplay("velocity")) {
                tracked |= applyVelocityTracker(entity, overlay, visible, occluded, cameraPos, tickDelta);
            }
            if (trackerConfig.hasDisplay("item_pickup")) {
                tracked |= applyItemPickupTracker(client, living, overlay, visible, occluded, cameraPos, tickDelta, trackerConfig);
            }
            if (trackerConfig.hasDisplay("pathfinding") && entity instanceof MobEntity mob) {
                tracked |= applyPathfindingTracker(mob, overlay, visible, occluded, cameraPos);
            }
            if (entity instanceof VillagerEntity villager) {
                if (trackerConfig.hasDisplay("villager_buddy_detection")) {
                    tracked |= applyVillagerBuddyTracker(client, villager, overlay, visible, occluded, cameraPos, tickDelta, trackerConfig);
                }
                if (trackerConfig.hasDisplay("villager_hostile_detection")) {
                    tracked |= applyVillagerHostileTracker(client, villager, overlay, visible, occluded, cameraPos, tickDelta, trackerConfig);
                }
                if (trackerConfig.hasDisplay("villager_breeding")) {
                    tracked |= applyVillagerBreedingTracker(client, villager, overlay, visible, occluded, cameraPos, tickDelta);
                }
                if (trackerConfig.hasDisplay("villager_iron_golem_spawning")) {
                    tracked |= applyVillagerGolemTracker(client, debugDataStore, villager, overlay, visible, occluded, cameraPos, tickDelta, trackerConfig);
                }
            }

            if (tracked) {
                overlay.tracker = true;
            }
        }
    }

    private boolean applyHealthTracker(LivingEntity living, EntityOverlayInfo overlay) {
        overlay.addLine(formatHealthLine(living.getHealth(), living.getMaxHealth()),
                living.getHealth() < living.getMaxHealth() ? 0xFFFFB1B1 : 0xFFF4F4F4);
        return true;
    }

    private boolean applyVelocityTracker(Entity entity,
                                         EntityOverlayInfo overlay,
                                         VertexConsumer visible,
                                         VertexConsumer occluded,
                                         Vec3d cameraPos,
                                         float tickDelta) {
        Vec3d velocityPerSecond = entity.getVelocity().multiply(20.0);
        double horizontal = Math.sqrt(velocityPerSecond.x * velocityPerSecond.x + velocityPerSecond.z * velocityPerSecond.z);
        double total = velocityPerSecond.length();
        overlay.addLine(String.format(Locale.ROOT, "Vel: %.3f | H %.3f", total, horizontal), 0xFF9BE7FF);
        overlay.addLine(String.format(Locale.ROOT, "XYZ: %.3f %.3f %.3f", velocityPerSecond.x, velocityPerSecond.y, velocityPerSecond.z), 0xFF8FD0F0);

        if (total < 0.01) {
            return true;
        }

        Vec3d source = entityCenter(entity, tickDelta);
        double length = Math.min(2.5, Math.max(0.35, total * 0.12));
        Vec3d target = source.add(velocityPerSecond.normalize().multiply(length));
        drawLineBetween(visible, occluded, source, target, cameraPos, 0.38f, 0.88f, 1.0f, 0.92f, 0.30f, 1.25f);
        return true;
    }

    private boolean applyItemPickupTracker(MinecraftClient client,
                                           LivingEntity living,
                                           EntityOverlayInfo overlay,
                                           VertexConsumer visible,
                                           VertexConsumer occluded,
                                           Vec3d cameraPos,
                                           float tickDelta,
                                           MobAiDebugClientState.TrackerConfig trackerConfig) {
        Box pickupBox = living.getBoundingBox().expand(1.0, 0.0, 1.0);
        if (trackerConfig.showBoxes()) {
            drawWorldBox(visible, occluded, pickupBox, cameraPos, 1.0f, 0.33f, 0.33f, alphaFromByte(trackerConfig.alpha(), 0.95f), alphaFromByte(trackerConfig.alpha(), 0.30f), 1.15f);
        }

        List<ItemEntity> items = client.world.getEntitiesByClass(ItemEntity.class, pickupBox, item -> !item.isRemoved());
        Vec3d source = entityCenter(living, tickDelta);
        for (ItemEntity item : items) {
            drawLineBetween(visible, occluded, source, entityCenter(item, tickDelta), cameraPos, 1.0f, 0.30f, 0.85f, 0.88f, 0.24f, 0.95f);
        }
        overlay.addLine("Pickup: " + items.size() + " item(s)", items.isEmpty() ? 0xFFD8DDE8 : 0xFFFFB0E7);
        return true;
    }

    private boolean applyPathfindingTracker(MobEntity mob,
                                            EntityOverlayInfo overlay,
                                            VertexConsumer visible,
                                            VertexConsumer occluded,
                                            Vec3d cameraPos) {
        Path path = mob.getNavigation().getCurrentPath();
        if (path == null || path.getLength() <= 0) {
            overlay.addLine("Path: idle", 0xFFD8DDE8);
            return true;
        }
        drawPath(visible, occluded, path, cameraPos, 0.18, true);
        addPathLine(overlay, "Path", path, 0xFFF0BE66);
        return true;
    }

    private boolean applyVillagerBuddyTracker(MinecraftClient client,
                                              VillagerEntity villager,
                                              EntityOverlayInfo overlay,
                                              VertexConsumer visible,
                                              VertexConsumer occluded,
                                              Vec3d cameraPos,
                                              float tickDelta,
                                              MobAiDebugClientState.TrackerConfig trackerConfig) {
        Box detectionBox = villager.getBoundingBox().expand(10.0, 10.0, 10.0);
        if (trackerConfig.showBoxes()) {
            drawWorldBox(visible, occluded, detectionBox, cameraPos, 0.40f, 0.22f, 0.08f, alphaFromByte(trackerConfig.alpha(), 0.92f), alphaFromByte(trackerConfig.alpha(), 0.24f), 1.05f);
        }

        List<VillagerEntity> buddies = client.world.getEntitiesByClass(VillagerEntity.class, detectionBox, other -> other != villager && !other.isRemoved());
        Vec3d source = entityCenter(villager, tickDelta);
        for (VillagerEntity buddy : buddies) {
            drawLineBetween(visible, occluded, source, entityCenter(buddy, tickDelta), cameraPos, 1.0f, 0.35f, 0.92f, 0.82f, 0.26f, 0.95f);
        }
        overlay.addLine("Buddies: " + buddies.size(), buddies.size() == 3 ? 0xFFFFD89A : buddies.size() > 3 ? 0xFFFFB36B : 0xFFB9D6FF);
        return true;
    }

    private boolean applyVillagerHostileTracker(MinecraftClient client,
                                                VillagerEntity villager,
                                                EntityOverlayInfo overlay,
                                                VertexConsumer visible,
                                                VertexConsumer occluded,
                                                Vec3d cameraPos,
                                                float tickDelta,
                                                MobAiDebugClientState.TrackerConfig trackerConfig) {
        String hostileFocus = trackerConfig.hostileFocus();
        int displayRadius = TRACKER_HOSTILE_RANGES.getOrDefault(hostileFocus, TRACKER_HOSTILE_RANGES.get("all"));
        if (trackerConfig.showBoxes()) {
            drawEntityRadiusSphere(visible, occluded, villager, cameraPos, tickDelta, displayRadius, 0.34f, 0.20f, 0.66f, alphaFromByte(trackerConfig.alpha(), 0.76f), alphaFromByte(trackerConfig.alpha(), 0.18f), 0.95f);
        }

        List<LivingEntity> hostiles = client.world.getEntitiesByClass(LivingEntity.class,
                villager.getBoundingBox().expand(16.0, 16.0, 16.0),
                other -> other != villager && isTrackedVillagerHostile(villager, other, hostileFocus));
        hostiles.sort(Comparator.comparingDouble(villager::squaredDistanceTo));

        LivingEntity nearest = hostiles.isEmpty() ? null : hostiles.getFirst();
        Vec3d source = entityCenter(villager, tickDelta);
        for (LivingEntity hostile : hostiles) {
            Vec3d target = entityCenter(hostile, tickDelta);
            if (hostile == nearest) {
                drawLineBetween(visible, occluded, source, target, cameraPos, 1.0f, 0.22f, 0.22f, 0.95f, 0.34f, 1.2f);
                drawEntityOutline(visible, occluded, hostile, cameraPos, tickDelta, 1.0f, 0.24f, 0.24f, 0.82f, 0.28f, 1.2f, 0.04);
            } else {
                drawEntityOutline(visible, occluded, hostile, cameraPos, tickDelta, 0.70f, 0.16f, 0.16f, 0.48f, 0.18f, 0.95f, 0.02);
            }
        }

        overlay.addLine(nearest == null
                        ? "Hostile: peaceful"
                        : "Hostile: " + compactId(Registries.ENTITY_TYPE.getId(nearest.getType())),
                nearest == null ? 0xFF9FD6A3 : 0xFFFFA3A3);
        if (hostileFocus != null && !"all".equals(hostileFocus)) {
            overlay.addLine("Focus: " + hostileFocus, 0xFFD9C0FF);
        }
        return true;
    }

    private boolean applyVillagerBreedingTracker(MinecraftClient client,
                                                 VillagerEntity villager,
                                                 EntityOverlayInfo overlay,
                                                 VertexConsumer visible,
                                                 VertexConsumer occluded,
                                                 Vec3d cameraPos,
                                                 float tickDelta) {
        Optional<GlobalPos> home = getOptionalMemory(villager.getBrain(), MemoryModuleType.HOME);
        if (home.isPresent() && client.world.getRegistryKey().equals(home.get().dimension())) {
            drawTargetLineAndBox(visible, occluded, entityCenter(villager, tickDelta), home.get().pos(), cameraPos, 1.0f, 0.66f, 0.86f, 0.88f, 0.26f, 1.0f);
        }

        int foodPortions = countVillagerFoodPoints(villager) / 12;
        int breedingAge = villager.getBreedingAge();
        overlay.addLine("Bed: " + (home.isPresent() ? "yes" : "no"), home.isPresent() ? 0xFFD8A8FF : 0xFFFFA3A3);
        overlay.addLine("Food: " + foodPortions + " portion(s)", foodPortions > 0 ? 0xFFFFD89A : 0xFFFFA3A3);
        overlay.addLine("Breed: " + breedingAge + "t", breedingAge == 0 ? 0xFF9FD6A3 : 0xFFFFB36B);
        return true;
    }

    private boolean applyVillagerGolemTracker(MinecraftClient client,
                                              DebugDataStore debugDataStore,
                                              VillagerEntity villager,
                                              EntityOverlayInfo overlay,
                                              VertexConsumer visible,
                                              VertexConsumer occluded,
                                              Vec3d cameraPos,
                                              float tickDelta,
                                              MobAiDebugClientState.TrackerConfig trackerConfig) {
        double villagerHalfWidth = villager.getWidth() * 0.5;
        if (trackerConfig.showBoxes()) {
            drawRelativeBox(visible, occluded, villager, cameraPos, tickDelta,
                    -8.0, -6.0, -8.0,
                    9.0, 7.0, 9.0,
                    0.86f, 0.22f, 0.22f, alphaFromByte(trackerConfig.alpha(), 0.92f), alphaFromByte(trackerConfig.alpha(), 0.22f), 1.05f);
            drawRelativeBox(visible, occluded, villager, cameraPos, tickDelta,
                    -16.0 - villagerHalfWidth, -16.0, -16.0 - villagerHalfWidth,
                    16.0 + villagerHalfWidth, 16.0 + villager.getHeight(), 16.0 + villagerHalfWidth,
                    0.88f, 0.86f, 0.26f, alphaFromByte(trackerConfig.alpha(), 0.78f), alphaFromByte(trackerConfig.alpha(), 0.20f), 0.95f);
        }

        long golemTimer = 0L;
        Optional<Boolean> recentlyDetected = getOptionalMemory(villager.getBrain(), MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        if (recentlyDetected.isPresent()) {
            golemTimer = villager.getBrain().getMemoryExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        }

        long worldTime = client.world.getTime();
        long lastSleptAgo = -1L;
        Optional<Long> lastSlept = getOptionalMemory(villager.getBrain(), MemoryModuleType.LAST_SLEPT);
        if (lastSlept.isPresent()) {
            lastSleptAgo = Math.max(0L, worldTime - lastSlept.get());
        }

        List<LivingEntity> golems = client.world.getEntitiesByClass(LivingEntity.class,
                villager.getBoundingBox().expand(16.0, 16.0, 16.0),
                other -> compactId(Registries.ENTITY_TYPE.getId(other.getType())).equals("iron_golem"));
        Vec3d source = entityCenter(villager, tickDelta);
        for (LivingEntity golem : golems) {
            drawEntityOutline(visible, occluded, golem, cameraPos, tickDelta, 0.85f, 0.85f, 1.0f, 0.64f, 0.20f, 1.0f, 0.03);
            drawLineBetween(visible, occluded, source, entityCenter(golem, tickDelta), cameraPos, 0.85f, 0.85f, 1.0f, 0.72f, 0.20f, 0.95f);
        }

        overlay.addLine("Golem: " + (golemTimer > 0 ? golemTimer + "t" : "clear"), golemTimer > 0 ? 0xFFFF8F8F : 0xFF9FD6A3);
        if (lastSleptAgo >= 0) {
            overlay.addLine("Slept: " + lastSleptAgo + "t", lastSleptAgo < 24000L ? 0xFF9FD6A3 : 0xFFFFB36B);
            if (lastSleptAgo < 24000L && golemTimer == 0L) {
                overlay.addLine("Attempt: " + (100 - Math.floorMod(worldTime, 100L)) + "t", 0xFFFFD89A);
            }
        }
        overlay.addLine("Golems: " + golems.size(), 0xFFE0DD99);
        if (debugDataStore != null) {
            debugDataStore.forEachEntityData(DebugSubscriptionTypes.BRAINS, (entity, debugData) -> {
                if (entity == villager && debugData.wantsGolem()) {
                    overlay.addLine("Status: wants golem", 0xFFFFB36B);
                }
            });
        }
        return true;
    }

    private void addClientBrainLines(MobEntity mob,
                                     EntityOverlayInfo overlay,
                                     boolean hasClientPath,
                                     boolean hasClientTargets) {
        Brain<?> brain = mob.getBrain();
        overlay.addLine(formatHealthLine(mob.getHealth(), mob.getMaxHealth()), mob.getHealth() < mob.getMaxHealth() ? 0xFFFFB1B1 : 0xFFF4F4F4);

        List<String> activities = brain.getPossibleActivities().stream()
                .sorted(Comparator.comparing(Activity::getId))
                .map(Activity::getId)
                .limit(2)
                .map(MobAiVisualizerRenderer::compactToken)
                .toList();
        if (!activities.isEmpty()) {
            overlay.addLine("Act: " + String.join(", ", activities), 0xFF96E3FF);
        }

        List<String> tasks = new ArrayList<>();
        for (Task<?> task : brain.getRunningTasks()) {
            tasks.add(compactToken(task.getName()));
            if (tasks.size() >= 2) {
                break;
            }
        }
        if (!tasks.isEmpty()) {
            overlay.addLine("Task: " + String.join(", ", tasks), 0xFFB7E7F7);
        }

        if (!hasClientPath && !hasClientTargets && tasks.isEmpty()) {
            overlay.addLine("State: idle", 0xFFD8DDE8);
        }
    }

    private boolean drawClientBrainTargets(MinecraftClient client,
                                           MobEntity mob,
                                           VertexConsumer visible,
                                           VertexConsumer occluded,
                                           Vec3d cameraPos,
                                           float tickDelta) {
        boolean drewAnything = false;
        Vec3d source = entityCenter(mob, tickDelta);
        Brain<?> brain = mob.getBrain();

        Optional<WalkTarget> walkTarget = getOptionalMemory(brain, MemoryModuleType.WALK_TARGET);
        if (walkTarget.isPresent()) {
            BlockPos pos = walkTarget.get().getLookTarget().getBlockPos();
            drawTargetLineAndBox(visible, occluded, source, pos, cameraPos, 0.25f, 1.0f, 0.40f, 0.95f, 0.45f, 1.15f);
            drewAnything = true;
        }

        Optional<LookTarget> lookTarget = getOptionalMemory(brain, MemoryModuleType.LOOK_TARGET);
        if (lookTarget.isPresent()) {
            drawTargetLineAndBox(visible, occluded, source, lookTarget.get().getBlockPos(), cameraPos, 0.20f, 0.85f, 1.0f, 0.92f, 0.42f, 1.0f);
            drewAnything = true;
        }

        Optional<LivingEntity> attackTarget = getOptionalMemory(brain, MemoryModuleType.ATTACK_TARGET);
        if (attackTarget.isPresent() && !attackTarget.get().isRemoved()) {
            drawLineBetweenEntities(visible, occluded, source, entityCenter(attackTarget.get(), tickDelta), cameraPos, 1.0f, 0.28f, 0.28f, 0.95f, 0.42f, 1.2f);
            drewAnything = true;
        }

        Optional<LivingEntity> interactionTarget = getOptionalMemory(brain, MemoryModuleType.INTERACTION_TARGET);
        if (interactionTarget.isPresent() && !interactionTarget.get().isRemoved()) {
            drawLineBetweenEntities(visible, occluded, source, entityCenter(interactionTarget.get(), tickDelta), cameraPos, 1.0f, 0.35f, 0.92f, 0.92f, 0.36f, 1.0f);
            drewAnything = true;
        }

        drewAnything |= drawGlobalPosTarget(client, getOptionalMemory(brain, MemoryModuleType.HOME), visible, occluded, source, cameraPos, 0.42f, 0.62f, 1.0f, 0.88f, 0.35f);
        drewAnything |= drawGlobalPosTarget(client, getOptionalMemory(brain, MemoryModuleType.JOB_SITE), visible, occluded, source, cameraPos, 0.84f, 0.60f, 1.0f, 0.86f, 0.35f);
        drewAnything |= drawGlobalPosTarget(client, getOptionalMemory(brain, MemoryModuleType.POTENTIAL_JOB_SITE), visible, occluded, source, cameraPos, 0.64f, 0.52f, 1.0f, 0.72f, 0.25f);

        Optional<BlockPos> disturbance = getOptionalMemory(brain, MemoryModuleType.DISTURBANCE_LOCATION);
        if (disturbance.isPresent()) {
            drawTargetLineAndBox(visible, occluded, source, disturbance.get(), cameraPos, 1.0f, 0.90f, 0.22f, 0.88f, 0.30f, 0.95f);
            drewAnything = true;
        }

        if (mob.hasPositionTarget()) {
            drawTargetLineAndBox(visible, occluded, source, mob.getPositionTarget(), cameraPos, 0.96f, 0.96f, 0.96f, 0.72f, 0.22f, 1.0f);
            drewAnything = true;
        }

        return drewAnything;
    }

    private void drawPreviewPath(VertexConsumer visible,
                                 VertexConsumer occluded,
                                 MobEntity mob,
                                 MobAiDebugClientState.PathPreviewState previewState,
                                 Vec3d cameraPos,
                                 float tickDelta) {
        Vec3d source = entityCenter(mob, tickDelta);
        List<BlockPos> nodes = previewState.nodes();

        if (nodes.isEmpty()) {
            drawTargetLineAndBox(
                    visible,
                    occluded,
                    source,
                    previewState.target(),
                    cameraPos,
                    1.0f,
                    0.32f,
                    0.32f,
                    0.94f,
                    0.42f,
                    1.35f
            );
            return;
        }

        Vec3d previous = source;
        int currentNodeIndex = clamp(previewState.currentNodeIndex(), 0, nodes.size() - 1);
        for (int i = 0; i < nodes.size(); i++) {
            Vec3d current = center(nodes.get(i));
            boolean activeSegment = i >= currentNodeIndex;
            float r = previewState.reachesTarget() ? 0.48f : 1.0f;
            float g = activeSegment ? 0.96f : 0.58f;
            float b = previewState.reachesTarget() ? 0.36f : 0.26f;
            float alpha = activeSegment ? 0.96f : 0.42f;
            drawLineBetween(visible, occluded, previous, current, cameraPos, r, g, b, alpha, alpha * 0.45f, 1.7f);
            previous = current;
        }

        for (int i = 0; i < nodes.size(); i++) {
            BlockPos nodePos = nodes.get(i);
            boolean current = i == currentNodeIndex;
            boolean visited = i < currentNodeIndex;
            float r = previewState.reachesTarget() ? 0.48f : 1.0f;
            float g = current ? 1.0f : (visited ? 0.62f : 0.92f);
            float b = previewState.reachesTarget() ? 0.36f : 0.28f;
            double half = current ? 0.28 : 0.20;
            drawMarkerBox(visible, occluded, nodePos, cameraPos, half, r, g, b, current ? 0.95f : 0.78f, current ? 0.42f : 0.24f, current ? 1.25f : 1.0f);
        }

        for (BlockPos openNode : previewState.openNodes()) {
            drawMarkerBox(visible, occluded, openNode, cameraPos, 0.14, 0.22f, 0.88f, 1.0f, 0.46f, 0.16f, 0.85f);
        }

        for (BlockPos closedNode : previewState.closedNodes()) {
            drawMarkerBox(visible, occluded, closedNode, cameraPos, 0.11, 0.90f, 0.90f, 0.90f, 0.24f, 0.10f, 0.78f);
        }

        drawMarkerBox(
                visible,
                occluded,
                previewState.target(),
                cameraPos,
                0.30,
                previewState.reachesTarget() ? 0.48f : 1.0f,
                previewState.reachesTarget() ? 0.98f : 0.38f,
                0.28f,
                0.95f,
                0.35f,
                1.4f
        );
    }

    private static <T> Optional<T> getOptionalMemory(Brain<?> brain, MemoryModuleType<T> type) {
        Optional<T> memory = brain.getOptionalMemory(type);
        return memory == null ? Optional.empty() : memory;
    }

    private boolean drawGlobalPosTarget(MinecraftClient client,
                                        Optional<GlobalPos> globalPos,
                                        VertexConsumer visible,
                                        VertexConsumer occluded,
                                        Vec3d source,
                                        Vec3d cameraPos,
                                        float r,
                                        float g,
                                        float b,
                                        float a,
                                        float occludedAlpha) {
        if (globalPos.isEmpty() || !globalPos.get().dimension().equals(client.world.getRegistryKey())) {
            return false;
        }

        drawTargetLineAndBox(visible, occluded, source, globalPos.get().pos(), cameraPos, r, g, b, a, occludedAlpha, 1.0f);
        return true;
    }

    private void drawBrainDebugTargets(VertexConsumer visible,
                                       VertexConsumer occluded,
                                       Entity entity,
                                       BrainDebugData debugData,
                                       Vec3d cameraPos,
                                       float tickDelta) {
        Vec3d source = entityCenter(entity, tickDelta);

        for (BlockPos poi : debugData.pois()) {
            drawTargetLineAndBox(visible, occluded, source, poi, cameraPos, 0.18f, 0.86f, 1.0f, 0.95f, 0.38f, 1.1f);
        }

        for (BlockPos poi : debugData.potentialPois()) {
            drawTargetLineAndBox(visible, occluded, source, poi, cameraPos, 0.62f, 0.52f, 1.0f, 0.68f, 0.24f, 0.9f);
        }
    }

    private void drawPath(VertexConsumer visible,
                          VertexConsumer occluded,
                          Path path,
                          Vec3d cameraPos,
                          double nodeHalfSize,
                          boolean drawDebugNodes) {
        if (path == null || path.getLength() <= 0) {
            return;
        }

        int currentNodeIndex = clamp(path.getCurrentNodeIndex(), 0, path.getLength() - 1);
        Vec3d previous = center(path.getNodePos(0));
        for (int i = 1; i < path.getLength(); i++) {
            Vec3d current = center(path.getNodePos(i));
            boolean activeSegment = i >= currentNodeIndex;
            float r = activeSegment ? 1.0f : 0.52f;
            float g = activeSegment ? 0.62f : 0.38f;
            float b = activeSegment ? 0.10f : 0.26f;
            float alpha = activeSegment ? 0.92f : 0.38f;
            drawLineBetween(visible, occluded, previous, current, cameraPos, r, g, b, alpha, alpha * 0.45f, 1.6f);
            previous = current;
        }

        if (MobAiVisualizerModule.INSTANCE.showPathNodes()) {
            for (int i = 0; i < path.getLength(); i++) {
                BlockPos nodePos = path.getNodePos(i);
                boolean visited = i < currentNodeIndex;
                boolean current = i == currentNodeIndex;
                float r = current ? 1.0f : (visited ? 0.55f : 0.25f);
                float g = current ? 0.95f : (visited ? 0.40f : 0.95f);
                float b = current ? 0.18f : (visited ? 0.25f : 0.30f);
                double half = current ? nodeHalfSize + 0.05 : nodeHalfSize;
                drawMarkerBox(visible, occluded, nodePos, cameraPos, half, r, g, b, current ? 0.95f : 0.70f, current ? 0.40f : 0.22f, 1.2f);
            }
        }

        drawMarkerBox(visible, occluded, path.getTarget(), cameraPos, nodeHalfSize + 0.08, 1.0f, 0.26f, 0.26f, 0.95f, 0.35f, 1.35f);

        if (!drawDebugNodes) {
            return;
        }

        Path.DebugNodeInfo debugNodeInfo = path.getDebugNodeInfos();
        if (debugNodeInfo == null) {
            return;
        }

        for (PathNode openNode : debugNodeInfo.openSet()) {
            drawMarkerBox(visible, occluded, openNode.getBlockPos(), cameraPos, 0.14, 0.22f, 0.88f, 1.0f, 0.42f, 0.12f, 0.85f);
        }

        for (PathNode closedNode : debugNodeInfo.closedSet()) {
            drawMarkerBox(visible, occluded, closedNode.getBlockPos(), cameraPos, 0.12, 0.78f, 0.78f, 0.78f, 0.28f, 0.10f, 0.8f);
        }

        for (TargetPathNode targetNode : debugNodeInfo.targetNodes()) {
            float r = targetNode.isReached() ? 0.32f : 1.0f;
            float g = targetNode.isReached() ? 1.0f : 0.34f;
            float b = 0.30f;
            drawMarkerBox(visible, occluded, targetNode.getBlockPos(), cameraPos, 0.16, r, g, b, 0.82f, 0.22f, 1.0f);
        }
    }

    private void drawEntityOutline(VertexConsumer visible,
                                   VertexConsumer occluded,
                                   Entity entity,
                                   Vec3d cameraPos,
                                   float tickDelta,
                                   float r,
                                   float g,
                                   float b,
                                   float alpha,
                                   float occludedAlpha,
                                   float width,
                                   double expand) {
        var box = lerpedBoundingBox(entity, tickDelta).expand(expand);
        float scaledWidth = scaleWidth(width);
        DrawUtil.drawOutlinedBoxSafe(
                visible,
                box.minX - cameraPos.x,
                box.minY - cameraPos.y,
                box.minZ - cameraPos.z,
                box.maxX - cameraPos.x,
                box.maxY - cameraPos.y,
                box.maxZ - cameraPos.z,
                r,
                g,
                b,
                alpha,
                scaledWidth
        );
        DrawUtil.drawOutlinedBoxSafe(
                occluded,
                box.minX - cameraPos.x,
                box.minY - cameraPos.y,
                box.minZ - cameraPos.z,
                box.maxX - cameraPos.x,
                box.maxY - cameraPos.y,
                box.maxZ - cameraPos.z,
                r,
                g,
                b,
                occludedAlpha,
                scaledWidth
        );
    }

    private void drawMarkerBox(VertexConsumer visible,
                               VertexConsumer occluded,
                               BlockPos pos,
                               Vec3d cameraPos,
                               double halfSize,
                               float r,
                               float g,
                               float b,
                               float alpha,
                               float occludedAlpha,
                               float width) {
        float scaledWidth = scaleWidth(width);
        double centerX = pos.getX() + 0.5 - cameraPos.x;
        double centerY = pos.getY() + 0.5 - cameraPos.y;
        double centerZ = pos.getZ() + 0.5 - cameraPos.z;
        double minX = centerX - halfSize;
        double minY = centerY - halfSize;
        double minZ = centerZ - halfSize;
        double maxX = centerX + halfSize;
        double maxY = centerY + halfSize;
        double maxZ = centerZ + halfSize;
        DrawUtil.drawOutlinedBoxSafe(visible, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha, scaledWidth);
        DrawUtil.drawOutlinedBoxSafe(occluded, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, occludedAlpha, scaledWidth);
    }

    private void drawTargetLineAndBox(VertexConsumer visible,
                                      VertexConsumer occluded,
                                      Vec3d source,
                                      BlockPos target,
                                      Vec3d cameraPos,
                                      float r,
                                      float g,
                                      float b,
                                      float alpha,
                                      float occludedAlpha,
                                      float width) {
        Vec3d targetCenter = center(target);
        drawLineBetween(visible, occluded, source, targetCenter, cameraPos, r, g, b, alpha, occludedAlpha, width);
        drawMarkerBox(visible, occluded, target, cameraPos, 0.24, r, g, b, alpha, occludedAlpha, width);
    }

    private void drawLineBetweenEntities(VertexConsumer visible,
                                         VertexConsumer occluded,
                                         Vec3d source,
                                         Vec3d target,
                                         Vec3d cameraPos,
                                         float r,
                                         float g,
                                         float b,
                                         float alpha,
                                         float occludedAlpha,
                                         float width) {
        drawLineBetween(visible, occluded, source, target, cameraPos, r, g, b, alpha, occludedAlpha, width);
    }

    private void drawLineBetween(VertexConsumer visible,
                                 VertexConsumer occluded,
                                 Vec3d source,
                                 Vec3d target,
                                 Vec3d cameraPos,
                                 float r,
                                 float g,
                                 float b,
                                 float alpha,
                                 float occludedAlpha,
                                 float width) {
        float scaledWidth = scaleWidth(width);
        DrawUtil.drawLineSafe(
                visible,
                source.x - cameraPos.x,
                source.y - cameraPos.y,
                source.z - cameraPos.z,
                target.x - cameraPos.x,
                target.y - cameraPos.y,
                target.z - cameraPos.z,
                r,
                g,
                b,
                alpha,
                scaledWidth
        );
        DrawUtil.drawLineSafe(
                occluded,
                source.x - cameraPos.x,
                source.y - cameraPos.y,
                source.z - cameraPos.z,
                target.x - cameraPos.x,
                target.y - cameraPos.y,
                target.z - cameraPos.z,
                r,
                g,
                b,
                occludedAlpha,
                scaledWidth
        );
    }

    private void drawWorldBox(VertexConsumer visible,
                              VertexConsumer occluded,
                              Box box,
                              Vec3d cameraPos,
                              float r,
                              float g,
                              float b,
                              float alpha,
                              float occludedAlpha,
                              float width) {
        float scaledWidth = scaleWidth(width);
        DrawUtil.drawOutlinedBoxSafe(visible,
                box.minX - cameraPos.x,
                box.minY - cameraPos.y,
                box.minZ - cameraPos.z,
                box.maxX - cameraPos.x,
                box.maxY - cameraPos.y,
                box.maxZ - cameraPos.z,
                r, g, b, alpha, scaledWidth);
        DrawUtil.drawOutlinedBoxSafe(occluded,
                box.minX - cameraPos.x,
                box.minY - cameraPos.y,
                box.minZ - cameraPos.z,
                box.maxX - cameraPos.x,
                box.maxY - cameraPos.y,
                box.maxZ - cameraPos.z,
                r, g, b, occludedAlpha, scaledWidth);
    }

    private void drawRelativeBox(VertexConsumer visible,
                                 VertexConsumer occluded,
                                 Entity entity,
                                 Vec3d cameraPos,
                                 float tickDelta,
                                 double minX,
                                 double minY,
                                 double minZ,
                                 double maxX,
                                 double maxY,
                                 double maxZ,
                                 float r,
                                 float g,
                                 float b,
                                 float alpha,
                                 float occludedAlpha,
                                 float width) {
        Vec3d renderPos = lerpedPosition(entity, tickDelta);
        drawWorldBox(visible, occluded, new Box(
                        renderPos.x + minX,
                        renderPos.y + minY,
                        renderPos.z + minZ,
                        renderPos.x + maxX,
                        renderPos.y + maxY,
                        renderPos.z + maxZ),
                cameraPos, r, g, b, alpha, occludedAlpha, width);
    }

    private void drawEntityRadiusSphere(VertexConsumer visible,
                                        VertexConsumer occluded,
                                        Entity entity,
                                        Vec3d cameraPos,
                                        float tickDelta,
                                        double radius,
                                        float r,
                                        float g,
                                        float b,
                                        float alpha,
                                        float occludedAlpha,
                                        float width) {
        Vec3d center = entityCenter(entity, tickDelta);
        drawCirclePlane(visible, center, cameraPos, radius, 42, 0, 1, 0, r, g, b, alpha, width);
        drawCirclePlane(visible, center, cameraPos, radius, 42, 1, 0, 0, r, g, b, alpha, width);
        drawCirclePlane(visible, center, cameraPos, radius, 42, 0, 0, 1, r, g, b, alpha, width);
        drawCirclePlane(occluded, center, cameraPos, radius, 42, 0, 1, 0, r, g, b, occludedAlpha, width);
        drawCirclePlane(occluded, center, cameraPos, radius, 42, 1, 0, 0, r, g, b, occludedAlpha, width);
        drawCirclePlane(occluded, center, cameraPos, radius, 42, 0, 0, 1, r, g, b, occludedAlpha, width);
    }

    private void drawCirclePlane(VertexConsumer buffer,
                                 Vec3d center,
                                 Vec3d cameraPos,
                                 double radius,
                                 int segments,
                                 int axisX,
                                 int axisY,
                                 int axisZ,
                                 float r,
                                 float g,
                                 float b,
                                 float alpha,
                                 float width) {
        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2.0 * i) / segments;
            double angle1 = (Math.PI * 2.0 * (i + 1)) / segments;
            Vec3d p0 = switch (axisX + axisY * 2 + axisZ * 4) {
                case 2 -> new Vec3d(center.x + Math.cos(angle0) * radius, center.y, center.z + Math.sin(angle0) * radius);
                case 1 -> new Vec3d(center.x, center.y + Math.cos(angle0) * radius, center.z + Math.sin(angle0) * radius);
                default -> new Vec3d(center.x + Math.cos(angle0) * radius, center.y + Math.sin(angle0) * radius, center.z);
            };
            Vec3d p1 = switch (axisX + axisY * 2 + axisZ * 4) {
                case 2 -> new Vec3d(center.x + Math.cos(angle1) * radius, center.y, center.z + Math.sin(angle1) * radius);
                case 1 -> new Vec3d(center.x, center.y + Math.cos(angle1) * radius, center.z + Math.sin(angle1) * radius);
                default -> new Vec3d(center.x + Math.cos(angle1) * radius, center.y + Math.sin(angle1) * radius, center.z);
            };
            DrawUtil.drawLineSafe(buffer,
                    p0.x - cameraPos.x, p0.y - cameraPos.y, p0.z - cameraPos.z,
                    p1.x - cameraPos.x, p1.y - cameraPos.y, p1.z - cameraPos.z,
                    r, g, b, alpha, scaleWidth(width));
        }
    }

    private void renderLabels(WorldRenderContext context,
                              VertexConsumerProvider consumers,
                              Vec3d cameraPos,
                              float tickDelta,
                              Iterable<EntityOverlayInfo> overlays) {
        MatrixStack matrices = context.matrices();
        if (matrices == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        float yaw = context.gameRenderer().getCamera().getYaw();
        float pitch = context.gameRenderer().getCamera().getPitch();

        for (EntityOverlayInfo overlay : overlays) {
            if (overlay.lines.isEmpty()) {
                continue;
            }

            Entity entity = overlay.entity;
            Vec3d renderPos = lerpedPosition(entity, tickDelta);
            matrices.push();
            matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y + entity.getHeight() + 0.65, renderPos.z - cameraPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

            double dx = renderPos.x - cameraPos.x;
            double dy = renderPos.y - cameraPos.y;
            double dz = renderPos.z - cameraPos.z;
            double distance = dx * dx + dy * dy + dz * dz;
            float scale = (float) clamp(Math.sqrt(distance) * 0.0025, 0.018, 0.035);
            matrices.scale(-scale, -scale, scale);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            int y = 0;
            for (LabelLine line : overlay.buildLines()) {
                int width = textRenderer.getWidth(line.text);
                textRenderer.draw(
                        line.text,
                        -width / 2f,
                        y,
                        line.color,
                        false,
                        matrix,
                        consumers,
                        TextRenderer.TextLayerType.SEE_THROUGH,
                        0,
                        0x00F000F0
                );
                y += 10;
            }

            matrices.pop();
        }
    }

    private static void addCompactListLine(EntityOverlayInfo overlay, String prefix, List<String> values, int limit, int color) {
        if (values == null || values.isEmpty()) {
            return;
        }

        List<String> compact = values.stream()
                .limit(limit)
                .map(MobAiVisualizerRenderer::compactToken)
                .toList();
        overlay.addLine(prefix + ": " + String.join(", ", compact), color);
    }

    private static void addPathLine(EntityOverlayInfo overlay, String prefix, Path path, int color) {
        if (path == null) {
            overlay.addLine(prefix + ": idle", color);
            return;
        }

        StringBuilder line = new StringBuilder(prefix)
                .append(": ")
                .append(path.getCurrentNodeIndex())
                .append('/')
                .append(path.getLength());
        if (path.isFinished()) {
            line.append(" done");
        } else {
            line.append(" -> ").append(formatPos(path.getTarget()));
        }
        overlay.addLine(line.toString(), color);
    }

    private static void addDebugBrainLines(BrainDebugData debugData, EntityOverlayInfo overlay) {
        String profession = compactToken(debugData.profession());
        if (!profession.isBlank() || debugData.xp() > 0) {
            String roleLine = profession.isBlank() ? "Role: xp " + debugData.xp() : "Role: " + profession + (debugData.xp() > 0 ? " | xp " + debugData.xp() : "");
            overlay.addLine(roleLine, 0xFFF2D49B);
        }

        String healthLine = formatHealthLine(debugData.health(), debugData.maxHealth());
        List<String> status = new ArrayList<>();
        if (debugData.wantsGolem()) {
            status.add("golem");
        }
        if (debugData.angerLevel() >= 0) {
            status.add("anger " + debugData.angerLevel());
        }
        if (!debugData.gossips().isEmpty()) {
            status.add("gossip " + debugData.gossips().size());
        }
        overlay.addLine(status.isEmpty() ? healthLine : healthLine + " | " + String.join(" | ", status), debugData.health() < debugData.maxHealth() ? 0xFFFFB1B1 : 0xFFF4F4F4);

        if (!debugData.inventory().isBlank()) {
            overlay.addLine("Inv: " + debugData.inventory(), 0xFFE4B19E);
        }
    }

    private static String formatHealthLine(float health, float maxHealth) {
        return String.format(Locale.ROOT, "HP: %.1f/%.1f", health, maxHealth);
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String compactId(Identifier id) {
        if (id == null) {
            return "?";
        }
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    private static String compactToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String compact = value;
        int lastSlash = compact.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < compact.length()) {
            compact = compact.substring(lastSlash + 1);
        }
        int lastDot = compact.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < compact.length()) {
            compact = compact.substring(lastDot + 1);
        }
        return truncate(compact, 28);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static Vec3d lerpedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ())
        );
    }

    private static net.minecraft.util.math.Box lerpedBoundingBox(Entity entity, float tickDelta) {
        Vec3d renderPos = lerpedPosition(entity, tickDelta);
        return entity.getBoundingBox().offset(renderPos.x - entity.getX(), renderPos.y - entity.getY(), renderPos.z - entity.getZ());
    }

    private static Vec3d entityCenter(Entity entity, float tickDelta) {
        Vec3d renderPos = lerpedPosition(entity, tickDelta);
        return new Vec3d(renderPos.x, renderPos.y + entity.getHeight() * 0.5, renderPos.z);
    }

    private static Vec3d center(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean isTrackedVillagerHostile(VillagerEntity villager, LivingEntity entity, String hostileFocus) {
        String type = compactId(Registries.ENTITY_TYPE.getId(entity.getType()));
        if ("all".equals(hostileFocus)) {
            Integer radius = TRACKER_HOSTILE_RANGES.get(type);
            return radius != null && villager.squaredDistanceTo(entity) <= (double) radius * (double) radius;
        }
        Integer radius = TRACKER_HOSTILE_RANGES.get(hostileFocus);
        return radius != null && hostileFocus.equals(type) && villager.squaredDistanceTo(entity) <= (double) radius * (double) radius;
    }

    private static int countVillagerFoodPoints(VillagerEntity villager) {
        int foodPoints = 0;
        for (int slot = 0; slot < villager.getInventory().size(); slot++) {
            ItemStack stack = villager.getInventory().getStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Integer points = VILLAGER_FOOD_POINTS.get(compactId(Registries.ITEM.getId(stack.getItem())));
            if (points != null) {
                foodPoints += points * stack.getCount();
            }
        }
        return foodPoints;
    }

    private static float alphaFromByte(int alpha, float multiplier) {
        return Math.clamp((alpha / 255.0f) * multiplier, 0.05f, 1.0f);
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    private static float scaleWidth(float width) {
        return width * WIDTH_MULTIPLIER;
    }

    private record LabelLine(String text, int color) {
    }

    private static final class EntityOverlayInfo {
        private final Entity entity;
        private final List<LabelLine> lines = new ArrayList<>();
        private boolean focused;
        private boolean pathDebug;
        private boolean brainDebug;
        private boolean clientPath;
        private boolean clientBrain;
        private boolean previewPath;
        private boolean inspected;
        private boolean tracker;
        private boolean idleVisual;

        private EntityOverlayInfo(Entity entity) {
            this.entity = entity;
        }

        private void addLine(String text, int color) {
            if (text == null || text.isBlank()) {
                return;
            }
            int limit = inspected ? 16 : previewPath ? 12 : (focused || tracker) ? 10 : 6;
            if (lines.size() >= limit) {
                return;
            }
            lines.add(new LabelLine(truncate(text, 56), color));
        }

        private boolean hasVisualMarker() {
            return pathDebug || brainDebug || clientPath || clientBrain || previewPath || inspected || tracker || idleVisual;
        }

        private List<LabelLine> buildLines() {
            List<LabelLine> built = new ArrayList<>();
            StringBuilder header = new StringBuilder(truncate(entity.getName().getString(), 24));
            List<String> badges = new ArrayList<>();
            if (pathDebug) {
                badges.add("P*");
            } else if (clientPath) {
                badges.add("P");
            }
            if (brainDebug) {
                badges.add("B*");
            } else if (clientBrain) {
                badges.add("B");
            }
            if (previewPath) {
                badges.add("SIM");
            }
            if (inspected) {
                badges.add("D");
            }
            if (tracker) {
                badges.add("T");
            }
            if (idleVisual && badges.isEmpty()) {
                badges.add("I");
            }
            if (!badges.isEmpty()) {
                header.append(" [").append(String.join("/", badges)).append(']');
            }
            built.add(new LabelLine(header.toString(), 0xFFF4F4F4));
            built.addAll(lines);
            return built;
        }
    }
}
