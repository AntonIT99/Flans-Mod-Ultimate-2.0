package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableRenderPresetsTest
{
    @Test
    void presetsAreIndependentAndRecognizeCustomValues()
    {
        ForgeConfigSpec spec = ModClientConfig.configSpec;
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        spec.setConfig(config);
        try
        {
            assertEquals(ModClientConfig.RenderPreset.BALANCED, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.BALANCED, ModClientConfig.currentImpostorPreset());

            // This advanced distance factor affects both systems, so neither slider owns it.
            ConfigSpecValues.apply(spec, List.of("Entity Rendering Settings", "groundVehicleLodDistanceFactor"), 1D);
            assertEquals(ModClientConfig.RenderPreset.BALANCED, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.BALANCED, ModClientConfig.currentImpostorPreset());

            ModClientConfig.applyLodPreset(ModClientConfig.RenderPreset.OFF);
            assertEquals(ModClientConfig.RenderPreset.OFF, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.BALANCED, ModClientConfig.currentImpostorPreset());

            ModClientConfig.applyImpostorPreset(ModClientConfig.RenderPreset.PERFORMANCE);
            assertEquals(ModClientConfig.RenderPreset.OFF, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.PERFORMANCE, ModClientConfig.currentImpostorPreset());

            ModClientConfig.applyLodPreset(ModClientConfig.RenderPreset.MAXIMUM_FPS);
            assertEquals(ModClientConfig.RenderPreset.MAXIMUM_FPS, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.PERFORMANCE, ModClientConfig.currentImpostorPreset());

            ConfigSpecValues.apply(spec, List.of("Entity Rendering Settings", "minimumDriveablePartPixelSize"), 0.25D);
            assertEquals(ModClientConfig.RenderPreset.CUSTOM, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.PERFORMANCE, ModClientConfig.currentImpostorPreset());

            ModClientConfig.applyImpostorPreset(ModClientConfig.RenderPreset.OFF);
            assertEquals(ModClientConfig.RenderPreset.OFF, ModClientConfig.currentImpostorPreset());
            assertEquals(ModClientConfig.RenderPreset.CUSTOM, ModClientConfig.currentLodPreset());
            assertEquals(1D, config.get(List.of("Entity Rendering Settings", "groundVehicleLodDistanceFactor")));

            ConfigSpecValues.apply(spec, List.of("Entity Rendering Settings", "enableDriveableLod"), false);
            ModClientConfig.applyImpostorPreset(ModClientConfig.RenderPreset.QUALITY);
            assertEquals(ModClientConfig.RenderPreset.OFF, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.currentImpostorPreset());

            ConfigSpecValues.apply(spec, List.of("Entity Rendering Settings", "enableDriveableLod"), false);
            ModClientConfig.applyLodPreset(ModClientConfig.RenderPreset.QUALITY);
            assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.currentLodPreset());
            assertEquals(ModClientConfig.RenderPreset.OFF, ModClientConfig.currentImpostorPreset());
        }
        finally
        {
            spec.setConfig(null);
            ModClientConfig.flushPendingChanges();
        }
    }
}
