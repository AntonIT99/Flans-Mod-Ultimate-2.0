package com.flansmodultimate.common.raytracing;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.util.ModUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * This class takes a snapshot of the player's position rotation and held items at a certain point in time.
 * It is used to handle bullet detection. The server will store a second or two of snapshots so that it
 * can work out where the player thought they were shooting accounting for packet lag
 */
public class PlayerSnapshot
{
    public static final int NUM_PLAYER_SNAPSHOTS = 20;
    
    /** The player this snapshot is for */
    public final Player player;
    /** The player's position at the point the snapshot was taken */
    public final Vector3f pos;
    /** The player's velocity at the point the snapshot was taken */
    public final Vector3f vel;
    /** The hitboxes for this player */
    public final List<PlayerHitbox> hitboxes = new ArrayList<>();
    /** The time at which this snapshot was taken */
    public final long time;

    public PlayerSnapshot(Player p)
    {
        player = p;
        time = p.level().getGameTime();
        pos = ModUtils.isThePlayer(p) ? new Vector3f(p.getX(), p.getY() - 1.6F, p.getZ()) : new Vector3f(p.position());
        vel = new Vector3f(p.getDeltaMovement());

        RotatedAxes bodyAxes = new RotatedAxes(p.yBodyRot, 0F, 0F);
        RotatedAxes headAxes = new RotatedAxes(p.getYHeadRot() - p.yBodyRot + 90, 0F, p.getXRot());
        Vector3f bodyBox = new Vector3f(0.5F, 0.67F, 0.3F);
        Vector3f bodyPos = new Vector3f(-0.25F, 0.75F, -0.15F);

        Vector3f headPos = new Vector3f(-0.25F, 0F, -0.25F);
        Vector3f headBox = new Vector3f(0.5F, 0.5F, 0.5F);

        Vector3f legPos = new Vector3f(-0.25F, 0F, -0.15F);
        Vector3f legBox = new Vector3f(0.5F, 0.75F, 0.3F);

        //head
        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(headAxes), new Vector3f(0.0F, 1.4F, 0F), headPos, headBox, vel, EnumHitboxType.HEAD));

        //body
        hitboxes.add(new PlayerHitbox(player, bodyAxes, new Vector3f(0F, 0F, 0F), bodyPos, bodyBox, vel, EnumHitboxType.BODY));

        //legs
        hitboxes.add(new PlayerHitbox(player, bodyAxes, new Vector3f(0F, 0F, 0F), legPos, legBox, vel, EnumHitboxType.LEGS));

        //Calculate rotation of arms using modified code from ModelBiped
        float yHead = (p.getYHeadRot() - p.yBodyRot) * Mth.DEG_TO_RAD;
        float xHead = p.getXRot() * Mth.DEG_TO_RAD;

        float zRight = 0.0F;
        float zLeft = 0.0F;
        float yRight = -0.1F + yHead - (Mth.PI / 2F);
        float yLeft = 0.1F + yHead + 0.4F - (Mth.PI / 2F);
        float xRight = -(Mth.PI / 2F) + xHead;
        float xLeft = -(Mth.PI / 2F) + xHead;

        zRight += Mth.cos(p.tickCount * 0.09F) * 0.05F + 0.05F;
        zLeft -= Mth.cos(p.tickCount * 0.09F) * 0.05F + 0.05F;
        xRight += Mth.sin(p.tickCount * 0.067F) * 0.05F;
        xLeft -= Mth.sin(p.tickCount * 0.067F) * 0.05F;

        RotatedAxes leftArmAxes = (new RotatedAxes()).rotateGlobalPitchInRads(xLeft).rotateGlobalYawInRads(Mth.PI + yLeft).rotateGlobalRollInRads(-zLeft);
        RotatedAxes rightArmAxes = (new RotatedAxes()).rotateGlobalPitchInRads(xRight).rotateGlobalYawInRads(Mth.PI + yRight).rotateGlobalRollInRads(-zRight);

        float originZRight = Mth.sin(-p.yBodyRot * Mth.DEG_TO_RAD) * 5.0F / 16F;
        float originXRight = -Mth.cos(-p.yBodyRot * Mth.DEG_TO_RAD) * 5.0F / 16F;

        float originZLeft = -Mth.sin(-p.yBodyRot * Mth.DEG_TO_RAD) * 5.0F / 16F;
        float originXLeft = Mth.cos(-p.yBodyRot * Mth.DEG_TO_RAD) * 5.0F / 16F;

        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(leftArmAxes), new Vector3f(originXLeft, 1.3F, originZLeft), new Vector3f(-2F / 16F, -0.6F, -2F / 16F), new Vector3f(0.25F, 0.7F, 0.25F), vel, EnumHitboxType.LEFTARM));
        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(-2F / 16F, -0.6F, -2F / 16F), new Vector3f(0.25F, 0.7F, 0.25F), vel, EnumHitboxType.RIGHTARM));

        //Add box for right hand shield
        ItemStack playerRightHandStack = player.getMainHandItem();
        if (!playerRightHandStack.isEmpty() && playerRightHandStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            if(gunType.isShield())
            {
                hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(gunType.getShieldOrigin().y, -1.05F + gunType.getShieldOrigin().x, -1F / 16F + gunType.getShieldOrigin().z), new Vector3f(gunType.getShieldDimensions().y, gunType.getShieldDimensions().x, gunType.getShieldDimensions().z), vel, EnumHitboxType.RIGHTITEM));
            }
        }

        ItemStack playerLeftHandStack = player.getOffhandItem();
        if (!playerLeftHandStack.isEmpty() && playerLeftHandStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            if (gunType.isShield())
            {
                hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(gunType.getShieldOrigin().y, -1.05F + gunType.getShieldOrigin().x, -1F / 16F + gunType.getShieldOrigin().z), new Vector3f(gunType.getShieldDimensions().y, gunType.getShieldDimensions().x, gunType.getShieldDimensions().z), vel, EnumHitboxType.RIGHTITEM));
            }
        }
    }

    public List<BulletHit> raytrace(Vector3f origin, Vector3f motion)
    {
        return raytrace(origin, motion, 0F, 1F);
    }

    public List<BulletHit> raytrace(Vec3 origin, Vec3 motion, float lowerBound, float upperBound)
    {
        return raytrace(new Vector3f(origin), new Vector3f(motion), lowerBound, upperBound);
    }

    public List<BulletHit> raytrace(Vector3f origin, Vector3f motion, float lowerBound, float upperBound)
    {
        //Prepare a list for the hits
        List<BulletHit> hits = new ArrayList<>();

        if (upperBound <= lowerBound)
            return hits;

        //Get the bullet raytrace vector into local coordinates
        Vector3f localOrigin = Vector3f.sub(origin, pos, null);

        //Check each hitbox for a hit
        for (PlayerHitbox hitbox : hitboxes)
        {
            PlayerBulletHit hit = hitbox.raytrace(localOrigin, motion);
            if (hit != null && hit.getIntersectTime() >= lowerBound && hit.getIntersectTime() <= upperBound)
            {
                hits.add(hit);
            }
        }

        return hits;
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSnapshot()
    {
        for (PlayerHitbox hitbox : hitboxes)
        {
            hitbox.renderHitbox(player.level(), pos);
        }
    }
}