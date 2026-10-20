package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

import java.util.*;
import java.util.function.Predicate;

/**
 * Equips an NPC with the loadout of a Flan's player class. Only ever run on a builder's explicit request: selecting a
 * class in the model picker changes the NPC's looks and nothing else. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanSoldierLoadout
{
    private static final List<EquipmentSlot> ARMOUR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /** Custom NPCs numbers armour slots from the head down, and vanilla from the feet up. */
    private static final int HEAD_ARMOUR_INDEX = 3;
    private static final int MAIN_HAND_INDEX = 0;
    private static final int PROJECTILE_INDEX = 1;
    private static final int OFF_HAND_INDEX = 2;

    /** What a starting item is to an NPC. */
    public enum Kind
    {
        /** A Flan gun the NPC fires from a hand. A deployable gun is not one: it is only carried. */
        GUN,
        /** A melee weapon or a supported non-Flan ranged weapon. */
        WEAPON,
        /** A shield, raised from the off hand. */
        SHIELD,
        /** A Flan grenade, reserved for the projectile slot. */
        THROWABLE,
        /** A throwable Flan gun, eligible for either the projectile slot or the main hand. */
        THROWN_WEAPON,
        /** Flan ammunition a gun is reloaded with, or arrows and rockets a bow or crossbow shoots. */
        AMMUNITION, OTHER
    }

    /** What the slot assignment needs to know about the starting items. */
    public interface Items<T>
    {
        Kind kind(T item);

        /** @return whether two guns are the same weapon, which an NPC may hold one of in each hand */
        boolean sameGun(T first, T second);

        /** @return whether the gun is reloaded with the ammunition */
        boolean loads(T gun, T ammunition);
    }

    /**
     * The starting items an NPC carries; null leaves a slot empty.
     *
     * @param inventory
     *            the items that are neither held nor in the projectile slot, in the order the class lists them
     */
    public record Slots<T>(@Nullable T mainHand, @Nullable T offHand, @Nullable T projectile, List<T> inventory)
    {}

    /** A loadout as Custom NPCs stores it: armour, weapons and inventory by its own slot numbers, without the empty slots. */
    public record Equipment(Map<Integer, IItemStack> armour, Map<Integer, IItemStack> weapons, Map<Integer, IItemStack> inventory)
    {}

    /**
     * Distributes the starting items of a class, in AddItem order. The first throwable is reserved for the projectile slot;
     * the first remaining handheld gun, melee weapon, throwable gun or supported non-Flan ranged weapon goes in the main hand. The
     * off hand takes a shield, or a second gun only when it is the same weapon as the first, for dual wielding. A gun, bow
     * or crossbow reloads from the projectile slot, which takes the first ammunition the main-hand weapon loads; without
     * any, or without such a weapon, it takes the first throwable. Everything else goes to the inventory, up to its size.
     */
    public static <T> Slots<T> assign(List<T> items, Items<T> rules)
    {
        int throwable = first(items, -1, item -> rules.kind(item) == Kind.THROWABLE || rules.kind(item) == Kind.THROWN_WEAPON);
        int mainHand = first(items, throwable, item -> rules.kind(item) == Kind.GUN || rules.kind(item) == Kind.WEAPON || rules.kind(item) == Kind.THROWN_WEAPON);
        T weapon = at(items, mainHand);
        int offHand = first(items, mainHand, item -> offHand(rules, weapon, item));
        int ammunition = weapon != null && (rules.kind(weapon) == Kind.GUN || rules.kind(weapon) == Kind.WEAPON)
            ? first(items, -1, item -> rules.kind(item) == Kind.AMMUNITION && rules.loads(weapon, item))
            : -1;
        int projectile = ammunition < 0 ? throwable : ammunition;
        List<T> inventory = new ArrayList<>();
        for (int i = 0; i < items.size() && inventory.size() < NpcAmmunition.INVENTORY_SLOTS; i++)
            if (i != mainHand && i != offHand && i != projectile)
                inventory.add(items.get(i));
        return new Slots<>(weapon, at(items, offHand), at(items, projectile), List.copyOf(inventory));
    }

    private static <T> boolean offHand(Items<T> rules, @Nullable T gun, T item)
    {
        Kind kind = rules.kind(item);
        return kind == Kind.SHIELD || gun != null && rules.kind(gun) == Kind.GUN && kind == Kind.GUN && rules.sameGun(gun, item);
    }

    /** @return the index of the first wanted item, never the skipped index, or -1 */
    private static <T> int first(List<T> items, int skipped, Predicate<T> wanted)
    {
        for (int i = 0; i < items.size(); i++)
            if (i != skipped && wanted.test(items.get(i)))
                return i;
        return -1;
    }

    @Nullable
    private static <T> T at(List<T> items, int index)
    {
        return index < 0 ? null : items.get(index);
    }

    /** The rules for real items: guns are the same weapon when they are the same item, whatever their attachments. */
    private static final class StackItems implements Items<ItemStack>
    {
        @Override
        public Kind kind(ItemStack stack)
        {
            return FlanSoldierLoadout.kind(stack);
        }

        @Override
        public boolean sameGun(ItemStack first, ItemStack second)
        {
            return ItemStack.isSameItem(first, second);
        }

        @Override
        public boolean loads(ItemStack gun, ItemStack ammunition)
        {
            if (gun.getItem() instanceof ProjectileWeaponItem weapon)
                return weapon.getAllSupportedProjectiles().test(ammunition);
            return FlansEquipment.acceptsAmmunition(gun, ammunition);
        }
    }

    public static Kind kind(ItemStack stack)
    {
        if (FlansProjectiles.isGrenade(stack))
            return Kind.THROWABLE;
        if (FlansProjectiles.isProjectile(stack) || stack.getItem() instanceof ArrowItem || stack.getItem() instanceof FireworkRocketItem)
            return Kind.AMMUNITION;
        Optional<EquippedWeaponProperties> weapon = FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY);
        if (NpcEquipment.shield(stack) || weapon.isPresent() && weapon.get().shield())
            return Kind.SHIELD;
        if (weapon.isEmpty())
            return NpcEquipment.ranged(stack) || NpcEquipment.meleeDamage(stack) > 1D ? Kind.WEAPON : Kind.OTHER;
        if (weapon.get().throwable())
            return Kind.THROWN_WEAPON;
        // A deployable gun is fired from where it is set up, so the next weapon of the class is the one to hold.
        if (FlansEquipment.isDeployable(stack))
            return Kind.OTHER;
        if (weapon.get().ranged())
            return Kind.GUN;
        return weapon.get().meleeDamage() > 1D ? Kind.WEAPON : Kind.OTHER;
    }

    /**
     * Carries out a builder's request. The NPC must be the one the player is editing, which Custom NPCs only records for
     * a player allowed to open its editor, and the player needs the wand and the permissions Custom NPCs asks for to
     * change an NPC's inventory and statistics.
     *
     * @param entityId
     *            the NPC the request was made for
     */
    public static void request(ServerPlayer player, int entityId, String team, String playerClass)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        boolean allowed = npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_INVENTORY) && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_STATS);
        if (!allowed)
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.soldier.loadout.denied"));
            return;
        }
        Optional<FlanSoldierSelection> selection = FlanSoldierSelection.resolve(team, playerClass);
        if (selection.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.soldier.loadout.unknown"));
            return;
        }
        apply(npc, selection.get());
    }

    /** Replaces the NPC's armour, weapons and inventory with the class loadout and asks for its statistics to follow them. */
    public static void apply(EntityNPCInterface npc, FlanSoldierSelection selection)
    {
        Equipment equipment = equipment(selection);
        npc.inventory.armor.clear();
        npc.inventory.armor.putAll(equipment.armour());
        npc.inventory.weapons.clear();
        npc.inventory.weapons.putAll(equipment.weapons());
        npc.inventory.drops.clear();
        npc.inventory.dropchance.clear();
        npc.inventory.drops.putAll(equipment.inventory());
        // The NPC's next tick imports them, where the new equipment is in place and inherited values are lifted first.
        NpcCombatSettings combat = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat();
        for (EquipmentProperty property : EquipmentProperty.values())
            combat.requestCopy(property);
        npc.updateClient = true;
        npc.updateAI = true;
    }

    /**
     * Builds the armour, weapons and inventory a class equips an NPC with. Either side: the server applies them, and the model
     * picker dresses its preview in them.
     */
    public static Equipment equipment(FlanSoldierSelection selection)
    {
        Map<Integer, IItemStack> armour = new HashMap<>();
        // As for a player joining a round: the class armour, and the team's where the class names none.
        for (EquipmentSlot slot : ARMOUR_SLOTS)
        {
            ItemStack stack = selection.playerClass().getArmour(slot);
            if (stack.isEmpty())
                stack = selection.team().getArmour(slot);
            if (!stack.isEmpty())
                armour.put(HEAD_ARMOUR_INDEX - slot.getIndex(), NpcAPI.Instance().getIItemStack(stack));
        }
        Slots<ItemStack> slots = assign(selection.playerClass().createStartingItems(), new StackItems());
        Map<Integer, IItemStack> weapons = new HashMap<>();
        if (slots.mainHand() != null)
            weapons.put(MAIN_HAND_INDEX, NpcAPI.Instance().getIItemStack(slots.mainHand()));
        if (slots.projectile() != null)
            weapons.put(PROJECTILE_INDEX, NpcAPI.Instance().getIItemStack(slots.projectile()));
        if (slots.offHand() != null)
            weapons.put(OFF_HAND_INDEX, NpcAPI.Instance().getIItemStack(slots.offHand()));
        Map<Integer, IItemStack> inventory = new HashMap<>();
        for (ItemStack stack : slots.inventory())
            inventory.put(inventory.size(), NpcAPI.Instance().getIItemStack(stack));
        return new Equipment(armour, weapons, inventory);
    }
}
