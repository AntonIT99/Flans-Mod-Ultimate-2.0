package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Item readouts are presentation only: no writes to NPC combat fields, even while the editor saves. */
public final class NpcEquipmentControls
{
    private NpcEquipmentControls()
    {}

    public static RangedControlLocks apply(GuiBasic gui, EntityNPCInterface npc)
    {
        Map<Integer, String> readouts = new HashMap<>();
        Map<Integer, Reason> fields = new HashMap<>();
        Map<Integer, Reason> buttons = new HashMap<>();
        Map<Integer, Reason> labels = new HashMap<>();
        Set<Integer> shown = new HashSet<>();
        if (NpcEquipment.weaponAuthority(npc))
            weapon(gui, npc, readouts, buttons, shown);
        if (gui instanceof SubGuiNpcProjectiles)
            buttons.keySet().stream().filter(id -> !shown.contains(id)).forEach(id ->
            {
                var button = gui.getButton(id);
                if (button != null)
                    button.setMessage(Component.translatable("wolffsmodnpcs.weapons.item_short"));
            });
        readouts.forEach((id, text) ->
        {
            var field = gui.getTextField(id);
            if (field != null)
            {
                field.setMaxLength(256);
                field.numbersOnly = false;
                field.floatsOnly = false;
                field.setValue(text);
                fields.put(id, Reason.ITEM_WEAPON);
                labels.put(id, Reason.ITEM_WEAPON);
            }
        });
        labels.putAll(buttons);
        // The projectile page labels its stick button (9) with ID 11.
        if (gui instanceof SubGuiNpcProjectiles && buttons.containsKey(9))
            labels.put(11, buttons.get(9));
        if (gui instanceof GuiNpcStats && NpcEquipment.enabled(npc, Feature.ITEM_ARMOR))
            armor(gui, npc, buttons, labels);
        RangedControlLocks locks = new RangedControlLocks(fields, buttons, labels);
        NpcRangedControls.markControls(gui, locks);
        return locks;
    }

    private static void weapon(GuiBasic gui, EntityNPCInterface npc, Map<Integer, String> readouts, Map<Integer, Reason> buttons, Set<Integer> shown)
    {
        ItemStack item = npc.getMainHandItem();
        String nativeValue = I18n.get("wolffsmodnpcs.weapons.item_value");
        if (gui instanceof SubGuiNpcMeleeProperties)
            melee(gui, npc, item, nativeValue, readouts, buttons);
        else if (gui instanceof SubGuiNpcRangeProperties)
            ranged(gui, npc, NpcEquipment.rangedWeapon(npc), nativeValue, readouts, buttons);
        else if (gui instanceof SubGuiNpcProjectiles)
            projectiles(gui, npc, nativeValue, readouts, buttons, shown);
    }

    private static void melee(GuiBasic gui, EntityNPCInterface npc, ItemStack item, String nativeValue, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        // Minecraft 1.21 damage and knockback enchantments depend on the real target, so the item decides those values.
        readouts.put(1, NpcPlatform.conditionalEffect(npc, item, NpcPlatform.Effect.DAMAGE) ? nativeValue : number(NpcEquipmentStats.meleeDamage(npc, item)));
        readouts.put(2, number(NpcEquipmentStats.meleeReach(item)));
        readouts.put(3, Integer.toString(NpcEquipment.meleeDelay(item)));
        readouts.put(4, NpcPlatform.conditionalEffect(npc, item, NpcPlatform.Effect.KNOCKBACK) ? nativeValue : number(NpcEquipmentStats.meleeKnockback(npc, item)));
        readouts.put(6, Integer.toString(NpcEquipment.fireSeconds(item, npc)));
        buttons.put(5, Reason.ITEM_WEAPON);
        buttons.put(7, Reason.ITEM_WEAPON);
        var effect = gui.getButton(5);
        if (effect != null)
            effect.setDisplay(NpcEquipment.fireSeconds(item, npc) > 0 ? 33 : 0);
    }

