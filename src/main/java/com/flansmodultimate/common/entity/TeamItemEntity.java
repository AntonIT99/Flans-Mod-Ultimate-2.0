package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.PacketBuffer;
import com.flansmodultimate.platform.entity.SpawnDataEntity;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * An item offered by a Teams item spawner, as the 1.7.10 {@code EntityTeamItem}.
 *
 * <p>It hovers in a small circle around the spawner instead of falling, cannot be destroyed, never
 * despawns or merges, and only players of the team owning the spawner's base can take it. The circling
 * is purely visual: the server keeps the item at the centre, and each client moves it along the circle
 * from the centre and phase it received in the spawn packet.</p>
 */
public class TeamItemEntity extends ItemEntity implements SpawnDataEntity
{
    private static final String NBT_CENTER_X = "center_x";
    private static final String NBT_CENTER_Y = "center_y";
    private static final String NBT_CENTER_Z = "center_z";
    private static final String NBT_ANGLE = "angle";
    private static final String NBT_SPAWNER_ID = "spawner_id";
    /** Radius of the circle the items travel around the spawner, in blocks. */
    private static final double ORBIT_RADIUS = 0.3D;
    /** Angle travelled per tick, in radians. */
    private static final double ORBIT_SPEED = 0.05D;

    private Vec3 center = Vec3.ZERO;
    private double angle;
    @Nullable
    private UUID spawnerId;

    public TeamItemEntity(EntityType<? extends TeamItemEntity> type, Level level)
    {
        super(type, level);
        init();
    }

    public TeamItemEntity(Level level, BlockPos spawnerPos, UUID spawnerId, ItemStack stack, double angle)
    {
        this(FlansMod.teamItemEntity.get(), level);
        this.center = Vec3.atCenterOf(spawnerPos);
        this.angle = angle;
        this.spawnerId = spawnerId;
        setItem(stack);
        setPos(center);
    }

    private void init()
    {
        setNoGravity(true);
        setInvulnerable(true);
        setNoPickUpDelay();
        lifespan = Integer.MAX_VALUE;
    }

    @Override
    public void tick()
    {
        // No physics, merging or despawning: the server only keeps the item where the spawner put it
        if (!level().isClientSide)
        {
            setDeltaMovement(Vec3.ZERO);
            if (getItem().isEmpty())
                discard();
            return;
        }

        super.tick();
        angle += ORBIT_SPEED;
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x + Math.cos(angle) * ORBIT_RADIUS, center.y, center.z + Math.sin(angle) * ORBIT_RADIUS);
        clearFire();
    }

    @Override
    public void playerTouch(@NotNull Player player)
    {
        if (!level().isClientSide && !canBeTakenBy(player))
            return;
        super.playerTouch(player);
    }

    /** Only the team owning the spawner may take its items; anyone may while no team owns it. */
    private boolean canBeTakenBy(Player player)
    {
        if (spawnerId == null || !(TeamsManager.getInstance().getObject(spawnerId).orElse(null) instanceof TeamSpawnerBlockEntity spawner))
            return true;
        Team owner = spawner.getOwningTeam();
        return owner == null || owner.equals(PlayerData.getInstance(player).getTeam());
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        return false;
    }

    @Override
    public boolean isOnFire()
    {
        return false;
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer)
    {
        buffer.writeDouble(center.x);
        buffer.writeDouble(center.y);
        buffer.writeDouble(center.z);
        buffer.writeDouble(angle);
    }

    @Override
    public void readSpawnData(PacketBuffer buffer)
    {
        center = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        angle = buffer.readDouble();
    }

    @Override
    @NotNull
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putDouble(NBT_CENTER_X, center.x);
        tag.putDouble(NBT_CENTER_Y, center.y);
        tag.putDouble(NBT_CENTER_Z, center.z);
        tag.putDouble(NBT_ANGLE, angle);
        if (spawnerId != null)
            tag.putUUID(NBT_SPAWNER_ID, spawnerId);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        center = new Vec3(tag.getDouble(NBT_CENTER_X), tag.getDouble(NBT_CENTER_Y), tag.getDouble(NBT_CENTER_Z));
        angle = tag.getDouble(NBT_ANGLE);
        spawnerId = tag.hasUUID(NBT_SPAWNER_ID) ? tag.getUUID(NBT_SPAWNER_ID) : null;
        init();
    }
}
