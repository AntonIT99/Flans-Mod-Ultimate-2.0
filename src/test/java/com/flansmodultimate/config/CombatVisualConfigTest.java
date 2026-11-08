package com.flansmodultimate.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombatVisualConfigTest
{
    @Test
    void combatVisualControlsBelongToTheServerWithNoClientOverride()
    {
        Map<String, Object> defaults = Map.of(
            "vehicleScreenShake", true, "gunScreenShake", true, "explosionScreenShake", true,
            "explosionShakeIntensity", 1D, "explosionShakeDuration", 0.5D,
            "suppressionDuration", 2D, "suppressionCameraRoll", 3.5D, "suppressionVignette", 0.82D);
        defaults.forEach((key, expected) ->
        {
            var value = ConfigSpecValues.valueSpec(ModCommonConfig.configSpec, List.of("Combat Visual Settings", key));
            assertNotNull(value, key);
            assertEquals(expected, value.getDefault(), key);
            assertNull(ConfigSpecValues.valueSpec(ModClientConfig.configSpec, List.of("Screen Shake and Suppression", key)), key);
        });
        for (String key : List.of("nearMissSuppression", "suppressionIntensity"))
        {
            assertNotNull(ConfigSpecValues.valueSpec(ModCommonConfig.configSpec, List.of("Shootable Settings", key)));
            assertNull(ConfigSpecValues.valueSpec(ModClientConfig.configSpec, List.of("Screen Shake and Suppression", key)));
        }
    }
}
