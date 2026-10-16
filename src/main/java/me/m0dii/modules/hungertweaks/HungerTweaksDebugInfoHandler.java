package me.m0dii.modules.hungertweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.food.FoodData;

import java.text.DecimalFormat;
import java.util.List;

public class HungerTweaksDebugInfoHandler {
    public static HungerTweaksDebugInfoHandler INSTANCE;

    private static final DecimalFormat SATURATION_DF = new DecimalFormat("#.##");
    private static final DecimalFormat EXHAUSTION_VALUE_DF = new DecimalFormat("0.00");
    private static final DecimalFormat EXHAUSTION_MAX_DF = new DecimalFormat("#.##");

    public static void init() {
        INSTANCE = new HungerTweaksDebugInfoHandler();
    }

    public void onTextRender(List<String> leftDebugInfo) {
        if (leftDebugInfo == null || !HungerTweaksModule.INSTANCE.showFoodDebugInfo()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        FoodData stats = client.player.getFoodData();
        leftDebugInfo.add("hunger: " + stats.getFoodLevel()
                + ", sat: " + SATURATION_DF.format(stats.getSaturationLevel())
                + ", exh: " + EXHAUSTION_VALUE_DF.format(HungerTweaksFoodHelper.getExhaustion(stats))
                + "/" + EXHAUSTION_MAX_DF.format(HungerTweaksFoodHelper.MAX_EXHAUSTION));
    }
}
