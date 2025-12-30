package me.m0dii.nbteditor.nbtreferences;

import lombok.Getter;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.packets.GetEntityC2SPacket;
import me.m0dii.nbteditor.packets.SetEntityC2SPacket;
import me.m0dii.nbteditor.packets.ViewEntityS2CPacket;
import me.m0dii.nbteditor.screens.ConfigScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EntityReference implements NBTReference<LocalEntity> {

    @Getter
    private final RegistryKey<World> world;
    private final UUID uuid;
    @Getter
    private EntityType<?> entityType;
    private NbtCompound nbt;

    public EntityReference(RegistryKey<World> world, UUID uuid, EntityType<?> entityType, NbtCompound nbt) {
        this.world = world;
        this.uuid = uuid;
        this.entityType = entityType;
        this.nbt = nbt;
    }

    public static CompletableFuture<Optional<EntityReference>> getEntity(RegistryKey<World> world, UUID uuid) {
        return M0DevToolsClient.SERVER_CONN
                .sendRequest(requestId -> new GetEntityC2SPacket(requestId, world, uuid), ViewEntityS2CPacket.class)
                .thenApply(optional -> optional.filter(ViewEntityS2CPacket::foundEntity)
                        .map(packet -> new EntityReference(packet.getWorld(), packet.getUUID(),
                                MVRegistry.ENTITY_TYPE.get(packet.getId()), packet.getNbt())));
    }

    public UUID getUUID() {
        return uuid;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public LocalEntity getLocalNBT() {
        return new LocalEntity(entityType, nbt);
    }

    @Override
    public Identifier getId() {
        return EntityType.getId(entityType);
    }

    @Override
    public NbtCompound getNBT() {
        return nbt;
    }

    @Override
    public void saveNBT(Identifier id, NbtCompound toSave, Runnable onFinished) {
        this.entityType = MVRegistry.ENTITY_TYPE.get(id);
        this.nbt = toSave;

        ClientNetworking.send(new SetEntityC2SPacket(world, uuid, id, toSave,
                ConfigScreen.isRecreateBlocksAndEntities()));

        onFinished.run();
    }

}
