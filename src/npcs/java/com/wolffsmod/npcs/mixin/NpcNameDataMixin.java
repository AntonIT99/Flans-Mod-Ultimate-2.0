package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.NpcDisplayNames;
import com.wolffsmod.npcs.names.SoldierNames;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Accepts soldier generators and saves/synchronizes independent content name/title bindings, clearing them on literal edits. */
@Mixin(value = DataDisplay.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges the holder into DataDisplay.
public abstract class NpcNameDataMixin implements NpcDisplayNames.Holder
{
    @Shadow
    private int markovGeneratorId;
    @Shadow
    private EntityNPCInterface npc;
    @Unique
    private final NpcDisplayNames wolffsmodnpcsNames = new NpcDisplayNames();

    @Override
    public NpcDisplayNames wolffsmodnpcsDisplayNames()
    {
        return wolffsmodnpcsNames;
    }

    @Override
    public void wolffsmodnpcsNamesChanged()
    {
        npc.updateClient = true;
        if (npc.bossInfo != null)
            npc.bossInfo.setName(npc.getDisplayName());
    }

    @Inject(method = "setName", at = @At("HEAD"))
    private void wolffsmodnpcsLiteralName(String value, CallbackInfo callback)
    {
        if (!((DataDisplay) (Object) this).getName().equals(value))
            wolffsmodnpcsNames.setName(null);
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void wolffsmodnpcsLiteralTitle(String value, CallbackInfo callback)
    {
        if (!((DataDisplay) (Object) this).getTitle().equals(value))
            wolffsmodnpcsNames.setTitle(null);
    }

    @Inject(method = "setMarkovGeneratorId", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsAcceptSoldierList(int generator, CallbackInfo callback)
    {
        if (SoldierNames.contains(generator))
        {
            markovGeneratorId = generator;
            callback.cancel();
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsSaveNameList(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        SoldierNames.saveSelection(markovGeneratorId, callback.getReturnValue());
        DataDisplay display = (DataDisplay) (Object) this;
        wolffsmodnpcsNames.save(callback.getReturnValue(), display.getName(), display.getTitle());
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsLoadNameList(CompoundTag tag, CallbackInfo callback)
    {
        markovGeneratorId = SoldierNames.loadSelection(markovGeneratorId, tag);
        DataDisplay display = (DataDisplay) (Object) this;
        wolffsmodnpcsNames.load(tag, display.getName(), display.getTitle());
        if (npc.bossInfo != null)
            npc.bossInfo.setName(npc.getDisplayName());
    }
}
