package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.CollisionMesh;
import com.flansmodultimate.common.driveables.DriveableCollisionProfile;
import com.flansmodultimate.common.driveables.DriveableExplosion;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.ParticleEmitter;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.driveables.armor.ResolvedVehicleArmor;
import com.flansmodultimate.common.driveables.armor.VehicleArmorResolver;
import com.flansmodultimate.common.driveables.armor.VehicleArmorSpec;
import com.flansmodultimate.common.driveables.armor.VehicleArmorSpecReader;
import com.flansmodultimate.common.driveables.armor.VehicleHealthScaler;
import com.flansmodultimate.common.driveables.physics.EnumDriveType;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.LegacyPhysicsHints;
import com.flansmodultimate.common.driveables.physics.RealWorldSpecReader;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehicleGeometry;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsResolver;
import com.flansmodultimate.common.guns.AmmoOverrides;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.LootTableLoadEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.flansmodultimate.util.TypeReaderUtils.*;

/** Shared content definition for planes, vehicles and mechas. */
@Getter
@NoArgsConstructor
public class DriveableType extends PaintableType implements IAmmoGroupUser, IAmmoOverrideUser
{
    /** Legacy default rate applied when a weapon bank states neither a rate nor a delay. */
    private static final float DEFAULT_ROUNDS_PER_MIN = 60F;

    protected final Map<EnumDriveablePart, CollisionBox> health = new EnumMap<>(EnumDriveablePart.class);
    /** Original, unscaled definitions retained so repeated finalization is idempotent. */
    private final Map<EnumDriveablePart, CollisionBox> authoredHealth = new EnumMap<>(EnumDriveablePart.class);
    protected final Map<EnumDriveablePart, DriveableExplosion> partDeathExplosions = new EnumMap<>(EnumDriveablePart.class);
    protected final Map<EnumDriveablePart, List<RecipeIngredient>> partwiseRecipe = new EnumMap<>(EnumDriveablePart.class);
    protected final List<RecipeIngredient> driveableRecipe = new ArrayList<>();

    protected boolean acceptAllAmmo = true;
    protected final Set<String> ammo = new LinkedHashSet<>();
    /**
     * Ammo groups pulled in with "UseAmmoGroup". Every ammo item declaring "AddToAmmoGroup" with one of these
     * names is accepted by this driveable, exactly as if it had been listed with "AddAmmo".
     */
    protected final Set<String> ammoGroups = new LinkedHashSet<>();
    /** Per-ammunition statistic overrides declared by this driveable. */
    @Getter
    protected AmmoOverrides ammoOverrides = AmmoOverrides.EMPTY;
    private volatile List<BulletType> resolvedAmmoTypes;
    /** Ammo group revision the cache above was built from; groups can still grow while later packs load */
    private volatile int resolvedAmmoGroupRevision;

    protected boolean harvestBlocks;
    protected final Set<String> materialsHarvested = new LinkedHashSet<>();
    protected boolean collectHarvest;
    protected boolean dropHarvest;
    protected Vector3f harvestBoxSize = new Vector3f();
    protected Vector3f harvestBoxPos = new Vector3f();
    protected int reloadSoundTick = 15_214_541;
    protected float fallDamageFactor = 1F;
    protected int engineStartTime;
    /** Optional engine shortname/item ID that overrides global automatic engine selection. */
    protected String engine = StringUtils.EMPTY;

    protected EnumWeaponType primary = EnumWeaponType.NONE;
    protected EnumWeaponType secondary = EnumWeaponType.NONE;
    protected boolean alternatePrimary;
    protected boolean alternateSecondary;
    protected float shootDelayPrimary = -1F;
    protected float shootDelaySecondary = -1F;
    /**
     * When true, a weapon bank whose shoot points mount a gun ignores its own
     * ShootDelay/BulletSpeed/BulletSpread/damage-multiplier fields and reads them
     * directly from the GunType referenced by that mount instead. This applies to
     * both banks, so a fighter firing wing machine guns as its primary weapon and
     * cannons as its secondary takes each bank's numbers from the mounted gun.
     */
    protected boolean readWeaponsFromGunTypes;
    protected float damageMultiplierPrimary = 1F;
    protected float damageMultiplierSecondary = 1F;
    protected EnumFireMode modePrimary = EnumFireMode.FULLAUTO;
    protected EnumFireMode modeSecondary = EnumFireMode.FULLAUTO;
    protected String shootSoundPrimary = StringUtils.EMPTY;
    protected String shootSoundSecondary = StringUtils.EMPTY;
    protected String shootReloadSound = StringUtils.EMPTY;
    protected final List<ShootPoint> shootPointsPrimary = new ArrayList<>();
    protected final List<ShootPoint> shootPointsSecondary = new ArrayList<>();
    protected final List<PilotGun> pilotGuns = new ArrayList<>();
    protected int reloadTimePrimary;
    protected int reloadTimeSecondary;
    protected String reloadSoundPrimary = StringUtils.EMPTY;
    protected String reloadSoundSecondary = StringUtils.EMPTY;
    protected int placeTimePrimary = 5;
    protected int placeTimeSecondary = 5;
    protected String placeSoundPrimary = StringUtils.EMPTY;
    protected String placeSoundSecondary = StringUtils.EMPTY;

    protected int numPassengers;
    protected final List<SeatInfo> seats = new ArrayList<>();
    protected int numPassengerGunners;
    protected float vehicleGunModelScale = 1F;
    protected boolean filterAmmunition;
    protected boolean worksUnderWater;

    public record ShootParticle(String name, float x, float y, float z) {}
    protected final List<ShootParticle> shootParticlesPrimary = new ArrayList<>();
    protected final List<ShootParticle> shootParticlesSecondary = new ArrayList<>();

    protected int numCargoSlots;
    protected int numBombSlots;
    protected int numMissileSlots;
    protected int fuelTankSize = 100;
    protected float yOffset = 10F / 16F;
    protected float cameraDistance = 5F;
    protected final List<ParticleEmitter> emitters = new ArrayList<>();
    protected boolean emittersRequireOccupant = true;

    protected float maxThrottle = 1F;
    protected float maxNegativeThrottle;
    protected float clutchBrake;
    protected Vector3f turretOrigin = new Vector3f();
    protected Vector3f turretOriginOffset = new Vector3f();
    protected final List<DriveablePosition> wheelPositions = new ArrayList<>();
    protected float wheelSpringStrength = 0.5F;
    protected float wheelStepHeight = 1F;
    /**
     * Vertical gap, in blocks, between the wheel anchors and the bottom of the
     * geometry that actually meets the ground, or NaN when the type declares no
     * wheel or track collision box to derive it from.
     *
     * <p>WheelPosition is only an anchor, and packs do not agree on where it
     * sits: the official content puts it on the contact plane, while others put
     * it an axle height above. The collision boxes of the wheel and track parts
     * do describe the geometry that touches the ground, so the gap between the
     * lowest of those and the lowest anchor is the clearance the suspension has
     * to keep for the rendered model to rest on the surface.</p>
     */
    protected float wheelContactClearance = Float.NaN;
    protected boolean canRoll = true;
    protected final List<DriveablePosition> collisionPoints = new ArrayList<>();
    protected float drag = 1F;

    protected boolean floatOnWater;
    protected boolean placeableOnLand = true;
    protected boolean placeableOnWater;
    protected boolean placeableOnSponge;
    protected float buoyancy = 0.0165F;
    protected float floatOffset;
    protected float bulletDetectionRadius = -1F;
    protected boolean onRadar;
    protected int animFrames = 2;

    protected int startSoundRange = 50;
    protected String startSound = StringUtils.EMPTY;
    protected int startSoundLength;
    protected int engineSoundRange = 50;
    protected String engineSound = StringUtils.EMPTY;
    protected int engineSoundLength;
    protected int backSoundRange = 50;
    protected String exitSound = StringUtils.EMPTY;
    protected int exitSoundLength = 50;
    protected String idleSound = StringUtils.EMPTY;
    protected int idleSoundLength = 50;
    protected String backSound = StringUtils.EMPTY;
    protected int backSoundLength;

    protected boolean collisionDamageEnable;
    protected float collisionDamageThrottle;
    protected float collisionDamageTimes;
    protected boolean canMountEntity;
    protected float bulletSpread;
    protected float bulletSpeed = 3F;
    protected boolean rangingGun;

