package me.m0dii.nbteditor.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes", "java:S3011", "java:S112"})
public class Reflection {

    private Reflection() {
    }

    public static final MappingResolver mappings = FabricLoader.getInstance().getMappingResolver();

    private static final Cache<String, Class<?>> CLASS_CACHE = CacheBuilder.newBuilder().build();
    private static final String INTERMEDIARY = "intermediary";

    public static Class<?> getClass(String name) {
        try {
            return CLASS_CACHE.get(name, () -> Class.forName(mappings.mapClassName(INTERMEDIARY, name)));
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw new RuntimeException("Error getting class", e);
        }
    }

    private static String getFieldName(Class<?> clazz, String field, String descriptor) {
        return mappings.mapFieldName(INTERMEDIARY, mappings.unmapClassName(INTERMEDIARY, clazz.getName()), field, descriptor);
    }

    public static FieldReference getField(Class<?> clazz, String field, String descriptor) {
        try {
            Field fieldObj = clazz.getDeclaredField(getFieldName(clazz, field, descriptor));
            fieldObj.setAccessible(true);
            return new FieldReference(fieldObj);
        } catch (Exception e) {
            throw new RuntimeException("Error getting field", e);
        }
    }

    private static String getIntermediaryDescriptor(MethodType type) {
        StringBuilder output = new StringBuilder("(");
        for (Class<?> param : type.parameterArray()) {
            output.append(getIntermediaryDescriptor(param));
        }
        output.append(")");
        output.append(getIntermediaryDescriptor(type.returnType()));
        return output.toString();
    }

    private static String getIntermediaryDescriptor(Class<?> clazz) {
        String descriptor = clazz.descriptorString();
        StringBuilder arrays = new StringBuilder();
        int typeStart = 0;
        while (descriptor.charAt(typeStart) == '[') {
            arrays.append('[');
            clazz = clazz.componentType();
            typeStart++;
        }
        if (descriptor.charAt(typeStart) == 'L') {
            return arrays + "L" + mappings.unmapClassName(INTERMEDIARY, clazz.getName()).replace('.', '/') + ";";
        } else {
            return descriptor;
        }
    }

    public static String getMethodName(Class<?> clazz, String method, MethodType type) {
        return mappings.mapMethodName(INTERMEDIARY, mappings.unmapClassName(INTERMEDIARY, clazz.getName()), method, getIntermediaryDescriptor(type));
    }

    public static MethodInvoker getMethod(Class<?> clazz, String method, MethodType type) {
        try {
            return new MethodInvoker(clazz, getMethodName(clazz, method, type), type);
        } catch (Exception e) {
            throw new RuntimeException("Error getting method", e);
        }
    }

    public static Supplier<MethodInvoker> getOptionalMethod(Supplier<Class<?>> clazz, Supplier<String> method, Supplier<MethodType> type) {
        return jitSupplier(() -> getMethod(clazz.get(), method.get(), type.get()));
    }

    public static Supplier<MethodInvoker> getOptionalMethod(Class<?> clazz, String method, MethodType type) {
        return getOptionalMethod(() -> clazz, () -> method, () -> type);
    }

    private static <T> Supplier<T> jitSupplier(Supplier<T> supplier) {
        return new Supplier<>() {
            private T value;

            @Override
            public T get() {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }

    public static class FieldReference {
        private final Field field;

        public FieldReference(Field field) {
            this.field = field;
        }

        public void set(Object obj, Object value) {
            try {
                field.set(obj, value);
            } catch (Exception e) {
                throw new RuntimeException("Error setting field", e);
            }
        }

        public <T> T get(Object obj) {
            try {
                return (T) field.get(obj);
            } catch (Exception e) {
                throw new RuntimeException("Error getting field value", e);
            }
        }
    }

    public static class MethodInvoker {
        private final Method method;

        public MethodInvoker(Class<?> clazz, String method, MethodType type) throws Exception {
            this.method = clazz.getDeclaredMethod(method, type.parameterArray());
            this.method.setAccessible(true);
            if (!type.returnType().isAssignableFrom(this.method.getReturnType())) {
                throw new NoSuchMethodException("Mismatched return types! Expected " + type.returnType().getName() +
                        " but found " + this.method.getReturnType().getName());
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T invoke(Object obj, Object... args) {
            try {
                return (T) method.invoke(obj, args);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking method", e);
            }
        }

    }

}
