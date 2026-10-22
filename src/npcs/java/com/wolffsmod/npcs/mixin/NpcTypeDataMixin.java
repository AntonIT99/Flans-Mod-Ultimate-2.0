package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.tactics.CustomNpcTactics;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAdvanced;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Keeps inherited movement/audio defaults and restores DataAI tactical settings through its existing transport. */
// After merging, this is one of the two data targets; the Object cast keeps the target check legal.
@SuppressWarnings({"AddedMixinMembersNamePattern", "ConstantValue", "java:S1905"})
@Mixin(value = {DataAI.class, DataAdvanced.class}, remap = false)
public abstract class NpcTypeDataMixin implements CustomNpcTactics.Holder
{
    @Shadow(remap = false)
    private EntityNPCInterface npc;

    @Unique
    private final CustomNpcTactics wolffsmodnpcsTactics = new CustomNpcTactics();

    @Override
    public CustomNpcTactics wolffsmodnpcsCustomTactics()
    {
        return wolffsmodnpcsTactics;
    }

    // Upstream scripting compatibility. These methods implement the original INPCAi signatures restored by INPCAiMixin.
    public int getTacticalType()
    {
        return wolffsmodnpcsTactics.getVariant();
    }

    public void setTacticalType(int type)
    {
        wolffsmodnpcsTactics.setVariant(type);
        npc.updateAI = true;
    }

    public int getTacticalRange()
    {
        return wolffsmodnpcsTactics.getRange();
    }

    public void setTacticalRange(int range)
    {
        wolffsmodnpcsTactics.setRange(range);
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsLoadedDefaults(CompoundTag tag, CallbackInfo callback)
    {
        if ((Object) this instanceof DataAI)
            wolffsmodnpcsTactics.load(tag);
        NpcTypeProperties.of(npc).loaded(NpcTypeProperties.component(this));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsStoredDefaults(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        if ((Object) this instanceof DataAI)
            wolffsmodnpcsTactics.save(callback.getReturnValue());
        NpcTypeProperties.of(npc).saveDefaults(NpcTypeProperties.component(this), callback.getReturnValue());
    }
}
