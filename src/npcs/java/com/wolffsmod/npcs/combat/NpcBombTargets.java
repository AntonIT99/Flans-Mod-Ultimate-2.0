package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Which targets an NPC drops the bombs of the weapon group beside its main attack on: vehicles and other large or armoured
 * targets on the ground, which its guns could hardly harm, and soldiers standing close enough together that one bomb
 * covers several of them. A single soldier or ordinary mob it leaves to its guns, and anything in the air to its guns
 * alone. A model whose main attack is its bombs drops them on whatever it fights.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBombTargets
{
    /** Blocks above the ground beyond which a target is in the air, where no bomb can hit it. */
    static final double AIRBORNE_HEIGHT = 4D;
    /** Blocks wide from which a mob counts as large enough to bomb. */
    static final double LARGE_WIDTH = 1.9D;
    /** Blocks round the target within which another enemy on the ground stands close enough to be caught by the same bomb. */
    static final double GROUP_RADIUS = 7D;
    /** Blocks above or below the target within which such an enemy counts, so that a floor above is no part of the group. */
    static final double GROUP_HEIGHT = 5D;
    /** How many enemies, the target among them, make a group worth a bomb. */
    static final int GROUP_SIZE = 3;
    /** Blocks off the meeting point a bomb may come down and still be let go, however small its blast. */
    static final double RELEASE_RADIUS = 4D;
    /** Share of its blast reach a bomb may come down off the meeting point by: a heavy bomb need not land on the target. */
    static final double RELEASE_SHARE = 0.5D;

    /**
     * @return whether the target is worth the bombs beside the NPC main attack: itself armoured or large, or standing in a
     *         group of enemies one bomb covers
     */
    public static boolean worthBombing(EntityNPCInterface npc, LivingEntity target)
    {
        return worthBombing(target) || grouped(npc, target);
    }

    /** @return whether the target itself is worth those bombs, taking no notice of whoever stands around it */
    public static boolean worthBombing(LivingEntity target)
    {
        Entity body = target.getRootVehicle();
        return !airborne(body) && (armoured(target) || armoured(body) || target.getBbWidth() >= LARGE_WIDTH);
    }

    /**
     * @param reach
     *            blocks round where it goes off within which the bomb harms; 0 for one whose blast is not known
     * @return blocks off where it has to meet the target a bomb of that blast may come down and still be let go. A bomb
     *         that covers a wide circle need not land on the target, and a plane that waits for a closer solution than its
     *         bombs need flies pass after pass without dropping anything.
     */
    public static double releaseRadius(double reach)
    {
        return Math.max(RELEASE_RADIUS, reach * RELEASE_SHARE);
    }

    /**
     * Whether enough enemies stand close enough around the target for one bomb to cover them, which makes a bomb worth
     * dropping on soldiers that single shots of it would be wasted on. The target itself counts towards the group.
     */
    private static boolean grouped(EntityNPCInterface npc, LivingEntity target)
    {
        if (!(npc.level() instanceof ServerLevel level) || airborne(target.getRootVehicle()))
            return false;
        NpcCombatants combatants = NpcCombatants.of(level);
        int near = 0;
        for (int index = 0; index < combatants.size() && near < GROUP_SIZE; index++)
        {
            LivingEntity other = combatants.get(index).entity();
            if (other != npc && other.isAlive() && !(other instanceof EntityNPCInterface fellow && fellow.isKilled()) && beside(target, other)
                && (other == target || NpcDrivers.hostile(npc, other)) && !airborne(other.getRootVehicle()))
                near++;
        }
        return near >= GROUP_SIZE;
    }

    /** @return whether the other entity stands near enough the target to be caught by a bomb meant for it */
    private static boolean beside(LivingEntity target, LivingEntity other)
    {
        return beside(other.getX() - target.getX(), other.getY() - target.getY(), other.getZ() - target.getZ());
    }

    /** @return whether something that far from the target stands close enough to it to be caught by the same bomb */
    static boolean beside(double x, double y, double z)
    {
        return Math.abs(y) <= GROUP_HEIGHT && x * x + z * z <= GROUP_RADIUS * GROUP_RADIUS;
    }

    /** @return whether the entity is a Flan's driveable that is no wreck, or an NPC drawn as a vehicle, mecha, plane or AA gun */
    private static boolean armoured(Entity entity)
    {
        if (entity instanceof EntityNPCInterface npc)
        {
            FlanModelEntity model = NpcModels.flan(npc);
            return model != null && model.getInfoType() != null && FlanModelKind.of(model.getInfoType()) != null;
        }
        return FlansEntityTypes.getDriveableType(entity).isPresent() && !FlansEntityTypes.isWreck(entity);
    }

    public static boolean airborne(Entity entity)
    {
        if (entity.onGround() || entity.isInWater())
            return false;
        int ground = entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING, entity.getBlockX(), entity.getBlockZ());
        return airborne(entity.getY(), ground);
    }

    /** @return whether something this high stands above ground this high */
    static boolean airborne(double height, double ground)
    {
        return height - ground > AIRBORNE_HEIGHT;
    }
}
