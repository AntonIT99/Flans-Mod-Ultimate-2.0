package com.wolffsmod.npcs.presets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.List;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * A Custom NPCs setting a preset can set, read and written as a number: a switch is 0 for No and 1 for Yes, a choice is
 * its index on the Custom NPCs screen and a range is itself. Saves and packets hold the name, so settings may be added
 * anywhere.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum NpcPresetSetting
{
    /** AI > Movement > Moving type: Standing, Wandering or Moving Path. */
    MOVING_TYPE(npc -> npc.ais.getMovingType(), (npc, value) -> npc.ais.setMovingType(value), List.of("ai.standing", "ai.wandering", "ai.movingpath")),
    /**
     * AI > Movement > Walking range. Custom NPCs' scripting setter refuses 0, which its editor accepts and its wander AI
     * reads as no limit around the start position, so the field is written directly.
     */
    WALKING_RANGE(npc -> npc.ais.walkingRange, (npc, value) -> npc.ais.walkingRange = Math.max(0, value), List.of()),
    /** AI > Return to start. */
    RETURN_TO_START(npc -> yes(npc.ais.returnToStart), (npc, value) -> npc.ais.returnToStart = value != 0, Values.NO_YES),
    /** AI > Movement > Interact with NPCs. */
    NPC_INTERACTING(npc -> yes(npc.ais.npcInteracting), (npc, value) -> npc.ais.npcInteracting = value != 0, Values.NO_YES),
    /** Stats > Respawn: Yes, Day, Night, No or Naturally. */
    RESPAWN(npc -> npc.stats.spawnCycle, (npc, value) -> npc.stats.spawnCycle = value, List.of("gui.yes", "gui.day", "gui.night", "gui.no", "stats.naturally")),
    /** Stats > Respawn > Hide body when killed. */
    HIDE_BODY(npc -> yes(npc.stats.hideKilledBody), (npc, value) -> npc.stats.hideKilledBody = value != 0, Values.NO_YES),
    /** AI > Door interact: Break, Open or Disabled. */
    DOOR_INTERACT(npc -> npc.ais.doorInteract, (npc, value) -> npc.ais.doorInteract = value, List.of("gui.break", "gui.open", "gui.disabled")),
    /** AI > Can sprint. */
    CAN_SPRINT(npc -> yes(npc.ais.canSprint), (npc, value) -> npc.ais.canSprint = value != 0, Values.NO_YES);

    private final ToIntFunction<EntityNPCInterface> getter;
    private final ObjIntConsumer<EntityNPCInterface> setter;
    /** The Custom NPCs translation key of each value, by number; empty for a setting whose value is shown as a number. */
    @Getter
    private final List<String> valueKeys;

    public int get(EntityNPCInterface npc)
    {
        return getter.applyAsInt(npc);
    }

    public void set(EntityNPCInterface npc, int value)
    {
        setter.accept(npc, value);
    }

    /** @return whether the number is one this setting can hold */
    public boolean accepts(int value)
    {
        return valueKeys.isEmpty() ? value >= 0 : value >= 0 && value < valueKeys.size();
    }

    public String key()
    {
        return "wolffsmodnpcs.preset.setting." + name().toLowerCase(Locale.ROOT);
    }

    private static int yes(boolean value)
    {
        return value ? Values.YES : Values.NO;
    }

    /** The numbers of the values presets use, named after what they mean on the Custom NPCs screens. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Values
    {
        public static final int NO = 0;
        public static final int YES = 1;
        public static final int WANDERING = 1;
        public static final int RESPAWN_NO = 3;
        public static final int DOORS_OPEN = 1;
        private static final List<String> NO_YES = List.of("gui.no", "gui.yes");
    }
}
