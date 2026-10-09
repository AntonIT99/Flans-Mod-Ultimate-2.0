package com.wolffsmod.npcs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Widens NPC module parameter help while retaining the default wrapping for other widget tooltips. */
@Mixin(Tooltip.class)
@SuppressWarnings("java:S1118") // Mixin handlers are merged into Tooltip; this is not an instantiable utility class.
public abstract class TooltipMixin
{
    @Unique
    private static final int WOLFFSMODNPCS_TOOLTIP_WIDTH = 300;

    @ModifyArg(method = "splitTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 1)
    private static int wolffsmodnpcsTooltipWidth(FormattedText text, int width)
    {
        if (text instanceof Component component && component.getContents() instanceof TranslatableContents translation && translation.getKey().startsWith("wolffsmodnpcs."))
            return Math.min(WOLFFSMODNPCS_TOOLTIP_WIDTH, Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 24));
        return width;
    }
}
