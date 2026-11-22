package com.flansmodultimate.common.entity.geometry;

import com.flansmodultimate.api.driveables.DriveableAttitude;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** The aircraft pose relative to the specific structure and block carrying its landing gear. */
public record PlaneTieDown(UUID deck, BlockPos support, Vec3 position, Vec3 front, Vec3 roof)
{
    public static final Codec<PlaneTieDown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("deck").forGetter(PlaneTieDown::deck),
        BlockPos.CODEC.fieldOf("support").forGetter(PlaneTieDown::support),
        Vec3.CODEC.fieldOf("position").forGetter(PlaneTieDown::position),
        Vec3.CODEC.fieldOf("front").forGetter(PlaneTieDown::front),
        Vec3.CODEC.fieldOf("roof").forGetter(PlaneTieDown::roof)).apply(instance, PlaneTieDown::new));

    public static PlaneTieDown capture(EmplacementFrame frame, BlockPos support, Vec3 position, DriveableAttitude attitude)
    {
        return new PlaneTieDown(frame.deckId(), support.immutable(), frame.localPosition(position), frame.localDirection(attitude.front()), frame.localDirection(attitude.roof()));
    }

    public DriveableAttitude worldAttitude(EmplacementFrame frame)
    {
        return new DriveableAttitude(frame.worldDirection(front), frame.worldDirection(roof));
    }
}
