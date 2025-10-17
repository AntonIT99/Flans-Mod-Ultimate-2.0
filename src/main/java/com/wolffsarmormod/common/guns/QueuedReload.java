package com.wolffsarmormod.common.guns;

import com.wolffsarmormod.common.item.GunItem;
import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QueuedReload {

    @Getter
    private float reloadTime = 0;
    private boolean didReload = false;

    private final Level level;
    private final Entity entity;
    //private final IInventory inventory;
    private final boolean creative;
    private final boolean combineAmmoOnReload;
    private final boolean ammoToUpperInventory;
    private final boolean forceReload;
    private final ItemStack gunStack;

    public QueuedReload(ItemStack gunStack, float reloadTime, Level level, Entity entity, /*IInventory inventory,*/ boolean creative, boolean forceReload, boolean combineAmmoOnReload, boolean ammoToUpperInventory)
    {
        this.gunStack = gunStack;
        this.reloadTime = reloadTime;
        this.level = level;
        this.entity = entity;
        //this.inventory = inventory;
        this.creative = creative;
        this.forceReload = forceReload;
        this.combineAmmoOnReload = combineAmmoOnReload;
        this.ammoToUpperInventory = ammoToUpperInventory;
    }

    public void decrementReloadTime()
    {
        if(reloadTime > 0)
            reloadTime--;
    }

    public void doReload() {
        if (didReload || gunStack == null)
            return;
        didReload = true;
        GunItem gun = ((GunItem)gunStack.getItem());
        //TODO: uncomment
        //gun.reload(gunStack, gun.getConfigType(), level, entity, inventory, creative, forceReload, combineAmmoOnReload, ammoToUpperInventory, reloadTime, false);
    }
}
