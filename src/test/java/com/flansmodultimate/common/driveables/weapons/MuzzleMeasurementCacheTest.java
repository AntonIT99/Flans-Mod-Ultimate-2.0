package com.flansmodultimate.common.driveables.weapons;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.PlatformPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The cache has to hand a restarted game exactly what measuring gave the last
 * one, and nothing when it was stored for other content.
 */
class MuzzleMeasurementCacheTest
{
    private static final IContentProvider PACK = new ContentPack("cachePack", Path.of("build", "test-packs", "cache"));

    private static VehicleType vehicle()
    {
        VehicleType vehicle = new VehicleType();
        vehicle.load(new TypeFile("cacheVehicle", EnumType.VEHICLE, PACK, List.of("ShortName cacheVehicle", "Driver 0 0 0", "BarrelPosition 10 20 0")));
        return vehicle;
    }

    private static AAGunType aaGun()
    {
        AAGunType aaGun = new AAGunType();
        aaGun.load(new TypeFile("cacheAaGun", EnumType.AA_GUN, PACK, List.of("ShortName cacheAaGun", "NumBarrels 2")));
        return aaGun;
    }

    private static GunType deployedGun()
    {
        GunType gun = new GunType();
        gun.load(new TypeFile("cacheDeployedGun", EnumType.GUN, PACK, List.of("ShortName cacheDeployedGun", "Deployable True")));
        return gun;
    }

    @Test
    void storedResultsReplayOntoFreshlyReadTypes()
    {
        MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        MuzzleMeasurementCache.recordMove(results, vehicle(), new MuzzleMeasurementCache.Move(false, false, 0, 30F, 22F, -4F));
        MuzzleMeasurementCache.recordBarrels(results, aaGun(), new Vec3[]{new Vec3(1, 2, 3), new Vec3(1, 2, -3)}, new Vec3[]{new Vec3(20, 2, 3), new Vec3(20, 2, -3)});
        MuzzleMeasurementCache.recordDeployedGunMuzzle(results, deployedGun(), new Vec3(0, 6, 0), new Vec3(0, 6, 20));

        MuzzleMeasurementCache.Results restored = roundTrip(results);
        VehicleType vehicle = vehicle();
        AAGunType aaGun = aaGun();
        GunType deployedGun = deployedGun();
        int moved = MuzzleMeasurementCache.apply(restored, List.of(vehicle, aaGun, deployedGun));

        assertEquals(1, moved);
        ShootPoint point = vehicle.shootPoints(false).get(0);
        Vector3f root = point.getRootPos().getPosition();
        assertEquals(30F, (root.x + point.getOffPos().x) * 16F, 1.0E-4F);
        assertEquals(-4F, (root.z + point.getOffPos().z) * 16F, 1.0E-4F);
        assertTrue(aaGun.hasMeasuredBarrels());
        assertEquals(new Vec3(20, 2, -3), aaGun.getMeasuredBarrelMuzzles()[1]);
        assertTrue(deployedGun.hasMeasuredDeployableMuzzle());
        assertEquals(new Vec3(0, 6, 20), deployedGun.getMeasuredDeployableMuzzle());
    }

    @Test
    void storedBarrelsReplayAndFollowTheirPointWhenItMoves()
    {
        MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        MuzzleMeasurementCache.recordSpread(results, vehicle(), new MuzzleMeasurementCache.Spread(false, false, 0, List.of(new Vector3f(0F, -2F, 6F), new Vector3f(0F, 2F, -6F))));
        MuzzleMeasurementCache.recordMove(results, vehicle(), new MuzzleMeasurementCache.Move(false, false, 0, 30F, 22F, 0F));

        MuzzleMeasurementCache.Results restored = roundTrip(results);
        VehicleType vehicle = vehicle();
        MuzzleMeasurementCache.apply(restored, List.of(vehicle));

        assertEquals(1, restored.mountCount());
        ShootPoint point = vehicle.shootPoints(false).get(0);
        assertEquals(2, point.getBarrelCount());
        Vector3f root = point.getRootPos().getPosition();
        Vector3f second = point.getBarrelOffPos(1);
        assertEquals(30F, (root.x + second.x) * 16F, 1.0E-4F);
        assertEquals(24F, (root.y + second.y) * 16F, 1.0E-4F);
        assertEquals(-6F, (root.z + second.z) * 16F, 1.0E-4F);

        // A later move, such as a debug override, carries the barrels with the point.
        vehicle.setDebugShootPoint(false, 0, new Vector3f(40F, 22F, 0F));
        assertEquals(2, vehicle.shootPoints(false).get(0).getBarrelCount());
    }

