package com.flansmodultimate.config;

import net.minecraft.network.FriendlyByteBuf;

public record ApocalypseConfigSnapshot(
    int version,

    boolean apocalypseEnabled,
    boolean apocalypseDimensionEnabled,
    boolean apocalypsePortalsEnabled,
    boolean apocalypseOverworldPortalGenerationEnabled,
    boolean apocalypseWorldgenEnabled,
    boolean apocalypseMobsEnabled,
    boolean apocalypseNukeDropsEnabled,
    int apocalypseCountdownLength,
    int apocalypseSurvivorRarity,
    int apocalypseWanderingSurvivorRarity,
    int apocalypseSkeletonRarity,
    int apocalypseDeadTreeRarity,
    int apocalypseVehicleRarity,
    int apocalypseAirportRarity,
    int apocalypseDyeFactoryRarity,
    int apocalypseLabRarity,
    int apocalypseAbandonedPortalRarity,
    int apocalypseAbandonedPortalOverworldRarity,
    int apocalypseReturnRadius,
    int apocalypseSpawnRadius,
    boolean apocalypseRespawnInApocalypse,
    ModApocalypseConfig.ApocalypseTeleportOption apocalypseTeleportOption,
    float apocalypseAcidDamage,
    float apocalypseNukeExplosionPower,
    int apocalypseNukeVisualTicks
)
{
    public static final int CURRENT_VERSION = 1;

    public static void write(FriendlyByteBuf buf, ApocalypseConfigSnapshot s)
    {
        buf.writeVarInt(s.version);

        buf.writeBoolean(s.apocalypseEnabled);
        buf.writeBoolean(s.apocalypseDimensionEnabled);
        buf.writeBoolean(s.apocalypsePortalsEnabled);
        buf.writeBoolean(s.apocalypseOverworldPortalGenerationEnabled);
        buf.writeBoolean(s.apocalypseWorldgenEnabled);
        buf.writeBoolean(s.apocalypseMobsEnabled);
        buf.writeBoolean(s.apocalypseNukeDropsEnabled);
        buf.writeVarInt(s.apocalypseCountdownLength);
        buf.writeVarInt(s.apocalypseSurvivorRarity);
        buf.writeVarInt(s.apocalypseWanderingSurvivorRarity);
        buf.writeVarInt(s.apocalypseSkeletonRarity);
        buf.writeVarInt(s.apocalypseDeadTreeRarity);
        buf.writeVarInt(s.apocalypseVehicleRarity);
        buf.writeVarInt(s.apocalypseAirportRarity);
        buf.writeVarInt(s.apocalypseDyeFactoryRarity);
        buf.writeVarInt(s.apocalypseLabRarity);
        buf.writeVarInt(s.apocalypseAbandonedPortalRarity);
        buf.writeVarInt(s.apocalypseAbandonedPortalOverworldRarity);
        buf.writeVarInt(s.apocalypseReturnRadius);
        buf.writeVarInt(s.apocalypseSpawnRadius);
        buf.writeBoolean(s.apocalypseRespawnInApocalypse);
        buf.writeEnum(s.apocalypseTeleportOption);
        buf.writeFloat(s.apocalypseAcidDamage);
        buf.writeFloat(s.apocalypseNukeExplosionPower);
        buf.writeVarInt(s.apocalypseNukeVisualTicks);
    }

    public static ApocalypseConfigSnapshot read(FriendlyByteBuf buf)
    {
        return new ApocalypseConfigSnapshot(
            readVersion(buf),

            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
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
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readEnum(ModApocalypseConfig.ApocalypseTeleportOption.class),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt()
        );
    }

    private static int readVersion(FriendlyByteBuf buf) throws IllegalStateException
    {
        int ver = buf.readVarInt();
        if (ver != CURRENT_VERSION)
            throw new IllegalStateException("Unsupported apocalypse config snapshot version: " + ver);
        return ver;
    }
}
