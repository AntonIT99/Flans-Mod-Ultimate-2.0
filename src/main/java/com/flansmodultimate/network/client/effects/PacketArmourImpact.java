package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.FlansModSounds;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.guns.ArmourImpactEffects;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** One server-confirmed armour contact, expanded locally through the existing particle budget and render hooks. */
@NoArgsConstructor
public class PacketArmourImpact implements IClientPacket
{
    private Vec3 position;
    private Vec3 incoming;
    private Vec3 normal;
    private float scale;
    private boolean blocked;

    public PacketArmourImpact(Vec3 position, Vec3 incoming, Vec3 normal, float scale, boolean blocked)
    {
        this.position = position;
        this.incoming = incoming;
        this.normal = normal;
        this.scale = scale;
        this.blocked = blocked;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeFloat((float) incoming.x);
        data.writeFloat((float) incoming.y);
        data.writeFloat((float) incoming.z);
        data.writeFloat((float) normal.x);
        data.writeFloat((float) normal.y);
        data.writeFloat((float) normal.z);
        data.writeFloat(scale);
        data.writeBoolean(blocked);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        incoming = new Vec3(data.readFloat(), data.readFloat(), data.readFloat()).normalize();
        normal = new Vec3(data.readFloat(), data.readFloat(), data.readFloat()).normalize();
        scale = data.readFloat();
        blocked = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!Float.isFinite(scale) || scale <= 0F || normal.lengthSqr() < 1.0E-12D || incoming.lengthSqr() < 1.0E-12D)
            return;
        float size = Mth.clamp(scale, 0.2F, 2.5F);
        Vec3 origin = position.add(normal.scale(0.035D));
        boolean glancing = blocked && Math.abs(incoming.dot(normal)) < 0.5D;
        float flashScale = size * (blocked ? 0.22F : 0.14F);
        spawn(FlanParticles.FM_FLASH, origin, Vec3.ZERO, flashScale, 0.5F);
        spawnFragments(level, origin, size, glancing);
        if (!blocked && size >= 0.5F)
            spawn(FlanParticles.SMOKE, origin, normal.scale(0.035D), size * 0.8F, 0.45F);
        if (blocked)
            ClientHooks.SOUND.playSound(FlansModSounds.SOUND_IMPACT_METAL, position, 16F + 16F * size, true, false, false, UUID.randomUUID(), null);
    }

    private void spawnFragments(Level level, Vec3 origin, float size, boolean glancing)
    {
        Vec3 spray = blocked ? ArmourImpactEffects.reflectedDirection(incoming, normal) : normal;
        int count = Math.round((blocked ? 8F : 5F) * size);
        double scatter = glancing ? 0.07D : 0.16D;
        double speed = glancing ? 0.8D : 0.3D;
        float sparkScale = blocked ? 0.65F : 0.4F;
        float lifetime = glancing ? 0.65F : 0.3F;
        for (int i = 0; i < count; i++)
        {
            Vec3 jitter = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).scale(scatter);
            Vec3 velocity = spray.scale(speed).add(jitter);
            // Scatter stays on the exterior side of the plate.
            double inward = velocity.dot(normal);
            if (inward < 0D)
                velocity = velocity.subtract(normal.scale(inward));
            spawn(FlanParticles.FIREWORKS_SPARK, origin, velocity.scale(Math.sqrt(size)), sparkScale, lifetime);
            if (!blocked && size >= 0.5F)
                spawn(FlanParticles.BLOCK_CRACK + "_minecraft:iron_block", origin, velocity.scale(0.5D), size * 0.4F, 0.4F);
        }
    }

    private static void spawn(String particle, Vec3 position, Vec3 velocity, float scale, float lifetime)
    {
        ClientHooks.RENDER.spawnParticle(particle, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, scale, lifetime);
    }
}
