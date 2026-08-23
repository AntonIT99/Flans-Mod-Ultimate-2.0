package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.util.ValueIOUtils;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class Flagpole extends Entity implements ITeamBase
{
    private static final String NBT_DEFAULT_OWNER = "default_owner";
    private static final String NBT_OWNER = "owner";
    private static final String NBT_NAME = "name";
    private static final String NBT_MAP = "map";
    private static final String NBT_FLAG = "flag";
    private static final String NBT_OBJECTS = "objects";
    private static final String NBT_ID = "id";

    private static final EntityDataAccessor<Integer> DATA_DEFAULT_OWNER = SynchedEntityData.defineId(Flagpole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(Flagpole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(Flagpole.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MAP = SynchedEntityData.defineId(Flagpole.class, EntityDataSerializers.STRING);

    @Nullable private UUID flagId;
    private final Set<UUID> objectIds = new LinkedHashSet<>();

    public Flagpole(EntityType<?> type, Level level)
    {
        super(type, level);
        noPhysics = true;
    }

    public Flagpole(Level level, Vec3 position)
    {
        this(FlansMod.flagpoleEntity.get(), level);
        setPos(position);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_DEFAULT_OWNER, 0);
        builder.define(DATA_OWNER, 0);
        builder.define(DATA_NAME, "Default Base");
        builder.define(DATA_MAP, "");
    }

    @Override
    public void tick()
    {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved())
            return;

        // Give all entities from a freshly loaded chunk time to register before
        // deciding the persisted flag is missing.
        if (tickCount < 20)
            return;

        Flag flag = getFlag();
        if (flag == null)
        {
            flag = new Flag(serverLevel, this);
            flagId = flag.getUUID();
            objectIds.add(flagId);
            serverLevel.addFreshEntity(flag);
        }
        else if (flag.isHome())
        {
            flag.setPos(getX(), getY() + 2D, getZ());
        }
    }

    @Override
    public void onAddedToLevel()
    {
        super.onAddedToLevel();
        if (!level().isClientSide())
            TeamsManager.getInstance().registerBase(this);
    }

    @Override
    public void remove(@NotNull RemovalReason reason)
    {
        if (!level().isClientSide())
            TeamsManager.getInstance().unregisterBase(getUUID());
        super.remove(reason);
    }

    @NotNull
    @Override
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand, @NotNull Vec3 location)
    {
        if (level().isClientSide())
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemOpStick stick && serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
        {
            stick.useOnTeamObject(serverPlayer, this, held);
            return InteractionResult.CONSUME;
        }
        TeamsManager.getInstance().getCurrentGameType().ifPresent(type -> type.baseClicked(TeamsManager.getInstance(), serverPlayer, this));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input)
    {
        CompoundTag tag = ValueIOUtils.toCompoundTag(input);
        setDefaultOwnerId(tag.getIntOr(NBT_DEFAULT_OWNER, 0)); setOwnerId(tag.getIntOr(NBT_OWNER, 0)); setBaseName(tag.getStringOr(NBT_NAME, "Default Base")); setMapId(tag.getStringOr(NBT_MAP, ""));
        flagId = tag.read(NBT_FLAG, UUIDUtil.LENIENT_CODEC).orElse(null);
        objectIds.clear();
        for (Tag entry : tag.getListOrEmpty(NBT_OBJECTS))
        {
            CompoundTag object = (CompoundTag) entry;
            object.read(NBT_ID, UUIDUtil.LENIENT_CODEC).ifPresent(objectIds::add);
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_DEFAULT_OWNER, getDefaultOwnerId()); tag.putInt(NBT_OWNER, getOwnerId()); tag.putString(NBT_NAME, getBaseName()); tag.putString(NBT_MAP, getMapId());
        if (flagId != null)
            tag.store(NBT_FLAG, UUIDUtil.CODEC, flagId);
        ListTag objects = new ListTag();
        for (UUID id : objectIds)
        {
            CompoundTag object = new CompoundTag();
            object.store(NBT_ID, UUIDUtil.CODEC, id);
            objects.add(object);
        }
        tag.put(NBT_OBJECTS, objects);
        ValueIOUtils.storeCompoundTag(output, tag);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount)
    {
        return false;
    }

    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity)
    {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public UUID getObjectId()
    {
        return getUUID();
    }

    @Override
    public ResourceKey<Level> getDimension()
    {
        return level().dimension();
    }

    @Override
    public Vec3 getTeamObjectPosition()
    {
        return position();
    }

    @Override
    public String getBaseName()
    {
        return entityData.get(DATA_NAME);
    }

    @Override
    public void setBaseName(String name)
    {
        entityData.set(DATA_NAME, StringUtils.defaultIfBlank(name, "Default Base"));
    }

    @Override
    public int getDefaultOwnerId()
    {
        return entityData.get(DATA_DEFAULT_OWNER);
    }

    @Override
    public void setDefaultOwnerId(int id)
    {
        entityData.set(DATA_DEFAULT_OWNER, Math.max(0, id));
        setOwnerId(id);
    }

    @Override
    public int getOwnerId()
    {
        return entityData.get(DATA_OWNER);
    }

    @Override
    public void setOwnerId(int id)
    {
        entityData.set(DATA_OWNER, Math.max(0, id));
    }

    @Override
    public String getMapId()
    {
        return entityData.get(DATA_MAP);
    }

    @Override
    public void setMapId(String mapId)
    {
        entityData.set(DATA_MAP, StringUtils.defaultString(mapId).toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public Collection<UUID> getObjectIds()
    {
        return Collections.unmodifiableSet(objectIds);
    }

    @Override
    public void addObject(UUID objectId)
    {
        objectIds.add(objectId);
    }

    @Override
    public void removeObject(UUID objectId)
    {
        objectIds.remove(objectId);
    }

    @Override
    public void startRound()
    {
        setOwnerId(getDefaultOwnerId());
        Flag flag = getFlag();
        if (flag != null)
            flag.resetToBase();
    }

    @Override
    public void roundCleanup()
    {
        Flag flag = getFlag();
        if (flag != null)
            flag.resetToBase();
    }

    @Nullable
    @Override
    public Flag getFlag()
    {
        if (flagId == null || !(level() instanceof ServerLevel serverLevel))
            return null;
        Entity entity = serverLevel.getEntity(flagId);
        return entity instanceof Flag flag ? flag : null;
    }

    @Override
    public void destroyTeamObject()
    {
        Flag flag = getFlag();
        if (flag != null)
            flag.discard();
        discard();
    }
}
