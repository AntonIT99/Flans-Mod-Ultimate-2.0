package com.flansmodultimate.client.gui.options;

import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.OptionalDouble;

/** Direct sound-range entry. Only finite values accepted by the config are queued for saving. */
public final class SoundRangeInput extends EditBox
{
    private static final int VALID_COLOR = 0xE0E0E0;
    private static final int INVALID_COLOR = 0xFF5555;

    public SoundRangeInput(int width, ConfigTarget target, ConfigValue<?> value, ValueSpec spec, double current, OptionInstance.TooltipSupplier<Double> tooltip)
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
            OptionalDouble parsed = parse(text, spec);
            setTextColor(parsed.isPresent() ? VALID_COLOR : INVALID_COLOR);
            if (parsed.isPresent())
            {
                double number = parsed.getAsDouble();
                target.setWhileDragging(value, number);
                setTooltip(tooltip.apply(number));
            }
        });
    }

    static OptionalDouble parse(String text, ValueSpec spec)
    {
        try
        {
            double number = Double.parseDouble(text.strip());
            return Double.isFinite(number) && spec.test(number) ? OptionalDouble.of(number) : OptionalDouble.empty();
        }
        catch (NumberFormatException ignored)
        {
            return OptionalDouble.empty();
        }
    }

    static String format(double number)
    {
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
}
