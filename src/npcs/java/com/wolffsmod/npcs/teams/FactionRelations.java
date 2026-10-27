package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * How Custom NPCs factions stand toward each other, outside the hidden factions of Teams rounds: enemies attack each
 * other, allies share driveables and keep each other out of their fire, and neutral factions do neither. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FactionRelations
{
    public enum Relation
    {
        NEUTRAL, ALLIED, HOSTILE
    }

    /** @return the factions a builder may relate, by name */
    public static List<Faction> editable()
    {
        return FactionController.instance.factions.values().stream().filter(faction -> !BotFactions.isRoundFaction(faction.name))
            .sorted(Comparator.comparing((Faction faction) -> faction.name, String.CASE_INSENSITIVE_ORDER).thenComparingInt(faction -> faction.id)).toList();
    }

    /** Sets every pair of the given factions to the relation and saves them. Round factions are left alone. */
    public static void set(List<Faction> factions, Relation relation)
    {
        List<Faction> chosen = factions.stream().filter(faction -> !BotFactions.isRoundFaction(faction.name)).distinct().toList();
        for (Faction faction : chosen)
        {
            for (Faction other : chosen)
            {
                if (other.id == faction.id)
                    continue;
                if (relation == Relation.HOSTILE)
                    faction.attackFactions.add(other.id);
                else
                    faction.attackFactions.remove(other.id);
                if (relation == Relation.ALLIED)
                    FactionAllies.of(faction).add(other.id);
                else
                    FactionAllies.of(faction).remove(other.id);
            }
            FactionController.instance.saveFaction(faction);
        }
    }

    /** Whether either faction has the other as an ally and neither attacks the other. */
    public static boolean allied(@Nullable Faction faction, @Nullable Faction other)
    {
        if (faction == null || other == null || faction.id == other.id || faction.hostileToFaction(other.id) || other.hostileToFaction(faction.id))
            return false;
        return FactionAllies.of(faction).contains(other.id) || FactionAllies.of(other).contains(faction.id);
    }
}
