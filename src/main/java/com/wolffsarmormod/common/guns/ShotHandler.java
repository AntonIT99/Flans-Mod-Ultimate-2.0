package com.wolffsarmormod.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.common.FlansExplosion;
import com.wolffsarmormod.common.entity.Bullet;
import com.wolffsarmormod.common.entity.PlayerBulletHit;
import com.wolffsarmormod.common.raytracing.BlockHit;
import com.wolffsarmormod.common.raytracing.BulletHit;
import com.wolffsarmormod.common.raytracing.DriveableHit;
import com.wolffsarmormod.common.raytracing.EntityHit;
import com.wolffsarmormod.common.raytracing.FlansModRaytracer;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.network.PacketBlockHitEffect;
import com.wolffsarmormod.network.PacketBulletTrail;
import com.wolffsarmormod.network.PacketFlak;
import com.wolffsarmormod.network.PacketHandler;
import com.wolffsarmormod.network.PacketHitMarker;
import com.wolffsarmormod.network.PacketPlaySound;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.Tags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
public class ShotHandler
{
    /**
     * For any kind of shooting this method should be used. It handles everything including the differentiation between spawning a EntityBullet and performing a raytrace
     *
     * @param world             World where the shot is fired
     * @param shot              FiredShot object, created using the guidelines
     * @param bulletAmount      Number how many bullets should be fired
     * @param rayTraceOrigin    Origin of the bullet
     * @param shootingDirection Direction where the bullet will travel
     */
    public static void fireGun(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection)
    {
        fireGun(world, shot, bulletAmount, rayTraceOrigin, shootingDirection, isExtraBullet -> {});
    }

    /**
     * For any kind of shooting this method should be used. It handles everything including the differentiation between spawning a EntityBullet and performing a raytrace
     *
     * @param world             World where the shot is fired
     * @param shot              FiredShot object, created using the guidelines
     * @param bulletAmount      Number how many bullets should be fired
     * @param rayTraceOrigin    Origin of the bullet
     * @param shootingDirection Direction where the bullet will travel
     * @param handler           ShootBulletHandler which is called every time a shot is fired (bulletAmount times)
     */
    public static void fireGun(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection, ShootBulletHandler handler)
    {
        if (shot.getFireableGun().getBulletSpeed() == 0F)
        {
            //Raytrace
            createMultipleShots(world, shot, bulletAmount, rayTraceOrigin, shootingDirection, handler);
        }
        else
        {
            //Spawn EntityBullet
            for (int i = 0; i < bulletAmount; i++)
            {
                world.addFreshEntity(new Bullet(world, shot, rayTraceOrigin.toVec3(), shootingDirection.toVec3()));
                handler.shooting(i < bulletAmount - 1);
            }
        }
    }

    private static void createMultipleShots(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection, ShootBulletHandler handler)
    {
        float bulletspread = 0.0025F * shot.getFireableGun().getSpread() * shot.getBulletType().getBulletSpread();
        for (int i = 0; i < bulletAmount; i++)
        {
            createShot(world, shot, bulletspread, rayTraceOrigin, new Vector3f(shootingDirection));
            handler.shooting(i < bulletAmount - 1);
        }
    }

