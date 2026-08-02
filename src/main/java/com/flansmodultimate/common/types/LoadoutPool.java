package com.flansmodultimate.common.types;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.common.teams.PlayerLoadout;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.*;

/** Content-pack definition for the ranked Teams progression and loadout system. */
@NoArgsConstructor
public final class LoadoutPool extends InfoType
{
    public static final int LOADOUT_COUNT = 5;
    private static final Map<String, LoadoutPool> POOLS = new LinkedHashMap<>();

    public record ExtraItem(String itemId, int count) {}
    public record LoadoutEntry(String typeId, int unlockRank, List<ExtraItem> extraItems) {}

    @Getter
    private int maxLevel = 20;
    @Getter
    private int experienceForKill = 10;
    @Getter
    private int experienceForDeath = 5;
    @Getter
    private int experienceForKillstreakBonus = 10;
    private int[] experiencePerLevel = new int[0];
    private final int[] loadoutUnlockLevels = { 0, 0, 5, 10, 20 };
    private final Map<LoadoutSlot, List<LoadoutEntry>> entries = new EnumMap<>(LoadoutSlot.class);
    private final List<PlayerLoadout> defaults = new ArrayList<>(LOADOUT_COUNT);
    @Getter
    private List<String> rewardBoxIds = List.of();
    private final Map<Integer, List<String>> rewardsPerLevel = new LinkedHashMap<>();

    @Override
    public void load(TypeFile file)
    {
        for (LoadoutSlot slot : LoadoutSlot.values()) entries.put(slot, new ArrayList<>());
        for (int i = 0; i < LOADOUT_COUNT; i++) defaults.add(new PlayerLoadout());
        super.load(file);
        if (StringUtils.isNotBlank(originalShortName)) POOLS.put(normalize(originalShortName), this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        maxLevel = Math.max(1, readValue("MaxLevel", maxLevel, file));
        experienceForKill = Math.max(0, readValue("XPForKill", experienceForKill, file));
        experienceForDeath = Math.max(0, readValue("XPForDeath", experienceForDeath, file));
        experienceForKillstreakBonus = Math.max(0, readValue("XPForKillstreakBonus", experienceForKillstreakBonus, file));

        String[] xp = readValues("XPPerLevel", file);
        experiencePerLevel = new int[maxLevel];
        for (int i = 0; i < maxLevel; i++)
            experiencePerLevel[i] = i < xp.length ? parseInt(xp[i], Math.max(30, (i + 1) * 30)) : Math.max(30, (i + 1) * 30);

        String[] levels = readValues("SlotUnlockLevels", file);
        for (int i = 0; i < Math.min(levels.length, loadoutUnlockLevels.length); i++)
            loadoutUnlockLevels[i] = Math.max(0, parseInt(levels[i], loadoutUnlockLevels[i]));

        readEntries(file, LoadoutSlot.PRIMARY, "AddPrimary");
        readEntries(file, LoadoutSlot.SECONDARY, "AddSecondary");
        readEntries(file, LoadoutSlot.SPECIAL, "AddSpecial");
        readEntries(file, LoadoutSlot.MELEE, "AddMelee");
        readEntries(file, LoadoutSlot.ARMOUR, "AddArmour");

        for (String[] values : readValuesInLines("DefaultLoadout", file, 2).orElse(List.of()))
        {
            int index = parseInt(values[0], 0) - 1;
            if (index < 0 || index >= defaults.size()) continue;
            for (int slot = 0; slot < LoadoutSlot.values().length && slot + 1 < values.length; slot++)
            {
                ItemStack stack = createStack(values[slot + 1]).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) defaults.get(index).set(LoadoutSlot.values()[slot], stack);
            }
        }

        rewardBoxIds = readValuesInLines("AddRewardBox", file, 1).orElse(List.of()).stream()
            .map(values -> values[0]).distinct().limit(3).toList();
        for (String[] values : readValuesInLines("AddReward", file, 2).orElse(List.of()))
        {
            int rank = parseInt(values[1], -1);
            if (rank > 0) rewardsPerLevel.computeIfAbsent(rank, ignored -> new ArrayList<>()).add(values[0]);
        }
    }

