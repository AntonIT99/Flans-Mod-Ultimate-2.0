package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
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
    private static final int INPUT_LABEL_WIDTH = 210;
    private static final int INPUT_WIDTH = 90;

    private final Map<ConfigOptionFactory.Option, AbstractWidget> widgets = new HashMap<>();

    public FlansOptionsList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight)
    {
        super(minecraft, width, height, top, bottom, itemHeight);
        centerListVertically = false;
    }

    public void addHeader(Component title)
    {
        addEntry(new HeaderEntry(title));
    }

    /** Adds two buttons per row, or a label and direct input field, in the order given. */
    public void addOptions(List<ConfigOptionFactory.Option> options)
    {
        int i = 0;
        while (i < options.size())
        {
            ConfigOptionFactory.Option option = options.get(i++);
            if (option.label() != null)
            {
                AbstractWidget input = option.widgetFactory().apply(INPUT_WIDTH);
                widgets.put(option, input);
                Component caption = Component.literal(minecraft.font.plainSubstrByWidth(option.label().getString(), INPUT_LABEL_WIDTH));
                StringWidget label = new StringWidget(INPUT_LABEL_WIDTH, 20, caption, minecraft.font)
                {
                    @Override
                    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
                    {
                        setTooltip(input.getTooltip());
                        super.renderWidget(graphics, mouseX, mouseY, partialTick);
                    }
                }.alignLeft();
                label.setX(width / 2 - ROW_HALF_WIDTH);
                input.setX(label.getX() + INPUT_LABEL_WIDTH + 10);
                addEntry(new OptionEntry(List.of(label, input)));
            }
            else
            {
                ConfigOptionFactory.Option right = i < options.size() && options.get(i).label() == null ? options.get(i++) : null;
                addRow(option, right);
            }
        }
    }

    private void addRow(ConfigOptionFactory.Option left, @Nullable ConfigOptionFactory.Option right)
    {
        int rowLeft = width / 2 - ROW_HALF_WIDTH;
        List<AbstractWidget> rowWidgets = new ArrayList<>(2);

        AbstractWidget leftWidget = left.widgetFactory().apply(OPTION_WIDTH);
        leftWidget.setX(rowLeft);
        widgets.put(left, leftWidget);
        rowWidgets.add(leftWidget);

        if (right != null)
        {
            AbstractWidget rightWidget = right.widgetFactory().apply(OPTION_WIDTH);
            rightWidget.setX(rowLeft + COLUMN_GAP);
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
    public AbstractWidget findWidget(ConfigOptionFactory.Option option)
    {
        return widgets.get(option);
    }

    public void tick()
    {
        for (AbstractWidget widget : widgets.values())
            if (widget instanceof EditBox input)
                ClientPlatform.tick(input);
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
    {}

    /** A section title. It holds no widgets, so it is skipped by keyboard navigation. */
    private class HeaderEntry extends Entry
    {
        private final Component title;

        private HeaderEntry(Component title)
        {
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovering, float partialTick)
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
        public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovering, float partialTick)
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
