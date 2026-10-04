package com.flansmod.client.tmt;

import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bone-bound vertices follow the 1.7.10 TMT transformation, the primary reference. */
class TransformGroupBoneTest
{
    @Test
    void anUntouchedBoneAtTheOriginLeavesItsVerticesInPlace()
    {
        PositionTransformVertex vertex = new PositionTransformVertex(3, -2, 5, 0, 0);
        vertex.addGroup(new TransformGroupBone(new Bone(0, 0, 0, 0), 1D));
        vertex.setTransformation();
        assertEquals(new Vec3(3, -2, 5), vertex.vector3D);
    }

    @Test
    void vertexPositionsAreTakenRelativeToTheBoneBaseAndRotatedByItsAngleChange()
    {
        Bone bone = new Bone(0, 0, 0, 0);
        TransformGroupBone group = new TransformGroupBone(bone, 1D);
        PositionTransformVertex vertex = new PositionTransformVertex(1, 2, 3, 0, 0);
        vertex.addGroup(group);
        float yaw = Mth.HALF_PI;
        bone.setRotations(0, yaw, 0);
        bone.prepareDraw();
        vertex.setTransformation();
        // 1.7.10: neutral - base, then rotate around x, y and z in that order with MathHelper's tables.
        double x = 1, y = 2, z = 3;
        double cos = Mth.cos(yaw), sin = Mth.sin(yaw);
        Vec3 expected = new Vec3(cos * x + sin * z, y, cos * z - sin * x);
        assertEquals(expected.x, vertex.vector3D.x, 1E-6);
        assertEquals(expected.y, vertex.vector3D.y, 1E-6);
        assertEquals(expected.z, vertex.vector3D.z, 1E-6);
    }

    @Test
    void weightedGroupsAverageTheirTransformations()
    {
        PositionTransformVertex vertex = new PositionTransformVertex(4, 0, 0, 0, 0);
        vertex.addGroup(new TransformGroupBone(new Bone(0, 0, 0, 0), 1D));
        vertex.addGroup(new TransformGroupBone(new Bone(0, 0, 0, 0), 3D));
        vertex.setTransformation();
        assertEquals(4, vertex.vector3D.x, 1E-9);
        assertEquals(0, vertex.vector3D.y, 1E-9);
        assertEquals(0, vertex.vector3D.z, 1E-9);
    }
}