    private static void ranged(GuiBasic gui, EntityNPCInterface npc, ItemStack item, String nativeValue, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        for (int id : new int[]{1, 3, 4, 5, 6, 7, 8, 10, 11})
            readouts.put(id, nativeValue);
        for (int id : new int[]{7, 10, 11})
            buttons.put(id, Reason.ITEM_WEAPON);
        buttons.put(9, Reason.ITEM_WEAPON);
        buttons.put(13, Reason.ITEM_WEAPON);
        var aim = gui.getButton(9);
        if (aim != null)
            aim.setDisplay(NpcEquipment.enabled(npc, Feature.WEAPON_ANIMATIONS) ? 1 : 0);
        var indirect = gui.getButton(13);
        if (indirect != null)
            indirect.setDisplay(0);
        readouts.put(3, number(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item, npc)));
        readouts.put(4, number(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item, npc)));
        readouts.put(5, number(NpcEquipment.shotDelay(item)));
        readouts.put(6, "1");
        NpcEquipmentStats.Shot shot = NpcEquipmentStats.shot(npc, item);
        if (shot != null)
        {
            readouts.put(1, number(Math.max(0, 100D - shot.spread() * 5D)));
            readouts.put(8, Integer.toString(shot.projectiles()));
            readouts.put(7, sound(shot.firingSound()));
            readouts.put(11, sound(shot.impact().entityHitSound()));
            readouts.put(10, shot.impact().materialImpactSounds() ? I18n.get("wolffsmodnpcs.weapons.material_sound") : sound(shot.impact().blockHitSound()));
        }
        if (item.getItem() instanceof NpcWeaponAdapter adapter)
            readouts.putAll(adapter.npcRangedReadouts(item));
    }

    private static void projectiles(GuiBasic gui, EntityNPCInterface npc, String nativeValue, Map<Integer, String> readouts, Map<Integer, Reason> buttons, Set<Integer> shown)
    {
        for (int id : new int[]{1, 2, 3, 4, 5})
            readouts.put(id, nativeValue);
        for (int id : new int[]{1, 4, 5, 6, 7, 8, 9, 10})
            buttons.put(id, Reason.ITEM_WEAPON);
        NpcEquipmentStats.Shot shot = NpcEquipmentStats.shot(npc, NpcEquipment.rangedWeapon(npc));
        if (shot == null)
        {
            buttons.put(0, Reason.ITEM_WEAPON);
            buttons.put(3, Reason.ITEM_WEAPON);
            return;
        }
        EquippedWeaponProperties.Impact impact = shot.impact();
        readouts.put(1, number(shot.damage()));
        readouts.put(2, number(impact.knockback()) + "x");
        readouts.put(3, number(impact.hitboxSize()) + "m");
        readouts.put(4, number(shot.speed() * 10D));
        // Item attacks never apply the Custom NPCs potion effect.
        readouts.put(5, "0");
        // Gravity and explosion buttons show the real projectile instead of the generic Item label.
        display(gui, 0, impact.gravity() > 0D ? 1 : 0, buttons, shown);
        // Flan rounds and vanilla projectiles only lose speed to drag.
        display(gui, 1, 0, buttons, shown);
        display(gui, 3, NpcEquipmentStats.explosionSize(impact.explosionRadius()), buttons, shown);
    }

    private static void display(GuiBasic gui, int id, int value, Map<Integer, Reason> buttons, Set<Integer> shown)
    {
        var button = gui.getButton(id);
        if (button != null)
            button.setDisplay(value);
        buttons.put(id, Reason.ITEM_WEAPON);
        shown.add(id);
    }

    private static String sound(String sound)
    {
        return sound.isBlank() ? I18n.get("wolffsmodnpcs.weapons.no_sound") : sound;
    }

    private static void armor(GuiBasic gui, EntityNPCInterface npc, Map<Integer, Reason> buttons, Map<Integer, Reason> labels)
    {
        for (ItemStack stack : npc.getArmorSlots())
            FlansEquipment.getArmorProperties(stack).ifPresent(properties ->
            {
                if (properties.fireResistance())
                    protection(gui, 4, 10, true, buttons, labels);
                if (properties.waterBreathing())
                    protection(gui, 5, 11, false, buttons, labels);
                if (properties.negatesFallDamage())
                    protection(gui, 7, 13, true, buttons, labels);
            });
    }

    private static void protection(GuiBasic gui, int id, int label, boolean value, Map<Integer, Reason> buttons, Map<Integer, Reason> labels)
    {
        var button = gui.getButton(id);
        if (button != null)
        {
            button.setDisplay(value ? 1 : 0);
            buttons.put(id, Reason.ITEM_ARMOR);
            labels.put(label, Reason.ITEM_ARMOR);
        }
    }

    private static String number(double value)
    {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
