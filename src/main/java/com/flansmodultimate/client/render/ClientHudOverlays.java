package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHudOverlays
{
    public static final IGuiOverlay SCOPE = (gui, g, partialTick, sw, sh) -> {
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

    public static final IGuiOverlay ARMOR = (gui, g, partialTick, sw, sh) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        Arrays.stream(EquipmentSlot.values())
            .filter(EquipmentSlot::isArmor)
            .forEach(slot -> {
                if (player.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem)
                    armorItem.getConfigType().getOverlay().ifPresent(overlayTexture -> renderArmorOverlay(g, overlayTexture, sw, sh));
            });
    };

    public static final IGuiOverlay HUD = (gui, g, partialTick, sw, sh) -> {
        renderPlayerAmmo(g, sw, sh);
        renderTeamInfo(g, sw, sh);
        renderKillMessages(g, sw, sh);
        renderVehicleDebug(g, sw, sh);
    };

    private static void renderArmorOverlay(GuiGraphics g, ResourceLocation texture, int sw, int sh)
    {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableCull();

        g.blit(texture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

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

        // Render ammo for each hand
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
                continue;

            GunType gunType = gunItem.getConfigType();
            int xAccum = 0;

            for (ItemStack bulletStack : gunItem.getBulletItemStackList(stack))
            {
                if (bulletStack == null || bulletStack.isEmpty())
                    continue;

                int max = bulletStack.getMaxDamage();
                int used = bulletStack.getDamageValue();
                int remaining = max - used;

                // Skip fully empty if it’s a damage-based magazine and shows as 0/Max
                if (max > 0 && remaining <= 0)
                    continue;

                // Position: right of center for MAIN_HAND, left for OFF_HAND
                int iconX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 + xAccum : sw / 2 - 176 - xAccum;
                int iconY = sh - 19;

                // Draw the item icon (no manual lighting/GL in 1.20+)
                g.renderItem(bulletStack, iconX, iconY);
                // Optional: vanilla decorations (stack count, durability bar). Keep if useful:
                g.renderItemDecorations(font, bulletStack, iconX, iconY);

                // Text “remaining/max” unless it’s a 1-durability item (old behavior)
                String s = (max == 1) ? "" : (remaining + "/" + max);

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                if (!s.isEmpty())
                {
                    g.drawString(font, s, textX + 1, textY + 1, 0x000000, false);
                    g.drawString(font, s, textX,     textY,     0xFFFFFF, false);
                }

                xAccum += 16 + font.width(s);
            }
        }
    }

    //TODO: implement these methods
    public static void renderTeamInfo(GuiGraphics g, int sw, int sh) {}
    public static void renderKillMessages(GuiGraphics g, int sw, int sh) {}
    public static void renderVehicleDebug(GuiGraphics g, int sw, int sh) {}
}
