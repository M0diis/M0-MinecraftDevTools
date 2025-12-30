package me.m0dii.nbteditor.tagreferences.specific.data;

import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.List;
import java.util.Optional;

public record CustomPotionContents(Optional<Integer> color, List<StatusEffectInstance> effects) {

}
