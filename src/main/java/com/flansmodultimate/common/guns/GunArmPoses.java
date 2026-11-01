package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.equipment.ArmPoseRule;
import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.config.EnumEntityAimPose;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.gun.PacketAimPoseState;
import com.flansmodultimate.network.client.gun.PacketGunShotPose;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Decides how the arms of a player or humanoid mob hold the guns in their hands, one arm at a time. The
 * client renders the result and the server builds its player hitboxes from the same result, so a raised
 * arm is where it looks to be.
 *
 * <p>
 * A shield comes first: it is always raised, on its own arm only, whatever the other hand holds and
 * whichever aim pose applies. A raised gun whose other hand is free takes the bow pose, the free arm
 * reaching across to support it. Two raised guns aim straight ahead with both arms. A raised gun next to
 * anything else, a shield included, raises only its own arm.
 * </p>
 *
 * <p>
 * In the enforced aim pose every held gun is raised. In the dynamic aim pose a gun is raised only
 * while it is fired, aimed or reloaded, unless its definition forces the aiming pose. Players choose the pose unless the
 * server decides it for them; mobs follow the server alone and, having no aim control, raise a gun only
 * when they fire or reload it.
 * </p>
 *
 * <p>
 * Charging a throw draws only the throwing arm back over the shoulder. The other arm keeps the pose it
 * had while the throwable was merely held, so a shield stays up and a free arm that supported the raised
 * throwable keeps pointing ahead.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunArmPoses
{
    /** How long a shot keeps a gun raised in the dynamic aim pose, so a single shot is not a one-frame twitch. */
    public static final int SHOT_HOLD_TICKS = 20;
    private static final long NEVER = Long.MIN_VALUE / 2;

    /** Last shot game time per hand, per side, so that each side reads only its own entities. */
    private static final Map<LivingEntity, long[]> serverShots = new WeakHashMap<>();
    private static final Map<LivingEntity, long[]> clientShots = new WeakHashMap<>();
    /** Game time until which a reload keeps each hand's gun raised, per side. */
    private static final Map<LivingEntity, long[]> serverReloads = new WeakHashMap<>();
    private static final Map<LivingEntity, long[]> clientReloads = new WeakHashMap<>();

    /** What a hand holds, as far as the arm pose is concerned. */
    public enum HandItem
    {
        EMPTY, OTHER, GUN, SHIELD
    }

    /** The pose one arm is given, or {@link #NONE} to leave it to vanilla. */
    public enum Arm
    {
        NONE,
        /** Explicit neutral rotations, ignoring native use and attack poses. */
        RELAXED,
        /** Only this arm is raised towards where the head looks. */
        ONE_ARM,
        /** The bow pose: this arm is raised and the free other arm reaches across to support it. */
        BOW,
        /** Both arms aim straight ahead, one gun in each hand. */
        BOTH,
        /** This arm draws a throw back over the shoulder while the other arm keeps its own pose. */
        THROW,
        /** The free arm keeps reaching across as in the bow pose while the other arm charges a throw. */
        SUPPORT
    }

    public record Result(Arm mainHand, Arm offHand)
    {
        public static final Result NONE = new Result(Arm.NONE, Arm.NONE);

        public Arm get(InteractionHand hand)
        {
            return hand == InteractionHand.OFF_HAND ? offHand : mainHand;
        }
    }

    /**
     * The arm poses for what the two hands hold.
     *
     * @param dynamic
     *            whether guns are raised only while active, rather than always
     * @param mainActive
     *            whether the main hand gun is being fired, aimed or reloaded
     * @param offActive
     *            whether the off hand gun is being fired, aimed or reloaded
     */
    public static Result resolve(HandItem main, HandItem off, boolean dynamic, boolean mainActive, boolean offActive)
    {
        boolean mainRaised = isRaised(main, dynamic, mainActive);
        boolean offRaised = isRaised(off, dynamic, offActive);
        return new Result(arm(main, mainRaised, off, offRaised), arm(off, offRaised, main, mainRaised));
    }

    /**
     * The arm poses while a throw is charged in the given hand, from those the hands had while the
     * throwable was merely held.
     */
    public static Result charging(Result held, InteractionHand throwingHand)
    {
        InteractionHand otherHand = throwingHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        // The bow pose of the throwing arm is what raised the free arm, so that arm keeps it on its own
        Arm other = held.get(throwingHand) == Arm.BOW ? Arm.SUPPORT : held.get(otherHand);
        return throwingHand == InteractionHand.MAIN_HAND ? new Result(Arm.THROW, other) : new Result(other, Arm.THROW);
    }

    private static Arm arm(HandItem held, boolean raised, HandItem other, boolean otherRaised)
    {
        if (!raised)
            return Arm.NONE;
        // A shield arm always keeps its own pose, never drawn into a two-handed gun pose
        if (held == HandItem.SHIELD)
            return Arm.ONE_ARM;
        if (other == HandItem.GUN && otherRaised)
            return Arm.BOTH;
        return other == HandItem.EMPTY ? Arm.BOW : Arm.ONE_ARM;
    }

    private static boolean isRaised(HandItem item, boolean dynamic, boolean active)
    {
        return switch (item)
        {
            // A shield is held up whatever else is going on
            case SHIELD -> true;
            case GUN -> !dynamic || active;
            default -> false;
        };
    }

    /** The arm poses of an entity, read on the side the entity lives on. */
    public static Result resolve(LivingEntity entity)
    {
        boolean enabled = !(entity instanceof IEquipmentPolicy policy) || policy.flansWeaponAnimations();
        ItemStack mainStack = entity.getMainHandItem();
        ItemStack offStack = entity.getOffhandItem();
        HandItem main = classify(mainStack);
        HandItem off = classify(offStack);
        InteractionHand throwingHand = chargingThrowHand(entity);
        boolean aiming = isAiming(entity);
        Result held = resolve(main, off, isDynamic(entity), aiming || isActive(entity, InteractionHand.MAIN_HAND) || forcesAimPose(main, mainStack),
            aiming || isActive(entity, InteractionHand.OFF_HAND) || forcesAimPose(off, offStack));
        Result base = throwingHand == null ? held : charging(held, throwingHand);
        return new Result(override(entity, InteractionHand.MAIN_HAND, main, off, enabled ? base.mainHand() : Arm.NONE),
            override(entity, InteractionHand.OFF_HAND, off, main, enabled ? base.offHand() : Arm.NONE));
    }

    private static Arm override(LivingEntity entity, InteractionHand hand, HandItem item, HandItem other, Arm normal)
    {
        // Presets and the animation switch cannot move shields; only explicit custom axes, applied last, may do that.
        if (item == HandItem.SHIELD)
            return Arm.ONE_ARM;
        ArmPoseRule rule = HeldItemPresentation.rule(entity, hand);
        return switch (rule.pose())
        {
            case DEFAULT, CUSTOM -> normal;
            case RELAXED -> Arm.RELAXED;
            case ONE_HANDED -> Arm.ONE_ARM;
            case TWO_HANDED -> other == HandItem.EMPTY ? Arm.BOW : Arm.ONE_ARM;
            case THROW -> Arm.THROW;
        };
    }

    /** The hand in which the entity charges a throw, or {@code null} when it charges none. */
    @Nullable
    public static InteractionHand chargingThrowHand(LivingEntity entity)
    {
        if (entity instanceof IEquipmentPolicy policy)
            for (InteractionHand hand : InteractionHand.values())
                if (policy.flansmodultimateChargingThrow(hand))
                    return hand;
        if (!entity.isUsingItem())
            return null;

        // The hand rather than the use stack, which a stack update from the server replaces until the next tick
        InteractionHand hand = entity.getUsedItemHand();
        return entity.getItemInHand(hand).getItem() instanceof GunItem gunItem && gunItem.getConfigType().isThrowable() ? hand : null;
    }

    private static boolean forcesAimPose(HandItem handItem, ItemStack stack)
    {
        return handItem == HandItem.GUN && stack.getItem() instanceof GunItem gunItem && gunItem.getConfigType().isForceAimPose();
    }

    /** A throwable being charged still counts as a gun, so that the other arm keeps the pose it had. */
    public static HandItem classify(ItemStack stack)
    {
        if (stack.isEmpty())
            return HandItem.EMPTY;
        if (stack.getItem() instanceof GunItem gunItem)
            return gunItem.getConfigType().isShield() ? HandItem.SHIELD : HandItem.GUN;
        return HandItem.OTHER;
    }

    /** Main-hand attacks leave a player's held off-hand shield at its pre-attack pose, on both sides. */
    public static boolean keepsOffhandShieldRaised(LivingEntity entity)
    {
        return entity instanceof Player && entity.swingingArm != InteractionHand.OFF_HAND && entity.getOffhandItem().getItem() instanceof GunItem gunItem && gunItem.getConfigType().isShield();
    }

    /** Whether the entity's guns are raised only while active, from the server's rules and the player's choice. */
    public static boolean isDynamic(LivingEntity entity)
    {
        if (entity instanceof IEquipmentPolicy policy)
        {
            var preference = policy.flansmodultimateDynamicAimPose();
            if (preference.isPresent())
                return preference.get();
        }
        if (entity instanceof Player player)
        {
            return switch (ModCommonConfig.playerAimPose())
            {
                case FREE_CHOICE -> PlayerData.getInstance(player).isDynamicAimPose();
                case ENFORCED -> false;
                case DYNAMIC -> true;
            };
        }
        if (entity instanceof IEquipmentPolicy policy && policy.flansmodultimateUsePlayerAimRules())
            return switch (ModCommonConfig.playerAimPose())
            {
                case DYNAMIC -> true;
                case ENFORCED -> false;
                case FREE_CHOICE -> ModCommonConfig.entityAimPose() == EnumEntityAimPose.DYNAMIC;
            };
        return ModCommonConfig.entityAimPose() == EnumEntityAimPose.DYNAMIC;
    }

    /** Mobs have no aim control; a player aims by looking through the sights. */
    private static boolean isAiming(LivingEntity entity)
    {
        if (!(entity instanceof Player player))
            return false;

        PlayerData data = PlayerData.getInstance(player);
        return entity.level().isClientSide ? data.isShownAiming() : data.isScoped();
    }

    /** Whether the gun in the given hand is fired or reloaded, either of which raises it in the dynamic aim pose. */
    private static boolean isActive(LivingEntity entity, InteractionHand hand)
    {
        return isFiring(entity, hand) || isReloading(entity, hand);
    }

    /** A player's own reload state on the server; otherwise the reload last recorded on this side. */
    private static boolean isReloading(LivingEntity entity, InteractionHand hand)
    {
        if (!entity.level().isClientSide && entity instanceof Player player && PlayerData.getInstance(player).isReloading(hand))
            return true;

        long[] reloadEnds = reloads(entity).get(entity);
        return reloadEnds != null && entity.level().getGameTime() < reloadEnds[hand.ordinal()];
    }

    private static boolean isFiring(LivingEntity entity, InteractionHand hand)
    {
        return isFiring(entity, hand, HeldItemPresentation.get(entity, hand).poses().shotHoldTicks());
    }

    public static boolean isFiring(LivingEntity entity, InteractionHand hand, int holdTicks)
    {
        if (entity instanceof Player player && PlayerData.getInstance(player).isShooting(hand))
            return true;

        long[] lastShots = shots(entity).get(entity);
        return lastShots != null && entity.level().getGameTime() - lastShots[hand.ordinal()] < holdTicks;
    }

    private static Map<LivingEntity, long[]> shots(LivingEntity entity)
    {
        return entity.level().isClientSide ? clientShots : serverShots;
    }

    private static Map<LivingEntity, long[]> reloads(LivingEntity entity)
    {
        return entity.level().isClientSide ? clientReloads : serverReloads;
    }

    /** Remembers that the entity reloads the gun in the given hand for the given ticks, on the side it is called on. */
    public static void recordReload(LivingEntity holder, InteractionHand hand, float reloadTicks)
    {
        long end = holder.level().getGameTime() + (long) Math.ceil(Math.max(0F, reloadTicks));
        reloads(holder).computeIfAbsent(holder, entity -> new long[]{NEVER, NEVER})[hand.ordinal()] = end;
    }

    /** Forgets a reload that was cancelled, on the side it is called on. */
    public static void clearReload(LivingEntity holder, InteractionHand hand)
    {
        long[] reloadEnds = reloads(holder).get(holder);
        if (reloadEnds != null)
            reloadEnds[hand.ordinal()] = NEVER;
    }

    /** Remembers that the entity fired the gun in the given hand, on the side it is called on. */
    public static void recordShot(LivingEntity shooter, InteractionHand hand)
    {
        shots(shooter).computeIfAbsent(shooter, entity -> new long[]{NEVER, NEVER})[hand.ordinal()] = shooter.level().getGameTime();
    }

    /**
     * Server side: records a shot and lets the clients that see the shooter know. A player's shots already
     * reach them through the shooting state, so only mobs need the extra packet.
     */
    public static void onShotFired(LivingEntity shooter, InteractionHand hand)
    {
        recordShot(shooter, hand);
        if (!(shooter instanceof Player))
            PacketHandler.sendToTracking(new PacketGunShotPose(shooter.getId(), hand, !(shooter instanceof IEquipmentPolicy policy) || policy.flansGunShotAnimations()), shooter);
    }

    /** Server side: tells the player and everyone who sees them their aim pose choice and whether they aim. */
    public static void syncPlayer(ServerPlayer player)
    {
        PacketHandler.sendToTracking(statePacket(player), player);
    }

    /** Server side: tells a player who just started seeing another one how that one holds their guns. */
    public static void sendPlayerState(ServerPlayer player, ServerPlayer receiver)
    {
        PacketHandler.sendTo(statePacket(player), receiver);
    }

    private static PacketAimPoseState statePacket(ServerPlayer player)
    {
        PlayerData data = PlayerData.getInstance(player);
        return new PacketAimPoseState(player.getUUID(), data.isDynamicAimPose(), data.isScoped());
    }
}
