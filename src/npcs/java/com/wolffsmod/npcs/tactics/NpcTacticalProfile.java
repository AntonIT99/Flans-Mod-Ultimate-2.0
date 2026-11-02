package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

/** Behaviour-only presets. INHERIT keeps the NPC's individual settings; no combat attributes are changed. */
public enum NpcTacticalProfile
{
    INHERIT, BALANCED, CAUTIOUS, AGGRESSIVE;

    @Nullable
    public Boolean feature(Feature feature)
    {
        if (this == INHERIT)
            return null;
        return switch (feature)
        {
            case TACTICAL_COVER, AVOID_UNWINNABLE_FIGHTS, VEHICLE_EVASION, TACTICAL_WEAPON_SWITCH, ALLY_SPACING, ADAPTIVE_VEHICLE_COMBAT, DEFENSIVE_GUNNERS -> true;
            case ADAPTIVE_FLANKING -> this != CAUTIOUS;
            default -> null;
        };
    }

    @Nullable
    public Integer value(Value value)
    {
        if (this == INHERIT)
            return null;
        return switch (value)
        {
            case COMBAT_MOVEMENT -> NpcTuning.MovementFreedom.LOCAL.ordinal();
            case MANOEUVRE_RADIUS -> this == AGGRESSIVE ? 32 : 24;
            case WITHDRAW_DISTANCE -> switch (this)
            {
                case CAUTIOUS -> 24;
                case AGGRESSIVE -> 12;
                default -> 16;
            };
            case ALLY_GAP -> this == CAUTIOUS ? 4 : 3;
            default -> null;
        };
    }

    /** Applies a profile to the ordinary NPC settings used by its editor, clones and saves. Server or editor thread. */
    public void apply(EntityNPCInterface npc)
    {
        for (Feature feature : Feature.values())
        {
            Boolean enabled = feature(feature);
            if (enabled != null)
                ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(feature, enabled);
        }
        for (Value value : Value.values())
        {
            Integer number = value(value);
            if (number != null)
                NpcTuning.of(npc).set(value, number);
        }
    }
}
