package com.flansmodultimate.platform.entity;

import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/** Version boundary for small entity API calls whose names or signatures changed. */
public final class EntityPlatform
{
    private EntityPlatform()
    {}

    /** Runs the mob's own spawn initialisation without spawn-group or NBT data. */
    public static void finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType)
    {
        mob.finalizeSpawn(level, difficulty, spawnType, null, null);
    }

    public static void igniteForSeconds(Entity entity, int seconds)
    {
        entity.setSecondsOnFire(seconds);
    }

    /** The player's connection latency in milliseconds. */
    public static int latency(ServerPlayer player)
    {
        return player.latency;
    }

    /** Effective step height, including the step-height attribute of living entities. */
    public static float stepHeight(Entity entity)
    {
        return entity.getStepHeight();
    }

    /** Whether the entity is unaffected by explosions. */
    // Part of the cross-version platform signature; other loader versions use the parameter.
    @SuppressWarnings("java:S1172")
    public static boolean ignoresExplosion(Entity entity, Explosion explosion)
    {
        return entity.ignoreExplosion();
    }

    /** Explosion knockback after the Blast Protection reduction of 15% per level. */
    public static double explosionKnockback(LivingEntity entity, double knockback)
    {
        return ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, knockback);
    }

    /** Runs the mob's spawn initialisation through the loader's finalize-spawn event, as vanilla spawn paths do. */
    public static void finalizeSpawnWithEvent(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType)
    {
        ForgeEventFactory.onFinalizeSpawn(mob, level, difficulty, spawnType, null, null);
    }

    /** The equipment slot the item is worn in by the entity. */
    // Part of the cross-version platform signature; other loader versions use the parameter.
    @SuppressWarnings("java:S1172")
    public static EquipmentSlot equipmentSlotFor(LivingEntity entity, ItemStack stack)
    {
        return LivingEntity.getEquipmentSlotForItem(stack);
    }

    /** Whether the slot is one of the four worn armor slots, not a hand or a body slot. */
    public static boolean isHumanoidArmor(EquipmentSlot slot)
    {
        return slot.getType() == EquipmentSlot.Type.ARMOR;
    }

    public static float width(EntityDimensions dimensions)
    {
        return dimensions.width;
    }

    public static float height(EntityDimensions dimensions)
    {
        return dimensions.height;
    }

    /** Whether the entity has been added to a level, as opposed to only constructed. */
    public static boolean isAddedToLevel(Entity entity)
    {
        return entity.isAddedToWorld();
    }

    /** Offset from a vehicle seat position to where the passenger stands. */
    // Part of the cross-version platform signature; 1.21 asks the passenger for its attachment point on the vehicle.
    @SuppressWarnings("java:S1172")
    public static Vec3 riderOffset(Entity passenger, Entity vehicle)
    {
        return new Vec3(0D, passenger.getMyRidingOffset(), 0D);
    }

    /** Ticks a crossbow takes to charge for the entity. */
    // Part of the cross-version platform signature; other loader versions use the parameter.
    @SuppressWarnings("java:S1172")
    public static int crossbowChargeDuration(ItemStack crossbow, LivingEntity entity)
    {
        return CrossbowItem.getChargeDuration(crossbow);
    }

    /** The loader's shared fake player for the profile, reused for every call with the same identity. */
    public static ServerPlayer fakePlayer(ServerLevel level, GameProfile profile)
    {
        return FakePlayerFactory.get(level, profile);
    }
}
