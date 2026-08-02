package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import static com.flansmodultimate.util.TypeReaderUtils.readValuesInLines;

@NoArgsConstructor
public final class RewardBox extends InfoType
{
    public record Reward(String key, String typeId, String paintName, Paintjob.EnumPaintjobRarity rarity) {}
    private static final Map<String, RewardBox> BOXES = new LinkedHashMap<>();

    private final Map<Paintjob.EnumPaintjobRarity, Float> rarityWeights = new EnumMap<>(Paintjob.EnumPaintjobRarity.class);
    @Getter
    private List<Reward> rewards = List.of();

    @Override
    public void load(TypeFile file)
    {
        for (Paintjob.EnumPaintjobRarity rarity : Paintjob.EnumPaintjobRarity.values()) rarityWeights.put(rarity, 1F);
        super.load(file);
        if (StringUtils.isNotBlank(originalShortName)) BOXES.put(normalize(originalShortName), this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        for (String[] values : readValuesInLines("RarityWeight", file, 2).orElse(List.of()))
            rarityWeights.put(parseRarity(values[0]), Math.max(0F, parseFloat(values[1], 1F)));

        List<Reward> parsed = new ArrayList<>();
        for (String[] values : readValuesInLines("AddPaintjob", file, 3).orElse(List.of()))
        {
            Paintjob.EnumPaintjobRarity rarity = parseRarity(values[0]);
            InfoType rawType = InfoType.getInfoType(values[1], contentPack);
            if (!(rawType instanceof PaintableType paintable))
            {
                FlansMod.log.warn("Unknown paintable type '{}' in reward box {}", values[1], originalShortName);
                continue;
            }
            Paintjob paintjob = findPaintjob(paintable, values[2]);
            if (paintjob == null)
            {
                FlansMod.log.warn("Unknown paintjob '{}' for '{}' in reward box {}", values[2], values[1], originalShortName);
                continue;
            }
            paintjob.setRarity(rarity);
            parsed.add(new Reward(rewardKey(paintable, paintjob), paintable.getOriginalShortName(), values[2], rarity));
        }
        rewards = List.copyOf(parsed);
    }

    @Nullable
    public Reward choose(RandomSource random, Predicate<String> alreadyOwned)
    {
        List<Reward> candidates = rewards.stream().filter(reward -> !alreadyOwned.test(reward.key())).toList();
        if (candidates.isEmpty()) candidates = rewards;
        float total = 0F;
        for (Reward reward : candidates) total += rarityWeights.getOrDefault(reward.rarity(), 1F);
        if (total <= 0F) return null;
        float pick = random.nextFloat() * total;
        for (Reward reward : candidates)
        {
            pick -= rarityWeights.getOrDefault(reward.rarity(), 1F);
            if (pick <= 0F) return reward;
        }
        return candidates.get(candidates.size() - 1);
    }

    @Nullable
    public static Paintjob resolve(Reward reward)
    {
        InfoType type = InfoType.getInfoTypes().values().stream()
            .filter(candidate -> reward.typeId().equalsIgnoreCase(candidate.getOriginalShortName())).findFirst().orElse(null);
        return type instanceof PaintableType paintable ? findPaintjob(paintable, reward.paintName()) : null;
    }

    @Nullable
    public static Reward findReward(String key)
    {
        return BOXES.values().stream().flatMap(box -> box.rewards.stream()).filter(reward -> reward.key().equals(key)).findFirst().orElse(null);
    }

    @Nullable
    public static Paintjob findPaintjob(PaintableType type, String name)
    {
        for (Paintjob paintjob : type.getPaintjobs().values())
            if (name.equalsIgnoreCase(paintjob.getTextureName()) || name.equalsIgnoreCase(paintjob.getIcon())) return paintjob;
        return null;
    }

    public static String rewardKey(PaintableType type, Paintjob paintjob)
    {
        return normalize(type.getOriginalShortName()) + ":" + normalize(paintjob.getTextureName());
    }

    public static Collection<RewardBox> values()
    {
        return Collections.unmodifiableCollection(BOXES.values());
    }

    @Nullable
    public static RewardBox get(@Nullable String id)
    {
        return StringUtils.isBlank(id) ? null : BOXES.get(normalize(id));
    }

    private static String normalize(String value)
    {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }

    private static float parseFloat(String value, float fallback)
    {
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static Paintjob.EnumPaintjobRarity parseRarity(String value)
    {
        try { return Paintjob.EnumPaintjobRarity.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return Paintjob.EnumPaintjobRarity.UNKNOWN; }
    }
}
