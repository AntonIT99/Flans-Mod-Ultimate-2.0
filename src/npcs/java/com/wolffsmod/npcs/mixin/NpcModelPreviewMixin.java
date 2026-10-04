package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.FlanModelMenuPreview;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = GuiCreationScreenInterface.class, remap = false)
public abstract class NpcModelPreviewMixin
{
    @Redirect(method = "render", at = @At(value = "INVOKE", target =
        "Lnoppes/npcs/client/gui/model/GuiCreationScreenInterface;drawNpc(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/LivingEntity;IIFI)V"))
    private void wolffsmodnpcs$fitPreview(GuiCreationScreenInterface screen, GuiGraphics graphics,
        LivingEntity npc, int x, int y, float zoom, int rotation)
    {
        if (screen instanceof GuiCreationEntities && screen.entity instanceof FlanModelEntity model)
            FlanModelMenuPreview.render(screen, graphics, model, rotation);
        else
            screen.drawNpc(graphics, npc, x, y, zoom, rotation);
    }
}
