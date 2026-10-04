package com.wolffsmod.npcs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.client.FlanModelBounds;
import com.wolffsmod.npcs.client.FlanNameplatePlacement;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;

/** Custom NPCs owns label visibility and appearance; only the Flan model's height changes. */
@Mixin(value = RenderNPCInterface.class, remap = false)
public abstract class NpcNameplateMixin
{
    @SuppressWarnings("unchecked")
    @ModifyVariable(method = "renderLivingLabel", at = @At("STORE"), ordinal = 1)
    private float wolffsmodnpcs$nameHeight(float original, EntityNPCInterface npc,
        PoseStack pose, MultiBufferSource buffers, int light)
    {
        if (!(npc instanceof EntityCustomNpc custom)
            || !(custom.modelData.getEntity(custom) instanceof FlanModelEntity model))
            return original;
        float scale = custom.modelData.simpleRender ? 1F : custom.display.getSize() / 5F;
        var renderer = (EntityRenderer<EntityNPCInterface>)(Object)this;
        double offset = renderer.getRenderOffset(npc, 0).y;
        return FlanNameplatePlacement.height(original, npc.getBbHeight(), FlanModelBounds.of(model), scale, offset);
    }
}
