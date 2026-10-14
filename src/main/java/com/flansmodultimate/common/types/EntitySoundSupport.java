package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.EngineSound;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.content.SoundPriority;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketCancelSound;
import com.flansmodultimate.network.client.effects.PacketPlaySound;

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
        if (sound.sound().isBlank())
            return false;
        ResourceLocation id = ResourceLocation.tryParse(sound.sound().contains(":") ? sound.sound() : FlansMod.FLANSMOD_ID + ":" + sound.sound());
        if (!(source.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || !source.isAlive() || source.isSilent() || !Float.isFinite(sound.range()) || sound.range() <= 0F
            || id == null)
            return false;
        PacketPlaySound packet = new PacketPlaySound(source.position(), sound.range(), id.toString(), false, false, true, instance, source);
        packet.setVariableEnginePitch(variablePitch);
        PacketHandler.sendToAllAround(packet, source.position(), sound.range(), source.level().dimension());
        return true;
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
