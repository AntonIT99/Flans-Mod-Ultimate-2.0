package com.wolffsmod.npcs.presets;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;

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
    CAN_SPRINT(npc -> yes(npc.ais.canSprint), (npc, value) -> npc.ais.canSprint = value != 0, Values.NO_YES),
    /** Movement. */
    MOVEMENT(npc -> npc.ais.movementType, (npc, value) -> npc.ais.movementType = value,
        List.of("wolffsmodnpcs.preset.value.ground", "wolffsmodnpcs.preset.value.flying", "wolffsmodnpcs.preset.value.swimming")),
    /** On attack. */
    ON_ATTACK(npc -> npc.ais.onAttack, (npc, value) -> npc.ais.onAttack = value,
        List.of("wolffsmodnpcs.preset.value.retaliate", "wolffsmodnpcs.preset.value.flee", "wolffsmodnpcs.preset.value.avoid", "wolffsmodnpcs.preset.value.nothing")),
    /** Tactical variant. */
    TACTICAL_VARIANT(npc -> npc.ais.tacticalVariant, (npc, value) -> npc.ais.tacticalVariant = value, List.of()),
    /** Terrain. */
    TERRAIN(Value.TERRAIN),
    /** Flight type. */
    FLIGHT_TYPE(Value.FLIGHT_TYPE),
    /** Tactic. */
    TACTIC(Value.TACTIC),
    /** Submarine depth. */
    SUBMARINE_DEPTH(Value.SUBMARINE_DEPTH),
    /** Land speed. */
    LAND_SPEED(Value.LAND_SPEED),
    /** Water speed. */
    WATER_SPEED(Value.WATER_SPEED),
    /** Consume ammo. */
    CONSUME_AMMO(Feature.CONSUME_AMMO),
    /** Item weapons. */
    ITEM_WEAPONS(Feature.ITEM_WEAPONS),
    /** Model weapon stats. */
    MODEL_WEAPON_STATS(Feature.MODEL_WEAPON_STATS),
    /** Model sounds. */
    MODEL_SOUNDS(Feature.MODEL_SOUNDS),
    /** Driving. */
    DRIVING(Feature.DRIVING),
    /** Drive driveables. */
    DRIVE_DRIVEABLES(Feature.DRIVE_DRIVEABLES),
    /** Hurt flash. */
    HURT_FLASH(npc -> yes(NpcPresentation.of(npc).isHurtFlash()), (npc, value) -> NpcPresentation.of(npc).setHurtFlash(value != 0), Values.NO_YES),
    /** Death rotation. */
    DEATH_ROTATION(npc -> yes(NpcPresentation.of(npc).isDeathRotation()), (npc, value) -> NpcPresentation.of(npc).setDeathRotation(value != 0), Values.NO_YES),
    /** Keep wreck. */
    KEEP_WRECK(npc -> yes(NpcPresentation.of(npc).isKeepWreck()), (npc, value) -> NpcPresentation.of(npc).setKeepWreck(value != 0), Values.NO_YES),
    /** Can drown. */
    CAN_DROWN(NpcTypeProperty.CAN_DROWN),
    /** No fall damage. */
    NO_FALL_DAMAGE(NpcTypeProperty.NO_FALL_DAMAGE),
    /** Potion immune. */
    POTION_IMMUNE(NpcTypeProperty.POTION_IMMUNE),
    /** Ignore cobweb. */
    IGNORE_COBWEB(NpcTypeProperty.IGNORE_COBWEB),
    /** Health regen. */
    HEALTH_REGEN(NpcTypeProperty.HEALTH_REGEN),
    /** Combat regen. */
    COMBAT_REGEN(NpcTypeProperty.COMBAT_REGEN),
    /** Engine idle. */
    ENGINE_IDLE(npc -> yes(NpcPresentation.of(npc).getVehicleSounds().isIdleEnabled()), (npc, value) -> NpcPresentation.of(npc).getVehicleSounds().setIdleEnabled(value != 0), Values.NO_YES),
    /** Engine movement. */
    ENGINE_MOVEMENT(npc -> yes(NpcPresentation.of(npc).getVehicleSounds().isMovementEnabled()), (npc, value) -> NpcPresentation.of(npc).getVehicleSounds().setMovementEnabled(value != 0),
        Values.NO_YES);

    private final ToIntFunction<EntityNPCInterface> getter;
    private final ObjIntConsumer<EntityNPCInterface> setter;
    /** The Custom NPCs translation key of each value, by number; empty for a setting whose value is shown as a number. */
    @Getter
    private final List<String> valueKeys;

    NpcPresetSetting(Value tuning)
    {
        this(npc -> NpcTuning.of(npc).get(tuning), (npc, value) -> NpcTuning.of(npc).set(tuning, value), List.of());
    }

    NpcPresetSetting(Feature feature)
    {
        this(npc -> yes(settings(npc).wolffsmodnpcsWeaponOptions().enabled(feature)), (npc, value) -> settings(npc).wolffsmodnpcsWeaponOptions().set(feature, value != 0), Values.NO_YES);
    }

    /** Packs the own value and inheritance mode together so Undo restores both. */
    NpcPresetSetting(NpcTypeProperty property)
    {
        this(npc -> ((NumericTag) settings(npc).wolffsmodnpcsTypeProperties().own(npc, property)).getAsInt() * 2 + yes(settings(npc).wolffsmodnpcsCombat().isCustom(property)), (npc, value) ->
        {
            settings(npc).wolffsmodnpcsTypeProperties().setOwn(npc, property, IntTag.valueOf(value / 2));
            settings(npc).wolffsmodnpcsCombat().setCustom(property, value % 2 != 0);
        }, List.of());
    }

    private static NpcWeaponSettings settings(EntityNPCInterface npc)
    {
        return (NpcWeaponSettings) npc.stats;
    }

    public String group()
    {
        return switch (this)
        {
            case RESPAWN, CAN_DROWN, NO_FALL_DAMAGE, POTION_IMMUNE, IGNORE_COBWEB, HEALTH_REGEN, COMBAT_REGEN -> "survival";
            case KEEP_WRECK, HIDE_BODY, HURT_FLASH, DEATH_ROTATION, MODEL_SOUNDS, ENGINE_IDLE, ENGINE_MOVEMENT -> "appearance";
            case CONSUME_AMMO, ITEM_WEAPONS, MODEL_WEAPON_STATS, ON_ATTACK -> "combat";
            default -> "movement";
        };
    }

    public int displayValue(int value)
    {
        return switch (this)
        {
            case CAN_DROWN, NO_FALL_DAMAGE, POTION_IMMUNE, IGNORE_COBWEB, HEALTH_REGEN, COMBAT_REGEN -> value / 2;
            default -> value;
        };
    }

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
