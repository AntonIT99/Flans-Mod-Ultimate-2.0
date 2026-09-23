package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.sync.ContentFingerprint;
import com.flansmodultimate.common.sync.EnumContentMismatch;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.TreeMap;

/**
 * The server's content-pack fingerprints, sent to a player as they join.
 *
 * <p>The client compares them with its own and says which packs differ. It is one-way on purpose:
 * the server learns nothing from the client, so there is nothing here for a client to lie about,
 * and nobody is kept out of the game over a pack. What the player gets is the name of the pack to
 * go and fix, instead of a weapon that quietly behaves differently from what their game shows.
 */
@NoArgsConstructor
public class PacketContentFingerprint implements IClientPacket
{
    /** Guards against a malformed or hostile packet allocating unbounded entries. */
    private static final int MAX_PACKS = 1024;

    private Map<String, String> fingerprints = Map.of();

    public PacketContentFingerprint(Map<String, String> fingerprints)
    {
        this.fingerprints = fingerprints;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(fingerprints.size());
        fingerprints.forEach((pack, fingerprint) -> {
            buf.writeUtf(pack);
            buf.writeUtf(fingerprint);
        });
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf buf)
    {
        int size = Math.min(buf.readVarInt(), MAX_PACKS);
        Map<String, String> read = new TreeMap<>();
        for (int i = 0; i < size; i++)
            read.put(buf.readUtf(), buf.readUtf());
        fingerprints = read;
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Map<String, EnumContentMismatch> mismatches = ContentFingerprint.compareWith(fingerprints);
        if (mismatches.isEmpty())
            return;

        player.displayClientMessage(Component.translatable("message.flansmodultimate.content_mismatch.header")
            .withStyle(ChatFormatting.GOLD), false);

        mismatches.forEach((pack, mismatch) -> {
            player.displayClientMessage(Component.translatable(mismatch.getTranslationKey(), pack)
                .withStyle(ChatFormatting.YELLOW), false);
            FlansMod.log.warn("Content pack '{}' does not match the server: {}", pack, mismatch);
        });
    }
}
