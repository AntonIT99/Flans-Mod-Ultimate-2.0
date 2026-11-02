package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponEffectPacketsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void mountedShotRoundTripsEveryFieldWithoutChangingItsDirection()
    {
        PacketWeaponShotEffects sent = new PacketWeaponShotEffects("example_gun", new Vec3(1234.5D, 64D, -678.25D), new Vec3(0.25D, 0.5D, -0.75D), 1.5F, true);
        PacketBuffer encoded = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
        sent.encodeInto(encoded);
        PacketWeaponShotEffects received = new PacketWeaponShotEffects();
        received.decodeInto(encoded);
        assertEquals(0, encoded.readableBytes());
        PacketBuffer reencoded = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
        received.encodeInto(reencoded);
        assertEquals("example_gun", reencoded.readUtf());
        assertEquals(1234.5D, reencoded.readDouble());
        assertEquals(64D, reencoded.readDouble());
        assertEquals(-678.25D, reencoded.readDouble());
        assertEquals(0.25F, reencoded.readFloat());
        assertEquals(0.5F, reencoded.readFloat());
        assertEquals(-0.75F, reencoded.readFloat());
        assertEquals(1.5F, reencoded.readFloat());
        assertEquals(true, reencoded.readBoolean());
    }

    @Test
    void lockReleaseRetainsItsNegativeTargetAndOffHand()
    {
        PacketBuffer encoded = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
        new PacketLockMarker(-1, InteractionHand.OFF_HAND).encodeInto(encoded);
        PacketLockMarker received = new PacketLockMarker();
        received.decodeInto(encoded);
        PacketBuffer reencoded = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
        received.encodeInto(reencoded);
        assertEquals(-1, reencoded.readInt());
        assertEquals(InteractionHand.OFF_HAND, reencoded.readEnum(InteractionHand.class));
        assertEquals(0, reencoded.readableBytes());
    }
}
