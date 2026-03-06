package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ArmorBoxScreen extends AbstractContainerScreen<ArmorBoxMenu>
{
    private int page = 0;
    private int scroll = 0;

    public ArmorBoxScreen(ArmorBoxMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
    }

    @Override
    protected void init()
    {
        super.init();

        // Arrow buttons
        // back button: x+77..87, y+87..97
        // forward:     x+89..99, y+87..97
        int x0 = leftPos;
        int y0 = topPos;

        addRenderableWidget(new ImageButton(
            x0 + 77, y0 + 87, 10, 10,
            176, 0, 10, FlansMod.armorBoxGuiTexture,
            btn -> { if (page > 0) page--; }
        ));

        addRenderableWidget(new ImageButton(
            x0 + 89, y0 + 87, 10, 10,
            186, 0, 10, FlansMod.armorBoxGuiTexture,
            btn -> {
                int max = menu.getBlock().getConfigType().getPages().size() - 1;
                if (page < max) page++;
            }
        ));
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        scroll++;
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gg, float partialTick, int mouseX, int mouseY)
    {
        gg.blit(FlansMod.armorBoxGuiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ArmorBoxType type = menu.getBlock().getConfigType();
        gg.drawCenteredString(font, menu.getBlock().getName().getString(), leftPos + imageWidth / 2, topPos + 5, 0xFFFFFF);

        // Grey out arrows like old GUI (draw overlay on top)
        if (page == 0)
            gg.blit(FlansMod.armorBoxGuiTexture, leftPos + 77, topPos + 87, 176, 0, 10, 10);
        if (page >= type.getPages().size() - 1)
            gg.blit(FlansMod.armorBoxGuiTexture, leftPos + 89, topPos + 87, 186, 0, 10, 10);

        drawRecipe(gg, type, page);
    }

    private void drawRecipe(GuiGraphics gg, ArmorBoxType type, int pageIndex) {
        if (type.getPages().isEmpty())
            return;
        ArmorBoxType.ArmourBoxEntry currentPage = type.getPages().get(pageIndex);
        if (currentPage == null)
            return;

        // Center title
        gg.drawCenteredString(font, currentPage.getName(), leftPos + 87, topPos + 25, 0xFFFFFF);

        // 2x2 armour panels:
        // armour icon at (x+9+83*i, y+44+22*j)
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                int idx = i * 2 + j;

                ArmorType armorType = currentPage.getArmours()[idx];
                if (armorType == null)
                    continue;

                int ax = leftPos + 9 + 83 * i;
                int ay = topPos + 44 + 22 * j;

                ModUtils.getItemStack(armorType).ifPresent(armorStack -> {
                    gg.renderItem(armorStack, ax, ay);
                    gg.renderItemDecorations(font, armorStack, ax, ay);
                });

                List<ItemStack> req = currentPage.getRequiredStacks()[idx];
                int numParts = req.size();
                if (numParts > 0)
                {
                    int startPart = 0;
                    if (numParts >= 4)
                    {
                        startPart = (scroll / 40) % (numParts - 2);
                    }
                    for (int p = 0; p < Math.min(numParts, 3); p++)
                    {
                        ItemStack part = req.get(startPart + p);
                        int px = leftPos + 30 + p * 19 + 83 * i;
                        int py = topPos + 44 + 22 * j;
                        gg.renderItem(part, px, py);
                        gg.renderItemDecorations(font, part, px, py);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 || button == 1)
        {
            int m = (int)mouseX - leftPos;
            int n = (int)mouseY - topPos;

            ArmorBoxType type = menu.getBlock().getConfigType();
            if (!type.getPages().isEmpty())
            {
                for (int x = 0; x < 2; x++)
                {
                    for (int y = 0; y < 2; y++)
                    {
                        int idx = x * 2 + y;
                        if (type.getPages().get(page).getArmours()[idx] != null
                            && m > 7 + 83 * x && m < 27 + 83 * x
                            && n > 42 + 22 * y && n < 62 + 22 * y)
                        {

                            // Send buy request to server (authoritative)
                            //TODO: implement packet
                            //PacketHandler.sendToServer(new ArmorBoxBuyPacket(menu.getPos(), page, idx));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
