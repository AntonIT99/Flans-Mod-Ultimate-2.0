package com.wolffsmod.npcs.presets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.data.Lines;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/** Spoken lines cleared by non-speaking presets, with snapshots for Undo and editor synchronization. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPresetLines
{
    private static List<Lines> groups(EntityNPCInterface npc)
    {
        var advanced = npc.advanced;
        return List.of(advanced.interactLines, advanced.worldLines, advanced.attackLines, advanced.killedLines, advanced.killLines, advanced.npcInteractLines);
    }

    public static boolean isEmpty(EntityNPCInterface npc)
    {
        return groups(npc).stream().allMatch(Lines::isEmpty);
    }

    public static void clear(EntityNPCInterface npc)
    {
        groups(npc).forEach(lines -> lines.lines.clear());
    }

    /** Stores only populated groups so Undo leaves later edits to unchanged groups alone. */
    public static CompoundTag capture(EntityNPCInterface npc)
    {
        CompoundTag saved = new CompoundTag();
        List<Lines> groups = groups(npc);
        for (int index = 0; index < groups.size(); index++)
            if (!groups.get(index).isEmpty())
                saved.put(Integer.toString(index), groups.get(index).save());
        return saved;
    }

    public static void restore(EntityNPCInterface npc, CompoundTag saved)
    {
        List<Lines> groups = groups(npc);
        for (int index = 0; index < groups.size(); index++)
        {
            String key = Integer.toString(index);
            if (saved.contains(key))
                groups.get(index).readNBT(saved.getCompound(key));
        }
    }
}
