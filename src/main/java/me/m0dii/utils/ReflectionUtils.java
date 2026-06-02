package me.m0dii.utils;

import net.minecraft.client.option.KeyBinding;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static List<Field> declaredFieldsInHierarchy(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Collections.addAll(fields, c.getDeclaredFields());
        }
        return fields;
    }

    public static Float getFloatField(Object obj, Class<?> clazz, String fieldName) {
        if (obj == null || clazz == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getFloat(obj);
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
        return null;
    }

    public static boolean setFloatField(Object obj, Class<?> clazz, String fieldName, float value) {
        if (obj == null || clazz == null || fieldName == null || fieldName.isBlank()) {
            return false;
        }
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.setFloat(obj, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
        return false;
    }

    public static String getGameKeybindDisplayName(KeyBinding kb, Object options) {
        String action = reflectActionName(kb);
        if ((action == null || action.isBlank()) && options != null) {
            action = deriveActionFromOptionsField(kb, options);
        }
        if (action == null || action.isBlank()) {
            return kb.getBoundKeyLocalizedText().getString();
        }
        return action;
    }

    private static String deriveActionFromOptionsField(KeyBinding kb, Object options) {
        for (Field f : declaredFieldsInHierarchy(options.getClass())) {
            if (!KeyBinding.class.isAssignableFrom(f.getType())) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object value = f.get(options);
                if (value == kb) {
                    return humanizeFieldName(f.getName());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String humanizeFieldName(String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        String raw = field.endsWith("Key") ? field.substring(0, field.length() - 3) : field;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                out.append(' ');
            }
            out.append(ch);
        }
        if (out.isEmpty()) {
            return null;
        }
        out.setCharAt(0, Character.toUpperCase(out.charAt(0)));
        return out.toString();
    }

    private static String reflectActionName(KeyBinding kb) {
        String action = extractActionKeyFromMethods(kb);
        if (action != null) {
            return humanizeTranslation(action);
        }

        action = extractActionKeyFromFields(kb);
        if (action != null) {
            return humanizeTranslation(action);
        }

        String fallback = kb.toString();
        if (fallback != null) {
            int idx = fallback.indexOf("key.");
            if (idx >= 0) {
                String candidate = fallback.substring(idx).split("[^a-zA-Z0-9_.]", 2)[0];
                if (isActionTranslationKey(candidate)) {
                    return humanizeTranslation(candidate);
                }
            }
        }

        return null;
    }

    private static String extractActionKeyFromMethods(KeyBinding kb) {
        try {
            Method m = kb.getClass().getMethod("getTranslationKey");
            Object value = m.invoke(kb);
            if (value instanceof String s && isActionTranslationKey(s)) {
                return s;
            }
        } catch (Exception ignored) {
        }

        for (Method m : kb.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != String.class) {
                continue;
            }
            try {
                Object value = m.invoke(kb);
                if (value instanceof String s && isActionTranslationKey(s)) {
                    return s;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String extractActionKeyFromFields(KeyBinding kb) {
        for (Field f : declaredFieldsInHierarchy(kb.getClass())) {
            if (f.getType() != String.class) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object value = f.get(kb);
                if (value instanceof String s && isActionTranslationKey(s)) {
                    return s;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean isActionTranslationKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (!key.startsWith("key.")) {
            return false;
        }
        return !key.startsWith("key.keyboard.") && !key.startsWith("key.mouse.");
    }

    private static String humanizeTranslation(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.split("\\.");
        String tail = parts.length == 0 ? key : parts[parts.length - 1];
        String spaced = tail.replace('_', ' ');
        if (spaced.isBlank()) {
            return key;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
