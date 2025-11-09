package com.wolffsarmormod;

import com.wolffsarmormod.util.ResourceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
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
}
