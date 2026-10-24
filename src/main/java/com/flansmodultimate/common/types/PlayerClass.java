package com.flansmodultimate.common.types;

import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.SkinOverrideVariant;
import com.flansmodultimate.common.guns.EnumAttachmentType;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.teams.editor.EditorCatalog;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValuesInLines;

/** Content-pack loadout definition used by the Teams system. */
@NoArgsConstructor
public class PlayerClass extends InfoType implements IPlayerClass
{
    private static final ItemlessTypeRegistry<PlayerClass> CLASSES = new ItemlessTypeRegistry<>("player class");

    @Getter
    private int unlockLevel;
    /** Prestige level a player needs before this class opens up, on top of the unlock rank. */
    @Getter
    private int requiredPrestigeLevel;
    /** Most players of one team who may use this class at once; negative for no limit. */
    @Getter
    private int classLimit = -1;
    /** Largest share of a team, in percent, that may use this class at once; negative for no limit. */
    @Getter
    private float classLimitPercent = -1F;
    @Getter
    private String skinOverride = StringUtils.EMPTY;
    /** Client-side location of the SkinOverride texture, before it is checked against the player model. */
    @Getter @Nullable
    private ResourceLocation skinOverrideTexture;
    @Getter
    private List<SkinOverrideVariant> skinOverrideVariants = List.of();
    private final Map<EquipmentSlot, String> armour = new LinkedHashMap<>();
    private List<StartingItem> startingItems = List.of();
    private List<ItemStack> previewItems;

    @Override
    public void load(TypeFile file)
    {
        super.load(file);
        uniqueShortName = CLASSES.register(this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        unlockLevel = Math.max(0, readValue("UnlockLevel", unlockLevel, file));
        requiredPrestigeLevel = Math.max(0, readValue("RequiredPrestigeLevel", requiredPrestigeLevel, file));
        classLimit = Math.max(-1, readValue("ClassLimit", classLimit, file));
        classLimitPercent = readValue("ClassLimitPercent", classLimitPercent, file);
        classLimitPercent = readValue("ClassLimitPercentage", classLimitPercent, file);
        classLimitPercent = Float.isFinite(classLimitPercent) ? Math.max(-1F, Math.min(100F, classLimitPercent)) : -1F;
        // Legacy packs write skin names like "Zombie"; texture paths must be sanitized
        skinOverrideVariants = readValuesInLines("SkinOverride", file, 1).orElse(List.of()).stream().flatMap(Arrays::stream).map(ResourceUtils::sanitize).filter(StringUtils::isNotBlank).distinct()
            .map(name -> new SkinOverrideVariant(name, null)).toList();
        skinOverride = skinOverrideVariants.isEmpty() ? StringUtils.EMPTY : skinOverrideVariants.get(0).name();
        readArmour(file, EquipmentSlot.HEAD, "Hat", "Helmet");
        // Body is the spelling ArmorType has always accepted for the chest slot.
        readArmour(file, EquipmentSlot.CHEST, "Chest", "Top", "Body");
        readArmour(file, EquipmentSlot.LEGS, "Legs", "Bottom");
        readArmour(file, EquipmentSlot.FEET, "Shoes", "Boots");

        List<StartingItem> parsed = new ArrayList<>();
        for (String[] values : readValuesInLines("AddItem", file, 1).orElse(List.of()))
        {
            int amount = parsePositive(values, 1, 1);
            int damage = parsePositive(values, 2, 0);
            int paintjob = 0;
            if (values.length > 3 && values[3].regionMatches(true, 0, "Paintjob=", 0, 9))
                paintjob = parsePositive(new String[]{values[3].substring(9)}, 0, 0);
            parsed.add(new StartingItem(values[0], amount, damage, paintjob));
        }
        startingItems = List.copyOf(parsed);
        previewItems = null;
    }

    @Override
    protected void readClient(TypeFile file)
    {
        super.readClient(file);
        skinOverrideVariants = skinOverrideVariants.stream().map(skin -> new SkinOverrideVariant(skin.name(), loadTexture(skin.name(), this))).toList();
        skinOverrideTexture = skinOverrideVariants.isEmpty() ? null : skinOverrideVariants.get(0).texture();
    }

    /** Later names win, so a definition listing several spellings keeps the last one authored. */
    private void readArmour(TypeFile file, EquipmentSlot slot, String... names)
    {
        String value = null;
        for (String name : names)
            value = readValue(name, value, file);
        if (StringUtils.isNotBlank(value) && !"none".equalsIgnoreCase(value))
            armour.put(slot, value);
    }

    @Override
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
        // Content pack shortnames are resolved first: an unqualified AddItem name means pack content in
        // every legacy pack, and a namespaced id such as "minecraft:stone" is never a shortname anyway.
        ItemStack stack = ModUtils.getItemStack(InfoType.getInfoType(parts[0], contentPack), definition.amount(), definition.damage())
            .or(() -> ModUtils.getItemStack(parts[0], definition.amount(), definition.damage())).orElse(ItemStack.EMPTY);
        if (stack.isEmpty())
        {
            FlansLog.log.warn("Unknown starting item '{}' in player class {}", parts[0], originalShortName);
            return ItemStack.EMPTY;
        }

        if (stack.getItem() instanceof GunItem gunItem && parts.length > 1)
        {
            gunItem.getConfigType().checkForTags(stack);
            CompoundTag gunData = ItemStackData.copy(stack);
            CompoundTag attachments = gunData.getCompound(GunItem.NBT_ATTACHMENTS);
            int generic = 0;
            for (int i = 1; i < parts.length; i++)
            {
                AttachmentType attachment = AttachmentType.getAttachment(parts[i]);
                ItemStack attachmentStack = attachment == null ? ItemStack.EMPTY : ModUtils.getItemStack(attachment).orElse(ItemStack.EMPTY);
                if (attachment == null || attachmentStack.isEmpty())
                    continue;
                String slot = attachmentSlot(attachment.getEnumAttachmentType(), generic);
                if (attachment.getEnumAttachmentType() == EnumAttachmentType.GENERIC)
                    generic++;
                attachments.put(slot, ItemStackData.saveBuiltIn(attachmentStack));
            }
            gunData.put(GunItem.NBT_ATTACHMENTS, attachments);
            ItemStackData.set(stack, gunData);
            gunItem.getConfigType().getDefaultAmmo().flatMap(ModUtils::getItemStack).ifPresent(ammo -> gunItem.setBulletItemStack(stack, ammo, 0, ItemStackData.builtInRegistries()));
        }
        applyPaintjob(stack, definition.paintjob());
        return stack;
    }

