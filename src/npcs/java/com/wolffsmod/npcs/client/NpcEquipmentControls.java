package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import net.minecraftforge.common.ForgeMod;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;

/** Item readouts are presentation only: no writes to NPC combat fields, even while the editor saves. */
public final class NpcEquipmentControls
{
    /** Vanilla inaccuracy 1 in Flan spread units, using the conversion Flan thrown weapons apply. */
    private static final double VANILLA_INACCURACY_SPREAD = 0.0172275D / 0.0025D;
    /** Arrow, trident and thrown-item hitbox width in blocks. */
    private static final double VANILLA_HITBOX = 0.5D;

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
            melee(gui, item, readouts, buttons);
        else if (gui instanceof SubGuiNpcRangeProperties)
            ranged(gui, npc, item, nativeValue, readouts, buttons);
        else if (gui instanceof SubGuiNpcProjectiles)
            projectiles(gui, npc, nativeValue, readouts, buttons, shown);
    }

    private static void melee(GuiBasic gui, ItemStack item, Map<Integer, String> readouts, Map<Integer, Reason> buttons)
    {
        readouts.put(1, number(NpcEquipment.meleeDamage(item) + EnchantmentHelper.getDamageBonus(item, net.minecraft.world.entity.MobType.UNDEFINED)));
        readouts.put(2, number(EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, ForgeMod.ENTITY_REACH.get(), 3D)));
        readouts.put(3, Integer.toString(NpcEquipment.meleeDelay(item)));
        readouts.put(4, number(EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK, 0D)
            + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, item)));
        readouts.put(6, Integer.toString(NpcEquipment.fireSeconds(item)));
        buttons.put(5, Reason.ITEM_WEAPON);
        buttons.put(7, Reason.ITEM_WEAPON);
        var effect = gui.getButton(5);
        if (effect != null)
            effect.setDisplay(NpcEquipment.fireSeconds(item) > 0 ? 33 : 0);
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
        readouts.put(3, number(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item)));
        readouts.put(4, number(NpcEquipment.shotDelay(item) + NpcEquipment.chargeTime(item)));
        readouts.put(5, number(NpcEquipment.shotDelay(item)));
        readouts.put(6, "1");
        Shot shot = shot(npc, item);
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
        Shot shot = shot(npc, npc.getMainHandItem());
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
        display(gui, 3, explosionSize(impact.explosionRadius()), buttons, shown);
    }

    /** Custom NPCs none/small/medium/large explosion choice nearest to a Flan radius in blocks. */
    private static int explosionSize(double radius)
    {
        if (radius <= 0D)
            return 0;
        if (radius < 2D)
            return 1;
        return radius < 4D ? 2 : 3;
    }

    private static void display(GuiBasic gui, int id, int value, Map<Integer, Reason> buttons, Set<Integer> shown)
    {
        var button = gui.getButton(id);
        if (button != null)
            button.setDisplay(value);
        buttons.put(id, Reason.ITEM_WEAPON);
        shown.add(id);
    }

    /** Ranged statistics of a Flan gun or a vanilla ranged item; {@code null} when the item's behavior cannot be inspected. */
    @Nullable
    private static Shot shot(EntityNPCInterface npc, ItemStack item)
    {
        var flan = NpcEquipment.flan(npc);
        if (flan.isPresent())
        {
            EquippedWeaponProperties properties = flan.get();
            return new Shot(properties.damage(), properties.speed(), properties.spread(), properties.projectiles(), properties.firingSound(), properties.impact());
        }
        String arrowHit = SoundEvents.ARROW_HIT.getLocation().toString();
        if (item.getItem() instanceof BowItem)
        {
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, item);
            return vanilla(2D + (power > 0 ? power * 0.5D + 0.5D : 0D), 3D, 1, SoundEvents.ARROW_SHOOT, arrowHit, arrowHit);
        }
        if (item.getItem() instanceof CrossbowItem)
            return vanilla(2D, NpcItemAttacks.CROSSBOW_VELOCITY, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, item) > 0 ? 3 : 1, SoundEvents.CROSSBOW_SHOOT, arrowHit, arrowHit);
        if (item.getItem() instanceof TridentItem)
            return vanilla(8D, 2.5D, 1, SoundEvents.TRIDENT_THROW, SoundEvents.TRIDENT_HIT.getLocation().toString(), SoundEvents.TRIDENT_HIT_GROUND.getLocation().toString());
        if (item.getItem() instanceof ThrowablePotionItem)
            return vanilla(0D, 0.5D, 1, SoundEvents.SNOWBALL_THROW, "", SoundEvents.SPLASH_POTION_BREAK.getLocation().toString());
        if (item.is(Items.SNOWBALL) || item.is(Items.EGG) || item.is(Items.ENDER_PEARL))
            return vanilla(0D, 1.5D, 1, SoundEvents.SNOWBALL_THROW, "", "");
        return null;
    }

    private static Shot vanilla(double damage, double speed, int projectiles, SoundEvent firing, String entityHit, String blockHit)
    {
        // Item attacks fire with vanilla inaccuracy 1; express it in Flan spread units like the gun readouts.
        double spread = VANILLA_INACCURACY_SPREAD;
        return new Shot(damage, speed, spread, projectiles, firing.getLocation().toString(), new EquippedWeaponProperties.Impact(1D, VANILLA_HITBOX, 1D, 0D, entityHit, blockHit, false));
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

    private record Shot(double damage, double speed, double spread, int projectiles, String firingSound, EquippedWeaponProperties.Impact impact)
    {}

    private static String number(double value)
    {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