    @Test
    void storedSeatGunPivotsReplay()
    {
        MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        MuzzleMeasurementCache.Pivot pivot = new MuzzleMeasurementCache.Pivot(2, 12F, 30F, -4F);
        MuzzleMeasurementCache.recordPivot(results, vehicle(), pivot);

        MuzzleMeasurementCache.Results restored = roundTrip(results);

        assertNotNull(restored);
        assertEquals(List.of(pivot), restored.pivots.get(MuzzleMeasurementCache.typeKey(vehicle())));
        // A seat without a gun takes none, and applying stored pivots onto it leaves it alone.
        VehicleType vehicle = vehicle();
        assertEquals(0, MuzzleMeasurementCache.apply(restored, List.of(vehicle)));
        assertFalse(vehicle.applyMeasuredGunPivot(2, new Vector3f(12F, 30F, -4F)));
    }

    @Test
    void resultsStoredForOtherContentAreIgnored()
    {
        MuzzleMeasurementCache.State state = new MuzzleMeasurementCache.State();
        state.packs.put("pack", new MuzzleMeasurementCache.Entry(Map.of("definitions", "before"), new MuzzleMeasurementCache.Results()));
        String json = MuzzleMeasurementCache.toJson(state);

        assertNull(MuzzleMeasurementCache.matching(MuzzleMeasurementCache.fromJson(json).packs.get("pack"), Map.of("definitions", "after")));
        assertNotNull(MuzzleMeasurementCache.matching(MuzzleMeasurementCache.fromJson(json).packs.get("pack"), Map.of("definitions", "before")));
    }

    @Test
    void digestedDefinitionsAreNotReadAgainButModelsStillAre(@TempDir Path folder) throws IOException, NoSuchAlgorithmException
    {
        IContentProvider pack = new ContentPack("keyPack", folder);
        Path definition = Files.createDirectories(folder.resolve("vehicles")).resolve("tank.txt");
        Path model = folder.resolve("ModelTank.class");
        Files.writeString(definition, "ShortName tank");
        Files.writeString(model, "model");

        String digested = packsKey(pack, Map.of(pack, "definitions"));
        String undigested = packsKey(pack, Map.of());
        Files.writeString(definition, "ShortName tank2");

        // The digest stands for the definitions, which only a pack without one has read from its folder.
        assertEquals(digested, packsKey(pack, Map.of(pack, "definitions")));
        assertNotEquals(digested, packsKey(pack, Map.of(pack, "other definitions")));
        assertNotEquals(undigested, packsKey(pack, Map.of()));

        Files.writeString(model, "other model");
        assertNotEquals(digested, packsKey(pack, Map.of(pack, "definitions")));
    }

    @Test
    void rebuiltArchivesIgnoreTimestampsEntryOrderAndTexturesButDetectModelChanges(@TempDir Path folder) throws IOException, NoSuchAlgorithmException
    {
        Path archive = folder.resolve("pack.zip");
        IContentProvider pack = new ContentPack("zipPack", archive);
        writeArchive(archive, "model", false);
        String before = packsKey(pack, Map.of(pack, "definitions"));
        writeArchive(archive, "model", true);
        Files.setLastModifiedTime(archive, FileTime.fromMillis(1000));
        assertEquals(before, packsKey(pack, Map.of(pack, "definitions")));

        writeArchive(archive, "changed model", true);
        assertNotEquals(before, packsKey(pack, Map.of(pack, "definitions")));
    }

    @Test
    void warmFingerprintsReusePersistedFileHashesAndRebuildsKeepContentIdentity(@TempDir Path folder) throws IOException, NoSuchAlgorithmException
    {
        Path model = folder.resolve("ModelTank.class");
        Files.writeString(model, "model");
        MuzzleMeasurementCache.State state = new MuzzleMeasurementCache.State();
        String before = fingerprint(folder, state);
        state = MuzzleMeasurementCache.fromJson(MuzzleMeasurementCache.toJson(state));
        String fileKey = model.toAbsolutePath().normalize().toString();
        MuzzleMeasurementCache.FileHash stored = state.files.get(fileKey);

        assertEquals(before, fingerprint(folder, state));
        assertSame(stored, state.files.get(fileKey));
        Files.setLastModifiedTime(model, FileTime.fromMillis(1000));
        assertEquals(before, fingerprint(folder, state));
        assertNotEquals(stored.modified(), state.files.get(fileKey).modified());

        Files.writeString(model, "changed model");
        assertNotEquals(before, fingerprint(folder, state));
        Files.delete(model);
        assertNotEquals(before, fingerprint(folder, state));
    }

    @Test
    void warmArchivesReuseTheirEntryIndex(@TempDir Path folder) throws IOException, NoSuchAlgorithmException
    {
        Path archive = folder.resolve("pack.zip");
        writeArchive(archive, "model", false);
        MuzzleMeasurementCache.State state = new MuzzleMeasurementCache.State();
        String before = fingerprint(archive, state);
        Map<String, MuzzleMeasurementCache.FileHash> stored = new TreeMap<>(state.files);

        assertEquals(before, fingerprint(archive, state));
        stored.forEach((key, value) -> assertSame(value, state.files.get(key)));

        writeArchive(archive, "model", true);
        Files.setLastModifiedTime(archive, FileTime.fromMillis(1000));
        assertEquals(before, fingerprint(archive, state));
    }

