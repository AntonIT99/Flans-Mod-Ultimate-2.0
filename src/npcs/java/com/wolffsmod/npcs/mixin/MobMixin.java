package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTickLod;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;

/**
 * Vanilla's server-side AI step, which every NPC runs through.
 *
 * <ul>
 * <li>Tick level of detail: an NPC far from every player works out its decisions only every few ticks, as
 * {@link NpcTickLod} decides. The rest of its tick, its movement and its physics among it, is untouched, so a skipped
 * decision leaves it steering where it already was. An active equipped weapon continues aiming, firing and reloading
 * every tick, without selecting another weapon or running the movement goals.</li>
 * </ul>
 */
@Mixin(Mob.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into Mob, so the self-cast is valid.
public abstract class MobMixin
{
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTickLod(CallbackInfo callback)
    {
        if (!NpcTickLod.decides((Mob) (Object) this))
        {
            NpcEquipmentRangedGoal.of((EntityNPCInterface) (Object) this).ifPresent(NpcEquipmentRangedGoal::tickBetweenDecisions);
            callback.cancel();
        }
    }
}
