package me.m0dii.modules.blockstatecycler;

import me.m0dii.modules.Module;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.packets.SetBlockC2SPacket;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.BlockStateProperties;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.List;

public class BlockStateCycler extends Module {

    public static final BlockStateCycler INSTANCE = new BlockStateCycler();

    private static int currentPropertyIndex = 0;

    private BlockStateCycler() {
        super("block_state_cycler", "Block State Cycler", true);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.cycle_block_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_DECIMAL, client -> {
            cycleBlockState();
        });
    }

    private static void cycleBlockState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (!client.player.getAbilities().creativeMode) {
            sendMessage(Text.literal("❌ You need creative mode to cycle block states!").formatted(Formatting.RED));
            return;
        }

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            sendMessage(Text.literal("❌ You must be looking at a block!").formatted(Formatting.RED));
            return;
        }

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos pos = blockHitResult.getBlockPos();
        World world = client.world;
        BlockState currentState = world.getBlockState(pos);

        Collection<Property<?>> properties = currentState.getProperties();

        if (properties.isEmpty()) {
            sendMessage(Text.literal("❌ This block has no states to cycle!").formatted(Formatting.YELLOW));
            return;
        }

        List<Property<?>> propertyList = properties.stream().toList();

        // Shift is held - switch to next property
        boolean shiftHeld = client.options.sneakKey.isPressed();

        Property<?> changedProperty;
        String oldValue;

        if (shiftHeld) {
            currentPropertyIndex = (currentPropertyIndex + 1) % propertyList.size();
            changedProperty = propertyList.get(currentPropertyIndex);

            oldValue = getPropertyValue(currentState, changedProperty);

            Text message = Text.literal("")
                    .append(Text.literal("🔀 ").formatted(Formatting.AQUA))
                    .append(Text.literal("Selected Property: ").formatted(Formatting.GRAY))
                    .append(Text.literal(changedProperty.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(" = ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(oldValue).formatted(Formatting.YELLOW));

            sendMessage(message);
        } else {
            if (currentPropertyIndex >= propertyList.size()) {
                currentPropertyIndex = 0;
            }

            changedProperty = propertyList.get(currentPropertyIndex);
            oldValue = getPropertyValue(currentState, changedProperty);
            BlockState newState = cycleProperty(currentState, changedProperty);
            String newValue = getPropertyValue(newState, changedProperty);

            setBlockStateWithPackets(pos, newState);

            Text message = Text.literal("")
                    .append(Text.literal("🔄 ").formatted(Formatting.GREEN))
                    .append(Text.literal("Cycled ").formatted(Formatting.GRAY))
                    .append(Text.literal(changedProperty.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(": ").formatted(Formatting.GRAY))
                    .append(Text.literal(oldValue).formatted(Formatting.YELLOW))
                    .append(Text.literal(" → ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(newValue).formatted(Formatting.GREEN));

            sendMessage(message);
        }
    }

    private static void setBlockStateWithPackets(BlockPos pos, BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null || client.world == null) {
            return;
        }

        World world = client.world;
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity == null) {
            sendMessage(Text.literal("❌ No block entity found at the position!").formatted(Formatting.RED));
            return;
        }

        NbtCompound nbt = NBTManagers.BLOCK_ENTITY.getOrCreateNbt(blockEntity);

        ClientNetworking.send(new SetBlockC2SPacket(MiscUtil.client.world.getRegistryKey(),
                pos,
                MVRegistry.BLOCK.getId(state.getBlock()),
                new BlockStateProperties(state),
                nbt,
                ConfigScreen.isRecreateBlocksAndEntities(),
                ConfigScreen.isTriggerBlockUpdates()));

        world.updateListeners(pos, state, state, 3);
    }

    private static <T extends Comparable<T>> BlockState cycleProperty(BlockState state, Property<T> property) {
        T currentValue = state.get(property);

        Collection<T> values = property.getValues();
        List<T> valueList = values.stream().toList();

        int currentIndex = valueList.indexOf(currentValue);

        int nextIndex = (currentIndex + 1) % valueList.size();
        T nextValue = valueList.get(nextIndex);

        return state.with(property, nextValue);
    }

    private static <T extends Comparable<T>> String getPropertyValue(BlockState state, Property<T> property) {
        T value = state.get(property);
        return property.name(value);
    }

    private static void sendMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(message, true);
        }
    }
}
