package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** What others hear and see when an NPC reloads: content reload sounds and the held gun's reload animation. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcReloadFeedback
{
    /** The NPC's model weapon bank has fired its magazine and starts reloading. */
    public static void modelReload(EntityNPCInterface npc)
    {
        IContentType model = NpcTypeProperties.of(npc).model();
        if (model != null && NpcEquipment.enabled(npc, Feature.RELOAD_SOUNDS))
            FlansProjectiles.playReloadSound(npc, model, NpcEquipment.enabled(npc, Feature.SECONDARY_BANK));
    }

    /** The model's main gun works its next round into the breech. */
    public static void chamber(EntityNPCInterface npc)
    {
        IContentType model = NpcTypeProperties.of(npc).model();
        if (model != null && NpcEquipment.enabled(npc, Feature.RELOAD_SOUNDS))
            FlansProjectiles.playChamberSound(npc, model);
    }

    /**
     * A Custom NPCs burst has ended while the NPC holds Flan guns it does not fire natively: the pause until the next
     * burst reads as a reload of every held gun.
     */
    public static void heldGuns(EntityNPCInterface npc, int pauseTicks)
    {
        for (InteractionHand hand : InteractionHand.values())
            if (isGun(npc.getItemInHand(hand)))
                reload(npc, hand, pauseTicks);
    }

    /** One held gun starts a reload lasting the given number of ticks. */
    public static void reload(EntityNPCInterface npc, InteractionHand hand, int reloadTicks)
    {
        if (NpcEquipment.enabled(npc, Feature.RELOAD_SOUNDS))
            FlansEquipment.playReloadSound(npc, npc.getItemInHand(hand));
        if (NpcEquipment.enabled(npc, Feature.GUN_RELOAD_ANIMATION))
            FlansEquipment.animateReload(npc, hand, Math.max(1, reloadTicks));
    }

    private static boolean isGun(ItemStack stack)
    {
        return !stack.isEmpty() && FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).filter(EquippedWeaponProperties::ranged).filter(properties -> !properties.throwable()).isPresent();
    }
}
