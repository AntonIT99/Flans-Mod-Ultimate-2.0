package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.types.ShootableType.EnumFragType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-detonation fragmentation balance targets for a full-health, unarmoured, fully exposed
 * player. Distance is from the detonation to the player's entity position, with no block cover.
 * Only the fragment channel is tested; nearby blast damage cannot mask a weak frag channel.
 * "Survives" means under 20 HP of deterministic damage in this model, not that a real stray
 * fragment could never be fatal at that distance.
 *
 * <p>The U.S. military quotes a 5 m lethal and 15 m casualty radius for the M67, and its WWII
 * medical history describes severe close-range grenade wounds, antipersonnel bomb fragments,
 * and the much longer reach of the S-Mine 35. These sources guide qualitative targets; they do
 * not give a guaranteed kill probability for each test distance. See
 * https://www.marines.mil/News/News-Display/Article/554615/marines-refresh-combat-skills-through-hand-grenade-training/
 * and https://achh.army.mil/history/book-wwii-woundblstcs-chapter1/.</p>
 */
class FragmentationPlayerBalanceTest
{
    private static final float FULL_PLAYER_HEALTH = 20F;

    private record Scenario(String explosive, String categoryFile, double distanceMeters, boolean kills) {}

    private static Scenario kills(String explosive, double distanceMeters)
    {
        return new Scenario(explosive, "grenade_categories.json", distanceMeters, true);
    }

    private static Scenario survives(String explosive, double distanceMeters)
    {
        return new Scenario(explosive, "grenade_categories.json", distanceMeters, false);
    }

    private static Scenario munitionKills(String explosive, double distanceMeters)
    {
        return new Scenario(explosive, "bullet_categories.json", distanceMeters, true);
    }

    private static Scenario munitionSurvives(String explosive, double distanceMeters)
    {
        return new Scenario(explosive, "bullet_categories.json", distanceMeters, false);
    }

