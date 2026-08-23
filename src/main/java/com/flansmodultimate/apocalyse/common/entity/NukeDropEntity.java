package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.config.ModApocalypseConfig;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NukeDropEntity extends Entity
{
    private static final String NBT_EXPLODED_TICKS = "exploded_ticks";
    private static final EntityDataAccessor<Integer> DATA_EXPLODED_TICKS = SynchedEntityData.defineId(NukeDropEntity.class, EntityDataSerializers.INT);

    public NukeDropEntity(EntityType<? extends NukeDropEntity> type, Level level)
    {
        super(type, level);
        setNoGravity(false);
    }

    @Override
    public void tick()
    {
        super.tick();

        int explodedTicks = getExplodedTicks();
        if (explodedTicks > 0)
        {
            setDeltaMovement(0.0D, 0.0D, 0.0D);
            setExplodedTicks(explodedTicks + 1);
            if (explodedTicks > ModApocalypseConfig.apocalypseNukeVisualTicks())
                discard();
            return;
        }

        if (!onGround())
        {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.01D, 0.0D));
            move(MoverType.SELF, getDeltaMovement());
            return;
        }

        impact(level());
    }

    public int getExplodedTicks()
    {
        return entityData.get(DATA_EXPLODED_TICKS);
    }

    private void setExplodedTicks(int ticks)
    {
        entityData.set(DATA_EXPLODED_TICKS, ticks);
    }

    private void impact(Level level)
    {
        setExplodedTicks(1);
        if (level.isClientSide() || !ModApocalypseConfig.apocalypseNukeDropsEnabled())
            return;

        float power = ModApocalypseConfig.apocalypseNukeExplosionPower();
        if (power > 0.0F && level instanceof ServerLevel serverLevel)
            serverLevel.explode(this, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.MOB);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_EXPLODED_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input)
    {
        setExplodedTicks(input.getIntOr(NBT_EXPLODED_TICKS, 0));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output)
    {
        output.putInt(NBT_EXPLODED_TICKS, getExplodedTicks());
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount)
    {
        return false;
    }
}
