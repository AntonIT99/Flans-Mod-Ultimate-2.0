package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.properties.NpcTypeProperty;
import lombok.Getter;

import net.minecraft.nbt.*;

import java.util.*;

/**
 * Per-NPC combat settings that are not plain switches: knockback handling, minimum hit damage, the model properties
 * the NPC keeps its own value for, and one-shot imports requested from the editor.
 *
 * <p>
 * Imports are requests: the editor only records them, and the server carries them out on the NPC's next tick, where the
 * model and the equipment are authoritative, then clears them.
 * </p>
 */
public final class NpcCombatSettings
{
    public static final String NBT_KEY = "WolffsModCombat";
    public static final int MAX_MINIMUM_HIT = 100_000;
    private static final String KNOCKBACK_KEY = "Knockback";
    private static final String MINIMUM_HITS_KEY = "MinimumHits";
    private static final String CUSTOM_KEY = "CustomProperties";
    private static final String PROPERTY_IMPORTS_KEY = "PropertyImports";
    private static final String EQUIPMENT_IMPORTS_KEY = "EquipmentImports";

    @Getter
    private Knockback knockback = Knockback.MODEL;
    private final Map<NpcDamageCategory, Integer> minimumHits = new EnumMap<>(NpcDamageCategory.class);
    private final Set<NpcTypeProperty> custom = EnumSet.noneOf(NpcTypeProperty.class);
    private final Set<NpcTypeProperty> propertyImports = EnumSet.noneOf(NpcTypeProperty.class);
    private final Set<EquipmentImport> equipmentImports = EnumSet.noneOf(EquipmentImport.class);

    /** How the NPC reacts to being knocked back. */
    public enum Knockback
    {
        /** Immune while the NPC looks like an AA gun, mecha, plane or vehicle. */
        MODEL,
        /** Never knocked back. */
        IMMUNE,
        /** Knocked back like any Custom NPC. */
        NORMAL
    }

    /** Equipment statistics that can be copied once into the Custom NPCs editor fields. */
    public enum EquipmentImport
    {
        MELEE, RANGED, ARMOR
    }

    public void setKnockback(Knockback mode)
    {
        knockback = mode == null ? Knockback.MODEL : mode;
    }

    /** @return the damage below which a hit of this kind is ignored; 0 accepts every hit */
    public int minimumHit(NpcDamageCategory category)
    {
        return minimumHits.getOrDefault(category, 0);
    }

    public void setMinimumHit(NpcDamageCategory category, int damage)
    {
        int clamped = Math.max(0, Math.min(MAX_MINIMUM_HIT, damage));
        if (clamped == 0)
            minimumHits.remove(category);
        else
            minimumHits.put(category, clamped);
    }

    /** @return whether the NPC keeps its own saved value instead of the selected model's */
    public boolean isCustom(NpcTypeProperty property)
    {
        return custom.contains(property);
    }

    public void setCustom(NpcTypeProperty property, boolean value)
    {
        if (value)
            custom.add(property);
        else
        {
            custom.remove(property);
            propertyImports.remove(property);
        }
    }

    /** Asks the server to copy the model's value into the NPC's saved value, which then stays the NPC's own. */
    public void requestImport(NpcTypeProperty property)
    {
        custom.add(property);
        propertyImports.add(property);
    }

    public void requestImport(EquipmentImport kind)
    {
        equipmentImports.add(kind);
    }

    public Set<NpcTypeProperty> customProperties()
    {
        return Collections.unmodifiableSet(custom);
    }

    public boolean importRequested(NpcTypeProperty property)
    {
        return propertyImports.contains(property);
    }

    public boolean importRequested(EquipmentImport kind)
    {
        return equipmentImports.contains(kind);
    }

    public boolean hasRequests()
    {
        return !propertyImports.isEmpty() || !equipmentImports.isEmpty();
    }

    /** @return the requested property imports, removed from this object */
    public Set<NpcTypeProperty> takePropertyImports()
    {
        Set<NpcTypeProperty> taken = propertyImports.isEmpty() ? EnumSet.noneOf(NpcTypeProperty.class) : EnumSet.copyOf(propertyImports);
        propertyImports.clear();
        return taken;
    }

    /** @return the requested equipment imports, removed from this object */
    public Set<EquipmentImport> takeEquipmentImports()
    {
        Set<EquipmentImport> taken = equipmentImports.isEmpty() ? EnumSet.noneOf(EquipmentImport.class) : EnumSet.copyOf(equipmentImports);
        equipmentImports.clear();
        return taken;
    }

    /** Forgets requests the server has been sent, so that saving the editor again does not repeat them. */
    public void clearRequests()
    {
        propertyImports.clear();
        equipmentImports.clear();
    }

    /** Identifies the settings that change which model properties are inherited. */
    public int customSignature()
    {
        int signature = 0;
        for (NpcTypeProperty property : custom)
            signature |= 1 << property.ordinal();
        return signature;
    }

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        knockback = named(Knockback.class, tag.getString(KNOCKBACK_KEY)).orElse(Knockback.MODEL);
        minimumHits.clear();
        CompoundTag hits = tag.getCompound(MINIMUM_HITS_KEY);
        for (NpcDamageCategory category : NpcDamageCategory.values())
            if (hits.contains(category.name(), Tag.TAG_ANY_NUMERIC))
                setMinimumHit(category, hits.getInt(category.name()));
        read(tag, CUSTOM_KEY, NpcTypeProperty.class, custom);
        read(tag, PROPERTY_IMPORTS_KEY, NpcTypeProperty.class, propertyImports);
        read(tag, EQUIPMENT_IMPORTS_KEY, EquipmentImport.class, equipmentImports);
        // A property can only be imported into the NPC's own value.
        custom.addAll(propertyImports);
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(KNOCKBACK_KEY, knockback.name());
        CompoundTag hits = new CompoundTag();
        minimumHits.forEach((category, damage) -> hits.putInt(category.name(), damage));
        tag.put(MINIMUM_HITS_KEY, hits);
        tag.put(CUSTOM_KEY, names(custom));
        tag.put(PROPERTY_IMPORTS_KEY, names(propertyImports));
        tag.put(EQUIPMENT_IMPORTS_KEY, names(equipmentImports));
        root.put(NBT_KEY, tag);
    }

    private static <E extends Enum<E>> void read(CompoundTag tag, String key, Class<E> type, Set<E> target)
    {
        target.clear();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++)
            named(type, list.getString(i)).ifPresent(target::add);
    }

    private static ListTag names(Set<? extends Enum<?>> values)
    {
        ListTag list = new ListTag();
        for (Enum<?> value : values)
            list.add(StringTag.valueOf(value.name()));
        return list;
    }

    private static <E extends Enum<E>> Optional<E> named(Class<E> type, String name)
    {
        for (E value : type.getEnumConstants())
            if (value.name().equals(name))
                return Optional.of(value);
        return Optional.empty();
    }
}
