package com.flansmodultimate.client.gui;

import com.flansmod.client.model.RenderGun;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PaintjobTableScreen extends AbstractContainerScreen<PaintjobTableMenu>
{
    public static final int TOP_H = 92;
    public static final int BOTTOM_H = 142;

    private static final int GUI_W = 224;
    private static final int GUI_H = TOP_H + BOTTOM_H;
    private static final int TEX_W = 512;
    private static final int TEX_H = 256;
    private static final int STRIP_H = 22;
    private static final int UV_FIRST_U = 256;
    private static final int UV_FIRST_V_MISSING = 0;
    private static final int UV_FIRST_V_HAVE = 23;
    private static final int UV_FIRST_W = 20;
    private static final int UV_MID_U = 277;
    private static final int UV_MID_W = 18;
    private static final int UV_LAST_U = 296;
    private static final int UV_LAST_W = 20;
    private static final int UV_SINGLE_U_MISSING = 356;
    private static final int UV_SINGLE_U_HAVE = 379;
    private static final int UV_SINGLE_V = 0;
    private static final int UV_SINGLE_W = 22;
    // Where to draw the dye item inside each segment
    private static final int ITEM_PAD_X = 3;
    private static final int ITEM_PAD_Y = 3;


    private Paintjob hoveringPaintjob = null;

    public PaintjobTableScreen(PaintjobTableMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        imageWidth = GUI_W;
        imageHeight = GUI_H;
    }

    @Override
    protected void init()
    {
        super.init();

        titleLabelX = 8;
        titleLabelY = 6;

        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY)
    {
        int x0 = leftPos;
        int y0 = topPos;

        gg.blit(FlansMod.paintjobTableGuiTexture, x0, y0, 0, 0, imageWidth, TOP_H, TEX_W, TEX_H);
        gg.blit(FlansMod.paintjobTableGuiTexture, x0, y0 + TOP_H, 0, TOP_H, imageWidth, BOTTOM_H, TEX_W, TEX_H);

        // Render paintjob icons
        ItemStack paintable = menu.slots.get(0).getItem();
        hoveringPaintjob = null;

        if (!paintable.isEmpty() && paintable.getItem() instanceof IPaintableItem<?> p)
        {
            PaintableType type = p.getPaintableType();
            List<Paintjob> paintjobs = type.getApplicablePaintjobs();

            int baseX = x0 + 8;
            int baseY = y0 + TOP_H + 8;

            for (int idx = 0; idx < paintjobs.size(); idx++)
            {
                int x = idx % 9;
                int y = idx / 9;

                Paintjob pj = paintjobs.get(idx);

                ItemStack iconStack = paintable.copy();
                type.applyPaintjobToStack(iconStack, pj);

                int px = baseX + x * 18;
                int py = baseY + y * 18;

                gg.renderItem(iconStack, px, py);

                if (mouseX >= px && mouseX < px + 18 && mouseY >= py && mouseY < py + 18)
                    hoveringPaintjob = pj;
            }

            renderPreview(gg, partialTick, paintable);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
        renderDyeRequirementStrip(gg, mouseX, mouseY, hoveringPaintjob);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && hoveringPaintjob != null)
        {
            PacketHandler.sendToServer(new PacketSelectPaintjob(hoveringPaintjob.getId()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPreview(GuiGraphics gg, float partialTick, ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        PoseStack pose = gg.pose();
        pose.pushPose();
        pose.translate(leftPos + (GUI_W / 2F), topPos + (TOP_H / 2F), 100F);


        float ticks = mc.level.getGameTime() + partialTick;
        float yRot = (ticks * 3F) % 360F;

        pose.mulPose(Axis.XP.rotationDegrees(160));
        pose.mulPose(Axis.YP.rotationDegrees(yRot));
        pose.scale(-60F, 60F, 60F);

        Lighting.setupFor3DItems();

        //TODO: make 3D item rendering for attachments
        if (stack.getItem() instanceof AttachmentItem attachmentItem)
            RenderGun.renderAttachment(attachmentItem.getConfigType(), stack, pose, gg.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        else
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, gg.bufferSource(), mc.level, 0);


        gg.flush();
        pose.popPose();
        Lighting.setupForFlatItems();
    }

    private void renderDyeRequirementStrip(GuiGraphics gg, int mouseX, int mouseY, Paintjob pj)
    {
        if (pj == null || minecraft == null)
            return;

        Player player = minecraft.player;
        if (player == null || player.getAbilities().instabuild)
            return;

        // Get dye requirements for this paintjob
        List<ItemStack> dyesNeeded = pj.getDyesNeeded();
        if (dyesNeeded.isEmpty())
            return;

        // Compute "have vs missing"
        boolean[] have = computeHaveRequirements(player, dyesNeeded);

        // Position
        int originX = mouseX + 6;
        int originY = mouseY - 20;

        if (dyesNeeded.size() == 1)
        {
            int u = have[0] ? UV_SINGLE_U_HAVE : UV_SINGLE_U_MISSING;
            gg.blit(FlansMod.paintjobTableGuiTexture, originX, originY, u, UV_SINGLE_V, UV_SINGLE_W, STRIP_H, TEX_W, TEX_H);
        }
        else
        {
            // First
            gg.blit(FlansMod.paintjobTableGuiTexture, originX, originY, UV_FIRST_U, have[0] ? UV_FIRST_V_HAVE : UV_FIRST_V_MISSING, UV_FIRST_W, STRIP_H, TEX_W, TEX_H);

            // Middles
            for (int s = 1; s < dyesNeeded.size() - 1; s++)
            {
                gg.blit(FlansMod.paintjobTableGuiTexture, originX + 2 + 18 * s, originY, UV_MID_U, have[s] ? UV_FIRST_V_HAVE : UV_FIRST_V_MISSING, UV_MID_W, STRIP_H, TEX_W, TEX_H);
            }

            // Last
            int last = dyesNeeded.size() - 1;
            gg.blit(FlansMod.paintjobTableGuiTexture, originX + 2 + 18 * last, originY, UV_LAST_U, have[last] ? UV_FIRST_V_HAVE : UV_FIRST_V_MISSING, UV_LAST_W, STRIP_H, TEX_W, TEX_H);
        }

        // Draw dye items + overlays (counts)
        Font font = minecraft.font;
        for (int s = 0; s < dyesNeeded.size(); s++)
        {
            ItemStack req = dyesNeeded.get(s);

            int itemX = originX + ITEM_PAD_X + s * 18;
            int itemY = originY + ITEM_PAD_Y;

            gg.renderItem(req, itemX, itemY);
            gg.renderItemDecorations(font, req, itemX, itemY);
        }

        // Show tooltip if mouse is over one of the requirement items
        for (int s = 0; s < dyesNeeded.size(); s++)
        {
            int itemX = originX + ITEM_PAD_X + s * 18;
            int itemY = originY + ITEM_PAD_Y;
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16)
            {
                gg.renderTooltip(minecraft.font, dyesNeeded.get(s), mouseX, mouseY);
                break;
            }
        }
    }

    private boolean[] computeHaveRequirements(Player player, List<ItemStack> required)
    {
        boolean[] have = new boolean[required.size()];

        // For each required stack, count matching items in player inventory
        for (int i = 0; i < required.size(); i++)
        {
            ItemStack req = required.get(i);
            if (req.isEmpty())
            {
                have[i] = true;
                continue;
            }

            int needed = req.getCount();
            for (int s = 0; s < player.getInventory().getContainerSize(); s++)
            {
                ItemStack inv = player.getInventory().getItem(s);
                if (inv.isEmpty())
                    continue;

                if (ItemStack.isSameItemSameTags(inv, req))
                {
                    needed -= inv.getCount();
                    if (needed <= 0)
                        break;
                }
            }

            have[i] = needed <= 0;
        }

        return have;
    }
}

