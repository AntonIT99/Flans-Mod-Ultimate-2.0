package com.flansmodultimate.common.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.guns.DefaultShootingHandler;
import com.flansmodultimate.common.guns.EnumSecondaryFunction;
import com.flansmodultimate.common.guns.InventoryHelper;
import com.flansmodultimate.common.guns.ShootingHandler;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IScope;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketGunFire;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.PacketPlaySound;
import net.minecraftforge.fml.LogicalSide;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

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

        PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, item.configType.getIdleSound(), false);
        item.soundDelay = item.configType.getIdleSoundLength();
    }

    public void shoot(InteractionHand hand, Player player, ItemStack gunStack, PlayerData data, Level level, GunAnimations animations)
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
            shootTime += configType.getShootDelay(gunStack);

            Optional<AmmoSlot> slot = findUsableAmmo(item, gunStack, configType);

            if (slot.isEmpty())
                continue;

            ShootableItem shootableItem = (ShootableItem) slot.get().stack().getItem();
            ShootableType shootableType = shootableItem.getConfigType();
            Vector3f rayTraceOrigin = new Vector3f(player.getEyePosition(0.0F));
            ShootingHandler handler = new DefaultShootingHandler(level, player, gunStack, hand, slot.get());

            //TODO: probably needs refactoring
            if (level.isClientSide)
            {
                int bulletAmount = configType.getNumBullets() * shootableType.getNumBullets();
                for (int i = 0; i < bulletAmount; i++)
                {
                    //Smooth effects, no need to wait for the server response
                    handler.shooting(i < bulletAmount - 1);
                }

                animations.doShoot(configType.getPumpDelay(), configType.getPumpTime());
                float recoil = configType.getRecoil(gunStack);
                ModClient.setPlayerRecoil(ModClient.getPlayerRecoil() + recoil);
                animations.recoil += recoil;
            }
            else
            {
                //TODO gunOrigin? & animation origin
                ShootingHelper.fireGun(level, player, configType, shootableType, gunStack, otherHand, handler);
                boolean silenced = Optional.ofNullable(configType.getBarrel(gunStack)).map(AttachmentType::isSilencer).orElse(false);
                playShotSound(level, rayTraceOrigin, silenced);
            }

            if (configType.isConsumeGunUponUse())
                player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        }
        data.setShootTime(hand, shootTime);

        DebugHelper.spawnDebugDot(level, FlansModRaytracer.getPlayerMuzzlePosition(player, hand), 1000);
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
        for (int i = 0; i < configType.getNumAmmoItemsInGun(gunstack); i++)
        {
            //Get the stack in the slot
            ItemStack bulletStack = item.getBulletItemStack(gunstack, i);

            //If there is no magazine, if the magazine is empty or if this is a forced reload
            if (bulletStack == null || bulletStack.isEmpty() || bulletStack.getDamageValue() == bulletStack.getMaxDamage() || forceReload)
            {
                //Iterate over all inventory slots and find the magazine / bullet item with the most bullets
                int bestSlot = -1;
                int bulletsInBestSlot = 0;

                List<ShootableType> allowedAmmoTypes = item.getConfigType().getAmmoTypes();

                for (int j = 0; j < inventory.getContainerSize(); j++)
                {
                    ItemStack item = inventory.getItem(j);
                    if (item.isEmpty())
                        continue;
                    if (item.getItem() instanceof ShootableItem shootableItem && allowedAmmoTypes.contains(shootableItem.getConfigType()))
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
                    if (bulletStack != null && bulletStack.getItem() instanceof ShootableItem shootableItem && !isCreative && bulletStack.getDamageValue() == bulletStack.getMaxDamage())
                    {
                        dropItem(level, entity, shootableItem.getConfigType().getDropItemOnReload(), shootableItem.getConfigType().getContentPack());
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
            PacketPlaySound.sendSoundPacket(position.x, position.y, position.z, FlansMod.SOUND_RANGE, level.dimension(), item.configType.getShootSound(), silenced);
            item.soundDelay = item.configType.getIdleSoundLength();
        }
    }

    /**
     * Method for dropping items on reload and on shoot
     */
    public static void dropItem(Level level, Entity entity, @Nullable String itemName, IContentProvider contentPack)
    {
        if (!level.isClientSide && StringUtils.isNotBlank(itemName))
        {
            ItemStack dropStack = InfoType.getRecipeElement(itemName, contentPack);
            entity.spawnAtLocation(dropStack, 0.5F);
        }
    }

    public boolean canReload(Container inventory)
    {
        List<ShootableType> allowedAmmoTypes = item.getConfigType().getAmmoTypes();

        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof ShootableItem shootableItem && allowedAmmoTypes.contains(shootableItem.getConfigType()))
                return true;
        }
        return false;
    }

    public record AmmoSlot(int index, ItemStack stack) {}

    public static Optional<AmmoSlot> findUsableAmmo(GunItem item, ItemStack gunStack, GunType configType)
    {
        int slots = configType.getNumAmmoItemsInGun(gunStack);
        for (int i = 0; i < slots; i++)
        {
            ItemStack s = item.getBulletItemStack(gunStack, i);
            if (s != null && !s.isEmpty() && s.getDamageValue() < s.getMaxDamage())
            {
                return Optional.of(new AmmoSlot(i, s));
            }
        }
        return Optional.empty();
    }
}
