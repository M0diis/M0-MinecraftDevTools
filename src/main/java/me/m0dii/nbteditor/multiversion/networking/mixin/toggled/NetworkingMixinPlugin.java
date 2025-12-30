package me.m0dii.nbteditor.multiversion.networking.mixin.toggled;

import me.m0dii.nbteditor.misc.BasicMixinPlugin;

import java.util.List;

public class NetworkingMixinPlugin extends BasicMixinPlugin {

    @Override
    public void addMixins(List<String> output) {
        output.add("toggled." + CustomPayload1Mixin.class.getSimpleName());
        output.add("toggled." + ClientConnectionMixin_1_20_5.class.getSimpleName());
    }

}
