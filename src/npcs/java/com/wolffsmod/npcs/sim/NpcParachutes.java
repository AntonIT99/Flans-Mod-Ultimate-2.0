package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * NPCs open the parachutes they carry as players do: a falling NPC with a Flan parachute tool in a hand, its projectile slot
 * or its inventory opens it once it has fallen far enough to get hurt and has the height left for it to help, and spends one
 * of the tool's uses. Under the parachute, an NPC with a target steers toward it. NPCs drawn as Flan models and flying NPCs
 * never open one. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcParachutes
{
    /** Blocks an NPC falls before it opens its parachute, as far as it can fall unhurt. */
    private static final float OPEN_FALL = 3F;
    /** Blocks above the ground under which a falling NPC lands before a parachute would slow it. */
    private static final double MIN_HEIGHT = 4D;
    /** Blocks across the ground from its target within which an NPC under a parachute stops steering. */
    private static final double ON_TARGET = 2D;
    /** Custom NPCs' movement type of an NPC that flies. */
    private static final int FLYING = 1;

    /** Opens a parachute over a falling NPC that carries one, and steers one already under its parachute. */
    public static void tick(EntityNPCInterface npc)
    {
        if (npc.isKilled() || !npc.isAlive())
            return;
        if (npc.isPassenger())
        {
            steer(npc);
            return;
        }
        if (npc.onGround() || npc.isInWater() || npc.isNoGravity() || npc.fallDistance < OPEN_FALL || npc.getDeltaMovement().y >= 0D || npc.ais.movementType == FLYING || NpcModels.flan(npc) != null
            || height(npc) <= MIN_HEIGHT)
            return;
        open(npc);
    }

    /** @return whether the NPC carries a parachute tool with a use left */
    public static boolean carries(EntityNPCInterface npc)
    {
        return NpcAmmunition.supplies(npc).stream().anyMatch(FlansEquipment::isUsableParachute);
    }

    /**
     * Opens the first parachute the NPC carries, spending one of its uses; a parachute used up is taken out of its slot.
     *
     * @return whether a parachute opened; false when the NPC carries none or there is no room above it
     */
    public static boolean open(EntityNPCInterface npc)
    {
        for (ItemStack stack : NpcAmmunition.supplies(npc))
        {
            if (FlansEquipment.deployParachute(npc, stack))
            {
                NpcAmmunition.clearUsedUp(npc, stack);
                return true;
            }
        }
        return false;
    }

    /** Walks an NPC under a parachute toward its target, which the parachute takes as steering, as it does a player's. */
    private static void steer(EntityNPCInterface npc)
    {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || !FlansEquipment.isUnderParachute(npc))
            return;
        double dx = target.getX() - npc.getX();
        double dz = target.getZ() - npc.getZ();
        if (dx * dx + dz * dz < ON_TARGET * ON_TARGET)
        {
            npc.zza = 0F;
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        npc.setYRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        // The parachute moves on its next tick, before the NPC's own movement takes this up again.
        npc.zza = 1F;
    }

    /** @return blocks the entity is above the ground under it */
    private static double height(Entity entity)
    {
        return entity.getY() - entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.getBlockX(), entity.getBlockZ());
    }
}
