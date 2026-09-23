package com.flansmodultimate.client.gui.options;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The scrolling option list of the mod's options screen. It works like the vanilla one, two options per
 * row, and adds section headers so a long list stays readable.
 */
public class FlansOptionsList extends ContainerObjectSelectionList<FlansOptionsList.Entry>
{
    private static final int ROW_HALF_WIDTH = 155;
    private static final int OPTION_WIDTH = 150;
    private static final int COLUMN_GAP = 160;
    private static final int HEADER_COLOUR = 0xFFFFA0;

    private final Map<OptionInstance<?>, AbstractWidget> widgets = new HashMap<>();

    public FlansOptionsList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight)
    {
        super(minecraft, width, bottom - top, top, itemHeight);
        centerListVertically = false;
    }

    public void addHeader(Component title)
    {
        addEntry(new HeaderEntry(title));
    }

    /** Adds the options two per row, in the order given. */
    public void addOptions(List<OptionInstance<?>> options)
    {
        for (int i = 0; i < options.size(); i += 2)
            addRow(options.get(i), i + 1 < options.size() ? options.get(i + 1) : null);
    }

    private void addRow(OptionInstance<?> left, @Nullable OptionInstance<?> right)
    {
        int rowLeft = width / 2 - ROW_HALF_WIDTH;
        List<AbstractWidget> rowWidgets = new ArrayList<>(2);

        AbstractWidget leftWidget = left.createButton(minecraft.options, rowLeft, 0, OPTION_WIDTH);
        widgets.put(left, leftWidget);
        rowWidgets.add(leftWidget);

        if (right != null)
        {
            AbstractWidget rightWidget = right.createButton(minecraft.options, rowLeft + COLUMN_GAP, 0, OPTION_WIDTH);
            widgets.put(right, rightWidget);
            rowWidgets.add(rightWidget);
        }

        addEntry(new OptionEntry(rowWidgets));
    }

    /** Adds widgets with the same two-column placement as ordinary options. */
    public void addWidgetRow(AbstractWidget left, @Nullable AbstractWidget right)
    {
        int rowLeft = width / 2 - ROW_HALF_WIDTH;
        left.setX(rowLeft);
        if (right == null)
        {
            addEntry(new OptionEntry(List.of(left)));
            return;
        }

        right.setX(rowLeft + COLUMN_GAP);
        addEntry(new OptionEntry(List.of(left, right)));
    }

    /** The widget built for an option, so that the screen can disable it or explain why it is disabled. */
    @Nullable
    public AbstractWidget findWidget(OptionInstance<?> option)
    {
        return widgets.get(option);
    }

    @Override
    public int getRowWidth()
    {
        return 400;
    }

    @Override
    protected int getScrollbarPosition()
    {
        return super.getScrollbarPosition() + 32;
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry>
    {
    }

    /** A section title. It holds no widgets, so it is skipped by keyboard navigation. */
    private class HeaderEntry extends Entry
    {
        private final Component title;

        private HeaderEntry(Component title)
        {
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovering, float partialTick)
        {
            int textTop = top + entryHeight - minecraft.font.lineHeight - 2;
            graphics.drawString(minecraft.font, title, width / 2 - ROW_HALF_WIDTH, textTop, HEADER_COLOUR, false);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of();
        }
    }

    /** One or two option widgets side by side, laid out like the vanilla options list. */
    private static class OptionEntry extends Entry
    {
        private final List<AbstractWidget> widgets;

        private OptionEntry(List<AbstractWidget> widgets)
        {
            this.widgets = widgets;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovering, float partialTick)
        {
            for (AbstractWidget widget : widgets)
            {
                widget.setY(top);
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return widgets;
        }
    }
}
