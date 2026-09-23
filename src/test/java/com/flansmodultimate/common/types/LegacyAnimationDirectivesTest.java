package com.flansmodultimate.common.types;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.EnumMeleeAnimation;
import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Definition-level overrides 1.7.10 read from gun and ammo type files. */
class LegacyAnimationDirectivesTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "animation"));

    @Test
    void gunDefinitionsOverrideTheModelsAnimationTypes()
    {
        GunType type = gun("animAnimationType CUSTOMRIFLE", "animMeleeAnimation stab_underarm");
        assertEquals(EnumAnimationType.CUSTOMRIFLE, type.getAnimationConfig().getAnimationType());
        assertEquals(EnumMeleeAnimation.STAB_UNDERARM, type.getAnimationConfig().getMeleeAnimation());

        ModelGun model = new ModelGun();
        model.setAnimationType(EnumAnimationType.BOTTOM_CLIP);
        model.setType(type);
        assertEquals(EnumAnimationType.CUSTOMRIFLE, model.getAnimationType());
        assertEquals(EnumMeleeAnimation.STAB_UNDERARM, model.getMeleeAnimation());
    }

    @Test
    void theModelKeepsItsOwnAnimationWhenTheDefinitionIsSilent()
    {
        GunType type = gun();
        assertNull(type.getAnimationConfig().getAnimationType());
        assertNull(type.getAnimationConfig().getMeleeAnimation());

        ModelGun model = new ModelGun();
        model.setAnimationType(EnumAnimationType.PISTOL_CLIP);
        model.setType(type);
        assertEquals(EnumAnimationType.PISTOL_CLIP, model.getAnimationType());
        assertEquals(EnumMeleeAnimation.DEFAULT, model.getMeleeAnimation());
    }

    @Test
    void revolverTwoNamesResolveToTheRevolverAnimationAsIn1710()
    {
        assertEquals(EnumAnimationType.REVOLVER, gun("animAnimationType REVOLVER2").getAnimationConfig().getAnimationType());
        assertEquals(EnumAnimationType.CUSTOMREVOLVER,
            gun("animAnimationType customRevolver2").getAnimationConfig().getAnimationType());
    }

    @Test
    void ammoKeepsTheDetailedTooltipUnlessThePackOptsOut()
    {
        assertTrue(bullet().isFancyDescription());
        assertFalse(bullet("FancyDescription false").isFancyDescription());
    }

    private static GunType gun(String... lines)
    {
        GunType type = new GunType();
        TypeFile file = new TypeFile("testGun", EnumType.GUN, PACK, List.of(lines));
        type.read(file);
        // Animation settings are read by the client-only readClient, which loads models too.
        type.animationConfig.read(file);
        return type;
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("testBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
