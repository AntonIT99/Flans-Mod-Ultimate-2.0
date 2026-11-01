package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.combat.EquipmentProperty;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentStats;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcMobility;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The ranges an NPC fights at once its firing range comes from the ranged weapon it holds: as far as the spread it fires
 * with still hits a man-sized target about two times in five, within a limit for its kind of weapon. Its aggro range
 * reaches at least that far, so that it notices what it can hit. An NPC without the switch, or without a ranged weapon in
 * hand, keeps its own ranges. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWeaponRange
{
    /** Radians a round leaves its aim line by per unit of Flan spread, at one standard deviation, as Flan guns fire. */
    static final double RADIANS_PER_SPREAD = 0.0025D;
    /** Blocks off the aim line, at one standard deviation, at which about two rounds in five still hit a man-sized target. */
    static final double HIT_DEVIATION = 0.5D;
    /** Shortest firing range a weapon gives, however wide it spreads. */
    static final int MIN_RANGE = 16;

    /** @return blocks within which the NPC fires at its target */
    public static int firing(EntityNPCInterface npc)
    {
        int weapon = weapon(npc);
        return weapon > 0 ? weapon : npc.stats.ranged.getRange();
    }

    /** @return blocks within which the NPC notices an enemy: its own aggro range, or as far as its weapon reaches when that is further */
    public static int aggro(EntityNPCInterface npc)
    {
        return Math.max(npc.stats.aggroRange, weapon(npc));
    }

    /** @return the firing range the NPC's weapon gives it, or 0 when its own firing range applies */
    public static int weapon(EntityNPCInterface npc)
    {
        if (!NpcEquipment.enabled(npc, Feature.WEAPON_FIRING_RANGE) || !NpcEquipment.ranged(npc))
            return 0;
        // A target search asks for every candidate it weighs.
        return NpcTacticState.of(npc).weaponRange(npc.level().getGameTime(), () -> measure(npc));
    }

    private static int measure(EntityNPCInterface npc)
    {
        WeaponRole role = NpcTactics.role(npc);
        // A thrown weapon reaches as far as the NPC throws it, which its own firing range says.
        if (role == WeaponRole.THROWN)
            return 0;
        InteractionHand hand = NpcEquipment.firingHand(npc);
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        NpcEquipmentStats.Shot shot = NpcEquipmentStats.shot(npc, weapon);
        boolean flan = FlansEquipment.getWeaponKind(weapon).isPresent();
        // Other ranged items always fire with the NPC's own accuracy; a Flan gun with the statistics it does not inherit.
        double spread = shot != null && flan && NpcEquipment.inherits(npc, EquipmentProperty.RANGED_ACCURACY.inHand(hand)) ? shot.spread() : NpcEquipment.spread(npc.stats.ranged.getAccuracy());
        int projectiles = shot == null ? 1 : shot.projectiles();
        if (flan && !NpcEquipment.inherits(npc, EquipmentProperty.RANGED_SHOT_COUNT.inHand(hand)))
            projectiles = npc.stats.ranged.getShotCount();
        return Math.min(NpcMobility.rangeLimit(), range(spread, projectiles, role));
    }

    /**
     * @param spread
     *            Flan spread units the weapon fires with
     * @param projectiles
     *            pellets a shot fires: each of several hits less often than one, but one of them hits as often as a single
     *            round of a tighter spread
     * @return the firing range a weapon of the role gives
     */
    static int range(double spread, int projectiles, WeaponRole role)
    {
        int limit = limit(role);
        if (spread <= 0D)
            return limit;
        double deviation = HIT_DEVIATION * Math.sqrt(Math.max(1, projectiles));
        double range = deviation / (spread * RADIANS_PER_SPREAD);
        return (int) Math.round(Math.max(MIN_RANGE, Math.min(limit, range)));
    }

    /** @return the farthest a weapon of the role reaches however tight its spread, since at range its rounds drop and slow */
    static int limit(WeaponRole role)
    {
        return switch (role)
        {
            case CLOSE -> 48;
            case MARKSMAN -> 256;
            case LAUNCHER -> 96;
            case THROWN -> MIN_RANGE;
            default -> 128;
        };
    }
}
