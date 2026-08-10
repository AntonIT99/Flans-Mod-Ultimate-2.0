package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import com.flansmodultimate.platform.item.ItemStackData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamSpawnerBlockEntity extends BlockEntity implements ITeamObject
{
    private static final String NBT_SPAWNER = "teams_spawner";
    private static final String NBT_OBJECT_ID = "object_id";
    private static final String NBT_BASE_ID = "base_id";
    private static final String NBT_MODE = "mode";
    private static final String NBT_SPAWN_DELAY = "spawn_delay";
    private static final String NBT_CURRENT_DELAY = "current_delay";
    private static final String NBT_ITEMS = "items";

    public enum Mode
    {
        PLAYER,
        ITEM,
        VEHICLE
    }

    private UUID objectId = UUID.randomUUID();
    @Nullable
    private UUID baseId;
    @Getter
    private Mode mode;
    private final List<ItemStack> templates = new ArrayList<>();
    @Getter
    private int spawnDelayTicks = 20 * 20;
    private int currentDelay = 10;

    public TeamSpawnerBlockEntity(BlockPos pos, BlockState state)
    {
        this(pos, state, Mode.PLAYER);
    }

    public TeamSpawnerBlockEntity(BlockPos pos, BlockState state, Mode mode)
    {
        super(FlansMod.teamSpawnerBlockEntity.get(), pos, state);
        this.mode = mode;
    }

    public void cycleSpawnDelay()
    {
        spawnDelayTicks += 10 * 20;
        if (spawnDelayTicks > 5 * 60 * 20)
            spawnDelayTicks = 10 * 20;
        setChangedAndSync();
    }

    public void addTemplate(ItemStack stack)
    {
        templates.add(stack.copy());
        currentDelay = 10;
        setChangedAndSync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeamSpawnerBlockEntity spawner)
    {
        if (spawner.mode == Mode.PLAYER || spawner.templates.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return;
        if (spawner.currentDelay-- > 0)
            return;
        spawner.currentDelay = spawner.spawnDelayTicks;

        AABB nearby = new AABB(pos).inflate(spawner.mode == Mode.VEHICLE ? 8D : 1.5D);
        if (!serverLevel.getEntities((Entity) null, nearby, entity -> entity.getPersistentData().hasUUID(NBT_SPAWNER)
            && spawner.objectId.equals(entity.getPersistentData().getUUID(NBT_SPAWNER))).isEmpty())
            return;

        for (ItemStack template : spawner.templates)
        {
            if (spawner.mode == Mode.VEHICLE)
            {
                Entity spawned = null;
                if (template.getItem() instanceof DriveableItem<?, ?> driveableItem)
                {
                    Driveable driveable = driveableItem.spawnDriveable(level, pos.getX() + 0.5D,
                        pos.getY() + 1D + driveableItem.getConfigType().getYOffset(), pos.getZ() + 0.5D,
                        0F, null, template.copyWithCount(1));
                    spawned = driveable;
                }
                else if (template.getItem() instanceof AAGunItem aaGunItem)
                    spawned = aaGunItem.spawnAAGun(level, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D, null);
                if (spawned != null)
                    spawned.getPersistentData().putUUID(NBT_SPAWNER, spawner.objectId);
                continue;
            }

            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, template.copy());
            item.setDefaultPickUpDelay();
            item.getPersistentData().putUUID(NBT_SPAWNER, spawner.objectId);
            level.addFreshEntity(item);
        }
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
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putUUID(NBT_OBJECT_ID, objectId);
        if (baseId != null)
            tag.putUUID(NBT_BASE_ID, baseId);
        tag.putString(NBT_MODE, mode.name()); tag.putInt(NBT_SPAWN_DELAY, spawnDelayTicks); tag.putInt(NBT_CURRENT_DELAY, currentDelay);
        ListTag items = new ListTag();
        templates.forEach(stack -> items.add(ItemStackData.save(stack, registries)));
        tag.put(NBT_ITEMS, items);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
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

        spawnDelayTicks = Math.max(20, tag.getInt(NBT_SPAWN_DELAY)); currentDelay = Math.max(0, tag.getInt(NBT_CURRENT_DELAY));
        templates.clear();

        for (Tag item : tag.getList(NBT_ITEMS, Tag.TAG_COMPOUND))
            templates.add(ItemStackData.parse(registries, (CompoundTag) item));
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
        this.baseId = baseId; setChangedAndSync();
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
