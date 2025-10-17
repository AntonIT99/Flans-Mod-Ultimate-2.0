package com.wolffsarmormod.common.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelGun;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.ModConstants;
import com.wolffsarmormod.common.PlayerData;
import com.wolffsarmormod.common.guns.EnumSecondaryFunction;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.common.types.IScope;
import com.wolffsarmormod.common.types.PaintableType;
import com.wolffsarmormod.config.ModClientConfigs;
import com.wolffsarmormod.network.PacketHandler;
import com.wolffsarmormod.network.PacketPlaySound;
import com.wolffsarmormod.network.PacketReload;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class GunItem extends Item implements IModelItem<GunType, ModelGun>, IOverlayItem<GunType>, IPaintableItem<GunType>
{
    protected record FireDecision(boolean shouldShoot, boolean needsReload) {}

    protected static boolean rightMouseHeld;
    protected static boolean lastRightMouseHeld;
    protected static boolean leftMouseHeld;
    protected static boolean lastLeftMouseHeld;
    protected static final String NBT_TAG_AMMO = "ammo";

    @Getter
    protected final GunType configType;
    @Getter @Setter
    protected ModelGun model;
    @Getter @Setter
    protected ResourceLocation texture;
    @Setter
    protected ResourceLocation overlay;
    protected int soundDelay = 0;

    public GunItem(GunType configType)
    {
        super(new Properties());
        this.configType = configType;

        if (FMLEnvironment.dist == Dist.CLIENT)
            clientSideInit();
    }

    @Override
    public void clientSideInit()
    {
        loadModelAndTexture(null);
        loadOverlay();
    }

    @Override
    public boolean useCustomItemRendering()
    {
        return true;
    }

    @Override
    public Optional<ResourceLocation> getOverlay()
    {
        return Optional.ofNullable(overlay);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);
    }

    @Override
    public PaintableType GetPaintableType()
    {
        return configType;
    }

    /**
     * Deployable guns only
     */
    //TODO: implement
    /*@Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {

        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.pass(stack);
    }*/

    //TODO: Implement this -> ClientEventHandler.onLiving()
    /*
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // equivalent of EnumAction.BOW
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // typical “bow draw” duration, if needed
    }
    */

    /**
     * Generic update method. If we have an offhand weapon, it will also make calls for that.
     */
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
    {
        if (!(entity instanceof Player player))
            return;

        // Figure out which hand holds this stack.
        // Not held -> ignore
        InteractionHand hand;
        if (player.getMainHandItem() == stack)
            hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem() == stack)
            hand = InteractionHand.OFF_HAND;
        else
            return;

        // Client-side input read (only when no GUI is open)
        if (level.isClientSide && Minecraft.getInstance().screen == null)
        {
            Minecraft mc = Minecraft.getInstance();
            lastRightMouseHeld = rightMouseHeld;
            lastLeftMouseHeld  = leftMouseHeld;
            rightMouseHeld = mc.options.keyUse.isDown();
            leftMouseHeld  = mc.options.keyAttack.isDown();
        }

        boolean hasOffHand = !player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty();

        if (level.isClientSide)
            onUpdateClient(stack, slotId, level, player, hand, hasOffHand);
        else
            onUpdateServer(stack, slotId, level, player, hand, hasOffHand);

        //TODO from FMUltimate
        //checkForLockOn()
        //checkForMelee()
    }

    @OnlyIn(Dist.CLIENT)
    public void onUpdateClient(ItemStack gunstack, int gunSlot, Level level, @NotNull Player player, InteractionHand hand, boolean hasOffHand)
    {
        //TODO: implement FMU stuff

        // Not for deployables
        if (configType.isDeployable())
            return;

        // Scope handling
        handleScopeToggleIfNeeded(gunstack, hand, hasOffHand);

        // Grab per-player data and input edge
        PlayerData data = PlayerData.getInstance(player);
        data.setMinigunSpeed(data.getMinigunSpeed() * 0.9F); // slow down minigun each tick

        boolean hold = getMouseHeld(hand);
        boolean held = getLastMouseHeld(hand);

        // Don’t shoot certain entities under crosshair
        if (shouldBlockFireAtCrosshair())
            hold = false;

        // Idle sound (TODO: ideally server-side)
        playIdleSoundIfAny(level, player);

        if (!gunCanBeHandled(configType, player))
            return;
        if (!configType.isUsableByPlayers())
            return;

        // Fire-mode decision
        GunAnimations anim = ModClient.getGunAnimations(player, hand);
        FireDecision decision = computeFireDecision(gunstack, hand, data, hold, held, anim);

        if (decision.needsReload())
            PacketHandler.sendToServer(new PacketReload(hand, false));
        else if (decision.shouldShoot())
            shoot(hand, player, gunstack, data, level, anim);
    }

    /**
     * Used to determine if for example an player is holding a two handed gun but the other hand (the one without a gun) is holding something else
     * For example a player is holding two miniguns, a gun requiring both hands, so this method returns true
     *
     * @param type   The GunType of the gun
     * @param player The player who is handling the gun
     * @return if the player can handle the gun based on the contents of the main and off hand and the GunType
     */
    public boolean gunCanBeHandled(GunType type, Player player)
    {
        // We can always use a 1H gun
        if(type.isOneHanded())
            return true;

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
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

    protected static boolean getMouseHeld(InteractionHand hand)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.shootOnRightClick.get()))
            return hand == InteractionHand.MAIN_HAND ? rightMouseHeld : leftMouseHeld;
        else
            return hand == InteractionHand.MAIN_HAND ? leftMouseHeld : rightMouseHeld;
    }

    protected static boolean getLastMouseHeld(InteractionHand hand)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.shootOnRightClick.get()))
            return hand == InteractionHand.MAIN_HAND ? lastRightMouseHeld : lastLeftMouseHeld;
        else
            return hand == InteractionHand.MAIN_HAND ? lastLeftMouseHeld : lastRightMouseHeld;
    }

    @OnlyIn(Dist.CLIENT)
    protected void handleScopeToggleIfNeeded(ItemStack gunstack, InteractionHand hand, boolean hasOffHand)
    {
        if (hasOffHand)
            return;
        if (!(configType.getSecondaryFunction() == EnumSecondaryFunction.ADS_ZOOM || configType.getSecondaryFunction() == EnumSecondaryFunction.ZOOM))
            return;

        IScope scope = configType.getCurrentScope(gunstack);
        // Your original logic: press opposite hand to toggle scope
        switch (hand)
        {
            case MAIN_HAND -> {
                if (getMouseHeld(InteractionHand.OFF_HAND) && !getLastMouseHeld(InteractionHand.OFF_HAND))
                    ModClient.setScope(scope);
            }
            case OFF_HAND -> {
                if (getMouseHeld(InteractionHand.MAIN_HAND) && !getLastMouseHeld(InteractionHand.MAIN_HAND))
                    ModClient.setScope(scope);
            }
        }
    }

    protected boolean shouldBlockFireAtCrosshair()
    {
        Minecraft mc = Minecraft.getInstance();
        HitResult hr = mc.hitResult;
        if (!(hr instanceof EntityHitResult ehr))
            return false;
        Entity hit = ehr.getEntity();

        //TODO uncomment
        return false;
        /*return hit instanceof EntityFlagpole
            || hit instanceof EntityFlag
            || hit instanceof EntityGunItem
            || (hit instanceof EntityGrenade g && g.type.isDeployableBag);*/
    }

    protected void playIdleSoundIfAny(Level level, Player player)
    {
        if (soundDelay > 0 || configType.getIdleSound() == null)
            return;

        PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), ModConstants.soundRange, level.dimension(), configType.getIdleSound(), false);
        soundDelay = configType.getIdleSoundLength();
    }

    protected FireDecision computeFireDecision(ItemStack gunstack, InteractionHand hand, PlayerData data, boolean hold, boolean held, GunAnimations anim)
    {
        boolean needsToReload = needsToReload(gunstack);
        boolean shouldShoot = false;

        switch (configType.getFireMode(gunstack))
        {
            case BURST -> {
                // continue burst if rounds remain
                if (data.getBurstRoundsRemaining(hand) > 0)
                    shouldShoot = true;

                // then behave like SEMIAUTO for edge press
                if (hold && !held)
                    shouldShoot = true;
                else
                    needsToReload = false;
            }
            case SEMIAUTO -> {
                if (hold && !held)
                    shouldShoot = true;
                else
                    needsToReload = false;
            }
            case MINIGUN -> {
                // if empty, only reload while holding
                if (needsToReload)
                    return new FireDecision(false, hold);

                // spin-up
                if (hold) {
                    accelerateMinigun(data, anim);
                    if (data.getMinigunSpeed() < configType.getMinigunStartSpeed())
                    {
                        playMinigunWarmupIfNeeded(data);
                        return new FireDecision(false, false); // still warming up
                    }
                }
                // fall-through into FULLAUTO behavior
                shouldShoot = hold;
                handleMinigunLoopingSounds(data);
            }
            case FULLAUTO -> {
                shouldShoot = hold;
                if (!shouldShoot)
                    needsToReload = false;
                handleMinigunLoopingSounds(data);
            }
            default -> needsToReload = false;
        }

        return new FireDecision(shouldShoot, needsToReload);
    }

    protected boolean needsToReload(ItemStack stack)
    {
        for (int i = 0; i < configType.getNumAmmoItemsInGun(); i++)
        {
            ItemStack bulletStack = getBulletItemStack(stack, i);
            if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage())
            {
                return false;
            }
        }
        return true;
    }

    protected void accelerateMinigun(PlayerData data, GunAnimations anim)
    {
        if (data.getMinigunSpeed() < configType.getMinigunMaxSpeed())
        {
            data.setMinigunSpeed(2.0F);
            anim.addMinigunBarrelRotationSpeed(2.0F);
        }
    }

    protected void playMinigunWarmupIfNeeded(PlayerData data)
    {
        if (!configType.useLoopingSounds())
            return;
        if (data.getLoopedSoundDelay() > 0)
            return;
        if (data.getMinigunSpeed() <= 0.1F)
            return;
        if (data.isReloadingRight() || data.isSpinning())
            return;

        data.setLoopedSoundDelay(configType.getWarmupSoundLength());
        LocalPlayer p = Minecraft.getInstance().player;
        Level lvl = p.level();
        PacketPlaySound.sendSoundPacket(p.getX(), p.getY(), p.getZ(), ModConstants.soundRange, lvl.dimension(), configType.getWarmupSound(), false);
        data.setSpinning(true);
    }

    protected void handleMinigunLoopingSounds(PlayerData data) {
        if (!configType.useLoopingSounds())
            return;

        LocalPlayer p = Minecraft.getInstance().player;
        Level lvl = p.level();

        // play loop when above start speed
        if (data.getLoopedSoundDelay() <= 0 && data.getMinigunSpeed() > configType.getMinigunStartSpeed())
        {
            data.setLoopedSoundDelay(configType.getLoopedSoundLength());
            PacketPlaySound.sendSoundPacket(p.getX(), p.getY(), p.getZ(), ModConstants.soundRange, lvl.dimension(), configType.getLoopedSound(), false);
            data.setSpinning(true);
        }

        // cooldown when we drop below start speed
        if (data.isSpinning() && data.getMinigunSpeed() < configType.getMinigunStartSpeed())
        {
            PacketPlaySound.sendSoundPacket(p.getX(), p.getY(), p.getZ(), ModConstants.soundRange, lvl.dimension(), configType.getCooldownSound(), false);
            data.setSpinning(true);
        }
    }

    public void onUpdateServer(ItemStack gunstack, int gunSlot, Level level, @NotNull Player player, InteractionHand hand, boolean hasOffHand)
    {
        //TODO
    }

    /**
     * Get the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets).
     */
    public ItemStack getBulletItemStack(ItemStack gun, int id) {
        if (gun.isEmpty())
            return ItemStack.EMPTY;

        CompoundTag tag = gun.getTag();
        if (tag == null)
        {
            gun.setTag(new CompoundTag());
            return ItemStack.EMPTY;
        }

        if (!tag.contains(NBT_TAG_AMMO, Tag.TAG_LIST))
        {
            // init empty slots
            ListTag list = new ListTag();
            for (int i = 0; i < configType.getNumAmmoItemsInGun(); i++)
                list.add(new CompoundTag());
            tag.put(NBT_TAG_AMMO, list);
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(NBT_TAG_AMMO, Tag.TAG_COMPOUND);
        if (id < 0 || id >= list.size())
            return ItemStack.EMPTY;

        CompoundTag slotTag = list.getCompound(id);
        return ItemStack.of(slotTag); // empty tag -> EMPTY stack
    }

    /**
     * Set the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets).
     */
    public void setBulletItemStack(ItemStack gun, ItemStack bullet, int id) {
        if (gun.isEmpty() || id < 0)
            return;

        CompoundTag tag = gun.getOrCreateTag();

        ListTag list;
        if (tag.contains(NBT_TAG_AMMO, Tag.TAG_LIST))
        {
            list = tag.getList(NBT_TAG_AMMO, Tag.TAG_COMPOUND);
        }
        else
        {
            list = new ListTag();
            for (int i = 0; i < configType.getNumAmmoItemsInGun(); i++) list.add(new CompoundTag());
            tag.put(NBT_TAG_AMMO, list);
        }

        // ensure index exists
        while (id >= list.size())
            list.add(new CompoundTag());

        // Represent empty slots by an empty CompoundTag
        CompoundTag slotTag = (bullet == null || bullet.isEmpty()) ? new CompoundTag() : bullet.save(new CompoundTag());

        list.set(id, slotTag);
        tag.put(NBT_TAG_AMMO, list); // write back (harmless if unchanged)
    }

    public void shoot(InteractionHand hand, Player player, ItemStack gunstack, PlayerData data, Level level, GunAnimations animations)
    {
        //TODO: implement
        /*if(type.usableByPlayers)
        {
            float shootTime = data.GetShootTime(hand);

            ItemStack otherHand = null;
            if(hand == EnumHand.MAIN_HAND)
                otherHand = player.getHeldItemOffhand();
            else otherHand = player.getHeldItemMainhand();

            if (!world.isRemote && shootTime > 0f)
            {
                //data.addToQueue(hand);
                //Hacky code
                //This essentially skips ticks for a smoother client experience
                if (shootTime < 4)
                {
                    while (shootTime > 0)
                    {
                        shootTime--;
                    }
                }
            }

            if (world.isRemote && shootTime <= 0)
                //Send the server the instruction to shoot
                FlansMod.getPacketHandler().sendToServer(new PacketGunFire(hand));

            // For each
            while(shootTime <= 0.0f)
            {

                // Add the delay for this shot and shoot it!
                shootTime += type.GetShootDelay(gunstack);

                int bulletID = 0;
                ItemStack bulletStack = ItemStack.EMPTY.copy();
                for(; bulletID < type.numAmmoItemsInGun; bulletID++)
                {
                    ItemStack checkingStack = getBulletItemStack(gunstack, bulletID);
                    if(checkingStack != null && checkingStack.getItemDamage() < checkingStack.getMaxDamage())
                    {
                        bulletStack = checkingStack;
                        break;
                    }
                }

                if(bulletStack.isEmpty())
                {
                    continue;
                }

                final ItemStack bullet = bulletStack;
                final Integer bulletid = bulletID;

                ItemShootable shootableItem = (ItemShootable)bulletStack.getItem();
                ShootableType shootableType = shootableItem.type;
                Vector3f rayTraceOrigin = new Vector3f(player.getPositionEyes(0.0f));

                ShootBulletHandler handler = isExtraBullet ->
                {
                    if(!isExtraBullet)
                    {
                        // Drop item on shooting if bullet requires it
                        if(shootableType.dropItemOnShoot != null && !player.capabilities.isCreativeMode)
                            dropItem(world, player, shootableType.dropItemOnShoot);
                        // Drop item on shooting if gun requires it
                        if(type.dropItemOnShoot != null)// && !entityplayer.capabilities.isCreativeMode)
                            dropItem(world, player, type.dropItemOnShoot);

                        if(type.knockback > 0)
                        {
                            //TODO : Apply knockback
                        }

                        //Damage the bullet item
                        bullet.setItemDamage(bullet.getItemDamage() + 1);

                        //Update the stack in the gun
                        setBulletItemStack(gunstack, bullet, bulletid);

                        if(type.consumeGunUponUse)
                        {
                            player.setHeldItem(hand, ItemStack.EMPTY.copy());
                        }
                    }
                };

                if (world.isRemote)
                {

                    Integer bulletAmount = type.numBullets*shootableType.numBullets;
                    for(int i = 0; i < bulletAmount; i++)
                    {
                        //Smooth effects, no need to wait for the server response
                        handler.shooting(i < bulletAmount - 1);
                    }

                    animations.doShoot(type.getPumpDelay(), type.getPumpTime());
                    Float recoil = type.getRecoil(gunstack);
                    FlansModClient.playerRecoil += recoil;
                    animations.recoil += recoil;

                } else
                {
                    Vector3f rayTraceDirection = new Vector3f(player.getLookVec());

                    if (shootableType instanceof BulletType)
                    {
                        //Fire gun
                        FireableGun fireableGun =
                                new FireableGun(type,
                                        type.getDamage(gunstack),
                                        type.getSpread(gunstack),
                                        type.bulletSpeed,
                                        type.getSpreadPattern(gunstack));

                        if(otherHand.getItem() instanceof ItemShield || otherHand.getItem() instanceof ItemGlove)
                        {
                            EnchantmentModule.ModifyGun(fireableGun, player, otherHand);
                        }

                        FiredShot shot = new FiredShot(fireableGun, (BulletType)shootableType, (EntityPlayerMP)player);
                        //TODO gunOrigin? & animation origin
                        ShotHandler.fireGun(world, shot, type.numBullets*shootableType.numBullets, rayTraceOrigin, rayTraceDirection, handler);
                    }
                    else if (shootableType instanceof GrenadeType)
                    {
                        //throw grenade
                        ItemGrenade grenade = (ItemGrenade) shootableItem;
                        grenade.throwGrenade(world, player);
                        handler.shooting(false);
                    }

                    boolean silenced = type.getBarrel(gunstack) != null && type.getBarrel(gunstack).silencer;
                    playShotSound(world, rayTraceOrigin, silenced);
                }
                int gunSlot = player.inventory.currentItem;
                if(type.consumeGunUponUse)
                    player.inventory.setInventorySlotContents(gunSlot, ItemStack.EMPTY.copy());
            }
            data.SetShootTime(hand, shootTime);
        }*/
    }

    public boolean reload(ItemStack gunstack, Level level, Entity entity, Inventory inventory, InteractionHand hand, boolean hasOffHand, boolean forceReload, boolean isCreative)
    {
        return true;
        /*
        //Deployable guns cannot be reloaded in the inventory

        //TODO investigate if this code can can actually be called by an deployable
        if(type.deployable)
            return false;

        //If you cannot reload half way through a clip, reject the player for trying to do so
        if(forceReload && !type.canForceReload)
            return false;

        //For playing sounds afterwards
        boolean reloadedSomething = false;
        //Check each ammo slot, one at a time
        for(int i = 0; i < type.numAmmoItemsInGun; i++)
        {
            //Get the stack in the slot
            ItemStack bulletStack = getBulletItemStack(gunstack, i);

            //If there is no magazine, if the magazine is empty or if this is a forced reload
            if(bulletStack == null || bulletStack.isEmpty() || bulletStack.getItemDamage() == bulletStack.getMaxDamage() || forceReload)
            {
                //Iterate over all inventory slots and find the magazine / bullet item with the most bullets
                int bestSlot = -1;
                int bulletsInBestSlot = 0;
                for(int j = 0; j < inventory.getSizeInventory(); j++)
                {
                    ItemStack item = inventory.getStackInSlot(j);
                    if(item.getItem() instanceof ItemShootable && type.isCorrectAmmo(((ItemShootable)(item.getItem())).type))
                    {
                        int bulletsInThisSlot = item.getMaxDamage() - item.getItemDamage();
                        if(bulletsInThisSlot > bulletsInBestSlot)
                        {
                            bestSlot = j;
                            bulletsInBestSlot = bulletsInThisSlot;
                        }
                    }
                }
                //If there was a valid non-empty magazine / bullet item somewhere in the inventory, load it
                if(bestSlot != -1)
                {
                    ItemStack newBulletStack = inventory.getStackInSlot(bestSlot);
                    ShootableType newBulletType = ((ItemShootable)newBulletStack.getItem()).type;

                    //Unload the old magazine (Drop an item if it is required and the player is not in creative mode)
                    if(bulletStack != null && bulletStack.getItem() instanceof ItemShootable && ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload != null && !isCreative && bulletStack.getItemDamage() == bulletStack.getMaxDamage())
                    {
                        if(!world.isRemote)
                            dropItem(world, entity, ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload);
                    }

                    //The magazine was not finished, pull it out and give it back to the player or, failing that, drop it
                    if(bulletStack != null && !bulletStack.isEmpty() && bulletStack.getItemDamage() < bulletStack.getMaxDamage())
                    {
                        if(!InventoryHelper.addItemStackToInventory(inventory, bulletStack, isCreative))
                        {
                            if(!world.isRemote)
                                entity.entityDropItem(bulletStack, 0.5F);
                        }
                    }

                    //Load the new magazine
                    ItemStack stackToLoad = newBulletStack.copy();
                    stackToLoad.setCount(1);
                    setBulletItemStack(gunstack, stackToLoad, i);

                    //Remove the magazine from the inventory
                    if(!isCreative)
                        newBulletStack.setCount(newBulletStack.getCount() - 1);
                    if(newBulletStack.getCount() <= 0)
                        newBulletStack = ItemStack.EMPTY.copy();
                    inventory.setInventorySlotContents(bestSlot, newBulletStack);


                    //Tell the sound player that we reloaded something
                    reloadedSomething = true;
                }
            }
        }
        return reloadedSomething;
         */
    }
}
