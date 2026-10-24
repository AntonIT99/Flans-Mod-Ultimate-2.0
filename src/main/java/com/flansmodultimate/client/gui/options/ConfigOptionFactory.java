package com.flansmodultimate.client.gui.options;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.config.*;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Turns config entries into the option widgets the options screen shows. Captions come from the language
 * file, tooltips from the comment already written in the config file, so a new config entry shows up with
 * its documentation without a second description having to be maintained here.
 *
 * <p>
 * Booleans, enums and bounded numbers are editable on screen. Free text and lists are left to the config
 * file itself. A toggle applies at once; sliders and numeric input fields record values while editing,
 * and the options screen applies them once it closes.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigOptionFactory
{
    private static final String KEY_PREFIX = "options.flansmodultimate.";
    private static final String VALUE_KEY_PREFIX = KEY_PREFIX + "value.";
    /** Steps a decimal slider offers between its bounds. */
    private static final int DOUBLE_SLIDER_STEPS = 100;
    /** Wider ranges are difficult to select precisely on a short slider in All Settings. */
    private static final int MAX_SLIDER_SPAN = 100;

    /** A widget factory, with a separate visible label for direct input fields. */
    public record Option(@Nullable Component label, IntFunction<AbstractWidget> widgetFactory, ConfigTarget target, ConfigValue<?> value)
    {}

    /** A config section and the options of it that can be edited on screen. */
    public record Section(Component title, List<Option> options)
    {}

    /** Every section of the target's config, in the order the config file declares them. */
    public static List<Section> sections(ConfigTarget target)
    {
        List<Section> sections = new ArrayList<>();
        collectSections(target, target.spec().getValues(), "", sections);
        return sections;
    }

    private static void collectSections(ConfigTarget target, UnmodifiableConfig values, String prefix, List<Section> sections)
    {
        for (Map.Entry<String, Object> entry : values.valueMap().entrySet())
        {
            if (!(entry.getValue() instanceof UnmodifiableConfig section))
                continue;

            String title = prefix.isEmpty() ? entry.getKey() : prefix + " / " + entry.getKey();
            List<Option> options = optionsOf(target, section);
            if (!options.isEmpty())
                sections.add(new Section(Component.literal(title), options));

            // A nested section follows the one it belongs to, under its own heading
            collectSections(target, section, title, sections);
        }
    }

    private static List<Option> optionsOf(ConfigTarget target, UnmodifiableConfig section)
    {
        List<Option> options = new ArrayList<>();
        for (Object child : section.valueMap().values())
        {
            if (!(child instanceof ConfigValue<?> value))
                continue;

            Option option = option(target, value, true);
            if (option != null)
                options.add(option);
        }
        return options;
    }

    /** The option widget for one config entry, or null when its type cannot be edited on screen. */
    @Nullable
    public static Option option(ConfigTarget target, ConfigValue<?> value)
    {
        return option(target, value, false);
    }

    @Nullable
    private static Option option(ConfigTarget target, ConfigValue<?> value, boolean allSettings)
    {
        if ((value == ModClientConfig.FLAN_NPC_RENDER_DISTANCE_MULTIPLIER || value == ModCommonConfig.FLAN_NPC_TRACKING_RANGE) && !PlatformEnvironment.isModLoaded("wolffsmodnpcs"))
            return null;
        ValueSpec valueSpec = ConfigSpecValues.valueSpec(target.spec(), value.getPath());
        if (valueSpec == null)
            return null;

        Object current = target.get(value);
        if (current instanceof Boolean flag)
            return widgetOption(target, value, booleanOption(target, value, valueSpec, flag));

        if (current instanceof Enum<?>)
            return widgetOption(target, value, enumOption(target, value, valueSpec, valueSpec.getClazz(), current));

        if (current instanceof Integer number)
            return integerWidgetOption(target, value, valueSpec, number, allSettings);

        if (current instanceof Double number)
            return doubleWidgetOption(target, value, valueSpec, number, allSettings);

        return null;
    }

    @Nullable
    private static Option integerWidgetOption(ConfigTarget target, ConfigValue<?> value, ValueSpec spec, int current, boolean allSettings)
    {
        if (allSettings && hasWideRange(spec))
            return numericOption(target, value, spec, current, input -> intValueText(value, input), Integer::valueOf);
        return widgetOption(target, value, intOption(target, value, spec, current));
    }

    @Nullable
    private static Option doubleWidgetOption(ConfigTarget target, ConfigValue<?> value, ValueSpec spec, double current, boolean allSettings)
    {
        if (ModCommonConfig.isSoundRange(value) || allSettings && hasWideRange(spec))
        {
            Function<Double, Component> valueText = number -> ModCommonConfig.isSoundRange(value)
                ? Component.translatable("options.flansmodultimate.distance.blocks", NumericConfigInput.format(number))
                : Component.literal(NumericConfigInput.format(number));
            return numericOption(target, value, spec, current, valueText, Double::valueOf);
        }
        return widgetOption(target, value, doubleOption(target, value, spec, current));
    }

    private static boolean hasWideRange(ValueSpec spec)
    {
        Range<?> range = spec.getRange();
        return range != null && range.getMin() instanceof Number min && range.getMax() instanceof Number max && max.doubleValue() - min.doubleValue() > MAX_SLIDER_SPAN;
    }

    private static <T extends Number> Option numericOption(ConfigTarget target, ConfigValue<?> value, ValueSpec spec, T current, Function<T, Component> valueText, Function<String, T> parser)
    {
        return new Option(Component.translatable(captionKey(value)),
            width -> new NumericConfigInput<>(width, target, value, current, tooltip(target, value, spec, valueText), text -> NumericConfigInput.parse(text, spec, parser)), target, value);
    }

    @Nullable
    private static Option widgetOption(ConfigTarget target, ConfigValue<?> value, @Nullable OptionInstance<?> option)
    {
        return option == null ? null : new Option(null, width -> option.createButton(Minecraft.getInstance().options, 0, 0, width), target, value);
    }

    private static OptionInstance<Boolean> booleanOption(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, boolean current)
    {
        return OptionInstance.createBoolean(captionKey(value), tooltip(target, value, valueSpec, CommonComponents::optionStatus), current, newValue -> target.set(value, newValue));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionInstance<?> enumOption(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, Class<?> type, Object current)
    {
        return enumOption(target, value, valueSpec, (Class) type, (Enum) current);
    }

    private static <T extends Enum<T>> OptionInstance<T> enumOption(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, Class<T> type, T current)
    {
        Codec<T> codec = Codec.STRING.xmap(constant -> Enum.valueOf(type, constant), Enum::name);
        Function<T, Component> valueText = constant -> valueLabel(type, constant);
        return new OptionInstance<>(captionKey(value), tooltip(target, value, valueSpec, valueText), (caption, constant) -> valueText.apply(constant),
            new OptionInstance.Enum<>(List.of(type.getEnumConstants()), codec), current, newValue -> target.set(value, newValue));
    }

    @Nullable
    private static OptionInstance<Integer> intOption(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, int current)
    {
        Range<Integer> range = valueSpec.getRange();
        if (range == null)
            return null;

        Function<Integer, Component> valueText = number -> intValueText(value, number);
        return new OptionInstance<>(captionKey(value), tooltip(target, value, valueSpec, valueText), (caption, number) -> Options.genericValueLabel(caption, valueText.apply(number)),
            new OptionInstance.IntRange(range.getMin(), range.getMax()), current, newValue -> target.setWhileDragging(value, newValue));
    }

    private static Component intValueText(ConfigValue<?> value, int number)
    {
        if (value == ModCommonConfig.DRIVEABLE_TRACKING_RANGE || value == ModCommonConfig.FLAN_NPC_TRACKING_RANGE)
            return Component.translatable("options.flansmodultimate.distance.blocks", number);
        if (value != ModClientConfig.GPU_MODEL_CACHE_MEGABYTES)
            return Component.literal(String.valueOf(number));
        return number == 0 ? Component.translatable(VALUE_KEY_PREFIX + "automatic") : Component.translatable("options.flansmodultimate.memory.mebibytes", number);
    }

    @Nullable
    private static OptionInstance<Double> doubleOption(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, double current)
    {
        Range<Double> range = valueSpec.getRange();
        if (range == null)
            return null;

        double min = range.getMin();
        boolean distanceMultiplier = value == ModClientConfig.DRIVEABLE_RENDER_DISTANCE_MULTIPLIER || value == ModClientConfig.FLAN_NPC_RENDER_DISTANCE_MULTIPLIER;
        int stepsCount = distanceMultiplier ? 75 : DOUBLE_SLIDER_STEPS;
        double step = (range.getMax() - min) / stepsCount;
        if (step <= 0)
            return null;

        Function<Double, Component> valueText = number -> Component.literal(String.format(Locale.ROOT, distanceMultiplier ? "%.2f×" : "%.2f", number));
        return new OptionInstance<>(captionKey(value), tooltip(target, value, valueSpec, valueText), (caption, number) -> Options.genericValueLabel(caption, valueText.apply(number)),
            new OptionInstance.IntRange(0, stepsCount).xmap(steps -> min + steps * step, number -> (int) Math.round((number - min) / step)), current,
            newValue -> target.setWhileDragging(value, newValue));
    }

    /** Vanilla builds the caption from a translation key, so every editable entry needs one. */
    private static String captionKey(ConfigValue<?> value)
    {
        return KEY_PREFIX + value.getPath().get(value.getPath().size() - 1);
    }

    /**
     * Every tooltip opens with the setting's name, current value and default, so a row stays readable when its
     * label is cut short or a slider is being dragged; the description follows below.
     */
    // ValueSpec and the formatter describe the same runtime config type.
    @SuppressWarnings("unchecked")
    private static <T> OptionInstance.TooltipSupplier<T> tooltip(ConfigTarget target, ConfigValue<?> value, ValueSpec valueSpec, Function<T, Component> valueText)
    {
        Component title = Component.translatable(captionKey(value));
        Component description = description(value, valueSpec);
        // Locked server rows still show their tooltip, so it says why they cannot be changed
        boolean locked = !target.editable();
        return current ->
        {
            MutableComponent text = tooltipHeader(title, valueText.apply(current), valueText.apply((T) valueSpec.getDefault()));
            if (description != null)
                text.append("\n\n").append(description);
            if (locked)
                text.append("\n\n").append(Component.translatable("gui.flansmodultimate.options.server_option_locked").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return Tooltip.create(text);
        };
    }

    /** The styled first lines of a setting's tooltip: its name, current value and default. */
    public static MutableComponent tooltipHeader(Component title, Component value, Component defaultValue)
    {
        return Component.empty().append(title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append("\n")
            .append(Component.translatable(KEY_PREFIX + "tooltip.current_value", value.copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)).append("\n")
            .append(Component.translatable(KEY_PREFIX + "tooltip.default_value", defaultValue.copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * The language file wins when it describes the entry, otherwise the comment from the config file is
     * shown, which is the same text the player would read when editing the file by hand.
     */
    @Nullable
    private static Component description(ConfigValue<?> value, ValueSpec valueSpec)
    {
        String key = captionKey(value) + ".tooltip";
        if (value == ModClientConfig.DRIVEABLE_RENDER_DISTANCE_MULTIPLIER || value == ModClientConfig.FLAN_NPC_RENDER_DISTANCE_MULTIPLIER)
            return Component.translatable(key).append("\n").append(Component.translatable("options.flansmodultimate.distance.tracking_limit",
                value == ModClientConfig.DRIVEABLE_RENDER_DISTANCE_MULTIPLIER ? ModCommonConfig.driveableTrackingRange() : ModCommonConfig.flanNpcTrackingRange()));
        if (value == ModClientConfig.GPU_MODEL_CACHE_MEGABYTES)
        {
            long video = GpuModelCache.reportedVideoMemoryMegabytes();
            Component automatic = video > 0
                ? Component.translatable(key + ".automatic", GpuModelCache.automaticBudgetMegabytes(), video)
                : Component.translatable(key + ".automatic_unreported", GpuModelCache.automaticBudgetMegabytes());
            return Component.translatable(key).append("\n").append(automatic);
        }
        if (I18n.exists(key))
            return Component.translatable(key);

        String comment = valueSpec.getComment();
        return comment == null || comment.isBlank() ? null : Component.literal(comment.strip());
    }

    /** Enum constants share their labels between every option using the same enum. */
    public static Component valueLabel(Class<?> type, Enum<?> constant)
    {
        String name = type.getSimpleName();
        if (name.startsWith("Enum"))
            name = name.substring("Enum".length());

        return Component.translatableWithFallback(VALUE_KEY_PREFIX + name.toLowerCase(Locale.ROOT) + "." + constant.name().toLowerCase(Locale.ROOT), constant.name());
    }
}
