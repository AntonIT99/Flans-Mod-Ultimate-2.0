package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class Flag extends Entity implements ITeamObject
{
    private static final String NBT_BASE = "base";
    private static final String NBT_CARRIER = "carrier";
    private static final String NBT_HOME = "home";
    private static final String NBT_TEAM = "team";
    private static final String NBT_COLOUR = "colour";
    private static final String NBT_RETURN_TICKS = "return_ticks";

    private static final EntityDataAccessor<String> DATA_BASE = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CARRIER = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.STRING);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_BASE, "");
        builder.define(DATA_CARRIER, "");
        builder.define(DATA_HOME, true);
        builder.define(DATA_TEAM, 0);
        builder.define(DATA_COLOUR, 0xFFFFFF);
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
        TeamsManager.getInstance().getCurrentGameType().ifPresent(type -> type.flagClicked(TeamsManager.getInstance(), serverPlayer, this));
        return InteractionResult.CONSUME;
    }

    public void pickUp(ServerPlayer player)
    {
        entityData.set(DATA_CARRIER, player.getUUID().toString());
        entityData.set(DATA_HOME, false);
        returnTicks = 0;
    }

    public void drop(int returnTimeTicks)
    {
        entityData.set(DATA_CARRIER, "");
        entityData.set(DATA_HOME, false);
        returnTicks = Math.max(1, returnTimeTicks);
    }

    public void resetToBase()
    {
        entityData.set(DATA_CARRIER, "");
        entityData.set(DATA_HOME, true);
        returnTicks = 0;
        Flagpole base = getBase();
        if (base != null)
            setPos(base.getX(), base.getY() + 2D, base.getZ());
    }

    public boolean isHome()
    {
        return entityData.get(DATA_HOME);
    }

    public boolean hasCarrier()
    {
        return getCarrierId() != null;
    }

    public boolean isCarriedBy(Player player)
    {
        return player.getUUID().equals(getCarrierId());
    }

    @Nullable
    public UUID getCarrierId()
    {
        return parseUuid(entityData.get(DATA_CARRIER));
    }

    public int getTeamId()
    {
        return entityData.get(DATA_TEAM);
    }

    public void setTeamId(int id)
    {
        entityData.set(DATA_TEAM, Math.max(0, id));
    }

    public int getColour()
    {
        return entityData.get(DATA_COLOUR);
    }

    public void setColour(int colour)
    {
        entityData.set(DATA_COLOUR, colour & 0xFFFFFF);
    }

    @Nullable
    public Flagpole getBase()
    {
        UUID id = getBaseId();
        return id == null ? null : TeamsManager.getInstance().getBase(id).orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input)
    {
        setBaseId(input.read(NBT_BASE, UUIDUtil.LENIENT_CODEC).orElse(null));
        entityData.set(DATA_CARRIER, input.read(NBT_CARRIER, UUIDUtil.LENIENT_CODEC).map(UUID::toString).orElse(""));
        entityData.set(DATA_HOME, input.getBooleanOr(NBT_HOME, true)); setTeamId(input.getIntOr(NBT_TEAM, 0));
        setColour(input.getIntOr(NBT_COLOUR, 0xFFFFFF)); returnTicks = input.getIntOr(NBT_RETURN_TICKS, 0);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output)
    {
        UUID baseId = getBaseId();
        UUID carrierId = getCarrierId();

        if (baseId != null)
            output.store(NBT_BASE, UUIDUtil.CODEC, baseId);
        if (carrierId != null)
            output.store(NBT_CARRIER, UUIDUtil.CODEC, carrierId);

        output.putBoolean(NBT_HOME, isHome()); output.putInt(NBT_TEAM, getTeamId()); output.putInt(NBT_COLOUR, getColour()); output.putInt(NBT_RETURN_TICKS, returnTicks);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount)
    {
        return false;
    }

    @Override
    @NotNull
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity)
    {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public boolean isPickable()
    {
        return true;
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
    @Nullable
    public UUID getBaseId()
    {
        return parseUuid(entityData.get(DATA_BASE));
    }

    @Override
    public void setBaseId(@Nullable UUID id)
    {
        entityData.set(DATA_BASE, id == null ? "" : id.toString());
    }

    @Override
    public boolean isSpawnPoint()
    {
        return false;
    }

    @Override
    public void destroyTeamObject()
    {
        discard();
    }

    @Nullable
    private static UUID parseUuid(String value)
    {
        if (value.isEmpty())
            return null;
        try
        {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
    }
}
