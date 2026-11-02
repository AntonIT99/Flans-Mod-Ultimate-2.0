package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/** Separates server-side pushes between ticks from the NPC's own movement, flight and gravity. */
public final class NpcExternalImpulses
{
    @Nullable
    private Vec3 baseline;

    /** Resolves pushes before the NPC moves. The first tick preserves loaded velocity. Server thread only. */
    public void absorb(EntityNPCInterface npc)
    {
        Vec3 current = npc.getDeltaMovement();
        Knockback mode = NpcProtection.settings(npc).getKnockback();
        FlanModelEntity model = NpcModels.flan(npc);
        npc.setDeltaMovement(absorb(current, mode, model == null ? null : model.getInfoType()));
    }

    Vec3 absorb(Vec3 current, Knockback mode, @Nullable IContentType type)
    {
        if (baseline == null)
        {
            baseline = current;
            return current;
        }
        Vec3 external = current.subtract(baseline);
        Vec3 retained = external;
        if (mode == Knockback.IMMUNE)
            retained = Vec3.ZERO;
        else if (mode == Knockback.MODEL && type != null)
            retained = FlansEntityTypes.scaleExternalImpulse(type, external);
        baseline = baseline.add(retained);
        return baseline;
    }

    /** Records the velocity after all of the NPC's own movement simulations. Server thread only. */
    public void settle(Vec3 velocity)
    {
        baseline = velocity;
    }
}
