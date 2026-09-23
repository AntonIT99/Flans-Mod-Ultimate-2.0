package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluidFuelTest
{
    private static ResourceLocation fluid(String namespace, String path)
    {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @AfterEach
    void clearTable()
    {
        FluidFuel.rebuild(List.of());
    }

    @Test
    void aWildcardCoversEveryGradeAndHeatVariantOfItsFamily()
    {
        FluidFuel.rebuild(List.of("buildcraftenergy:oil*; 1000", "buildcraftenergy:fuel*; 2000"));

        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));
        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil_heavy")));
        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil_heat_2")));
        assertEquals(2000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "fuel_light")));
        assertEquals(2000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "fuel_dense_heat_1")));
    }

    @Test
    void nothingBurnsUnlessTheTableSaysSo()
    {
        FluidFuel.rebuild(List.of("buildcraftenergy:oil*; 1000"));

        assertEquals(0, FluidFuel.fuelPerBucket(fluid("minecraft", "water")),
            "water must never refuel a driveable");
        assertEquals(0, FluidFuel.fuelPerBucket(fluid("minecraft", "lava")));
        assertEquals(0, FluidFuel.fuelPerBucket(fluid("someothermod", "oil")),
            "a wildcard is scoped to the namespace that declared it");
    }

    @Test
    void anExactEntryAboveAWildcardOverridesIt()
    {
        FluidFuel.rebuild(List.of("buildcraftenergy:oil_residue; 250", "buildcraftenergy:oil*; 1000"));

        assertEquals(250, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil_residue")));
        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));
    }

    @Test
    void aWildcardDoesNotMatchAShorterPathThanItsPrefix()
    {
        FluidFuel.rebuild(List.of("buildcraftenergy:oil_heavy*; 1500"));

        assertEquals(1500, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil_heavy")));
        assertEquals(0, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));
    }

    @Test
    void malformedAndCommentedLinesAreSkippedWithoutLosingTheRest()
    {
        FluidFuel.rebuild(List.of(
            "# a comment",
            "",
            "missing a semicolon",
            "buildcraftenergy:oil; not a number",
            "nonamespace; 500",
            "buildcraftenergy:fuel_light; 0",
            "buildcraftenergy:fuel_light; -5",
            "buildcraftenergy:oil; 1000"));

        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));
        assertEquals(0, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "fuel_light")),
            "a non-positive fuel value is rejected rather than stored");
    }

    @Test
    void rebuildingReplacesTheWholeTable()
    {
        FluidFuel.rebuild(List.of("buildcraftenergy:oil*; 1000"));
        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));

        FluidFuel.rebuild(List.of("thermal:crude_oil; 800"));
        assertEquals(0, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil")));
        assertEquals(800, FluidFuel.fuelPerBucket(fluid("thermal", "crude_oil")));
    }

    @Test
    void entriesAreCaseInsensitiveBecauseRegistryIdsAreLowerCase()
    {
        FluidFuel.rebuild(List.of("BuildCraftEnergy:Oil*; 1000"));

        assertEquals(1000, FluidFuel.fuelPerBucket(fluid("buildcraftenergy", "oil_dense")));
    }
}
