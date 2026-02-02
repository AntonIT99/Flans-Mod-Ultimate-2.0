package com.flansmodultimate.config;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record CommonConfigSnapshot(
    int version,

    boolean addAllPaintjobsToCreative,

    boolean realisticRecoil,
    boolean enableSightDownwardMovement,
    int bonusRegenAmount,
    int bonusRegenTickDelay,
    double bonusRegenFoodLimit,

    double newDamageSystemReference,
    double headshotDamageModifier,
    double chestshotDamageModifier,
    double armshotDamageModifier,
    double legshotModifier,
    double vehicleWheelSeatExplosionModifier,

    int breakableArmor,
    int defaultArmorDurability,
    int defaultArmorEnchantability,
    boolean enableOldArmorRatioSystem,

    double gunDamageModifier,
    double gunRecoilModifier,
    double defaultADSSpreadMultiplier,
    double defaultADSSpreadMultiplierShotgun,
    boolean cancelReloadOnWeaponSwitch,
    boolean combineAmmoOnReload,
    boolean ammoToUpperInventoryOnReload,

    int shootableDefaultRespawnTime,
    boolean shootableProximityTriggerFriendlyFire,

    float soundRange,
    float gunFireSoundRange,
    float explosionSoundRange,

    boolean useNewPenetrationSystem,
    boolean enableBlockPenetration,
    double blockPenetrationModifier,

    List<String> penetrableBlocksLines
)
{
    public static final int CURRENT_VERSION = 1;

    public static CommonConfigSnapshot from(ModCommonConfig c)
    {
        return new CommonConfigSnapshot(
            CURRENT_VERSION,

            c.addAllPaintjobsToCreative,

            c.realisticRecoil,
            c.enableSightDownwardMovement,
            c.bonusRegenAmount,
            c.bonusRegenTickDelay,
            c.bonusRegenFoodLimit,

            c.newDamageSystemReference,
            c.headshotDamageModifier,
            c.chestshotDamageModifier,
            c.armshotDamageModifier,
            c.legshotModifier,
            c.vehicleWheelSeatExplosionModifier,

            c.breakableArmor,
            c.defaultArmorDurability,
            c.defaultArmorEnchantability,
            c.enableOldArmorRatioSystem,

            c.gunDamageModifier,
            c.gunRecoilModifier,
            c.defaultADSSpreadMultiplier,
            c.defaultADSSpreadMultiplierShotgun,
            c.cancelReloadOnWeaponSwitch,
            c.combineAmmoOnReload,
            c.ammoToUpperInventoryOnReload,

            c.shootableDefaultRespawnTime,
            c.shootableProximityTriggerFriendlyFire,

            c.soundRange,
            c.gunFireSoundRange,
            c.explosionSoundRange,

            c.useNewPenetrationSystem,
            c.enableBlockPenetration,
            c.blockPenetrationModifier,

            List.copyOf(c.penetrableBlocksLines)
        );
    }

    public static void write(FriendlyByteBuf buf, CommonConfigSnapshot s)
    {
        buf.writeVarInt(s.version());

        buf.writeBoolean(s.addAllPaintjobsToCreative());

        buf.writeBoolean(s.realisticRecoil());
        buf.writeBoolean(s.enableSightDownwardMovement());
        buf.writeVarInt(s.bonusRegenAmount());
        buf.writeVarInt(s.bonusRegenTickDelay());
        buf.writeDouble(s.bonusRegenFoodLimit());

        buf.writeDouble(s.newDamageSystemReference());
        buf.writeDouble(s.headshotDamageModifier());
        buf.writeDouble(s.chestshotDamageModifier());
        buf.writeDouble(s.armshotDamageModifier());
        buf.writeDouble(s.legshotModifier());
        buf.writeDouble(s.vehicleWheelSeatExplosionModifier());

        buf.writeVarInt(s.breakableArmor());
        buf.writeVarInt(s.defaultArmorDurability());
        buf.writeVarInt(s.defaultArmorEnchantability());
        buf.writeBoolean(s.enableOldArmorRatioSystem());

        buf.writeDouble(s.gunDamageModifier());
        buf.writeDouble(s.gunRecoilModifier());
        buf.writeDouble(s.defaultADSSpreadMultiplier());
        buf.writeDouble(s.defaultADSSpreadMultiplierShotgun());
        buf.writeBoolean(s.cancelReloadOnWeaponSwitch());
        buf.writeBoolean(s.combineAmmoOnReload());
        buf.writeBoolean(s.ammoToUpperInventoryOnReload());

        buf.writeVarInt(s.shootableDefaultRespawnTime());
        buf.writeBoolean(s.shootableProximityTriggerFriendlyFire());

        buf.writeFloat(s.soundRange());
        buf.writeFloat(s.gunFireSoundRange());
        buf.writeFloat(s.explosionSoundRange());

        buf.writeBoolean(s.useNewPenetrationSystem());
        buf.writeBoolean(s.enableBlockPenetration());
        buf.writeDouble(s.blockPenetrationModifier());

        buf.writeVarInt(s.penetrableBlocksLines().size());
        for (String line : s.penetrableBlocksLines())
            buf.writeUtf(line, 32767);
    }

    public static CommonConfigSnapshot read(FriendlyByteBuf buf)
    {
        int ver = buf.readVarInt();
        if (ver != CURRENT_VERSION)
            throw new IllegalStateException("Unsupported config snapshot version: " + ver);

        boolean addAllPaintjobsToCreative = buf.readBoolean();

        boolean realisticRecoil = buf.readBoolean();
        boolean enableSightDownwardMovement = buf.readBoolean();
        int bonusRegenAmount = buf.readVarInt();
        int bonusRegenTickDelay = buf.readVarInt();
        double bonusRegenFoodLimit = buf.readDouble();

        double newDamageSystemReference = buf.readDouble();
        double headshotDamageModifier = buf.readDouble();
        double chestshotDamageModifier = buf.readDouble();
        double armshotDamageModifier = buf.readDouble();
        double legshotModifier = buf.readDouble();
        double vehicleWheelSeatExplosionModifier = buf.readDouble();

        int breakableArmor = buf.readVarInt();
        int defaultArmorDurability = buf.readVarInt();
        int defaultArmorEnchantability = buf.readVarInt();
        boolean enableOldArmorRatioSystem = buf.readBoolean();

        double gunDamageModifier = buf.readDouble();
        double gunRecoilModifier = buf.readDouble();
        double defaultADSSpreadMultiplier = buf.readDouble();
        double defaultADSSpreadMultiplierShotgun = buf.readDouble();
        boolean cancelReloadOnWeaponSwitch = buf.readBoolean();
        boolean combineAmmoOnReload = buf.readBoolean();
        boolean ammoToUpperInventoryOnReload = buf.readBoolean();

        int shootableDefaultRespawnTime = buf.readVarInt();
        boolean shootableProximityTriggerFriendlyFire = buf.readBoolean();

        float soundRange = buf.readFloat();
        float gunFireSoundRange = buf.readFloat();
        float explosionSoundRange = buf.readFloat();

        boolean useNewPenetrationSystem = buf.readBoolean();
        boolean enableBlockPenetration = buf.readBoolean();
        double blockPenetrationModifier = buf.readDouble();

        int n = buf.readVarInt();
        List<String> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            lines.add(buf.readUtf(32767));

        return new CommonConfigSnapshot(
            ver,

            addAllPaintjobsToCreative,

            realisticRecoil,
            enableSightDownwardMovement,
            bonusRegenAmount,
            bonusRegenTickDelay,
            bonusRegenFoodLimit,

            newDamageSystemReference,
            headshotDamageModifier,
            chestshotDamageModifier,
            armshotDamageModifier,
            legshotModifier,
            vehicleWheelSeatExplosionModifier,

            breakableArmor,
            defaultArmorDurability,
            defaultArmorEnchantability,
            enableOldArmorRatioSystem,

            gunDamageModifier,
            gunRecoilModifier,
            defaultADSSpreadMultiplier,
            defaultADSSpreadMultiplierShotgun,
            cancelReloadOnWeaponSwitch,
            combineAmmoOnReload,
            ammoToUpperInventoryOnReload,

            shootableDefaultRespawnTime,
            shootableProximityTriggerFriendlyFire,

            soundRange,
            gunFireSoundRange,
            explosionSoundRange,

            useNewPenetrationSystem,
            enableBlockPenetration,
            blockPenetrationModifier,

            List.copyOf(lines)
        );
    }
}