package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.OptionalDouble;

/**
 * Lets an NPC wearing a vehicle or mecha whose definition declares {@code SquashMobs} run over what it drives into, as the
 * driveable itself does: each living entity in its way takes damage that grows with its speed. Allies, riders and what it
 * rides are spared. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcRunOver
{
    /** Blocks the NPC's box reaches out ahead of it at the most, and above and below it. */
    private static final double MAX_REACH = 1.5D;
    private static final double VERTICAL_REACH = 0.25D;

    /**
     * @param model
     *            the vehicle or mecha the NPC wears
     * @param speed
     *            blocks the NPC moved across the ground during the last tick
     */
    public static void tick(EntityNPCInterface npc, IContentType model, double speed, NpcWeaponOptions options)
    {
        if (!options.enabled(Feature.RUN_OVER) || npc.isKilled() || !npc.isAlive())
            return;
        OptionalDouble damage = FlansEntityTypes.getSquashDamage(model, speed);
        if (damage.isEmpty())
            return;
        double reach = Math.min(MAX_REACH, speed + 0.25D);
        AABB own = npc.getBoundingBox();
        AABB box = own.inflate(reach, VERTICAL_REACH, reach);
        for (LivingEntity victim : npc.level().getEntitiesOfClass(LivingEntity.class, box,
            candidate -> candidate != npc && candidate.isAlive() && !spared(npc, candidate) && !onTop(own, candidate.getY())))
            victim.hurt(npc.damageSources().mobAttack(npc), (float) damage.getAsDouble());
    }

    /**
     * @param feet
     *            the height of an entity's feet
     * @return whether the entity stands on top of the NPC's box rather than in its way, as one riding its deck or turret
     *         does; knocking it about would throw it off the vehicle it rides on
     */
    static boolean onTop(AABB npc, double feet)
    {
        return feet >= npc.maxY - VERTICAL_REACH;
    }

    /** @return whether the NPC drives past the entity unharmed: an ally, one riding with it, or what it rides */
    static boolean spared(EntityNPCInterface npc, LivingEntity other)
    {
        return NpcDrivers.ally(npc, other) || npc.hasIndirectPassenger(other) || other.hasIndirectPassenger(npc) || other.isPassengerOfSameVehicle(npc);
    }
}
