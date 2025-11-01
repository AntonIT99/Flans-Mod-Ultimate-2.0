package com.wolffsarmormod;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

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

        id = id.trim().toLowerCase();

        // If no namespace, assume minecraft
        if (!id.contains(":"))
            id = "minecraft:" + id;

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null)
            return Optional.empty();

        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(rl));
    }
}
