package com.wolffsmod.npcs.scripting;

import com.flansmodultimate.api.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.ScriptContainer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * The {@code flans} global in Custom NPCs script executors. Wrapper conversion belongs here, leaving the main API free
 * of Custom NPCs classes. Mutations run on the server entity thread and use the same API and storage as the editor.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused") // Public methods are invoked dynamically by Custom NPCs script engines.
public final class FlansScriptApi
{
    private static final FlansScriptApi INSTANCE = new FlansScriptApi();

    /** Registers through Custom NPCs' executor bindings, before scripts are initialized. Common setup only. */
    public static void register()
    {
        ScriptContainer.Data.put("flans", INSTANCE);
    }

    /** Unwraps any script entity for the main-mod public API, including nonliving driveables and projectiles. */
    public Entity entity(IEntity<?> wrapper)
    {
        return wrapper.getMCEntity();
    }

    /** Unwraps a living script entity for equipment APIs, rejecting nonliving entities. */
    public LivingEntity living(IEntity<?> wrapper)
    {
        if (wrapper.getMCEntity() instanceof LivingEntity living)
            return living;
        throw new IllegalArgumentException("Flan equipment APIs require a living entity");
    }

    /** Unwraps a script item for any main-mod public API. */
    public ItemStack stack(IItemStack wrapper)
    {
        return wrapper.getMCItemStack();
    }

    /** Returns immutable settings; hand is main/off or MAIN_HAND/OFF_HAND, case insensitive. Either side. */
    public HeldItemSettings getHeldItemSettings(IEntity<?> holder, String hand)
    {
        return FlansItemAnimations.get(living(holder), hand(hand));
    }

    /** Saves and synchronizes immutable settings assembled with the public API types; server thread only. */
    public void setHeldItemSettings(IEntity<?> holder, String hand, HeldItemSettings settings)
    {
        FlansItemAnimations.set(living(holder), hand(hand), settings);
    }

    /** Sets item offsets in model pixels and XYZ rotations in degrees; preserves arm rules. Server thread only. */
    @SuppressWarnings("java:S107") // Six transform components plus the script holder and hand form one operation.
    public void setItemTransform(IEntity<?> holder, String hand, float x, float y, float z, float rotationX, float rotationY, float rotationZ)
    {
        HeldItemSettings old = getHeldItemSettings(holder, hand);
        setHeldItemSettings(holder, hand, new HeldItemSettings(new HeldItemTransform(x, y, z, rotationX, rotationY, rotationZ), old.poses()));
    }

    /** Sets a preset for IDLE/MOVING/TARGETING/FIRING/USING; shields keep their normal pose. Server thread only. */
    public void setArmPose(IEntity<?> holder, String hand, String state, String pose)
    {
        setRule(holder, hand, state, ArmPoseRule.of(ArmPoseRule.Pose.valueOf(key(pose))));
    }

    /**
     * Sets explicit body-relative XYZ arm rotations in degrees. Axis bits 1/2/4 select X/Y/Z; unselected axes inherit their
     * normal pose. This is the only override allowed for shield arms. Server thread only.
     */
    public void setArmRotations(IEntity<?> holder, String hand, String state, int axes, float x, float y, float z)
    {
        setRule(holder, hand, state, new ArmPoseRule(ArmPoseRule.Pose.CUSTOM, axes, x, y, z));
    }

    /** Sets how long a shot retains the firing pose, in ticks (0..200); server thread only. */
    public void setShotHoldTicks(IEntity<?> holder, String hand, int ticks)
    {
        HeldItemSettings old = getHeldItemSettings(holder, hand);
        WeaponPoseRules rules = old.poses();
        setHeldItemSettings(holder, hand, new HeldItemSettings(old.transform(), new WeaponPoseRules(rules.idle(), rules.moving(), rules.targeting(), rules.firing(), rules.using(), ticks)));
    }

    /** Clears placement and all arm overrides for this hand; server thread only. */
    public void resetHeldItem(IEntity<?> holder, String hand)
    {
        FlansItemAnimations.reset(living(holder), hand(hand));
    }

    /** Plays a native held-gun shot animation without firing or consuming ammunition; server thread only. */
    public void animateShot(IEntity<?> holder, String hand)
    {
        FlansEquipment.animateShot(living(holder), hand(hand));
    }

    /** Plays a native held-gun reload animation for the supplied ticks without changing ammunition; server thread only. */
    public void animateReload(IEntity<?> holder, String hand, float ticks)
    {
        FlansEquipment.animateReload(living(holder), hand(hand), ticks);
    }

    /** Plays the native held-gun melee animation without applying damage; server thread only. */
    public void animateMelee(IEntity<?> holder, String hand)
    {
        FlansEquipment.animateMelee(living(holder), hand(hand));
    }

    private void setRule(IEntity<?> holder, String hand, String state, ArmPoseRule rule)
    {
        HeldItemSettings old = getHeldItemSettings(holder, hand);
        setHeldItemSettings(holder, hand, new HeldItemSettings(old.transform(), old.poses().with(WeaponPoseRules.State.valueOf(key(state)), rule)));
    }

    private static InteractionHand hand(String name)
    {
        return switch (key(name))
        {
            case "MAIN", "MAIN_HAND" -> InteractionHand.MAIN_HAND;
            case "OFF", "OFF_HAND" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("Hand must be main or off: " + name);
        };
    }

    private static String key(String name)
    {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
