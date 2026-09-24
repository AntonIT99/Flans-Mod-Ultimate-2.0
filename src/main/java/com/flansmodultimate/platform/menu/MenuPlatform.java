package com.flansmodultimate.platform.menu;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
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
        return IForgeMenuType.create((containerId, inventory, data) ->
            factory.create(containerId, inventory, data == null ? null : new PacketBuffer(data)));
    }

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos)
    {
        NetworkHooks.openScreen(player, provider, pos);
    }

    public static void open(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData)
    {
        NetworkHooks.openScreen(player, provider, extraData);
    }
}
