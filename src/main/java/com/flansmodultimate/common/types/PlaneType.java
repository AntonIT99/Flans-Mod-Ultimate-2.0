package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.Propeller;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.LegacyPhysicsHints;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class PlaneType extends DriveableType
{
    protected EnumPlaneMode mode = EnumPlaneMode.PLANE;
    protected float lookDownModifier = 1F;
    protected float lookUpModifier = 1F;
    protected float rollLeftModifier = 1F;
    protected float rollRightModifier = 1F;
    protected float turnLeftModifier = 1F;
    protected float turnRightModifier = 1F;
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
        mode = EnumPlaneMode.parse(readValue("Mode", mode.name(), file));
        newFlightControl = readValue("NewFlightControl", newFlightControl, file);
        turnLeftModifier = readOptionalValue("TurnLeftSpeed", turnLeftModifier, file);
        turnRightModifier = readValue("TurnRightSpeed", turnRightModifier, file);
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
        shootSoundPrimary = readSound("ShootSound", shootSoundPrimary, file);
        shootSoundSecondary = readSound("BombSound", shootSoundSecondary, file);

        hasGear = readValue("HasGear", hasGear, file);
        hasGear = readValue("HasLandingGear", hasGear, file);
        hasDoor = readValue("HasDoor", hasDoor, file);
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
        return new LegacyPhysicsHints(false, false, maxNegativeThrottle, floatOnWater,
            newFlightControl, false);
    }

    private void readPropellers(String key, List<Propeller> destination, TypeFile file)
    {
        for (String[] values : readValuesInLines(key, file).orElse(List.of()))
        {
            if (values == null || values.length < 6)
            {
                logError(key + " expects id x y z part item", file);
                continue;
            }
            try
            {
                Propeller propeller = new Propeller(Integer.parseInt(values[0]),
                    new Vector3f(parseLegacyFloat(values[1]) / 16F, parseLegacyFloat(values[2]) / 16F, parseLegacyFloat(values[3]) / 16F),
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

    private static float parseLegacyFloat(String raw)
    {
        String value = raw.trim();
        if (value.indexOf(',') == value.lastIndexOf(',') && value.indexOf(',') > 0 && value.indexOf('.') < 0)
            value = value.replace(',', '.');
        return Float.parseFloat(value);
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
