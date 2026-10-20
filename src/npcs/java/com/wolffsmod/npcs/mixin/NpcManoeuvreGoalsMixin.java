package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcTactics;
import noppes.npcs.ai.EntityAIAmbushTarget;
import noppes.npcs.ai.EntityAIStalkTarget;
import noppes.npcs.ai.EntityAIZigZagTarget;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Custom NPCs' ambush, stalk and zig-zag tactics: they wait while a tactic of the mod's own moves the NPC. */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = {EntityAIAmbushTarget.class, EntityAIStalkTarget.class, EntityAIZigZagTarget.class}, remap = false)
public abstract class NpcManoeuvreGoalsMixin
{
    // A mixin of several targets has to say itself that its shadow is not remapped.
    @Shadow(remap = false)
    private EntityNPCInterface npc;

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsYieldToTactic(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcTactics.of(npc) != Tactic.CUSTOM_NPCS)
            callback.setReturnValue(false);
    }
}
