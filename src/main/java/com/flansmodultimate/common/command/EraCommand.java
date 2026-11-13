package com.flansmodultimate.common.command;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.eras.Era;
import com.flansmodultimate.common.eras.EraEntities;
import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.EraRestrictions;
import com.flansmodultimate.common.eras.EraSavedData;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.IssueDateOverrides;
import com.flansmodultimate.common.eras.IssueDates;
import com.flansmodultimate.common.eras.ScopedRestriction;
import com.flansmodultimate.common.eras.ServiceDate;
import com.flansmodultimate.common.eras.ServicePeriod;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@code /flansmodultimate era}: item dates, the server and dimension restrictions, checks and the operator bypass.
 * Restrictions of Teams rounds and battles are set through their own commands. Output is operator-facing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraCommand
{
    private static final String ITEM = "item";
    private static final String DATES = "dates";
    private static final String RESTRICTION = "restriction";
    private static final String DIMENSION = "dimension";
    private static final String PLAYER = "player";
    private static final String FILTER = "filter";
    private static final String PAGE = "page";
    private static final int PAGE_SIZE = 15;
    private static final double UNTAG_REACH = 8D;
    private static final String DATED = "dated";
    private static final String UNDATED = "undated";
    private static final String OVERRIDES = "overrides";
    private static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(id -> Component.literal("Unknown Flan's item '" + id + "'"));
    private static final SuggestionProvider<CommandSourceStack> ITEMS = (context, builder) -> SharedSuggestionProvider.suggest(InfoType.getInfoTypes().keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> era = Commands.literal("era");
        era.then(Commands.literal("check").executes(context -> check(context.getSource(), context.getSource().getPlayerOrException(), null))
            .then(Commands.argument(PLAYER, EntityArgument.player()).requires(EraCommand::operator)
                .executes(context -> check(context.getSource(), EntityArgument.getPlayer(context, PLAYER), null))
                .then(Commands.argument(ITEM, StringArgumentType.word()).suggests(ITEMS)
                    .executes(context -> check(context.getSource(), EntityArgument.getPlayer(context, PLAYER), item(context))))));
        era.then(Commands.literal("date").requires(EraCommand::operator)
            .then(Commands.literal("set").then(Commands.argument(ITEM, StringArgumentType.word()).suggests(ITEMS)
                .then(Commands.argument(DATES, StringArgumentType.greedyString()).executes(EraCommand::setDate))))
            .then(Commands.literal("clear").then(Commands.argument(ITEM, StringArgumentType.word()).suggests(ITEMS).executes(EraCommand::clearDate)))
            .then(Commands.literal("show").then(Commands.argument(ITEM, StringArgumentType.word()).suggests(ITEMS).executes(context -> showDate(context.getSource(), item(context)))))
            .then(Commands.literal("list").executes(context -> listDates(context.getSource(), OVERRIDES, 1))
                .then(Commands.argument(FILTER, StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of(DATED, UNDATED, OVERRIDES), builder))
                    .executes(context -> listDates(context.getSource(), StringArgumentType.getString(context, FILTER), 1))
                    .then(Commands.argument(PAGE, IntegerArgumentType.integer(1))
                        .executes(context -> listDates(context.getSource(), StringArgumentType.getString(context, FILTER), IntegerArgumentType.getInteger(context, PAGE)))))));
        era.then(Commands.literal("server").requires(EraCommand::operator).executes(context -> showWorld(context.getSource()))
            .then(Commands.literal("set").then(Commands.argument(RESTRICTION, StringArgumentType.greedyString()).executes(context -> setRestriction(context, null))))
            .then(Commands.literal("clear").executes(context -> clearRestriction(context.getSource(), null))));
        era.then(Commands.literal(DIMENSION).requires(EraCommand::operator).then(Commands.argument(DIMENSION, DimensionArgument.dimension()).executes(context -> showWorld(context.getSource()))
            .then(Commands.literal("set").then(Commands.argument(RESTRICTION, StringArgumentType.greedyString()).executes(context -> setRestriction(context, dimension(context)))))
            .then(Commands.literal("clear").executes(context -> clearRestriction(context.getSource(), dimension(context))))));
        era.then(Commands.literal("bypass").requires(EraCommand::operator).executes(context -> bypass(context.getSource(), null))
            .then(Commands.literal("on").executes(context -> bypass(context.getSource(), true)))
            .then(Commands.literal("off").executes(context -> bypass(context.getSource(), false))));
        era.then(Commands.literal("untag").requires(EraCommand::operator).executes(context -> untag(context.getSource())));
        era.then(Commands.literal("eras").requires(EraCommand::operator).executes(context -> listEras(context.getSource()))
            .then(Commands.literal("list").executes(context -> listEras(context.getSource())))
            .then(Commands.literal("reload").executes(context -> reload(context.getSource()))));
        dispatcher.register(Commands.literal(FlansModCommandAliases.ROOT).then(era));
    }

    private static boolean operator(CommandSourceStack source)
    {
        return source.hasPermission(2);
    }

    private static InfoType item(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String id = StringArgumentType.getString(context, ITEM);
        InfoType type = InfoType.getInfoType(id, null);
        if (type == null)
            throw UNKNOWN_ITEM.create(id);
        return type;
    }

    private static String dimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        return DimensionArgument.getDimension(context, DIMENSION).dimension().location().toString();
    }

    private static int setDate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        InfoType type = item(context);
        ServicePeriod period;
        try
        {
            period = ServicePeriod.parse(StringArgumentType.getString(context, DATES));
        }
        catch (IllegalArgumentException exception)
        {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        if (!period.isResolvable(IssueDates.eras()))
        {
            source.sendFailure(Component.literal("Unknown era in '" + period + "'; see /flansmodultimate era eras"));
            return 0;
        }
        if (!period.isOrdered(IssueDates.eras()))
        {
            source.sendFailure(Component.literal("The retirement ends before the introduction starts"));
            return 0;
        }
        IssueDates.overrides().set(type.getShortName(), period);
        if (!saveOverrides(source))
            return 0;
        source.sendSuccess(() -> Component.literal(type.getShortName() + " is now dated " + period + " by override"), true);
        return 1;
    }

    private static int clearDate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        InfoType type = item(context);
        if (!IssueDates.overrides().clear(type.getShortName()))
        {
            source.sendFailure(Component.literal(type.getShortName() + " has no date override"));
            return 0;
        }
        if (!saveOverrides(source))
            return 0;
        ServicePeriod own = type.getServicePeriod();
        source.sendSuccess(() -> Component.literal(type.getShortName() + " override removed; it is now " + (own == null ? "undated" : "dated " + own + " by its pack or category")), true);
        return 1;
    }

    private static boolean saveOverrides(CommandSourceStack source)
    {
        try
        {
            IssueDates.overrides().save();
        }
        catch (IOException exception)
        {
            source.sendFailure(Component.literal("The override applies until the server stops but could not be saved: " + exception.getMessage()));
            EraRestrictions.datesChanged(source.getServer());
            return false;
        }
        EraRestrictions.datesChanged(source.getServer());
        return true;
    }

    private static int showDate(CommandSourceStack source, InfoType type)
    {
        Optional<ServicePeriod> override = IssueDates.overrides().get(type.getShortName());
        ServicePeriod own = type.getServicePeriod();
        StringBuilder text = new StringBuilder(type.getShortName()).append(": ");
        if (override.isPresent())
            text.append(override.get()).append(" (server override").append(own == null ? "" : "; its own dates are " + own).append(')');
        else if (own != null)
            text.append(own).append(" (pack or category)");
        else
            text.append("undated");
        ServicePeriod effective = IssueDates.periodOf(type);
        if (effective != null && !effective.isResolvable(IssueDates.eras()))
            text.append(", names an unknown era and counts as undated");
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int listDates(CommandSourceStack source, String filter, int page)
    {
        Stream<Map.Entry<String, InfoType>> types = InfoType.getInfoTypes().entrySet().stream().sorted(Map.Entry.comparingByKey());
        IssueDateOverrides overrides = IssueDates.overrides();
        List<String> lines = switch (filter)
        {
            case DATED -> types.filter(entry -> IssueDates.periodOf(entry.getValue()) != null).map(entry -> entry.getKey() + ": " + IssueDates.periodOf(entry.getValue())).toList();
            case UNDATED -> types.filter(entry -> IssueDates.periodOf(entry.getValue()) == null).map(Map.Entry::getKey).toList();
            case OVERRIDES -> overrides.getAll().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).toList();
            default -> null;
        };
        if (lines == null)
        {
            source.sendFailure(Component.literal("Expected dated, undated or overrides"));
            return 0;
        }
        int pages = Math.max(1, (lines.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int shown = Math.min(page, pages);
        source.sendSuccess(() -> Component.literal(lines.size() + " " + filter + " item(s), page " + shown + " of " + pages).withStyle(ChatFormatting.GOLD), false);
        for (String line : lines.subList((shown - 1) * PAGE_SIZE, Math.min(lines.size(), shown * PAGE_SIZE)))
            source.sendSuccess(() -> Component.literal(line), false);
        return lines.size();
    }

    private static int setRestriction(CommandContext<CommandSourceStack> context, @Nullable String dimension)
    {
        CommandSourceStack source = context.getSource();
        EraRestriction restriction;
        try
        {
            restriction = EraRestriction.parse(StringArgumentType.getString(context, RESTRICTION));
        }
        catch (IllegalArgumentException exception)
        {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        Optional<String> problem = restriction.validate(IssueDates.eras());
        if (problem.isPresent())
        {
            source.sendFailure(Component.literal(problem.get()));
            return 0;
        }
        EraSavedData data = EraSavedData.get(source.getServer());
        if (dimension == null)
            data.setServer(restriction);
        else
            data.setDimension(dimension, restriction);
        EraRestrictions.restrictionsChanged(source.getServer());
        String scope = dimension == null ? "Server" : "Dimension " + dimension;
        source.sendSuccess(() -> Component.literal(scope + " era restriction: " + restriction), true);
        return 1;
    }

    private static int clearRestriction(CommandSourceStack source, @Nullable String dimension)
    {
        EraSavedData data = EraSavedData.get(source.getServer());
        boolean present = dimension == null ? data.getServer().isPresent() : data.getDimension(dimension).isPresent();
        if (!present)
        {
            source.sendFailure(Component.literal(dimension == null ? "The server has no era restriction" : "Dimension " + dimension + " has no era restriction"));
            return 0;
        }
        if (dimension == null)
            data.setServer(null);
        else
            data.setDimension(dimension, null);
        EraRestrictions.restrictionsChanged(source.getServer());
        source.sendSuccess(() -> Component.literal(dimension == null ? "Server era restriction cleared" : "Dimension " + dimension + " era restriction cleared"), true);
        return 1;
    }

    private static int showWorld(CommandSourceStack source)
    {
        EraSavedData data = EraSavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal("Server: " + data.getServer().map(EraRestriction::toString).orElse("none")), false);
        if (data.getDimensions().isEmpty())
            source.sendSuccess(() -> Component.literal("No dimension restrictions"), false);
        data.getDimensions().forEach((dimension, restriction) -> source.sendSuccess(() -> Component.literal(dimension + ": " + restriction), false));
        return 1;
    }

    private static int check(CommandSourceStack source, ServerPlayer player, @Nullable InfoType requested)
    {
        List<ScopedRestriction> restrictions = EraRestrictions.restrictionsFor(player);
        boolean bypassing = EraRestrictions.isBypassing(player);
        if (restrictions.isEmpty())
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " has no era restrictions"), false);
        for (ScopedRestriction scoped : restrictions)
            source.sendSuccess(() -> Component.literal(scoped.toString()), false);
        if (bypassing)
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " is bypassing them").withStyle(ChatFormatting.YELLOW), false);
        InfoType type = requested;
        if (type == null && player.getMainHandItem().getItem() instanceof IFlanItem<?> held)
            type = held.getConfigType();
        if (type != null)
        {
            InfoType checked = type;
            EraVerdicts.Verdict verdict = EraRestrictions.check(player, checked);
            ServicePeriod period = IssueDates.periodOf(checked);
            String dates = period == null ? "undated" : period.toString();
            String outcome = verdict.allowed() ? "allowed" : "blocked by " + verdict.blockedBy();
            source.sendSuccess(() -> Component.literal(checked.getShortName() + " (" + dates + "): " + outcome).withStyle(verdict.allowed() ? ChatFormatting.GREEN : ChatFormatting.RED),
                false);
        }
        return 1;
    }

    private static int bypass(CommandSourceStack source, @Nullable Boolean requested) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        boolean bypass = requested == null ? !EraRestrictions.isBypassing(player) : requested;
        EraRestrictions.setBypassing(player, bypass);
        source.sendSuccess(() -> Component.literal(bypass ? "Era bypass on until you turn it off or log out" : "Era bypass off"), true);
        return 1;
    }

    /** Clears the bypass mark of the Flan's entity the operator rides or looks at, so strict despawning covers it again. */
    private static int untag(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        Entity target = player.getVehicle() instanceof Seat seat ? seat.getDriveable() : player.getVehicle();
        if (target == null || EraEntities.typeOf(target) == null)
        {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(UNTAG_REACH));
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1D),
                entity -> EraEntities.typeOf(entity) != null, UNTAG_REACH * UNTAG_REACH);
            target = hit == null ? null : hit.getEntity();
        }
        if (target == null)
        {
            source.sendFailure(Component.literal("Ride or look at a Flan's vehicle, AA gun or deployed gun within " + (int) UNTAG_REACH + " blocks"));
            return 0;
        }
        boolean tagged = EraEntities.isBypassTagged(target);
        target.getPersistentData().remove(EraEntities.BYPASS_TAG);
        Entity untagged = target;
        source.sendSuccess(() -> Component.literal(tagged ? untagged.getName().getString() + " is no longer kept by an operator bypass" : untagged.getName().getString() + " had no bypass mark"),
            true);
        if (tagged && EraEntities.shouldDespawn(untagged))
            EraEntities.sweep(source.getServer());
        return tagged ? 1 : 0;
    }

    private static int listEras(CommandSourceStack source)
    {
        for (Era era : IssueDates.eras().getEras())
        {
            String aliases = era.aliases().isEmpty() ? "" : " (" + String.join(", ", era.aliases()) + ")";
            source.sendSuccess(() -> Component.literal(era.id() + aliases + ": " + ServiceDate.formatMonth(era.first()) + " to " + ServiceDate.formatMonth(era.last())), false);
        }
        source.sendSuccess(() -> Component.literal(Era.FUTURE_ID + ": after every dated era").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int reload(CommandSourceStack source)
    {
        IssueDates.loadAll();
        EraRestrictions.datesChanged(source.getServer());
        EraSavedData data = EraSavedData.get(source.getServer());
        data.getServer().flatMap(restriction -> restriction.validate(IssueDates.eras()))
            .ifPresent(problem -> source.sendFailure(Component.literal("The server restriction is now ignored: " + problem)));
        data.getDimensions().forEach((dimension, restriction) -> restriction.validate(IssueDates.eras())
            .ifPresent(problem -> source.sendFailure(Component.literal("The " + dimension + " restriction is now ignored: " + problem))));
        source.sendSuccess(() -> Component.literal("Reloaded " + IssueDates.ERAS_FILE + " and " + IssueDates.OVERRIDES_FILE + "; see the log for any problems"), true);
        return 1;
    }
}
