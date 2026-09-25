package com.flansmodultimate.platform.entity;

import net.neoforged.neoforge.event.EventHooks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerLevelAccessor;

/** Version boundary for small entity API calls whose names or signatures changed. */
public final class EntityPlatform
{
    private EntityPlatform() {}

    /** Runs the mob's own spawn initialisation without spawn-group or NBT data. */
    @SuppressWarnings("deprecation") // NeoForge marks this as override-only; this keeps the direct call of 1.20.1.
    public static void finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType)
    {
        mob.finalizeSpawn(level, difficulty, spawnType, null);
    }

    public static void igniteForSeconds(Entity entity, int seconds)
    {
        entity.igniteForSeconds(seconds);
    }

    /** The player's connection latency in milliseconds. */
    public static int latency(ServerPlayer player)
    {
        return player.connection.latency();
    }

    /** Effective step height, including the step-height attribute of living entities. */
    public static float stepHeight(Entity entity)
    {
        return entity.maxUpStep();
    }

    /** Whether the entity is unaffected by explosions. */
    public static boolean ignoresExplosion(Entity entity, Explosion explosion)
    {
        return entity.ignoreExplosion(explosion);
    }

    /** Explosion knockback after the Blast Protection reduction of 15% per level. */
    public static double explosionKnockback(LivingEntity entity, double knockback)
    {
        int level = EnchantmentHelper.getEnchantmentLevel(
            entity.level().registryAccess().holderOrThrow(Enchantments.BLAST_PROTECTION), entity);
        return knockback * Math.max(0.0, 1.0 - level * 0.15);
    }

    /** Runs the mob's spawn initialisation through the loader's finalize-spawn event, as vanilla spawn paths do. */
    public static void finalizeSpawnWithEvent(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType)
    {
        EventHooks.finalizeMobSpawn(mob, level, difficulty, spawnType, null);
    }

    /** The equipment slot the item is worn in by the entity. */
    public static EquipmentSlot equipmentSlotFor(LivingEntity entity, ItemStack stack)
    {
        return entity.getEquipmentSlotForItem(stack);
    }

    /** Ticks a crossbow takes to charge for the entity. */
    public static int crossbowChargeDuration(ItemStack crossbow, LivingEntity entity)
    {
        return CrossbowItem.getChargeDuration(crossbow, entity);
    }
}
