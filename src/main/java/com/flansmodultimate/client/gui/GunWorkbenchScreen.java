package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.client.render.item.LegacyItemPreviewRenderer;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import com.flansmodultimate.util.InventoryHelper;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private static final float DISP_MIN = 0.01F;
    private static final float DISP_MAX = 1F;
    private static final float RECOIL_MIN = 1F;
    private static final float RECOIL_MAX = 20F;
    private static final float REL_MIN = 1F;
    private static final float REL_MAX = 8F;

    private Paintjob hoveringPaintjob;
    private Component hoveringModSlotTooltip;
    private boolean flipGunModel;

    public GunWorkbenchScreen(GunWorkbenchMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title, 331, 236);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick)
    {
        updateHoveringModSlotTooltip(mouseX, mouseY);
        super.extractRenderState(gg, mouseX, mouseY, partialTick);
        renderCustomTooltips(gg, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gg, int mouseX, int mouseY)
    {
        final int color = 0x404040;

        gg.text(font, "Gun Modification Table", 6, 6, color, false);
        gg.text(font, "Inventory", 7, 142, color, false);
        gg.text(font, "Gun Information", 179, 22, color, false);
        gg.text(font, "Paint Jobs", 179, 128, color, false);

        ItemStack gunStack = menu.getGunStack();
        if (!gunStack.isEmpty() && gunStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();

            // text stats
            gg.text(font, gunStack.getHoverName(), 207, 36, color, false);
            gg.text(font, Component.literal(gunType.getDescription()), 207, 46, color, false);

            gg.text(font, "Damage", 181, 61, color, false);
            gg.text(font, "Dispersion", 181, 73, color, false);
            gg.text(font, "Recoil", 181, 85, color, false);
            gg.text(font, "Reload", 181, 97, color, false);
            gg.text(font, "Control", 181, 109, color, false);

            gg.text(font, "Sprint", 240, 119, color, false);
            gg.text(font, "Sneak", 290, 119, color, false);

            gg.text(font, String.valueOf(round2(getDamageStat(gunStack, gunItem))), 241, 62, color, false);
            gg.text(font, round2(gunType.getDispersionForDisplay(gunStack)) + "°", 241, 74, color, false);
            gg.text(font, String.valueOf(round2(gunType.getDisplayVerticalRecoil(gunStack))), 241, 86, color, false);
            gg.text(font, round2(gunType.getReloadTime(gunStack)/ 20F) + "s", 241, 98, color, false);

            float sprinting = roundN(1F - gunType.getRecoilControl(gunStack, true, false), 2);
            float normal = roundN(1F - gunType.getRecoilControl(gunStack, false, false), 2);
            float sneaking = roundN(1F - gunType.getRecoilControl(gunStack, false, true), 2);
            gg.text(font, String.format("%3.2f  %3.2f  %3.2f", sprinting, normal, sneaking), 241, 110, color, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(gg, mouseX, mouseY, partialTick);

        // whole background
        gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 512, 256);

        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
        {
            hoveringPaintjob = null;
            lastStats[0] = lastStats[1] = lastStats[2] = lastStats[3] = lastStats[4] = 0;
            return;
        }

        GunType type = gunItem.getConfigType();

        // flip button
        gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 146, topPos + 63, 340F, 166F, 20, 10, 512, 256);

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
                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + ATTACH_SLOTS_GUI_X + (m * SLOT_SIZE), topPos + ATTACH_SLOTS_GUI_Y - 1, 340F + (m * SLOT_SIZE), 136F, SLOT_SIZE, SLOT_SIZE, 512, 256);
        }

        // generic attachment slot backgrounds
        for (int x = 0; x < 8; x++)
        {
            if (x < type.getNumGenericAttachmentSlots())
                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + ATTACH_SLOTS_GUI_X + (SLOT_SIZE * x), topPos + GENERIC_SLOTS_GUI_Y - 1, 340F, 100F, SLOT_SIZE, SLOT_SIZE, 512, 256);
        }

        List<Paintjob> applicable = type.getApplicablePaintjobs();

        renderGunPreview(gg, gunStack);
        renderPaintjobs(gg, gunStack, type, applicable);
        updateHoveringPaintjob(mouseX, mouseY, applicable);
        renderDyeRequirementsRow(gg, hoveringPaintjob);
    }

    private static float getDamageStat(ItemStack gunStack, GunItem gunItem)
    {
        GunType gunType = gunItem.getConfigType();

        Optional<ShootableType> currentAmmoType = Optional.ofNullable(gunItem.getAmmoItemStack(gunStack, 0, Minecraft.getInstance().level.registryAccess()))
            .map(stack -> stack.getItem() instanceof ShootableItem shootableItem ? shootableItem.getConfigType() : null);
        Optional<ShootableType> defaultAmmoType = gunType.getDefaultAmmo();

        float damage = 0F;

        if (currentAmmoType.isPresent())
            damage = gunType.getDamageForDisplay(currentAmmoType.get(), gunStack, null);
        else if (defaultAmmoType.isPresent())
            damage = gunType.getDamageForDisplay(defaultAmmoType.get(), gunStack, null);

        if (damage == 0F && gunType.getMeleeDamage(gunStack, false) > 0)
            damage = gunType.getMeleeDamage(gunStack, false);

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

    private void renderGunPreview(GuiGraphicsExtractor gg, ItemStack gunStack)
    {
        LegacyItemPreviewRenderer.submit(gg, gunStack,
            leftPos, topPos, leftPos + 180, topPos + 100, 60F,
            160F, flipGunModel ? 210F : 30F);
    }

    private void renderPaintjobs(GuiGraphicsExtractor gg, ItemStack gunStack, GunType type, List<Paintjob> applicable)
    {
        int num = applicable.size();
        int rows = num / 2 + 1;

        for (int y = 0; y < rows; y++)
        {
            for (int x = 0; x < 2; x++)
            {
                int idx = 2 * y + x;
                if (idx >= num)
                    continue;

                int slotX = 181 + SLOT_SIZE * x;
                int slotY = 150 + SLOT_SIZE * y;

                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + slotX, topPos + slotY, 340F, 100F, SLOT_SIZE, SLOT_SIZE, 512, 256);

                Paintjob pj = applicable.get(idx);

                ItemStack icon = gunStack.copy();
                type.applyPaintjobToStack(icon, pj);

                gg.item(icon, leftPos + slotX + 1, topPos + slotY + 1);
                gg.itemDecorations(this.font, icon, leftPos + slotX + 1, topPos + slotY + 1);
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

    private void renderDyeRequirementsRow(GuiGraphicsExtractor gg, Paintjob paintjob)
    {
        if (paintjob == null || minecraft == null || minecraft.player == null || minecraft.player.getAbilities().instabuild)
            return;

        List<ItemStack> needed = paintjob.getDyesNeeded();
        if (needed.isEmpty())
            return;

        Inventory inv = minecraft.player.getInventory();

        int startX = this.leftPos + 223;
        int startY = this.topPos + 150;

        // Draw each required dye slot background + item + overlay
        for (int i = 0; i < needed.size(); i++)
        {
            ItemStack want = needed.get(i);
            if (want == null || want.isEmpty()) continue;

            int haveCount = InventoryHelper.countInInventory(inv, want);
            boolean enough = haveCount >= want.getCount();

            int u = enough ? 358 : 340;
            int v = 118;

            gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, startX + SLOT_SIZE * i, startY, u, v, SLOT_SIZE, SLOT_SIZE, 512, 256);

            // Render the dye icon at +1,+1 like vanilla slots
            int itemX = startX + SLOT_SIZE * i + 1;
            int itemY = startY + 1;

            gg.item(want, itemX, itemY);
            gg.itemDecorations(this.font, want, itemX, itemY);
        }
    }

    private void renderCustomTooltips(GuiGraphicsExtractor gg, int mouseX, int mouseY)
    {
        if (hoveringModSlotTooltip != null)
            gg.setTooltipForNextFrame(this.font, hoveringModSlotTooltip, mouseX, mouseY);

        if (hoveringPaintjob != null)
        {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Paintjob: " + hoveringPaintjob.getId()));

            gg.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderStatBars(GuiGraphicsExtractor gg, int[] targetWidthsPx)
    {
        // grey bar backgrounds
        for (int y = 0; y < 5; y++)
        {
            gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * y), 340F, 80F, BAR_W, BAR_H, 512, 256);
        }

        for (int k = 0; k < 5; k++)
        {
            if (k == 4)
            {
                // control stat
                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * k), 340F, 80F, 32, BAR_H, 512, 256);
                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 239 + 26, topPos + 60 + (12 * k), 341F, 90F, 28, BAR_H, 512, 256);
                gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 239 + 26 + 28,topPos + 60 + (12 * k), 394F, 70F, 32, BAR_H, 512, 256);
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

            gg.blit(RenderPipelines.GUI_TEXTURED, FlansMod.gunWorkbenchGuiTexture, leftPos + 239, topPos + 60 + (12 * k), 340F, 90F, width, BAR_H, 512, 256);
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
}
