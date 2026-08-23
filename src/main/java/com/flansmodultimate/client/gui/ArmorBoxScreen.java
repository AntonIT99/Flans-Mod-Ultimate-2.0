package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.ArmorBoxBuyPacket;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ArmorBoxScreen extends AbstractContainerScreen<ArmorBoxMenu>
{
    private int page = 0;
    private int scroll = 0;

    public ArmorBoxScreen(ArmorBoxMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title, 176, 182);
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
        Identifier guiTexture = menu.getBlock().getConfigType().getGuiTexture();

        addRenderableWidget(new TextureRegionButton(x0 + 77, y0 + 87, 176, guiTexture,
            btn -> {
                if (page > 0)
                    page--;
            }
        ));

        addRenderableWidget(new TextureRegionButton(x0 + 89, y0 + 87, 186, guiTexture,
            btn -> {
                int max = menu.getBlock().getConfigType().getPages().size() - 1;
                if (page < max)
                    page++;
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
    public void extractRenderState(@NotNull GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(gg, mouseX, mouseY, partialTick);
        ArmorBoxType type = menu.getBlock().getConfigType();
        Identifier guiTexture = type.getGuiTexture();
        gg.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        gg.centeredText(font, menu.getBlock().getName().getString(), leftPos + imageWidth / 2, topPos + 5, 0xFFFFFF);

        // Grey out arrows like old GUI (draw overlay on top)
        if (page == 0)
            gg.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos + 77, topPos + 87, 176, 0, 10, 10, 256, 256);
        if (page >= type.getPages().size() - 1)
            gg.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos + 89, topPos + 87, 186, 0, 10, 10, 256, 256);

        drawRecipe(gg, type, page);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor gg, int mouseX, int mouseY)
    {
        // The armor box GUI draws its own title in renderBg and does not need the default inventory label.
    }

    /** Keeps the legacy GUI-sheet arrows while using the 1.21 button API. */
    private static final class TextureRegionButton extends Button
    {
        private final int textureU;
        private final Identifier texture;

        private TextureRegionButton(int x, int y, int textureU, Identifier texture, OnPress onPress)
        {
            super(x, y, 10, 10, Component.empty(), onPress, DEFAULT_NARRATION);
            this.textureU = textureU;
            this.texture = texture;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
        {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), textureU, 0, width, height, 256, 256);
        }
    }

    private void drawRecipe(GuiGraphicsExtractor gg, ArmorBoxType type, int pageIndex)
    {
        if (type.getPages().isEmpty())
            return;
        ArmorBoxType.ArmourBoxEntry currentPage = type.getPages().get(pageIndex);
        if (currentPage == null)
            return;

        // Center title
        gg.centeredText(font, currentPage.getName(), leftPos + 87, topPos + 25, 0xFFFFFF);

        // 2x2 armour panels:
        // armour icon at (x+9+83*i, y+44+22*j)
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                int idx = i * 2 + j;

                ArmorType armorType = currentPage.getArmorType(idx);
                if (armorType == null)
                    continue;

                int ax = leftPos + 9 + 83 * i;
                int ay = topPos + 44 + 22 * j;

                ModUtils.getItemStack(armorType).ifPresent(armorStack -> {
                    gg.item(armorStack, ax, ay);
                    gg.itemDecorations(font, armorStack, ax, ay);
                });

                List<ItemStack> req = currentPage.getRequiredStacks().get(idx);
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
                        gg.item(part, px, py);
                        gg.itemDecorations(font, part, px, py);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
                        if (type.getPages().get(page).getArmorType(idx) != null
                            && m > 7 + 83 * x && m < 27 + 83 * x
                            && n > 42 + 22 * y && n < 62 + 22 * y)
                        {

                            PacketHandler.sendToServer(new ArmorBoxBuyPacket(menu.getPos(), page, idx));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
