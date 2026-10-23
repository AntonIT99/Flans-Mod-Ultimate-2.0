package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcTactics;
import noppes.npcs.ai.EntityAIAvoidTarget;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Custom NPCs' avoid-target goal, which waits while a tactic of the mod's own moves the NPC. The NeoForge Custom NPCs build
 * has no surround, dodge, ambush, stalk or zig-zag tactics; it uses this goal only to retreat from an attacker, and that
 * goal never asks whether the NPC fights at range. It also yields while a throwable gun sends the NPC into melee.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = EntityAIAvoidTarget.class, remap = false)
public abstract class NpcTacticalGoalsMixin
{
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsYieldToTactic(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipmentRangedGoal.rushes(npc) || NpcTactics.of(npc) != Tactic.CUSTOM_NPCS)
            callback.setReturnValue(false);
    }
}
