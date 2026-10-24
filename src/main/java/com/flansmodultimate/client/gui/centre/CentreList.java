package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** A list of rows of the command centre that the operator picks from, scrolled with the mouse wheel. Client only. */
final class CentreList extends AbstractWidget
{
    static final int ROW_HEIGHT = 22;
    private static final int SCROLLBAR = 3;

    private final List<CommandCentreRow> rows;
    private final Component empty;
    private final Consumer<String> onSelect;
    private final IntConsumer onScroll;
    private int scroll;

    CentreList(ScreenRectangle area, List<CommandCentreRow> rows, Component empty, int scroll, Consumer<String> onSelect, IntConsumer onScroll)
    {
        super(area.left(), area.top(), area.width(), area.height(), Component.empty());
        this.rows = List.copyOf(rows);
        this.empty = empty;
        this.onSelect = onSelect;
        this.onScroll = onScroll;
        this.scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll()
    {
        return Math.max(0, rows.size() * ROW_HEIGHT - 2 - height);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        Font font = Minecraft.getInstance().font;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x80000000);
        if (rows.isEmpty())
        {
            graphics.drawCenteredString(font, empty, getX() + width / 2, getY() + Math.min(height / 2 - 4, 16), CentreStyle.MUTED);
            return;
        }
        boolean scrolling = maxScroll() > 0;
        int rowWidth = width - (scrolling ? SCROLLBAR + 1 : 0);
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        int first = scroll / ROW_HEIGHT;
        for (int i = first; i < rows.size(); i++)
        {
            int top = getY() + i * ROW_HEIGHT - scroll;
            if (top >= getY() + height)
                break;
            renderRow(graphics, font, rows.get(i), top, rowWidth, isHovered() && mouseX < getX() + rowWidth && mouseY >= top && mouseY < top + ROW_HEIGHT - 2);
        }
        graphics.disableScissor();
        if (scrolling)
        {
            int track = height;
            int thumb = Math.max(12, track * height / (rows.size() * ROW_HEIGHT));
            int thumbTop = getY() + (track - thumb) * scroll / maxScroll();
            graphics.fill(getX() + width - SCROLLBAR, getY(), getX() + width, getY() + height, 0x60000000);
            graphics.fill(getX() + width - SCROLLBAR, thumbTop, getX() + width, thumbTop + thumb, CentreStyle.DIM);
        }
    }

    private void renderRow(GuiGraphics graphics, Font font, CommandCentreRow row, int top, int rowWidth, boolean hovered)
    {
        int background = CentreStyle.ROW;
        if (row.selected())
            background = CentreStyle.ROW_SELECTED;
        else if (hovered)
            background = CentreStyle.ROW_HOVER;
        graphics.fill(getX(), top, getX() + rowWidth, top + ROW_HEIGHT - 2, background);
        graphics.fill(getX(), top, getX() + 3, top + ROW_HEIGHT - 2, row.colour() == 0 ? CentreStyle.ACCENT : CentreStyle.opaque(row.colour()));
        int textRight = getX() + rowWidth - 4;
        if (!row.badge().getString().isEmpty())
            textRight -= CentreStyle.badge(graphics, font, row.badge(), getX() + rowWidth - 3, top + 4, row.badgeColour()) + 4;
        int textWidth = textRight - getX() - 7;
        if (row.detail().getString().isEmpty())
            CentreStyle.drawText(graphics, font, row.title(), getX() + 7, top + 6, textWidth, CentreStyle.TEXT);
        else
        {
            CentreStyle.drawText(graphics, font, row.title(), getX() + 7, top + 2, textWidth, CentreStyle.TEXT);
            CentreStyle.drawText(graphics, font, row.detail(), getX() + 7, top + 11, textWidth, CentreStyle.DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!visible || button != 0 || !isMouseOver(mouseX, mouseY))
            return false;
        int index = (int) (mouseY - getY() + scroll) / ROW_HEIGHT;
        if (index < 0 || index >= rows.size() || mouseX >= getX() + width - (maxScroll() > 0 ? SCROLLBAR + 1 : 0))
            return true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        onSelect.accept(rows.get(index).key());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!isMouseOver(mouseX, mouseY) || maxScroll() == 0)
            return false;
        scroll = Mth.clamp(scroll - (int) Math.round(delta * ROW_HEIGHT), 0, maxScroll());
        onScroll.accept(scroll);
        return true;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output)
    {
        rows.stream().filter(CommandCentreRow::selected).findFirst().ifPresent(row -> output.add(NarratedElementType.TITLE, row.title()));
    }
}
