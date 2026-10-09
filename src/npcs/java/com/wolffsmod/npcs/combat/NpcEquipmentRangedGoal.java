package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.FlansEquipment;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/** Native item charge/reload/cadence while Custom NPCs still selects the combat target. */
public final class NpcEquipmentRangedGoal extends Goal
{
    private final EntityNPCInterface npc;
    private final NpcItemCadence cadence = new NpcItemCadence();
    private ItemStack lastWeapon;
    private float spinSpeed;
    private int charging;
    private int reloading;
    /** A finished reload loaded nothing; stop cycling reloads until the weapon changes or the goal restarts. */
    private boolean reloadFailed;

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
            return;
        cadence.tick();
        spinSpeed *= 0.9F;
        npc.getLookControl().setLookAt(target, 30F, 30F);
        if (!npc.isInRange(target, npc.stats.ranged.getRange()) || !npc.getSensing().hasLineOfSight(target))
        {
            npc.getNavigation().moveTo(target, 1D);
            cancelUse();
            return;
        }
        npc.getNavigation().stop();
        ItemStack weapon = npc.getMainHandItem();
        updateWeapon(weapon);
        var properties = NpcEquipment.flan(npc);
        if (properties.isPresent() && spinSpeed < properties.get().spinUpMaximum())
            spinSpeed += 2F;
        if (!cadence.ready() || reload(weapon) || properties.isPresent() && spinSpeed < properties.get().spinUpThreshold())
            return;
        if (charge(weapon))
            return;
        aim(target);
        boolean automatic = properties.map(value -> value.automaticFire() && !value.throwable()).orElse(false);
        fire(target, weapon, automatic);
    }

    private void updateWeapon(ItemStack weapon)
    {
        if (weapon == lastWeapon)
            return;
        cancelUse();
        spinSpeed = 0F;
        reloading = 0;
        reloadFailed = false;
        cadence.reset();
        lastWeapon = weapon;
    }

    private void fire(LivingEntity target, ItemStack weapon, boolean automatic)
    {
        do
        {
            boolean fired = NpcItemAttacks.ranged(npc, target);
            cancelUse();
            double delay = weapon.getItem() instanceof NpcWeaponAdapter adapter ? adapter.npcShotDelay(weapon) : NpcEquipment.shotDelay(weapon);
            cadence.fired(delay);
            if (!fired)
                break;
            launched(target);
        } while (automatic && cadence.ready() && npc.getMainHandItem() == weapon && FlansEquipment.hasLoadedRound(weapon));
    }

    private boolean charge(ItemStack weapon)
    {
        int chargeTime = NpcEquipment.chargeTime(weapon);
        if (chargeTime > 0 && charging++ < chargeTime)
        {
            if (!npc.isUsingItem() || npc.getUsedItemHand() != InteractionHand.MAIN_HAND)
            {
                npc.stopUsingItem();
                npc.startUsingItem(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private void launched(LivingEntity target)
    {
        NpcItemAttacks.launched(npc, target);
        if (NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.WEAPON_ANIMATIONS))
            npc.swing(InteractionHand.MAIN_HAND);
    }

    private boolean reload(ItemStack weapon)
    {
        var flan = NpcEquipment.flan(npc);
        if (flan.isEmpty() || flan.get().throwable() || FlansEquipment.hasLoadedRound(weapon))
        {
            reloadFailed = false;
            return false;
        }
        if (reloadFailed)
            return true;
        if (reloading <= 0)
        {
            reloading = Math.max(1, (int) Math.ceil(flan.get().reloadTime()));
            if (NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.FLAN_SOUNDS))
                FlansEquipment.playReloadSound(npc, weapon);
        }
        if (--reloading <= 0)
            reloadFailed = !NpcItemAttacks.loadFlanGun(npc);
        return true;
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
        if (npc.isUsingItem() && !NpcEquipment.shield(npc.getUseItem()))
            npc.stopUsingItem();
    }

    @Override
    public void stop()
    {
        cancelUse();
        reloading = 0;
        reloadFailed = false;
        spinSpeed = 0F;
        npc.getNavigation().stop();
    }
}
