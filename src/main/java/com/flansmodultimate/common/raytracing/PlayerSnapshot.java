package com.flansmodultimate.common.raytracing;

import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModelCache;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    private static final float PI = (float) Math.PI;
    
    /**
     * The player this snapshot is for
     */
    public final Player player;
    /**
     * The player's position at the point the snapshot was taken
     */
    public final Vector3f pos;
    /**
     * The hitboxes for this player
     */
    public final List<PlayerHitbox> hitboxes = new ArrayList<>();
    /**
     * The time at which this snapshot was taken
     */
    public final long time;

    public PlayerSnapshot(Player p)
    {
        player = p;
        time = p.level().getGameTime();
        pos = new Vector3f(p.getX(), p.getY(), p.getZ());

        RotatedAxes bodyAxes = new RotatedAxes(p.yBodyRot, 0F, 0F);
        RotatedAxes headAxes = new RotatedAxes(p.getYHeadRot() - p.yBodyRot, p.getXRot(), 0F);

        hitboxes.add(new PlayerHitbox(player, bodyAxes, new Vector3f(0F, 0F, 0F), new Vector3f(-0.25F, 0F, -0.15F), new Vector3f(0.5F, 1.4F, 0.3F), EnumHitboxType.BODY));
        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(headAxes), new Vector3f(0.0F, 1.4F, 0F), new Vector3f(-0.25F, 0F, -0.25F), new Vector3f(0.5F, 0.5F, 0.5F), EnumHitboxType.HEAD));

        //Calculate rotation of arms using modified code from ModelBiped
        float yHead = (p.getYHeadRot() - p.yBodyRot) / (180F / PI);
        float xHead = p.getXRot() / (180F / PI);

        float zRight = 0.0F;
        float zLeft = 0.0F;
        float yRight = -0.1F + yHead - (PI / 2F);
        float yLeft = 0.1F + yHead + 0.4F - (PI / 2F);
        float xRight = -(PI / 2F) + xHead;
        float xLeft = -(PI / 2F) + xHead;

        zRight += Mth.cos(p.tickCount * 0.09F) * 0.05F + 0.05F;
        zLeft -= Mth.cos(p.tickCount * 0.09F) * 0.05F + 0.05F;
        xRight += Mth.sin(p.tickCount * 0.067F) * 0.05F;
        xLeft -= Mth.sin(p.tickCount * 0.067F) * 0.05F;

        RotatedAxes leftArmAxes = (new RotatedAxes()).rotateGlobalPitchInRads(xLeft).rotateGlobalYawInRads(PI + yLeft).rotateGlobalRollInRads(-zLeft);
        RotatedAxes rightArmAxes = (new RotatedAxes()).rotateGlobalPitchInRads(xRight).rotateGlobalYawInRads(PI + yRight).rotateGlobalRollInRads(-zRight);

        float originZRight = Mth.sin(-p.yBodyRot * PI / 180F) * 5.0F / 16F;
        float originXRight = -Mth.cos(-p.yBodyRot * PI / 180F) * 5.0F / 16F;

        float originZLeft = -Mth.sin(-p.yBodyRot * PI / 180F) * 5.0F / 16F;
        float originXLeft = Mth.cos(-p.yBodyRot * PI / 180F) * 5.0F / 16F;

        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(leftArmAxes), new Vector3f(originXLeft, 1.3F, originZLeft), new Vector3f(-2F / 16F, -0.6F, -2F / 16F), new Vector3f(0.25F, 0.7F, 0.25F), EnumHitboxType.LEFTARM));
        hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(-2F / 16F, -0.6F, -2F / 16F), new Vector3f(0.25F, 0.7F, 0.25F), EnumHitboxType.RIGHTARM));

        //Add box for right hand shield
        ItemStack playerRightHandStack = player.getMainHandItem();
        if(!playerRightHandStack.isEmpty() && playerRightHandStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            if(gunType.isShield())
            {
                hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(gunType.getShieldOrigin().y, -1.05F + gunType.getShieldOrigin().x, -1F / 16F + gunType.getShieldOrigin().z), new Vector3f(gunType.getShieldDimensions().y, gunType.getShieldDimensions().x, gunType.getShieldDimensions().z), EnumHitboxType.RIGHTITEM));
            }
        }
        ItemStack playerLeftHandStack = player.getOffhandItem();
        if(!playerLeftHandStack.isEmpty() && playerLeftHandStack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            if (gunType.isShield())
            {
                hitboxes.add(new PlayerHitbox(player, bodyAxes.findLocalAxesGlobally(rightArmAxes), new Vector3f(originXRight, 1.3F, originZRight), new Vector3f(gunType.getShieldOrigin().y, -1.05F + gunType.getShieldOrigin().x, -1F / 16F + gunType.getShieldOrigin().z), new Vector3f(gunType.getShieldDimensions().y, gunType.getShieldDimensions().x, gunType.getShieldDimensions().z), EnumHitboxType.RIGHTITEM));
            }
        }
    }

    public List<BulletHit> raytrace(Vector3f origin, Vector3f motion)
    {
        //Get the bullet raytrace vector into local coordinates
        Vector3f localOrigin = Vector3f.sub(origin, pos, null);
        //Prepare a list for the hits
        ArrayList<BulletHit> hits = new ArrayList<>();

        //Check each hitbox for a hit
        for(PlayerHitbox hitbox : hitboxes)
        {
            PlayerBulletHit hit = hitbox.raytrace(localOrigin, motion);
            if(hit != null && hit.intersectTime >= 0F && hit.intersectTime <= 1F)
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

    public PlayerHitbox getHitbox(EnumHitboxType type)
    {
        for (PlayerHitbox hitbox : hitboxes)
        {
            if (hitbox.type == type)
                return hitbox;
        }
        return null;
    }

    public Vector3f getMuzzleLocation(GunType gunType, AttachmentType barrelAttachment, InteractionHand hand)
    {
        PlayerHitbox hitbox = getHitbox(hand == InteractionHand.OFF_HAND ? EnumHitboxType.LEFTARM : EnumHitboxType.RIGHTARM);
        Vector3f muzzlePos = new Vector3f(hitbox.o.x, hitbox.o.y + hitbox.d.y * 0.5f, hitbox.o.z + hitbox.d.z * 0.5f);

        ModelGun modelGun = (ModelGun) ModelCache.getOrLoadTypeModel(gunType);
        if (modelGun != null)
        {
            Vector3f barrelAttach = new Vector3f(modelGun.getBarrelAttachPoint().z, -modelGun.getBarrelAttachPoint().x, modelGun.getBarrelAttachPoint().y);
            Vector3f.add(muzzlePos, barrelAttach, muzzlePos);
        }

        muzzlePos = hitbox.axes.findLocalVectorGlobally(muzzlePos);

        Vector3f.add(muzzlePos, hitbox.rP, muzzlePos);
        return muzzlePos;
    }
}