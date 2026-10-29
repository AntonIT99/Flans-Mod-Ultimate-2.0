package com.flansmodultimate.common.types;

import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class DriveableMeleeThresholdTest
{
    @Test
    void definitionsInheritTheLiveServerDefaultUnlessExplicitlyOverridden()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::driveableMeleeDamageThreshold).thenReturn(10F);
            DriveableType inherited = parse("Name Test");
            assertEquals(10F, inherited.getMeleeDamageThreshold());
            assertEquals(25F, parse("MeleeDamageThreshold 25").getMeleeDamageThreshold());
            assertEquals(0F, parse("MeleeDamageThreshold 0").getMeleeDamageThreshold());
            for (String value : List.of("-1", "NaN", "Infinity", "invalid"))
                assertEquals(10F, parse("MeleeDamageThreshold " + value).getMeleeDamageThreshold());
            config.when(ModCommonConfig::driveableMeleeDamageThreshold).thenReturn(15F);
            assertEquals(15F, inherited.getMeleeDamageThreshold());
        }
    }

    private static DriveableType parse(String line)
    {
        DriveableType type = new DriveableType();
        type.read(new TypeFile("testVehicle", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "test")), List.of("Pilot 0 0 0", line)));
        return type;
    }
}
