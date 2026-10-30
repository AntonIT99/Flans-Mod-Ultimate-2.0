package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunTypeProneTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void anUnauthoredGunSteadiesProneByItsSneakingModifierSquared()
    {
        assertEquals(0.63F * 0.63F, read("ShortName testGun").getProneSpreadModifier(), 1E-6F);
        assertEquals(0.25F, read("ShortName testGun", "SneakSpreadModifier 0.5").getProneSpreadModifier(), 1E-6F);
    }

    @Test
    void anAuthoredProneModifierWinsUnderEitherName()
    {
        assertEquals(0.3F, read("ShortName testGun", "SneakSpreadModifier 0.5", "ProneSpreadModifier 0.3").getProneSpreadModifier(), 1E-6F);
        assertEquals(0.2F, read("ShortName testGun", "ProneSpreadMultiplier 0.2").getProneSpreadModifier(), 1E-6F);
    }

    @Test
    void theGroundGivesMoreRecoilControlThanSneaking()
    {
        GunType unauthored = read("ShortName testGun");
        assertEquals(GunType.DEFAULT_RECOIL_CONTROL_PRONE, unauthored.getRecoilControl(ItemStack.EMPTY, false, false, true), 1E-6F);
        assertTrue(unauthored.getRecoilControl(ItemStack.EMPTY, false, false, true) < unauthored.getRecoilControl(ItemStack.EMPTY, false, true));
        assertEquals(0.36F, read("ShortName testGun", "CounterRecoilForceSneaking 0.6").getRecoilControl(ItemStack.EMPTY, false, true, true), 1E-6F);
        assertEquals(0.5F, read("ShortName testGun", "CounterRecoilForceProne 0.5").getRecoilControl(ItemStack.EMPTY, false, true, true), 1E-6F);
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
