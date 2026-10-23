package com.wolffsmod.npcs.presets;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.IDriveableType;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelFolder;
import com.wolffsmod.npcs.sim.NpcTuning;
import lombok.Getter;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.wolffsmod.npcs.presets.NpcPresetSetting.*;

/**
 * A set of Custom NPCs settings the Wolff's Mod tab applies in one go. A preset only sets what it lists; everything else
 * the NPC has stays as it is. Packets hold the name, so presets may be added anywhere.
 */
public enum NpcPreset
{
    MOB(List.of(), new Target(MOVING_TYPE, Values.WANDERING), new Target(WALKING_RANGE, 0), new Target(RETURN_TO_START, 0), new Target(NPC_INTERACTING, 0), new Target(RESPAWN, Values.RESPAWN_NO),
        new Target(HIDE_BODY, 1), new Target(KEEP_WRECK, 0), new Target(DOOR_INTERACT, 2), new Target(CAN_SPRINT, 0), new Target(MOVEMENT, 0), new Target(TERRAIN, 0), new Target(FLIGHT_TYPE, 0),
        new Target(TACTIC, 0), new Target(TACTICAL_VARIANT, 0), new Target(SUBMARINE_DEPTH, 0), new Target(LAND_SPEED, 0), new Target(WATER_SPEED, 0), new Target(DRIVE_DRIVEABLES, 0),
        new Target(ON_ATTACK, 0)),
    /** A freely roaming person using equipped weapons. */
    HUMAN(MOB.targets, new Target(DOOR_INTERACT, Values.DOORS_OPEN), new Target(CAN_SPRINT, 1), new Target(ITEM_WEAPONS, 1)),
    /** Player movement, respawning and ammunition consumption. */
    PLAYER(HUMAN.targets, new Target(RESPAWN, 0), new Target(CONSUME_AMMO, 1)),
    /** Ground vehicles and mechas, with machine protections and persistent wrecks. */
    VEHICLE(MOB.targets, new Target(TERRAIN, 1), new Target(HIDE_BODY, 0), new Target(KEEP_WRECK, 1), new Target(HURT_FLASH, 0), new Target(DEATH_ROTATION, 0), new Target(CAN_DROWN, 1),
        new Target(NO_FALL_DAMAGE, 1), new Target(POTION_IMMUNE, 3), new Target(IGNORE_COBWEB, 3), new Target(HEALTH_REGEN, 1), new Target(COMBAT_REGEN, 1), new Target(MODEL_WEAPON_STATS, 1),
        new Target(MODEL_SOUNDS, 1), new Target(DRIVING, 1), new Target(ENGINE_IDLE, 1), new Target(ENGINE_MOVEMENT, 1), new Target(DRIVER_GUNS, 1)),
    /** Aircraft flight, automatically adapted to helicopters. */
    PLANE(VEHICLE.targets, new Target(MOVEMENT, 1), new Target(TERRAIN, 0), new Target(FLIGHT_TYPE, 1), new Target(TACTIC, NpcTuning.Tactic.STRAFING_RUN.ordinal())),
    /** Surface watercraft. */
    SHIP(VEHICLE.targets, new Target(MOVEMENT, 2), new Target(TERRAIN, 2)),
    /** A freely roaming creature that flees. */
    PASSIVE_CREATURE(MOB.targets, new Target(ON_ATTACK, 1), new Target(ITEM_WEAPONS, 0)),
    /** A static weapon that returns if displaced. */
    TURRET(VEHICLE.targets, new Target(MOVING_TYPE, 0), new Target(RETURN_TO_START, 1), new Target(TACTIC, 2), new Target(DRIVING, 0));

    /** What the preset sets, in the order its tooltip lists it: those of the preset it builds on first. */
    @Getter
    private final List<Target> targets;

    NpcPreset(List<Target> base, Target... own)
    {
        Map<NpcPresetSetting, Target> all = new LinkedHashMap<>();
        base.forEach(target -> all.put(target.setting(), target));
        for (Target target : own)
            all.put(target.setting(), target);
        targets = List.copyOf(all.values());
    }

    /** @return whether the NPC already has every setting of the preset */
    public boolean matches(EntityNPCInterface npc)
    {
        return targets.stream().allMatch(target -> target.setting().get(npc) == value(npc, target));
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
            target.setting().set(npc, value(npc, target));
        npc.updateAI = true;
        npc.updateClient = true;
    }

    /** Helicopters use hovering flight and a compatible tactic. */
    public int value(EntityNPCInterface npc, Target target)
    {
        if (this == PLANE && model(npc) instanceof IDriveableType driveable && driveable.isHelicopter())
        {
            if (target.setting() == FLIGHT_TYPE)
                return NpcTuning.Flight.HELICOPTER.ordinal();
            if (target.setting() == TACTIC)
                return NpcTuning.Tactic.STRAFE.ordinal();
        }
        if (this == VEHICLE && target.setting() == DRIVING && FlanModelFolder.of(model(npc)) == FlanModelFolder.MECHA)
            return 0;
        return target.setting().presetValue(npc, target.value());
    }

    @Nullable
    public static IContentType model(EntityNPCInterface npc)
    {
        return npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model ? model.getInfoType() : null;
    }

    public static NpcPreset suggested(EntityNPCInterface npc)
    {
        FlanModelFolder folder = FlanModelFolder.of(model(npc));
        return folder == null ? HUMAN : switch (folder)
        {
            case AA_GUN -> TURRET;
            case VEHICLE, MECHA -> VEHICLE;
            case PLANE -> PLANE;
            case SHIP -> SHIP;
        };
    }

    public String key()
    {
        return "wolffsmodnpcs.preset." + name().toLowerCase(Locale.ROOT);
    }

    /** One setting of a preset and the value it is given. */
    public record Target(NpcPresetSetting setting, int value)
    {}
}
