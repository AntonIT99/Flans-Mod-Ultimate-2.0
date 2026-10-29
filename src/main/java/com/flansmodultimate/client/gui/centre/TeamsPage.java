package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.client.commandcentre.ICommandCentrePage;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.common.teams.centre.CommandLines;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;

/** A page of the Teams command centre's own, for the Teams game modes, with the widgets its pages share. Client only. */
abstract class TeamsPage implements ICommandCentrePage
{
    static final String KEY = "gui.flansmodultimate.centre.";
    /** Height of a button or field. */
    static final int WIDGET_HEIGHT = 18;
    /** Vertical step from one row of widgets to the next. */
    static final int ROW = 21;
    static final int GAP = 4;

    private final String name;
    private final ItemLike icon;
    private final int order;

    TeamsPage(String name, ItemLike icon, int order)
    {
        this.name = name;
        this.icon = icon;
        this.order = order;
    }

    @Override
    public String getId()
    {
        return FlansMod.MOD_ID + ":" + name;
    }

    @Override
    public Component getTitle()
    {
        return tr("page." + name);
    }

    @Override
    public Component getDescription()
    {
        return tr("page." + name + ".description");
    }

    @Override
    public ItemStack getIcon()
    {
        return new ItemStack(icon);
    }

    @Override
    public int getOrder()
    {
        return order;
    }

    static MutableComponent tr(String key, Object... args)
    {
        return Component.translatable(KEY + key, args);
    }

    static TeamsData data()
    {
        return CommandCentreClient.teams();
    }

    static Button button(ICommandCentreView view, Component text, int x, int y, int width, Runnable action)
    {
        return view.addWidget(Button.builder(text, pressed -> action.run()).bounds(x, y, width, WIDGET_HEIGHT).build());
    }

    static Button button(ICommandCentreView view, Component text, int x, int y, int width, @Nullable Component tooltip, Runnable action)
    {
        Button button = button(view, text, x, y, width, action);
        if (tooltip != null)
            button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    /** An on/off switch that runs the command for the value chosen. */
    static void toggle(ICommandCentreView view, Component label, boolean value, int x, int y, int width, @Nullable Component tooltip, Consumer<Boolean> onChange)
    {
        CycleButton<Boolean> toggle = CycleButton.onOffBuilder(value).create(x, y, width, WIDGET_HEIGHT, label, (button, chosen) -> onChange.accept(chosen));
        if (tooltip != null)
            toggle.setTooltip(Tooltip.create(tooltip));
        view.addWidget(toggle);
    }

    /** A choice among values, cycled through by clicking. */
    static <T> CycleButton<T> choice(ICommandCentreView view, Component label, List<T> values, T value, Function<T, Component> names, int x, int y, int width, Consumer<T> onChange)
    {
        return view.addWidget(CycleButton.builder(names).withValues(values).withInitialValue(value).create(x, y, width, WIDGET_HEIGHT, label, (button, chosen) -> onChange.accept(chosen)));
    }

    /** Runs a command of words joined by spaces. */
    static void run(ICommandCentreView view, Object... words)
    {
        view.runCommand(CommandLines.line(words));
    }

    /** A string argument, quoted where it has to be. */
    static String quote(String value)
    {
        return CommandLines.quote(value);
    }

    /**
     * Reads a whole number from a field, or tells the operator it is not one within the bounds.
     *
     * @param what
     *            what the number is, to name it in the message
     */
    static OptionalInt number(ICommandCentreView view, String field, int least, int most, Component what)
    {
        try
        {
            int value = Integer.parseInt(view.getField(field).strip());
            if (value >= least && value <= most)
                return OptionalInt.of(value);
        }
        catch (NumberFormatException e)
        {
            // Told below, as a number out of bounds is.
        }
        view.showMessage(tr("bad_number", what, least, most), true);
        return OptionalInt.empty();
    }

    /** A range of whole numbers as it is shown, such as {@code 2–8}, or a single number when both ends are the same. */
    static String range(int least, int most)
    {
        return least == most ? Integer.toString(least) : least + "–" + most;
    }

    /** Draws a line of text cut to a width. */
    static void label(ICommandCentreView view, GuiGraphics graphics, Component text, int x, int y, int width, int colour)
    {
        CentreStyle.drawText(graphics, view.getFont(), text, x, y, width, colour);
    }

    /** Draws text wrapped to a width, and gives the height it took. */
    static int paragraph(ICommandCentreView view, GuiGraphics graphics, Component text, int x, int y, int width, int colour)
    {
        graphics.drawWordWrap(view.getFont(), text, x, y, width, colour);
        return view.getFont().wordWrapHeight(text, width);
    }

    /** A row of tab buttons, the chosen one pressed in; returns the height the row takes. */
    static <T extends Enum<T>> int tabs(ICommandCentreView view, T[] values, T chosen, Function<T, Component> names, int x, int y, int width, Consumer<T> onChoose)
    {
        int tabWidth = (width - GAP * (values.length - 1)) / values.length;
        for (T value : values)
        {
            Button tab = button(view, names.apply(value), x + value.ordinal() * (tabWidth + GAP), y, tabWidth, () ->
            {
                onChoose.accept(value);
                view.rebuild();
            });
            tab.active = value != chosen;
        }
        return WIDGET_HEIGHT + GAP;
    }
}
