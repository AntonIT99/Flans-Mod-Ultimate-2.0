package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.ShotCooldown;
import com.flansmodultimate.common.guns.handler.DeployableGunShootingHandler;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketBuffer;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.platform.entity.SpawnDataEntity;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DeployedGun extends Entity implements SpawnDataEntity, IFlanEntity<GunType>
{
    private boolean suppressRemovalDrops;
    public static final int RENDER_DISTANCE = 64;
    public static final float DEFAULT_HITBOX_SIZE = 1F;

    public static final String NBT_TYPE_NAME = "type";
    public static final String NBT_AMMO = "ammo";
    public static final String NBT_BLOCK_X = "block_x";
    public static final String NBT_BLOCK_Y = "block_y";
    public static final String NBT_BLOCK_Z = "block_z";
    public static final String NBT_DIRECTION = "direction";

    protected static final EntityDataAccessor<String> DATA_GUN_TYPE = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Boolean> DATA_HAS_AMMO = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> DATA_RELOAD_TIMER = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_GUN_DIRECTION = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);
    /** Remaining ticks for the client-side flash drawn on the deployed model. */
    protected static final EntityDataAccessor<Integer> DATA_MUZZLE_FLASH_TICKS = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);
    /** Stable random frame of a three-frame flash for the current shot. */
    protected static final EntityDataAccessor<Integer> DATA_MUZZLE_FLASH_FRAME = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);
    /** Rounds the loaded ammunition still has to fire, for the gunner's HUD. */
    protected static final EntityDataAccessor<Integer> DATA_ROUNDS_LEFT = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);
    /** What a full magazine of the loaded ammunition holds. */
    protected static final EntityDataAccessor<Integer> DATA_MAGAZINE_SIZE = SynchedEntityData.defineId(DeployedGun.class, EntityDataSerializers.INT);

    protected GunType configType;
    protected String shortname = StringUtils.EMPTY;
    protected BlockPos blockPos;
    /** Local yaw used for the operator's position around the fixed emplacement. */
    protected float riderYawOffset;
    @Getter
    protected ItemStack ammo = ItemStack.EMPTY;
    protected int reloadTimer;
    protected int soundTimer;
    protected float shootTimer;
    protected int ticksSinceUsed;
    @Getter @Setter
    protected boolean shootKeyPressed;
    @Getter @Setter
    protected boolean prevShootKeyPressed;

    public DeployedGun(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public DeployedGun(Level level, BlockPos pos, Direction direction, GunType gunType)
    {
        super(FlansMod.deployedGunEntity.get(), level);
        setShortName(gunType.getShortName());
        blockPos = pos;
        setGunDirection(direction.get2DDataValue());
        configType = gunType;
        setPos(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
        resetToPlacementFacing();
        setXRot(-60F);
    }

    @Override
    public GunType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof GunType gType)
        {
            configType = gType;
        }
        return configType;
    }

    public String getShortName()
    {
        return entityData.get(DATA_GUN_TYPE);
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(DATA_GUN_TYPE, shortname);
    }

    public int getReloadTimer()
    {
        return entityData.get(DATA_RELOAD_TIMER);
    }

    public void setReloadTimer(int v)
    {
        reloadTimer = v;
        entityData.set(DATA_RELOAD_TIMER, v);
    }

    public boolean hasAmmo()
    {
        return entityData.get(DATA_HAS_AMMO);
    }

    public void setHasAmmo(boolean v)
    {
        entityData.set(DATA_HAS_AMMO, v);
    }

    public int getGunDirection()
    {
        return entityData.get(DATA_GUN_DIRECTION);
    }

    private void setGunDirection(int d)
    {
        entityData.set(DATA_GUN_DIRECTION, Direction.from2DDataValue(d).get2DDataValue());
    }

    private void resetToPlacementFacing()
    {
        float baseYaw = Direction.from2DDataValue(getGunDirection()).toYRot();
        setYRot(baseYaw);
        yRotO = baseYaw;
        riderYawOffset = 0F;
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public boolean shouldRiderSit()
    {
        return false;
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        return ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq)
    {
        double r = getRenderDistance();
        return distSq < r * r;
    }

    private static int getRenderDistance()
    {
        ModClientConfig config = ModClientConfig.get();
        return config == null ? RENDER_DISTANCE : config.deployedGunRenderDistance;
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling()
    {
        // No frustum-culling within the render distance
        double r = getRenderDistance();
        return new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r);
    }

    @Override
    protected void defineSynchedData()
    {
        defineEntityData(new SynchedDataDefinition(entityData));
    }

    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_GUN_TYPE, StringUtils.EMPTY);
        data.define(DATA_HAS_AMMO, false);
        data.define(DATA_RELOAD_TIMER, 0);
        data.define(DATA_GUN_DIRECTION, 0);
        data.define(DATA_MUZZLE_FLASH_TICKS, 0);
        data.define(DATA_MUZZLE_FLASH_FRAME, 0);
        data.define(DATA_ROUNDS_LEFT, 0);
        data.define(DATA_MAGAZINE_SIZE, 0);
    }

    public int getMuzzleFlashTicks()
    {
        return entityData.get(DATA_MUZZLE_FLASH_TICKS);
    }

    public int getMuzzleFlashFrame()
    {
        return entityData.get(DATA_MUZZLE_FLASH_FRAME);
    }

    public int getRoundsLeft()
    {
        return entityData.get(DATA_ROUNDS_LEFT);
    }

    public void setAmmo(ItemStack stack)
    {
        ammo = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
        updateAmmoState();
    }

    /** Keeps the synced ammunition state in step with the stack the gun is firing. */
    protected void updateAmmoState()
    {
        int rounds = ShootableItem.getTotalRounds(ammo);
        // The capacity to count down from is what went in at the last reload, so a
        // belt reads 247/300 rather than against one item's worth.
        if (rounds > entityData.get(DATA_ROUNDS_LEFT))
            entityData.set(DATA_MAGAZINE_SIZE, rounds);
        if (entityData.get(DATA_ROUNDS_LEFT) != rounds)
            entityData.set(DATA_ROUNDS_LEFT, rounds);
        setHasAmmo(rounds > 0);
    }

    public int getMagazineSize()
    {
        return entityData.get(DATA_MAGAZINE_SIZE);
    }

    @Override
    public void writeSpawnData(PacketBuffer buf)
    {
        buf.writeUtf(shortname);
        buf.writeInt(getGunDirection());
        buf.writeInt(blockPos.getX());
        buf.writeInt(blockPos.getY());
        buf.writeInt(blockPos.getZ());
        buf.writeBoolean(!ammo.isEmpty());
    }

    @Override
    public void readSpawnData(PacketBuffer buf)
    {
        try
        {
            setShortName(buf.readUtf());
            if (InfoType.getInfoType(shortname) instanceof GunType gType)
                configType = gType;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown gun type {}, discarding.", shortname);
                discard();
            }
            setGunDirection(buf.readInt());
            blockPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            setHasAmmo(buf.readBoolean());
            resetToPlacementFacing();
        }
        catch (Exception e)
        {
            discard();
            FlansMod.log.warn("Failed to read deployable gun spawn data", e);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        setShortName(tag.getString(NBT_TYPE_NAME));

        if (InfoType.getInfoType(shortname) instanceof GunType gType)
            configType = gType;
        else
            discard();

        setGunDirection(tag.getInt(NBT_DIRECTION));
        blockPos = new BlockPos(tag.getInt(NBT_BLOCK_X), tag.getInt(NBT_BLOCK_Y), tag.getInt(NBT_BLOCK_Z));
        resetToPlacementFacing();

        if (tag.contains(NBT_AMMO, Tag.TAG_COMPOUND))
            ammo = ItemStackData.parse(level().registryAccess(), tag.getCompound(NBT_AMMO));
        else
            ammo = ItemStack.EMPTY;
        setHasAmmo(!ammo.isEmpty());
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        if (configType == null)
        {
            discard();
            return;
        }

        tag.putString(NBT_TYPE_NAME, shortname);
        tag.putInt(NBT_DIRECTION, getGunDirection());
        tag.putInt(NBT_BLOCK_X, blockPos.getX());
        tag.putInt(NBT_BLOCK_Y, blockPos.getY());
        tag.putInt(NBT_BLOCK_Z, blockPos.getZ());

        if (!ammo.isEmpty())
        {
            CompoundTag ammoTag = new CompoundTag();
            ItemStackData.save(ammo, level().registryAccess(), ammoTag);
            tag.put(NBT_AMMO, ammoTag);
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason)
    {
        try
        {
            Level level = level();

            // Only do "death drops" on the server, and not when the entity is merely being unloaded
            if (!suppressRemovalDrops && !level.isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK)
            {
                if (FlansMod.teamsManager.getWeaponDrops() == TeamsManager.EnumWeaponDrop.SMART_DROPS)
                {
                    level.addFreshEntity(new GunItemEntity(level, getX(), getY(), getZ(), ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY), Collections.singletonList(ammo)));
                }
                else if (FlansMod.teamsManager.getWeaponDrops() == TeamsManager.EnumWeaponDrop.DROPS)
                {
                    spawnAtLocation(ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY), 0F);
                    if (!ammo.isEmpty())
                        spawnAtLocation(ammo.copy(), 0.5F);
                }
            }
        }
        catch (Exception e)
        {
            FlansMod.log.error("Error removing deployable gun entity", e);
        }

        super.remove(reason);
    }

    /** Removes this gun for an administrative cleanup without creating item drops. */
    public void discardWithoutDrops()
    {
        suppressRemovalDrops = true;
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        Entity entity = source.getEntity();
        Entity gunner = getFirstPassenger();

        // If the gunner left-clicked the gun: ignore
        if (gunner == entity)
            return true;

        // If someone else hits the gun while someone is mounted: forward damage to gunner
        if (gunner != null)
            return gunner.hurt(source, amount);

        // If unmounted and allowed to break guns: remove it
        if (FlansMod.teamsManager.isCanBreakGuns())
            discard();

        return true;
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        Level level = level();
        Entity gunner = getFirstPassenger();

        // If someone else is mounted, ignore
        if (player != gunner && gunner != null)
            return InteractionResult.sidedSuccess(level.isClientSide);

        // Client: just return success so the hand animates; server does the logic
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        PlayerData data = PlayerData.getInstance(player);

        // If this is the player currently using this MG, dismount
        if (player == gunner)
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }

        // If this person is already mounting something else, dismount it first
        if (player.getVehicle() != null)
        {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }

        // Spectators can't mount guns
        if (FlansMod.teamsManager.getCurrentRound().isPresent() && Team.SPECTATORS.equals(data.getTeam()))
            return InteractionResult.CONSUME;

        if (configType == null || blockPos == null)
            return InteractionResult.CONSUME;

        float baseYaw = Direction.from2DDataValue(getGunDirection()).toYRot();
        Vec3 behind = getRiderPosition(player, baseYaw);
        if (behind == null)
            return InteractionResult.CONSUME;

        // A player operating the gun from its front must move clear of the
        // tripod before mounting, rather than being pulled through it later.
        double yawRad = baseYaw * Mth.DEG_TO_RAD;
        double rearDot = (player.getX() - getX()) * Math.sin(yawRad)
            - (player.getZ() - getZ()) * Math.cos(yawRad);
        if (rearDot < 0D)
        {
            if (player instanceof ServerPlayer serverPlayer)
                serverPlayer.teleportTo((ServerLevel) level, behind.x, behind.y, behind.z,
                    baseYaw, player.getXRot());
            else
            {
                player.setPos(behind);
                player.setYRot(baseYaw);
            }
        }

        // None of the above applied, so mount the gun
        player.startRiding(this, true);
        // Auto-reload if ammo empty
        reloadGun(level, player);

        return InteractionResult.CONSUME;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move)
    {
        if (!(passenger instanceof Player p) || configType == null || blockPos == null)
            return;

        float baseYaw = Direction.from2DDataValue(getGunDirection()).toYRot();
        float side = configType.getSideViewLimit();
        float requestedYaw = Mth.clamp(Mth.wrapDegrees(p.getYRot() - baseYaw), -side, side);
        riderYawOffset = limitRiderYaw(p, baseYaw, riderYawOffset, requestedYaw);

        // Clamp pitch
        float top = configType.getTopViewLimit();
        float bottom = configType.getBottomViewLimit();
        if (top > bottom)
        {
            float tmp = top;
            top = bottom; bottom = tmp;
        }
        float pitch = Mth.clamp(p.getXRot(), top, bottom);

        float gunYaw = baseYaw + requestedYaw;
        setYRot(gunYaw);
        setXRot(pitch);
        // The weapon keeps traversing within its own limits even when an
        // obstruction prevents the operator from following it around.
        if (Math.abs(Mth.wrapDegrees(p.getYRot() - gunYaw)) > 0.01F)
            p.setYRot(gunYaw);

        float riderYaw = baseYaw + riderYawOffset;
        Vec3 standing = getRiderPosition(passenger, riderYaw);
        if (standing == null)
        {
            if (!level().isClientSide)
                p.stopRiding();
            return;
        }
        move.accept(passenger, standing.x, standing.y, standing.z);
        p.yBodyRot += Mth.wrapDegrees(riderYaw - p.yBodyRot);

        // Prevent sliding / falling weirdness while mounted
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.fallDistance = 0.0F;
    }

    /** Stop only the operator at the first yaw where their standing box hits an obstruction. */
    private float limitRiderYaw(Entity passenger, float baseYaw, float currentYaw, float requestedYaw)
    {
        float delta = requestedYaw - currentYaw;
        int steps = Math.max(1, Mth.ceil(Math.abs(delta) / 5F));
        float safeYaw = currentYaw;
        for (int step = 1; step <= steps; step++)
        {
            float candidate = currentYaw + delta * step / steps;
            if (getRiderPosition(passenger, baseYaw + candidate) == null)
            {
                float blockedYaw = candidate;
                for (int refine = 0; refine < 6; refine++)
                {
                    float middle = (safeYaw + blockedYaw) * 0.5F;
                    if (getRiderPosition(passenger, baseYaw + middle) == null)
                        blockedYaw = middle;
                    else
                        safeYaw = middle;
                }
                return safeYaw;
            }
            safeYaw = candidate;
        }
        return requestedYaw;
    }

    /** Position behind the gun if it has ground and room for the player. */
    private Vec3 getRiderPosition(Entity passenger, float yaw)
    {
        double yawRad = yaw * Mth.DEG_TO_RAD;
        double distance = configType.getStandBackDist();
        double x = getX() + distance * Math.sin(yawRad);
        double z = getZ() - distance * Math.cos(yawRad);
        double y = getStandingY(passenger, x, z);
        if (!Double.isFinite(y))
            return null;
        AABB targetBox = passenger.getBoundingBox().move(x - passenger.getX(), y - passenger.getY(), z - passenger.getZ());
        return level().noCollision(passenger, targetBox) ? new Vec3(x, y, z) : null;
    }

    /** Surface under the operator, or NaN if no support is within one block below the gun. */
    private double getStandingY(Entity passenger, double x, double z)
    {
        int gunY = blockPos.getY();
        double halfWidth = passenger.getBbWidth() * 0.5D;
        double surface = Double.NEGATIVE_INFINITY;
        for (int dy = -1; dy >= -2; dy--)
        {
            for (int blockX = Mth.floor(x - halfWidth); blockX <= Mth.floor(x + halfWidth); blockX++)
            {
                for (int blockZ = Mth.floor(z - halfWidth); blockZ <= Mth.floor(z + halfWidth); blockZ++)
                {
                    BlockPos support = new BlockPos(blockX, gunY + dy, blockZ);
                    VoxelShape shape = level().getBlockState(support).getCollisionShape(level(), support, CollisionContext.of(passenger));
                    double localX = x - blockX;
                    double localZ = z - blockZ;
                    for (AABB box : shape.toAabbs())
                    {
                        if (box.maxX <= localX - halfWidth || box.minX >= localX + halfWidth
                            || box.maxZ <= localZ - halfWidth || box.minZ >= localZ + halfWidth)
                            continue;
                        double top = support.getY() + box.maxY;
                        if (top >= gunY - 1D && top <= gunY)
                            surface = Math.max(surface, top);
                    }
                }
            }
        }
        return surface > Double.NEGATIVE_INFINITY ? surface : Double.NaN;
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger)
    {
        super.addPassenger(passenger);

        resetToPlacementFacing();

        shootKeyPressed = false;
        prevShootKeyPressed = false;
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger)
    {
        super.removePassenger(passenger);

        resetToPlacementFacing();

        shootKeyPressed = false;
        prevShootKeyPressed = false;
    }

    @Override
    @NotNull
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger)
    {
        Level level = level();

        if (blockPos != null)
        {
            // The barrel recenters when the operator leaves; keep the player
            // at their own last grounded position instead of following it.
            double x = passenger.getX();
            double z = passenger.getZ();
            double standingY = getStandingY(passenger, x, z);
            if (Double.isFinite(standingY))
            {
                Vec3 preferred = new Vec3(x, standingY, z);

                // Make it safe: must not collide at that position.
                if (isSafeDismount(level, passenger, preferred))
                    return preferred;
            }
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    private boolean isSafeDismount(Level level, LivingEntity passenger, Vec3 targetPos)
    {
        // Move passenger BB to targetPos
        Vec3 delta = targetPos.subtract(passenger.position());
        AABB movedBB = passenger.getBoundingBox().move(delta);

        // No collision with blocks/entities
        return level.noCollision(passenger, movedBB);
    }

    @Override
    public void tick()
    {
        super.tick();

        Level level = level();
        Entity gunner = getFirstPassenger();

        if (blockPos == null)
            blockPos = this.blockPosition();

        if (gunner == null || !gunner.isAlive())
        {
            shootKeyPressed = false;
            resetToPlacementFacing();
        }

        if (level.isClientSide)
            ClientHooks.GUN.tickDeployedGun(this);
        else
            serverTick(level);
    }

    protected void serverTick(Level level)
    {
        // Type must exist
        if (configType == null)
        {
            discard();
            return;
        }

        ticksSinceUsed++;

        // Lifetime expiry
        int mgLife = FlansMod.teamsManager.getMgLife();
        if (mgLife > 0 && ticksSinceUsed > mgLife * 20)
        {
            discard();
            return;
        }

        // Check supporting block
        BlockPos supportPos = blockPos.below();
        if (level.isEmptyBlock(supportPos))
            discard();

        // Timers
        shootTimer = ShotCooldown.tick(shootTimer);
        if (soundTimer > 0)
            soundTimer--;
        if (reloadTimer > 0)
            setReloadTimer(reloadTimer - 1);
        int muzzleFlashTicks = getMuzzleFlashTicks();
        if (muzzleFlashTicks > 0)
            entityData.set(DATA_MUZZLE_FLASH_TICKS, muzzleFlashTicks - 1);

        // Ammo broken/empty
        if (!ammo.isEmpty() && ammo.isDamageableItem() && ammo.getDamageValue() >= ammo.getMaxDamage())
            ammo = ItemStack.EMPTY;
        updateAmmoState();

        if (getFirstPassenger() instanceof LivingEntity living)
        {
            // Auto-reload if mounted by a player and ammo empty (takes ammo from inventory)
            if (living instanceof Player player)
                reloadGun(level, player);
            fireGun(level, living);
        }
    }

    protected int findAmmo(Player player)
    {
        List<ShootableType> allowed = configType.getAmmoTypes();
        Inventory inv = player.getInventory();

        int selected = inv.selected;
        ItemStack selectedStack = inv.getItem(selected);
        if (selectedStack.getItem() instanceof ShootableItem shootableItem && allowed.contains(shootableItem.getConfigType()))
            return selected;

        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof ShootableItem shootableItem) || !allowed.contains(shootableItem.getConfigType()))
                continue;

            int score = getPreferredAmmoScore(i, stack, selected);

            if (score > bestScore)
            {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    protected static int getPreferredAmmoScore(int i, ItemStack stack, int selected)
    {
        int score = 0;

        // Prefer hotbar strongly
        if (i < 9)
            score += 1_000_000;

        // Prefer "fuller" ammo:
        if (stack.isDamageableItem())
        {
            int remaining = stack.getMaxDamage() - stack.getDamageValue(); // higher = fuller
            score += remaining * 1000;
        }
        else
        {
            score += stack.getCount() * 1000;
        }

        // Tie-breaker: closer to selected slot
        if (i < 9)
            score -= Math.abs(i - selected);
        return score;
    }

    public void fireGun(Level level, LivingEntity gunner)
    {
        if (level.isClientSide || !gunner.isAlive() || !ShootableItem.hasRoundsLeft(ammo) || reloadTimer > 0 || !ShotCooldown.isReady(shootTimer) || !(ammo.getItem() instanceof ShootableItem shootableItem))
            return;

        boolean automaticFire = configType.getFireMode(null).isAutomaticFire();
        if ((automaticFire && shootKeyPressed) || (!automaticFire && shootKeyPressed && !prevShootKeyPressed))
        {
            float shootDelay = ShotCooldown.clampDelay(configType.getShootDelay(null));

            while (ShotCooldown.isReady(shootTimer))
            {
                ShootingHelper.fireGun(level, gunner, this, shootableItem.getConfigType(), ammo, new DeployableGunShootingHandler(ammo));
                entityData.set(DATA_MUZZLE_FLASH_FRAME, random.nextInt(3));
                entityData.set(DATA_MUZZLE_FLASH_TICKS, 2);

                if (soundTimer <= 0)
                {
                    if (StringUtils.isNotBlank(configType.getShootSound()))
                    {
                        PacketPlaySound.sendSoundPacket(this, configType.getGunSoundRange(), configType.getShootSound(), configType.isDistortSound(), configType.isSilencedSound(null));
                        soundTimer = configType.getShootSoundLength();
                    }

                    if (StringUtils.isNotBlank(configType.getDistantShootSound()))
                        PacketHandler.sendToDonut(level.dimension(), position(), configType.getGunSoundRange(), configType.getDistantSoundRange(), new PacketPlaySound(position(), configType.getDistantSoundRange(), configType.getDistantShootSound(), false, false, null));
                }

                shootTimer += shootDelay;

                if (!automaticFire)
                    break;
            }
        }
    }

    public void reloadGun(Level level, Player gunner)
    {
        // The gun reloads once the loaded item has no rounds left in it, which is
        // not the same as the slot being empty: a spent belt is still an item.
        if (level.isClientSide || !gunner.isAlive() || ShootableItem.hasRoundsLeft(ammo) || reloadTimer > 0)
            return;

        int slot = findAmmo(gunner); // you port this to modern inventory below
        if (slot >= 0)
        {
            // Take the stack from inventory
            ItemStack taken = gunner.getInventory().getItem(slot);
            if (!taken.isEmpty())
            {
                if (!gunner.getAbilities().instabuild)
                    gunner.getInventory().setItem(slot, ItemStack.EMPTY);

                reloadGun(level, gunner, taken);
            }
        }
    }

    public void reloadGun(Level level, LivingEntity gunner, ItemStack newAmmo)
    {
        if (level.isClientSide || !gunner.isAlive() || ShootableItem.hasRoundsLeft(ammo) || reloadTimer > 0)
            return;

        ammo = newAmmo.copy();
        setHasAmmo(true);
        float reloadFactor = ammo.getItem() instanceof ShootableItem shootableItem
            ? shootableItem.getConfigType().getReloadTimeMultiplier() : 1F;
        // Reloading never lets the gun outrun its own rate of fire, so the wait
        // after the round that emptied it is the longer of the two.
        setReloadTimer(Mth.ceil(Math.max(configType.getReloadTime() * reloadFactor, configType.getShootDelay(null))));
        String reloadSound = configType.getReloadSound(null);

        // Play reload sound
        if (StringUtils.isNotBlank(reloadSound))
            PacketPlaySound.sendSoundPacket(gunner, configType.getReloadSoundRange(), reloadSound, false, false);
    }

    public Vec3 getShootingOrigin()
    {
        if (configType.hasMeasuredDeployableMuzzle())
        {
            Vec3 pivot = configType.getMeasuredDeployableMuzzlePivot();
            Vec3 muzzle = configType.getMeasuredDeployableMuzzle();
            if (pivot != null && muzzle != null)
                return position().add(DeployedGunMuzzleGeometry.modelMuzzleOffset(pivot, muzzle,
                    configType.getModelScale(), getShootingYaw(), getShootingPitch()));
        }
        return new Vec3(blockPos.getX() + 0.5, blockPos.getY() + configType.getPivotHeight(), blockPos.getZ() + 0.5);
    }

    public Vec3 getShootingDirection()
    {
        return ModUtils.getDirectionFromPitchAndYaw(getShootingPitch(), getShootingYaw());
    }

    public float getShootingPitch()
    {
        return getXRot();
    }

    public float getShootingYaw()
    {
        return getYRot();
    }
}
