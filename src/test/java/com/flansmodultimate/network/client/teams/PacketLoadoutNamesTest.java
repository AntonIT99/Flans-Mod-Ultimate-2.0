package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketLoadoutNamesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void poolItemAndRewardBoxNamesStayTranslatableAcrossTheNetwork() throws ReflectiveOperationException
    {
        Component poolName = Component.translatableWithFallback("loadout_pool.flansmod.sample", "Default Pool");
        Component itemName = Component.translatableWithFallback("item.flansmod.sample_2", "Default Item");
        Component boxName = Component.translatableWithFallback("item.flansmod.sample_box", "Default Box");
        PacketLoadoutState packet = new PacketLoadoutState();
        set(packet, "poolName", poolName);
        set(packet, "entries", List.of(new PacketLoadoutState.Entry(LoadoutSlot.values()[0], "sample_2", itemName, 3, ItemStack.EMPTY)));
        set(packet, "boxes", List.of(new PacketLoadoutState.BoxView(UUID.randomUUID(), "sample_box", boxName, false, "", ItemStack.EMPTY)));
        set(packet, "boxTypes", List.of(new PacketLoadoutState.BoxTypeView("sample_box", boxName, ItemStack.EMPTY, 2)));
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            packet.encodeInto(buffer);
            PacketLoadoutState decoded = new PacketLoadoutState();
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            assertEquals(poolName, decoded.getPoolName());
            assertEquals("Default Pool", decoded.getPoolName().getString());
            assertEquals(packet.getEntries(), decoded.getEntries());
            assertEquals(packet.getBoxes(), decoded.getBoxes());
            assertEquals(packet.getBoxTypes(), decoded.getBoxTypes());
        }
        finally
        {
            buffer.release();
        }
    }

    private static void set(PacketLoadoutState packet, String name, Object value) throws ReflectiveOperationException
    {
        var field = PacketLoadoutState.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(packet, value);
    }
}
