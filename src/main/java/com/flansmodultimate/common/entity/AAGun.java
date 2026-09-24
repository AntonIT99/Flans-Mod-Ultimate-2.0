package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.physics.ExternalImpulseTracker;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.ShotCooldown;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketBuffer;
import com.flansmodultimate.platform.entity.SpawnDataEntity;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import com.flansmodultimate.platform.network.PacketIO;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AAGun extends Entity implements SpawnDataEntity, IFlanEntity<AAGunType>, IMassiveEntity
{
    private boolean suppressRemovalDrops;
    public static final int RENDER_DISTANCE = 128;
    public static final float DEFAULT_HITBOX_SIZE = 2F;

    private static final double SENTRY_ORIGIN_Y_OFFSET = 1.5D;
    private static final double LEGACY_PLAYER_EYE_HEIGHT = 1.62D;
    private static final int TARGET_ACQUIRE_INTERVAL = 10;

    public static final String NBT_TYPE_NAME = "type";
    public static final String NBT_HEALTH = "health";
    public static final String NBT_GUN_YAW = "gun_yaw";
    public static final String NBT_GUN_PITCH = "gun_pitch";
    public static final String NBT_RELOAD_TIMER = "reload_timer";
    public static final String NBT_CURRENT_BARREL = "current_barrel";
    public static final String NBT_SHOTS_FIRED = "shots_fired";
    public static final String NBT_PLACER = "placer";
    public static final String NBT_AMMO = "ammo";
    public static final String NBT_AMMO_SLOT = "slot";
    public static final String NBT_AMMO_STACK = "stack";

    protected static final EntityDataAccessor<String> DATA_AA_TYPE = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Float> DATA_GUN_YAW = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_GUN_PITCH = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_AMMO_MASK = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_RELOAD_TIMER = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_CURRENT_BARREL = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_HEALTH = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Component> DATA_CURRENT_AMMO_NAME = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.COMPONENT);
    protected static final EntityDataAccessor<Integer> DATA_MAGAZINE_LEFT = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_MAGAZINE_SIZE = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.INT);

    protected AAGunType configType;
    protected String shortname = StringUtils.EMPTY;
    protected float shootDelay;
    protected int soundTimer;
    protected int currentBarrel;
    protected int ticksSinceUsed;
    protected int shotsFired;
    @Getter
    protected float prevGunYaw;
    @Getter
    protected float prevGunPitch;
    @Getter
    protected float[] barrelRecoil = new float[0];
    protected ItemStack[] ammo = new ItemStack[0];
    protected Vec3[] modelBarrelPivots = new Vec3[0];
    protected Vec3[] modelBarrelMuzzles = new Vec3[0];
    @Getter @Setter
    protected boolean shootKeyPressed;
    @Getter @Setter
    protected boolean prevShootKeyPressed;
    @Nullable
    protected UUID placerId;
    @Nullable
    protected Entity target;
    /** Tells the gun's own motion apart from outside pushes, which are weighed against its mass. Server only. */
    private final ExternalImpulseTracker externalImpulses = new ExternalImpulseTracker();

    public AAGun(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public AAGun(Level level, AAGunType type, double x, double y, double z, @Nullable Player placer)
    {
        super(FlansMod.aaGunEntity.get(), level);
        configType = type;
        setShortName(type.getShortName());
        initType();
        setPos(x, y, z);
        if (placer != null)
        {
            placerId = placer.getUUID();
            setGunYaw(placer.getYRot());
        }
        setGunPitch(0F);
        setYRot(getGunYaw());
        setXRot(getGunPitch());
    }

    @Override
    public AAGunType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof AAGunType aaGunType)
        {
            configType = aaGunType;
            initType();
        }
        return configType;
    }

    private void initType()
    {
        if (configType == null)
            return;

        setHealth(configType.getHealth());
        barrelRecoil = new float[configType.getNumBarrels()];

        ItemStack[] previousAmmo = ammo;
        ammo = new ItemStack[configType.getAmmoSlotCount()];
        Arrays.fill(ammo, ItemStack.EMPTY);
        System.arraycopy(previousAmmo, 0, ammo, 0, Math.min(previousAmmo.length, ammo.length));

        updateAmmoMask();
    }

    public void setModelBarrelOriginData(Vec3[] pivots, Vec3[] muzzles)
    {
        AAGunType type = getConfigType();
        if (type == null || pivots == null || muzzles == null || pivots.length != type.getNumBarrels() || muzzles.length != type.getNumBarrels())
            return;

        Vec3[] safePivots = new Vec3[type.getNumBarrels()];
        Vec3[] safeMuzzles = new Vec3[type.getNumBarrels()];
        for (int i = 0; i < type.getNumBarrels(); i++)
        {
            if (!isValidModelBarrelVector(pivots[i]) || !isValidModelBarrelVector(muzzles[i]))
                return;
            safePivots[i] = pivots[i];
            safeMuzzles[i] = muzzles[i];
        }

        modelBarrelPivots = safePivots;
        modelBarrelMuzzles = safeMuzzles;
    }

    private static boolean isValidModelBarrelVector(Vec3 vector)
    {
        return vector != null
            && Double.isFinite(vector.x)
            && Double.isFinite(vector.y)
            && Double.isFinite(vector.z)
            && Math.abs(vector.x) <= 512D
            && Math.abs(vector.y) <= 512D
            && Math.abs(vector.z) <= 512D;
    }

    @Override
    public String getShortName()
    {
        return entityData.get(DATA_AA_TYPE);
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(DATA_AA_TYPE, shortname);
    }

    public float getGunYaw()
    {
        return entityData.get(DATA_GUN_YAW);
    }

    public void setGunYaw(float yaw)
    {
        entityData.set(DATA_GUN_YAW, Mth.wrapDegrees(yaw));
    }

    public float getGunPitch()
    {
        return entityData.get(DATA_GUN_PITCH);
    }

    public void setGunPitch(float pitch)
    {
        entityData.set(DATA_GUN_PITCH, clampPitch(pitch));
    }

    public int getReloadTimer()
    {
        return entityData.get(DATA_RELOAD_TIMER);
    }

    public void setReloadTimer(int v)
    {
        entityData.set(DATA_RELOAD_TIMER, v);
    }

    public int getHealth()
    {
        return entityData.get(DATA_HEALTH);
    }

    public void setHealth(int value)
    {
        entityData.set(DATA_HEALTH, value);
    }

    public Component getCurrentAmmoName()
    {
        return entityData.get(DATA_CURRENT_AMMO_NAME);
    }

    public void setCurrentBarrel(int barrel)
    {
        AAGunType type = getConfigType();
        int barrelCount = type == null ? 0 : type.getNumBarrels();
        currentBarrel = barrelCount <= 0 ? 0 : Math.floorMod(barrel, barrelCount);
        entityData.set(DATA_CURRENT_BARREL, currentBarrel);
        updateCurrentAmmoName();
    }

    public int getAmmoMask()
    {
        return entityData.get(DATA_AMMO_MASK);
    }

    public int getAmmoSlotCount()
    {
        return ammo.length;
    }

    public ItemStack getAmmo(int slot)
    {
        return slot >= 0 && slot < ammo.length ? ammo[slot] : ItemStack.EMPTY;
    }

    public void setAmmo(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= ammo.length)
            return;
        ammo[slot] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
        updateAmmoMask();
    }

    public void setAmmoMask(int mask)
    {
        entityData.set(DATA_AMMO_MASK, mask);
    }

    public boolean hasAmmo(int barrelIndex)
    {
        AAGunType type = getConfigType();
        if (type == null)
            return false;

        int slot = ammoSlotForBarrel(barrelIndex);
        return slot >= 0 && slot < Integer.SIZE && (getAmmoMask() & (1 << slot)) != 0;
    }

    private void updateAmmoMask()
    {
        // The client never holds the ammunition stacks, only what the server
        // syncs about them. Recomputing here on the client - initType does, when
        // a loaded gun comes back into view - would publish "no ammo" over the
        // real state until the server next changed it.
        if (level().isClientSide)
            return;
        int mask = 0;
        for (int i = 0; i < Math.min(ammo.length, Integer.SIZE); i++)
        {
            if (!ammo[i].isEmpty())
                mask |= (1 << i);
        }
        setAmmoMask(mask);
        updateCurrentAmmoName();
        updateMagazineState();
    }

    private void updateCurrentAmmoName()
    {
        int currentSlot = ammoSlotForBarrel(getCurrentBarrelIndex());
        if (currentSlot >= 0 && currentSlot < ammo.length && !ammo[currentSlot].isEmpty())
        {
            entityData.set(DATA_CURRENT_AMMO_NAME, ammo[currentSlot].getHoverName());
            return;
        }
        for (ItemStack stack : ammo)
        {
            if (!stack.isEmpty())
            {
                entityData.set(DATA_CURRENT_AMMO_NAME, stack.getHoverName());
                return;
            }
        }
        entityData.set(DATA_CURRENT_AMMO_NAME, Component.empty());
    }

    /**
     * Rounds the whole mounting still has, and what it holds when full. Every
     * barrel reloads together, so the crew is shown the mounting's total rather
     * than whichever barrel happens to be next.
     */
    private void updateMagazineState()
    {
        int left = 0;
        for (ItemStack stack : ammo)
            left += ShootableItem.getTotalRounds(stack);

        // The capacity to count down from is whatever went aboard at the last
        // restock, which is the only figure that means anything here: the gun
        // reloads when every barrel is dry, so a mounting loaded with three
        // forty-round drums is forty short after forty rounds, not after one item.
        if (left > entityData.get(DATA_MAGAZINE_LEFT))
            entityData.set(DATA_MAGAZINE_SIZE, left);
        if (entityData.get(DATA_MAGAZINE_LEFT) != left)
            entityData.set(DATA_MAGAZINE_LEFT, left);
    }

    public int getMagazineLeft()
    {
        return entityData.get(DATA_MAGAZINE_LEFT);
    }

    public int getMagazineSize()
    {
        return entityData.get(DATA_MAGAZINE_SIZE);
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq)
    {
        double r = getRenderDistance();
        return distSq < r * r;
    }

    private static int getRenderDistance()
    {
        ModClientConfig config = ModClientConfig.get();
        return config == null ? RENDER_DISTANCE : config.aaGunRenderDistance;
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling()
    {
        double r = getRenderDistance();
        return new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r);
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        return ModUtils.getItemStack(getConfigType()).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData()
    {
        defineEntityData(new SynchedDataDefinition(entityData));
    }

    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_AA_TYPE, StringUtils.EMPTY);
        data.define(DATA_GUN_YAW, 0F);
        data.define(DATA_GUN_PITCH, 0F);
        data.define(DATA_AMMO_MASK, 0);
        data.define(DATA_RELOAD_TIMER, 0);
        data.define(DATA_CURRENT_BARREL, 0);
        data.define(DATA_HEALTH, 0);
        data.define(DATA_CURRENT_AMMO_NAME, Component.empty());
        data.define(DATA_MAGAZINE_LEFT, 0);
        data.define(DATA_MAGAZINE_SIZE, 0);
    }

    @Override
    public void writeSpawnData(PacketBuffer buf)
    {
        buf.writeUtf(getShortName());
        buf.writeFloat(getGunYaw());
        buf.writeFloat(getGunPitch());
        buf.writeInt(getAmmoMask());
        buf.writeInt(getReloadTimer());
        buf.writeInt(getCurrentBarrelIndex());
        buf.writeInt(getHealth());
        PacketIO.writeComponent(buf, getCurrentAmmoName());
        buf.writeInt(getMagazineLeft());
        buf.writeInt(getMagazineSize());
    }

    @Override
    public void readSpawnData(PacketBuffer buf)
    {
        try
        {
            setShortName(buf.readUtf());
            if (InfoType.getInfoType(shortname) instanceof AAGunType aaGunType)
                configType = aaGunType;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown AA gun type {}, discarding.", shortname);
                discard();
                return;
            }
            initType();
            setGunYaw(buf.readFloat());
            setGunPitch(buf.readFloat());
            setAmmoMask(buf.readInt());
            setReloadTimer(buf.readInt());
            setCurrentBarrel(buf.readInt());
            setHealth(buf.readInt());
            entityData.set(DATA_CURRENT_AMMO_NAME, PacketIO.readComponent(buf));
            entityData.set(DATA_MAGAZINE_LEFT, buf.readInt());
            entityData.set(DATA_MAGAZINE_SIZE, buf.readInt());
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

        if (InfoType.getInfoType(shortname) instanceof AAGunType aaGunType)
            configType = aaGunType;
        else
        {
            discard();
            return;
        }

        initType();
        setHealth(tag.contains(NBT_HEALTH, Tag.TAG_INT) ? tag.getInt(NBT_HEALTH) : configType.getHealth());
        setGunYaw(tag.getFloat(NBT_GUN_YAW));
        setGunPitch(tag.getFloat(NBT_GUN_PITCH));
        setReloadTimer(tag.getInt(NBT_RELOAD_TIMER));
        setCurrentBarrel(tag.getInt(NBT_CURRENT_BARREL));
        shotsFired = tag.getInt(NBT_SHOTS_FIRED);
        if (tag.hasUUID(NBT_PLACER))
            placerId = tag.getUUID(NBT_PLACER);

        if (tag.contains(NBT_AMMO, Tag.TAG_LIST))
        {
            ListTag ammoList = tag.getList(NBT_AMMO, Tag.TAG_COMPOUND);
            for (int i = 0; i < ammoList.size(); i++)
            {
                CompoundTag ammoTag = ammoList.getCompound(i);
                int slot = ammoTag.getInt(NBT_AMMO_SLOT);
                if (slot >= 0 && slot < ammo.length && ammoTag.contains(NBT_AMMO_STACK, Tag.TAG_COMPOUND))
                    ammo[slot] = ItemStackData.parse(level().registryAccess(), ammoTag.getCompound(NBT_AMMO_STACK));
            }
        }
        updateAmmoMask();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        AAGunType type = getConfigType();
        if (type == null)
            return;

        tag.putString(NBT_TYPE_NAME, getShortName());
        tag.putInt(NBT_HEALTH, getHealth());
        tag.putFloat(NBT_GUN_YAW, getGunYaw());
        tag.putFloat(NBT_GUN_PITCH, getGunPitch());
        tag.putInt(NBT_RELOAD_TIMER, getReloadTimer());
        tag.putInt(NBT_CURRENT_BARREL, getCurrentBarrelIndex());
        tag.putInt(NBT_SHOTS_FIRED, shotsFired);
        if (placerId != null)
            tag.putUUID(NBT_PLACER, placerId);

        ListTag ammoList = new ListTag();
        for (int i = 0; i < ammo.length; i++)
        {
            if (ammo[i].isEmpty())
                continue;

            CompoundTag ammoTag = new CompoundTag();
            CompoundTag stackTag = new CompoundTag();
            ItemStackData.save(ammo[i], level().registryAccess(), stackTag);
            ammoTag.putInt(NBT_AMMO_SLOT, i);
            ammoTag.put(NBT_AMMO_STACK, stackTag);
            ammoList.add(ammoTag);
        }
        tag.put(NBT_AMMO, ammoList);
    }

    @Override
    public void remove(@NotNull RemovalReason reason)
    {
        try
        {
            Level level = level();
            AAGunType type = getConfigType();

            if (!suppressRemovalDrops && !level.isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK && type != null && FlansMod.teamsManager.getWeaponDrops() != TeamsManager.EnumWeaponDrop.NONE)
            {
                if (type.isDropThis())
                    spawnAtLocation(ModUtils.getItemStack(type).orElse(ItemStack.EMPTY), 0F);
                for (ItemStack stack : ammo)
                {
                    if (!stack.isEmpty())
                        spawnAtLocation(stack.copy(), 0.5F);
                }
            }
        }
        catch (Exception e)
        {
            FlansMod.log.error("Error removing AA gun entity", e);
        }

        super.remove(reason);
    }

    /** Removes this gun for an administrative cleanup without creating item drops. */
    public void discardWithoutDrops()
    {
        suppressRemovalDrops = true;
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        Entity attacker = source.getEntity();
        Entity gunner = getFirstPassenger();

        if (attacker == gunner)
            return true;
        if (gunner != null)
            return gunner.hurt(source, amount);

        if (attacker instanceof Player && FlansMod.teamsManager.isCanBreakGuns())
        {
            discard();
            return true;
        }

        setHealth(getHealth() - (int) amount);
        if (getHealth() <= 0)
            discard();

        return true;
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        AAGunType type = getConfigType();
        if (type == null)
            return InteractionResult.PASS;

        Level level = level();
        Entity gunner = getFirstPassenger();

        if (player != gunner && gunner != null)
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (player == gunner)
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }

        if (!type.isSentry())
        {
            if (player.getVehicle() != null)
                player.stopRiding();
            player.startRiding(this, true);
        }

        reloadGun(level, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        AAGunType type = getConfigType();
        if (!(passenger instanceof Player) || type == null)
            return;

        Vec3 gunnerSeatPosition = getGunnerSeatPosition();
        move.accept(passenger, gunnerSeatPosition.x, gunnerSeatPosition.y, gunnerSeatPosition.z);
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.fallDistance = 0F;
    }

    @Override
    @NotNull
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger)
    {
        float yawRad = getGunYaw() * Mth.DEG_TO_RAD;
        Vec3 preferred = position().add(1.5D * Mth.sin(yawRad), 0D, -1.5D * Mth.cos(yawRad));
        AABB movedBB = passenger.getBoundingBox().move(preferred.subtract(passenger.position()));
        if (level().noCollision(passenger, movedBB))
            return preferred;
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public void tick()
    {
        super.tick();

        AAGunType type = getConfigType();
        if (type == null)
        {
            discard();
            return;
        }

        prevGunYaw = getGunYaw();
        prevGunPitch = getGunPitch();

        Entity passenger = getFirstPassenger();
        if (passenger instanceof LivingEntity living)
            updateAimFromPassenger(living);

        for (int i = 0; i < barrelRecoil.length; i++)
            barrelRecoil[i] *= 0.9F;

        if (level().isClientSide)
        {
            ClientHooks.GUN.tickAAGun(this);
            return;
        }

        serverTick(level());
    }

    private void serverTick(Level level)
    {
        AAGunType type = getConfigType();
        if (type == null)
        {
            discard();
            return;
        }

        ticksSinceUsed++;
        if (getFirstPassenger() != null)
            ticksSinceUsed = 0;

        int aaLife = FlansMod.teamsManager.getAaLife();
        if (aaLife > 0 && ticksSinceUsed > aaLife * 20)
        {
            discard();
            return;
        }

        shootDelay = ShotCooldown.tick(shootDelay);
        if (soundTimer > 0)
            soundTimer--;
        if (getReloadTimer() > 0)
            setReloadTimer(getReloadTimer() - 1);

        clearSpentAmmo();

        if (type.isSentry())
            updateSentryTarget(level);
        else
            target = null;

        if (getFirstPassenger() instanceof Player player)
        {
            reloadGun(level, player);
            fireGun(level, player, true);
        }
        else if (target != null)
        {
            fireGun(level, getPlacer(level).orElse(null), false);
        }

        absorbExternalImpulses();
        applyMotion();
        externalImpulses.settle(getDeltaMovement());
    }

    /** Weighs every velocity change the gun did not make itself since its last tick against its mass. */
    private void absorbExternalImpulses()
    {
        Vec3 current = getDeltaMovement();
        if (ModCommonConfig.forceLegacyVehicleKnockback())
        {
            externalImpulses.settle(current);
            return;
        }
        Vec3 absorbed = externalImpulses.absorb(current, getImpulseMassKg(),
            ModCommonConfig.vehicleKnockbackReferenceMassKg());
        if (absorbed != current)
            setDeltaMovement(absorbed);
    }

    @Override
    public double getImpulseMassKg()
    {
        AAGunType type = getConfigType();
        return type == null ? ModCommonConfig.fallbackAAGunMassKg() : type.getImpulseMass().massKg();
    }

    @Override
    public void applyResolvedImpulse(@NotNull Vec3 impulse)
    {
        setDeltaMovement(getDeltaMovement().add(impulse));
        externalImpulses.addResolvedImpulse(impulse);
    }

    private void updateAimFromPassenger(LivingEntity passenger)
    {
        setGunYaw(passenger.getYRot());
        setGunPitch(passenger.getXRot());
        setYRot(getGunYaw());
        setXRot(getGunPitch());
    }

    private void updateSentryTarget(Level level)
    {
        if (target != null && (!target.isAlive() || !isValidTarget(target)))
            target = null;
        if (target == null && tickCount % TARGET_ACQUIRE_INTERVAL == 0)
            target = findValidTarget(level).orElse(null);
        if (target != null)
            aimAtTarget(target);
    }

    private Optional<Entity> findValidTarget(Level level)
    {
        double range = getConfigType().getTargetRange();
        return level.getEntities(this, getBoundingBox().inflate(range), this::isValidTarget)
            .stream()
            .min(Comparator.comparingDouble(this::distanceToSqr));
    }

    private boolean isValidTarget(Entity candidate)
    {
        AAGunType type = getConfigType();
        if (type == null || candidate == this || !candidate.isAlive() || candidate == getFirstPassenger())
            return false;
        if (placerId != null && placerId.equals(candidate.getUUID()))
            return false;
        if (candidate.distanceToSqr(this) > type.getTargetRange() * type.getTargetRange())
            return false;
        if (!canSeeTarget(candidate))
            return false;

        if (type.isTargetPlayers() && candidate instanceof Player player)
            return !player.isSpectator();
        if (type.isTargetMobs() && candidate instanceof Enemy)
            return true;
        if (type.isTargetPlanes() && (candidate instanceof Plane || ModUtils.isPlaneLike(candidate)))
            return true;
        if (type.isTargetVehicles() && (candidate instanceof Vehicle || ModUtils.isVehicleLike(candidate)))
            return true;
        return type.isTargetMechas() && candidate instanceof Mecha;
    }

    private void aimAtTarget(Entity targetEntity)
    {
        Vec3 origin = position().add(0D, 1.5D, 0D);
        Vec3 targetPos = targetEntity instanceof LivingEntity living ? living.getEyePosition() : targetEntity.position().add(0D, targetEntity.getBbHeight() * 0.5D, 0D);
        Vec3 direction = targetPos.subtract(origin);
        float targetYaw = ModUtils.getYawFromDirection(direction);
        float targetPitch = ModUtils.getPitchFromDirection(direction);

        if (getConfigType().isCanShootHomingMissile())
        {
            setGunYaw(targetYaw);
            setGunPitch(targetPitch);
        }
        else
        {
            setGunYaw(Mth.rotLerp(0.25F, getGunYaw(), targetYaw));
            setGunPitch(Mth.lerp(0.25F, getGunPitch(), targetPitch));
        }
        setYRot(getGunYaw());
        setXRot(getGunPitch());
    }

    private boolean canSeeTarget(Entity candidate)
    {
        Vec3 start = position().add(0D, 1.5D, 0D);
        Vec3 end = candidate instanceof LivingEntity living ? living.getEyePosition() : candidate.position().add(0D, candidate.getBbHeight() * 0.5D, 0D);
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void fireGun(Level level, @Nullable LivingEntity attacker, boolean requireInput)
    {
        AAGunType type = getConfigType();
        if (type == null || !ShotCooldown.isReady(shootDelay) || getReloadTimer() > 0)
            return;
        if (requireInput && !shootKeyPressed)
            return;
        if (!requireInput && target == null)
            return;

        // A delay shorter than a tick puts more than one volley into this tick.
        // Charging the delay rather than assigning it carries the remainder, so a
        // rate that does not divide evenly into ticks stays honest over time.
        while (ShotCooldown.isReady(shootDelay))
        {
            if (!fireVolley(level, attacker, requireInput))
                return;
            shootDelay = ShotCooldown.charge(shootDelay, type.getShootDelay());
        }
    }

    /**
     * One pass over the barrels. Returns false when the gun fired nothing, either
     * because no barrel had ammunition or because the gun destroyed itself.
     */
    private boolean fireVolley(Level level, @Nullable LivingEntity attacker, boolean requireInput)
    {
        AAGunType type = getConfigType();
        boolean attempted = false;
        for (int barrel = 0; barrel < type.getNumBarrels(); barrel++)
        {
            if (type.isFireAlternately() && barrel != getCurrentBarrelIndex())
                continue;

            int slot = ammoSlotForBarrel(barrel);
            if (slot < 0 || slot >= ammo.length || ammo[slot].isEmpty())
                continue;

            attempted = true;
            if (fireBarrel(level, attacker, barrel, slot, !requireInput)
                && type.getCountExplodeAfterShoot() != -1 && shotsFired >= type.getCountExplodeAfterShoot())
            {
                discard();
                return false;
            }
        }

        if (attempted)
            setCurrentBarrel(getCurrentBarrelIndex() + 1);
        return attempted;
    }

    private boolean fireBarrel(Level level, @Nullable LivingEntity attacker, int barrel, int slot, boolean sentryShot)
    {
        AAGunType type = getConfigType();
        ItemStack ammoStack = ammo[slot];
        if (type == null || !(ammoStack.getItem() instanceof ShootableItem shootableItem) || !(shootableItem.getConfigType() instanceof BulletType bulletType))
            return false;

        // The AA gun declares no velocity of its own, so it hands over the default as a fallback and
        // lets the ammunition's MuzzleVelocity win.
        FireableGun fireableGun = new FireableGun(type, type.getDamage(), type.getBulletSpread(),
            BulletType.DEFAULT_BULLET_SPEED, type.getSpreadPattern());
        fireableGun.applyAmmunition(bulletType);
        FiredShot firedShot = new FiredShot(fireableGun, bulletType, this, attacker, ShootableItem.getRoundsFired(ammoStack));

        Vec3 shootingDir = getShootingDirection();
        Vec3 barrelOrigin = getBarrelOrigin(barrel, sentryShot);

        ShootingHelper.fireGun(level, firedShot, type.getNumBullets(), barrelOrigin, shootingDir, () -> damageAmmo(slot));

        barrelRecoil[barrel] = type.getRecoil() * bulletType.getRecoilMultiplier();
        shotsFired++;

        if (soundTimer <= 0 && StringUtils.isNotBlank(type.getShootSound()))
        {
            PacketPlaySound.sendSoundPacket(this, type.getGunSoundRange(), type.getShootSound(), true);
            soundTimer = type.getShootSoundLength();
        }

        return true;
    }

    private void damageAmmo(int slot)
    {
        if (slot < 0 || slot >= ammo.length || ammo[slot].isEmpty())
            return;

        ItemStack stack = ammo[slot];
        if (stack.getItem() instanceof ShootableItem)
        {
            // Spending a round is the item's own business: a magazine or belt
            // holding several rounds has to move on to the next item of the stack
            // when the current one runs dry, rather than throwing the rest away.
            if (!ShootableItem.consumeRound(stack) || !ShootableItem.hasRoundsLeft(stack))
                ammo[slot] = ItemStack.EMPTY;
        }
        else
        {
            stack.shrink(1);
            if (stack.isEmpty())
                ammo[slot] = ItemStack.EMPTY;
        }
        updateAmmoMask();
    }

    private void reloadGun(Level level, Player player)
    {
        AAGunType type = getConfigType();
        if (level.isClientSide || type == null || getReloadTimer() > 0 || !isFullySpent())
            return;

        boolean loadedAny = false;
        float reloadFactor = 1F;
        for (int i = 0; i < ammo.length; i++)
        {
            if (!ammo[i].isEmpty())
                continue;

            int slot = findAmmo(player);
            if (slot < 0)
                break;

            ItemStack loaded = takeAmmoStack(player, slot);
            if (!loaded.isEmpty())
            {
                ammo[i] = loaded;
                loadedAny = true;
                // A slower round to load holds up the whole reload, so the
                // heaviest factor among what went in wins.
                if (loaded.getItem() instanceof ShootableItem shootableItem)
                    reloadFactor = Math.max(reloadFactor, shootableItem.getConfigType().getReloadTimeMultiplier());
            }
        }

        if (loadedAny)
        {
            updateAmmoMask();
            // Reloading never lets the gun outrun its own rate of fire, so the wait
            // after the round that emptied a barrel is the longer of the two.
            setReloadTimer(Mth.ceil(Math.max(type.getReloadTime() * reloadFactor, type.getShootDelay())));
            if (StringUtils.isNotBlank(type.getReloadSound()))
                PacketPlaySound.sendSoundPacket(this, type.getReloadSoundRange(), type.getReloadSound(), false);
        }
    }

    /**
     * Whether every barrel has run dry.
     *
     * <p>A multi-barrel mount is worked as one weapon, so the crew reloads it in
     * one go once nothing is left to fire. Reloading the moment a single barrel
     * emptied would stand the whole mount down while its other barrels still had
     * rounds in them, which is the opposite of what a second barrel is for.
     */
    private boolean isFullySpent()
    {
        for (ItemStack stack : ammo)
        {
            if (ShootableItem.hasRoundsLeft(stack))
                return false;
        }
        return true;
    }

    private int findAmmo(Player player)
    {
        AAGunType type = getConfigType();
        if (type == null)
            return -1;

        Inventory inv = player.getInventory();
        ItemStack selected = inv.getItem(inv.selected);
        if (type.isAmmo(selected))
            return inv.selected;

        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (type.isAmmo(inv.getItem(i)))
                return i;
        }
        return -1;
    }

    private static ItemStack takeAmmoStack(Player player, int slot)
    {
        Inventory inv = player.getInventory();
        ItemStack source = inv.getItem(slot);
        if (source.isEmpty())
            return ItemStack.EMPTY;

        ItemStack loaded = source.copy();
        loaded.setCount(1);

        if (!player.getAbilities().instabuild)
        {
            source.shrink(1);
            if (source.isEmpty())
                inv.setItem(slot, ItemStack.EMPTY);
            inv.setChanged();
        }

        return loaded;
    }

    private void clearSpentAmmo()
    {
        boolean changed = false;
        for (int i = 0; i < ammo.length; i++)
        {
            ItemStack stack = ammo[i];
            if (stack.isEmpty())
                continue;

            boolean spent;
            if (stack.getItem() instanceof ShootableItem shootableItem && shootableItem.getConfigType().getRoundsPerItem() > 1)
                spent = ShootableItem.getRoundsRemaining(stack) <= 0;
            else
                spent = stack.isEmpty();

            if (spent)
            {
                ammo[i] = ItemStack.EMPTY;
                changed = true;
            }
        }
        if (changed)
            updateAmmoMask();
    }

    private void applyMotion()
    {
        Vec3 motion = getDeltaMovement();
        if (!onGround())
            motion = motion.add(0D, -9.8D / 400D, 0D);

        move(MoverType.SELF, motion);
        if (onGround())
            setDeltaMovement(motion.x * 0.5D, 0D, motion.z * 0.5D);
        else
            setDeltaMovement(motion.multiply(0.5D, 0.98D, 0.5D));
    }

    private int ammoSlotForBarrel(int barrel)
    {
        AAGunType type = getConfigType();
        if (type == null)
            return -1;
        return type.isShareAmmo() ? 0 : barrel;
    }

    public int getCurrentBarrelIndex()
    {
        AAGunType type = getConfigType();
        int barrelCount = type == null ? 0 : type.getNumBarrels();
        return barrelCount <= 0 ? 0 : Math.floorMod(entityData.get(DATA_CURRENT_BARREL), barrelCount);
    }

    public Vec3 getGunnerSeatPosition()
    {
        AAGunType type = getConfigType();
        if (type == null)
            return position();

        // Map legacy position to actual position
        double x = type.getGunnerZ() / 16D;
        double y = type.getGunnerY() / 16D;
        double z = -type.getGunnerX() / 16D;

        double yaw = -getGunYaw() * Mth.DEG_TO_RAD;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double x2 = x * cosYaw + z * sinYaw;
        double z2 = -x * sinYaw + z * cosYaw;

        // 1.7.10 placed the local player's eye point (feet + 1.62) at the gunner position.
        return new Vec3(getX() + x2, getY() + y - LEGACY_PLAYER_EYE_HEIGHT, getZ() + z2);
    }

    public Vec3 getBarrelOrigin(int barrel, boolean sentryShot)
    {
        AAGunType type = getConfigType();
        if (type == null || barrel < 0 || barrel >= type.getNumBarrels())
            return position();

        if (barrel < modelBarrelPivots.length && barrel < modelBarrelMuzzles.length)
            return position().add(transformModelBarrelOffset(modelBarrelPivots[barrel], modelBarrelMuzzles[barrel]));

        Vec3 origin = position().add(transformLegacyConfigBarrelOffset(type, barrel));
        return sentryShot && type.isSentry() ? origin.add(0D, SENTRY_ORIGIN_Y_OFFSET, 0D) : origin;
    }

    private Vec3 transformModelBarrelOffset(Vec3 pivot, Vec3 muzzle)
    {
        double pitch = -getGunPitch() * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double modelX = pivot.x + muzzle.x * cosPitch - muzzle.y * sinPitch;
        double modelY = pivot.y + muzzle.x * sinPitch + muzzle.y * cosPitch;
        double modelZ = pivot.z + muzzle.z;

        double yaw = (270D - getGunYaw()) * Mth.DEG_TO_RAD;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        double x = modelX * cosYaw + modelZ * sinYaw;
        double z = -modelX * sinYaw + modelZ * cosYaw;

        return new Vec3(x / 16D, modelY / 16D, z / 16D);
    }

    private Vec3 transformLegacyConfigBarrelOffset(AAGunType type, int barrel)
    {
        // Map legacy position to actual position
        double barrelX = type.getBarrelZ()[barrel];
        double barrelY = type.getBarrelY()[barrel];
        double barrelZ = -type.getBarrelX()[barrel];

        double x = (barrelX - barrelZ) / 16D;
        double y = barrelY / 16D;
        double z = (barrelX + barrelZ) / 16D;

        return rotate(x, y, z, getGunPitch(), getGunYaw());
    }

    public Vec3 rotate(double x, double y, double z, double gunPitch, double gunYaw)
    {
        double yaw = 180D - gunYaw * Mth.DEG_TO_RAD;
        double pitch = gunPitch * Mth.DEG_TO_RAD;

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double newX = x * cosYaw + (y * sinPitch + z * cosPitch) * sinYaw;
        double newY = y * cosPitch - z * sinPitch;
        double newZ = -x * sinYaw + (y * sinPitch + z * cosPitch) * cosYaw;

        return new Vec3(newX, newY, newZ);
    }

    public Vec3 getShootingDirection()
    {
        return ModUtils.getDirectionFromPitchAndYaw(getGunPitch(), getGunYaw());
    }

    private float clampPitch(float pitch)
    {
        AAGunType type = configType;
        if (type == null && InfoType.getInfoType(getShortName()) instanceof AAGunType aaGunType)
            type = aaGunType;
        if (type == null)
            return pitch;

        float top = -Math.abs(type.getTopViewLimit());
        float bottom = type.getBottomViewLimit();
        if (top > bottom)
        {
            float tmp = top;
            top = bottom;
            bottom = tmp;
        }
        return Mth.clamp(pitch, top, bottom);
    }

    private Optional<LivingEntity> getPlacer(Level level)
    {
        if (placerId == null)
            return Optional.empty();
        return Optional.ofNullable(level.getPlayerByUUID(placerId)).map(LivingEntity.class::cast);
    }
}
