package com.flansmodultimate.mixin;

import com.flansmodultimate.client.model.ItemIconExtrusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.List;

/**
 * Hooks for {@link ItemModelGenerator}.
 * <ul>
 * <li>Item icon atlas: builds the edge walls of large Flan icons from a coarse outline.</li>
 * </ul>
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin
{
    @Inject(method = "createSideElements", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateCoarseIconWalls(SpriteContents sprite, String texture, int tintIndex, CallbackInfoReturnable<List<BlockElement>> cir)
    {
        List<BlockElement> walls = ItemIconExtrusion.sideElements(sprite, texture, tintIndex);
        if (walls != null)
            cir.setReturnValue(walls);
    }
}
