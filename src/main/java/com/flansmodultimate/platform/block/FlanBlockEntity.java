package com.flansmodultimate.platform.block;

import com.flansmodultimate.platform.item.ItemStackData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Version boundary for Flan block entities. Subclasses implement the registry-aware hooks below;
 * this class adapts them to the loader's persistence, client-sync and item-capability callbacks.
 */
public abstract class FlanBlockEntity extends BlockEntity
{
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.empty();

    protected FlanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    /** Writes this block entity's own saved data. */
    protected void saveData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Reads this block entity's own saved data. */
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Tag sent to clients with the chunk; defaults to the loader's update tag. */
    protected CompoundTag createUpdateTag(HolderLookup.Provider registries)
    {
        return defaultUpdateTag(registries);
    }

    /** Applies a tag received with the chunk; defaults to the loader's handling. */
    protected void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.handleUpdateTag(tag);
    }

    /** Applies a block-entity data packet; defaults to the loader's handling. */
    protected void readDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries)
    {
        super.onDataPacket(connection, packet);
    }

    /** Item handler exposed as the item capability, or {@code null} for none. */
    @Nullable
    public IItemHandler getItemHandler()
    {
        return null;
    }

    /** Writes the complete saved state, including loader data, as the save callback does. */
    protected final void writeFullData(CompoundTag tag, HolderLookup.Provider registries)
    {
        saveAdditional(tag);
    }

    /** Reads the complete saved state, including loader data, as the load callback does. */
    protected final void readFullData(CompoundTag tag, HolderLookup.Provider registries)
    {
        load(tag);
    }

    protected final CompoundTag defaultUpdateTag(HolderLookup.Provider registries)
    {
        return super.getUpdateTag();
    }

    protected static CompoundTag serializeItems(ItemStackHandler items, HolderLookup.Provider registries)
    {
        return items.serializeNBT();
    }

    protected static void deserializeItems(ItemStackHandler items, HolderLookup.Provider registries, CompoundTag tag)
    {
        items.deserializeNBT(tag);
    }

    /** 1.20.1 callbacks carry no registry context; stack data does not need one on this version. */
    private HolderLookup.Provider registries()
    {
        return level != null ? level.registryAccess() : ItemStackData.builtInRegistries();
    }

    @Override
    protected final void saveAdditional(@NotNull CompoundTag tag)
    {
        super.saveAdditional(tag);
        saveData(tag, registries());
    }

    @Override
    public final void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        loadData(tag, registries());
    }

    @Override
    @NotNull
    public final CompoundTag getUpdateTag()
    {
        return createUpdateTag(registries());
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag)
    {
        readUpdateTag(tag, registries());
    }

    @Override
    public final void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet)
    {
        readDataPacket(connection, packet, registries());
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        IItemHandler handler = getItemHandler();
        if (handler != null)
            itemCapability = LazyOptional.of(() -> handler);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side)
    {
        if (capability == ForgeCapabilities.ITEM_HANDLER && getItemHandler() != null)
            return itemCapability.cast();
        return super.getCapability(capability, side);
    }
}
