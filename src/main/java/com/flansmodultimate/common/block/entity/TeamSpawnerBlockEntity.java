package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
    public enum Mode { PLAYER, ITEM, VEHICLE }

    private UUID objectId = UUID.randomUUID();
    @Nullable private UUID baseId;
    private Mode mode;
    private final List<ItemStack> templates = new ArrayList<>();
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

    public Mode getMode() { return mode; }
    public int getSpawnDelayTicks() { return spawnDelayTicks; }

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

        AABB nearby = new AABB(pos).inflate(1.5D);
        if (!serverLevel.getEntitiesOfClass(ItemEntity.class, nearby, entity -> entity.getPersistentData().hasUUID("TeamsSpawner")
            && spawner.objectId.equals(entity.getPersistentData().getUUID("TeamsSpawner"))).isEmpty())
            return;

        for (ItemStack template : spawner.templates)
        {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, template.copy());
            item.setDefaultPickUpDelay();
            item.getPersistentData().putUUID("TeamsSpawner", spawner.objectId);
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
    protected void saveAdditional(@NotNull CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putUUID("ObjectId", objectId);
        if (baseId != null)
            tag.putUUID("BaseId", baseId);
        tag.putString("Mode", mode.name());
        tag.putInt("SpawnDelay", spawnDelayTicks);
        tag.putInt("CurrentDelay", currentDelay);
        ListTag items = new ListTag();
        templates.forEach(stack -> items.add(stack.save(new CompoundTag())));
        tag.put("Items", items);
    }

    @Override
    public void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        objectId = tag.hasUUID("ObjectId") ? tag.getUUID("ObjectId") : UUID.randomUUID();
        baseId = tag.hasUUID("BaseId") ? tag.getUUID("BaseId") : null;
        try { mode = Mode.valueOf(tag.getString("Mode").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { mode = Mode.PLAYER; }
        spawnDelayTicks = Math.max(20, tag.getInt("SpawnDelay"));
        currentDelay = Math.max(0, tag.getInt("CurrentDelay"));
        templates.clear();
        for (Tag item : tag.getList("Items", Tag.TAG_COMPOUND))
            templates.add(ItemStack.of((CompoundTag) item));
    }

    private void setChangedAndSync()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public UUID getObjectId() { return objectId; }
    @Override public ResourceKey<Level> getDimension() { return level == null ? Level.OVERWORLD : level.dimension(); }
    @Override public Vec3 getTeamObjectPosition() { return Vec3.atBottomCenterOf(worldPosition); }
    @Override public @Nullable UUID getBaseId() { return baseId; }
    @Override public void setBaseId(@Nullable UUID baseId) { this.baseId = baseId; setChangedAndSync(); }
    @Override public boolean isSpawnPoint() { return mode == Mode.PLAYER; }
    @Override public void destroyTeamObject() { if (level != null) level.destroyBlock(worldPosition, false); }
}
