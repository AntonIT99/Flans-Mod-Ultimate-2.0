package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PaintjobTableScreen extends AbstractContainerScreen<PaintjobTableMenu>
{
    private Paintjob hoveringPaintjob = null;

    public PaintjobTableScreen(PaintjobTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 224;
        this.imageHeight = 264;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.blit(FlansMod.paintjobTableGuiTexture, leftPos, topPos, 0, 0, imageWidth, 114, 512, 256);
        gg.blit(FlansMod.paintjobTableGuiTexture, leftPos, topPos + 122, 0, 114, imageWidth, 142, 512, 256);

        // Render paintjob icons
        ItemStack paintable = menu.slots.get(0).getItem();
        hoveringPaintjob = null;

        if (!paintable.isEmpty() && paintable.getItem() instanceof IPaintableItem<?> p) {
            PaintableType type = p.getPaintableType();
            List<Paintjob> paintjobs = getAvailablePaintjobs(type);

            int baseX = leftPos + 8;
            int baseY = topPos + 130;

            for (int idx = 0; idx < paintjobs.size(); idx++) {
                int x = idx % 9;
                int y = idx / 9;

                Paintjob pj = paintjobs.get(idx);

                ItemStack iconStack = paintable.copy();
                // show preview by temporarily applying paintjob tag (client-side only)
                iconStack.getOrCreateTag().putInt("FlansPaintjob", pj.getId());

                int px = baseX + x * 18;
                int py = baseY + y * 18;

                gg.renderItem(iconStack, px, py);

                if (mouseX >= px && mouseX < px + 18 && mouseY >= py && mouseY < py + 18) {
                    hoveringPaintjob = pj;
                }
            }

            // Preview render: at minimum, render the item big somewhere
            // (If you have a custom BEWLR / renderer for the gun, Minecraft will use it here too.)
            gg.renderItem(paintable, leftPos + 90, topPos + 40);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
        renderDyeRequirementStrip(gg, mouseX, mouseY, hoveringPaintjob);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveringPaintjob != null)
        {
            PacketHandler.sendToServer(new PacketSelectPaintjob(hoveringPaintjob.getId()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderDyeRequirementStrip(GuiGraphics gg, int mouseX, int mouseY, Paintjob pj) {
        if (minecraft == null || minecraft.player == null || minecraft.player.getAbilities().instabuild)
            return;
        //TODO
        // draw small slot strip similar to old GUI, then gg.renderItem(dyeStack, ...)
        // and optionally a tooltip list.
    }

    private static List<Paintjob> getAvailablePaintjobs(PaintableType type)
    {
        return type.getPaintjobs().values().stream().filter(Paintjob::isAddToTables).toList();
    }
}

