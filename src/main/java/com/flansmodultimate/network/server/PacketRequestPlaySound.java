package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

@NoArgsConstructor
public class PacketRequestPlaySound implements IServerPacket
{
    private float posX;
    private float posY;
    private float posZ;
    private float range;
    private String sound;

    public PacketRequestPlaySound(Vec3 pos, float range, @Nullable String sound)
    {
        posX = (float) pos.x;
        posY = (float) pos.y;
        posZ = (float) pos.z;
        this.range = range;
        this.sound = Objects.requireNonNullElse(sound, StringUtils.EMPTY);
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        data.writeFloat(range);
        data.writeUtf(sound);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        range = data.readFloat();
        sound = data.readUtf();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (sound.isBlank())
            return;

        RegistryObject<SoundEvent> event = FlansMod.getSoundEvent(sound).orElse(null);
        if (event == null || event.getId() == null)
        {
            FlansMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, sound));
            return;
        }

        float volume = range / 16F;

        // Exclude sender so they don't double-hear (they already played locally)
        level.playSound(player, posX, posY, posZ, event.get(), SoundSource.PLAYERS, volume, 1F);
    }
}
