package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.teams.BotTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * The start signal an NPC set to wait for it holds where it was placed until: the entity tag {@link #TAG}, given with the
 * vanilla tag command, so that a command block on a button or a timer sends a whole line at once. Taking the tag away
 * sends it back to wait. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSignal
{
    /** The entity tag that gives the signal. */
    public static final String TAG = "fmu_go";

    /** @return whether the NPC waits for the signal and has not had it yet; Teams bots follow their squad's orders instead */
    public static boolean waiting(EntityNPCInterface npc)
    {
        return NpcEquipment.enabled(npc, Feature.WAIT_FOR_SIGNAL) && !npc.getTags().contains(TAG) && !BotTag.isBot(npc);
    }
}
