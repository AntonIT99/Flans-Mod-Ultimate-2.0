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
    float nameTagRenderRange,
    float nameTagSneakRenderRange,
    int noticeSpawnKillTime,

    boolean disableCrosshairForGuns,
    boolean explosionsBreakBlocks,
    boolean forceNewExplosionsBreakBlocks,
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
    boolean driveableCollisionsBreakBlocks,
    boolean autoRefillVehicleAmmo,
    boolean gunsInDestroyedPartsWork,

    int breakableArmor,
    int defaultArmorDurability,
    int defaultArmorEnchantability,
    boolean forceDefenseAsModernArmor,
    int ambientMobArmorSpawnRate,
    int ambientMobArmorDropRate,

    boolean gunsAlwaysUsableByPlayersInCreativeMode,
    boolean forceAllowAllAttachments,
    boolean disableDualWielding,
    boolean reloadOnEmptyFire,
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
    EnumPlayerAimPose playerAimPose,
    EnumEntityAimPose entityAimPose,

    boolean shootablesCanBreakGlass,
    float newDamageSystemDamageReference,
    float newDamageSystemExplosiveDamageReference,
    float newDamageSystemExplosivePowerReference,
    float newDamageSystemExplosiveRadiusReference,
    float newDamageSystemBlastRadiusReference,
    float newDamageSystemBlastFalloffSharpness,
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
    double kineticPenetrationReference,

    List<String> penetrableBlocksLines,

    boolean enableDigitalAmmoSystem,
    int digitalAmmoDefaultAmount,
    int digitalAmmoMaxAmount,
    int digitalAmmoNumTypes,
    List<String> digitalAmmoSupplyBlocks,
    int digitalAmmoSupplyAmount,

    boolean forceLegacyPlanePhysics,
    boolean forceLegacyVehiclePhysics,
    boolean enableAircraftRollSelfLeveling,
    double realisticAircraftReferenceSpeedScale,
    double realisticAircraftThrottleResponse,
    double realisticPlaneSpeedScale,
    double realisticGroundVehicleSpeedScale,
    double maxPlaneSpeedKmh,
    double maxVehicleSpeedKmh,
    boolean forceLegacyVehicleKnockback,
    double vehicleKnockbackReferenceMassKg,
    double fallbackGroundVehicleMassTons,
    double fallbackAircraftMassTons,
    double fallbackAAGunMassTons,
    double realisticVehicleHealthScale,
    double maxArmorImpactAngleDeg,
    double armoredBlastResistanceKPaPerMm,
    double minimumBlastDistanceMeters,
    double maxExplosionRadius,
    double maxBlastRadius,

    boolean enchantmentModuleEnabled,

    List<String> fluidFuelLines
)
{
    public static final int CURRENT_VERSION = 33;

    public static void write(FriendlyByteBuf buf, CommonConfigSnapshot s)
    {
        buf.writeVarInt(s.version);

        buf.writeBoolean(s.addAllPaintjobsToCreative);
        buf.writeBoolean(s.validateContentReferencesOnWorldLoad);
        buf.writeUtf(s.defaultVehicleEngine, 32767);
        buf.writeUtf(s.defaultPlaneEngine, 32767);
        buf.writeUtf(s.defaultMechaEngine, 32767);
        buf.writeFloat(s.nameTagRenderRange);
        buf.writeFloat(s.nameTagSneakRenderRange);
        buf.writeVarInt(s.noticeSpawnKillTime);

        buf.writeBoolean(s.disableCrosshairForGuns);
        buf.writeBoolean(s.explosionsBreakBlocks);
        buf.writeBoolean(s.forceNewExplosionsBreakBlocks);
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
        buf.writeBoolean(s.driveableCollisionsBreakBlocks);
        buf.writeBoolean(s.autoRefillVehicleAmmo);
        buf.writeBoolean(s.gunsInDestroyedPartsWork);

        buf.writeVarInt(s.breakableArmor);
        buf.writeVarInt(s.defaultArmorDurability);
        buf.writeVarInt(s.defaultArmorEnchantability);
        buf.writeBoolean(s.forceDefenseAsModernArmor);
        buf.writeVarInt(s.ambientMobArmorSpawnRate);
        buf.writeVarInt(s.ambientMobArmorDropRate);

        buf.writeBoolean(s.gunsAlwaysUsableByPlayersInCreativeMode);
        buf.writeBoolean(s.forceAllowAllAttachments);
        buf.writeBoolean(s.disableDualWielding);
        buf.writeBoolean(s.reloadOnEmptyFire);
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
        buf.writeEnum(s.playerAimPose);
        buf.writeEnum(s.entityAimPose);

        buf.writeBoolean(s.shootablesCanBreakGlass);
        buf.writeFloat(s.newDamageSystemDamageReference);
        buf.writeFloat(s.newDamageSystemExplosiveDamageReference);
        buf.writeFloat(s.newDamageSystemExplosivePowerReference);
        buf.writeFloat(s.newDamageSystemExplosiveRadiusReference);
        buf.writeFloat(s.newDamageSystemBlastRadiusReference);
        buf.writeFloat(s.newDamageSystemBlastFalloffSharpness);
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
        buf.writeDouble(s.kineticPenetrationReference);

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

        buf.writeBoolean(s.forceLegacyPlanePhysics);
        buf.writeBoolean(s.forceLegacyVehiclePhysics);
        buf.writeBoolean(s.enableAircraftRollSelfLeveling);
        buf.writeDouble(s.realisticAircraftReferenceSpeedScale);
        buf.writeDouble(s.realisticAircraftThrottleResponse);
        buf.writeDouble(s.realisticPlaneSpeedScale);
        buf.writeDouble(s.realisticGroundVehicleSpeedScale);
        buf.writeDouble(s.maxPlaneSpeedKmh);
        buf.writeDouble(s.maxVehicleSpeedKmh);
        buf.writeBoolean(s.forceLegacyVehicleKnockback);
        buf.writeDouble(s.vehicleKnockbackReferenceMassKg);
        buf.writeDouble(s.fallbackGroundVehicleMassTons);
        buf.writeDouble(s.fallbackAircraftMassTons);
        buf.writeDouble(s.fallbackAAGunMassTons);
        buf.writeDouble(s.realisticVehicleHealthScale);
        buf.writeDouble(s.maxArmorImpactAngleDeg);
        buf.writeDouble(s.armoredBlastResistanceKPaPerMm);
        buf.writeDouble(s.minimumBlastDistanceMeters);
        buf.writeDouble(s.maxExplosionRadius);
        buf.writeDouble(s.maxBlastRadius);

        buf.writeBoolean(s.enchantmentModuleEnabled);

        buf.writeVarInt(s.fluidFuelLines.size());
        for (String line : s.fluidFuelLines)
            buf.writeUtf(line, 32767);
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
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),

            buf.readBoolean(),
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
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),

            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),

            buf.readBoolean(),
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
            buf.readEnum(EnumPlayerAimPose.class),
            buf.readEnum(EnumEntityAimPose.class),

            buf.readBoolean(),
            buf.readFloat(),
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
            buf.readDouble(),

            List.copyOf(readLines(buf)),

            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            List.copyOf(readLines(buf)),
            buf.readVarInt(),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readBoolean(),
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

            buf.readBoolean(),

            List.copyOf(readLines(buf))
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
