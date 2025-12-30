package me.m0dii.nbteditor.screens.configurable;

public interface ConfigValue<T, V extends ConfigValue<T, V>> extends Configurable<V> {
    T getDefaultValue();

    T getValue();

    void setValue(T value);

    default T getValidValue() {
        return isValueValid() ? getValue() : getDefaultValue();
    }

    V addValueListener(ConfigValueListener<V> listener);
}
