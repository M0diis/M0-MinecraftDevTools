package me.m0dii.nbteditor.localnbt;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.nbtreferences.EntityReference;
import me.m0dii.nbteditor.packets.SummonEntityC2SPacket;
import me.m0dii.nbteditor.packets.ViewEntityS2CPacket;
import me.m0dii.nbteditor.server.ServerMiscUtil;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LocalEntity implements LocalNBT {

    @Setter
    @Getter
    private EntityType<?> entityType;
    private NbtCompound nbt;
    private Entity cachedEntity;
    private NbtCompound cachedNbt;

    public LocalEntity(EntityType<?> entityType, NbtCompound nbt) {
        this.entityType = entityType;
        this.nbt = nbt;
    }

    public static LocalEntity deserialize(NbtCompound nbt, int defaultDataVersion) {
        NbtCompound tag = nbt.getCompound("tag");
        tag.putString("id", nbt.getString("id"));
        tag = MiscUtil.updateDynamic(TypeReferences.ENTITY, tag, nbt.get("DataVersion"), defaultDataVersion);
        String id = tag.getString("id");
        tag.remove("id");
        return new LocalEntity(MVRegistry.ENTITY_TYPE.get(IdentifierInst.of(id)), tag);
    }

    private Entity getCachedEntity() {
        if (cachedEntity != null && cachedEntity.getType() == entityType && Objects.equals(cachedNbt, nbt)) {
            return cachedEntity;
        }

        cachedEntity = ServerMiscUtil.createEntity(entityType, MiscUtil.client.world);
        NBTManagers.ENTITY.setNbt(cachedEntity, nbt);

        cachedNbt = nbt.copy();

        return cachedEntity;
    }

    @Override
    public boolean isEmpty(Identifier id) {
        return false;
    }

    @Override
    public Text getName() {
        return MiscUtil.getNbtNameSafely(nbt, "CustomName", () -> TextInst.of(getDefaultName()));
    }

    @Override
    public void setName(Text name) {
        if (name == null) {
            getOrCreateNBT().remove("CustomName");
        } else {
            getOrCreateNBT().putString("CustomName", TextInst.toJsonString(name));
        }
    }

    @Override
    public String getDefaultName() {
        return entityType.getName().getString();
    }

    @Override
    public Identifier getId() {
        return MVRegistry.ENTITY_TYPE.getId(entityType);
    }

    @Override
    public void setId(Identifier id) {
        this.entityType = MVRegistry.ENTITY_TYPE.get(id);
    }

    @Override
    public Set<Identifier> getIdOptions() {
        return MVRegistry.ENTITY_TYPE.getIds();
    }

    @Override
    public NbtCompound getNBT() {
        return nbt;
    }

    @Override
    public void setNBT(NbtCompound nbt) {
        this.nbt = nbt;
    }

    @Override
    public NbtCompound getOrCreateNBT() {
        return nbt;
    }

    @Override
    public void renderIcon(MatrixStack matrices, int x, int y, float tickDelta) {
        matrices.push();
        matrices.translate(0.0, 8.0, 0.0);

        MVMatrix4f.ofScale(1, 1, -1).applyToPositionMatrix(matrices);
        MVQuaternionf rotation = LocalNBT.makeRotatingIcon(matrices, x, y, 0.75f, true);
        rotation.conjugate();
        rotation.rotateY((float) Math.PI);

        DiffuseLighting.method_34742();
        VertexConsumerProvider.Immediate provider = DrawableHelper.getVertexConsumerProvider();
        EntityRenderDispatcher dispatcher = MiscUtil.client.getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);
        rotation.applyToEntityRenderDispatcher(dispatcher);
        dispatcher.render(getCachedEntity(), 0, 0, 0, tickDelta, matrices, provider, 0xF000F0);
        dispatcher.setRenderShadows(true);
        provider.draw();

        matrices.pop();
    }

    @Override
    public Optional<ItemStack> toItem() {
        ItemStack output = null;
        for (Item item : MVRegistry.ITEM) {
            if (item instanceof SpawnEggItem spawnEggItem && MVMisc.getEntityType(new ItemStack(spawnEggItem)) == entityType) {
                output = new ItemStack(spawnEggItem);
            }
        }
        if (output == null) {
            if (entityType == EntityType.ARMOR_STAND) {
                output = new ItemStack(Items.ARMOR_STAND);
            } else {
                output = new ItemStack(Items.PIG_SPAWN_EGG);
            }
        }

        NbtCompound nbt = this.nbt.copy();
        nbt.putString("id", getId().toString());
        ItemTagReferences.ENTITY_DATA.set(output, nbt);

        return Optional.of(output);
    }

    @Override
    public NbtCompound serialize() {
        NbtCompound output = new NbtCompound();
        output.putString("id", getId().toString());
        output.put("tag", nbt);
        output.putString("type", "entity");
        return output;
    }

    @Override
    public Text toHoverableText() {
        UUID uuid = (nbt.containsUuid("UUID") ? nbt.getUuid("UUID") : UUID.nameUUIDFromBytes(new byte[]{0, 0, 0, 0}));
        return TextInst.bracketed(getName()).styled(
                style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(
                        entityType, uuid, MiscUtil.getNbtNameSafely(nbt, "CustomName", () -> null)))));
    }

    public CompletableFuture<Optional<EntityReference>> summon(RegistryKey<World> world, Vec3d pos) {
        return M0DevToolsClient.SERVER_CONN
                .sendRequest(requestId -> new SummonEntityC2SPacket(requestId, world, pos, getId(), nbt), ViewEntityS2CPacket.class)
                .thenApply(optional -> optional.filter(ViewEntityS2CPacket::foundEntity)
                        .map(packet -> {
                            EntityReference ref = new EntityReference(packet.getWorld(), packet.getUUID(),
                                    MVRegistry.ENTITY_TYPE.get(packet.getId()), packet.getNbt());
                            MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.get.entity")
                                    .append(ref.getLocalNBT().toHoverableText()), false);
                            return ref;
                        }));
    }

    @Override
    public LocalEntity copy() {
        return new LocalEntity(entityType, nbt.copy());
    }

    @Override
    public boolean equals(Object nbt) {
        if (nbt instanceof LocalEntity entity) {
            return this.entityType == entity.entityType && this.nbt.equals(entity.nbt);
        }
        return false;
    }

}
