package com.flansmod.client.tmt;

import net.minecraft.world.phys.Vec3;

public abstract class TransformGroup
{
	public abstract double getWeight();

	public abstract Vec3 doTransformation(PositionTransformVertex vertex);
}
