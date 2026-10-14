package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EngineSound;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NpcPresentationTest
{
    @Test
    void flanSelectionResetsVisualsButExplicitChoicesSurviveTicksAndReloads()
    {
        NpcPresentation settings = new NpcPresentation();
        assertTrue(settings.selectModel("wolffsmodnpcs:vehicle_tank", true));
        assertFalse(settings.isHurtFlash());
        assertFalse(settings.isDeathRotation());
        settings.setHurtFlash(true);
        settings.setDeathRotation(true);
        assertFalse(settings.selectModel("wolffsmodnpcs:vehicle_tank", true));
        assertTrue(settings.isHurtFlash());
        assertTrue(settings.isDeathRotation());
        CompoundTag root = new CompoundTag();
        settings.save(root);
        NpcPresentation restored = new NpcPresentation();
        restored.load(root);
        assertFalse(restored.selectModel("wolffsmodnpcs:vehicle_tank", true));
        assertTrue(restored.isHurtFlash());
        assertTrue(restored.isDeathRotation());
        assertTrue(restored.selectModel("wolffsmodnpcs:aa_gun", true));
        assertFalse(restored.isHurtFlash());
        assertFalse(restored.isDeathRotation());
    }

    @Test
    void oldFlanSavesReceiveNewDefaultsWhileOrdinarySavedChoicesArePreserved()
    {
        NpcPresentation settings = new NpcPresentation();
        settings.load(new CompoundTag());
        settings.selectModel("wolffsmodnpcs:mecha", true);
        assertFalse(settings.isHurtFlash());
        assertFalse(settings.isDeathRotation());
        settings.selectModel("minecraft:cow", false);
        assertTrue(settings.isHurtFlash());
        assertTrue(settings.isDeathRotation());
        settings.setHurtFlash(false);
        settings.setDeathRotation(false);
        CompoundTag saved = new CompoundTag();
        settings.save(saved);
        saved.getCompound(NpcPresentation.NBT_KEY).remove("DisplayModel");
        settings.load(saved);
        settings.selectModel("minecraft:cow", false);
        assertFalse(settings.isHurtFlash());
        assertFalse(settings.isDeathRotation());
    }

    @Test
    void oldSavesKeepNormalVisualsAndRequestedAudioDefaults()
    {
        NpcPresentation settings = new NpcPresentation();
        settings.load(new CompoundTag());
        assertTrue(settings.isHurtFlash());
        assertTrue(settings.isDeathRotation());
        NpcVehicleSounds sounds = settings.getVehicleSounds();
        assertFalse(sounds.isIdleEnabled());
        assertTrue(sounds.isMovementEnabled());
        assertTrue(sounds.isVariablePitch());
        assertTrue(sounds.isModelDefaults());
    }

    @Test
    void independentChannelEditsSurviveStatsAndSpawnTransport()
    {
        NpcPresentation settings = new NpcPresentation();
        settings.setHurtFlash(false);
        settings.setDeathRotation(false);
        NpcVehicleSounds sounds = settings.getVehicleSounds();
        sounds.setIdleEnabled(true);
        sounds.setMovementEnabled(false);
        sounds.setVariablePitch(false);
        sounds.setModelDefaults(false);
        sounds.getIdle().initialize(Optional.of(new EngineSound("idle", 96, 30)), 150);
        sounds.getIdle().setOverrideRepeat(true);
        sounds.getIdle().setRepeatTicks(81);
        sounds.getMovement().initialize(Optional.empty(), 150);
        sounds.getMovement().setSound("minecraft:block.note_block.bass");
        CompoundTag root = new CompoundTag();
        settings.save(root);
        NpcPresentation restored = new NpcPresentation();
        restored.load(root);
        assertFalse(restored.isHurtFlash());
        assertFalse(restored.isDeathRotation());
        NpcVehicleSounds audio = restored.getVehicleSounds();
        assertTrue(audio.isIdleEnabled());
        assertFalse(audio.isMovementEnabled());
        assertFalse(audio.isVariablePitch());
        assertFalse(audio.isModelDefaults());
        assertEquals(new EngineSound("flansmod:idle", 150, 81), audio.getIdle().resolve().orElseThrow());
        assertEquals("minecraft:block.note_block.bass", audio.getMovement().getSound());
        assertFalse(audio.getMovement().automaticRepeat());
    }

    @Test
    void switchingModelsDoesNotOverwriteCustomAudioAndClearMeansSilence()
    {
        NpcEngineSettings channel = new NpcEngineSettings();
        channel.initialize(Optional.of(new EngineSound("engine", 96, 60)), 123);
        assertEquals("flansmod:engine", channel.getSound());
        assertEquals(123, channel.getRange());
        channel.setSound("");
        channel.initialize(Optional.of(new EngineSound("another", 64, 30)), 456);
        assertTrue(channel.resolve().isEmpty());
        assertEquals(123, channel.getRange());
    }

    @Test
    void selectingVehicleDefaultsDiscardsConflictingCustomValues()
    {
        NpcEngineSettings channel = new NpcEngineSettings();
        channel.initialize(Optional.empty(), 50);
        channel.setSound("minecraft:block.note_block.bass");
        channel.setRange(200);
        channel.setOverrideRepeat(true);
        channel.setRepeatTicks(99);
        channel.resetToVehicle(Optional.of(new EngineSound("engine", 96, 60)), 50);
        assertEquals("flansmod:engine", channel.getSound());
        assertEquals(96, channel.getRange());
        assertEquals(60, channel.getRepeatTicks());
        assertFalse(channel.isOverrideRepeat());
        channel.resetToVehicle(Optional.empty(), 50);
        assertTrue(channel.resolve().isEmpty());
    }

    @Test
    void corruptedOverridesAreClampedAndMalformedSoundIdsRejected()
    {
        NpcEngineSettings channel = new NpcEngineSettings();
        channel.setSound("Bad Sound:broken");
        channel.setRange(Integer.MAX_VALUE);
        channel.setRepeatTicks(-1);
        assertTrue(channel.resolve().isEmpty());
        assertEquals(4096, channel.getRange());
        assertEquals(1, channel.getRepeatTicks());
    }

    @Test
    void legacyRangeOnlyOverrideKeepsTheVehicleSoundAndDuration()
    {
        CompoundTag old = new CompoundTag();
        old.putInt("EngineRange", 200);
        NpcVehicleSounds audio = new NpcVehicleSounds();
        audio.load(old);
        CompoundTag saved = new CompoundTag();
        audio.save(saved);
        audio.load(saved);
        audio.initializeCustom(Optional.empty(), Optional.of(new EngineSound("engine", 96, 60)), 50);
        assertEquals("flansmod:engine", audio.getMovement().getSound());
        assertEquals(200, audio.getMovement().getRange());
        assertEquals(60, audio.getMovement().getRepeatTicks());
        assertFalse(audio.getMovement().isOverrideRepeat());
    }

    @Test
    void earlierMovementOverridesMigrateWithoutLosingExplicitValues()
    {
        CompoundTag old = new CompoundTag();
        old.putBoolean("EngineEnabled", false);
        old.putString("EngineSound", "customengine");
        old.putInt("EngineRange", 120);
        old.putInt("EngineRepeatTicks", 81);
        NpcVehicleSounds audio = new NpcVehicleSounds();
        audio.load(old);
        assertFalse(audio.isMovementEnabled());
        assertFalse(audio.isModelDefaults());
        assertEquals(new EngineSound("flansmod:customengine", 120, 81), audio.getMovement().resolve().orElseThrow());
    }
}
