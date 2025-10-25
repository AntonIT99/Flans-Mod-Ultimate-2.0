package com.wolffsarmormod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.common.item.CustomArmorItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHudOverlays
{
    private static final ResourceLocation HIT_MARKER = ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "textures/gui/hit_marker.png");

    public static final IGuiOverlay SCOPE = (gui, g, partialTick, sw, sh) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        ResourceLocation scopeTexture = null;

        boolean hasScope = ModClient.getCurrentScope() != null && ModClient.getCurrentScope().hasZoomOverlay();
        boolean noScreen = Minecraft.getInstance().screen == null;
        boolean zoomedIn = ModClient.getZoomProgress() > 0.8F;

        if (hasScope && noScreen && zoomedIn)
            scopeTexture = ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, "textures/gui/" + ModClient.getCurrentScope().getZoomOverlay() + ".png");


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
                {
                    Optional<ResourceLocation> overlayTexture = armorItem.getOverlay();
                    overlayTexture.ifPresent(tex -> renderArmorOverlay(g, tex, sw, sh));
                }
            });
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

    /** Draw the hit marker at screen center with fade-out alpha. */
    public static void renderHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        if (ModClient.getHitMarkerTime() <= 0)
            return;

        // Fade like old code: ((time - 10 + pt) / 10)^+ clamped
        float a = Math.max((ModClient.getHitMarkerTime() - 10.0f + partialTick) / 10.0f, 0.0f);

        // 9x9 region from a 16x16 texture, centered
        int w = 9;
        int h = 9;
        int x = sw / 2 - 4;
        int y = sh / 2 - 4;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, a);
        g.blit(HIT_MARKER, x, y, 0, 0, w, h, 16, 16);
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

    //TODO
    public static void renderPlayerAmmo(GuiGraphics g, int sw, int sh) {}
    public static void renderTeamInfo(GuiGraphics g, int sw, int sh) {}
    public static void renderKillMessages(GuiGraphics g, int sw, int sh) {}
    public static void renderVehicleDebug(GuiGraphics g, int sw, int sh) {}
}
