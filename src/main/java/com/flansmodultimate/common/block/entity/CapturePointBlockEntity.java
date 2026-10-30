package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.FlansModBlocks;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.block.FlanBlockEntity;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The capture point of the Labjac fork: a flag on a pole that the team standing around it runs
 * up, and a Teams base whose connected spawners follow whoever holds it.
 */
public final class CapturePointBlockEntity extends FlanBlockEntity implements ITeamCapturePoint, ITeamFlagpole
{
    private static final String NBT_POLE_HEIGHT = "pole_height";
    public static final String DEFAULT_NAME = "Capture Point";
    public static final int DEFAULT_RADIUS = 16;
    public static final int MAX_RADIUS = 256;
    public static final int MAX_NAME_LENGTH = 60;
    public static final char FIRST_SYMBOL = 'A';
    public static final char LAST_SYMBOL = 'E';
    private static final String NBT_OBJECT_ID = "object_id";
    private static final String NBT_DEFAULT_OWNER = "default_owner";
    private static final String NBT_OWNER = "owner";
    private static final String NBT_CAPTURING = "capturing";
    private static final String NBT_PROGRESS = "progress";
    private static final String NBT_CONTESTED = "contested";
    private static final String NBT_NAME = "name";
    private static final String NBT_SYMBOL = "symbol";
    private static final String NBT_MAP = "map";
    private static final String NBT_YAW = "yaw";
    private static final String NBT_RADIUS = "radius";
    private static final String NBT_OBJECTS = "objects";
    private static final String NBT_ID = "id";
    private static final String NBT_ROUND = "round";
    private static final String NBT_DISPLAY_TEAM = "display_team";
    /** Capture progress has to move this far before the flag is redrawn on clients. */
    private static final float SYNC_PROGRESS_STEP = 0.02F;
    /** Milliseconds the flag takes to slide to a newly received height. */
    private static final float CLIENT_PROGRESS_GLIDE_MILLIS = 550F;

    private UUID objectId = UUID.randomUUID();
    @Getter
    private int poleHeight = ModCommonConfig.defaultFlagpoleHeight();
    private int defaultOwnerId;
    private CaptureState state = CaptureState.NEUTRAL;
    private String baseName = DEFAULT_NAME;
    private char symbol = FIRST_SYMBOL;
    private String mapId = StringUtils.EMPTY;
    /** Direction the placing player faced, which the flag flies towards. */
    @Getter
    private float yaw;
    private int captureRadius = DEFAULT_RADIUS;
    private final Set<UUID> objectIds = new LinkedHashSet<>();
    @Nullable
    private UUID roundId;
    /** Short name of the team whose flag flies here, sent to clients; blank while the point is neutral. */
    @Getter
    private String displayTeam = StringUtils.EMPTY;
    @Nullable
    private CaptureState lastSyncedState;
    private float clientProgressStart;
    private float clientProgressTarget;
    private long clientProgressStartMillis;
    private boolean clientProgressInitialised;

    public CapturePointBlockEntity(BlockPos pos, BlockState state)
    {
        this(FlansModBlocks.capturePointBlockEntity.get(), pos, state);
    }

    CapturePointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (level instanceof ServerLevel)
            TeamsManager.getInstance().registerBase(this);
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel)
            TeamsManager.getInstance().unregisterBase(objectId);
        super.setRemoved();
    }

    @Override
    protected void saveData(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putUUID(NBT_OBJECT_ID, objectId);
        tag.putInt(NBT_DEFAULT_OWNER, defaultOwnerId);
        tag.putInt(NBT_OWNER, state.ownerTeamId());
        tag.putInt(NBT_CAPTURING, state.capturingTeamId());
        tag.putFloat(NBT_PROGRESS, state.progress());
        tag.putBoolean(NBT_CONTESTED, state.contested());
        tag.putString(NBT_NAME, baseName);
        tag.putString(NBT_SYMBOL, String.valueOf(symbol));
        tag.putString(NBT_MAP, mapId);
        tag.putFloat(NBT_YAW, yaw);
        tag.putInt(NBT_RADIUS, captureRadius);
        tag.putInt(NBT_POLE_HEIGHT, poleHeight);
        if (roundId != null)
            tag.putUUID(NBT_ROUND, roundId);
        tag.putString(NBT_DISPLAY_TEAM, displayTeam);
        ListTag objects = new ListTag();
        for (UUID id : objectIds)
        {
            CompoundTag object = new CompoundTag();
            object.putUUID(NBT_ID, id);
            objects.add(object);
        }
        tag.put(NBT_OBJECTS, objects);
    }

    @Override
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries)
    {
        objectId = tag.hasUUID(NBT_OBJECT_ID) ? tag.getUUID(NBT_OBJECT_ID) : objectId;
        defaultOwnerId = Math.max(0, tag.getInt(NBT_DEFAULT_OWNER));
        state = tag.contains(NBT_PROGRESS)
            ? new CaptureState(tag.getInt(NBT_OWNER), tag.getInt(NBT_CAPTURING), tag.getFloat(NBT_PROGRESS), tag.getBoolean(NBT_CONTESTED))
            : CaptureState.ownedBy(defaultOwnerId);
        baseName = StringUtils.defaultIfBlank(tag.getString(NBT_NAME), DEFAULT_NAME);
        symbol = normaliseSymbol(tag.getString(NBT_SYMBOL));
        mapId = tag.getString(NBT_MAP);
        yaw = tag.getFloat(NBT_YAW);
        captureRadius = tag.contains(NBT_RADIUS) ? Mth.clamp(tag.getInt(NBT_RADIUS), 1, MAX_RADIUS) : DEFAULT_RADIUS;
        poleHeight = tag.contains(NBT_POLE_HEIGHT, Tag.TAG_ANY_NUMERIC)
            ? Mth.clamp(tag.getInt(NBT_POLE_HEIGHT), ModCommonConfig.MIN_FLAGPOLE_HEIGHT, ModCommonConfig.MAX_FLAGPOLE_HEIGHT)
            : ModCommonConfig.defaultFlagpoleHeight();
        roundId = tag.hasUUID(NBT_ROUND) ? tag.getUUID(NBT_ROUND) : null;
        displayTeam = tag.getString(NBT_DISPLAY_TEAM);
        objectIds.clear();
        for (Tag entry : tag.getList(NBT_OBJECTS, Tag.TAG_COMPOUND))
        {
            CompoundTag object = (CompoundTag) entry;
            if (object.hasUUID(NBT_ID))
                objectIds.add(object.getUUID(NBT_ID));
        }
        glideClientProgressTo(state.progress());
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
        if (level == null || level.isClientSide)
            return;
        Team team = TeamsManager.getInstance().getCurrentRound().map(round -> round.getTeam(state.displayTeamId() - 2)).orElse(null);
        displayTeam = team == null ? StringUtils.EMPTY : team.getShortName();
        lastSyncedState = state;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Applies what an operator entered in the capture point editor. */
    public void applySettings(String name, char newSymbol, int radius)
    {
        setBaseName(name);
        symbol = normaliseSymbol(String.valueOf(newSymbol));
        captureRadius = Mth.clamp(radius, 1, MAX_RADIUS);
        setChangedAndSync();
    }

    public void setYaw(float yaw)
    {
        this.yaw = Mth.wrapDegrees(yaw);
        setChangedAndSync();
    }

    @Override
    public void setPoleHeight(int height)
    {
        poleHeight = Mth.clamp(height, ModCommonConfig.MIN_FLAGPOLE_HEIGHT, ModCommonConfig.MAX_FLAGPOLE_HEIGHT);
        setChangedAndSync();
    }

    private static char normaliseSymbol(String value)
    {
        char upper = value.isEmpty() ? FIRST_SYMBOL : value.toUpperCase(Locale.ROOT).charAt(0);
        return upper < FIRST_SYMBOL || upper > LAST_SYMBOL ? FIRST_SYMBOL : upper;
    }

    /**
     * How far up the pole the flag is drawn, gliding towards the progress last received instead
     * of jumping, since the server only reports progress in steps.
     */
    public float getClientProgress()
    {
        if (!clientProgressInitialised)
            return state.progress();
        float elapsed = Mth.clamp((System.currentTimeMillis() - clientProgressStartMillis) / CLIENT_PROGRESS_GLIDE_MILLIS, 0F, 1F);
        return Mth.lerp(elapsed, clientProgressStart, clientProgressTarget);
    }

    private void glideClientProgressTo(float target)
    {
        clientProgressStart = clientProgressInitialised ? getClientProgress() : target;
        clientProgressTarget = target;
        clientProgressStartMillis = System.currentTimeMillis();
        clientProgressInitialised = true;
    }

    // --- ITeamCapturePoint ---

    @Override
    public char getSymbol()
    {
        return symbol;
    }

    @Override
    public int getCaptureRadius()
    {
        return captureRadius;
    }

    @Override
    public CaptureState getCaptureState()
    {
        return state;
    }

    @Override
    public void setCaptureState(CaptureState newState)
    {
        if (newState.equals(state))
            return;
        state = newState;
        if (needsSync())
            setChangedAndSync();
        else
            setChanged();
    }

    private boolean needsSync()
    {
        CaptureState synced = lastSyncedState;
        if (synced == null || synced.ownerTeamId() != state.ownerTeamId() || synced.capturingTeamId() != state.capturingTeamId() || synced.contested() != state.contested())
            return true;
        return Math.abs(synced.progress() - state.progress()) >= SYNC_PROGRESS_STEP || state.progress() <= 0F || state.progress() >= 1F;
    }

    @Nullable
    @Override
    public UUID getRoundId()
    {
        return roundId;
    }

    @Override
    public void setRoundId(@Nullable UUID roundId)
    {
        this.roundId = roundId;
        setChanged();
    }

    // --- ITeamBase ---

    @Override
    public String getBaseName()
    {
        return baseName;
    }

    @Override
    public void setBaseName(String name)
    {
        baseName = StringUtils.left(StringUtils.defaultIfBlank(name, DEFAULT_NAME).trim(), MAX_NAME_LENGTH);
        setChangedAndSync();
    }

    @Override
    public int getDefaultOwnerId()
    {
        return defaultOwnerId;
    }

    @Override
    public void setDefaultOwnerId(int id)
    {
        defaultOwnerId = Math.max(0, id);
        setOwnerId(defaultOwnerId);
    }

    @Override
    public int getOwnerId()
    {
        return state.ownerTeamId();
    }

    @Override
    public void setOwnerId(int id)
    {
        state = CaptureState.ownedBy(id);
        setChangedAndSync();
    }

    @Override
    public String getMapId()
    {
        return mapId;
    }

    @Override
    public void setMapId(String mapId)
    {
        this.mapId = StringUtils.defaultString(mapId).toLowerCase(Locale.ROOT);
        setChangedAndSync();
    }

    @Override
    public Collection<UUID> getObjectIds()
    {
        return Collections.unmodifiableSet(objectIds);
    }

    @Override
    public void addObject(UUID objectId)
    {
        if (objectIds.add(objectId))
            setChanged();
    }

    @Override
    public void removeObject(UUID objectId)
    {
        if (objectIds.remove(objectId))
            setChanged();
    }

    /**
     * Returns the point to its default owner, unless it already carries the state of the round
     * being played: points load with their chunks, which can happen in the middle of a round.
     */
    @Override
    public void startRound()
    {
        if (roundId != null && Objects.equals(roundId, TeamsManager.getInstance().getRoundInstanceId()))
            return;
        roundCleanup();
    }

    @Override
    public void roundCleanup()
    {
        roundId = null;
        state = CaptureState.ownedBy(defaultOwnerId);
        setChangedAndSync();
    }

    // --- ITeamObject ---

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
    public void destroyTeamObject()
    {
        if (level != null)
            level.destroyBlock(worldPosition, false);
    }
}
