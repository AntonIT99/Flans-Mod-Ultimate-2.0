package com.flansmodultimate.config;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record CommonConfigSnapshot(
    int version,

    boolean addAllPaintjobsToCreative,
    boolean validateContentReferencesOnWorldLoad,
    String defaultVehicleEngine,
    String defaultPlaneEngine,
    String defaultMechaEngine,

    boolean disableCrosshairForGuns,
    boolean explosionsBreakBlocks,
    boolean flanExplosionsDropBlocks,
    int bonusRegenAmount,
    int bonusRegenTickDelay,
    int bonusRegenFoodLimit,
    int bulletTrackingRange,
    int grenadeTrackingRange,
    int deployedGunTrackingRange,
    int aaGunTrackingRange,

    float headshotDamageModifier,
    float chestshotDamageModifier,
    float armshotDamageModifier,
    float legshotModifier,
    float vehicleWheelSeatExplosionModifier,

    int breakableArmor,
    int defaultArmorDurability,
    int defaultArmorEnchantability,
    boolean forceDefenseAsModernArmor,

    boolean gunsAlwaysUsableByPlayersInCreativeMode,
    boolean forceAllowAllAttachments,
    float gunDamageModifier,
    float gunRecoilModifier,
    float gunDispersionModifier,
    float gunAccuracySpreadModifier,
    float defaultADSSpreadMultiplier,
    float defaultADSSpreadMultiplierShotgun,
    boolean cancelReloadOnWeaponSwitch,
    boolean combineAmmoOnReload,
    boolean ammoToUpperInventoryOnReload,
    boolean realisticRecoil,
    boolean enableSightDownwardMovement,
    boolean disableSprintHipFireByDefault,
    boolean muzzleFlashParticlesDefault,

    boolean shootablesCanBreakGlass,
    float newDamageSystemDamageReference,
    float newDamageSystemExplosiveDamageReference,
    float newDamageSystemExplosivePowerReference,
    float newDamageSystemExplosiveRadiusReference,
    float newDamageSystemBlastToExplosionRadiusRatio,
    int shootableDefaultRespawnTime,
    boolean shootableProximityTriggerFriendlyFire,
    double lockOnRange,
    int flakParticlesRange,
    double entityHitParticleRange,
    double blockHitParticleRange,
    int smokeParticlesCount,
    double smokeParticlesRange,

    float soundRange,
    float gunFireSoundRange,
    float explosionSoundRange,

    boolean useNewPenetrationSystem,
    boolean enableBlockPenetration,
    double blockPenetrationModifier,

    List<String> penetrableBlocksLines,

    boolean enableDigitalAmmoSystem,
    int digitalAmmoDefaultAmount,
    int digitalAmmoMaxAmount,
    int digitalAmmoNumTypes,
    List<String> digitalAmmoSupplyBlocks,
    int digitalAmmoSupplyAmount,

    double realisticAircraftReferenceSpeedScale,
    double realisticAircraftThrottleResponse,
    double realisticPlaneSpeedScale,
    double realisticGroundVehicleSpeedScale,
    double maxPlaneSpeedKmh,
    double maxVehicleSpeedKmh,
    double realisticVehicleHealthScale,
    double penetrationVelocityExponent,
    double maxArmorImpactAngleDeg,
    double armoredBlastResistanceKPaPerMm,
    double minimumBlastDistanceMeters,

    boolean enchantmentModuleEnabled
)
{
    public static final int CURRENT_VERSION = 18;

    public static void write(FriendlyByteBuf buf, CommonConfigSnapshot s)
    {
        buf.writeVarInt(s.version);

        buf.writeBoolean(s.addAllPaintjobsToCreative);
        buf.writeBoolean(s.validateContentReferencesOnWorldLoad);
        buf.writeUtf(s.defaultVehicleEngine, 32767);
        buf.writeUtf(s.defaultPlaneEngine, 32767);
        buf.writeUtf(s.defaultMechaEngine, 32767);

        buf.writeBoolean(s.disableCrosshairForGuns);
        buf.writeBoolean(s.explosionsBreakBlocks);
        buf.writeBoolean(s.flanExplosionsDropBlocks);
        buf.writeVarInt(s.bonusRegenAmount);
        buf.writeVarInt(s.bonusRegenTickDelay);
        buf.writeVarInt(s.bonusRegenFoodLimit);
        buf.writeVarInt(s.bulletTrackingRange);
        buf.writeVarInt(s.grenadeTrackingRange);
        buf.writeVarInt(s.deployedGunTrackingRange);
        buf.writeVarInt(s.aaGunTrackingRange);

        buf.writeFloat(s.headshotDamageModifier);
        buf.writeFloat(s.chestshotDamageModifier);
        buf.writeFloat(s.armshotDamageModifier);
        buf.writeFloat(s.legshotModifier);
        buf.writeFloat(s.vehicleWheelSeatExplosionModifier);

        buf.writeVarInt(s.breakableArmor);
        buf.writeVarInt(s.defaultArmorDurability);
        buf.writeVarInt(s.defaultArmorEnchantability);
        buf.writeBoolean(s.forceDefenseAsModernArmor);

        buf.writeBoolean(s.gunsAlwaysUsableByPlayersInCreativeMode);
        buf.writeBoolean(s.forceAllowAllAttachments);
        buf.writeFloat(s.gunDamageModifier);
        buf.writeFloat(s.gunRecoilModifier);
        buf.writeFloat(s.gunDispersionModifier);
        buf.writeFloat(s.gunAccuracySpreadModifier);
        buf.writeFloat(s.defaultADSSpreadMultiplier);
        buf.writeFloat(s.defaultADSSpreadMultiplierShotgun);
        buf.writeBoolean(s.cancelReloadOnWeaponSwitch);
        buf.writeBoolean(s.combineAmmoOnReload);
        buf.writeBoolean(s.ammoToUpperInventoryOnReload);
        buf.writeBoolean(s.realisticRecoil);
        buf.writeBoolean(s.enableSightDownwardMovement);
        buf.writeBoolean(s.disableSprintHipFireByDefault);
        buf.writeBoolean(s.muzzleFlashParticlesDefault);

        buf.writeBoolean(s.shootablesCanBreakGlass);
        buf.writeFloat(s.newDamageSystemDamageReference);
        buf.writeFloat(s.newDamageSystemExplosiveDamageReference);
        buf.writeFloat(s.newDamageSystemExplosivePowerReference);
        buf.writeFloat(s.newDamageSystemExplosiveRadiusReference);
        buf.writeFloat(s.newDamageSystemBlastToExplosionRadiusRatio);
        buf.writeVarInt(s.shootableDefaultRespawnTime);
        buf.writeBoolean(s.shootableProximityTriggerFriendlyFire);
        buf.writeDouble(s.lockOnRange);
        buf.writeVarInt(s.flakParticlesRange);
        buf.writeDouble(s.entityHitParticleRange);
        buf.writeDouble(s.blockHitParticleRange);
        buf.writeVarInt(s.smokeParticlesCount);
        buf.writeDouble(s.smokeParticlesRange);

        buf.writeFloat(s.soundRange);
        buf.writeFloat(s.gunFireSoundRange);
        buf.writeFloat(s.explosionSoundRange);

        buf.writeBoolean(s.useNewPenetrationSystem);
        buf.writeBoolean(s.enableBlockPenetration);
        buf.writeDouble(s.blockPenetrationModifier);

        buf.writeVarInt(s.penetrableBlocksLines.size());
        for (String line : s.penetrableBlocksLines)
            buf.writeUtf(line, 32767);

        buf.writeBoolean(s.enableDigitalAmmoSystem);
        buf.writeVarInt(s.digitalAmmoDefaultAmount);
        buf.writeVarInt(s.digitalAmmoMaxAmount);
        buf.writeVarInt(s.digitalAmmoNumTypes);
        buf.writeVarInt(s.digitalAmmoSupplyBlocks.size());
        for (String block : s.digitalAmmoSupplyBlocks)
            buf.writeUtf(block, 32767);
        buf.writeVarInt(s.digitalAmmoSupplyAmount);

        buf.writeDouble(s.realisticAircraftReferenceSpeedScale);
        buf.writeDouble(s.realisticAircraftThrottleResponse);
        buf.writeDouble(s.realisticPlaneSpeedScale);
        buf.writeDouble(s.realisticGroundVehicleSpeedScale);
        buf.writeDouble(s.maxPlaneSpeedKmh);
        buf.writeDouble(s.maxVehicleSpeedKmh);
        buf.writeDouble(s.realisticVehicleHealthScale);
        buf.writeDouble(s.penetrationVelocityExponent);
        buf.writeDouble(s.maxArmorImpactAngleDeg);
        buf.writeDouble(s.armoredBlastResistanceKPaPerMm);
        buf.writeDouble(s.minimumBlastDistanceMeters);

        buf.writeBoolean(s.enchantmentModuleEnabled);
    }

    public static CommonConfigSnapshot read(FriendlyByteBuf buf)
    {
        return new CommonConfigSnapshot(
            readVersion(buf),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf(32767),
            buf.readUtf(32767),
            buf.readUtf(32767),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),

            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),

            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),

            buf.readBoolean(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readDouble(),
            buf.readVarInt(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readVarInt(),
            buf.readDouble(),

            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readDouble(),

            List.copyOf(readLines(buf)),

            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            List.copyOf(readLines(buf)),
            buf.readVarInt(),

            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),

            buf.readBoolean()
        );
    }

    private static int readVersion(FriendlyByteBuf buf) throws IllegalStateException
    {
        int ver = buf.readVarInt();
        if (ver != CURRENT_VERSION)
            throw new IllegalStateException("Unsupported config snapshot version: " + ver);
        return ver;
    }

    private static List<String> readLines(FriendlyByteBuf buf)
    {
        int n = buf.readVarInt();
        List<String> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            lines.add(buf.readUtf(32767));
        return lines;
    }
}
