package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.flansmodultimate.api.projectiles.BallisticDrag;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Speed-dependent projectile drag: the coefficient rules, the closed forms and the aim fit. */
class ProjectileDragModelTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "drag"));

    @Test
    void massCalibrationMatchesTheReferenceRifleRoundAndScalesWithMass()
    {
        BulletType type = bullet();
        double rifle = ProjectileDragModel.coefficient(type, 4F, 45F);
        assertEquals(0.7D, ProjectileDragModel.speedShare(rifle, 300D), 1e-9D);
        double heavy = ProjectileDragModel.coefficient(type, 42F, 45F);
        assertEquals(rifle * Math.pow(42D / 4D, -1D / 3D), heavy, 1e-12D);
        assertTrue(ProjectileDragModel.speedShare(heavy, 300D) > 0.84D);
        assertTrue(ProjectileDragModel.speedShare(ProjectileDragModel.coefficient(type, 4500F, 80F), 1000D) > 0.88D);
    }

    @Test
    void roundsWithoutMassOrWithAuthoredDragKeepConstantRetention()
    {
        assertEquals(0D, ProjectileDragModel.coefficient(bullet(), 0F, 45F));
        assertEquals(0D, ProjectileDragModel.coefficient(bullet("DragInAir 0.98"), 9F, 45F));
        assertEquals(0D, ProjectileDragModel.coefficient(bullet("QuadraticDrag false"), 9F, 45F));
    }

    @Test
    void forcedConversionKeepsTheAuthoredRetentionAtTheMuzzle()
    {
        double coefficient = ProjectileDragModel.coefficient(bullet("QuadraticDrag true", "DragInAir 0.98"), 0F, 10F);
        assertEquals(0.98D, BallisticDrag.retention(coefficient, 10D), 1e-6D);
        assertTrue(BallisticDrag.retention(coefficient, 5D) > 0.98D, "a slower round loses less");
        assertEquals(0D, ProjectileDragModel.coefficient(bullet("QuadraticDrag true", "DragInAir 1"), 0F, 10F));
    }

    @Test
    void multiplierScalesTheCoefficient()
    {
        double base = ProjectileDragModel.coefficient(bullet(), 9F, 45F);
        assertEquals(2D * base, ProjectileDragModel.coefficient(bullet("QuadraticDragMultiplier 2"), 9F, 45F), 1e-12D);
        assertEquals(0D, ProjectileDragModel.coefficient(bullet("QuadraticDragMultiplier -1"), 9F, 45F));
    }

    @Test
    void poweredGuidedDroppedAndUnderwaterOrdnanceKeepsItsOwnMotion()
    {
        for (String line : List.of("Missile", "Bomb", "Torpedo true", "depthCharge true", "ManualGuidance true", "LaserGuidance true", "EnableSACLOS true"))
            assertEquals(0D, ProjectileDragModel.coefficient(bullet(line), 9F, 45F), line);
        assertTrue(ProjectileDragModel.coefficient(bullet("Shell"), 9000F, 40F) > 0D);
    }

    @Test
    void penetrationFollowsDeMarreFromItsHundredMetreRating()
    {
        double coefficient = ProjectileDragModel.coefficient(bullet(), 4F, 45F);
        assertEquals(1D, ProjectileDragModel.penetrationShare(coefficient, 100D, 1.43D), 1e-12D);
        assertEquals(Math.pow(ProjectileDragModel.speedShare(coefficient, 200D), 1.43D), ProjectileDragModel.penetrationShare(coefficient, 300D, 1.43D), 1e-9D);
        // A rifle round point-blank penetrates about 18% more than its 100 m rating, as its 100 m speed loss implies.
        assertEquals(1.185D, ProjectileDragModel.penetrationShare(coefficient, 0D, 1.43D), 0.005D);
        assertEquals(Math.pow(ProjectileDragModel.speedShare(coefficient, 200D), 1D), ProjectileDragModel.penetrationShare(coefficient, 300D, 1D), 1e-9D);
        assertEquals(1D, ProjectileDragModel.penetrationShare(coefficient, 900D, 0D));
        assertEquals(1D, ProjectileDragModel.penetrationShare(0D, 900D, 1.43D));
    }

    @Test
    void penetrationExponentDefaultsToDeMarreAndIsBounded()
    {
        assertEquals(1.43F, bullet().getPenetrationVelocityExponent());
        assertEquals(1F, bullet("PenetrationVelocityExponent 1").getPenetrationVelocityExponent());
        assertEquals(0F, bullet("PenetrationVelocityExponent -2").getPenetrationVelocityExponent());
        assertEquals(3F, bullet("PenetrationVelocityExponent 9").getPenetrationVelocityExponent());
        assertEquals(1.43F, bullet("PenetrationVelocityExponent NaN").getPenetrationVelocityExponent());
    }

    @Test
    void closedFormsMatchTheDiscreteFlight()
    {
        double k = 0.002D;
        double speed = 40D;
        double velocity = speed;
        double distance = 0D;
        int ticks = 0;
        while (distance < 500D)
        {
            distance += velocity;
            velocity *= BallisticDrag.retention(k, velocity);
            ticks++;
        }
        // The closed forms are continuous; the discrete flight runs about half a step ahead of them.
        assertEquals(BallisticDrag.speedAt(speed, k, distance), velocity, velocity * 0.05D);
        assertEquals(BallisticDrag.flightTicks(speed, k, distance), ticks, 1.5D);
    }

    @Test
    void effectiveRetentionCoversTheDistanceInTheQuadraticFlightTime()
    {
        double k = 0.0012D;
        double speed = 45D;
        double distance = 400D;
        double retention = BallisticDrag.effectiveRetention(1D, k, speed, distance);
        double ticks = BallisticDrag.flightTicks(speed, k, distance);
        assertEquals(distance, speed * (1D - Math.pow(retention, ticks)) / (1D - retention), 0.01D);
        assertEquals(0.97D, BallisticDrag.effectiveRetention(0.97D, 0D, speed, distance));
        assertEquals(0.97D, BallisticDrag.effectiveRetention(0.97D, k, speed, 0D));
    }

    @Test
    void ballisticsWithoutQuadraticDragKeepTheirConstantRetention()
    {
        WeaponBallistics legacy = new WeaponBallistics(3D, 0.05D, 0.99D, false);
        assertEquals(0D, legacy.quadraticDrag());
        assertEquals(0.99D, legacy.drag(200D));
        WeaponBallistics quadratic = new WeaponBallistics(45D, 0.0245D, 1D, false, 0.0012D);
        assertTrue(quadratic.drag(400D) < 1D);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("drag", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