    private static void createShot(Level level, FiredShot shot, float bulletspread, Vector3f rayTraceOrigin, Vector3f shootingDirection)
    {
        randomizeVectorDirection(level, shootingDirection, bulletspread, shot.getFireableGun().getSpreadPattern());
        shootingDirection.scale(500.0f);

        float penetrationPower = shot.getBulletType().getPenetratingPower();
        //first tries to get the player because the players vehicle is also ignored, or get the player independent shooter or null
        Entity ignore = shot.getPlayerOptional().isPresent() ? shot.getPlayerOptional().get() : shot.getShooterOptional().orElse(null);

        List<BulletHit> hits = FlansModRaytracer.raytrace(level, ignore, false, null, rayTraceOrigin, shootingDirection, 0, penetrationPower);
        Vector3f previousHitPos = rayTraceOrigin;
        Vector3f finalhit = null;

        for (BulletHit hit : hits)
        {
            Vector3f shotVector = new Vector3f(shootingDirection).scale(hit.intersectTime);
            Vector3f hitPos = Vector3f.add(rayTraceOrigin, shotVector, null);

            //TODO: debug
            /*if(FlansMod.DEBUG)
            {
                if (hit instanceof BlockHit)
                {
                    level.spawnEntity(new EntityDebugDot(level, hitPos, 1000, 1.0f, 0f, 1.0f));
                }
                else
                {
                    level.spawnEntity(new EntityDebugDot(level, hitPos, 1000, 1.0f, 1.0f, 1.0f));
                }
                level.spawnEntity(new EntityDebugVector(level, previousHitPos, Vector3f.sub(hitPos, previousHitPos, null), 1000, 0.5f, 0.5f, ((float)i/hits.size())));
            }*/
            previousHitPos = hitPos;

            penetrationPower = onHit(level, hitPos, shootingDirection, shot, hit, penetrationPower);
            if (penetrationPower <= 0f)
            {
                onDetonate(level, shot, hitPos);
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
     * @return The remaining penetrationPower after hitting the object specified in the BulletHit
     */
    public static float onHit(Level level, Vector3f hit, Vector3f shootingDirection, FiredShot shot, BulletHit bulletHit, float penetratingPower)
    {
        float damage = shot.getFireableGun().getDamage();

        BulletType bulletType = shot.getBulletType();
        if (bulletHit instanceof DriveableHit driveableHit)
        {
            penetratingPower = driveableHit.driveable.bulletHit(bulletType, shot.getFireableGun().getDamageAgainstVehicles(), driveableHit, penetratingPower);
            //TODO: Debug Mode
            /*if(FlansMod.DEBUG)
                level.spawnEntity(new EntityDebugDot(level, hit, 1000, 0F, 0F, 1F));*/

            //Send hit marker, if player is present
            shot.getPlayerOptional().ifPresent((ServerPlayer player) -> PacketHandler.sendTo(new PacketHitMarker(), player));
        }
        else if (bulletHit instanceof PlayerBulletHit playerHit)
        {
            penetratingPower = playerHit.hitbox.hitByBullet(shot, damage, penetratingPower);

            //TODO: debug mode
            /*if (FlansMod.DEBUG)
                level.spawnEntity(new EntityDebugDot(level, hit, 1000, 1F, 0F, 0F));*/

            Optional<ServerPlayer> optionalPlayer = shot.getPlayerOptional();

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
                PacketHandler.sendTo(new PacketHitMarker(), player);
            }
        }
        else if (bulletHit instanceof EntityHit entityHit)
        {
            if (entityHit.entity != null)
            {
                if (entityHit.entity.hurt(shot.getDamageSource(level), damage * bulletType.getDamageVsLiving()) && entityHit.entity instanceof LivingEntity living)
                {
                    for (MobEffectInstance effect : bulletType.getHitEffects())
                    {
                        living.addEffect(new MobEffectInstance(effect));
                    }
                    // If the attack was allowed, we should remove their immortality cooldown so we can shoot them again. Without this, any rapid fire gun become useless
                    living.invulnerableTime = 0;
                }
                if(bulletType.isSetEntitiesOnFire())
                    entityHit.entity.setSecondsOnFire(20);
                penetratingPower -= 1F;

                //TODO: Debug Mode
                /*if(FlansMod.DEBUG)
                    level.spawnEntity(new EntityDebugDot(level, hit, 1000, 1F, 1F, 0F));*/
            }

            //Send hit marker, if player is present
            shot.getPlayerOptional().ifPresent((ServerPlayer player) -> PacketHandler.sendTo(new PacketHitMarker(), player));


        }
        else if (bulletHit instanceof BlockHit bh && bh.getHitResult().getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult bhr = (BlockHitResult) bh.getHitResult();
            BlockPos pos = bhr.getBlockPos();
            Direction direction = bhr.getDirection();
            Vec3 impact = bhr.getLocation();

            // State is already “actual” in modern MC (no getActualState)
            BlockState state = level.getBlockState(pos);

            //TODO: Debug mode
            /*if(FlansMod.DEBUG)
                level.spawnEntity(new EntityDebugDot(level, hit, 1000, 0F, 1F, 0F));*/

            boolean isGlass = state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE);
            //If the bullet breaks glass, and can do so according to FlansMod, do so.
            if (bulletType.isBreaksGlass() && isGlass)
            {
                //TODO: Teams
                /*if (TeamsManager.canBreakGlass)
                {
                    WorldServer levelServer = (WorldServer)level;
                    destroyBlock(levelServer, pos, shot.getPlayerOptional().orElse(null), false);
                }*/
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

    /**
     * @param level       World which contains the detonatePos
     * @param shot        FiredShot instance of the shot
     * @param detonatePos Location where the detonation should happen
     */
    public static void onDetonate(Level level, FiredShot shot, Vector3f detonatePos)
    {
        BulletType bulletType = shot.getBulletType();

        if (bulletType.getExplosionRadius() > 0)
        {
            new FlansExplosion(level, shot.getShooterOptional().orElse(null), shot.getPlayerOptional().orElse(null), bulletType,
                    detonatePos.x, detonatePos.y, detonatePos.z, bulletType.getExplosionRadius(), bulletType.getFireRadius() > 0, bulletType.getFlak() > 0, bulletType.isExplosionBreaksBlocks());
        }
        if (bulletType.getFireRadius() > 0)
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
        // Send flak packet
        if (bulletType.getFlak() > 0)
        {
            PacketHandler.sendToAllAround(new PacketFlak(detonatePos.x, detonatePos.y, detonatePos.z, bulletType.getFlak(), bulletType.getFlakParticles()), detonatePos.x, detonatePos.y, detonatePos.z, 200, level.dimension());
        }
        // Drop item on hitting if bullet requires it
        if (bulletType.getDropItemOnHit() != null)
        {
            //TODO save ItemStack on load into the bulletType
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
