package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.guns.*;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;

import static org.junit.jupiter.api.Assertions.*;

class BulletTracerDataTest
{
    @BeforeAll
    static void bootstrapRegistries() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void spawnDataCarriesTheResolvedTracerEvenWithoutTheFiringWeapon()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet source = bullet(world);
            PacketBuffer buffer = new PacketBuffer(new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE));
            try
            {
                source.writeSpawnData(buffer);
                Bullet client = new Bullet(EntityType.SNOWBALL, world.level);
                // A normal spawn resolves this through the registered type; no weapon is transmitted.
                client.configType = source.configType;
                client.readSpawnData(buffer);
                assertFalse(client.isRemoved());
                assertEquals(FlanParticles.FM_TRACER_GREEN, client.roundTracerParticle);
                assertEquals(7, client.firedShot.getShot());
                assertNull(client.firedShot.getFireableGun());
                assertEquals("", client.firedShot.getTracerParticle(), "the server visual must survive even when the client cannot reconstruct the override");
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void savesTracerAndBeltPositionWithoutAGun()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Bullet source = bullet(world);
            CompoundTag tag = new CompoundTag();
            source.addAdditionalSaveData(tag);
            assertEquals(FlanParticles.FM_TRACER_GREEN, tag.getString(Bullet.NBT_ROUND_TRACER));
            assertEquals(7, tag.getInt(Bullet.NBT_SHOT));
            Bullet restored = new Bullet(EntityType.SNOWBALL, world.level);
            restored.readAdditionalSaveData(tag);
            assertFalse(restored.isRemoved());
            assertEquals(source.roundTracerParticle, restored.roundTracerParticle);
            assertEquals(7, restored.firedShot.getShot());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void restoresSavedWeaponWithCurrentAndLegacySpeedData(boolean hasMultiplier)
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            GunType type = new SavedGunType();
            type.onItemRegistration(type.getShortName());
            Bullet source = bullet(world);
            FireableGun gun = new FireableGun(type, 3F, 0.5F, 40F, 1.5F, EnumSpreadPattern.CIRCLE);
            source.firedShot = new FiredShot(gun, source.configType, null, null, 7);
            CompoundTag tag = new CompoundTag();
            source.addAdditionalSaveData(tag);
            if (!hasMultiplier)
                tag.getCompound(Bullet.NBT_FIREABLE_GUN).remove(Bullet.NBT_FIREABLE_GUN_SPEED_MULTIPLIER);

            Bullet restored = new Bullet(EntityType.SNOWBALL, world.level);
            restored.readAdditionalSaveData(tag);

            assertFalse(restored.isRemoved());
            assertEquals(FlanParticles.FM_TRACER_GREEN, restored.roundTracerParticle);
            FireableGun saved = restored.firedShot.getFireableGun();
            assertNotNull(saved);
            assertSame(type, saved.getType());
            assertEquals(40F, saved.getBulletSpeed());
            assertEquals(hasMultiplier ? 1.5F : 1F, saved.getBulletSpeedMultiplier());
        }
    }

    private static Bullet bullet(EntityTestWorld world)
    {
        BulletType type = new BulletType();
        type.onItemRegistration("tracer_data_test");
        Bullet bullet = new Bullet(EntityType.SNOWBALL, world.level);
        bullet.setShortName("tracer_data_test");
        bullet.configType = type;
        bullet.firedShot = new FiredShot(null, type, null, null, 7);
        bullet.roundTracerParticle = FlanParticles.FM_TRACER_GREEN;
        return bullet;
    }

    /** Supplies an item registry name without booting the full content loader. */
    private static final class SavedGunType extends GunType
    {
        @Override
        @NotNull
        public String getShortName()
        {
            return "saved_tracer_gun";
        }
    }
}
