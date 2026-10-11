package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import com.wolffsmod.npcs.sim.NpcTuning;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Uses Custom NPCs' existing permission-checked stats menu and normal persistence. */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = DataStats.class, remap = false)
public abstract class NpcWeaponSettingsMixin implements NpcWeaponSettings
{
    @Shadow
    private EntityNPCInterface npc;

    @Unique
    private final NpcWeaponOptions wolffsmodnpcsOptions = new NpcWeaponOptions();

    @Unique
    private final NpcTypeProperties wolffsmodnpcsProperties = new NpcTypeProperties();

    @Unique
    private final NpcPresentation wolffsmodnpcsPresentation = new NpcPresentation();

    @Unique
    private final NpcCombatSettings wolffsmodnpcsCombat = new NpcCombatSettings();
    @Unique
    private final NpcTuning wolffsmodnpcsTuning = new NpcTuning();

    @Inject(method = "readToNBT", at = @At("HEAD"))
    private void wolffsmodnpcsRetainHealth(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsProperties.beforeStatsLoad(npc);
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsReadWeapons(CompoundTag compound, CallbackInfo callback)
    {
        wolffsmodnpcsOptions.load(compound);
        wolffsmodnpcsPresentation.load(compound);
        wolffsmodnpcsCombat.load(compound);
        wolffsmodnpcsTuning.load(compound);
        wolffsmodnpcsProperties.loaded(Component.STATS);
        wolffsmodnpcsProperties.afterStatsLoad(npc);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsSaveWeapons(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        wolffsmodnpcsOptions.save(callback.getReturnValue());
        wolffsmodnpcsPresentation.save(callback.getReturnValue());
        wolffsmodnpcsCombat.save(callback.getReturnValue());
        wolffsmodnpcsTuning.save(callback.getReturnValue());
        wolffsmodnpcsProperties.saveDefaults(Component.STATS, callback.getReturnValue());
    }

    @Override
    public NpcWeaponOptions wolffsmodnpcsWeaponOptions()
    {
        return wolffsmodnpcsOptions;
    }

    @Override
    public NpcTypeProperties wolffsmodnpcsTypeProperties()
    {
        return wolffsmodnpcsProperties;
    }

    @Override
    public NpcPresentation wolffsmodnpcsPresentation()
    {
        return wolffsmodnpcsPresentation;
    }

    @Override
    public NpcCombatSettings wolffsmodnpcsCombat()
    {
        return wolffsmodnpcsCombat;
    }

    @Override
    public NpcTuning wolffsmodnpcsTuning()
    {
        return wolffsmodnpcsTuning;
    }
}
