package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class MuzzleFlashPacketTest
{
    @Test
    void shotPacketRetainsWeaponAndModelFrameGeometry()
    {
        MuzzleFlashPacket shot = new MuzzleFlashPacket(42, "browning", new Vec3(-1.5D, 2D, -3D), new Vec3(0.1D, 0.2D, -0.9D));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            shot.write(buffer);
            assertEquals(shot, MuzzleFlashPacket.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void existingNpcsEnableFlashesAndExplicitOptOutSurvivesSave()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.load(new CompoundTag());
        assertTrue(options.enabled(NpcWeaponOptions.Feature.MUZZLE_FLASHES));
        options.set(NpcWeaponOptions.Feature.SHOOT_PARTICLES, false);
        assertTrue(options.enabled(NpcWeaponOptions.Feature.MUZZLE_FLASHES));
        options.set(NpcWeaponOptions.Feature.MUZZLE_FLASHES, false);
        CompoundTag saved = new CompoundTag();
        options.save(saved);
        NpcWeaponOptions loaded = new NpcWeaponOptions();
        loaded.load(saved);
        assertFalse(loaded.enabled(NpcWeaponOptions.Feature.MUZZLE_FLASHES));
    }
}
