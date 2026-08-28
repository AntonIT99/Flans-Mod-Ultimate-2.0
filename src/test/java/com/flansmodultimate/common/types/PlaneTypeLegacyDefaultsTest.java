package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy HasGear and HasDoor flags only decided whether the toggle printed
 * a message. Almost no type file declares them, so anything that gates real
 * behaviour on them has to default to on.
 */
class PlaneTypeLegacyDefaultsTest
{
    @Test
    void planesThatNeverDeclareTheFlagKeepRetractableGear()
    {
        assertTrue(parse("Model Spitfire").isHasGear());
    }

    @Test
    void packsCanStillPinTheGearDownWithEitherSpelling()
    {
        assertFalse(parse("HasGear False").isHasGear());
        assertFalse(parse("HasLandingGear false").isHasGear());
    }

    @Test
    void automaticDeploymentOnApproachIsOnUnlessAPackOptsOut()
    {
        assertTrue(parse("Model Spitfire").isAutoDeployLandingGearNearGround());
        assertFalse(parse("AutoDeployLandingGearNearGround False").isAutoDeployLandingGearNearGround());
    }

    @Test
    void planesThatNeverDeclareTheFlagKeepWorkingDoors()
    {
        assertTrue(parse("Model Spitfire").isHasDoor());
        assertFalse(parse("HasDoor False").isHasDoor());
    }

    @Test
    void automaticDoorsOpenOnLandingAndStayShutInFlightUnlessAPackOptsOut()
    {
        PlaneType stock = parse("Model Spitfire");
        assertTrue(stock.isAutoOpenDoorsNearGround());
        assertFalse(stock.isFlyWithOpenDoor());
        assertTrue(parse("FlyWithOpenDoor True").isFlyWithOpenDoor());
    }

    private static PlaneType parse(String... lines)
    {
        // A driver line keeps DriveableType.read off its logging path, which
        // would pull in the mod class and its registries.
        List<String> definition = new ArrayList<>(List.of("Pilot 0 0 0"));
        definition.addAll(List.of(lines));
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "test"));
        PlaneType type = new PlaneType();
        type.read(new TypeFile("testPlane", EnumType.PLANE, pack, definition));
        return type;
    }
}
