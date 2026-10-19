package me.m0dii.modules.camera;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.macros.MacroPlaceholderProvider;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.ConfigFileSafety;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class CameraPathManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = M0DevToolsClient.SETTINGS_FOLDER.toPath().resolve("camera-paths.json");

    private static final MacroPlaceholderProvider PLACEHOLDER_PROVIDER = new MacroPlaceholderProvider() {
        @Override
        public String getProviderId() {
            return "camera";
        }

        @Override
        public List<String> getPlaceholderDocs() {
            return List.of(
                    "[Module placeholders: Camera]",
                    "{camera.playing} => true while a camera path is playing",
                    "{camera.points} => current working path point count",
                    "{camera.selected} => selected point index, 0 when nothing is selected",
                    "{camera.interpolation} => current interpolation mode",
                    "{camera.visible} => true when the camera path overlay is visible"
            );
        }

        @Override
        public List<String> getKnownPlaceholderTokens() {
            return List.of(
                    "camera.playing",
                    "camera.points",
                    "camera.selected",
                    "camera.interpolation",
                    "camera.visible"
            );
        }

        @Override
        public String resolvePlaceholder(String token, Minecraft client, Player player, boolean canvasMode) {
            return switch (token) {
                case "camera.playing" -> Boolean.toString(isPlaybackActive());
                case "camera.points" -> Integer.toString(workingPath.points.size());
                case "camera.selected" -> Integer.toString(selectedIndex >= 0 ? selectedIndex + 1 : 0);
                case "camera.interpolation" -> workingPath.interpolation.name().toLowerCase(Locale.ROOT);
                case "camera.visible" -> Boolean.toString(overlayVisible);
                default -> null;
            };
        }
    };

    private static final Map<String, CameraPath> SAVED_PATHS = new LinkedHashMap<>();

    private static CameraPath workingPath = new CameraPath(null, "", Interpolation.CATMULL_ROM, new ArrayList<>());
    private static boolean overlayVisible = true;
    private static int selectedIndex = -1;
    private static @Nullable PlaybackController playback;
    private static boolean registered;

    private CameraPathManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        loadFromDisk();
        MacroPlaceholders.registerProvider(PLACEHOLDER_PROVIDER);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetRuntimeState());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickRecording());
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!overlayVisible) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || workingPath.points.isEmpty()) {
                return;
            }
            if (!currentDimensionId().equals(workingPath.dimensionId)) {
                return;
            }

            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }

            VertexConsumer visible = consumers.getBuffer(RenderTypes.LINES);
            VertexConsumer through = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            Vec3 cameraPos = context.gameRenderer().mainCamera().position();

            renderPathLines(visible, through, cameraPos);
            renderKeyframes(visible, through, cameraPos);
            renderLabels(context.matrices(), consumers, context.gameRenderer().mainCamera(), cameraPos, client.font);
        });
    }

    public static void onClientWorldUnavailable() {
        if (playback != null) {
            playback.stop();
            playback = null;
        }
    }

    public static boolean isPlaybackActive() {
        if (playback == null) {
            return false;
        }
        if (!CameraEntity.hasController()) {
            playback = null;
            return false;
        }
        return playback.isActive();
    }

    public static void applyRenderPose(float tickProgress) {
        if (playback == null || !playback.isActive()) {
            return;
        }

        CameraEntity camera = CameraEntity.getCamera();
        if (camera == null) {
            return;
        }

        CameraPose pose = samplePose(playback.path, playback.renderTime(tickProgress));
        camera.setRenderPoseExact(pose.position, pose.yaw, pose.pitch);
    }

    public static List<String> savedPathNames() {
        return SAVED_PATHS.values().stream()
                .sorted(Comparator.comparing(path -> path.name == null ? "" : path.name.toLowerCase(Locale.ROOT)))
                .map(path -> path.name == null ? "" : path.name)
                .filter(name -> !name.isBlank())
                .toList();
    }

    public static int pointCount() {
        return workingPath.points.size();
    }

    public static List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        String name = workingPath.name == null ? "unsaved" : workingPath.name;
        lines.add("Path: " + name + " | points=" + workingPath.points.size()
                + " | interp=" + workingPath.interpolation.name().toLowerCase(Locale.ROOT));
        if (recording) {
            lines.add("RECORDING · " + workingPath.points.size() + " points · " + formatSeconds(recordTicks / 20.0));
        }
        lines.add("Dimension: " + (workingPath.dimensionId.isBlank() ? "unset" : workingPath.dimensionId)
                + " | overlay=" + (overlayVisible ? "shown" : "hidden")
                + " | playing=" + (isPlaybackActive() ? "yes" : "no"));
        lines.add("Selected: " + (selectedIndex >= 0 ? selectedIndex + 1 : "none")
                + " | duration=" + formatSeconds(totalDurationTicks(workingPath) / 20.0));
        if (!workingPath.points.isEmpty()) {
            Keyframe first = workingPath.points.getFirst();
            Keyframe last = workingPath.points.getLast();
            lines.add("Start: " + formatKeyframe(first));
            if (workingPath.points.size() > 1) {
                lines.add("End: " + formatKeyframe(last));
            }
        }
        return lines;
    }

    public static String startPath() {
        stopPlaybackInternal();
        CameraPose pose = currentPose();
        workingPath = new CameraPath(null, currentDimensionId(), workingPath.interpolation, new ArrayList<>(List.of(
                new Keyframe(pose.position, pose.yaw, pose.pitch, 0)
        )));
        selectedIndex = 0;
        saveToDisk();
        return "Started camera path at the current camera position.";
    }

    public static String clearPath() {
        stopPlaybackInternal();
        workingPath = new CameraPath(null, currentDimensionIdOrEmpty(), workingPath.interpolation, new ArrayList<>());
        selectedIndex = -1;
        saveToDisk();
        return "Cleared the working camera path.";
    }

    public static String addPoint(double seconds) {
        CameraPose pose = currentPose();
        if (workingPath.points.isEmpty()) {
            workingPath = new CameraPath(null, currentDimensionId(), workingPath.interpolation, new ArrayList<>(List.of(
                    new Keyframe(pose.position, pose.yaw, pose.pitch, 0)
            )));
            selectedIndex = 0;
            saveToDisk();
            return "Started camera path at the current position. Move the camera, then run /camera add again.";
        }

        ensureDimensionMatchesCurrent();
        Keyframe lastPoint = workingPath.points.getLast();
        if (samePose(lastPoint, pose)) {
            throw new IllegalStateException("Current camera pose matches the last point. Move the camera or use /camera duration.");
        }

        int nextTime = lastPoint.timeTicks + secondsToTicks(seconds);
        workingPath.points.add(new Keyframe(pose.position, pose.yaw, pose.pitch, nextTime));
        selectedIndex = workingPath.points.size() - 1;
        saveToDisk();
        return "Added point " + (selectedIndex + 1) + " at +" + formatSeconds(seconds) + ".";
    }

    public static String prependPoint(double seconds) {
        CameraPose pose = currentPose();
        if (workingPath.points.isEmpty()) {
            workingPath = new CameraPath(null, currentDimensionId(), workingPath.interpolation, new ArrayList<>(List.of(
                    new Keyframe(pose.position, pose.yaw, pose.pitch, 0)
            )));
            selectedIndex = 0;
            saveToDisk();
            return "Started camera path at the current position. Move the camera, then run /camera prepend again if needed.";
        }

        ensureDimensionMatchesCurrent();
        if (samePose(workingPath.points.getFirst(), pose)) {
            throw new IllegalStateException("Current camera pose matches the first point. Move the camera before prepending.");
        }

        int delta = secondsToTicks(seconds);
        List<Keyframe> shifted = new ArrayList<>(workingPath.points.size() + 1);
        shifted.add(new Keyframe(pose.position, pose.yaw, pose.pitch, 0));
        for (Keyframe point : workingPath.points) {
            shifted.add(new Keyframe(point.position, point.yaw, point.pitch, point.timeTicks + delta));
        }
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, shifted);
        selectedIndex = 0;
        saveToDisk();
        return "Prepended a new start point " + formatSeconds(seconds) + " before the old path.";
    }

    public static String selectNearest() {
        ensurePathExists();
        Vec3 current = currentPose().position;
        int bestIndex = -1;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int i = 0; i < workingPath.points.size(); i++) {
            double distanceSq = workingPath.points.get(i).position.distanceToSqr(current);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestIndex = i;
            }
        }
        selectedIndex = bestIndex;
        saveToDisk();
        return "Selected point " + (selectedIndex + 1) + ".";
    }

    public static String selectPoint(int oneBasedIndex) {
        ensurePathExists();
        int targetIndex = oneBasedIndex - 1;
        if (targetIndex < 0 || targetIndex >= workingPath.points.size()) {
            throw new IllegalStateException("Point index must be between 1 and " + workingPath.points.size() + ".");
        }
        selectedIndex = targetIndex;
        saveToDisk();
        return "Selected point " + oneBasedIndex + ".";
    }

    public static String moveSelectedToCurrent() {
        ensureSelectedPoint();
        ensureDimensionMatchesCurrent();
        CameraPose pose = currentPose();
        Keyframe selected = workingPath.points.get(selectedIndex);
        workingPath.points.set(selectedIndex, new Keyframe(pose.position, pose.yaw, pose.pitch, selected.timeTicks));
        saveToDisk();
        return "Moved point " + (selectedIndex + 1) + " to the current camera position.";
    }

    public static String setSelectedSegmentDuration(double seconds) {
        ensurePathHasSegments();
        int ticks = secondsToTicks(seconds);
        List<Keyframe> updated = new ArrayList<>(workingPath.points);

        if (selectedIndex >= updated.size() - 1) {
            int lastIndex = updated.size() - 1;
            Keyframe previous = updated.get(lastIndex - 1);
            Keyframe last = updated.get(lastIndex);
            updated.set(lastIndex, new Keyframe(last.position, last.yaw, last.pitch, previous.timeTicks + ticks));
        } else {
            int nextIndex = selectedIndex + 1;
            Keyframe current = updated.get(selectedIndex);
            int oldNextTime = updated.get(nextIndex).timeTicks;
            int newNextTime = current.timeTicks + ticks;
            int delta = newNextTime - oldNextTime;
            for (int i = nextIndex; i < updated.size(); i++) {
                Keyframe point = updated.get(i);
                int nextTime = i == nextIndex ? newNextTime : point.timeTicks + delta;
                updated.set(i, new Keyframe(point.position, point.yaw, point.pitch, nextTime));
            }
        }

        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
        saveToDisk();
        return "Updated the selected segment duration to " + formatSeconds(seconds) + ".";
    }

    public static String splitSelectedSegment() {
        ensurePathHasSegments();
        if (selectedIndex >= workingPath.points.size() - 1) {
            throw new IllegalStateException("Select a point that has a following segment to split.");
        }

        Keyframe current = workingPath.points.get(selectedIndex);
        Keyframe next = workingPath.points.get(selectedIndex + 1);
        int midpointTime = current.timeTicks + Math.max(1, (next.timeTicks - current.timeTicks) / 2);
        CameraPose midpoint = samplePose(workingPath, midpointTime);

        List<Keyframe> updated = new ArrayList<>(workingPath.points);
        updated.add(selectedIndex + 1, new Keyframe(midpoint.position, midpoint.yaw, midpoint.pitch, midpointTime));
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
        selectedIndex++;
        saveToDisk();
        return "Split the segment and inserted point " + (selectedIndex + 1) + ".";
    }

    public static String deleteSelectedPoint() {
        ensureSelectedPoint();
        List<Keyframe> updated = new ArrayList<>(workingPath.points);
        updated.remove(selectedIndex);
        if (updated.isEmpty()) {
            workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
            selectedIndex = -1;
        } else {
            int timeShift = updated.getFirst().timeTicks;
            if (timeShift != 0) {
                for (int i = 0; i < updated.size(); i++) {
                    Keyframe point = updated.get(i);
                    updated.set(i, new Keyframe(point.position, point.yaw, point.pitch, point.timeTicks - timeShift));
                }
            }
            workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
            selectedIndex = Math.clamp(selectedIndex, 0, updated.size() - 1);
        }
        saveToDisk();
        return "Deleted the selected point.";
    }

    public static String trimAfterSelected() {
        ensureSelectedPoint();
        List<Keyframe> trimmed = new ArrayList<>(workingPath.points.subList(0, selectedIndex + 1));
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, trimmed);
        selectedIndex = trimmed.size() - 1;
        saveToDisk();
        return "Trimmed all points after the selected point.";
    }

    public static String transposeToCurrent() {
        ensurePathExists();
        ensureDimensionMatchesCurrent();
        CameraPose pose = currentPose();
        Vec3 delta = pose.position.subtract(workingPath.points.getFirst().position);
        List<Keyframe> updated = new ArrayList<>(workingPath.points.size());
        for (Keyframe point : workingPath.points) {
            updated.add(new Keyframe(point.position.add(delta), point.yaw, point.pitch, point.timeTicks));
        }
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
        saveToDisk();
        return "Transposed the entire path so the first point matches the current camera position.";
    }

    public static String stretchTimeline(int percent) {
        ensurePathHasSegments();
        int clamped = Math.clamp(percent, 25, 400);
        List<Keyframe> updated = new ArrayList<>(workingPath.points.size());
        updated.add(workingPath.points.getFirst());
        for (int i = 1; i < workingPath.points.size(); i++) {
            Keyframe point = workingPath.points.get(i);
            int scaledTime = Math.max(updated.get(i - 1).timeTicks + 1, Math.round(point.timeTicks * (clamped / 100.0f)));
            updated.add(new Keyframe(point.position, point.yaw, point.pitch, scaledTime));
        }
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, workingPath.interpolation, updated);
        saveToDisk();
        return "Scaled the timeline to " + clamped + "%.";
    }

    public static String setInterpolation(String token) {
        return setInterpolation(parseInterpolation(token));
    }

    private static String setInterpolation(Interpolation interpolation) {
        workingPath = new CameraPath(workingPath.name, workingPath.dimensionId, interpolation, new ArrayList<>(workingPath.points));
        saveToDisk();
        return "Interpolation set to " + interpolation.name().toLowerCase(Locale.ROOT) + ".";
    }

    public static String showOverlay() {
        overlayVisible = true;
        saveToDisk();
        return "Camera path overlay enabled.";
    }

    public static String hideOverlay() {
        overlayVisible = false;
        saveToDisk();
        return "Camera path overlay hidden.";
    }

    public static String play(int loops) {
        ensurePathHasSegments();
        ensureDimensionMatchesCurrent();
        stopPlaybackInternal();

        PlaybackController controller = new PlaybackController(workingPath.copy(), Math.max(1, loops));
        playback = controller;
        CameraEntity.setController(controller);
        CameraEntity.setCameraState(true);
        CameraEntity camera = CameraEntity.getCamera();
        if (camera != null) {
            CameraPose pose = samplePose(controller.path, 0);
            camera.setScriptedPose(pose.position, pose.yaw, pose.pitch);
        }
        return "Playing camera path" + (loops > 1 ? " (" + loops + " loops)" : "") + ".";
    }

    public static String stopPlayback() {
        if (!isPlaybackActive()) {
            return "Camera path playback is not running.";
        }
        stopPlaybackInternal();
        return "Stopped camera path playback.";
    }

    public static String saveWorkingPath(String rawName) {
        ensurePathExists();
        String name = normalizeName(rawName);
        if (name.isBlank()) {
            throw new IllegalStateException("Path name cannot be empty.");
        }
        CameraPath saved = new CameraPath(rawName.trim(), workingPath.dimensionId, workingPath.interpolation, new ArrayList<>(workingPath.points));
        SAVED_PATHS.put(name, saved);
        workingPath = saved.copy();
        saveToDisk();
        return "Saved camera path as '" + saved.name + "'.";
    }

    public static String loadPath(String rawName) {
        String name = normalizeName(rawName);
        CameraPath saved = SAVED_PATHS.get(name);
        if (saved == null) {
            throw new IllegalStateException("No saved path named '" + rawName + "'.");
        }
        stopPlaybackInternal();
        workingPath = saved.copy();
        selectedIndex = workingPath.points.isEmpty() ? -1 : 0;
        saveToDisk();
        return "Loaded camera path '" + saved.name + "'.";
    }

    public static String deleteSavedPath(String rawName) {
        String name = normalizeName(rawName);
        CameraPath removed = SAVED_PATHS.remove(name);
        if (removed == null) {
            throw new IllegalStateException("No saved path named '" + rawName + "'.");
        }
        saveToDisk();
        return "Deleted saved camera path '" + removed.name + "'.";
    }

    public static String listPaths() {
        if (SAVED_PATHS.isEmpty()) {
            return "No saved camera paths.";
        }
        List<String> names = savedPathNames();
        return "Saved paths: " + String.join(", ", names);
    }

    private static void stopPlaybackInternal() {
        if (playback != null) {
            playback.stop();
            playback = null;
        }
        CameraEntity.clearController();
    }

    private static void resetRuntimeState() {
        if (recording) {
            finishRecording();
        }
        stopPlaybackInternal();
    }

    // ---- recording ----------------------------------------------------------------------------------------

    private static final int MAX_RECORDED_POINTS = 2000;
    private static boolean recording;
    private static int recordTicks;
    private static int recordIntervalTicks;
    private static int lastRecordedTick;
    private static String recordingDimension = "";

    public static boolean isRecording() {
        return recording;
    }

    /**
     * Starts recording the camera (or player) as it moves: a keyframe every {@code intervalSeconds} while moving,
     * plus one when it stops so pauses are kept. Replaces the working path.
     */
    public static String startRecording(double intervalSeconds) {
        requireClientReady();
        if (recording) {
            return "Already recording. Use /camera record stop to finish.";
        }
        stopPlaybackInternal();
        CameraPose pose = currentPose();
        recordingDimension = currentDimensionId();
        workingPath = new CameraPath(null, recordingDimension, workingPath.interpolation, new ArrayList<>(List.of(
                new Keyframe(pose.position, pose.yaw, pose.pitch, 0)
        )));
        selectedIndex = 0;
        recordIntervalTicks = secondsToTicks(intervalSeconds);
        recordTicks = 0;
        lastRecordedTick = 0;
        recording = true;
        return "Recording: move the camera (freecam works well), then /camera record stop. A point every "
                + formatSeconds(recordIntervalTicks / 20.0) + ".";
    }

    public static String stopRecording() {
        if (!recording) {
            return "Not recording. Start with /camera record [seconds between points].";
        }
        return finishRecording();
    }

    private static String finishRecording() {
        recording = false;
        try {
            CameraPose pose = currentPose();
            Keyframe last = workingPath.points.getLast();
            if (!samePose(last, pose) && recordTicks > last.timeTicks) {
                workingPath.points.add(new Keyframe(pose.position, pose.yaw, pose.pitch, recordTicks));
            }
        } catch (IllegalStateException ignored) {
            // no camera any more (disconnected); keep what was recorded
        }
        selectedIndex = workingPath.points.size() - 1;
        saveToDisk();
        return "Recorded " + workingPath.points.size() + " points over "
                + formatSeconds(totalDurationTicks(workingPath) / 20.0) + ". Use /camera play to watch it, /camera save <name> to keep it.";
    }

    private static void tickRecording() {
        if (!recording) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !recordingDimension.equals(currentDimensionIdOrEmpty())) {
            String message = finishRecording();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("[Camera] Recording stopped (dimension changed). " + message));
            }
            return;
        }
        recordTicks++;
        if (recordTicks - lastRecordedTick < recordIntervalTicks) {
            return;
        }
        CameraPose pose = currentPose();
        List<Keyframe> points = workingPath.points;
        Keyframe last = points.getLast();
        boolean moved = !samePose(last, pose);
        boolean lastWasMoving = points.size() < 2 || !samePose(points.get(points.size() - 2), last);
        if (moved || lastWasMoving) {
            // While still, add one "hold" keyframe so the pause is kept in playback.
            points.add(new Keyframe(pose.position, pose.yaw, pose.pitch, recordTicks));
            lastRecordedTick = recordTicks;
        }
        if (points.size() >= MAX_RECORDED_POINTS) {
            String message = finishRecording();
            client.player.sendSystemMessage(Component.literal("[Camera] Recording stopped at " + MAX_RECORDED_POINTS + " points. " + message));
        }
    }

    private static void ensureEditablePath() {
        requireClientReady();
        if (workingPath.points.isEmpty()) {
            startPath();
        } else {
            ensureDimensionMatchesCurrent();
        }
    }

    private static void ensurePathExists() {
        if (workingPath.points.isEmpty()) {
            throw new IllegalStateException("No camera path is loaded. Run /camera start first.");
        }
    }

    private static void ensureSelectedPoint() {
        ensurePathExists();
        if (selectedIndex < 0 || selectedIndex >= workingPath.points.size()) {
            throw new IllegalStateException("No camera point is selected.");
        }
    }

    private static void ensurePathHasSegments() {
        ensureSelectedPoint();
        if (workingPath.points.size() < 2) {
            throw new IllegalStateException("The camera path needs at least two points.");
        }
    }

    private static void ensureDimensionMatchesCurrent() {
        String currentDimension = currentDimensionId();
        if (!workingPath.dimensionId.equals(currentDimension)) {
            throw new IllegalStateException("Current world dimension does not match the working path dimension.");
        }
    }

    private static void requireClientReady() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            throw new IllegalStateException("You must be in a world to edit camera paths.");
        }
    }

    private static CameraPose currentPose() {
        Minecraft client = Minecraft.getInstance();
        Entity entity = CameraEntity.getCamera();
        if (entity == null) {
            entity = client.getCameraEntity();
        }
        if (entity == null) {
            entity = client.player;
        }
        if (entity == null) {
            throw new IllegalStateException("No active camera entity is available.");
        }
        return new CameraPose(new Vec3(entity.getX(), entity.getY(), entity.getZ()), entity.getYRot(), entity.getXRot());
    }

    private static String currentDimensionId() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            throw new IllegalStateException("You must be in a world to use camera paths.");
        }
        return client.level.dimension().identifier().toString();
    }

    private static String currentDimensionIdOrEmpty() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? "" : client.level.dimension().identifier().toString();
    }

    private static CameraPose samplePose(CameraPath path, int timeTicks) {
        return samplePose(path, (double) timeTicks);
    }

    private static CameraPose samplePose(CameraPath path, double timeTicks) {
        if (path.points.isEmpty()) {
            return new CameraPose(Vec3.ZERO, 0.0f, 0.0f);
        }
        if (path.points.size() == 1) {
            Keyframe only = path.points.getFirst();
            return new CameraPose(only.position, only.yaw, only.pitch);
        }

        double clampedTime = Math.clamp(timeTicks, 0.0, path.points.getLast().timeTicks);
        int segment = 0;
        while (segment < path.points.size() - 2 && clampedTime > path.points.get(segment + 1).timeTicks) {
            segment++;
        }

        Keyframe p1 = path.points.get(segment);
        Keyframe p2 = path.points.get(segment + 1);
        int duration = Math.max(1, p2.timeTicks - p1.timeTicks);
        double t = Math.clamp((clampedTime - p1.timeTicks) / duration, 0.0, 1.0);

        Keyframe p0 = segment > 0 ? path.points.get(segment - 1) : p1;
        Keyframe p3 = segment + 2 < path.points.size() ? path.points.get(segment + 2) : p2;

        Vec3 position = switch (path.interpolation) {
            case LINEAR -> p1.position.lerp(p2.position, t);
            case CATMULL_ROM -> catmullRom(p0.position, p1.position, p2.position, p3.position, t);
        };

        float yaw = switch (path.interpolation) {
            case LINEAR -> Mth.rotLerp((float) t, p1.yaw, p2.yaw);
            case CATMULL_ROM -> Mth.wrapDegrees(catmullRomAngle(p0.yaw, p1.yaw, p2.yaw, p3.yaw, t));
        };
        float pitch = switch (path.interpolation) {
            case LINEAR -> Mth.rotLerp((float) t, p1.pitch, p2.pitch);
            case CATMULL_ROM -> Mth.clamp(catmullRomAngle(p0.pitch, p1.pitch, p2.pitch, p3.pitch, t), -90.0f, 90.0f);
        };
        return new CameraPose(position, yaw, pitch);
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(
                0.5 * ((2.0 * p1.x) + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3),
                0.5 * ((2.0 * p1.y) + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3),
                0.5 * ((2.0 * p1.z) + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3)
        );
    }

    private static float catmullRomAngle(float a0, float a1, float a2, float a3, double t) {
        double p1 = a1;
        double p0 = normalizeAngleAround(p1, a0);
        double p2 = normalizeAngleAround(p1, a2);
        double p3 = normalizeAngleAround(p2, a3);
        double t2 = t * t;
        double t3 = t2 * t;
        return (float) (0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3));
    }

    private static double normalizeAngleAround(double reference, double value) {
        double delta = Mth.wrapDegrees((float) (value - reference));
        return reference + delta;
    }

    private static void renderPathLines(VertexConsumer visible, VertexConsumer through, Vec3 cameraPos) {
        float[] visibleColor = color(0x3CE8FF, 0.95f);
        float[] throughColor = color(0x3CE8FF, 0.30f);
        if (workingPath.points.size() == 1) {
            return;
        }

        CameraPose previous = samplePose(workingPath, 0);
        int totalTicks = totalDurationTicks(workingPath);
        int sampleStep = 1;
        for (int tick = sampleStep; tick <= totalTicks; tick += sampleStep) {
            CameraPose current = samplePose(workingPath, Math.min(tick, totalTicks));
            drawSegment(visible, previous.position, current.position, cameraPos, visibleColor, 1.6f);
            drawSegment(through, previous.position, current.position, cameraPos, throughColor, 1.6f);
            previous = current;
        }
    }

    private static void renderKeyframes(VertexConsumer visible, VertexConsumer through, Vec3 cameraPos) {
        for (int i = 0; i < workingPath.points.size(); i++) {
            Keyframe point = workingPath.points.get(i);
            boolean selected = i == selectedIndex;
            float[] visibleColor = color(selected ? 0xFFE45A : 0x7FFFD4, selected ? 1.0f : 0.90f);
            float[] throughColor = color(selected ? 0xFFE45A : 0x7FFFD4, selected ? 0.35f : 0.26f);
            double radius = selected ? 0.35 : 0.22;

            drawBox(visible, point.position, cameraPos, radius, visibleColor, 1.75f);
            drawBox(through, point.position, cameraPos, radius, throughColor, 1.75f);

            Vec3 lookDirection = Vec3.directionFromRotation(point.pitch, point.yaw);
            Vec3 lookEnd = point.position.add(lookDirection.scale(selected ? 1.6 : 1.1));
            drawSegment(visible, point.position, lookEnd, cameraPos, visibleColor, 1.3f);
            drawSegment(through, point.position, lookEnd, cameraPos, throughColor, 1.3f);
        }
    }

    private static void renderLabels(@Nullable PoseStack matrices,
                                     OverlayBufferSource consumers,
                                     net.minecraft.client.Camera camera,
                                     Vec3 cameraPos,
                                     Font textRenderer) {
        if (matrices == null) {
            return;
        }

        for (int i = 0; i < workingPath.points.size(); i++) {
            if (i != selectedIndex && i != 0 && i != workingPath.points.size() - 1 && i % 4 != 0) {
                continue;
            }

            Keyframe point = workingPath.points.get(i);
            double distance = cameraPos.distanceTo(point.position);
            if (distance > 160.0) {
                continue;
            }

            String label = buildLabel(i, point);
            matrices.pushPose();
            matrices.translate(point.position.x - cameraPos.x, point.position.y - cameraPos.y + 0.8, point.position.z - cameraPos.z);
            matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
            matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));
            float scale = (float) Math.clamp(distance * 0.0024, 0.018, 0.045);
            matrices.scale(-scale, -scale, scale);

            Matrix4f matrix = matrices.last().pose();
            int color = i == selectedIndex ? 0xFFFFE17A : 0xFFB8FFF2;
            int width = textRenderer.width(label);
            OverlayBufferSource.drawText(textRenderer, label,
                    -width / 2f,
                    0,
                    color,
                    false,
                    matrix,
                    consumers,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    0x00F000F0);
            matrices.popPose();
        }
    }

    private static String buildLabel(int index, Keyframe point) {
        StringBuilder label = new StringBuilder()
                .append('#')
                .append(index + 1)
                .append(" @ ")
                .append(formatSeconds(point.timeTicks / 20.0));

        if (workingPath.points.size() > 1) {
            if (index < workingPath.points.size() - 1) {
                double segment = (workingPath.points.get(index + 1).timeTicks - point.timeTicks) / 20.0;
                label.append(" -> ").append(formatSeconds(segment));
            } else if (index > 0) {
                double segment = (point.timeTicks - workingPath.points.get(index - 1).timeTicks) / 20.0;
                label.append(" <- ").append(formatSeconds(segment));
            }
        }
        return label.toString();
    }

    private static void drawBox(VertexConsumer buffer, Vec3 worldPos, Vec3 cameraPos, double radius, float[] color, float width) {
        DrawUtil.drawOutlinedBoxSafe(buffer,
                worldPos.x - radius - cameraPos.x,
                worldPos.y - radius - cameraPos.y,
                worldPos.z - radius - cameraPos.z,
                worldPos.x + radius - cameraPos.x,
                worldPos.y + radius - cameraPos.y,
                worldPos.z + radius - cameraPos.z,
                color[0], color[1], color[2], color[3], width);
    }

    private static void drawSegment(VertexConsumer buffer,
                                    Vec3 start,
                                    Vec3 end,
                                    Vec3 cameraPos,
                                    float[] color,
                                    float width) {
        DrawUtil.drawLineSafe(buffer,
                start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z,
                end.x - cameraPos.x, end.y - cameraPos.y, end.z - cameraPos.z,
                color[0], color[1], color[2], color[3], width);
    }

    private static float[] color(int rgb, float alpha) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f,
                alpha
        };
    }

    private static int totalDurationTicks(CameraPath path) {
        return path.points.isEmpty() ? 0 : path.points.getLast().timeTicks;
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, Math.round((float) (seconds * 20.0)));
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    private static String formatKeyframe(Keyframe point) {
        return String.format(Locale.ROOT,
                "%.2f %.2f %.2f yaw %.1f pitch %.1f",
                point.position.x, point.position.y, point.position.z, point.yaw, point.pitch);
    }

    private static String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean samePose(Keyframe point, CameraPose pose) {
        return point.position.distanceToSqr(pose.position) <= 1.0e-6
                && angleDistance(point.yaw, pose.yaw) <= 0.05f
                && angleDistance(point.pitch, pose.pitch) <= 0.05f;
    }

    private static boolean samePose(Keyframe first, Keyframe second) {
        return samePose(first, new CameraPose(second.position, second.yaw, second.pitch));
    }

    private static float angleDistance(float first, float second) {
        return Math.abs(Mth.wrapDegrees(first - second));
    }

    private static void saveToDisk() {
        JsonObject root = new JsonObject();
        root.addProperty("overlayVisible", overlayVisible);
        if (!workingPath.points.isEmpty()) {
            root.add("workingPath", writePath(workingPath));
            root.addProperty("selectedIndex", selectedIndex);
        }

        JsonArray saved = new JsonArray();
        for (CameraPath path : SAVED_PATHS.values()) {
            saved.add(writePath(path));
        }
        root.add("savedPaths", saved);

        try {
            Files.createDirectories(SAVE_PATH.getParent());
            ConfigFileSafety.writeAtomically(SAVE_PATH, GSON.toJson(root));
        } catch (Exception ignored) {
        }
    }

    private static void loadFromDisk() {
        SAVED_PATHS.clear();
        workingPath = new CameraPath(null, "", Interpolation.CATMULL_ROM, new ArrayList<>());
        selectedIndex = -1;
        overlayVisible = true;

        try {
            if (!Files.exists(SAVE_PATH)) {
                return;
            }

            JsonObject root = GSON.fromJson(Files.readString(SAVE_PATH), JsonObject.class);
            if (root == null) {
                return;
            }

            overlayVisible = !root.has("overlayVisible") || root.get("overlayVisible").getAsBoolean();
            if (root.has("workingPath")) {
                CameraPath loadedWorking = readPath(root.getAsJsonObject("workingPath"));
                if (loadedWorking != null) {
                    workingPath = loadedWorking;
                    selectedIndex = root.has("selectedIndex")
                            ? Math.clamp(root.get("selectedIndex").getAsInt(), -1, workingPath.points.size() - 1)
                            : (workingPath.points.isEmpty() ? -1 : 0);
                }
            }
            if (root.has("savedPaths") && root.get("savedPaths").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("savedPaths")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    CameraPath path = readPath(element.getAsJsonObject());
                    if (path == null || path.name == null || path.name.isBlank()) {
                        continue;
                    }
                    SAVED_PATHS.put(normalizeName(path.name), path);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static JsonObject writePath(CameraPath path) {
        JsonObject json = new JsonObject();
        if (path.name != null) {
            json.addProperty("name", path.name);
        }
        json.addProperty("dimension", path.dimensionId);
        json.addProperty("interpolation", path.interpolation.name());

        JsonArray points = new JsonArray();
        for (Keyframe point : path.points) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", point.position.x);
            entry.addProperty("y", point.position.y);
            entry.addProperty("z", point.position.z);
            entry.addProperty("yaw", point.yaw);
            entry.addProperty("pitch", point.pitch);
            entry.addProperty("time", point.timeTicks);
            points.add(entry);
        }
        json.add("points", points);
        return json;
    }

    private static @Nullable CameraPath readPath(JsonObject json) {
        try {
            String name = json.has("name") ? json.get("name").getAsString() : null;
            String dimension = json.has("dimension") ? json.get("dimension").getAsString() : "";
            Interpolation interpolation = json.has("interpolation")
                    ? Interpolation.valueOf(json.get("interpolation").getAsString().toUpperCase(Locale.ROOT))
                    : Interpolation.CATMULL_ROM;
            List<Keyframe> points = new ArrayList<>();
            if (json.has("points") && json.get("points").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("points")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject pointJson = element.getAsJsonObject();
                    points.add(new Keyframe(
                            new Vec3(pointJson.get("x").getAsDouble(), pointJson.get("y").getAsDouble(), pointJson.get("z").getAsDouble()),
                            pointJson.get("yaw").getAsFloat(),
                            pointJson.get("pitch").getAsFloat(),
                            pointJson.get("time").getAsInt()
                    ));
                }
            }
            return new CameraPath(name, dimension, interpolation, points);
        } catch (Exception ignored) {
            return null;
        }
    }

    private enum Interpolation {
        LINEAR,
        CATMULL_ROM;

        private static Interpolation fromToken(String token) {
            return switch (token == null ? "" : token.trim().toLowerCase(Locale.ROOT)) {
                case "linear" -> LINEAR;
                case "catmull", "catmull_rom", "catmull-rom", "smooth" -> CATMULL_ROM;
                default -> throw new IllegalStateException("Interpolation must be 'linear' or 'catmull_rom'.");
            };
        }
    }

    private record Keyframe(Vec3 position, float yaw, float pitch, int timeTicks) {
    }

    private record CameraPose(Vec3 position, float yaw, float pitch) {
    }

    private static final class CameraPath {
        private final @Nullable String name;
        private final String dimensionId;
        private final Interpolation interpolation;
        private final List<Keyframe> points;

        private CameraPath(@Nullable String name, String dimensionId, Interpolation interpolation, List<Keyframe> points) {
            this.name = name;
            this.dimensionId = dimensionId == null ? "" : dimensionId;
            this.interpolation = interpolation == null ? Interpolation.CATMULL_ROM : interpolation;
            this.points = points == null ? new ArrayList<>() : points;
        }

        private CameraPath copy() {
            return new CameraPath(this.name, this.dimensionId, this.interpolation, new ArrayList<>(this.points));
        }
    }

    private static final class PlaybackController implements CameraEntity.CameraController {
        private final CameraPath path;
        private int loopsRemaining;
        private int elapsedTicks;
        private boolean active = true;

        private PlaybackController(CameraPath path, int loops) {
            this.path = path;
            this.loopsRemaining = Math.max(1, loops);
        }

        @Override
        public void tick(CameraEntity camera) {
            if (!this.active) {
                return;
            }

            int totalTicks = totalDurationTicks(this.path);
            CameraPose pose = samplePose(this.path, this.elapsedTicks);
            camera.setScriptedPose(pose.position, pose.yaw, pose.pitch);

            if (this.elapsedTicks >= totalTicks) {
                if (this.loopsRemaining > 1) {
                    this.loopsRemaining--;
                    this.elapsedTicks = 0;
                    return;
                }
                this.active = false;
                playback = null;
                return;
            }

            this.elapsedTicks++;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        @Override
        public boolean blocksManualControl() {
            return true;
        }

        private double renderTime(float tickProgress) {
            return Math.max(0.0, this.elapsedTicks - 2.0 + Math.max(0.0f, tickProgress));
        }

        private void stop() {
            this.active = false;
        }
    }

    public static Interpolation parseInterpolation(String token) {
        return Interpolation.fromToken(token);
    }
}
