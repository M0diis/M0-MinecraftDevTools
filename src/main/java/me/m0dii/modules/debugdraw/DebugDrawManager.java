package me.m0dii.modules.debugdraw;

import com.google.gson.*;
import me.m0dii.M0DevToolsClient;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class DebugDrawManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = M0DevToolsClient.SETTINGS_FOLDER.toPath().resolve("debugdraw-shapes.json");
    private static final Path SELECTION_SAVE_PATH = M0DevToolsClient.SETTINGS_FOLDER.toPath().resolve("debugdraw-selection.json");
    private static final String DEFAULT_WAND_ITEM_ID = "minecraft:wooden_axe";
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final List<DrawShape> SHAPES = new ArrayList<>();

    public enum SelectionShape {
        BOX,
        CIRCLE,
        CYLINDER,
        SPHERE
    }

    private static boolean selectionEnabled = false;
    private static boolean selectionUseAnyClick = false;
    private static BlockPos selectionPos1;
    private static BlockPos selectionPos2;
    private static SelectionShape selectionShape = SelectionShape.BOX;
    private static String selectionWandItemId = DEFAULT_WAND_ITEM_ID;
    private static Item selectionWandItem;

    public record ShapeDescriptor(int id,
                                  String type,
                                  int rgb,
                                  double secondsRemaining,
                                  int segments,
                                  double x1,
                                  double y1,
                                  double z1,
                                  double x2,
                                  double y2,
                                  double z2,
                                  double radius,
                                  double height) {
    }

    private DebugDrawManager() {
    }

    public static void registerRenderer() {
        loadFromDisk();
        loadSelection();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            long now = System.currentTimeMillis();
            List<DrawShape> active = snapshotActive(now);

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) {
                consumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            VertexConsumer visible = consumers.getBuffer(RenderLayers.LINES);
            VertexConsumer through = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);

            double cameraX = context.gameRenderer().getCamera().getCameraPos().x;
            double cameraY = context.gameRenderer().getCamera().getCameraPos().y;
            double cameraZ = context.gameRenderer().getCamera().getCameraPos().z;

            if (!active.isEmpty()) {
                for (DrawShape shape : active) {
                    if (shape instanceof LineShape line) {
                        drawLine(visible, line, cameraX, cameraY, cameraZ, 1.0f);
                        drawLine(through, line, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof BoxShape box) {
                        drawBox(visible, box, cameraX, cameraY, cameraZ, 1.0f);
                        drawBox(through, box, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof CircleShape circle) {
                        drawCircle(visible, circle, cameraX, cameraY, cameraZ, 1.0f);
                        drawCircle(through, circle, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof CylinderShape cylinder) {
                        drawCylinder(visible, cylinder, cameraX, cameraY, cameraZ, 1.0f);
                        drawCylinder(through, cylinder, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof SphereShape sphere) {
                        drawSphere(visible, sphere, cameraX, cameraY, cameraZ, 1.0f);
                        drawSphere(through, sphere, cameraX, cameraY, cameraZ, 0.45f);
                    }
                }
            }

            renderSelectionPreview(visible, through, cameraX, cameraY, cameraZ);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayers.LINES);
                immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
            }
        });
    }

    public static synchronized int clear() {
        int removed = SHAPES.size();
        SHAPES.clear();
        return removed;
    }

    public static synchronized void setSelectionEnabled(boolean enabled) {
        selectionEnabled = enabled;
    }

    public static synchronized boolean isSelectionEnabled() {
        return selectionEnabled;
    }

    public static synchronized void setSelectionUseAnyClick(boolean useAnyClick) {
        selectionUseAnyClick = useAnyClick;
    }

    public static synchronized boolean isSelectionUseAnyClick() {
        return selectionUseAnyClick;
    }

    public static synchronized String getSelectionWandItemId() {
        return selectionWandItemId;
    }

    public static synchronized boolean setSelectionWandItem(String token) {
        String value = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || "default".equals(value)) {
            value = DEFAULT_WAND_ITEM_ID;
        }
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        Identifier id = Identifier.tryParse(value);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return false;
        }
        selectionWandItemId = id.toString();
        selectionWandItem = Registries.ITEM.get(id);
        return true;
    }

    public static synchronized SelectionShape getSelectionShape() {
        return selectionShape;
    }

    public static synchronized void setSelectionShape(SelectionShape shape) {
        selectionShape = shape == null ? SelectionShape.BOX : shape;
    }

    public static synchronized void clearSelectionPositions() {
        selectionPos1 = null;
        selectionPos2 = null;
    }

    public static synchronized BlockPos getSelectionPos1() {
        return selectionPos1;
    }

    public static synchronized BlockPos getSelectionPos2() {
        return selectionPos2;
    }

    public static synchronized void setSelectionPos(boolean rightClick, BlockPos pos) {
        if (rightClick) {
            selectionPos2 = pos;
        } else {
            selectionPos1 = pos;
        }
    }

    public static synchronized boolean shouldCaptureSelectionClick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!selectionEnabled || client == null || client.player == null || client.world == null || client.currentScreen != null) {
            return false;
        }
        if (selectionUseAnyClick) {
            return true;
        }
        return isWandHeld(client.player.getMainHandStack()) || isWandHeld(client.player.getOffHandStack());
    }

    public static synchronized boolean isSelectionWandEquipped() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return isWandHeld(client.player.getMainHandStack()) || isWandHeld(client.player.getOffHandStack());
    }

    public static synchronized boolean pickSelectionPos(boolean rightClick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return false;
        }
        BlockHitResult hit = null;
        if (client.crosshairTarget instanceof BlockHitResult blockHit) {
            hit = blockHit;
        } else {
            HitResult trace = client.player.raycast(20.0, 1.0f, false);
            if (trace instanceof BlockHitResult blockHit) {
                hit = blockHit;
            }
        }
        if (hit == null) {
            return false;
        }
        setSelectionPos(rightClick, hit.getBlockPos());
        return true;
    }

    public static synchronized int addSelectionShape(int rgb, double seconds) {
        if (selectionPos1 == null || selectionPos2 == null) {
            return -1;
        }
        return switch (selectionShape) {
            case CIRCLE -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, false);
                yield addCircle(selectionPos1.getX() + 0.5, selectionPos1.getY() + 0.05, selectionPos1.getZ() + 0.5,
                        radius, rgb, seconds, 36);
            }
            case CYLINDER -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, false);
                double baseY = Math.min(selectionPos1.getY(), selectionPos2.getY());
                double height = Math.max(1.0, Math.abs(selectionPos1.getY() - selectionPos2.getY()) + 1.0);
                yield addCylinder(selectionPos1.getX() + 0.5, baseY, selectionPos1.getZ() + 0.5,
                        radius, height, rgb, seconds, 36);
            }
            case SPHERE -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, true);
                yield addSphere(selectionPos1.getX() + 0.5, selectionPos1.getY() + 0.5, selectionPos1.getZ() + 0.5,
                        radius, rgb, seconds, 32);
            }
            case BOX -> addBox(
                    selectionPos1.getX(), selectionPos1.getY(), selectionPos1.getZ(),
                    selectionPos2.getX() + 1.0, selectionPos2.getY() + 1.0, selectionPos2.getZ() + 1.0,
                    rgb,
                    seconds
            );
        };
    }

    public static synchronized String selectionStatus() {
        String left = selectionPos1 == null ? "-" : selectionPos1.getX() + " " + selectionPos1.getY() + " " + selectionPos1.getZ();
        String right = selectionPos2 == null ? "-" : selectionPos2.getX() + " " + selectionPos2.getY() + " " + selectionPos2.getZ();
        String mode = selectionUseAnyClick ? "any-click" : "wand";
        return "enabled=" + selectionEnabled
                + " mode=" + mode
                + " wand=" + selectionWandItemId
                + " shape=" + selectionShape.name().toLowerCase(Locale.ROOT)
                + " pos1=" + left
                + " pos2=" + right;
    }

    public static synchronized boolean saveSelection() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", selectionEnabled);
        json.addProperty("useAnyClick", selectionUseAnyClick);
        json.addProperty("wandItem", selectionWandItemId);
        json.addProperty("shape", selectionShape.name());
        if (selectionPos1 != null) {
            json.addProperty("x1", selectionPos1.getX());
            json.addProperty("y1", selectionPos1.getY());
            json.addProperty("z1", selectionPos1.getZ());
        }
        if (selectionPos2 != null) {
            json.addProperty("x2", selectionPos2.getX());
            json.addProperty("y2", selectionPos2.getY());
            json.addProperty("z2", selectionPos2.getZ());
        }
        try {
            Files.createDirectories(SELECTION_SAVE_PATH.getParent());
            Files.writeString(SELECTION_SAVE_PATH, GSON.toJson(json));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static synchronized boolean loadSelection() {
        try {
            if (!Files.exists(SELECTION_SAVE_PATH)) {
                return false;
            }
            JsonObject json = GSON.fromJson(Files.readString(SELECTION_SAVE_PATH), JsonObject.class);
            if (json == null) {
                return false;
            }
            selectionEnabled = json.has("enabled") && json.get("enabled").getAsBoolean();
            selectionUseAnyClick = json.has("useAnyClick") && json.get("useAnyClick").getAsBoolean();
            if (json.has("wandItem")) {
                if (!setSelectionWandItem(json.get("wandItem").getAsString())) {
                    setSelectionWandItem(DEFAULT_WAND_ITEM_ID);
                }
            } else {
                setSelectionWandItem(DEFAULT_WAND_ITEM_ID);
            }
            if (json.has("shape")) {
                try {
                    selectionShape = SelectionShape.valueOf(json.get("shape").getAsString().toUpperCase());
                } catch (Exception ignored) {
                    selectionShape = SelectionShape.BOX;
                }
            }
            selectionPos1 = readPos(json, "x1", "y1", "z1");
            selectionPos2 = readPos(json, "x2", "y2", "z2");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static synchronized boolean remove(int id) {
        return SHAPES.removeIf(shape -> shape.id == id);
    }

    public static synchronized int addLine(double x1,
                                           double y1,
                                           double z1,
                                           double x2,
                                           double y2,
                                           double z2,
                                           int rgb,
                                           double seconds) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new LineShape(id, expiryFromSeconds(seconds), rgb,
                x1, y1, z1,
                x2, y2, z2));
        return id;
    }

    public static synchronized int addBox(double x1,
                                          double y1,
                                          double z1,
                                          double x2,
                                          double y2,
                                          double z2,
                                          int rgb,
                                          double seconds) {
        int id = NEXT_ID.getAndIncrement();
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        double maxZ = Math.max(z1, z2);
        SHAPES.add(new BoxShape(id, expiryFromSeconds(seconds), rgb,
                minX, minY, minZ,
                maxX, maxY, maxZ));
        return id;
    }

    public static synchronized int addCircle(double centerX,
                                             double centerY,
                                             double centerZ,
                                             double radius,
                                             int rgb,
                                             double seconds,
                                             int segments) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new CircleShape(id, expiryFromSeconds(seconds), rgb,
                centerX, centerY, centerZ,
                Math.max(0.05, radius),
                clampSegments(segments)));
        return id;
    }

    public static synchronized int addCylinder(double centerX,
                                               double baseY,
                                               double centerZ,
                                               double radius,
                                               double height,
                                               int rgb,
                                               double seconds,
                                               int segments) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new CylinderShape(id, expiryFromSeconds(seconds), rgb,
                centerX, baseY, centerZ,
                Math.max(0.05, radius),
                Math.max(0.05, height),
                clampSegments(segments)));
        return id;
    }

    public static synchronized int addSphere(double centerX,
                                             double centerY,
                                             double centerZ,
                                             double radius,
                                             int rgb,
                                             double seconds,
                                             int segments) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new SphereShape(id, expiryFromSeconds(seconds), rgb,
                centerX, centerY, centerZ,
                Math.max(0.05, radius),
                clampSegments(segments)));
        return id;
    }

    public static synchronized List<String> describeActive() {
        long now = System.currentTimeMillis();
        pruneExpired(now);
        List<String> out = new ArrayList<>();
        for (DrawShape shape : SHAPES) {
            double remaining = Math.max(0.0, (shape.expiresAtMillis - now) / 1000.0);
            switch (shape) {
                case LineShape line -> out.add(String.format("#%d line (%.2f %.2f %.2f -> %.2f %.2f %.2f) %s %.1fs",
                        line.id,
                        line.x1, line.y1, line.z1,
                        line.x2, line.y2, line.z2,
                        formatColor(line.rgb),
                        remaining));
                case BoxShape box -> out.add(String.format("#%d box (%.2f %.2f %.2f -> %.2f %.2f %.2f) %s %.1fs",
                        box.id,
                        box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ,
                        formatColor(box.rgb),
                        remaining));
                case CircleShape circle -> out.add(String.format("#%d circle (%.2f %.2f %.2f r=%.2f s=%d) %s %.1fs",
                        circle.id, circle.centerX, circle.centerY, circle.centerZ,
                        circle.radius, circle.segments, formatColor(circle.rgb), remaining));
                case CylinderShape cylinder ->
                        out.add(String.format("#%d cylinder (%.2f %.2f %.2f r=%.2f h=%.2f s=%d) %s %.1fs",
                                cylinder.id, cylinder.centerX, cylinder.baseY, cylinder.centerZ,
                                cylinder.radius, cylinder.height, cylinder.segments, formatColor(cylinder.rgb), remaining));
                case SphereShape sphere -> out.add(String.format("#%d sphere (%.2f %.2f %.2f r=%.2f s=%d) %s %.1fs",
                        sphere.id, sphere.centerX, sphere.centerY, sphere.centerZ,
                        sphere.radius, sphere.segments, formatColor(sphere.rgb), remaining));
                default -> {
                }
            }
        }
        return out;
    }

    public static synchronized List<ShapeDescriptor> getActiveShapeDescriptors() {
        long now = System.currentTimeMillis();
        pruneExpired(now);
        List<ShapeDescriptor> out = new ArrayList<>();
        for (DrawShape shape : SHAPES) {
            out.add(toDescriptor(shape, now));
        }
        return out;
    }

    public static synchronized ShapeDescriptor getShapeDescriptor(int id) {
        long now = System.currentTimeMillis();
        pruneExpired(now);
        for (DrawShape shape : SHAPES) {
            if (shape.id == id) {
                return toDescriptor(shape, now);
            }
        }
        return null;
    }

    public static synchronized boolean updateShape(ShapeDescriptor descriptor) {
        if (descriptor == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < SHAPES.size(); i++) {
            if (SHAPES.get(i).id == descriptor.id) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }

        DrawShape updated = fromDescriptor(descriptor);
        if (updated == null) {
            return false;
        }
        SHAPES.set(index, updated);
        NEXT_ID.set(Math.max(NEXT_ID.get(), descriptor.id + 1));
        return true;
    }

    public static synchronized int saveToDisk() {
        pruneExpired(System.currentTimeMillis());
        JsonArray array = new JsonArray();
        long now = System.currentTimeMillis();
        for (DrawShape shape : SHAPES) {
            JsonObject json = shape.toJson(now);
            if (json != null) {
                array.add(json);
            }
        }
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            Files.writeString(SAVE_PATH, GSON.toJson(array));
            return array.size();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static synchronized int loadFromDisk() {
        try {
            if (!Files.exists(SAVE_PATH)) {
                SHAPES.clear();
                return 0;
            }
            String raw = Files.readString(SAVE_PATH);
            JsonArray array = GSON.fromJson(raw, JsonArray.class);
            if (array == null) {
                return 0;
            }
            SHAPES.clear();
            int maxId = 0;
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                DrawShape shape = DrawShape.fromJson(element.getAsJsonObject());
                if (shape != null) {
                    SHAPES.add(shape);
                    maxId = Math.max(maxId, shape.id);
                }
            }
            NEXT_ID.set(Math.max(1, maxId + 1));
            return SHAPES.size();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static long expiryFromSeconds(double seconds) {
        double clamped = Math.clamp(seconds, 0.5, 3600.0);
        return System.currentTimeMillis() + (long) (clamped * 1000.0);
    }

    private static ShapeDescriptor toDescriptor(DrawShape shape, long now) {
        double seconds = Math.max(0.2, (shape.expiresAtMillis - now) / 1000.0);
        return switch (shape) {
            case LineShape line -> new ShapeDescriptor(line.id, "line", line.rgb, seconds, 0,
                    line.x1, line.y1, line.z1,
                    line.x2, line.y2, line.z2,
                    0.0, 0.0);
            case BoxShape box -> new ShapeDescriptor(box.id, "box", box.rgb, seconds, 0,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    0.0, 0.0);
            case CircleShape circle -> new ShapeDescriptor(circle.id, "circle", circle.rgb, seconds, circle.segments,
                    circle.centerX, circle.centerY, circle.centerZ,
                    0.0, 0.0, 0.0,
                    circle.radius, 0.0);
            case CylinderShape cylinder ->
                    new ShapeDescriptor(cylinder.id, "cylinder", cylinder.rgb, seconds, cylinder.segments,
                            cylinder.centerX, cylinder.baseY, cylinder.centerZ,
                            0.0, 0.0, 0.0,
                            cylinder.radius, cylinder.height);
            case SphereShape sphere -> new ShapeDescriptor(sphere.id, "sphere", sphere.rgb, seconds, sphere.segments,
                    sphere.centerX, sphere.centerY, sphere.centerZ,
                    0.0, 0.0, 0.0,
                    sphere.radius, 0.0);
            default -> new ShapeDescriptor(shape.id, "unknown", shape.rgb, seconds, 0,
                    0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0,
                    0.0, 0.0);
        };
    }

    private static DrawShape fromDescriptor(ShapeDescriptor d) {
        long expiresAt = expiryFromSeconds(d.secondsRemaining);
        int rgb = d.rgb & 0xFFFFFF;
        int segments = clampSegments(d.segments <= 0 ? 36 : d.segments);
        return switch (d.type == null ? "" : d.type.toLowerCase()) {
            case "line" -> new LineShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, d.x2, d.y2, d.z2);
            case "box" -> new BoxShape(d.id, expiresAt, rgb,
                    Math.min(d.x1, d.x2), Math.min(d.y1, d.y2), Math.min(d.z1, d.z2),
                    Math.max(d.x1, d.x2), Math.max(d.y1, d.y2), Math.max(d.z1, d.z2));
            case "circle" ->
                    new CircleShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius), segments);
            case "cylinder" ->
                    new CylinderShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius), Math.max(0.05, d.height), segments);
            case "sphere" ->
                    new SphereShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius), segments);
            default -> null;
        };
    }

    private static synchronized List<DrawShape> snapshotActive(long now) {
        pruneExpired(now);
        return new ArrayList<>(SHAPES);
    }

    private static void pruneExpired(long now) {
        SHAPES.removeIf(shape -> shape.expiresAtMillis <= now);
    }

    private static void drawLine(VertexConsumer buffer,
                                 LineShape line,
                                 double cameraX,
                                 double cameraY,
                                 double cameraZ,
                                 float alphaMul) {
        float[] color = toColor(line.rgb, alphaMul);
        DrawUtil.drawLineSafe(buffer,
                line.x1 - cameraX, line.y1 - cameraY, line.z1 - cameraZ,
                line.x2 - cameraX, line.y2 - cameraY, line.z2 - cameraZ,
                color[0], color[1], color[2], color[3], 1.5f);
    }

    private static void drawBox(VertexConsumer buffer,
                                BoxShape box,
                                double cameraX,
                                double cameraY,
                                double cameraZ,
                                float alphaMul) {
        float[] color = toColor(box.rgb, alphaMul);
        DrawUtil.drawOutlinedBoxSafe(buffer,
                box.minX - cameraX, box.minY - cameraY, box.minZ - cameraZ,
                box.maxX - cameraX, box.maxY - cameraY, box.maxZ - cameraZ,
                color[0], color[1], color[2], color[3], 1.5f);
    }

    private static void renderSelectionPreview(VertexConsumer visible,
                                               VertexConsumer through,
                                               double cameraX,
                                               double cameraY,
                                               double cameraZ) {
        if (!selectionEnabled || selectionPos1 == null) {
            return;
        }
        if (selectionPos2 == null) {
            double x = selectionPos1.getX();
            double y = selectionPos1.getY();
            double z = selectionPos1.getZ();
            DrawUtil.drawOutlinedBoxSafe(visible,
                    x - cameraX, y - cameraY, z - cameraZ,
                    x + 1.0 - cameraX, y + 1.0 - cameraY, z + 1.0 - cameraZ,
                    0.2f, 1.0f, 0.2f, 0.95f, 2.0f);
            DrawUtil.drawOutlinedBoxSafe(through,
                    x - cameraX, y - cameraY, z - cameraZ,
                    x + 1.0 - cameraX, y + 1.0 - cameraY, z + 1.0 - cameraZ,
                    0.2f, 1.0f, 0.2f, 0.35f, 2.0f);
            return;
        }

        switch (selectionShape) {
            case CIRCLE -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, false);
                CircleShape circle = new CircleShape(-1, Long.MAX_VALUE, 0x33FFAA,
                        selectionPos1.getX() + 0.5, selectionPos1.getY() + 0.05, selectionPos1.getZ() + 0.5,
                        radius, 36);
                drawCircle(visible, circle, cameraX, cameraY, cameraZ, 0.95f);
                drawCircle(through, circle, cameraX, cameraY, cameraZ, 0.35f);
            }
            case CYLINDER -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, false);
                double baseY = Math.min(selectionPos1.getY(), selectionPos2.getY());
                double height = Math.max(1.0, Math.abs(selectionPos1.getY() - selectionPos2.getY()) + 1.0);
                CylinderShape cylinder = new CylinderShape(-1, Long.MAX_VALUE, 0x33FFAA,
                        selectionPos1.getX() + 0.5, baseY, selectionPos1.getZ() + 0.5,
                        radius, height, 36);
                drawCylinder(visible, cylinder, cameraX, cameraY, cameraZ, 0.95f);
                drawCylinder(through, cylinder, cameraX, cameraY, cameraZ, 0.35f);
            }
            case SPHERE -> {
                double radius = selectionRadius(selectionPos1, selectionPos2, true);
                SphereShape sphere = new SphereShape(-1, Long.MAX_VALUE, 0x33FFAA,
                        selectionPos1.getX() + 0.5, selectionPos1.getY() + 0.5, selectionPos1.getZ() + 0.5,
                        radius, 32);
                drawSphere(visible, sphere, cameraX, cameraY, cameraZ, 0.95f);
                drawSphere(through, sphere, cameraX, cameraY, cameraZ, 0.35f);
            }
            case BOX -> {
                double minX = Math.min(selectionPos1.getX(), selectionPos2.getX());
                double minY = Math.min(selectionPos1.getY(), selectionPos2.getY());
                double minZ = Math.min(selectionPos1.getZ(), selectionPos2.getZ());
                double maxX = Math.max(selectionPos1.getX(), selectionPos2.getX()) + 1.0;
                double maxY = Math.max(selectionPos1.getY(), selectionPos2.getY()) + 1.0;
                double maxZ = Math.max(selectionPos1.getZ(), selectionPos2.getZ()) + 1.0;
                DrawUtil.drawOutlinedBoxSafe(visible,
                        minX - cameraX, minY - cameraY, minZ - cameraZ,
                        maxX - cameraX, maxY - cameraY, maxZ - cameraZ,
                        0.2f, 1.0f, 0.2f, 0.95f, 2.0f);
                DrawUtil.drawOutlinedBoxSafe(through,
                        minX - cameraX, minY - cameraY, minZ - cameraZ,
                        maxX - cameraX, maxY - cameraY, maxZ - cameraZ,
                        0.2f, 1.0f, 0.2f, 0.35f, 2.0f);
            }
        }
    }

    private static double selectionRadius(BlockPos center, BlockPos edge, boolean includeY) {
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        double ex = edge.getX() + 0.5;
        double ey = edge.getY() + 0.5;
        double ez = edge.getZ() + 0.5;
        double dx = ex - cx;
        double dz = ez - cz;
        double dy = includeY ? (ey - cy) : 0.0;
        return Math.max(0.5, Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static BlockPos readPos(JsonObject json, String x, String y, String z) {
        if (!json.has(x) || !json.has(y) || !json.has(z)) {
            return null;
        }
        return new BlockPos(json.get(x).getAsInt(), json.get(y).getAsInt(), json.get(z).getAsInt());
    }

    private static boolean isWandHeld(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item wand = selectionWandItem;
        if (wand == null) {
            Identifier id = Identifier.tryParse(selectionWandItemId);
            if (id == null || !Registries.ITEM.containsId(id)) {
                id = Identifier.tryParse(DEFAULT_WAND_ITEM_ID);
            }
            if (id == null || !Registries.ITEM.containsId(id)) {
                return false;
            }
            wand = Registries.ITEM.get(id);
            selectionWandItem = wand;
        }
        return stack.isOf(wand);
    }

    private static float[] toColor(int rgb, float alphaMul) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        float a = Math.clamp(alphaMul, 0.05f, 1.0f);
        return new float[]{r, g, b, a};
    }

    public static String formatColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static int clampSegments(int segments) {
        return Math.clamp(segments, 8, 128);
    }

    private static void drawCircle(VertexConsumer buffer,
                                   CircleShape circle,
                                   double cameraX,
                                   double cameraY,
                                   double cameraZ,
                                   float alphaMul) {
        float[] color = toColor(circle.rgb, alphaMul);
        for (int i = 0; i < circle.segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / circle.segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / circle.segments;
            double x0 = circle.centerX + Math.cos(a0) * circle.radius;
            double z0 = circle.centerZ + Math.sin(a0) * circle.radius;
            double x1 = circle.centerX + Math.cos(a1) * circle.radius;
            double z1 = circle.centerZ + Math.sin(a1) * circle.radius;
            DrawUtil.drawLineSafe(buffer,
                    x0 - cameraX, circle.centerY - cameraY, z0 - cameraZ,
                    x1 - cameraX, circle.centerY - cameraY, z1 - cameraZ,
                    color[0], color[1], color[2], color[3], 1.25f);
        }
    }

    private static void drawCylinder(VertexConsumer buffer,
                                     CylinderShape cylinder,
                                     double cameraX,
                                     double cameraY,
                                     double cameraZ,
                                     float alphaMul) {
        float[] color = toColor(cylinder.rgb, alphaMul);
        double topY = cylinder.baseY + cylinder.height;
        int verticalStep = Math.max(1, cylinder.segments / 8);
        for (int i = 0; i < cylinder.segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / cylinder.segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / cylinder.segments;
            double x0 = cylinder.centerX + Math.cos(a0) * cylinder.radius;
            double z0 = cylinder.centerZ + Math.sin(a0) * cylinder.radius;
            double x1 = cylinder.centerX + Math.cos(a1) * cylinder.radius;
            double z1 = cylinder.centerZ + Math.sin(a1) * cylinder.radius;

            DrawUtil.drawLineSafe(buffer,
                    x0 - cameraX, cylinder.baseY - cameraY, z0 - cameraZ,
                    x1 - cameraX, cylinder.baseY - cameraY, z1 - cameraZ,
                    color[0], color[1], color[2], color[3], 1.25f);
            DrawUtil.drawLineSafe(buffer,
                    x0 - cameraX, topY - cameraY, z0 - cameraZ,
                    x1 - cameraX, topY - cameraY, z1 - cameraZ,
                    color[0], color[1], color[2], color[3], 1.25f);
            if (i % verticalStep == 0) {
                DrawUtil.drawLineSafe(buffer,
                        x0 - cameraX, cylinder.baseY - cameraY, z0 - cameraZ,
                        x0 - cameraX, topY - cameraY, z0 - cameraZ,
                        color[0], color[1], color[2], color[3], 1.25f);
            }
        }
    }

    private static void drawSphere(VertexConsumer buffer,
                                   SphereShape sphere,
                                   double cameraX,
                                   double cameraY,
                                   double cameraZ,
                                   float alphaMul) {
        float[] color = toColor(sphere.rgb, alphaMul);
        for (int i = 0; i < sphere.segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / sphere.segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / sphere.segments;

            double x1 = sphere.centerX + Math.cos(a0) * sphere.radius - cameraX;
            double x2 = sphere.centerX + Math.cos(a1) * sphere.radius - cameraX;

            double z1 = sphere.centerZ + Math.sin(a0) * sphere.radius - cameraZ;
            double z2 = sphere.centerZ + Math.sin(a1) * sphere.radius - cameraZ;

            DrawUtil.drawLineSafe(buffer,
                    x1, sphere.centerY - cameraY, z1,
                    x2, sphere.centerY - cameraY, z2,
                    color[0], color[1], color[2], color[3], 1.25f);
            DrawUtil.drawLineSafe(buffer,
                    sphere.centerX - cameraX, sphere.centerY + Math.cos(a0) * sphere.radius - cameraY, z1,
                    sphere.centerX - cameraX, sphere.centerY + Math.cos(a1) * sphere.radius - cameraY, z2,
                    color[0], color[1], color[2], color[3], 1.25f);
            DrawUtil.drawLineSafe(buffer,
                    x1, sphere.centerY + Math.sin(a0) * sphere.radius - cameraY, sphere.centerZ - cameraZ,
                    x2, sphere.centerY + Math.sin(a1) * sphere.radius - cameraY, sphere.centerZ - cameraZ,
                    color[0], color[1], color[2], color[3], 1.25f);
        }
    }

    private abstract static sealed class DrawShape {
        protected final int id;
        protected final long expiresAtMillis;
        protected final int rgb;

        private DrawShape(int id, long expiresAtMillis, int rgb) {
            this.id = id;
            this.expiresAtMillis = expiresAtMillis;
            this.rgb = rgb & 0xFFFFFF;
        }

        protected JsonObject baseJson(long now, String type) {
            JsonObject json = new JsonObject();
            json.addProperty("id", this.id);
            json.addProperty("type", type);
            json.addProperty("rgb", this.rgb);
            json.addProperty("seconds", Math.max(0.1, (this.expiresAtMillis - now) / 1000.0));
            return json;
        }

        protected abstract JsonObject toJson(long now);

        private static DrawShape fromJson(JsonObject json) {
            try {
                int id = json.get("id").getAsInt();
                String type = json.get("type").getAsString();
                int rgb = json.get("rgb").getAsInt();
                double seconds = json.get("seconds").getAsDouble();
                long expires = expiryFromSeconds(seconds);

                return switch (type) {
                    case "line" -> new LineShape(id, expires, rgb,
                            json.get("x1").getAsDouble(), json.get("y1").getAsDouble(), json.get("z1").getAsDouble(),
                            json.get("x2").getAsDouble(), json.get("y2").getAsDouble(), json.get("z2").getAsDouble());
                    case "box" -> new BoxShape(id, expires, rgb,
                            json.get("minX").getAsDouble(), json.get("minY").getAsDouble(), json.get("minZ").getAsDouble(),
                            json.get("maxX").getAsDouble(), json.get("maxY").getAsDouble(), json.get("maxZ").getAsDouble());
                    case "circle" -> new CircleShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("centerY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble(), clampSegments(json.get("segments").getAsInt()));
                    case "cylinder" -> new CylinderShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("baseY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble(), json.get("height").getAsDouble(), clampSegments(json.get("segments").getAsInt()));
                    case "sphere" -> new SphereShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("centerY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble(), clampSegments(json.get("segments").getAsInt()));
                    default -> null;
                };
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static final class LineShape extends DrawShape {
        private final double x1;
        private final double y1;
        private final double z1;
        private final double x2;
        private final double y2;
        private final double z2;

        private LineShape(int id,
                          long expiresAtMillis,
                          int rgb,
                          double x1,
                          double y1,
                          double z1,
                          double x2,
                          double y2,
                          double z2) {
            super(id, expiresAtMillis, rgb);
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "line");
            json.addProperty("x1", this.x1);
            json.addProperty("y1", this.y1);
            json.addProperty("z1", this.z1);
            json.addProperty("x2", this.x2);
            json.addProperty("y2", this.y2);
            json.addProperty("z2", this.z2);
            return json;
        }
    }

    private static final class BoxShape extends DrawShape {
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;

        private BoxShape(int id,
                         long expiresAtMillis,
                         int rgb,
                         double minX,
                         double minY,
                         double minZ,
                         double maxX,
                         double maxY,
                         double maxZ) {
            super(id, expiresAtMillis, rgb);
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "box");
            json.addProperty("minX", this.minX);
            json.addProperty("minY", this.minY);
            json.addProperty("minZ", this.minZ);
            json.addProperty("maxX", this.maxX);
            json.addProperty("maxY", this.maxY);
            json.addProperty("maxZ", this.maxZ);
            return json;
        }
    }

    private static final class CircleShape extends DrawShape {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;
        private final int segments;

        private CircleShape(int id, long expiresAtMillis, int rgb,
                            double centerX, double centerY, double centerZ,
                            double radius, int segments) {
            super(id, expiresAtMillis, rgb);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.segments = segments;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "circle");
            json.addProperty("centerX", this.centerX);
            json.addProperty("centerY", this.centerY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            json.addProperty("segments", this.segments);
            return json;
        }
    }

    private static final class CylinderShape extends DrawShape {
        private final double centerX;
        private final double baseY;
        private final double centerZ;
        private final double radius;
        private final double height;
        private final int segments;

        private CylinderShape(int id, long expiresAtMillis, int rgb,
                              double centerX, double baseY, double centerZ,
                              double radius, double height, int segments) {
            super(id, expiresAtMillis, rgb);
            this.centerX = centerX;
            this.baseY = baseY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.height = height;
            this.segments = segments;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "cylinder");
            json.addProperty("centerX", this.centerX);
            json.addProperty("baseY", this.baseY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            json.addProperty("height", this.height);
            json.addProperty("segments", this.segments);
            return json;
        }
    }

    private static final class SphereShape extends DrawShape {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;
        private final int segments;

        private SphereShape(int id, long expiresAtMillis, int rgb,
                            double centerX, double centerY, double centerZ,
                            double radius, int segments) {
            super(id, expiresAtMillis, rgb);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.segments = segments;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "sphere");
            json.addProperty("centerX", this.centerX);
            json.addProperty("centerY", this.centerY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            json.addProperty("segments", this.segments);
            return json;
        }
    }
}