    @Test
    void definitionEditsInvalidateOnlyTheirPackAndItsSeatGunConsumers(@TempDir Path folder) throws IOException
    {
        IContentProvider vehicles = new ContentPack("vehicles", folder.resolve("vehicles"));
        IContentProvider guns = new ContentPack("guns", folder.resolve("guns"));
        IContentProvider unrelated = new ContentPack("unrelated", folder.resolve("unrelated"));
        List<IContentProvider> packs = List.of(vehicles, guns, unrelated);
        VehicleType vehicle = mock(VehicleType.class);
        SeatInfo seat = mock(SeatInfo.class);
        GunType gun = mock(GunType.class);
        when(vehicle.getContentPack()).thenReturn(vehicles);
        when(vehicle.getNumPassengers()).thenReturn(1);
        when(vehicle.getSeat(1)).thenReturn(seat);
        when(seat.getGunType()).thenReturn(gun);
        when(gun.getContentPack()).thenReturn(guns);

        try (MockedStatic<PlatformPaths> paths = mockStatic(PlatformPaths.class))
        {
            paths.when(PlatformPaths::gameDir).thenReturn(folder);
            paths.when(PlatformPaths::configDir).thenReturn(folder.resolve("config"));
            Map<IContentProvider, Map<String, String>> before = MuzzleMeasurementCache.inputs(packs, Map.of(vehicles, "v1", guns, "g1", unrelated, "u1"), List.of(vehicle), true,
                new MuzzleMeasurementCache.State());
            Map<IContentProvider, Map<String, String>> after = MuzzleMeasurementCache.inputs(packs, Map.of(vehicles, "v1", guns, "g2", unrelated, "u1"), List.of(vehicle), true,
                new MuzzleMeasurementCache.State());

            assertEquals(before.get(unrelated), after.get(unrelated));
            assertNotEquals(before.get(guns), after.get(guns));
            assertNotEquals(before.get(vehicles), after.get(vehicles));
            assertEquals(List.of("seat gun definitions guns"), MuzzleMeasurementCache.changedInputs(new MuzzleMeasurementCache.Entry(before.get(vehicles), null), after.get(vehicles)));

            when(seat.getGunType()).thenReturn(null);
            Map<IContentProvider, Map<String, String>> missingGun = MuzzleMeasurementCache.inputs(packs, Map.of(vehicles, "v1", guns, "g1", unrelated, "u1"), List.of(vehicle), true,
                new MuzzleMeasurementCache.State());
            assertNotEquals(before.get(vehicles), missingGun.get(vehicles));

            Files.createDirectories(guns.getPath());
            Files.writeString(guns.getPath().resolve("ModelGun.class"), "changed shared model");
            Map<IContentProvider, Map<String, String>> changedModel = MuzzleMeasurementCache.inputs(packs, Map.of(vehicles, "v1", guns, "g1", unrelated, "u1"), List.of(vehicle), true,
                new MuzzleMeasurementCache.State());
            for (IContentProvider pack : packs)
                assertNotEquals(missingGun.get(pack), changedModel.get(pack));
        }
    }

    @Test
    void oldCacheFormatRequiresOneNewMeasurementPass()
    {
        assertTrue(MuzzleMeasurementCache.fromJson("{\"version\":4,\"key\":\"old\",\"results\":{}}").packs.isEmpty());
    }

    @Test
    void explicitNullMapsInCacheAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> MuzzleMeasurementCache.fromJson("{\"version\":5,\"state\":{\"packs\":null,\"files\":{}}}"));
        assertThrows(IllegalArgumentException.class, () -> MuzzleMeasurementCache.fromJson("{\"version\":5,\"state\":{\"packs\":{},\"files\":null}}"));
    }

    private static String fingerprint(Path path, MuzzleMeasurementCache.State state) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        MuzzleMeasurementCache.fingerprint(digest, path, false, false, state);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void writeArchive(Path archive, String model, boolean reverse) throws IOException
    {
        List<String> names = new ArrayList<>(List.of("ModelTank.class", "assets/flansmod/textures/tank.png"));
        if (reverse)
            Collections.reverse(names);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive)))
        {
            for (String name : names)
            {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(reverse ? 2000 : 1000);
                out.putNextEntry(entry);
                out.write((name.endsWith(".class") ? model : "texture " + reverse).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static MuzzleMeasurementCache.Results roundTrip(MuzzleMeasurementCache.Results results)
    {
        MuzzleMeasurementCache.State state = new MuzzleMeasurementCache.State();
        state.packs.put("pack", new MuzzleMeasurementCache.Entry(Map.of("definitions", "key"), results));
        return MuzzleMeasurementCache.matching(MuzzleMeasurementCache.fromJson(MuzzleMeasurementCache.toJson(state)).packs.get("pack"), Map.of("definitions", "key"));
    }

    private static String packsKey(IContentProvider pack, Map<IContentProvider, String> definitionDigests) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        MuzzleMeasurementCache.fingerprint(digest, pack.getPath(), !definitionDigests.containsKey(pack), false, new MuzzleMeasurementCache.State());
        digest.update(definitionDigests.getOrDefault(pack, "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
}
