package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.GunItemBehavior;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Applies all "on shoot" side-effects for the final bullet in a burst.
 */
public final class DefaultShootingHandler implements ShootingHandler
{

    private final Level level;
    private final Player player;
    private final ItemStack gunStack;
    private final InteractionHand hand;

    private GunItem gunItem;
    private GunType gunType;
    private ItemStack bulletStack;
    private ShootableType shootableType;
    private int bulletId;

    public DefaultShootingHandler(Level level, Player player, ItemStack gunStack, InteractionHand hand)
    {
        this.level = level;
        this.player = player;
        this.hand = hand;
        this.gunStack = gunStack;

        if (gunStack.getItem() instanceof GunItem item)
        {
            gunItem = item;
            gunType = gunItem.getConfigType();

            Optional<GunItemBehavior.AmmoSlot> ammoSlot = GunItemBehavior.findUsableAmmo(gunItem, gunStack, gunType);

            if (ammoSlot.isPresent())
            {
                bulletId = ammoSlot.get().index();
                bulletStack = ammoSlot.get().stack();

                if (bulletStack.getItem() instanceof ShootableItem shootableItem)
                {
                    shootableType = shootableItem.getConfigType();
                }
            }
        }
    }

    public DefaultShootingHandler(Level level, Player player, ItemStack gunStack, InteractionHand hand, GunItemBehavior.AmmoSlot ammoSlot)
    {
        this.level = level;
        this.player = player;
        this.hand = hand;
        this.gunStack = gunStack;

        if (gunStack.getItem() instanceof GunItem item)
        {
            gunItem = item;
            gunType = gunItem.getConfigType();
            bulletId = ammoSlot.index();
            bulletStack = ammoSlot.stack();

            if (bulletStack.getItem() instanceof ShootableItem shootableItem)
            {
                shootableType = shootableItem.getConfigType();
            }
        }
    }

    @Override
    public void shooting(boolean isExtraBullet)
    {
        if (isExtraBullet || gunType == null || shootableType == null)
            return;

        // Drop item on shooting if bullet requires it
        if (!player.isCreative())
        {
            GunItemBehavior.dropItem(level, player, shootableType.getDropItemOnShoot(), shootableType.getContentPack());
        }

        // Drop item on shooting if gun requires it
        GunItemBehavior.dropItem(level, player, gunType.getDropItemOnShoot(), gunType.getContentPack());

        if (gunType.getKnockback() > 0F)
            knockbackOppositeLook(player, gunType.getKnockback());

        // Damage the bullet item
        bulletStack.setDamageValue(bulletStack.getDamageValue() + 1);

        // Update the stack in the gun
        gunItem.setBulletItemStack(gunStack, bulletStack, bulletId);

        // Optionally consume the gun
        if (gunType.isConsumeGunUponUse())
        {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    public static void knockbackOppositeLook(Player player, double strength)
    {
        if (player.level().isClientSide)
            return;

        // Get where the player is looking
        Vec3 look = player.getLookAngle(); // (x, y, z)

        // Invert the look direction (opposite of where they look) and optionally flatten Y so it's mostly horizontal
        Vec3 dir = new Vec3(-look.x, 0.0, -look.z);

        if (dir.lengthSqr() < 1.0E-4)
            return; // avoid NaN if the vector is too small

        // Normalize & scale by your strength
        dir = dir.normalize().scale(strength);

        // Apply knockback
        player.setDeltaMovement(dir);

        // important so the client actually updates
        player.hurtMarked = true;
    }
}
