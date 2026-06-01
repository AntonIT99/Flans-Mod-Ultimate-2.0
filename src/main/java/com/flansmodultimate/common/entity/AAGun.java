package com.flansmodultimate.common.entity;

import com.google.common.collect.ImmutableList;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.handler.DeployableGunShootingHandler;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.server.PacketAAGunInput;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AAGun extends Entity implements IFlanEntity<AAGunType>, IEntityWithComplexSpawn
{
    public static final int RENDER_DISTANCE = 64;
    public static final float DEFAULT_HITBOX_SIZE = 1.5F;

    public static final String NBT_TYPE_NAME = "type";
    public static final String NBT_AMMO = "ammo";
    public static final String NBT_BLOCK_X = "block_x";
    public static final String NBT_BLOCK_Y = "block_y";
    public static final String NBT_BLOCK_Z = "block_z";
    public static final String NBT_DIRECTION = "direction";
    public static final String NBT_HEALTH = "health";

    protected static final EntityDataAccessor<String> DATA_AA_TYPE = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Boolean> DATA_HAS_AMMO = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> DATA_RELOAD_TIMER = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_GUN_DIRECTION = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);

    protected AAGunType configType;
    protected String shortname = StringUtils.EMPTY;
    protected BlockPos blockPos;
    protected int gunDirection;
    protected ItemStack ammo = ItemStack.EMPTY;
    protected int reloadTimer;
    protected int soundTimer;
    protected float shootTimer;
    protected int ticksSinceUsed;
    @Getter @Setter
    protected boolean shootKeyPressed;
    @Getter @Setter
    protected boolean prevShootKeyPressed;
    protected int health;
    protected Entity targetEntity;
    protected int targetScanCooldown;

    protected Vec3 barrelOffset = Vec3.ZERO;
    protected boolean barrelParsed;

    public AAGun(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public AAGun(Level level, BlockPos pos, Direction direction, AAGunType aaGunType)
    {
        super(FlansMod.aaGunEntity.get(), level);
        setShortName(aaGunType.getShortName());
        blockPos = pos;
        setGunDirection(direction.get2DDataValue());
        configType = aaGunType;
        setPos(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
        setYRot(0F);
        setXRot(-60F);
        health = aaGunType.getHealth();
        parseBarrelOffset();
    }

    @Override
    public AAGunType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof AAGunType aaType)
        {
            configType = aaType;
        }
        return configType;
    }

    public String getShortName()
    {
        return entityData.get(DATA_AA_TYPE);
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(DATA_AA_TYPE, shortname);
    }

    public int getReloadTimer()
    {
        return entityData.get(DATA_RELOAD_TIMER);
    }

    public void setReloadTimer(int v)
    {
        reloadTimer = v;
        entityData.set(DATA_RELOAD_TIMER, v);
    }

    public boolean hasAmmo()
    {
        return entityData.get(DATA_HAS_AMMO);
    }

    public void setHasAmmo(boolean v)
    {
        entityData.set(DATA_HAS_AMMO, v);
    }

    public int getGunDirection()
    {
        return entityData.get(DATA_GUN_DIRECTION);
    }

    public void setGunDirection(int d)
    {
        gunDirection = d;
        entityData.set(DATA_GUN_DIRECTION, d);
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        return ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq)
    {
        double r = RENDER_DISTANCE;
        return distSq < r * r;
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling()
    {
        return new AABB(getX() - RENDER_DISTANCE, getY() - RENDER_DISTANCE, getZ() - RENDER_DISTANCE, getX() + RENDER_DISTANCE, getY() + RENDER_DISTANCE, getZ() + RENDER_DISTANCE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_AA_TYPE, StringUtils.EMPTY);
        builder.define(DATA_HAS_AMMO, false);
        builder.define(DATA_RELOAD_TIMER, 0);
        builder.define(DATA_GUN_DIRECTION, 0);
    }

    public void writeSpawnData(RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
        buf.writeInt(gunDirection);
        buf.writeInt(blockPos.getX());
        buf.writeInt(blockPos.getY());
        buf.writeInt(blockPos.getZ());
        buf.writeBoolean(!ammo.isEmpty());
        buf.writeInt(health);
    }

    public void readSpawnData(RegistryFriendlyByteBuf buf)
    {
        try
        {
            setShortName(buf.readUtf());
            if (InfoType.getInfoType(shortname) instanceof AAGunType aaType)
                configType = aaType;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown AA gun type {}, discarding.", shortname);
                discard();
            }
            gunDirection = buf.readInt();
            blockPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            setHasAmmo(buf.readBoolean());
            health = buf.readInt();
            parseBarrelOffset();
        }
        catch (Exception e)
        {
            discard();
            FlansMod.log.warn("Failed to read AA gun spawn data", e);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setShortName(tag.getString(NBT_TYPE_NAME));

        if (InfoType.getInfoType(shortname) instanceof AAGunType aaType)
            configType = aaType;
        else
            discard();

        setGunDirection(tag.getInt(NBT_DIRECTION));
        blockPos = new BlockPos(tag.getInt(NBT_BLOCK_X), tag.getInt(NBT_BLOCK_Y), tag.getInt(NBT_BLOCK_Z));

        if (tag.contains(NBT_AMMO, Tag.TAG_COMPOUND))
        {
            CompoundTag ammoTag = tag.getCompound(NBT_AMMO);
            ammo = ammoTag.isEmpty() ? ItemStack.EMPTY : ItemStack.parse(level().registryAccess(), ammoTag).orElse(ItemStack.EMPTY);
        }
        else
            ammo = ItemStack.EMPTY;
        setHasAmmo(!ammo.isEmpty());
        health = tag.getInt(NBT_HEALTH);
        if (health <= 0 && configType != null)
            health = configType.getHealth();
        parseBarrelOffset();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        if (configType == null)
        {
            discard();
            return;
        }

        tag.putString(NBT_TYPE_NAME, shortname);
        tag.putInt(NBT_DIRECTION, gunDirection);
        tag.putInt(NBT_BLOCK_X, blockPos.getX());
        tag.putInt(NBT_BLOCK_Y, blockPos.getY());
        tag.putInt(NBT_BLOCK_Z, blockPos.getZ());

        if (!ammo.isEmpty())
        {
            CompoundTag ammoTag = new CompoundTag();
            ammo.save(level().registryAccess(), ammoTag);
            tag.put(NBT_AMMO, ammoTag);
        }
        tag.putInt(NBT_HEALTH, health);
    }

    @Override
    public void remove(@NotNull RemovalReason reason)
    {
        try
        {
            Level level = level();

            if (!level.isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK)
            {
                if (FlansMod.teamsManager.getWeaponDrops() == TeamsManager.EnumWeaponDrop.SMART_DROPS)
                {
                    level.addFreshEntity(new GunItemEntity(level, getX(), getY(), getZ(), ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY), Collections.singletonList(ammo)));
                }
                else if (FlansMod.teamsManager.getWeaponDrops() == TeamsManager.EnumWeaponDrop.DROPS)
                {
                    spawnAtLocation(ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY), 0F);
                    if (!ammo.isEmpty())
                        spawnAtLocation(ammo.copy(), 0.5F);
                }
            }
        }
        catch (Exception e)
        {
            FlansMod.log.error("Error removing AA gun entity", e);
        }

        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        Entity entity = source.getEntity();
        Entity gunner = getFirstPassenger();

        if (gunner == entity)
            return true;

        if (gunner != null)
            return gunner.hurt(source, amount);

        health -= (int) amount;
        if (health <= 0)
            discard();

        return true;
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        Level level = level();
        Entity gunner = getFirstPassenger();

        if (player != gunner && gunner != null)
            return InteractionResult.sidedSuccess(level.isClientSide);

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        PlayerData data = PlayerData.getInstance(player);

        if (player == gunner)
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }

        if (player.getVehicle() != null)
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }

        if (FlansMod.teamsManager.getCurrentRound().isPresent() && Team.SPECTATORS.equals(data.getTeam()))
            return InteractionResult.CONSUME;

        player.startRiding(this, true);
        reloadGun(level, player);

        return InteractionResult.CONSUME;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        if (!(passenger instanceof Player p) || configType == null || blockPos == null)
            return;

        float baseYaw = Direction.from2DDataValue(getGunDirection()).toYRot();
        float localYaw = Mth.wrapDegrees(p.getYRot() - baseYaw);

        float side = configType.getSideViewLimit();
        localYaw = Mth.clamp(localYaw, -side, side);

        float top = configType.getTopViewLimit();
        float bottom = configType.getBottomViewLimit();
        if (top > bottom)
        {
            float tmp = top;
            top = bottom; bottom = tmp;
        }
        float pitch = Mth.clamp(p.getXRot(), top, bottom);

        setYRot(baseYaw + localYaw);
        setXRot(pitch);

        double standBack = configType.getStandBackDist();
        float yawRad = getYRot() * Mth.DEG_TO_RAD;

        double offX = standBack * Math.sin(yawRad);
        double offZ = -standBack * Math.cos(yawRad);

        double x = getX() + offX;
        double z = getZ() + offZ;

        float maxAbsPitch = Math.max(Math.abs(top), Math.abs(bottom));
        float pitchNorm = (maxAbsPitch > 0.0001F) ? (pitch / maxAbsPitch) : 0F;
        double maxPitchYOffset = 0.5D;
        double pitchYOffset = maxPitchYOffset * pitchNorm;
        double baseY = blockPos.getY() + 0.5D - 0.65D;
        double y = baseY + pitchYOffset;

        move.accept(passenger, x, y, z);

        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.fallDistance = 0.0F;
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger)
    {
        super.addPassenger(passenger);

        shootKeyPressed = false;
        prevShootKeyPressed = false;
        targetEntity = null;
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger)
    {
        super.removePassenger(passenger);

        shootKeyPressed = false;
        prevShootKeyPressed = false;
        targetEntity = null;
    }

    @Override
    @NotNull
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger)
    {
        Level level = level();

        if (blockPos != null)
        {
            float yawRad = getShootingYaw() * Mth.DEG_TO_RAD;

            double dist = configType.getStandBackDist();
            Vec3 preferred = new Vec3(blockPos.getX() + 0.5D, blockPos.getY() - 1D, blockPos.getZ() + 0.5D)
                .add(dist * Math.sin(yawRad), 0.0D, -dist * Math.cos(yawRad));

            if (isSafeDismount(level, passenger, preferred))
                return preferred;
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    private boolean isSafeDismount(Level level, LivingEntity passenger, Vec3 targetPos)
    {
        Vec3 delta = targetPos.subtract(passenger.position());
        AABB movedBB = passenger.getBoundingBox().move(delta);

        return level.noCollision(passenger, movedBB);
    }

    @Override
    public void tick()
    {
        super.tick();

        Level level = level();
        Entity gunner = getFirstPassenger();

        if (blockPos == null)
            blockPos = this.blockPosition();

        if (gunner == null || !gunner.isAlive())
            shootKeyPressed = false;

        if (level.isClientSide)
            clientTick(level);
        else
            serverTick(level);
    }

    protected void clientTick(Level level)
    {
        if (getFirstPassenger() != net.minecraft.client.Minecraft.getInstance().player)
            return;

        if (net.minecraft.client.Minecraft.getInstance().screen != null)
        {
            if (shootKeyPressed)
            {
                shootKeyPressed = false;
                PacketHandler.sendToServer(new PacketAAGunInput(this, false, prevShootKeyPressed));
            }
        }
        else
        {
            GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(InteractionHand.MAIN_HAND);
            shootKeyPressed = primaryFunctionState.isPressed();
            prevShootKeyPressed = primaryFunctionState.isPrevPressed();

            if (shootKeyPressed != prevShootKeyPressed)
                PacketHandler.sendToServer(new PacketAAGunInput(this, shootKeyPressed, prevShootKeyPressed));
        }
    }

    protected void serverTick(Level level)
    {
        if (configType == null)
        {
            discard();
            return;
        }

        ticksSinceUsed++;

        int mgLife = FlansMod.teamsManager.getMgLife();
        if (mgLife > 0 && ticksSinceUsed > mgLife * 20)
        {
            discard();
            return;
        }

        BlockPos supportPos = blockPos.below();
        if (level.isEmptyBlock(supportPos))
            discard();

        if (shootTimer > 0)
            shootTimer--;
        if (soundTimer > 0)
            soundTimer--;
        if (reloadTimer > 0)
            setReloadTimer(reloadTimer - 1);

        if (!ammo.isEmpty() && ammo.isDamageableItem() && ammo.getDamageValue() >= ammo.getMaxDamage())
        {
            ammo = ItemStack.EMPTY;
            setHasAmmo(false);
        }

        Entity gunner = getFirstPassenger();

        if (gunner instanceof LivingEntity living)
        {
            if (living instanceof Player player)
                reloadGun(level, player);
            fireGun(level, living);
        }
        else
        {
            updateAutoTargeting(level);
            autoFire(level);
        }
    }

    protected void updateAutoTargeting(Level level)
    {
        if (configType == null || !isAnyTargetEnabled())
            return;

        if (targetScanCooldown > 0)
        {
            targetScanCooldown--;
        }
        else
        {
            targetScanCooldown = 10;
            targetEntity = findBestTarget(level);
        }

        if (targetEntity == null || !targetEntity.isAlive())
        {
            targetEntity = null;
            return;
        }

        double pivotX = blockPos.getX() + 0.5;
        double pivotZ = blockPos.getZ() + 0.5;
        double pivotY = blockPos.getY() + configType.getPivotHeight();

        double dx = targetEntity.getX() - pivotX;
        double dz = targetEntity.getZ() - pivotZ;
        double dy = targetEntity.getY() + targetEntity.getBbHeight() / 2 - pivotY;

        float targetYaw = (float) Mth.atan2(dz, dx) * Mth.RAD_TO_DEG - 90F;
        float targetPitch = (float) Mth.atan2(-dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG;

        float baseYaw = Direction.from2DDataValue(getGunDirection()).toYRot();

        float localYaw = Mth.wrapDegrees(targetYaw - baseYaw);
        float side = configType.getSideViewLimit();
        localYaw = Mth.clamp(localYaw, -side, side);
        float clampedYaw = baseYaw + localYaw;

        float minPitch = -configType.getTopViewLimit();
        float maxPitch = configType.getBottomViewLimit();
        float clampedPitch = Mth.clamp(targetPitch, minPitch, maxPitch);

        setYRot(clampedYaw);
        setXRot(clampedPitch);
    }

    protected boolean isAnyTargetEnabled()
    {
        return configType.isTargetMobs() || configType.isTargetPlayers() || configType.isTargetVehicles() || configType.isTargetPlanes() || configType.isTargetMechas();
    }

    @Nullable
    protected Entity findBestTarget(Level level)
    {
        if (configType == null)
            return null;

        double range = configType.getTargetRange();
        AABB searchBox = new AABB(getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range);

        List<Entity> candidates = level.getEntities(this, searchBox, this::isValidTarget);
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity candidate : candidates)
        {
            double dist = distanceToSqr(candidate);
            if (dist < closestDist && hasLineOfSight(level, candidate))
            {
                closestDist = dist;
                closest = candidate;
            }
        }

        return closest;
    }

    protected boolean isValidTarget(Entity entity)
    {
        if (!entity.isAlive() || entity == getFirstPassenger() || !entity.isPickable())
            return false;

        if (configType.isTargetPlayers() && entity instanceof Player)
            return true;
        if (configType.isTargetMobs() && entity instanceof Mob)
            return true;
        if (configType.isTargetVehicles())
        {
            ResourceLocation key = EntityType.getKey(entity.getType());
            String path = key.getPath().toLowerCase();
            if (path.contains("vehicle") || path.contains("car") || path.contains("tank") || path.contains("boat") || entity.getType().getCategory().isFriendly())
                return true;
        }
        if (configType.isTargetPlanes())
        {
            ResourceLocation key = EntityType.getKey(entity.getType());
            String path = key.getPath().toLowerCase();
            if (path.contains("plane") || path.contains("aircraft") || path.contains("helicopter") || path.contains("fly"))
                return true;
        }
        if (configType.isTargetMechas())
        {
            ResourceLocation key = EntityType.getKey(entity.getType());
            String path = key.getPath().toLowerCase();
            if (path.contains("mecha") || path.contains("robot") || path.contains("golem"))
                return true;
        }

        return false;
    }

    protected boolean hasLineOfSight(Level level, Entity target)
    {
        Vec3 origin = getShootingOrigin();
        Vec3 targetPos = target.getEyePosition();
        HitResult result = level.clip(new net.minecraft.world.level.ClipContext(origin, targetPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetPos) < 1.0;
    }

    protected void autoFire(Level level)
    {
        if (targetEntity == null || !targetEntity.isAlive())
            return;

        if (ammo.isEmpty() || reloadTimer > 0 || shootTimer > 0 || !(ammo.getItem() instanceof ShootableItem shootableItem))
        {
            if (ammo.isEmpty() && reloadTimer <= 0)
                autoReload(level);
            return;
        }

        float delay = configType.getShootDelay();

        while (shootTimer <= 0)
        {
            ShootingHelper.fireGun(level, null, this, shootableItem.getConfigType(), ammo, new DeployableGunShootingHandler(ammo));

            float recoilAmount = configType.getRecoil();
            setXRot(getXRot() + recoilAmount);

            if (soundTimer <= 0)
            {
                if (StringUtils.isNotBlank(configType.getShootSound()))
                {
                    PacketPlaySound.sendSoundPacket(this, 40F, configType.getShootSound(), false, false);
                    soundTimer = 10;
                }
            }

            shootTimer += delay;

            if (!configType.isFireAlternately())
                break;
        }
    }

    protected void autoReload(Level level)
    {
        if (configType == null)
            return;

        double range = 4.0;
        AABB searchBox = new AABB(getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range);

        for (Entity entity : level.getEntities((Entity) null, searchBox))
        {
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity)
            {
                ItemStack stack = itemEntity.getItem();
                if (stack.getItem() instanceof ShootableItem shootableItem && configType.getAmmoTypes().contains(shootableItem.getConfigType()))
                {
                    int needed = configType.getNumBarrels();
                    int taken = Math.min(stack.getCount(), needed);
                    ItemStack newAmmo = stack.copy();
                    newAmmo.setCount(taken);
                    if (configType.isShareAmmo())
                        stack.shrink(taken);
                    else
                        stack.shrink(1);

                    ammo = newAmmo.copy();
                    setHasAmmo(true);
                    setReloadTimer(configType.getReloadTime());

                    String reloadSound = configType.getReloadSound();
                    if (StringUtils.isNotBlank(reloadSound))
                        PacketPlaySound.sendSoundPacket(this, 30F, reloadSound, false, false);
                    return;
                }
            }
        }

        if (configType.isShareAmmo())
        {
            for (Entity entity : level.getEntities((Entity) null, searchBox))
            {
                if (entity instanceof AAGun otherGun && otherGun != this && otherGun.isAlive() && otherGun.configType != null && !otherGun.ammo.isEmpty())
                {
                    if (StringUtils.equals(configType.getAmmo(), otherGun.configType.getAmmo()))
                    {
                        ItemStack shared = otherGun.ammo.split(1);
                        ammo = shared;
                        setHasAmmo(true);
                        setReloadTimer(configType.getReloadTime());
                        return;
                    }
                }
            }
        }
    }

    public void fireGun(Level level, LivingEntity gunner)
    {
        if (level.isClientSide || !gunner.isAlive() || ammo.isEmpty() || reloadTimer > 0 || shootTimer > 0 || !(ammo.getItem() instanceof ShootableItem shootableItem))
            return;

        if (shootKeyPressed)
        {
            float delay = configType.getShootDelay();

            while (shootTimer <= 0)
            {
                ShootingHelper.fireGun(level, gunner, this, shootableItem.getConfigType(), ammo, new DeployableGunShootingHandler(ammo));

                float recoilAmount = configType.getRecoil();
                setXRot(getXRot() + recoilAmount);

                if (soundTimer <= 0)
                {
                    if (StringUtils.isNotBlank(configType.getShootSound()))
                    {
                        PacketPlaySound.sendSoundPacket(this, 40F, configType.getShootSound(), false, false);
                        soundTimer = 10;
                    }
                }

                shootTimer += delay;

                if (!configType.isFireAlternately())
                    break;
            }
        }
    }

    public void reloadGun(Level level, Player gunner)
    {
        if (level.isClientSide || !gunner.isAlive() || !ammo.isEmpty() || reloadTimer > 0)
            return;

        int slot = findAmmo(gunner);
        if (slot >= 0)
        {
            ItemStack taken = gunner.getInventory().getItem(slot);
            if (!taken.isEmpty())
            {
                if (!gunner.getAbilities().instabuild)
                    gunner.getInventory().setItem(slot, ItemStack.EMPTY);

                reloadGun(level, gunner, taken);
            }
        }
    }

    public void reloadGun(Level level, LivingEntity gunner, ItemStack newAmmo)
    {
        if (level.isClientSide || !gunner.isAlive() || !ammo.isEmpty() || reloadTimer > 0)
            return;

        ammo = newAmmo.copy();
        setHasAmmo(true);
        setReloadTimer(configType.getReloadTime());
        String reloadSound = configType.getReloadSound();

        if (StringUtils.isNotBlank(reloadSound))
            PacketPlaySound.sendSoundPacket(gunner, 30F, reloadSound, false, false);
    }

    protected int findAmmo(Player player)
    {
        List<ShootableType> allowed = configType.getAmmoTypes();
        Inventory inv = player.getInventory();

        int selected = inv.selected;
        ItemStack selectedStack = inv.getItem(selected);
        if (selectedStack.getItem() instanceof ShootableItem shootableItem && allowed.contains(shootableItem.getConfigType()))
            return selected;

        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof ShootableItem shootableItem) || !allowed.contains(shootableItem.getConfigType()))
                continue;

            int score = getPreferredAmmoScore(i, stack, selected);

            if (score > bestScore)
            {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    protected static int getPreferredAmmoScore(int i, ItemStack stack, int selected)
    {
        int score = 0;

        if (i < 9)
            score += 1_000_000;

        if (stack.isDamageableItem())
        {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            score += remaining * 1000;
        }
        else
        {
            score += stack.getCount() * 1000;
        }

        if (i < 9)
            score -= Math.abs(i - selected);
        return score;
    }

    public Vec3 getShootingOrigin()
    {
        double pivotX = blockPos.getX() + 0.5;
        double pivotY = blockPos.getY() + configType.getPivotHeight();
        double pivotZ = blockPos.getZ() + 0.5;

        float pitchRad = -getShootingPitch() * Mth.DEG_TO_RAD;
        float yawRad = getShootingYaw() * Mth.DEG_TO_RAD;

        double cosP = Mth.cos(pitchRad);
        double sinP = Mth.sin(pitchRad);

        double rx = barrelOffset.x;
        double ry = barrelOffset.y * cosP - barrelOffset.z * sinP;
        double rz = barrelOffset.y * sinP + barrelOffset.z * cosP;

        double cosY = Mth.cos(yawRad);
        double sinY = Mth.sin(yawRad);

        double wx = rx * cosY - rz * sinY;
        double wy = ry;
        double wz = rx * sinY + rz * cosY;

        return new Vec3(pivotX + wx, pivotY + wy, pivotZ + wz);
    }

    public Vec3 getShootingDirection()
    {
        return ModUtils.getDirectionFromPitchAndYaw(getShootingPitch(), getShootingYaw());
    }

    public float getShootingPitch()
    {
        return getXRot();
    }

    public float getShootingYaw()
    {
        return getYRot();
    }

    protected void parseBarrelOffset()
    {
        if (barrelParsed || configType == null)
            return;
        barrelParsed = true;

        String barrelStr = configType.getBarrel();
        if (StringUtils.isNotBlank(barrelStr))
        {
            String[] parts = barrelStr.split("\\s+");
            if (parts.length >= 5)
            {
                try
                {
                    float modelX = Float.parseFloat(parts[2]);
                    float modelY = Float.parseFloat(parts[3]);
                    float modelZ = Float.parseFloat(parts[4]);
                    barrelOffset = new Vec3(modelX / 16.0, -modelY / 16.0, modelZ / 16.0);
                }
                catch (NumberFormatException e)
                {
                    barrelOffset = Vec3.ZERO;
                }
            }
        }
    }
}