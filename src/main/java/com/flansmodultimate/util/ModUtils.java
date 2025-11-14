package com.flansmodultimate.util;

import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModUtils
{
    /**
     * Returns true iff the given player is the local client player.
     * Always false on a dedicated server.
     * Safe for common code: only accesses Minecraft client classes when running on CLIENT.
     */
    public static boolean isThePlayer(@Nullable Player player)
    {
        if (player == null)
            return false;

        // Avoid classloading client-only classes on server
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            LocalPlayer local = mc.player;
            return local != null && local.getUUID().equals(player.getUUID());
        }
        return false;
    }

    public static List<Entity> queryEntities(Level level, @Nullable Entity except, AABB box, @Nullable Predicate<? super Entity> filter)
    {
        return level.getEntities(except, box, e -> e != null && e.isAlive() && (except == null || e != except) && (filter == null || filter.test(e)));
    }

    public static <T extends Entity> List<T> queryEntities(Level level, @Nullable Entity except, AABB box, Class<T> type, @Nullable Predicate<? super T> filter)
    {
        return level.getEntitiesOfClass(type, box, e -> e != null && e.isAlive() && (except == null || e != except) && (filter == null || filter.test(e)));
    }

    public static List<Entity> queryEntitiesInRange(Level level, Entity source, double radius, @Nullable Predicate<? super Entity> filter)
    {
        AABB box = source.getBoundingBox().inflate(radius);
        return queryEntities(level, source, box, filter);
    }

    public static <T extends Entity> List<T> queryEntitiesInRange(Level level, Entity source, double radius, Class<T> type, @Nullable Predicate<? super T> filter)
    {
        AABB box = source.getBoundingBox().inflate(radius);
        return queryEntities(level, source, box, type, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, @Nullable Entity except, AABB box, @Nullable Predicate<? super LivingEntity> filter)
    {
        return queryEntities(level, except, box, LivingEntity.class, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, AABB box, @Nullable Predicate<? super LivingEntity> filter)
    {
        return queryEntities(level, null, box, LivingEntity.class, filter);
    }

    public static List<LivingEntity> queryLivingEntities(Level level, AABB box)
    {
        return queryEntities(level, null, box, LivingEntity.class, null);
    }

    public static Optional<ItemStack> getItemStack(@Nullable String id, int amount, int damage)
    {
        Optional<Item> item = getItemById(id);
        if (item.isPresent())
        {
            ItemStack stack = new ItemStack(item.get(), amount);
            if (damage > 0)
                stack.setDamageValue(damage);
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    public static Optional<Item> getItemById(@Nullable String id)
    {
        if (id == null || id.isBlank())
            return Optional.empty();

        id = ResourceUtils.sanitize(id.trim());

        // If no namespace, assume minecraft
        if (!id.contains(":"))
            id = "minecraft:" + id;

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null)
            return Optional.empty();

        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(rl));
    }

    public static boolean isGlass(BlockState state)
    {
        return state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE);
    }

    public static void destroyBlock(ServerLevel level, BlockPos pos, @Nullable Entity cause, boolean dropBlock)
    {
        Player player;

        if (cause instanceof Player p)
        {
            // real player
            player = p;
        }
        else
        {
            // fake player (Forge helper)
            GameProfile profile = new GameProfile(UUID.randomUUID(), "fakePlayer");
            player = FakePlayerFactory.get(level, profile);
        }

        BlockState state = level.getBlockState(pos);

        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(breakEvent);

        if (breakEvent.isCanceled())
            return;

        level.destroyBlock(pos, dropBlock);
    }
}
