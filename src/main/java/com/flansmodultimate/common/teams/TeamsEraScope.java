package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.EraRestrictions;
import com.flansmodultimate.common.eras.EraScope;
import com.flansmodultimate.common.eras.EraScopeProvider;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.ScopedRestriction;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.util.FlansLog;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The era restriction of the running Teams round, from its {@link RoundRule#ERA_WINDOW} rule. It covers the round's
 * participants, players on a team or choosing one, and the vehicles and items the round's spawners create. Also checks
 * before a round starts that every one of its teams has a class to play. Server thread.
 */
public final class TeamsEraScope implements EraScopeProvider
{
    private static final AtomicReference<Parsed> parsed = new AtomicReference<>(new Parsed("", null));

    private record Parsed(String text, @Nullable EraRestriction restriction)
    {}

    @Override
    public void collect(Entity subject, Consumer<ScopedRestriction> restrictions)
    {
        TeamsManager manager = TeamsManager.getInstance();
        ScopedRestriction current = current(manager);
        if (current != null && isParticipant(subject))
            restrictions.accept(current);
    }

    @Override
    public void collectAt(ServerLevel level, Consumer<ScopedRestriction> restrictions)
    {
        TeamsManager manager = TeamsManager.getInstance();
        ScopedRestriction current = current(manager);
        if (current != null && manager.getCurrentLevel() == level)
            restrictions.accept(current);
    }

    /** The Flan's types of the class the player may not use; empty when they may take it. */
    public static List<InfoType> blockedTypes(ServerPlayer player, PlayerClass playerClass)
    {
        return blockedTypes(playerClass, type -> EraRestrictions.isAllowed(player, type));
    }

    public static List<InfoType> blockedTypes(PlayerClass playerClass, Predicate<InfoType> allowed)
    {
        return playerClass.getGrantedTypes().stream().filter(allowed.negate()).toList();
    }

    /** The blocked types by name, for a refusal or a tooltip. */
    public static Component names(List<InfoType> types)
    {
        return ComponentUtils.formatList(types, type -> Component.translatable(type.getNameTranslationKey()));
    }

    /**
     * Why the round cannot start under its era window, the server's and its map dimension's: a team outside the window,
     * or a team none of whose classes can be played. Empty when it can start.
     */
    public static Optional<String> startProblem(TeamsManager manager, TeamsRound round, RoundRules defaults)
    {
        List<ScopedRestriction> restrictions = new ArrayList<>();
        ServerLevel level = manager.getMap(round.getMapId()).map(map -> manager.getServer().getLevel(map.getDimension())).orElse(manager.getServer().overworld());
        if (level != null)
            restrictions.addAll(EraRestrictions.worldRestrictionsAt(level));
        EraRestriction window = parse(round.getRules().resolve(defaults).get(RoundRule.ERA_WINDOW));
        if (window != null)
            restrictions.add(new ScopedRestriction(new EraScope(EraScope.Kind.ROUND, round.getMapId()), window));
        if (restrictions.isEmpty())
            return Optional.empty();
        EraVerdicts verdicts = EraRestrictions.verdicts();
        for (String teamId : round.getTeamIds())
        {
            Team team = Team.getTeam(teamId);
            if (team == null)
                continue;
            if (!verdicts.check(restrictions, team).allowed())
                return Optional.of("Team " + team.getName() + " is outside the era window");
            List<PlayerClass> classes = team.getClasses();
            if (!classes.isEmpty() && classes.stream().noneMatch(playerClass -> blockedTypes(playerClass, type -> verdicts.check(restrictions, type).allowed()).isEmpty()))
                return Optional.of("Team " + team.getName() + " has no class whose equipment fits the era window");
        }
        return Optional.empty();
    }

    /** The restriction a rule value describes, or {@code null} for none or one that cannot be read. */
    @Nullable
    public static EraRestriction parse(String text)
    {
        Parsed last = parsed.get();
        if (last.text().equals(text))
            return last.restriction();
        EraRestriction restriction = null;
        if (!text.isBlank())
        {
            try
            {
                restriction = EraRestriction.parse(text);
            }
            catch (IllegalArgumentException exception)
            {
                FlansLog.log.warn("Ignoring the unreadable round era window '{}': {}", text, exception.getMessage());
            }
        }
        parsed.set(new Parsed(text, restriction));
        return restriction;
    }

    @Nullable
    private static ScopedRestriction current(@Nullable TeamsManager manager)
    {
        if (manager == null || !manager.isRoundRunning())
            return null;
        EraRestriction restriction = parse(manager.getRoundRuleRuntime().rules().get(RoundRule.ERA_WINDOW));
        if (restriction == null)
            return null;
        String key = manager.getCurrentRound().map(TeamsRound::getMapId).orElse("");
        return new ScopedRestriction(new EraScope(EraScope.Kind.ROUND, key), restriction);
    }

    /** A player on a team or choosing one, a combatant such as a round bot, or an entity a Teams spawner created. */
    private static boolean isParticipant(Entity subject)
    {
        if (subject instanceof ServerPlayer player)
        {
            PlayerData data = PlayerData.getInstance(player);
            return isPlaying(data.getTeam()) || isPlaying(data.getNewTeam());
        }
        return subject.getPersistentData().hasUUID(SpawnerVehicles.SPAWNER) || isPlaying(TeamsCombatants.getPlayingTeam(TeamsManager.getInstance(), subject));
    }

    /** The running round's restriction, for what its combatants are given before they exist. */
    public static Optional<ScopedRestriction> currentRestriction()
    {
        return Optional.ofNullable(current(TeamsManager.getInstance()));
    }

    private static boolean isPlaying(@Nullable Team team)
    {
        return team != null && team != Team.SPECTATORS;
    }
}
