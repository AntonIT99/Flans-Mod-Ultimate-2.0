package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.*;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.gun.PacketHeldItemPresentation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Saved presentation and transient server state needed by clients selecting an arm rule. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeldItemPresentation
{
    private static final String KEY = "FlansHeldItemPresentation";
    private static final String TARGETING = "FlansHeldItemTargeting";
    private static final String SHOT_HOLD = "ShotHoldTicks";
    private static final Map<LivingEntity, Boolean> targets = new WeakHashMap<>();

    public static HeldItemSettings get(LivingEntity holder, InteractionHand hand)
    {
        if (holder instanceof IHeldItemPresentation storage)
            return storage.flansmodultimateHeldItemSettings(hand);
        CompoundTag persistent = holder.getPersistentData();
        if (persistent == null || !persistent.contains(KEY))
            return HeldItemSettings.DEFAULT;
        CompoundTag tag = persistent.getCompound(KEY).getCompound(hand.name());
        return tag.isEmpty() ? HeldItemSettings.DEFAULT : load(tag);
    }

    public static void set(LivingEntity holder, InteractionHand hand, HeldItemSettings settings)
    {
        if (!(holder.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Held item presentation changes require the server entity thread");
        Objects.requireNonNull(hand);
        Objects.requireNonNull(settings);
        if (settings.equals(get(holder, hand)))
            return;
        store(holder, hand, settings);
        PacketHandler.sendToTracking(packet(holder), holder);
    }

    private static void store(LivingEntity holder, InteractionHand hand, HeldItemSettings settings)
    {
        if (holder instanceof IHeldItemPresentation storage)
            storage.flansmodultimateHeldItemSettings(hand, settings);
        else
        {
            CompoundTag tag = holder.getPersistentData().getCompound(KEY);
            tag.put(hand.name(), save(settings));
            holder.getPersistentData().put(KEY, tag);
        }
    }

    public static void receive(LivingEntity holder, HeldItemSettings main, HeldItemSettings off, boolean targeting)
    {
        if (!holder.level().isClientSide)
            return;
        store(holder, InteractionHand.MAIN_HAND, main);
        store(holder, InteractionHand.OFF_HAND, off);
        holder.getPersistentData().putBoolean(TARGETING, targeting);
    }

    public static void sendTo(LivingEntity holder, ServerPlayer receiver)
    {
        if (get(holder, InteractionHand.MAIN_HAND).equals(HeldItemSettings.DEFAULT) && get(holder, InteractionHand.OFF_HAND).equals(HeldItemSettings.DEFAULT))
            return;
        PacketHandler.sendTo(packet(holder), receiver);
    }

    private static PacketHeldItemPresentation packet(LivingEntity holder)
    {
        return new PacketHeldItemPresentation(holder.getId(), get(holder, InteractionHand.MAIN_HAND), get(holder, InteractionHand.OFF_HAND), targeting(holder));
    }

    public static void tick(LivingEntity holder)
    {
        if (get(holder, InteractionHand.MAIN_HAND).equals(HeldItemSettings.DEFAULT) && get(holder, InteractionHand.OFF_HAND).equals(HeldItemSettings.DEFAULT))
        {
            targets.remove(holder);
            return;
        }
        boolean targeting = targeting(holder);
        Boolean previous = targets.put(holder, targeting);
        if (previous == null || previous != targeting)
            PacketHandler.sendToTracking(packet(holder), holder);
    }

    private static boolean targeting(LivingEntity holder)
    {
        if (holder.level().isClientSide)
            return holder.getPersistentData().getBoolean(TARGETING);
        return holder instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive();
    }

    public static ArmPoseRule rule(LivingEntity holder, InteractionHand hand)
    {
        WeaponPoseRules rules = get(holder, hand).poses();
        if (rules.equals(WeaponPoseRules.DEFAULT))
            return ArmPoseRule.DEFAULT;
        WeaponPoseRules.State state;
        if (GunArmPoses.chargingThrowHand(holder) == hand || holder.isUsingItem() && holder.getUsedItemHand() == hand)
            state = WeaponPoseRules.State.USING;
        else if (GunArmPoses.isFiring(holder, hand, rules.shotHoldTicks()))
            state = WeaponPoseRules.State.FIRING;
        else if (targeting(holder))
            state = WeaponPoseRules.State.TARGETING;
        else if (holder.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5 || Math.abs(holder.getX() - holder.xo) + Math.abs(holder.getZ() - holder.zo) > 0.003)
            state = WeaponPoseRules.State.MOVING;
        else
            state = WeaponPoseRules.State.IDLE;
        ArmPoseRule selected = rules.get(state);
        return selected.pose() == ArmPoseRule.Pose.CUSTOM || holder.getItemInHand(hand).getItem() instanceof IFlanItem<?> ? selected : ArmPoseRule.DEFAULT;
    }

    public static CompoundTag save(HeldItemSettings settings)
    {
        CompoundTag tag = new CompoundTag();
        HeldItemTransform t = settings.transform();
        tag.putFloat("X", t.x());
        tag.putFloat("Y", t.y());
        tag.putFloat("Z", t.z());
        tag.putFloat("RotationX", t.rotationX());
        tag.putFloat("RotationY", t.rotationY());
        tag.putFloat("RotationZ", t.rotationZ());
        tag.putInt(SHOT_HOLD, settings.poses().shotHoldTicks());
        for (WeaponPoseRules.State state : WeaponPoseRules.State.values())
        {
            ArmPoseRule rule = settings.poses().get(state);
            CompoundTag pose = new CompoundTag();
            pose.putString("Pose", rule.pose().name());
            pose.putInt("Axes", rule.axes());
            pose.putFloat("X", rule.x());
            pose.putFloat("Y", rule.y());
            pose.putFloat("Z", rule.z());
            tag.put(state.name(), pose);
        }
        return tag;
    }

    public static HeldItemSettings load(CompoundTag tag)
    {
        HeldItemTransform transform = new HeldItemTransform(number(tag, "X"), number(tag, "Y"), number(tag, "Z"), number(tag, "RotationX"), number(tag, "RotationY"), number(tag, "RotationZ"));
        WeaponPoseRules rules = WeaponPoseRules.DEFAULT;
        for (WeaponPoseRules.State state : WeaponPoseRules.State.values())
        {
            CompoundTag pose = tag.getCompound(state.name());
            ArmPoseRule.Pose choice;
            try
            {
                choice = ArmPoseRule.Pose.valueOf(pose.getString("Pose"));
            }
            catch (IllegalArgumentException ignored)
            {
                choice = ArmPoseRule.Pose.DEFAULT;
            }
            rules = rules.with(state, new ArmPoseRule(choice, pose.getInt("Axes"), number(pose, "X"), number(pose, "Y"), number(pose, "Z")));
        }
        if (tag.contains(SHOT_HOLD))
            rules = new WeaponPoseRules(rules.idle(), rules.moving(), rules.targeting(), rules.firing(), rules.using(), tag.getInt(SHOT_HOLD));
        return new HeldItemSettings(transform, rules);
    }

    private static float number(CompoundTag tag, String key)
    {
        float value = tag.getFloat(key);
        return Float.isFinite(value) ? value : 0F;
    }
}