    private void readEntries(TypeFile file, LoadoutSlot slot, String key)
    {
        for (String[] values : readValuesInLines(key, file, 2).orElse(List.of()))
        {
            List<ExtraItem> extras = new ArrayList<>();
            for (int i = 2; i + 1 < values.length; i += 2)
                extras.add(new ExtraItem(values[i], Math.max(1, parseInt(values[i + 1], 1))));
            entries.get(slot).add(new LoadoutEntry(values[0], Math.max(0, parseInt(values[1], 0)), List.copyOf(extras)));
        }
    }

    public List<LoadoutEntry> getEntries(LoadoutSlot slot)
    {
        return Collections.unmodifiableList(entries.get(slot));
    }

    public int getLoadoutUnlockLevel(int index)
    {
        return loadoutUnlockLevels[Math.max(0, Math.min(LOADOUT_COUNT - 1, index))];
    }

    public PlayerLoadout getDefaultLoadout(int index)
    {
        return defaults.get(Math.max(0, Math.min(index, defaults.size() - 1))).copy();
    }

    public List<String> getRewardsForRank(int rank)
    {
        return List.copyOf(rewardsPerLevel.getOrDefault(rank, List.of()));
    }

    /** XP needed to advance from the supplied one-based rank. */
    public int getExperienceForRank(int rank)
    {
        if (rank >= maxLevel) return Integer.MAX_VALUE;
        // Legacy ranked data started at level 0; PlayerStats is one-based, so advancing
        // from rank 1 uses the second XPPerLevel value (the old level 1 -> 2 boundary).
        return experiencePerLevel[Math.max(0, Math.min(rank, experiencePerLevel.length - 1))];
    }

    @Nullable
    public LoadoutEntry findEntry(LoadoutSlot slot, InfoType target)
    {
        if (target == null) return null;
        for (LoadoutEntry entry : entries.get(slot))
            if (InfoType.getInfoType(entry.typeId(), contentPack) == target) return entry;
        return null;
    }

    public boolean isEntryUnlocked(LoadoutSlot slot, InfoType target, int rank)
    {
        LoadoutEntry entry = findEntry(slot, target);
        return entry != null && entry.unlockRank() <= rank;
    }

    public List<ItemStack> createExtraItems(LoadoutSlot slot, InfoType target)
    {
        LoadoutEntry entry = findEntry(slot, target);
        if (entry == null) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (ExtraItem extra : entry.extraItems())
            createStack(extra.itemId()).ifPresent(stack -> { stack.setCount(extra.count()); result.add(stack); });
        return result;
    }

    private java.util.Optional<ItemStack> createStack(String id)
    {
        if (StringUtils.isBlank(id) || "none".equalsIgnoreCase(id)) return java.util.Optional.empty();
        InfoType type = InfoType.getInfoType(id, contentPack);
        return type == null ? ModUtils.getItemStack(id, 1, 0) : java.util.Optional.of(createEntryStack(type));
    }

    public ItemStack createEntryStack(InfoType type)
    {
        ItemStack stack = ModUtils.getItemStack(type).orElse(ItemStack.EMPTY);
        if (stack.getItem() instanceof GunItem gunItem)
        {
            gunItem.getConfigType().checkForTags(stack);
            gunItem.getConfigType().getDefaultAmmo().flatMap(ModUtils::getItemStack)
                .ifPresent(ammo -> gunItem.setBulletItemStack(stack, ammo, 0));
        }
        return stack;
    }

    public boolean validate(PlayerLoadout loadout, int rank, java.util.function.Predicate<String> ownsPaint)
    {
        for (LoadoutSlot slot : LoadoutSlot.values())
        {
            ItemStack stack = loadout.get(slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof IFlanItem<?> item) || !isEntryUnlocked(slot, item.getConfigType(), rank)) return false;
            if (item.getConfigType() instanceof GunType gun)
                for (AttachmentType attachment : gun.getCurrentAttachments(stack))
                    if (!isEntryUnlocked(slot, attachment, rank)) return false;
            if (item.getConfigType() instanceof PaintableType paintable)
            {
                var paintjob = paintable.getPaintjob(stack);
                String key = RewardBox.rewardKey(paintable, paintjob);
                if (!paintjob.isDefault() && !ownsPaint.test(key)) return false;
            }
        }
        return true;
    }

    public static Collection<LoadoutPool> values() { return Collections.unmodifiableCollection(POOLS.values()); }
    @Nullable public static LoadoutPool get(@Nullable String id) { return StringUtils.isBlank(id) ? null : POOLS.get(normalize(id)); }
    private static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static int parseInt(String value, int fallback)
    {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
