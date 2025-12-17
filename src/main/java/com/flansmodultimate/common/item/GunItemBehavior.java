package com.flansmodultimate.common.item;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.guns.DefaultShootingHandler;
import com.flansmodultimate.common.guns.EnumFireDecision;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.EnumSecondaryFunction;
import com.flansmodultimate.common.guns.InventoryHelper;
import com.flansmodultimate.common.guns.ShootingHandler;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.EnumHitboxType;
import com.flansmodultimate.common.raytracing.FlansModRaytracer;
import com.flansmodultimate.common.raytracing.PlayerHitbox;
import com.flansmodultimate.common.raytracing.PlayerSnapshot;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IScope;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModClientConfigs;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunReloadClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
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
        if (item.configType.isOneHanded())
            return true;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean dualWield = !main.isEmpty() && !off.isEmpty();

        if (dualWield)
        {
            return false;
            // TODO: implement gloves
            // Gloves are special enchantable items that can be placed in the offhand while still letting you shoot 2H
            //return off.getItem() instanceof ItemGlove;
        }

        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleScope(ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        if (dualWield)
            return;
        if (item.configType.getSecondaryFunction() != EnumSecondaryFunction.ADS_ZOOM && item.configType.getSecondaryFunction() != EnumSecondaryFunction.ZOOM)
            return;

        IScope scope = null;
        EnumAimType aimType = ModClientConfigs.aimType.get();
        if (aimType == EnumAimType.HOLD)
        {
            scope = GunInputState.isAimPressed() ? item.configType.getCurrentScope(gunStack) : null;
        }
        else if (aimType == EnumAimType.TOGGLE)
        {
            scope = ModClient.getCurrentScope();
            if (GunInputState.isAimPressed() && !GunInputState.isPrevAimPressed())
                scope = (scope == null) ? item.configType.getCurrentScope(gunStack) : null;
        }

        ModClient.updateScope(scope, gunStack, item);
    }

    @OnlyIn(Dist.CLIENT)
    public void handleGunSwitchDelay(@NotNull PlayerData data, @NotNull GunAnimations animations, InteractionHand hand)
    {
        float animationLength = item.configType.getSwitchDelay();
        if (animationLength == 0)
        {
            animations.switchAnimationLength = animations.switchAnimationProgress = 0;
        }
        else
        {
            animations.switchAnimationProgress = 1;
            animations.switchAnimationLength = animationLength;
            ModClient.setSwitchTime(Math.max(ModClient.getSwitchTime(), animationLength));

            //TODO: data should be also updated on Server
            data.setShootTime(hand, Math.max(data.getShootTime(hand), animationLength));
        }
    }

    public boolean shouldBlockFireAtCrosshair()
    {
        HitResult hr = Minecraft.getInstance().hitResult;
        if (!(hr instanceof EntityHitResult ehr))
            return false;

        // Do not shoot ammo bags, flags or dropped gun items
        Entity entity = ehr.getEntity();
        return (entity instanceof Flagpole
                || entity instanceof Flag
                || entity instanceof GunItemEntity
                || entity instanceof Grenade grenade && grenade.getConfigType().isDeployableBag());
    }

    public void doPlayerShoot(Level level, ServerPlayer player, PlayerData data, ItemStack gunStack, InteractionHand hand)
    {
        //TODO: here only server code
        if (item.configType.isDeployable() || !item.configType.isUsableByPlayers() || !gunCanBeHandled(player))
            return;

        data.setShooting(hand, true);

        GunType gunType = item.getConfigType();

        float shootTime = data.getShootTime(hand);

        // This essentially skips ticks for a smoother client experience
        if (!level.isClientSide && shootTime > 0F && shootTime < 4F)
        {
            shootTime = 0F;
        }

        //Send the server the instruction to shoot
        //if (level.isClientSide && shootTime <= 0F)
        //    PacketHandler.sendToServer(new PacketGunShoot(hand));

        while (shootTime <= 0F)
        {
            // Add the delay for this shot and shoot it!
            shootTime += gunType.getShootDelay(gunStack);

            Optional<AmmoSlot> slot = findUsableAmmo(item, gunStack, gunType);

            if (slot.isEmpty())
                continue;

            ItemStack shootableStack = slot.get().stack();
            ShootableItem shootableItem = (ShootableItem) slot.get().stack().getItem();
            ShootableType shootableType = shootableItem.getConfigType();
            ShootingHandler handler = new DefaultShootingHandler(level, player, gunStack, hand, slot.get());

            //TODO: probably needs refactoring
            if (level.isClientSide)
            {
                int bulletAmount = gunType.getNumBullets() * shootableType.getNumBullets();
                for (int i = 0; i < bulletAmount; i++)
                {
                    //Smooth effects, no need to wait for the server response
                    handler.shooting(i < bulletAmount - 1);
                }

                //TODO: fix recoil
                float recoil = gunType.getRecoilPitch(gunStack, false, false);
                ModClient.setPlayerRecoil(ModClient.getPlayerRecoil() + recoil);

                //TODO: move to client shoot code
                /*if (animations != null)
                {
                    animations.doShoot(gunType.getPumpDelay(), gunType.getPumpTime());
                    animations.recoil += recoil;
                }*/
            }
            else
            {
                //TODO gunOrigin? & animation origin
                ShootingHelper.fireGun(level, player, gunType, shootableType, gunStack, shootableStack, (hand == InteractionHand.MAIN_HAND) ? player.getOffhandItem() : player.getMainHandItem(), handler);
                boolean silenced = Optional.ofNullable(gunType.getBarrel(gunStack)).map(AttachmentType::isSilencer).orElse(false);

                // Play shot sounds
                if (StringUtils.isNotBlank(item.configType.getShootSound()))
                {
                    PacketPlaySound.sendSoundPacket(player.getEyePosition(), FlansMod.SOUND_RANGE, level.dimension(), item.configType.getShootSound(), silenced);
                    item.soundDelay = item.configType.getShootSoundLength();
                }

            }

            if (gunType.isConsumeGunUponUse())
                player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        }
        data.setShootTime(hand, shootTime);

        if (level.isClientSide)
            DebugHelper.spawnDebugDot(level, FlansModRaytracer.getPlayerMuzzlePosition(player, hand), 1000);
    }

    public EnumFireDecision computeFireDecision(PlayerData data, ItemStack gunStack, InteractionHand hand)
    {
        GunType type = item.getConfigType();
        EnumFireMode mode = type.getFireMode(gunStack);
        boolean emptyAmmo = hasEmptyAmmo(gunStack);
        boolean shootPressed = data.isShootKeyPressed(hand);
        boolean shootEdgePressed = data.isShootKeyPressed(hand) && !data.isPrevShootKeyPressed(hand);
        boolean actionRequested = switch (mode)
        {
            case FULLAUTO, MINIGUN -> shootPressed;
            case SEMIAUTO -> shootEdgePressed;
            case BURST -> shootEdgePressed || data.getBurstRoundsRemaining(hand) > 0;
        };

        if (!actionRequested)
            return EnumFireDecision.NO_ACTION;
        if (emptyAmmo)
            return EnumFireDecision.RELOAD;
        if (mode == EnumFireMode.MINIGUN)
            return (data.getMinigunSpeed() >= type.getMinigunStartSpeed()) ? EnumFireDecision.SHOOT : EnumFireDecision.NO_ACTION;
        return EnumFireDecision.SHOOT;
    }

    public boolean hasEmptyAmmo(ItemStack gunStack)
    {
        for (ItemStack bulletStack : item.getBulletItemStackList(gunStack))
        {
            if (bulletStack.getDamageValue() < bulletStack.getMaxDamage())
                return false;
        }
        return true;
    }

    public void handleMinigunEffects(Level level, Player player, PlayerData data, EnumFireMode mode, InteractionHand hand)
    {
        accelerateMinigun(level, player, data, mode, hand);
        handleMinigunLoopingSounds(level, player, data, mode, hand);
    }

    private void accelerateMinigun(Level level, Player player, PlayerData data, EnumFireMode mode, InteractionHand hand)
    {
        if (data.isShootKeyPressed(hand) && data.getMinigunSpeed() < item.configType.getMinigunMaxSpeed())
        {
            data.setMinigunSpeed(data.getMinigunSpeed() + 2.0F);
            if (level.isClientSide)
                ModClient.getGunAnimations(player, hand).addMinigunBarrelRotationSpeed(2.0F);
        }
    }

    private void handleMinigunLoopingSounds(Level level, Player player, PlayerData data, EnumFireMode mode, InteractionHand hand)
    {
        if (data.isReloading(hand) || !item.configType.isUseLoopingSounds() || data.getLoopedSoundDelay() > 0)
            return;

        if (data.isShootKeyPressed(hand) && !data.isPrevShootKeyPressed(hand))
        {
            data.setLoopedSoundDelay(item.configType.getWarmupSoundLength());
            if (!level.isClientSide)
                PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, item.configType.getWarmupSound(), false);
        }
        else if (data.isShootKeyPressed(hand))
        {
            data.setLoopedSoundDelay(item.configType.getLoopedSoundLength());
            if (!level.isClientSide)
                PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, item.configType.getLoopedSound(), false);
        }
        else if (!data.isShootKeyPressed(hand))
        {
            PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, item.configType.getCooldownSound(), false);
        }
    }

    public void doPlayerReload(Level level, ServerPlayer player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean isForced)
    {
        //TODO: compare with handleServerSide() from PacketReload
        if (reload(level, player, gunStack, player.getInventory(), isForced, player.isCreative()))
        {
            int maxAmmo = item.configType.getNumAmmoItemsInGun(gunStack);
            boolean hasMultipleAmmo = (maxAmmo > 1);
            int reloadCount = item.getReloadCount(gunStack);
            float reloadTime = item.getActualReloadTime(gunStack);

            //TODO: implement Enchantments
            //reloadTime = EnchantmentModule.ModifyReloadTime(reloadTime, player, otherHand);

            // Set player shoot delay to be the reload delay - Set both gun delays to avoid reloading two guns at once
            data.setShootTimeRight(reloadTime);
            data.setShootTimeLeft(reloadTime);
            data.setReloading(hand, true);
            data.setBurstRoundsRemaining(hand,0);

            PacketHandler.sendTo(new PacketGunReloadClient(hand, reloadTime, reloadCount, hasMultipleAmmo), player);

            // Play reload sound
            if (StringUtils.isNotBlank(item.configType.getReloadSound()))
                PacketPlaySound.sendSoundPacket(player, FlansMod.SOUND_RANGE, item.configType.getReloadSound(), false);
        }
    }

    /**
     * Returns true if the gun was actually reloaded
     */
    public boolean reload(Level level, Entity reloadingEntity, ItemStack gunStack, Inventory inventory, boolean forceReload, boolean isCreative)
    {
        //Deployable guns cannot be reloaded in the inventory
        //TODO investigate if this code can can actually be called by an deployable
        if (item.configType.isDeployable())
            return false;

        //If you cannot reload halfway through a clip, reject the player for trying to do so
        if (forceReload && !item.configType.isCanForceReload())
            return false;

        boolean reloadedSomething = false;

        //Check each ammo slot, one at a time
        for (int i = 0; i < item.configType.getNumAmmoItemsInGun(gunStack); i++)
        {
            //Get the stack in the slot
            ItemStack bulletStack = item.getBulletItemStack(gunStack, i);

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
                        dropItem(level, reloadingEntity, shootableItem.getConfigType().getDropItemOnReload(), shootableItem.getConfigType().getContentPack());
                    }

                    //The magazine was not finished, pull it out and give it back to the player or, failing that, drop it
                    if (bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage() && !InventoryHelper.addItemStackToInventory(inventory, bulletStack, isCreative))
                    {
                        reloadingEntity.spawnAtLocation(bulletStack, 0.5F);
                    }

                    //Load the new magazine
                    ItemStack stackToLoad = newBulletStack.copy();
                    stackToLoad.setCount(1);
                    item.setBulletItemStack(gunStack, stackToLoad, i);

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

    public void checkForLockOn(Level level, Player player, PlayerData data, InteractionHand hand)
    {
        if (!item.configType.isCanSetPosition())
            item.impactX = item.impactY = item.impactZ = 0;

        if (item.lockOnSoundDelay > 0)
            item.lockOnSoundDelay--;

        if (!(item.configType.isLockOnToLivings() || item.configType.isLockOnToMechas() || item.configType.isLockOnToPlanes() || item.configType.isLockOnToPlayers() || item.configType.isLockOnToVehicles()))
            return;

        Entity closest = findLockOnTarget(level, player, data, hand);

        if (closest != null)
            closest.getPersistentData().putBoolean(GunItem.NBT_ENTITY_LOCK_ON, true);

        if (shouldPlayLockOnSound(level, player, closest))
        {
            playLockOnSounds((ServerPlayer) player, closest);
            item.lockOnSoundDelay = item.configType.getLockOnSoundTime();
        }
    }

    private Entity findLockOnTarget(Level level, Player player, PlayerData data, InteractionHand hand)
    {
        if (data.isReloading(hand))
            return null;

        // Range query instead of scanning loadedEntityList
        double range = item.configType.getMaxRangeLockOn();
        AABB box = player.getBoundingBox().inflate(range);

        // Look direction (1.20.1)
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position(); // matches your old posX/posY/posZ-ish basis

        // Cone threshold: angle < canLockOnAngle  <=> dot > cos(angle)
        double cosThreshold = Math.cos(Math.toRadians(item.configType.getCanLockOnAngle()));

        Entity best = null;
        double bestDistSqr = Double.MAX_VALUE;

        List<Entity> candidates = level.getEntities(player, box, e -> e.isAlive() && e != player);

        for (Entity e : candidates)
        {
            if (!isValidLockOnType(e))
                continue;

            Vec3 to = e.position().subtract(origin);
            double distSqr = to.lengthSqr();
            if (distSqr > range * range)
                continue;

            Vec3 dir = to.normalize();
            double dot = look.dot(dir);

            if (dot <= cosThreshold)
                continue;

            // Optional: choose closest in distance
            if (distSqr < bestDistSqr)
            {
                bestDistSqr = distSqr;
                best = e;
            }
        }

        return best;
    }

    private boolean isValidLockOnType(Entity entity)
    {
        return ((item.configType.isLockOnToMechas() && entity instanceof Mecha)
            || (item.configType.isLockOnToVehicles() && ModUtils.isVehicleLike(entity))
            || (item.configType.isLockOnToPlanes() && ModUtils.isPlaneLike(entity))
            || (item.configType.isLockOnToPlayers() && entity instanceof Player)
            || (item.configType.isLockOnToLivings() && entity instanceof LivingEntity));
    }

    private boolean shouldPlayLockOnSound(Level level, Player player, Entity target)
    {
        if (target == null || item.lockOnSoundDelay > 0 || level.isClientSide)
            return false;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty())
            return false;

        return held.getItem() instanceof GunItem && player instanceof ServerPlayer;
    }

    private void playLockOnSounds(ServerPlayer player, Entity closestEntity)
    {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem itemGun))
            return;

        PacketPlaySound.sendSoundPacket(player, GunItem.LOCK_ON_SOUND_RANGE, itemGun.configType.getLockOnSound(), false);

        if (closestEntity instanceof Driveable driveable && driveable.getConfigType().isHasFlare())
        {
            String lockingOnSound = driveable.getConfigType().lockingOnSound;
            if (StringUtils.isNotBlank(lockingOnSound))
                PacketPlaySound.sendSoundPacket(closestEntity, driveable.getConfigType().getLockedOnSoundRange(), lockingOnSound, false);
        }
    }

    public void checkForMelee(Level level, Player player, PlayerData data, ItemStack itemstack)
    {
        if (!shouldProcessMelee(player, data, itemstack))
            return;

        for (int pointIdx = 0; pointIdx < item.configType.getMeleeDamagePoints().size(); pointIdx++)
            processDamagePoint(level, player, data, itemstack, pointIdx);

        advanceAndResetIfDone(data);
    }

    private boolean shouldProcessMelee(Player player, PlayerData data, ItemStack itemstack)
    {
        if (data.getMeleeLength() <= 0)
            return false;
        if (item.configType.getMeleePath().isEmpty())
            return false;
        return player.getInventory().getSelected() == itemstack;
    }

    private void processDamagePoint(Level level, Player player, PlayerData data, ItemStack itemstack, int pointIdx)
    {
        Vector3f nextWorldPosF = computeNextDamagePointWorldPos(player, data, pointIdx);
        Vector3f lastWorldPosF = data.getLastMeleePositions()[pointIdx];

        Vec3 end = new Vec3(nextWorldPosF.x, nextWorldPosF.y, nextWorldPosF.z);
        Vec3 start = (lastWorldPosF == null) ? end : new Vec3(lastWorldPosF.x, lastWorldPosF.y, lastWorldPosF.z);
        MeleeSegment segment = new MeleeSegment(start, end);

        if (segment.isDegenerate())
        {
            data.getLastMeleePositions()[pointIdx] = nextWorldPosF;
            return;
        }

        Vector3f nextPosInWorldCoords = new Vector3f(segment.end);
        Vector3f dPos = (data.getLastMeleePositions()[pointIdx] == null) ? new Vector3f() : Vector3f.sub(nextPosInWorldCoords, data.getLastMeleePositions()[pointIdx], null);

        List<BulletHit> hits = collectHits(level, player, data, segment, pointIdx, dPos);

        if (!hits.isEmpty())
        {
            Collections.sort(hits);
            applyHits(level, player, data, itemstack, segment, hits, pointIdx, dPos);
        }

        data.getLastMeleePositions()[pointIdx] = nextWorldPosF;
    }

    private Vector3f computeNextDamagePointWorldPos(Player player, PlayerData data, int pointIdx)
    {
        Vector3f meleeDamagePoint = item.configType.getMeleeDamagePoints().get(pointIdx);

        Vector3f nextPos = item.configType.getMeleePath().get((data.getMeleeProgress() + 1) % item.configType.getMeleePath().size());
        Vector3f nextAngles = item.configType.getMeleePathAngles().get((data.getMeleeProgress() + 1) % item.configType.getMeleePathAngles().size());

        RotatedAxes nextAxes = new RotatedAxes().rotateGlobalRoll(-nextAngles.x).rotateGlobalPitch(-nextAngles.z).rotateGlobalYaw(-nextAngles.y);

        Vector3f nextPosInGunCoords = nextAxes.findLocalVectorGlobally(meleeDamagePoint);
        Vector3f.add(nextPos, nextPosInGunCoords, nextPosInGunCoords);

        Vector3f nextPosInPlayerCoords = new RotatedAxes(player.getYRot() + 90F, player.getXRot(), 0F).findLocalVectorGlobally(nextPosInGunCoords);

        if (!ModUtils.isThePlayer(player))
        {
            nextPosInPlayerCoords.y += 1.6F;
        }

        return new Vector3f((float) (player.getX() + nextPosInPlayerCoords.x), (float) (player.getY() + nextPosInPlayerCoords.y), (float) (player.getZ() + nextPosInPlayerCoords.z));
    }

    private List<BulletHit> collectHits(Level level, Player attacker, PlayerData attackerData, MeleeSegment segment, int pointIdx, Vector3f dPos)
    {
        ArrayList<BulletHit> hits = new ArrayList<>();

        AABB broadPhase = segment.asAabb().inflate(1.0);
        for (Entity candidate : ModUtils.queryEntities(level, attacker, broadPhase))
        {
            collectHitsForEntity(attacker, attackerData, segment, pointIdx, candidate, hits, dPos);
        }

        return hits;
    }

    private void collectHitsForEntity(Player attacker, PlayerData attackerData, MeleeSegment segment, int pointIdx, Entity candidate, List<BulletHit> outHits, Vector3f dPos)
    {
        if (candidate instanceof Player otherPlayer)
        {
            collectHitsForPlayer(attacker, attackerData, segment, pointIdx, otherPlayer, outHits, dPos);
        }
        else if (shouldConsiderNonPlayer(candidate, attackerData, pointIdx))
        {
            Optional<Vec3> clip = candidate.getBoundingBox().clip(segment.start, segment.end);
            clip.ifPresent(hit -> outHits.add(new EntityHit(candidate, (float) segment.lambdaAt(hit), hit)));
        }
    }

    private boolean shouldConsiderNonPlayer(Entity e, PlayerData attackerData, int pointIdx)
    {
        if (!(e instanceof LivingEntity || e instanceof AAGun))
            return false;

        return attackerData.getLastMeleePositions() != null && attackerData.getLastMeleePositions()[pointIdx] != null;
    }

    private void collectHitsForPlayer(Player attacker, PlayerData attackerData, MeleeSegment segment, int pointIdx, Player otherPlayer, List<BulletHit> outHits, Vector3f dPos)
    {
        PlayerData otherData = PlayerData.getInstance(otherPlayer);

        if (otherData != null)
        {
            if (!otherPlayer.isAlive() || otherData.getTeam() == Team.SPECTATORS)
                return;

            PlayerSnapshot snapshot = selectSnapshot(attacker, otherData);
            if (snapshot != null)
            {
                List<BulletHit> playerHits = snapshot.raytrace(attackerData.getLastMeleePositions()[pointIdx] == null ? new Vector3f(segment.end) : attackerData.getLastMeleePositions()[pointIdx], dPos);
                outHits.addAll(playerHits);
                return;
            }
        }

        Optional<Vec3> clip = otherPlayer.getBoundingBox().clip(segment.start, segment.end);
        clip.ifPresent(hit -> outHits.add(new PlayerBulletHit(new PlayerHitbox(otherPlayer, new RotatedAxes(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), EnumHitboxType.BODY), (float) segment.lambdaAt(hit))));
    }

    private PlayerSnapshot selectSnapshot(Player attacker, PlayerData otherData)
    {
        int snapshotToTry = 0;
        if (attacker instanceof ServerPlayer sp)
            snapshotToTry = sp.latency / 50;

        if (snapshotToTry >= otherData.getSnapshots().length)
            snapshotToTry = otherData.getSnapshots().length - 1;

        PlayerSnapshot snapshot = otherData.getSnapshots()[snapshotToTry];
        if (snapshot == null)
            snapshot = otherData.getSnapshots()[0];
        return snapshot;
    }

    private void applyHits(Level level, Player attacker, PlayerData attackerData, ItemStack itemstack, MeleeSegment segment, List<BulletHit> hits, int pointIdx, Vector3f dPos)
    {
        double swingDistance = segment.length();

        for (BulletHit hit : hits)
        {
            if (doesHitBlock(segment.end, attacker))
                continue;

            if (hit instanceof PlayerBulletHit ph)
                applyPlayerHit(level, attacker, attackerData, itemstack, swingDistance, ph, pointIdx, dPos);
            else if (hit instanceof EntityHit eh)
                applyEntityHit(level, attacker, attackerData, itemstack, swingDistance, eh, pointIdx, dPos);
        }
    }

    public boolean doesHitBlock(Vec3 endPos, Entity player)
    {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition(1.0F);

        ClipContext ctx = new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);

        BlockHitResult bhr = level.clip(ctx);
        if (bhr.getType() != HitResult.Type.BLOCK)
            return false;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float destroySpeed = state.getDestroySpeed(level, pos);

        return destroySpeed < 0.0F || destroySpeed > 0.2F;
    }

    private void applyPlayerHit(Level level, Player attacker, PlayerData attackerData, ItemStack itemstack, double swingDistance, PlayerBulletHit hit, int pointIdx, Vector3f dPos)
    {
        Player attackedPlayer = hit.getHitbox().player;
        float damage = (float) (swingDistance * item.configType.getMeleeDamage(itemstack, false));

        boolean didHurt = attackedPlayer.hurt(FlansDamageSources.createDamageSource(level, attacker, FlansDamageSources.FLANS_MELEE), damage);
        if (didHurt)
            attackedPlayer.invulnerableTime = attackedPlayer.hurtDuration / 2;

        DebugHelper.spawnDebugDot(level, new Vector3f(attackerData.getLastMeleePositions()[pointIdx].x + dPos.x * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].y + dPos.y * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].z + dPos.z * hit.getIntersectTime()), 1000, 1F, 0F, 0F);
    }

    private void applyEntityHit(Level level, Player attacker, PlayerData attackerData, ItemStack itemstack, double swingDistance, EntityHit hit, int pointIdx, Vector3f dPos)
    {
        Entity target = hit.getEntity();
        float damage = (float) (swingDistance * item.configType.getMeleeDamage(itemstack, target instanceof Driveable));

        boolean didHurt = target.hurt(FlansDamageSources.createDamageSource(level, attacker, FlansDamageSources.FLANS_MELEE), damage);
        if (didHurt && target instanceof LivingEntity living)
            living.invulnerableTime = living.hurtDuration / 2;

        DebugHelper.spawnDebugDot(level, new Vector3f(attackerData.getLastMeleePositions()[pointIdx].x + dPos.x * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].y + dPos.y * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].z + dPos.z * hit.getIntersectTime()), 1000, 1F, 0F, 0F);
    }

    private void advanceAndResetIfDone(PlayerData data)
    {
        data.setMeleeProgress(data.getMeleeProgress() + 1);
        if (data.getMeleeProgress() >= data.getMeleeLength())
        {
            data.setMeleeProgress(0);
            data.setMeleeLength(0);
        }
    }

    /**
     * Simple value object for the ray segment
     */
    private record MeleeSegment(Vec3 start, Vec3 end)
    {
        boolean isDegenerate()
        {
            return start.equals(end);
        }

        double length()
        {
            return end.subtract(start).length();
        }

        AABB asAabb()
        {
            return new AABB(start, end);
        }

        double lambdaAt(Vec3 hit)
        {
            Vec3 d = end.subtract(start);

            double ax = Math.abs(d.x);
            double ay = Math.abs(d.y);
            double az = Math.abs(d.z);
            if (ax >= ay && ax >= az && d.x != 0)
                return Math.abs((hit.x - start.x) / d.x);
            if (ay >= ax && ay >= az && d.y != 0)
                return Math.abs((hit.y - start.y) / d.y);
            if (az != 0)
                return Math.abs((hit.z - start.z) / d.z);
            return 0.0;
        }
    }
}
