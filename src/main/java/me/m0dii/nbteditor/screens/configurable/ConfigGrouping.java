package me.m0dii.nbteditor.screens.configurable;

import lombok.Getter;
import me.m0dii.nbteditor.util.OrderedMap;
import net.minecraft.text.Text;

import java.util.*;

public abstract class ConfigGrouping<K, T extends ConfigGrouping<K, T>> implements ConfigPathNamed {

    @Getter
    protected final Text name;
    protected final OrderedMap<K, ConfigPath> paths;
    protected final List<ConfigValueListener<ConfigValue<?, ?>>> onChanged;
    private final Constructor<K, T> cloneImpl;

    protected Text namePrefix;

    protected ConfigGrouping(Text name, Constructor<K, T> cloneImpl) {
        this.name = name;
        this.paths = new OrderedMap<>();
        this.cloneImpl = cloneImpl;
        this.onChanged = new ArrayList<>();
    }

    @Override
    public Text getNamePrefix() {
        return namePrefix;
    }

    @Override
    public void setNamePrefix(Text prefix) {
        namePrefix = prefix;
    }

    @SuppressWarnings("unchecked")
    public T setConfigurable(K key, ConfigPath path) {
        paths.put(key, path);
        path.addValueListener(source -> onChanged.forEach(listener -> listener.onValueChanged(source)));
        path.setParent(this);
        return (T) this;
    }

    public ConfigPath getConfigurable(K key) {
        return paths.get(key);
    }

    public Map<K, ConfigPath> getConfigurables() {
        return Collections.unmodifiableMap(paths);
    }

    @SuppressWarnings("unchecked")
    public T sort(Comparator<K> sorter) {
        paths.sort(sorter);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setSorter(Comparator<K> sorter) {
        paths.setSorter(sorter);
        return (T) this;
    }

    @Override
    public boolean isValueValid() {
        return paths.values().stream().allMatch(ConfigPath::isValueValid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener) {
        onChanged.add(listener);
        return (T) this;
    }

    @Override
    public T clone(boolean defaults) {
        T output = cloneImpl.newInstance(name);
        paths.forEach((key, path) -> output.setConfigurable(key, path.clone(defaults)));
        output.onChanged.addAll(onChanged);
        return output;
    }

    // Make sure subclasses offset the mouse properly
    @Override
    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);

    @Override
    public abstract boolean mouseReleased(double mouseX, double mouseY, int button);

    @Override
    public abstract void mouseMoved(double mouseX, double mouseY);

    @Override
    public abstract boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);

    @Override
    public abstract boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount);

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ConfigPath path : new ArrayList<>(paths.values())) {
            if (path.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (ConfigPath path : new ArrayList<>(paths.values())) {
            if (path.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ConfigPath path : new ArrayList<>(paths.values())) {
            if (path.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        for (ConfigPath path : new ArrayList<>(paths.values())) {
            path.tick();
        }
    }

    protected interface Constructor<K, T extends ConfigGrouping<K, T>> {
        T newInstance(Text name);
    }
}
