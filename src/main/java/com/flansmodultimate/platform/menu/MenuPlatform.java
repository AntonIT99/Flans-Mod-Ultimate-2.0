package com.flansmodultimate.platform.menu;

import com.flansmodultimate.network.PacketBuffer;
import net.neoforged.neoforge.network.IContainerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Consumer;

/** Loader boundary for menu types and for opening menus whose client side needs extra network data. */
public final class MenuPlatform
{
    private MenuPlatform() {}

    /** Creates the client-side menu from the extra data written when the menu was opened. */
    @FunctionalInterface
    public interface NetworkMenuFactory<T extends AbstractContainerMenu>
    {
        T create(int containerId, Inventory inventory, PacketBuffer data);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> menuType(NetworkMenuFactory<T> factory)
    {
        IContainerFactory<T> containerFactory = (containerId, inventory, data) ->
            factory.create(containerId, inventory, data == null ? null : new PacketBuffer(data));
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos)
    {
        player.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
    }

    public static void open(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData)
    {
        player.openMenu(provider, extraData);
    }
}
