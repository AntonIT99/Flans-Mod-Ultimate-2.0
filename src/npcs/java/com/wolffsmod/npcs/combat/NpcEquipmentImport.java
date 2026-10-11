package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcCombatSettings.EquipmentImport;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Resistance;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Shot;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataRanged;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Copies what the NPC's equipment does into the Custom NPCs statistics once, as editable starting values. They are the
 * values Custom NPCs uses when equipment authority is switched off, so a builder can import a weapon or an armor set and
 * then tune the NPC by hand. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipmentImport
{
    /** Custom NPCs potion-effect index of fire, as its melee and projectile effect buttons list it. */
    private static final int FIRE_EFFECT = 33;
    /** Largest burst delay the Custom NPCs editor accepts, in ticks. */
    static final int MAX_BURST_DELAY = 30;
    static final int MAX_BURST = 100;
    static final int MAX_DELAY = 9999;

    /** The Custom NPCs burst that reproduces a weapon's rhythm. */
    record Rhythm(int delay, int burst, int burstDelay)
    {}

    /** @return whether the equipment offered anything to import */
    public static boolean apply(EntityNPCInterface npc, EquipmentImport kind)
    {
        return switch (kind)
        {
            case MELEE -> melee(npc);
            case RANGED -> ranged(npc);
            case ARMOR -> armor(npc);
        };
    }

    private static boolean melee(EntityNPCInterface npc)
    {
        ItemStack item = npc.getMainHandItem();
        if (item.isEmpty())
            return false;
        DataMelee melee = npc.stats.melee;
        melee.setStrength((int) Math.max(0L, Math.round(NpcEquipmentStats.meleeDamage(item))));
        melee.setRange((float) Math.max(0D, NpcEquipmentStats.meleeReach(item)));
        melee.setDelay(Math.max(1, NpcEquipment.meleeDelay(item)));
        melee.setKnockback((int) Math.max(0L, Math.round(NpcEquipmentStats.meleeKnockback(item))));
        int fire = NpcEquipment.fireSeconds(item);
        melee.setEffect(fire > 0 ? FIRE_EFFECT : 0, 0, fire);
        return true;
    }

    private static boolean ranged(EntityNPCInterface npc)
    {
        ItemStack item = npc.getMainHandItem();
        Shot shot = item.isEmpty() ? null : NpcEquipmentStats.shot(npc, item);
        if (shot == null)
            return false;
        DataRanged ranged = npc.stats.ranged;
        ranged.setStrength((int) Math.max(0L, Math.round(shot.damage())));
        ranged.setSpeed((int) Math.max(1L, Math.min(100L, Math.round(shot.speed() * 10D))));
        ranged.setAccuracy((int) Math.round(NpcEquipmentStats.accuracy(shot.spread())));
        ranged.setShotCount(Math.max(1, Math.min(10, shot.projectiles())));
        ranged.setHasGravity(shot.impact().gravity() > 0D);
        ranged.setExplodeSize(NpcEquipmentStats.explosionSize(shot.impact().explosionRadius()));
        if (!shot.firingSound().isBlank())
            ranged.setSound(0, shot.firingSound());
        Rhythm rhythm = rhythm(npc, item);
        ranged.setDelay(rhythm.delay(), rhythm.delay());
        ranged.setBurst(rhythm.burst());
        ranged.setBurstDelay(rhythm.burstDelay());
        return true;
    }

    private static Rhythm rhythm(EntityNPCInterface npc, ItemStack item)
    {
        Optional<EquippedWeaponProperties> flan = NpcEquipment.flan(npc).filter(properties -> !properties.throwable());
        if (flan.isEmpty())
            return rhythm(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item), 0D, 1);
        ItemStack ammunition = FlansEquipment.getLoadedAmmunition(item).orElseGet(() -> ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
        return rhythm(flan.get().shotDelay(), flan.get().reloadTime(), ammunition == null ? 1 : FlansProjectiles.getRoundsPerItem(ammunition));
    }

    /**
     * A magazine becomes one Custom NPCs burst followed by the reload. A weapon too slow for the editor's burst delay,
     * or holding a single round, fires one shot per cycle instead, waiting for whichever of its delay and reload is longer.
     */
    static Rhythm rhythm(double shotDelay, double reloadTime, int rounds)
    {
        int shot = (int) Math.max(1L, Math.min(MAX_DELAY, Math.round(Double.isFinite(shotDelay) ? shotDelay : 20D)));
        int reload = (int) Math.max(0L, Math.min(MAX_DELAY, (long) Math.ceil(Double.isFinite(reloadTime) ? reloadTime : 0D)));
        if (rounds > 1 && shot <= MAX_BURST_DELAY)
            return new Rhythm(Math.max(shot, reload), Math.min(MAX_BURST, rounds), shot);
        return new Rhythm(rounds > 1 ? shot : Math.max(shot, reload), 1, 0);
    }

    private static boolean armor(EntityNPCInterface npc)
    {
        boolean worn = false;
        for (ItemStack stack : npc.getArmorSlots())
        {
            if (stack.isEmpty())
                continue;
            worn = true;
            FlansEquipment.getArmorProperties(stack).ifPresent(properties ->
            {
                if (properties.fireResistance())
                    npc.stats.immuneToFire = true;
                if (properties.waterBreathing())
                    npc.stats.canDrown = false;
                if (properties.negatesFallDamage())
                    npc.stats.noFallDamage = true;
            });
        }
        if (!worn)
            return false;
        npc.stats.resistances.knockback = resistance(NpcEquipmentStats.protection(npc, Resistance.KNOCKBACK));
        npc.stats.resistances.arrow = resistance(NpcEquipmentStats.protection(npc, Resistance.PROJECTILE));
        npc.stats.resistances.melee = resistance(NpcEquipmentStats.protection(npc, Resistance.MELEE));
        npc.stats.resistances.explosion = resistance(NpcEquipmentStats.protection(npc, Resistance.EXPLOSION));
        return true;
    }

    /** Custom NPCs stores a resistance as 1 plus the share absorbed: 1 is neutral and 2 is immune. */
    static float resistance(float protection)
    {
        return 1F + Math.max(0F, Math.min(1F, Float.isFinite(protection) ? protection : 0F));
    }
}
