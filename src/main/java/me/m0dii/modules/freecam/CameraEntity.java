package me.m0dii.modules.freecam;


import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CameraEntity extends LocalPlayer {
    public interface CameraController {
        void tick(CameraEntity camera);

        boolean isActive();

        default boolean blocksManualControl() {
            return false;
        }
    }

    @Nullable
    private static CameraEntity camera;

    @Nullable
    private static Entity originalCameraEntity;
    @Nullable
    private static CameraController controller;
    private static Vec3 cameraMotion = new Vec3(0.0, 0.0, 0.0);

    private static boolean cullChunksOriginal;
    private static boolean sprinting;
    private static boolean originalCameraWasPlayer;

    private CameraEntity(@NotNull Minecraft mc,
                         @NotNull ClientLevel world,
                         @NotNull ClientPacketListener netHandler,
                         @NotNull StatsCounter stats,
                         @NotNull ClientRecipeBook recipeBook) {
        // MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler,
        // StatHandler stats, ClientRecipeBook recipeBook, PlayerInput lastPlayerInput, boolean lastSprinting
        super(mc, world, netHandler, stats, recipeBook, null, false, mc.player.chatAbilities(), new net.minecraft.client.player.ItemActivation());
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    public static void movementTick() {
        CameraEntity camera = getCamera();

        if (camera != null) {
            if (controller != null) {
                camera.updateLastTickPosition();
                controller.tick(camera);
                if (controller.isActive()) {
                    return;
                }
                controller = null;
                return;
            }

            Options options = Minecraft.getInstance().options;

            camera.updateLastTickPosition();

            if (FreecamModule.INSTANCE.isControllingPlayer()) {
                // The player is being moved; keep the camera still.
                cameraMotion = Vec3.ZERO;
                return;
            }

            if (options.keySprint.isDown()) {
                sprinting = true;
            } else if (!options.keyUp.isDown() && !options.keyDown.isDown()) {
                sprinting = false;
            }

            cameraMotion = calculatePlayerMotionWithDeceleration(cameraMotion, 0.15, 0.4);
            double forward = sprinting ? cameraMotion.x * 3 : cameraMotion.x;

            camera.handleMotion(forward, cameraMotion.y, cameraMotion.z);
        }
    }

    public static Vec3 calculatePlayerMotionWithDeceleration(@NotNull Vec3 lastMotion,
                                                              double rampAmount,
                                                              double decelerationFactor) {
        Options options = Minecraft.getInstance().options;
        int forward = 0;
        int vertical = 0;
        int strafe = 0;

        if (options.keyUp.isDown()) {
            forward += 1;
        }
        if (options.keyDown.isDown()) {
            forward -= 1;
        }
        if (options.keyLeft.isDown()) {
            strafe += 1;
        }
        if (options.keyRight.isDown()) {
            strafe -= 1;
        }
        if (options.keyJump.isDown()) {
            vertical += 1;
        }
        if (options.keyShift.isDown()) {
            vertical -= 1;
        }

        double speed = (forward != 0 && strafe != 0) ? 1.2 : 1.0;
        double forwardRamped = getRampedMotion(lastMotion.x, forward, rampAmount, decelerationFactor) / speed;
        double verticalRamped = getRampedMotion(lastMotion.y, vertical, rampAmount, decelerationFactor);
        double strafeRamped = getRampedMotion(lastMotion.z, strafe, rampAmount, decelerationFactor) / speed;

        return new Vec3(forwardRamped, verticalRamped, strafeRamped);
    }

    public static double getRampedMotion(double current, int input, double rampAmount, double decelerationFactor) {
        if (input != 0) {
            if (input < 0) {
                rampAmount *= -1.0;
            }

            // Immediately kill the motion when changing direction to the opposite
            if ((input < 0) != (current < 0.0)) {
                current = 0.0;
            }

            current = Mth.clamp(current + rampAmount, -1.0, 1.0);
        } else {
            current *= decelerationFactor;
        }

        return current;
    }

    private static double getMoveSpeed() {
        double base = 0.07;
        return base * 10 * FreecamModule.INSTANCE.speedMultiplier();
    }

    private void handleMotion(double forward, double up, double strafe) {
        float yaw = this.getYRot();
        double scale = getMoveSpeed();
        double xFactor = Math.sin(yaw * Math.PI / 180.0);
        double zFactor = Math.cos(yaw * Math.PI / 180.0);

        double x = (strafe * zFactor - forward * xFactor) * scale;
        double y = up * scale;
        double z = (forward * zFactor + strafe * xFactor) * scale;

        this.setDeltaMovement(new Vec3(x, y, z));
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void updateLastTickPosition() {
        // Camera interpolation uses lastX/lastY/lastZ and lastYaw/lastPitch.
        // Keep them in sync before applying this tick's movement.
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.yHeadRotO = this.yHeadRot;

        // Keep render history aligned as well.
        this.xOld = this.xo;
        this.yOld = this.yo;
        this.zOld = this.zo;
    }

    public void setCameraRotations(float yaw, float pitch) {
        this.setYRot(yaw);
        this.setXRot(pitch);

        this.yHeadRot = yaw;
    }

    public void setScriptedPose(Vec3 position, float yaw, float pitch) {
        this.setPos(position.x, position.y, position.z);
        this.setRot(yaw, pitch);
        this.setCameraRotations(yaw, pitch);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void setRenderPoseExact(Vec3 position, float yaw, float pitch) {
        this.xo = position.x;
        this.yo = position.y;
        this.zo = position.z;
        this.xOld = position.x;
        this.yOld = position.y;
        this.zOld = position.z;
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.yHeadRotO = yaw;
        this.setScriptedPose(position, yaw, pitch);
    }

    public void updateCameraRotations(float yawChange, float pitchChange) {
        float yaw = this.getYRot() + yawChange * 0.15F;
        float pitch = Mth.clamp(this.getXRot() + pitchChange * 0.15F, -90F, 90F);

        this.setYRot(yaw);
        this.setXRot(pitch);

        this.setCameraRotations(yaw, pitch);
    }

    private static CameraEntity create(@NotNull Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }

        CameraEntity camera = new CameraEntity(mc, mc.level, player.connection, player.getStats(), player.getRecipeBook());
        camera.noPhysics = true;
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        camera.snapTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        camera.setRot(yaw, pitch);

        return camera;
    }

    @Nullable
    public static CameraEntity getCamera() {
        return camera;
    }

    public static boolean hasController() {
        return controller != null && controller.isActive();
    }

    public static boolean blocksManualControl() {
        return controller != null && controller.isActive() && controller.blocksManualControl();
    }

    public static void setController(@Nullable CameraController nextController) {
        controller = nextController;
    }

    public static void clearController() {
        controller = null;
    }

    public static void setCameraState(boolean enabled) {
        Minecraft mc = Minecraft.getInstance();

        if (!enabled) {
            // Always clear the static state, even when the world is already gone (disconnect),
            // so a stale camera from the previous world is never reused.
            removeCamera(mc);
            return;
        }

        if (mc.level != null && mc.player != null) {
            createAndSetCamera(mc);
        }
    }

    /**
     * True when the active camera belongs to a world other than the one currently loaded
     * (after a disconnect, dimension change or respawn into a new level).
     */
    public static boolean isStale() {
        Minecraft mc = Minecraft.getInstance();
        return camera == null || mc.level == null || mc.player == null || camera.level() != mc.level;
    }

    private static void createAndSetCamera(Minecraft mc) {
        camera = create(mc);
        if (camera == null) {
            return;
        }

        originalCameraEntity = mc.getCameraEntity();
        originalCameraWasPlayer = originalCameraEntity == mc.player;
        cullChunksOriginal = mc.smartCull;
        cameraMotion = Vec3.ZERO;
        sprinting = false;

        mc.setCameraEntity(camera);
        mc.smartCull = false; // Disable chunk culling
    }

    private static void removeCamera(Minecraft mc) {
        if (mc.level != null && camera != null) {
            // Re-fetch the player entity, in case the player died while in Free Camera mode and the instance changed
            boolean originalStillValid = originalCameraEntity != null
                    && !originalCameraEntity.isRemoved()
                    && originalCameraEntity.level() == mc.level;
            mc.setCameraEntity(originalCameraWasPlayer || !originalStillValid ? mc.player : originalCameraEntity);
            mc.smartCull = cullChunksOriginal;

            final int chunkX = Mth.floor(camera.getX()) >> 4;
            final int chunkZ = Mth.floor(camera.getZ()) >> 4;
            CameraUtils.markChunksForRebuildOnDeactivation(chunkX, chunkZ);
        }

        originalCameraEntity = null;
        originalCameraWasPlayer = false;
        camera = null;
        controller = null;
        cameraMotion = Vec3.ZERO;
        sprinting = false;
    }
}
