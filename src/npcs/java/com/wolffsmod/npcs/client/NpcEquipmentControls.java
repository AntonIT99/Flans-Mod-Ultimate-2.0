package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.combat.EquipmentAttributes;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponAdapter;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.client.gui.SubGuiNpcMeleeProperties;
import noppes.npcs.client.gui.SubGuiNpcProjectiles;
import noppes.npcs.client.gui.SubGuiNpcRangeProperties;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

import java.util.HashMap;
import java.util.Map;

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
        if (NpcEquipment.weaponAuthority(npc))
            weapon(gui, npc, readouts, buttons);
        if (gui instanceof SubGuiNpcProjectiles)
            buttons.forEach((id, reason) ->
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
        if (gui instanceof GuiNpcStats && NpcEquipment.enabled(npc, Feature.ITEM_ARMOR))
            armor(gui, npc, buttons, labels);
        RangedControlLocks locks = new RangedControlLocks(fields, buttons, labels);
        NpcRangedControls.markControls(gui, locks);
        return locks;
    }

    private static void weapon(GuiBasic gui, EntityNPCInterface npc, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        ItemStack item = npc.getMainHandItem();
        String nativeValue = I18n.get("wolffsmodnpcs.weapons.item_value");
        if (gui instanceof SubGuiNpcMeleeProperties)
            melee(gui, npc, item, nativeValue, readouts, buttons);
        else if (gui instanceof SubGuiNpcRangeProperties)
            ranged(gui, npc, item, nativeValue, readouts, buttons);
        else if (gui instanceof SubGuiNpcProjectiles)
            projectiles(npc, nativeValue, readouts, buttons);
    }

    private static void melee(GuiBasic gui, EntityNPCInterface npc, ItemStack item, String nativeValue, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        readouts.put(1, hasEffects(item, npc, EnchantmentEffectComponents.DAMAGE) ? nativeValue : number(NpcEquipment.meleeDamage(item)));
        readouts.put(2, number(EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, Attributes.ENTITY_INTERACTION_RANGE, 3D)));
        readouts.put(3, Integer.toString(NpcEquipment.meleeDelay(item)));
        readouts.put(4, hasEffects(item, npc, EnchantmentEffectComponents.KNOCKBACK) ? nativeValue : number(EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, Attributes.ATTACK_KNOCKBACK, 0D)));
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
        NpcEquipment.flan(npc).ifPresent(properties ->
        {
            readouts.put(1, number(Math.max(0, 100D - properties.spread() * 5D)));
            readouts.put(8, Integer.toString(properties.projectiles()));
            readouts.put(7, properties.firingSound().isBlank() ? nativeValue : properties.firingSound());
        });
        if (item.getItem() instanceof NpcWeaponAdapter adapter)
            readouts.putAll(adapter.npcRangedReadouts(item));
    }

    private static void projectiles(EntityNPCInterface npc, String nativeValue, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        for (int id : new int[]{1, 2, 3, 4, 5})
            readouts.put(id, nativeValue);
        for (int id : new int[]{0, 1, 3, 4, 5, 6, 7, 8, 9, 10})
            buttons.put(id, Reason.ITEM_WEAPON);
        NpcEquipment.flan(npc).ifPresent(properties ->
        {
            readouts.put(1, number(properties.damage()));
            readouts.put(4, number(properties.speed() * 10D));
        });
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

    /** Conditional 1.21 effects require the real server target; the editor displays item authority instead. */
    private static boolean hasEffects(ItemStack item, EntityNPCInterface npc, DataComponentType<?> component)
    {
        return item.getAllEnchantments(npc.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)).keySet().stream().anyMatch(enchantment -> enchantment.value().effects().has(component));
    }

    private static String number(double value)
    {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
