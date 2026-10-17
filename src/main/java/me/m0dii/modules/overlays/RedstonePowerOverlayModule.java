package me.m0dii.modules.overlays;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RedstonePowerOverlayModule extends BlockTextOverlayModule implements PersistentSettings {

    public static final RedstonePowerOverlayModule INSTANCE = new RedstonePowerOverlayModule();

    private int XZ_RADIUS = 16;
    private int Y_RADIUS = 4;
    private boolean showComponents = true;

    private RedstonePowerOverlayModule() {
        super("redstone_overlay", "Redstone Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(KeybindCatalog.REDSTONE_OVERLAY_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.REDSTONE_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Nullable
    @Override
    protected TextRenderInfo shouldRender(BlockPos pos, Vec3 cameraPos) {
        BlockState state = getClient().level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        if (state.getBlock() instanceof RedstoneWireBlock) {
            int wirePower;
            try {
                wirePower = state.getValue(RedstoneWireBlock.POWER);
            } catch (Exception e) {
                return null;
            }
            return new TextRenderInfo(Integer.toString(wirePower), powerColor(wirePower), 0.5, 0.05, 0.35);
        }

        if (!showComponents) {
            return null;
        }

        String text = null;
        int color = 0xFFFFFF;
        try {
            if (getClient().level.getBlockEntity(pos) instanceof ComparatorBlockEntity comparator) {
                int signal = comparator.getOutputSignal();
                boolean subtract = state.hasProperty(BlockStateProperties.MODE_COMPARATOR)
                        && state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.SUBTRACT;
                text = signal + (subtract ? "-" : "");
                color = powerColor(signal);
            } else if (state.hasProperty(BlockStateProperties.DELAY)) {
                int delay = state.getValue(BlockStateProperties.DELAY);
                boolean powered = state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED);
                boolean locked = state.hasProperty(BlockStateProperties.LOCKED) && state.getValue(BlockStateProperties.LOCKED);
                text = delay + "t" + (locked ? " locked" : "");
                color = powered ? 0xFF5555 : 0xCCCCCC;
            } else if (state.hasProperty(BlockStateProperties.POWER)) {
                int power = state.getValue(BlockStateProperties.POWER);
                text = Integer.toString(power);
                color = powerColor(power);
            } else if (state.getBlock() instanceof RedstoneTorchBlock && state.hasProperty(BlockStateProperties.LIT)) {
                boolean lit = state.getValue(BlockStateProperties.LIT);
                text = lit ? "ON" : "off";
                color = lit ? 0xFF5555 : 0xAAAAAA;
            } else if (state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) {
                text = "ON";
                color = 0xFF5555;
            }
        } catch (RuntimeException e) {
            return null;
        }

        if (text == null) {
            return null;
        }
        // Float above the block (and above repeater/comparator torches) so the model never hides the label.
        return TextRenderInfo.floating(text, color, 0.5, Math.max(topOf(state, pos), 0.45) + 0.18, 0.5);
    }

    private double topOf(BlockState state, BlockPos pos) {
        try {
            VoxelShape shape = state.getShape(getClient().level, pos);
            return shape.isEmpty() ? 0.05 : shape.max(Direction.Axis.Y);
        } catch (RuntimeException e) {
            return 1.0;
        }
    }

    private static int powerColor(int power) {
        int g = (int) Math.round((Math.clamp(power, 0, 15) / 15.0) * 255.0);
        return (0xFF << 16) | (g << 8);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("XZ radius : " + XZ_RADIUS);
        settings.add("Y radius : " + Y_RADIUS);
        settings.add("X+");
        settings.add("X-");
        settings.add("Y+");
        settings.add("Y-");
        settings.add("Components (repeaters, comparators, ...): " + (showComponents ? "ON" : "OFF"));

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> XZ_RADIUS = Math.min(64, XZ_RADIUS + 1);
            case 4 -> XZ_RADIUS = Math.max(1, XZ_RADIUS - 1);
            case 5 -> Y_RADIUS = Math.min(32, Y_RADIUS + 1);
            case 6 -> Y_RADIUS = Math.max(1, Y_RADIUS - 1);
            case 7 -> showComponents = !showComponents;
            default -> {
                // Nothing
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("xzRadius", XZ_RADIUS);
        json.addProperty("yRadius", Y_RADIUS);
        json.addProperty("showComponents", showComponents);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        XZ_RADIUS = PersistentSettings.readInt(settings, "xzRadius", XZ_RADIUS, 1, 64);
        Y_RADIUS = PersistentSettings.readInt(settings, "yRadius", Y_RADIUS, 1, 32);
        showComponents = PersistentSettings.readBoolean(settings, "showComponents", showComponents);
    }

    @Override
    protected int getXZRadius() {
        return XZ_RADIUS;
    }

    @Override
    protected int getYRadius() {
        return Y_RADIUS;
    }
}
