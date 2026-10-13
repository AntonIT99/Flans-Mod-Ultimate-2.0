package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.mixin.NpcModelScrollAccessor;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** Shows the full translated name of a visible model-picker row when Custom NPCs shortens it. */
public class NpcModelNameScroll extends GuiCustomScrollNop
{
    public NpcModelNameScroll(Screen parent, int id)
    {
        super(parent, id);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (!visible)
            return;
        NpcModelScrollAccessor state = (NpcModelScrollAccessor) this;
        int hovered = state.wolffsmodnpcsGetHover();
        if (hovered < 0 || hovered >= list.size())
            return;
        int displayIndex = 0;
        for (int i = 0; i < hovered; i++)
            if (state.wolffsmodnpcsIsSearched(list.get(i)))
                displayIndex++;
        int rowY = 14 * displayIndex + 4 - state.wolffsmodnpcsGetScrollY();
        // Match drawItems: partly hidden rows are not rendered, and its width allowance depends on the scrollbar.
        if (rowY < 4 || rowY + 12 >= height)
            return;
        int widthAllowance = state.wolffsmodnpcsGetScrollHeight() < height - 8 ? 0 : 10;
        String name = I18n.get(list.get(hovered));
        if (font.width(name) > (getWidth() + widthAllowance - 8) * 0.8F)
            graphics.renderTooltip(font, Component.literal(name), mouseX, mouseY);
    }
}