    private static void applyPaintjob(ItemStack stack, int paintjob)
    {
        if (stack.getItem() instanceof IPaintableItem<?> paintableItem)
        {
            PaintableType paintableType = paintableItem.getPaintableType();
            paintableType.applyPaintjobToStack(stack, paintableType.getPaintjob(paintjob));
        }
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

    @Override
    public ItemStack getArmour(EquipmentSlot slot)
    {
        String id = armour.get(slot);
        if (id == null)
            return ItemStack.EMPTY;
        return ModUtils.getItemStack(InfoType.getInfoType(id, contentPack)).orElse(ItemStack.EMPTY);
    }

    public static java.util.Collection<PlayerClass> values()
    {
        return EditorCatalog.current().classes(CLASSES.values());
    }

    public static Collection<PlayerClass> packValues()
    {
        return CLASSES.values();
    }

    /** Explicit slot templates are only used by edited kits; legacy packs keep ordered insertion. */
    public Map<Integer, ItemStack> createStartingInventory()
    {
        return Map.of();
    }

    /** Resolves a class by the unique name {@link #getShortName()} reports. */
    @Nullable
    public static PlayerClass getPlayerClass(@Nullable String id)
    {
        return EditorCatalog.current().playerClass(id, CLASSES.get(id));
    }

    /** Resolves a class named inside {@code provider}, such as the {@code AddClass} line of a team. */
    @Nullable
    public static PlayerClass getPlayerClass(@Nullable String id, @Nullable IContentProvider provider)
    {
        return EditorCatalog.current().playerClass(id, CLASSES.get(id, provider));
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

    private record StartingItem(String itemAndAttachments, int amount, int damage, int paintjob)
    {}
}
