package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShootableRecipeStackSizeTest
{
    @Test
    void recipeOutputRaisesAuthoredMaximum()
    {
        BulletType bullet = load("MaxStackSize 1", "RecipeOutput 32");
        assertEquals(32, bullet.getMaxStackSize());
    }

    @Test
    void largerAuthoredMaximumIsPreserved()
    {
        BulletType bullet = load("StackSize 64", "RecipeOutput 3");
        assertEquals(64, bullet.getMaxStackSize());
    }

    private static BulletType load(String stackSize, String recipeOutput)
    {
        BulletType bullet = new BulletType();
        bullet.load(new TypeFile("syntheticBullet", EnumType.BULLET,
            new ContentPack("test", Path.of("build", "test-packs", "recipe-stack")),
            List.of("ShortName synthetic_bullet", stackSize, recipeOutput)));
        return bullet;
    }
}
