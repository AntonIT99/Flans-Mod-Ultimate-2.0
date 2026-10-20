package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.ReloadCycle;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcReloadFeedback;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcTacticSteering;
import com.wolffsmod.npcs.tactics.NpcTactics;
import noppes.npcs.ai.EntityAIRangedAttack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import java.util.Optional;

/**
 * Custom NPCs' fallback ranged AI.
 *
 * <ul>
 * <li>It never fires synthetic shots alongside an authoritative equipped weapon, and reports that weapon's shots to the tactics
 * that wait for it to have fired.</li>
 * <li>It retains its targeting and movement while letting native inherited banks fire at their authored cadence.</li>
 * <li>With a reload cycle, an inherited bank fires its magazine, reloads for the model's reload time and plays the model's
 * reload and chambering sounds. Losing the target does not cut a reload short.</li>
 * <li>When a Custom NPCs burst ends, Flan guns the NPC merely holds play their reload sound and animation.</li>
 * <li>The trigger waits, without spending its cooldown or its burst, until a model's gun is on target and until nothing the NPC
 * must not fire through is in the way; meanwhile the NPC keeps closing in.</li>
 * <li>With a tactic of the mod's own, the tactic moves the NPC while this goal fires, and learns of its reloads and pauses.</li>
 * </ul>
 */
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = EntityAIRangedAttack.class, remap = false)
public abstract class EntityAIRangedAttackMixin
{
    @Shadow @Final
    private EntityNPCInterface npc;
    @Shadow @SuppressWarnings("FieldCanBeLocal") // Shadowed target state, written for the target.
    private int burstCount;
    @Shadow
    private int rangedAttackTime;
    @Shadow @SuppressWarnings("FieldCanBeLocal") // Shadowed target state, written for the target.
    private boolean hasFired;
    @Shadow @SuppressWarnings("FieldCanBeLocal") // Shadowed target state, written for the target.
    private int moveTries;
    @Unique
    private final NpcTypeCadence wolffsmodnpcsCadence = new NpcTypeCadence();
    @Unique
    private boolean wolffsmodnpcsWasInherited;
    @Unique
    private double wolffsmodnpcsLastInterval = -1D;
    /** Triggers fired from the current magazine. */
    @Unique
    private int wolffsmodnpcsVolleys;
    /** Game time at which the running reload ends, so a new engagement cannot skip it. */
    @Unique
    private long wolffsmodnpcsReloadedAt;

