package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansModBlocks;
import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.common.entity.TeamItemEntity;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.block.FlanBlockEntity;
import com.flansmodultimate.platform.item.ItemStackData;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class TeamSpawnerBlockEntity extends FlanBlockEntity implements ITeamObject
{
    public static final int MAX_SPAWN_DELAY_SECONDS = Integer.MAX_VALUE / 20;
    private static final String NBT_WAIT_FOR_VEHICLE = "wait_for_vehicle";
    private static final String NBT_SPAWNER = "teams_spawner";
    private static final String NBT_OBJECT_ID = "object_id";
    private static final String NBT_BASE_ID = "base_id";
    private static final String NBT_MODE = "mode";
    private static final String NBT_SPAWN_DELAY = "spawn_delay";
    private static final String NBT_CURRENT_DELAY = "current_delay";
    private static final String NBT_ITEMS = "items";
    private static final String NBT_TEAM_COLOUR = "team_colour";
    /** How often the owning team is re-checked, in ticks. */
    private static final int TEAM_COLOUR_REFRESH_TICKS = 20;
    /** Legacy light grey used by spawners that belong to no team. */
    public static final int UNOWNED_COLOUR = 0x808080;

    public enum Mode
    {
        PLAYER, ITEM, VEHICLE
    }

    private UUID objectId = UUID.randomUUID();
    @Nullable
    private UUID baseId;
    @Getter
    private Mode mode;
    private final List<ItemStack> templates = new ArrayList<>();
    @Getter
    private int spawnDelayTicks = 20 * 20;
    /** Colour of the owning team, synced to clients for the block colour handler */
    @Getter
    private int teamColour = UNOWNED_COLOUR;
    private int currentDelay = 10;
    @Getter
    private boolean waitForVehicle = true;
    @Getter
    private SpawnerNpcPolicy npcPolicy = SpawnerNpcPolicy.ALL;

    public void setVehicleSettings(boolean waitForVehicle, SpawnerNpcPolicy npcPolicy)
    {
        if (mode != Mode.VEHICLE)
            return;
        this.waitForVehicle = waitForVehicle;
        this.npcPolicy = npcPolicy;
        setChangedAndSync();
    }

    public TeamSpawnerBlockEntity(BlockPos pos, BlockState state)
    {
        this(pos, state, Mode.PLAYER);
    }

    public TeamSpawnerBlockEntity(BlockPos pos, BlockState state, Mode mode)
    {
        super(FlansModBlocks.teamSpawnerBlockEntity.get(), pos, state);
        this.mode = mode;
    }

    public void setSpawnDelaySeconds(int seconds)
    {
        if (mode == Mode.PLAYER || seconds < 1 || seconds > MAX_SPAWN_DELAY_SECONDS)
            return;
        spawnDelayTicks = seconds * 20;
        setChangedAndSync();
    }

    public void addTemplate(ItemStack stack)
    {
        templates.add(stack.copy());
        currentDelay = 10;
        setChangedAndSync();
    }

    // Signature of BlockEntityTicker, which the method is registered as.
    @SuppressWarnings("java:S1172")
    public static void serverTick(Level level, BlockPos pos, BlockState state, TeamSpawnerBlockEntity spawner)
    {
        if (level.getGameTime() % TEAM_COLOUR_REFRESH_TICKS == 0)
            spawner.refreshTeamColour();
        if (spawner.mode == Mode.PLAYER || spawner.templates.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return;
        if (spawner.currentDelay-- > 0)
            return;
        spawner.currentDelay = spawner.spawnDelayTicks;

        if (spawner.hasExistingSpawn(serverLevel, pos))
            return;

        for (int index = 0; index < spawner.templates.size(); index++)
        {
            ItemStack template = spawner.templates.get(index);
            if (spawner.mode == Mode.VEHICLE)
            {
                Entity spawned = spawner.spawnVehicle(serverLevel, pos, template);
                if (spawned != null)
                {
                    spawned.getPersistentData().putUUID(NBT_SPAWNER, spawner.objectId);
                    SpawnerVehicles.get(serverLevel).observe(spawned);
                }
                continue;
            }

            // Spawned items circle the spawner, evenly spaced, until a player of the owning team takes them
            TeamItemEntity item = new TeamItemEntity(level, pos, spawner.objectId, template.copy(), index * Math.PI * 2D / spawner.templates.size());
            item.getPersistentData().putUUID(NBT_SPAWNER, spawner.objectId);
            level.addFreshEntity(item);
        }
    }

    @Nullable
    private Entity spawnVehicle(ServerLevel level, BlockPos pos, ItemStack template)
    {
        if (template.getItem() instanceof DriveableItem<?, ?> driveableItem)
        {
            Vec3 spawnPosition = Vec3.atBottomCenterOf(pos.above());
            if (driveableItem.getConfigType().isShip())
            {
                spawnPosition = TeamsShipSpawns.find(level, spawnPosition).orElse(null);
                if (spawnPosition == null || !waitForVehicle
                    && !level.getEntities((Entity) null, new AABB(BlockPos.containing(spawnPosition)).inflate(8D), entity -> (mode != Mode.VEHICLE || SpawnerVehicles.operational(entity))
                        && entity.getPersistentData().hasUUID(NBT_SPAWNER) && objectId.equals(entity.getPersistentData().getUUID(NBT_SPAWNER))).isEmpty())
                    return null;
            }
            return driveableItem.spawnDriveable(level, spawnPosition.x, spawnPosition.y + driveableItem.getConfigType().getYOffset(), spawnPosition.z, 0F, null, template.copyWithCount(1),
                getOwningTeam());
        }
        if (template.getItem() instanceof AAGunItem aaGunItem)
            return aaGunItem.spawnAAGun(level, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D, null, getOwningTeam());
        return null;
    }

    private boolean hasExistingSpawn(ServerLevel level, BlockPos pos)
    {
        if (mode == Mode.VEHICLE && waitForVehicle)
        {
            TeamsManager manager = TeamsManager.getInstance();
            ITeamBase base = baseId == null ? null : manager.getBase(baseId).orElse(null);
            TeamsMap map = base == null ? null : manager.getMap(base.getMapId()).orElse(null);
            return SpawnerVehicles.get(level).exists(level, objectId, map);
        }
        AABB nearby = new AABB(pos).inflate(mode == Mode.VEHICLE ? 8D : 1.5D);
        return !level.getEntities((Entity) null, nearby, entity -> (mode != Mode.VEHICLE || SpawnerVehicles.operational(entity)) && entity.getPersistentData().hasUUID(NBT_SPAWNER)
            && objectId.equals(entity.getPersistentData().getUUID(NBT_SPAWNER))).isEmpty();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (level instanceof ServerLevel)
            TeamsManager.getInstance().registerObject(this);
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
        tag.putString(NBT_MODE, mode.name());
        tag.putInt(NBT_SPAWN_DELAY, spawnDelayTicks);
        tag.putInt(NBT_CURRENT_DELAY, currentDelay);
        tag.putBoolean(NBT_WAIT_FOR_VEHICLE, waitForVehicle);
        tag.put("npc_policy", npcPolicy.save());
        tag.putInt(NBT_TEAM_COLOUR, teamColour);
        ListTag items = new ListTag();
        templates.forEach(stack -> items.add(ItemStackData.save(stack, registries)));
        tag.put(NBT_ITEMS, items);
    }

    @Override
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries)
    {
        objectId = tag.hasUUID(NBT_OBJECT_ID) ? tag.getUUID(NBT_OBJECT_ID) : UUID.randomUUID();
        baseId = tag.hasUUID(NBT_BASE_ID) ? tag.getUUID(NBT_BASE_ID) : null;

        try
        {
            mode = Mode.valueOf(tag.getString(NBT_MODE).toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored)
        {
            mode = Mode.PLAYER;
        }

        spawnDelayTicks = Math.max(20, tag.getInt(NBT_SPAWN_DELAY));
        currentDelay = Math.max(0, tag.getInt(NBT_CURRENT_DELAY));
        waitForVehicle = !tag.contains(NBT_WAIT_FOR_VEHICLE) || tag.getBoolean(NBT_WAIT_FOR_VEHICLE);
        npcPolicy = SpawnerNpcPolicy.load(tag.getCompound("npc_policy"));
        teamColour = tag.contains(NBT_TEAM_COLOUR) ? tag.getInt(NBT_TEAM_COLOUR) : UNOWNED_COLOUR;
        templates.clear();

        for (Tag item : tag.getList(NBT_ITEMS, Tag.TAG_COMPOUND))
            templates.add(ItemStackData.parse(registries, (CompoundTag) item));
    }

    /**
     * The spawner plate carries a tinted overlay in its owning team's colour, the way the
     * legacy coloured render pass did. The colour is kept on the block entity so the client
     * block colour handler can read it without knowing anything about the Teams runtime.
     */
    private void refreshTeamColour()
    {
        if (level == null || level.isClientSide)
            return;
        Team team = getOwningTeam();
        int colour = team == null ? UNOWNED_COLOUR : team.getTeamColour();
        if (colour == teamColour)
            return;
        teamColour = colour;
        setChangedAndSync();
    }

    /** The team owning the base this spawner is connected to in the current round, or null when nobody owns it. */
    @Nullable
    public Team getOwningTeam()
    {
        if (baseId == null)
            return null;
        ITeamBase base = TeamsManager.getInstance().getBase(baseId).orElse(null);
        return base == null ? null : TeamsManager.getInstance().getTeamForBase(base);
    }

    @Override
    @NotNull
    protected CompoundTag createUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = defaultUpdateTag(registries);
        writeFullData(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public UUID getObjectId()
    {
        return objectId;
    }

    @Override
    public ResourceKey<Level> getDimension()
    {
        return level == null ? Level.OVERWORLD : level.dimension();
    }

    @Override
    public Vec3 getTeamObjectPosition()
    {
        return Vec3.atBottomCenterOf(worldPosition);
    }

    @Override
    public @Nullable UUID getBaseId()
    {
        return baseId;
    }

    @Override
    public void setBaseId(@Nullable UUID baseId)
    {
        this.baseId = baseId;
        setChangedAndSync();
    }

    @Override
    public boolean isSpawnPoint()
    {
        return mode == Mode.PLAYER;
    }

    @Override
    public void destroyTeamObject()
    {
        if (level != null)
            level.destroyBlock(worldPosition, false);
    }
}
