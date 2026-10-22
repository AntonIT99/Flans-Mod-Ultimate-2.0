package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Tells what each Flan class does in a squad besides fighting, from what it carries: a medical tool makes a medic, a
 * repair tool an engineer, a gun that goes through armour or a launcher with a big blast an anti-tank soldier, and a
 * deployable or belt-fed gun a support soldier. A team may set the role of a class by hand instead. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotRoles
{
    /** Millimetres of armour at 100 metres a round has to go through for its gun to count as anti-tank. */
    static final double ANTI_TANK_PENETRATION = 40D;
    /** Blocks of blast radius that make a launcher anti-tank whatever its penetration. */
    static final double ANTI_TANK_BLAST = 2D;
    /** Rounds a magazine of a light machine gun holds at least. */
    static final int SUPPORT_MAGAZINE = 50;
    /** Health a medic gives back, or hit points an engineer repairs, when the class's tool names no amount. */
    static final int DEFAULT_AMOUNT = 4;
    static final int DEFAULT_REPAIR = 10;

    /** What a class carries that matters for its role, worked out once per class. */
    record Kit(BotSettings.ClassRole role, int healAmount, int repairAmount)
    {}

    private static final Map<String, Kit> KITS = new HashMap<>();

    /** @return the role of a class in a team: the one the team set for it, or the one its equipment suggests */
    static BotSettings.ClassRole of(BotSettings.TeamForce force, String playerClass)
    {
        BotSettings.ClassRole set = force.getClassRoles().get(playerClass);
        return set != null ? set : kit(playerClass).role();
    }

    /** @return what a class carries, worked out from its starting items the first time it is asked for */
    static Kit kit(String playerClass)
    {
        return KITS.computeIfAbsent(playerClass, name -> FlansModApi.getPlayerClass(name).map(found -> detect(found.createStartingItems())).orElse(new Kit(BotSettings.ClassRole.RIFLEMAN, 0, 0)));
    }

    /** Forgets what classes carry, so content and class edits are taken up by the next round. */
    static void forget()
    {
        KITS.clear();
    }

    static Kit detect(List<ItemStack> items)
    {
        int heal = 0;
        int repair = 0;
        boolean antiTank = false;
        boolean support = false;
        for (ItemStack item : items)
        {
            Optional<ToolProperties> tool = FlansEquipment.getToolProperties(item);
            if (tool.isPresent())
            {
                heal = Math.max(heal, amount(tool.get(), tool.get().heals(), DEFAULT_AMOUNT));
                repair = Math.max(repair, amount(tool.get(), tool.get().repairs(), DEFAULT_REPAIR));
            }
            else
            {
                antiTank |= antiTank(item, items);
                support |= support(item, items);
            }
        }
        return new Kit(role(heal > 0, repair > 0, antiTank, support), heal, repair);
    }

    /** What a tool gives with each use for one of its jobs: its amount, a default when it names none, or 0 when it does not do the job. */
    private static int amount(ToolProperties tool, boolean does, int fallback)
    {
        if (!does)
            return 0;
        return tool.amount() > 0 ? tool.amount() : fallback;
    }

    /** The most specialised role a kit allows: medic, engineer, anti-tank, support, then rifleman. */
    static BotSettings.ClassRole role(boolean heals, boolean repairs, boolean antiTank, boolean support)
    {
        if (heals)
            return BotSettings.ClassRole.MEDIC;
        if (repairs)
            return BotSettings.ClassRole.ENGINEER;
        if (antiTank)
            return BotSettings.ClassRole.ANTI_TANK;
        return support ? BotSettings.ClassRole.SUPPORT : BotSettings.ClassRole.RIFLEMAN;
    }

    private static boolean antiTank(ItemStack gun, List<ItemStack> items)
    {
        ItemStack ammunition = ammunitionFor(gun, items);
        double penetration = FlansEquipment.getArmorPenetration(gun, ammunition).orElse(0D);
        double blast = FlansEquipment.getWeaponProperties(gun, ammunition).map(properties -> properties.impact().explosionRadius()).orElse(0D);
        return penetration >= ANTI_TANK_PENETRATION || blast >= ANTI_TANK_BLAST;
    }

    private static boolean support(ItemStack gun, List<ItemStack> items)
    {
        if (FlansEquipment.isDeployable(gun))
            return true;
        ItemStack ammunition = ammunitionFor(gun, items);
        boolean automatic = FlansEquipment.getWeaponProperties(gun, ammunition).map(EquippedWeaponProperties::automaticFire).orElse(false);
        int magazine = FlansEquipment.getWeaponTraits(gun, ammunition).map(WeaponTraits::magazineCapacity).orElse(0);
        return automatic && magazine >= SUPPORT_MAGAZINE;
    }

    /** The first starting item a gun takes as ammunition, to judge the gun by; empty when it carries none. */
    private static ItemStack ammunitionFor(ItemStack gun, List<ItemStack> items)
    {
        return items.stream().filter(item -> item != gun && FlansEquipment.acceptsAmmunition(gun, item)).findFirst().orElse(ItemStack.EMPTY);
    }
}
