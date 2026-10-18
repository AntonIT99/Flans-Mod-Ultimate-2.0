package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.mojang.serialization.Lifecycle;
import org.mockito.MockedStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

/** Supplies world data while leaving Minecraft's block tracing and the entity code real. */
final class EntityTestWorld implements AutoCloseable
{
    final ServerLevel level = mock(ServerLevel.class);
    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final List<Entity> targets = new ArrayList<>();
    private final MockedStatic<ModCommonConfig> config = mockStatic(ModCommonConfig.class, CALLS_REAL_METHODS);
    // Packet transport needs the loader's networking bootstrap, which is outside these unit tests.
    private final MockedStatic<PacketHandler> packets = mockStatic(PacketHandler.class);

    EntityTestWorld()
    {
        CommonConfigSnapshot values = mock(CommonConfigSnapshot.class);
        when(values.headshotDamageModifier()).thenReturn(2F);
        when(values.chestshotDamageModifier()).thenReturn(1F);
        when(values.armshotDamageModifier()).thenReturn(0.7F);
        when(values.legshotModifier()).thenReturn(0.8F);
        config.when(ModCommonConfig::get).thenReturn(values);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        MappedRegistry<DamageType> damageTypes = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
        Registry.register(damageTypes, FlanDamageSources.SHOOTABLE, new DamageType("shootable", 0F));
        Registry.register(damageTypes, FlanDamageSources.HEADSHOT, new DamageType("headshot", 0F));
        damageTypes.freeze();
        when(level.registryAccess()).thenReturn(new RegistryAccess.ImmutableRegistryAccess(List.of(damageTypes)));
        when(level.getProfiler()).thenReturn(InactiveProfiler.INSTANCE);
        when(level.getGameRules()).thenReturn(new GameRules());
        when(level.getWorldBorder()).thenReturn(new WorldBorder());
        when(level.getMinBuildHeight()).thenReturn(-64);
        when(level.getMaxBuildHeight()).thenReturn(320);
        when(level.isPositionEntityTicking(any())).thenReturn(true);
        when(level.getBlockState(any())).thenAnswer(call -> blocks.getOrDefault(call.getArgument(0), Blocks.AIR.defaultBlockState()));
        when(level.getFluidState(any())).thenAnswer(call -> level.getBlockState(call.getArgument(0)).getFluidState());
        doCallRealMethod().when(level).clip(any(ClipContext.class));
        doCallRealMethod().when(level).clipWithInteractionOverride(any(), any(), any(), any(), any());
        when(level.getEntities(nullable(Entity.class), any(AABB.class), any())).thenAnswer(call ->
        {
            java.util.function.Predicate<Entity> filter = call.getArgument(2);
            AABB bounds = call.getArgument(1);
            return targets.stream().filter(entity -> bounds.intersects(entity.getBoundingBox()) && filter.test(entity)).toList();
        });
    }

    <T extends LivingEntity> T target(Class<T> type, Vec3 feet)
    {
        T entity = mock(type);
        when(entity.level()).thenReturn(level);
        when(entity.getUUID()).thenReturn(UUID.randomUUID());
        when(entity.isAlive()).thenReturn(true);
        when(entity.getX()).thenReturn(feet.x);
        when(entity.getY()).thenReturn(feet.y);
        when(entity.getZ()).thenReturn(feet.z);
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(entity.getBoundingBox()).thenReturn(new AABB(feet.x - 0.3D, feet.y, feet.z - 0.3D, feet.x + 0.3D, feet.y + 1.8D, feet.z + 0.3D));
        when(entity.getItemBySlot(any(EquipmentSlot.class))).thenReturn(ItemStack.EMPTY);
        targets.add(entity);
        return entity;
    }

    void tick(Entity entity)
    {
        // ServerLevel advances the age before calling Entity.tick; a unit test must do the same.
        entity.tickCount++;
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(entity::tick);
    }

    @Override
    public void close()
    {
        packets.close();
        config.close();
    }
}
