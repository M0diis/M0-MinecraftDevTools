package me.m0dii.modules.debugdraw;


import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.gui.local.UiForms;
import me.m0dii.gui.local.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

public final class DebugDrawScreen extends Screen implements ThemedScreen {
    private static String sType = "line";
    private static String sPos1X = "~";
    private static String sPos1Y = "~";
    private static String sPos1Z = "~";
    private static String sPos2X = "~1";
    private static String sPos2Y = "~1";
    private static String sPos2Z = "~1";
    private static String sColor = "#00FFFF";
    private static String sSeconds = "20";
    private static String sRadius = "3";
    private static String sHeight = "4";
    private static String sSegments = "36";

    private final Screen parent;

    private EditBox typeField;
    private EditBox pos1XField;
    private EditBox pos1YField;
    private EditBox pos1ZField;
    private EditBox pos2XField;
    private EditBox pos2YField;
    private EditBox pos2ZField;
    private EditBox colorField;
    private EditBox secondsField;
    private EditBox radiusField;
    private EditBox heightField;
    private EditBox segmentsField;

    private final List<DebugDrawManager.ShapeDescriptor> shapes = new ArrayList<>();
    private int selectedShapeIdx = -1;
    private String status = "";

    private DebugDrawScreen(Screen parent) {
        super(Component.literal("Debug Draw"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new DebugDrawScreen(parent);
    }

    @Override
    protected void init() {
        int margin = 10;
        int leftW = Math.max(310, (this.width - margin * 3) / 2);
        int rightX = margin * 2 + leftW;
        int rightW = Math.max(220, this.width - rightX - margin);

        int top = 24;
        int bottom = this.height - 34;

        this.typeField = addField(margin + 86, top + 10, 110, sType);
        this.pos1XField = addField(margin + 86, top + 36, 58, sPos1X);
        this.pos1YField = addField(margin + 148, top + 36, 58, sPos1Y);
        this.pos1ZField = addField(margin + 210, top + 36, 58, sPos1Z);

        this.pos2XField = addField(margin + 86, top + 60, 58, sPos2X);
        this.pos2YField = addField(margin + 148, top + 60, 58, sPos2Y);
        this.pos2ZField = addField(margin + 210, top + 60, 58, sPos2Z);

        this.colorField = addField(margin + 86, top + 86, 86, sColor);
        this.secondsField = addField(margin + 176, top + 86, 54, sSeconds);
        this.radiusField = addField(margin + 86, top + 110, 48, sRadius);
        this.heightField = addField(margin + 138, top + 110, 48, sHeight);
        this.segmentsField = addField(margin + 190, top + 110, 78, sSegments);

        this.addRenderableWidget(Button.builder(Component.literal("Type +"), b -> stepType(1)).bounds(margin + 200, top + 10, 68, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pos1 Here"), b -> setPosFromPlayer(true)).bounds(margin + 272, top + 36, 76, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pos2 Here"), b -> setPosFromPlayer(false)).bounds(margin + 272, top + 60, 76, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pos2 Look"), b -> setPos2FromLook()).bounds(margin + 352, top + 60, 76, 20).build());

        int actionY = top + 138;
        this.addRenderableWidget(Button.builder(Component.literal("Add/Draw"), b -> addShapeFromFields()).bounds(margin, actionY, 88, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Update"), b -> updateSelectedShape()).bounds(margin + 92, actionY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Remove"), b -> removeSelectedShape()).bounds(margin + 166, actionY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveShapes()).bounds(margin + 240, actionY, 58, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Load"), b -> loadShapes()).bounds(margin + 302, actionY, 58, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Clear"), b -> clearShapes()).bounds(margin + 364, actionY, 58, 20).build());

        int selectY = actionY + 24;
        this.addRenderableWidget(Button.builder(Component.literal("Sel On/Off"), b -> toggleSelectionEnabled()).bounds(margin, selectY, 84, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sel Pos1"), b -> setSelectionPosFromLook(false)).bounds(margin + 88, selectY, 68, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sel Pos2"), b -> setSelectionPosFromLook(true)).bounds(margin + 160, selectY, 68, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sel->Form"), b -> loadFormFromSelection()).bounds(margin + 232, selectY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sel Add"), b -> addShapeFromSelection()).bounds(margin + 306, selectY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sel Clear"), b -> clearSelection()).bounds(margin + 370, selectY, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Prev"), b -> moveSelection(-1)).bounds(rightX + 8, top + 10, 56, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Next"), b -> moveSelection(1)).bounds(rightX + 68, top + 10, 56, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Use Sel"), b -> loadSelectionIntoFields()).bounds(rightX + 128, top + 10, 64, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 76, this.height - 24, 64, 20)
                .build());

        refreshShapeList(false);
        this.selectedShapeIdx = Math.clamp(this.selectedShapeIdx, -1, this.shapes.size() - 1);
    }

    private EditBox addField(int x, int y, int width, String value) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.empty());
        field.setMaxLength(96);
        field.setValue(value == null ? "" : value);
        this.addRenderableWidget(field);
        return field;
    }

    private void refreshShapeList(boolean keepStatus) {
        this.shapes.clear();
        this.shapes.addAll(DebugDrawManager.getActiveShapeDescriptors());
        if (!keepStatus) {
            this.status = "Loaded " + this.shapes.size() + " shape(s).";
        }
    }

    private void moveSelection(int delta) {
        if (this.shapes.isEmpty()) {
            this.selectedShapeIdx = -1;
            this.status = "No shapes.";
            return;
        }
        int current = Math.max(this.selectedShapeIdx, 0);
        this.selectedShapeIdx = Math.clamp(current + delta, 0, this.shapes.size() - 1);
        this.status = "Selected #" + this.shapes.get(this.selectedShapeIdx).id();
    }

    private void loadSelectionIntoFields() {
        if (this.selectedShapeIdx < 0 || this.selectedShapeIdx >= this.shapes.size()) {
            this.status = "No selected shape.";
            return;
        }
        DebugDrawManager.ShapeDescriptor d = this.shapes.get(this.selectedShapeIdx);
        this.typeField.setValue(d.type());
        this.pos1XField.setValue(fmt(d.x1()));
        this.pos1YField.setValue(fmt(d.y1()));
        this.pos1ZField.setValue(fmt(d.z1()));
        this.pos2XField.setValue(fmt(d.x2()));
        this.pos2YField.setValue(fmt(d.y2()));
        this.pos2ZField.setValue(fmt(d.z2()));
        this.colorField.setValue(DebugDrawManager.formatColor(d.rgb()));
        this.secondsField.setValue(fmt(d.secondsRemaining()));
        this.radiusField.setValue(fmt(d.radius()));
        this.heightField.setValue(fmt(d.height()));
        this.segmentsField.setValue(Integer.toString(Math.max(8, d.segments())));
        this.status = "Loaded shape #" + d.id() + " into form.";
        persistForm();
    }

    private void addShapeFromFields() {
        persistForm();
        ShapeData d = parseForm();
        if (d == null) {
            return;
        }
        int id;
        id = switch (d.type) {
            case "line" -> DebugDrawManager.addLine(d.x1, d.y1, d.z1, d.x2, d.y2, d.z2, d.rgb, d.seconds);
            case "box" -> DebugDrawManager.addBox(d.x1, d.y1, d.z1, d.x2, d.y2, d.z2, d.rgb, d.seconds);
            case "circle" -> DebugDrawManager.addCircle(d.x1, d.y1, d.z1, d.radius, d.rgb, d.seconds, d.segments);
            case "cylinder" ->
                    DebugDrawManager.addCylinder(d.x1, d.y1, d.z1, d.radius, d.height, d.rgb, d.seconds, d.segments);
            case "sphere" -> DebugDrawManager.addSphere(d.x1, d.y1, d.z1, d.radius, d.rgb, d.seconds, d.segments);
            case "diamond" -> DebugDrawManager.addDiamond(d.x1, d.y1, d.z1, d.radius, d.rgb, d.seconds);
            case "pyramid" -> DebugDrawManager.addPyramid(d.x1, d.y1, d.z1, d.radius, d.height, d.rgb, d.seconds);
            case "cone" -> DebugDrawManager.addCone(d.x1, d.y1, d.z1, d.radius, d.height, d.rgb, d.seconds, d.segments);
            default -> -1;
        };
        if (id < 0) {
            this.status = "Unknown type: " + d.type;
            return;
        }
        refreshShapeList(true);
        this.selectedShapeIdx = findShapeIndexById(id);
        this.status = "Added shape #" + id;
    }

    private void updateSelectedShape() {
        if (this.selectedShapeIdx < 0 || this.selectedShapeIdx >= this.shapes.size()) {
            this.status = "Select a shape first.";
            return;
        }
        persistForm();
        ShapeData d = parseForm();
        if (d == null) {
            return;
        }
        int id = this.shapes.get(this.selectedShapeIdx).id();
        DebugDrawManager.ShapeDescriptor descriptor = new DebugDrawManager.ShapeDescriptor(
                id, d.type, d.rgb, d.seconds, d.segments,
                d.x1, d.y1, d.z1, d.x2, d.y2, d.z2, d.radius, d.height
        );
        boolean ok = DebugDrawManager.updateShape(descriptor);
        refreshShapeList(true);
        this.selectedShapeIdx = findShapeIndexById(id);
        this.status = ok ? "Updated shape #" + id : "Update failed.";
    }

    private void removeSelectedShape() {
        if (this.selectedShapeIdx < 0 || this.selectedShapeIdx >= this.shapes.size()) {
            this.status = "Select a shape first.";
            return;
        }
        int id = this.shapes.get(this.selectedShapeIdx).id();
        DebugDrawManager.remove(id);
        refreshShapeList(true);
        this.selectedShapeIdx = Math.min(this.selectedShapeIdx, this.shapes.size() - 1);
        this.status = "Removed shape #" + id;
    }

    private int findShapeIndexById(int id) {
        for (int i = 0; i < this.shapes.size(); i++) {
            if (this.shapes.get(i).id() == id) {
                return i;
            }
        }
        return -1;
    }

    private void saveShapes() {
        int saved = DebugDrawManager.saveToDisk();
        this.status = saved >= 0 ? "Saved " + saved + " shapes." : "Save failed.";
    }

    private void loadShapes() {
        int loaded = DebugDrawManager.loadFromDisk();
        refreshShapeList(true);
        this.selectedShapeIdx = Math.min(this.selectedShapeIdx, this.shapes.size() - 1);
        this.status = loaded >= 0 ? "Loaded " + loaded + " shapes." : "Load failed.";
    }

    private void clearShapes() {
        int removed = DebugDrawManager.clear();
        refreshShapeList(true);
        this.selectedShapeIdx = -1;
        this.status = "Cleared " + removed + " shapes.";
    }

    private void setPosFromPlayer(boolean first) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (first) {
            this.pos1XField.setValue(Integer.toString(client.player.getBlockX()));
            this.pos1YField.setValue(Integer.toString(client.player.getBlockY()));
            this.pos1ZField.setValue(Integer.toString(client.player.getBlockZ()));
        } else {
            this.pos2XField.setValue(Integer.toString(client.player.getBlockX()));
            this.pos2YField.setValue(Integer.toString(client.player.getBlockY()));
            this.pos2ZField.setValue(Integer.toString(client.player.getBlockZ()));
        }
    }

    private void setPos2FromLook() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            this.status = "No block under crosshair.";
            return;
        }
        this.pos2XField.setValue(Integer.toString(hit.getBlockPos().getX()));
        this.pos2YField.setValue(Integer.toString(hit.getBlockPos().getY()));
        this.pos2ZField.setValue(Integer.toString(hit.getBlockPos().getZ()));
    }

    private void toggleSelectionEnabled() {
        boolean next = !DebugDrawManager.isSelectionEnabled();
        DebugDrawManager.setSelectionEnabled(next);
        this.status = "Selection " + (next ? "enabled" : "disabled") + ".";
    }

    private void setSelectionPosFromLook(boolean rightClick) {
        boolean ok = DebugDrawManager.pickSelectionPos(rightClick);
        this.status = ok ? "Set " + (rightClick ? "pos2" : "pos1") + " from crosshair." : "No block under crosshair.";
    }

    private void loadFormFromSelection() {
        BlockPos pos1 = DebugDrawManager.getSelectionPos1();
        BlockPos pos2 = DebugDrawManager.getSelectionPos2();
        if (pos1 == null) {
            this.status = "Selection pos1 not set.";
            return;
        }
        this.pos1XField.setValue(Integer.toString(pos1.getX()));
        this.pos1YField.setValue(Integer.toString(pos1.getY()));
        this.pos1ZField.setValue(Integer.toString(pos1.getZ()));
        if (pos2 != null) {
            this.pos2XField.setValue(Integer.toString(pos2.getX()));
            this.pos2YField.setValue(Integer.toString(pos2.getY()));
            this.pos2ZField.setValue(Integer.toString(pos2.getZ()));
        }
        this.typeField.setValue(DebugDrawManager.getSelectionShape().name().toLowerCase());
        persistForm();
        this.status = pos2 == null ? "Loaded pos1 from selection." : "Loaded selection into form.";
    }

    private void addShapeFromSelection() {
        persistForm();
        try {
            int rgb = DrawClientCommand.parseColorToken(this.colorField.getValue());
            double seconds = Math.max(0.2, parseDouble(this.secondsField.getValue(), 20.0));
            int id = DebugDrawManager.addSelectionShape(rgb, seconds);
            if (id < 0) {
                this.status = "Set selection pos1 and pos2 first.";
                return;
            }
            refreshShapeList(true);
            this.selectedShapeIdx = findShapeIndexById(id);
            this.status = "Added selection shape #" + id;
        } catch (Exception e) {
            this.status = "Invalid color/TTL: " + e.getMessage();
        }
    }

    private void clearSelection() {
        DebugDrawManager.clearSelectionPositions();
        this.status = "Cleared selection positions.";
    }

    private void stepType(int delta) {
        String[] types = {"line", "box", "circle", "cylinder", "sphere", "diamond", "pyramid", "cone"};
        String current = this.typeField.getValue().trim().toLowerCase();
        int idx = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(current)) {
                idx = i;
                break;
            }
        }
        int next = Math.floorMod(idx + delta, types.length);
        this.typeField.setValue(types[next]);
    }

    private void persistForm() {
        sType = this.typeField.getValue();
        sPos1X = this.pos1XField.getValue();
        sPos1Y = this.pos1YField.getValue();
        sPos1Z = this.pos1ZField.getValue();
        sPos2X = this.pos2XField.getValue();
        sPos2Y = this.pos2YField.getValue();
        sPos2Z = this.pos2ZField.getValue();
        sColor = this.colorField.getValue();
        sSeconds = this.secondsField.getValue();
        sRadius = this.radiusField.getValue();
        sHeight = this.heightField.getValue();
        sSegments = this.segmentsField.getValue();
    }

    private ShapeData parseForm() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            this.status = "No player context.";
            return null;
        }
        try {
            String type = this.typeField.getValue().trim().toLowerCase();
            double x1 = parseCoord(this.pos1XField.getValue(), client.player.getX());
            double y1 = parseCoord(this.pos1YField.getValue(), client.player.getY());
            double z1 = parseCoord(this.pos1ZField.getValue(), client.player.getZ());
            double x2 = parseCoord(this.pos2XField.getValue(), client.player.getX());
            double y2 = parseCoord(this.pos2YField.getValue(), client.player.getY());
            double z2 = parseCoord(this.pos2ZField.getValue(), client.player.getZ());
            int rgb = DrawClientCommand.parseColorToken(this.colorField.getValue());
            double seconds = Math.max(0.2, parseDouble(this.secondsField.getValue(), 20.0));
            double radius = Math.max(0.05, parseDouble(this.radiusField.getValue(), 2.0));
            double height = Math.max(0.05, parseDouble(this.heightField.getValue(), 4.0));
            int segments = Math.max(8, (int) parseDouble(this.segmentsField.getValue(), 36));
            return new ShapeData(type, x1, y1, z1, x2, y2, z2, rgb, seconds, radius, height, segments);
        } catch (Exception e) {
            this.status = "Invalid input: " + e.getMessage();
            return null;
        }
    }

    private static double parseCoord(String token, double base) {
        String t = token == null ? "" : token.trim();
        if (t.isEmpty() || "~".equals(t)) {
            return base;
        }
        if (t.startsWith("~")) {
            return base + Double.parseDouble(t.substring(1));
        }
        return Double.parseDouble(t);
    }

    private static double parseDouble(String token, double fallback) {
        try {
            return Double.parseDouble(token == null ? "" : token.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }

    @Override
    public void onClose() {
        persistForm();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, UiTheme.BG);

        int margin = 10;
        int leftW = Math.max(310, (this.width - margin * 3) / 2);
        int rightX = margin * 2 + leftW;
        int rightW = Math.max(220, this.width - rightX - margin);
        int top = 24;
        int bottom = this.height - 34;

        UiForms.drawPanel(context, margin, top, leftW, bottom - top);
        UiForms.drawPanel(context, rightX, top, rightW, bottom - top);

        context.text(this.font, "Debug Draw Editor", margin, 10, UiTheme.TEXT);

        context.text(this.font, "Type", margin + 8, top + 16, UiTheme.TEXT_MUTED);
        context.text(this.font, "Pos1", margin + 8, top + 42, UiTheme.TEXT_MUTED);
        context.text(this.font, "Pos2", margin + 8, top + 66, UiTheme.TEXT_MUTED);
        context.text(this.font, "Color", margin + 8, top + 92, UiTheme.TEXT_MUTED);
        context.text(this.font, "TTL", margin + 176, top + 92, UiTheme.TEXT_MUTED);
        context.text(this.font, "R", margin + 8, top + 116, UiTheme.TEXT_MUTED);
        context.text(this.font, "H", margin + 138, top + 116, UiTheme.TEXT_MUTED);
        context.text(this.font, "Seg", margin + 190, top + 116, UiTheme.TEXT_MUTED);

        context.text(this.font, "Shapes", rightX + 8, top + 38, UiTheme.TEXT);
        int y = top + 52;
        int max = Math.min(this.shapes.size(), 16);
        for (int i = 0; i < max; i++) {
            DebugDrawManager.ShapeDescriptor d = this.shapes.get(i);
            int color = (i == this.selectedShapeIdx) ? 0xFFB7E3FF : UiTheme.TEXT_MUTED;
            context.text(this.font,
                    String.format("#%d %s %s", d.id(), d.type(), DebugDrawManager.formatColor(d.rgb())),
                    rightX + 8, y, color);
            y += 10;
        }

        context.text(this.font,
                "Use 'Use Sel' then edit fields, click Update.",
                margin, this.height - 44, UiTheme.TEXT_MUTED);
        context.text(this.font,
                "Selection: " + DebugDrawManager.selectionStatus(),
                margin, this.height - 56, UiTheme.TEXT_MUTED);
        context.text(this.font, this.status, margin, this.height - 30, UiTheme.TEXT_ACCENT);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private record ShapeData(String type,
                             double x1,
                             double y1,
                             double z1,
                             double x2,
                             double y2,
                             double z2,
                             int rgb,
                             double seconds,
                             double radius,
                             double height,
                             int segments) {
    }
}

