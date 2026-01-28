package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PaintjobTableBlockEntity extends BlockEntity implements MenuProvider
{
    public static final String NBT_ITEMS = "items";
    public static final int SLOT_PAINTABLE = 0;
    public static final int SLOT_PAINTCANS = 1;

    private final ItemStackHandler items = new ItemStackHandler(2)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();

    public PaintjobTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(FlansMod.paintjobTableEntity.get(), pos, state);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        itemCap = LazyOptional.of(() -> items);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_ITEMS, items.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        items.deserializeNBT(tag.getCompound(NBT_ITEMS));
    }

    // MenuProvider
    @Override
    @NotNull
    public Component getDisplayName()
    {
        return Component.translatable("container.<modid>.paintjob_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player)
    {
        if (level != null)
            return new PaintjobTableMenu(id, inv, ContainerLevelAccess.create(level, worldPosition), this);
        else
            return null;
    }

    public ItemStack getPaintableStack()
    {
        return items.getStackInSlot(SLOT_PAINTABLE);
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