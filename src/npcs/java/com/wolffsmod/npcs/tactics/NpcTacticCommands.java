package com.wolffsmod.npcs.tactics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;

/**
 * {@code /flansnpctactic}: shows and sets the tactic NPCs fight with, as the Wolff's Mod tab does for one NPC. Operators only.
 * <ul>
 * <li>{@code /flansnpctactic near <radius> [faction <faction>]} lists the tactics of the NPCs within the radius.</li>
 * <li>{@code /flansnpctactic near <radius> [faction <faction>] <tactic> [distance]} sets theirs.</li>
 * <li>{@code /flansnpctactic select <targets> [<tactic> [distance]]} does the same for the NPCs a selector picks.</li>
 * </ul>
 * {@code reset} goes back to Custom NPCs' own tactical variant. An NPC the tactic is not written for keeps its own.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcTacticCommands
{
    public static final int MAX_RADIUS = 256;
    private static final int MAX_DISTANCE = 256;
    private static final String KEY = "wolffsmodnpcs.tactic_command.";
    private static final String RADIUS = "radius";
    private static final String FACTION = "faction";
    private static final String TARGETS = "targets";
    private static final String DISTANCE = "distance";
    private static final SimpleCommandExceptionType NONE = new SimpleCommandExceptionType(Component.translatable(KEY + "none"));
    private static final SimpleCommandExceptionType NO_FACTION = new SimpleCommandExceptionType(Component.translatable(KEY + "unknown_faction"));

    /** How the NPCs a command acts on are found. */
    @FunctionalInterface
    private interface Selection
    {
        List<EntityNPCInterface> find(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    /** How many NPCs took a tactic, and how many it is not written for. */
    record Outcome(int changed, int skipped)
    {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        var radius = Commands.argument(RADIUS, IntegerArgumentType.integer(1, MAX_RADIUS));
        Selection near = context -> near(context, null);
        Selection nearFaction = context -> near(context, faction(context));
        var faction = Commands.argument(FACTION, StringArgumentType.string())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionController.instance.factions.values().stream().map(found -> StringArgumentType.escapeIfRequired(found.name)), builder));
        radius.then(Commands.literal(FACTION).then(withTactics(faction, nearFaction)));
        Selection selected = context -> EntityArgument.getEntities(context, TARGETS).stream().filter(EntityNPCInterface.class::isInstance)
            .map(EntityNPCInterface.class::cast).toList();
        dispatcher.register(Commands.literal("flansnpctactic").requires(source -> source.hasPermission(2)).then(Commands.literal("near").then(withTactics(radius, near)))
            .then(Commands.literal("select").then(withTactics(Commands.argument(TARGETS, EntityArgument.entities()), selected))));
    }

    /** Adds the status report and a literal for each tactic, with its optional distance, to the node that ends a selection. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTactics(T node, Selection selection)
    {
        node.executes(context -> status(context, selection));
        for (Tactic tactic : Tactic.values())
            node.then(tactic(word(tactic), tactic, selection));
        return node.then(tactic("reset", Tactic.CUSTOM_NPCS, selection));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> tactic(String word, Tactic tactic, Selection selection)
    {
        var literal = Commands.literal(word).executes(context -> set(context, selection, tactic, null));
        if (tactic.usesDistance())
            literal.then(Commands.argument(DISTANCE, IntegerArgumentType.integer(0, MAX_DISTANCE)).executes(context -> set(context, selection, tactic, IntegerArgumentType.getInteger(context, DISTANCE))));
        return literal;
    }

    /** @return the tactic's name as the command spells it */
    static String word(Tactic tactic)
    {
        return tactic.name().toLowerCase(Locale.ROOT);
    }

    private static List<EntityNPCInterface> near(CommandContext<CommandSourceStack> context, @Nullable Faction faction)
    {
        int radius = IntegerArgumentType.getInteger(context, RADIUS);
        Vec3 centre = context.getSource().getPosition();
        double reach = (double) radius * radius;
        return context.getSource().getLevel().getEntitiesOfClass(EntityNPCInterface.class, new AABB(centre, centre).inflate(radius),
            npc -> npc.isAlive() && npc.distanceToSqr(centre) <= reach && (faction == null || npc.faction != null && npc.faction.id == faction.id));
    }

    private static Faction faction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, FACTION));
        if (faction == null)
            throw NO_FACTION.create();
        return faction;
    }

    private static int status(CommandContext<CommandSourceStack> context, Selection selection) throws CommandSyntaxException
    {
        List<EntityNPCInterface> npcs = found(context, selection);
        Map<Tactic, Integer> counts = new EnumMap<>(Tactic.class);
        npcs.forEach(npc -> counts.merge(NpcTuning.of(npc).tactic(), 1, Integer::sum));
        MutableComponent list = Component.empty();
        counts.forEach((tactic, count) ->
        {
            if (!list.getSiblings().isEmpty())
                list.append(", ");
            list.append(Component.translatable(KEY + "entry", count, name(tactic)));
        });
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "status", npcs.size(), list), false);
        return npcs.size();
    }

    private static int set(CommandContext<CommandSourceStack> context, Selection selection, Tactic tactic, @Nullable Integer distance) throws CommandSyntaxException
    {
        Outcome outcome = apply(found(context, selection), tactic, distance, TacticKind::of);
        Component name = name(tactic);
        if (outcome.changed() > 0)
        {
            Component message = distance == null ? Component.translatable(KEY + "set", outcome.changed(), name)
                : Component.translatable(KEY + "set_distance", outcome.changed(), name, distance);
            context.getSource().sendSuccess(() -> message, true);
        }
        if (outcome.skipped() > 0)
            context.getSource().sendFailure(Component.translatable(KEY + "skipped", outcome.skipped(), name));
        return outcome.changed();
    }

    private static List<EntityNPCInterface> found(CommandContext<CommandSourceStack> context, Selection selection) throws CommandSyntaxException
    {
        List<EntityNPCInterface> npcs = selection.find(context);
        if (npcs.isEmpty())
            throw NONE.create();
        return npcs;
    }

    /**
     * Gives the NPCs the tactic it is written for, and the distance when one is given; the others keep their own.
     *
     * @param distance
     *            blocks the tactic keeps from the target, 0 for a share of the firing range, or null to keep the NPC's
     */
    static Outcome apply(Collection<? extends EntityNPCInterface> npcs, Tactic tactic, @Nullable Integer distance,
        Function<EntityNPCInterface, TacticKind> kinds)
    {
        int changed = 0;
        int skipped = 0;
        for (EntityNPCInterface npc : npcs)
        {
            if (!tactic.appliesTo(kinds.apply(npc)))
            {
                skipped++;
                continue;
            }
            NpcTuning tuning = NpcTuning.of(npc);
            tuning.set(Value.TACTIC, tactic.ordinal());
            if (distance != null)
                tuning.set(Value.TACTIC_DISTANCE, distance);
            changed++;
        }
        return new Outcome(changed, skipped);
    }

    private static Component name(Tactic tactic)
    {
        return Component.translatable("wolffsmodnpcs.tuning.tactic." + word(tactic));
    }

}
