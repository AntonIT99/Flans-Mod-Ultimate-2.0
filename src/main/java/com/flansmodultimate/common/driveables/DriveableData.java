package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.platform.item.ItemStackData;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Server-authoritative inventory and damage state carried by a driveable item/entity.
 * It understands both the modern compact lists and the legacy per-slot NBT keys.
 */
public final class DriveableData implements Container
{
    /** Hard packet bound; mecha slots are always preferred over decorative missile rows. */
    public static final int MAX_RENDER_SYNC_SLOTS = 256;
    public static final String NBT_DATA = "driveable_data";
    private static final String NBT_TYPE = "type";
    private static final String NBT_ENGINE = "engine";
    private static final String NBT_FUEL = "fuel_in_tank";
    private static final String NBT_PAINT = "paintjob_id";
    private static final String NBT_ITEMS = "items";
    private static final String NBT_PARTS = "parts";

    @Getter private final DriveableType driveableType;
    private final HolderLookup.Provider registries;
    @Getter private final int numAmmoSlots;
    @Getter private final int numBombSlots;
    @Getter private final int numMissileSlots;
    @Getter private final int numCargoSlots;
    @Getter private final int numMechaSlots;
    @Getter private final Map<EnumDriveablePart, DriveablePart> parts;
    @Getter private final NonNullList<ItemStack> inventory;
    @Getter private float fuelInTank;
    @Getter @Setter private int paintjobID;
    @Getter @Setter private boolean inventoryChanged;
    @Getter private String engineShortName = StringUtils.EMPTY;
    private CompoundTag preservedTag = new CompoundTag();

    public DriveableData(@NotNull DriveableType driveableType, @NotNull HolderLookup.Provider registries)
    {
        this.driveableType = driveableType;
        this.registries = registries;
        numAmmoSlots = Math.max(0, driveableType.getNumAmmoSlots());
        numBombSlots = Math.max(0, driveableType.getNumBombSlots());
        numMissileSlots = Math.max(0, driveableType.getNumMissileSlots());
        numCargoSlots = Math.max(0, driveableType.getNumCargoSlots());
        numMechaSlots = driveableType instanceof MechaType ? EnumMechaSlotType.values().length : 0;
        inventory = NonNullList.withSize(getFuelSlot() + 1, ItemStack.EMPTY);

        EnumMap<EnumDriveablePart, DriveablePart> mutableParts = new EnumMap<>(EnumDriveablePart.class);
        for (EnumDriveablePart part : EnumDriveablePart.values())
            mutableParts.put(part, new DriveablePart(part, driveableType.getHealth().get(part)));
        parts = Collections.unmodifiableMap(mutableParts);

        PartType defaultEngine = PartType.getDefaultEngine(driveableType.getType());
        if (defaultEngine != null)
            engineShortName = defaultEngine.getShortName();
    }

    public DriveableData(@NotNull DriveableType driveableType, @Nullable CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        this(driveableType, registries);
        if (tag != null)
            load(tag);
    }

    public static DriveableData fromStack(@NotNull DriveableType type, @NotNull ItemStack stack, @NotNull HolderLookup.Provider registries)
    {
        return new DriveableData(type, ItemStackData.copy(stack), registries);
    }

    public String getType()
    {
        return driveableType.getShortName();
    }

    @Nullable
    public DriveablePart getPart(EnumDriveablePart part)
    {
        return parts.get(part);
    }

    public void setEngineShortName(@Nullable String engineShortName)
    {
        this.engineShortName = StringUtils.defaultString(engineShortName).trim();
    }

    public void setFuelInTank(float fuelInTank)
    {
        float finiteFuel = Float.isFinite(fuelInTank) ? fuelInTank : 0F;
        this.fuelInTank = Math.max(0F, Math.min(finiteFuel, Math.max(0F, driveableType.getFuelTankSize())));
    }

    @Nullable
    public PartType getEngine()
    {
        InfoType resolved = InfoType.getInfoType(engineShortName, driveableType.getContentPack());
        return resolved instanceof PartType partType ? partType : PartType.getDefaultEngine(driveableType.getType());
    }

