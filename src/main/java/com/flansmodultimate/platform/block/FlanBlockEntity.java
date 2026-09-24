package com.flansmodultimate.platform.block;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Version boundary for Flan block entities. Subclasses implement the registry-aware hooks below;
 * this class adapts them to the loader's persistence and client-sync callbacks. NeoForge exposes
 * {@link #getItemHandler()} through {@code RegisterCapabilitiesEvent}.
 */
public abstract class FlanBlockEntity extends BlockEntity
{
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
        super.handleUpdateTag(tag, registries);
    }

    /** Applies a block-entity data packet; defaults to the loader's handling. */
    protected void readDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries)
    {
        super.onDataPacket(connection, packet, registries);
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
        saveAdditional(tag, registries);
    }

    /** Reads the complete saved state, including loader data, as the load callback does. */
    protected final void readFullData(CompoundTag tag, HolderLookup.Provider registries)
    {
        loadAdditional(tag, registries);
    }

    protected final CompoundTag defaultUpdateTag(HolderLookup.Provider registries)
    {
        return super.getUpdateTag(registries);
    }

    protected static CompoundTag serializeItems(ItemStackHandler items, HolderLookup.Provider registries)
    {
        return items.serializeNBT(registries);
    }

    protected static void deserializeItems(ItemStackHandler items, HolderLookup.Provider registries, CompoundTag tag)
    {
        items.deserializeNBT(registries, tag);
    }

    @Override
    protected final void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        saveData(tag, registries);
    }

    @Override
    protected final void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        loadData(tag, registries);
    }

    @Override
    @NotNull
    public final CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries)
    {
        return createUpdateTag(registries);
    }

    @Override
    public final void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        readUpdateTag(tag, registries);
    }

    @Override
    public final void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket packet,
                                   @NotNull HolderLookup.Provider registries)
    {
        readDataPacket(connection, packet, registries);
    }
}
