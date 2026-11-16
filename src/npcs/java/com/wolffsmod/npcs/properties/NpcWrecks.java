package com.wolffsmod.npcs.properties;

import com.wolffsmod.npcs.teams.BotTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

/**
 * Retained wrecks: the visible body an NPC with Retain wreck leaves when it dies. One that never respawns stays where it
 * fell; one that respawns leaves an inert copy of its body behind as it comes back. Retain wreck and Hide body exclude
 * each other: switching either on switches the other off. Teams bots follow their round's own wreck rules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWrecks
{
    /** CustomNPCs' respawn cycles of NPCs that never come back. */
    private static final int NEVER_RESPAWN = 3;
    private static final int DESPAWN = 4;

    /** @return whether the NPC's own body stays as its wreck after death instead of being removed */
    public static boolean retainsBody(EntityNPCInterface npc)
    {
        return npc.stats.spawnCycle == NEVER_RESPAWN && keeps(npc);
    }

    public static void setKeepWreck(EntityNPCInterface npc, boolean keep)
    {
        NpcPresentation.of(npc).setKeepWreck(keep);
        if (keep)
            npc.stats.hideKilledBody = false;
    }

    public static void setHideBody(EntityNPCInterface npc, boolean hide)
    {
        npc.stats.hideKilledBody = hide;
        if (hide)
            NpcPresentation.of(npc).setKeepWreck(false);
    }

    /**
     * Leaves the wreck of a respawning NPC as it comes back: a copy of its dead body that never respawns, stays where the
     * NPC died and runs no scripts. Called before Custom NPCs revives the NPC. Server side.
     */
    public static void leaveOnRespawn(EntityNPCInterface npc)
    {
        if (!(npc.level() instanceof ServerLevel level) || !npc.hasDied || npc.stats.spawnCycle == NEVER_RESPAWN || npc.stats.spawnCycle == DESPAWN || !keeps(npc)
            || BotTag.read(npc).isPresent())
            return;
        CompoundTag tag = npc.saveWithoutId(new CompoundTag());
        if (!(npc.getType().create(level) instanceof EntityNPCInterface wreck))
            return;
        wreck.load(tag);
        wreck.setUUID(Mth.createInsecureUUID(level.random));
        // An unlinked copy, or the linked template would bring its respawning back.
        wreck.linkedName = "";
        wreck.stats.spawnCycle = NEVER_RESPAWN;
        wreck.script.setEnabled(false);
        wreck.setHealth(0F);
        level.addFreshEntity(wreck);
    }

    private static boolean keeps(EntityNPCInterface npc)
    {
        return !npc.stats.hideKilledBody && NpcPresentation.of(npc).isKeepWreck();
    }
}
