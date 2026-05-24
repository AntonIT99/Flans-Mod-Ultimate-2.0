package com.flansmodultimate.common.guns.handler;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumFireMode;
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

        if (!player.getAbilities().instabuild)
            ModUtils.dropItem(level, player, shootableType.getDropItemOnShoot(), shootableType.getContentPack());

        ModUtils.dropItem(level, player, gunType.getDropItemOnShoot(), gunType.getContentPack());

        if (gunType.getKnockback() > 0F && !player.isCrouching())
            knockbackOppositeLook(player, gunType.getKnockback());

        ShootableItem.consumeRound(bulletStack);

        gunItem.setBulletItemStack(gunStack, bulletStack, ammoIndex);

        if (gunType.isConsumeGunUponUse())
            player.setItemInHand(hand, ItemStack.EMPTY);

        PlayerData data = PlayerData.getInstance(player, level.isClientSide ? net.minecraftforge.fml.LogicalSide.CLIENT : net.minecraftforge.fml.LogicalSide.SERVER);
        EnumFireMode mode = gunType.getFireMode(gunStack);
        
        if (mode == EnumFireMode.BURST)
        {
            int remaining = data.getBurstRoundsRemaining(hand);
            if (remaining > 0)
            {
                data.setBurstRoundsRemaining(hand, remaining - 1);
            }
            else
            {
                data.setBurstRoundsRemaining(hand, gunType.getNumBurstRounds() - 1);
            }
        }
    }

    private void knockbackOppositeLook(Player player, float knockback)
    {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement().add(
            -look.x * knockback * 0.1,
            -look.y * knockback * 0.1,
            -look.z * knockback * 0.1
        ));
    }
}
