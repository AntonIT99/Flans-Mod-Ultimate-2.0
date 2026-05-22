package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHudOverlays
{
    @FunctionalInterface
    interface OverlayRenderer {
        void render(net.minecraft.client.gui.Gui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight);
    }

    private static final ResourceLocation AMMO_GUI_TEXTURE = ResourceLocation.tryParse("flansmod:textures/gui/ammoGui.png");
    private static final int TEXTURE_WIDTH = 120;
    private static final int TEXTURE_HEIGHT = 64;
    private static final double BAR_MAX_WIDTH = 14.5;
    private static final int BAR_HEIGHT = 3;
    private static final double BAR_REFERENCE = 100.0;

    private static final double[] BAR_X_OFFSETS = {
        2.0, 19.0, 36.0, 53.0, 70.0, 87.0, 104.0
    };

    public static final OverlayRenderer SCOPE = (gui, g, partialTick, sw, sh) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        ResourceLocation scopeTexture = null;

        boolean hasScope = ModClient.getCurrentScope() != null && ModClient.getCurrentScope().hasZoomOverlay();
        boolean noScreen = Minecraft.getInstance().screen == null;
        boolean zoomedIn = ModClient.getZoomProgress() > 0.8F;

        if (hasScope && noScreen && zoomedIn)
            scopeTexture = ModClient.getCurrentScope().getZoomOverlay();

        if (scopeTexture != null)
            renderScopeOverlay(g, scopeTexture, sw, sh);
    };

    public static void renderArmorOverlay(GuiGraphics g, int sw, int sh)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableCull();

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (!slot.isArmor())
                continue;
            if (player.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem)
            {
                armorItem.getConfigType().getOverlay().ifPresent(overlayTexture -> 
                    g.blit(overlayTexture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh));
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public static final OverlayRenderer ARMOR = (gui, g, partialTick, sw, sh) ->
        renderArmorOverlay(g, sw, sh);

    public static final OverlayRenderer HUD = (gui, g, partialTick, sw, sh) -> {
        renderPlayerAmmo(g, sw, sh);
        renderDigitalAmmo(g, sw, sh);
        renderTeamInfo(g, sw, sh);
        renderKillMessages(g, sw, sh);
        renderVehicleDebug(g, sw, sh);
    };

    //TODO: FMU Style hit marker
    /** Draw the hit marker at screen center with fade-out alpha. */
    public static void renderHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        if (ModClient.getHitMarkerTime() <= 0)
            return;

        float a = Math.max((ModClient.getHitMarkerTime() - 10.0f + partialTick) / 10.0f, 0.0f);

        int w = 9;
        int h = 9;
        int x = sw / 2 - 5;
        int y = sh / 2 - 5;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, a);
        g.blit(FlansMod.hitmarkerTexture, x, y, 0, 0, w, h, 16, 16);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Fullscreen scope/helmet overlay; mirrors your old quad (centered square using screen height). */
    public static void renderScopeOverlay(GuiGraphics g, ResourceLocation texture, int sw, int sh)
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(texture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh);

        //TODO: compare with renderArmorOverlay()
    }

    public static void renderPlayerAmmo(GuiGraphics g, int sw, int sh)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        Font font = mc.font;

        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
                continue;

            GunType gunType = gunItem.getConfigType();

            EnumFireMode currentMode = gunType.getFireMode(stack);
            String modeText = currentMode.getDisplayName();
            int modeX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 : sw / 2 - 132 - font.width(modeText);
            int modeY = sh - 31;
            int modeColor = gunType.canSwitchFireMode(stack) ? 0xAAAAFF : 0x888888;
            g.drawString(font, modeText, modeX + 1, modeY + 1, 0x000000, false);
            g.drawString(font, modeText, modeX,     modeY,     modeColor, false);

            int xAccum = 0;

            List<ItemStack> bulletStacks = gunItem.getBulletItemStackList(stack, net.minecraft.client.Minecraft.getInstance().level.registryAccess());
            
            java.util.Map<ShootableItem, Integer> simpleAmmoTotals = new java.util.LinkedHashMap<>();
            java.util.Map<ShootableItem, ItemStack> simpleAmmoSamples = new java.util.LinkedHashMap<>();
            java.util.List<ItemStack> magazineAmmo = new java.util.ArrayList<>();

            for (ItemStack bulletStack : bulletStacks)
            {
                if (bulletStack == null || bulletStack.isEmpty())
                    continue;

                if (!(bulletStack.getItem() instanceof ShootableItem shootableItem))
                    continue;

                int roundsPerItem = shootableItem.getConfigType().getRoundsPerItem();
                if (roundsPerItem <= 1)
                {
                    simpleAmmoTotals.merge(shootableItem, bulletStack.getCount(), Integer::sum);
                    simpleAmmoSamples.putIfAbsent(shootableItem, bulletStack);
                }
                else
                {
                    magazineAmmo.add(bulletStack);
                }
            }

            for (java.util.Map.Entry<ShootableItem, Integer> entry : simpleAmmoTotals.entrySet())
            {
                ShootableItem shootableItem = entry.getKey();
                int totalCount = entry.getValue();
                ItemStack sampleStack = simpleAmmoSamples.get(shootableItem);

                int iconX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 + xAccum : sw / 2 - 176 - xAccum;
                int iconY = sh - 19;

                g.renderItem(sampleStack, iconX, iconY);

                String s = totalCount > 1 ? String.valueOf(totalCount) : "";

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                if (!s.isEmpty())
                {
                    g.drawString(font, s, textX + 1, textY + 1, 0x000000, false);
                    g.drawString(font, s, textX,     textY,     0xFFFFFF, false);
                }

                xAccum += 16 + font.width(s);
            }

            for (ItemStack bulletStack : magazineAmmo)
            {
                int max = ((ShootableItem) bulletStack.getItem()).getConfigType().getRoundsPerItem();
                int remaining = ShootableItem.getRoundsRemaining(bulletStack);

                if (remaining <= 0)
                    continue;

                int iconX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 + xAccum : sw / 2 - 176 - xAccum;
                int iconY = sh - 19;

                g.renderItem(bulletStack, iconX, iconY);
                g.renderItemDecorations(font, bulletStack, iconX, iconY);

                int stackCount = bulletStack.getCount();
                String s;
                if (stackCount > 1)
                    s = remaining + "/" + max + " x" + stackCount;
                else
                    s = remaining + "/" + max;

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                g.drawString(font, s, textX + 1, textY + 1, 0x000000, false);
                g.drawString(font, s, textX,     textY,     0xFFFFFF, false);

                xAccum += 16 + font.width(s);
            }
        }
    }

    //TODO: implement these methods
    public static void renderTeamInfo(GuiGraphics g, int sw, int sh) {}
    public static void renderKillMessages(GuiGraphics g, int sw, int sh) {}
    public static void renderVehicleDebug(GuiGraphics g, int sw, int sh) {}

    public static void renderDigitalAmmo(GuiGraphics g, int sw, int sh)
    {
        if (!ModCommonConfig.get().enableDigitalAmmoSystem())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        int numTypes = Math.min(LocalBulletManager.getNumTypes(), BAR_X_OFFSETS.length);

        RenderSystem.setShaderTexture(0, AMMO_GUI_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int bgX = 10;
        int bgY = sh - 50;

        g.blit(AMMO_GUI_TEXTURE, bgX, bgY, 0, 30, 120, 12, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        for (int i = 0; i < numTypes; i++)
        {
            double amount = LocalBulletManager.getBullets(i + 1);
            double percentage = Math.min(amount / BAR_REFERENCE, 1.0);
            int barWidth = (int) (BAR_MAX_WIDTH * percentage);

            if (barWidth > 0)
            {
                int barX = (int) Math.round(bgX + BAR_X_OFFSETS[i]);
                int barY = bgY + 12;

                RenderSystem.setShaderTexture(0, AMMO_GUI_TEXTURE);
                g.blit(AMMO_GUI_TEXTURE, barX, barY, 2, 18, barWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }

        RenderSystem.disableBlend();
    }
}
