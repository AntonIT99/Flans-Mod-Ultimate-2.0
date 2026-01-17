package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GunWorkbenchScreen extends AbstractContainerScreen<GunWorkbenchMenu>
{
    private static final int ATTACH_SLOTS_GUI_X = 16;
    private static final int ATTACH_SLOTS_GUI_Y = 89;
    private static final int GENERIC_SLOTS_GUI_Y = 115;
    private static final int SLOT_SIZE = 18;
    private final int[] lastStats = new int[] {0, 0, 0, 0, 0};

    private static final int BAR_W = 80;
    private static final int BAR_H = 10;
    private static final int BAR_MIN_PX = 2;
    private static final float DMG_MIN = 1F;
    private static final float DMG_MAX = 25F;
    private static final float DISP_MIN = 0.2F;
    private static final float DISP_MAX = 3F;
    private static final float RECOIL_MIN = 1F;
    private static final float RECOIL_MAX = 8F;
    private static final float REL_MIN = 1F;
    private static final float REL_MAX = 8F;

    private Paintjob hoveringPaintjob;
    private Component hoveringModSlotTooltip;
    private boolean flipGunModel;

    public GunWorkbenchScreen(GunWorkbenchMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        imageWidth = 331;
        imageHeight = 236;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // flip button
        double m = mouseX - leftPos;
        double n = mouseY - topPos;
        if ((button == 0 || button == 1) && m >= 146 && m <= 165 && n >= 63 && n <= 72) {
            flipGunModel = !flipGunModel;
            return true;
        }

        // paintjob click → send packet
        if (button == 0 && hoveringPaintjob != null && !menu.getGunStack().isEmpty())
        {
            PacketHandler.sendToServer(new PacketSelectPaintjob(hoveringPaintjob.getId()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(gg);
        updateHoveringModSlotTooltip(mouseX, mouseY);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
        renderCustomTooltips(gg, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY)
    {
        final int color = 0x404040;

        gg.drawString(font, "Gun Modification Table", 6, 6, color, false);
        gg.drawString(font, "Inventory", 7, 142, color, false);
        gg.drawString(font, "Gun Information", 179, 22, color, false);
        gg.drawString(font, "Paint Jobs", 179, 128, color, false);

        ItemStack gunStack = menu.getGunStack();
        if (!gunStack.isEmpty() && gunStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();

            // text stats
            gg.drawString(font, gunStack.getHoverName(), 207, 36, color, false);
            gg.drawString(font, Component.literal(gunType.getDescription()), 207, 46, color, false);

            gg.drawString(font, "Damage", 181, 61, color, false);
            gg.drawString(font, "Dispersion", 181, 73, color, false);
            gg.drawString(font, "Recoil", 181, 85, color, false);
            gg.drawString(font, "Reload", 181, 97, color, false);
            gg.drawString(font, "Control", 181, 109, color, false);

            gg.drawString(font, "Sprint", 240, 119, color, false);
            gg.drawString(font, "Sneak", 290, 119, color, false);

            gg.drawString(font, String.valueOf(round2(getDamageStat(gunStack, gunItem))), 241, 62, color, false);
            gg.drawString(font, round2(gunType.getDispersionForDisplay(gunStack)) + "°", 241, 74, color, false);
            gg.drawString(font, String.valueOf(round2(gunType.getDisplayVerticalRecoil(gunStack))), 241, 86, color, false);
            gg.drawString(font, round2(gunType.getReloadTime(gunStack)/ 20F) + "s", 241, 98, color, false);

            float sprinting = roundN(1F - gunType.getRecoilControl(gunStack, true, false), 2);
            float normal = roundN(1F - gunType.getRecoilControl(gunStack, false, false), 2);
            float sneaking = roundN(1F - gunType.getRecoilControl(gunStack, false, true), 2);
            gg.drawString(font, String.format("%3.2f  %3.2f  %3.2f", sprinting, normal, sneaking), 241, 110, color, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderTexture(0, FlansMod.gunWorkbenchGuiTexture);

        // whole background
        gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 512, 256);

        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
        {
            hoveringPaintjob = null;
            lastStats[0] = lastStats[1] = lastStats[2] = lastStats[3] = lastStats[4] = 0;
            return;
        }

        GunType type = gunItem.getConfigType();

        // flip button
        gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 146, topPos + 63, 340F, 166F, 20, 10, 512, 256);

        // stat bars
        float damage = getDamageStat(gunStack, gunItem);
        float dispersion = type.getDispersionForDisplay(gunStack);
        float recoil = type.getDisplayVerticalRecoil(gunStack);
        float reloadTime = type.getReloadTime(gunStack) / 20F;
        int[] targetsPx = new int[]
        {
            ratioToPixels(ratioGood(damage, DMG_MIN, DMG_MAX, false)),
            ratioToPixels(ratioGood(dispersion, DISP_MIN, DISP_MAX, true)),
            ratioToPixels(ratioGood(recoil, RECOIL_MIN, RECOIL_MAX, true)),
            ratioToPixels(ratioGood(reloadTime, REL_MIN, REL_MAX, true)),
            0
        };
        renderStatBars(gg, targetsPx);

        // attachment icons
        boolean[] allow = new boolean[] { type.isAllowBarrelAttachments(), type.isAllowScopeAttachments(), type.isAllowStockAttachments(), type.isAllowGripAttachments(), type.isAllowGadgetAttachments(), type.isAllowSlideAttachments(), type.isAllowPumpAttachments(), type.isAllowAccessoryAttachments() };

        for (int m = 0; m < allow.length; m++)
        {
            if (allow[m])
                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + ATTACH_SLOTS_GUI_X + (m * SLOT_SIZE), topPos + ATTACH_SLOTS_GUI_Y - 1, 340F + (m * SLOT_SIZE), 136F, SLOT_SIZE, SLOT_SIZE, 512, 256);
        }

        // generic attachment slot backgrounds
        for (int x = 0; x < 8; x++)
        {
            if (x < type.getNumGenericAttachmentSlots())
                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + ATTACH_SLOTS_GUI_X + (SLOT_SIZE * x), topPos + GENERIC_SLOTS_GUI_Y - 1, 340F, 100F, SLOT_SIZE, SLOT_SIZE, 512, 256);
        }

        List<Paintjob> applicable = getApplicablePaintjobs(type);

        renderGunPreview(gg, gunStack);
        renderPaintjobs(gg, gunStack, type, applicable);
        updateHoveringPaintjob(mouseX, mouseY, applicable);
        renderDyeRequirementsRow(gg, hoveringPaintjob);
    }

    private static float getDamageStat(ItemStack gunStack, GunItem gunItem)
    {
        float damage;
        if (gunItem.getAmmoItemStack(gunStack, 0).getItem() instanceof ShootableItem shootableItem)
            damage = shootableItem.getConfigType().getDamageForDisplay(gunItem.getConfigType(), gunStack, null);
        else
            damage = gunItem.getConfigType().getAmmoTypes().get(0).getDamageForDisplay(gunItem.getConfigType(), gunStack, null);
        if (damage == 0F && gunItem.getConfigType().getMeleeDamage(gunStack, false) > 0)
            damage = gunItem.getConfigType().getMeleeDamage(gunStack, false);
        return damage;
    }

    private void updateHoveringModSlotTooltip(int mouseX, int mouseY)
    {
        hoveringModSlotTooltip = null;

        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
            return;

        GunType type = gunItem.getConfigType();

        String[] text = {"Barrel", "Scope", "Stock", "Grip", "Gadget", "Slide", "Pump", "Accessory"};
        boolean[] allow = {type.isAllowBarrelAttachments(), type.isAllowScopeAttachments(), type.isAllowStockAttachments(), type.isAllowGripAttachments(), type.isAllowGadgetAttachments(), type.isAllowSlideAttachments(), type.isAllowPumpAttachments(), type.isAllowAccessoryAttachments()};

        int guiX = mouseX - leftPos;
        int guiY = mouseY - topPos;

        final int slotY = ATTACH_SLOTS_GUI_Y;
        for (int a = 0; a < allow.length; a++)
        {
            int slotX = ATTACH_SLOTS_GUI_X + a * SLOT_SIZE;

            if (allow[a]
                && guiX >= slotX && guiX < slotX + SLOT_SIZE
                && guiY >= slotY && guiY < slotY + SLOT_SIZE
                && menu.isAttachmentSlotEmpty(a))
            {
                hoveringModSlotTooltip = Component.literal(text[a]);
                return;
            }
        }
    }

    private void renderGunPreview(GuiGraphics gg, ItemStack gunStack)
    {
        var pose = gg.pose();
        pose.pushPose();
        pose.translate(leftPos + 90F, topPos + 45F, 100F);

        pose.mulPose(Axis.XP.rotationDegrees(160));
        pose.mulPose(Axis.YP.rotationDegrees(30));
        if (flipGunModel)
        {
            pose.mulPose(Axis.YP.rotationDegrees(180));
        }
        pose.scale(-60F, 60F, 60F);

        Lighting.setupFor3DItems();
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(gunStack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, gg.bufferSource(), mc.level, 0);
        gg.flush();
        pose.popPose();
        Lighting.setupForFlatItems();
    }

    private void renderPaintjobs(GuiGraphics gg, ItemStack gunStack, GunType type, List<Paintjob> applicable)
    {
        int num = applicable.size();
        int rows = num / 2 + 1;

        for (int y = 0; y < rows; y++)
        {
            for (int x = 0; x < 2; x++)
            {
                int idx = 2 * y + x;
                if (idx >= num) continue;

                int slotX = 181 + SLOT_SIZE * x;
                int slotY = 150 + SLOT_SIZE * y;

                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + slotX, topPos + slotY, 340F, 100F, SLOT_SIZE, SLOT_SIZE, 512, 256);

                Paintjob pj = applicable.get(idx);

                ItemStack icon = gunStack.copy();
                type.applyPaintjobToStack(icon, pj);

                gg.renderItem(icon, leftPos + slotX + 1, topPos + slotY + 1);
                gg.renderItemDecorations(this.font, icon, leftPos + slotX + 1, topPos + slotY + 1);
            }
        }
    }

    private void updateHoveringPaintjob(int mouseX, int mouseY, List<Paintjob> applicable)
    {
        hoveringPaintjob = null;

        int num = applicable.size();
        int rows = num / 2 + 1;

        int guiX = mouseX - leftPos;
        int guiY = mouseY - topPos;

        for (int y = 0; y < rows; y++)
        {
            for (int x = 0; x < 2; x++)
            {
                int idx = 2 * y + x;
                if (idx >= num) continue;

                int slotX = 181 + x * SLOT_SIZE;
                int slotY = 150 + y * SLOT_SIZE;

                if (guiX >= slotX && guiX < slotX + SLOT_SIZE && guiY >= slotY && guiY < slotY + SLOT_SIZE)
                {
                    hoveringPaintjob = applicable.get(idx);
                    return;
                }
            }
        }
    }

    private static List<Paintjob> getApplicablePaintjobs(GunType type)
    {
        List<Paintjob> applicable = new ArrayList<>();
        if (type.isAddAnyPaintjobToTables())
        {
            for (Paintjob pj : type.getPaintjobs().values())
                if (pj.isAddToTables())
                    applicable.add(pj);
        }
        applicable.sort(Comparator.comparingInt(Paintjob::getId));
        return applicable;
    }

    private void renderDyeRequirementsRow(GuiGraphics gg, Paintjob paintjob)
    {
        if (paintjob == null || minecraft == null || minecraft.player == null || minecraft.player.getAbilities().instabuild)
            return;

        List<Supplier<ItemStack>> needed = paintjob.getDyesNeeded();
        if (needed.isEmpty())
            return;

        Inventory inv = minecraft.player.getInventory();

        int startX = this.leftPos + 223;
        int startY = this.topPos + 150;

        // Draw each required dye slot background + item + overlay
        for (int i = 0; i < needed.size(); i++)
        {
            ItemStack want = needed.get(i).get();
            if (want == null || want.isEmpty()) continue;

            int haveCount = countInInventory(inv, want);
            boolean enough = haveCount >= want.getCount();

            int u = enough ? 358 : 340;
            int v = 118;

            gg.blit(FlansMod.gunWorkbenchGuiTexture, startX + SLOT_SIZE * i, startY, u, v, SLOT_SIZE, SLOT_SIZE, 512, 256);

            // Render the dye icon at +1,+1 like vanilla slots
            int itemX = startX + SLOT_SIZE * i + 1;
            int itemY = startY + 1;

            gg.renderItem(want, itemX, itemY);
            gg.renderItemDecorations(this.font, want, itemX, itemY);
        }
    }

    private void renderCustomTooltips(GuiGraphics gg, int mouseX, int mouseY)
    {
        if (hoveringModSlotTooltip != null)
            gg.renderTooltip(this.font, hoveringModSlotTooltip, mouseX, mouseY);

        if (hoveringPaintjob != null)
        {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Paintjob: " + hoveringPaintjob.getId()));

            gg.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderStatBars(GuiGraphics gg, int[] targetWidthsPx)
    {
        // grey bar backgrounds
        for (int y = 0; y < 5; y++)
        {
            gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * y), 340F, 80F, BAR_W, BAR_H, 512, 256);
        }

        for (int k = 0; k < 5; k++)
        {
            if (k == 4)
            {
                // control stat
                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * k), 340F, 80F, 32, BAR_H, 512, 256);
                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 239 + 26, topPos + 60 + (12 * k), 341F, 90F, 28, BAR_H, 512, 256);
                gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 239 + 26 + 28,topPos + 60 + (12 * k), 394F, 70F, 32, BAR_H, 512, 256);
                continue;
            }

            int target = Mth.clamp(targetWidthsPx[k], 0, BAR_W);

            int diff = target - lastStats[k];
            int step = 1;
            if (diff != 0)
            {
                int move = Mth.clamp(diff, -step, step);
                lastStats[k] += move;
            }

            int width = Mth.clamp(lastStats[k], 0, BAR_W);

            gg.blit(FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * k), 340F, 90F, width, BAR_H, 512, 256);
        }
    }

    private static float round2(float v)
    {
        return roundN(v, 2);
    }

    private static float roundN(float value, int points)
    {
        int pow = 10;
        for (int i = 1; i < points; i++)
            pow *= 10;
        float result = value * pow;
        return (float) (int) (((result - (int) result) >= 0.5f) ? (result + 1) : result) / pow;
    }

    private static float clamp01(float v)
    {
        return Mth.clamp(v, 0f, 1f);
    }

    private static float normalize(float value, float min, float max)
    {
        if (max <= min)
            return 0f;
        return clamp01((value - min) / (max - min));
    }

    /**
     * Returns ratio where 1.0 means "good/full bar".
     * If lowerIsBetter is true, we invert the normalized value.
     */
    private static float ratioGood(float value, float min, float max, boolean lowerIsBetter)
    {
        float t = normalize(value, min, max);
        return lowerIsBetter ? (1f - t) : t;
    }

    private static int ratioToPixels(float ratio)
    {
        int span = BAR_W - BAR_MIN_PX;
        return BAR_MIN_PX + Math.round(clamp01(ratio) * span);
    }

    public static int countInInventory(Inventory inv, ItemStack wanted)
    {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack have = inv.getItem(i);
            if (sameItem(have, wanted))
                total += have.getCount();
        }
        return total;
    }

    public static boolean sameItem(ItemStack a, ItemStack b)
    {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }
}
