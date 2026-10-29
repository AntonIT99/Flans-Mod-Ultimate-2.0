package com.flansmodultimate.common.types;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.common.teams.editor.EditorCatalog;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static com.flansmodultimate.util.TypeReaderUtils.*;

/**
 * A content-pack-defined team. Runtime membership and scores deliberately live in
 * {@code TeamsManager}; an InfoType is shared content data and must stay immutable
 * after loading.
 */
@NoArgsConstructor
public class Team extends InfoType implements ITeam
{
    public static final String SPECTATORS_ID = "spectators";
    public static final Team SPECTATORS = new Team(SPECTATORS_ID, "Spectators", 0x404040, ChatFormatting.GRAY);

    private static final ItemlessTypeRegistry<Team> TEAMS = new ItemlessTypeRegistry<>("team");
    /** Sound names of the Labjac fork; silent unless a content pack provides sounds of these names. */
    public static final String DEFAULT_VICTORY_SOUND = "victory";
    public static final String DEFAULT_DEFEAT_SOUND = "defeat";
    public static final String DRAW_SOUND = "draw";

    @Getter
    private int teamColour = 0xFFFFFF;
    @Getter
    private ChatFormatting textColour = ChatFormatting.WHITE;
    @Getter
    private boolean allowedForRoundsGenerator;
    /** Whether a round this team wins is announced with victory and defeat sounds at all. */
    @Getter
    private boolean victorySoundEnabled = true;
    /** Sound the members of this team hear when it wins. */
    @Getter
    private String victorySound = DEFAULT_VICTORY_SOUND;
    /** Sound the members of this team hear when it loses. */
    @Getter
    private String defeatSound = DEFAULT_DEFEAT_SOUND;
    /** Skin flown on the capture points this team holds and shown when it wins; blank for a plain flag in the team colour. */
    @Getter
    private String victoryFlagTexture = StringUtils.EMPTY;
    /** Client-side location of {@link #victoryFlagTexture}, or null when the team has none. */
    @Getter @Nullable
    private ResourceLocation victoryFlagTextureLocation;
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
        uniqueShortName = TEAMS.register(this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        readIntValues("TeamColour", file, 3).ifPresent(values -> teamColour = (values[0] << 16) | (values[1] << 8) | values[2]);
        textColour = parseFormatting(readValue("TextColour", textColour.getName(), file));
        allowedForRoundsGenerator = readValue("AllowedForRoundsGenerator", allowedForRoundsGenerator, file);
        victorySoundEnabled = readValue("HasVictorySound", victorySoundEnabled, file);
        victorySound = readSound("VictorySound", victorySound, file);
        defeatSound = readSound("DefeatSound", defeatSound, file);
        victoryFlagTexture = readResource("VictoryFlagTexture", victoryFlagTexture, file);

        readArmour(file, EquipmentSlot.HEAD, "Hat", "Helmet");
        readArmour(file, EquipmentSlot.CHEST, "Chest", "Top");
        readArmour(file, EquipmentSlot.LEGS, "Legs", "Bottom");
        readArmour(file, EquipmentSlot.FEET, "Shoes", "Boots");

        classIds = readValuesInLines("AddDefaultClass", file, 1).orElse(List.of()).stream().map(values -> values[0]).toList();
        List<String> added = readValuesInLines("AddClass", file, 1).orElse(List.of()).stream().map(values -> values[0]).toList();
        if (!added.isEmpty())
        {
            java.util.ArrayList<String> all = new java.util.ArrayList<>(classIds);
            all.addAll(added);
            classIds = List.copyOf(all);
        }
    }

    @Override
    protected void readClient(TypeFile file)
    {
        super.readClient(file);
        victoryFlagTextureLocation = StringUtils.isBlank(victoryFlagTexture) ? null : loadSkinTexture(victoryFlagTexture, this);
    }

    private void readArmour(TypeFile file, EquipmentSlot slot, String primary, String alias)
    {
        String value = readValue(primary, null, file);
        value = readValue(alias, value, file);
        if (StringUtils.isNotBlank(value) && !"none".equalsIgnoreCase(value))
            armour.put(slot, value);
    }

    /** Original content names keep translation keys independent of registry collision aliases. */
    @Override
    public String getNameTranslationKey()
    {
        return StringUtils.isBlank(originalShortName) ? StringUtils.EMPTY : "team.flansmod." + originalShortName.toLowerCase(Locale.ROOT);
    }

    public Component getDisplayComponent()
    {
        return getNameComponent().withStyle(textColour);
    }

    /** Web address of the flag image, which only the Teams editor sets; null for a flag of a texture. */
    @Nullable
    public String getVictoryFlagUrl()
    {
        return null;
    }

    /**
     * The classes this team's {@code AddClass} lines name, resolved against this team's own content
     * pack first so that a shortname another pack also uses cannot pull in the wrong class.
     */
    @Override
    public List<PlayerClass> getClasses()
    {
        return classIds.stream().map(id -> PlayerClass.getPlayerClass(id, contentPack)).filter(java.util.Objects::nonNull).toList();
    }

    @Override
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
        return EditorCatalog.current().teams(TEAMS.values());
    }

    public static Collection<Team> packValues()
    {
        return TEAMS.values();
    }

    /** Resolves a team by the unique name {@link #getShortName()} reports. */
    @Nullable
    public static Team getTeam(@Nullable String id)
    {
        if (StringUtils.isBlank(id))
            return null;
        if (SPECTATORS_ID.equalsIgnoreCase(id.trim()))
            return SPECTATORS;
        return EditorCatalog.current().team(id, TEAMS.get(id));
    }

    /** Resolves a team named inside {@code provider}, so a pack always sees its own teams first. */
    @Nullable
    public static Team getTeam(@Nullable String id, @Nullable IContentProvider provider)
    {
        if (StringUtils.isNotBlank(id) && SPECTATORS_ID.equalsIgnoreCase(id.trim()))
            return SPECTATORS;
        return EditorCatalog.current().team(id, TEAMS.get(id, provider));
    }

    private static ChatFormatting parseFormatting(@Nullable String value)
    {
        if (StringUtils.isBlank(value))
            return ChatFormatting.WHITE;
        if ("orange".equalsIgnoreCase(value))
            return ChatFormatting.GOLD;
        String normalized = value.replace("L", "LIGHT_").replace("Grey", "Gray").toUpperCase(Locale.ROOT);
        ChatFormatting formatting = ChatFormatting.getByName(normalized);
        return formatting != null && formatting.isColor() ? formatting : ChatFormatting.WHITE;
    }
}