    public int getAmmoInventoryStart() { return 0; }
    public int getBombInventoryStart() { return numAmmoSlots; }
    public int getMissileInventoryStart() { return numAmmoSlots + numBombSlots; }
    public int getCargoInventoryStart() { return numAmmoSlots + numBombSlots + numMissileSlots; }
    public int getMechaInventoryStart() { return numAmmoSlots + numBombSlots + numMissileSlots + numCargoSlots; }
    public int getFuelSlot() { return getMechaInventoryStart() + numMechaSlots; }

    public int getRenderSlotCount()
    {
        int mechaSlots = Math.min(numMechaSlots, MAX_RENDER_SYNC_SLOTS);
        return mechaSlots + Math.min(numMissileSlots, MAX_RENDER_SYNC_SLOTS - mechaSlots);
    }

    /** Maps a compact render-state index to the real container slot. */
    public int getRenderSlotIndex(int renderIndex)
    {
        int mechaSlots = Math.min(numMechaSlots, MAX_RENDER_SYNC_SLOTS);
        int missileSlots = Math.min(numMissileSlots, MAX_RENDER_SYNC_SLOTS - mechaSlots);
        if (renderIndex < 0 || renderIndex >= missileSlots + mechaSlots)
            return -1;
        return renderIndex < missileSlots
            ? getMissileInventoryStart() + renderIndex
            : getMechaInventoryStart() + renderIndex - missileSlots;
    }

    public boolean isRenderSlot(int slot)
    {
        int mechaSlots = Math.min(numMechaSlots, MAX_RENDER_SYNC_SLOTS);
        int missileSlots = Math.min(numMissileSlots, MAX_RENDER_SYNC_SLOTS - mechaSlots);
        return slot >= getMissileInventoryStart() && slot < getMissileInventoryStart() + missileSlots
            || slot >= getMechaInventoryStart() && slot < getMechaInventoryStart() + mechaSlots;
    }

    public ItemStack getAmmo(int index) { return getTypedSlot(index, numAmmoSlots, getAmmoInventoryStart()); }
    public ItemStack getBomb(int index) { return getTypedSlot(index, numBombSlots, getBombInventoryStart()); }
    public ItemStack getMissile(int index) { return getTypedSlot(index, numMissileSlots, getMissileInventoryStart()); }
    public ItemStack getCargo(int index) { return getTypedSlot(index, numCargoSlots, getCargoInventoryStart()); }
    public ItemStack getMechaAddon(EnumMechaSlotType slot) { return slot == null ? ItemStack.EMPTY : getTypedSlot(slot.ordinal(), numMechaSlots, getMechaInventoryStart()); }
    public ItemStack getFuelStack() { return getItem(getFuelSlot()); }

    public void setAmmo(int index, ItemStack stack) { setTypedSlot(index, numAmmoSlots, getAmmoInventoryStart(), stack); }
    public void setBomb(int index, ItemStack stack) { setTypedSlot(index, numBombSlots, getBombInventoryStart(), stack); }
    public void setMissile(int index, ItemStack stack) { setTypedSlot(index, numMissileSlots, getMissileInventoryStart(), stack); }
    public void setCargo(int index, ItemStack stack) { setTypedSlot(index, numCargoSlots, getCargoInventoryStart(), stack); }
    public void setMechaAddon(EnumMechaSlotType slot, ItemStack stack) { if (slot != null) setTypedSlot(slot.ordinal(), numMechaSlots, getMechaInventoryStart(), stack); }
    public void setFuelStack(ItemStack stack) { setItem(getFuelSlot(), stack); }

    private ItemStack getTypedSlot(int index, int size, int offset)
    {
        return index >= 0 && index < size ? getItem(offset + index) : ItemStack.EMPTY;
    }

    private void setTypedSlot(int index, int size, int offset, ItemStack stack)
    {
        if (index >= 0 && index < size)
            setItem(offset + index, stack);
    }