    @TestFactory
    Stream<DynamicTest> realFragmentationWeaponsAgainstExposedPlayers() throws IOException
    {
        List<Scenario> cases = List.of(
            // The published M67 lethal radius is 5 m; 15 m is a casualty, not a certain-kill radius.
            kills("M67 Fragmentation Hand Grenade", 2D),
            kills("M67 Fragmentation Hand Grenade", 5D),
            survives("M67 Fragmentation Hand Grenade", 8D),
            survives("M67 Fragmentation Hand Grenade", 15D),
            survives("M67 Fragmentation Hand Grenade", 25D),

            kills("Mk 2 Pineapple Grenade", 2D),
            kills("Mk 2 Pineapple Grenade", 4D),
            survives("Mk 2 Pineapple Grenade", 12D),
            survives("Mk 2 Pineapple Grenade", 20D),
            kills("F1 Fragmentation Grenade", 2D),
            kills("F1 Fragmentation Grenade", 4D),
            survives("F1 Fragmentation Grenade", 15D),
            kills("Mills Bomb", 2D),
            kills("Mills Bomb", 4D),
            survives("Mills Bomb", 15D),

            kills("RGD-33 Grenade", 2D),
            kills("RGD-33 Grenade", 5D),
            survives("RGD-33 Grenade", 15D),
            survives("RGD-33 Grenade", 25D),
            kills("Stielhandgranate 43", 2D),
            kills("Stielhandgranate 43", 5D),
            survives("Stielhandgranate 43", 20D),
            kills("Type 97 Grenade", 2D),
            kills("Type 97 Grenade", 4D),
            survives("Type 97 Grenade", 15D),
            kills("Kugelrohrhandgranate", 2D),
            survives("Kugelrohrhandgranate", 20D),

            // The S-Mine scatters steel balls after jumping above the ground.
            kills("S-Mine 35", 5D),
            kills("S-Mine 35", 15D),
            survives("S-Mine 35", 30D),
            survives("S-Mine 35", 100D),
            kills("25mm HE Airburst Grenade", 1D),
            survives("25mm HE Airburst Grenade", 10D),
            survives("25mm HE Airburst Grenade", 20D),
            kills("20x28mm HE Airburst Grenade", 1D),
            survives("20x28mm HE Airburst Grenade", 15D),

            munitionKills("40x53mm HV Grenade", 1D),
            munitionKills("40x53mm HV Grenade", 3D),
            munitionSurvives("40x53mm HV Grenade", 10D),
            munitionKills("75mm M48 HE", 5D),
            munitionKills("75mm M48 HE", 10D),
            munitionSurvives("75mm M48 HE", 25D),
            munitionKills("88mm Sprgr.43", 5D),
            munitionKills("88mm Sprgr.43", 15D),
            munitionSurvives("88mm Sprgr.43", 30D),
            munitionKills("SC 10 Fragmentation Bomb", 5D),
            munitionKills("SC 10 Fragmentation Bomb", 15D),
            munitionSurvives("SC 10 Fragmentation Bomb", 40D)
        );

        Map<String, JsonObject> categories = Map.of(
            "grenade_categories.json", readCategories("grenade_categories.json"),
            "bullet_categories.json", readCategories("bullet_categories.json"));
        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
            testCase.explosive + " at " + testCase.distanceMeters + " m -> "
                + (testCase.kills ? "frag kill" : "survive fragments"),
            () -> assertBalance(testCase, categories)));
    }

    private static void assertBalance(Scenario testCase, Map<String, JsonObject> categories)
    {
        JsonObject category = categories.get(testCase.categoryFile).getAsJsonObject(testCase.explosive);
        assertNotNull(category, "missing built-in category " + testCase.explosive);
        JsonObject properties = category.getAsJsonObject("properties");
        assertTrue(category.getAsJsonArray("items").size() > 0, "category has no shipped item");
        EnumFragType fragType = EnumFragType.valueOf(properties.get("FragType").getAsString());
        assertTrue(fragType != EnumFragType.DEFAULT, "scenario must use a fragmentation casing");
        float chargeKg = properties.has("ExplosiveMassTNTKg")
            ? properties.get("ExplosiveMassTNTKg").getAsFloat()
            : properties.get("ExplosiveMassTNTg").getAsFloat() / 1000F;
        assertTrue(chargeKg > 0F);

        float totalMass = properties.has("Mass") ? properties.get("Mass").getAsFloat() : 0F;
        float peakDamage = properties.has("FragDamage") ? properties.get("FragDamage").getAsFloat()
            : ExplosionScaling.fragPeakDamage(fragType.kFragDamage);
        double count = properties.has("FragCount") ? properties.get("FragCount").getAsDouble() : 0D;
        double metal = properties.has("FragMetalMassg") ? properties.get("FragMetalMassg").getAsDouble() : 0D;
        FragmentationModel.Pattern pattern = properties.has("FragPattern")
            ? FragmentationModel.Pattern.valueOf(properties.get("FragPattern").getAsString())
            : FragmentationModel.Pattern.RADIAL;
        FragmentationModel.Burst burst = FragmentationModel.create(fragType, chargeKg, totalMass,
            metal, count, pattern).withPeak(peakDamage);
        double hitChance = burst.hitChance(1D, testCase.distanceMeters, 1D);
        float damage = (float) burst.damage(peakDamage, 1D, testCase.distanceMeters, 1D);
        String result = String.format(java.util.Locale.ROOT,
            "%s at %.1f m: %s, %.3f kg TNT, %,.0f fragments, effective reach %.1f m, peak %.1f HP, hit chance %.3f, dealt %.1f/%.1f HP",
            testCase.explosive, testCase.distanceMeters, fragType, chargeKg,
            burst.fragmentCount(), burst.queryRadius(), peakDamage, hitChance, damage, FULL_PLAYER_HEALTH);
        assertEquals(testCase.kills, damage >= FULL_PLAYER_HEALTH, result);
    }

    private static JsonObject readCategories(String file) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/config", file)))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
