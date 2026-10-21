package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansModBlocks;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.block.FlanBlockEntity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A persistent base link with a server-derived colour, synced without exposing Teams runtime state to the renderer. */
public final class TeamBannerBlockEntity extends FlanBlockEntity implements ITeamObject
{
    public static final int UNOWNED_COLOUR = 0x808080;
    private static final int COLOUR_REFRESH_TICKS = 20;
    private static final String NBT_OBJECT_ID = "object_id";
    private static final String NBT_BASE_ID = "base_id";
    private static final String NBT_TEAM_COLOUR = "team_colour";

    @Getter
    private UUID objectId = UUID.randomUUID();
    @Getter @Nullable
    private UUID baseId;
    @Getter
    private int teamColour = UNOWNED_COLOUR;

    public TeamBannerBlockEntity(BlockPos pos, BlockState state)
    {
        this(FlansModBlocks.teamBannerBlockEntity.get(), pos, state);
    }

    TeamBannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    // Signature of BlockEntityTicker, which the method is registered as.
    @SuppressWarnings({"java:S1172", "unused"})
    public static void serverTick(Level level, BlockPos pos, BlockState state, TeamBannerBlockEntity banner)
    {
        if (!level.isClientSide && level.getGameTime() % COLOUR_REFRESH_TICKS == 0)
            banner.refreshTeamColour();
    }

    private void refreshTeamColour()
    {
        if (!(level instanceof ServerLevel))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = baseId == null ? null : manager.getBase(baseId).orElse(null);
        Team team = base == null ? null : manager.getTeamForBase(base);
        int colour = team == null ? UNOWNED_COLOUR : team.getTeamColour();
        if (teamColour != colour)
        {
            teamColour = colour;
            setChangedAndSync();
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (level instanceof ServerLevel)
        {
            TeamsManager.getInstance().registerObject(this);
            refreshTeamColour();
        }
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel)
            TeamsManager.getInstance().unregisterObject(objectId);
        super.setRemoved();
    }

    @Override
    protected void saveData(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putUUID(NBT_OBJECT_ID, objectId);
        if (baseId != null)
            tag.putUUID(NBT_BASE_ID, baseId);
        tag.putInt(NBT_TEAM_COLOUR, teamColour);
    }

    @Override
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries)
    {
        objectId = tag.hasUUID(NBT_OBJECT_ID) ? tag.getUUID(NBT_OBJECT_ID) : UUID.randomUUID();
        baseId = tag.hasUUID(NBT_BASE_ID) ? tag.getUUID(NBT_BASE_ID) : null;
        teamColour = tag.contains(NBT_TEAM_COLOUR) ? tag.getInt(NBT_TEAM_COLOUR) & 0xFFFFFF : UNOWNED_COLOUR;
    }

    @Override
    @NotNull
    protected CompoundTag createUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = defaultUpdateTag(registries);
        writeFullData(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level instanceof ServerLevel)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void setBaseId(@Nullable UUID id)
    {
        baseId = id;
        refreshTeamColour();
        setChangedAndSync();
    }

    @Override
    public ResourceKey<Level> getDimension()
    {
        return level == null ? Level.OVERWORLD : level.dimension();
    }

    @Override
    public Vec3 getTeamObjectPosition()
    {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public boolean isSpawnPoint()
    {
        return false;
    }

    @Override
    public void destroyTeamObject()
    {
        if (level instanceof ServerLevel && level.getBlockEntity(worldPosition) == this)
            level.destroyBlock(worldPosition, false);
    }
}
