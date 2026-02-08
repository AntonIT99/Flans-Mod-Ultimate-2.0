package com.flansmodultimate.common.guns;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.FlanExplosion;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.ShootableFactory;
import com.flansmodultimate.common.guns.handler.ShootingHandler;
import com.flansmodultimate.common.guns.penetration.PenetrableBlock;
import com.flansmodultimate.common.guns.penetration.PenetrationLoss;
import com.flansmodultimate.common.raytracing.Raytracer;
import com.flansmodultimate.common.raytracing.hits.BlockHit;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketExplodeParticles;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class containing a bunch of shooting related functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootingHelper
{
    public static final float ANGULAR_SPREAD_FACTOR = 0.0025F;

    //TODO: make configurable
    private static final double ENTITY_HIT_PARTICLE_RANGE = 64.0;
    private static final double BLOCK_HIT_PARTICLE_RANGER = 64.0;

    /** Call this to fire bullets or grenades from a living entity holding a gun item (Server side) */
    public static void fireGun(@NotNull Level level, @NotNull LivingEntity shooter, @NotNull GunType gunType, @NotNull ShootableType shootableType, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack, @NotNull ShootingHandler handler)
    {
        int numBullets = gunType.getNumBullets(gunStack, shootableType);

        if (gunType.getBulletSpeed(gunStack, shootableStack) == 0F && shootableType instanceof BulletType bulletType)
        {
            // Raytrace without entity
            FiredShot firedShot = new FiredShot(gunType, bulletType, gunStack, shootableStack, otherHandStack, shooter);
            for (int i = 0; i < numBullets; i++)
                createShot(level, firedShot, shooter.getEyePosition(0.0F), shooter.getLookAngle());
        }
        else
        {
            // Spawn shootable entities
            Shootable shootable = ShootableFactory.createShootable(level, gunType, shootableType, shooter, gunStack, shootableStack, otherHandStack);
            for (int i = 0; i < numBullets; i++)
                level.addFreshEntity(shootable);
        }

        handler.onShoot();
    }

    /** Call this to fire bullets or grenades from a living entity controlling a deployed gun (Server side) */
    public static void fireGun(@NotNull Level level, @Nullable LivingEntity shooter, @NotNull DeployedGun deployedGun, @NotNull ShootableType shootableType, @NotNull ItemStack shootableStack, @NotNull ShootingHandler handler)
    {
        int numBullets = deployedGun.getConfigType().getNumBullets(null, shootableType);

        if (deployedGun.getConfigType().getBulletSpeed() == 0F && shootableType instanceof BulletType bulletType)
        {
            // Raytrace without entity
            FiredShot firedShot = new FiredShot(new FireableGun(deployedGun.getConfigType(), shootableStack), bulletType, deployedGun, shooter);
            for (int i = 0; i < numBullets; i++)
                createShot(level, firedShot, deployedGun.getShootingOrigin(), deployedGun.getShootingDirection());
        }
        else
        {
            // Spawn shootable entities
            Shootable shootable = ShootableFactory.createShootable(level, shootableType, deployedGun, shooter, shootableStack);
            for (int i = 0; i < numBullets; i++)
                level.addFreshEntity(shootable);
        }

        handler.onShoot();
    }

    /** Call this to fire bullets from other sources (Server side) */
    public static void fireGun(@NotNull Level level, @NotNull FiredShot firedShot, int numBullets, Vec3 shootingOrigin, Vec3 shootingDirection, @NotNull ShootingHandler handler)
    {
        if (firedShot.getFireableGun().getBulletSpeed() == 0F)
        {
            // Raytrace without entity
            for (int i = 0; i < numBullets; i++)
                createShot(level, firedShot, shootingOrigin, shootingDirection);
        }
        else
        {
            // Spawn shootable entities
            Bullet bullet = new Bullet(level, firedShot, shootingOrigin, shootingDirection);
            for (int i = 0; i < numBullets; i++)
                level.addFreshEntity(bullet);
        }

        handler.onShoot();
    }

    public record HitData(float penetratingPower, float lastHitPenAmount, boolean lastHitHeadshot) {}

    public static HitData onHit(Level level, FiredShot shot, BulletHit bulletHit, Vec3 hit, Vec3 shootingMotion, HitData hitData, @Nullable Bullet bullet)
    {
        float penetratingPower = hitData.penetratingPower();
        float lastHitPenAmount = hitData.lastHitPenAmount();
        boolean lastHitHeadshot = hitData.lastHitHeadshot();
        boolean showHitMarker = false;

        BulletType bulletType = shot.getBulletType();
        Optional<ServerPlayer> playerOwner = shot.getPlayerAttacker();
        LivingEntity owner = shot.getAttacker().orElse(null);

        if (bulletHit instanceof DriveableHit driveableHit)
        {
            if (bulletType.isEntityHitSoundEnable() && !level.isClientSide)
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true, null);

            AtomicBoolean isFriendly = new AtomicBoolean(false);
            driveableHit.getDriveable().setLastAtkEntity(owner);

            playerOwner.ifPresent(serverPlayer ->
                FlansMod.teamsManager.getCurrentRound().ifPresent(round -> {
                    for (Seat seat : driveableHit.getDriveable().getSeats())
                    {
                        if (seat.getRiddenByEntity() instanceof Player controllingPlayer)
                        {
                            PlayerData dataDriver = PlayerData.getInstance(controllingPlayer);
                            PlayerData dataAttacker = PlayerData.getInstance(serverPlayer);
                            if (dataDriver.getTeam().getShortName().equals(dataAttacker.getTeam().getShortName()))
                                isFriendly.set(true);
                        }
                    }
                }
            ));

            if (isFriendly.get())
                penetratingPower = 0F;
            else
            {
                HitData driveableHitData = driveableHit.getDriveable().bulletHit(bulletType, driveableHit, hitData);
                penetratingPower = driveableHitData.penetratingPower();
                lastHitPenAmount = driveableHitData.lastHitPenAmount();
                lastHitHeadshot = driveableHitData.lastHitHeadshot();
            }

            if (bulletType.isCanSpotEntityDriveable())
                driveableHit.getDriveable().setEntityMarker(200);

            ClientHooks.RENDER.spawnDebugDot(hit, 1000, 0F, 0F, 1F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof PlayerBulletHit playerHit)
        {
            if (bulletType.isEntityHitSoundEnable() && !level.isClientSide)
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true, null);

            float prevPenetratingPower = penetratingPower;
            HitData playerHitData = playerHit.getHitbox().hitByBullet(shot, hitData, bullet);
            penetratingPower = playerHitData.penetratingPower();
            lastHitPenAmount = playerHitData.lastHitPenAmount();
            lastHitHeadshot = playerHitData.lastHitHeadshot();

            if (bullet != null)
                bullet.getPenetrationLosses().add(new PenetrationLoss((prevPenetratingPower - penetratingPower), PenetrationLoss.EnumType.PLAYER));

            ClientHooks.RENDER.spawnDebugDot(hit, 1000, 1F, 0F, 0F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof EntityHit entityHit && entityHit.getEntity() != null)
        {
            Entity entity = entityHit.getEntity();

            if (bulletType.isEntityHitSoundEnable() && !level.isClientSide)
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true, null);

            if (owner instanceof Player)
                lastHitPenAmount = 1F;

            if (!level.isClientSide)
            {
                float damage = ShootingHelper.getDamage(entity, bullet, shot);

                if (entity.hurt(shot.getDamageSource(level, bullet), damage) && entity instanceof LivingEntity living)
                {
                    PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.RED_DUST, entityHit.getEntity().getX(), entityHit.getEntity().getY(), entityHit.getEntity().getZ(), 0, 0, 0), entityHit.getEntity().position(), ENTITY_HIT_PARTICLE_RANGE, level.dimension());
                    bulletType.getHitEffects().forEach(effect -> living.addEffect(new MobEffectInstance(effect)));
                    // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                    living.invulnerableTime = living.hurtDuration / 2;
                }
            }

            if (bulletType.isSetEntitiesOnFire())
                entity.setSecondsOnFire(20);

            penetratingPower -= 1F;

            if (bullet != null)
                bullet.getPenetrationLosses().add(new PenetrationLoss(1F, PenetrationLoss.EnumType.ENTITY));

            ClientHooks.RENDER.spawnDebugDot(hit, 1000, 1F, 1F, 0F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof BlockHit bh && bh.getHitResult().getType() == HitResult.Type.BLOCK)
        {
            penetratingPower = handleBlockHit(level, bh.getHitResult(), shootingMotion, shot, penetratingPower, bullet);

            ClientHooks.RENDER.spawnDebugDot(hit, 1000, 0F, 1F, 0F);
        }

        if (penetratingPower <= 0F || (bulletType.isExplodeOnImpact()))
            penetratingPower = -1F;

        //Send hit marker, if player is present
        if (!level.isClientSide && showHitMarker && playerOwner.isPresent())
            PacketHandler.sendTo(new PacketHitMarker(lastHitHeadshot, lastHitPenAmount, false), playerOwner.get());

        return new HitData(penetratingPower, lastHitPenAmount, lastHitHeadshot);
    }

    private static float handleBlockHit(Level level, BlockHitResult hitResult, Vec3 shootingMotion, FiredShot shot, float penetratingPower, @Nullable Bullet bullet)
    {
        BlockPos pos = hitResult.getBlockPos();
        Vec3 hitVec = hitResult.getLocation();
        BlockState state = level.getBlockState(pos);

        // Block penetration (may consume power and let the bullet continue)
        if (ModCommonConfig.get().enableBlockPenetration())
        {
            float hardness = getBlockPenetrationDecrease(level, state, pos, shot.getBulletType());
            penetratingPower -= hardness;

            // No penetration
            if (penetratingPower < 0F)
                return penetratingPower;

            PenetrableBlock penetrableBlock = PenetrableBlock.get(state);

            if (penetrableBlock != null && penetrableBlock.breaksOnPenetration() && !level.isClientSide)
                ModUtils.destroyBlock((ServerLevel) level, pos, shot.getAttacker().orElse(null), false);

            if (bullet != null)
                bullet.getPenetrationLosses().add(new PenetrationLoss(hardness, PenetrationLoss.EnumType.BLOCK));
        }

        // Special handling: glass breaking
        handleGlassBreak(level, pos, state, shot);

        // Impact sound
        playImpactSound(level, pos, state, shot.getBulletType());

        //Particles
        spawnBlockHitParticles(level, hitResult, shootingMotion, shot.getBulletType(), bullet != null ? bullet.getBbWidth() : Shootable.DEFAULT_HITBOX_SIZE);

        // Bounce / ricochet or stop
        if (bullet != null)
            bullet.handleBounceOrStop(level, hitResult, hitVec);

        return penetratingPower;
    }

    private static void handleGlassBreak(Level level, BlockPos pos, BlockState state, FiredShot shot)
    {
        if (level.isClientSide || !shot.getBulletType().isBreaksGlass() || !ModUtils.isGlass(state) || !FlansMod.teamsManager.isCanBreakGlass())
            return;

        ModUtils.destroyBlock((ServerLevel) level, pos, shot.getAttacker().orElse(null), false);
    }

    private static void playImpactSound(Level level, BlockPos pos, BlockState state, BulletType type)
    {
        if (level.isClientSide || !type.isHitSoundEnable())
            return;

        String hitToUse = resolveImpactSound(state, state.getBlock(), type).orElse(null);
        if (hitToUse == null)
            return;

        PacketPlaySound.sendSoundPacket(pos.getCenter(), type.getHitSoundRange(), level.dimension(), hitToUse, true, null);
    }

    private static Optional<String> resolveImpactSound(BlockState state, Block block, BulletType type)
    {
        if (StringUtils.isNotBlank(type.getHitSound()))
            return Optional.of(type.getHitSound());

        // special-case certain blocks if you want
        if (block == Blocks.BRICKS)
            return Optional.of(FlansMod.SOUND_IMPACT_BRICKS);

        SoundType sound = state.getSoundType();

        // "dirt-ish" stuff
        if (sound == SoundType.GRAVEL || sound == SoundType.SAND || sound == SoundType.ROOTED_DIRT || sound == SoundType.MUD)
            return Optional.of(FlansMod.SOUND_IMPACT_DIRT);

        // glass / brittle
        if (sound == SoundType.GLASS || sound == SoundType.TUFF)
            return Optional.of(FlansMod.SOUND_IMPACT_GLASS);

        // metal-ish
        if (sound == SoundType.METAL || sound == SoundType.CHAIN || sound == SoundType.LANTERN || sound == SoundType.COPPER)
            return Optional.of(FlansMod.SOUND_IMPACT_METAL);

        // stone / rock
        if (sound == SoundType.STONE || sound == SoundType.DEEPSLATE || sound == SoundType.NETHER_BRICKS || sound == SoundType.NETHERRACK || sound == SoundType.BASALT)
            return Optional.of(FlansMod.SOUND_IMPACT_ROCK);

        // wood
        if (sound == SoundType.WOOD || sound == SoundType.NETHER_WOOD || sound == SoundType.SCAFFOLDING || sound == SoundType.LADDER)
            return Optional.of(FlansMod.SOUND_IMPACT_WOOD);

        return Optional.empty();
    }

    private static void spawnBlockHitParticles(Level level, BlockHitResult hitResult, Vec3 shootingMotion, BulletType type, float bbWidth)
    {
        if (level.isClientSide)
            return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.isAir())
            return;

        Vec3 hitVec = hitResult.getLocation();
        Direction direction = hitResult.getDirection();
        PacketHandler.sendToAllAround(new PacketBlockHitEffect(hitVec, shootingMotion, pos, direction, type.getExplosionRadius(), type.getBlockHitFXScale(), bbWidth), hitVec, BLOCK_HIT_PARTICLE_RANGER, level.dimension());
    }

    public static float getDamage(Entity entity, @Nullable Shootable shootable, @Nullable FiredShot firedShot)
    {
        ShootableType type = null;
        if (firedShot != null)
            type = firedShot.getBulletType();
        if (shootable != null)
            type = shootable.getConfigType();

        if (type == null)
            return 0F;

        if (shootable != null && type.getMass() > 0F)
        {
            return (float) (ModCommonConfig.get().newDamageSystemReference() * 0.001 * Math.sqrt(type.getMass()) * shootable.getDeltaMovement().length() * 20.0);
        }
        else
        {
            float baseDamage = type.getDamage().getDamageAgainstEntity(entity);
            if (shootable instanceof Grenade)
                return (float) (baseDamage * shootable.getDeltaMovement().lengthSqr() * 3.0);
            else if (shootable instanceof Bullet bullet && firedShot != null)
                return baseDamage * ShootingHelper.getDamageAffectedByPenetration(firedShot.getFireableGun().getDamage(), bullet.getConfigType(), bullet);
            else if (firedShot != null)
                return baseDamage * ShootingHelper.getDamageAffectedByPenetration(firedShot.getFireableGun().getDamage(), firedShot.getBulletType(), null);
            else
                return baseDamage;
        }
    }

    public static float getDamageAffectedByPenetration(float gunDamage, BulletType type, @Nullable Bullet bullet)
    {
        if (bullet == null || type.getPenetratingPower() <= 0F || (type.getPlayerPenetrationEffectOnDamage() == 0F && type.getEntityPenetrationEffectOnDamage() == 0F && type.getBlockPenetrationEffectOnDamage() == 0F && type.getPenetrationDecayEffectOnDamage() == 0F))
            return gunDamage;

        float totalPenetrationLostPercentage = 0F;

        for (PenetrationLoss penetrationLoss : bullet.getPenetrationLosses())
        {
            float effectOnDamage = penetrationLoss.type().getEffectOnDamage(type);
            float loss = penetrationLoss.loss();

            if (effectOnDamage <= 0 || effectOnDamage > 1 || loss <= 0)
                continue;

            float penetrationLostPercentage = (loss / type.getPenetratingPower());
            if (penetrationLostPercentage == 0)
                continue;

            totalPenetrationLostPercentage += (penetrationLostPercentage - penetrationLostPercentage * (1 - effectOnDamage));
        }

        return gunDamage * (1 - totalPenetrationLostPercentage);
    }

    public static void onDetonate(Level level, FiredShot firedShot, Vec3 detonatePos)
    {
        onDetonate(level, firedShot.getBulletType(), detonatePos, null, firedShot.getAttacker().orElse(null));
    }


    public static void onDetonate(Level level, ShootableType type, Vec3 position, @Nullable Shootable shootable, @Nullable LivingEntity causingEntity)
    {
        if (level.isClientSide)
            return;

        playDetonateSound(level, type, position);
        doExplosion(level, type, position, shootable, causingEntity);
        spreadFire(level, type, position, true);
        spawnExplosionParticles(level, type, position);
        dropItemsOnDetonate(level, type.getDropItemOnDetonate(), type.getContentPack(), position, shootable);
    }

    public static void onBulletDeath(Level level, BulletType type, Vec3 position, @Nullable Shootable shootable, @Nullable LivingEntity causingEntity)
    {
        if (level.isClientSide)
            return;

        doExplosion(level, type, position, shootable, causingEntity);
        spreadFire(level, type, position, false);
        spawnFlakParticles(level, type, position);
        dropItemsOnDetonate(level, type.getDropItemOnHit(), type.getContentPack(), position, shootable);
    }

    private static void playDetonateSound(Level level, ShootableType type, Vec3 position)
    {
        PacketPlaySound.sendSoundPacket(position, ModCommonConfig.get().explosionSoundRange(), level.dimension(), type.getDetonateSound(), true, null);
    }

    private static void doExplosion(Level level, ShootableType type, Vec3 position, @Nullable Entity explosive, @Nullable LivingEntity causingEntity)
    {
        if (type.getExplosionRadius() <= 0.1F)
            return;

        new FlanExplosion(level, explosive, causingEntity, type, position.x, position.y, position.z, false);

        // Despawn bullets (not grenades)
        if (explosive instanceof Bullet bullet)
            bullet.discard();
    }

    private static void spreadFire(Level level, ShootableType type, Vec3 position, boolean volumetric)
    {
        if (type.getFireRadius() <= 0.1F)
            return;

        float fireRadius = type.getFireRadius();
        for (float i = -fireRadius; i < fireRadius; i++)
        {
            for (float k = -fireRadius; k < fireRadius; k++)
            {

                if (volumetric)
                {
                    for (float j = -fireRadius; j < fireRadius; j++)
                    {
                        if (i * i + j * j + k * k > fireRadius * fireRadius)
                            continue;

                        BlockPos pos = BlockPos.containing(position.x + i, position.y + j, position.z + k);
                        if (level.isEmptyBlock(pos) && level.random.nextBoolean())
                            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
                else
                {
                    for (int j = -1; j < 1; j++)
                    {
                        BlockPos pos = BlockPos.containing(position.x + i, position.y + j, position.z + k);
                        if (level.isEmptyBlock(pos))
                            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void spawnExplosionParticles(Level level, ShootableType type, Vec3 position)
    {
        if (type.getExplodeParticles() > 0)
            PacketHandler.sendToAllAround(new PacketExplodeParticles(type.getExplodeParticleType(), type.getExplodeParticles(), position), position, ShootableType.EXPLODE_PARTICLES_RANGE, level.dimension());
    }

    private static void spawnFlakParticles(Level level, BulletType type, Vec3 position)
    {
        if (type.getFlak() > 0)
            PacketHandler.sendToAllAround(new PacketFlak(position, type.getFlak(), type.getFlakParticles()), position, BulletType.FLAK_PARTICLES_RANGE, level.dimension());
    }

    private static void dropItemsOnDetonate(Level level, String itemName, IContentProvider contentPack, Vec3 position, @Nullable Shootable shootable)
    {
        if (StringUtils.isBlank(itemName))
            return;

        ItemStack dropStack = InfoType.getRecipeElement(itemName, contentPack);
        if (dropStack != null && !dropStack.isEmpty())
        {
            if (shootable != null)
            {
                shootable.spawnAtLocation(dropStack, 1.0F);
            }
            else
            {
                ItemEntity entityitem = new ItemEntity(level, position.x, position.y, position.z, dropStack);
                entityitem.setDefaultPickUpDelay();
                level.addFreshEntity(entityitem);
            }
        }
    }

    private static void createShot(Level level, FiredShot shot, Vec3 shootingOrigin, Vec3 shootingDirection)
    {
        Vec3 shootingVector = calculateShootingMotionVector(level.random, shootingDirection, shot.getSpread(), 500F, shot.getFireableGun().getSpreadPattern());

        HitData hitData = new HitData(shot.getBulletType().getPenetratingPower(), 0F, false);
        List<BulletHit> hits = Raytracer.raytraceShot(level, null, shot.getAttacker().orElse(null), shot.getOwnerEntities(), shootingOrigin, shootingVector, 0, hitData.penetratingPower(), 0F, shot.getBulletType());
        Vec3 previousHitPos = shootingOrigin;
        Vec3 finalhit = null;

        for (int i = 0; i < hits.size(); i++)
        {
            BulletHit hit = hits.get(i);
            Vec3 shotVector = shootingVector.scale(hit.getIntersectTime());
            Vec3 hitPos = shootingOrigin.add(shotVector);

            if (hit instanceof BlockHit)
                ClientHooks.RENDER.spawnDebugDot(hitPos, 1000, 1F, 0F, 1F);
            else
                ClientHooks.RENDER.spawnDebugDot(hitPos, 1000);
            ClientHooks.RENDER.spawnDebugVector(previousHitPos, hitPos.subtract(previousHitPos), 1000, 1F, 1F, ((float) i / hits.size()));

            previousHitPos = hitPos;
            hitData = onHit(level, shot, hit, hitPos, shotVector, hitData, null);

            if (hitData.penetratingPower() <= 0F)
            {
                onDetonate(level, shot, hitPos);
                finalhit = hitPos;
                break;
            }
        }

        if (finalhit == null)
        {
            finalhit = shootingOrigin.add(shootingDirection);
        }

        PacketHandler.sendToAllAround(new PacketBulletTrail(shootingOrigin, finalhit, 0.05F, 10F, 10F, shot.getBulletType().getTrailTexture()), shootingOrigin.x, shootingOrigin.y, shootingOrigin.z, 500F, level.dimension());
    }

    public static Vec3 calculateShootingMotionVector(RandomSource random, Vec3 direction, float spread, float speed, EnumSpreadPattern pattern)
    {
        double angularSpread = ANGULAR_SPREAD_FACTOR * spread;

        // Make sure direction is sane
        if (direction.lengthSqr() == 0.0D)
            return direction;
        else
            direction = direction.normalize();

        // Build a stable local basis (xAxis = "right", yAxis = "up" relative to forward)
        Vec3 worldUp = Math.abs(direction.y) < 0.999D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 xAxis = direction.cross(worldUp).normalize();
        Vec3 yAxis = xAxis.cross(direction).normalize();

        Vec3 perturbedDir = direction;

        switch (pattern)
        {
            case CIRCLE ->
            {
                double x = Mth.clamp(random.nextGaussian(), -3.0, 3.0) * angularSpread;
                double y = Mth.clamp(random.nextGaussian(), -3.0, 3.0) * angularSpread;

                Vec3 offset = xAxis.scale(x).add(yAxis.scale(y));
                perturbedDir = direction.add(offset);
            }
            case CUBE ->
            {
                double x = random.nextGaussian() * angularSpread;
                double y = random.nextGaussian() * angularSpread;

                Vec3 offset = xAxis.scale(x).add(yAxis.scale(y));
                perturbedDir = direction.add(offset);
            }
            case HORIZONTAL ->
            {
                double x = (random.nextDouble() - random.nextDouble()) * angularSpread;

                Vec3 offset = xAxis.scale(x);
                perturbedDir = direction.add(offset);
            }
            case VERTICAL ->
            {
                double y = (random.nextDouble() - random.nextDouble()) * angularSpread;

                Vec3 offset = yAxis.scale(y);
                perturbedDir = direction.add(offset);
            }
            case TRIANGLE ->
            {
                double x = (random.nextDouble() - random.nextDouble()) * angularSpread;
                double y = (random.nextDouble() - random.nextDouble()) * angularSpread;

                Vec3 offset = xAxis.scale(x).add(yAxis.scale(y));
                perturbedDir = direction.add(offset);
            }
        }

        return perturbedDir.normalize().scale(speed);
    }

    public static float getBlockPenetrationDecrease(Level level, BlockState blockstate, BlockPos pos, BulletType type)
    {
        float penetrationModifier = (type.getBlockPenetrationModifier() > 0F ? (1F / type.getBlockPenetrationModifier()) : 1F);
        PenetrableBlock penetrableBlock = PenetrableBlock.get(blockstate);
        float hardness = ((penetrableBlock != null) ? (float) penetrableBlock.hardness() : blockstate.getDestroySpeed(level, pos));
        return 2F * hardness * penetrationModifier;
    }
}