    protected boolean explosionWhenDestroyed;
    protected float deathFireRadius;
    protected float deathExplosionRadius = 4F;
    protected float deathExplosionPower = 1F;
    protected boolean deathExplosionBreaksBlocks;
    protected float deathExplosionDamageVsLiving = 1F;
    protected float deathExplosionDamageVsPlayer = 1F;
    protected float deathExplosionDamageVsPlane = 1F;
    protected float deathExplosionDamageVsVehicle = 1F;

    protected String lockedOnSound = StringUtils.EMPTY;
    protected int soundTime;
    protected int canLockOnAngle = 10;
    protected int lockOnSoundTime = 60;
    protected String lockOnSound = StringUtils.EMPTY;
    protected int maxRangeLockOn = 500;
    protected int lockedOnSoundRange = 5;
    public String lockingOnSound = StringUtils.EMPTY;
    protected boolean lockOnToPlanes;
    protected boolean lockOnToVehicles;
    protected boolean lockOnToMechas;
    protected boolean lockOnToPlayers;
    protected boolean lockOnToLivings;

    protected boolean hasFlare;
    protected int flareDelay = 200;
    protected String flareSound = StringUtils.EMPTY;
    protected int timeFlareUsing = 1;
    protected float recoilTime = 5F;
    protected boolean fixedPrimaryFire;
    protected Vector3f primaryFireAngle = new Vector3f();
    protected boolean fixedSecondaryFire;
    protected Vector3f secondaryFireAngle = new Vector3f();
    protected boolean setPlayerInvisible;
    protected float maxThrottleInWater = 0.5F;
    protected int maxDepth = 3;
    protected final List<Vector3f> leftTrackPoints = new ArrayList<>();
    protected final List<Vector3f> rightTrackPoints = new ArrayList<>();
    protected float trackLinkLength;
    protected boolean IT1;
    protected final List<CollisionMesh> collisionMeshes = new ArrayList<>();
    protected boolean fancyCollision;
    private transient volatile DriveableCollisionProfile collisionProfile;

    /**
     * Optional real-world source data exactly as authored, in real-world units.
     * Empty for every definition that declares no {@code Real*} key, which is what
     * keeps such definitions on the legacy physics path.
     */
    protected RealWorldVehicleSpec realWorldSpec = RealWorldVehicleSpec.EMPTY;
    /**
     * Minecraft-scaled physics resolved from {@link #realWorldSpec}, existing
     * geometry and the legacy fields. Never null: runtime code branches on
     * {@link ResolvedVehiclePhysics#mode()} instead of null-checking.
     */
    protected ResolvedVehiclePhysics resolvedPhysics =
        ResolvedVehiclePhysics.legacy(EnumVehicleCategory.OTHER, EnumDriveType.RWD);

