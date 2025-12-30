package me.m0dii.nbteditor.tagreferences;

import com.mojang.authlib.GameProfile;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.tagreferences.general.NBTComponentTagReference;
import me.m0dii.nbteditor.tagreferences.general.TagReference;
import net.minecraft.component.type.ProfileComponent;

import java.util.Optional;

public class BlockTagReferences {

    private BlockTagReferences() {
    }

    public static final TagReference<Optional<GameProfile>, LocalBlock> PROFILE = TagReference.forLocalNBT(Optional::empty,
            new NBTComponentTagReference<>("profile", ProfileComponent.CODEC, Optional::empty,
                    profile -> Optional.of(profile.gameProfile()),
                    profile -> profile.map(ProfileComponent::new).orElse(null)));

}
