package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.types.GrenadeType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Grenade extends Shootable
{
    @Getter
    protected GrenadeType configType;

    protected Grenade(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData()
    {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer)
    {

    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData)
    {

    }
}
