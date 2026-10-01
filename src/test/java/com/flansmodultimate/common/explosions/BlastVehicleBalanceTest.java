package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.ExplosionVehicleDamageResolver;
import com.flansmodultimate.common.driveables.armor.VehicleHealthScaler;
import com.flansmodultimate.common.types.ShootableType.EnumFragType;
import com.flansmodultimate.config.ModCommonConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qualitative, one-detonation blast balance targets, not claims of historical kill probabilities.
 * A listed distance is from the detonation to the nearest exposed CORE surface, with open sky,
 * full visibility, no prior damage and no projectile impact/penetration. The core is the first
 * part hit; destroying it destroys a driveable. Other parts cannot shield it in these scenarios.
 *
 * <p>Category masses, armor and fragment types are read from the shipped defaults. Core HP is
 * calculated by {@link VehicleHealthScaler} from the named pack's actual SetupPart weights.
 * Default config values are pinned here to make future tuning misses visible even when an
 * unrelated test has installed a server config snapshot.</p>
 *
 * <p>Qualitative source context: the USAAF's D-Day weapons trials identified 100 lb HE as
 * effective against vehicles (https://www.ibiblio.org/hyperwar/AAF/AAF-W-Normandy/index.html),
 * and US Navy transport doctrine describes fragmentation bombs as attacks on grounded aircraft
 * and motor vehicles, with 500 lb bombs for defended positions
 * (https://www.ibiblio.org/hyperwar/USN/ref/Transport/transport-26.html). Those reports do not
 * establish the exact kill distances chosen here; these are explicit gameplay targets.</p>
 */
class BlastVehicleBalanceTest
{
    private static final double BLAST_DAMAGE_REFERENCE = 80D;
    private static final double BLAST_FALLOFF_SHARPNESS = 2.5D;

    private static final Vehicle JEEP = vehicle("Willys MB Jeep", "vehicle_categories.json", "jeep",
        "src/officialpacks/resources/flans_content/ww2/definitions/vehicles/Jeep.txt");
    private static final Vehicle TRUCK = vehicle("GMC CCKW 353", "vehicle_categories.json", "44_gmctruck",
        "src/warfare44pack/resources/flans_content/warfare44/definitions/vehicles/44_GMCTruck.txt");
    private static final Vehicle STUART = vehicle("Light Tank M5A1 Stuart", "vehicle_categories.json", "44_m5a1stuart",
        "src/warfare44pack/resources/flans_content/warfare44/definitions/vehicles/44_M5A1Stuart.txt");
    private static final Vehicle SHERMAN = vehicle("Medium Tank M4 Sherman (75mm)", "vehicle_categories.json", "sherman",
        "src/officialpacks/resources/flans_content/ww2/definitions/vehicles/Sherman.txt");
    private static final Vehicle TIGER = vehicle("Panzerkampfwagen VI Ausf. H1 Tiger", "vehicle_categories.json", "tiger",
        "src/officialpacks/resources/flans_content/ww2/definitions/vehicles/Tiger.txt");
    private static final Vehicle FW190 = vehicle("Focke-Wulf Fw 190 A-4", "plane_categories.json", "ww2_plane_fw190_1a",
        "src/manuspacks/resources/flans_content/ww2/definitions/planes/WW2_Plane_Fw190_1A.txt");
    private static final Vehicle B17 = vehicle("Boeing B-17G Flying Fortress", "plane_categories.json", "ww2_ww2_plane_b17g_1a",
        "src/manuspacks/resources/flans_content/ww2/definitions/planes/WW2_Plane_B17G_1A.txt");
    private static final Vehicle B52 = vehicle("Boeing B-52H Stratofortress", "plane_categories.json", "b52",
        "src/officialpacks/resources/flans_content/modernwarfare/definitions/planes/B52.txt");

    private record Vehicle(String category, String categoryFile, String shortName, Path definition) {}
    private record Scenario(String explosive, String explosiveFile, Vehicle vehicle,
                            double distanceMeters, boolean destroys) {}

    private static Vehicle vehicle(String category, String categoryFile, String shortName, String definition)
    {
        return new Vehicle(category, categoryFile, shortName, Path.of(definition));
    }

    private static Scenario destroys(String explosive, Vehicle vehicle, double distanceMeters)
    {
        return new Scenario(explosive, "bullet_categories.json", vehicle, distanceMeters, true);
    }

    private static Scenario survives(String explosive, Vehicle vehicle, double distanceMeters)
    {
        return new Scenario(explosive, "bullet_categories.json", vehicle, distanceMeters, false);
    }

    private static Scenario survivesGrenade(String explosive, Vehicle vehicle, double distanceMeters)
    {
        return new Scenario(explosive, "grenade_categories.json", vehicle, distanceMeters, false);
    }

    @TestFactory
    Stream<DynamicTest> realOrdnanceAgainstRepresentativeVehicles() throws IOException
    {
        // Each pair expresses a one-blast kill boundary. A near burst means the charge is beside
        // the named core, not that the projectile pierced the vehicle. Far bursts test survival.
        List<Scenario> cases = List.of(
            survivesGrenade("Mk 2 Pineapple Grenade", JEEP, 0.5),
            survivesGrenade("Mk 2 Pineapple Grenade", STUART, 0.5),
            survivesGrenade("Stielhandgranate 24", SHERMAN, 0.5),
            survives("40x53mm HV Grenade", JEEP, 0.5),
            survives("75mm M48 HE", JEEP, 0.5),
            survives("75mm M48 HE", STUART, 0.5),
            survives("155mm M107 HE Shell", SHERMAN, 3D),
            survives("SC 10 Fragmentation Bomb", JEEP, 0.5),
            // The loaded 4,000 kg A-4 profile gives its core 180 HP, above this blast's damage.
            survives("SC 10 Fragmentation Bomb", FW190, 0.5),
            survives("SC 10 Fragmentation Bomb", B17, 0.5),

            destroys("AN-M30 100 lb General-Purpose Bomb", JEEP, 0.5),
            survives("AN-M30 100 lb General-Purpose Bomb", JEEP, 20D),
            destroys("AN-M30 100 lb General-Purpose Bomb", FW190, 0.5),
            survives("AN-M30 100 lb General-Purpose Bomb", B17, 12D),
            survives("AN-M30 100 lb General-Purpose Bomb", SHERMAN, 3D),
            destroys("SC 50 General-Purpose Bomb", JEEP, 0.5),
            survives("SC 50 General-Purpose Bomb", JEEP, 25D),
            survives("SC 50 General-Purpose Bomb", TIGER, 0.5),
            destroys("FAB-100 General-Purpose Bomb", TRUCK, 0.5),
            survives("FAB-100 General-Purpose Bomb", TRUCK, 25D),

            destroys("AN-M64 500 lb General-Purpose Bomb", TRUCK, 0.5),
            survives("AN-M64 500 lb General-Purpose Bomb", TRUCK, 40D),
            destroys("AN-M64 500 lb General-Purpose Bomb", B17, 0.5),
            survives("AN-M64 500 lb General-Purpose Bomb", B17, 40D),
            survives("AN-M64 500 lb General-Purpose Bomb", B52, 0.5),
            destroys("SC 500 General-Purpose Bomb", STUART, 0.5),
            survives("SC 500 General-Purpose Bomb", STUART, 25D),
            destroys("SC 1000 General-Purpose Bomb", SHERMAN, 0.5),
            survives("SC 1000 General-Purpose Bomb", TIGER, 25D),
            destroys("SC 4000 High-Capacity Bomb", TIGER, 0.5),
            survives("SC 4000 High-Capacity Bomb", TIGER, 100D)
        );

        Map<String, JsonObject> categories = Map.of(
            "bullet_categories.json", readCategories("bullet_categories.json"),
            "grenade_categories.json", readCategories("grenade_categories.json"),
            "vehicle_categories.json", readCategories("vehicle_categories.json"),
            "plane_categories.json", readCategories("plane_categories.json"));
        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
            testCase.explosive + " at " + testCase.distanceMeters + " m from "
                + testCase.vehicle.category + " core -> " + (testCase.destroys ? "destroy" : "survive"),
            () -> assertBalance(testCase, categories)));
    }

    private static void assertBalance(Scenario testCase, Map<String, JsonObject> categories) throws IOException
    {
        JsonObject explosive = category(categories, testCase.explosiveFile, testCase.explosive);
        JsonObject vehicle = category(categories, testCase.vehicle.categoryFile, testCase.vehicle.category);
        JsonObject ordnance = explosive.getAsJsonObject("properties");
        JsonObject hull = vehicle.getAsJsonObject("properties");
        assertTrue(vehicle.getAsJsonArray("items").asList().stream()
            .anyMatch(item -> item.getAsString().equalsIgnoreCase(testCase.vehicle.shortName)),
            "representative definition must belong to its category");
        assertTrue(hull.get("UseRealisticVehicleHealth").getAsBoolean());

        float chargeKg = ordnance.has("ExplosiveMassTNTKg")
            ? ordnance.get("ExplosiveMassTNTKg").getAsFloat()
            : ordnance.get("ExplosiveMassTNTg").getAsFloat() / 1000F;
        assertTrue(chargeKg > 0F);
        EnumFragType fragType = ordnance.has("FragType")
            ? EnumFragType.valueOf(ordnance.get("FragType").getAsString()) : EnumFragType.DEFAULT;
        float massKg = hull.get("RealMassKg").getAsFloat();
        float armorMm = testCase.vehicle.categoryFile.equals("plane_categories.json")
            ? 0F : Float.parseFloat(hull.get("ArmorFrontMm").getAsString().split("\\s+")[0]);
        float coreHp = coreHp(testCase.vehicle.definition, testCase.vehicle.shortName, massKg);

        float blastRadius = ExplosionScaling.blastRadius(ModCommonConfig.DEFAULT_BLAST_RADIUS_REFERENCE, chargeKg);
        float blast = testCase.distanceMeters <= blastRadius
            ? ExplosionScaling.blastDamage(BLAST_DAMAGE_REFERENCE, chargeKg)
                * (float) Math.sqrt(ExplosionScaling.blastFalloff(
                    testCase.distanceMeters, blastRadius, BLAST_FALLOFF_SHARPNESS)) : 0F;
        blast *= ExplosionVehicleDamageResolver.structuralBlastMultiplier(
            chargeKg, testCase.distanceMeters, blastRadius);
        double totalMass = ordnance.has("Mass") ? ordnance.get("Mass").getAsDouble() : 0D;
        double count = ordnance.has("FragCount") ? ordnance.get("FragCount").getAsDouble() : 0D;
        double metal = ordnance.has("FragMetalMassg") ? ordnance.get("FragMetalMassg").getAsDouble() : 0D;
        FragmentationModel.Pattern pattern = ordnance.has("FragPattern")
            ? FragmentationModel.Pattern.valueOf(ordnance.get("FragPattern").getAsString())
            : FragmentationModel.Pattern.RADIAL;
        FragmentationModel.Burst burst = FragmentationModel.create(fragType, chargeKg, totalMass,
            metal, count, pattern);
        float fragments = (float) burst.damage(burst.peakDamage(), 1D, testCase.distanceMeters, 1D);
        float damage = ExplosionVehicleDamageResolver.resolve(armorMm, chargeKg,
            testCase.distanceMeters, blast, fragments,
            ModCommonConfig.DEFAULT_ARMORED_BLAST_RESISTANCE_KPA_PER_MM,
            ModCommonConfig.DEFAULT_MINIMUM_BLAST_DISTANCE_METERS).totalDamage();

        String result = String.format(java.util.Locale.ROOT,
            "%s vs %s at %.1f m: blast %.1f + fragments %.1f, resolved %.1f, core HP %.1f",
            testCase.explosive, testCase.vehicle.category, testCase.distanceMeters,
            blast, fragments, damage, coreHp);
        assertEquals(testCase.destroys, damage >= coreHp, result);
    }

    private static float coreHp(Path definition, String shortName, float massKg) throws IOException
    {
        EnumMap<EnumDriveablePart, CollisionBox> authored = new EnumMap<>(EnumDriveablePart.class);
        List<String> lines = Files.readAllLines(definition);
        assertTrue(lines.stream().anyMatch(line -> line.trim().equalsIgnoreCase("ShortName " + shortName)),
            "definition short name differs from category item: " + definition);
        for (String line : lines)
        {
            String[] fields = line.trim().split("\\s+");
            if (fields.length >= 3 && fields[0].equalsIgnoreCase("SetupPart"))
            {
                EnumDriveablePart part = EnumDriveablePart.getPart(fields[1]);
                assertNotNull(part, "unknown SetupPart in " + definition);
                float health = Float.parseFloat(fields[2]);
                authored.put(part, CollisionBox.inWorldUnits(health, 0F, 0F, 0F,
                    1F, 1F, 1F, 0F, 0F));
            }
        }
        assertTrue(authored.containsKey(EnumDriveablePart.CORE), "missing core in " + definition);
        VehicleHealthScaler.Result scaled = VehicleHealthScaler.resolve(true, massKg, authored,
            ModCommonConfig.DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE);
        assertTrue(scaled.enabled(), "realistic health unavailable for " + definition);
        return scaled.allocations().get(EnumDriveablePart.CORE);
    }

    private static JsonObject category(Map<String, JsonObject> categories, String file, String name)
    {
        JsonObject result = categories.get(file).getAsJsonObject(name);
        assertNotNull(result, "missing built-in category " + name + " in " + file);
        return result;
    }

    private static JsonObject readCategories(String file) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/config", file)))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
