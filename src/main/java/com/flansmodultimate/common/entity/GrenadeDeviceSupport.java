package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.GrenadeDeviceProperties;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GrenadeType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Native grenade operations shared by players and addon-controlled living entities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrenadeDeviceSupport
{
    private static final double PLACEMENT_REACH = 4.5D;
    private static final double SUPPLY_REACH = 3D;

    private static Optional<GrenadeType> type(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ShootableItem item && item.getConfigType() instanceof GrenadeType grenade ? Optional.of(grenade) : Optional.empty();
    }

    public static Optional<GrenadeDeviceProperties> properties(ItemStack stack)
    {
        return type(stack).flatMap(GrenadeDeviceSupport::properties);
    }

    public static Optional<GrenadeDeviceProperties> properties(Entity entity)
    {
        return entity instanceof Grenade grenade && !entity.isRemoved() ? properties(grenade.getConfigType()) : Optional.empty();
    }

    private static Optional<GrenadeDeviceProperties> properties(GrenadeType type)
    {
        if (type == null || type.isStickToThrower())
            return Optional.empty();
        double living = Math.max(0F, type.getLivingProximityTrigger());
        double vehicle = Math.max(0F, type.getDriveableProximityTrigger());
        if (!type.isRemote() && !type.isDeployableBag() && living <= 0D && vehicle <= 0D)
            return Optional.empty();
        return Optional.of(new GrenadeDeviceProperties(living, vehicle, Math.max(0, type.getPrimeDelay()), type.isRemote(), type.isDeployableBag()));
    }

    public static List<Entity> find(Level level, AABB area)
    {
        return level.getEntitiesOfClass(Grenade.class, area, entity -> properties(entity).isPresent()).stream().map(Entity.class::cast).toList();
    }

    public static boolean ownedBy(Entity device, LivingEntity owner)
    {
        return device instanceof Grenade grenade && (grenade.getOwner().filter(entity -> entity.getUUID().equals(owner.getUUID())).isPresent() || owner.getUUID().equals(grenade.throwerUUID));
    }

    public static Optional<LivingEntity> owner(Entity device)
    {
        return device instanceof Grenade grenade ? grenade.getOwner() : Optional.empty();
    }

    public static Optional<DetonationRadii> radii(Entity device)
    {
        if (!(device instanceof Grenade grenade) || grenade.getConfigType() == null)
            return Optional.empty();
        var stats = grenade.getConfigType().getExplosionStats(null);
        return Optional
            .of(new DetonationRadii(Math.max(0F, stats.explosionRadius()), Math.max(0F, stats.blastRadius()), Math.max(0F, stats.fragRadius()), Math.max(0F, grenade.getConfigType().getFireRadius())));
    }

    public static Optional<Entity> place(LivingEntity operator, ItemStack stack, BlockPos support, Vec3 facing, boolean sound)
    {
        Optional<GrenadeType> type = type(stack).filter(value -> properties(value).isPresent());
        if (offServerThread(operator) || type.isEmpty() || invalidDirection(facing) || facing.horizontalDistanceSqr() < 1.0E-6D || !operator.isAlive())
            return Optional.empty();
        Level level = operator.level();
        if (!level.getChunkSource().hasChunk(support.getX() >> 4, support.getZ() >> 4))
            return Optional.empty();
        var state = level.getBlockState(support);
        if (!state.isFaceSturdy(level, support, Direction.UP) || !level.getFluidState(support.above()).isEmpty())
            return Optional.empty();
        Vec3 point = Vec3.atBottomCenterOf(support.above()).add(0D, 0.02D, 0D);
        double size = Math.max(0.5D, type.get().getHitBoxSize());
        if (operator.position().distanceToSqr(point) > PLACEMENT_REACH * PLACEMENT_REACH || !level.noCollision(new AABB(point.add(-size / 2D, 0D, -size / 2D), point.add(size / 2D, size, size / 2D))))
            return Optional.empty();
        Grenade grenade = new Grenade(level, type.get(), point, facing, operator, sound);
        grenade.setDeltaMovement(Vec3.ZERO);
        grenade.angularVelocity = Vec3.ZERO;
        grenade.stuck = true;
        grenade.stuckPos = support.immutable();
        grenade.setStationaryPlacement(true);
        return level.addFreshEntity(grenade) ? Optional.of(grenade) : Optional.empty();
    }

    public static boolean canDetonate(Entity device, LivingEntity operator)
    {
        return operator.isAlive() && device instanceof Grenade grenade && !grenade.detonated && !grenade.isRemoved() && grenade.level() == operator.level() && ownedBy(grenade, operator)
            && grenade.getConfigType() != null && grenade.getConfigType().isRemote() && grenade.tickCount >= grenade.getConfigType().getPrimeDelay();
    }

    public static boolean detonate(Entity device, LivingEntity operator)
    {
        if (offServerThread(operator) || !canDetonate(device, operator))
            return false;
        ((Grenade) device).detonate(device.level());
        return true;
    }

    public static boolean needsSupply(ItemStack bag, LivingEntity recipient, ItemStack weapon, boolean ammunition)
    {
        return type(bag).filter(value -> value.isDeployableBag() && value.getNumUses() > 0).map(value -> useful(value, recipient, weapon, ammunition)).orElse(false);
    }

    public static boolean needsSupply(Entity bag, LivingEntity recipient, ItemStack weapon, boolean ammunition)
    {
        return bag instanceof Grenade grenade && !bag.isRemoved() && grenade.getConfigType() != null && grenade.getConfigType().isDeployableBag() && grenade.numUsesRemaining > 0
            && useful(grenade.getConfigType(), recipient, weapon, ammunition);
    }

    private static boolean useful(GrenadeType type, LivingEntity recipient, ItemStack weapon, boolean ammunition)
    {
        return type.getHealAmount() > 0F && recipient.getHealth() < recipient.getMaxHealth() || type.getPotionEffects().stream().anyMatch(effect -> usefulEffect(recipient, effect))
            || ammunition && type.getNumClips() > 0 && rearmable(weapon);
    }

    private static boolean usefulEffect(LivingEntity recipient, MobEffectInstance effect)
    {
        MobEffectInstance present = recipient.getEffect(effect.getEffect());
        return present == null || present.getAmplifier() < effect.getAmplifier() || present.getAmplifier() == effect.getAmplifier() && present.getDuration() < effect.getDuration() / 2;
    }

    public static boolean useSupply(Entity bag, LivingEntity recipient, ItemStack weapon, Predicate<ItemStack> receiver)
    {
        double reach = recipient instanceof Player ? 6D : SUPPLY_REACH;
        if (offServerThread(recipient) || bag.level() != recipient.level() || !recipient.isAlive() || recipient.distanceToSqr(bag) > reach * reach || !needsSupply(bag, recipient, weapon, true))
            return false;
        Grenade grenade = (Grenade) bag;
        GrenadeType type = grenade.getConfigType();
        boolean used = heal(recipient, type);
        for (MobEffectInstance effect : type.getPotionEffects())
            if (usefulEffect(recipient, effect))
                used |= recipient.addEffect(new MobEffectInstance(effect));
        used |= rearm(weapon, type.getNumClips(), receiver);
        if (used && --grenade.numUsesRemaining <= 0)
            grenade.discard();
        return used;
    }

    private static boolean heal(LivingEntity recipient, GrenadeType type)
    {
        if (type.getHealAmount() <= 0F || recipient.getHealth() >= recipient.getMaxHealth())
            return false;
        float before = recipient.getHealth();
        recipient.heal(type.getHealAmount());
        if (recipient.getHealth() <= before)
            return false;
        ((ServerLevel) recipient.level()).sendParticles(ParticleTypes.HEART, recipient.getX(), recipient.getY() + 1D, recipient.getZ(), 5, 0.2D, 0.3D, 0.2D, 0D);
        return true;
    }

    private static boolean rearmable(ItemStack weapon)
    {
        return weapon.getItem() instanceof GunItem gun && gun.getConfigType().isAllowRearm() && FlansEquipment.createDefaultAmmunition(weapon).isPresent();
    }

    private static boolean rearm(ItemStack weapon, int clips, Predicate<ItemStack> receiver)
    {
        if (clips <= 0 || !rearmable(weapon))
            return false;
        GunItem gun = (GunItem) weapon.getItem();
        ItemStack ammo = FlansEquipment.createDefaultAmmunition(weapon).orElse(ItemStack.EMPTY);
        int remaining = (int) Math.min(4096L, (long) clips * gun.getConfigType().getNumAmmoItemsInGun(weapon));
        boolean used = false;
        while (remaining > 0)
        {
            int count = Math.min(remaining, ammo.getMaxStackSize());
            if (!receiver.test(ammo.copyWithCount(count)))
                break;
            used = true;
            remaining -= count;
        }
        return used;
    }

    private static boolean offServerThread(Entity entity)
    {
        return !(entity.level() instanceof ServerLevel level) || !level.getServer().isSameThread();
    }

    private static boolean invalidDirection(Vec3 point)
    {
        return !Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z);
    }
}
