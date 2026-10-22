package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.platform.registry.RegistryEntry;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.*;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseLoot
{
    /** The eight 1.12.2 survivor journals, as {title, pages...}. */
    private static final String[][] SURVIVOR_JOURNALS = new String[][]{{"Help me!", "I have no food. My child has no food. We are going to die. Why did this have to happen?"}, {"The Endtimes",
        "It's amazing how fast your world can be torn down around you. Just three days ago, I was happily trading emeralds at the village market. Now all that is gone. I am left to wander this wasteland alone. I don't know how long I'll last, or how long I'll stay sane..."},
        {"Day 5", "We found water today! At the bottom of a village well. We drank and bathed and filled our bottles and left. But for reference, the village was at- *bloodstains*"},
        {"Day 7",
            "They got my brother! Just after we left the village, he was snatched by some sort of... robot... Also, I think the water may have been contaminated. I've been sweating an awful lot, and it's not just the heat."},
        {"Day 10", "I have been violently ill, but have not found a new water source yet. I may have to drink more contaminated water to stay alive. I couldn't get worse, could I?"},
        {"The Wasteland",
            "This world is harsh and unforgiving. I've had to make difficult choices, but they are necessary in order to survive. If I hadn't pulled the trigger, they would have done so instead. I'm sure of it."},
        {"", "We spotted an airstrip in the distance! We're going to head over there under cover of darkness and see if we can acquire ourselves a plane. Let's get out of this terrible place."},
        {"Time Portal: Instruction Manual", "The Time Portal uses the portal properties of obsidian combined with-",
            "Beware! The mechas are coming! The time portal is of great importance! You must-"}};

    /** The 1.12.2 research lab notes, as {title, page}. */
    private static final String[][] SCIENTIST_JOURNAL = new String[][]{{"Research Journal: Entry 1",
        "We are trying to find ways to disable the AI mechas. Unfortunately, this involves bringing specimens into our lab for testing. I protested to management, but they wouldn't listen, as ever. This will be the death of us, I know it."},
        {"Research Journal: Entry 2",
            "The Mechas are almost... evolving... We try something new (today it was EMPs), boot them back up for another test and they've become resistant. Just like that. And I fear that the mechas we have here may be contacting others on the outside."},
        {"Research Journal: Entry 3",
            "I lose hope with every passing day. There is no clever way to destroy these Mechas or shut them down. Their programming forms a vast, global, interconnected web. You shut down one and already every other Mecha knows what you did and how to become immune to it"},
        {"Research Journal: Entry 4",
            "Finally, we are looking into other approaches, though I must say, I am quite surprised. Management must have gone a bit mad, they've got us looking for a way to travel back in time... back in time! To destroy the first AI Mecha! How absurd!"},
        {"Research Journal: Entry 5",
            "The time travel research is slow, but having heard some of the ideas from the others, I think we may actually have a shot. Not that this helps, though. I've been trying to explain stable time loops to management, but they either don't understand, or are just too desperate."},
        {"Research Journal: Entry 6", "We actually did it! I cannot believe it, but we sent someone back in time! Admittedly, they ended up walking with Creepersauruses, but nonetheless, we did it!"},
        {"Research Journal: Entry 7", "They're here! The mechas are here! If you read this, please, go back in time, destroy the creator, stop th..."}, {"Time Portal: Instruction Manual",
            "The Time Portal uses the portal properties of obsidian combined with our state-of-the-art power cubes. Place one in each corner of the obsidian grid to activate the portal."}};

    /** The 1.12.2 brewing-stand pool (legacy potion metadata 8193-8206). */
    private static final List<Supplier<ItemStack>> BREWING_STAND_POTIONS = List.of(() -> ItemStackData.potion(Items.POTION, Potions.REGENERATION),
        () -> ItemStackData.potion(Items.POTION, Potions.SWIFTNESS), () -> ItemStackData.potion(Items.POTION, Potions.FIRE_RESISTANCE), () -> ItemStackData.potion(Items.POTION, Potions.HEALING),
        () -> ItemStackData.potion(Items.POTION, Potions.NIGHT_VISION), () -> ItemStackData.potion(Items.POTION, Potions.STRENGTH), () -> ItemStackData.potion(Items.POTION, Potions.LEAPING),
        () -> ItemStackData.potion(Items.POTION, Potions.WATER_BREATHING), () -> ItemStackData.potion(Items.POTION, Potions.INVISIBILITY));

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
            ApocalypseGunHelper.randomGun(random, false).flatMap(gun -> ApocalypseGunHelper.spareAmmoFor(gun, random)).ifPresent(stack ->
            {
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
                brewingStand.setItem(slot, BREWING_STAND_POTIONS.get(random.nextInt(BREWING_STAND_POTIONS.size())).get());
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
            return ApocalypseGunHelper.randomLootGun(random).orElse(ItemStack.EMPTY);
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
        List<Item> buckets = BuiltInRegistries.FLUID.stream().filter(fluid -> fluid.isSource(fluid.defaultFluidState())).map(Fluid::getBucket).filter(bucket -> bucket != Items.AIR).distinct()
            .sorted(Comparator.comparing(bucket -> String.valueOf(BuiltInRegistries.ITEM.getKey(bucket)))).toList();
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
        return InfoType.getInfoTypes().values().stream().filter(kind::isInstance).map(kind::cast).distinct().filter(type -> ModUtils.getItemStack(type).isPresent())
            .sorted(Comparator.comparing(InfoType::getShortName, String.CASE_INSENSITIVE_ORDER)).toList();
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
        int foodCount = random.nextInt(5);
        for (int i = 0; i < foodCount; i++)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), randomFood(random, 5));

        if (random.nextInt(5) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.OAK_LOG, 5 + random.nextInt(10)));
        if (random.nextInt(12) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.BOOK));
        if (random.nextInt(12) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.FLINT_AND_STEEL));
        if (random.nextInt(40) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.IRON_AXE));
        if (random.nextInt(40) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.IRON_PICKAXE));
        if (random.nextInt(40) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.IRON_SHOVEL));
        if (random.nextInt(4) == 0)
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.TORCH, 1 + random.nextInt(5)));
        if (random.nextBoolean())
            Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), survivorJournal(random));
        ApocalypseGunHelper.randomAmmoStack(random).ifPresent(stack -> Containers.dropItemStack(level, mob.getX(), mob.getY(), mob.getZ(), stack));
    }

    public static ItemStack survivorJournal(RandomSource random)
    {
        int entry = random.nextInt(SURVIVOR_JOURNALS.length);
        String[] journal = SURVIVOR_JOURNALS[entry];
        List<Component> pages = Arrays.stream(journal).skip(1).map(text -> (Component) Component.literal(text)).toList();
        ItemStack book = ItemStackData.writtenBook(journal[0], "Flan", pages);
        if (entry == 7)
            ItemStackData.setBookGeneration(book, 3);
        return book;
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
        List<Team> teams = Team.values().stream().filter(team -> team != Team.SPECTATORS).sorted(Comparator.comparing(InfoType::getShortName, String.CASE_INSENSITIVE_ORDER)).toList();
        if (teams.isEmpty())
        {
            equipRandomArmorPiece(entity, random);
            return;
        }
        Team team = teams.get(random.nextInt(teams.size()));
        List<PlayerClass> classes = team.getClasses();
        PlayerClass playerClass = classes.isEmpty() ? null : classes.get(random.nextInt(classes.size()));
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack armor = playerClass == null ? ItemStack.EMPTY : playerClass.getArmour(slot);
            if (armor.isEmpty())
                armor = team.getArmour(slot);
            if (!armor.isEmpty())
                entity.setItemSlot(slot, armor.copy());
        }
    }

    private static void equipArmor(LivingEntity entity, ArmorType armor)
    {
        ModUtils.getItemStack(armor).ifPresent(stack ->
        {
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
        armor.sort(Comparator.comparing(InfoType::getShortName, String.CASE_INSENSITIVE_ORDER));
        return armor;
    }

    private static ItemStack randomFood(RandomSource random)
    {
        return randomFood(random, 6);
    }

    private static ItemStack randomFood(RandomSource random, int choices)
    {
        return switch (random.nextInt(choices))
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
