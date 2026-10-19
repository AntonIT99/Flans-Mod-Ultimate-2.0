package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.api.FlansProjectiles;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Native item charge/reload/cadence while Custom NPCs still selects the combat target. A second Flan gun in the off hand
 * fires on its own cadence and reloads on its own, under the handling rules players have: a two-handed gun does not fire
 * while the other hand is full.
 */
public final class NpcEquipmentRangedGoal extends Goal
{
    private final EntityNPCInterface npc;
    private final Hand main = new Hand(InteractionHand.MAIN_HAND);
    private final Hand off = new Hand(InteractionHand.OFF_HAND);
    private int charging;

    /** Cadence, spin-up and reload progress of the weapon in one hand. */
    private static final class Hand
    {
        private final InteractionHand side;
        private final NpcItemCadence cadence = new NpcItemCadence();
        private ItemStack lastWeapon;
        private Item lastItem;
        private boolean lastThrowable;
        private float spinSpeed;
        private int reloading;
        /** A finished reload loaded nothing; stop cycling reloads until the weapon changes or the goal restarts. */
        private boolean reloadFailed;

        private Hand(InteractionHand side)
        {
            this.side = side;
        }

        private void reset()
        {
            reset(false);
        }

        private void reset(boolean keepCadence)
        {
            spinSpeed = 0F;
            reloading = 0;
            reloadFailed = false;
            if (!keepCadence)
                cadence.reset();
        }
    }

