package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NpcCrewSettingsTest
{
    @Test
    void aNewNpcSavesNothingAndKeepsTheDefaults()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        CompoundTag stats = new CompoundTag();
        settings.save(stats);
        assertFalse(stats.contains(NpcCrewSettings.NBT_KEY));
        assertEquals(0, settings.changes());
        assertTrue(settings.isProtectedCrew());
        assertEquals(NpcCrewSettings.Destroyed.BAIL_OUT, settings.getDestroyed());
        assertFalse(settings.namesAnyone());
    }

    @Test
    void settingsSurviveSavingAndLoading()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        settings.setSource(CrewPost.DRIVER, new CrewSource(CrewSource.Kind.CLASS, 1, "", "germany", "tank_crewman"));
        settings.setSource(CrewPost.GUNNER, new CrewSource(CrewSource.Kind.CLONE, 4, "Gunner", "", ""));
        settings.setAutoCrew(true);
        settings.setProtectedCrew(false);
        settings.setDestroyed(NpcCrewSettings.Destroyed.VANISH);
        settings.setPassengersFire(true);
        CompoundTag stats = new CompoundTag();
        settings.save(stats);

        NpcCrewSettings loaded = new NpcCrewSettings();
        loaded.load(stats);
        assertEquals(settings.write(), loaded.write());
        assertEquals(new CrewSource(CrewSource.Kind.CLASS, 1, "", "germany", "tank_crewman"), loaded.source(CrewPost.DRIVER));
        assertEquals(CrewSource.NONE, loaded.source(CrewPost.PASSENGER));
        assertEquals(6, loaded.changes());
        assertTrue(loaded.namesAnyone());
    }

    @Test
    void statsWithoutCrewReturnEverySettingToItsDefault()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        settings.setSource(CrewPost.PASSENGER, new CrewSource(CrewSource.Kind.CLASS, 1, "", "", "rifleman"));
        settings.setProtectedCrew(false);
        settings.load(new CompoundTag());
        assertEquals(0, settings.changes());
        assertTrue(settings.isProtectedCrew());
    }

    @Test
    void aSourceWithoutANameNamesNobody()
    {
        assertFalse(new CrewSource(CrewSource.Kind.CLONE, 2, " ", "", "").names());
        assertFalse(new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "").names());
        assertTrue(new CrewSource(CrewSource.Kind.CLASS, 1, "", "", "rifleman").names());
        assertEquals(CrewSource.MAX_CLONE_TAB, new CrewSource(CrewSource.Kind.CLONE, 99, "x", "", "").cloneTab());
    }

    @Test
    void seatsArePostsByWhatTheyOffer()
    {
        IContentType gun = mock(IContentType.class);
        assertEquals(CrewPost.DRIVER, CrewPost.of(mount(0, true, gun)));
        assertEquals(CrewPost.GUNNER, CrewPost.of(mount(1, false, gun)));
        assertEquals(CrewPost.PASSENGER, CrewPost.of(mount(2, false, null)));
    }

    private static WeaponMount mount(int seat, boolean driver, @Nullable IContentType gun)
    {
        return new WeaponMount(seat, driver, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.ofNullable(gun));
    }
}
