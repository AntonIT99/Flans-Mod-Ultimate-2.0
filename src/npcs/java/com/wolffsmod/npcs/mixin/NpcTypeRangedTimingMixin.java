package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeCadence;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import noppes.npcs.ai.EntityAIRangedAttack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains Custom NPCs targeting/movement while letting native inherited banks fire at their authored cadence. */
@Mixin(value = EntityAIRangedAttack.class, remap = false)
public abstract class NpcTypeRangedTimingMixin
{
    @Shadow @Final
    private EntityNPCInterface npc;
    @Shadow
    private int burstCount;
    @Shadow
    private int rangedAttackTime;
    @Shadow
    private boolean hasFired;
    @Unique
    private final NpcTypeCadence wolffsmodnpcsCadence = new NpcTypeCadence();
    @Unique
    private boolean wolffsmodnpcsWasInherited;
    @Unique
    private double wolffsmodnpcsLastInterval = -1D;

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
            rangedAttackTime = 0;
            wolffsmodnpcsWasInherited = inherited;
            wolffsmodnpcsLastInterval = interval;
        }
        if (inherited)
            burstCount = 1;
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
        return wolffsmodnpcsCadence.next(properties.shotDelay());
    }

    @Inject(method = {"stop", "m_8041_"}, at = @At("TAIL"))
    private void wolffsmodnpcsResetTiming(CallbackInfo callback)
    {
        wolffsmodnpcsWasInherited = false;
        wolffsmodnpcsCadence.reset();
    }
}