    /** Optional authored armour. Missing entries remain distinct from explicit zero plates. */
    protected VehicleArmorSpec armorSpec = VehicleArmorSpec.EMPTY;
    /** Immutable definition-time armour table used by projectile and explosion hits. */
    protected ResolvedVehicleArmor resolvedArmor = VehicleArmorResolver.resolve(VehicleArmorSpec.EMPTY, List.of());
    /** Explicit opt-in; false preserves authored HP exactly. */
    protected boolean useRealisticVehicleHealth;
    /** Final normalized or legacy health allocation exposed to UI/debug consumers. */
    protected VehicleHealthScaler.Result resolvedHealth =
        VehicleHealthScaler.resolve(false, null, Map.of(), ModCommonConfig.DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE);

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        engine = readValue("Engine", engine, file).trim();
        readSeats(file);
        readWheels(file);
        readPartsAndRecipes(file);
        readWeapons(file);
        readMovement(file);
        readSounds(file);
        readCollisionMeshes(file);
        readParticles(file);
        readRealWorldSpec(file);
        readArmorAndHealthSpec(file);
        finishDerivedValues();
    }

    /**
     * Reads the optional real-world keys. This touches no key that already
     * existed, so legacy parsing is unchanged, and a malformed optional value is
     * reported and dropped rather than aborting the content pack load.
     */
    private void readRealWorldSpec(TypeFile file)
    {
        RealWorldSpecReader.Result result = RealWorldSpecReader.read(file);
        realWorldSpec = result.spec();
        for (String warning : result.warnings())
            logError(warning, file);
    }

    private void readArmorAndHealthSpec(TypeFile file)
    {
        VehicleArmorSpecReader.Result armorResult = VehicleArmorSpecReader.read(file);
        armorSpec = armorResult.spec();
        for (String warning : armorResult.warnings())
            logError(warning, file);

        useRealisticVehicleHealth = readValue("UseRealisticVehicleHealth", false, file);
        if (useRealisticVehicleHealth && realWorldSpec.massKg() == null)
            logError("UseRealisticVehicleHealth requires a valid RealMassKg; authored hitbox health will be retained", file);
        if (useRealisticVehicleHealth && health.values().stream().noneMatch(box -> box != null && box.getHealth() > 0F))
            logError("UseRealisticVehicleHealth requires at least one positive hitbox health weight; authored health will be retained", file);
    }

    private void readSeats(TypeFile file)
    {
        List<String[]> passengerLines = readValuesInLines("Passenger", file).orElse(List.of());
        int largestId = passengerLines.stream().filter(values -> values != null && values.length > 0)
            .mapToInt(values -> parseInt(values[0], 0, "Passenger id", file)).max().orElse(0);
        int requestedPassengers = Math.max(0, readValue("NumPassengers", passengerLines.size(), file));
        requestedPassengers = Math.max(0, readValue("Passengers", requestedPassengers, file));
        numPassengers = Math.max(requestedPassengers, largestId);

        for (int i = 0; i <= numPassengers; i++)
            seats.add(null);

        for (String[] values : passengerLines)
        {
            if (values == null || values.length < 5)
            {
                logError("Passenger requires: id x y z part [minYaw maxYaw minPitch maxPitch [gunType gunName]]", file);
                continue;
            }
            try
            {
                int id = Integer.parseInt(values[0]);
                if (id <= 0 || id >= seats.size())
                {
                    logError("Passenger id " + id + " is outside 1.." + numPassengers, file);
                    continue;
                }
                float minYaw = values.length > 5 ? Float.parseFloat(values[5]) : -360F;
                float maxYaw = values.length > 6 ? Float.parseFloat(values[6]) : 360F;
                float minPitch = values.length > 7 ? Float.parseFloat(values[7]) : -89F;
                float maxPitch = values.length > 8 ? Float.parseFloat(values[8]) : 89F;
                String gunType = values.length > 9 ? values[9] : StringUtils.EMPTY;
                String gunName = values.length > 10 ? values[10] : StringUtils.EMPTY;
                SeatInfo seat = new SeatInfo(id, modelVector(values, 1), EnumDriveablePart.getPart(values[4]), false,
                    minYaw, maxYaw, minPitch, maxPitch, gunType, gunName, contentPack);
                if (StringUtils.isNotBlank(gunType))
                {
                    seat.setGunnerID(numPassengerGunners++);
                    driveableRecipe.add(RecipeIngredient.parse(gunType, 1, contentPack));
                }
                seats.set(id, seat);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse Passenger definition", file, ex);
            }
        }

        String driverPartName = readValue("DriverPart", "core", file);
        String driverGun = readValue("DriverGun", readValue("PilotGun", StringUtils.EMPTY, file), file);
        List<String[]> drivers = new ArrayList<>();
        drivers.addAll(readValuesInLines("Driver", file).orElse(List.of()));
        drivers.addAll(readValuesInLines("Pilot", file).orElse(List.of()));
        SeatInfo driver;
        if (drivers.isEmpty() || drivers.get(0) == null || drivers.get(0).length < 3)
        {
            logError("No valid Driver or Pilot definition; using model origin", file);
            driver = new SeatInfo(0, new Vector3f(), EnumDriveablePart.getPart(driverPartName), true,
                -360F, 360F, -89F, 89F, null, driverGun, contentPack);
        }
        else
        {
            String[] values = drivers.get(0);
            try
            {
                float minYaw = values.length > 3 ? Float.parseFloat(values[3]) : -360F;
                float maxYaw = values.length > 4 ? Float.parseFloat(values[4]) : 360F;
                float minPitch = values.length > 5 ? Float.parseFloat(values[5]) : -89F;
                float maxPitch = values.length > 6 ? Float.parseFloat(values[6]) : 89F;
                driver = new SeatInfo(0, modelVector(values, 0), EnumDriveablePart.getPart(driverPartName), true,
                    minYaw, maxYaw, minPitch, maxPitch, null, driverGun, contentPack);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse Driver/Pilot definition; using model origin", file, ex);
                driver = new SeatInfo(0, new Vector3f(), EnumDriveablePart.CORE, true,
                    -360F, 360F, -89F, 89F, null, driverGun, contentPack);
            }
        }
        seats.set(0, driver);

        driver.setGunOrigin(scaledVector("DriverGunOrigin", driver.getGunOrigin(), file));
        driver.setRotatedOffset(scaledVector("RotatedDriverOffset", driver.getRotatedOffset(), file));
        driver.setAimingSpeed(readVector("DriverAimSpeed", driver.getAimingSpeed(), file));
        driver.setLegacyAiming(readValue("DriverLegacyAiming", driver.isLegacyAiming(), file));
        driver.setYawBeforePitch(readValue("DriverYawBeforePitch", driver.isYawBeforePitch(), file));
        driver.setLatePitch(readValue("DriverLatePitch", driver.isLatePitch(), file));
        driver.setTraverseSounds(readValue("DriverTraverseSounds", driver.isTraverseSounds(), file));

        readSeatVectorLines("RotatedPassengerOffset", file, SeatInfo::setRotatedOffset, true);
        readSeatVectorLines("PassengerAimSpeed", file, SeatInfo::setAimingSpeed, false);
        readSeatVectorLines("GunOrigin", file, SeatInfo::setGunOrigin, true);
        readSeatBooleanLines("PassengerLegacyAiming", file, SeatInfo::setLegacyAiming);
        readSeatBooleanLines("PassengerYawBeforePitch", file, SeatInfo::setYawBeforePitch);
        readSeatBooleanLines("PassengerLatePitch", file, SeatInfo::setLatePitch);
        readSeatBooleanLines("PassengerTraverseSounds", file, SeatInfo::setTraverseSounds);
        readSeatIntLines("PassengerYawSoundLength", file, SeatInfo::setYawSoundLength);
        readSeatIntLines("PassengerPitchSoundLength", file, SeatInfo::setPitchSoundLength);
        readSeatStringLines("PassengerYawSound", file, SeatInfo::setYawSound);
        readSeatStringLines("PassengerPitchSound", file, SeatInfo::setPitchSound);
    }

    private void readWheels(TypeFile file)
    {
        List<String[]> lines = new ArrayList<>();
        lines.addAll(readValuesInLines("Wheel", file).orElse(List.of()));
        lines.addAll(readValuesInLines("WheelPosition", file).orElse(List.of()));
        int maxIndex = lines.stream().filter(v -> v != null && v.length > 0)
            .mapToInt(v -> parseInt(v[0], -1, "wheel index", file)).max().orElse(-1);
        for (int i = 0; i <= maxIndex; i++)
            wheelPositions.add(null);
        for (String[] values : lines)
        {
            if (values == null || values.length < 4)
                continue;
            try
            {
                int index = Integer.parseInt(values[0]);
                if (index < 0)
                    continue;
                while (wheelPositions.size() <= index)
                    wheelPositions.add(null);
                EnumDriveablePart part = values.length > 4 ? EnumDriveablePart.getPart(values[4]) : EnumDriveablePart.CORE_WHEEL;
                wheelPositions.set(index, new DriveablePosition(modelVector(values, 1), part));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse wheel definition", file, ex);
            }
        }
    }

    private void readPartsAndRecipes(TypeFile file)
    {
        readPartBoxes("SetupPart", file, 0F);
        readPartBoxes("SetupArmoredPart", file, 0F);
        readPartBoxes("SetupCrewedPart", file, 1F);
        readPartBoxes("SetupAnimalPart", file, 1F);
        readPartBoxes("SetupCompositeArmoredPart", file, 0F);
        readPartBoxes("SetuCrewedpPart", file, 1F);

        forEachLine("PartDeathExplosion", file, 4, values -> {
            EnumDriveablePart part = EnumDriveablePart.getPart(values[0]);
            if (part == null)
                return;
            float living = values.length > 4 ? Float.parseFloat(values[4]) : 1F;
            float player = values.length > 5 ? Float.parseFloat(values[5]) : living;
            float plane = values.length > 6 ? Float.parseFloat(values[6]) : 1F;
            float vehicle = values.length > 7 ? Float.parseFloat(values[7]) : plane;
            partDeathExplosions.put(part, new DriveableExplosion(Float.parseFloat(values[1]), Float.parseFloat(values[2]),
                parseBoolean(values[3]), living, player, plane, vehicle));
        });

        readPartRecipes("AddRecipeParts", file);
        readPartRecipes("AddRecipePart", file);
        readPartRecipes("AddRecipieParts", file);
        forEachLine("AddDye", file, 2, values -> {
            int amount = Integer.parseInt(values[0]);
            driveableRecipe.add(RecipeIngredient.parse("minecraft:" + normalizeDye(values[1]) + "_dye", amount, contentPack));
        });
    }

    private void addPartRecipe(String[] values, TypeFile file, String context)
    {
        EnumDriveablePart part = EnumDriveablePart.getPart(values[0]);
        List<RecipeIngredient> refs;
        if (values.length == 2 && !isInteger(values[1]))
            refs = List.of(RecipeIngredient.parse(values[1], 1, contentPack));
        else
            refs = RecipeParser.parseAmountThenItemReferences(values, 1, contentPack, file, context);
        driveableRecipe.addAll(refs);
        if (part == null)
        {
            logError("Unknown driveable part in " + context + ": " + values[0] + "; assigning ingredients to core", file);
            part = EnumDriveablePart.CORE;
        }
        partwiseRecipe.computeIfAbsent(part, ignored -> new ArrayList<>()).addAll(refs);
    }

    private void readPartRecipes(String key, TypeFile file)
    {
        for (String[] values : readValuesInLines(key, file).orElse(List.of()))
        {
            if (values.length == 0)
                continue;
            if (values.length == 1)
            {
                EnumDriveablePart part = EnumDriveablePart.getPart(values[0]);
                if (part != null)
                    partwiseRecipe.computeIfAbsent(part, ignored -> new ArrayList<>());
                continue;
            }
            try
            {
                addPartRecipe(values, file, key);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    private void readPartBoxes(String key, TypeFile file, float defaultCrewMultiplier)
    {
        forEachLine(key, file, 8, values -> {
            int start = values[0].equalsIgnoreCase(key) ? 1 : 0;
            if (values.length - start < 8)
            {
                logError(key + " expects part health x y z width height depth [resistance] [crew multiplier]", file);
                return;
            }
            EnumDriveablePart part = EnumDriveablePart.getPart(values[start]);
            if (part == null)
            {
                logError("Unknown driveable part in " + key + ": " + values[start], file);
                return;
            }
            float resistance = values.length > start + 8 ? parseLegacyFloat(values[start + 8]) : 5F;
            float crew = values.length > start + 9 ? parseLegacyFloat(values[start + 9]) : defaultCrewMultiplier;
            health.put(part, new CollisionBox(parseLegacyFloat(values[start + 1]), parseLegacyFloat(values[start + 2]),
                parseLegacyFloat(values[start + 3]), parseLegacyFloat(values[start + 4]), parseLegacyFloat(values[start + 5]),
                parseLegacyFloat(values[start + 6]), parseLegacyFloat(values[start + 7]), resistance, crew));
        });
    }

    private void readWeapons(TypeFile file)
    {
        resolvedAmmoTypes = null;
        acceptAllAmmo = readValue("AllowAllAmmo", acceptAllAmmo, file);
        acceptAllAmmo = readValue("AcceptAllAmmo", acceptAllAmmo, file);
        readLines("AddAmmo", file).ifPresent(lines -> lines.stream().filter(StringUtils::isNotBlank)
            .map(String::trim).forEach(ammo::add));
        ShootableType.readAmmoGroups(file, ammoGroups);
        ammoOverrides = readAmmoOverrides(file);

        primary = EnumWeaponType.parse(readOptionalValue("Primary", primary.name(), file), primary);
        secondary = EnumWeaponType.parse(readOptionalValue("Secondary", secondary.name(), file), secondary);
        damageMultiplierPrimary = readValue("DamageMultiplierPrimary", damageMultiplierPrimary, file);
        damageMultiplierPrimary = readValue("DamageModifierPrimary", damageMultiplierPrimary, file);
        damageMultiplierPrimary = readValue("DammageModifierPrimary", damageMultiplierPrimary, file);
        damageMultiplierSecondary = readValue("DamageMultiplierSecondary", damageMultiplierSecondary, file);
        damageMultiplierSecondary = readValue("DamageModifierSecondary", damageMultiplierSecondary, file);
        shootDelayPrimary = resolveShootDelay(file, shootDelayPrimary, "ShootDelayPrimarySeconds", "RoundsPerMinPrimary", "ShootDelayPrimary", "ShellDelay", "BombDelay");
        shootDelaySecondary = resolveShootDelay(file, shootDelaySecondary, "ShootDelaySecondarySeconds", "RoundsPerMinSecondary", "ShootDelaySecondary", "ShootDelay");
        readWeaponsFromGunTypes = readValue("ReadSecondaryWeaponFromGunType", readWeaponsFromGunTypes, file);
        readWeaponsFromGunTypes = readValue("ReadWeaponsFromGunTypes", readWeaponsFromGunTypes, file);
        placeTimePrimary = Math.max(0, readOptionalValue("PlaceTimePrimary", placeTimePrimary, file));
        placeTimeSecondary = Math.max(0, readOptionalValue("PlaceTimeSecondary", placeTimeSecondary, file));
        reloadTimePrimary = Math.max(0, readOptionalValue("ReloadTimePrimary", reloadTimePrimary, file));
        reloadTimeSecondary = Math.max(0, readOptionalValue("ReloadTimeSecondary", reloadTimeSecondary, file));
        alternatePrimary = readValue("AlternatePrimary", alternatePrimary, file);
        alternateSecondary = readValue("AlternateSecondary", alternateSecondary, file);
        modePrimary = EnumFireMode.getFireMode(readValue("ModePrimary", modePrimary.name(), file));
        modeSecondary = EnumFireMode.getFireMode(readValue("ModeSecondary", modeSecondary.name(), file));
        bulletSpeed = readValue("BulletSpeed", bulletSpeed, file);
        bulletSpread = readValue("BulletSpread", bulletSpread, file);
        rangingGun = readValue("RangingGun", rangingGun, file);
        recoilTime = Math.max(0F, readValue("RecoilTime", recoilTime, file));

        readShootPoints("ShootPointPrimary", shootPointsPrimary, file);
        readShootPoints("ShootPointSecondary", shootPointsSecondary, file);
        readShootParticles("ShootParticlesPrimary", shootParticlesPrimary, file);
        readShootParticles("ShootParticlesSecondary", shootParticlesSecondary, file);
        readShootParticles("ShootParticleSecondary", shootParticlesSecondary, file);
        readLegacyGuns(file);
        readLegacyWeaponPosition("BombPosition", EnumDriveablePart.CORE, EnumWeaponType.BOMB, file);
        readLegacyWeaponPosition("BarrelPosition", EnumDriveablePart.TURRET, EnumWeaponType.SHELL, file);

        setPlayerInvisible = readValue("SetPlayerInvisible", setPlayerInvisible, file);
        IT1 = readValue("IT1", IT1, file);
        fixedPrimaryFire = readValue("FixedPrimary", fixedPrimaryFire, file);
        fixedSecondaryFire = readValue("FixedSecondary", fixedSecondaryFire, file);
        primaryFireAngle = readVector("PrimaryAngle", primaryFireAngle, file);
        secondaryFireAngle = readVector("SecondaryAngle", secondaryFireAngle, file);
    }

    private void readMovement(TypeFile file)
    {
        vehicleGunModelScale = readValue("VehicleGunModelScale", vehicleGunModelScale, file);
        reloadSoundTick = readValue("VehicleGunReloadTick", reloadSoundTick, file);
        explosionWhenDestroyed = readValue("IsExplosionWhenDestroyed", explosionWhenDestroyed, file);
        deathFireRadius = aliasFloat(deathFireRadius, file, "DeathFireRadius", "DeathFire");
        deathExplosionRadius = aliasFloat(deathExplosionRadius, file, "DeathExplosionRadius", "DeathExplosion");
        deathExplosionPower = readValue("DeathExplosionPower", deathExplosionPower, file);
        deathExplosionBreaksBlocks = readValue("DeathExplosionBreaksBlocks", deathExplosionBreaksBlocks, file);
        deathExplosionDamageVsLiving = readValue("DeathExplosionDamageVsLiving", deathExplosionDamageVsLiving, file);
        deathExplosionDamageVsPlayer = readValue("DeathExplosionDamageVsPlayer", deathExplosionDamageVsPlayer, file);
        deathExplosionDamageVsPlane = readValue("DeathExplosionDamageVsPlane", deathExplosionDamageVsPlane, file);
        deathExplosionDamageVsVehicle = readValue("DeathExplosionDamageVsVehicle", deathExplosionDamageVsVehicle, file);
        fallDamageFactor = readValue("FallDamageFactor", fallDamageFactor, file);

        maxThrottle = readValue("MaxThrottle", maxThrottle, file);
        maxNegativeThrottle = readValue("MaxNegativeThrottle", maxNegativeThrottle, file);
        clutchBrake = readValue("ClutchBrake", clutchBrake, file);
        maxThrottleInWater = readFloatOrBoolean("MaxThrottleInWater", maxThrottleInWater, file);
        maxDepth = readValue("MaxDepth", maxDepth, file);
        drag = Math.max(0F, readValue("Drag", drag, file));
        turretOrigin = scaledVector("TurretOrigin", turretOrigin, file);
        turretOriginOffset = scaledVector("TurretOriginOffset", turretOriginOffset, file);

        readPositionLines("CollisionPoint", collisionPoints, file, false);
        readPositionLines("AddCollisionPoint", collisionPoints, file, false);
        readPositionLines("CollisoinPoint", collisionPoints, file, false);
        readPositionLines("ollisionPoint", collisionPoints, file, false);
        collisionDamageEnable = readValue("CollisionDamageEnable", collisionDamageEnable, file);
        collisionDamageThrottle = readValue("CollisionDamageThrottle", collisionDamageThrottle, file);
        collisionDamageTimes = readValue("CollisionDamageTimes", collisionDamageTimes, file);
        canLockOnAngle = readValue("CanLockAngle", canLockOnAngle, file);
        canLockOnAngle = readValue("CanLockOnAngle", canLockOnAngle, file);
        lockOnSoundTime = readValue("LockOnSoundTime", lockOnSoundTime, file);
        maxRangeLockOn = readValue("MaxRangeLockOn", maxRangeLockOn, file);
        boolean allDriveables = readValue("LockOnToDriveables", false, file);
        lockOnToVehicles = readValue("LockOnToVehicles", allDriveables || lockOnToVehicles, file);
        lockOnToPlanes = readValue("LockOnToPlanes", allDriveables || lockOnToPlanes, file);
        lockOnToMechas = readValue("LockOnToMechas", allDriveables || lockOnToMechas, file);
        lockOnToPlayers = readValue("LockOnToPlayers", lockOnToPlayers, file);
        lockOnToLivings = readValue("LockOnToLivings", lockOnToLivings, file);
        lockedOnSoundRange = readValue("LockedOnSoundRange", lockedOnSoundRange, file);
        canRoll = readValue("CanRoll", canRoll, file);

        hasFlare = readValue("HasFlare", hasFlare, file);
        flareDelay = Math.max(1, readValue("FlareDelay", flareDelay, file));
        timeFlareUsing = Math.max(1, readValue("TimeFlareUsing", timeFlareUsing, file));

        if (file.hasConfigLine("Boat"))
        {
            placeableOnLand = false;
            placeableOnWater = true;
            floatOnWater = true;
            wheelStepHeight = 0F;
        }
        placeableOnLand = readValue("PlaceableOnLand", placeableOnLand, file);
        placeableOnWater = readValue("PlaceableOnWater", placeableOnWater, file);
        placeableOnSponge = readValue("PlaceableOnSponge", placeableOnSponge, file);
        worksUnderWater = readValue("WorksUnderwater", worksUnderWater, file);
        worksUnderWater = readValue("WorksUnderWater", worksUnderWater, file);
        floatOnWater = readValue("FloatOnWater", floatOnWater, file);
        buoyancy = readValue("Buoyancy", buoyancy, file);
        floatOffset = readValue("FloatOffset", floatOffset, file);
        canMountEntity = readValue("CanMountEntity", canMountEntity, file);
        wheelStepHeight = aliasFloat(wheelStepHeight, file, "WheelRadius", "WheelStepHeight");
        wheelSpringStrength = aliasFloat(wheelSpringStrength, file, "WheelSpringStrength", "SpringStrength");
        animFrames = Math.max(1, readValue("TrackFrames", animFrames, file));

        harvestBlocks = readValue("Harvester", harvestBlocks, file);
        collectHarvest = readValue("CollectHarvest", collectHarvest, file);
        dropHarvest = readValue("DropHarvest", dropHarvest, file);
        readLines("HarvestMaterial", file).ifPresent(lines -> lines.stream().filter(StringUtils::isNotBlank)
            .map(value -> value.trim().toLowerCase(Locale.ROOT)).forEach(materialsHarvested::add));
        readLines("HarvestToolType", file).ifPresent(lines -> lines.stream().filter(StringUtils::isNotBlank)
            .forEach(this::addHarvestToolType));
        readValues("HarvestBox", file, 2).ifPresent(values -> {
            try
            {
                harvestBoxSize = parseVector(values[0], 1F / 16F);
                harvestBoxPos = parseVector(values[1], 1F / 16F);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse HarvestBox", file, ex);
            }
        });

        numCargoSlots = Math.max(0, readValue("CargoSlots", numCargoSlots, file));
        numBombSlots = Math.max(0, aliasInt(numBombSlots, file, "BombSlots", "MineSlots"));
        numMissileSlots = Math.max(0, aliasInt(numMissileSlots, file, "MissileSlots", "ShellSlots"));
        fuelTankSize = Math.max(0, readValue("FuelTankSize", fuelTankSize, file));
        engineStartTime = Math.max(0, readValue("EngineStartTime", engineStartTime, file));
        filterAmmunition = readValue("FilterAmmunitionInput", filterAmmunition, file);
        bulletDetectionRadius = readValue("BulletDetection", bulletDetectionRadius, file);
        yOffset = readValue("YOffset", yOffset, file);
        cameraDistance = Math.max(1F, readValue("CameraDistance", cameraDistance, file));
        onRadar = readValue("OnRadar", onRadar, file);
        trackLinkLength = readValue("TrackLinkLength", trackLinkLength, file);
        readVectorValueLines("LeftLinkPoint", leftTrackPoints, file, 1F);
        readVectorValueLines("RightLinkPoint", rightTrackPoints, file, 1F);
    }

    private void readSounds(TypeFile file)
    {
        startSoundRange = readValue("StartSoundRange", startSoundRange, file);
        startSoundLength = readSoundLength("StartSoundLength", startSoundLength, file);
        engineSoundRange = readValue("EngineSoundRange", engineSoundRange, file);
        engineSoundLength = readSoundLength("EngineSoundLength", engineSoundLength, file);
        idleSoundLength = readSoundLength("IdleSoundLength", idleSoundLength, file);
        exitSoundLength = readSoundLength("ExitSoundLength", exitSoundLength, file);
        backSoundRange = readValue("BackSoundRange", backSoundRange, file);
        backSoundLength = readSoundLength("BackSoundLength", backSoundLength, file);
        soundTime = readValue("SoundTime", soundTime, file);

        SeatInfo driver = getSeat(0);
        if (driver != null)
        {
            driver.setYawSoundLength(readSoundLength("YawSoundLength", driver.getYawSoundLength(), file));
            driver.setPitchSoundLength(readSoundLength("PitchSoundLength", driver.getPitchSoundLength(), file));
            driver.setYawSound(readSound("YawSound", driver.getYawSound(), file));
            driver.setPitchSound(readSound("PitchSound", driver.getPitchSound(), file));
        }
        startSound = readSound("StartSound", startSound, file);
        engineSound = readSound("EngineSound", engineSound, file);
        idleSound = readSound("IdleSound", idleSound, file);
        exitSound = readSound("ExitSound", exitSound, file);
        backSound = readSound("BackSound", backSound, file);
        shootSoundPrimary = aliasSound(shootSoundPrimary, file, "ShootMainSound", "BombSound", "ShootSoundPrimary", "ShellSound");
        shootReloadSound = readSound("ShootReloadSound", shootReloadSound, file);
        shootSoundSecondary = aliasSound(shootSoundSecondary, file, "ShootSecondarySound", "ShootSoundSecondary");
        placeSoundPrimary = readSound("PlaceSoundPrimary", placeSoundPrimary, file);
        placeSoundSecondary = readSound("PlaceSoundSecondary", placeSoundSecondary, file);
        reloadSoundPrimary = readSound("ReloadSoundPrimary", reloadSoundPrimary, file);
        reloadSoundSecondary = readSound("ReloadSoundSecondary", reloadSoundSecondary, file);
        lockedOnSound = readSound("LockedOnSound", lockedOnSound, file);
        lockOnSound = readSound("LockOnSound", lockOnSound, file);
        lockingOnSound = readSound("LockingOnSound", lockingOnSound, file);
        flareSound = readSound("FlareSound", flareSound, file);
    }

    private void readCollisionMeshes(TypeFile file)
    {
        collisionProfile = null;
        fancyCollision = readValue("FancyCollision", fancyCollision, file);
        readMeshLines("AddCollisionMesh", EnumDriveablePart.CORE, file);
        readMeshLines("AddTurretCollisionMesh", EnumDriveablePart.TURRET, file);
        readRawMeshLines("AddCollisionMeshRaw", EnumDriveablePart.CORE, file);
        readRawMeshLines("AddTurretCollisionMeshRaw", EnumDriveablePart.TURRET, file);
    }

    private void readParticles(TypeFile file)
    {
        emittersRequireOccupant = readValue("EmittersRequireOccupant", emittersRequireOccupant, file);
        Consumer<String[]> parser = values -> {
            Vector3f origin = parseVector(values[2], 1F / 16F);
            Vector3f extents = parseVector(values[3], 1F / 16F);
            Vector3f velocity = parseVector(values[4], 1F / 16F);
            emitters.add(new ParticleEmitter(values[0], Integer.parseInt(values[1]), origin, extents, velocity,
                Float.parseFloat(values[5]), Float.parseFloat(values[6]), Float.parseFloat(values[7]),
                Float.parseFloat(values[8]), EnumDriveablePart.getPart(values[9])));
        };
        forEachLine("AddParticle", file, 10, parser);
        forEachLine("AddEmitter", file, 10, parser);
    }

    /**
     * Finalization stage. Subclasses call this again after their own reads so
     * that physics resolution sees the complete definition; every step here is
     * idempotent, which is what makes the second call safe.
     */
    protected void finishDerivedValues()
    {
        if (authoredHealth.isEmpty() && !health.isEmpty())
            authoredHealth.putAll(health);
        if (!authoredHealth.isEmpty())
        {
            health.clear();
            health.putAll(authoredHealth);
        }
        if (bulletDetectionRadius < 0F)
        {
            bulletDetectionRadius = 0F;
            for (CollisionBox box : health.values())
                bulletDetectionRadius = Math.max(bulletDetectionRadius, box.getRootPosition().length() + box.getRadius());
            bulletDetectionRadius += 1F;
        }
        deriveWheelContactClearance();
        resolvedPhysics = VehiclePhysicsResolver.resolve(physicsCategory(), realWorldSpec,
            deriveGeometry(), legacyPhysicsHints());
        resolvedArmor = VehicleArmorResolver.resolve(armorSpec, authoredHealth.keySet());
        resolvedHealth = VehicleHealthScaler.resolve(useRealisticVehicleHealth, realWorldSpec.massKg(),
            authoredHealth, ModCommonConfig.realisticVehicleHealthScale());
        health.clear();
        health.putAll(resolvedHealth.boxes());
    }

    /**
     * Returns the maximum combined health represented by this driveable's parts.
     *
     * <p>Normalized health has an authoritative total before its per-part values
     * are rounded to floats. Legacy driveables instead define their total as the
     * sum of the authored health of every part.</p>
     */
    public float getTotalHp()
    {
        if (resolvedHealth != null && resolvedHealth.enabled())
            return resolvedHealth.totalHp();

        float total = 0F;
        for (CollisionBox box : health.values())
        {
            if (box != null)
                total += box.getHealth();
        }
        return total;
    }

    /** Which coupled real-world profile this type can qualify for. */
    protected EnumVehicleCategory physicsCategory()
    {
        return EnumVehicleCategory.OTHER;
    }

    /** The legacy fields the resolver needs in order to pick its fallbacks. */
    protected LegacyPhysicsHints legacyPhysicsHints()
    {
        return new LegacyPhysicsHints(false, false, maxNegativeThrottle, floatOnWater, false, false);
    }

    /**
     * Derives physical dimensions from data the definition already declares, so
     * no new parsing keys are needed for length, width, beam, wheelbase or track.
     * Length, width and height come from the core collision box; the wheelbase and
     * track come from the spread of the declared wheel positions, whose legacy X
     * is the fore-aft axis and legacy Z the lateral one.
     */
    /** Parts whose collision boxes describe where a driveable meets the ground. */
    private static final Set<EnumDriveablePart> GROUND_CONTACT_PARTS = EnumSet.of(
        EnumDriveablePart.CORE_WHEEL, EnumDriveablePart.FRONT_WHEEL, EnumDriveablePart.BACK_WHEEL,
        EnumDriveablePart.FRONT_LEFT_WHEEL, EnumDriveablePart.FRONT_RIGHT_WHEEL,
        EnumDriveablePart.BACK_LEFT_WHEEL, EnumDriveablePart.BACK_RIGHT_WHEEL,
        EnumDriveablePart.LEFT_TRACK, EnumDriveablePart.RIGHT_TRACK, EnumDriveablePart.TAIL_WHEEL,
        EnumDriveablePart.LEFT_WING_WHEEL, EnumDriveablePart.RIGHT_WING_WHEEL, EnumDriveablePart.SKIDS);

    /**
     * Measures {@link #wheelContactClearance} from the authored geometry.
     *
     * <p>The contact plane is the lower of the two things a type declares about
     * where it meets the ground: its wheel anchors and the bottoms of its wheel
     * and track boxes. Taking the lower of the two is what makes the measurement
     * safe against either one being authored loosely, since a box that stops
     * short of the anchors is a tight box rather than a driveable that hovers.
     * The upper bound then stops a single mis-authored box from levitating the
     * whole driveable.</p>
     */
    private void deriveWheelContactClearance()
    {
        float lowestBox = Float.NaN;
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : authoredHealth.entrySet())
        {
            if (!GROUND_CONTACT_PARTS.contains(entry.getKey()))
                continue;
            float bottom = entry.getValue().getY();
            if (Float.isNaN(lowestBox) || bottom < lowestBox)
                lowestBox = bottom;
        }
        float lowestAnchor = Float.NaN;
        for (DriveablePosition wheel : wheelPositions)
        {
            if (wheel == null)
                continue;
            float anchor = wheel.getPosition().y;
            if (Float.isNaN(lowestAnchor) || anchor < lowestAnchor)
                lowestAnchor = anchor;
        }
        wheelContactClearance = Float.isNaN(lowestBox) || Float.isNaN(lowestAnchor)
            ? Float.NaN : Math.max(0F, Math.min(0.5F, lowestAnchor - lowestBox));
    }

    private VehicleGeometry deriveGeometry()
    {
        int wheelCount = 0;
        for (DriveablePosition wheel : wheelPositions)
        {
            if (wheel != null)
                ++wheelCount;
        }
        Float wheelbase = null;
        Float trackWidth = null;
        if (wheelCount >= 2)
        {
            float[] forward = new float[wheelCount];
            float[] lateral = new float[wheelCount];
            int index = 0;
            for (DriveablePosition wheel : wheelPositions)
            {
                if (wheel == null)
                    continue;
                forward[index] = wheel.getPosition().x;
                lateral[index] = wheel.getPosition().z;
                ++index;
            }
            wheelbase = VehiclePhysicsResolver.deriveWheelbase(forward);
            trackWidth = VehiclePhysicsResolver.deriveTrackWidth(lateral);
        }

        CollisionBox core = health.get(EnumDriveablePart.CORE);
        if (core == null)
            return new VehicleGeometry(null, null, null, wheelbase, trackWidth);
        return VehicleGeometry.fromCoreBox(core.getWidth(), core.getHeight(), core.getDepth(), wheelbase, trackWidth);
    }

    public List<BulletType> getAmmoTypes()
    {
        int revision = ShootableType.getAmmoGroupRevision();
        List<BulletType> cached = resolvedAmmoTypes;
        if (cached != null && resolvedAmmoGroupRevision == revision)
            return cached;

        List<BulletType> result = new ArrayList<>();
        for (String shortName : ammo)
        {
            InfoType resolved = InfoType.getInfoType(shortName, contentPack);
            if (resolved instanceof BulletType bulletType)
                result.add(bulletType);
        }
        for (ShootableType ammoInGroup : ShootableType.findAmmoTypesInGroups(ammoGroups))
        {
            if (ammoInGroup instanceof BulletType bulletType && !result.contains(bulletType))
                result.add(bulletType);
        }
        cached = List.copyOf(result);
        resolvedAmmoTypes = cached;
        resolvedAmmoGroupRevision = revision;
        return cached;
    }

    public boolean isValidAmmo(@Nullable BulletType bulletType)
    {
        return bulletType != null && (acceptAllAmmo || getAmmoTypes().contains(bulletType));
    }

    public boolean isValidAmmo(@Nullable BulletType bulletType, @Nullable EnumWeaponType weaponType)
    {
        return isValidAmmo(bulletType) && weaponType != null && bulletType.getWeaponType() == weaponType;
    }

    public int getNumAmmoSlots()
    {
        return numPassengerGunners + pilotGuns.size();
    }

    public int ammoSlots()
    {
        return getNumAmmoSlots();
    }

    /**
     * Resolves the mounted gun fed by an ammo inventory slot. Legacy driveable
     * inventories store passenger guns first, ordered by their automatically
     * assigned gunner id, followed by the pilot guns.
     */
    @Nullable
    public GunType getGunTypeForAmmoSlot(int ammoSlot)
    {
        if (ammoSlot < 0 || ammoSlot >= getNumAmmoSlots())
            return null;
        if (ammoSlot < numPassengerGunners)
        {
            for (SeatInfo seat : seats)
            {
                if (seat != null && seat.getGunnerID() == ammoSlot)
                    return seat.getGunType();
            }
            return null;
        }

        int pilotGunIndex = ammoSlot - numPassengerGunners;
        return pilotGunIndex < pilotGuns.size() ? pilotGuns.get(pilotGunIndex).getType() : null;
    }

    /** The gun definition, rather than the vehicle AddAmmo list, owns this rule. */
    public boolean isValidGunAmmo(int ammoSlot, @Nullable ShootableType ammoType)
    {
        GunType gunType = getGunTypeForAmmoSlot(ammoSlot);
        return gunType != null && ammoType != null && gunType.getAmmoTypes().contains(ammoType);
    }

    @Nullable
    public SeatInfo getSeat(int id)
    {
        return id >= 0 && id < seats.size() ? seats.get(id) : null;
    }

    @Nullable
    public DriveablePosition getWheelPosition(int id)
    {
        return id >= 0 && id < wheelPositions.size() ? wheelPositions.get(id) : null;
    }

    public List<ShootPoint> shootPoints(boolean secondaryWeapon)
    {
        return secondaryWeapon ? Collections.unmodifiableList(shootPointsSecondary) : Collections.unmodifiableList(shootPointsPrimary);
    }

    public boolean alternate(boolean secondaryWeapon)
    {
        return secondaryWeapon ? alternateSecondary : alternatePrimary;
    }

    public float shootDelay(boolean secondaryWeapon)
    {
        if (readWeaponsFromGunTypes)
        {
            GunType gunType = getPilotGunType(secondaryWeapon);
            if (gunType != null)
                return gunType.getShootDelay(null);
        }
        return secondaryWeapon ? shootDelaySecondary : shootDelayPrimary;
    }

    /** The GunType referenced by the AddGun/PilotGun mount used for this weapon bank, if any. */
    @Nullable
    public GunType getPilotGunType(boolean secondaryWeapon)
    {
        for (ShootPoint point : shootPoints(secondaryWeapon))
        {
            if (point.getRootPos() instanceof PilotGun pilotGun)
            {
                GunType gunType = pilotGun.getType();
                if (gunType != null)
                    return gunType;
            }
        }
        return null;
    }

    public String shootSound(boolean secondaryWeapon)
    {
        return secondaryWeapon ? shootSoundSecondary : shootSoundPrimary;
    }

    public List<ShootParticle> shootParticle(boolean secondaryWeapon)
    {
        return Collections.unmodifiableList(secondaryWeapon ? shootParticlesSecondary : shootParticlesPrimary);
    }

    public int reloadTime(boolean secondaryWeapon)
    {
        return secondaryWeapon ? reloadTimeSecondary : reloadTimePrimary;
    }

    public EnumFireMode fireMode(boolean secondaryWeapon)
    {
        return secondaryWeapon ? modeSecondary : modePrimary;
    }

    public EnumWeaponType weaponType(boolean secondaryWeapon)
    {
        return secondaryWeapon ? secondary : primary;
    }

    public int numEngines()
    {
        return 1;
    }

    public int getNumEngines()
    {
        return numEngines();
    }

    public List<ItemStack> getItemsRequired(DriveablePart part, @Nullable PartType engine)
    {
        if (part == null)
            return List.of();
        List<ItemStack> stacks = resolveRecipe(partwiseRecipe.getOrDefault(part.getType(), List.of()));
        for (PilotGun gun : pilotGuns)
        {
            if (gun.getPart() == part.getType())
                ModUtils.getItemStack(gun.getType()).ifPresent(stacks::add);
        }
        for (SeatInfo seat : seats)
        {
            if (seat != null && seat.getPart() == part.getType())
                ModUtils.getItemStack(seat.getGunType()).ifPresent(stacks::add);
        }
        return stacks;
    }

    public float getRecommendedScale()
    {
        return 100F / Math.max(1F, cameraDistance);
    }

    /** Legacy-capitalized alias retained for model code and older extensions. */
    public float GetRecommendedScale()
    {
        return getRecommendedScale();
    }

    public List<ItemStack> getDriveableRecipe()
    {
        return resolveRecipe(driveableRecipe);
    }

    public void validateRecipeIngredients()
    {
        getDriveableRecipe();
        partwiseRecipe.values().forEach(this::resolveRecipe);
    }

    private List<ItemStack> resolveRecipe(List<RecipeIngredient> refs)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (RecipeIngredient ref : refs)
        {
            ItemStack stack = ref.resolve();
            if (!stack.isEmpty())
                stacks.add(stack);
        }
        return stacks;
    }

    private void readShootPoints(String key, List<ShootPoint> destination, TypeFile file)
    {
        for (String[] values : readValuesInLines(key, file).orElse(List.of()))
        {
            if (values.length == 0)
                continue;
            if (values.length < 3)
            {
                logError(key + " expects at least x y z", file);
                continue;
            }
            try
            {
                Vector3f root = modelVector(values, 0);
                int cursor = 3;
                EnumDriveablePart part = EnumDriveablePart.CORE;
                if (cursor < values.length)
                {
                    EnumDriveablePart parsedPart = EnumDriveablePart.getPart(values[cursor]);
                    if (parsedPart != null)
                    {
                        part = parsedPart;
                        cursor++;
                    }
                }

                DriveablePosition rootPosition;
                if (cursor < values.length && !isFloat(values[cursor]))
                {
                    String gunName = values[cursor++];
                    PilotGun gun = new PilotGun(root, part, gunName, contentPack);
                    pilotGuns.add(gun);
                    rootPosition = gun;
                    driveableRecipe.add(RecipeIngredient.parse(gunName, 1, contentPack));
                }
                else
                    rootPosition = new DriveablePosition(root, part);

                Vector3f offset = values.length >= cursor + 3 ? modelVector(values, cursor) : new Vector3f();
                destination.add(new ShootPoint(rootPosition, offset));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    private void readLegacyGuns(TypeFile file)
    {
        for (String[] values : readValuesInLines("AddGun", file).orElse(List.of()))
        {
            if (values.length == 0)
                continue;
            try
            {
                Vector3f position;
                EnumDriveablePart part;
                String gunName;
                Vector3f offset = new Vector3f();
                if (values.length == 1)
                {
                    position = new Vector3f();
                    part = EnumDriveablePart.CORE;
                    gunName = values[0];
                }
                else if (values.length >= 5)
                {
                    position = modelVector(values, 0);
                    part = EnumDriveablePart.getPart(values[3]);
                    gunName = values[4];
                    if (values.length >= 8)
                        offset = modelVector(values, 5);
                }
                else
                {
                    logError("AddGun expects either a gun name or x y z part gun [offset x y z]", file);
                    continue;
                }
                PilotGun gun = new PilotGun(position, part, gunName, contentPack);
                pilotGuns.add(gun);
                shootPointsSecondary.add(new ShootPoint(gun, offset));
                driveableRecipe.add(RecipeIngredient.parse(gunName, 1, contentPack));
                secondary = EnumWeaponType.GUN;
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse AddGun", file, ex);
            }
        }
    }

    private void readShootParticles(String key, List<ShootParticle> destination, TypeFile file)
    {
        forEachLine(key, file, 4, values -> destination.add(new ShootParticle(values[0], Float.parseFloat(values[1]),
            Float.parseFloat(values[2]), Float.parseFloat(values[3]))));
    }

    private void readLegacyWeaponPosition(String key, EnumDriveablePart part, EnumWeaponType weapon, TypeFile file)
    {
        List<String[]> lines = readValuesInLines(key, file).orElse(List.of());
        if (!lines.isEmpty())
            primary = weapon;
        for (String[] values : lines)
        {
            if (values == null || values.length < 3)
                continue;
            try
            {
                Vector3f offset = values.length >= 6 ? modelVector(values, 3) : new Vector3f();
                shootPointsPrimary.add(new ShootPoint(new DriveablePosition(modelVector(values, 0), part), offset));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    private void readPositionLines(String key, List<DriveablePosition> destination, TypeFile file, boolean indexed)
    {
        forEachLine(key, file, indexed ? 5 : 4, values -> {
            int start = indexed ? 1 : 0;
            destination.add(new DriveablePosition(modelVector(values, start), EnumDriveablePart.getPart(values[start + 3])));
        });
    }

    private void readVectorValueLines(String key, List<Vector3f> destination, TypeFile file, float scale)
    {
        readLines(key, file).ifPresent(lines -> lines.stream().filter(StringUtils::isNotBlank).forEach(value -> {
            try
            {
                destination.add(parseVector(value, scale));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }));
    }

    private void readMeshLines(String key, EnumDriveablePart part, TypeFile file)
    {
        forEachLine(key, file, 10, values -> {
            Vector3f position = parseVector(values[0], 1F / 16F);
            Vector3f size = parseVector(values[1], 1F / 16F);
            List<Vector3f> vertices = new ArrayList<>(8);
            for (int i = 2; i < 10; i++)
                vertices.add(parseVector(values[i], 1F / 16F));
            collisionMeshes.add(new CollisionMesh(position, size, vertices, part));
        });
    }

    private void readRawMeshLines(String key, EnumDriveablePart part, TypeFile file)
    {
        // Some generators include an unused compatibility token after size,
        // while documented raw lines contain exactly 30 numeric values.
        forEachLine(key, file, 30, values -> {
            Vector3f position = modelVector(values, 0);
            Vector3f size = modelVector(values, 3);
            int vertexStart = values.length >= 31 ? 7 : 6;
            List<Vector3f> vertices = new ArrayList<>(8);
            for (int i = 0; i < 8 && vertexStart + i * 3 + 2 < values.length; i++)
                vertices.add(modelVector(values, vertexStart + i * 3));
            if (vertices.size() == 8)
                collisionMeshes.add(new CollisionMesh(position, size, vertices, part));
        });
    }

    /** Lazily compiled once per immutable content type and shared by entities. */
    public DriveableCollisionProfile getCollisionProfile()
    {
        DriveableCollisionProfile cached = collisionProfile;
        if (cached == null)
        {
            synchronized (this)
            {
                cached = collisionProfile;
                if (cached == null)
                    collisionProfile = cached = DriveableCollisionProfile.compile(this);
            }
        }
        return cached;
    }

    private void readSeatVectorLines(String key, TypeFile file, SeatVectorSetter setter, boolean modelUnits)
    {
        int minimumValues = 4;
        forEachLine(key, file, minimumValues, values -> {
            SeatInfo seat = getSeat(Integer.parseInt(values[0]));
            if (seat != null)
            {
                Vector3f vector = new Vector3f(parseLegacyFloat(values[1]), parseLegacyFloat(values[2]),
                    values.length > 3 ? parseLegacyFloat(values[3]) : 0F);
                if (modelUnits)
                    vector.scale(1F / 16F);
                setter.set(seat, vector);
            }
        });
    }

    private void readSeatBooleanLines(String key, TypeFile file, SeatBooleanSetter setter)
    {
        forEachLine(key, file, 2, values -> {
            SeatInfo seat = getSeat(Integer.parseInt(values[0]));
            if (seat != null)
                setter.set(seat, parseBoolean(values[1]));
        });
    }

    private void readSeatIntLines(String key, TypeFile file, SeatIntSetter setter)
    {
        forEachLine(key, file, 2, values -> {
            SeatInfo seat = getSeat(Integer.parseInt(values[0]));
            if (seat != null)
                setter.set(seat, Integer.parseInt(values[1]));
        });
    }

    private void readSeatStringLines(String key, TypeFile file, SeatStringSetter setter)
    {
        forEachLine(key, file, 2, values -> {
            SeatInfo seat = getSeat(Integer.parseInt(values[0]));
            if (seat != null)
                setter.set(seat, values[1]);
        });
    }

    private void forEachLine(String key, TypeFile file, int minimumValues, Consumer<String[]> consumer)
    {
        for (String[] values : readValuesInLines(key, file).orElse(List.of()))
        {
            // Empty repeated directives occur in several established packs as
            // placeholders. They carry no data and are equivalent to omission.
            if (values == null || values.length == 0)
                continue;
            if (values.length < minimumValues)
            {
                logError(key + " expects at least " + minimumValues + " values", file);
                continue;
            }
            try
            {
                consumer.accept(values);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse " + key, file, ex);
            }
        }
    }

    private static Vector3f rawVector(String[] values, int start)
    {
        return new Vector3f(parseLegacyFloat(values[start]), parseLegacyFloat(values[start + 1]), parseLegacyFloat(values[start + 2]));
    }

    private static Vector3f modelVector(String[] values, int start)
    {
        return rawVector(values, start).scale(1F / 16F);
    }

    private static Vector3f parseVector(String raw, float scale)
    {
        String cleaned = raw.trim().replace('[', ' ').replace(']', ' ').replace(',', ' ');
        String[] values = cleaned.trim().split("\\s+");
        if (values.length < 3)
            throw new IllegalArgumentException("Expected three vector values");
        return new Vector3f(Float.parseFloat(values[0]) * scale, Float.parseFloat(values[1]) * scale, Float.parseFloat(values[2]) * scale);
    }

    private static boolean parseBoolean(String raw)
    {
        return "1".equals(raw) || Boolean.parseBoolean(raw);
    }

    private static float readFloatOrBoolean(String key, float fallback, TypeFile file)
    {
        String raw = readValue(key, (String) null, file);
        if (raw == null)
            return fallback;
        if (raw.equalsIgnoreCase("true"))
            return 1F;
        if (raw.equalsIgnoreCase("false"))
            return 0F;
        try
        {
            return parseLegacyFloat(raw);
        }
        catch (RuntimeException ex)
        {
            logError("Invalid " + key + ": " + raw, file);
            return fallback;
        }
    }

    private static boolean isFloat(String raw)
    {
        try
        {
            parseLegacyFloat(raw);
            return true;
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private static boolean isInteger(String raw)
    {
        try
        {
            Integer.parseInt(raw);
            return true;
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private static float parseLegacyFloat(String raw)
    {
        String value = raw.trim();
        if (value.indexOf(',') == value.lastIndexOf(',') && value.indexOf(',') > 0 && value.indexOf('.') < 0)
            value = value.replace(',', '.');
        return Float.parseFloat(value);
    }

    private static int parseInt(String raw, int fallback, String context, TypeFile file)
    {
        try
        {
            return Integer.parseInt(raw);
        }
        catch (RuntimeException ex)
        {
            logError("Invalid " + context + ": " + raw, file);
            return fallback;
        }
    }

    private static String normalizeDye(String raw)
    {
        return raw.trim().toLowerCase(Locale.ROOT).replace("lightblue", "light_blue")
            .replace("lightgray", "light_gray").replace("silver", "light_gray");
    }

    /**
     * Maps the old material-based harvesting presets onto stable 1.20 block and
     * mineable-tag name fragments. Keeping the generic tool fragment as well
     * makes this compatible with modded {@code mineable/*} tags.
     */
    private void addHarvestToolType(String raw)
    {
        String tool = raw.trim().toLowerCase(Locale.ROOT);
        switch (tool)
        {
            case "axe" -> Collections.addAll(materialsHarvested,
                "axe", "wood", "log", "plank", "plant", "vine");
            case "pickaxe", "drill" -> Collections.addAll(materialsHarvested,
                "pickaxe", "stone", "ore", "iron", "anvil", "rock");
            case "spade", "shovel", "excavator" -> Collections.addAll(materialsHarvested,
                "shovel", "dirt", "grass", "sand", "gravel", "snow", "clay", "ground");
            case "hoe", "combine" -> Collections.addAll(materialsHarvested,
                "hoe", "crop", "plant", "leaves", "vine", "cactus", "pumpkin", "melon", "gourd");
            case "tank" -> Collections.addAll(materialsHarvested,
                "axe", "wood", "log", "plank", "plant", "leaves", "cactus");
            default -> materialsHarvested.add(tool);
        }
    }

    private Vector3f scaledVector(String key, Vector3f fallback, TypeFile file)
    {
        Vector3f vector = readVector(key, null, file);
        return vector == null ? fallback : vector.scale(1F / 16F);
    }

    /**
     * Resolves a weapon bank's shoot delay under a deliberate, descending
     * precedence: an explicit delay in seconds first, then the rounds-per-minute
     * rate, then the bank's own delay key, then the legacy delay keys it
     * inherited. The first key actually present in the file wins, so a pack
     * that carries several of them for backwards compatibility still gets the
     * reading it intends.
     *
     * <p>{@link #aliasFloat} cannot express this, because it lets the
     * <em>last</em> key present win.
     */
    private static float resolveShootDelay(TypeFile file, float current, String secondsKey, String roundsPerMinKey, String... delayKeys)
    {
        Float seconds = readOptionalFloat(secondsKey, file);
        if (seconds != null)
            return Math.max(1F, seconds * 20F);
        Float roundsPerMin = readOptionalFloat(roundsPerMinKey, file);
        if (roundsPerMin != null)
            return delayFromRoundsPerMin(roundsPerMin);
        for (String key : delayKeys)
        {
            Float delay = readOptionalFloat(key, file);
            if (delay != null)
                return Math.max(1F, delay);
        }
        return current >= 0F ? current : delayFromRoundsPerMin(DEFAULT_ROUNDS_PER_MIN);
    }

    /** A number of legacy vehicle definitions declare unused delay fields with no value. */
    private static Float readOptionalFloat(String key, TypeFile file)
    {
        return hasValueForConfigField(key, file) ? readFloat(key, file) : null;
    }

    private static float delayFromRoundsPerMin(float roundsPerMin)
    {
        return Math.max(1F, 1200F / Math.max(1F, roundsPerMin));
    }

    private static float aliasFloat(float fallback, TypeFile file, String... keys)
    {
        float result = fallback;
        for (String key : keys)
            result = readOptionalValue(key, result, file);
        return result;
    }

    private static int aliasInt(int fallback, TypeFile file, String... keys)
    {
        int result = fallback;
        for (String key : keys)
            result = readValue(key, result, file);
        return result;
    }

    private static String aliasSound(String fallback, TypeFile file, String... keys)
    {
        String result = fallback;
        for (String key : keys)
            result = readSound(key, result, file);
        return result;
    }

    @FunctionalInterface private interface SeatVectorSetter { void set(SeatInfo seat, Vector3f value); }
    @FunctionalInterface private interface SeatBooleanSetter { void set(SeatInfo seat, boolean value); }
    @FunctionalInterface private interface SeatIntSetter { void set(SeatInfo seat, int value); }
    @FunctionalInterface private interface SeatStringSetter { void set(SeatInfo seat, String value); }

    @Override
    public void addLoot(LootTableLoadEvent event)
    {
        // Driveables are intentionally excluded from generic dungeon loot.
    }
}
