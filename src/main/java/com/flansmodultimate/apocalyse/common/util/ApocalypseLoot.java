package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseLoot
{
    private static final String TAG_BOOK_TITLE = "title";
    private static final String TAG_BOOK_AUTHOR = "author";
    private static final String TAG_BOOK_PAGES = "pages";
    private static final String[] JOURNAL_LINES = new String[] {
        "The sky turned yellow today. The portal brought us somewhere worse than the wasteland.",
        "If you find the power cubes, do not stand between them unless you are ready to leave.",
        "The survivors stopped trusting anyone with clean armor. They still trade bullets for food.",
        "Sulphur pits mark the old roads. Keep water away from the acid and keep moving.",
        "The skull drones patrol at night. Their boss laughs before the nukes fall."
    };

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
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.getOrCreateTag().putString(TAG_BOOK_TITLE, "Survivor Journal");
        book.getOrCreateTag().putString(TAG_BOOK_AUTHOR, "Unknown Survivor");
        ListTag pages = new ListTag();
        String text = JOURNAL_LINES[random.nextInt(JOURNAL_LINES.length)];
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY))));
        book.getOrCreateTag().put(TAG_BOOK_PAGES, pages);
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
