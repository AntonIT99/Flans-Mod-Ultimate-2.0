package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.common.eras.Era;
import com.flansmodultimate.common.eras.EraCatalog;
import com.flansmodultimate.common.eras.ServiceDate;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The timeline: era bands along the top, a year axis that pans and zooms, one row per item drawn from its introduction to
 * the end of its service, and the window of the chosen restriction as two brackets that can be dragged. Items outside
 * the window are dimmed. Client thread.
 */
final class EraTimelineWidget extends AbstractWidget
{
    private static final int LABEL_WIDTH = 96;
    private static final int BAND_HEIGHT = 11;
    private static final int AXIS_HEIGHT = 11;
    private static final int ROW_HEIGHT = 10;
    private static final int GRAB = 3;
    private static final double MIN_SPAN = 4D;
    private static final double MAX_SPAN = 6000D;
    private static final double DEFAULT_START = 1860D;
    private static final double DEFAULT_END = 2030D;
    private static final int[] STEPS = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000};
    private static final int BAND = 0xFF2A3546;
    private static final int BAND_BORDER = 0xFF3B4A60;
    private static final int GRID = 0x30FFFFFF;
    private static final int WINDOW_TINT = 0x2050A0FF;
    private static final int WINDOW_EDGE = 0xFF5AA8FF;
    private static final int ALLOWED_BAR = 0xFF4F8FD8;
    private static final int BLOCKED_BAR = 0x60808080;
    private static final int SELECTED = 0x40FFFFFF;

    /** One item as drawn: its months, whether its start is approximate and whether the window lets it through. */
    record Entry(InfoType type, String name, int start, int end, boolean approximate, boolean openEnd, boolean allowed)
    {}

    private final BiConsumer<Integer, Integer> onWindowMoved;
    private final Consumer<Entry> onSelected;
    private List<Entry> entries = List.of();
    private EraCatalog eras = EraCatalog.defaults();
    private int windowLower = ServiceDate.OPEN_LOW;
    private int windowUpper = ServiceDate.OPEN_HIGH;
    private boolean hasWindow;
    private double startYear = DEFAULT_START;
    private double endYear = DEFAULT_END;
    private int firstRow;
    @Nullable
    private InfoType selected;
    /** 0 for nothing, 1 while dragging the lower bracket, 2 the upper, 3 while panning. */
    private int dragging;
    private double dragFrom;

    EraTimelineWidget(BiConsumer<Integer, Integer> onWindowMoved, Consumer<Entry> onSelected)
    {
        super(0, 0, 0, 0, Component.empty());
        this.onWindowMoved = onWindowMoved;
        this.onSelected = onSelected;
    }

    void place(int x, int y, int width, int height)
    {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /** What to draw; a window bound of {@link ServiceDate#OPEN_LOW} or {@code OPEN_HIGH} is open. */
    void show(List<Entry> shown, EraCatalog catalog, boolean window, int lower, int upper, @Nullable InfoType chosen)
    {
        entries = List.copyOf(shown);
        eras = catalog;
        hasWindow = window;
        windowLower = lower;
        windowUpper = upper;
        selected = chosen;
        firstRow = Math.max(0, Math.min(firstRow, entries.size() - 1));
    }

    void resetView()
    {
        startYear = DEFAULT_START;
        endYear = DEFAULT_END;
        firstRow = 0;
    }

    private int plotLeft()
    {
        return getX() + LABEL_WIDTH;
    }

    private int plotWidth()
    {
        return Math.max(1, getWidth() - LABEL_WIDTH - 2);
    }

    private int rowsTop()
    {
        return getY() + BAND_HEIGHT + AXIS_HEIGHT + 2;
    }

    private int visibleRows()
    {
        return Math.max(1, (getY() + getHeight() - rowsTop()) / ROW_HEIGHT);
    }

    private double yearOf(int month)
    {
        return month / 12D;
    }

    private int xOfYear(double year)
    {
        return plotLeft() + (int) Math.round((year - startYear) / (endYear - startYear) * plotWidth());
    }

    /** The month under a screen x. */
    private int monthAt(double x)
    {
        double year = startYear + (x - plotLeft()) / plotWidth() * (endYear - startYear);
        return (int) Math.floor(year * 12D);
    }

    /** Where a month is drawn, held to the plot so open ends and the far past or future reach its edges. */
    private int xOfMonth(int month)
    {
        if (month == ServiceDate.OPEN_LOW)
            return plotLeft();
        if (month >= ServiceDate.FUTURE)
            return plotLeft() + plotWidth();
        return Math.max(plotLeft(), Math.min(plotLeft() + plotWidth(), xOfYear(yearOf(month))));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        Font font = Minecraft.getInstance().font;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), CentreStyle.PANEL);
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        renderEras(graphics, font);
        renderAxis(graphics, font);
        renderWindow(graphics);
        Entry hovered = renderRows(graphics, font, mouseX, mouseY);
        graphics.disableScissor();
        if (hovered != null)
        {
            String end = hovered.openEnd() ? "" : " " + ServiceDate.formatMonth(hovered.end());
            graphics.renderComponentTooltip(font, List.of(Component.literal(hovered.name()), Component.literal(ServiceDate.formatMonth(hovered.start()) + " –" + end)), mouseX,
                mouseY);
        }
    }

    private void renderEras(GuiGraphics graphics, Font font)
    {
        int y0 = getY() + 1;
        for (Era era : eras.getEras())
        {
            int x0 = xOfMonth(era.first());
            int x1 = xOfMonth(era.last());
            if (x1 - x0 < 2 || x1 <= plotLeft() || x0 >= plotLeft() + plotWidth())
                continue;
            graphics.fill(x0, y0, x1, y0 + BAND_HEIGHT - 2, BAND);
            graphics.fill(x0, y0, x0 + 1, y0 + BAND_HEIGHT - 2, BAND_BORDER);
            int textWidth = font.width(era.id());
            if (textWidth < x1 - x0 - 4)
                graphics.drawString(font, era.id(), (x0 + x1 - textWidth) / 2, y0 + 1, CentreStyle.DIM, false);
        }
    }

    private void renderAxis(GuiGraphics graphics, Font font)
    {
        int y0 = getY() + BAND_HEIGHT;
        double span = endYear - startYear;
        int step = STEPS[STEPS.length - 1];
        for (int candidate : STEPS)
        {
            if (span / candidate <= 10D)
            {
                step = candidate;
                break;
            }
        }
        long first = (long) Math.ceil(startYear / step) * step;
        for (long year = first; year <= endYear; year += step)
        {
            int x = xOfYear(year);
            graphics.fill(x, y0 + AXIS_HEIGHT - 2, x + 1, getY() + getHeight(), GRID);
            String label = year >= 1 ? Long.toString(year) : (1 - year) + " BC";
            graphics.drawString(font, label, x - font.width(label) / 2, y0, CentreStyle.MUTED, false);
        }
    }

    private void renderWindow(GuiGraphics graphics)
    {
        if (!hasWindow)
            return;
        int x0 = windowLower == ServiceDate.OPEN_LOW ? plotLeft() : xOfMonth(windowLower);
        int x1 = windowUpper == ServiceDate.OPEN_HIGH ? plotLeft() + plotWidth() : xOfMonth(windowUpper + 1);
        int y0 = getY() + BAND_HEIGHT;
        graphics.fill(x0, y0, x1, getY() + getHeight(), WINDOW_TINT);
        if (windowLower != ServiceDate.OPEN_LOW)
            graphics.fill(x0 - 1, y0, x0 + 1, getY() + getHeight(), WINDOW_EDGE);
        if (windowUpper != ServiceDate.OPEN_HIGH)
            graphics.fill(x1 - 1, y0, x1 + 1, getY() + getHeight(), WINDOW_EDGE);
    }

    /** Draws the visible rows and gives back the one under the pointer, if any. */
    @Nullable
    private Entry renderRows(GuiGraphics graphics, Font font, int mouseX, int mouseY)
    {
        int top = rowsTop();
        int last = Math.min(entries.size(), firstRow + visibleRows());
        Entry hovered = null;
        for (int index = firstRow; index < last; index++)
        {
            Entry entry = entries.get(index);
            int y = top + (index - firstRow) * ROW_HEIGHT;
            if (entry.type() == selected)
                graphics.fill(getX(), y, getX() + getWidth(), y + ROW_HEIGHT, SELECTED);
            String name = font.plainSubstrByWidth(entry.name(), LABEL_WIDTH - 4);
            graphics.drawString(font, name, getX() + 2, y + 1, entry.allowed() ? CentreStyle.TEXT : CentreStyle.MUTED, false);
            int x0 = xOfMonth(entry.start());
            int x1 = Math.max(x0 + 2, xOfMonth(entry.end() + 1));
            int colour = entry.allowed() ? ALLOWED_BAR : BLOCKED_BAR;
            graphics.fill(x0, y + 2, x1, y + ROW_HEIGHT - 2, colour);
            if (entry.approximate())
                graphics.fill(x0 - 3, y + 4, x0, y + ROW_HEIGHT - 4, colour);
            if (entry.openEnd())
                graphics.fill(x1, y + 3, x1 + 3, y + ROW_HEIGHT - 3, colour);
            if (mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX >= getX() && mouseX < getX() + getWidth())
                hovered = entry;
        }
        return hovered;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!isMouseOver(mouseX, mouseY) || button != 0)
            return false;
        if (hasWindow && mouseX >= plotLeft() && mouseY >= getY() + BAND_HEIGHT)
        {
            if (windowLower != ServiceDate.OPEN_LOW && Math.abs(mouseX - xOfMonth(windowLower)) <= GRAB)
                dragging = 1;
            else if (windowUpper != ServiceDate.OPEN_HIGH && Math.abs(mouseX - xOfMonth(windowUpper + 1)) <= GRAB)
                dragging = 2;
        }
        if (dragging == 0)
        {
            int row = (int) ((mouseY - rowsTop()) / ROW_HEIGHT) + firstRow;
            if (mouseY >= rowsTop() && row >= 0 && row < entries.size())
            {
                selected = entries.get(row).type();
                onSelected.accept(entries.get(row));
            }
            dragging = 3;
            dragFrom = mouseX;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        switch (dragging)
        {
            case 1 -> windowLower = Math.min(monthAt(mouseX), windowUpper == ServiceDate.OPEN_HIGH ? Integer.MAX_VALUE - 2 : windowUpper);
            case 2 -> windowUpper = Math.max(monthAt(mouseX) - 1, windowLower == ServiceDate.OPEN_LOW ? Integer.MIN_VALUE + 1 : windowLower);
            case 3 ->
            {
                double years = (mouseX - dragFrom) / plotWidth() * (endYear - startYear);
                startYear -= years;
                endYear -= years;
                dragFrom = mouseX;
            }
            default ->
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        int released = dragging;
        dragging = 0;
        if (released == 1 || released == 2)
        {
            onWindowMoved.accept(windowLower, windowUpper);
            return true;
        }
        return released != 0;
    }

    /** The wheel scrolls the rows; with Shift held it zooms the axis about the pointer. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (!isMouseOver(mouseX, mouseY))
            return false;
        if (Screen.hasShiftDown())
        {
            double pivot = startYear + (mouseX - plotLeft()) / plotWidth() * (endYear - startYear);
            double factor = delta > 0 ? 0.8D : 1.25D;
            double span = Math.max(MIN_SPAN, Math.min(MAX_SPAN, (endYear - startYear) * factor));
            double share = (pivot - startYear) / (endYear - startYear);
            startYear = pivot - span * share;
            endYear = startYear + span;
        }
        else
            firstRow = Math.max(0, Math.min(Math.max(0, entries.size() - visibleRows()), firstRow - (int) Math.signum(delta) * 3));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, Component.literal(entries.size() + " items"));
    }
}
