package com.flansmod.client.tmt;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The PositionTransformGroup class adds a class which allows for vertex transformations.
 * @author GaryCXJk
 *
 */
@SuppressWarnings("unused")
public class TransformGroupBone extends TransformGroup
{
	protected Angle3D baseAngles;
	protected Vec3 baseVector;
	protected Bone attachedBone;
	protected double weight;

	public TransformGroupBone(Bone bone, double wght)
	{
		baseVector = bone.getPosition();
		baseAngles = bone.getAbsoluteAngle();
		attachedBone = bone;
		weight = wght;
	}
	
	public Angle3D getBaseAngles()
	{
		return baseAngles.copy();
	}
	
	public Angle3D getTransformAngle()
	{
		Angle3D returnAngle = attachedBone.getAbsoluteAngle().copy();
		returnAngle.angleX-= baseAngles.angleX;
		returnAngle.angleY-= baseAngles.angleY;
		returnAngle.angleZ-= baseAngles.angleZ;
		return returnAngle;
	}

	public Vec3 getBaseVector()
	{
		return new Vec3(baseVector.x, baseVector.y, baseVector.z);
	}
	
	public Vec3 getTransformVector()
	{
		return baseVector.subtract(attachedBone.getPosition());
	}
	
	public Vec3 getCurrentVector()
	{
		return attachedBone.getPosition();
	}
	
	@Override
	public double getWeight()
	{
		return weight;
	}
	
	public void attachBone(Bone bone)
	{
		baseVector = bone.getPosition();
		baseAngles = bone.getAbsoluteAngle();
		attachedBone = bone;
	}
	
	@Override
	public Vec3 doTransformation(PositionTransformVertex vertex)
	{
		Vec3 vector = new Vec3(
				baseVector.x - vertex.neutralVector.x,
				baseVector.y - vertex.neutralVector.y,
				baseVector.z - vertex.neutralVector.z);
		Angle3D angle = attachedBone.absoluteAngles;
		return setVectorRotations(vector,
				angle.angleX - baseAngles.angleX,
				angle.angleY - baseAngles.angleY,
				angle.angleZ - baseAngles.angleZ);
	}

	protected Vec3 setVectorRotations(Vec3 vector, float xRot, float yRot, float zRot)
	{
		float xC = Mth.cos(xRot);
		float xS = Mth.sin(xRot);
		float yC = Mth.cos(yRot);
		float yS = Mth.sin(yRot);
		float zC = Mth.cos(zRot);
		float zS = Mth.sin(zRot);

		double xVec = vector.x;
		double yVec = vector.y;
		double zVec = vector.z;

		// rotation around x
		double xy = xC * yVec - xS * zVec;
		double xz = xC * zVec + xS * yVec;
		// rotation around y
		double yz = yC * xz - yS * xVec;
		double yx = yC * xVec + yS * xz;
		// rotation around z
		double zx = zC * yx - zS * xy;
		double zy = zC * xy + zS * yx;

		xVec = zx;
		yVec = zy;
		zVec = yz;

		return new Vec3(xVec, yVec, zVec);
	}
}
