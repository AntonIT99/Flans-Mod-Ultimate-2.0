package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.common.FlansExplosion;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.ShootableFactory;
import com.flansmodultimate.common.raytracing.BlockHit;
import com.flansmodultimate.common.raytracing.BulletHit;
import com.flansmodultimate.common.raytracing.DriveableHit;
import com.flansmodultimate.common.raytracing.EntityHit;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.raytracing.PlayerBulletHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketParticles;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Class containing a bunch of shooting related functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootingHelper
{
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
    public static void fireGun(Level level, @NotNull LivingEntity shooter, @NotNull GunType gunType, @NotNull ShootableType shootableType, @NotNull ItemStack gunStack, @Nullable ItemStack otherHandStack, ShootingHandler handler)
    {
        int bulletAmount = gunType.getNumBullets() * shootableType.getNumBullets();

        if (gunType.getBulletSpeed(gunStack) == 0F && shootableType instanceof BulletType bulletType)
        {
            // Raytrace without entity
            createMultipleShots(level, new FiredShot(gunType, bulletType, gunStack, otherHandStack, shooter), bulletAmount, shooter.getEyePosition(0.0F), shooter.getLookAngle(), handler);
        }
        else
        {
            // Spawn shootable entities
            Shootable shootable = ShootableFactory.createShootable(level, gunType, shootableType, shooter, gunStack, otherHandStack);

            for (int i = 0; i < bulletAmount; i++)
            {

                level.addFreshEntity(shootable);
                handler.shooting(i < bulletAmount - 1);
            }
        }
    }

    public static float onHit(Level level, FiredShot shot, BulletHit bulletHit, Vec3 hit, Vec3 shootingDirection, float penetratingPower)
    {
        return onHit(level, shot, bulletHit, hit, shootingDirection, penetratingPower, null);
    }

    public static float onHit(Level level, FiredShot shot, BulletHit bulletHit, Vec3 hit, Vec3 shootingDirection, float penetratingPower, @Nullable Bullet bullet)
    {
        float damage = shot.getFireableGun().getDamage();
        BulletType bulletType = shot.getBulletType();
        Optional<ServerPlayer> player = shot.getPlayerAttacker();

        if (bulletHit instanceof DriveableHit driveableHit)
        {
            penetratingPower = driveableHit.driveable.bulletHit(bulletType, shot.getFireableGun().getDamage(), driveableHit, penetratingPower);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 0F, 1F);

            //Send hit marker, if player is present
            player.ifPresent(p -> PacketHandler.sendTo(new PacketHitMarker(), p));
        }
        else if (bulletHit instanceof PlayerBulletHit playerHit)
        {
            penetratingPower = playerHit.hitbox.hitByBullet(shot, damage, penetratingPower, bullet);

            DebugHelper.spawnDebugDot(level, hit, 1000, 1F, 0F, 0F);

            // Check teams
            if (player.isPresent())
            {
                //TODO: Teams
                /*TeamsRound round;
                if (TeamsManager.getInstance() != null && (round = TeamsManager.getInstance().currentRound) != null)
                {
                    Optional<Team> shooterTeam = round.getTeam(player);
                    Optional<Team> victimTeam = round.getTeam(playerHit.hitbox.player);

                    if (!shooterTeam.isPresent() || !victimTeam.isPresent() || !shooterTeam.get().equals(victimTeam.get()))
                    {
                        FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
                    }
                }
                else // No teams mod, just add marker
                {
                    FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
                }*/
                //TODO: check arguments in FMU
                PacketHandler.sendTo(new PacketHitMarker(), player.get());
            }
        }
        else if (bulletHit instanceof EntityHit entityHit)
        {
            if (entityHit.getEntity() != null)
            {
                if (entityHit.getEntity().hurt(shot.getDamageSource(level, null), damage * bulletType.getDamage().getDamageAgainstEntity(entityHit.getEntity())) && entityHit.getEntity() instanceof LivingEntity living)
                {
                    //TODO: Check origin code
                    bulletType.getHitEffects().forEach(effect -> living.addEffect(new MobEffectInstance(effect)));
                    // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                    living.invulnerableTime = 0;
                }
                if (bulletType.isSetEntitiesOnFire())
                    entityHit.getEntity().setSecondsOnFire(20);
                penetratingPower -= 1F;

                DebugHelper.spawnDebugDot(level, hit, 1000, 1F, 1F, 0F);
            }

            //Send hit marker, if player is present
            shot.getPlayerAttacker().ifPresent(p -> PacketHandler.sendTo(new PacketHitMarker(), p));
        }
        else if (bulletHit instanceof BlockHit bh && bh.getHitResult().getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult bhr = (BlockHitResult) bh.getHitResult();
            BlockPos pos = bhr.getBlockPos();
            Direction direction = bhr.getDirection();

            // State is already “actual” in modern MC (no getActualState)
            BlockState state = level.getBlockState(pos);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 1F, 0F);

            //If the bullet breaks glass, and can do so according to FlansMod, do so.
            if (bulletType.isBreaksGlass() && ModUtils.isGlass(state) && FlansMod.teamsManager.isCanBreakGlass() && !level.isClientSide)
            {
                ModUtils.destroyBlock((ServerLevel) level, pos, shot.getAttacker().orElse(null), false);
            }

            penetratingPower -= getBlockPenetrationDecrease(state, pos, level);

            Vector3f bulletDir = new Vector3f(shootingDirection);
            bulletDir.normalise();
            bulletDir.scale(0.5F);

            for (Player p : level.players())
            {
                //Checks if the player is in a radius of 300 Blocks (300 squared = 90000)
                if (p.distanceToSqr(Vec3.atCenterOf(pos)) < 90000)
                {
                    PacketHandler.sendTo(new PacketBlockHitEffect(new Vector3f(hit), bulletDir, pos, direction), (ServerPlayer) p);
                }
            }

            //play sound when bullet hits block
            PacketPlaySound.sendSoundPacket(hit.x, hit.y, hit.z, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), false);
        }

        if (penetratingPower <= 0F || (bulletType.isExplodeOnImpact()))
        {
            return -1F;
        }
        return penetratingPower;
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
            PacketHandler.sendToAllAround(new PacketParticles(type.getExplodeParticleType(), type.getExplodeParticles(), position), position.x, position.y, position.z, ShootableType.EXPLODE_PARTICLES_RANGE, level.dimension());
    }

    private static void spawnFlakParticles(Level level, BulletType type, Vec3 position)
    {
        if (type.getFlak() > 0)
            PacketHandler.sendToAllAround(new PacketFlak(position.x, position.y, position.z, type.getFlak(), type.getFlakParticles()), position.x, position.y, position.z, BulletType.FLAK_PARTICLES_RANGE, level.dimension());
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

        float penetrationPower = shot.getBulletType().getPenetratingPower();

        List<BulletHit> hits = FlansModRaytracer.raytraceShot(level, null, shot.getOwnerEntities(), rayTraceOrigin, shootingDirection, 0, penetrationPower, 0F);
        Vec3 previousHitPos = rayTraceOrigin;
        Vec3 finalhit = null;

        for (int i = 0; i < hits.size(); i++)
        {
            BulletHit hit = hits.get(i);
            Vec3 shotVector = shootingDirection.scale(hit.intersectTime);
            Vec3 hitPos = rayTraceOrigin.add(shotVector);

            if (hit instanceof BlockHit)
                DebugHelper.spawnDebugDot(level, hitPos, 1000, 1F, 0f, 1F);
            else
                DebugHelper.spawnDebugDot(level, hitPos, 1000);
            DebugHelper.spawnDebugVector(level, previousHitPos, hitPos.subtract(previousHitPos), 1000, 1F, 1F, ((float) i / hits.size()));

            previousHitPos = hitPos;

            penetrationPower = onHit(level, shot, hit, hitPos, shootingDirection, penetrationPower);
            if (penetrationPower <= 0f)
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
        //Animation
        //TODO should this be send to all Players?
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

    public static float getBlockPenetrationDecrease(BlockState blockstate, BlockPos pos, Level level)
    {
        float hardness = blockstate.getDestroySpeed(level, pos) * 2;
        if (hardness < 0)
            return 1000; // Some high value for invincible blocks
        else
            return Math.max(hardness, 1);
    }
}
