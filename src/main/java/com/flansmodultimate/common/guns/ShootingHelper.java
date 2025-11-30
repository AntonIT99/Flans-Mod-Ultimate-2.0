package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.PenetrableBlock;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.common.FlansExplosion;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.driveables.Seat;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.ShootableFactory;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.raytracing.hits.BlockHit;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketParticles;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    //TODO: make configurable
    private static final double ENTITY_HIT_PARTICLE_RANGE = 64.0;
    private static final double BLOCK_HIT_PARTICLE_RANGER = 64.0;

    private static final RandomSource random = RandomSource.create();

    /** Call this when fire bullets from vehicles or other sources (Server side) */
    public static void fireGun(Level level, @NotNull FiredShot firedShot, int bulletAmount, Vec3 rayTraceOrigin, Vec3 shootingDirection, ShootingHandler handler)
    {
        if (firedShot.getFireableGun().getBulletSpeed() == 0F && firedShot.getBulletType() instanceof BulletType)
        {
            // Raytrace without entity
            createMultipleShots(level, firedShot, bulletAmount, rayTraceOrigin, shootingDirection, handler);
        }
        else
        {
            // Spawn shootable entities
            Bullet bullet = new Bullet(level, firedShot, rayTraceOrigin, shootingDirection);

            for (int i = 0; i < bulletAmount; i++)
            {
                level.addFreshEntity(bullet);
                handler.shooting(i < bulletAmount - 1);
            }
        }
    }

    /** Call this to fire bullets or grenades from a living entity holding a gun (Server side) */
    public static void fireGun(Level level, @NotNull LivingEntity shooter, @NotNull GunType gunType, @NotNull ShootableType shootableType, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack, ShootingHandler handler)
    {
        int bulletAmount = gunType.getNumBullets() * shootableType.getNumBullets();

        if (gunType.getBulletSpeed(gunStack, shootableStack) == 0F && shootableType instanceof BulletType bulletType)
        {
            // Raytrace without entity
            createMultipleShots(level, new FiredShot(gunType, bulletType, gunStack, shootableStack, otherHandStack, shooter), bulletAmount, shooter.getEyePosition(0.0F), shooter.getLookAngle(), handler);
        }
        else
        {
            // Spawn shootable entities
            Shootable shootable = ShootableFactory.createShootable(level, gunType, shootableType, shooter, gunStack, shootableStack, otherHandStack);

            for (int i = 0; i < bulletAmount; i++)
            {

                level.addFreshEntity(shootable);
                handler.shooting(i < bulletAmount - 1);
            }
        }
    }

    public record HitData(float penetratingPower, float lastHitPenAmount, boolean lastHitHeadshot) {}

    public static HitData onHit(Level level, FiredShot shot, BulletHit bulletHit, Vec3 hit, Vec3 shootingMotion, HitData hitData)
    {
        return onHit(level, shot, bulletHit, hit, shootingMotion, hitData, null);
    }

    public static HitData onHit(Level level, FiredShot shot, BulletHit bulletHit, Vec3 hit, Vec3 shootingMotion, HitData hitData, @Nullable Bullet bullet)
    {
        float penetratingPower = hitData.penetratingPower();
        float lastHitPenAmount = hitData.lastHitPenAmount();
        boolean lastHitHeadshot = hitData.lastHitHeadshot();
        float damage = shot.getFireableGun().getDamage();
        boolean showHitMarker = false;

        BulletType bulletType = shot.getBulletType();
        Optional<ServerPlayer> playerOwner = shot.getPlayerAttacker();
        LivingEntity owner = shot.getAttacker().orElse(null);

        if (bulletHit instanceof DriveableHit driveableHit)
        {
            if (bulletType.isEntityHitSoundEnable())
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true);

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
                HitData driveableHitData = driveableHit.getDriveable().bulletHit(bulletType, damage, driveableHit, hitData);
                penetratingPower = driveableHitData.penetratingPower();
                lastHitPenAmount = driveableHitData.lastHitPenAmount();
                lastHitHeadshot = driveableHitData.lastHitHeadshot();
            }

            if (bulletType.isCanSpotEntityDriveable())
                driveableHit.getDriveable().setEntityMarker(200);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 0F, 1F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof PlayerBulletHit playerHit)
        {
            if (bulletType.isEntityHitSoundEnable())
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true);

            float prevPenetratingPower = penetratingPower;
            HitData playerHitData = playerHit.getHitbox().hitByBullet(shot, damage, hitData, bullet);
            penetratingPower = playerHitData.penetratingPower();
            lastHitPenAmount = playerHitData.lastHitPenAmount();
            lastHitHeadshot = playerHitData.lastHitHeadshot();

            if (bullet != null)
                bullet.getPenetrationLosses().add(new PenetrationLoss((prevPenetratingPower - penetratingPower), PenetrationLoss.Type.PLAYER));

            DebugHelper.spawnDebugDot(level, hit, 1000, 1F, 0F, 0F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof EntityHit entityHit && entityHit.getEntity() != null)
        {
            Entity entity = entityHit.getEntity();

            if (bulletType.isEntityHitSoundEnable())
                PacketPlaySound.sendSoundPacket(hit, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), true);

            if (owner instanceof Player)
                lastHitPenAmount = 1F;

            damage = getDamageAffectedByPenetration(damage, bulletType, bullet) * bulletType.getDamage().getDamageAgainstEntity(entity);

            if (entity.hurt(shot.getDamageSource(level, bullet), damage) && entity instanceof LivingEntity living)
            {
                PacketHandler.sendToAllAround(new PacketParticle(ParticleHelper.RED_DUST, entityHit.getEntity().getX(), entityHit.getEntity().getY(), entityHit.getEntity().getZ(), 0, 0, 0), entityHit.getEntity().position(), ENTITY_HIT_PARTICLE_RANGE, level.dimension());
                bulletType.getHitEffects().forEach(effect -> living.addEffect(new MobEffectInstance(effect)));
                // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                living.invulnerableTime = 0;
            }

            if (bulletType.isSetEntitiesOnFire())
                entity.setSecondsOnFire(20);

            penetratingPower -= 1F;

            if (bullet != null)
                bullet.getPenetrationLosses().add(new PenetrationLoss(1F, PenetrationLoss.Type.ENTITY));

            DebugHelper.spawnDebugDot(level, hit, 1000, 1F, 1F, 0F);
            showHitMarker = true;
        }
        else if (bulletHit instanceof BlockHit bh && bh.getHitResult().getType() == HitResult.Type.BLOCK)
        {
            penetratingPower = handleBlockHit(level, bh.getHitResult(), shootingMotion, shot, penetratingPower, bullet);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 1F, 0F);
        }

        if (penetratingPower <= 0F || (bulletType.isExplodeOnImpact()))
            penetratingPower = -1F;

        //Send hit marker, if player is present
        if (showHitMarker && playerOwner.isPresent())
            PacketHandler.sendTo(new PacketHitMarker(lastHitHeadshot, lastHitPenAmount, false), playerOwner.get());

        return new HitData(penetratingPower, lastHitPenAmount, lastHitHeadshot);
    }

    private static float handleBlockHit(Level level, BlockHitResult hitResult, Vec3 shootingMotion, FiredShot shot, float penetratingPower, @Nullable Bullet bullet)
    {
        BlockPos pos = hitResult.getBlockPos();
        Vec3 hitVec = hitResult.getLocation();
        BlockState state = level.getBlockState(pos);

        // Block penetration (may consume power and let the bullet continue)
        if (BooleanUtils.isTrue(ModCommonConfigs.enableBlockPenetration.get()))
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
                bullet.getPenetrationLosses().add(new PenetrationLoss(hardness, PenetrationLoss.Type.BLOCK));
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

    private static  void handleGlassBreak(Level level, BlockPos pos, BlockState state, FiredShot shot)
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

        PacketPlaySound.sendSoundPacket(pos.getCenter(), type.getHitSoundRange(), level.dimension(), hitToUse, true);
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
        PacketPlaySound.sendSoundPacket(position, FlansMod.SOUND_RANGE, level.dimension(), type.getDetonateSound(), true);
    }

    private static void doExplosion(Level level, ShootableType type, Vec3 position, @Nullable Entity explosive, @Nullable LivingEntity causingEntity)
    {
        if (type.getExplosionRadius() <= 0.1F)
            return;

        new FlansExplosion(level, explosive, causingEntity, type, position.x, position.y, position.z, false);

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
                        if (level.isEmptyBlock(pos) && random.nextBoolean())
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
            PacketHandler.sendToAllAround(new PacketParticles(type.getExplodeParticleType(), type.getExplodeParticles(), position), position, ShootableType.EXPLODE_PARTICLES_RANGE, level.dimension());
    }

    private static void spawnFlakParticles(Level level, BulletType type, Vec3 position)
    {
        if (type.getFlak() > 0)
            PacketHandler.sendToAllAround(new PacketFlak(position.x, position.y, position.z, type.getFlak(), type.getFlakParticles()), position, BulletType.FLAK_PARTICLES_RANGE, level.dimension());
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

    private static void createMultipleShots(Level level, FiredShot shot, Integer bulletAmount, Vec3 rayTraceOrigin, Vec3 shootingDirection, ShootingHandler handler)
    {
        float bulletspread = 0.0025F * shot.getFireableGun().getSpread() * shot.getBulletType().getBulletSpread();
        for (int i = 0; i < bulletAmount; i++)
        {
            createShot(level, shot, bulletspread, rayTraceOrigin, shootingDirection);
            handler.shooting(i < bulletAmount - 1);
        }
    }

    private static void createShot(Level level, FiredShot shot, float bulletspread, Vec3 rayTraceOrigin, Vec3 shootingDirection)
    {
        shootingDirection = randomizeVectorDirection(level, shootingDirection, bulletspread, shot.getFireableGun().getSpreadPattern());
        shootingDirection.scale(500F);

        HitData hitData = new HitData(shot.getBulletType().getPenetratingPower(), 0F, false);
        List<BulletHit> hits = FlansModRaytracer.raytraceShot(level, null, shot.getAttacker().orElse(null), shot.getOwnerEntities(), rayTraceOrigin, shootingDirection, 0, hitData.penetratingPower(), 0F, shot.getBulletType());
        Vec3 previousHitPos = rayTraceOrigin;
        Vec3 finalhit = null;

        for (int i = 0; i < hits.size(); i++)
        {
            BulletHit hit = hits.get(i);
            Vec3 shotVector = shootingDirection.scale(hit.getIntersectTime());
            Vec3 hitPos = rayTraceOrigin.add(shotVector);

            if (hit instanceof BlockHit)
                DebugHelper.spawnDebugDot(level, hitPos, 1000, 1F, 0F, 1F);
            else
                DebugHelper.spawnDebugDot(level, hitPos, 1000);
            DebugHelper.spawnDebugVector(level, previousHitPos, hitPos.subtract(previousHitPos), 1000, 1F, 1F, ((float) i / hits.size()));

            previousHitPos = hitPos;
            hitData = onHit(level, shot, hit, hitPos, shotVector, hitData);

            if (hitData.penetratingPower() <= 0F)
            {
                onDetonate(level, shot, hitPos);
                finalhit = hitPos;
                break;
            }
        }

        if (finalhit == null)
        {
            finalhit = rayTraceOrigin.add(shootingDirection);
        }

        PacketHandler.sendToAllAround(new PacketBulletTrail(new Vector3f(rayTraceOrigin), new Vector3f(finalhit), 0.05F, 10F, 10F, shot.getBulletType().getTrailTexture()), rayTraceOrigin.x, rayTraceOrigin.y, rayTraceOrigin.z, 500F, level.dimension());
    }

    private static Vec3 randomizeVectorDirection(Level level, Vec3 vector, float spread, EnumSpreadPattern pattern)
    {
        Vector3f result = new Vector3f(vector);
        Vector3f xAxis = Vector3f.cross(result, new Vector3f(0f, 1f, 0f), null);
        xAxis.normalise();
        Vector3f yAxis = Vector3f.cross(result, xAxis, null);
        yAxis.normalise();

        switch (pattern)
        {
            case CIRCLE:
            {
                float theta = (float)(level.random.nextDouble() * Math.PI * 2.0f);
                float radius = (float)level.random.nextDouble() * spread;
                float xComponent = radius * (float)Math.sin(theta);
                float yComponent = radius * (float)Math.cos(theta);

                xAxis.scale(xComponent);
                yAxis.scale(yComponent);

                Vector3f.add(result, xAxis, result);
                Vector3f.add(result, yAxis, result);

                break;
            }
            case CUBE:
            {
                result.x += (float)level.random.nextGaussian() * spread;
                result.y += (float)level.random.nextGaussian() * spread;
                result.z += (float)level.random.nextGaussian() * spread;
                break;
            }
            case HORIZONTAL:
            {
                float xComponent = spread * (level.random.nextFloat() * 2f - 1f);

                xAxis.scale(xComponent);

                Vector3f.add(result, xAxis, result);

                break;
            }
            case VERTICAL:
            {
                float yComponent = spread * (level.random.nextFloat() * 2f - 1f);

                yAxis.scale(yComponent);

                Vector3f.add(result, yAxis, result);

                break;
            }
            case TRIANGLE:
            {
                // Random square, then fold the corners
                float xComponent = level.random.nextFloat() * 2f - 1f;
                float yComponent = level.random.nextFloat() * 2f - 1f;

                if(xComponent > 0f)
                {
                    if(yComponent > 1.0f - xComponent * 2f)
                    {
                        yComponent = -yComponent;
                        xComponent = 1f - xComponent;
                    }
                }
                else
                {
                    if(yComponent > xComponent * 2f + 1f)
                    {
                        yComponent = -yComponent;
                        xComponent = -1f - xComponent;
                    }
                }

                xComponent *= spread;
                yComponent *= spread;

                xAxis.scale(xComponent);
                yAxis.scale(yComponent);

                Vector3f.add(result, xAxis, result);
                Vector3f.add(result, yAxis, result);

                break;
            }
            default:
                break;
        }
        return result.toVec3();
    }

    public static float getBlockPenetrationDecrease(Level level, BlockState blockstate, BlockPos pos, BulletType type)
    {
        float penetrationModifier = (type.getBlockPenetrationModifier() > 0F ? (1F / type.getBlockPenetrationModifier()) : 1F);
        PenetrableBlock penetrableBlock = PenetrableBlock.get(blockstate);
        float hardness = ((penetrableBlock != null) ? (float) penetrableBlock.hardness() : blockstate.getDestroySpeed(level, pos));
        return 2F * hardness * penetrationModifier;
    }
}
