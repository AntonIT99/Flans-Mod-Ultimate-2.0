package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NukeDropEntity extends Entity
{
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
            if (explodedTicks > ModCommonConfig.apocalypseNukeVisualTicks())
                discard();
            return;
        }

        if (!onGround())
        {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.01D, 0.0D));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            return;
        }

        impact();
    }

    public int getExplodedTicks()
    {
        return entityData.get(DATA_EXPLODED_TICKS);
    }

    private void setExplodedTicks(int ticks)
    {
        entityData.set(DATA_EXPLODED_TICKS, ticks);
    }

    private void impact()
    {
        setExplodedTicks(1);
        if (level().isClientSide || !ModCommonConfig.apocalypseNukeDropsEnabled())
            return;

        float power = ModCommonConfig.apocalypseNukeExplosionPower();
        if (power > 0.0F && level() instanceof ServerLevel serverLevel)
            serverLevel.explode(this, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.MOB);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_EXPLODED_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setExplodedTicks(tag.getInt("ExplodedTicks"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        tag.putInt("ExplodedTicks", getExplodedTicks());
    }
}
