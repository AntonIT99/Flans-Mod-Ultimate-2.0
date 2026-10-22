package com.wolffsmod.npcs.network;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.teams.PlayerOrder;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class NpcEditorPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void editorRequestsAndRepliesPreserveRoundIdentityAndNestedTemplates()
    {
        UUID round = UUID.randomUUID();
        CompoundTag settings = settings();
        CompoundTag template = new CompoundTag();
        template.putString("name", "Infantry α");
        template.put("settings", settings.copy());
        roundTrip(new BotEditorActionPacket(BotEditorActionPacket.Action.SAVE_SETTINGS, 2, round, settings), BotEditorActionPacket::write, BotEditorActionPacket::read);
        roundTrip(new BotEditorStatePacket(List.of(new BotEditorStatePacket.Round(round, "Round α", List.of("blue", "red"))), 0, settings, List.of(template, new CompoundTag()), List.of("Assault α")),
            BotEditorStatePacket::write, BotEditorStatePacket::read);
        roundTrip(new BotEditorStatePacket(List.of(), -1, new CompoundTag(), List.of(), List.of()), BotEditorStatePacket::write, BotEditorStatePacket::read);
    }

    @Test
    void ordersAndRepliesPreserveMapCoordinatesAndUnassignedSentinels()
    {
        roundTrip(BotOrderPacket.refresh(), BotOrderPacket::write, BotOrderPacket::read);
        roundTrip(new BotOrderPacket(BotOrderPacket.Action.ORDER, 3, PlayerOrder.Kind.RELEASE, -1, new Vec3(-12.5D, 0D, 400.25D)), BotOrderPacket::write, BotOrderPacket::read);
        roundTrip(
            new BotOrdersStatePacket(true, 1, true, "Commander α", List.of(new BotOrdersStatePacket.Squad(3, "Squad α", 8, "hold", true, 40, -12.5D, 400.25D)),
                List.of(new BotOrdersStatePacket.Objective('A', "Bridge", -1, -10D, 20D)), new BotOrdersStatePacket.Leading(3, true, -1, true), true),
            BotOrdersStatePacket::write, BotOrdersStatePacket::read);
        roundTrip(BotOrdersStatePacket.notPlaying(), BotOrdersStatePacket::write, BotOrdersStatePacket::read);
        roundTrip(new BotWaypointsPacket(List.of(new BotWaypointsPacket.Waypoint(-12.5D, 75D, 400.25D, 3, "rally"))), BotWaypointsPacket::write, BotWaypointsPacket::read);
    }

    @Test
    void presetAndCrewMessagesPreserveActionsAndSettingsInBothDirections()
    {
        for (NpcPresetPacket.Action action : NpcPresetPacket.Action.values())
            roundTrip(new NpcPresetPacket(42, action, "PLAYER_LIKE", settings()), NpcPresetPacket::write, NpcPresetPacket::read);
        for (CrewActionPacket.Action action : CrewActionPacket.Action.values())
            roundTrip(new CrewActionPacket(42, action, settings()), CrewActionPacket::write, CrewActionPacket::read);
    }

    private static CompoundTag settings()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("total", 24);
        tag.putBoolean("enabled", true);
        return tag;
    }

    private static <P> void roundTrip(P expected, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            write.accept(expected, buffer);
            assertEquals(expected, read.apply(buffer));
            assertEquals(0, buffer.readableBytes(), "the receiver must consume the complete packet");
        }
        finally
        {
            buffer.release();
        }
    }
}
