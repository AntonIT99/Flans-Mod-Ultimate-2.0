package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelRendering;
import com.wolffsmod.npcs.properties.NpcModelHealth;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/** Enforces normal Flan rendering and captures entity-model health before Custom NPCs copies the NPC's health onto it. */
@Mixin(value = ModelData.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into ModelData, so the self-cast is valid.
public abstract class ModelDataMixin implements NpcModelHealth
{
    @Unique
    private double wolffsmodnpcsModelMaxHealth = Double.NaN;

    @Inject(method = "load", at = @At("TAIL"))
    private void wolffsmodnpcsLoadRenderer(CompoundTag tag, CallbackInfo callback)
    {
        FlanModelRendering.loaded((ModelData) (Object) this, tag);
        wolffsmodnpcsModelMaxHealth = Double.NaN;
    }

    @Inject(method = "getEntity", at = @At("HEAD"))
    private void wolffsmodnpcsUseRenderer(EntityNPCInterface npc, CallbackInfoReturnable<LivingEntity> callback)
    {
        FlanModelRendering.enforce((ModelData) (Object) this);
    }

    // getEntity copies MAX_HEALTH after loading the model's extra NBT and before copying the NPC's equipment.
    @SuppressWarnings("DefaultAnnotationParam") // Explicitly remap the vanilla call inside this non-remapped Custom NPCs mixin.
    @Redirect(method = "getEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V", remap = true))
    private void wolffsmodnpcsCaptureModelHealth(AttributeInstance attribute, double maximum)
    {
        wolffsmodnpcsModelMaxHealth = attribute.getValue();
        attribute.setBaseValue(maximum);
    }

    @Override
    public double wolffsmodnpcsModelMaxHealth()
    {
        return wolffsmodnpcsModelMaxHealth;
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void wolffsmodnpcsSaveRenderer(CallbackInfoReturnable<CompoundTag> callback)
    {
        FlanModelRendering.enforce((ModelData) (Object) this);
    }
}
