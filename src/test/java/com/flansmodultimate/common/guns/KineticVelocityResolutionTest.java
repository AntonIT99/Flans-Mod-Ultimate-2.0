package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KineticVelocityResolutionTest
{
    private static final IContentProvider PACK = new ContentPack("test",
        Path.of("build", "test-packs", "kinetic-velocity"));

    @Test
    void ammunitionVelocityTakesPrecedenceOverWeaponVelocity()
    {
        BulletType bullet = read("MuzzleVelocity 800");
        FireableGun gun = new FireableGun(bullet, 1F, 0F, 20F, EnumSpreadPattern.CIRCLE);

        assertEquals(40F, ShootingHelper.getMuzzleVelocity(bullet, 0, gun));
    }

    @Test
    void weaponVelocityIsTheFallback()
    {
        BulletType bullet = read();
        FireableGun gun = new FireableGun(bullet, 1F, 0F, 20F, EnumSpreadPattern.CIRCLE);

        assertEquals(20F, ShootingHelper.getMuzzleVelocity(bullet, 0, gun));
    }

    @Test
    void selectedBeltRoundVelocityTakesPrecedence()
    {
        BulletType bullet = read("RoundsPerItem 2", "AddRound AP 1 162 0 800 45",
            "AddRound HE 1 135 0.01 835 0");
        FireableGun gun = new FireableGun(bullet, 1F, 0F, 20F, EnumSpreadPattern.CIRCLE);

        assertEquals(40F, ShootingHelper.getMuzzleVelocity(bullet, 0, gun));
        assertEquals(41.75F, ShootingHelper.getMuzzleVelocity(bullet, 1, gun));
    }

    @Test
    void massProgressionPreservesInfantryAnchorAndSeparatesShells()
    {
        assertEquals(5F, ShootingHelper.getKineticDamage(9F, 333D / 20D), 0.01F);
        assertEquals(82.4F, ShootingHelper.getKineticDamage(162F, 800D / 20D), 0.2F);
        assertEquals(1_260F, ShootingHelper.getKineticDamage(10_200F, 773D / 20D), 2F);
    }

    private static BulletType read(String... lines)
    {
        TestBulletType type = new TestBulletType();
        type.readDefinition(new TypeFile("kineticVelocity", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static final class TestBulletType extends BulletType
    {
        private void readDefinition(TypeFile file)
        {
            read(file);
        }
    }
}
