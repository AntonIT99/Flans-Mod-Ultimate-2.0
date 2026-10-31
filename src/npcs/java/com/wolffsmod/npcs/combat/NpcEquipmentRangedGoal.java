package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.WeaponKind;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.diag.NpcWatch;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Native item charge/reload/cadence while Custom NPCs still selects the combat target. A second Flan gun in the off hand
 * fires on its own cadence and reloads on its own, under the handling rules players have: a two-handed gun does not fire
 * while the other hand is full. A Flan gun in the off hand with no ranged weapon in the main hand is fired as the NPC's
 * only gun.
 *
 * <p>
 * The goal only aims and fires, and claims no flag: where the NPC goes meanwhile is up to the tactical movement Custom NPCs
 * chose for it, or else to {@link NpcCombatApproachGoal}.
 * </p>
 */
public final class NpcEquipmentRangedGoal extends Goal
{
    private final EntityNPCInterface npc;
    private final Hand main = new Hand(InteractionHand.MAIN_HAND);
    private final Hand off = new Hand(InteractionHand.OFF_HAND);
    private int charging;
    /** Whether the NPC has fired since it took up this engagement, which Custom NPCs' tactics wait for. */
    @Getter
    private boolean fired;

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
    }

    /** @return the goal of the NPC, once Custom NPCs has set up its goals */
    public static Optional<NpcEquipmentRangedGoal> of(EntityNPCInterface npc)
    {
        return npc instanceof Holder holder ? Optional.ofNullable(holder.wolffsmodnpcsItemGoal()) : Optional.empty();
    }

    /** @return whether the NPC fights its target with an equipped ranged weapon: it has one, and the target is beyond melee range */
    public static boolean engages(EntityNPCInterface npc)
    {
        LivingEntity target = npc.getTarget();
        return !com.wolffsmod.npcs.sim.NpcRepairs.active(npc) && !NpcCrew.holdsFire(npc) && !npc.isKilled() && npc.ais.onAttack == 0 && NpcEquipment.ranged(npc) && target != null && target.isAlive()
            && target.level() == npc.level() && !rushes(npc) && (!npc.isInRange(target, Math.max(0, npc.stats.ranged.getMeleeRange())));
    }

    /** A throwable gun with a melee attack closes in while it waits for its next throw. */
    public static boolean rushes(EntityNPCInterface npc)
    {
        return of(npc).map(goal -> NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ITEM_WEAPONS) && goal.main.lastThrowable && !goal.main.cadence.ready()
            && NpcEquipment.meleeWeapon(npc.getMainHandItem()) && FlansEquipment.getWeaponKind(NpcEquipment.rangedWeapon(npc)).filter(WeaponKind::throwable).isPresent()).orElse(false);
    }

    @Override
    public boolean canUse()
    {
        return !npc.isKilled() && (main.reloading > 0 || off.reloading > 0 || !main.cadence.ready() || !off.cadence.ready() || engages(npc));
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        main.cadence.tick();
        off.cadence.tick();
        if (NpcThrowGoal.throwing(npc))
        {
            // A throw beside the weapon shows its own item in hand; the weapon waits, and a reload it has begun goes on.
            charging = 0;
            if (npc.isUsingItem() && !NpcEquipment.shield(npc.getUseItem()))
                npc.stopUsingItem();
            continueReload(main);
            continueReload(off);
            return;
        }
        LivingEntity target = npc.getTarget();
        if (target == null || !engages(npc))
        {
            cancelUse();
            continueReload(main);
            continueReload(off);
            return;
        }
        main.spinSpeed *= 0.9F;
        off.spinSpeed *= 0.9F;
        npc.getLookControl().setLookAt(target, 30F, 30F);
        if (!npc.isInRange(target, npc.stats.ranged.getRange()) || !npc.getSensing().hasLineOfSight(target))
        {
            cancelUse();
            // A reload goes on out of sight, which is where an NPC fighting from cover goes to reload.
            continueReload(main);
            continueReload(off);
            return;
        }
        // An aircraft below its attack altitude climbs first; it keeps its target and its aim meanwhile.
        // An NPC at the wheel of a Flan's driveable fights with the driveable's weapons.
        if (com.wolffsmod.npcs.sim.NpcRepairs.active(npc) || NpcVehicleState.of(npc).getFlight().holdsFire() || NpcDrivers.holdsFire(npc))
        {
            cancelUse();
            continueReload(main);
            continueReload(off);
            return;
        }
        if (NpcEquipment.offHandPrimary(npc))
        {
            // The off-hand gun is the NPC's weapon; what the main hand holds is for melee or blocking.
            if (main.lastWeapon != null)
            {
                main.lastWeapon = null;
                main.reset();
            }
            tickOffHand(target, true);
            return;
        }
        tickMainHand(target);
        if (NpcEquipment.offHandGun(npc))
            tickOffHand(target, false);
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
            boolean throwable = FlansProjectiles.isGrenade(weapon) || properties.map(EquippedWeaponProperties::throwable).orElse(false);
            main.reset(throwable && main.lastThrowable && weapon.getItem() == main.lastItem);
            main.lastWeapon = weapon;
            main.lastItem = weapon.getItem();
            main.lastThrowable = throwable;
        }
        if (properties.isPresent() && main.spinSpeed < properties.get().spinUpMaximum())
            main.spinSpeed += 2F;
        // Reload progress overlaps the previous shot's cooldown instead of waiting for it to expire first.
        if (reload(main, weapon) || !main.cadence.ready() || properties.isPresent() && main.spinSpeed < properties.get().spinUpThreshold())
            return;
        if (charge(weapon))
            return;
        if (NpcSkill.holdsFire(npc, target))
            return;
        aim(target);
        fire(main, target, weapon, properties.map(value -> value.automaticFire() && !value.throwable()).orElse(false));
    }

    /**
     * @param alone
     *            whether the off-hand gun is the only one the NPC fires, rather than a second gun beside the main hand's
     */
    private void tickOffHand(LivingEntity target, boolean alone)
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
            if (!alone)
                off.cadence.fired(NpcEquipment.shotDelay(weapon) * 0.5D);
        }
        if (off.spinSpeed < properties.get().spinUpMaximum())
            off.spinSpeed += 2F;
        if (reload(off, weapon) || !off.cadence.ready() || off.spinSpeed < properties.get().spinUpThreshold() || NpcSkill.holdsFire(npc, target))
            return;
        // A weapon being drawn or charged in the main hand keeps the NPC's aim.
        if (alone || !npc.isUsingItem() || NpcEquipment.shield(npc.getUseItem()))
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
            boolean shot = mainHand ? NpcItemAttacks.ranged(npc, target) : NpcItemAttacks.fireFlan(npc, target, hand.side);
            if (mainHand)
                cancelUse();
            hand.cadence.fired(delay);
            if (!shot)
                break;
            fired = true;
            NpcWatch.attack(npc);
            launched(target, hand.side);
        } while (automatic && hand.cadence.ready() && npc.getItemInHand(hand.side) == weapon && FlansEquipment.hasLoadedRound(weapon));
    }

    private boolean charge(ItemStack weapon)
    {
        int chargeTime = NpcEquipment.chargeTime(weapon, npc);
        if (chargeTime > 0 && charging++ < chargeTime)
        {
            boolean throwable = FlansProjectiles.isGrenade(weapon) || FlansEquipment.getWeaponKind(weapon).map(WeaponKind::throwable).orElse(false);
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
        NpcItemAttacks.launched(npc, target, side);
        // Gunfire has its own shot animation. A native arm swing is reserved for throws and melee attacks.
        if (main.lastThrowable && side == InteractionHand.MAIN_HAND && NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.WEAPON_ANIMATIONS))
            npc.swing(side);
    }

    private boolean reload(Hand hand, ItemStack weapon)
    {
        if (weapon != npc.getItemInHand(hand.side))
            return false;
        var flan = NpcEquipment.flan(npc, hand.side);
        if (flan.isEmpty() || flan.get().throwable() || FlansEquipment.hasLoadedRound(weapon))
        {
            hand.reloading = 0;
            hand.reloadFailed = false;
            return false;
        }
        if (hand.reloadFailed)
            return true;
        // An NPC does not go through the motions of a reload it has nothing for, which it would repeat whenever the goal restarts.
        if (hand.reloading <= 0 && NpcAmmunition.nothingToLoad(npc, weapon))
        {
            outOfAmmunition(hand);
            return true;
        }
        if (hand.reloading <= 0)
        {
            hand.reloading = Math.max(1, (int) Math.ceil(flan.get().reloadTime()));
            NpcReloadFeedback.reload(npc, hand.side, hand.reloading);
            NpcTacticState.of(npc).reloading(npc.level().getGameTime(), hand.reloading);
        }
        if (--hand.reloading > 0)
            return true;
        if (!NpcItemAttacks.loadFlanGun(npc, hand.side))
        {
            outOfAmmunition(hand);
            return true;
        }
        return false;
    }

    /** Carries on a reload the hand has started, as long as it still holds the weapon it started it for. */
    private void continueReload(Hand hand)
    {
        if (hand.reloading <= 0)
            return;
        if (hand.lastWeapon != null && hand.lastWeapon == npc.getItemInHand(hand.side))
            reload(hand, hand.lastWeapon);
        else
            hand.reset();
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
        main.reloadFailed = false;
        main.spinSpeed = 0F;
        off.reloadFailed = false;
        off.spinSpeed = 0F;
        fired = false;
    }

    /** Implemented on the Custom NPCs entity, which keeps the goal it last set up. */
    public interface Holder
    {
        @Nullable
        NpcEquipmentRangedGoal wolffsmodnpcsItemGoal();
    }
}
