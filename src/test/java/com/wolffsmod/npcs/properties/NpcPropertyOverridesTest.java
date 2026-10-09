package com.wolffsmod.npcs.properties;

import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPropertyOverridesTest
{
    @Test
    void modelChangesUseNewValuesWithoutLosingSavedNpcDefaults()
    {
        Map<NpcTypeProperty, Tag> live = new EnumMap<>(NpcTypeProperty.class);
        live.put(NpcTypeProperty.HEALTH, IntTag.valueOf(20));
        live.put(NpcTypeProperty.DAMAGE, IntTag.valueOf(4));
        NpcPropertyOverrides overlay = new NpcPropertyOverrides();
        assertTrue(overlay.refresh(Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(200), NpcTypeProperty.DAMAGE, IntTag.valueOf(12)), live::get, live::put));
        assertFalse(overlay.refresh(Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(200), NpcTypeProperty.DAMAGE, IntTag.valueOf(12)), live::get, live::put));
        overlay.refresh(Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(400)), live::get, live::put);
        assertEquals(IntTag.valueOf(400), live.get(NpcTypeProperty.HEALTH));
        assertEquals(IntTag.valueOf(4), live.get(NpcTypeProperty.DAMAGE));
        CompoundTag save = new CompoundTag();
        save.putInt("MaxHealth", 400);
        save.putInt("HealthRegen", 7);
        overlay.saveDefaults(Component.STATS, save);
        assertEquals(20, save.getInt("MaxHealth"));
        assertEquals(7, save.getInt("HealthRegen"));
        assertEquals(IntTag.valueOf(400), live.get(NpcTypeProperty.HEALTH));
        overlay.refresh(Map.of(), live::get, live::put);
        assertEquals(IntTag.valueOf(20), live.get(NpcTypeProperty.HEALTH));
    }

    @Test
    void pairedSettersCannotContaminateTheCapturedDefaults()
    {
        Map<NpcTypeProperty, Tag> live = new EnumMap<>(NpcTypeProperty.class);
        live.put(NpcTypeProperty.DELAY_MIN, IntTag.valueOf(20));
        live.put(NpcTypeProperty.DELAY_MAX, IntTag.valueOf(40));
        NpcPropertyOverrides overlay = new NpcPropertyOverrides();
        overlay.refresh(Map.of(NpcTypeProperty.DELAY_MIN, IntTag.valueOf(100), NpcTypeProperty.DELAY_MAX, IntTag.valueOf(100)), live::get, (property, value) ->
        {
            live.put(property, value);
            if (property == NpcTypeProperty.DELAY_MIN)
                live.put(NpcTypeProperty.DELAY_MAX, value);
        });
        CompoundTag save = new CompoundTag();
        overlay.saveDefaults(Component.STATS, save);
        assertEquals(20, save.getInt("minDelay"));
        assertEquals(40, save.getInt("maxDelay"));
    }

    @Test
    void editingOneComponentReplacesOnlyItsSavedFallbacks()
    {
        Map<NpcTypeProperty, Tag> live = new EnumMap<>(NpcTypeProperty.class);
        live.put(NpcTypeProperty.HEALTH, IntTag.valueOf(20));
        live.put(NpcTypeProperty.IDLE_SOUND, StringTag.valueOf("minecraft:stored"));
        NpcPropertyOverrides overlay = new NpcPropertyOverrides();
        Map<NpcTypeProperty, Tag> inherited = Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(200), NpcTypeProperty.IDLE_SOUND, StringTag.valueOf("flansmod:engine"));
        overlay.refresh(inherited, live::get, live::put);
        live.put(NpcTypeProperty.HEALTH, IntTag.valueOf(77));
        overlay.loaded(Component.STATS);
        overlay.refresh(inherited, live::get, live::put);
        CompoundTag stats = new CompoundTag();
        overlay.saveDefaults(Component.STATS, stats);
        assertEquals(77, stats.getInt("MaxHealth"));
        assertFalse(stats.contains("NpcIdleSound"));
        CompoundTag advanced = new CompoundTag();
        overlay.saveDefaults(Component.ADVANCED, advanced);
        assertEquals("minecraft:stored", advanced.getString("NpcIdleSound"));
        overlay.refresh(Map.of(), live::get, live::put);
        assertEquals(IntTag.valueOf(77), live.get(NpcTypeProperty.HEALTH));
        assertEquals(StringTag.valueOf("minecraft:stored"), live.get(NpcTypeProperty.IDLE_SOUND));
    }
}
