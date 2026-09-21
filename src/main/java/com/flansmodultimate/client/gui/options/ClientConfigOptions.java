package com.flansmodultimate.client.gui.options;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns client config entries into the option widgets the options screen shows. Captions come from the
 * language file, tooltips from the comment already written in the config file, so a new config entry shows
 * up with its documentation without a second description having to be maintained here.
 *
 * <p>Booleans, enums and bounded numbers are editable on screen. Free text and lists are left to the config
 * file itself. A toggle applies at once; a slider only marks the config dirty while it is being dragged, and
 * the options screen writes the file once it closes.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConfigOptions
{
    private static final String KEY_PREFIX = "options.flansmodultimate.";
    private static final String VALUE_KEY_PREFIX = KEY_PREFIX + "value.";
    /** Steps a decimal slider offers between its bounds. */
    private static final int DOUBLE_SLIDER_STEPS = 100;

    /** A config section and the options of it that can be edited on screen. */
    public record Section(Component title, List<OptionInstance<?>> options)
    {
    }

    /** Every client config section, in the order the config file declares them. */
    public static List<Section> sections()
    {
        List<Section> sections = new ArrayList<>();
        collectSections(ModClientConfig.configSpec, ModClientConfig.configSpec.getValues(), sections);
        return sections;
    }

    private static void collectSections(ForgeConfigSpec spec, UnmodifiableConfig values, List<Section> sections)
    {
        for (Map.Entry<String, Object> entry : values.valueMap().entrySet())
        {
            if (!(entry.getValue() instanceof UnmodifiableConfig section))
                continue;

            List<OptionInstance<?>> options = new ArrayList<>();
            for (Object child : section.valueMap().values())
            {
                if (child instanceof ForgeConfigSpec.ConfigValue<?> value)
                {
                    OptionInstance<?> option = option(value);
                    if (option != null)
                        options.add(option);
                }
            }

            if (!options.isEmpty())
                sections.add(new Section(Component.literal(entry.getKey()), options));
        }
    }

    /** The option widget for one config entry, or null when its type cannot be edited on screen. */
    @Nullable
    public static OptionInstance<?> option(ForgeConfigSpec.ConfigValue<?> value)
    {
        ForgeConfigSpec.ValueSpec valueSpec = ModClientConfig.configSpec.get(value.getPath());
        if (valueSpec == null)
            return null;

        if (value instanceof ForgeConfigSpec.BooleanValue booleanValue)
            return booleanOption(booleanValue, valueSpec);

        if (value instanceof ForgeConfigSpec.EnumValue<?> enumValue)
            return enumOption(enumValue, valueSpec);

        if (value instanceof ForgeConfigSpec.IntValue intValue)
            return intOption(intValue, valueSpec);

        if (value instanceof ForgeConfigSpec.DoubleValue doubleValue)
            return doubleOption(doubleValue, valueSpec);

        return null;
    }

    public static OptionInstance<Boolean> booleanOption(ForgeConfigSpec.BooleanValue value, ForgeConfigSpec.ValueSpec valueSpec)
    {
        return OptionInstance.createBoolean(captionKey(value), tooltip(value, valueSpec), value.get(),
            newValue -> ModClientConfig.setAndSave(value, newValue));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionInstance<?> enumOption(ForgeConfigSpec.EnumValue<?> value, ForgeConfigSpec.ValueSpec valueSpec)
    {
        return enumOption((ForgeConfigSpec.EnumValue) value, valueSpec, (Class) valueSpec.getClazz());
    }

    private static <T extends Enum<T>> OptionInstance<T> enumOption(ForgeConfigSpec.EnumValue<T> value, ForgeConfigSpec.ValueSpec valueSpec, Class<T> type)
    {
        Codec<T> codec = Codec.STRING.xmap(constant -> Enum.valueOf(type, constant), Enum::name);
        return new OptionInstance<>(captionKey(value), tooltip(value, valueSpec),
            (caption, constant) -> valueLabel(type, constant),
            new OptionInstance.Enum<>(List.of(type.getEnumConstants()), codec),
            value.get(),
            newValue -> ModClientConfig.setAndSave(value, newValue));
    }

    @Nullable
    private static OptionInstance<Integer> intOption(ForgeConfigSpec.IntValue value, ForgeConfigSpec.ValueSpec valueSpec)
    {
        ForgeConfigSpec.Range<Integer> range = valueSpec.getRange();
        if (range == null)
            return null;

        return new OptionInstance<>(captionKey(value), tooltip(value, valueSpec),
            (caption, number) -> Options.genericValueLabel(caption, Component.literal(String.valueOf(number))),
            new OptionInstance.IntRange(range.getMin(), range.getMax()),
            value.get(),
            newValue -> ModClientConfig.set(value, newValue));
    }

    @Nullable
    private static OptionInstance<Double> doubleOption(ForgeConfigSpec.DoubleValue value, ForgeConfigSpec.ValueSpec valueSpec)
    {
        ForgeConfigSpec.Range<Double> range = valueSpec.getRange();
        if (range == null)
            return null;

        double min = range.getMin();
        double step = (range.getMax() - min) / DOUBLE_SLIDER_STEPS;
        if (step <= 0)
            return null;

        return new OptionInstance<>(captionKey(value), tooltip(value, valueSpec),
            (caption, number) -> Options.genericValueLabel(caption, Component.literal(String.format(Locale.ROOT, "%.2f", number))),
            new OptionInstance.IntRange(0, DOUBLE_SLIDER_STEPS).xmap(
                steps -> min + steps * step,
                number -> (int) Math.round((number - min) / step)),
            value.get(),
            newValue -> ModClientConfig.set(value, newValue));
    }

    /** Vanilla builds the caption from a translation key, so every editable entry needs one. */
    private static String captionKey(ForgeConfigSpec.ConfigValue<?> value)
    {
        return KEY_PREFIX + value.getPath().get(value.getPath().size() - 1);
    }

    /**
     * The language file wins when it describes the entry, otherwise the comment from the config file is
     * shown, which is the same text the player would read when editing the file by hand.
     */
    private static <T> OptionInstance.TooltipSupplier<T> tooltip(ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec valueSpec)
    {
        String key = captionKey(value) + ".tooltip";
        if (I18n.exists(key))
            return OptionInstance.cachedConstantTooltip(Component.translatable(key));

        String comment = valueSpec.getComment();
        if (comment == null || comment.isBlank())
            return OptionInstance.noTooltip();

        return OptionInstance.cachedConstantTooltip(Component.literal(comment.strip()));
    }

    /** Enum constants share their labels between every option using the same enum. */
    public static Component valueLabel(Class<?> type, Enum<?> constant)
    {
        String name = type.getSimpleName();
        if (name.startsWith("Enum"))
            name = name.substring("Enum".length());

        return Component.translatableWithFallback(
            VALUE_KEY_PREFIX + name.toLowerCase(Locale.ROOT) + "." + constant.name().toLowerCase(Locale.ROOT),
            constant.name());
    }
}
