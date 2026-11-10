package com.flansmodultimate.client.eras;

import com.flansmodultimate.common.eras.EraEnforcement;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.ServicePeriod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** What era restrictions change on screen: tooltip lines, greyed slots and the contents of creative tabs. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraClientViews
{
    /** Drawn over a blocked item's slot: dark and slightly red, so the icon stays recognisable. */
    private static final int BLOCKED_OVERLAY = 0x90401818;

    /** Adds the item's service dates and, when it is blocked for the local player, the window it falls outside. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip)
    {
        InfoType type = EraEnforcement.typeOf(stack);
        if (type == null)
            return;
        ServicePeriod period = EraClientState.periodOf(type);
        if (period != null)
            tooltip.add(Component.translatable("tooltip.flansmodultimate.era.service", EraEnforcement.describePeriod(period)).withStyle(ChatFormatting.GRAY));
        EraVerdicts.Verdict verdict = EraClientState.check(type);
        if (!verdict.allowed())
        {
            String[] window = EraEnforcement.describeWindow(verdict.blockedBy(), EraClientState.state().eras());
            tooltip.add(Component.translatable("tooltip.flansmodultimate.era.blocked", window[0], window[1]).withStyle(ChatFormatting.RED));
        }
    }

    /** Greys a blocked item's slot; never replaces the item's own rendering. */
    public static void renderSlotOverlay(GuiGraphics graphics, ItemStack stack, int x, int y)
    {
        InfoType type = EraEnforcement.typeOf(stack);
        if (type == null || EraClientState.check(type).allowed())
            return;
        graphics.pose().pushPose();
        // Above the item and its count, as vanilla's cooldown overlay is.
        graphics.pose().translate(0F, 0F, 200F);
        graphics.fill(x, y, x + 16, y + 16, BLOCKED_OVERLAY);
        graphics.pose().popPose();
    }

    /** Rebuilds the creative tabs after the restrictions on the local player changed, reopening the creative screen if shown. */
    public static void refreshCreativeTabs()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null)
            return;
        FeatureFlagSet features = minecraft.player.connection.enabledFeatures();
        boolean operatorItems = minecraft.player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(features, operatorItems, minecraft.level.registryAccess());
        for (CreativeModeTab tab : CreativeModeTabs.allTabs())
        {
            if (tab.getType() == CreativeModeTab.Type.SEARCH)
                continue;
            try
            {
                tab.buildContents(parameters);
            }
            catch (RuntimeException exception)
            {
                FlansLog.log.warn("Could not rebuild creative tab {} after an era change", tab.getDisplayName().getString(), exception);
            }
        }
        // The search tab lists what the other tabs hold, so it is rebuilt last.
        CreativeModeTabs.searchTab().buildContents(parameters);
        if (minecraft.screen instanceof CreativeModeInventoryScreen)
            minecraft.setScreen(new CreativeModeInventoryScreen(minecraft.player, features, operatorItems));
    }
}
