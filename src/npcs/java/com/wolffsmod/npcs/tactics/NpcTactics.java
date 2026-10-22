package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.item.ItemStack;

/** Which tactic moves an NPC in a fight, and the distances it fights at. Server side. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcTactics
{
    /** @return the tactic the NPC fights by: Custom NPCs' own, unless one of these is set and fits the NPC */
    public static Tactic of(EntityNPCInterface npc)
    {
        Tactic stored = NpcTuning.of(npc).tactic();
        // Most NPCs keep Custom NPCs' tactics, which needs neither their kind nor their weapon.
        if (stored == Tactic.CUSTOM_NPCS)
            return stored;
        return resolve(stored, TacticKind.of(npc), NpcEquipment.fightsAtRange(npc));
    }

    /**
     * @param atRange
     *            whether the NPC fights at range, which every tactic but Custom NPCs' own needs
     */
    static Tactic resolve(Tactic stored, TacticKind kind, boolean atRange)
    {
        return atRange && stored.appliesTo(kind) ? stored : Tactic.CUSTOM_NPCS;
    }

    /** @return whether a tactic of the mod's own moves the NPC along a path; a plane flies its own */
    public static boolean followsPath(Tactic tactic)
    {
        return tactic != Tactic.CUSTOM_NPCS && !tactic.flown();
    }

    /** @return the distances the NPC's tactic works with, from its firing range, its tactic distance and its weapon */
    public static Engagement engagement(EntityNPCInterface npc)
    {
        return Engagement.of(npc.stats.ranged.getRange(), NpcTuning.of(npc).get(Value.TACTIC_DISTANCE), role(npc));
    }

    /** @return the role of the ranged weapon the NPC holds; a projectile fired from the slot alone counts as a rifle */
    public static WeaponRole role(EntityNPCInterface npc)
    {
        if (!NpcEquipment.ranged(npc))
            return WeaponRole.GENERAL;
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        return NpcTacticState.of(npc).role(weapon.getItem(), () ->
        {
            IItemStack slot = npc.inventory.getProjectile();
            return WeaponRole.of(weapon, slot == null ? ItemStack.EMPTY : slot.getMCItemStack());
        });
    }

    /** @return blocks beyond its aggro range a tactic of the mod's own may carry the NPC, which keeps its target that far */
    public static int reach(EntityNPCInterface npc, Tactic tactic)
    {
        if (!tactic.usesDistance())
            return 0;
        Engagement engagement = engagement(npc);
        // A plane carries on past its target for its tactic distance before it turns.
        return (int) Math.ceil(tactic == Tactic.STRAFING_RUN ? engagement.preferred() + engagement.firing() : engagement.preferred());
    }
}
