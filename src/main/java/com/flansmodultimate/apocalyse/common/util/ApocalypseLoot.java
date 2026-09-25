package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.platform.registry.RegistryEntry;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseLoot
{
    private static final String[] JOURNAL_LINES = new String[] {
        "The sky turned yellow today. The portal brought us somewhere worse than the wasteland.",
        "If you find the power cubes, do not stand between them unless you are ready to leave.",
        "The survivors stopped trusting anyone with clean armor. They still trade bullets for food.",
        "Sulphur pits mark the old roads. Keep water away from the acid and keep moving.",
        "The skull drones patrol at night. Their boss laughs before the nukes fall."
    };

    /** The 1.12.2 research lab notes, as {title, page}. */
    private static final String[][] SCIENTIST_JOURNAL = new String[][] {
        {"Research Journal: Entry 1", "We are trying to find ways to disable the AI mechas. Unfortunately, this involves bringing specimens into our lab for testing. I protested to management, but they wouldn't listen, as ever. This will be the death of us, I know it."},
        {"Research Journal: Entry 2", "The Mechas are almost... evolving... We try something new (today it was EMPs), boot them back up for another test and they've become resistant. Just like that. And I fear that the mechas we have here may be contacting others on the outside."},
        {"Research Journal: Entry 3", "I lose hope with every passing day. There is no clever way to destroy these Mechas or shut them down. Their programming forms a vast, global, interconnected web. You shut down one and already every other Mecha knows what you did and how to become immune to it"},
        {"Research Journal: Entry 4", "Finally, we are looking into other approaches, though I must say, I am quite surprised. Management must have gone a bit mad, they've got us looking for a way to travel back in time... back in time! To destroy the first AI Mecha! How absurd!"},
        {"Research Journal: Entry 5", "The time travel research is slow, but having heard some of the ideas from the others, I think we may actually have a shot. Not that this helps, though. I've been trying to explain stable time loops to management, but they either don't understand, or are just too desperate."},
        {"Research Journal: Entry 6", "We actually did it! I cannot believe it, but we sent someone back in time! Admittedly, they ended up walking with Creepersauruses, but nonetheless, we did it!"},
        {"Research Journal: Entry 7", "They're here! The mechas are here! If you read this, please, go back in time, destroy the creator, stop th..."},
        {"Time Portal: Instruction Manual", "The Time Portal uses the portal properties of obsidian combined with our state-of-the-art power cubes. Place one in each corner of the obsidian grid to activate the portal."}
    };

    /** The 1.12.2 brewing-stand pool (legacy potion metadata 8193-8206). */
    private static final List<Holder<Potion>> BREWING_STAND_POTIONS = List.of(Potions.REGENERATION, Potions.SWIFTNESS,
        Potions.FIRE_RESISTANCE, Potions.HEALING, Potions.NIGHT_VISION, Potions.STRENGTH, Potions.LEAPING,
        Potions.WATER_BREATHING, Potions.INVISIBILITY);

    public static ItemStack randomLoot(RandomSource random, boolean gunsOnly)
    {
        if (gunsOnly || random.nextInt(3) != 0)
        {
            Optional<ItemStack> gun = ApocalypseGunHelper.randomLoadedGun(random, false);
            if (gun.isPresent())
                return gun.get();
        }

        if (random.nextInt(5) == 0)
            return survivorJournal(random);
        if (random.nextInt(4) == 0)
            return new ItemStack(ApocalypseContent.SULPHUR.get(), 1 + random.nextInt(4));
        if (random.nextBoolean())
        {
            Optional<ItemStack> ammo = ApocalypseGunHelper.randomAmmoStack(random);
            if (ammo.isPresent())
                return ammo.get();
        }
        return randomPart(random).orElseGet(() -> randomFood(random));
    }

    public static void fillContainer(RandomSource random, Container container)
    {
        int stacks = 3 + random.nextInt(6);
        for (int i = 0; i < stacks; i++)
        {
            ItemStack stack = randomLoot(random, false);
            if (stack.isEmpty())
                continue;
            int slot = random.nextInt(container.getContainerSize());
            if (container.getItem(slot).isEmpty())
                container.setItem(slot, stack);
        }
    }

    /** 1.12.2 {@code fillVillageChest}: parts, ammo, fuel, raw food, and sometimes a mecha item or tool. */
    public static void fillVillageChest(RandomSource random, Container container)
    {
        int numParts = random.nextInt(6) + 1;
        int numAmmo = random.nextInt(6) + 1;
        int numFuel = random.nextInt(3);
        int numFood = random.nextInt(3);

        List<PartType> parts = sortedTypes(PartType.class);
        for (int i = 0; i < numParts && !parts.isEmpty(); i++)
            putRandomSlot(random, container, ModUtils.getItemStack(parts.get(random.nextInt(parts.size()))).orElse(ItemStack.EMPTY));

        List<ShootableType> shootables = sortedTypes(ShootableType.class);
        for (int i = 0; i < numAmmo && !shootables.isEmpty(); i++)
        {
            ShootableType ammo = shootables.get(random.nextInt(shootables.size()));
            if (ammo.getDungeonChance() != 0)
                putRandomSlot(random, container, ModUtils.getItemStack(ammo, 1 + (ammo.getMaxStackSize() > 1 && random.nextBoolean() ? 1 : 0)).orElse(ItemStack.EMPTY));
        }

        List<PartType> fuels = parts.stream().filter(part -> part.getCategory() == PartType.Category.FUEL).toList();
        for (int i = 0; i < numFuel && !fuels.isEmpty(); i++)
        {
            ItemStack fuel = ModUtils.getItemStack(fuels.get(random.nextInt(fuels.size()))).orElse(ItemStack.EMPTY);
            if (!fuel.isEmpty())
                fuel.setCount(random.nextInt(Math.max(1, Math.min(fuel.getMaxStackSize() - 1, 2))) + 1);
            putRandomSlot(random, container, fuel);
        }

        for (int i = 0; i < numFood; i++)
        {
            putRandomSlot(random, container, switch (random.nextInt(4))
            {
                case 0 -> new ItemStack(Items.CHICKEN, random.nextInt(2) + 1);
                case 1 -> new ItemStack(Items.PORKCHOP, random.nextInt(2) + 1);
                case 2 -> new ItemStack(Items.BEEF, random.nextInt(2) + 1);
                default -> new ItemStack(Items.BAKED_POTATO, random.nextInt(3) + 1);
            });
        }

        List<MechaItemType> mechaItems = sortedTypes(MechaItemType.class);
        if (random.nextBoolean() && random.nextBoolean() && !mechaItems.isEmpty())
            putRandomSlot(random, container, ModUtils.getItemStack(mechaItems.get(random.nextInt(mechaItems.size()))).orElse(ItemStack.EMPTY));

        List<ToolType> tools = sortedTypes(ToolType.class);
        if (random.nextBoolean() && !tools.isEmpty())
            putRandomSlot(random, container, ModUtils.getItemStack(tools.get(random.nextInt(tools.size()))).orElse(ItemStack.EMPTY));
    }

    /** 1.12.2 {@code fillWeaponChest}: three to five rounds for random guns and one attachment. */
    public static void fillWeaponChest(RandomSource random, Container container)
    {
        int ammoCount = 3 + random.nextInt(3);
        for (int i = 0; i < ammoCount; i++)
        {
            ApocalypseGunHelper.randomGun(random, false)
                .flatMap(gun -> ApocalypseGunHelper.spareAmmoFor(gun, random))
                .ifPresent(stack -> {
                    stack.setCount(1);
                    putRandomSlot(random, container, stack);
                });
        }
        List<AttachmentType> attachments = sortedTypes(AttachmentType.class);
        if (!attachments.isEmpty())
            putRandomSlot(random, container, ModUtils.getItemStack(attachments.get(random.nextInt(attachments.size()))).orElse(ItemStack.EMPTY));
    }

    /** 1.12.2 {@code fillLiquidLabChest}: bowls, buckets, strength potions, sulphur and research notes. */
    public static void fillLiquidLabChest(RandomSource random, Container container)
    {
        int numItems = 3 + random.nextInt(4);
        for (int i = 0; i < numItems; i++)
        {
            ItemStack stack = switch (random.nextInt(10))
            {
                case 0 -> new ItemStack(Items.BOWL, random.nextInt(5) + 1);
                case 1 -> new ItemStack(Items.WATER_BUCKET);
                case 2 -> randomFluidBucket(random);
                case 3, 4, 5, 6 -> ItemStackData.potion(Items.POTION, Potions.STRENGTH);
                case 7 -> new ItemStack(ApocalypseContent.SULPHUR.get(), random.nextInt(12) + 1);
                default -> scientistJournal(random);
            };
            putRandomSlot(random, container, stack);
        }
    }

    /** 1.12.2 {@code fillBrewingStand}: each bottle slot has an even chance of a random beneficial potion. */
    public static void fillBrewingStand(RandomSource random, Container brewingStand)
    {
        for (int slot = 0; slot < 3 && slot < brewingStand.getContainerSize(); slot++)
        {
            if (random.nextBoolean())
                brewingStand.setItem(slot, ItemStackData.potion(Items.POTION, BREWING_STAND_POTIONS.get(random.nextInt(BREWING_STAND_POTIONS.size()))));
        }
    }

    /** 1.12.2 {@code fillDyeFactoryChest}: dyes plus the odd textile supply. */
    public static void fillDyeFactoryChest(RandomSource random, Container container)
    {
        int numDyes = random.nextInt(4);
        int numMisc = random.nextInt(2);
        for (int i = 0; i < numDyes; i++)
            putRandomSlot(random, container, new ItemStack(DyeItem.byColor(DyeColor.byId(random.nextInt(16))), random.nextInt(8) + 1));
        for (int i = 0; i < numMisc; i++)
        {
            putRandomSlot(random, container, switch (random.nextInt(4))
            {
                case 0 -> new ItemStack(Items.STRING, random.nextInt(5) + 1);
                case 1 -> new ItemStack(Items.FEATHER, random.nextInt(5) + 1);
                case 2 -> new ItemStack(Items.LEATHER, random.nextInt(8) + 1);
                default -> new ItemStack(Items.CLAY_BALL, random.nextInt(32) + 1);
            });
        }
    }

    /** 1.12.2 {@code addRandomLoot}: guns two thirds of the time, otherwise a journal or rotten flesh. */
    public static ItemStack itemHolderLoot(RandomSource random, boolean gunsOnly)
    {
        if (gunsOnly || random.nextInt(3) != 0)
            return ApocalypseGunHelper.randomGun(random, false)
                .flatMap(gun -> ApocalypseGunHelper.loadGun(gun, random, true))
                .orElse(ItemStack.EMPTY);
        if (random.nextBoolean())
            return survivorJournal(random);
        if (random.nextBoolean())
            return new ItemStack(Items.ROTTEN_FLESH, 1 + random.nextInt(3));
        return ItemStack.EMPTY;
    }

    /** 1.12.2 {@code getRandomWeaponBox}: an armour box a quarter of the time, otherwise a gun box. */
    public static Optional<Block> randomWeaponBox(RandomSource random)
    {
        EnumType boxType = random.nextInt(4) == 0 ? EnumType.ARMOR_BOX : EnumType.GUN_BOX;
        Map<String, RegistryEntry<Block>> boxes = FlansMod.getBlocks().get(boxType);
        if (boxes == null || boxes.isEmpty())
            return Optional.empty();
        List<String> names = boxes.keySet().stream().sorted().toList();
        return Optional.of(boxes.get(names.get(random.nextInt(names.size()))).get());
    }

    public static ItemStack scientistJournal(RandomSource random)
    {
        String[] entry = SCIENTIST_JOURNAL[random.nextInt(SCIENTIST_JOURNAL.length)];
        return ItemStackData.writtenBook(entry[0], "Dr. Brazier", List.of(Component.literal(entry[1])));
    }

    private static ItemStack randomFluidBucket(RandomSource random)
    {
        List<Item> buckets = BuiltInRegistries.FLUID.stream()
            .filter(fluid -> fluid.isSource(fluid.defaultFluidState()))
            .map(Fluid::getBucket)
            .filter(bucket -> bucket != Items.AIR)
            .distinct()
            .sorted(Comparator.comparing(bucket -> String.valueOf(BuiltInRegistries.ITEM.getKey(bucket))))
            .toList();
        return buckets.isEmpty() ? ItemStack.EMPTY : new ItemStack(buckets.get(random.nextInt(buckets.size())));
    }

    private static void putRandomSlot(RandomSource random, Container container, ItemStack stack)
    {
        if (!stack.isEmpty() && container.getContainerSize() > 0)
            container.setItem(random.nextInt(container.getContainerSize()), stack);
    }

    /**
     * Info types live in a hash map, so candidates are sorted before a worldgen RNG picks one;
     * that keeps a given seed and pack set producing the same loot.
     */
    private static <T extends InfoType> List<T> sortedTypes(Class<T> kind)
    {
        return InfoType.getInfoTypes().values().stream()
            .filter(kind::isInstance)
            .map(kind::cast)
            .distinct()
            .filter(type -> ModUtils.getItemStack(type).isPresent())
            .sorted(Comparator.comparing(InfoType::getShortName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public static void dressMob(LivingEntity entity, RandomSource random)
    {
        if (random.nextBoolean())
            equipRandomArmorPiece(entity, random);
        else
            equipRandomArmorSet(entity, random);
    }

    public static void dropSurvivorLoot(Mob mob)
    {
        Level level = mob.level();
        RandomSource random = mob.getRandom();
        for (int i = 0; i < random.nextInt(5); i++)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), randomFood(random));

        if (random.nextInt(5) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.OAK_LOG, 5 + random.nextInt(10)));
        if (random.nextInt(12) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.FLINT_AND_STEEL));
        if (random.nextInt(40) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.IRON_AXE));
        if (random.nextInt(40) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.IRON_PICKAXE));
        if (random.nextInt(4) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.TORCH, 1 + random.nextInt(5)));
        if (random.nextBoolean())
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), survivorJournal(random));
        ApocalypseGunHelper.randomAmmoStack(random).ifPresent(stack -> Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), stack));
    }

    public static ItemStack survivorJournal(RandomSource random)
    {
        String text = JOURNAL_LINES[random.nextInt(JOURNAL_LINES.length)];
        return ItemStackData.writtenBook("Survivor Journal", "Unknown Survivor", List.of(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY)));
    }

    public static Optional<ItemStack> randomPart(RandomSource random)
    {
        List<PartType> parts = new ArrayList<>();
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            if (type instanceof PartType part && !part.isAiChip() && ModUtils.getItemStack(part).isPresent())
                parts.add(part);
        }
        if (parts.isEmpty())
            return Optional.empty();
        return ModUtils.getItemStack(parts.get(random.nextInt(parts.size())), 1 + random.nextInt(2));
    }

    private static void equipRandomArmorPiece(LivingEntity entity, RandomSource random)
    {
        List<ArmorType> armor = armorTypes();
        if (armor.isEmpty())
            return;
        equipArmor(entity, armor.get(random.nextInt(armor.size())));
    }

    private static void equipRandomArmorSet(LivingEntity entity, RandomSource random)
    {
        List<ArmorType> armor = armorTypes();
        if (armor.isEmpty())
            return;
        for (ArmorType type : armor)
            if (random.nextInt(3) != 0)
                equipArmor(entity, type);
    }

    private static void equipArmor(LivingEntity entity, ArmorType armor)
    {
        ModUtils.getItemStack(armor).ifPresent(stack -> {
            EquipmentSlot slot = armor.getArmorItemType().getSlot();
            if (entity.getItemBySlot(slot).isEmpty())
                entity.setItemSlot(slot, stack);
        });
    }

    private static List<ArmorType> armorTypes()
    {
        List<ArmorType> armor = new ArrayList<>();
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            if (type instanceof ArmorType armorType && ModUtils.getItemStack(armorType).isPresent())
                armor.add(armorType);
        }
        return armor;
    }

    private static ItemStack randomFood(RandomSource random)
    {
        return switch (random.nextInt(6))
        {
            case 0 -> new ItemStack(Items.COOKED_BEEF);
            case 1 -> new ItemStack(Items.BREAD);
            case 2 -> new ItemStack(Items.MUSHROOM_STEW);
            case 3 -> new ItemStack(Items.COOKED_RABBIT);
            case 4 -> new ItemStack(Items.COOKED_CHICKEN);
            default -> new ItemStack(Items.BAKED_POTATO, 1 + random.nextInt(3));
        };
    }
}
