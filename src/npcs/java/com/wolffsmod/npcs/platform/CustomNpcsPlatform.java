package com.wolffsmod.npcs.platform;

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
        return CustomNpcResourceListener.getDefaultTextColor();
    }

    /** A melee reach in the unit this build stores, fractional blocks. */
    public static float meleeRange(double reach)
    {
        return (float) reach;
    }

    public static void setMeleeRange(DataMelee melee, double reach)
    {
        melee.setRange(meleeRange(reach));
    }

    public static void setDisplaySize(DataDisplay display, int size)
    {
        display.setSize(size);
    }

    /** The NPC's Custom NPCs tactical variant, which this build stores and runs itself. */
    public static int tacticalVariant(EntityNPCInterface npc)
    {
        return npc.ais.tacticalVariant;
    }

    /** The distance setting of the NPC's Custom NPCs tactical variant. */
    public static int tacticalRange(EntityNPCInterface npc)
    {
        return npc.ais.getTacticalRange();
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
