package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PaintjobTableBlockEntity extends BlockEntity implements MenuProvider
{
    public static final String NBT_ITEMS = "items";

    private final BlockState blockState;

    private final ItemStackHandler items = new ItemStackHandler(2)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    public PaintjobTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(FlansMod.paintjobTableEntity.get(), pos, state);
        blockState = state;
    }

    public IItemHandler getItemHandler()
    {
        return items;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put(NBT_ITEMS, items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound(NBT_ITEMS));
    }

    @Override
    @NotNull
    public Component getDisplayName()
    {
        return blockState.getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player)
    {
        if (level != null)
            return new PaintjobTableMenu(id, inv, worldPosition, this);
        else
            return null;
    }

    public void dropContents(Level level, BlockPos pos)
    {
        if (level instanceof ServerLevel)
        {
            Containers.dropContents(level, pos, new SimpleContainer(
                items.getStackInSlot(0),
                items.getStackInSlot(1)
            ));
            items.setStackInSlot(0, ItemStack.EMPTY);
            items.setStackInSlot(1, ItemStack.EMPTY);
        }
    }
}
