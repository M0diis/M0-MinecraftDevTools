package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.utils.ReflectionUtils;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MacroWorkbenchEntityModelRenderer {
    private MacroWorkbenchEntityModelRenderer() {
    }

    public static void draw(DrawContext context,
                            MinecraftClient client,
                            MacroHudDataHandler.HudElement element,
                            int x,
                            int y,
                            int w,
                            int h) {
        if (client == null || element == null) {
            return;
        }
        net.minecraft.entity.Entity target = resolveCanvasModelTargetEntity(client, element);
        if (target == null) {
            return;
        }
        int safeW = Math.max(1, w);
        int safeH = Math.max(1, h);
        int innerPad = element.drawBorder ? 2 : 1;
        int boxW = Math.max(1, safeW - (innerPad * 2));
        int boxH = Math.max(1, safeH - (innerPad * 2));
        int baseSize = element.modelAutoFit
                ? Math.max(8, Math.round(Math.min(boxW, boxH) * 0.48f))
                : Math.max(8, Math.min(safeW, safeH) - 4);
        int size = Math.max(8, Math.round(baseSize * Math.clamp(element.modelZoom, 0.2f, 2.5f)));
        int left = x + innerPad + element.modelOffsetX;
        int top = y + innerPad + element.modelOffsetY;
        int right = x + safeW - innerPad + element.modelOffsetX;
        int bottom = y + safeH - innerPad + element.modelOffsetY;
        if (right <= left) {
            right = left + 1;
        }
        if (bottom <= top) {
            bottom = top + 1;
        }
        drawEntityModelReflective(context, target, left, top, right, bottom, size,
                element.modelYaw, element.modelPitch, element.modelFollowLook);
    }

    private static net.minecraft.entity.Entity resolveCanvasModelTargetEntity(MinecraftClient client,
                                                                              MacroHudDataHandler.HudElement element) {
        String id = StringUtils.safe(element == null ? null : element.iconId).toLowerCase(Locale.ROOT);
        if (id.isBlank() || "player".equals(id) || "minecraft:player".equals(id)) {
            if (client.player != null) {
                return client.player;
            }
            net.minecraft.entity.Entity camera = client.getCameraEntity();
            return camera instanceof LivingEntity ? camera : null;
        }
        if (client.world != null && !id.contains(":")) {
            for (var player : client.world.getPlayers()) {
                if (player != null && player.getName().getString().equalsIgnoreCase(id)) {
                    return player;
                }
            }
        }
        if (client.player != null) {
            return client.player;
        }
        net.minecraft.entity.Entity camera = client.getCameraEntity();
        return camera instanceof LivingEntity ? camera : null;
    }

    private static void drawEntityModelReflective(DrawContext context,
                                                  net.minecraft.entity.Entity entity,
                                                  int left,
                                                  int top,
                                                  int right,
                                                  int bottom,
                                                  int size,
                                                  float yaw,
                                                  float pitch,
                                                  boolean followLook) {
        if (entity == null || size < 1) {
            return;
        }
        try {
            float[] resolvedAngles = resolveModelAngles(entity, yaw, pitch, followLook);
            float resolvedYaw = resolvedAngles[0];
            float resolvedPitch = resolvedAngles[1];

            if (drawEntityModelDirect(context, entity, left, top, right, bottom, size, resolvedYaw, resolvedPitch)) {
                return;
            }

            Class<?> inventoryScreen = Class.forName("net.minecraft.client.gui.screen.ingame.InventoryScreen");
            Class<?> drawContextClass = Class.forName("net.minecraft.client.gui.DrawContext");
            Class<?> entityClass = Class.forName("net.minecraft.entity.Entity");
            Class<?> vectorClass = Class.forName("org.joml.Vector3f");
            Class<?> quaternionClass = Class.forName("org.joml.Quaternionf");
            Class<?> livingEntityClass = Class.forName("net.minecraft.entity.LivingEntity");

            Object vecZero = vectorClass.getConstructor(float.class, float.class, float.class).newInstance(0.0f, 0.0f, 0.0f);
            Object modelQuat = buildModelQuaternion(quaternionClass, resolvedPitch);
            Object identityQuat = quaternionClass.getConstructor().newInstance();

            if (invokePreferredEntityDrawPreview(inventoryScreen, context, entity,
                    left, top, right, bottom, size,
                    vecZero, modelQuat, identityQuat,
                    drawContextClass, vectorClass, quaternionClass, entityClass, livingEntityClass,
                    resolvedYaw, resolvedPitch)) {
                return;
            }

            List<Class<?>> owners = List.of(inventoryScreen, drawContextClass);
            for (Class<?> owner : owners) {
                for (Method method : owner.getMethods()) {
                    if (!"drawEntity".equals(method.getName())) {
                        continue;
                    }
                    boolean staticMethod = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                    if (!staticMethod && !drawContextClass.isAssignableFrom(owner)) {
                        continue;
                    }

                    Class<?>[] paramTypes = method.getParameterTypes();
                    int intParams = 0;
                    for (Class<?> type : paramTypes) {
                        if (type == int.class || type == Integer.class) {
                            intParams++;
                        }
                    }
                    if (intParams < 3) {
                        continue;
                    }

                    int[] intArgValues = buildEntityDrawIntArgs(left, top, right, bottom, size, intParams);
                    if (intArgValues == null) {
                        continue;
                    }

                    float fallbackCx = (intArgValues.length >= 2)
                            ? (intArgValues[0] + (intArgValues.length >= 4 ? intArgValues[2] : intArgValues[0])) / 2.0f
                            : 0.0f;
                    float fallbackCy = (intArgValues.length >= 2)
                            ? (intArgValues[1] + (intArgValues.length >= 4 ? intArgValues[3] : intArgValues[1])) / 2.0f
                            : 0.0f;
                    float mouseX = fallbackCx - 40.0f * (float) Math.tan(Math.clamp(resolvedYaw, -30f, 30f) / 20.0f);
                    float mouseY = fallbackCy + 40.0f * (float) Math.tan(Math.clamp(resolvedPitch, -30f, 30f) / 20.0f);

                    Object[] args = new Object[paramTypes.length];
                    int intArg = 0;
                    int quatArg = 0;
                    int floatArg = 0;
                    boolean accepted = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> type = paramTypes[i];
                        if (drawContextClass.isAssignableFrom(type)) {
                            args[i] = context;
                        } else if (type == int.class || type == Integer.class) {
                            args[i] = intArgValues[Math.min(intArg++, intArgValues.length - 1)];
                        } else if (type == float.class || type == Float.class) {
                            switch (floatArg++) {
                                case 0 -> args[i] = 0.0625f;
                                case 1 -> args[i] = mouseX;
                                case 2 -> args[i] = mouseY;
                                default -> args[i] = 0.0f;
                            }
                        } else if (type == double.class || type == Double.class) {
                            args[i] = 0.0d;
                        } else if (type == boolean.class || type == Boolean.class) {
                            args[i] = false;
                        } else if (type.getName().equals("org.joml.Vector3f")) {
                            args[i] = vecZero;
                        } else if (type.getName().equals("org.joml.Quaternionf")) {
                            args[i] = (quatArg++ == 0) ? modelQuat : identityQuat;
                        } else if (entityClass.isAssignableFrom(type)) {
                            args[i] = entity;
                        } else {
                            accepted = false;
                            break;
                        }
                    }
                    if (!accepted) {
                        continue;
                    }
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(staticMethod ? null : context, args);
                        return;
                    } catch (Throwable ignoredInvokeFailure) {
                        // Keep trying compatible signatures.
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }
            }
        } catch (Exception ignored) {
            // Graceful fallback when mapped signatures differ across versions.
        }
    }

    private static boolean drawEntityModelDirect(DrawContext context,
                                                 net.minecraft.entity.Entity entity,
                                                 int left,
                                                 int top,
                                                 int right,
                                                 int bottom,
                                                 int size,
                                                 float resolvedYaw,
                                                 float resolvedPitch) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        try {
            float cx = (left + right) / 2.0f;
            float cy = (top + bottom) / 2.0f;
            float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
            float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
            float mx = cx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
            float my = cy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
            InventoryScreen.drawEntity(context, left, top, right, bottom, size, 0.0625f, mx, my, living);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokePreferredEntityDrawPreview(Class<?> inventoryScreen,
                                                            DrawContext context,
                                                            net.minecraft.entity.Entity entity,
                                                            int left,
                                                            int top,
                                                            int right,
                                                            int bottom,
                                                            int size,
                                                            Object vecZero,
                                                            Object modelQuat,
                                                            Object identityQuat,
                                                            Class<?> drawContextClass,
                                                            Class<?> vectorClass,
                                                            Class<?> quaternionClass,
                                                            Class<?> entityClass,
                                                            Class<?> livingEntityClass,
                                                            float resolvedYaw,
                                                            float resolvedPitch) {
        int cx = left + ((right - left) / 2);
        int cy = bottom;

        List<Method> candidates = new ArrayList<>();
        for (Method m : inventoryScreen.getMethods()) {
            if ("drawEntity".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                candidates.add(m);
            }
        }

        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesFloatXYSizeVecQuatSignature(p, drawContextClass, vectorClass, quaternionClass, livingEntityClass)) {
                continue;
            }
            if (!p[7].isInstance(entity)) {
                continue;
            }
            try {
                float entityScale = (entity instanceof LivingEntity le) ? le.getScale() : 1.0f;
                float q = (float) size / entityScale;
                float mcYaw = 180.0f + resolvedYaw;
                float pitchRad = (float) (-resolvedPitch * Math.PI / 180.0);
                Object uiQuat = quaternionClass.getConstructor().newInstance();
                Method rotZ = quaternionClass.getMethod("rotateZ", float.class);
                Method rotX = quaternionClass.getMethod("rotateX", float.class);
                rotZ.invoke(uiQuat, (float) Math.PI);
                rotX.invoke(uiQuat, pitchRad);
                Object lightQuat = quaternionClass.getConstructor().newInstance();
                rotX.invoke(lightQuat, pitchRad);
                Object vec = vectorClass.getConstructor(float.class, float.class, float.class)
                        .newInstance(0.0f, entity.getHeight() / 2.0f + 0.0625f * entityScale, 0.0f);
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                applyEntityOrientationForScreen(entity, mcYaw, resolvedPitch);
                try {
                    method.invoke(null, context, fcx, fcy, q, vec, uiQuat, lightQuat, entity);
                    return true;
                } finally {
                    restoreEntityOrientation(entity, snapshot);
                }
            } catch (Throwable ignored) {
            }
        }

        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesRectMouseFloat3Signature(p, drawContextClass, livingEntityClass)) {
                continue;
            }
            if (!p[9].isInstance(entity)) {
                continue;
            }
            try {
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                float mx = fcx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                float my = fcy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                method.invoke(null, context, left, top, right, bottom, size, 0.0625f, mx, my, entity);
                return true;
            } catch (Throwable ignored) {
            }
        }

        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            try {
                if (matchesRectQuatSignature(p, drawContextClass, vectorClass, quaternionClass)
                        && p[9].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, left, top, right, bottom, size, vecZero, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                if (matchesCenterQuatSignature(p, drawContextClass, quaternionClass, entityClass)
                        && p[6].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, cx, cy, size, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                if (matchesMouseFloatSignature(p, drawContextClass, entityClass)
                        && p[6].isInstance(entity)) {
                    float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                    float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                    float mx = cx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                    float my = cy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                    method.invoke(null, context, cx, cy, size, mx, my, entity);
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean matchesRectMouseFloat3Signature(Class<?>[] p, Class<?> dcClass, Class<?> livingEntityClass) {
        return p.length == 10
                && dcClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == int.class && p[5] == int.class
                && p[6] == float.class && p[7] == float.class && p[8] == float.class
                && livingEntityClass.isAssignableFrom(p[9]);
    }

    private static boolean matchesFloatXYSizeVecQuatSignature(Class<?>[] p,
                                                              Class<?> dcClass,
                                                              Class<?> vectorClass,
                                                              Class<?> quaternionClass,
                                                              Class<?> livingEntityClass) {
        return p.length == 8
                && dcClass.isAssignableFrom(p[0])
                && p[1] == float.class && p[2] == float.class && p[3] == float.class
                && vectorClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && (quaternionClass.getName().equals(p[6].getName()) || !p[6].isPrimitive())
                && livingEntityClass.isAssignableFrom(p[7]);
    }

    private static boolean matchesRectQuatSignature(Class<?>[] p,
                                                    Class<?> drawContextClass,
                                                    Class<?> vectorClass,
                                                    Class<?> quaternionClass) {
        return p.length == 10
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class && p[5] == int.class
                && vectorClass.getName().equals(p[6].getName())
                && quaternionClass.getName().equals(p[7].getName())
                && quaternionClass.getName().equals(p[8].getName());
    }

    private static boolean matchesCenterQuatSignature(Class<?>[] p,
                                                      Class<?> drawContextClass,
                                                      Class<?> quaternionClass,
                                                      Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && quaternionClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && entityClass.isAssignableFrom(p[6]);
    }

    private static boolean matchesMouseFloatSignature(Class<?>[] p,
                                                      Class<?> drawContextClass,
                                                      Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == float.class && p[5] == float.class
                && entityClass.isAssignableFrom(p[6]);
    }

    private static EntityOrientationSnapshot captureEntityOrientation(net.minecraft.entity.Entity entity) {
        if (entity == null) {
            return new EntityOrientationSnapshot(0.0f, 0.0f, null, null, null);
        }
        Float bodyYaw = null;
        Float headYaw = null;
        Float prevHeadYaw = null;
        if (entity instanceof LivingEntity living) {
            bodyYaw = living.getBodyYaw();
            headYaw = living.getHeadYaw();
            prevHeadYaw = ReflectionUtils.getFloatField(living, LivingEntity.class, "prevHeadYaw");
        }
        return new EntityOrientationSnapshot(entity.getYaw(), entity.getPitch(), bodyYaw, headYaw, prevHeadYaw);
    }

    private static void applyEntityOrientationForScreen(net.minecraft.entity.Entity entity, float displayYaw, float pitch) {
        if (entity == null) {
            return;
        }
        entity.setYaw(displayYaw);
        entity.setPitch(pitch);
        if (entity instanceof LivingEntity living) {
            living.setBodyYaw(displayYaw);
            living.setHeadYaw(displayYaw);
            ReflectionUtils.setFloatField(living, LivingEntity.class, "prevHeadYaw", displayYaw);
        }
    }

    private static void restoreEntityOrientation(net.minecraft.entity.Entity entity, EntityOrientationSnapshot snapshot) {
        if (entity == null || snapshot == null) {
            return;
        }
        entity.setYaw(snapshot.yaw());
        entity.setPitch(snapshot.pitch());
        if (entity instanceof LivingEntity living) {
            if (snapshot.bodyYaw() != null) {
                living.setBodyYaw(snapshot.bodyYaw());
            }
            if (snapshot.headYaw() != null) {
                living.setHeadYaw(snapshot.headYaw());
            }
            if (snapshot.prevHeadYaw() != null) {
                ReflectionUtils.setFloatField(living, LivingEntity.class, "prevHeadYaw", snapshot.prevHeadYaw());
            }
        }
    }

    private record EntityOrientationSnapshot(float yaw, float pitch, Float bodyYaw, Float headYaw, Float prevHeadYaw) {
    }

    private static float[] resolveModelAngles(net.minecraft.entity.Entity entity, float yawOffset, float pitchOffset, boolean followLook) {
        float baseYaw = 0.0f;
        float basePitch = 0.0f;
        if (followLook && entity != null) {
            baseYaw = entity instanceof LivingEntity living ? living.getHeadYaw() : entity.getYaw();
            basePitch = entity.getPitch();
        }
        float resolvedYaw = wrapDegrees(baseYaw + yawOffset);
        float resolvedPitch = Math.clamp(basePitch + pitchOffset, -90.0f, 90.0f);
        return new float[]{resolvedYaw, resolvedPitch};
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private static Object buildModelQuaternion(Class<?> quaternionClass, float pitch) {
        try {
            Object quat = quaternionClass.getConstructor().newInstance();
            try {
                Method rotateZ = quaternionClass.getMethod("rotateZ", float.class);
                rotateZ.invoke(quat, (float) Math.PI);
            } catch (Exception ignored) {
            }
            float pitchRad = (float) (-pitch * Math.PI / 180.0);
            try {
                Method rotateX = quaternionClass.getMethod("rotateX", float.class);
                rotateX.invoke(quat, pitchRad);
            } catch (Exception ignored) {
            }
            return quat;
        } catch (Exception ignored) {
            try {
                return quaternionClass.getConstructor().newInstance();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private static int[] buildEntityDrawIntArgs(int left, int top, int right, int bottom, int size, int intParams) {
        int safeSize = Math.max(8, size);
        int safeLeft = left;
        int safeTop = top;
        int safeRight = Math.max(right, safeLeft + 1);
        int safeBottom = Math.max(bottom, safeTop + 1);
        if (intParams == 3) {
            int cx = safeLeft + ((safeRight - safeLeft) / 2);
            int cy = safeBottom;
            return new int[]{cx, cy, safeSize};
        }
        if (intParams >= 5) {
            int[] args = new int[intParams];
            args[0] = safeLeft;
            args[1] = safeTop;
            args[2] = safeRight;
            args[3] = safeBottom;
            args[4] = safeSize;
            for (int i = 5; i < intParams; i++) {
                args[i] = safeSize;
            }
            return args;
        }
        return null;
    }
}
