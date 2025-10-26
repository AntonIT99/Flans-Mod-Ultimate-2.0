package com.wolffsarmormod.common.item;

import com.wolffsarmormod.IContentProvider;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.ModConstants;
import com.wolffsarmormod.client.anim.GunAnimations;
import com.wolffsarmormod.common.PlayerData;
import com.wolffsarmormod.common.entity.Grenade;
import com.wolffsarmormod.common.guns.EnumSecondaryFunction;
import com.wolffsarmormod.common.guns.FireableGun;
import com.wolffsarmormod.common.guns.FiredShot;
import com.wolffsarmormod.common.guns.InventoryHelper;
import com.wolffsarmormod.common.guns.ShootBulletHandler;
import com.wolffsarmormod.common.guns.ShotHandler;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsarmormod.common.types.GrenadeType;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.common.types.IScope;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.ShootableType;
import com.wolffsarmormod.common.vector.Vector3f;
import com.wolffsarmormod.network.PacketGunFire;
import com.wolffsarmormod.network.PacketHandler;
import com.wolffsarmormod.network.PacketPlaySound;
import net.minecraftforge.fml.LogicalSide;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public record GunItemBehavior(GunItem item)
{
    /**
     * Used to determine if, for example, a player is holding a two-handed gun but the other hand (the one without a gun) is holding something else
     * For example a player is holding two miniguns, a gun requiring both hands, so this method returns true
     *
     * @param player The player who is handling the gun
     * @return if the player can handle the gun based on the contents of the main and off hand and the GunType
     */
    public boolean gunCanBeHandled(Player player)
    {
        // We can always use a 1H gun
        if(item.configType.isOneHanded())
            return true;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean hasItemInBothHands = !main.isEmpty() && !off.isEmpty();

        if (hasItemInBothHands)
        {
            return false;
            // TODO: implement gloves
            // Gloves are special enchantable items that can be placed in the offhand while still letting you shoot 2H
            //return off.getItem() instanceof ItemGlove;
        }

        return true;
    }

    public void handleScopeToggleIfNeeded(ItemStack gunstack, InteractionHand hand, boolean hasOffHand)
    {
        if (hasOffHand)
            return;
        if (!(item.configType.getSecondaryFunction() == EnumSecondaryFunction.ADS_ZOOM || item.configType.getSecondaryFunction() == EnumSecondaryFunction.ZOOM))
            return;

        IScope scope = item.configType.getCurrentScope(gunstack);
        // Your original logic: press opposite hand to toggle scope

        if ((hand == InteractionHand.MAIN_HAND && GunItem.getMouseHeld(InteractionHand.OFF_HAND) && !GunItem.getLastMouseHeld(InteractionHand.OFF_HAND))
                || (hand == InteractionHand.OFF_HAND && GunItem.getMouseHeld(InteractionHand.MAIN_HAND) && !GunItem.getLastMouseHeld(InteractionHand.MAIN_HAND)))
            ModClient.setScope(scope);
    }

    public boolean shouldBlockFireAtCrosshair()
    {
        Minecraft mc = Minecraft.getInstance();
        HitResult hr = mc.hitResult;
        if (!(hr instanceof EntityHitResult ehr))
            return false;
        Entity hit = ehr.getEntity();

        return hit instanceof Grenade grenade && grenade.getConfigType().isDeployableBag();

        //TODO uncomment
        /*return hit instanceof EntityFlagpole
            || hit instanceof EntityFlag
            || hit instanceof EntityGunItem
            || (hit instanceof EntityGrenade g && g.type.isDeployableBag);*/
    }

    public void playIdleSoundIfAny(Level level, Player player)
    {
        if (item.soundDelay > 0 || item.configType.getIdleSound() == null)
            return;

        PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), ModConstants.SOUND_RANGE, level.dimension(), item.configType.getIdleSound(), false);
        item.soundDelay = item.configType.getIdleSoundLength();
    }

    public void shoot(InteractionHand hand, Player player, ItemStack gunstack, PlayerData data, Level level, GunAnimations animations)
    {
        GunType configType = item.getConfigType();

        if (!configType.isUsableByPlayers())
            return;

        float shootTime = data.getShootTime(hand);

        ItemStack otherHand;
        if (hand == InteractionHand.MAIN_HAND)
            otherHand = player.getOffhandItem();
        else
            otherHand = player.getMainHandItem();

        // This essentially skips ticks for a smoother client experience
        if (!level.isClientSide && shootTime > 0F && shootTime < 4F)
        {
            while (shootTime > 0F)
            {
                shootTime--;
            }
        }

        //Send the server the instruction to shoot
        if (level.isClientSide && shootTime <= 0F)
            PacketHandler.sendToServer(new PacketGunFire(hand));

        while (shootTime <= 0F)
        {
            // Add the delay for this shot and shoot it!
            shootTime += configType.getShootDelay(gunstack);

            int bulletID = 0;
            ItemStack bulletStack = ItemStack.EMPTY;
            for (; bulletID < configType.getNumAmmoItemsInGun(); bulletID++)
            {
                ItemStack checkingStack = item.getBulletItemStack(gunstack, bulletID);
                if (checkingStack != null && checkingStack.getDamageValue() < checkingStack.getMaxDamage())
                {
                    bulletStack = checkingStack;
                    break;
                }
            }

            if (bulletStack.isEmpty())
                continue;

            final ItemStack bullet = bulletStack;
            final int bulletid = bulletID;

            ShootableItem shootableItem = (ShootableItem) bulletStack.getItem();
            ShootableType shootableType = shootableItem.getConfigType();
            Vector3f rayTraceOrigin = new Vector3f(player.getEyePosition(0.0F));

            ShootBulletHandler handler = isExtraBullet -> {
                if (!isExtraBullet)
                {
                    // Drop item on shooting if bullet requires it
                    if (!player.isCreative())
                        dropItem(level, player, shootableType.getDropItemOnShoot(), shootableType.getContentPack());

                    // Drop item on shooting if gun requires it
                    dropItem(level, player, configType.getDropItemOnShoot(), configType.getContentPack());

                    //TODO : Apply knockback
                    //if (configType.getKnockback() > 0F) {}

                    //Damage the bullet item
                    bullet.setDamageValue(bullet.getDamageValue() + 1);

                    //Update the stack in the gun
                    item.setBulletItemStack(gunstack, bullet, bulletid);

                    if (configType.isConsumeGunUponUse())
                        player.setItemInHand(hand, ItemStack.EMPTY);
                }
            };

            if (level.isClientSide)
            {

                int bulletAmount = configType.getNumBullets() * shootableType.getNumBullets();
                for(int i = 0; i < bulletAmount; i++)
                {
                    //Smooth effects, no need to wait for the server response
                    handler.shooting(i < bulletAmount - 1);
                }

                animations.doShoot(configType.getPumpDelay(), configType.getPumpTime());
                float recoil = configType.getRecoil(gunstack);
                ModClient.setPlayerRecoil(ModClient.getPlayerRecoil() + recoil);
                animations.recoil += recoil;

            }
            else
            {
                Vector3f rayTraceDirection = new Vector3f(player.getLookAngle());

                if (shootableType instanceof BulletType bulletType)
                {
                    //Fire gun
                    FireableGun fireableGun = new FireableGun(configType, configType.getDamage(gunstack), configType.getSpread(gunstack), configType.getBulletSpeed(), configType.getSpreadPattern(gunstack));

                    //TODO: enchantments & gloves
                    /*if (otherHand.getItem() instanceof ShieldItem || otherHand.getItem() instanceof ItemGlove)
                    {
                        EnchantmentModule.ModifyGun(fireableGun, player, otherHand);
                    }*/

                    FiredShot shot = new FiredShot(fireableGun, bulletType, (ServerPlayer)player);
                    //TODO gunOrigin? & animation origin
                    ShotHandler.fireGun(level, shot, configType.getNumBullets() * shootableType.getNumBullets(), rayTraceOrigin, rayTraceDirection, handler);
                }
                else if (shootableType instanceof GrenadeType)
                {
                    //throw grenade
                    GrenadeItem grenade = (GrenadeItem) shootableItem;
                    grenade.throwGrenade(level, player);
                    handler.shooting(false);
                }

                boolean silenced = configType.getBarrel(gunstack) != null && configType.getBarrel(gunstack).isSilencer();
                playShotSound(level, rayTraceOrigin, silenced);
            }

            if (configType.isConsumeGunUponUse())
                player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        }
        data.setShootTime(hand, shootTime);
    }

    public void shootServer(InteractionHand hand, ServerPlayer player, ItemStack gunstack)
    {
        // Get useful objects
        PlayerData data = PlayerData.getInstance(player, LogicalSide.SERVER);
        Level level = player.serverLevel();

        // This code is not for deployables
        if (item.configType.isDeployable())
            return;

        if (!gunCanBeHandled(player))
            return;

        shoot(hand, player, gunstack, data, level, null);

        //TODO: Debug Mode
        /*if (FlansMod.DEBUG)
        {
            Vector3f gunOrigin = FlansModRaytracer.GetPlayerMuzzlePosition(player, hand);
            world.spawnEntity(new EntityDebugDot(world, gunOrigin, 100, 1.0f, 1.0f, 1.0f));
        }*/
    }

    public boolean reload(ItemStack gunstack, Level level, Entity entity, Inventory inventory, InteractionHand hand, boolean hasOffHand, boolean forceReload, boolean isCreative)
    {
        GunType configType = item.getConfigType();

        //Deployable guns cannot be reloaded in the inventory

        //TODO investigate if this code can can actually be called by an deployable
        if (configType.isDeployable())
            return false;

        //If you cannot reload half way through a clip, reject the player for trying to do so
        if (forceReload && !configType.isCanForceReload())
            return false;

        //For playing sounds afterwards
        boolean reloadedSomething = false;
        //Check each ammo slot, one at a time
        for (int i = 0; i < configType.getNumAmmoItemsInGun(); i++)
        {
            //Get the stack in the slot
            ItemStack bulletStack = item.getBulletItemStack(gunstack, i);

            //If there is no magazine, if the magazine is empty or if this is a forced reload
            if (bulletStack == null || bulletStack.isEmpty() || bulletStack.getDamageValue() == bulletStack.getMaxDamage() || forceReload)
            {
                //Iterate over all inventory slots and find the magazine / bullet item with the most bullets
                int bestSlot = -1;
                int bulletsInBestSlot = 0;
                for (int j = 0; j < inventory.getContainerSize(); j++)
                {
                    ItemStack item = inventory.getItem(j);
                    if (item.getItem() instanceof ShootableItem shootableItem && configType.isCorrectAmmo(shootableItem.getConfigType()))
                    {
                        int bulletsInThisSlot = item.getMaxDamage() - item.getDamageValue();
                        if (bulletsInThisSlot > bulletsInBestSlot)
                        {
                            bestSlot = j;
                            bulletsInBestSlot = bulletsInThisSlot;
                        }
                    }
                }
                //If there was a valid non-empty magazine / bullet item somewhere in the inventory, load it
                if (bestSlot != -1)
                {
                    ItemStack newBulletStack = inventory.getItem(bestSlot);

                    //Unload the old magazine (Drop an item if it is required and the player is not in creative mode)
                    if (bulletStack != null && bulletStack.getItem() instanceof ShootableItem shootableItem && shootableItem.getConfigType().getDropItemOnReload() != null && !isCreative && bulletStack.getDamageValue() == bulletStack.getMaxDamage())
                    {
                        dropItem(level, entity, ((ShootableItem) bulletStack.getItem()).getConfigType().getDropItemOnReload(), ((ShootableItem) bulletStack.getItem()).getConfigType().getContentPack());
                    }

                    //The magazine was not finished, pull it out and give it back to the player or, failing that, drop it
                    if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage() && !InventoryHelper.addItemStackToInventory(inventory, bulletStack, isCreative))
                    {
                        entity.spawnAtLocation(bulletStack, 0.5F);
                    }

                    //Load the new magazine
                    ItemStack stackToLoad = newBulletStack.copy();
                    stackToLoad.setCount(1);
                    item.setBulletItemStack(gunstack, stackToLoad, i);

                    //Remove the magazine from the inventory
                    if(!isCreative)
                        newBulletStack.setCount(newBulletStack.getCount() - 1);
                    if(newBulletStack.getCount() <= 0)
                        newBulletStack = ItemStack.EMPTY.copy();
                    inventory.setItem(bestSlot, newBulletStack);

                    //Tell the sound player that we reloaded something
                    reloadedSomething = true;
                }
            }
        }
        return reloadedSomething;
    }

    private void playShotSound(Level level, Vector3f position, Boolean silenced) {
        // Play shot sounds
        if (item.soundDelay <= 0 && item.configType.getShootSound() != null)
        {
            PacketPlaySound.sendSoundPacket(position.x, position.y, position.z, ModConstants.SOUND_RANGE, level.dimension(), item.configType.getShootSound(), silenced);
            item.soundDelay = item.configType.getIdleSoundLength();
        }
    }

    /**
     * Method for dropping items on reload and on shoot
     */
    private static void dropItem(Level level, Entity entity, @Nullable String itemName, IContentProvider contentPack)
    {
        if (!level.isClientSide && itemName != null)
        {
            ItemStack dropStack = InfoType.getRecipeElement(itemName, contentPack);
            entity.spawnAtLocation(dropStack, 0.5F);
        }
    }
}
