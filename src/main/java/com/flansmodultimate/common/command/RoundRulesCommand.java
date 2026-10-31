package com.flansmodultimate.common.command;

import com.flansmodultimate.common.teams.*;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Player readiness and operator configuration of inherited round rules. */
public final class RoundRulesCommand
{
    private static final String DEFAULTS = "defaults";
    private static final String INHERIT = "inherit";
    private static final String TARGET = "target";
    private static final String RULE = "rule";
    private static final String VALUE = "value";

    private RoundRulesCommand()
    {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(Commands.literal("ready").executes(context -> ready(context, true))
            .then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context -> ready(context, BoolArgumentType.getBool(context, VALUE)))));
        root.then(Commands.literal("admin").requires(source -> source.hasPermission(2)).then(Commands.literal("roundrule").then(Commands.argument(TARGET, StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of(DEFAULTS), IntStream.range(0, manager().getRounds().size()).mapToObj(Integer::toString)), builder))
            .then(Commands.argument(RULE, StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(RoundRule.values()).map(RoundRule::getKey), builder))
                .executes(context -> configure(context, false)).then(Commands.argument(VALUE, StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{INHERIT, "true", "false", "0"}, builder)).executes(context -> configure(context, true)))))));
    }

    private static int ready(CommandContext<CommandSourceStack> context, boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        if (manager().getRoundRuleRuntime().ready(context.getSource().getPlayerOrException(), value))
            return 1;
        context.getSource().sendFailure(Component.translatable("message.flansmodultimate.teams.ready_unavailable"));
        return 0;
    }

    private static int configure(CommandContext<CommandSourceStack> context, boolean write)
    {
        try
        {
            String target = StringArgumentType.getString(context, TARGET);
            int index = DEFAULTS.equals(target) ? -1 : Integer.parseInt(target);
            if (index < -1 || !DEFAULTS.equals(target) && index < 0 || index >= manager().getRounds().size())
                throw new IllegalArgumentException("Invalid round index");
            RoundRule rule = RoundRule.find(StringArgumentType.getString(context, RULE)).orElseThrow(() -> new IllegalArgumentException("Unknown round rule"));
            if (write)
            {
                String value = StringArgumentType.getString(context, VALUE);
                validateItems(rule, value);
                if (!manager().setRoundRule(index, rule, INHERIT.equals(value) ? null : value))
                    throw new IllegalArgumentException("Invalid round index");
            }
            RoundRules configured = index < 0 ? manager().getRoundRuleDefaults() : manager().getRounds().get(index).getRules().resolve(manager().getRoundRuleDefaults());
            context.getSource().sendSuccess(() -> Component.literal(rule.getKey() + " = " + configured.get(rule) + (write ? " (saved; applies when the round next starts)" : "")), write);
            return 1;
        }
        catch (IllegalArgumentException exception)
        {
            context.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    @SuppressWarnings("deprecation") // Forge 1.20.1 retains the built-in item registry for validating item ids.
    private static void validateItems(RoundRule rule, String value)
    {
        if (rule.getKind() != RoundRule.Kind.ITEMS || INHERIT.equals(value))
            return;
        String validated = rule.validate(value);
        if (validated.isEmpty())
            return;
        for (String item : validated.split(","))
        {
            if (!BuiltInRegistries.ITEM.containsKey(java.util.Objects.requireNonNull(ResourceLocation.tryParse(item))))
                throw new IllegalArgumentException("Unknown item: " + item);
        }
    }

    private static TeamsManager manager()
    {
        return TeamsManager.getInstance();
    }
}
