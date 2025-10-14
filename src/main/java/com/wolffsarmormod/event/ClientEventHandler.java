package com.wolffsarmormod.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.item.CustomArmorItem;
import com.wolffsarmormod.common.item.GunItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler
{
    /**
     * Set gui overlay for armors
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.getCameraType() != CameraType.FIRST_PERSON || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type())
        {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        for (EquipmentSlot slot : Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmor).toList())
        {
            if (player.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem)
            {
                Optional<ResourceLocation> overlayTexture = armorItem.getOverlay();
                if (overlayTexture.isEmpty())
                    continue;

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, overlayTexture.get());

                guiGraphics.blit(overlayTexture.get(), 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    /**
     * Aim Pose when GunItem is held by a humanoid entity
     */
    @SubscribeEvent
    public static void onLiving(RenderLivingEvent.Pre<?, ?> event)
    {
        var model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        ItemStack main = event.getEntity().getMainHandItem();
        ItemStack off  = event.getEntity().getOffhandItem();
        boolean force = isGunItem(main) || isGunItem(off);
        if (!force) return;

        // Force the bow-aiming arm pose on both arms
        humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    private static boolean isGunItem(ItemStack s)
    {
        return !s.isEmpty() && s.getItem() instanceof GunItem;
    }
}
