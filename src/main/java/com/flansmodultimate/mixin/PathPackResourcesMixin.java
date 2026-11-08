package com.flansmodultimate.mixin;

import com.flansmodultimate.util.TypeReaderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.Util;
import net.minecraft.server.packs.PathPackResources;

/**
 * Hooks into folder resource packs:
 * <ul>
 * <li>Content pack .obj models and animations keep their legacy mixed-case names and are read directly from the pack, so the vanilla invalid path message about them is a false positive.</li>
 * </ul>
 */
@Mixin(PathPackResources.class)
public abstract class PathPackResourcesMixin
{
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;logAndPauseIfInIde(Ljava/lang/String;)V"))
    private static void flansmodultimateRouteInvalidPathMessage(String message)
    {
        if (TypeReaderUtils.isDirectlyReadPackFileMessage(message))
            TypeReaderUtils.logRecoverable(message);
        else
            Util.logAndPauseIfInIde(message);
    }
}
