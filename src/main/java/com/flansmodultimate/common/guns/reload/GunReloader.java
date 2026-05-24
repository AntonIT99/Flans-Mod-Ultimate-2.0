package com.flansmodultimate.common.guns.reload;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.digitalammo.DigitalAmmoHelper;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.event.GunReloadEvent;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketCancelGunReloadClient;
import com.flansmodultimate.network.client.PacketCancelSound;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record GunReloader(GunItem item)
{
    /**
     * Returns true if we reloaded or successfully queued a reload
     */
    public boolean reload(Level level, ServerPlayer player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean forceReload, boolean instabuild, boolean combineAmmoOnReload, boolean ammoToUpperInventory, float reloadTime, UUID reloadSoundUUID)
    {
        // Deployable guns cannot be reloaded in the inventory
        if (item.getConfigType().isDeployable())
            return false;

        // Cannot force reload mid-clip
        if (forceReload && !item.getConfigType().isCanForceReload())
            return false;

        // melee/no-ammo guard
        List<ShootableType> allowed = item.getConfigType().getAmmoTypes();
        if (allowed == null || allowed.isEmpty())
            return false;

        // Prevent spam / double queue
        if (data.getPendingReload().isPresent())
            return false;

        // Pre-reload event (cancelable + can override needsAmmo)
        GunReloadEvent evt = new GunReloadEvent(player, gunStack);
        MinecraftForge.EVENT_BUS.post(evt);
        if (evt.isCanceled())
            return false;

        // Digital ammo system: check if enabled and try to use it
        if (ModCommonConfig.get().enableDigitalAmmoSystem())
        {
            List<ReloadPlan> digitalPlans = computeDigitalAmmoPlansWithCheck(level, player, gunStack, allowed, forceReload);
            if (!digitalPlans.isEmpty())
            {
                long ticks = (long) Math.ceil(reloadTime);
                long applyAt = level.getGameTime() + ticks;
                return data.queuePendingReload(new PendingReload(gunStack, hand, applyAt, digitalPlans, forceReload, instabuild, combineAmmoOnReload, ammoToUpperInventory, reloadSoundUUID, true));
            }
        }

        // Compute plans
        String preferredAmmo = item.getPreferredAmmo(gunStack);
        List<ReloadPlan> plans = computePlans(gunStack, player.getInventory(), allowed, preferredAmmo, forceReload);

        if (plans.isEmpty())
            return false;

        long ticks = (long) Math.ceil(reloadTime);
        long applyAt = level.getGameTime() + ticks;

        return data.queuePendingReload(new PendingReload(gunStack, hand, applyAt, plans, forceReload, instabuild, combineAmmoOnReload, ammoToUpperInventory, reloadSoundUUID, false));
    }

    public static void handlePendingReload(Level level, ServerPlayer player, PlayerData data)
    {
        PendingReload pendingReload = data.getPendingReload().orElse(null);
        if (pendingReload == null)
            return;

        boolean canceled = cancelReloadIfSwitched(player, pendingReload);
        if (canceled)
        {
            PacketHandler.sendTo(new PacketCancelGunReloadClient(pendingReload.hand()), player);
            PacketHandler.sendToDimension(level.dimension(), new PacketCancelSound(pendingReload.reloadSoundUUID()));
            data.clearPendingReload();
            return;
        }

        tryApplyPendingReload(level, player, data, pendingReload);
    }

    private static boolean cancelReloadIfSwitched(ServerPlayer player, PendingReload pendingReload)
    {
        if (!ModCommonConfig.get().cancelReloadOnWeaponSwitch())
            return false;

        return !ItemStack.matches(player.getItemInHand(pendingReload.hand()), pendingReload.gunStack());
    }

    private static void tryApplyPendingReload(Level level, ServerPlayer player, PlayerData data, PendingReload pendingReload)
    {
        if (level.getGameTime() < pendingReload.applyAtGameTime())
            return;

        ItemStack currentGunStack = player.getItemInHand(pendingReload.hand());
        if (currentGunStack.getItem() instanceof GunItem gunItem)
        {
            if (!ItemStack.isSameItem(currentGunStack, pendingReload.gunStack()))
            {
                data.clearPendingReload();
                return;
            }
            applyPlans(level, player, player.getInventory(), pendingReload, gunItem, currentGunStack);
            data.clearPendingReload();
        }
    }

    private static void applyPlans(Level level, Entity reloadingEntity, Container inventory, PendingReload pending, GunItem gunItem, ItemStack actualGunStack)
    {
        if (pending.useDigitalAmmo() && reloadingEntity instanceof ServerPlayer serverPlayer)
        {
            applyDigitalAmmoPlans(serverPlayer, actualGunStack, gunItem, pending.plans());
            return;
        }

        for (ReloadPlan plan : pending.plans())
        {
            applySinglePlan(level, reloadingEntity, inventory, pending, plan, gunItem, actualGunStack);
        }
    }

    private static void applySinglePlan(Level level, Entity reloadingEntity, Container inventory, PendingReload pending, ReloadPlan plan, GunItem gunItem, ItemStack actualGunStack)
    {
        int ammoIndex = plan.gunAmmoIndex();

        int invSlot = plan.inventorySlot();

        ItemStack newMag = inventory.getItem(invSlot);
        if (newMag.isEmpty())
            return;
        if (!(newMag.getItem() instanceof ShootableItem newShootable))
            return;

        // Re-validate ammo allowed (inventory may have changed since queue)
        List<ShootableType> allowed = gunItem.getConfigType().getAmmoTypes();
        if (allowed == null || !allowed.contains(newShootable.getConfigType()))
            return;

        ItemStack oldMag = gunItem.getAmmoItemStack(actualGunStack, ammoIndex);

        // Drop-on-reload when old mag is empty (null-safe)
        if (!pending.creative()
            && oldMag != null && !oldMag.isEmpty()
            && !ShootableItem.hasRoundsLeft(oldMag)
            && oldMag.getItem() instanceof ShootableItem oldShootable)
        {

            ModUtils.dropItem(level, reloadingEntity, oldShootable.getConfigType().getDropItemOnReload(), oldShootable.getConfigType().getContentPack());
        }

        // Return unfinished old mag
        if (oldMag != null && !oldMag.isEmpty()
            && ShootableItem.hasRoundsLeft(oldMag))
        {

            ItemStack toReturn = oldMag.copy();
            boolean added = InventoryHelper.addItemStackToContainer(inventory, toReturn, pending.creative(), pending.combineAmmoOnReload(), pending.ammoToUpperInventory(), Inventory.getSelectionSize());
            if (!added)
                reloadingEntity.spawnAtLocation(toReturn, 0.5F);
        }

        // Load new mag into gun
        ItemStack stackToLoad = newMag.copy();
        stackToLoad.setCount(1);
        gunItem.setBulletItemStack(actualGunStack, stackToLoad, ammoIndex);

        // Consume inventory mag
        if (!pending.creative())
            newMag.setCount(newMag.getCount() - 1);

        if (newMag.getCount() <= 0)
            inventory.setItem(invSlot, ItemStack.EMPTY);
        else
            inventory.setItem(invSlot, newMag);
    }

    private static void applyDigitalAmmoPlans(ServerPlayer player, ItemStack actualGunStack, GunItem gunItem, List<ReloadPlan> plans)
    {
        List<Integer> ammoSlots = plans.stream().map(ReloadPlan::gunAmmoIndex).toList();
        DigitalAmmoHelper.tryReloadFromDigitalAmmo(player, gunItem, actualGunStack, ammoSlots);
    }

    private List<ReloadPlan> computeDigitalAmmoPlans(ItemStack gunStack, List<ShootableType> allowedAmmoTypes, boolean forceReload)
    {
        int ammoSlots = item.getConfigType().getNumAmmoItemsInGun(gunStack);
        List<ReloadPlan> plans = new ArrayList<>(ammoSlots);

        for (int i = 0; i < ammoSlots; i++)
        {
            ItemStack current = item.getAmmoItemStack(gunStack, i);
            if (!needsSwap(current, forceReload))
                continue;

            plans.add(new ReloadPlan(i, -1));
        }

        return plans;
    }

    private List<ReloadPlan> computeDigitalAmmoPlansWithCheck(Level level, ServerPlayer player, ItemStack gunStack, List<ShootableType> allowedAmmoTypes, boolean forceReload)
    {
        int ammoSlots = item.getConfigType().getNumAmmoItemsInGun(gunStack);
        List<ReloadPlan> plans = new ArrayList<>(ammoSlots);

        for (int i = 0; i < ammoSlots; i++)
        {
            ItemStack current = item.getAmmoItemStack(gunStack, i);
            if (needsSwap(current, forceReload))
            {
                plans.add(new ReloadPlan(i, -1));
            }
        }

        if (plans.isEmpty())
            return new ArrayList<>();

        if (!DigitalAmmoHelper.hasEnoughDigitalAmmo(player, item))
            return new ArrayList<>();

        return plans;
    }

    private List<ReloadPlan> computePlans(ItemStack gunStack, Container inventory, List<ShootableType> allowedAmmoTypes, String preferredAmmo, boolean forceReload)
    {
        int ammoSlots = item.getConfigType().getNumAmmoItemsInGun(gunStack);
        List<ReloadPlan> plans = new ArrayList<>(ammoSlots);

        int invSize = inventory.getContainerSize();
        int[] remainingCount = new int[invSize];
        for (int s = 0; s < invSize; s++)
        {
            ItemStack stack = inventory.getItem(s);
            if (!stack.isEmpty() && stack.getItem() instanceof ShootableItem shootableItem && allowedAmmoTypes.contains(shootableItem.getConfigType()))
            {
                int bullets = ShootableItem.getRoundsRemaining(stack);
                remainingCount[s] = (bullets > 0) ? stack.getCount() : 0;
            }
        }

        for (int i = 0; i < ammoSlots; i++)
        {
            ItemStack current = item.getAmmoItemStack(gunStack, i);
            if (!needsSwap(current, forceReload))
                continue;

            int bestSlot = findBestSlotWithRemaining(inventory, allowedAmmoTypes, preferredAmmo, remainingCount);
            if (bestSlot == -1)
                continue;

            remainingCount[bestSlot]--;
            plans.add(new ReloadPlan(i, bestSlot));
        }

        return plans;
    }

    private static boolean needsSwap(@Nullable ItemStack bulletStack, boolean forceReload)
    {
        if (forceReload || bulletStack == null || bulletStack.isEmpty())
            return true;

        return !ShootableItem.hasRoundsLeft(bulletStack);
    }

    public static int findBestSlotPreferredFirst(Container inventory, List<ShootableType> allowedAmmoTypes, @Nullable String preferredAmmoShortName, boolean[] reservedSlots)
    {
        int bestAnySlot = -1;
        int bestAnyBullets = 0;
        int bestPrefSlot = -1;
        int bestPrefBullets = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (reservedSlots != null && reservedSlots[i])
                continue;

            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty())
                continue;

            if (!(stack.getItem() instanceof ShootableItem shootableItem))
                continue;

            ShootableType type = shootableItem.getConfigType();
            if (!allowedAmmoTypes.contains(type))
                continue;

            int bullets = ShootableItem.getRoundsRemaining(stack);
            if (bullets <= 0)
                continue;

            boolean preferred = preferredAmmoShortName != null && preferredAmmoShortName.equals(type.getOriginalShortName());

            if (preferred)
            {
                if (bullets > bestPrefBullets)
                {
                    bestPrefBullets = bullets;
                    bestPrefSlot = i;
                }
            }
            else if (bestPrefSlot == -1 && bullets > bestAnyBullets)
            {
                bestAnyBullets = bullets;
                bestAnySlot = i;
            }
        }
        return bestPrefSlot != -1 ? bestPrefSlot : bestAnySlot;
    }

    private static int findBestSlotWithRemaining(Container inventory, List<ShootableType> allowedAmmoTypes, @Nullable String preferredAmmoShortName, int[] remainingCount)
    {
        int bestAnySlot = -1;
        int bestAnyBullets = 0;
        int bestPrefSlot = -1;
        int bestPrefBullets = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (remainingCount[i] <= 0)
                continue;

            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty())
                continue;

            if (!(stack.getItem() instanceof ShootableItem shootableItem))
                continue;

            ShootableType type = shootableItem.getConfigType();
            if (!allowedAmmoTypes.contains(type))
                continue;

            int bullets = ShootableItem.getRoundsRemaining(stack);
            if (bullets <= 0)
                continue;

            boolean preferred = preferredAmmoShortName != null && preferredAmmoShortName.equals(type.getOriginalShortName());

            if (preferred)
            {
                if (bullets > bestPrefBullets)
                {
                    bestPrefBullets = bullets;
                    bestPrefSlot = i;
                }
            }
            else if (bestPrefSlot == -1 && bullets > bestAnyBullets)
            {
                bestAnyBullets = bullets;
                bestAnySlot = i;
            }
        }
        return bestPrefSlot != -1 ? bestPrefSlot : bestAnySlot;
    }
}
