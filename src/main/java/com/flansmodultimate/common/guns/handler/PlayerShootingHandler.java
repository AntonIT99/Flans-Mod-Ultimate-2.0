package com.flansmodultimate.common.guns.handler;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.ModUtils;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Applies all "on shoot" side-effects for the final bullet in a burst.
 */
public final class PlayerShootingHandler implements ShootingHandler
{

    private final Level level;
    private final Player player;
    private final ItemStack gunStack;
    private final InteractionHand hand;

    private GunItem gunItem;
    private GunType gunType;
    private ItemStack bulletStack;
    private ShootableType shootableType;
    private int ammoIndex;

    public PlayerShootingHandler(Level level, Player player, InteractionHand hand, ItemStack gunStack, ItemStack bulletStack, int ammoIndex)
    {
        this.level = level;
        this.player = player;
        this.hand = hand;
        this.gunStack = gunStack;

        if (gunStack.getItem() instanceof GunItem item)
        {
            gunItem = item;
            gunType = gunItem.getConfigType();
            this.bulletStack = bulletStack;
            this.ammoIndex = ammoIndex;

            if (bulletStack.getItem() instanceof ShootableItem shootableItem)
            {
                shootableType = shootableItem.getConfigType();
            }
        }
    }

    @Override
    public void onShoot()
    {
        if (gunType == null || shootableType == null)
            return;

        // Drop item on shooting if bullet requires it
        if (!player.getAbilities().instabuild)
            ModUtils.dropItem(level, player, shootableType.getDropItemOnShoot(), shootableType.getContentPack());

        // Drop item on shooting if gun requires it
        ModUtils.dropItem(level, player, gunType.getDropItemOnShoot(), gunType.getContentPack());

        // Apply knockback to Player
        if (gunType.getKnockback() > 0F && !player.isCrouching())
            knockbackOppositeLook(player, gunType.getKnockback());

        // Damage the bullet item
        bulletStack.setDamageValue(bulletStack.getDamageValue() + 1);

        // Update the stack in the gun
        gunItem.setBulletItemStack(gunStack, bulletStack, ammoIndex);

        // Optionally consume the gun
        if (gunType.isConsumeGunUponUse())
            player.setItemInHand(hand, ItemStack.EMPTY);
    }

    public static void knockbackOppositeLook(Player player, double strength)
    {
        if (player.level().isClientSide)
            return;

        // Get where the player is looking
        Vec3 look = player.getLookAngle();

        // Invert the look direction (opposite of where they look) and flatten Y so it's mostly horizontal
        Vec3 dir = new Vec3(-look.x, 0.0, -look.z);

        if (dir.lengthSqr() < 1.0E-4)
            return;

        // Normalize & scale by your strength
        dir = dir.normalize().scale(strength);

        // Apply knockback
        player.setDeltaMovement(dir);

        // important so the client actually updates
        player.hurtMarked = true;
    }
}
