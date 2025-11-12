package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.ParticleHelper;
import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.FlansExplosion;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketFlak;
import com.flansmodultimate.network.PacketFlashBang;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Grenade extends Shootable
{
    public static final int RENDER_DISTANCE = 64;

    @Getter
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
    protected boolean isThisStick;
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

    @Override
    public boolean isPickable()
    {
        return !isRemoved() && grenadeType.isDeployableBag();
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
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport)
    {
        // no-op: ignore vanilla client interpolation
    }

    @Override
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

        return InteractionResult.sidedSuccess(level().isClientSide);
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
            //TODO tick()
            resolveUUID(level);
        }
        catch (Exception ex)
        {
            FlansMod.log.error("Error ticking grenade", ex);
            discard();
        }
    }

    /** Resolve UUID of thrower */
    protected void resolveUUID(Level level)
    {
        if (thrower == null && throwerUUID != null && level instanceof ServerLevel sLevel && sLevel.getEntity(throwerUUID) instanceof LivingEntity living)
            thrower = living;
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
        if (level.isClientSide)
            return;
        if (grenadeType.getFireRadius() <= 0.1F)
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
        if (level.isClientSide)
            return;
        if (StringUtils.isBlank(grenadeType.getDropItemOnDetonate()))
            return;

        ItemStack dropStack = InfoType.getRecipeElement(grenadeType.getDropItemOnDetonate(), grenadeType.getContentPack());
        if (dropStack != null && !dropStack.isEmpty())
            spawnAtLocation(dropStack, 1.0F);
    }

    protected void handleSmokeAndFlashbang(Level level)
    {
        if (grenadeType.getSmokeTime() > 0) {
            smoking = true;
            smokeTime = grenadeType.getSmokeTime();
        }
        else if (!level.isClientSide)
        {
            discard();
        }

        if (!grenadeType.isFlashBang())
            return;
        if (level.isClientSide)
            return;

        double smokeRadius = grenadeType.getSmokeRadius();
        AABB aabb = getBoundingBox().inflate(smokeRadius, smokeRadius, smokeRadius);

        List<LivingEntity> list = ModUtils.queryEntities(level, null, aabb, LivingEntity.class, null);
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

        PacketHandler.sendToAllAround(new PacketFlak(getX(), getY(), getZ(), 50, grenadeType.getSmokeParticleType()), getX(), getY(), getZ(), 30, level.dimension());

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
