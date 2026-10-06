package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Uses Custom NPCs' existing permission-checked stats menu and normal persistence. */
@Mixin(value = DataStats.class, remap = false)
public abstract class NpcWeaponSettingsMixin implements NpcWeaponSettings
{
    @Shadow
    private EntityNPCInterface npc;

    @Unique
    private final NpcWeaponOptions wolffsmodnpcsOptions = new NpcWeaponOptions();

    @Unique
    private final NpcTypeProperties wolffsmodnpcsProperties = new NpcTypeProperties();

    @Inject(method = "readToNBT", at = @At("HEAD"))
    private void wolffsmodnpcsRetainHealth(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsProperties.beforeStatsLoad(npc);
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsReadWeapons(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsOptions.load(tag);
        wolffsmodnpcsProperties.loaded(Component.STATS);
        wolffsmodnpcsProperties.afterStatsLoad(npc);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsSaveWeapons(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        wolffsmodnpcsOptions.save(callback.getReturnValue());
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
}
