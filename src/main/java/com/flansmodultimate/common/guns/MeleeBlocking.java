package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.network.client.gun.PacketMeleeBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Melee blocking with a {@code CanBlock} weapon, for players and other living entities alike. A raised block lasts
 * {@code BlockTime} ticks, or with {@code HoldToBlock} until the key is released, at most that long. While it is up the
 * holder moves at {@code BlockMoveSpeed}, cannot attack with the weapon, blocks frontal hits and may deflect projectiles.
 *
 * <p>
 * The server decides; each side keeps its own state, the client's from {@link PacketMeleeBlock}, so that arm poses and the
 * guard stance render where the server has the block.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MeleeBlocking
{
    /** A held block key renews a timed block once fewer ticks than this are left, as in the Labjac Edition */
    public static final int RENEW_TICKS = 10;
    private static final UUID MOVE_SPEED_MODIFIER = UUID.fromString("3b0c6f4e-5d7a-4f2e-9a61-1c8d2e7b4a90");
    /** The front half-circle, as for shields: the cosine of 90 degrees */
    private static final double FRONT_COS = 0D;

    private static final Map<LivingEntity, Block> serverBlocks = new WeakHashMap<>();
    private static final Map<LivingEntity, Block> clientBlocks = new WeakHashMap<>();
    /** Holders whose held block ran out and who must release the key before raising another, server side */
    private static final Set<LivingEntity> awaitingRelease = Collections.newSetFromMap(new WeakHashMap<>());

    /** A raised block: the hand holding the weapon and the game time it ends at */
    private record Block(InteractionHand hand, long until)
    {}

    private static Map<LivingEntity, Block> blocks(Entity entity)
    {
        return entity.level().isClientSide ? clientBlocks : serverBlocks;
    }

    @Nullable
    private static GunType blockingType(LivingEntity holder, InteractionHand hand)
    {
        ItemStack stack = holder.getItemInHand(hand);
        if (!(stack.getItem() instanceof GunItem gunItem) || !gunItem.getConfigType().isCanBlock() || gunItem.getConfigType().isPoweredOff(stack))
            return null;
        return gunItem.getConfigType();
    }

    /** @return whether the holder has a block raised with the {@code CanBlock} weapon in this hand, on the side it is called on */
    public static boolean isBlocking(LivingEntity holder, InteractionHand hand)
    {
        Block block = blocks(holder).get(holder);
        return block != null && block.hand() == hand && holder.level().getGameTime() < block.until() && blockingType(holder, hand) != null;
    }

    /** @return whether the holder has a block raised with either hand, on the side it is called on */
    public static boolean isBlocking(LivingEntity holder)
    {
        return isBlocking(holder, InteractionHand.MAIN_HAND) || isBlocking(holder, InteractionHand.OFF_HAND);
    }

    /** @return the hand of the raised block, if any */
    public static Optional<InteractionHand> blockingHand(LivingEntity holder)
    {
        for (InteractionHand hand : InteractionHand.values())
            if (isBlocking(holder, hand))
                return Optional.of(hand);
        return Optional.empty();
    }

    /** @return ticks left of the raised block, 0 without one */
    public static int remainingTicks(LivingEntity holder)
    {
        Block block = blocks(holder).get(holder);
        if (block == null || !isBlocking(holder, block.hand()))
            return 0;
        return (int) Math.min(Integer.MAX_VALUE, block.until() - holder.level().getGameTime());
    }

    /** @return whether the weapon in this hand currently acts as a shield: always for a plain shield, while blocking for a {@code CanBlock} one */
    public static boolean isShieldUp(LivingEntity holder, InteractionHand hand)
    {
        ItemStack stack = holder.getItemInHand(hand);
        if (!(stack.getItem() instanceof GunItem gunItem))
            return false;
        GunType type = gunItem.getConfigType();
        return type.isShield() && (!type.isCanBlock() || isBlocking(holder, hand));
    }

    /** @return whether a direction the hit comes from, pointing from the holder towards its source, is in front of the holder */
    public static boolean isFromFront(LivingEntity holder, Vec3 towardsSource)
    {
        Vec3 flat = new Vec3(towardsSource.x, 0D, towardsSource.z);
        if (flat.lengthSqr() < 1.0E-6D)
            return true;
        Vec3 look = holder.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0D, look.z);
        return flatLook.lengthSqr() < 1.0E-6D || flat.normalize().dot(flatLook.normalize()) > FRONT_COS;
    }

    /**
     * Raises or renews a block with the {@code CanBlock} weapon in this hand for its {@code BlockTime}, slows the holder and tells
     * the clients. Server side.
     *
     * @return whether a block is now raised
     */
    public static boolean raise(LivingEntity holder, InteractionHand hand)
    {
        GunType type = blockingType(holder, hand);
        if (holder.level().isClientSide || type == null)
            return false;
        boolean renewing = isBlocking(holder, hand);
        Optional<InteractionHand> other = blockingHand(holder);
        if (other.isPresent() && other.get() != hand)
            lower(holder);

        serverBlocks.put(holder, new Block(hand, holder.level().getGameTime() + type.getBlockTime()));
        applyMoveSpeed(holder, type.getBlockMoveSpeed());
        PacketHandler.sendToTracking(new PacketMeleeBlock(holder.getId(), hand, type.getBlockTime()), holder);
        if (!renewing && StringUtils.isNotBlank(type.getBlockSound()))
            PacketPlaySound.sendSoundPacket(holder, type.getMeleeSoundRange(), type.getBlockSound(), type.isDistortSound());
        return true;
    }

    /** Lowers any raised block and removes its slowdown. Server side. */
    public static void lower(LivingEntity holder)
    {
        if (holder.level().isClientSide)
            return;
        Block block = serverBlocks.remove(holder);
        removeMoveSpeed(holder);
        if (block != null)
            PacketHandler.sendToTracking(new PacketMeleeBlock(holder.getId(), block.hand(), 0), holder);
    }

    /**
     * Ends a block that ran out or whose weapon left the hand. A held block that runs out must be released before the next one.
     * Server side, once per holder tick.
     */
    public static void tick(LivingEntity holder)
    {
        Block block = serverBlocks.get(holder);
        if (block == null)
            return;
        if (holder.level().getGameTime() >= block.until())
        {
            GunType type = blockingType(holder, block.hand());
            if (type != null && type.isHoldToBlock())
                awaitingRelease.add(holder);
            lower(holder);
        }
        else if (blockingType(holder, block.hand()) == null || !holder.isAlive())
            lower(holder);
    }

    /** Ticks the blocks of entities that are not players, which have no per-player tick of their own. Server side. */
    public static void tickNonPlayers()
    {
        for (LivingEntity holder : List.copyOf(serverBlocks.keySet()))
            if (!(holder instanceof Player))
                tick(holder);
    }

    /** @return whether the holder's held block ran out and the key has not been released since; server side */
    public static boolean isAwaitingRelease(LivingEntity holder)
    {
        return awaitingRelease.contains(holder);
    }

    /** Records that the block key was released, allowing a new held block; server side */
    public static void release(LivingEntity holder)
    {
        awaitingRelease.remove(holder);
    }

    /**
     * Whether a projectile flying with this velocity would meet the holder's raised block from the front, before any deflection roll.
     * Either side: clients use it to leave the outcome of such a hit to the server.
     */
    public static boolean meetsDeflectingBlock(LivingEntity holder, Vec3 projectileVelocity)
    {
        Optional<InteractionHand> hand = blockingHand(holder);
        if (hand.isEmpty())
            return false;
        GunType type = blockingType(holder, hand.get());
        return type != null && type.getBlockDeflectChance() > 0F && isFromFront(holder, projectileVelocity.reverse());
    }

    /**
     * Rolls the raised block's {@code BlockDeflectChance} against a frontal projectile. Server side.
     *
     * @return the direction to send the projectile in, where the holder looks, when it is deflected
     */
    public static Optional<Vec3> rollDeflection(LivingEntity holder, Vec3 projectileVelocity)
    {
        if (holder.level().isClientSide || !meetsDeflectingBlock(holder, projectileVelocity))
            return Optional.empty();
        GunType type = blockingType(holder, blockingHand(holder).orElseThrow());
        if (type == null || holder.getRandom().nextFloat() >= type.getBlockDeflectChance())
            return Optional.empty();
        if (StringUtils.isNotBlank(type.getDeflectSound()))
            PacketPlaySound.sendSoundPacket(holder, type.getMeleeSoundRange(), type.getDeflectSound(), type.isDistortSound());
        return Optional.of(holder.getLookAngle());
    }

    /** Applies a block state received from the server; client side. Zero ticks lowers it. */
    public static void applyClient(LivingEntity holder, InteractionHand hand, int ticks)
    {
        if (ticks <= 0)
            clientBlocks.remove(holder);
        else
            clientBlocks.put(holder, new Block(hand, holder.level().getGameTime() + ticks));
    }

    private static void applyMoveSpeed(LivingEntity holder, float moveSpeed)
    {
        AttributeInstance speed = holder.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
            return;
        speed.removeModifier(MOVE_SPEED_MODIFIER);
        if (moveSpeed < 1F)
            speed.addTransientModifier(new AttributeModifier(MOVE_SPEED_MODIFIER, "Melee block", Math.max(0F, moveSpeed) - 1D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeMoveSpeed(LivingEntity holder)
    {
        AttributeInstance speed = holder.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null)
            speed.removeModifier(MOVE_SPEED_MODIFIER);
    }
}
