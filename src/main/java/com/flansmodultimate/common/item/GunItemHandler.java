package com.flansmodultimate.common.item;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.guns.EnumFireDecision;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.handler.PlayerShootingHandler;
import com.flansmodultimate.common.guns.handler.ShootingHandler;
import com.flansmodultimate.common.guns.reload.GunReloader;
import com.flansmodultimate.common.raytracing.EnumHitboxType;
import com.flansmodultimate.common.raytracing.PlayerHitbox;
import com.flansmodultimate.common.raytracing.PlayerSnapshot;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.event.GunFiredEvent;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunMeleeClient;
import com.flansmodultimate.network.client.PacketGunReloadClient;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
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
import java.util.UUID;

public class GunItemHandler
{
    private final GunItem item;
    @Getter
    private final GunReloader gunReloader;

    public GunItemHandler(GunItem item)
    {
        this.item = item;
        gunReloader = new GunReloader(item);
    }

    /**
     * Used to determine if, for example, a player is holding a two-handed gun but the other hand (the one without a gun) is holding something else
     * For example a player is holding two miniguns, a gun requiring both hands, so this method returns true
     *
     * @param entity The LivingEntity who is handling the gun
     * @return if the player can handle the gun based on the contents of the main and off hand and the GunType
     */
    public boolean gunCanBeHandled(LivingEntity entity)
    {
        // We can always use a 1H gun
        if (item.configType.isOneHanded())
            return true;

        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
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

    public boolean shouldBlockFireAtCrosshair()
    {
        HitResult hr = ClientHooks.GUN.getClientHitResult();
        if (!(hr instanceof EntityHitResult ehr))
            return false;

        // Do not shoot ammo bags, flags or dropped gun items
        Entity entity = ehr.getEntity();
        return (entity instanceof Flagpole
            || entity instanceof Flag
            || entity instanceof GunItemEntity
            || entity instanceof Grenade grenade && grenade.getConfigType().isDeployableBag());
    }

    public EnumFireDecision computeFireDecision(PlayerData data, ItemStack gunStack, InteractionHand hand)
    {
        GunType type = item.configType;
        EnumFireMode mode = type.getFireMode(gunStack);

        boolean emptyAmmo = hasEmptyAmmo(gunStack);
        boolean shootPressed = data.isShootKeyPressed(hand);
        boolean shootEdgePressed = data.isShootKeyPressed(hand) && !data.isPrevShootKeyPressed(hand);

        boolean actionRequested;
        if (mode.isAutomaticFire())
            actionRequested = shootPressed;
        else if (mode == EnumFireMode.BURST)
            actionRequested = shootEdgePressed || data.getBurstRoundsRemaining(hand) > 0;
        else
            actionRequested = shootEdgePressed;

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

    public void doCustomMelee(Level level, ServerPlayer player, PlayerData data, InteractionHand hand)
    {
        if (item.configType.isDeployable() || !item.configType.isUsableByPlayers() || !gunCanBeHandled(player) || data.getMeleeLength() > 0)
            return;

        if (StringUtils.isNotBlank(item.configType.getMeleeSound()))
            PacketPlaySound.sendSoundPacket(player, item.configType.getMeleeSoundRange(), item.configType.getMeleeSound(), true);

        data.doMelee(player, item.configType.getMeleeTime(), item.configType);
        PacketHandler.sendToDimension(level.dimension(), new PacketGunMeleeClient(player.getUUID(), hand));
    }

    public void doPlayerShoot(Level level, ServerPlayer player, PlayerData data, ItemStack gunStack, InteractionHand hand)
    {
        //TODO: compare with tryToShoot() and shoot() (Server side)

        if (item.configType.isDeployable() || !item.configType.isUsableByPlayers() || !gunCanBeHandled(player))
            return;

        GunFiredEvent gunFireEvent = new GunFiredEvent(player);
        MinecraftForge.EVENT_BUS.post(gunFireEvent);
        if (gunFireEvent.isCanceled())
        {
            data.setShooting(hand, false);
            PacketHandler.sendToDimension(level.dimension(), new PacketGunShootClient(player.getUUID(), hand, false));
            return;
        }

        data.setShooting(hand, true);
        PacketHandler.sendToDimension(level.dimension(), new PacketGunShootClient(player.getUUID(), hand, true));

        boolean automaticFire = item.configType.getFireMode(null).isAutomaticFire();
        float shootTime = data.getShootTime(hand);
        float shootDelay = item.configType.getShootDelay(gunStack);

        while (shootTime <= 0F)
        {
            AmmoSlot ammoSlot = findLoadedAmmoInGun(item, gunStack, item.configType).orElse(null);
            if (ammoSlot == null)
                return;

            ItemStack shootableStack = ammoSlot.stack();
            ShootableItem shootableItem = (ShootableItem) ammoSlot.stack().getItem();
            ShootableType shootableType = shootableItem.getConfigType();
            ShootingHandler handler = new PlayerShootingHandler(level, player, hand, gunStack, ammoSlot.stack(), ammoSlot.index());

            ShootingHelper.fireGun(level, player, item.configType, shootableType, gunStack, shootableStack, (hand == InteractionHand.MAIN_HAND) ? player.getOffhandItem() : player.getMainHandItem(), handler);

            if (StringUtils.isNotBlank(item.configType.getShootSound()))
            {
                PacketPlaySound.sendSoundPacket(player, item.configType.getGunSoundRange(), item.configType.getShootSound(), item.configType.isDistortSound(), item.configType.isSilencedSound(gunStack));
                item.soundDelay = item.configType.getShootSoundLength();
            }

            if (StringUtils.isNotBlank(item.configType.getDistantShootSound()))
                PacketHandler.sendToDonut(level.dimension(), player.position(), item.configType.getGunSoundRange(), item.configType.getDistantSoundRange(), new PacketPlaySound(player.position(), item.configType.getDistantSoundRange(), item.configType.getDistantShootSound(), false, false, null));

            shootTime += shootDelay;

            if (!automaticFire)
                break;
        }
        data.setShootTime(hand, shootTime);
    }

    public void doPlayerReload(Level level, ServerPlayer player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean isForced)
    {
        UUID reloadSoundUUID = UUID.randomUUID();
        if (gunReloader.reload(level, player, data, gunStack, hand, isForced, player.getAbilities().instabuild, ModCommonConfig.get().combineAmmoOnReload(), ModCommonConfig.get().combineAmmoOnReload(), reloadSoundUUID))
        {
            int maxAmmo = item.configType.getNumAmmoItemsInGun(gunStack);
            boolean hasMultipleAmmo = (maxAmmo > 1);
            int reloadCount = item.getReloadCount(gunStack);
            float reloadTime = item.getActualReloadTime(gunStack);

            data.doGunReload(hand, reloadTime);
            PacketHandler.sendToDimension(level.dimension(), new PacketGunReloadClient(player.getUUID(), hand, reloadTime, reloadCount, hasMultipleAmmo));

            String reloadSound = item.configType.getReloadSound(gunStack);
            // Play reload sound
            if (StringUtils.isNotBlank(reloadSound))
                PacketPlaySound.sendSoundPacket(player, item.configType.getReloadSoundRange(), reloadSound, false, false, true, reloadSoundUUID);
        }
    }

    public boolean canReload(Container inventory)
    {
        List<ShootableType> allowedAmmoTypes = item.configType.getAmmoTypes();

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

    public static Optional<AmmoSlot> findLoadedAmmoInGun(GunItem item, ItemStack gunStack, GunType configType)
    {
        int slots = configType.getNumAmmoItemsInGun(gunStack);
        for (int i = 0; i < slots; i++)
        {
            ItemStack s = item.getAmmoItemStack(gunStack, i);
            if (s != null && !s.isEmpty() && s.getDamageValue() < s.getMaxDamage())
            {
                return Optional.of(new AmmoSlot(i, s));
            }
        }
        return Optional.empty();
    }

    public void handleMinigunEffects(Level level, Player player, PlayerData data, InteractionHand hand)
    {
        accelerateMinigun(player, data, hand);
        handleMinigunLoopingSounds(level, player, data, hand);
    }

    private void accelerateMinigun(Player player, PlayerData data, InteractionHand hand)
    {
        if (data.isShootKeyPressed(hand) && data.getMinigunSpeed() < item.configType.getMinigunMaxSpeed())
        {
            data.setMinigunSpeed(data.getMinigunSpeed() + 2.0F);
            ClientHooks.GUN.accelerateMinigun(player, hand, 2.0F);
        }
    }

    private void handleMinigunLoopingSounds(Level level, Player player, PlayerData data, InteractionHand hand)
    {
        if (data.isReloading(hand) || !item.configType.isUseLoopingSounds() || data.getLoopedSoundDelay() > 0)
            return;

        if (data.isShootKeyPressed(hand) && !data.isPrevShootKeyPressed(hand))
        {
            data.setLoopedSoundDelay(item.configType.getWarmupSoundLength());
            if (!level.isClientSide)
                PacketPlaySound.sendSoundPacket(player, ModCommonConfig.get().soundRange(), item.configType.getWarmupSound(), false);
        }
        else if (data.isShootKeyPressed(hand))
        {
            data.setLoopedSoundDelay(item.configType.getLoopedSoundLength());
            if (!level.isClientSide)
                PacketPlaySound.sendSoundPacket(player, ModCommonConfig.get().soundRange(), item.configType.getLoopedSound(), false);
        }
        else if (!data.isShootKeyPressed(hand))
        {
            PacketPlaySound.sendSoundPacket(player, ModCommonConfig.get().soundRange(), item.configType.getCooldownSound(), false);
        }
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

        if (level.isClientSide)
            ClientHooks.RENDER.spawnDebugVector(data.getLastMeleePositions()[pointIdx].toVec3(), dPos.toVec3(), 200, 1F, 0F, 0F);

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

        return new Vector3f((float) (player.getX() + nextPosInPlayerCoords.x), (float) (player.getEyeY() + nextPosInPlayerCoords.y), (float) (player.getZ() + nextPosInPlayerCoords.z));
    }

    private List<BulletHit> collectHits(Level level, Player attacker, PlayerData attackerData, MeleeSegment segment, int pointIdx, Vector3f dPos)
    {
        List<BulletHit> hits = new ArrayList<>();

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

        if (!otherPlayer.isAlive() || Team.SPECTATORS.equals(otherData.getTeam()))
            return;

        PlayerSnapshot snapshot = selectSnapshot(attacker, otherData);
        if (snapshot != null)
        {
            List<BulletHit> playerHits = snapshot.raytrace(attackerData.getLastMeleePositions()[pointIdx] == null ? new Vector3f(segment.end) : attackerData.getLastMeleePositions()[pointIdx], dPos);
            outHits.addAll(playerHits);
            return;
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

        boolean didHurt = attackedPlayer.hurt(FlanDamageSources.createDamageSource(level, attacker, FlanDamageSources.MELEE), damage);
        if (didHurt)
            attackedPlayer.invulnerableTime = attackedPlayer.hurtDuration / 2;

        ClientHooks.RENDER.spawnDebugDot(new Vec3(attackerData.getLastMeleePositions()[pointIdx].x + dPos.x * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].y + dPos.y * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].z + dPos.z * hit.getIntersectTime()), 1000, 1F, 0F, 0F);
    }

    private void applyEntityHit(Level level, Player attacker, PlayerData attackerData, ItemStack itemstack, double swingDistance, EntityHit hit, int pointIdx, Vector3f dPos)
    {
        Entity target = hit.getEntity();
        float damage = (float) (swingDistance * item.configType.getMeleeDamage(itemstack, target instanceof Driveable));

        boolean didHurt = target.hurt(FlanDamageSources.createDamageSource(level, attacker, FlanDamageSources.MELEE), damage);
        if (didHurt && target instanceof LivingEntity living)
            living.invulnerableTime = living.hurtDuration / 2;

        ClientHooks.RENDER.spawnDebugDot(new Vec3(attackerData.getLastMeleePositions()[pointIdx].x + dPos.x * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].y + dPos.y * hit.getIntersectTime(), attackerData.getLastMeleePositions()[pointIdx].z + dPos.z * hit.getIntersectTime()), 1000, 1F, 0F, 0F);
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

    public boolean tryPlaceDeployable(Level level, Player player, ItemStack stack)
    {
        double length = 5.0D;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(length));

        BlockHitResult hit = level.clip(new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP)
        {
            BlockPos pos = hit.getBlockPos();

            // if the hit block is snow layer, treat as the block below
            BlockState stateAtHit = level.getBlockState(pos);
            if (stateAtHit.is(Blocks.SNOW) || stateAtHit.getBlock() instanceof SnowLayerBlock)
            {
                pos = pos.below();
            }

            Direction direction = player.getDirection();

            // placement positions:
            BlockPos base = pos;
            BlockPos mgPos = pos.above();
            BlockPos forwardAbove = mgPos.relative(direction);

            if (!level.isClientSide && isSolidTop(level, base) && isReplaceableOrAir(level, mgPos) && isReplaceableOrAir(level, forwardAbove) && isReplaceableOrAir(level, base.relative(direction)))
            {
                // check if an MG already exists at that block position
                boolean exists = !level.getEntitiesOfClass(DeployedGun.class, new AABB(mgPos)).isEmpty();

                if (!exists)
                {
                    DeployedGun mg = new DeployedGun(level, mgPos, direction, item.configType);
                    level.addFreshEntity(mg);

                    if (!player.getAbilities().instabuild)
                        stack.shrink(1);

                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isReplaceableOrAir(Level level, BlockPos pos)
    {
        BlockState st = level.getBlockState(pos);
        return st.isAir() || st.is(Blocks.SNOW) || st.canBeReplaced();
    }

    private static boolean isSolidTop(Level level, BlockPos pos)
    {
        BlockState st = level.getBlockState(pos);
        return st.isSolidRender(level, pos) && st.canOcclude();
    }
}