    @Override
    public int getContainerSize()
    {
        return inventory.size();
    }

    @Override
    public boolean isEmpty()
    {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        ItemStack removed = ContainerHelper.removeItem(inventory, slot, amount);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack removed = ContainerHelper.takeItem(inventory, slot);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= inventory.size())
            return;
        ItemStack safeStack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
        if (!safeStack.isEmpty())
            safeStack.setCount(Math.min(safeStack.getCount(), Math.min(getMaxStackSize(), safeStack.getMaxStackSize())));
        inventory.set(slot, safeStack);
        setChanged();
    }

    @Override
    public void setChanged()
    {
        inventoryChanged = true;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        inventory.replaceAll(ignored -> ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= inventory.size() || stack == null || stack.isEmpty())
            return false;
        if (slot < getBombInventoryStart())
            return isGunAmmo(slot, stack);
        if (slot < getMissileInventoryStart())
            return isAmmo(stack, EnumWeaponType.BOMB, EnumWeaponType.MINE);
        if (slot < getCargoInventoryStart())
            return isAmmo(stack, EnumWeaponType.MISSILE, EnumWeaponType.SHELL);
        if (slot < getMechaInventoryStart())
            return canPlaceCargo(stack);
        if (slot < getFuelSlot())
        {
            int mechaSlot = slot - getMechaInventoryStart();
            if (mechaSlot < 0 || mechaSlot >= EnumMechaSlotType.values().length)
                return false;
            EnumMechaSlotType slotType = EnumMechaSlotType.values()[mechaSlot];
            if ((slotType == EnumMechaSlotType.LEFT_TOOL || slotType == EnumMechaSlotType.RIGHT_TOOL) && stack.getItem() instanceof GunItem)
                return true;
            if ((slotType == EnumMechaSlotType.LEFT_ARM || slotType == EnumMechaSlotType.RIGHT_ARM) && stack.getItem() instanceof BulletItem)
                return true;
            return stack.getItem() instanceof MechaAddonItem addon && slotType.accepts(addon.getConfigType().getMechaItemType());
        }
        PartType engine = getEngine();
        if (engine != null && engine.isUseRFPower())
            return ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM) != null;
        return stack.getItem() instanceof PartItem partItem && partItem.getConfigType().getCategory() == PartType.Category.FUEL;
    }

    private boolean isAmmo(ItemStack stack, EnumWeaponType first, EnumWeaponType second)
    {
        if (!(stack.getItem() instanceof BulletItem bulletItem))
            return false;
        BulletType bulletType = bulletItem.getConfigType();
        EnumWeaponType weaponType = bulletType.getWeaponType();
        return driveableType.isValidAmmo(bulletType) && (weaponType == first || weaponType == second);
    }

    private boolean isGunAmmo(int slot, ItemStack stack)
    {
        return stack.getItem() instanceof ShootableItem shootable
            && driveableType.isValidGunAmmo(slot, shootable.getConfigType());
    }

    private boolean canPlaceCargo(ItemStack stack)
    {
        if (!(driveableType instanceof MechaType mechaType) || !mechaType.isRestrictInventoryInput())
            return true;
        if (stack.getItem() instanceof ShootableItem)
            return true;
        if (stack.getItem() instanceof PartItem partItem && partItem.getConfigType().getCategory() == PartType.Category.FUEL)
            return true;
        return mechaType.isAllowMechaToolsInRestrictedInv() && stack.getItem() instanceof MechaAddonItem;
    }

    public CompoundTag save(@Nullable CompoundTag destination)
    {
        CompoundTag output = destination == null ? new CompoundTag() : destination;
        CompoundTag data = preservedTag.copy();
        removeSerializedState(data);
        data.putString(NBT_TYPE, getType());
        data.putString(NBT_ENGINE, engineShortName);
        data.putFloat(NBT_FUEL, fuelInTank);
        data.putInt(NBT_PAINT, paintjobID);

        ListTag itemTags = new ListTag();
        for (int slot = 0; slot < inventory.size(); slot++)
        {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty())
                continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", slot);
            ItemStackData.save(stack, registries, entry);
            itemTags.add(entry);
        }
        data.put(NBT_ITEMS, itemTags);

        ListTag partTags = new ListTag();
        for (DriveablePart part : parts.values())
        {
            CompoundTag entry = new CompoundTag();
            part.save(entry);
            partTags.add(entry);
        }
        data.put(NBT_PARTS, partTags);

        // Keep one root-level legacy mirror for old worlds and integrations. Writing it
        // inside the canonical payload as well needlessly tripled occupied slot data.
        writeLegacyState(output);
        output.put(NBT_DATA, data);
        output.putInt(IPaintableItem.NBT_PAINTJOB_ID, paintjobID);
        return output;
    }

    /**
     * Writes only state needed by remote renderers. This deliberately excludes cargo,
     * fuel items and weapon banks from entity spawn packets.
     */
    public CompoundTag saveRenderState(@Nullable CompoundTag destination)
    {
        CompoundTag output = destination == null ? new CompoundTag() : destination;
        CompoundTag data = new CompoundTag();
        data.putString(NBT_TYPE, getType());
        data.putString(NBT_ENGINE, engineShortName);
        data.putFloat(NBT_FUEL, fuelInTank);
        data.putInt(NBT_PAINT, paintjobID);

        ListTag itemTags = new ListTag();
        for (int index = 0; index < getRenderSlotCount(); index++)
        {
            int slot = getRenderSlotIndex(index);
            ItemStack stack = getItem(slot);
            if (stack.isEmpty())
                continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", slot);
            ItemStackData.save(stack, registries, entry);
            itemTags.add(entry);
        }
        data.put(NBT_ITEMS, itemTags);

        ListTag partTags = new ListTag();
        for (DriveablePart part : parts.values())
        {
            CompoundTag entry = new CompoundTag();
            part.save(entry);
            partTags.add(entry);
        }
        data.put(NBT_PARTS, partTags);
        output.put(NBT_DATA, data);
        output.putInt(IPaintableItem.NBT_PAINTJOB_ID, paintjobID);
        return output;
    }

    /** Applies a trusted, bounded S2C render slot without dirtying server inventory state. */
    public void applyRenderSlot(int slot, @Nullable ItemStack stack)
    {
        if (!isRenderSlot(slot))
            return;
        putLoadedStack(slot, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    /** Removes inventory/damage payloads while retaining unrelated item metadata and capabilities. */
    public void removeSerializedState(@Nullable CompoundTag tag)
    {
        if (tag == null)
            return;
        boolean canonicalAtRoot = !tag.contains(NBT_DATA)
            && (tag.contains(NBT_ITEMS) || tag.contains(NBT_PARTS)
                || getType().equalsIgnoreCase(tag.getString(NBT_TYPE).orElse(StringUtils.EMPTY)));
        tag.remove(NBT_DATA);
        if (canonicalAtRoot)
        {
            tag.remove(NBT_TYPE);
            tag.remove(NBT_ENGINE);
            tag.remove(NBT_FUEL);
            tag.remove(NBT_PAINT);
            tag.remove(NBT_ITEMS);
            tag.remove(NBT_PARTS);
        }
        tag.remove(IPaintableItem.NBT_PAINTJOB_ID);
        tag.remove("Type");
        tag.remove("Engine");
        tag.remove("FuelInTank");
        tag.remove("Paint");
        tag.remove("Fuel");
        for (String key : new ArrayList<>(tag.keySet()))
        {
            if (isLegacyIndexedKey(key, "Ammo ") || isLegacyIndexedKey(key, "Bombs ")
                || isLegacyIndexedKey(key, "Missiles ") || isLegacyIndexedKey(key, "Cargo "))
                tag.remove(key);
        }
        for (EnumMechaSlotType slot : EnumMechaSlotType.values())
            tag.remove(legacyMechaSlotName(slot));
        for (EnumDriveablePart part : EnumDriveablePart.values())
        {
            tag.remove(part.getShortName() + "_Health");
            tag.remove(part.getShortName() + "_Fire");
        }
    }

    public ItemStack copyToStack(ItemStack stack)
    {
        ItemStackData.set(stack, save(ItemStackData.copy(stack)));
        return stack;
    }

    public void load(CompoundTag source)
    {
        boolean hasNestedData = source.getCompound(NBT_DATA).isPresent();
        CompoundTag data = hasNestedData ? source.getCompoundOrEmpty(NBT_DATA) : source;
        // Never embed an entire legacy item/entity root inside the modern payload.
        // Unknown keys already nested in our own payload are retained for extensions.
        preservedTag = hasNestedData ? data.copy() : new CompoundTag();
        preservedTag.remove(NBT_DATA);
        engineShortName = firstNonBlank(data.getString(NBT_ENGINE).orElse(StringUtils.EMPTY), data.getString("Engine").orElse(StringUtils.EMPTY),
            hasNestedData ? source.getString(NBT_ENGINE).orElse(StringUtils.EMPTY) : StringUtils.EMPTY,
            hasNestedData ? source.getString("Engine").orElse(StringUtils.EMPTY) : StringUtils.EMPTY, engineShortName);
        setFuelInTank(readFuel(data, hasNestedData ? source : null));
        paintjobID = source.contains(IPaintableItem.NBT_PAINTJOB_ID)
            ? source.getInt(IPaintableItem.NBT_PAINTJOB_ID).orElse(0)
            : data.getInt(NBT_PAINT).orElseGet(() -> data.getInt("Paint").orElseGet(() ->
                hasNestedData ? source.getInt(NBT_PAINT).orElseGet(() -> source.getInt("Paint").orElse(0)) : 0));

        inventory.replaceAll(ignored -> ItemStack.EMPTY);
        if (data.getList(NBT_ITEMS).isPresent())
            loadItemList(data.getListOrEmpty(NBT_ITEMS));
        else if (hasNestedData && source.getList(NBT_ITEMS).isPresent())
            loadItemList(source.getListOrEmpty(NBT_ITEMS));
        else
        {
            if (hasNestedData)
            {
                loadLegacyInventory(source);
                loadLegacyMechaInventory(source);
            }
            loadLegacyInventory(data);
            loadLegacyMechaInventory(data);
        }

        if (data.getList(NBT_PARTS).isPresent())
            loadPartList(data.getListOrEmpty(NBT_PARTS));
        else if (hasNestedData && source.getList(NBT_PARTS).isPresent())
            loadPartList(source.getListOrEmpty(NBT_PARTS));
        else
        {
            if (hasNestedData)
                parts.values().forEach(part -> part.loadLegacy(source));
            parts.values().forEach(part -> part.loadLegacy(data));
        }
        inventoryChanged = false;
    }

    private float readFuel(CompoundTag data, @Nullable CompoundTag root)
    {
        if (data.getFloat(NBT_FUEL).isPresent())
            return data.getFloat(NBT_FUEL).orElse(0F);
        if (data.getFloat("FuelInTank").isPresent())
            return data.getFloat("FuelInTank").orElse(0F);
        if (root != null && root.getFloat(NBT_FUEL).isPresent())
            return root.getFloat(NBT_FUEL).orElse(0F);
        return root == null ? 0F : root.getFloat("FuelInTank").orElse(0F);
    }

    private void loadItemList(ListTag itemTags)
    {
        for (int i = 0; i < itemTags.size(); i++)
        {
            CompoundTag entry = itemTags.getCompoundOrEmpty(i);
            putLoadedStack(entry.getInt("slot").orElse(-1), ItemStackData.parse(registries, entry));
        }
    }

    private void loadPartList(ListTag partTags)
    {
        for (int i = 0; i < partTags.size(); i++)
        {
            CompoundTag entry = partTags.getCompoundOrEmpty(i);
            DriveablePart part = parts.get(EnumDriveablePart.getPart(entry.getString("part").orElse(StringUtils.EMPTY)));
            if (part != null)
                part.load(entry);
        }
    }

    private void loadLegacyInventory(CompoundTag data)
    {
        loadLegacyRange(data, "Ammo ", getAmmoInventoryStart(), numAmmoSlots);
        loadLegacyRange(data, "Bombs ", getBombInventoryStart(), numBombSlots);
        loadLegacyRange(data, "Missiles ", getMissileInventoryStart(), numMissileSlots);
        loadLegacyRange(data, "Cargo ", getCargoInventoryStart(), numCargoSlots);
        if (data.getCompound("Fuel").isPresent())
            putLoadedStack(getFuelSlot(), ItemStackData.parse(registries, data.getCompoundOrEmpty("Fuel")));
    }

    private void loadLegacyRange(CompoundTag data, String prefix, int offset, int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (data.getCompound(prefix + i).isPresent())
                putLoadedStack(offset + i, ItemStackData.parse(registries, data.getCompoundOrEmpty(prefix + i)));
        }
    }

    private void loadLegacyMechaInventory(CompoundTag data)
    {
        for (EnumMechaSlotType slot : EnumMechaSlotType.values())
        {
            String key = legacyMechaSlotName(slot);
            if (data.getCompound(key).isPresent())
                putLoadedStack(getMechaInventoryStart() + slot.ordinal(), ItemStackData.parse(registries, data.getCompoundOrEmpty(key)));
        }
    }

    private void putLoadedStack(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= inventory.size())
            return;
        if (stack.isEmpty())
        {
            inventory.set(slot, ItemStack.EMPTY);
            return;
        }
        stack.setCount(Math.min(stack.getCount(), Math.min(getMaxStackSize(), stack.getMaxStackSize())));
        inventory.set(slot, stack);
    }

    private void writeLegacyState(CompoundTag tag)
    {
        tag.putString("Type", getType());
        tag.putString("Engine", engineShortName);
        tag.putInt("FuelInTank", Math.round(fuelInTank));
        tag.putInt("Paint", paintjobID);

        writeLegacyRange(tag, "Ammo ", getAmmoInventoryStart(), numAmmoSlots);
        writeLegacyRange(tag, "Bombs ", getBombInventoryStart(), numBombSlots);
        writeLegacyRange(tag, "Missiles ", getMissileInventoryStart(), numMissileSlots);
        writeLegacyRange(tag, "Cargo ", getCargoInventoryStart(), numCargoSlots);
        writeLegacyStack(tag, "Fuel", getFuelStack());

        for (EnumMechaSlotType slot : EnumMechaSlotType.values())
            writeLegacyStack(tag, legacyMechaSlotName(slot), getMechaAddon(slot));
        parts.values().forEach(part -> part.saveLegacy(tag));
    }

    private void writeLegacyRange(CompoundTag tag, String prefix, int offset, int length)
    {
        for (String key : new ArrayList<>(tag.keySet()))
        {
            if (isLegacyIndexedKey(key, prefix))
                tag.remove(key);
        }
        for (int index = 0; index < length; index++)
            writeLegacyStack(tag, prefix + index, getItem(offset + index));
    }

    private static boolean isLegacyIndexedKey(String key, String prefix)
    {
        if (!key.startsWith(prefix) || key.length() == prefix.length())
            return false;
        for (int index = prefix.length(); index < key.length(); index++)
        {
            if (!Character.isDigit(key.charAt(index)))
                return false;
        }
        return true;
    }

    private void writeLegacyStack(CompoundTag tag, String key, ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            tag.remove(key);
            return;
        }
        CompoundTag stackTag = new CompoundTag();
        ItemStackData.save(stack, registries, stackTag);
        tag.put(key, stackTag);
    }

    private static String legacyMechaSlotName(EnumMechaSlotType slot)
    {
        String[] words = slot.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++)
            name.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        return name.toString();
    }

    private static String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (StringUtils.isNotBlank(value))
                return value;
        }
        return StringUtils.EMPTY;
    }
}
