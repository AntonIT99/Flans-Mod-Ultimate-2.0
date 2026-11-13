package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.eras.EraScopeRestriction;
import com.flansmodultimate.api.eras.FlansEras;
import com.flansmodultimate.api.eras.IEraContext;
import com.flansmodultimate.api.eras.IEraScope;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The era windows of skirmishes and wars, whose engagements inherit the war's. A live skirmish's window covers its bots
 * and the players who are members of a faction fighting it. Server thread.
 */
public final class BattleEras implements IEraScope
{
    private BattleEras()
    {}

    public static void register()
    {
        FlansEras.addScope(new BattleEras());
    }

    @Override
    public List<EraScopeRestriction> restrictionsFor(Entity subject)
    {
        MinecraftServer server = subject.getServer();
        if (server == null)
            return List.of();
        SkirmishStore store = SkirmishStore.get(server);
        Optional<BotTag.Data> bot = BotTag.read(subject);
        if (bot.isPresent())
            return windows(store.all().stream().filter(skirmish -> skirmish.isLive() && skirmish.getId().equals(bot.get().instance())));
        if (!(subject instanceof Player player))
            return List.of();
        Optional<Integer> faction = FactionMembers.get(server).factionOf(player.getUUID());
        if (faction.isEmpty())
            return List.of();
        return windows(store.all().stream().filter(skirmish -> skirmish.isLive() && skirmish.sideOf(faction.get()) >= 0));
    }

    /** What a bot about to spawn for an order may be given: its level's, its round's and its battle's restrictions. */
    static IEraContext contextFor(ServerLevel level, BotFactory.Order order)
    {
        IEraContext context = FlansEras.contextAt(level, order.combatant());
        for (Skirmish skirmish : SkirmishStore.get(level.getServer()).all())
            if (skirmish.getId().equals(order.instance()) && !skirmish.getEraWindow().isBlank())
                context = context.with(skirmish.getName(), skirmish.getEraWindow());
        return context;
    }

    /**
     * The class a bot plays: the one asked for when allowed, otherwise another allowed class of the team, preferring one
     * whose name shares the asked class's role.
     */
    @Nullable
    static IPlayerClass allowedClass(IEraContext context, ITeam team, @Nullable IPlayerClass asked)
    {
        if (asked == null || context.allowsClass(asked))
            return asked;
        List<? extends IPlayerClass> allowed = team.getClasses().stream().filter(context::allowsClass).toList();
        return allowed.isEmpty() ? null : allowed.get(0);
    }

    static boolean allows(IEraContext context, @Nullable IContentType type)
    {
        return type == null || context.allows(type);
    }

    /** Why a skirmish may not start: a team serving it none of whose classes fits its era window, or empty. */
    static Optional<String> teamWithoutClass(ServerLevel level, Skirmish skirmish, List<? extends ITeam> teams)
    {
        IEraContext context = FlansEras.contextAt(level, false);
        if (!skirmish.getEraWindow().isBlank())
            context = context.with(skirmish.getName(), skirmish.getEraWindow());
        IEraContext checked = context;
        for (ITeam team : teams)
            if (!team.getClasses().isEmpty() && team.getClasses().stream().noneMatch(checked::allowsClass))
                return Optional.of(team.getName());
        return Optional.empty();
    }

    private static List<EraScopeRestriction> windows(Stream<Skirmish> skirmishes)
    {
        return skirmishes.filter(skirmish -> !skirmish.getEraWindow().isBlank()).map(skirmish -> new EraScopeRestriction(skirmish.getName(), skirmish.getEraWindow())).toList();
    }
}
