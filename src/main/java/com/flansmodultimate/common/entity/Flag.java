package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class Flag extends Entity implements ITeamObject
{
    private static final EntityDataAccessor<Optional<UUID>> DATA_BASE = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CARRIER = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HOME = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TEAM = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.INT);
    private int returnTicks;

    public Flag(EntityType<?> type, Level level)
    {
        super(type, level);
        noPhysics = true;
    }

    public Flag(ServerLevel level, Flagpole base)
    {
        this(FlansMod.flagEntity.get(), level);
        setBaseId(base.getUUID());
        setTeamId(base.getOwnerId());
        setPos(base.getX(), base.getY() + 2D, base.getZ());
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_BASE, Optional.empty());
        entityData.define(DATA_CARRIER, Optional.empty());
        entityData.define(DATA_HOME, true);
        entityData.define(DATA_TEAM, 0);
        entityData.define(DATA_COLOUR, 0xFFFFFF);
    }

    @Override
    public void tick()
    {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        UUID carrierId = getCarrierId();
        if (carrierId != null)
        {
            ServerPlayer carrier = serverLevel.getServer().getPlayerList().getPlayer(carrierId);
            if (carrier == null || !carrier.isAlive() || carrier.level() != level())
                drop(30 * 20);
            else
                setPos(carrier.getX(), carrier.getEyeY() + 0.25D, carrier.getZ());
            return;
        }

        if (!isHome() && returnTicks > 0 && --returnTicks == 0)
            resetToBase();
        else if (isHome())
        {
            Flagpole base = getBase();
            if (base != null)
            {
                setTeamId(base.getOwnerId());
                com.flansmodultimate.common.types.Team team = TeamsManager.getInstance().getTeamForBase(base);
                setColour(team == null ? 0xFFFFFF : team.getTeamColour());
                setPos(base.getX(), base.getY() + 2D, base.getZ());
            }
        }
    }

    @NotNull
    @Override
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        if (level().isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemOpStick stick && serverPlayer.hasPermissions(2))
        {
            stick.useOnTeamObject(serverPlayer, this, held);
            return InteractionResult.CONSUME;
        }
        TeamsManager.getInstance().getCurrentGameType().ifPresent(type -> type.flagClicked(TeamsManager.getInstance(), serverPlayer, this));
        return InteractionResult.CONSUME;
    }

    public void pickUp(ServerPlayer player)
    {
        entityData.set(DATA_CARRIER, Optional.of(player.getUUID()));
        entityData.set(DATA_HOME, false);
        returnTicks = 0;
    }

    public void drop(int returnTimeTicks)
    {
        entityData.set(DATA_CARRIER, Optional.empty());
        entityData.set(DATA_HOME, false);
        returnTicks = Math.max(1, returnTimeTicks);
    }

    public void resetToBase()
    {
        entityData.set(DATA_CARRIER, Optional.empty());
        entityData.set(DATA_HOME, true);
        returnTicks = 0;
        Flagpole base = getBase();
        if (base != null)
            setPos(base.getX(), base.getY() + 2D, base.getZ());
    }

    public boolean isHome() { return entityData.get(DATA_HOME); }
    public boolean hasCarrier() { return getCarrierId() != null; }
    public boolean isCarriedBy(Player player) { return player.getUUID().equals(getCarrierId()); }
    @Nullable public UUID getCarrierId() { return entityData.get(DATA_CARRIER).orElse(null); }
    public int getTeamId() { return entityData.get(DATA_TEAM); }
    public void setTeamId(int id) { entityData.set(DATA_TEAM, Math.max(0, id)); }
    public int getColour() { return entityData.get(DATA_COLOUR); }
    public void setColour(int colour) { entityData.set(DATA_COLOUR, colour & 0xFFFFFF); }

    @Nullable
    public Flagpole getBase()
    {
        UUID id = getBaseId();
        return id == null ? null : TeamsManager.getInstance().getBase(id).orElse(null);
    }

    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setBaseId(tag.hasUUID("Base") ? tag.getUUID("Base") : null);
        entityData.set(DATA_CARRIER, tag.hasUUID("Carrier") ? Optional.of(tag.getUUID("Carrier")) : Optional.empty());
        entityData.set(DATA_HOME, tag.getBoolean("Home"));
        setTeamId(tag.getInt("Team"));
        setColour(tag.contains("Colour") ? tag.getInt("Colour") : 0xFFFFFF);
        returnTicks = tag.getInt("ReturnTicks");
    }

    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        UUID baseId = getBaseId();
        UUID carrierId = getCarrierId();
        if (baseId != null) tag.putUUID("Base", baseId);
        if (carrierId != null) tag.putUUID("Carrier", carrierId);
        tag.putBoolean("Home", isHome());
        tag.putInt("Team", getTeamId());
        tag.putInt("Colour", getColour());
        tag.putInt("ReturnTicks", returnTicks);
    }

    @NotNull @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isInvulnerableTo(@NotNull net.minecraft.world.damagesource.DamageSource source) { return true; }
    @Override public UUID getObjectId() { return getUUID(); }
    @Override public ResourceKey<Level> getDimension() { return level().dimension(); }
    @Override public Vec3 getTeamObjectPosition() { return position(); }
    @Override public @Nullable UUID getBaseId() { return entityData.get(DATA_BASE).orElse(null); }
    @Override public void setBaseId(@Nullable UUID id) { entityData.set(DATA_BASE, Optional.ofNullable(id)); }
    @Override public boolean isSpawnPoint() { return false; }
    @Override public void destroyTeamObject() { discard(); }
}
