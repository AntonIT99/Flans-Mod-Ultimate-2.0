package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.IAAGunType;
import com.flansmodultimate.common.driveables.armor.VehicleHealthScaler;
import com.flansmodultimate.common.driveables.physics.RealWorldSpecReader;
import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics;
import com.flansmodultimate.common.guns.AmmoOverrides;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.RemovedAmmo;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.ShotCooldown;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class AAGunType extends InfoType implements IAAGunType, IAmmoGroupUser, IAmmoOverrideUser
{
    public static final int MAX_BARRELS = 16;
    public static final float DEFAULT_HIT_BOX_SIZE = 2F;
    public static final float MIN_HIT_BOX_SIZE = 0.0625F;

    /** The ammo types used by this gun */
    protected Set<String> ammo = new LinkedHashSet<>();
    /**
     * Ammo groups pulled in with "UseAmmoGroup". Every ammo item declaring "AddToAmmoGroup" with one of these
     * names is usable in this gun, exactly as if it had been listed individually.
     */
    protected Set<String> ammoGroups = new LinkedHashSet<>();
    /** Per-ammunition statistic overrides declared by this AA gun. */
    @Getter
    protected AmmoOverrides ammoOverrides = AmmoOverrides.EMPTY;
    /** Ammunition this weapon explicitly refuses; applied after every other ammunition source. */
    @Getter
    protected RemovedAmmo removedAmmo = RemovedAmmo.EMPTY;
    protected int reloadTime;
    protected float recoil = 5F;
    protected float bulletSpread;
    protected boolean readDispersion;
    protected float damage;
    protected float shootDelay;
    protected float roundsPerMin;
    protected int shootSoundLength;
    protected int numBullets = 1;
    protected int numBarrels = 1;
    protected boolean fireAlternately;
    protected int health;
    protected Float realMassKg;
    protected boolean useRealisticVehicleHealth;
    protected boolean realisticVehicleHealthEnabled;
    protected int gunnerX;
    protected int gunnerY;
    protected int gunnerZ;
    protected String shootSound = StringUtils.EMPTY;
    protected String reloadSound = StringUtils.EMPTY;
    protected int gunSoundRange = -1;
    protected int reloadSoundRange = -1;
    protected float topViewLimit = 75F;
    protected float bottomViewLimit = 0F;
    protected float sideViewLimit = 180F;
    /** Maximum yaw and pitch change in degrees per second; zero keeps legacy instant aim. */
    protected float traverseSpeed;
    protected float[] barrelX = new float[] { 0F };
    protected float[] barrelY = new float[] { 0F };
    protected float[] barrelZ = new float[] { 0F };
    /** Which barrels a {@code Barrel} line places, rather than leaving them at the 0 0 0 default. */
    @Getter(lombok.AccessLevel.NONE)
    protected boolean[] barrelLineAuthored = new boolean[] { false };
    /** Width and height, in blocks, of the entity box that is hit, picked and collided with. */
    protected float hitBoxWidth = DEFAULT_HIT_BOX_SIZE;
    protected float hitBoxHeight = DEFAULT_HIT_BOX_SIZE;

    /** Sentry mode. If target players is true then it either targets everyone on the other team, or everyone other than the owner when not playing with teams */
    protected boolean targetMobs;
    protected boolean targetPlayers;
    protected boolean targetVehicles;
    protected boolean targetPlanes;
    protected boolean targetMechas;
    /** Targeting radius */
    protected float targetRange = 10F;
    /** If true, then all barrels share the same ammo slot */
    protected boolean shareAmmo;

    protected boolean canShootHomingMissile;
    protected int countExplodeAfterShoot = -1;
    protected boolean dropThis = true;
    protected EnumSpreadPattern spreadPattern = EnumSpreadPattern.CIRCLE;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        damage = readValue("Damage", damage, file);
        reloadTime = readValue("ReloadTime", reloadTime, file);
        recoil = readValue("Recoil", recoil, file);
        bulletSpread = readValue("Accuracy", bulletSpread, file);
        bulletSpread = readValue("Spread", bulletSpread, file);
        if (hasValueForConfigField("Dispersion", file))
        {
            bulletSpread = readValue("Dispersion", 0F, file) * Mth.DEG_TO_RAD / ShootingHelper.ANGULAR_SPREAD_FACTOR;
            readDispersion = true;
        }
        shootDelay = readValue("ShootDelay", shootDelay, file);
        roundsPerMin = readValue("RoundsPerMin", roundsPerMin, file);
        shootSoundLength = readValue("SoundLength", shootSoundLength, file);
        shootSoundLength = readValue("ShootSoundLength", shootSoundLength, file);
        fireAlternately = readValue("FireAlternately", fireAlternately, file);
        health = readValue("Health", health, file);
        topViewLimit = readValue("TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue("BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue("SideViewLimit", sideViewLimit, file);
        float parsedTraverseSpeed = readValue("TraverseSpeed", traverseSpeed, file);
        if (Float.isFinite(parsedTraverseSpeed) && parsedTraverseSpeed >= 0F)
            traverseSpeed = parsedTraverseSpeed;
        else
            logError("TraverseSpeed must be a finite, non-negative number of degrees per second", file);
        spreadPattern = readValue("SpreadPattern", spreadPattern, EnumSpreadPattern.class, file);
        numBullets = readValue("NumBullets", numBullets, file);

        targetMobs = readValue("TargetMobs", targetMobs, file);
        targetPlayers = readValue("TargetPlayers", targetPlayers, file);
        targetVehicles = readValue("TargetVehicles", targetVehicles, file);
        targetPlanes = readValue("TargetPlanes", targetPlanes, file);
        targetMechas = readValue("TargetMechas", targetMechas, file);
        if (file.hasConfigLine("TargetDriveables"))
        {
            boolean targetDriveables = readValue("TargetDriveables", false, file);
            targetVehicles = targetDriveables;
            targetPlanes = targetDriveables;
            targetMechas = targetDriveables;
        }

        shareAmmo = readValue("ShareAmmo", shareAmmo, file);
        targetRange = readValue("TargetRange", targetRange, file);
        canShootHomingMissile = readValue("CanShootHomingMissile", canShootHomingMissile, file);
        countExplodeAfterShoot = readValue("CountExplodeAfterShoot", countExplodeAfterShoot, file);
        dropThis = readValue("IsDropThis", dropThis, file);

        shootSound = readSound("ShootSound", shootSound, file);
        reloadSound = readSound("ReloadSound", reloadSound, file);
        gunSoundRange = readValue("GunSoundRange", gunSoundRange, file);
        reloadSoundRange = readValue("ReloadSoundRange", reloadSoundRange, file);

        registerSoundTimer("ShootSoundLength", () -> shootSound, () -> shootSoundLength, length -> shootSoundLength = length);

        numBarrels = Math.max(1, Math.min(MAX_BARRELS, readValue("NumBarrels", numBarrels, file)));
        barrelX = new float[numBarrels];
        barrelY = new float[numBarrels];
        barrelZ = new float[numBarrels];
        barrelLineAuthored = new boolean[numBarrels];
        readBarrels(file);
        readLines("Ammo", file).ifPresent(lines -> lines.forEach(ammoLine -> ammo.add(ResourceUtils.sanitize(ammoLine))));
        ShootableType.readAmmoGroups(file, ammoGroups);
        ammoOverrides = readAmmoOverrides(file);
        removedAmmo = RemovedAmmo.read(file);
        readGunnerPosition(file);
        readHitBox(file);
        resolveRealisticHealth(file);
    }

    private void resolveRealisticHealth(TypeFile file)
    {
        RealWorldSpecReader.Result specResult = RealWorldSpecReader.read(file);
        realMassKg = specResult.spec().massKg();
        for (String warning : specResult.warnings())
            logError(warning, file);

        useRealisticVehicleHealth = readValue("UseRealisticVehicleHealth", false, file);
        VehicleHealthScaler.SingleResult result = VehicleHealthScaler.resolveSingle(
            useRealisticVehicleHealth, realMassKg, health, ModCommonConfig.realisticVehicleHealthScale());
        for (String warning : result.warnings())
            logError(warning, file);
        realisticVehicleHealthEnabled = result.enabled();
        if (result.enabled())
            health = Math.max(1, Math.round(result.health()));
    }

    /** Mass outside pushes and collisions are weighed against: RealMassKg, else the configured AA gun fallback. */
    public VehicleImpulsePhysics.ImpulseMass getImpulseMass()
    {
        return VehicleImpulsePhysics.resolveMass(realMassKg, null, ModCommonConfig.fallbackAAGunMassKg());
    }

    private void readBarrels(TypeFile file)
    {
        // 1.7.10 read whole pixels; fractions are accepted so a line can match a
        // measured muzzle, which rarely falls on the legacy transform's pixel grid.
        readFloatValuesInLines("Barrel", file, 4).ifPresent(lines -> lines.stream()
            .filter(values -> values != null && values.length >= 4)
            .forEach(values -> {
                int id = (int) values[0];
                if (id != values[0] || id < 0 || id >= numBarrels)
                {
                    logError("Barrel index " + values[0] + " is not a barrel below NumBarrels " + numBarrels, file);
                    return;
                }
                barrelX[id] = values[1];
                barrelY[id] = values[2];
                barrelZ[id] = values[3];
                barrelLineAuthored[id] = true;
            }));
    }

    /** Barrel lines as authored, kept while {@code /flandebug} overrides them, by barrel index. */
    @Getter(lombok.AccessLevel.NONE)
    private final Map<Integer, float[]> authoredBarrels = new TreeMap<>();

    /**
     * Moves one barrel's type-file line for the shoot-point debug command,
     * rounded to the hundredth of a pixel the command prints.
     */
    public boolean setDebugBarrel(int barrel, Vector3f legacyPixels)
    {
        if (barrel < 0 || barrel >= numBarrels)
            return false;
        authoredBarrels.computeIfAbsent(barrel, ignored -> new float[] { barrelX[barrel], barrelY[barrel], barrelZ[barrel] });
        barrelX[barrel] = roundBarrelPixels(legacyPixels.x);
        barrelY[barrel] = roundBarrelPixels(legacyPixels.y);
        barrelZ[barrel] = roundBarrelPixels(legacyPixels.z);
        return true;
    }

    /** Barrel line values are kept to the hundredth of a model pixel. */
    public static float roundBarrelPixels(float pixels)
    {
        return Math.round(pixels * 100F) / 100F;
    }

    public boolean isBarrelOverridden(int barrel)
    {
        return authoredBarrels.containsKey(barrel);
    }

    public boolean hasDebugOverrides()
    {
        return !authoredBarrels.isEmpty();
    }

    public void resetDebugOverrides()
    {
        authoredBarrels.forEach((barrel, line) -> {
            barrelX[barrel] = line[0];
            barrelY[barrel] = line[1];
            barrelZ[barrel] = line[2];
        });
        authoredBarrels.clear();
    }

    /**
     * Barrel pivots and muzzles measured off the model while the content was
     * loading, in model pixels as {@code ModelAAGun} reports them, or empty when
     * the model could not be measured. They stand in for the ones a client
     * reports, so the server places the muzzles on its own authority: a barrel
     * fires from the measured muzzle unless {@link #firesFromBarrelLine} holds, and
     * then from its line, elevated round the measured pivot.
     */
    @Getter(lombok.AccessLevel.NONE)
    private Vec3[] measuredBarrelPivots = new Vec3[0];
    @Getter(lombok.AccessLevel.NONE)
    private Vec3[] measuredBarrelMuzzles = new Vec3[0];
    /**
     * Whether the {@code Barrel} lines are trusted over the measured muzzles:
     * set for the packs a mod ships, whose definitions match their models, and
     * for every pack when overriding configured shoot points is switched off.
     */
    @Getter(lombok.AccessLevel.NONE)
    private boolean trustBarrelLines;

    public void setTrustBarrelLines(boolean trust)
    {
        trustBarrelLines = trust;
    }

    /**
     * Whether a barrel fires from its {@code Barrel} line rather than the model's
     * muzzle. A trusted pack that gives a barrel no line would otherwise fire it
     * from the gun's feet, so that barrel still takes the model's muzzle.
     */
    public boolean firesFromBarrelLine(int barrel)
    {
        return trustBarrelLines && barrel >= 0 && barrel < barrelLineAuthored.length && barrelLineAuthored[barrel];
    }

    /** Keeps the measured barrels, or clears them unless there is one pivot and one muzzle per barrel. */
    public void setMeasuredBarrels(Vec3[] pivots, Vec3[] muzzles)
    {
        boolean complete = pivots != null && muzzles != null && pivots.length == numBarrels && muzzles.length == numBarrels;
        measuredBarrelPivots = complete ? pivots.clone() : new Vec3[0];
        measuredBarrelMuzzles = complete ? muzzles.clone() : new Vec3[0];
    }

    public boolean hasMeasuredBarrels()
    {
        return measuredBarrelPivots.length > 0;
    }

    public Vec3[] getMeasuredBarrelPivots()
    {
        return measuredBarrelPivots.clone();
    }

    public Vec3[] getMeasuredBarrelMuzzles()
    {
        return measuredBarrelMuzzles.clone();
    }

    private void readHitBox(TypeFile file)
    {
        float size = readValue("HitBoxSize", DEFAULT_HIT_BOX_SIZE, file);
        hitBoxWidth = Math.max(MIN_HIT_BOX_SIZE, readValue("HitBoxWidth", size, file));
        hitBoxHeight = Math.max(MIN_HIT_BOX_SIZE, readValue("HitBoxHeight", size, file));
    }

    private void readGunnerPosition(TypeFile file)
    {
        readIntValues("GunnerPos", file, 3).ifPresent(values -> {
            gunnerX = values[0];
            gunnerY = values[1];
            gunnerZ = values[2];
        });
    }

    public int getAmmoSlotCount()
    {
        return shareAmmo ? 1 : numBarrels;
    }

    /**
     * {@code RoundsPerMin} overrides the legacy tick delay, matching {@link GunType}.
     *
     * <p>A gun declaring neither key falls back to one tick, which is the cadence
     * such a gun has always had: its delay of zero left it ready on every tick.
     * Naming it keeps the firing loop, which charges this value back onto the
     * cooldown, from being handed a delay of nothing.
     */
    public float getShootDelay()
    {
        return ShotCooldown.baseDelay(roundsPerMin, shootDelay, 1F);
    }

    public float getGunSoundRange()
    {
        return gunSoundRange > 0 ? gunSoundRange : ModCommonConfig.get().gunFireSoundRange();
    }

    public float getReloadSoundRange()
    {
        return reloadSoundRange > 0 ? reloadSoundRange : ModCommonConfig.get().soundRange();
    }

    public boolean isSentry()
    {
        return targetMobs || targetPlayers || targetVehicles || targetPlanes || targetMechas;
    }

    public boolean isAmmo(ShootableType type)
    {
        return getAmmoTypes().contains(type);
    }

    public boolean isAmmo(@Nullable ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ShootableItem shootableItem && isAmmo(shootableItem.getConfigType());
    }

    public List<ShootableType> getAmmoTypes()
    {
        List<ShootableType> ammoInGunType = ShootableType.findAmmoTypes(ammo, contentPack);
        List<ShootableType> ammoFromAdditionalMapping = ShootableType.getAdditionalAmmoMapping().getOrDefault(originalShortName, List.of());
        List<ShootableType> ammoFromGroups = ShootableType.findAmmoTypesInGroups(ammoGroups);
        List<ShootableType> ammoTypes = new ArrayList<>(ammoInGunType.size() + ammoFromAdditionalMapping.size() + ammoFromGroups.size());
        ammoTypes.addAll(ammoInGunType);
        ammoTypes.addAll(ammoFromAdditionalMapping);
        ammoFromGroups.stream().filter(ammoType -> !ammoTypes.contains(ammoType)).forEach(ammoTypes::add);
        // RemoveAmmo is applied last so it overrides Ammo, AddAmmo and every ammo group.
        if (!removedAmmo.isEmpty())
            ammoTypes.removeIf(ammoType -> removedAmmo.removes(ammoType.getOriginalShortName()));
        return ammoTypes;
    }

    public Optional<ShootableType> getDefaultAmmo()
    {
        if (!ammo.isEmpty())
            return ShootableType.findAmmoType(ammo.iterator().next(), contentPack);
        return getAmmoTypes().stream().findFirst();
    }

    public float getDamageForDisplay(ShootableType type)
    {
        return getDamageForDisplay(type, null);
    }

    public float getDamageForDisplay(ShootableType type, @Nullable Class<? extends Entity> entityClass)
    {
        if (type.useKineticDamageSystem())
        {
            float bulletSpeed = (type instanceof BulletType bulletType) ? bulletType.getBulletSpeed(true) : 1F;
            return ShootingHelper.getKineticDamage(type.getMass(), bulletSpeed);
        }
        else
            return type.getDamage().getDamageAgainstEntityClass(entityClass) * getDamage();
    }

    public float getDispersionForDisplay()
    {
        return Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * bulletSpread;
    }

    @Override
    public void addLoot(LootTableLoadEvent event)
    {
        // keep AA guns out of dungeon chests.
    }
}
