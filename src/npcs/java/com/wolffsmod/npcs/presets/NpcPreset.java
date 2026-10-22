package com.wolffsmod.npcs.presets;

import com.wolffsmod.npcs.presets.NpcPresetSetting.Values;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.wolffsmod.npcs.presets.NpcPresetSetting.*;

/**
 * A set of Custom NPCs settings the Wolff's Mod tab applies in one go. A preset only sets what it lists; everything else
 * the NPC has stays as it is. Packets hold the name, so presets may be added anywhere.
 */
public enum NpcPreset
{
    /** Roams freely like a vanilla mob, dies for good and leaves no body. */
    MOB(List.of(), new Target(MOVING_TYPE, Values.WANDERING), new Target(WALKING_RANGE, 0), new Target(RETURN_TO_START, Values.NO), new Target(NPC_INTERACTING, Values.NO),
        new Target(RESPAWN, Values.RESPAWN_NO), new Target(HIDE_BODY, Values.YES)),
    /** Everything the mob preset sets, and moves like a player: opens doors and sprints. */
    PLAYER(MOB.targets, new Target(DOOR_INTERACT, Values.DOORS_OPEN), new Target(CAN_SPRINT, Values.YES));

    /** What the preset sets, in the order its tooltip lists it: those of the preset it builds on first. */
    @Getter
    private final List<Target> targets;

    NpcPreset(List<Target> base, Target... own)
    {
        List<Target> all = new ArrayList<>(base);
        Collections.addAll(all, own);
        targets = List.copyOf(all);
    }

    /** @return whether the NPC already has every setting of the preset */
    public boolean matches(EntityNPCInterface npc)
    {
        return targets.stream().allMatch(target -> target.setting().get(npc) == target.value());
    }

    /** @return whether the preset sets this setting */
    public boolean sets(NpcPresetSetting setting)
    {
        return targets.stream().anyMatch(target -> target.setting() == setting);
    }

    /** Gives the NPC every setting of the preset. Server thread. */
    public void apply(EntityNPCInterface npc)
    {
        for (Target target : targets)
            target.setting().set(npc, target.value());
        npc.updateAI = true;
        npc.updateClient = true;
    }

    public String key()
    {
        return "wolffsmodnpcs.preset." + name().toLowerCase(Locale.ROOT);
    }

    /** One setting of a preset and the value it is given. */
    public record Target(NpcPresetSetting setting, int value)
    {}
}
