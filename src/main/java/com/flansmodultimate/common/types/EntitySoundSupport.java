package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.entity.EngineSound;
import com.flansmodultimate.api.entity.SoundRange;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.content.SoundPriority;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketCancelSound;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.OptionalInt;
import java.util.UUID;

/** Sound transport shared by addon engine controllers. */
public final class EntitySoundSupport
{
    private EntitySoundSupport()
    {}

    public static boolean play(Entity source, EngineSound sound, UUID instance, boolean variablePitch)
    {
        ResourceLocation id = resolve(source, sound.sound(), sound.range());
        if (id == null)
            return false;
        PacketPlaySound packet = new PacketPlaySound(source.position(), sound.range(), id.toString(), false, false, true, instance, source);
        packet.setVariableEnginePitch(variablePitch);
        PacketHandler.sendToAllAround(packet, source.position(), sound.range(), source.level().dimension());
        return true;
    }

    public static boolean playClip(Entity source, String sound, float range, boolean distort)
    {
        ResourceLocation id = resolve(source, sound, range);
        if (id == null)
            return false;
        // A sound of this mod travels as its compact registry id, which matters for a clip sent with every shot.
        PacketPlaySound.sendSoundPacket(source, range, FlansMod.FLANSMOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString(), distort);
        return true;
    }

    /** @return the sound event the source may emit at that radius, or null when the sound, the radius or the source rules it out */
    @Nullable
    private static ResourceLocation resolve(Entity source, String sound, float range)
    {
        if (StringUtils.isBlank(sound) || !(source.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || !source.isAlive() || source.isSilent() || !Float.isFinite(range)
            || range <= 0F)
            return null;
        return ResourceLocation.tryParse(sound.contains(":") ? sound : FlansMod.FLANSMOD_ID + ":" + sound);
    }

    public static float range(SoundRange role)
    {
        CommonConfigSnapshot config = ModCommonConfig.get();
        return switch (role)
        {
            case GUN_FIRE -> config.gunFireSoundRange();
            case GUN_ACTION -> config.gunActionSoundRange();
            case RELOAD -> config.reloadSoundRange();
            case MELEE -> config.meleeSoundRange();
            case THROW -> config.grenadeThrowSoundRange();
            case BULLET_HIT -> config.bulletHitSoundRange();
            case EXPLOSION -> config.explosionSoundRange();
            case VEHICLE -> config.vehicleSoundRange();
            case PLANE -> config.planeSoundRange();
            case VEHICLE_UTILITY -> config.vehicleUtilitySoundRange();
        };
    }

    public static OptionalInt length(ResourceLocation id)
    {
        return FlansMod.FLANSMOD_ID.equals(id.getNamespace()) ? SoundPriority.measuredLength(id.getPath()) : OptionalInt.empty();
    }

    public static float defaultRange()
    {
        return ModCommonConfig.get().vehicleSoundRange();
    }

    public static void stop(Entity source, UUID instance)
    {
        if (source.level() instanceof ServerLevel level && level.getServer().isSameThread())
            PacketHandler.sendToDimension(level.dimension(), new PacketCancelSound(instance));
    }
}
