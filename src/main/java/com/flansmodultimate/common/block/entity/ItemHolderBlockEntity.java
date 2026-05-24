package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.ItemHolderBlock;
import com.flansmodultimate.common.types.ItemHolderType;
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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemHolderBlockEntity extends BlockEntity
{
    public static final String NBT_ITEMS = "items";
    public static final String NBT_TYPE = "type";

    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private ItemHolderType type;

    private final ItemStackHandler items = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChangedAndSync();
        }
    };

    public ItemHolderBlockEntity(BlockPos pos, BlockState state)
    {
        super(FlansMod.itemHolderBlockEntity.get(), pos, state);
        if (state.getBlock() instanceof ItemHolderBlock itemHolderBlock)
            type = itemHolderBlock.getConfigType();
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
    protected void saveAdditional(@NotNull CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put(NBT_ITEMS, items.serializeNBT());
        ItemHolderType holderType = getItemHolderType();
        if (holderType != null)
            tag.putString(NBT_TYPE, holderType.getShortName());
    }

    @Override
    public void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        items.deserializeNBT(tag.getCompound(NBT_ITEMS));
        if (tag.contains(NBT_TYPE))
            type = ItemHolderType.getItemHolder(tag.getString(NBT_TYPE));
    }

    @Nullable
    public ItemHolderType getItemHolderType()
    {
        if (type == null && getBlockState().getBlock() instanceof ItemHolderBlock itemHolderBlock)
            type = itemHolderBlock.getConfigType();
        return type;
    }

    public ItemStack getStack()
    {
        return items.getStackInSlot(0);
    }

    public void setStack(ItemStack stack)
    {
        items.setStackInSlot(0, stack);
    }

    public void dropContents(Level level, BlockPos pos)
    {
        ItemStack stack = getStack();
        if (!stack.isEmpty())
        {
            Containers.dropContents(level, pos, new SimpleContainer(stack.copy()));
            setStack(ItemStack.EMPTY);
        }
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet)
    {
        CompoundTag tag = packet.getTag();
        if (tag != null)
            load(tag);
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}
