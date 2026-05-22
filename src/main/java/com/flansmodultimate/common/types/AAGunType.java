package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class AAGunType extends InfoType
{
    public static final int MAX_BARRELS = 16;

    protected Set<String> ammo = new LinkedHashSet<>();
    protected int reloadTime;
    protected float recoil = 5F;
    protected float accuracy;
    protected float damage;
    protected int shootDelay;
    protected int shootSoundLength;
    protected int numBullets = 1;
    protected int numBarrels = 1;
    protected boolean fireAlternately;
    protected int health;
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
    protected int[] barrelX = new int[] { 0 };
    protected int[] barrelY = new int[] { 0 };
    protected int[] barrelZ = new int[] { 0 };

    protected boolean targetMobs;
    protected boolean targetPlayers;
    protected boolean targetVehicles;
    protected boolean targetPlanes;
    protected boolean targetMechas;
    protected float targetRange = 10F;
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
        accuracy = readValue("Accuracy", accuracy, file);
        accuracy = readValue("Spread", accuracy, file);
        shootDelay = readValue("ShootDelay", shootDelay, file);
        shootSoundLength = readValue("SoundLength", shootSoundLength, file);
        shootSoundLength = readValue("ShootSoundLength", shootSoundLength, file);
        fireAlternately = readValue("FireAlternately", fireAlternately, file);
        health = readValue("Health", health, file);
        topViewLimit = readValue("TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue("BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue("SideViewLimit", sideViewLimit, file);
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

        numBarrels = Math.max(1, Math.min(MAX_BARRELS, readValue("NumBarrels", numBarrels, file)));
        barrelX = new int[numBarrels];
        barrelY = new int[numBarrels];
        barrelZ = new int[numBarrels];
        readBarrels(file);
        readAmmo(file);
        readGunnerPosition(file);
    }

    private void readBarrels(TypeFile file)
    {
        readIntValuesInLines("Barrel", file, 4).ifPresent(lines -> lines.stream()
            .filter(values -> values != null && values.length >= 4)
            .forEach(values -> {
                int id = values[0];
                if (id < 0 || id >= numBarrels)
                {
                    logError("Barrel index " + id + " is outside NumBarrels " + numBarrels, file);
                    return;
                }
                barrelX[id] = values[1];
                barrelY[id] = values[2];
                barrelZ[id] = values[3];
            }));
    }

    private void readAmmo(TypeFile file)
    {
        readLines("Ammo", file).ifPresent(lines -> lines.forEach(line -> {
            if (StringUtils.isBlank(line))
                return;

            String[] split = line.trim().split("\\s+");
            if (split.length > 0 && StringUtils.isNotBlank(split[0]))
                ammo.add(ResourceUtils.sanitize(split[0]));
        }));
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
        List<ShootableType> ammoTypes = new ArrayList<>(ammoInGunType.size() + ammoFromAdditionalMapping.size());
        ammoTypes.addAll(ammoInGunType);
        ammoTypes.addAll(ammoFromAdditionalMapping);
        return ammoTypes;
    }

    public Optional<ShootableType> getDefaultAmmo()
    {
        if (!ammo.isEmpty())
            return ShootableType.findAmmoType(ammo.iterator().next(), contentPack);
        return Optional.empty();
    }

    public float getDefaultBulletSpeed(BulletType bulletType)
    {
        return bulletType.getBulletSpeed() > 0F ? bulletType.getBulletSpeed() : bulletType.getSpeedMultiplier() * 3F;
    }

    public static AAGunType getAAGun(String shortName)
    {
        if (InfoType.getInfoType(shortName) instanceof AAGunType aaGunType)
            return aaGunType;
        return null;
    }
}
