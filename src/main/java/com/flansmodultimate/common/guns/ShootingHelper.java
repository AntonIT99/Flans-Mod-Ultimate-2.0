package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.render.ParticleHelper;
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
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketBlockHitEffect;
import com.flansmodultimate.network.PacketBulletTrail;
import com.flansmodultimate.network.PacketFlak;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.PacketHitMarker;
import com.flansmodultimate.network.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
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

    //TODO: Refactor this depending on usage
    public static void fireGun(Level level, @NotNull ShootableType shootableType, @Nullable FiredShot firedShot, int bulletAmount, Vec3 rayTraceOrigin, Vec3 shootingDirection, ShootingHandler handler)
    {
        if (firedShot != null && firedShot.getFireableGun().getBulletSpeed() == 0F && shootableType instanceof BulletType)
        {
            // Raytrace
            createMultipleShots(level, firedShot, bulletAmount, new Vector3f(rayTraceOrigin), new Vector3f(shootingDirection), handler);
        }
        else
        {
            // Spawn shootable entities
            Shootable shootable = ShootableFactory.createShootable(level, shootableType, rayTraceOrigin, shootingDirection, firedShot);

            for (int i = 0; i < bulletAmount; i++)
            {
                level.addFreshEntity(shootable);
                handler.shooting(i < bulletAmount - 1);
            }
        }
    }

    public static void fireGun(Level level, @NotNull LivingEntity shooter, @NotNull GunType gunType, @NotNull ShootableType shootableType, @NotNull ItemStack gunStack, @Nullable ItemStack otherHandStack, ShootingHandler handler)
    {
        int bulletAmount = gunType.getNumBullets() * shootableType.getNumBullets();

        if (gunType.getBulletSpeed(gunStack) == 0F && shootableType instanceof BulletType bulletType)
        {
            // Raytrace
            createMultipleShots(level, new FiredShot(gunType, bulletType, gunStack, otherHandStack), bulletAmount, new Vector3f(shooter.getEyePosition(0.0F)), new Vector3f(shooter.getLookAngle()), handler);
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

    private static void createMultipleShots(Level level, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection, ShootingHandler handler)
    {
        float bulletspread = 0.0025F * shot.getFireableGun().getSpread() * shot.getBulletType().getBulletSpread();
        for (int i = 0; i < bulletAmount; i++)
        {
            createShot(level, shot, bulletspread, rayTraceOrigin, new Vector3f(shootingDirection));
            handler.shooting(i < bulletAmount - 1);
        }
    }

    private static void createShot(Level level, FiredShot shot, float bulletspread, Vector3f rayTraceOrigin, Vector3f shootingDirection)
    {
        randomizeVectorDirection(level, shootingDirection, bulletspread, shot.getFireableGun().getSpreadPattern());
        shootingDirection.scale(500.0f);

        float penetrationPower = shot.getBulletType().getPenetratingPower();
        //first tries to get the player because the players vehicle is also ignored, or get the player independent shooter or null
        Optional<ServerPlayer> playerOptional = shot.getPlayerAttacker();
        Entity ignore = playerOptional.isPresent() ? playerOptional.get() : shot.getCausingEntity().orElse(null);

        List<BulletHit> hits = FlansModRaytracer.raytrace(level, ignore, false, null, rayTraceOrigin, shootingDirection, 0, penetrationPower, 0F);
        Vector3f previousHitPos = rayTraceOrigin;
        Vector3f finalhit = null;

        for (int i = 0; i < hits.size(); i++)
        {
            BulletHit hit = hits.get(i);
            Vector3f shotVector = new Vector3f(shootingDirection).scale(hit.intersectTime);
            Vector3f hitPos = Vector3f.add(rayTraceOrigin, shotVector, null);

            if (hit instanceof BlockHit)
                DebugHelper.spawnDebugDot(level, hitPos, 1000, 1F, 0f, 1F);
            else
                DebugHelper.spawnDebugDot(level, hitPos, 1000);
            DebugHelper.spawnDebugVector(level, previousHitPos, Vector3f.sub(hitPos, previousHitPos, null), 1000, 1F, 1F, ((float) i / hits.size()));

            previousHitPos = hitPos;

            penetrationPower = onHit(level, hitPos, shootingDirection, shot, hit, penetrationPower, null);
            if (penetrationPower <= 0f)
            {
                onDetonate(level, shot, hitPos.toVec3(), null);
                finalhit = hitPos;
                break;
            }
        }

        if (finalhit == null)
        {
            finalhit = Vector3f.add(rayTraceOrigin, shootingDirection, null);
        }
        //Animation
        //TODO should this be send to all Players?
        PacketHandler.sendToAllAround(new PacketBulletTrail(rayTraceOrigin, finalhit, 0.05F, 10F, 10F, shot.getBulletType().getTrailTexture()), rayTraceOrigin.x, rayTraceOrigin.y, rayTraceOrigin.z, 500F, level.dimension());
    }

    /**
     * @param level             World which contains the location of the hit
     * @param hit               The location of the hit
     * @param shootingDirection The direction the shot was fired
     * @param shot              The FiredShot instance of the shot
     * @param bulletHit         BulletHit if the object hit
     * @param penetratingPower  Power of the bullet to penetrate objects
     * @param bullet            The bullet entity
     * @return The remaining penetrationPower after hitting the object specified in the BulletHit
     */
    public static float onHit(Level level, Vector3f hit, Vector3f shootingDirection, FiredShot shot, BulletHit bulletHit, float penetratingPower, @Nullable Bullet bullet)
    {
        float damage = shot.getFireableGun().getDamage();

        BulletType bulletType = shot.getBulletType();
        if (bulletHit instanceof DriveableHit driveableHit)
        {
            penetratingPower = driveableHit.driveable.bulletHit(bulletType, shot.getFireableGun().getDamageAgainstVehicles(), driveableHit, penetratingPower);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 0F, 1F);

            //Send hit marker, if player is present
            shot.getPlayerAttacker().ifPresent((ServerPlayer player) -> PacketHandler.sendTo(new PacketHitMarker(), player));
        }
        else if (bulletHit instanceof PlayerBulletHit playerHit)
        {
            penetratingPower = playerHit.hitbox.hitByBullet(shot, damage, penetratingPower, bullet);

            DebugHelper.spawnDebugDot(level, hit, 1000, 1F, 0F, 0F);

            Optional<ServerPlayer> optionalPlayer = shot.getPlayerAttacker();

            // Check teams
            if (optionalPlayer.isPresent())
            {
                ServerPlayer player = optionalPlayer.get();

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
                PacketHandler.sendTo(new PacketHitMarker(), player);
            }
        }
        else if (bulletHit instanceof EntityHit entityHit)
        {
            if (entityHit.getEntity() != null)
            {
                if (entityHit.getEntity().hurt(shot.getDamageSource(level, null), damage * bulletType.getDamage().getDamageValue(entityHit.getEntity())) && entityHit.getEntity() instanceof LivingEntity living)
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
            Vec3 impact = bhr.getLocation();

            // State is already “actual” in modern MC (no getActualState)
            BlockState state = level.getBlockState(pos);

            DebugHelper.spawnDebugDot(level, hit, 1000, 0F, 1F, 0F);

            //If the bullet breaks glass, and can do so according to FlansMod, do so.
            if (bulletType.isBreaksGlass() && ModUtils.isGlass(state) && TeamsManager.isCanBreakGlass() && !level.isClientSide)
            {
                ModUtils.destroyBlock((ServerLevel) level, pos, shot.getAttacker().orElse(null), false);
            }

            penetratingPower -= getBlockPenetrationDecrease(state, pos, level);

            Vector3f bulletDir = new Vector3f(shootingDirection);
            bulletDir.normalise();
            bulletDir.scale(0.5f);

            for (Player player : level.players())
            {
                //Checks if the player is in a radius of 300 Blocks (300 squared = 90000)
                if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 90000)
                {
                    PacketHandler.sendTo(new PacketBlockHitEffect(hit, bulletDir, pos, direction), (ServerPlayer) player);
                }
            }

            //play sound when bullet hits block
            PacketPlaySound.sendSoundPacket(hit.x, hit.y, hit.z, bulletType.getHitSoundRange(), level.dimension(), bulletType.getHitSound(), false);
        }

        if (penetratingPower <= 0F || (bulletType.isExplodeOnImpact()))
        {
            return -1f;
        }
        return penetratingPower;
    }

    public static void onDetonate(Level level, FiredShot shot, Vec3 detonatePos, @Nullable Bullet bullet)
    {
        BulletType bulletType = shot.getBulletType();

        // Play detonate sound
        PacketPlaySound.sendSoundPacket(detonatePos.x, detonatePos.y, detonatePos.z, FlansMod.SOUND_RANGE, level.dimension(), bulletType.getDetonateSound(), true);

        // Explode
        if (!level.isClientSide && bulletType.getExplosionRadius() > 0.1F)
        {
            new FlansExplosion(level, bullet, shot.getAttacker().orElse(null), bulletType, detonatePos.x, detonatePos.y, detonatePos.z, bulletType.getFlak() > 0, false);
        }

        // Make fire
        if (!level.isClientSide && bulletType.getFireRadius() > 0.1F)
        {
            for (float i = -bulletType.getFireRadius(); i < bulletType.getFireRadius(); i++)
            {
                for (float k = -bulletType.getFireRadius(); k < bulletType.getFireRadius(); k++)
                {
                    for (int j = -1; j < 1; j++)
                    {
                        BlockPos pos = new BlockPos(Mth.floor(detonatePos.x + i), Mth.floor(detonatePos.y + j), Mth.floor(detonatePos.z + k));
                        if (level.isEmptyBlock(pos))
                            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        }

        // Make explosion particles
        if (level.isClientSide)
        {
            ParticleHelper.spawnFromString((ClientLevel) level, bulletType.getExplodeParticleType(), detonatePos.x, detonatePos.y, detonatePos.z, random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        }


        // Send flak packet
        if (bulletType.getFlak() > 0)
        {
            PacketHandler.sendToAllAround(new PacketFlak(detonatePos.x, detonatePos.y, detonatePos.z, bulletType.getFlak(), bulletType.getFlakParticles()), detonatePos.x, detonatePos.y, detonatePos.z, BulletType.FLAK_PARTICLES_RANGE, level.dimension());
        }

        // Drop item on hitting if bullet requires it
        if (StringUtils.isNotBlank(bulletType.getDropItemOnHit()))
        {
            ItemStack dropStack = InfoType.getRecipeElement(bulletType.getDropItemOnHit(), bulletType.getContentPack());

            if (dropStack != null && !dropStack.isEmpty())
            {
                ItemEntity entityitem = new ItemEntity(level, detonatePos.x, detonatePos.y, detonatePos.z, dropStack);
                entityitem.setDefaultPickUpDelay();
                level.addFreshEntity(entityitem);
            }
        }

    }

    public static void randomizeVectorDirection(Level level, Vector3f vector, Float spread, EnumSpreadPattern pattern)
    {
        Vector3f xAxis = Vector3f.cross(vector, new Vector3f(0f, 1f, 0f), null);
        xAxis.normalise();
        Vector3f yAxis = Vector3f.cross(vector, xAxis, null);
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

                Vector3f.add(vector, xAxis, vector);
                Vector3f.add(vector, yAxis, vector);

                break;
            }
            case CUBE:
            {
                vector.x += (float)level.random.nextGaussian() * spread;
                vector.y += (float)level.random.nextGaussian() * spread;
                vector.z += (float)level.random.nextGaussian() * spread;
                break;
            }
            case HORIZONTAL:
            {
                float xComponent = spread * (level.random.nextFloat() * 2f - 1f);

                xAxis.scale(xComponent);

                Vector3f.add(vector, xAxis, vector);

                break;
            }
            case VERTICAL:
            {
                float yComponent = spread * (level.random.nextFloat() * 2f - 1f);

                yAxis.scale(yComponent);

                Vector3f.add(vector, yAxis, vector);

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

                Vector3f.add(vector, xAxis, vector);
                Vector3f.add(vector, yAxis, vector);

                break;
            }
            default:
                break;
        }

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
