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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.TargetPathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.BrainDebugData;
import org.joml.Matrix4f;

import java.util.*;

final class MobAiVisualizerRenderer {
    private static final float WIDTH_MULTIPLIER = 1.55f;

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
        if (!module.isEnabled() && !hasCommandData) {
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

        renderCommandDebugData(client, cameraPos, tickDelta, visible, occluded, overlays);

        if (module.showLabels() || hasCommandData) {
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

    private static int clamp(int value, int min, int max) {
        return Math.clamp(max, min, value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.clamp(max, min, value);
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
        private boolean idleVisual;

        private EntityOverlayInfo(Entity entity) {
            this.entity = entity;
        }

        private void addLine(String text, int color) {
            if (text == null || text.isBlank()) {
                return;
            }
            int limit = inspected ? 16 : previewPath ? 12 : focused ? 8 : 5;
            if (lines.size() >= limit) {
                return;
            }
            lines.add(new LabelLine(truncate(text, 56), color));
        }

        private boolean hasVisualMarker() {
            return pathDebug || brainDebug || clientPath || clientBrain || previewPath || inspected || idleVisual;
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
