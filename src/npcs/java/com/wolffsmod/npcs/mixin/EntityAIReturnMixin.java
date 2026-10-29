package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcReturnHome;
import noppes.npcs.ai.EntityAIReturn;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Treats zero wandering range as unlimited for distance returns, and optionally replaces teleport recovery with bounded physical retries. */
@Mixin(value = EntityAIReturn.class, remap = false)
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
public abstract class EntityAIReturnMixin
{
    @Shadow @Final
    private EntityNPCInterface npc;
    @Shadow @SuppressWarnings("FieldCanBeLocal") // Resets the target goal's continuation timeout; this is not a local counter.
    private int totalTicks;
    @Unique
    private final NpcReturnHome wolffsmodnpcsReturn = new NpcReturnHome();

    @Shadow
    private void navigate(boolean intermediate)
    {
        throw new AssertionError();
    }

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/EntityNPCInterface;isInRange(DDDD)Z"))
    private boolean wolffsmodnpcsUnlimitedWanderingRange(EntityNPCInterface entity, double x, double y, double z, double range)
    {
        // This call only checks the wandering radius; the earlier post-combat return remains intact.
        return range == 0 || entity.isInRange(x, y, z, range);
    }

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsWaitBeforeRestart(CallbackInfoReturnable<Boolean> callback)
    {
        if (!NpcEquipment.enabled(npc, Feature.RETURN_HOME_TELEPORT) && !wolffsmodnpcsReturn.ready(npc.level().getGameTime()))
            callback.setReturnValue(false);
    }

    @Inject(method = "start", at = @At("TAIL"))
    private void wolffsmodnpcsInitialAttempt(CallbackInfo callback)
    {
        wolffsmodnpcsReturn.attempted(npc.level().getGameTime());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsPhysicalReturn(CallbackInfo callback)
    {
        if (NpcEquipment.enabled(npc, Feature.RETURN_HOME_TELEPORT))
            return;
        // The dependency's continuation test must not expire this goal after 600 ticks.
        totalTicks = 0;
        long tick = npc.level().getGameTime();
        if (npc.getNavigation().isDone() && wolffsmodnpcsReturn.ready(tick))
        {
            wolffsmodnpcsReturn.attempted(tick);
            navigate(wolffsmodnpcsReturn.nextDetour());
        }
        callback.cancel();
    }
}
