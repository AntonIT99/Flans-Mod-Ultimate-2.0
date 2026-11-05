package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.teams.BotTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * How long the body of a fallen NPC that does not respawn stays after its death animation. Teams bots follow the Teams
 * {@code corpses} and {@code corpseLife} rules; any other NPC its own Keep corpse switch and Corpse time. Vehicles keep
 * their own wreck handling, and a hidden body is never kept. Server thread only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCorpses
{
    /** CustomNPCs' respawn cycles of NPCs that never come back, whose bodies vanilla removes after the death animation. */
    private static final int NEVER_RESPAWN = 3;
    private static final int DESPAWN = 4;

    /**
     * @return the seconds the body stays, 0 for as long as nothing removes it (for a bot, until its battle ends), or empty
     *         when it vanishes as usual
     */
    public static OptionalInt lifeSeconds(EntityNPCInterface npc)
    {
        if (npc.stats.spawnCycle != NEVER_RESPAWN && npc.stats.spawnCycle != DESPAWN || npc.stats.hideKilledBody || NpcPresentation.vehicleType(npc).isPresent())
            return OptionalInt.empty();
        Optional<BotTag.Data> bot = BotTag.read(npc);
        if (bot.isPresent())
            return bot.get().vehicle() || bot.get().enlisted() ? OptionalInt.empty() : FlansTeams.getBotCorpseLife();
        if (!((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.KEEP_CORPSE))
            return OptionalInt.empty();
        return OptionalInt.of(NpcTuning.of(npc).get(NpcTuning.Value.CORPSE_TIME));
    }
}
