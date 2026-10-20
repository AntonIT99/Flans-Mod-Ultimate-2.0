package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Resistance;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Shot;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataRanged;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Copies what the NPC's equipment does into the Custom NPCs statistics once, one statistic at a time, as editable values.
 * They are the values Custom NPCs uses for a statistic the NPC keeps its own value for, or while equipment authority is
 * switched off, so a builder can import a weapon or an armor set and then tune the NPC by hand. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipmentImport
{
    /** Custom NPCs effect type that sets the target on fire instead of applying a potion effect. */
    public static final int FIRE_EFFECT = 666;
    /** Largest burst delay the Custom NPCs editor accepts, in ticks. */
    static final int MAX_BURST_DELAY = 30;
    static final int MAX_BURST = 100;
    static final int MAX_DELAY = 9999;

    /** The Custom NPCs burst that reproduces a weapon's rhythm: ticks after a burst, shots in it, and ticks between them. */
    public record Rhythm(int delay, int burst, int burstDelay)
    {}

    /** @return whether the equipment offered the statistic to import */
    public static boolean apply(EntityNPCInterface npc, EquipmentProperty property)
    {
        return switch (property.getGroup())
        {
            case MELEE -> melee(npc, property);
            case RANGED, FIRING, PROJECTILE -> ranged(npc, property, NpcEquipment.mainRangedWeapon(npc));
            // An off-hand gun's values go to the same ranged values the NPC fires either hand with.
            case OFF_HAND_GUN -> property.importable() && ranged(npc, property.mainHand(), npc.getOffhandItem());
            case OFF_HAND_SHIELD -> false;
            case ARMOR -> resistance(npc, property);
            case ARMOR_EFFECTS -> armorEffect(npc, property);
        };
    }

    private static boolean melee(EntityNPCInterface npc, EquipmentProperty property)
    {
        ItemStack item = npc.getMainHandItem();
        if (item.isEmpty())
            return false;
        DataMelee melee = npc.stats.melee;
        switch (property)
        {
            case MELEE_DAMAGE -> melee.setStrength((int) Math.max(0L, Math.round(NpcEquipmentStats.meleeDamage(npc, item))));
            case MELEE_REACH -> melee.setRange((float) Math.max(0D, NpcEquipmentStats.meleeReach(item)));
            case MELEE_DELAY -> melee.setDelay(Math.max(1, NpcEquipment.meleeDelay(item)));
            case MELEE_KNOCKBACK -> melee.setKnockback((int) Math.max(0L, Math.round(NpcEquipmentStats.meleeKnockback(npc, item))));
            default -> {
                int fire = NpcEquipment.fireSeconds(item, npc);
                melee.setEffect(fire > 0 ? FIRE_EFFECT : 0, 0, fire);
            }
        }
        return true;
    }

    private static boolean ranged(EntityNPCInterface npc, EquipmentProperty property, ItemStack item)
    {
        Shot shot = item.isEmpty() || !NpcEquipment.ranged(item) ? null : NpcEquipmentStats.shot(npc, item);
        if (shot == null)
            return false;
        DataRanged ranged = npc.stats.ranged;
        switch (property)
        {
            case RANGED_DAMAGE -> ranged.setStrength((int) Math.max(0L, Math.round(shot.damage())));
            case RANGED_ACCURACY -> ranged.setAccuracy((int) Math.round(NpcEquipmentStats.accuracy(shot.spread())));
            case RANGED_SPEED -> ranged.setSpeed((int) Mth.clamp(Math.round(shot.speed() * 10D), 1L, 100L));
            case RANGED_SHOT_COUNT -> ranged.setShotCount(Mth.clamp(shot.projectiles(), 1, 10));
            case FIRING_SOUND -> {
                if (shot.firingSound().isBlank())
                    return false;
                ranged.setSound(0, shot.firingSound());
            }
            case FIRING_DELAY -> ranged.setDelay(rhythm(npc, item).delay(), rhythm(npc, item).delay());
            case FIRING_BURST -> ranged.setBurst(rhythm(npc, item).burst());
            case FIRING_BURST_DELAY -> ranged.setBurstDelay(rhythm(npc, item).burstDelay());
            // Custom NPCs keeps the entity hit sound in slot 1 and the block hit sound in slot 2.
            case FIRING_ENTITY_HIT_SOUND -> ranged.setSound(1, shot.impact().entityHitSound());
            case FIRING_BLOCK_HIT_SOUND -> {
                // A sound picked from the struck block's material has no single name to copy.
                if (shot.impact().materialImpactSounds())
                    return false;
                ranged.setSound(2, shot.impact().blockHitSound());
            }
            case PROJECTILE_GRAVITY -> ranged.setHasGravity(shot.impact().gravity() > 0D);
            case PROJECTILE_EXPLOSION -> ranged.setExplodeSize(NpcEquipmentStats.explosionSize(shot.impact().explosionRadius()));
            default -> {
                return false;
            }
        }
        return true;
    }

    /** The burst an import would give the NPC for this ranged weapon. */
    public static Rhythm rhythm(EntityNPCInterface npc, ItemStack item)
    {
        Optional<EquippedWeaponProperties> flan = FlansEquipment.getWeaponProperties(item, ItemStack.EMPTY).filter(properties -> !properties.throwable());
        if (flan.isEmpty())
            return rhythm(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item, npc), 0D, 1);
        ItemStack ammunition = FlansEquipment.getLoadedAmmunition(item).orElseGet(() -> ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
        return rhythm(flan.get().shotDelay(), flan.get().reloadTime(), ammunition == null ? 1 : FlansProjectiles.getRoundsPerItem(ammunition));
    }

    /**
     * A magazine becomes one Custom NPCs burst followed by the reload. A weapon too slow for the editor's burst delay,
     * or holding a single round, fires one shot per cycle instead, waiting for whichever of its delay and reload is longer.
     */
    static Rhythm rhythm(double shotDelay, double reloadTime, int rounds)
    {
        int shot = (int) Mth.clamp(Math.round(Double.isFinite(shotDelay) ? shotDelay : 20D), 1L, MAX_DELAY);
        int reload = (int) Mth.clamp((long) Math.ceil(Double.isFinite(reloadTime) ? reloadTime : 0D), 0L, MAX_DELAY);
        if (rounds > 1 && shot <= MAX_BURST_DELAY)
            return new Rhythm(Math.max(shot, reload), Math.min(MAX_BURST, rounds), shot);
        return new Rhythm(rounds > 1 ? shot : Math.max(shot, reload), 1, 0);
    }

    private static boolean noArmor(EntityNPCInterface npc)
    {
        for (ItemStack stack : npc.getArmorSlots())
            if (!stack.isEmpty())
                return false;
        return true;
    }

    private static boolean resistance(EntityNPCInterface npc, EquipmentProperty property)
    {
        if (noArmor(npc))
            return false;
        Resistance kind = switch (property)
        {
            case ARMOR_PROJECTILE -> Resistance.PROJECTILE;
            case ARMOR_MELEE -> Resistance.MELEE;
            case ARMOR_EXPLOSION -> Resistance.EXPLOSION;
            default -> Resistance.KNOCKBACK;
        };
        float value = resistance(NpcEquipmentStats.protection(npc, kind));
        switch (kind)
        {
            case PROJECTILE -> npc.stats.resistances.arrow = value;
            case MELEE -> npc.stats.resistances.melee = value;
            case EXPLOSION -> npc.stats.resistances.explosion = value;
            case KNOCKBACK -> npc.stats.resistances.knockback = value;
        }
        return true;
    }

    /** Copies whether the worn armor grants the effect, so an imported No clears an NPC setting the armor does not back. */
    private static boolean armorEffect(EntityNPCInterface npc, EquipmentProperty property)
    {
        if (noArmor(npc))
            return false;
        switch (property)
        {
            case ARMOR_FIRE -> npc.stats.immuneToFire = NpcEquipment.worn(npc, EquippedArmorProperties::fireResistance);
            case ARMOR_BREATHING -> npc.stats.canDrown = !NpcEquipment.worn(npc, EquippedArmorProperties::waterBreathing);
            default -> npc.stats.noFallDamage = NpcEquipment.worn(npc, EquippedArmorProperties::negatesFallDamage);
        }
        return true;
    }

    /** Custom NPCs stores a resistance as 1 plus the share absorbed: 1 is neutral and 2 is immune. */
    static float resistance(float protection)
    {
        return 1F + Mth.clamp(Float.isFinite(protection) ? protection : 0F, 0F, 1F);
    }
}
