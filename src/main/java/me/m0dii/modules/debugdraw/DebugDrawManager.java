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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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
    private static final int DEFAULT_SELECTION_POS1_COLOR = 0x59FF59;
    private static final int DEFAULT_SELECTION_POS2_COLOR = 0xFF5959;
    private static final int DEFAULT_SELECTION_CONNECTOR_COLOR = 0xFFFFFF;
    private static final int DEFAULT_SELECTION_BOX_COLOR = 0xFFF24D;
    private static final int DEFAULT_SELECTION_GRID_COLOR = 0x73FF73;
    private static final int DEFAULT_SELECTION_SHAPE_COLOR = 0x33FFAA;
    private static final double SELECTION_GRID_FACE_OFFSET = 0.002;
    private static final int MAX_SELECTION_GRID_DIVISIONS = 96;
    private static final float SELECTION_GRID_LINE_WIDTH = 1.4f;
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final List<DrawShape> SHAPES = new ArrayList<>();

    public enum SelectionShape {
        BOX,
        CIRCLE,
        CYLINDER,
        SPHERE
    }

    public enum SelectionColorSlot {
        POS1,
        POS2,
        CONNECTOR,
        BOX,
        GRID,
        SHAPE
    }

    private static boolean selectionEnabled = false;
    private static boolean selectionUseAnyClick = false;
    private static BlockPos selectionPos1;
    private static BlockPos selectionPos2;
    private static SelectionShape selectionShape = SelectionShape.BOX;
    private static String selectionWandItemId = DEFAULT_WAND_ITEM_ID;
    private static Item selectionWandItem;
    private static int selectionPos1Color = DEFAULT_SELECTION_POS1_COLOR;
    private static int selectionPos2Color = DEFAULT_SELECTION_POS2_COLOR;
    private static int selectionConnectorColor = DEFAULT_SELECTION_CONNECTOR_COLOR;
    private static int selectionBoxColor = DEFAULT_SELECTION_BOX_COLOR;
    private static int selectionGridColor = DEFAULT_SELECTION_GRID_COLOR;
    private static int selectionShapeColor = DEFAULT_SELECTION_SHAPE_COLOR;

    public record SelectionBounds(BlockPos min, BlockPos max) {
        public int sizeX() {
            return this.max.getX() - this.min.getX() + 1;
        }

        public int sizeY() {
            return this.max.getY() - this.min.getY() + 1;
        }

        public int sizeZ() {
            return this.max.getZ() - this.min.getZ() + 1;
        }

        public long volume() {
            return (long) sizeX() * sizeY() * sizeZ();
        }

        public double centerX() {
            return (this.min.getX() + this.max.getX() + 1.0) * 0.5;
        }

        public double centerY() {
            return (this.min.getY() + this.max.getY() + 1.0) * 0.5;
        }

        public double centerZ() {
            return (this.min.getZ() + this.max.getZ() + 1.0) * 0.5;
        }
    }

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
                    } else if (shape instanceof DiamondShape diamond) {
                        drawDiamond(visible, diamond, cameraX, cameraY, cameraZ, 1.0f);
                        drawDiamond(through, diamond, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof PyramidShape pyramid) {
                        drawPyramid(visible, pyramid, cameraX, cameraY, cameraZ, 1.0f);
                        drawPyramid(through, pyramid, cameraX, cameraY, cameraZ, 0.45f);
                    } else if (shape instanceof ConeShape cone) {
                        drawCone(visible, cone, cameraX, cameraY, cameraZ, 1.0f);
                        drawCone(through, cone, cameraX, cameraY, cameraZ, 0.45f);
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

    public static synchronized int getSelectionColor(SelectionColorSlot slot) {
        return switch (slot) {
            case POS1 -> selectionPos1Color;
            case POS2 -> selectionPos2Color;
            case CONNECTOR -> selectionConnectorColor;
            case BOX -> selectionBoxColor;
            case GRID -> selectionGridColor;
            case SHAPE -> selectionShapeColor;
        };
    }

    public static synchronized void setSelectionColor(SelectionColorSlot slot, int rgb) {
        int color = rgb & 0xFFFFFF;
        switch (slot) {
            case POS1 -> selectionPos1Color = color;
            case POS2 -> selectionPos2Color = color;
            case CONNECTOR -> selectionConnectorColor = color;
            case BOX -> selectionBoxColor = color;
            case GRID -> selectionGridColor = color;
            case SHAPE -> selectionShapeColor = color;
        }
    }

    public static synchronized void resetSelectionColors() {
        selectionPos1Color = DEFAULT_SELECTION_POS1_COLOR;
        selectionPos2Color = DEFAULT_SELECTION_POS2_COLOR;
        selectionConnectorColor = DEFAULT_SELECTION_CONNECTOR_COLOR;
        selectionBoxColor = DEFAULT_SELECTION_BOX_COLOR;
        selectionGridColor = DEFAULT_SELECTION_GRID_COLOR;
        selectionShapeColor = DEFAULT_SELECTION_SHAPE_COLOR;
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

    public static synchronized SelectionBounds getSelectionBounds() {
        if (selectionPos1 == null || selectionPos2 == null) {
            return null;
        }
        return new SelectionBounds(
                new BlockPos(
                        Math.min(selectionPos1.getX(), selectionPos2.getX()),
                        Math.min(selectionPos1.getY(), selectionPos2.getY()),
                        Math.min(selectionPos1.getZ(), selectionPos2.getZ())
                ),
                new BlockPos(
                        Math.max(selectionPos1.getX(), selectionPos2.getX()),
                        Math.max(selectionPos1.getY(), selectionPos2.getY()),
                        Math.max(selectionPos1.getZ(), selectionPos2.getZ())
                )
        );
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
        SelectionBounds bounds = getSelectionBounds();
        return "enabled=" + selectionEnabled
                + " mode=" + mode
                + " wand=" + selectionWandItemId
                + " shape=" + selectionShape.name().toLowerCase(Locale.ROOT)
                + " pos1=" + left
                + " pos2=" + right
                + (bounds == null ? "" : " size=" + bounds.sizeX() + "x" + bounds.sizeY() + "x" + bounds.sizeZ() + " volume=" + bounds.volume());
    }

    public static synchronized boolean saveSelection() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", selectionEnabled);
        json.addProperty("useAnyClick", selectionUseAnyClick);
        json.addProperty("wandItem", selectionWandItemId);
        json.addProperty("shape", selectionShape.name());
        json.addProperty("pos1Color", selectionPos1Color);
        json.addProperty("pos2Color", selectionPos2Color);
        json.addProperty("connectorColor", selectionConnectorColor);
        json.addProperty("boxColor", selectionBoxColor);
        json.addProperty("gridColor", selectionGridColor);
        json.addProperty("shapeColor", selectionShapeColor);
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
            selectionPos1Color = readColor(json, "pos1Color", DEFAULT_SELECTION_POS1_COLOR);
            selectionPos2Color = readColor(json, "pos2Color", DEFAULT_SELECTION_POS2_COLOR);
            selectionConnectorColor = readColor(json, "connectorColor", DEFAULT_SELECTION_CONNECTOR_COLOR);
            selectionBoxColor = readColor(json, "boxColor", DEFAULT_SELECTION_BOX_COLOR);
            selectionGridColor = readColor(json, "gridColor", DEFAULT_SELECTION_GRID_COLOR);
            selectionShapeColor = readColor(json, "shapeColor", DEFAULT_SELECTION_SHAPE_COLOR);
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

    public static synchronized int addDiamond(double centerX,
                                              double centerY,
                                              double centerZ,
                                              double radius,
                                              int rgb,
                                              double seconds) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new DiamondShape(id, expiryFromSeconds(seconds), rgb,
                centerX, centerY, centerZ,
                Math.max(0.05, radius)));
        return id;
    }

    public static synchronized int addPyramid(double centerX,
                                              double baseY,
                                              double centerZ,
                                              double radius,
                                              double height,
                                              int rgb,
                                              double seconds) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new PyramidShape(id, expiryFromSeconds(seconds), rgb,
                centerX, baseY, centerZ,
                Math.max(0.05, radius),
                Math.max(0.05, height)));
        return id;
    }

    public static synchronized int addCone(double centerX,
                                           double baseY,
                                           double centerZ,
                                           double radius,
                                           double height,
                                           int rgb,
                                           double seconds,
                                           int segments) {
        int id = NEXT_ID.getAndIncrement();
        SHAPES.add(new ConeShape(id, expiryFromSeconds(seconds), rgb,
                centerX, baseY, centerZ,
                Math.max(0.05, radius),
                Math.max(0.05, height),
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
                case DiamondShape diamond -> out.add(String.format("#%d diamond (%.2f %.2f %.2f r=%.2f) %s %.1fs",
                        diamond.id, diamond.centerX, diamond.centerY, diamond.centerZ,
                        diamond.radius, formatColor(diamond.rgb), remaining));
                case PyramidShape pyramid -> out.add(String.format("#%d pyramid (%.2f %.2f %.2f r=%.2f h=%.2f) %s %.1fs",
                        pyramid.id, pyramid.centerX, pyramid.baseY, pyramid.centerZ,
                        pyramid.radius, pyramid.height, formatColor(pyramid.rgb), remaining));
                case ConeShape cone -> out.add(String.format("#%d cone (%.2f %.2f %.2f r=%.2f h=%.2f s=%d) %s %.1fs",
                        cone.id, cone.centerX, cone.baseY, cone.centerZ,
                        cone.radius, cone.height, cone.segments, formatColor(cone.rgb), remaining));
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
            case DiamondShape diamond -> new ShapeDescriptor(diamond.id, "diamond", diamond.rgb, seconds, 0,
                    diamond.centerX, diamond.centerY, diamond.centerZ,
                    0.0, 0.0, 0.0,
                    diamond.radius, 0.0);
            case PyramidShape pyramid -> new ShapeDescriptor(pyramid.id, "pyramid", pyramid.rgb, seconds, 0,
                    pyramid.centerX, pyramid.baseY, pyramid.centerZ,
                    0.0, 0.0, 0.0,
                    pyramid.radius, pyramid.height);
            case ConeShape cone -> new ShapeDescriptor(cone.id, "cone", cone.rgb, seconds, cone.segments,
                    cone.centerX, cone.baseY, cone.centerZ,
                    0.0, 0.0, 0.0,
                    cone.radius, cone.height);
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
            case "diamond" ->
                    new DiamondShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius));
            case "pyramid" ->
                    new PyramidShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius), Math.max(0.05, d.height));
            case "cone" ->
                    new ConeShape(d.id, expiresAt, rgb, d.x1, d.y1, d.z1, Math.max(0.05, d.radius), Math.max(0.05, d.height), segments);
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
        if (selectionPos1 == null) {
            return;
        }

        drawSelectionAnchor(visible, through, selectionPos1, cameraX, cameraY, cameraZ, selectionPos1Color);
        if (selectionPos2 == null) {
            return;
        }

        drawSelectionAnchor(visible, through, selectionPos2, cameraX, cameraY, cameraZ, selectionPos2Color);
        drawSelectionConnector(visible, through, selectionPos1, selectionPos2, cameraX, cameraY, cameraZ, selectionConnectorColor);

        SelectionBounds bounds = getSelectionBounds();
        if (bounds == null) {
            return;
        }

        drawSelectionBox(visible, through, bounds, cameraX, cameraY, cameraZ, selectionBoxColor, 1.0f, 0.40f);

        switch (selectionShape) {
            case CIRCLE -> {
                renderSelectionCirclePreview(visible, through, bounds, cameraX, cameraY, cameraZ);
            }
            case CYLINDER -> {
                double radius = Math.max(0.5, Math.min(bounds.sizeX(), bounds.sizeZ()) * 0.5);
                double baseY = bounds.min.getY();
                double height = bounds.sizeY();
                CylinderShape cylinder = new CylinderShape(-1, Long.MAX_VALUE, selectionShapeColor,
                        bounds.centerX(), baseY, bounds.centerZ(),
                        radius, height, 36);
                drawCylinder(visible, cylinder, cameraX, cameraY, cameraZ, 0.95f);
                drawCylinder(through, cylinder, cameraX, cameraY, cameraZ, 0.35f);
            }
            case SPHERE -> {
                double radius = Math.max(0.5, Math.min(bounds.sizeX(), Math.min(bounds.sizeY(), bounds.sizeZ())) * 0.5);
                SphereShape sphere = new SphereShape(-1, Long.MAX_VALUE, selectionShapeColor,
                        bounds.centerX(), bounds.centerY(), bounds.centerZ(),
                        radius, 32);
                drawSphere(visible, sphere, cameraX, cameraY, cameraZ, 0.95f);
                drawSphere(through, sphere, cameraX, cameraY, cameraZ, 0.35f);
            }
            case BOX -> {
                drawSelectionBox(visible, through, bounds, cameraX, cameraY, cameraZ, selectionShapeColor, 0.98f, 0.45f);
                drawSelectionGrid(visible, through, bounds, cameraX, cameraY, cameraZ, selectionGridColor, 0.72f, 0.34f);
            }
        }
    }

    private static void drawSelectionAnchor(VertexConsumer visible,
                                            VertexConsumer through,
                                            BlockPos pos,
                                            double cameraX,
                                            double cameraY,
                                            double cameraZ,
                                            int rgb) {
        double inset = -0.02;
        double x1 = pos.getX() + inset - cameraX;
        double y1 = pos.getY() + inset - cameraY;
        double z1 = pos.getZ() + inset - cameraZ;
        double x2 = pos.getX() + 1.0 - inset - cameraX;
        double y2 = pos.getY() + 1.0 - inset - cameraY;
        double z2 = pos.getZ() + 1.0 - inset - cameraZ;
        float[] visibleColor = toColor(rgb, 1.0f);
        float[] throughColor = toColor(rgb, 0.45f);
        DrawUtil.drawOutlinedBoxSafe(visible, x1, y1, z1, x2, y2, z2,
                visibleColor[0], visibleColor[1], visibleColor[2], visibleColor[3], 2.5f);
        DrawUtil.drawOutlinedBoxSafe(through, x1, y1, z1, x2, y2, z2,
                throughColor[0], throughColor[1], throughColor[2], throughColor[3], 2.5f);
    }

    private static void drawSelectionConnector(VertexConsumer visible,
                                               VertexConsumer through,
                                               BlockPos pos1,
                                               BlockPos pos2,
                                               double cameraX,
                                               double cameraY,
                                               double cameraZ,
                                               int rgb) {
        double x1 = pos1.getX() + 0.5 - cameraX;
        double y1 = pos1.getY() + 0.5 - cameraY;
        double z1 = pos1.getZ() + 0.5 - cameraZ;
        double x2 = pos2.getX() + 0.5 - cameraX;
        double y2 = pos2.getY() + 0.5 - cameraY;
        double z2 = pos2.getZ() + 0.5 - cameraZ;
        float[] visibleColor = toColor(rgb, 0.9f);
        float[] throughColor = toColor(rgb, 0.35f);
        DrawUtil.drawLineSafe(visible, x1, y1, z1, x2, y2, z2,
                visibleColor[0], visibleColor[1], visibleColor[2], visibleColor[3], 1.75f);
        DrawUtil.drawLineSafe(through, x1, y1, z1, x2, y2, z2,
                throughColor[0], throughColor[1], throughColor[2], throughColor[3], 1.75f);
    }

    private static void drawSelectionBox(VertexConsumer visible,
                                         VertexConsumer through,
                                         SelectionBounds bounds,
                                         double cameraX,
                                         double cameraY,
                                         double cameraZ,
                                         int rgb,
                                         float visibleAlpha,
                                         float throughAlpha) {
        float[] visibleColor = toColor(rgb, visibleAlpha);
        float[] throughColor = toColor(rgb, throughAlpha);
        DrawUtil.drawOutlinedBoxSafe(visible,
                bounds.min.getX() - cameraX, bounds.min.getY() - cameraY, bounds.min.getZ() - cameraZ,
                bounds.max.getX() + 1.0 - cameraX, bounds.max.getY() + 1.0 - cameraY, bounds.max.getZ() + 1.0 - cameraZ,
                visibleColor[0], visibleColor[1], visibleColor[2], visibleColor[3], 2.25f);
        DrawUtil.drawOutlinedBoxSafe(through,
                bounds.min.getX() - cameraX, bounds.min.getY() - cameraY, bounds.min.getZ() - cameraZ,
                bounds.max.getX() + 1.0 - cameraX, bounds.max.getY() + 1.0 - cameraY, bounds.max.getZ() + 1.0 - cameraZ,
                throughColor[0], throughColor[1], throughColor[2], throughColor[3], 2.25f);
    }

    private static void drawSelectionGrid(VertexConsumer visible,
                                          VertexConsumer through,
                                          SelectionBounds bounds,
                                          double cameraX,
                                          double cameraY,
                                          double cameraZ,
                                          int rgb,
                                          float visibleAlpha,
                                          float throughAlpha) {
        drawSelectionGridFaces(visible, bounds, cameraX, cameraY, cameraZ, rgb, visibleAlpha);
        drawSelectionGridFaces(through, bounds, cameraX, cameraY, cameraZ, rgb, throughAlpha);
    }

    private static void drawSelectionGridFaces(VertexConsumer buffer,
                                               SelectionBounds bounds,
                                               double cameraX,
                                               double cameraY,
                                               double cameraZ,
                                               int rgb,
                                               float alpha) {
        int stepX = selectionGridStep(bounds.sizeX());
        int stepY = selectionGridStep(bounds.sizeY());
        int stepZ = selectionGridStep(bounds.sizeZ());

        double minX = bounds.min.getX();
        double minY = bounds.min.getY();
        double minZ = bounds.min.getZ();
        double maxX = bounds.max.getX() + 1.0;
        double maxY = bounds.max.getY() + 1.0;
        double maxZ = bounds.max.getZ() + 1.0;

        double leftX = minX - SELECTION_GRID_FACE_OFFSET;
        double rightX = maxX + SELECTION_GRID_FACE_OFFSET;
        double bottomY = minY - SELECTION_GRID_FACE_OFFSET;
        double topY = maxY + SELECTION_GRID_FACE_OFFSET;
        double frontZ = minZ - SELECTION_GRID_FACE_OFFSET;
        double backZ = maxZ + SELECTION_GRID_FACE_OFFSET;

        for (int z = bounds.min.getZ() + stepZ; z < bounds.max.getZ() + 1; z += stepZ) {
            double lineZ = z - cameraZ;
            drawGridLine(buffer, leftX - cameraX, minY - cameraY, lineZ, leftX - cameraX, maxY - cameraY, lineZ, rgb, alpha);
            drawGridLine(buffer, rightX - cameraX, minY - cameraY, lineZ, rightX - cameraX, maxY - cameraY, lineZ, rgb, alpha);
        }

        for (int y = bounds.min.getY() + stepY; y < bounds.max.getY() + 1; y += stepY) {
            double lineY = y - cameraY;
            drawGridLine(buffer, leftX - cameraX, lineY, minZ - cameraZ, leftX - cameraX, lineY, maxZ - cameraZ, rgb, alpha);
            drawGridLine(buffer, rightX - cameraX, lineY, minZ - cameraZ, rightX - cameraX, lineY, maxZ - cameraZ, rgb, alpha);
        }

        for (int x = bounds.min.getX() + stepX; x < bounds.max.getX() + 1; x += stepX) {
            double lineX = x - cameraX;
            drawGridLine(buffer, lineX, bottomY - cameraY, minZ - cameraZ, lineX, bottomY - cameraY, maxZ - cameraZ, rgb, alpha);
            drawGridLine(buffer, lineX, topY - cameraY, minZ - cameraZ, lineX, topY - cameraY, maxZ - cameraZ, rgb, alpha);
        }

        for (int z = bounds.min.getZ() + stepZ; z < bounds.max.getZ() + 1; z += stepZ) {
            double lineZ = z - cameraZ;
            drawGridLine(buffer, minX - cameraX, bottomY - cameraY, lineZ, maxX - cameraX, bottomY - cameraY, lineZ, rgb, alpha);
            drawGridLine(buffer, minX - cameraX, topY - cameraY, lineZ, maxX - cameraX, topY - cameraY, lineZ, rgb, alpha);
        }

        for (int x = bounds.min.getX() + stepX; x < bounds.max.getX() + 1; x += stepX) {
            double lineX = x - cameraX;
            drawGridLine(buffer, lineX, minY - cameraY, frontZ - cameraZ, lineX, maxY - cameraY, frontZ - cameraZ, rgb, alpha);
            drawGridLine(buffer, lineX, minY - cameraY, backZ - cameraZ, lineX, maxY - cameraY, backZ - cameraZ, rgb, alpha);
        }

        for (int y = bounds.min.getY() + stepY; y < bounds.max.getY() + 1; y += stepY) {
            double lineY = y - cameraY;
            drawGridLine(buffer, minX - cameraX, lineY, frontZ - cameraZ, maxX - cameraX, lineY, frontZ - cameraZ, rgb, alpha);
            drawGridLine(buffer, minX - cameraX, lineY, backZ - cameraZ, maxX - cameraX, lineY, backZ - cameraZ, rgb, alpha);
        }
    }

    private static int selectionGridStep(int size) {
        if (size <= 1) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, Math.ceilDiv(size - 1, MAX_SELECTION_GRID_DIVISIONS));
    }

    private static void drawGridLine(VertexConsumer buffer,
                                     double x1,
                                     double y1,
                                     double z1,
                                     double x2,
                                     double y2,
                                     double z2,
                                     int rgb,
                                     float alpha) {
        float[] color = toColor(rgb, alpha);
        DrawUtil.drawLineSafe(buffer, x1, y1, z1, x2, y2, z2,
                color[0], color[1], color[2], color[3], SELECTION_GRID_LINE_WIDTH);
    }

    private static void renderSelectionCirclePreview(VertexConsumer visible,
                                                     VertexConsumer through,
                                                     SelectionBounds bounds,
                                                     double cameraX,
                                                     double cameraY,
                                                     double cameraZ) {
        Direction.Axis normalAxis = smallestAxis(bounds);
        double radius = switch (normalAxis) {
            case X -> Math.max(0.5, Math.min(bounds.sizeY(), bounds.sizeZ()) * 0.5);
            case Y -> Math.max(0.5, Math.min(bounds.sizeX(), bounds.sizeZ()) * 0.5);
            case Z -> Math.max(0.5, Math.min(bounds.sizeX(), bounds.sizeY()) * 0.5);
        };
        drawOrientedCircle(visible, bounds.centerX(), bounds.centerY(), bounds.centerZ(), radius, normalAxis, cameraX, cameraY, cameraZ, selectionShapeColor, 0.95f);
        drawOrientedCircle(through, bounds.centerX(), bounds.centerY(), bounds.centerZ(), radius, normalAxis, cameraX, cameraY, cameraZ, selectionShapeColor, 0.35f);
    }

    private static Direction.Axis smallestAxis(SelectionBounds bounds) {
        int x = bounds.sizeX();
        int y = bounds.sizeY();
        int z = bounds.sizeZ();
        if (x <= y && x <= z) {
            return Direction.Axis.X;
        }
        if (y <= x && y <= z) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }

    private static void drawOrientedCircle(VertexConsumer buffer,
                                           double centerX,
                                           double centerY,
                                           double centerZ,
                                           double radius,
                                           Direction.Axis normalAxis,
                                           double cameraX,
                                           double cameraY,
                                           double cameraZ,
                                           int rgb,
                                           float a) {
        float[] color = toColor(rgb, a);
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;

            Vec3d p0 = orientedCirclePoint(centerX, centerY, centerZ, radius, normalAxis, a0);
            Vec3d p1 = orientedCirclePoint(centerX, centerY, centerZ, radius, normalAxis, a1);
            DrawUtil.drawLineSafe(buffer,
                    p0.x - cameraX, p0.y - cameraY, p0.z - cameraZ,
                    p1.x - cameraX, p1.y - cameraY, p1.z - cameraZ,
                    color[0], color[1], color[2], color[3], 1.75f);
        }
    }

    private static Vec3d orientedCirclePoint(double centerX,
                                             double centerY,
                                             double centerZ,
                                             double radius,
                                             Direction.Axis normalAxis,
                                             double angle) {
        double cos = Math.cos(angle) * radius;
        double sin = Math.sin(angle) * radius;
        return switch (normalAxis) {
            case X -> new Vec3d(centerX, centerY + cos, centerZ + sin);
            case Y -> new Vec3d(centerX + cos, centerY, centerZ + sin);
            case Z -> new Vec3d(centerX + cos, centerY + sin, centerZ);
        };
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

    private static int readColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        try {
            return json.get(key).getAsInt() & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
        }
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

    private static void drawDiamond(VertexConsumer buffer,
                                    DiamondShape diamond,
                                    double cameraX,
                                    double cameraY,
                                    double cameraZ,
                                    float alphaMul) {
        float[] color = toColor(diamond.rgb, alphaMul);
        double cx = diamond.centerX - cameraX;
        double cy = diamond.centerY - cameraY;
        double cz = diamond.centerZ - cameraZ;
        double r = diamond.radius;

        Vec3d top = new Vec3d(cx, cy + r, cz);
        Vec3d bottom = new Vec3d(cx, cy - r, cz);
        Vec3d east = new Vec3d(cx + r, cy, cz);
        Vec3d west = new Vec3d(cx - r, cy, cz);
        Vec3d south = new Vec3d(cx, cy, cz + r);
        Vec3d north = new Vec3d(cx, cy, cz - r);

        drawShapeLine(buffer, top, east, color);
        drawShapeLine(buffer, top, west, color);
        drawShapeLine(buffer, top, south, color);
        drawShapeLine(buffer, top, north, color);
        drawShapeLine(buffer, bottom, east, color);
        drawShapeLine(buffer, bottom, west, color);
        drawShapeLine(buffer, bottom, south, color);
        drawShapeLine(buffer, bottom, north, color);
        drawShapeLine(buffer, east, south, color);
        drawShapeLine(buffer, south, west, color);
        drawShapeLine(buffer, west, north, color);
        drawShapeLine(buffer, north, east, color);
    }

    private static void drawPyramid(VertexConsumer buffer,
                                    PyramidShape pyramid,
                                    double cameraX,
                                    double cameraY,
                                    double cameraZ,
                                    float alphaMul) {
        float[] color = toColor(pyramid.rgb, alphaMul);
        double cx = pyramid.centerX - cameraX;
        double baseY = pyramid.baseY - cameraY;
        double cz = pyramid.centerZ - cameraZ;
        double r = pyramid.radius;

        Vec3d c1 = new Vec3d(cx - r, baseY, cz - r);
        Vec3d c2 = new Vec3d(cx + r, baseY, cz - r);
        Vec3d c3 = new Vec3d(cx + r, baseY, cz + r);
        Vec3d c4 = new Vec3d(cx - r, baseY, cz + r);
        Vec3d apex = new Vec3d(cx, baseY + pyramid.height, cz);

        drawShapeLine(buffer, c1, c2, color);
        drawShapeLine(buffer, c2, c3, color);
        drawShapeLine(buffer, c3, c4, color);
        drawShapeLine(buffer, c4, c1, color);
        drawShapeLine(buffer, c1, apex, color);
        drawShapeLine(buffer, c2, apex, color);
        drawShapeLine(buffer, c3, apex, color);
        drawShapeLine(buffer, c4, apex, color);
    }

    private static void drawCone(VertexConsumer buffer,
                                 ConeShape cone,
                                 double cameraX,
                                 double cameraY,
                                 double cameraZ,
                                 float alphaMul) {
        float[] color = toColor(cone.rgb, alphaMul);
        CircleShape base = new CircleShape(-1, Long.MAX_VALUE, cone.rgb, cone.centerX, cone.baseY, cone.centerZ, cone.radius, cone.segments);
        drawCircle(buffer, base, cameraX, cameraY, cameraZ, alphaMul);

        Vec3d apex = new Vec3d(cone.centerX - cameraX, cone.baseY + cone.height - cameraY, cone.centerZ - cameraZ);
        int sideStep = Math.max(1, cone.segments / 8);
        for (int i = 0; i < cone.segments; i += sideStep) {
            double angle = (Math.PI * 2.0 * i) / cone.segments;
            Vec3d edge = new Vec3d(
                    cone.centerX + Math.cos(angle) * cone.radius - cameraX,
                    cone.baseY - cameraY,
                    cone.centerZ + Math.sin(angle) * cone.radius - cameraZ
            );
            drawShapeLine(buffer, edge, apex, color);
        }
    }

    private static void drawShapeLine(VertexConsumer buffer, Vec3d from, Vec3d to, float[] color) {
        DrawUtil.drawLineSafe(buffer, from.x, from.y, from.z, to.x, to.y, to.z, color[0], color[1], color[2], color[3], 1.25f);
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
                    case "diamond" -> new DiamondShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("centerY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble());
                    case "pyramid" -> new PyramidShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("baseY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble(), json.get("height").getAsDouble());
                    case "cone" -> new ConeShape(id, expires, rgb,
                            json.get("centerX").getAsDouble(), json.get("baseY").getAsDouble(), json.get("centerZ").getAsDouble(),
                            json.get("radius").getAsDouble(), json.get("height").getAsDouble(), clampSegments(json.get("segments").getAsInt()));
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

    private static final class DiamondShape extends DrawShape {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;

        private DiamondShape(int id, long expiresAtMillis, int rgb,
                             double centerX, double centerY, double centerZ,
                             double radius) {
            super(id, expiresAtMillis, rgb);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "diamond");
            json.addProperty("centerX", this.centerX);
            json.addProperty("centerY", this.centerY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            return json;
        }
    }

    private static final class PyramidShape extends DrawShape {
        private final double centerX;
        private final double baseY;
        private final double centerZ;
        private final double radius;
        private final double height;

        private PyramidShape(int id, long expiresAtMillis, int rgb,
                             double centerX, double baseY, double centerZ,
                             double radius, double height) {
            super(id, expiresAtMillis, rgb);
            this.centerX = centerX;
            this.baseY = baseY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.height = height;
        }

        @Override
        protected JsonObject toJson(long now) {
            JsonObject json = baseJson(now, "pyramid");
            json.addProperty("centerX", this.centerX);
            json.addProperty("baseY", this.baseY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            json.addProperty("height", this.height);
            return json;
        }
    }

    private static final class ConeShape extends DrawShape {
        private final double centerX;
        private final double baseY;
        private final double centerZ;
        private final double radius;
        private final double height;
        private final int segments;

        private ConeShape(int id, long expiresAtMillis, int rgb,
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
            JsonObject json = baseJson(now, "cone");
            json.addProperty("centerX", this.centerX);
            json.addProperty("baseY", this.baseY);
            json.addProperty("centerZ", this.centerZ);
            json.addProperty("radius", this.radius);
            json.addProperty("height", this.height);
            json.addProperty("segments", this.segments);
            return json;
        }
    }
}

