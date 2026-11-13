package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.common.command.FlansModCommandAliases;
import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.EraSection;
import com.flansmodultimate.common.teams.RoundRule;
import com.flansmodultimate.common.teams.RoundRules;
import com.flansmodultimate.common.teams.centre.CommandLines;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The places an operator can give an era window from the command centre: the server, each dimension, the round-rule
 * defaults and each round of the rotation. Read from the era and Teams sections; client thread.
 */
final class EraTargets
{
    private static final String ERA = FlansModCommandAliases.ROOT + " era ";

    private EraTargets()
    {}

    /**
     * One place a window can be given.
     *
     * @param world
     *            whether it is the server or a dimension, which alone may despawn what they block
     */
    record Target(String id, Component name, String restriction, boolean world, String setPrefix, String clearCommand)
    {
        /** The window as written, or null for none or one that cannot be read. */
        @Nullable
        EraRestriction parsed()
        {
            if (restriction.isBlank())
                return null;
            try
            {
                return EraRestriction.parse(restriction);
            }
            catch (IllegalArgumentException exception)
            {
                return null;
            }
        }

        /** The command that gives this place the window. */
        String setCommand(String window)
        {
            return world ? setPrefix + " " + window : setPrefix + " " + CommandLines.quote(window);
        }
    }

    static List<Target> all()
    {
        List<Target> targets = new ArrayList<>();
        CompoundTag eras = CommandCentreClient.sections().getCompound(EraSection.KEY.toString());
        targets.add(new Target("server", TeamsPage.tr("eras.server"), eras.getString(EraSection.SERVER), true, ERA + "server set", ERA + "server clear"));
        CompoundTag dimensions = eras.getCompound(EraSection.DIMENSIONS);
        for (Tag level : eras.getList(EraSection.LEVELS, Tag.TAG_STRING))
        {
            String dimension = level.getAsString();
            targets.add(new Target("dimension:" + dimension, TeamsPage.tr("eras.dimension", dimension), dimensions.getString(dimension), true, ERA + "dimension " + dimension + " set",
                ERA + "dimension " + dimension + " clear"));
        }
        TeamsData data = TeamsPage.data();
        RoundRules defaults = RoundRules.load(data.rules().getCompound("roundRuleDefaults"));
        targets.add(new Target("round:defaults", TeamsPage.tr("eras.round_defaults"), defaults.get(RoundRule.ERA_WINDOW), false, roundPrefix("defaults"),
            roundPrefix("defaults") + " " + CommandLines.quote("")));
        for (TeamsData.Round round : data.rounds())
        {
            RoundRules overrides = RoundRules.load(data.rules().getCompound("roundRuleOverrides").getCompound(round.id()));
            String window = overrides.resolve(defaults).get(RoundRule.ERA_WINDOW);
            String prefix = roundPrefix(Integer.toString(round.index()));
            targets.add(new Target("round:" + round.id(), TeamsPage.tr("eras.round", round.index(), data.title(round)), window, false, prefix, prefix + " inherit"));
        }
        return targets;
    }

    static Optional<Target> byId(List<Target> targets, String id)
    {
        return targets.stream().filter(target -> target.id().equals(id)).findFirst();
    }

    private static String roundPrefix(String destination)
    {
        return CommandLines.line("teams admin roundrule", destination, RoundRule.ERA_WINDOW.getKey());
    }
}
