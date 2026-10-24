package com.flansmodultimate.client.gui.options;

import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

/** Direct numeric entry. Only finite values of the correct type accepted by the config are queued for saving. */
public final class NumericConfigInput<T extends Number> extends EditBox
{
    private static final int VALID_COLOR = 0xE0E0E0;
    private static final int INVALID_COLOR = 0xFF5555;

    public NumericConfigInput(int width, ConfigTarget target, ConfigValue<?> value, T current, OptionInstance.TooltipSupplier<T> tooltip, Function<String, Optional<T>> parser)
    {
        super(Minecraft.getInstance().font, 0, 0, width, 20, Component.translatable("options.flansmodultimate." + value.getPath().get(value.getPath().size() - 1)));
        setMaxLength(64);
        setValue(format(current));
        setEditable(target.editable());
        setTooltip(tooltip.apply(current));
        setResponder(text ->
        {
            if (!target.editable())
                return;
            Optional<T> parsed = parser.apply(text);
            setTextColor(parsed.isPresent() ? VALID_COLOR : INVALID_COLOR);
            if (parsed.isPresent())
            {
                T number = parsed.orElseThrow();
                target.setWhileDragging(value, number);
                setTooltip(tooltip.apply(number));
            }
        });
    }

    static <T extends Number> Optional<T> parse(String text, ValueSpec spec, Function<String, T> parser)
    {
        try
        {
            T number = parser.apply(text.strip());
            return Double.isFinite(number.doubleValue()) && spec.test(number) ? Optional.of(number) : Optional.empty();
        }
        catch (NumberFormatException ignored)
        {
            return Optional.empty();
        }
    }

    static String format(Number number)
    {
        return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
    }
}
