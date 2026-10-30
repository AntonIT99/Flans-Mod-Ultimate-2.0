package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Which targets an NPC drops the bombs of the weapon group beside its main attack on: vehicles and other large or armoured
 * targets on the ground, which its guns could hardly harm. Soldiers and ordinary mobs it leaves to its guns, and anything
 * in the air to its guns alone. A model whose main attack is its bombs drops them on whatever it fights.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBombTargets
{
    /** Blocks above the ground beyond which a target is in the air, where no bomb can hit it. */
    static final double AIRBORNE_HEIGHT = 4D;
    /** Blocks wide from which a mob counts as large enough to bomb. */
    static final double LARGE_WIDTH = 1.9D;

    /** @return whether the target is worth the bombs beside the NPC's main attack */
    public static boolean worthBombing(LivingEntity target)
    {
        Entity body = target.getRootVehicle();
        return !airborne(body) && (armoured(target) || armoured(body) || target.getBbWidth() >= LARGE_WIDTH);
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

    private static boolean airborne(Entity entity)
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
