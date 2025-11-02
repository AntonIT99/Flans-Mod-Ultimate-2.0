package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.types.GunType;
import lombok.Getter;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DeployedGun extends Entity implements IEntityAdditionalSpawnData, IFlanEntity
{
    protected static final EntityDataAccessor<String> GUN_TYPE = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.STRING);

    protected static List<DeployedGun> mgs = new ArrayList<>();

    /** Client and Server side */
    protected String shortname = StringUtils.EMPTY;

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

    public DeployedGun(EntityType<?> entityType, Level level, GunType gunType)
    {
        super(entityType, level);
        shortname = gunType.getShortName();
        type = gunType;
    }

    public String getShortName()
    {
        shortname = entityData.get(GUN_TYPE);
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(GUN_TYPE, shortname);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(GUN_TYPE, StringUtils.EMPTY);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        shortname = buf.readUtf();
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
