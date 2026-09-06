package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.driveables.EnumMechaToolType;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.MechaPhysics;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.inventory.MechaInventoryMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.event.GunFiredEvent;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mecha runtime with server-owned locomotion, addon effects and hand tools. */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Mecha extends Driveable
{
    private static final int JUMP_COOLDOWN = 20;
    private static final String NBT_LEG_YAW = "LegsYaw";
    private static final EntityDataAccessor<Float> DATA_LEG_YAW =
        SynchedEntityData.defineId(Mecha.class, EntityDataSerializers.FLOAT);

    @Getter protected float legSwing;
    @Getter protected float prevLegSwing;
    @Getter protected float legYaw;
    @Getter protected float prevLegYaw;
    @Getter protected float shieldEnergy;
    private int jumpDelay;
    private int stompDelay;
    private int shieldRechargeDelay;
    private int lastShieldCapacity = -1;
    private boolean hipsStateInitialized;
    private boolean lastHipsIntact;
    private boolean legYawInitialized;
    private final int[] toolCooldown = new int[2];
    private final int[] handGunCooldown = new int[2];
    private final int[] handGunHeldTicks = new int[2];
    private final int[] handGunBurstRemaining = new int[2];
    @Nullable private BlockPos breakingBlock;
    private float breakingProgress;

    public Mecha(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Mecha(Level level, MechaType type, double x, double y, double z, float yaw,
                 @Nullable Player placer, ItemStack sourceStack)
    {
        super(FlansMod.mechaEntity.get(), level, type, x, y, z, yaw, placer, sourceStack);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(DATA_LEG_YAW, 0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_LEG_YAW, Tag.TAG_ANY_NUMERIC))
        {
            legYaw = prevLegYaw = Mth.wrapDegrees(tag.getFloat(NBT_LEG_YAW));
            entityData.set(DATA_LEG_YAW, legYaw);
            legYawInitialized = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putFloat(NBT_LEG_YAW, legYawInitialized ? legYaw : getYaw());
    }

    @Nullable
    public MechaType getMechaType()
    {
        return getConfigType() instanceof MechaType type ? type : null;
    }

    @Override
    protected void tickDriveable()
    {
        MechaType type = getMechaType();
        if (type == null)
            return;
        setMaxUpStep(Mth.clamp(type.getStepHeight(), 0F, 8F));
        boolean hipsIntact = isPartIntact(EnumDriveablePart.HIPS);
        if (!hipsStateInitialized || hipsIntact != lastHipsIntact)
        {
            hipsStateInitialized = true;
            lastHipsIntact = hipsIntact;
            refreshDimensions();
        }
        if (jumpDelay > 0)
            --jumpDelay;
        if (stompDelay > 0)
            --stompDelay;
        for (int index = 0; index < toolCooldown.length; index++)
        {
            if (toolCooldown[index] > 0)
                --toolCooldown[index];
            if (handGunCooldown[index] > 0)
                --handGunCooldown[index];
        }

        updateAddonSystems();
        int input = getInputMask();
        float forwardInput = axis(input, DriveableInput.FORWARD, DriveableInput.BACKWARD);
        float sideInput = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        Vec3 intent = MechaPhysics.movementIntent(
            MechaPhysics.driverMovementYaw(getYaw() + getTurretYaw()), forwardInput, sideInput);
        boolean walking = intent.lengthSqr() > 0.01D;
        boolean canMove = getControllingEntity() != null && isEngineActive() && hasFuelForMovement()
            && isPartIntact(EnumDriveablePart.HIPS);
        double moveSpeed = MechaPhysics.movementSpeed(type.getMoveSpeed(), getEngineSpeed(), speedMultiplier());
        Vec3 current = getDeltaMovement();
        float rocketPower = Mth.clamp(jetPackPower(), 0.1F, 8F);
        MechaItemType rocket = rocketPack();
        Vec3 velocity = new Vec3(0D, current.y, 0D);

        boolean rocketThrust = false;
        if (canMove && DriveableInput.isDown(input, DriveableInput.ASCEND))
        {
            if (onGround() && jumpDelay <= 0)
            {
                velocity = velocity.add(0D, type.getJumpVelocity(), 0D);
                jumpDelay = JUMP_COOLDOWN;
                consumeFuel(20F);
            }
            else if (!onGround() && rocket != null && hasFuelForAddon(10F * rocketPower))
            {
                rocketThrust = true;
                velocity = velocity.multiply(1D, 0.95D, 1D).add(0D, 0.07D * rocketPower, 0D);
                fallDistance = 0F;
                consumeAddonFuel(10F * rocketPower);
                if (toolCooldown[0] <= 0 && StringUtils.isNotBlank(rocket.getSoundEffect()))
                {
                    PacketPlaySound.sendSoundPacket(this, 64D, rocket.getSoundEffect(), false);
                    toolCooldown[0] = Math.max(1, Mth.ceil(rocket.getSoundTime()));
                }
            }
        }

        boolean boostedAirMovement = canMove && walking && !onGround() && rocket != null
            && hasFuelForAddon(10F * rocketPower + engineFuelPerTick());
        if (boostedAirMovement)
            moveSpeed *= rocketPower;
        Vec3 desired = canMove ? intent.scale(moveSpeed) : Vec3.ZERO;
        // 1.7.10 rebuilt horizontal motion from the current input every tick.
        velocity = new Vec3(desired.x, velocity.y, desired.z);

        if (!rocketThrust && isInWater() && shouldFloat())
            velocity = velocity.multiply(0.89D, 0.89D, 0.89D).add(0D, 0.06D, 0D);
        else
            velocity = applyGravityAndBuoyancy(velocity, 0.04D);
        double descent = velocity.y;
        moveWithCollisions(velocity);
        if (verticalCollision && descent < -0.55D)
            handleLanding(type, descent);
        fallDistance = descent < 0D ? fallDistance + (float) -descent : 0F;

        setThrottle(walking && canMove ? MechaPhysics.throttle(forwardInput, sideInput) : 0F);
        updateLegFacing(type, intent, walking && canMove);
        updateLegAnimation(type, walking && canMove);
        useHandTool(EnumMechaSlotType.LEFT_TOOL, true, DriveableInput.isDown(input, DriveableInput.PRIMARY_FIRE));
        useHandTool(EnumMechaSlotType.RIGHT_TOOL, false, DriveableInput.isDown(input, DriveableInput.SECONDARY_FIRE));
        if (walking && canMove)
        {
            consumeFuel(20F);
            if (boostedAirMovement)
                consumeAddonFuel(10F * rocketPower);
        }
    }

    @Override
    protected void tickClientDriveable()
    {
        MechaType type = getMechaType();
        if (type != null)
        {
            setMaxUpStep(Mth.clamp(type.getStepHeight(), 0F, 8F));
            float forwardInput = axis(getInputMask(), DriveableInput.FORWARD, DriveableInput.BACKWARD);
            float sideInput = axis(getInputMask(), DriveableInput.RIGHT, DriveableInput.LEFT);
            Vec3 intent = MechaPhysics.movementIntent(
                MechaPhysics.driverMovementYaw(getYaw() + getTurretYaw()), forwardInput, sideInput);
            boolean walking = Math.abs(getThrottle()) > 0.01F && intent.lengthSqr() > 0.01D;
            updateLegFacing(type, intent, walking);
            updateLegAnimation(type, walking);
        }
    }

    private void updateLegFacing(MechaType type, Vec3 intent, boolean walking)
    {
        prevLegYaw = legYaw;
        if (level().isClientSide)
        {
            float syncedYaw = entityData.get(DATA_LEG_YAW);
            if (!legYawInitialized)
                prevLegYaw = syncedYaw;
            legYaw = syncedYaw;
            legYawInitialized = true;
            return;
        }
        if (!legYawInitialized)
        {
            legYaw = Mth.wrapDegrees(getYaw());
            prevLegYaw = legYaw;
            legYawInitialized = true;
        }
        if (walking)
        {
            float target = MechaPhysics.movementYaw(intent, legYaw);
            legYaw = MechaPhysics.approachYaw(legYaw, target, type.getRotateSpeed());
        }
        entityData.set(DATA_LEG_YAW, legYaw);
    }

    @Override
    public void acceptInput(@NotNull ServerPlayer player, int mask, float aimYaw, float aimPitch,
                            float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        float oldBodyYaw = getYaw();
        float relativeAimYaw = MechaPhysics.relativeAimYaw(oldBodyYaw, aimYaw);
        super.acceptInput(player, mask, relativeAimYaw, aimPitch, flightPitch, flightRoll, mouseControl, sequence);
        MechaType type = getMechaType();
        Seat seat = getSeat(player);
        if (type == null || seat == null || !seat.isDriverSeat())
            return;

        // 1.7.10 consumed driver look yaw into the mecha's torso axes. Keeping it
        // as turret-relative yaw leaves the complete chassis fixed in world space.
        float relativeYaw = getTurretYaw();
        float bodyYaw = Mth.wrapDegrees(oldBodyYaw + relativeYaw);
        if (type.isLimitHeadTurn())
        {
            float limit = Mth.clamp(Math.abs(type.getLimitHeadTurnValue()), 0F, 180F);
            bodyYaw = Mth.wrapDegrees(legYaw + Mth.clamp(Mth.wrapDegrees(bodyYaw - legYaw), -limit, limit));
        }
        float consumedYaw = Mth.wrapDegrees(bodyYaw - oldBodyYaw);
        float pitch = getTurretPitch();
        setOrientation(bodyYaw, 0F, 0F);
        seat.consumeAimYaw(consumedYaw);
        setTurretAim(Mth.wrapDegrees(relativeYaw - consumedYaw), pitch);
    }

    private void updateLegAnimation(MechaType type, boolean walking)
    {
        prevLegSwing = legSwing;
        if (!walking)
        {
            legSwing *= 0.75F;
            return;
        }
        float previousPhase = legSwing;
        float increment = type.getLegAnimSpeed() > 0F ? type.getLegAnimSpeed() : 0.12F;
        legSwing += increment * Math.max(0.25F, speedMultiplier());
        if (legSwing > 1F)
            legSwing -= Mth.floor(legSwing);
        if (stompDelay <= 0 && crossedRange(previousPhase, legSwing, type.getStompRangeLower(), type.getStompRangeUpper())
            && StringUtils.isNotBlank(type.getStompSound()))
        {
            if (!level().isClientSide)
                PacketPlaySound.sendSoundPacket(this, 50D, type.getStompSound(), false);
            stompDelay = Math.max(1, type.getStompSoundLength());
        }
    }

    private static boolean crossedRange(float previous, float current, float lower, float upper)
    {
        if (previous <= current)
            return previous < upper && current >= lower;
        return previous < upper || current >= lower;
    }

    private void handleLanding(MechaType type, double descent)
    {
        float force = (float) Math.max(0D, -descent - 0.45D);
        if (type.isTakeFallDamage() && !stopFallDamage())
            damagePart(EnumDriveablePart.HIPS, force * 18F * type.getFallDamageMultiplier()
                * Math.max(0F, type.getFallDamageFactor()), level().damageSources().fall());
        if ((type.isDamageBlocksFromFalling() || breakBlocksUponFalling()) && force * type.getBlockDamageFromFalling() > 0.8F)
            breakLandingBlocks(Math.min(3, 1 + Mth.floor(force * type.getBlockDamageFromFalling())));
    }

    private void breakLandingBlocks(int radius)
    {
        if (!(level() instanceof ServerLevel serverLevel) || !(getControllingEntity() instanceof Player player)
            || !FlansMod.teamsManager.isDriveablesBreakBlocks())
            return;
        BlockPos centre = BlockPos.containing(getX(), getBoundingBox().minY - 0.1D, getZ());
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 0, radius)))
        {
            if (pos.distSqr(centre) > radius * radius || !serverLevel.mayInteract(player, pos))
                continue;
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(serverLevel, pos) >= 0F && serverLevel.getBlockEntity(pos) == null)
                ModUtils.destroyBlock(serverLevel, pos, player, true);
        }
    }

    private void updateAddonSystems()
    {
        int capacity = energyShieldCapacity();
        if (lastShieldCapacity < 0)
            shieldEnergy = capacity;
        else if (capacity > lastShieldCapacity)
            shieldEnergy += capacity - lastShieldCapacity;
        shieldEnergy = Mth.clamp(shieldEnergy, 0F, capacity);
        lastShieldCapacity = capacity;
        if (shieldRechargeDelay > 0)
            --shieldRechargeDelay;
        else if (shieldEnergy < capacity)
            shieldEnergy = Math.min(capacity, shieldEnergy + Math.max(0.1F, capacity / 200F));

        if (tickCount % 20 == 0 && autoRepair() > 0F && driveableData != null)
        {
            for (DriveablePart part : driveableData.getParts().values())
            {
                if (part.getHealth() <= 0F || !canRepairPart(part.getType()) || !hasFuelForAddon(10F))
                    continue;
                if (repairPart(part.getType(), autoRepair()))
                    consumeAddonFuel(10F);
            }
        }
        if (tickCount % 10 == 0 && vacuumItems())
            vacuumNearbyItems();
        if (tickCount % 100 == 0 && diamondDetect() != null)
            scanForDiamonds(diamondDetect());
    }

    @Override
    public boolean damagePart(@Nullable EnumDriveablePart partType, float amount, @Nullable net.minecraft.world.damagesource.DamageSource source)
    {
        if (!level().isClientSide && amount > 0F && shieldEnergy > 0F)
        {
            float absorbed = Math.min(shieldEnergy, amount);
            shieldEnergy -= absorbed;
            amount -= absorbed;
            shieldRechargeDelay = 100;
            if (amount <= 0F)
                return true;
        }
        return super.damagePart(partType, amount * vulnerability(), source);
    }

    private void useHandTool(EnumMechaSlotType slot, boolean left, boolean held)
    {
        int index = left ? 0 : 1;
        if (driveableData == null || getControllingEntity() == null
            || !isPartIntact(left ? EnumDriveablePart.LEFT_ARM : EnumDriveablePart.RIGHT_ARM))
        {
            handGunHeldTicks[index] = 0;
            handGunBurstRemaining[index] = 0;
            return;
        }
        ItemStack stack = driveableData.getMechaAddon(slot);
        if (stack.getItem() instanceof GunItem gunItem)
        {
            useHandGun(slot, left, held, gunItem, stack);
            return;
        }
        handGunHeldTicks[index] = 0;
        handGunBurstRemaining[index] = 0;
        if (!held || !(stack.getItem() instanceof MechaAddonItem addon))
            return;
        MechaItemType tool = addon.getConfigType();
        if (tool.getFunction() == EnumMechaToolType.SWORD)
            useMeleeTool(tool, index);
        else
            useMiningTool(tool, index);
    }

    private void useHandGun(EnumMechaSlotType slot, boolean left, boolean held, GunItem gunItem, ItemStack gunStack)
    {
        int index = left ? 0 : 1;
        GunType gunType = gunItem.getConfigType();
        int input = left ? DriveableInput.PRIMARY_FIRE : DriveableInput.SECONDARY_FIRE;
        boolean rising = held && !DriveableInput.isDown(previousInputMask, input);
        handGunHeldTicks[index] = held ? handGunHeldTicks[index] + 1 : 0;

        // A mecha hand always operates the gun's primary action, matching 1.7.10.
        if (gunType.getSecondaryFire(gunStack))
        {
            gunType.setSecondaryFire(gunStack, false);
            driveableData.setMechaAddon(slot, gunStack);
            acknowledgeInternalWeaponInventoryChange();
        }
        EnumFireMode mode = gunType.getFireMode(gunStack);
        if (mode == EnumFireMode.BURST && rising)
            handGunBurstRemaining[index] = Math.max(1, gunType.getNumBurstRounds());
        if (handGunCooldown[index] > 0
            || !shouldFireHandGun(mode, held, rising, handGunHeldTicks[index], handGunBurstRemaining[index]))
            return;

        LoadedHandAmmo loaded = findLoadedHandAmmo(gunItem, gunType, gunStack);
        if (loaded == null)
        {
            float reloadTime = gunItem.getActualReloadTime(gunStack, oppositeHandStack(left));
            if (reloadHandGun(slot, gunItem, gunType, gunStack))
            {
                handGunCooldown[index] = Math.max(1, Mth.ceil(Math.max(0F, reloadTime)));
                String reloadSound = gunType.getReloadSound(gunStack);
                if (StringUtils.isNotBlank(reloadSound))
                    PacketPlaySound.sendSoundPacket(this, gunType.getReloadSoundRange(), reloadSound, false);
            }
            else
            {
                String clickSound = gunType.getClickSoundOnEmpty(!rising);
                if (StringUtils.isNotBlank(clickSound))
                    PacketPlaySound.sendSoundPacket(this, gunType.getReloadSoundRange(), clickSound, true);
                handGunCooldown[index] = Math.max(4, Mth.ceil(Math.max(1F, gunType.getShootDelay(gunStack))));
            }
            return;
        }
        if (!weaponEnabled(EnumWeaponType.GUN) || MinecraftForge.EVENT_BUS.post(new GunFiredEvent(this)))
            return;

        LivingEntity attacker = getControllingEntity() instanceof LivingEntity living ? living : null;
        ItemStack otherHand = oppositeHandStack(left);
        FireableGun fireable = new FireableGun(gunType, gunStack, attacker, otherHand,
            attacker == null ? EnumMovement.NONE : ModUtils.getEnumMovement(attacker), !onGround());
        FiredShot shot = new FiredShot(fireable, loaded.bulletType(), this, attacker,
            ShootableItem.getRoundsFired(loaded.stack()));
        boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
        boolean consumeAmmo = !creative && !infiniteAmmo();
        boolean lastBullet = countLoadedHandRounds(gunItem, gunType, gunStack) <= 1;
        MechaType mechaType = getMechaType();
        if (mechaType == null)
            return;
        Vec3 origin = handGunOrigin(mechaType, left);
        Vec3 direction = aimDirection();
        ShootingHelper.fireGun(level(), shot, Math.max(1, gunType.getNumBullets(gunStack, loaded.bulletType())),
            origin, direction, () -> {
                if (consumeAmmo)
                {
                    ShootableItem.consumeRound(loaded.stack());
                    gunItem.setBulletItemStack(gunStack, loaded.stack(), loaded.slot());
                    if (StringUtils.isNotBlank(loaded.bulletType().getDropItemOnShoot()))
                        ModUtils.dropItem(level(), this, loaded.bulletType().getDropItemOnShoot(), loaded.bulletType().getContentPack());
                    driveableData.setMechaAddon(slot, gunStack);
                    acknowledgeInternalWeaponInventoryChange();
                }
            });

        String shootSound = gunType.getShootSound(gunStack, lastBullet);
        if (StringUtils.isNotBlank(shootSound))
            PacketPlaySound.sendSoundPacket(this, gunType.getGunSoundRange(), shootSound, true);
        float delay = gunType.getShootDelay(gunStack);
        if (mode == EnumFireMode.SEMIAUTO)
            delay = Math.max(delay, 5F);
        handGunCooldown[index] = Math.max(1, Mth.ceil(Math.max(1F, delay)));
        if (mode == EnumFireMode.BURST && handGunBurstRemaining[index] > 0)
            --handGunBurstRemaining[index];
    }

    @Nullable
    private LoadedHandAmmo findLoadedHandAmmo(GunItem gunItem, GunType gunType, ItemStack gunStack)
    {
        for (int slot = 0; slot < gunType.getNumAmmoItemsInGun(gunStack); slot++)
        {
            ItemStack stack = gunItem.getAmmoItemStack(gunStack, slot);
            if (stack.getItem() instanceof ShootableItem shootableItem
                && shootableItem.getConfigType() instanceof BulletType bulletType
                && gunType.getAmmoTypes().contains(bulletType) && ShootableItem.hasRoundsLeft(stack))
                return new LoadedHandAmmo(slot, stack, bulletType);
        }
        return null;
    }

    private boolean reloadHandGun(EnumMechaSlotType handSlot, GunItem gunItem, GunType gunType, ItemStack gunStack)
    {
        if (gunType.getAmmoTypes().isEmpty())
            return false;
        String preferred = gunStack.hasTag() ? gunStack.getTag().getString(GunItem.NBT_PREFERRED_AMMO) : StringUtils.EMPTY;
        boolean creative = getControllingEntity() instanceof Player player && player.getAbilities().instabuild;
        boolean preserveSource = creative || infiniteAmmo();
        boolean reloaded = false;

        for (int internalSlot = 0; internalSlot < gunType.getNumAmmoItemsInGun(gunStack); internalSlot++)
        {
            ItemStack current = gunItem.getAmmoItemStack(gunStack, internalSlot);
            if (ShootableItem.hasRoundsLeft(current))
                continue;
            int sourceSlot = findBestReloadSource(gunType, preferred);
            if (sourceSlot < 0)
                break;
            ItemStack source = driveableData.getItem(sourceSlot);
            ItemStack loaded = source.copy();
            loaded.setCount(1);
            gunItem.setBulletItemStack(gunStack, loaded, internalSlot);
            if (!preserveSource)
            {
                source.shrink(1);
                driveableData.setItem(sourceSlot, source.isEmpty() ? ItemStack.EMPTY : source);
            }
            reloaded = true;
        }
        if (reloaded)
        {
            driveableData.setMechaAddon(handSlot, gunStack);
            acknowledgeInternalWeaponInventoryChange();
        }
        return reloaded;
    }

    private int findBestReloadSource(GunType gunType, String preferred)
    {
        int bestSlot = -1;
        int bestRounds = 0;
        boolean bestPreferred = false;
        List<ShootableType> allowed = gunType.getAmmoTypes();
        for (int slot = 0; slot < driveableData.getContainerSize(); slot++)
        {
            ItemStack candidate = driveableData.getItem(slot);
            if (!(candidate.getItem() instanceof ShootableItem shootableItem)
                || !allowed.contains(shootableItem.getConfigType()) || !ShootableItem.hasRoundsLeft(candidate))
                continue;
            int rounds = ShootableItem.getRoundsRemaining(candidate);
            boolean candidatePreferred = StringUtils.isNotBlank(preferred)
                && preferred.equalsIgnoreCase(shootableItem.getConfigType().getShortName());
            if ((candidatePreferred && !bestPreferred) || candidatePreferred == bestPreferred && rounds > bestRounds)
            {
                bestSlot = slot;
                bestRounds = rounds;
                bestPreferred = candidatePreferred;
            }
        }
        return bestSlot;
    }

    private int countLoadedHandRounds(GunItem gunItem, GunType gunType, ItemStack gunStack)
    {
        int rounds = 0;
        for (int slot = 0; slot < gunType.getNumAmmoItemsInGun(gunStack); slot++)
            rounds += ShootableItem.getTotalRounds(gunItem.getAmmoItemStack(gunStack, slot));
        return rounds;
    }

    private ItemStack oppositeHandStack(boolean left)
    {
        return driveableData == null ? ItemStack.EMPTY
            : driveableData.getMechaAddon(left ? EnumMechaSlotType.RIGHT_TOOL : EnumMechaSlotType.LEFT_TOOL);
    }

    private Vec3 handGunOrigin(MechaType type, boolean left)
    {
        com.flansmod.common.vector.Vector3f arm = left ? type.getLeftArmOrigin() : type.getRightArmOrigin();
        Vec3 localArm = LegacyDriveableCoordinates.toLocal(arm);
        Vec3 legacyExtension = new Vec3(type.getArmLength() + 1.2F * type.getHeldItemScale(),
            0.5F * type.getHeldItemScale(), 0D);
        Vec3 extension = rotateTurretLocalDirection(LegacyDriveableCoordinates.toLocal(legacyExtension),
            getTurretYaw(), getTurretPitch());
        return localToWorld(localArm.x + extension.x, localArm.y + extension.y, localArm.z + extension.z);
    }

    private static boolean shouldFireHandGun(EnumFireMode mode, boolean held, boolean rising, int heldTicks, int burstRemaining)
    {
        return switch (mode)
        {
            case SEMIAUTO -> rising;
            case BURST -> burstRemaining > 0;
            case MINIGUN -> held && heldTicks >= 10;
            case FULLAUTO -> held;
        };
    }

    private record LoadedHandAmmo(int slot, ItemStack stack, BulletType bulletType) {}

    private void useMeleeTool(MechaItemType tool, int index)
    {
        if (toolCooldown[index] > 0 || !(getControllingEntity() instanceof LivingEntity attacker))
            return;
        MechaType mechaType = getMechaType();
        if (mechaType == null)
            return;
        double reach = Mth.clamp(tool.getReach() * mechaType.getReach(), 1F, 32F);
        Vec3 origin = attacker.getEyePosition();
        Vec3 direction = aimDirection();
        AABB sweep = new AABB(origin, origin.add(direction.scale(reach))).inflate(1.25D);
        Entity target = level().getEntities(this, sweep, entity -> entity instanceof LivingEntity && entity != attacker && !isPartOfThis(entity))
            .stream().min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(origin))).orElse(null);
        if (target != null)
        {
            target.hurt(level().damageSources().mobAttack(attacker), Math.max(1F, 6F * tool.getSpeed()));
            if (tool.isFlameBurst())
                target.setSecondsOnFire(4);
        }
        playToolSound(tool);
        toolCooldown[index] = Math.max(4, Mth.ceil(10F / Math.max(0.1F, tool.getSpeed())));
    }

    private void useMiningTool(MechaItemType tool, int index)
    {
        if (!(level() instanceof ServerLevel serverLevel) || !(getControllingEntity() instanceof Player player)
            || !FlansMod.teamsManager.isDriveablesBreakBlocks())
            return;
        MechaType mechaType = getMechaType();
        if (mechaType == null)
            return;
        double reach = Mth.clamp(tool.getReach() * mechaType.getReach(), 1F, 32F);
        Vec3 origin = player.getEyePosition();
        BlockHitResult hit = level().clip(new ClipContext(origin, origin.add(aimDirection().scale(reach)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !serverLevel.mayInteract(player, hit.getBlockPos())
            || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), ItemStack.EMPTY))
        {
            breakingBlock = null;
            breakingProgress = 0F;
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = serverLevel.getBlockState(pos);
        float hardness = state.getDestroySpeed(serverLevel, pos);
        if (state.isAir() || hardness < 0F || serverLevel.getBlockEntity(pos) != null)
            return;
        if (!pos.equals(breakingBlock))
        {
            breakingBlock = pos.immutable();
            breakingProgress = 0F;
        }
        boolean effective = effectiveAgainst(tool.getFunction(), state) && tool.getToolHardness() + 0.001F >= hardness;
        float speed = effective ? Math.max(0.05F, tool.getSpeed()) : 0.1F;
        breakingProgress += hardness <= 0F ? 1F : speed / Math.max(1F, hardness * 20F);
        if (breakingProgress < 1F)
            return;

        harvestMinedBlock(serverLevel, player, pos, state, effective);
        breakingBlock = null;
        breakingProgress = 0F;
        toolCooldown[index] = 2;
        playToolSound(tool);
    }

    private void harvestMinedBlock(ServerLevel level, Player player, BlockPos pos, BlockState state, boolean effective)
    {
        if (!vacuumItems())
        {
            ModUtils.destroyBlock(level, pos, player, effective);
            return;
        }
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY));
        if (!ModUtils.destroyBlock(level, pos, player, false))
            return;
        for (ItemStack stack : drops)
        {
            ItemStack transformed = transformMinedDrop(stack);
            if (transformed.isEmpty())
                continue;
            ItemStack remainder = insertIntoCargo(transformed);
            if (!remainder.isEmpty())
                Block.popResource(level, pos, remainder);
        }
    }

    private ItemStack transformMinedDrop(ItemStack stack)
    {
        if (stack.isEmpty())
            return stack;
        if (refineIron() && stack.is(Blocks.IRON_ORE.asItem()) && hasFuelForAddon(5F))
        {
            stack = new ItemStack(Items.IRON_INGOT, stack.getCount());
            consumeAddonFuel(5F);
        }
        if (wasteCompact() && (stack.is(Items.COBBLESTONE) || stack.is(Items.DIRT) || stack.is(Items.SAND)))
            return ItemStack.EMPTY;
        float multiplier = stack.is(Items.DIAMOND) ? diamondMultiplier()
            : stack.is(Items.REDSTONE) ? redstoneMultiplier()
            : stack.is(Items.COAL) ? coalMultiplier()
            : stack.is(Items.EMERALD) ? emeraldMultiplier()
            : stack.is(Items.IRON_INGOT) ? ironMultiplier() : 1F;
        if (multiplier > 1F)
        {
            int whole = Mth.floor(multiplier);
            int multiplied = stack.getCount() * (whole + (random.nextFloat() < multiplier - whole ? 1 : 0));
            stack.setCount(Math.min(multiplied, stack.getMaxStackSize() * 16));
        }
        if (autoCoal() && stack.is(Items.COAL) && getConfigType() != null && getFuel() < getConfigType().getFuelTankSize())
        {
            setFuel(Math.min(getConfigType().getFuelTankSize(), getFuel() + stack.getCount() * 1000F));
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private void vacuumNearbyItems()
    {
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(6D), Entity::isAlive))
        {
            ItemStack remainder = insertIntoCargo(item.getItem().copy());
            if (remainder.isEmpty())
                item.discard();
            else
                item.setItem(remainder);
        }
    }

    private void scanForDiamonds(@Nullable MechaItemType detector)
    {
        if (detector == null || !(level() instanceof ServerLevel) || StringUtils.isBlank(detector.getDetectSound())
            || getControllingEntity() == null)
            return;
        BlockPos centre = blockPosition();
        int radius = 12;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius)))
        {
            if (pos.distSqr(centre) <= radius * radius && level().getBlockState(pos).is(Blocks.DIAMOND_ORE))
            {
                PacketPlaySound.sendSoundPacket(this, 48D, detector.getDetectSound(), false);
                return;
            }
        }
    }

    private void playToolSound(MechaItemType tool)
    {
        if (StringUtils.isNotBlank(tool.getSoundEffect()))
            PacketPlaySound.sendSoundPacket(this, 64D, tool.getSoundEffect(), false);
    }

    private Vec3 aimDirection()
    {
        return ModUtils.getDirectionFromPitchAndYaw(getPitch() + getTurretPitch(), getYaw() + getTurretYaw()).normalize();
    }

    private static boolean effectiveAgainst(EnumMechaToolType function, BlockState state)
    {
        return switch (function)
        {
            case PICKAXE -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case SHEARS -> state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL);
            case SWORD -> false;
        };
    }

    @Override
    protected boolean canFireWeaponBank(boolean secondary)
    {
        if (driveableData != null)
        {
            EnumMechaSlotType slot = secondary ? EnumMechaSlotType.RIGHT_TOOL : EnumMechaSlotType.LEFT_TOOL;
            ItemStack stack = driveableData.getMechaAddon(slot);
            if (stack.getItem() instanceof MechaAddonItem || stack.getItem() instanceof GunItem)
                return false;
        }
        return super.canFireWeaponBank(secondary);
    }

    @Override
    protected void consumeAmmo(AmmoSelection selection)
    {
        if (!infiniteAmmo())
            super.consumeAmmo(selection);
    }

    @Override
    protected boolean shouldSquashEntities()
    {
        MechaType type = getMechaType();
        return type != null && type.isSquashMobs();
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose)
    {
        MechaType type = getMechaType();
        if (type == null)
            return super.getDimensions(pose);
        float height = type.getHeight();
        if (driveableData != null && !isPartIntact(EnumDriveablePart.HIPS))
            height -= type.getChassisHeight();
        return EntityDimensions.scalable(Mth.clamp(type.getWidth(), 0.5F, 4F), Mth.clamp(height, 0.5F, 8F));
    }

    public List<MechaItemType> getUpgradeTypes()
    {
        if (driveableData == null)
            return Collections.emptyList();
        List<MechaItemType> types = new ArrayList<>();
        for (EnumMechaSlotType slot : EnumMechaSlotType.values())
        {
            ItemStack stack = driveableData.getMechaAddon(slot);
            if (stack.getItem() instanceof MechaAddonItem addon)
                types.add(addon.getConfigType());
        }
        return types;
    }

    public boolean stopFallDamage() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isStopMechaFallDamage); }
    public boolean breakBlocksUponFalling() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isForceBlockFallDamage); }
    public boolean vacuumItems() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isVacuumItems); }
    public boolean refineIron() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isRefineIron); }
    public boolean wasteCompact() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isWasteCompact); }
    public boolean autoCoal() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isAutoCoal); }
    public boolean infiniteAmmo() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isInfiniteAmmo); }
    public boolean shouldFloat() { return getUpgradeTypes().stream().anyMatch(MechaItemType::isFloater); }
    @Nullable public MechaItemType diamondDetect() { return getUpgradeTypes().stream().filter(MechaItemType::isDiamondDetect).findFirst().orElse(null); }
    @Nullable public MechaItemType rocketPack() { return getUpgradeTypes().stream().filter(MechaItemType::isRocketPack).findFirst().orElse(null); }
    public boolean shouldFly() { return rocketPack() != null; }

    public float autoRepair()
    {
        return getUpgradeTypes().stream().filter(MechaItemType::isAutoRepair).map(MechaItemType::getAutoRepairAmount)
            .max(Float::compare).orElse(0F);
    }

    public float speedMultiplier() { return product(MechaItemType::getSpeedMultiplier); }
    public float diamondMultiplier() { return product(MechaItemType::getFortuneDiamond); }
    public float redstoneMultiplier() { return product(MechaItemType::getFortuneRedstone); }
    public float coalMultiplier() { return product(MechaItemType::getFortuneCoal); }
    public float emeraldMultiplier() { return product(MechaItemType::getFortuneEmerald); }
    public float ironMultiplier() { return product(MechaItemType::getFortuneIron); }
    public float jetPackPower() { return product(MechaItemType::getRocketPower); }

    public float vulnerability()
    {
        float result = 1F;
        for (MechaItemType type : getUpgradeTypes())
            result *= Mth.clamp(1F - type.getDamageResistance(), 0.05F, 1F);
        return Mth.clamp(result, 0.05F, 1F);
    }

    private float product(java.util.function.ToDoubleFunction<MechaItemType> getter)
    {
        double result = 1D;
        for (MechaItemType type : getUpgradeTypes())
            result *= Math.max(0D, getter.applyAsDouble(type));
        return (float) Mth.clamp(result, 0D, 32D);
    }

    public int lightLevel()
    {
        return getUpgradeTypes().stream().mapToInt(MechaItemType::getLightLevel).max().orElse(0);
    }

    public boolean forceDark()
    {
        return getUpgradeTypes().stream().anyMatch(MechaItemType::isForceDark);
    }

    private int energyShieldCapacity()
    {
        return getUpgradeTypes().stream().mapToInt(MechaItemType::getEnergyShield).sum();
    }

    private boolean hasFuelForAddon(float amount)
    {
        return !usesFuel() || getControllingEntity() instanceof Player player && player.getAbilities().instabuild
            || getFuel() >= amount;
    }

    private boolean hasFuelForMovement()
    {
        return hasFuelForAddon(engineFuelPerTick());
    }

    private float engineFuelPerTick()
    {
        PartType engine = driveableData == null ? null : driveableData.getEngine();
        return engine == null ? 1F : Math.max(0F, engine.getFuelConsumption());
    }

    private void consumeAddonFuel(float amount)
    {
        if (usesFuel() && (!(getControllingEntity() instanceof Player player) || !player.getAbilities().instabuild))
            setFuel(Math.max(0F, getFuel() - Math.max(0F, amount)));
    }

    private boolean usesFuel()
    {
        return getConfigType() != null && getConfigType().getFuelTankSize() >= 0F
            && FlansMod.teamsManager.isVehiclesNeedFuel();
    }

    /** Mechas get their own window instead of the paged driveable one. */
    @Override
    public boolean openDriveableMenu(@NotNull ServerPlayer player)
    {
        if (!canPlayerAccessInventory(player) || getDriveableData() == null || getConfigType() == null)
            return false;
        NetworkHooks.openScreen(player,
            new SimpleMenuProvider((containerId, inventory, ignored) -> new MechaInventoryMenu(containerId, inventory, this),
                Component.literal(getConfigType().getName())),
            buffer -> buffer.writeVarInt(getId()));
        return true;
    }

    private static float axis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }
}
