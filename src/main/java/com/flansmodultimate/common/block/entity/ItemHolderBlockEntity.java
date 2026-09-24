package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.ItemHolderBlock;
import com.flansmodultimate.common.types.ItemHolderType;
import com.flansmodultimate.platform.block.FlanBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemHolderBlockEntity extends FlanBlockEntity
{
    public static final String NBT_ITEMS = "items";
    public static final String NBT_TYPE = "type";

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
    @NotNull
    public IItemHandler getItemHandler()
    {
        return items;
    }

    @Override
    protected void saveData(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.put(NBT_ITEMS, serializeItems(items, registries));
        ItemHolderType holderType = getItemHolderType();
        if (holderType != null)
            tag.putString(NBT_TYPE, holderType.getShortName());
    }

    @Override
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries)
    {
        deserializeItems(items, registries, tag.getCompound(NBT_ITEMS));
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
    protected CompoundTag createUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        writeFullData(tag, registries);
        return tag;
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        readFullData(tag, registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void readDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries)
    {
        CompoundTag tag = packet.getTag();
        if (tag != null)
            readFullData(tag, registries);
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}
