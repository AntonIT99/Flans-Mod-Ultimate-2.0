package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.model.FlanModelKind;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

/** The kinds of NPC that move differently enough in a fight to need tactics of their own. Server side. */
public enum TacticKind
{
    /** An NPC that is not drawn as a content-pack model and does not fly as an aircraft. */
    STANDARD, AA_GUN, VEHICLE, MECHA,
    /** An NPC that flies as a plane, or a plane model that does not fly as a helicopter. */
    PLANE,
    /** An NPC that flies as a helicopter. */
    HELICOPTER;

    public static TacticKind of(EntityNPCInterface npc)
    {
        return of(NpcVehicleState.of(npc).kind(), NpcFlight.type(npc));
    }

    /** How an NPC flies decides first: a soldier flying as a plane fights as one. */
    public static TacticKind of(@Nullable FlanModelKind model, NpcTuning.Flight flight)
    {
        if (flight == NpcTuning.Flight.PLANE)
            return PLANE;
        if (flight == NpcTuning.Flight.HELICOPTER)
            return HELICOPTER;
        if (model == null)
            return STANDARD;
        return switch (model)
        {
            case AA_GUN -> AA_GUN;
            case VEHICLE -> VEHICLE;
            case MECHA -> MECHA;
            case PLANE -> PLANE;
        };
    }
}
