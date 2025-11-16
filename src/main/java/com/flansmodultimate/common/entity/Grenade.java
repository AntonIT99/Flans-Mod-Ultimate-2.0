package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.ParticleHelper;
import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.FlansExplosion;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.event.GrenadeProximityEvent;
import com.flansmodultimate.network.PacketFlak;
import com.flansmodultimate.network.PacketFlashBang;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Grenade extends Shootable
{
    public static final int RENDER_DISTANCE = 64;

    protected GrenadeType grenadeType;

    /** The entity that threw them */
    @Nullable
    protected LivingEntity thrower;
    @Nullable
    protected UUID throwerUUID;
    /** This is to avoid players grenades teamkilling after they switch team */
    //TODO: add Teams
    //protected Team teamOfThrower;
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
    protected int stuckToX;
    protected int stuckToY;
    protected int stuckToZ;
    /** Stop repeat detonations */
    protected boolean detonated;
    /** For deployable bags */
    protected int numUsesRemaining;
    @Nullable
    protected Entity stickedEntity;
    protected int motionTime;

    public Grenade(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    /**
     * General constructor. Example usecase: grenades spawned via console command
     *
     * @param level         Level in which the grenade will spawn in
     * @param grenadeType   GrenadeType of the grenade
     * @param position      Position the grenade will spawn at
     * @param rotationPitch Pitch of the direction the grenade will fly
     * @param rotationYaw   Yaw of the direction the grenade will fly
     */
    public Grenade(Level level, GrenadeType grenadeType, Vec3 position, float rotationPitch, float rotationYaw)
    {
        super(FlansMod.grenadeEntity.get(), level, grenadeType);
        setPos(position);
        this.grenadeType = grenadeType;
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
    }

    /**
     * General constructor for entitys throwing grenades. This should not be used when a player throws the grenade
     *
     * @param livingEntity  Entity throwing the grenade
     * @param grenadeType   GrenadeType of the grenade
     */
    public Grenade(@NotNull LivingEntity livingEntity, GrenadeType grenadeType)
    {
        this(livingEntity.level(), grenadeType, livingEntity.getEyePosition(), livingEntity.getXRot(), livingEntity.getYRot());
        thrower = livingEntity;
    }

    /**
     * Constructor for grenades thrown where a player and/or an entity can be associated with.
     * E.g. mecha using a grenade launcher. In this case the 'entity' is the mecha and the 'player' the player controlling the mecha
     *
     * @param level         World in which the grenade will spawn in
     * @param grenadeType   GrenadeType of the grenade
     * @param position      Position the grenade will spawn at
     * @param rotationPitch Pitch of the direction the grenade will fly
     * @param rotationYaw   Yaw of the direction the grenade will fly
     * @param entity        The entity throwing the grenade.
     */
    public Grenade(Level level, GrenadeType grenadeType, Vec3 position, float rotationPitch, float rotationYaw, @Nullable LivingEntity entity)
    {
        this(level, grenadeType, position, rotationPitch, rotationYaw);
        thrower = entity;
    }

    public GrenadeType getGrenadeType()
    {
        if (grenadeType == null && InfoType.getInfoType(getShortName()) instanceof GrenadeType gType)
        {
            grenadeType = gType;
        }
        return grenadeType;
    }

    @Override
    public boolean isPickable()
    {
        return !isRemoved() && getGrenadeType().isDeployableBag();
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
        buf.writeInt(thrower == null ? 0 : thrower.getId());
        buf.writeFloat(axes.getYaw());
        buf.writeFloat(axes.getPitch());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        try
        {
            super.readSpawnData(buf);
            if (InfoType.getInfoType(shortname) instanceof GrenadeType type)
                grenadeType = type;
            if (grenadeType == null)
            {
                FlansMod.log.warn("Unknown grenade type {}, discarding.", shortname);
                discard();
                return;
            }

            int entityId = buf.readInt();
            Entity ent = level().getEntity(entityId);
            if (ent instanceof LivingEntity living)
            {
                thrower = living;
                throwerUUID = living.getUUID();
            }

            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            setRot(yaw, pitch);
            yRotO = yaw;
            xRotO = pitch;
            axes.setAngles(yaw, pitch, 0F);
            if (grenadeType.isSpinWhenThrown())
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
        setShortName(tag.getString("type"));
        InfoType infoType = InfoType.getInfoType(shortname);
        if (infoType instanceof GrenadeType gType)
        {
            grenadeType = gType;

            if (tag.hasUUID("thrower"))
                throwerUUID = tag.getUUID("thrower");

            // Orientation
            float yaw   = tag.getFloat("rotationyaw");
            float pitch = tag.getFloat("rotationpitch");
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
        tag.putString("type", shortname);

        // Store UUID (robust across renames/offline)
        if (thrower != null)
            throwerUUID = thrower.getUUID();
        if (throwerUUID != null)
            tag.putUUID("thrower", throwerUUID);

        tag.putFloat("rotationyaw", axes.getYaw());
        tag.putFloat("rotationpitch", axes.getPitch());
    }

    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        if (!grenadeType.isDeployableBag())
            return InteractionResult.PASS;

        Level level = level();

        if (!level.isClientSide)
        {
            boolean used = false;

            // Heal
            if (grenadeType.getHealAmount() > 0 && player.getHealth() < player.getMaxHealth())
            {
                player.heal(grenadeType.getHealAmount());

                // Hearts around the player (server side)
                ServerLevel serverLevel = (ServerLevel) level;
                serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.2, 0.3, 0.2, 0.0);
                used = true;
            }

            // Potion effects
            for (MobEffectInstance effect : grenadeType.getPotionEffects())
            {
                // clone to avoid sharing mutable instances
                player.addEffect(new MobEffectInstance(effect));
                used = true;
            }

            // Ammo give
            if (grenadeType.getNumClips() > 0)
            {
                ItemStack inHand = player.getItemInHand(hand);
                if (!inHand.isEmpty() && inHand.getItem() instanceof GunItem gunItem && gunItem.getConfigType().isAllowRearm())
                {
                    GunType gunType = gunItem.getConfigType();
                    List<ShootableType> ammoTypes = gunType.getAmmoTypes();

                    if (!ammoTypes.isEmpty())
                    {
                        ShootableType bulletToGive = ammoTypes.get(0);
                        int numToGive = Math.min(bulletToGive.getMaxStackSize(), grenadeType.getNumClips() * gunType.getNumAmmoItemsInGun(inHand));
                        Item item = ItemFactory.createItem(bulletToGive);
                        if (item != null && player.getInventory().add(new ItemStack(item, numToGive)))
                            used = true;
                    }
                }
            }

            // Consume a use and remove when empty
            if (used)
            {
                this.numUsesRemaining--;
                if (this.numUsesRemaining <= 0)
                    this.discard();
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        if (grenadeType.isDetonateWhenShot())
        {
            detonate();
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
            applyGravity();

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
        if (thrower == null && throwerUUID != null && level instanceof ServerLevel sLevel && sLevel.getEntity(throwerUUID) instanceof LivingEntity living)
            thrower = living;
    }

    protected void decrementMotionTime()
    {
        if (motionTime > 0)
            motionTime--;
    }

    protected boolean shouldDespawn()
    {
        int despawnTime = grenadeType.getDespawnTime();
        if (ModCommonConfigs.grenadeDefaultRespawnTime.get() > 0)
        {
            despawnTime = Math.min(despawnTime, ModCommonConfigs.grenadeDefaultRespawnTime.get());
        }
        if (despawnTime > 0 && tickCount > despawnTime)
        {
            detonated = true;
            return true;
        }
        return false;
    }

    protected void spawnTrailParticles(Level level) {
        if (!level.isClientSide || !grenadeType.isTrailParticles())
            return;

        // Using previous position fields (Mojmap: xo, yo, zo)
        double dx = (getX() - xo) / 10.0;
        double dy = (getY() - yo) / 10.0;
        double dz = (getZ() - zo) / 10.0;

        for (int i = 0; i < 10; i++)
        {
            double px = xo + dx * i;
            double py = yo + dy * i;
            double pz = zo + dz * i;
            ParticleHelper.spawnFromString((ClientLevel) level, grenadeType.getTrailParticleType(), px, py, pz);
        }
    }

    protected void handleSmoke(Level level) {
        if (!smoking)
            return;

        // Send flak packet to spawn particles
        if (!level.isClientSide)
            PacketHandler.sendToAllAround(new PacketFlak(getX(), getY(), getZ(), GrenadeType.SMOKE_PARTICLES_COUNT, grenadeType.getSmokeParticleType()), getX(), getY(), getZ(), GrenadeType.SMOKE_PARTICLES_RANGE, level.dimension());

        // Apply potion effects in smoke radius
        double r = grenadeType.getSmokeRadius();
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
                grenadeType.getSmokeEffects().forEach(effect -> entity.addEffect(new MobEffectInstance(effect)));
        }

        smokeTime--;
        if (smokeTime <= 0)
            discard();
    }

    protected void handleDetonationConditions(Level level)
    {
        if (level.isClientSide)
            return;

        // Fuse
        if (grenadeType.getFuse() > 0 && tickCount > grenadeType.getFuse())
            detonate();

        // Proximity triggers
        if (grenadeType.getLivingProximityTrigger() <= 0 && grenadeType.getDriveableProximityTrigger() <= 0)
            return;

        float checkRadius = Math.max(grenadeType.getLivingProximityTrigger(), grenadeType.getDriveableProximityTrigger());
        double rLivingSq = grenadeType.getLivingProximityTrigger() * grenadeType.getLivingProximityTrigger();
        double rDriveableSq = grenadeType.getDriveableProximityTrigger() * grenadeType.getDriveableProximityTrigger();

        List<Entity> list = ModUtils.queryEntities(level, this, getBoundingBox().inflate(checkRadius, checkRadius, checkRadius));
        for (Entity entity : list)
        {
            if (entity == thrower && (tickCount < 10 || !ModCommonConfigs.grenadeProximityTriggerFriendlyFire.get()))
                continue;

            // Living proximity
            if (entity instanceof LivingEntity living && living.distanceToSqr(this) < rLivingSq)
            {
                //TODO: Teams
                //TODO: check team of thrower and check ModCommonConfigs.grenadeProximityTriggerFriendlyFire.get()

                // Friendly fire check
                /*if (TeamsManager.getInstance() != null
                        && TeamsManager.getInstance().currentRound != null
                        && entity instanceof ServerPlayer
                        && player.isPresent()) {

                    EntityDamageSourceFlan damageSource =
                            new EntityDamageSourceFlan(getType().shortName, this, player.get(), getType());

                    if (!TeamsManager.getInstance().currentRound.gametype.playerAttacked((ServerPlayer) entity, damageSource)) {
                        continue;
                    }
                }*/

                if (handleEntityInProximityTriggerRange(living))
                    break;
            }

            // Driveable proximity
            if (entity instanceof Driveable driveable && entity.distanceToSqr(this) < rDriveableSq && handleEntityInProximityTriggerRange(driveable))
                break;
        }
    }

    protected boolean handleEntityInProximityTriggerRange(Entity entity)
    {
        GrenadeProximityEvent event = new GrenadeProximityEvent(this, entity);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return false;

        if (getGrenadeType().getDamageToTriggerer() > 0F)
            entity.hurt(getGrenadeDamage(), getGrenadeType().getDamageToTriggerer());

        detonate();
        return true;
    }

    protected void updateStuckState(Level level)
    {
        if (stuck)
        {
            BlockPos pos = new BlockPos(stuckToX, stuckToY, stuckToZ);
            if (level.isEmptyBlock(pos))
                stuck = false;
        }
    }

    protected void handlePhysicsAndMotion(Level level)
    {
        if (stuck || grenadeType.isStickToThrower())
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
        if (grenadeType.isExplodeOnImpact())
        {
            detonate();
            return;
        }

        // Break glass
        if (grenadeType.isBreaksGlass() && ModUtils.isGlass(state) && TeamsManager.isCanBreakGlass() && !level.isClientSide)
        {
            ModUtils.destroyBlock((ServerLevel) level, blockPos, thrower, false);
        }

        // Bounce / stick if not penetrating blocks
        if (!grenadeType.isPenetratesBlocks())
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
        postHitMotVec.scale(grenadeType.getBounciness() / 2F);

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
            FlansMod.getSoundEvent(grenadeType.getBounceSound()).ifPresent(soundEvent ->
                playSound(soundEvent.get(), 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F)));
        }

        // Sticky grenades
        if (grenadeType.isSticky())
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
            BlockPos bp = hit.getBlockPos();
            stuckToX = bp.getX();
            stuckToY = bp.getY();
            stuckToZ = bp.getZ();
        }
    }

    protected void updateStickToThrower() {
        if (!grenadeType.isStickToThrower())
            return;

        if (thrower == null || !thrower.isAlive())
            discard();
        else
            setPos(thrower.getX(), thrower.getY(), thrower.getZ());
    }

    protected void handleStickToEntity(Level level)
    {
        if (!grenadeType.isStickToEntity())
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
        if (!grenadeType.isStickToDriveable())
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
        if (!grenadeType.isStickToEntityAfter())
            return;

        if (stickedEntity == null)
        {
            ModUtils.queryEntities(level, this, getBoundingBox(), entity -> entity != thrower && !(entity instanceof Grenade)).stream()
                .findFirst()
                .ifPresent(entity -> {
                    if (grenadeType.isAllowStickSound())
                    {
                        PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), grenadeType.getStickSoundRange(), level.dimension(), grenadeType.getStickSound(), true);
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
        if (stuck || (grenadeType.getDamage().getDamageVsLiving() <= 0F && grenadeType.getDamage().getDamageVsPlayer() <= 0F))
            return;

        double speedSq = velocity.lengthSqr();
        if (speedSq < 0.01D)
            return;

        List<LivingEntity> list = ModUtils.queryLivingEntities(level, this, getBoundingBox());
        for (LivingEntity living : list)
        {
            if (living == thrower && tickCount < 10)
                continue;

            float damageFactor = (float) (speedSq * 3.0D);

            if (living instanceof Player player)
                player.hurt(getGrenadeDamage(), grenadeType.getDamage().getDamageVsPlayer() * damageFactor);
            else
                living.hurt(getGrenadeDamage(), grenadeType.getDamage().getDamageVsLiving() * damageFactor);
        }
    }

    protected void applyGravity()
    {
        double gravity = 9.81D / 400D * grenadeType.getFallSpeed();
        velocity = velocity.add(0, - gravity, 0);
        setDeltaMovement(velocity);
    }

    public void detonate()
    {
        Level level = level();
        //Do not detonate before grenade is primed
        if (!shouldDetonateNow())
            return;
        //Stop repeat detonations
        if (detonated)
            return;
        detonated = true;

        playDetonateSound(level);
        doExplosion(level);
        spreadFire(level);
        spawnExplosionParticles(level);
        dropItemsOnDetonate(level);
        handleSmokeAndFlashbang(level);
    }

    protected boolean shouldDetonateNow()
    {
        return tickCount >= grenadeType.getPrimeDelay();
    }

    protected void playDetonateSound(Level level)
    {
        PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.SOUND_RANGE, level.dimension(), grenadeType.getDetonateSound(), true);
    }

    protected void doExplosion(Level level) {
        if (level.isClientSide || grenadeType.getExplosionRadius() <= 0.1F)
            return;

        new FlansExplosion(level(), this, thrower, grenadeType, getX(), getY(), getZ(), false);
    }

    protected void spreadFire(Level level)
    {
        if (level.isClientSide || grenadeType.getFireRadius() <= 0.1F)
            return;

        float fireRadius = grenadeType.getFireRadius();
        for (float i = -fireRadius; i < fireRadius; i++)
        {
            for (float j = -fireRadius; j < fireRadius; j++)
            {
                for (float k = -fireRadius; k < fireRadius; k++)
                {
                    if (i * i + j * j + k * k > fireRadius * fireRadius)
                        continue;

                    BlockPos pos = BlockPos.containing(i + getX(), j + getY(), k + getZ());
                    if (level.getBlockState(pos).isAir() && random.nextBoolean()) {
                        // Keep the 1.12.2 behavior that immediately sets and schedules fire
                        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 2);
                        level.scheduleTick(pos, Blocks.FIRE, 0);
                    }
                }
            }
        }
    }

    protected void spawnExplosionParticles(Level level)
    {
        if (!level.isClientSide)
            return;

        for (int i = 0; i < grenadeType.getExplodeParticles(); i++)
        {
            ParticleHelper.spawnFromString((ClientLevel) level, grenadeType.getExplodeParticleType(), getX(), getY(), getZ(), random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        }
    }

    protected void dropItemsOnDetonate(Level level)
    {
        if (level.isClientSide || StringUtils.isBlank(grenadeType.getDropItemOnDetonate()))
            return;

        ItemStack dropStack = InfoType.getRecipeElement(grenadeType.getDropItemOnDetonate(), grenadeType.getContentPack());
        if (dropStack != null && !dropStack.isEmpty())
            spawnAtLocation(dropStack, 1.0F);
    }

    protected void handleSmokeAndFlashbang(Level level)
    {
        if (grenadeType.getSmokeTime() > 0)
        {
            smoking = true;
            smokeTime = grenadeType.getSmokeTime();
        }
        else if (!level.isClientSide)
        {
            discard();
        }

        if (!grenadeType.isFlashBang() || level.isClientSide)
            return;

        double smokeRadius = grenadeType.getSmokeRadius();
        AABB aabb = getBoundingBox().inflate(smokeRadius, smokeRadius, smokeRadius);

        List<LivingEntity> list = ModUtils.queryLivingEntities(level, aabb);
        for (LivingEntity entity : list)
        {
            if (entity.distanceTo(this) < grenadeType.getFlashRange() && grenadeType.isFlashDamageEnable())
            {
                if (grenadeType.isFlashEffects())
                {
                    MobEffect effect = MobEffect.byId(grenadeType.getFlashEffectsId());
                    if (effect != null)
                        entity.addEffect(new MobEffectInstance(effect, grenadeType.getFlashEffectsDuration(), grenadeType.getFlashEffectsLevel()));
                }
                entity.hurt(this.getGrenadeDamage(), grenadeType.getFlashDamage());
            }
        }

        PacketHandler.sendToAllAround(new PacketFlak(getX(), getY(), getZ(), GrenadeType.SMOKE_PARTICLES_COUNT, grenadeType.getSmokeParticleType()), getX(), getY(), getZ(), GrenadeType.SMOKE_PARTICLES_RANGE, level.dimension());

        if (grenadeType.isFlashSoundEnable())
            PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), grenadeType.getFlashSoundRange(), level.dimension(), grenadeType.getFlashSound(), true);

        PacketHandler.sendToAllAround(new PacketFlashBang(grenadeType.getFlashTime()), getX(), getY(), getZ(), grenadeType.getFlashRange(), level.dimension());

        discard();
    }

    protected DamageSource getGrenadeDamage()
    {
        return FlansDamageSources.createDamageSource(level(), this, thrower, FlansDamageSources.FLANS_SHOOTABLE);
    }
}
