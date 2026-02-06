package com.flansmodultimate.config;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record CommonConfigSnapshot(
    int version,

    boolean addAllPaintjobsToCreative,

    boolean explosionsBreakBlocks,
    int bonusRegenAmount,
    int bonusRegenTickDelay,
    double bonusRegenFoodLimit,

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
    boolean realisticRecoil,
    boolean enableSightDownwardMovement,

    float newDamageSystemReference,
    float newDamageSystemExplosiveReference,
    float newDamageSystemExplosivePowerReference,
    float newDamageSystemExplosiveRadiusReference,
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

    public static void write(FriendlyByteBuf buf, CommonConfigSnapshot s)
    {
        buf.writeVarInt(s.version);

        buf.writeBoolean(s.addAllPaintjobsToCreative);

        buf.writeBoolean(s.explosionsBreakBlocks);
        buf.writeVarInt(s.bonusRegenAmount);
        buf.writeVarInt(s.bonusRegenTickDelay);
        buf.writeDouble(s.bonusRegenFoodLimit);

        buf.writeDouble(s.headshotDamageModifier);
        buf.writeDouble(s.chestshotDamageModifier);
        buf.writeDouble(s.armshotDamageModifier);
        buf.writeDouble(s.legshotModifier);
        buf.writeDouble(s.vehicleWheelSeatExplosionModifier);

        buf.writeVarInt(s.breakableArmor);
        buf.writeVarInt(s.defaultArmorDurability);
        buf.writeVarInt(s.defaultArmorEnchantability);
        buf.writeBoolean(s.enableOldArmorRatioSystem);

        buf.writeDouble(s.gunDamageModifier);
        buf.writeDouble(s.gunRecoilModifier);
        buf.writeDouble(s.defaultADSSpreadMultiplier);
        buf.writeDouble(s.defaultADSSpreadMultiplierShotgun);
        buf.writeBoolean(s.cancelReloadOnWeaponSwitch);
        buf.writeBoolean(s.combineAmmoOnReload);
        buf.writeBoolean(s.ammoToUpperInventoryOnReload);
        buf.writeBoolean(s.realisticRecoil);
        buf.writeBoolean(s.enableSightDownwardMovement);

        buf.writeFloat(s.newDamageSystemReference);
        buf.writeFloat(s.newDamageSystemExplosiveReference);
        buf.writeFloat(s.newDamageSystemExplosivePowerReference);
        buf.writeFloat(s.newDamageSystemExplosiveRadiusReference);
        buf.writeVarInt(s.shootableDefaultRespawnTime);
        buf.writeBoolean(s.shootableProximityTriggerFriendlyFire);

        buf.writeFloat(s.soundRange);
        buf.writeFloat(s.gunFireSoundRange);
        buf.writeFloat(s.explosionSoundRange);

        buf.writeBoolean(s.useNewPenetrationSystem);
        buf.writeBoolean(s.enableBlockPenetration);
        buf.writeDouble(s.blockPenetrationModifier);

        buf.writeVarInt(s.penetrableBlocksLines.size());
        for (String line : s.penetrableBlocksLines)
            buf.writeUtf(line, 32767);
    }

    public static CommonConfigSnapshot read(FriendlyByteBuf buf)
    {
        return new CommonConfigSnapshot(
            readVersion(buf),

            buf.readBoolean(),

            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readDouble(),

            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),

            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),

            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),

            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readBoolean(),

            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readDouble(),

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