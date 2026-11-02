package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Optional;

/** Target-aware choices from real carried items. Unknown protection is never treated as immunity. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWeaponChoice
{
    public enum Effectiveness
    {
        EFFECTIVE, INEFFECTIVE, UNKNOWN
    }

    public static Effectiveness effectiveness(EntityNPCInterface npc, LivingEntity target)
    {
        if (!NpcVehicleThreats.groundVehicle(target))
            return Effectiveness.EFFECTIVE;
        Optional<PartArmor> armor = NpcVehicleThreats.protection(target, npc.position());
        if (armor.isEmpty())
            return Effectiveness.UNKNOWN;
        boolean known = false;
        for (ItemStack weapon : NpcAmmunition.supplies(npc))
        {
            if (unusable(npc, weapon))
                continue;
            ItemStack ammunition = ammunition(npc, weapon);
            var penetration = FlansEquipment.getArmorPenetration(weapon, ammunition);
            if (penetration.isPresent())
            {
                known = true;
                boolean heat = FlansProjectiles.getArmorPerformance(ammunition, 0).map(AmmunitionArmor::shapedCharge).orElse(false);
                if (penetration.getAsDouble() >= (heat ? armor.get().heatThicknessMm() : armor.get().thicknessMm()))
                    return Effectiveness.EFFECTIVE;
            }
        }
        return known ? Effectiveness.INEFFECTIVE : Effectiveness.UNKNOWN;
    }

    public static boolean antiArmor(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack ammunition = ammunition(npc, weapon);
        return FlansEquipment.getArmorPenetration(weapon, ammunition).orElse(0D) >= 40D;
    }

    public static ItemStack ammunition(EntityNPCInterface npc, ItemStack weapon)
    {
        return FlansEquipment.getLoadedAmmunition(weapon)
            .or(() -> NpcAmmunition.supplies(npc).stream().filter(ammo -> FlansEquipment.acceptsAmmunition(weapon, ammo))
                .max(Comparator.comparingDouble(ammo -> FlansProjectiles.getArmorPerformance(ammo, 0).map(AmmunitionArmor::penetrationMm).orElse(0D))))
            .or(() -> FlansEquipment.createDefaultAmmunition(weapon).filter(ammo -> FlansEquipment.acceptsAmmunition(weapon, ammo))).orElse(ItemStack.EMPTY);
    }

    /** Scores compatible ammunition only; callers remain responsible for compatibility and actual supply. */
    public static double ammunitionScore(EntityNPCInterface npc, LivingEntity target, ItemStack ammunition)
    {
        double reach = NpcBlastSafety.reach(ammunition);
        if (reach > 0D && npc.distanceTo(target) < reach + 2D)
            return -1000D;
        double penetration = FlansProjectiles.getArmorPerformance(ammunition, 0).map(AmmunitionArmor::penetrationMm).orElse(0D);
        return NpcVehicleThreats.groundVehicle(target) ? penetration : Math.min(20D, reach * 4D) - Math.min(5D, penetration / 100D);
    }

    private static boolean unusable(EntityNPCInterface npc, ItemStack weapon)
    {
        return !NpcEquipment.ranged(weapon) || FlansEquipment.isDeployable(weapon) || NpcEquipment.shield(weapon) || !(FlansEquipment.hasLoadedRound(weapon) || supplied(npc, weapon));
    }

    private static boolean supplied(EntityNPCInterface npc, ItemStack weapon)
    {
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return NpcAmmunition.supplies(npc).stream().anyMatch(ammo -> FlansEquipment.acceptsAmmunition(weapon, ammo));
        return !ammunition(npc, weapon).isEmpty();
    }

    static double score(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        if (unusable(npc, weapon))
            return -1D;
        ItemStack ammunition = ammunition(npc, weapon);
        Optional<EquippedWeaponProperties> properties = FlansEquipment.getWeaponProperties(weapon, ammunition);
        if (properties.isEmpty())
            return 1D;
        double blast = NpcBlastSafety.reach(ammunition);
        if (blast > 0D && npc.distanceTo(target) < blast + 2D)
            return 0D;
        double penetration = FlansEquipment.getArmorPenetration(weapon, ammunition).orElse(0D);
        if (NpcVehicleThreats.groundVehicle(target))
            return 2D + Math.min(300D, penetration) + Math.min(12D, blast);
        double delay = Math.max(1D, properties.get().shotDelay());
        return 10D + Math.min(30D, properties.get().damage() / delay) - Math.min(9D, blast * 2D);
    }

    /** Exchanges actual inventory entries only when the new choice is materially better, with a switch cooldown. */
    public static boolean select(EntityNPCInterface npc, LivingEntity target)
    {
        if (!(npc instanceof NpcTacticState.Holder) || !NpcEquipment.enabled(npc, Feature.TACTICAL_WEAPON_SWITCH) || npc.isPassenger() || !NpcEquipment.enabled(npc, Feature.ITEM_WEAPONS))
            return false;
        long now = npc.level().getGameTime();
        var state = NpcTacticState.of(npc).getSafety();
        if (now - state.getWeaponChangedAt() < 40L)
            return false;
        ItemStack held = npc.getMainHandItem();
        // Do not put another two-handed gun beside a shield or a different weapon.
        if (!npc.getOffhandItem().isEmpty())
            return false;
        var items = NpcAmmunition.inventory(npc);
        double current = score(npc, target, held);
        int slot = NpcAmmunition.best(items, item -> score(npc, target, item), Math.max(1D, current * 1.25D));
        if (slot < 0)
            return false;
        NpcAmmunition.swap(npc, 0, slot);
        state.setWeaponChangedAt(now);
        state.setSelectedWeapon(npc.getMainHandItem());
        npc.updateClient = true;
        return true;
    }
}
