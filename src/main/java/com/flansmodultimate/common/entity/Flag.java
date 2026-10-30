package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansModEntities;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.entity.FlanSpawnEntity;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class Flag extends FlanSpawnEntity implements ITeamObject
{
    private static final String NBT_BASE = "base";
    private static final String NBT_CARRIER = "carrier";
    private static final String NBT_HOME = "home";
    private static final String NBT_TEAM = "team";
    private static final String NBT_COLOUR = "colour";
    private static final String NBT_TEAM_NAME = "team_name";
    private static final String NBT_RETURN_TICKS = "return_ticks";

    private static final EntityDataAccessor<Optional<UUID>> DATA_BASE = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CARRIER = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HOME = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TEAM = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.INT);
    /** Short name of the team the flag belongs to, so that clients can draw that team's flag image. */
    private static final EntityDataAccessor<String> DATA_TEAM_NAME = SynchedEntityData.defineId(Flag.class, EntityDataSerializers.STRING);
    private int returnTicks;

    public Flag(EntityType<?> type, Level level)
    {
        super(type, level);
        noPhysics = true;
    }

    public Flag(ServerLevel level, Flagpole base)
    {
        this(FlansModEntities.flagEntity.get(), level);
        setBaseId(base.getUUID());
        setTeamId(base.getOwnerId());
        setPos(base.getX(), base.getY() + base.getPoleHeight() - 1D, base.getZ());
    }

    @Override
    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_BASE, Optional.empty());
        data.define(DATA_CARRIER, Optional.empty());
        data.define(DATA_HOME, true);
        data.define(DATA_TEAM, 0);
        data.define(DATA_COLOUR, 0xFFFFFF);
        data.define(DATA_TEAM_NAME, "");
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
            Entity carrier = findCarrier(serverLevel, carrierId);
            if (!(carrier instanceof LivingEntity living) || !living.isAlive() || carrier.level() != level())
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
                showTeam(TeamsManager.getInstance().getTeamForBase(base));
                setPos(base.getX(), base.getY() + base.getPoleHeight() - 1D, base.getZ());
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

    /** The flag's carrier: a player, or a combatant of the round, which a bot of an addon can be. */
    @Nullable
    private static Entity findCarrier(ServerLevel level, UUID carrierId)
    {
        Entity player = level.getServer().getPlayerList().getPlayer(carrierId);
        return player != null ? player : level.getEntity(carrierId);
    }

    public void pickUp(LivingEntity carrier)
    {
        entityData.set(DATA_CARRIER, Optional.of(carrier.getUUID()));
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
            setPos(base.getX(), base.getY() + base.getPoleHeight() - 1D, base.getZ());
    }

    public boolean isHome()
    {
        return entityData.get(DATA_HOME);
    }

    public boolean hasCarrier()
    {
        return getCarrierId() != null;
    }

    public boolean isCarriedBy(Entity carrier)
    {
        return carrier.getUUID().equals(getCarrierId());
    }

    @Nullable
    public UUID getCarrierId()
    {
        return entityData.get(DATA_CARRIER).orElse(null);
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

    /** Shows the colour and flag image of {@code team}, or a plain white flag for none. */
    private void showTeam(@Nullable com.flansmodultimate.common.types.Team team)
    {
        setColour(team == null ? 0xFFFFFF : team.getTeamColour());
        entityData.set(DATA_TEAM_NAME, team == null ? "" : Objects.toString(team.getShortName(), ""));
    }

    /** Short name of the team owning the flag, blank when none does. */
    public String getTeamName()
    {
        return entityData.get(DATA_TEAM_NAME);
    }

    @Nullable
    public Flagpole getBase()
    {
        UUID id = getBaseId();
        return id == null ? null : TeamsManager.getInstance().getBase(id).filter(Flagpole.class::isInstance).map(Flagpole.class::cast).orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setBaseId(tag.hasUUID(NBT_BASE) ? tag.getUUID(NBT_BASE) : null);
        entityData.set(DATA_CARRIER, tag.hasUUID(NBT_CARRIER) ? Optional.of(tag.getUUID(NBT_CARRIER)) : Optional.empty());
        entityData.set(DATA_HOME, tag.getBoolean(NBT_HOME));
        setTeamId(tag.getInt(NBT_TEAM));
        setColour(tag.contains(NBT_COLOUR) ? tag.getInt(NBT_COLOUR) : 0xFFFFFF);
        entityData.set(DATA_TEAM_NAME, tag.getString(NBT_TEAM_NAME));
        returnTicks = tag.getInt(NBT_RETURN_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        UUID baseId = getBaseId();
        UUID carrierId = getCarrierId();

        if (baseId != null)
            tag.putUUID(NBT_BASE, baseId);
        if (carrierId != null)
            tag.putUUID(NBT_CARRIER, carrierId);

        tag.putBoolean(NBT_HOME, isHome());
        tag.putInt(NBT_TEAM, getTeamId());
        tag.putInt(NBT_COLOUR, getColour());
        tag.putString(NBT_TEAM_NAME, getTeamName());
        tag.putInt(NBT_RETURN_TICKS, returnTicks);
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source)
    {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        // Still invulnerable: the running game type is only told about the attempt.
        if (!level().isClientSide)
            TeamsManager.getInstance().teamEntityAttacked(this, source);
        return false;
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
        return entityData.get(DATA_BASE).orElse(null);
    }

    @Override
    public void setBaseId(@Nullable UUID id)
    {
        entityData.set(DATA_BASE, Optional.ofNullable(id));
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
}
