package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.*;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Independent stationary/movement channels. Selecting vehicle defaults resets custom sound values. */
@Getter
public final class NpcVehicleSounds
{
    private static final String NBT_KEY = "VehicleSounds";
    private static final String MOVEMENT_ENABLED = "MovementEnabled";
    private static final String VARIABLE_PITCH = "VariablePitch";
    private static final String MODEL_DEFAULTS = "ModelDefaults";
    private static final String LEGACY_SOUND = "EngineSound";
    private static final String LEGACY_RANGE = "EngineRange";
    private static final String LEGACY_REPEAT = "EngineRepeatTicks";

    @Setter
    private boolean idleEnabled;
    @Setter
    private boolean movementEnabled = true;
    @Setter
    private boolean variablePitch = true;
    @Setter
    private boolean modelDefaults = true;
    private final NpcEngineSettings idle = new NpcEngineSettings();
    private final NpcEngineSettings movement = new NpcEngineSettings();
    private CompoundTag legacyMovement = new CompoundTag();

    public void initializeCustom(EntityNPCInterface npc)
    {
        initializeCustom(defaultSound(npc, false), defaultSound(npc, true), FlansEntitySounds.getDefaultEngineRange());
    }

    void initializeCustom(Optional<EngineSound> idleDefaults, Optional<EngineSound> movementDefaults, float serverRange)
    {
        idle.initialize(idleDefaults, serverRange);
        movement.initialize(movementDefaults, legacyMovement.isEmpty() ? serverRange : movementDefaults.map(EngineSound::range).orElse(serverRange));
        if (!legacyMovement.isEmpty())
        {
            if (!legacyMovement.getString(LEGACY_SOUND).isBlank())
                movement.setSound(legacyMovement.getString(LEGACY_SOUND));
            if (legacyMovement.getInt(LEGACY_RANGE) > 0)
                movement.setRange(legacyMovement.getInt(LEGACY_RANGE));
            if (legacyMovement.getInt(LEGACY_REPEAT) > 0)
            {
                movement.setOverrideRepeat(true);
                movement.setRepeatTicks(legacyMovement.getInt(LEGACY_REPEAT));
            }
            legacyMovement = new CompoundTag();
        }
    }

    public void useVehicleDefaults(EntityNPCInterface npc)
    {
        modelDefaults = true;
        legacyMovement = new CompoundTag();
        float range = FlansEntitySounds.getDefaultEngineRange();
        idle.resetToVehicle(defaultSound(npc, false), range);
        movement.resetToVehicle(defaultSound(npc, true), range);
    }

    public Optional<EngineSound> resolve(EntityNPCInterface npc, boolean moving)
    {
        if (!(moving ? movementEnabled : idleEnabled))
            return Optional.empty();
        if (!modelDefaults)
        {
            initializeCustom(npc);
            return (moving ? movement : idle).resolve();
        }
        return defaultSound(npc, moving).map(sound -> new EngineSound(sound.sound(), sound.range(),
            FlansEntitySounds.getSoundLength(ResourceLocation.parse(sound.sound().contains(":") ? sound.sound() : "flansmod:" + sound.sound())).orElse(sound.repeatTicks())));
    }

    private static Optional<EngineSound> defaultSound(EntityNPCInterface npc, boolean moving)
    {
        return NpcPresentation.vehicleType(npc).flatMap(type -> moving ? FlansEntityTypes.getEngineSound(type) : FlansEntityTypes.getEngineIdleSound(type));
    }

    public boolean automaticRepeat(boolean moving)
    {
        return modelDefaults || (moving ? movement : idle).automaticRepeat();
    }

    public static double movementSquared(EntityNPCInterface npc)
    {
        return npc.position().distanceToSqr(new Vec3(npc.xo, npc.yo, npc.zo));
    }

    public static boolean moving(EntityNPCInterface npc)
    {
        double distance = movementSquared(npc);
        // Ignore sub-centimetre jitter and teleport jumps; reverse, flight and swimming still count.
        return distance > 0.0001D && distance < 256D;
    }

    public static float pitch(EntityNPCInterface npc)
    {
        double distance = movementSquared(npc);
        double speed = distance < 256D ? Math.sqrt(distance) : 0D;
        double normalSpeed = Math.max(0.01D, npc.ais.getWalkingSpeed() / 20D);
        float fraction = (float) Math.min(1D, speed / normalSpeed);
        return NpcPresentation.vehicleType(npc).map(type -> FlansEntityTypes.getEnginePitch(type, fraction)).orElse(0.5F + 0.7F * fraction);
    }

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        idleEnabled = tag.getBoolean("IdleEnabled");
        movementEnabled = !tag.contains(MOVEMENT_ENABLED, Tag.TAG_BYTE) || tag.getBoolean(MOVEMENT_ENABLED);
        variablePitch = !tag.contains(VARIABLE_PITCH, Tag.TAG_BYTE) || tag.getBoolean(VARIABLE_PITCH);
        modelDefaults = !tag.contains(MODEL_DEFAULTS, Tag.TAG_BYTE) || tag.getBoolean(MODEL_DEFAULTS);
        idle.load(tag.getCompound("Idle"));
        movement.load(tag.getCompound("Movement"));
        legacyMovement = tag.getCompound("LegacyMovement").copy();
        if (!root.contains(NBT_KEY, Tag.TAG_COMPOUND))
            migrate(root);
    }

    private void migrate(CompoundTag tag)
    {
        movementEnabled = !tag.contains("EngineEnabled", Tag.TAG_BYTE) || tag.getBoolean("EngineEnabled");
        if (!tag.getString(LEGACY_SOUND).isBlank() || tag.getInt(LEGACY_RANGE) > 0 || tag.getInt(LEGACY_REPEAT) > 0)
        {
            modelDefaults = false;
            CompoundTag migrated = new CompoundTag();
            migrated.putString("Sound", tag.getString(LEGACY_SOUND));
            migrated.putInt("Range", tag.getInt(LEGACY_RANGE) > 0 ? tag.getInt(LEGACY_RANGE) : 50);
            migrated.putInt("RepeatTicks", tag.getInt(LEGACY_REPEAT) > 0 ? tag.getInt(LEGACY_REPEAT) : 20);
            migrated.putBoolean("OverrideRepeat", tag.getInt(LEGACY_REPEAT) > 0);
            migrated.putBoolean("Initialized", false);
            movement.load(migrated);
            legacyMovement = tag.copy();
        }
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IdleEnabled", idleEnabled);
        tag.putBoolean(MOVEMENT_ENABLED, movementEnabled);
        tag.putBoolean(VARIABLE_PITCH, variablePitch);
        tag.putBoolean(MODEL_DEFAULTS, modelDefaults);
        tag.put("Idle", idle.save());
        tag.put("Movement", movement.save());
        if (!legacyMovement.isEmpty())
            tag.put("LegacyMovement", legacyMovement.copy());
        root.put(NBT_KEY, tag);
    }
}
