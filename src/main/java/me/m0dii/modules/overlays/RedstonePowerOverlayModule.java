package me.m0dii.modules.overlays;

import me.m0dii.utils.KeybindCatalog;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RedstonePowerOverlayModule extends BlockTextOverlayModule {

    public static final RedstonePowerOverlayModule INSTANCE = new RedstonePowerOverlayModule();

    private int XZ_RADIUS = 16;
    private int Y_RADIUS = 4;

    private RedstonePowerOverlayModule() {
        super("redstone_overlay", "Redstone Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(KeybindCatalog.REDSTONE_OVERLAY_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.REDSTONE_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Nullable
    @Override
    protected TextRenderInfo shouldRender(BlockPos pos, Vec3d cameraPos) {
        BlockState state = getClient().world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        if (!(state.getBlock() instanceof RedstoneWireBlock)) {
            return null;
        }

        int wirePower;
        try {
            wirePower = state.get(RedstoneWireBlock.POWER);
        } catch (Exception e) {
            return null;
        }

        int g = (int) Math.round((wirePower / 15.0) * 255.0);
        int color = (0xFF << 16) | (g << 8);

        return new TextRenderInfo(Integer.toString(wirePower), color, 0.5, 0.05, 0.35);
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

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> XZ_RADIUS++;
            case 4 -> XZ_RADIUS = Math.max(1, XZ_RADIUS - 1);
            case 5 -> Y_RADIUS++;
            case 6 -> Y_RADIUS = Math.max(1, Y_RADIUS - 1);
            default -> {
                // Nothing
            }
        }
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
