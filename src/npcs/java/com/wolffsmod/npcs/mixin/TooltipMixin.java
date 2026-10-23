package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.BotEditorScreen;
import com.wolffsmod.npcs.client.BotOrdersScreen;
import com.wolffsmod.npcs.client.GuiWolffsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Widens the tooltips of the NPC module, those on its screens and any naming one of its texts, while retaining the default
 * wrapping for other widget tooltips.
 */
@Mixin(Tooltip.class)
@SuppressWarnings("java:S1118") // Mixin handlers are merged into Tooltip; this is not an instantiable utility class.
public abstract class TooltipMixin
{
    @Unique
    private static final int WOLFFSMODNPCS_TOOLTIP_WIDTH = 360;
    @Unique
    private static final String WOLFFSMODNPCS_KEY_PREFIX = "wolffsmodnpcs.";

    @ModifyArg(method = "splitTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 1)
    @SuppressWarnings("MathClampMigration") // The vanilla minimum can exceed our preferred maximum; retain that minimum.
    private static int wolffsmodnpcsTooltipWidth(FormattedText text, int width)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!wolffsmodnpcsOwnScreen(minecraft.screen) && !(text instanceof Component component && wolffsmodnpcsNamesOwnText(component)))
            return width;
        return Math.max(width, Math.min(WOLFFSMODNPCS_TOOLTIP_WIDTH, minecraft.getWindow().getGuiScaledWidth() - 24));
    }

    @Unique
    private static boolean wolffsmodnpcsOwnScreen(Screen screen)
    {
        return screen instanceof GuiWolffsMod || screen instanceof BotEditorScreen || screen instanceof BotOrdersScreen;
    }

    /** Whether the text, or a part appended to it, is one of the module's translations. */
    @Unique
    private static boolean wolffsmodnpcsNamesOwnText(Component component)
    {
        if (component.getContents() instanceof TranslatableContents translation && translation.getKey().startsWith(WOLFFSMODNPCS_KEY_PREFIX))
            return true;
        return component.getSiblings().stream().anyMatch(TooltipMixin::wolffsmodnpcsNamesOwnText);
    }
}
