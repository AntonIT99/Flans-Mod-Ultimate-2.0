package com.flansmodultimate.common.entity.geometry;

import com.flansmodultimate.api.driveables.DriveableAttitude;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** The driveable pose relative to the specific structure and block supporting its wheels. */
public record DeckTieDown(UUID deck, BlockPos support, Vec3 position, Vec3 front, Vec3 roof)
{
    public static final Codec<DeckTieDown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("deck").forGetter(DeckTieDown::deck),
        BlockPos.CODEC.fieldOf("support").forGetter(DeckTieDown::support),
        Vec3.CODEC.fieldOf("position").forGetter(DeckTieDown::position),
        Vec3.CODEC.fieldOf("front").forGetter(DeckTieDown::front),
        Vec3.CODEC.fieldOf("roof").forGetter(DeckTieDown::roof)).apply(instance, DeckTieDown::new));

    public static DeckTieDown capture(EmplacementFrame frame, BlockPos support, Vec3 position, DriveableAttitude attitude)
    {
        return new DeckTieDown(frame.deckId(), support.immutable(), frame.localPosition(position), frame.localDirection(attitude.front()), frame.localDirection(attitude.roof()));
    }

    public DriveableAttitude worldAttitude(EmplacementFrame frame)
    {
        return new DriveableAttitude(frame.worldDirection(front), frame.worldDirection(roof));
    }
}