    // Equipment authority

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeWeaponAI(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.weaponAuthority(npc))
            callback.setReturnValue(false);
    }

    /** Custom NPCs' dodge and hit-and-run tactics wait for this goal to have fired; with an equipped weapon, its own goal fires. */
    @Inject(method = "hasFired", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsEquipmentFired(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.weaponAuthority(npc))
            callback.setReturnValue(NpcEquipmentRangedGoal.of(npc).map(NpcEquipmentRangedGoal::isFired).orElse(false));
    }

    // Tactics. An @At inherits the mixin's remap = false; the navigator is a Minecraft class, whose names are remapped.

    /** A tactic of the mod's own moves the NPC while this goal keeps firing; its own advance and halt then wait. */
    @SuppressWarnings("DefaultAnnotationParam")
    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z", remap = true))
    private boolean wolffsmodnpcsTacticAdvances(PathNavigation navigation, Entity target, double speed)
    {
        return NpcTactics.followsPath(NpcTactics.of(npc)) || navigation.moveTo(target, speed);
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;stop()V", remap = true))
    private void wolffsmodnpcsTacticHalts(PathNavigation navigation)
    {
        if (!NpcTactics.followsPath(NpcTactics.of(npc)))
            navigation.stop();
    }

    @Inject(method = {"tick", "m_8037_"}, at = @At("TAIL"))
    private void wolffsmodnpcsFollowTactic(CallbackInfo callback)
    {
        Tactic tactic = NpcTactics.of(npc);
        if (NpcTactics.followsPath(tactic))
            NpcTacticSteering.steer(npc, tactic);
    }

    // Inherited cadence and reload cycle

    @Inject(method = {"tick", "m_8037_"}, at = @At("HEAD"))
    private void wolffsmodnpcsSelectTiming(CallbackInfo callback)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        boolean inherited = properties.nativeTiming();
        double interval = properties.shotDelay();
        if (inherited != wolffsmodnpcsWasInherited || inherited && interval != wolffsmodnpcsLastInterval)
        {
            wolffsmodnpcsCadence.reset();
            burstCount = inherited ? 1 : 0;
            rangedAttackTime = inherited ? (int) Math.max(0L, Math.min(Integer.MAX_VALUE, wolffsmodnpcsReloadedAt - npc.level().getGameTime())) : 0;
            wolffsmodnpcsWasInherited = inherited;
            wolffsmodnpcsLastInterval = interval;
        }
        if (!inherited)
            return;
        burstCount = 1;
        Optional<ReloadCycle> cycle = properties.reloadCycle();
        if (cycle.isPresent() && cycle.get().chamberSoundTick().isPresent() && rangedAttackTime == cycle.get().chamberSoundTick().getAsInt())
            NpcReloadFeedback.chamber(npc);
    }

    // Declared after the timing hook, which may reset the countdown this hook then holds.
    @Inject(method = {"tick", "m_8037_"}, at = @At("HEAD"))
    private void wolffsmodnpcsHoldFire(CallbackInfo callback)
    {
        NpcVehicleState state = NpcVehicleState.of(npc);
        // An aircraft below its attack altitude climbs first; it keeps its target meanwhile.
        boolean hold = state.isActive() && !state.isAligned() || state.getFlight().holdsFire();
        LivingEntity target = npc.getTarget();
        if (target != null && state.holdsFire(npc, target))
        {
            hold = true;
            // Custom NPCs stops advancing once it has seen its target for a while; it has to keep going for a clear shot.
            moveTries = 0;
        }
        if (hold && rangedAttackTime <= 0)
            rangedAttackTime = 1;
    }

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataRanged;getBurst()I"))
    private int wolffsmodnpcsContinuousBank(DataRanged ranged)
    {
        return NpcTypeProperties.of(npc).nativeTiming() ? Integer.MAX_VALUE : ranged.getBurst();
    }

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataRanged;getBurstDelay()I"))
    private int wolffsmodnpcsBankInterval(DataRanged ranged)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        if (!properties.nativeTiming())
            return ranged.getBurstDelay();
        hasFired = true;
        NpcNativeRhythm rhythm = NpcNativeRhythm.of(properties.shotDelay(), properties.reloadCycle().orElse(null), wolffsmodnpcsOwn(NpcTypeProperty.BURST_DELAY, ranged.getBurstDelay()),
            wolffsmodnpcsOwn(NpcTypeProperty.BURST, ranged.getBurst()), wolffsmodnpcsOwnDelay(ranged));
        // This trigger fires right after the interval is read, so the burst's last round still leaves the barrel.
        if (++wolffsmodnpcsVolleys < rhythm.volleys())
            return wolffsmodnpcsCadence.next(rhythm.shotDelay());
        wolffsmodnpcsVolleys = 0;
        int pause = wolffsmodnpcsCadence.next(rhythm.pause());
        if (rhythm.reloads())
        {
            wolffsmodnpcsReloadedAt = npc.level().getGameTime() + pause;
            NpcReloadFeedback.modelReload(npc);
        }
        NpcTacticState.of(npc).reloading(npc.level().getGameTime(), pause);
        return pause;
    }

    @Unique
    private int wolffsmodnpcsOwn(NpcTypeProperty property, int value)
    {
        return NpcTypeProperties.usesOwnValue(npc, property) ? value : NpcNativeRhythm.INHERITED;
    }

    @Unique
    private int wolffsmodnpcsOwnDelay(DataRanged ranged)
    {
        boolean own = NpcTypeProperties.usesOwnValue(npc, NpcTypeProperty.DELAY_MIN) || NpcTypeProperties.usesOwnValue(npc, NpcTypeProperty.DELAY_MAX);
        return own ? ranged.getDelayRNG() : NpcNativeRhythm.INHERITED;
    }

    @Inject(method = {"stop", "m_8041_"}, at = @At("TAIL"))
    private void wolffsmodnpcsResetTiming(CallbackInfo callback)
    {
        wolffsmodnpcsWasInherited = false;
        wolffsmodnpcsCadence.reset();
    }

    // Held guns

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataRanged;getDelayRNG()I"))
    private int wolffsmodnpcsBurstEnded(DataRanged ranged)
    {
        int pause = ranged.getDelayRNG();
        NpcReloadFeedback.heldGuns(npc, pause);
        // The pause after a burst is when an NPC fighting from cover ducks back in.
        NpcTacticState.of(npc).reloading(npc.level().getGameTime(), pause);
        return pause;
    }
}
