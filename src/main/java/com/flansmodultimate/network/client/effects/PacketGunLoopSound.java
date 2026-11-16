package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.common.guns.MinigunSounds;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.SoundNameCodec;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Starts, renews or stops the looping spin sound of a gun held by an entity. Clients loop the sound
 * themselves, so it plays without gaps; see {@link MinigunSounds}.
 */
@NoArgsConstructor
public class PacketGunLoopSound implements IClientPacket
{
    private int entityId;
    private boolean offHand;
    /** The sound to loop, or empty to stop the loop. */
    private String sound;
    private float range;

    public PacketGunLoopSound(Entity entity, InteractionHand hand, @Nullable String sound, float range)
    {
        entityId = entity.getId();
        offHand = hand == InteractionHand.OFF_HAND;
        this.sound = Objects.requireNonNullElse(sound, StringUtils.EMPTY);
        this.range = range;
    }

    /** Sends the loop, or its end when the sound is blank, to every player who can hear the entity. */
    public static void send(Entity entity, InteractionHand hand, @Nullable String sound, double range)
    {
        double audibleRange = Math.max(1D, range);
        PacketHandler.sendToAllAround(new PacketGunLoopSound(entity, hand, sound, (float) audibleRange), entity.position(), audibleRange, entity.level().dimension());
    }

    public static String channel(boolean offHand)
    {
        return offHand ? "gun_spin_off_hand" : "gun_spin_main_hand";
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeBoolean(offHand);
        SoundNameCodec.write(data, sound);
        data.writeFloat(range);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        offHand = data.readBoolean();
        sound = SoundNameCodec.read(data);
        range = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Entity entity = level.getEntity(entityId);
        if (entity == null)
            return;
        if (StringUtils.isBlank(sound))
            ClientHooks.SOUND.setLoopingEntitySound(entity, channel(offHand), null, range, false);
        else
            ClientHooks.SOUND.leaseLoopingEntitySound(entity, channel(offHand), sound, range, MinigunSounds.LOOP_LEASE_TICKS);
    }
}
