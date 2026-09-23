package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableRenderPresetsTest
{
    @Test
    void presetsAreIndependentAndRecognizeCustomValues()
    {
        ModConfigSpec spec = ModClientConfig.configSpec;
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        TestConfigLoader.load(spec, config);
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

            for (ModClientConfig.RenderPreset preset : ModClientConfig.RenderPreset.values())
            {
                if (preset == ModClientConfig.RenderPreset.CUSTOM)
                    continue;
                ModClientConfig.applyLodPreset(preset);
                ModClientConfig.applyImpostorPreset(preset);
                assertEquals(preset, ModClientConfig.currentLodPreset());
                assertEquals(preset, ModClientConfig.currentImpostorPreset());
            }
            assertEquals(new ModClientConfig.LodValues(6, 16, 4, 32, 16),
                ModClientConfig.currentLodValues());
            assertEquals(new ModClientConfig.ImpostorValues(160, 16, 32, 2, 64, 8),
                ModClientConfig.currentImpostorValues());
        }
        finally
        {
            spec.acceptConfig(null);
            ModClientConfig.flushPendingChanges();
        }
    }
}
