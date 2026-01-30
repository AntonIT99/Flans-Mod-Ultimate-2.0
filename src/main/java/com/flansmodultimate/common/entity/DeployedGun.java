package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.handler.DeployableGunShootingHandler;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.server.PacketDeployedGunInput;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DeployedGun extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<GunType>
{
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

    protected GunType configType;
    protected String shortname = StringUtils.EMPTY;
    protected BlockPos blockPos;
    protected int gunDirection;
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
        setYRot(0F);
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

    public void setGunDirection(int d)
    {
        gunDirection = d;
        entityData.set(DATA_GUN_DIRECTION, d);
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        return ModUtils.getItemStack(configType).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq)
    {
        double r = RENDER_DISTANCE;
        return distSq < r * r;
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling()
    {
        // No frustum-culling within the render distance
        return new AABB(getX() - RENDER_DISTANCE, getY() - RENDER_DISTANCE, getZ() - RENDER_DISTANCE, getX() + RENDER_DISTANCE, getY() + RENDER_DISTANCE, getZ() + RENDER_DISTANCE);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DATA_GUN_TYPE, StringUtils.EMPTY);
        entityData.define(DATA_HAS_AMMO, false);
        entityData.define(DATA_RELOAD_TIMER, 0);
        entityData.define(DATA_GUN_DIRECTION, 0);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
        buf.writeInt(gunDirection);
        buf.writeInt(blockPos.getX());
        buf.writeInt(blockPos.getY());
        buf.writeInt(blockPos.getZ());
        buf.writeBoolean(!ammo.isEmpty());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
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
            gunDirection = buf.readInt();
            blockPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            setHasAmmo(buf.readBoolean());
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

        if (tag.contains(NBT_AMMO, Tag.TAG_COMPOUND))
            ammo = ItemStack.of(tag.getCompound(NBT_AMMO));
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
        tag.putInt(NBT_DIRECTION, gunDirection);
        tag.putInt(NBT_BLOCK_X, blockPos.getX());
        tag.putInt(NBT_BLOCK_Y, blockPos.getY());
        tag.putInt(NBT_BLOCK_Z, blockPos.getZ());

        if (!ammo.isEmpty())
        {
            CompoundTag ammoTag = new CompoundTag();
            ammo.save(ammoTag);
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
            if (!level.isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK)
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
        float localYaw = Mth.wrapDegrees(p.getYRot() - baseYaw);

        // Clamp yaw
        float side = configType.getSideViewLimit();
        localYaw = Mth.clamp(localYaw, -side, side);

        // Clamp pitch
        float top = configType.getTopViewLimit();
        float bottom = configType.getBottomViewLimit();
        if (top > bottom)
        {
            float tmp = top;
            top = bottom; bottom = tmp;
        }
        float pitch = Mth.clamp(p.getXRot(), top, bottom);

        setYRot(baseYaw + localYaw);
        setXRot(pitch);

        double standBack = configType.getStandBackDist();
        float yawRad = getYRot() * Mth.DEG_TO_RAD;

        double offX = standBack * Math.sin(yawRad);
        double offZ = -standBack * Math.cos(yawRad);

        double x = getX() + offX;
        double z = getZ() + offZ;

        // Pitch -> Y offset
        // Positive pitch (looking down) -> +Y (go up)
        // Negative pitch (looking up) -> -Y (go down)
        float maxAbsPitch = Math.max(Math.abs(top), Math.abs(bottom));
        float pitchNorm = (maxAbsPitch > 0.0001F) ? (pitch / maxAbsPitch) : 0F;
        double maxPitchYOffset = 0.5D;
        double pitchYOffset = maxPitchYOffset * pitchNorm;
        double baseY = blockPos.getY() + p.getMyRidingOffset() - 0.65D;
        double y = baseY + pitchYOffset;

        move.accept(passenger, x, y, z);

        // Prevent sliding / falling weirdness while mounted
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.fallDistance = 0.0F;
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger)
    {
        super.addPassenger(passenger);

        shootKeyPressed = false;
        prevShootKeyPressed = false;
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger)
    {
        super.removePassenger(passenger);

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
            // Put them behind the gun (tweak distance as you like)
            float yawRad = getShootingYaw() * Mth.DEG_TO_RAD;

            double dist = configType.getStandBackDist();
            Vec3 preferred = new Vec3(blockPos.getX() + 0.5D, blockPos.getY() - 1D, blockPos.getZ() + 0.5D)
                .add(dist * Math.sin(yawRad), 0.0D, -dist * Math.cos(yawRad));

            // Make it safe: must not collide at that position.
            if (isSafeDismount(level, passenger, preferred))
                return preferred;
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
            shootKeyPressed = false;

        if (level.isClientSide)
            clientTick();
        else
            serverTick(level);
    }

    @OnlyIn(Dist.CLIENT)
    protected void clientTick()
    {
        if (getFirstPassenger() != Minecraft.getInstance().player)
            return;

        // Force release key when entering a GUI
        if (Minecraft.getInstance().screen != null)
        {
            if (shootKeyPressed)
            {
                shootKeyPressed = false;
                PacketHandler.sendToServer(new PacketDeployedGunInput(this, false, prevShootKeyPressed));
            }
        }
        else
        {
            GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(InteractionHand.MAIN_HAND);
            shootKeyPressed = primaryFunctionState.isPressed();
            prevShootKeyPressed = primaryFunctionState.isPrevPressed();

            // Send update to server when key is pressed or released
            if (shootKeyPressed != prevShootKeyPressed)
                PacketHandler.sendToServer(new PacketDeployedGunInput(this, shootKeyPressed, prevShootKeyPressed));
        }
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
        if (shootTimer > 0)
            shootTimer--;
        if (soundTimer > 0)
            soundTimer--;
        if (reloadTimer > 0)
            setReloadTimer(reloadTimer - 1);

        // Ammo broken/empty
        if (!ammo.isEmpty() && ammo.isDamageableItem() && ammo.getDamageValue() >= ammo.getMaxDamage())
        {
            ammo = ItemStack.EMPTY;
            setHasAmmo(false);
        }

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
        if (level.isClientSide || !gunner.isAlive() || ammo.isEmpty() || reloadTimer > 0 || shootTimer > 0 || !(ammo.getItem() instanceof ShootableItem shootableItem))
            return;

        boolean automaticFire = configType.getFireMode(null).isAutomaticFire();
        if ((automaticFire && shootKeyPressed) || (!automaticFire && shootKeyPressed && !prevShootKeyPressed))
        {
            float shootDelay = configType.getShootDelay(null);

            while (shootTimer <= 0)
            {
                ShootingHelper.fireGun(level, gunner, this, shootableItem.getConfigType(), ammo, new DeployableGunShootingHandler(ammo));

                if (soundTimer <= 0)
                {
                    if (StringUtils.isNotBlank(configType.getShootSound()))
                    {
                        PacketPlaySound.sendSoundPacket(this, configType.getGunSoundRange(), configType.getShootSound(), configType.isDistortSound(), configType.isSilencedSound(null));
                        soundTimer = configType.getShootSoundLength();
                    }

                    if (StringUtils.isNotBlank(configType.getDistantShootSound()))
                        PacketHandler.sendToDonut(level.dimension(), position(), configType.getGunSoundRange(), configType.getDistantSoundRange(), new PacketPlaySound(getX(), getY(), getZ(), configType.getDistantSoundRange(), configType.getDistantShootSound(), false, false));
                }

                shootTimer += shootDelay;

                if (!automaticFire)
                    break;
            }
        }
    }

    public void reloadGun(Level level, Player gunner)
    {
        if (level.isClientSide || !gunner.isAlive() || !ammo.isEmpty() || reloadTimer > 0)
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
        if (level.isClientSide || !gunner.isAlive() || !ammo.isEmpty() || reloadTimer > 0)
            return;

        ammo = newAmmo.copy();
        setHasAmmo(true);
        setReloadTimer(configType.getReloadTime());
        String reloadSound = configType.getReloadSound(null);

        // Play reload sound
        if (StringUtils.isNotBlank(reloadSound))
            PacketPlaySound.sendSoundPacket(gunner, configType.getReloadSoundRange(), reloadSound, false, false);
    }

    public Vec3 getShootingOrigin()
    {
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
