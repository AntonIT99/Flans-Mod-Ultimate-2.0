package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.types.GunType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DeployedGun extends Entity
{
    protected static List<DeployedGun> mgs = new ArrayList<>();

    @Getter
    protected GunType configType;

    protected int blockX;
    protected int blockY;
    protected int blockZ;
    protected int direction;
    protected GunType type;
    @Getter
    protected ItemStack ammo = ItemStack.EMPTY;
    @Getter
    protected int reloadTimer;
    protected int soundDelay;
    protected float shootDelay;

    protected Player gunner;
    //Server side
    protected boolean isShooting;
    //Client side
    protected boolean wasShooting = false;

    protected int ticksSinceUsed = 0;

    public DeployedGun(EntityType<?> entityType, Level level)
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
}
