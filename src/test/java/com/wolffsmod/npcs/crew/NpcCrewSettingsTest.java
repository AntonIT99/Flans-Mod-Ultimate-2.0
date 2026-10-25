package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void individualSeatsOverrideLegacyRolesIncludingAnExplicitEmptySeat()
    {
        CrewSource rifleman = new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "rifleman");
        CrewSource gunner = new CrewSource(CrewSource.Kind.CLONE, 3, "Gunner", "", "");
        NpcCrewSettings settings = new NpcCrewSettings();
        settings.setSource(CrewPost.PASSENGER, rifleman);
        settings.setSource(2, CrewSource.NONE);
        settings.setSource(7, gunner);
        assertEquals(rifleman, settings.source(1, CrewPost.PASSENGER));
        assertEquals(CrewSource.NONE, settings.source(2, CrewPost.PASSENGER));
        assertEquals(gunner, settings.source(7, CrewPost.PASSENGER));

        NpcCrewSettings loaded = new NpcCrewSettings();
        CompoundTag stats = new CompoundTag();
        settings.save(stats);
        loaded.load(stats);
        assertEquals(settings.write(), loaded.write());
        assertTrue(loaded.hasSeatSource(2));
        assertEquals(CrewSource.NONE, loaded.source(2, CrewPost.PASSENGER));
        loaded.clearSeatSource(2);
        assertEquals(rifleman, loaded.source(2, CrewPost.PASSENGER));
        assertEquals(gunner, loaded.source(7, CrewPost.GUNNER));
        loaded.load(new CompoundTag());
        assertFalse(loaded.hasSeatSource(7));
        assertEquals(0, loaded.changes());
    }

    @Test
    void aSeatOnlySetupAndItsRandomSkinAreSavedWithoutAnyRoleDefaults()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        CrewSource source = new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "crew", CrewSource.RANDOM_SKIN);
        settings.setSource(5, source);
        assertTrue(settings.namesAnyone());
        assertEquals(1, settings.changes());
        CompoundTag stats = new CompoundTag();
        settings.save(stats);
        NpcCrewSettings loaded = new NpcCrewSettings();
        loaded.load(stats);
        assertEquals(source, loaded.source(5, CrewPost.GUNNER));
        assertEquals(CrewSource.NONE, loaded.source(CrewPost.GUNNER));
    }

    @Test
    void malformedSeatEntriesCannotReplaceTheDriverSelection()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        CrewSource driver = new CrewSource(CrewSource.Kind.CLONE, 2, "Driver", "", "");
        settings.setSource(CrewPost.DRIVER, driver);
        CompoundTag data = settings.write();
        ListTag seats = new ListTag();
        CompoundTag missingIndex = new CompoundTag();
        missingIndex.put("Source", CrewSource.NONE.save());
        seats.add(missingIndex);
        data.put("Seats", seats);
        settings.read(data);
        assertFalse(settings.hasSeatSource(0));
        assertEquals(driver, settings.source(0, CrewPost.DRIVER));
    }

    @Test
    void oldSourcesKeepTheFirstSkinAndFixedSkinsDoNotConsumeRandomness()
    {
        CrewSource original = new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "crew");
        CompoundTag legacy = original.save();
        legacy.remove("Skin");
        assertEquals(0, CrewSource.load(legacy).skin());
        RandomSource random = mock(RandomSource.class);
        assertEquals(2, original.withSkin(2).chooseSkin(3, random));
        assertEquals(0, original.withSkin(9).chooseSkin(3, random));
        assertEquals(0, original.withSkin(CrewSource.RANDOM_SKIN).chooseSkin(0, random));
        verifyNoInteractions(random);
    }

    @Test
    void randomSkinsAreChosenForEachNewRecruitAndSurviveSourceCopies()
    {
        RandomSource random = mock(RandomSource.class);
        when(random.nextInt(3)).thenReturn(2, 1);
        CrewSource source = new CrewSource(CrewSource.Kind.CLASS, 1, "", "team", "crew", CrewSource.RANDOM_SKIN);
        assertEquals(2, source.chooseSkin(3, random));
        assertEquals(1, source.chooseSkin(3, random));
        assertEquals(CrewSource.RANDOM_SKIN, CrewSource.load(source.save()).skin());
        assertEquals(CrewSource.RANDOM_SKIN, source.withClass("team", "crew").skin());
        assertEquals(0, source.withClass("team", "other").skin());
    }

    @Test
    void hiddenSeatVisibilityIsSavedIndependentlyAndOldSettingsStayVisible()
    {
        NpcCrewSettings settings = new NpcCrewSettings();
        assertTrue(settings.isSeatVisible(3));
        settings.setSeatVisible(3, false);
        assertFalse(settings.isSeatVisible(3));
        assertTrue(settings.isSeatVisible(7));
        assertEquals(1, settings.selectionChanges());
        assertFalse(settings.namesAnyone());
        CompoundTag stats = new CompoundTag();
        settings.save(stats);
        NpcCrewSettings loaded = new NpcCrewSettings();
        loaded.load(stats);
        assertFalse(loaded.isSeatVisible(3));
        assertTrue(loaded.isSeatVisible(7));
        assertEquals(CrewSource.NONE, loaded.source(3, CrewPost.GUNNER));
        loaded.setSeatVisible(3, true);
        assertEquals(0, loaded.changes());
        loaded.load(stats);
        loaded.load(new CompoundTag());
        assertTrue(loaded.isSeatVisible(3));
        assertEquals(0, loaded.changes());
    }

    private static WeaponMount mount(int seat, boolean driver, @Nullable IContentType gun)
    {
        return new WeaponMount(seat, driver, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.ofNullable(gun));
    }
}
