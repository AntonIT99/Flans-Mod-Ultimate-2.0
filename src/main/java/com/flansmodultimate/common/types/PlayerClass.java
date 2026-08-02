package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.EnumAttachmentType;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValuesInLines;

/** Content-pack loadout definition used by the Teams system. */
@NoArgsConstructor
public class PlayerClass extends InfoType
{
    private static final Map<String, PlayerClass> CLASSES = new LinkedHashMap<>();

    @Getter
    private int unlockLevel;
    @Getter
    private String skinOverride = StringUtils.EMPTY;
    private final Map<EquipmentSlot, String> armour = new LinkedHashMap<>();
    private List<StartingItem> startingItems = List.of();
    private List<ItemStack> previewItems;

    @Override
    public void load(TypeFile file)
    {
        super.load(file);
        if (StringUtils.isNotBlank(originalShortName))
            CLASSES.put(normalize(originalShortName), this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        unlockLevel = Math.max(0, readValue("UnlockLevel", unlockLevel, file));
        skinOverride = readValue("SkinOverride", skinOverride, file);
        readArmour(file, EquipmentSlot.HEAD, "Hat", "Helmet");
        readArmour(file, EquipmentSlot.CHEST, "Chest", "Top");
        readArmour(file, EquipmentSlot.LEGS, "Legs", "Bottom");
        readArmour(file, EquipmentSlot.FEET, "Shoes", "Boots");

        List<StartingItem> parsed = new ArrayList<>();
        for (String[] values : readValuesInLines("AddItem", file, 1).orElse(List.of()))
        {
            int amount = parsePositive(values, 1, 1);
            int damage = parsePositive(values, 2, 0);
            parsed.add(new StartingItem(values[0], amount, damage));
        }
        startingItems = List.copyOf(parsed);
        previewItems = null;
    }

    private void readArmour(TypeFile file, EquipmentSlot slot, String primary, String alias)
    {
        String value = readValue(primary, null, file);
        value = readValue(alias, value, file);
        if (StringUtils.isNotBlank(value) && !"none".equalsIgnoreCase(value))
            armour.put(slot, value);
    }

    public List<ItemStack> createStartingItems()
    {
        List<ItemStack> result = new ArrayList<>(startingItems.size());
        for (StartingItem definition : startingItems)
        {
            ItemStack stack = createStack(definition);
            if (!stack.isEmpty())
                result.add(stack);
        }
        return result;
    }

    /** Cached server-side preview used by frequent scoreboard snapshots. */
    public List<ItemStack> createStartingItemPreviews()
    {
        if (previewItems == null)
            previewItems = createStartingItems().stream().map(ItemStack::copy).toList();
        return previewItems.stream().map(ItemStack::copy).toList();
    }

    private ItemStack createStack(StartingItem definition)
    {
        String[] parts = definition.itemAndAttachments().split("\\+");
        ItemStack stack = ModUtils.getItemStack(parts[0], definition.amount(), definition.damage()).orElseGet(() ->
            ModUtils.getItemStack(InfoType.getInfoType(parts[0], contentPack), definition.amount(), definition.damage()).orElse(ItemStack.EMPTY));
        if (stack.isEmpty())
        {
            FlansMod.log.warn("Unknown starting item '{}' in player class {}", parts[0], originalShortName);
            return ItemStack.EMPTY;
        }

        if (stack.getItem() instanceof GunItem gunItem && parts.length > 1)
        {
            gunItem.getConfigType().checkForTags(stack);
            CompoundTag attachments = stack.getOrCreateTag().getCompound(GunItem.NBT_ATTACHMENTS);
            int generic = 0;
            for (int i = 1; i < parts.length; i++)
            {
                AttachmentType attachment = AttachmentType.getAttachment(parts[i]);
                if (attachment == null)
                    continue;
                ItemStack attachmentStack = ModUtils.getItemStack(attachment).orElse(ItemStack.EMPTY);
                if (attachmentStack.isEmpty())
                    continue;
                String slot = attachmentSlot(attachment.getEnumAttachmentType(), generic);
                if (attachment.getEnumAttachmentType() == EnumAttachmentType.GENERIC)
                    generic++;
                attachments.put(slot, attachmentStack.save(new CompoundTag()));
            }
            stack.getOrCreateTag().put(GunItem.NBT_ATTACHMENTS, attachments);
            gunItem.getConfigType().getDefaultAmmo().flatMap(ModUtils::getItemStack)
                .ifPresent(ammo -> gunItem.setBulletItemStack(stack, ammo, 0));
        }
        return stack;
    }

    private static String attachmentSlot(EnumAttachmentType type, int generic)
    {
        return switch (type)
        {
            case BARREL -> GunItem.NBT_BARREL;
            case SIGHTS -> GunItem.NBT_SCOPE;
            case STOCK -> GunItem.NBT_STOCK;
            case GRIP -> GunItem.NBT_GRIP;
            case GADGET -> GunItem.NBT_GADGET;
            case SLIDE -> GunItem.NBT_SLIDE;
            case PUMP -> GunItem.NBT_PUMP;
            case ACCESSORY -> GunItem.NBT_ACCESSORY;
            case GENERIC -> GunItem.NBT_GENERIC + generic;
        };
    }

    public ItemStack getArmour(EquipmentSlot slot)
    {
        String id = armour.get(slot);
        if (id == null)
            return ItemStack.EMPTY;
        return ModUtils.getItemStack(InfoType.getInfoType(id, contentPack)).orElse(ItemStack.EMPTY);
    }

    public static java.util.Collection<PlayerClass> values()
    {
        return Collections.unmodifiableCollection(CLASSES.values());
    }

    @Nullable
    public static PlayerClass getPlayerClass(@Nullable String id)
    {
        return StringUtils.isBlank(id) ? null : CLASSES.get(normalize(id));
    }

    private static int parsePositive(String[] values, int index, int fallback)
    {
        if (index >= values.length)
            return fallback;
        try
        {
            return Math.max(0, Integer.parseInt(values[index]));
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record StartingItem(String itemAndAttachments, int amount, int damage) {}
}
