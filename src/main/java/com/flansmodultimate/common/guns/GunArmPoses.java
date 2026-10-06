package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.IEquipmentPolicy;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.config.EnumEntityAimPose;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketAimPoseState;
import com.flansmodultimate.network.client.PacketGunShotPose;
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
 * while it is fired or aimed, unless its definition forces the aiming pose. Players choose the pose unless the
 * server decides it for them; mobs follow the server alone and, having no aim control, raise a gun only
 * when they fire it.
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

    /** What a hand holds, as far as the arm pose is concerned. */
    public enum HandItem
    {
        EMPTY, OTHER, GUN, SHIELD
    }

    /** The pose one arm is given, or {@link #NONE} to leave it to vanilla. */
    public enum Arm
    {
        NONE,
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
     *            whether the main hand gun is being fired or aimed
     * @param offActive
     *            whether the off hand gun is being fired or aimed
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
        if (entity instanceof IEquipmentPolicy policy && !policy.flansWeaponAnimations())
            return Result.NONE;
        ItemStack mainStack = entity.getMainHandItem();
        ItemStack offStack = entity.getOffhandItem();
        HandItem main = classify(mainStack);
        HandItem off = classify(offStack);
        if (!isGunOrShield(main) && !isGunOrShield(off))
            return Result.NONE;

        boolean aiming = isAiming(entity);
        Result held = resolve(main, off, isDynamic(entity), aiming || isFiring(entity, InteractionHand.MAIN_HAND) || forcesAimPose(main, mainStack),
            aiming || isFiring(entity, InteractionHand.OFF_HAND) || forcesAimPose(off, offStack));
        InteractionHand throwingHand = chargingThrowHand(entity);
        return throwingHand == null ? held : charging(held, throwingHand);
    }

    /** The hand in which the entity charges a throw, or {@code null} when it charges none. */
    @Nullable
    public static InteractionHand chargingThrowHand(LivingEntity entity)
    {
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
        if (stack.getItem() instanceof GunItem gunItem && gunItem.useAimingAnimation())
            return gunItem.getConfigType().isShield() ? HandItem.SHIELD : HandItem.GUN;
        return HandItem.OTHER;
    }

    private static boolean isGunOrShield(HandItem item)
    {
        return item == HandItem.GUN || item == HandItem.SHIELD;
    }

    /** Whether the entity's guns are raised only while active, from the server's rules and the player's choice. */
    public static boolean isDynamic(LivingEntity entity)
    {
        if (entity instanceof Player player)
        {
            return switch (ModCommonConfig.playerAimPose())
            {
                case FREE_CHOICE -> PlayerData.getInstance(player).isDynamicAimPose();
                case ENFORCED -> false;
                case DYNAMIC -> true;
            };
        }
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

    private static boolean isFiring(LivingEntity entity, InteractionHand hand)
    {
        if (entity instanceof Player player && PlayerData.getInstance(player).isShooting(hand))
            return true;

        long[] lastShots = shots(entity).get(entity);
        return lastShots != null && entity.level().getGameTime() - lastShots[hand.ordinal()] < SHOT_HOLD_TICKS;
    }

    private static Map<LivingEntity, long[]> shots(LivingEntity entity)
    {
        return entity.level().isClientSide ? clientShots : serverShots;
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
            PacketHandler.sendToTracking(new PacketGunShotPose(shooter.getId(), hand), shooter);
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
