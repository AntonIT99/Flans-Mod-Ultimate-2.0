package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.tactics.CustomNpcTactics;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataMelee;

/** Version boundary for the API differences between the Forge and NeoForge Custom NPCs builds. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomNpcsPlatform
{
    /** The editor's default label colour. */
    public static int defaultTextColor()
    {
        return CustomNpcResourceListener.DefaultTextColor;
    }

    /** A melee reach in the unit this build stores: whole blocks, rounded to the nearest. */
    public static float meleeRange(double reach)
    {
        return Math.round((float) reach);
    }

    public static void setMeleeRange(DataMelee melee, double reach)
    {
        melee.setRange(Math.round((float) reach));
    }

    public static void setDisplaySize(DataDisplay display, int size)
    {
        display.setSize(size);
    }

    /** The NPC's Custom NPCs tactical variant, which this build lacks and the module restores. */
    public static int tacticalVariant(EntityNPCInterface npc)
    {
        return CustomNpcTactics.of(npc).getVariant();
    }

    /** Changes the restored tactical variant using the same validated state as the AI editor and scripts. */
    public static void setTacticalVariant(EntityNPCInterface npc, int variant)
    {
        CustomNpcTactics.of(npc).setVariant(variant);
    }

    /** The distance setting of the NPC's Custom NPCs tactical variant. */
    public static int tacticalRange(EntityNPCInterface npc)
    {
        return CustomNpcTactics.of(npc).getRange();
    }

    /** Whether Custom NPCs holds every NPC still, the switch its remote calls Freeze NPCs. */
    public static boolean frozen()
    {
        return CustomNpcs.FreezeNPCs;
    }

    // Custom NPCs keeps the switch in a public static field, which its own remote writes too.
    @SuppressWarnings("java:S2696")
    public static void setFrozen(boolean frozen)
    {
        CustomNpcs.FreezeNPCs = frozen;
    }
}
