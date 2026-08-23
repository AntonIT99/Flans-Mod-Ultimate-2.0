package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PaintjobTableBlockEntity extends BlockEntity implements MenuProvider
{
    public static final String NBT_ITEMS = "items";

    private final BlockState blockState;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(2)
    {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents)
        {
            setChanged();
        }
    };

    public PaintjobTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(FlansMod.paintjobTableBlockEntity.get(), pos, state);
        blockState = state;
    }

    @NotNull
    public ItemStacksResourceHandler getItemHandler()
    {
        return items;
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        items.serialize(output.child(NBT_ITEMS));
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        items.deserialize(input.childOrEmpty(NBT_ITEMS));
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
                ItemUtil.getStack(items, 0),
                ItemUtil.getStack(items, 1)
            ));
            items.set(0, ItemResource.EMPTY, 0);
            items.set(1, ItemResource.EMPTY, 0);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        if (level != null)
            dropContents(level, pos);
        super.preRemoveSideEffects(pos, state);
    }
}
