package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.SoundNameCodec;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.common.types.EntitySoundSupport;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Starts, renews or stops the engine loop of an entity driven through the API. Clients loop the sound themselves, so it
 * plays without the gaps between repeated clips; see {@link EntitySoundSupport#loop}.
 */
@NoArgsConstructor
public class PacketEngineLoopSound implements IClientPacket
{
    public static final String CHANNEL = "api_engine";

    private int entityId;
    /** The sound to loop, or empty to stop the loop. */
    private String sound;
    private float range;
    private boolean enginePitch;

    public PacketEngineLoopSound(Entity entity, @Nullable String sound, float range, boolean enginePitch)
    {
        entityId = entity.getId();
        this.sound = Objects.requireNonNullElse(sound, StringUtils.EMPTY);
        this.range = range;
        this.enginePitch = enginePitch;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        SoundNameCodec.write(data, sound);
        data.writeFloat(range);
        data.writeBoolean(enginePitch);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        sound = SoundNameCodec.read(data);
        range = data.readFloat();
        enginePitch = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Entity entity = level.getEntity(entityId);
        if (entity == null)
            return;
        if (StringUtils.isBlank(sound))
            ClientHooks.SOUND.setLoopingEntitySound(entity, CHANNEL, null, range, false);
        else
            ClientHooks.SOUND.leaseEngineLoop(entity, CHANNEL, sound, range, EntitySoundSupport.LOOP_LEASE_TICKS, enginePitch);
    }
}
