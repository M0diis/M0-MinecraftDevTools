package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.ResetableDataTracker;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DataTracker.class)
public class DataTrackerMixin implements ResetableDataTracker {

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private DataTracker.Entry<?>[] entries;

    @Override
    public void reset() {
        for (DataTracker.Entry<?> entry : this.entries) {
            resetEntry(entry);
            entry.setDirty(true);
        }
        dirty = true;
    }

    private <T> void resetEntry(DataTracker.Entry<T> entry) {
        entry.set(entry.initialValue);
    }
}
