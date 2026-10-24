package com.wolffsmod.npcs.client.centre;

import com.flansmodultimate.api.client.commandcentre.ICommandCentrePage;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.teams.BattlesSection;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A page the module adds to the Teams command centre, for battles fought by bots, with the widgets its pages share. It
 * is listed while the server sends the module's section. Client only.
 */
abstract class BattlePage implements ICommandCentrePage
{
    static final String KEY = "wolffsmodnpcs.centre.";
    static final int WIDGET_HEIGHT = 18;
    static final int ROW = 21;
    static final int GAP = 4;
    static final int TEXT = 0xFFFFFFFF;
    static final int DIM = 0xFFA9B4C2;
    static final int MUTED = 0xFF6F7B8A;
    static final int ACCENT = 0xFFE3A13B;
    static final int BAD = 0xFFFF6B6B;
    private static final Component ELLIPSIS = Component.literal("…");

    private final String name;
    private final ItemLike icon;
    private final int order;
    /** The section as the page was laid out from. */
    BattlesData data = BattlesData.read(new CompoundTag());

    BattlePage(String name, ItemLike icon, int order)
    {
        this.name = name;
        this.icon = icon;
        this.order = order;
    }

    @Override
    public String getId()
    {
        return NpcsMod.MOD_ID + ":" + name;
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

    @Override
    public boolean isAvailable(ICommandCentreView view)
    {
        return view.hasSection(BattlesSection.KEY);
    }

    @Override
    public final void init(ICommandCentreView view)
    {
        data = BattlesData.read(view.getSection(BattlesSection.KEY));
        layout(view);
    }

    /** Lays the page out from {@link #data}, read just before. */
    abstract void layout(ICommandCentreView view);

    static MutableComponent tr(String key, Object... args)
    {
        return Component.translatable(KEY + key, args);
    }

    /** A setting's value in words, such as a skirmish's intent, by the name the command takes it under. */
    static Component value(String setting, String value)
    {
        String key = KEY + setting + "." + value;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(value);
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

    static CycleButton<Boolean> toggle(ICommandCentreView view, Component label, boolean value, int x, int y, int width, Consumer<Boolean> onChange)
    {
        return view.addWidget(CycleButton.onOffBuilder(value).create(x, y, width, WIDGET_HEIGHT, label, (button, chosen) -> onChange.accept(chosen)));
    }

    static <T> CycleButton<T> choice(ICommandCentreView view, Component label, List<T> values, T value, Function<T, Component> names, int x, int y, int width, Consumer<T> onChange)
    {
        return view.addWidget(CycleButton.builder(names).withValues(values).withInitialValue(value).create(x, y, width, WIDGET_HEIGHT, label, (button, chosen) -> onChange.accept(chosen)));
    }

    /** Runs a command of words joined by spaces. */
    static void run(ICommandCentreView view, Object... words)
    {
        view.runCommand(Arrays.stream(words).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    /** A string argument, quoted where it has to be, as faction and skirmish names may need. */
    static String quote(String value)
    {
        return StringArgumentType.escapeIfRequired(value);
    }

    /** Reads a whole number from a field, or tells the operator it is not one within the bounds. */
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

    /** Draws a line of text cut to a width, ending in an ellipsis where it was cut. */
    static void label(ICommandCentreView view, GuiGraphics graphics, Component text, int x, int y, int width, int colour)
    {
        Font font = view.getFont();
        if (font.width(text) <= width)
        {
            graphics.drawString(font, text, x, y, colour);
            return;
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, width - font.width(ELLIPSIS)));
        graphics.drawString(font, Language.getInstance().getVisualOrder(FormattedText.composite(cut, ELLIPSIS)), x, y, colour);
    }

    /** Draws text wrapped to a width, and gives the height it took. */
    static int paragraph(ICommandCentreView view, GuiGraphics graphics, Component text, int x, int y, int width, int colour)
    {
        graphics.drawWordWrap(view.getFont(), text, x, y, width, colour);
        return view.getFont().wordWrapHeight(text, width);
    }

    /** A row of tab buttons, the chosen one pressed in. */
    static <T extends Enum<T>> void tabs(ICommandCentreView view, T[] values, T chosen, Function<T, Component> names, int x, int y, int width, Consumer<T> onChoose)
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
    }

    /** The first of some values when the one chosen is not among them, as after the server removed it. */
    static <T> T keep(List<T> values, T chosen, T none)
    {
        if (values.contains(chosen))
            return chosen;
        return values.isEmpty() ? none : values.get(0);
    }
}
