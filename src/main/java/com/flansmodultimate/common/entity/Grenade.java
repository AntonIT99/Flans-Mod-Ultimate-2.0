package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.event.GrenadeProximityEvent;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketFlashBang;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Grenade extends Shootable implements IFlanEntity<GrenadeType>
{
    public static final int RENDER_DISTANCE = 64;

    public static final String NBT_THROWER = "thrower_uuid";
    public static final String NBT_PITCH = "rotation_pitch";
    public static final String NBT_YAW = "rotation_yaw";

    protected GrenadeType configType;
    /** The entity that threw them */
    @Nullable
    protected LivingEntity thrower;
    /** Yeah, I want my grenades to have fancy physics */
    @Getter
    protected RotatedAxes axes = new RotatedAxes();
    protected Vec3 angularVelocity = new Vec3(0, 0, 0);
    @Getter
    protected float prevRotationRoll;
    /** Set to the smoke amount when the grenade detonates and decremented every tick after that */
    protected int smokeTime;
    /** Set to true when smoke grenade detonates */
    protected boolean smoking;
    /** Set to true when a sticky grenade sticks. Impedes further movement */
    @Getter
    protected boolean stuck;
    /** Stores the position of the block this grenade is stuck to. Used to determine when to unstick */
    protected BlockPos stuckPos;
    /** For deployable bags */
    protected int numUsesRemaining;
    @Nullable
    protected Entity stickedEntity;
    protected int motionTime;

    /** These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order */
    protected boolean checkForUUIDs;
    @Nullable
    protected UUID throwerUUID;

    public Grenade(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Grenade(Level level, GrenadeType grenadeType, Vec3 position, Vec3 direction, @Nullable LivingEntity entity)
    {
        this(level, grenadeType, position, ModUtils.getPitchFromDirection(direction), ModUtils.getYawFromDirection(direction), entity);
    }

    /** For living entities throwing grenades. */
    public Grenade(Level level, GrenadeType grenadeType, @NotNull LivingEntity livingEntity)
    {
        this(level, grenadeType, livingEntity.getEyePosition(), livingEntity.getXRot(), livingEntity.getYRot(), livingEntity);
    }

    public Grenade(Level level, GrenadeType grenadeType, Vec3 position, float rotationPitch, float rotationYaw, @Nullable LivingEntity entity)
    {
        super(FlansMod.grenadeEntity.get(), level, grenadeType);
        setPos(position);
        this.configType = grenadeType;
        numUsesRemaining = grenadeType.getNumUses();

        //Set the grenade to be facing the way the Pitch and Yaw variables define
        axes.setAngles(rotationYaw + 90F, grenadeType.isSpinWhenThrown() ? rotationPitch : 0F, 0F);
        rotationYaw = grenadeType.isSpinWhenThrown() ? rotationYaw + 90F : 0F;
        setXRot(rotationPitch);
        setYRot(rotationYaw);
        xRotO = rotationPitch;
        yRotO = rotationYaw;

        //Give the grenade velocity in the direction the player is looking
        float speed = 0.5F * grenadeType.getThrowSpeed();
        setDeltaMovement(axes.getXAxis().x * speed, axes.getXAxis().y * speed, axes.getXAxis().z * speed);
        if (grenadeType.isSpinWhenThrown())
            angularVelocity = new Vec3(0, 0, 10);

        if (grenadeType.getThrowSound() != null)
            PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.SOUND_RANGE, level.dimension(), grenadeType.getThrowSound(), true);

        thrower = entity;
        // If this can be remotely detonated, add it to the players detonate list
        if (grenadeType.isRemote() && thrower instanceof Player player)
            PlayerData.getInstance(player).getRemoteExplosives().add(this);
    }

    @Override
    public GrenadeType getConfigType()
    {
        if (configType == null && InfoType.getInfoType(getShortName()) instanceof GrenadeType gType)
        {
            configType = gType;
        }
        return configType;
    }

    @Override
    public boolean isPickable()
    {
        return !isRemoved() && getConfigType().isDeployableBag();
    }

    @Override
    public boolean displayFireAnimation()
    {
        return false;
    }

    @Override
    public boolean fireImmune()
    {
        return true;
    }

    @Override
    public boolean isOnFire()
    {
        return false;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        super.writeSpawnData(buf);
        buf.writeInt(Optional.ofNullable(thrower).map(Entity::getId).orElse(0));
        buf.writeFloat(axes.getYaw());
        buf.writeFloat(axes.getPitch());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            int throwerId = buf.readInt();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();

            if (InfoType.getInfoType(shortname) instanceof GrenadeType gType)
                configType = gType;
            if (configType == null)
            {
                FlansMod.log.warn("Unknown grenade type {}, discarding.", shortname);
                discard();
                return;
            }

            Level level = level();
            if (throwerId != 0 && level.getEntity(throwerId) instanceof LivingEntity living)
            {

                thrower = living;
                throwerUUID = living.getUUID();
            }

            setRot(yaw, pitch);
            yRotO = yaw;
            xRotO = pitch;
            axes.setAngles(yaw, pitch, 0F);

            if (configType.isSpinWhenThrown())
                angularVelocity = new Vec3(0, 0, 10);
        }
        catch (Exception e)
        {
            discard();
            FlansMod.log.warn("Failed to read grenade spawn data", e);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (InfoType.getInfoType(shortname) instanceof GrenadeType gType)
        {
            configType = gType;

            if (tag.hasUUID(NBT_THROWER))
            {
                throwerUUID = tag.getUUID(NBT_THROWER);
                checkForUUIDs = true;
            }

            // Orientation
            float yaw = tag.getFloat(NBT_YAW);
            float pitch = tag.getFloat(NBT_PITCH);
            setYRot(yaw);
            setXRot(pitch);
            yRotO = yaw;
            xRotO = pitch;
            axes.setAngles(yaw, pitch, 0f);
        }
        else
        {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);

        if (thrower != null)
            throwerUUID = thrower.getUUID();
        if (throwerUUID != null)
            tag.putUUID(NBT_THROWER, throwerUUID);

        tag.putFloat(NBT_YAW, axes.getYaw());
        tag.putFloat(NBT_PITCH, axes.getPitch());
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        if (!configType.isDeployableBag())
            return InteractionResult.PASS;

        Level level = level();

        if (!level.isClientSide)
        {
            boolean used = false;

            // Heal
            if (configType.getHealAmount() > 0 && player.getHealth() < player.getMaxHealth())
            {
                player.heal(configType.getHealAmount());

                // Hearts around the player (server side)
                ServerLevel serverLevel = (ServerLevel) level;
                serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.2, 0.3, 0.2, 0.0);
                used = true;
            }

            // Potion effects
            for (MobEffectInstance effect : configType.getPotionEffects())
            {
                // clone to avoid sharing mutable instances
                player.addEffect(new MobEffectInstance(effect));
                used = true;
            }

            // Ammo give
            if (configType.getNumClips() > 0)
            {
                ItemStack inHand = player.getItemInHand(hand);
                if (!inHand.isEmpty() && inHand.getItem() instanceof GunItem gunItem && gunItem.getConfigType().isAllowRearm())
                {
                    GunType gunType = gunItem.getConfigType();
                    List<ShootableType> ammoTypes = gunType.getAmmoTypes();

                    if (!ammoTypes.isEmpty())
                    {
                        ShootableType bulletToGive = ammoTypes.get(0);
                        int numToGive = Math.min(bulletToGive.getMaxStackSize(), configType.getNumClips() * gunType.getNumAmmoItemsInGun(inHand));
                        Item item = ItemFactory.createItem(bulletToGive);
                        if (item != null && player.getInventory().add(new ItemStack(item, numToGive)))
                            used = true;
                    }
                }
            }

            // Consume a use and remove when empty
            if (used)
            {
                numUsesRemaining--;
                if (numUsesRemaining <= 0)
                    discard();
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (configType.isDetonateWhenShot())
        {
            detonate(level());
            return true;
        }
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();
        Level level = level();
        try
        {
            if (shouldDespawn())
            {
                detonated = true;
                discard();
                return;
            }

            resolveUUID(level);
            decrementMotionTime();
            spawnTrailParticles(level);
            handleSmoke(level);
            handleDetonationConditions(level);
            updateStuckState(level);
            handlePhysicsAndMotion(level);
            updateStickToThrower();
            handleStickToEntity(level);
            handleStickToDriveable(level);
            handleStickToEntityAfter(level);
            handleImpactDamage(level);
            applyDragAndGravity();

            // Fire glitch fix
            if (level.isClientSide)
                clearFire();
        }
        catch (Exception ex)
        {
            FlansMod.log.error("Error ticking grenade {}", shortname, ex);
            discard();
        }
    }

    /** Resolve UUID of thrower */
    protected void resolveUUID(Level level)
    {
        if (level.isClientSide || !checkForUUIDs)
            return;

        if (thrower == null && throwerUUID != null && ((ServerLevel)level).getEntity(throwerUUID) instanceof LivingEntity living)
            thrower = living;

        checkForUUIDs = false;
    }

    protected void decrementMotionTime()
    {
        if (motionTime > 0)
            motionTime--;
    }

    protected void spawnTrailParticles(Level level)
    {
        if (!level.isClientSide || !configType.isTrailParticles())
            return;

        double dx = (getX() - xo) / 10.0;
        double dy = (getY() - yo) / 10.0;
        double dz = (getZ() - zo) / 10.0;

        for (int i = 0; i < 10; i++)
        {
            double x = xo + dx * i;
            double y = yo + dy * i;
            double z = zo + dz * i;

            ParticleHelper.spawnFromString(configType.getTrailParticleType(), x, y, z, 1F);
        }
    }

    protected void handleSmoke(Level level)
    {
        if (!smoking)
            return;

        // Send flak packet to spawn particles
        if (!level.isClientSide)
            PacketHandler.sendToAllAround(new PacketFlak(position(), GrenadeType.SMOKE_PARTICLES_COUNT, configType.getSmokeParticleType()), position(), GrenadeType.SMOKE_PARTICLES_RANGE, level.dimension());

        // Apply potion effects in smoke radius
        double r = configType.getSmokeRadius();
        double rSq = r * r;
        AABB aabb = getBoundingBox().inflate(r, r, r);

        List<LivingEntity> list = ModUtils.queryLivingEntities(level, aabb);
        for (LivingEntity entity : list)
        {
            if (entity.distanceToSqr(this) >= rSq)
                continue;

            // Check for gas masks / smoke protection
            boolean smokeThem = true;
            for (EquipmentSlot slot : EquipmentSlot.values())
            {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorItem armour && armour.getConfigType().isSmokeProtection())
                {
                    smokeThem = false;
                    break;
                }
            }

            if (smokeThem)
                configType.getSmokeEffects().forEach(effect -> entity.addEffect(new MobEffectInstance(effect)));
        }

        smokeTime--;
        if (smokeTime <= 0)
            discard();
    }

    protected boolean handleEntityInProximityTriggerRange(Level level, Entity entity)
    {
        GrenadeProximityEvent event = new GrenadeProximityEvent(this, entity);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return false;

        if (getConfigType().getDamageToTriggerer() > 0F)
            entity.hurt(getDamageSource(), getConfigType().getDamageToTriggerer());

        return true;
    }

    @Override
    public boolean isShooterEntity(Entity entity)
    {
        return entity == thrower;
    }

    @Override
    public Optional<LivingEntity> getOwner()
    {
        return Optional.ofNullable(thrower);
    }

    protected void updateStuckState(Level level)
    {
        if (stuck && stuckPos != null && level.isEmptyBlock(stuckPos))
            stuck = false;
    }

    protected void handlePhysicsAndMotion(Level level)
    {
        if (stuck || configType.isStickToThrower())
            return;

        // Update rotation from axes + angular velocity
        yRotO = axes.getYaw();
        xRotO = axes.getPitch();
        prevRotationRoll = axes.getRoll();

        if (angularVelocity.lengthSqr() > 0.00000001)
            axes.rotateLocal((float) angularVelocity.length(), new Vector3f(angularVelocity.normalize()));

        Vec3 posVec = position(); // current position
        Vec3 nextPosVec = posVec.add(velocity);

        BlockHitResult result = level.clip(new ClipContext(posVec, nextPosVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (result.getType() == HitResult.Type.BLOCK)
            handleBlockHit(level, posVec, velocity, result);
        else
            // No hit, just move
            setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
    }

    protected void handleBlockHit(Level level, Vec3 posVec, Vec3 motionVec, BlockHitResult hit)
    {
        BlockPos blockPos = hit.getBlockPos();
        BlockState state = level.getBlockState(blockPos);

        // If the shape is empty, the block doesn't collide (air, some plants, etc.)
        boolean blocksMovement = !state.getCollisionShape(level(), blockPos, CollisionContext.empty()).isEmpty();

        if (!blocksMovement)
        {
            // Just move if block doesn't stop motion
            setPos(getX() + motionVec.x, getY() + motionVec.y, getZ() + motionVec.z);
            return;
        }

        // Explode on impact
        if (configType.isExplodeOnImpact())
        {
            detonate(level);
            return;
        }

        // Break glass
        if (configType.isBreaksGlass() && ModUtils.isGlass(state) && FlansMod.teamsManager.isCanBreakGlass() && !level.isClientSide)
        {
            ModUtils.destroyBlock((ServerLevel) level, blockPos, thrower, false);
        }

        // Bounce / stick if not penetrating blocks
        if (!configType.isPenetratesBlocks())
            handleBounceAndSticky(posVec, motionVec, hit);
    }

    protected void handleBounceAndSticky(Vec3 posVec, Vec3 motionVec, BlockHitResult hit)
    {
        Vector3f posVecF = new Vector3f((float) posVec.x, (float) posVec.y, (float) posVec.z);
        Vector3f motVecF = new Vector3f((float) motionVec.x, (float) motionVec.y, (float) motionVec.z);
        Vector3f hitVecF = new Vector3f((float) hit.getLocation().x, (float) hit.getLocation().y, (float) hit.getLocation().z);

        // Motion pre- / post-hit
        Vector3f preHitMotVec = Vector3f.sub(hitVecF, posVecF, null);
        Vector3f postHitMotVec = Vector3f.sub(motVecF, preHitMotVec, null);

        // Reflect based on side hit
        Direction sideHit = hit.getDirection();
        switch (sideHit)
        {
            case UP, DOWN:
                postHitMotVec.setY(-postHitMotVec.getY());
                break;
            case EAST, WEST:
                postHitMotVec.setX(-postHitMotVec.getX());
                break;
            case NORTH, SOUTH:
                postHitMotVec.setZ(-postHitMotVec.getZ());
                break;
        }

        float motLenSq = motVecF.lengthSquared();
        float lambda = Math.abs(motLenSq) < 0.00000001F ? 1F : postHitMotVec.length() / (float) Math.sqrt(motLenSq);

        // Scale by bounciness
        postHitMotVec.scale(configType.getBounciness() / 2F);

        // Move grenade along path including reflection
        setPos(getX() + preHitMotVec.x + postHitMotVec.x, getY() + preHitMotVec.y + postHitMotVec.y, getZ() + preHitMotVec.z + postHitMotVec.z);

        // Set motion
        velocity = new Vec3(postHitMotVec.x / lambda, postHitMotVec.y / lambda, postHitMotVec.z / lambda);
        setDeltaMovement(velocity);

        // Random spin
        float randomSpinner = 90F;
        angularVelocity = angularVelocity.add(random.nextGaussian() * randomSpinner, random.nextGaussian() * randomSpinner, random.nextGaussian() * randomSpinner);

        // Slow spin based on motion
        angularVelocity = angularVelocity.scale(velocity.lengthSqr());

        // Bounce sound
        if (velocity.lengthSqr() > 0.01D)
        {
            FlansMod.getSoundEvent(configType.getBounceSound()).ifPresent(soundEvent ->
                playSound(soundEvent.get(), 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F)));
        }

        // Sticky grenades
        if (configType.isSticky())
        {
            // Move to exact hit position
            setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);

            // Stop all motion
            velocity = Vec3.ZERO;
            setDeltaMovement(velocity);
            angularVelocity = new Vec3(0, 0, 0);

            float yaw = axes.getYaw();
            switch (hit.getDirection())
            {
                case DOWN:
                    axes.setAngles(yaw, 180F, 0F);
                    break;
                case UP:
                    axes.setAngles(yaw, 0F, 0F);
                    break;
                case NORTH:
                    axes.setAngles(270F, 90F, 0F);
                    axes.rotateLocalYaw(yaw);
                    break;
                case SOUTH:
                    axes.setAngles(90F, 90F, 0F);
                    axes.rotateLocalYaw(yaw);
                    break;
                case WEST:
                    axes.setAngles(180F, 90F, 0F);
                    axes.rotateLocalYaw(yaw);
                    break;
                case EAST:
                    axes.setAngles(0F, 90F, 0F);
                    axes.rotateLocalYaw(yaw);
                    break;
            }

            stuck = true;
            stuckPos = hit.getBlockPos();
        }
    }

    protected void updateStickToThrower() {
        if (!configType.isStickToThrower())
            return;

        if (thrower == null || !thrower.isAlive())
            discard();
        else
            setPos(thrower.getX(), thrower.getY(), thrower.getZ());
    }

    protected void handleStickToEntity(Level level)
    {
        if (!configType.isStickToEntity())
            return;

        if (stickedEntity == null && !stuck)
        {
            ModUtils.queryEntities(level, this, getBoundingBox(), entity -> entity != thrower && !(entity instanceof Grenade)).stream()
                .findFirst()
                .ifPresent(entity -> stickedEntity = entity);
        }

        if (stickedEntity != null)
        {
            setPos(stickedEntity.getX(), stickedEntity.getY(), stickedEntity.getZ());
            if (!stickedEntity.isAlive())
                discard();
        }
    }

    protected void handleStickToDriveable(Level level)
    {
        if (!configType.isStickToDriveable())
            return;

        if (stickedEntity == null && !stuck)
        {
            ModUtils.queryEntities(level, null, getBoundingBox(), Driveable.class, null).stream()
                .findFirst()
                .ifPresent(driveable -> stickedEntity = driveable);
        }

        if (stickedEntity != null)
        {
            setPos(stickedEntity.getX(), stickedEntity.getY(), stickedEntity.getZ());
            if (!stickedEntity.isAlive())
                discard();
        }
    }

    protected void handleStickToEntityAfter(Level level)
    {
        if (!configType.isStickToEntityAfter())
            return;

        if (stickedEntity == null)
        {
            ModUtils.queryEntities(level, this, getBoundingBox(), entity -> entity != thrower && !(entity instanceof Grenade)).stream()
                .findFirst()
                .ifPresent(entity -> {
                    if (configType.isAllowStickSound())
                    {
                        PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), configType.getStickSoundRange(), level.dimension(), configType.getStickSound(), true);
                    }
                    stickedEntity = entity;
                });
        }

        if (stickedEntity != null)
        {
            setPos(stickedEntity.getX(), stickedEntity.getY(), stickedEntity.getZ());
            if (!stickedEntity.isAlive())
                discard();
        }
    }

    protected void handleImpactDamage(Level level)
    {
        if (stuck || (configType.getDamage().getDamageVsLiving() <= 0F && configType.getDamage().getDamageVsPlayer() <= 0F))
            return;

        double speedSq = velocity.lengthSqr();
        if (speedSq < 0.01)
            return;

        List<LivingEntity> list = ModUtils.queryLivingEntities(level, this, getBoundingBox());
        for (LivingEntity living : list)
        {
            if (living == thrower && tickCount < 10)
                continue;

            living.hurt(getDamageSource(), ShootingHelper.getDamage(living, this, null));
        }
    }

    @Override
    public void detonate(Level level)
    {
        if (level.isClientSide || detonated || isRemoved() || tickCount < configType.getPrimeDelay())
            return;

        detonate(level, thrower);
        startSmokeCounter();
        handleFlashbang(level);
    }

    protected void startSmokeCounter()
    {
        if (configType.getSmokeTime() > 0)
        {
            smoking = true;
            smokeTime = configType.getSmokeTime();
        }
        else
        {
            discard();
        }
    }

    protected void handleFlashbang(Level level)
    {
        if (!configType.isFlashBang() || level.isClientSide)
            return;

        double smokeRadius = configType.getSmokeRadius();
        AABB aabb = getBoundingBox().inflate(smokeRadius, smokeRadius, smokeRadius);

        List<LivingEntity> list = ModUtils.queryLivingEntities(level, aabb);
        for (LivingEntity entity : list)
        {
            if (entity.distanceTo(this) < configType.getFlashRange() && configType.isFlashDamageEnable())
            {
                if (configType.isFlashEffects())
                {
                    MobEffect effect = MobEffect.byId(configType.getFlashEffectsId());
                    if (effect != null)
                        entity.addEffect(new MobEffectInstance(effect, configType.getFlashEffectsDuration(), configType.getFlashEffectsLevel()));
                }
                entity.hurt(getDamageSource(), configType.getFlashDamage());
            }
        }

        PacketHandler.sendToAllAround(new PacketFlak(position(), GrenadeType.SMOKE_PARTICLES_COUNT, configType.getSmokeParticleType()), position(), GrenadeType.SMOKE_PARTICLES_RANGE, level.dimension());

        if (configType.isFlashSoundEnable())
            PacketPlaySound.sendSoundPacket(position(), configType.getFlashSoundRange(), level.dimension(), configType.getFlashSound(), true);

        PacketHandler.sendToAllAround(new PacketFlashBang(configType.getFlashTime()), position(), configType.getFlashRange(), level.dimension());

        discard();
    }

    protected DamageSource getDamageSource()
    {
        return FlanDamageSources.createDamageSource(level(), this, thrower, FlanDamageSources.SHOOTABLE);
    }
}
