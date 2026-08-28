package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PartTypeDefaultEngineTest
{
    @AfterEach
    void clearDefaults()
    {
        PartType.clearDefaultEngines();
    }

    @Test
    void samePackEngineWinsOverFasterGlobalEngine()
    {
        IContentProvider titanPack = pack("titan");
        IContentProvider spacePack = pack("space");
        PartType titanCpu = engine(titanPack, 1F, true, EnumType.MECHA);
        PartType spaceEngine = engine(spacePack, 20F, true, EnumType.MECHA);

        titanCpu.registerAsDefaultEngine();
        spaceEngine.registerAsDefaultEngine();

        assertSame(titanCpu, PartType.getDefaultEngine(EnumType.MECHA, titanPack));
        assertSame(titanCpu, PartType.getDefaultEngine(EnumType.MECHA));
    }

    @Test
    void worksWithAndCanBeDefaultEngineAreRequired()
    {
        IContentProvider pack = pack("restricted");
        PartType planeOnly = engine(pack, 30F, true, EnumType.PLANE);
        PartType optedOut = engine(pack, 40F, false, EnumType.MECHA);

        planeOnly.registerAsDefaultEngine();
        optedOut.registerAsDefaultEngine();

        assertNull(PartType.getDefaultEngine(EnumType.MECHA, pack));
        assertSame(planeOnly, PartType.getDefaultEngine(EnumType.PLANE, pack));
    }

    @Test
    void officialGlobalEngineWinsBeforeClosestNonOfficialEngine()
    {
        PartType exactCustom = engine(pack("custom"), 1F, true, EnumType.MECHA);
        PartType official = engine(officialPack("official"), 0.7F, true, EnumType.MECHA);
        exactCustom.registerAsDefaultEngine();
        official.registerAsDefaultEngine();

        assertSame(official, PartType.getDefaultEngine(EnumType.MECHA, pack("empty")));
    }

    @Test
    void configuredEngineAcceptsShortnameAndNamespacedItemId()
    {
        PartType configured = engine(pack("configured"), 3F, true, EnumType.VEHICLE);
        configured.originalShortName = "configuredEngine";
        configured.registerAsDefaultEngine();

        assertSame(configured, PartType.findConfiguredDefault(EnumType.VEHICLE, "configuredEngine"));
        assertSame(configured, PartType.findConfiguredDefault(EnumType.VEHICLE, "flansmod:configuredEngine"));
        assertNull(PartType.findConfiguredDefault(EnumType.PLANE, "configuredEngine"));
        assertNull(PartType.findConfiguredDefault(EnumType.VEHICLE, "missingEngine"));
    }

    @Test
    void driveableEngineOverrideWinsAndInvalidOverrideFallsBack()
    {
        IContentProvider pack = pack("driveable");
        PartType local = engine(pack, 1F, true, EnumType.VEHICLE);
        local.originalShortName = "localEngine";
        PartType override = engine(pack("external"), 5F, true, EnumType.VEHICLE);
        override.originalShortName = "overrideEngine";
        local.registerAsDefaultEngine();
        override.registerAsDefaultEngine();

        assertSame(override, PartType.getDefaultEngine(EnumType.VEHICLE, pack, "flansmod:overrideEngine"));
        assertSame(local, PartType.getDefaultEngine(EnumType.VEHICLE, pack, "missingEngine"));
    }

    private static PartType engine(IContentProvider pack, float speed, boolean canBeDefault, EnumType... worksWith)
    {
        PartType engine = new PartType();
        engine.contentPack = pack;
        engine.category = PartType.Category.ENGINE;
        engine.engineSpeed = speed;
        engine.canBeDefaultEngine = canBeDefault;
        engine.worksWith = EnumSet.copyOf(java.util.List.of(worksWith));
        return engine;
    }

    private static IContentProvider pack(String name)
    {
        return new ContentPack(name, Path.of("build", "test-packs", name));
    }

    private static IContentProvider officialPack(String name)
    {
        return new ContentPack(name, Path.of("build", "test-packs", name))
        {
            @Override
            public boolean isOfficial()
            {
                return true;
            }
        };
    }
}
