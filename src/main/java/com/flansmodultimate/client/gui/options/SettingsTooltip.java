package com.flansmodultimate.client.gui.options;

import org.joml.Vector2i;
import org.joml.Vector2ic;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Keeps settings tooltips on screen, paging descriptions that exceed the GUI height. */
final class SettingsTooltip
{
    private static final int MARGIN = 6;
    private static final int LINE_HEIGHT = 10;
    private List<FormattedCharSequence> lines = List.of();
    private ClientTooltipPositioner positioner;
    private boolean queued;
    private int page;
    private int pageCount = 1;

    void beginFrame()
    {
        queued = false;
    }

    void queue(List<FormattedCharSequence> text, ClientTooltipPositioner preferredPositioner, boolean override)
    {
        if (!queued || override)
        {
            if (!lines.equals(text))
                page = 0;
            lines = text;
            positioner = preferredPositioner;
            queued = true;
        }
    }

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY)
    {
        if (!queued || lines.isEmpty())
        {
            lines = List.of();
            page = 0;
            pageCount = 1;
            return;
        }

        int capacity = lineCapacity(graphics.guiHeight());
        List<FormattedCharSequence> visible = lines;
        pageCount = 1;
        if (lines.size() > capacity)
        {
            int pageSize = Math.max(1, capacity - 1);
            pageCount = (lines.size() + pageSize - 1) / pageSize;
            page = Mth.clamp(page, 0, pageCount - 1);
            int start = page * pageSize;
            visible = new ArrayList<>(lines.subList(start, Math.min(start + pageSize, lines.size())));
            visible.add(Component.translatable("gui.flansmodultimate.options.tooltip_page", page + 1, pageCount).getVisualOrderText());
        }

        graphics.renderTooltip(font, visible, (width, height, x, y, tooltipWidth, tooltipHeight) -> boundedPosition(width, height, tooltipWidth, tooltipHeight,
            positioner.positionTooltip(width, height, x, y, tooltipWidth, tooltipHeight)), mouseX, mouseY);
    }

    boolean scroll(double delta)
    {
        if (!queued || pageCount <= 1 || delta == 0)
            return false;
        page = Mth.clamp(page + (delta < 0 ? 1 : -1), 0, pageCount - 1);
        return true;
    }

    static int lineCapacity(int screenHeight)
    {
        // GuiGraphics adds two pixels after the first line, in addition to the tooltip border.
        return Math.max(2, (screenHeight - 2 * MARGIN - 2) / LINE_HEIGHT);
    }

    static Vector2ic boundedPosition(int width, int height, int tooltipWidth, int tooltipHeight, Vector2ic preferred)
    {
        return new Vector2i(Mth.clamp(preferred.x(), MARGIN, Math.max(MARGIN, width - tooltipWidth - MARGIN)), Mth.clamp(preferred.y(), MARGIN, Math.max(MARGIN, height - tooltipHeight - 2 - MARGIN)));
    }
}
