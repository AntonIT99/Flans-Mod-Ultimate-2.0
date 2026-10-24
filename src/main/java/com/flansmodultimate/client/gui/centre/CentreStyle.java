package com.flansmodultimate.client.gui.centre;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** The look of the Teams command centre: its colours, panels, badges and text cut to fit. Client only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CentreStyle
{
    public static final int FRAME = 0xF2101620;
    public static final int FRAME_BORDER = 0xFF33415A;
    public static final int HEADER = 0xFF18212D;
    public static final int SIDEBAR = 0xFF0C1118;
    public static final int SIDEBAR_HOVER = 0xFF18212C;
    public static final int SIDEBAR_SELECTED = 0xFF233147;
    public static final int PANEL = 0xD01A2230;
    public static final int PANEL_BORDER = 0xFF2B3646;
    public static final int PANEL_TITLE = 0xFF212B39;
    public static final int ROW = 0xFF1C2531;
    public static final int ROW_HOVER = 0xFF263242;
    public static final int ROW_SELECTED = 0xFF33445E;
    public static final int ACCENT = 0xFFE3A13B;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int DIM = 0xFFA9B4C2;
    public static final int MUTED = 0xFF6F7B8A;
    public static final int GOOD = 0xFF6FD08C;
    public static final int BAD = 0xFFFF6B6B;
    /** Height of a panel's heading strip. */
    public static final int TITLE_HEIGHT = 14;
    private static final Component ELLIPSIS = Component.literal("…");

    /** A panel with an optional heading strip: the frame pages group what they show in. */
    public static void drawPanel(GuiGraphics graphics, Font font, int x, int y, int width, int height, @Nullable Component title)
    {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.renderOutline(x, y, width, height, PANEL_BORDER);
        if (title == null)
            return;
        graphics.fill(x + 1, y + 1, x + width - 1, y + TITLE_HEIGHT, PANEL_TITLE);
        graphics.fill(x + 1, y + 1, x + 3, y + TITLE_HEIGHT, ACCENT);
        graphics.drawString(font, fit(font, title, width - 10), x + 7, y + 4, ACCENT);
    }

    /**
     * A small tag drawn right-aligned, its text in the colour on a faint pill of it.
     *
     * @return the badge's width
     */
    public static int badge(GuiGraphics graphics, Font font, Component text, int right, int y, int rgb)
    {
        int width = font.width(text) + 6;
        graphics.fill(right - width, y, right, y + 11, 0x40000000 | (rgb & 0xFFFFFF));
        graphics.drawString(font, text, right - width + 3, y + 2, 0xFF000000 | rgb, false);
        return width;
    }

    /** A line of text cut to a width, ending in an ellipsis where it was cut. */
    public static FormattedCharSequence fit(Font font, Component text, int width)
    {
        if (font.width(text) <= width)
            return text.getVisualOrderText();
        FormattedText cut = font.substrByWidth(text, Math.max(0, width - font.width(ELLIPSIS)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, ELLIPSIS));
    }

    /** Draws a line of text cut to a width. */
    public static void drawText(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int colour)
    {
        graphics.drawString(font, fit(font, text, width), x, y, colour);
    }

    /** A colour as an opaque ARGB colour, for an RGB one. */
    public static int opaque(int rgb)
    {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Minutes and seconds of a tick count, such as {@code 12:05}. */
    public static String clock(int ticks)
    {
        int seconds = Math.max(0, ticks) / 20;
        return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }
}
