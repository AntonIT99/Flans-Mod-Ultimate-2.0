package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * This ItemEntity must carry an ItemGun as its main stack.
 * It also carries extra ammo stacks in ammoStacks (persisted in NBT).
 * Touching it transfers compatible ammo into player inventory if player has matching guns.
 * Right-clicking it with a gun swaps gun+ammo bundles (player's gun+ammo <-> ground gun+ammo).
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GunItemEntity extends ItemEntity
{
    protected static final String NBT_AMMO_LIST = "AmmoStacks";
    protected final List<ItemStack> ammoStacks = new ArrayList<>();

    public GunItemEntity(EntityType<? extends GunItemEntity> type, Level level)
    {
        super(type, level);
    }

    public GunItemEntity(ItemEntity itemEntity)
    {
        this(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), itemEntity.getItem());
    }

    public GunItemEntity(Level level, double x, double y, double z, ItemStack gunStack)
    {
        super(FlansMod.gunItemEntity.get(), level);
        setPos(x, y, z);
        setDeltaMovement(level.random.nextDouble() * 0.2D - 0.1D, 0.2D, level.random.nextDouble() * 0.2D - 0.1D);
        setItem(gunStack);
        gunStack.getItem();
        lifespan = gunStack.getEntityLifespan(level);
    }

    public GunItemEntity(Level level, double x, double y, double z, ItemStack gunStack, List<ItemStack> stacks)
    {
        this(level, x, y, z, gunStack);
        for (ItemStack s : stacks)
        {
            if (s != null && !s.isEmpty() && (s.getItem() instanceof BulletItem))
                ammoStacks.add(s.copy());
        }
    }

    @Override
    public void tick() {
        super.tick();

        Level level = level();

        // If not a gun => die
        ItemStack stack = getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem))
        {
            discard();
            return;
        }

        // client side extinguish.
        if (level.isClientSide)
        {
            clearFire();
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount)
    {
        return false;
    }

    @Override
    public boolean isOnFire()
    {
        return false;
    }

    /**
     * Called when a player touches the entity (walk pickup mechanics).
     * If ammoStacks has ammo, distribute compatible ammo into player inventory
     * ONLY if player has a gun that accepts it. Remove ammo from ammoStacks.
     * If ammoStacks becomes empty -> discard entity.
     */
    @Override
    public void playerTouch(@NotNull Player player)
    {
        Level level = level();

        if (level.isClientSide)
            return;

        if (!ammoStacks.isEmpty())
        {
            // For each gun in inventory, try to add matching ammo stacks.
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            {
                ItemStack invStack = player.getInventory().getItem(i);
                if (!invStack.isEmpty() && invStack.getItem() instanceof GunItem gunItem)
                {
                    GunType gunType = gunItem.getConfigType();
                    List<ShootableType> compatibleAmmoTypes = gunType.getAmmoTypes();

                    // iterate backwards so removals are safe
                    for (int j = ammoStacks.size() - 1; j >= 0; j--)
                    {
                        ItemStack ammoStack = ammoStacks.get(j);
                        if (ammoStack == null || ammoStack.isEmpty() || !(ammoStack.getItem() instanceof ShootableItem shootable))
                            continue;

                        ShootableType bulletType = shootable.getConfigType();
                        if (compatibleAmmoTypes.contains(bulletType) && player.getInventory().add(ammoStack))
                        {
                            // Vanilla-ish pickup sound
                            level.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F);
                            ammoStacks.remove(j);
                        }

                    }

                    if (ammoStacks.isEmpty())
                    {
                        discard();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Right-click interaction: swap the player's held gun+ammo bundle with the entity's gun+ammo bundle.
     * This is the 1.7.10 interactFirst logic.
     */
    @Override
    @NotNull
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand)
    {
        Level level = level();

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        ItemStack currentItem = player.getItemInHand(hand);
        if (currentItem.isEmpty() || !(currentItem.getItem() instanceof GunItem gunItem))
            return InteractionResult.PASS;

        ItemStack groundGun = getItem();
        if (groundGun.isEmpty() || !(groundGun.getItem() instanceof GunItem))
        {
            discard();
            return InteractionResult.FAIL;
        }

        GunType gunType = gunItem.getConfigType();
        List<ShootableType> compatibleAmmoTypes = gunType.getAmmoTypes();

        // Collect all compatible ammo from player's inventory, removing it from inventory
        List<ItemStack> newAmmoStacks = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ShootableItem shootable)
            {
                ShootableType bulletType = shootable.getConfigType();
                if (compatibleAmmoTypes.contains(bulletType))
                {
                    newAmmoStacks.add(stack.copy());
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // Spawn new dropped entity with the player's current gun + collected ammo
        level.addFreshEntity(new GunItemEntity(level, getX(), getY(), getZ(), currentItem.copy(), newAmmoStacks));

        // Put the ground gun into the player's hand slot
        player.setItemInHand(hand, groundGun.copy());

        // Give the player this entity's stored ammo
        for (ItemStack ammo : ammoStacks)
        {
            if (ammo != null && !ammo.isEmpty()) {
                player.getInventory().add(ammo);
            }
        }

        // Remove this entity (we swapped it)
        discard();

        PlayerData.getInstance(player).setShootClickDelay(10);
        PlayerData.getInstance(player).setShooting(hand, false);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);

        ListTag ammoList = new ListTag();
        for (ItemStack stack : ammoStacks)
        {
            if (stack != null && !stack.isEmpty())
                ammoList.add(stack.save(new CompoundTag()));
        }
        tag.put(NBT_AMMO_LIST, ammoList);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);

        if (tag.contains(NBT_AMMO_LIST, Tag.TAG_LIST))
        {
            ListTag ammoList = tag.getList(NBT_AMMO_LIST, Tag.TAG_COMPOUND);
            for (int i = 0; i < ammoList.size(); i++)
            {
                CompoundTag stackTag = ammoList.getCompound(i);
                ItemStack stack = ItemStack.of(stackTag);
                if (!stack.isEmpty())
                    ammoStacks.add(stack);
            }
        }
    }
}