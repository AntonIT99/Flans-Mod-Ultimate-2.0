package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;

class CustomNpcTacticsTest
{
    @Test
    void oldSavesDefaultToRushAndEveryForgeChoiceRoundTripsThroughTheAiPacketTag()
    {
        CustomNpcTactics settings = new CustomNpcTactics();
        settings.load(new CompoundTag());
        assertEquals(CustomNpcTactics.RUSH, settings.getVariant());
        assertEquals(4, settings.getRange());
        for (int variant = 0; variant <= 6; variant++)
        {
            CompoundTag forgeSave = new CompoundTag();
            forgeSave.putInt("TacticalVariant", variant);
            forgeSave.putInt("TacticalRadius", 31);
            settings.load(forgeSave);
            CompoundTag packet = new CompoundTag();
            packet.putString("OtherAiSetting", "retained");
            settings.save(packet);
            assertEquals(variant, packet.getInt("TacticalVariant"));
            assertEquals(31, packet.getInt("TacticalRadius"));
            assertEquals("retained", packet.getString("OtherAiSetting"));
            CustomNpcTactics client = new CustomNpcTactics();
            client.load(packet);
            assertEquals(variant, client.getVariant());
            assertEquals(31, client.getRange());
        }
    }

    @Test
    void malformedPacketValuesCannotCreateAnUnknownTacticOrAnUnboundedRandomSearch()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TacticalVariant", Integer.MAX_VALUE);
        tag.putInt("TacticalRadius", Integer.MIN_VALUE);
        CustomNpcTactics settings = new CustomNpcTactics();
        settings.load(tag);
        assertEquals(CustomNpcTactics.NONE, settings.getVariant());
        assertEquals(1, settings.getRange());
        settings.setVariant(-1);
        settings.setRange(Integer.MAX_VALUE);
        assertEquals(CustomNpcTactics.RUSH, settings.getVariant());
        assertEquals(4096, settings.getRange());
    }
}
