package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.LegacyPhysicsHints;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class VehicleType extends DriveableType
{
    public record SmokePoint(Vector3f position, Vector3f direction, int detonationTime, EnumDriveablePart part) {}

    protected float turnLeftModifier = 1F;
    protected float turnRightModifier = 1F;
    protected boolean squashMobs;
    protected boolean fourWheelDrive;
    protected boolean rotateWheels;
    protected boolean tank;
    protected float throttleDecay = 0.0035F;
    protected int vehicleShootDelay;
    protected int vehicleShellDelay;
    protected boolean hasDoor;
    protected float mass = 1000F;
    protected boolean useRealisticAcceleration;
    protected float brakingModifier = 1F;
    protected float maxFallSpeed = 0.85F;
    protected float gravity = 0.175F;
    protected Vector3f doorPos1 = new Vector3f();
    protected Vector3f doorPos2 = new Vector3f();
    protected Vector3f doorRot1 = new Vector3f();
    protected Vector3f doorRot2 = new Vector3f();
    protected Vector3f doorRate = new Vector3f();
    protected Vector3f doorRotRate = new Vector3f();
    protected Vector3f door2Pos1 = new Vector3f();
    protected Vector3f door2Pos2 = new Vector3f();
    protected Vector3f door2Rot1 = new Vector3f();
    protected Vector3f door2Rot2 = new Vector3f();
    protected Vector3f door2Rate = new Vector3f();
    protected Vector3f door2RotRate = new Vector3f();
    protected boolean shootWithOpenDoor;
    protected int trackLinkFix = 5;
    protected final List<SmokePoint> smokers = new ArrayList<>();
    protected String stompSoundFrontRight = StringUtils.EMPTY;
    protected String stompSoundFrontLeft = StringUtils.EMPTY;
    protected String stompSoundBackRight = StringUtils.EMPTY;
    protected String stompSoundBackLeft = StringUtils.EMPTY;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        turnLeftModifier = readOptionalValue("TurnLeftSpeed", turnLeftModifier, file);
        turnRightModifier = readValue("TurnRightSpeed", turnRightModifier, file);
        squashMobs = readValue("SquashMobs", squashMobs, file);
        fourWheelDrive = readValue("FourWheelDrive", fourWheelDrive, file);
        tank = readValue("Tank", tank, file);
        tank = readValue("TankMode", tank, file);
        throttleDecay = Math.max(0F, readValue("ThrottleDecay", throttleDecay, file));
        mass = Math.max(1F, readValue("Mass", mass, file));
        useRealisticAcceleration = readValue("UseRealisticAcceleration", useRealisticAcceleration, file);
        gravity = readValue("Gravity", gravity, file);
        maxFallSpeed = Math.max(0F, readValue("MaxFallSpeed", maxFallSpeed, file));
        brakingModifier = Math.max(0F, readValue("BrakingModifier", brakingModifier, file));
        hasDoor = readValue("HasDoor", hasDoor, file);
        shootWithOpenDoor = readValue("ShootWithOpenDoor", shootWithOpenDoor, file);
        rotateWheels = readValue("RotateWheels", rotateWheels, file);
        trackLinkFix = readValue("FixTrackLink", trackLinkFix, file);
        trackLinkFix = readValue("TrackLinkFix", trackLinkFix, file);
        vehicleShootDelay = Math.max(0, Math.round(readValue("ShootDelay", (float) vehicleShootDelay, file)));
        vehicleShellDelay = Math.max(0, Math.round(readValue("ShellDelay", (float) vehicleShellDelay, file)));
        shootSoundPrimary = readSound("ShootSound", shootSoundPrimary, file);
        shootSoundSecondary = readSound("ShellSound", shootSoundSecondary, file);

        doorPos1 = readVector("DoorPosition1", doorPos1, file);
        doorPos2 = readVector("DoorPosition2", doorPos2, file);
        doorRot1 = readVector("DoorRotation1", doorRot1, file);
        doorRot2 = readVector("DoorRotation2", doorRot2, file);
        doorRate = readVector("DoorRate", doorRate, file);
        doorRotRate = readVector("DoorRotRate", doorRotRate, file);
        door2Pos1 = readVector("Door2Position1", door2Pos1, file);
        door2Pos2 = readVector("Door2Position2", door2Pos2, file);
        door2Rot1 = readVector("Door2Rotation1", door2Rot1, file);
        door2Rot2 = readVector("Door2Rotation2", door2Rot2, file);
        door2Rate = readVector("Door2Rate", door2Rate, file);
        door2RotRate = readVector("Door2RotRate", door2RotRate, file);

        readSmokePoints("AddSmokePoint", file);
        readSmokePoints("AddSmokeDispenser", file);
        stompSoundFrontRight = readSound("StompSoundFrontRight", stompSoundFrontRight, file);
        stompSoundFrontLeft = readSound("StompSoundFrontLeft", stompSoundFrontLeft, file);
        stompSoundBackRight = readSound("StompSoundBackRight", stompSoundBackRight, file);
        stompSoundBackLeft = readSound("StompSoundBackLeft", stompSoundBackLeft, file);

        // Re-run finalization now that Tank, FourWheelDrive and the rest are read,
        // so physics resolution sees the complete definition.
        finishDerivedValues();
    }

    /**
     * Boats are vehicles in this repository, so they share the ground category
     * and reach the marine draft override through {@code FloatOnWater}.
     */
    @Override
    protected EnumVehicleCategory physicsCategory()
    {
        return EnumVehicleCategory.GROUND;
    }

    @Override
    protected LegacyPhysicsHints legacyPhysicsHints()
    {
        return new LegacyPhysicsHints(tank, fourWheelDrive, maxNegativeThrottle, floatOnWater,
            false, useRealisticAcceleration);
    }

    private void readSmokePoints(String key, TypeFile file)
    {
        for (String[] values : readValuesInLines(key, file).orElse(List.of()))
        {
            if (values == null || values.length < 4)
                continue;
            try
            {
                Vector3f position = parseBracketVector(values[0]).scale(1F / 16F);
                Vector3f direction = parseBracketVector(values[1]);
                smokers.add(new SmokePoint(position, direction, Integer.parseInt(values[2]), EnumDriveablePart.getPart(values[3])));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    private static Vector3f parseBracketVector(String raw)
    {
        String[] values = raw.replace('[', ' ').replace(']', ' ').replace(',', ' ').trim().split("\\s+");
        return new Vector3f(Float.parseFloat(values[0]), Float.parseFloat(values[1]), Float.parseFloat(values[2]));
    }
}
