package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class PlaneType extends DriveableType
{
    protected String stukaSound = "";
    protected int stukaSoundLength = 1;
    protected int stukaSoundRange = 50;
    protected float stukaSpeed = 9000F;

    @Override
    public DriveableKind getDriveableKind()
    {
        return DriveableKind.PLANE;
    }

    @Override
    public boolean isHelicopter()
    {
        return mode == EnumPlaneMode.HELI;
    }

    @Override
    public String getEngineStartupSound()
    {
        return StringUtils.firstNonBlank(startEngineSound, startSound);
    }

    @Override
    public int getEngineStartupSoundLength()
    {
        return StringUtils.isNotBlank(startEngineSound) ? startEngineSoundLength : startSoundLength;
    }

    @Override
    public String getEngineIdleLoopSound()
    {
        return StringUtils.firstNonBlank(idleSound, engineSound);
    }

    @Override
    public float getEngineIdleLoopPitchRange()
    {
        if (!StringUtils.isBlank(idleSound))
            return 0F;
        return Float.isFinite(engineSoundPitchRange) ? engineSoundPitchRange : 1F;
    }

    @Override
    public boolean usesEngineSoundAsIdleLoop()
    {
        return StringUtils.isBlank(idleSound);
    }

    protected EnumPlaneMode mode = EnumPlaneMode.PLANE;
    protected float lookDownModifier = 1F;
    protected float lookUpModifier = 1F;
    protected float rollLeftModifier = 1F;
    protected float rollRightModifier = 1F;
    protected float turnLeftModifier = 1F;
    protected float turnRightModifier = 1F;
    /** Full ground-steering yaw rate at 10 km/h; does not affect flight controls. */
    protected float realTurnRateDegPerSec;
    protected float restingPitch;
    protected boolean spinWithoutTail;
    protected boolean heliThrottlePull = true;
    protected boolean newFlightControl;
    protected float lift = 1F;
    protected float takeoffSpeed = 0.5F;
    protected float maxSpeed = 2F;
    protected boolean supersonic;
    protected float wingArea = 1F;
    protected float maxThrust = 50F;
    protected float mass = 1000F;
    protected float emptyDrag = 1F;
    protected int planeShootDelay;
    protected int planeBombDelay;

    protected Vector3f wingPos1 = new Vector3f();
    protected Vector3f wingPos2 = new Vector3f();
    protected Vector3f wingRot1 = new Vector3f();
    protected Vector3f wingRot2 = new Vector3f();
    protected Vector3f wingRate = new Vector3f();
    protected Vector3f wingRotRate = new Vector3f();
    protected Vector3f wingWheelPos1 = new Vector3f();
    protected Vector3f wingWheelPos2 = new Vector3f();
    protected Vector3f wingWheelRot1 = new Vector3f();
    protected Vector3f wingWheelRot2 = new Vector3f();
    protected Vector3f wingWheelRate = new Vector3f();
    protected Vector3f wingWheelRotRate = new Vector3f();
    protected Vector3f bodyWheelPos1 = new Vector3f();
    protected Vector3f bodyWheelPos2 = new Vector3f();
    protected Vector3f bodyWheelRot1 = new Vector3f();
    protected Vector3f bodyWheelRot2 = new Vector3f();
    protected Vector3f bodyWheelRate = new Vector3f();
    protected Vector3f bodyWheelRotRate = new Vector3f();
    protected Vector3f tailWheelPos1 = new Vector3f();
    protected Vector3f tailWheelPos2 = new Vector3f();
    protected Vector3f tailWheelRot1 = new Vector3f();
    protected Vector3f tailWheelRot2 = new Vector3f();
    protected Vector3f tailWheelRate = new Vector3f();
    protected Vector3f tailWheelRotRate = new Vector3f();
    protected Vector3f wheelCoverPos1 = new Vector3f();
    protected Vector3f wheelCoverPos2 = new Vector3f();
    protected Vector3f wheelCoverRot1 = new Vector3f();
    protected Vector3f wheelCoverRot2 = new Vector3f();
    protected Vector3f wheelCoverRate = new Vector3f();
    protected Vector3f wheelCoverRotRate = new Vector3f();
    protected Vector3f wingFlapPos1 = new Vector3f();
    protected Vector3f wingFlapPos2 = new Vector3f();
    protected Vector3f wingFlapRot1 = new Vector3f();
    protected Vector3f wingFlapRot2 = new Vector3f();
    protected Vector3f wingFlapRate = new Vector3f();
    protected Vector3f wingFlapRotRate = new Vector3f();
    /** Retracts the wing wheels before closing their covers, and opens the covers before lowering the wheels */
    protected boolean gearCoverPriority;
    protected Vector3f doorPos1 = new Vector3f();
    protected Vector3f doorPos2 = new Vector3f();
    protected Vector3f doorRot1 = new Vector3f();
    protected Vector3f doorRot2 = new Vector3f();
    protected Vector3f doorRate = new Vector3f();
    protected Vector3f doorRotRate = new Vector3f();

    protected final List<Propeller> propellers = new ArrayList<>();
    protected final List<Propeller> heliPropellers = new ArrayList<>();
    protected final List<Propeller> heliTailPropellers = new ArrayList<>();
    /**
     * Legacy HasGear only chose whether the mount hint advertised the gear key;
     * the gear itself was always retractable, so the great majority of type
     * files never declare it. Defaulting to true keeps those planes working,
     * while a pack that opts out with {@code HasGear False} (helicopter skids,
     * fixed undercarriages) still keeps its gear planted.
     */
    protected boolean hasGear = true;
    /**
     * HasDoor is the same kind of legacy hint as {@link #hasGear}: it only chose
     * whether the toggle printed a message, while the automatic door handling
     * ran on every plane. No shipped type file declares it, so gating behaviour
     * on it switches the doors off everywhere.
     */
    protected boolean hasDoor = true;
    /**
     * Air brakes are standard equipment on this model: every fixed-wing type has
     * them unless a pack opts out with {@code HasAirBrake False}. A helicopter has
     * no airframe surface to stand into the airflow, so {@code Mode Heli} defaults
     * the other way. The deployed brake area itself comes from
     * {@code RealAirBrakeAreaM2}, or from wing area when that is not researched.
     */
    protected boolean hasAirBrake = true;
    private static final float KILOWATTS_PER_HORSEPOWER = 0.7457F;
    private static final float KILOWATTS_PER_PS = 0.7355F;

    protected boolean hasAfterBurner;
    protected String afterBurnName = "Afterburner";
    /** Fuel burned while lit, relative to the same throttle without it */
    protected float afterBurnFuelPenalty = 1.5F;
    protected boolean afterburnWing;
    protected boolean afterburnWingFlipped;
    /** Rating without the afterburner or war emergency power; zero when not authored */
    protected float realDryThrustKn;
    protected float realDryPowerKw;
    /** Legacy cruise speed as a share of top speed; zero when not authored */
    protected float afterburnCruiseRatio;
    protected float airBrakeStrengthMultiplier = 1F;
    protected boolean hasWing;
    protected boolean foldWingForLand;
    protected boolean flyWithOpenDoor;
    protected boolean autoOpenDoorsNearGround = true;
    protected boolean autoDeployLandingGearNearGround = true;
    protected boolean valkyrie;
    protected boolean invInflight = true;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        stukaSound = readSound("StukaSound", stukaSound, file);
        stukaSoundLength = Math.max(1, readValue("StukaSoundLength", stukaSoundLength, file));
        stukaSoundRange = Math.max(0, readValue("StukaSoundRange", stukaSoundRange, file));
        stukaSpeed = Math.max(0F, readValue("StukaSpeed", stukaSpeed, file));
        mode = EnumPlaneMode.parse(readValue("Mode", mode.name(), file));
        newFlightControl = readValue("NewFlightControl", newFlightControl, file);
        turnLeftModifier = readOptionalValue("TurnLeftSpeed", turnLeftModifier, file);
        turnRightModifier = readValue("TurnRightSpeed", turnRightModifier, file);
        String groundTurnKey = file.hasConfigLine("RealTurnRateDegPerSec") ? "RealTurnRateDegPerSec" : "TurnRateDegPerSec";
        float groundTurnRate = readValue(groundTurnKey, 0F, file);
        realTurnRateDegPerSec = Float.isFinite(groundTurnRate) && groundTurnRate > 0F ? groundTurnRate : 0F;
        lookUpModifier = readValue("LookUpSpeed", lookUpModifier, file);
        lookDownModifier = readValue("LookDownSpeed", lookDownModifier, file);
        rollLeftModifier = readValue("RollLeftSpeed", rollLeftModifier, file);
        rollRightModifier = readValue("RollRightSpeed", rollRightModifier, file);
        lift = readValue("Lift", lift, file);
        takeoffSpeed = readValue("TakeoffSpeed", takeoffSpeed, file);
        maxSpeed = readValue("MaxSpeed", maxSpeed, file);
        maxSpeed = readValue("MaximumSpeed", maxSpeed, file);
        supersonic = readValue("Supersonic", supersonic, file);
        maxThrust = readValue("MaxThrust", maxThrust, file);
        mass = Math.max(1F, readValue("Mass", mass, file));
        wingArea = Math.max(0F, readValue("WingArea", wingArea, file));
        heliThrottlePull = readValue("HeliThrottlePull", heliThrottlePull, file);
        emptyDrag = Math.max(0F, readValue("EmptyDrag", emptyDrag, file));
        planeShootDelay = Math.max(0, Math.round(readValue("ShootDelay", (float) planeShootDelay, file)));
        planeBombDelay = Math.max(0, Math.round(readValue("BombDelay", (float) planeBombDelay, file)));

        readPropellers("Propeller", propellers, file);
        readPropellers("HeliPropeller", heliPropellers, file);
        readPropellers("HeliTailPropeller", heliTailPropellers, file);
        engineSoundLength = readSoundLength("PropSoundLength", engineSoundLength, file);
        engineSound = readSound("PropSound", engineSound, file);
        registerSoundTimer("PropSoundLength", () -> engineSound, () -> engineSoundLength, length -> engineSoundLength = length);

        hasGear = readValue("HasGear", hasGear, file);
        hasGear = readValue("HasLandingGear", hasGear, file);
        hasDoor = readValue("HasDoor", hasDoor, file);
        hasAirBrake = readValue("HasAirBrake", mode != EnumPlaneMode.HELI, file);
        hasAirBrake = readValue("AirBrake", hasAirBrake, file);
        hasAfterBurner = readValue("HasAfterBurner", hasAfterBurner, file);
        afterBurnName = readValue("AfterBurnName", afterBurnName, file);
        afterBurnFuelPenalty = Math.max(0F, readValue("AfterBurnFuelPenalty", afterBurnFuelPenalty, file));
        afterburnWing = readValue("AfterburnWing", afterburnWing, file);
        afterburnWingFlipped = readValue("AfterburnWingFlipped", afterburnWingFlipped, file);
        realDryThrustKn = readValue("RealDryThrustKn", realDryThrustKn, file);
        realDryPowerKw = readValue("RealDryPowerKw", 0F, file);
        if (realDryPowerKw <= 0F)
            realDryPowerKw = readValue("RealDryPowerHp", 0F, file) * KILOWATTS_PER_HORSEPOWER;
        if (realDryPowerKw <= 0F)
            realDryPowerKw = readValue("RealDryPowerPS", 0F, file) * KILOWATTS_PER_PS;
        Float cruiseSpeed = readFloat("cruiseSpeed", file);
        Float topSpeed = readFloat("maxSpeed", file);
        if (cruiseSpeed != null && topSpeed != null && cruiseSpeed > 0F && topSpeed > 0F)
            afterburnCruiseRatio = Math.min(1F, cruiseSpeed / topSpeed);
        airBrakeStrengthMultiplier = readValue("AirBrakeStrengthMultiplier", airBrakeStrengthMultiplier, file);
        if (!Float.isFinite(airBrakeStrengthMultiplier) || airBrakeStrengthMultiplier < 0F)
            airBrakeStrengthMultiplier = 1F;
        hasWing = readValue("HasWing", hasWing, file);
        foldWingForLand = readValue("FoldWingForLand", foldWingForLand, file);
        flyWithOpenDoor = readValue("FlyWithOpenDoor", flyWithOpenDoor, file);
        autoOpenDoorsNearGround = readValue("AutoOpenDoorsNearGround", autoOpenDoorsNearGround, file);
        autoDeployLandingGearNearGround = readValue("AutoDeployLandingGearNearGround", autoDeployLandingGearNearGround, file);
        restingPitch = readValue("RestingPitch", restingPitch, file);
        spinWithoutTail = readValue("SpinWithoutTail", spinWithoutTail, file);
        valkyrie = readValue("Valkyrie", valkyrie, file);
        invInflight = readValue("InflightInventory", invInflight, file);
        invInflight = readValue("InvInflight", invInflight, file);

        wingPos1 = readVector("WingPosition1", wingPos1, file);
        wingPos2 = readVector("WingPosition2", wingPos2, file);
        wingRot1 = readVector("WingRotation1", wingRot1, file);
        wingRot2 = readVector("WingRotation2", wingRot2, file);
        wingRate = readVector("WingRate", wingRate, file);
        wingRotRate = readVector("WingRotRate", wingRotRate, file);
        wingWheelPos1 = readVector("WingWheelPosition1", wingWheelPos1, file);
        wingWheelPos2 = readVector("WingWheelPosition2", wingWheelPos2, file);
        wingWheelRot1 = readVector("WingWheelRotation1", wingWheelRot1, file);
        wingWheelRot2 = readVector("WingWheelRotation2", wingWheelRot2, file);
        wingWheelRate = readVector("WingWheelRate", wingWheelRate, file);
        wingWheelRotRate = readVector("WingWheelRotRate", wingWheelRotRate, file);
        bodyWheelPos1 = readVector("BodyWheelPosition1", bodyWheelPos1, file);
        bodyWheelPos2 = readVector("BodyWheelPosition2", bodyWheelPos2, file);
        bodyWheelRot1 = readVector("BodyWheelRotation1", bodyWheelRot1, file);
        bodyWheelRot2 = readVector("BodyWheelRotation2", bodyWheelRot2, file);
        bodyWheelRate = readVector("BodyWheelRate", bodyWheelRate, file);
        bodyWheelRotRate = readVector("BodyWheelRotRate", bodyWheelRotRate, file);
        tailWheelPos1 = readVector("TailWheelPosition1", tailWheelPos1, file);
        tailWheelPos2 = readVector("TailWheelPosition2", tailWheelPos2, file);
        tailWheelRot1 = readVector("TailWheelRotation1", tailWheelRot1, file);
        tailWheelRot2 = readVector("TailWheelRotation2", tailWheelRot2, file);
        tailWheelRate = readVector("TailWheelRate", tailWheelRate, file);
        tailWheelRotRate = readVector("TailWheelRotRate", tailWheelRotRate, file);
        wheelCoverPos1 = readVector("WheelCoverPosition1", wheelCoverPos1, file);
        wheelCoverPos2 = readVector("WheelCoverPosition2", wheelCoverPos2, file);
        wheelCoverRot1 = readVector("WheelCoverRotation1", wheelCoverRot1, file);
        wheelCoverRot2 = readVector("WheelCoverRotation2", wheelCoverRot2, file);
        wheelCoverRate = readVector("WheelCoverRate", wheelCoverRate, file);
        wheelCoverRotRate = readVector("WheelCoverRotRate", wheelCoverRotRate, file);
        wingFlapPos1 = readVector("WingFlapPosition1", wingFlapPos1, file);
        wingFlapPos2 = readVector("WingFlapPosition2", wingFlapPos2, file);
        wingFlapRot1 = readVector("WingFlapRotation1", wingFlapRot1, file);
        wingFlapRot2 = readVector("WingFlapRotation2", wingFlapRot2, file);
        wingFlapRate = readVector("WingFlapRate", wingFlapRate, file);
        wingFlapRotRate = readVector("WingFlapRotRate", wingFlapRotRate, file);
        gearCoverPriority = readValue("GearCoverPriority", gearCoverPriority, file);
        doorPos1 = readVector("DoorPosition1", doorPos1, file);
        doorPos2 = readVector("DoorPosition2", doorPos2, file);
        doorRot1 = readVector("DoorRotation1", doorRot1, file);
        doorRot2 = readVector("DoorRotation2", doorRot2, file);
        doorRate = readVector("DoorRate", doorRate, file);
        doorRotRate = readVector("DoorRotRate", doorRotRate, file);

        // Re-run finalization now that Mode, propellers and NewFlightControl are
        // read, so physics resolution sees the complete definition.
        finishDerivedValues();
    }

    @Override
    protected EnumVehicleCategory physicsCategory()
    {
        return EnumVehicleCategory.AIRCRAFT;
    }

    @Override
    protected LegacyPhysicsHints legacyPhysicsHints()
    {
        return new LegacyPhysicsHints(false, false, maxNegativeThrottle, floatOnWater, newFlightControl, false);
    }

    /** Restores the space some definitions lack between the z coordinate and the part name. */
    private static String[] repairPropellerLine(String key, String[] values, TypeFile file)
    {
        if (values == null || values.length != 5)
            return values;
        String[] zAndPart = splitNumberThenName(values[3]);
        if (zAndPart == null || EnumDriveablePart.getPart(zAndPart[1]) == null)
            return values;
        logRecoverable(key + ": missing space in '" + values[3] + "'", file);
        return new String[]{values[0], values[1], values[2], zAndPart[0], zAndPart[1], values[4]};
    }

    /** Whether the plane defines a landing flap movement that its pilot can toggle */
    public boolean hasLandingFlaps()
    {
        return !isZero(wingFlapRate) || !isZero(wingFlapRotRate);
    }

    private static boolean isZero(Vector3f vector)
    {
        return vector.x == 0F && vector.y == 0F && vector.z == 0F;
    }

    private void readPropellers(String key, List<Propeller> destination, TypeFile file)
    {
        for (String[] line : readValuesInLines(key, file).orElse(List.of()))
        {
            String[] values = repairPropellerLine(key, line, file);
            if (values == null || values.length < 6)
            {
                logError(key + " expects id x y z part item", file);
                continue;
            }
            try
            {
                Propeller propeller = new Propeller(Integer.parseInt(values[0]), new Vector3f(parseLegacyFloat(values[1]) / 16F, parseLegacyFloat(values[2]) / 16F, parseLegacyFloat(values[3]) / 16F),
                    EnumDriveablePart.getPart(values[4]), values[5], contentPack);
                destination.add(propeller);
                driveableRecipe.add(RecipeIngredient.parse(values[5], 1, contentPack));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    @Override
    public int numEngines()
    {
        return switch (mode)
        {
            case VTOL -> Math.max(propellers.size(), heliPropellers.size());
            case PLANE, SIXDOF -> propellers.size();
            case HELI -> heliPropellers.size();
        };
    }

    @Override
    public List<ItemStack> getItemsRequired(DriveablePart part, PartType engine)
    {
        List<ItemStack> stacks = new ArrayList<>(super.getItemsRequired(part, engine));
        if (part == null)
            return stacks;
        for (Propeller propeller : propellers)
        {
            if (propeller.getPlanePart() != part.getType())
                continue;
            ModUtils.getItemStack(propeller.getItemType()).ifPresent(stacks::add);
            ModUtils.getItemStack(engine).ifPresent(stacks::add);
        }
        return stacks;
    }
}
