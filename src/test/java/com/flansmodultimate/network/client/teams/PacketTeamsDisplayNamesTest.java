package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PacketTeamsDisplayNamesTest
{
    @Test
    void roundTripRetainsCanonicalIdentityAndUnresolvedDisplayComponents() throws ReflectiveOperationException
    {
        Component teamName = Component.translatableWithFallback("team.pack.army", "Army");
        Component className = Component.translatableWithFallback("class.pack.crew", "Crewman");
        PacketTeamsState packet = new PacketTeamsState();
        set(packet, "teamChoices", List.of(new PacketTeamsState.TeamChoice("army_2", "Army", 0x123456, teamName)));
        set(packet, "classChoices", List.of(new PacketTeamsState.ClassChoice("crew", "Crewman", 0, 0, 0, -1, List.of(), className)));
        var player = new PacketTeamsState.PlayerScore("[Bot] Army", 5, 6, 7, 0, "Crewman", Component.literal("[Bot] ").append(teamName), className);
        set(packet, "teamScores", List.of(new PacketTeamsState.TeamScore("army_2", "Army", 0x123456, 5, 2, List.of(player), teamName)));
        set(packet, "voteOptions", List.of(new PacketTeamsState.VoteOption("Map", "TDM", teamName.copy().append(" vs ").append(teamName), 1)));
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            packet.encodeInto(buffer);
            PacketTeamsState decoded = new PacketTeamsState();
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            assertEquals("army_2", decoded.getTeamChoices().get(0).id());
            assertEquals("Army", decoded.getTeamChoices().get(0).name());
            assertEquals("team.pack.army", ((TranslatableContents) decoded.getTeamChoices().get(0).displayName().getContents()).getKey());
            assertEquals(className, decoded.getClassChoices().get(0).displayName());
            assertEquals(player, decoded.getTeamScores().get(0).players().get(0));
            assertEquals(packet.getVoteOptions(), decoded.getVoteOptions());
        }
        finally
        {
            buffer.release();
        }
    }

    private static void set(PacketTeamsState packet, String name, Object value) throws ReflectiveOperationException
    {
        var field = PacketTeamsState.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(packet, value);
    }
}
