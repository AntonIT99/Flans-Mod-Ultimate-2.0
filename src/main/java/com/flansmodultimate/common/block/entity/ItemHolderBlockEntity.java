package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.ItemHolderBlock;
import com.flansmodultimate.common.types.ItemHolderType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemHolderBlockEntity extends BlockEntity
{
    public static final String NBT_ITEMS = "items";
    public static final String NBT_TYPE = "type";

    private ItemHolderType type;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents)
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
        ItemHolderType holderType = getItemHolderType();
        if (holderType != null)
            output.putString(NBT_TYPE, holderType.getShortName());
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        items.deserialize(input.childOrEmpty(NBT_ITEMS));
        type = ItemHolderType.getItemHolder(input.getStringOr(NBT_TYPE, ""));
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
        return ItemUtil.getStack(items, 0);
    }

    public void setStack(ItemStack stack)
    {
        items.set(0, ItemResource.of(stack), stack.getCount());
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        if (level != null)
            dropContents(level, pos);
        super.preRemoveSideEffects(pos, state);
    }
}
