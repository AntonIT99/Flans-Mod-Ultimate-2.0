package com.wolffsmod.npcs.sim;

import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Shows or hides the name tag of every NPC at once, from the Server pages of the Wolff's Mod tab. */
public final class NpcNameTags
{
    /** Custom NPCs' name tag settings: shown, and hidden. */
    private static final int SHOW_NAME = 0;
    private static final int HIDE_NAME = 1;

    private NpcNameTags()
    {}

    /**
     * Shows or hides the name tag of every NPC loaded in any level; NPCs that are not loaded keep theirs.
     *
     * @return how many NPCs changed
     */
    public static int setAll(MinecraftServer server, boolean shown)
    {
        int wanted = shown ? SHOW_NAME : HIDE_NAME;
        int changed = 0;
        for (ServerLevel level : server.getAllLevels())
        {
            for (Entity entity : level.getAllEntities())
            {
                if (entity instanceof EntityNPCInterface npc && npc.display.getShowName() != wanted)
                {
                    npc.display.setShowName(wanted);
                    npc.updateClient = true;
                    changed++;
                }
            }
        }
        return changed;
    }
}
