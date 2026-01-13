package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
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

public class DeployedGun extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<GunType>
{
    public static final int RENDER_DISTANCE = 64;
    public static final float DEFAULT_HITBOX_SIZE = 1F;

    protected static final EntityDataAccessor<String> GUN_TYPE = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.STRING);

    @Getter
    protected GunType configType;
    protected String shortname = StringUtils.EMPTY;
    protected BlockPos blockPos;
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

    public DeployedGun(Level level, BlockPos pos, int dir, GunType gunType)
    {
        super(FlansMod.deployedGunEntity.get(), level);
        shortname = gunType.getShortName();
        blockPos = pos;
        direction = dir;
        type = gunType;
        this.xo = blockPos.getX() + 0.5D;
        this.yo = blockPos.getY();
        this.zo = blockPos.getZ() + 0.5D;
        this.setPos(xo, yo, zo);
        this.yRotO = 0.0F;
        this.xRotO = -60.0F;
        this.setYRot(yRotO);
        this.setXRot(xRotO);
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
    public boolean shouldRender(double x, double y, double z)
    {
        return true;
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
        if (InfoType.getInfoType(shortname) instanceof GunType gType)
            configType = gType;
        if (configType == null)
        {
            FlansMod.log.warn("Unknown gun type {}, discarding.", shortname);
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        if (InfoType.getInfoType(shortname) instanceof GunType gType)
        {
            configType = gType;
        }
        else
        {
            discard();
        }
    }
}
