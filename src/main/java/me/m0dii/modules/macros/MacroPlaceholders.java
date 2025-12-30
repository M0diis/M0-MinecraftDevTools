package me.m0dii.modules.macros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MacroPlaceholders {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");
    private static final Random RAND = new Random();

    private MacroPlaceholders() {
    }

    public static String expand(MinecraftClient client, String input) {
        if (input == null || input.isEmpty() || client == null || client.player == null) {
            return input;
        }

        PlayerEntity p = client.player;
        var world = client.world;
        HitResult hit = client.crosshairTarget;
        BlockPos lookBlock = null;
        Entity lookEntity = null;
        if (hit instanceof BlockHitResult bhr) {
            lookBlock = bhr.getBlockPos();
        } else if (hit instanceof EntityHitResult ehr) {
            lookEntity = ehr.getEntity();
        }

        Matcher m = PLACEHOLDER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(1).trim();
            String replacement = resolveToken(token, p, world != null ? world.getRegistryKey().getValue() : null, lookBlock, lookEntity);
            if (replacement == null) {
                replacement = "";
            }
            // Escape backslashes and dollars for regex append
            replacement = Matcher.quoteReplacement(replacement);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveToken(String token, PlayerEntity p, Identifier dimId, BlockPos lookBlock, Entity lookEntity) {
        // Functions with args: rand.int(a,b)
        if (token.startsWith("rand.int(") && token.endsWith(")")) {
            String inside = token.substring("rand.int(".length(), token.length() - 1);
            String[] parts = inside.split(",");
            try {
                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());
                if (a > b) {
                    int t = a;
                    a = b;
                    b = t;
                }
                int v = a + RAND.nextInt(b - a + 1);
                return Integer.toString(v);
            } catch (Exception ignored) {
                // ignore malformed args
            }
            return "0";
        }

        switch (token) {
            case "sel.self" -> {
                return "@s";
            }
            case "sel.nearest" -> {
                return "@p";
            }
            case "sel.random" -> {
                return "@r";
            }
            case "sel.all" -> {
                return "@a";
            }
            case "sel.entities" -> {
                return "@e";
            }
            default -> {
                // no-op
            }
        }

        // Player info
        switch (token) {
            case "player.name" -> {
                return p.getGameProfile().getName();
            }
            case "player.uuid" -> {
                return p.getUuidAsString();
            }
            case "hp" -> {
                return Integer.toString((int) Math.floor(p.getHealth()));
            }
            case "max_hp" -> {
                return Integer.toString((int) Math.floor(p.getMaxHealth()));
            }
            case "food" -> {
                return Integer.toString(p.getHungerManager().getFoodLevel());
            }
            case "xp" -> {
                return Integer.toString(p.totalExperience);
            }
            case "level" -> {
                return Integer.toString(p.experienceLevel);
            }
            case "saturation" -> {
                return Integer.toString((int) p.getHungerManager().getSaturationLevel());
            }
            case "yaw" -> {
                return String.format("%.1f", p.getYaw());
            }
            case "pitch" -> {
                return String.format("%.1f", p.getPitch());
            }
            case "player.gamemode" -> {
                // Provide a simple gamemode placeholder for client-side use.
                // Prefer spectator check, then creative. Default to "survival" when unknown.
                try {
                    if (p.isSpectator()) {
                        return "spectator";
                    }
                    var abilities = p.getAbilities();
                    if (abilities != null && abilities.creativeMode) {
                        return "creative";
                    }
                } catch (Exception ignored) {
                    // defensive: fall back to survival
                }
                return "survival";
            }
            default -> {
                // no-op
            }
        }

        switch (token) {
            case "pos.x" -> {
                return Integer.toString(p.getBlockX());
            }
            case "pos.y" -> {
                return Integer.toString(p.getBlockY());
            }
            case "pos.z" -> {
                return Integer.toString(p.getBlockZ());
            }
            case "pos.xyz" -> {
                return p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
            }
            case "pos.xf" -> {
                return formatDouble(p.getX());
            }
            case "pos.yf" -> {
                return formatDouble(p.getY());
            }
            case "pos.zf" -> {
                return formatDouble(p.getZ());
            }
            case "pos.biome" -> {
                if (p.getWorld() != null) {
                    RegistryEntry<Biome> biomeRegistryEntry = p.getWorld().getBiome(p.getBlockPos());

                    if (biomeRegistryEntry != null) {
                        RegistryKey<Biome> biomeKey = biomeRegistryEntry.getKey().orElse(null);
                        if (biomeKey != null) {
                            Identifier biomeId = biomeKey.getValue();
                            if (biomeId != null) {
                                return biomeId.toString();
                            }
                        }
                    }
                }
                return "";
            }
            case "pos.dim" -> {
                if (p.getWorld() != null) {
                    RegistryKey<?> dimKey = p.getWorld().getRegistryKey();
                    if (dimKey != null) {
                        Identifier dimIdentifier = dimKey.getValue();
                        if (dimIdentifier != null) {
                            return dimIdentifier.toString();
                        }
                    }
                }
                return "";
            }
            case "pos.light" -> {
                if (p.getWorld() != null) {
                    return Integer.toString(p.getWorld().getLightLevel(p.getBlockPos()));
                }
                return "0";
            }
            case "pos.facing" -> {
                return p.getHorizontalFacing().getName();
            }
            default -> {
                // no-op
            }
        }

        if ("dim".equals(token)) {
            return dimId != null ? dimId.toString() : "";
        }

        if (lookBlock != null) {
            switch (token) {
                case "look.block.x" -> {
                    return Integer.toString(lookBlock.getX());
                }
                case "look.block.y" -> {
                    return Integer.toString(lookBlock.getY());
                }
                case "look.block.z" -> {
                    return Integer.toString(lookBlock.getZ());
                }
                case "look.block.xyz" -> {
                    return lookBlock.getX() + " " + lookBlock.getY() + " " + lookBlock.getZ();
                }
                default -> {
                    // no-op
                }
            }
        }

        if (lookEntity != null) {
            switch (token) {
                case "look.entity.name" -> {
                    return lookEntity.getName().getString();
                }
                case "look.entity.uuid" -> {
                    return lookEntity.getUuidAsString();
                }
                case "look.entity.id" -> {
                    return Integer.toString(lookEntity.getId());
                }
                default -> {
                    // no-op
                }
            }
        }

        if (token.startsWith("look.dir")) {
            Vec3d vec = p.getRotationVec(1.0f).normalize();
            switch (token) {
                case "look.dir.x" -> {
                    return formatDouble(vec.x);
                }
                case "look.dir.y" -> {
                    return formatDouble(vec.y);
                }
                case "look.dir.z" -> {
                    return formatDouble(vec.z);
                }
                default -> {
                    // no-op
                }
            }
        }

        if (token.startsWith("hand")) {
            ItemStack hand = p.getMainHandStack();

            switch (token) {
                case "hand.item" -> {
                    return hand.getName().getString();
                }
                case "hand.id" -> {
                    // getTranslationKey is @VisibleForTesting in mappings; use a safe fallback
                    return hand.getItem().toString(); // e.g. "minecraft:diamond_sword"
                }
                case "hand.count" -> {
                    return Integer.toString(hand.getCount());
                }
                case "hand.damage" -> {
                    return Integer.toString(hand.getDamage());
                }
                case "hand.max_damage" -> {
                    return Integer.toString(hand.getMaxDamage());
                }
                case "hand.durability" -> {
                    int dmg = hand.getDamage();
                    int max = hand.getMaxDamage();
                    if (max <= 0) {
                        return "0";
                    }
                    return Integer.toString(max - dmg);
                }
                default -> {
                    // no-op
                }
            }
        }

        return null;
    }

    private static String formatDouble(double d) {
        return String.format(java.util.Locale.ROOT, "%.3f", d);
    }
}