    public NpcEquipmentRangedGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = npc.getTarget();
        return !npc.isKilled() && npc.ais.onAttack == 0 && NpcEquipment.ranged(npc) && target != null && target.isAlive() && target.level() == npc.level()
            && (!npc.isInRange(target, Math.max(0, npc.stats.ranged.getMeleeRange())));
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        LivingEntity target = npc.getTarget();
        if (target == null)
        {
            cancelUse();
            return;
        }
        main.cadence.tick();
        off.cadence.tick();
        main.spinSpeed *= 0.9F;
        off.spinSpeed *= 0.9F;
        npc.getLookControl().setLookAt(target, 30F, 30F);
        if (!npc.isInRange(target, npc.stats.ranged.getRange()) || !npc.getSensing().hasLineOfSight(target))
        {
            npc.getNavigation().moveTo(target, 1D);
            cancelUse();
            return;
        }
        npc.getNavigation().stop();
        // An aircraft below its attack altitude climbs first; it keeps its target and its aim meanwhile.
        if (NpcVehicleState.of(npc).getFlight().holdsFire())
        {
            cancelUse();
            return;
        }
        tickMainHand(target);
        if (NpcEquipment.offHandGun(npc))
            tickOffHand(target);
        else if (off.lastWeapon != null)
        {
            off.lastWeapon = null;
            off.reset();
        }
    }

    private void tickMainHand(LivingEntity target)
    {
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        var properties = FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY);
        if (weapon != main.lastWeapon)
        {
            cancelUse();
            // Taking another copy of the same thrown weapon must not bypass its item cooldown.
            boolean throwable = FlansProjectiles.isGrenade(weapon) || properties.map(value -> value.throwable()).orElse(false);
            main.reset(throwable && main.lastThrowable && weapon.getItem() == main.lastItem);
            main.lastWeapon = weapon;
            main.lastItem = weapon.getItem();
            main.lastThrowable = throwable;
        }
        if (properties.isPresent() && main.spinSpeed < properties.get().spinUpMaximum())
            main.spinSpeed += 2F;
        if (!main.cadence.ready() || reload(main, weapon) || properties.isPresent() && main.spinSpeed < properties.get().spinUpThreshold())
            return;
        if (charge(weapon))
            return;
        aim(target);
        fire(main, target, weapon, properties.map(value -> value.automaticFire() && !value.throwable()).orElse(false));
    }

    private void tickOffHand(LivingEntity target)
    {
        ItemStack weapon = npc.getOffhandItem();
        var properties = NpcEquipment.flan(npc, InteractionHand.OFF_HAND);
        if (properties.isEmpty())
            return;
        if (weapon != off.lastWeapon)
        {
            off.reset();
            off.lastWeapon = weapon;
            // Start half a beat behind the main hand, so two guns alternate instead of firing as one.
            off.cadence.fired(NpcEquipment.shotDelay(weapon) * 0.5D);
        }
        if (off.spinSpeed < properties.get().spinUpMaximum())
            off.spinSpeed += 2F;
        if (!off.cadence.ready() || reload(off, weapon) || off.spinSpeed < properties.get().spinUpThreshold())
            return;
        // A weapon being drawn or charged in the main hand keeps the NPC's aim.
        if (!npc.isUsingItem() || NpcEquipment.shield(npc.getUseItem()))
            aim(target);
        fire(off, target, weapon, properties.get().automaticFire());
    }

    private void fire(Hand hand, LivingEntity target, ItemStack weapon, boolean automatic)
    {
        boolean mainHand = hand == main;
        do
        {
            // A successful throw can empty the stack, so read its cadence before consuming it.
            double delay = mainHand && weapon.getItem() instanceof NpcWeaponAdapter adapter ? adapter.npcShotDelay(weapon) : NpcEquipment.shotDelay(weapon);
            boolean fired = mainHand ? NpcItemAttacks.ranged(npc, target) : NpcItemAttacks.fireFlan(npc, target, hand.side);
            if (mainHand)
                cancelUse();
            hand.cadence.fired(delay);
            if (!fired)
                break;
            launched(target, hand.side);
        } while (automatic && hand.cadence.ready() && npc.getItemInHand(hand.side) == weapon && FlansEquipment.hasLoadedRound(weapon));
    }

    private boolean charge(ItemStack weapon)
    {
        int chargeTime = NpcEquipment.chargeTime(weapon, npc);
        if (chargeTime > 0 && charging++ < chargeTime)
        {
            boolean throwable = FlansProjectiles.isGrenade(weapon) || FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY).map(value -> value.throwable()).orElse(false);
            if (throwable)
            {
                if (NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.WEAPON_ANIMATIONS))
                    NpcThrowPresentation.show(npc, weapon);
                else
                    NpcThrowPresentation.clear(npc);
            }
            // Virtual throws use only the synchronized display; vanilla bow/crossbow use still owns its normal active hand.
            else if (weapon == npc.getMainHandItem() && (!npc.isUsingItem() || npc.getUsedItemHand() != InteractionHand.MAIN_HAND))
            {
                npc.stopUsingItem();
                npc.startUsingItem(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private void launched(LivingEntity target, InteractionHand side)
    {
        NpcItemAttacks.launched(npc, target);
        if (NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.WEAPON_ANIMATIONS))
            npc.swing(side);
    }

    private boolean reload(Hand hand, ItemStack weapon)
    {
        if (weapon != npc.getItemInHand(hand.side))
            return false;
        var flan = NpcEquipment.flan(npc, hand.side);
        if (flan.isEmpty() || flan.get().throwable() || FlansEquipment.hasLoadedRound(weapon))
        {
            hand.reloadFailed = false;
            return false;
        }
        if (hand.reloadFailed)
            return true;
        // An NPC that uses up its ammunition does not go through the motions of a reload it has nothing for.
        if (hand.reloading <= 0 && NpcAmmunition.exhausted(npc, weapon))
        {
            outOfAmmunition(hand);
            return true;
        }
        if (hand.reloading <= 0)
        {
            hand.reloading = Math.max(1, (int) Math.ceil(flan.get().reloadTime()));
            NpcReloadFeedback.reload(npc, hand.side, hand.reloading);
        }
        if (--hand.reloading <= 0 && !NpcItemAttacks.loadFlanGun(npc, hand.side))
            outOfAmmunition(hand);
        return true;
    }

    /** The reload loaded nothing. The main hand may take another weapon, which the next tick finds there. */
    private void outOfAmmunition(Hand hand)
    {
        hand.reloadFailed = hand != main || !NpcAmmunition.switchWeapon(npc);
    }

    private void aim(LivingEntity target)
    {
        var delta = target.getEyePosition().subtract(npc.getEyePosition());
        npc.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        npc.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
        npc.yHeadRot = npc.getYRot();
    }

    private void cancelUse()
    {
        charging = 0;
        NpcThrowPresentation.clear(npc);
        if (npc.isUsingItem() && !NpcEquipment.shield(npc.getUseItem()))
            npc.stopUsingItem();
    }

    @Override
    public void stop()
    {
        cancelUse();
        main.reloading = 0;
        main.reloadFailed = false;
        main.spinSpeed = 0F;
        off.reloading = 0;
        off.reloadFailed = false;
        off.spinSpeed = 0F;
        npc.getNavigation().stop();
    }
}
