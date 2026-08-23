package com.flansmodultimate.common.types;

import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.*;

/**
 * A content-pack-defined team. Runtime membership and scores deliberately live in
 * {@code TeamsManager}; an InfoType is shared content data and must stay immutable
 * after loading.
 */
@NoArgsConstructor
public class Team extends InfoType
{
    public static final String SPECTATORS_ID = "spectators";
    public static final Team SPECTATORS = new Team(SPECTATORS_ID, "Spectators", 0x404040, ChatFormatting.GRAY);

    private static final Map<String, Team> TEAMS = new LinkedHashMap<>();

    @Getter
    private int teamColour = 0xFFFFFF;
    @Getter
    private ChatFormatting textColour = ChatFormatting.WHITE;
    @Getter
    private boolean allowedForRoundsGenerator;
    private final Map<EquipmentSlot, String> armour = new LinkedHashMap<>();
    private List<String> classIds = List.of();

    private Team(String shortName, String displayName, int colour, ChatFormatting formatting)
    {
        originalShortName = shortName;
        name = displayName;
        teamColour = colour;
        textColour = formatting;
    }

    @Override
    public void load(TypeFile file)
    {
        super.load(file);
        if (StringUtils.isNotBlank(originalShortName))
            TEAMS.put(normalize(originalShortName), this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        readIntValues("TeamColour", file, 3).ifPresent(values ->
            teamColour = (values[0] << 16) | (values[1] << 8) | values[2]);
        textColour = parseFormatting(readValue("TextColour", textColour.name(), file));
        allowedForRoundsGenerator = readValue("AllowedForRoundsGenerator", allowedForRoundsGenerator, file);

        readArmour(file, EquipmentSlot.HEAD, "Hat", "Helmet");
        readArmour(file, EquipmentSlot.CHEST, "Chest", "Top");
        readArmour(file, EquipmentSlot.LEGS, "Legs", "Bottom");
        readArmour(file, EquipmentSlot.FEET, "Shoes", "Boots");

        classIds = readValuesInLines("AddDefaultClass", file, 1).orElse(List.of()).stream()
            .map(values -> values[0]).toList();
        List<String> added = readValuesInLines("AddClass", file, 1).orElse(List.of()).stream()
            .map(values -> values[0]).toList();
        if (!added.isEmpty())
        {
            java.util.ArrayList<String> all = new java.util.ArrayList<>(classIds);
            all.addAll(added);
            classIds = List.copyOf(all);
        }
    }

    private void readArmour(TypeFile file, EquipmentSlot slot, String primary, String alias)
    {
        String value = readValue(primary, null, file);
        value = readValue(alias, value, file);
        if (StringUtils.isNotBlank(value) && !"none".equalsIgnoreCase(value))
            armour.put(slot, value);
    }

    public Component getDisplayComponent()
    {
        return Component.literal(name).withStyle(textColour);
    }

    public List<PlayerClass> getClasses()
    {
        return classIds.stream().map(PlayerClass::getPlayerClass).filter(java.util.Objects::nonNull).toList();
    }

    public ItemStack getArmour(EquipmentSlot slot)
    {
        String id = armour.get(slot);
        if (id == null)
            return ItemStack.EMPTY;
        InfoType type = InfoType.getInfoType(id, contentPack);
        return ModUtils.getItemStack(type).orElse(ItemStack.EMPTY);
    }

    public static Collection<Team> values()
    {
        return Collections.unmodifiableCollection(TEAMS.values());
    }

    @Nullable
    public static Team getTeam(@Nullable String id)
    {
        if (StringUtils.isBlank(id))
            return null;
        if (SPECTATORS_ID.equalsIgnoreCase(id))
            return SPECTATORS;
        return TEAMS.get(normalize(id));
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static ChatFormatting parseFormatting(@Nullable String value)
    {
        if (StringUtils.isBlank(value))
            return ChatFormatting.WHITE;
        if ("orange".equalsIgnoreCase(value))
            return ChatFormatting.GOLD;
        String normalized = value.replace("L", "LIGHT_").replace("Grey", "Gray").toUpperCase(Locale.ROOT);
        try
        {
            ChatFormatting formatting = ChatFormatting.valueOf(normalized);
            return switch (formatting)
            {
                case OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, RESET -> ChatFormatting.WHITE;
                default -> formatting;
            };
        }
        catch (IllegalArgumentException ignored)
        {
            return ChatFormatting.WHITE;
        }
    }
}
