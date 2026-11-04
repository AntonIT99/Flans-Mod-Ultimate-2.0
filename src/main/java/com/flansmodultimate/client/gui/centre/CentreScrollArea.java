package com.flansmodultimate.client.gui.centre;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/**
 * The scrolling of a page whose rows do not all fit: covers the area the rows are laid out in, takes the mouse wheel over
 * it (also over the switches, which would otherwise cycle) and draws and drags a scroll bar at its right edge. The page
 * lays out only the rows from the first one shown; this widget reports where the operator scrolled to. Client only.
 */
final class CentreScrollArea extends AbstractWidget
{
    /** Width of the scroll bar. */
    static final int BAR = 3;
    /** Width a page leaves free at the right edge for the scroll bar, including the gap. */
    static final int BAR_SPACE = BAR + 3;

    private final int totalRows;
    private final int visibleRows;
    private final int firstRow;
    private final IntConsumer onScroll;

    CentreScrollArea(int x, int y, int width, int height, int totalRows, int visibleRows, int firstRow, IntConsumer onScroll)
    {
        super(x, y, width, height, Component.empty());
        this.totalRows = totalRows;
        this.visibleRows = visibleRows;
        this.firstRow = firstRow;
        this.onScroll = onScroll;
    }

    private int maxFirst()
    {
        return Math.max(0, totalRows - visibleRows);
    }

    private int barLeft()
    {
        return getX() + width - BAR;
    }

    private void scrollTo(int row)
    {
        int target = Mth.clamp(row, 0, maxFirst());
        if (target != firstRow)
            onScroll.accept(target);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (maxFirst() == 0)
            return;
        int thumb = Math.max(12, height * visibleRows / totalRows);
        int thumbTop = getY() + (height - thumb) * firstRow / maxFirst();
        graphics.fill(barLeft(), getY(), barLeft() + BAR, getY() + height, 0x60000000);
        graphics.fill(barLeft(), thumbTop, barLeft() + BAR, thumbTop + thumb, CentreStyle.DIM);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (maxFirst() == 0 || !isMouseOver(mouseX, mouseY))
            return false;
        scrollTo(firstRow - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0 || maxFirst() == 0 || !isMouseOver(mouseX, mouseY) || mouseX < barLeft() - 2)
            return false;
        dragTo(mouseY);
        return true;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY)
    {
        if (maxFirst() > 0 && mouseX >= barLeft() - 8)
            dragTo(mouseY);
    }

    private void dragTo(double mouseY)
    {
        scrollTo((int) Math.round((mouseY - getY()) / height * maxFirst()));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output)
    {
        // Nothing to narrate: the rows narrate themselves.
    }
}
