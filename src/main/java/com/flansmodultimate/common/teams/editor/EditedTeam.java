package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.common.teams.TeamFlagUrl;
import com.flansmodultimate.common.types.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Effective team whose authored fields override a pack's defaults. */
public final class EditedTeam extends Team
{
    private static final String VICTORY_FLAG = "victoryFlag";

    @Nullable
    private final Team original;
    private final JsonObject patch;

    public EditedTeam(String id, @Nullable Team original, JsonObject patch)
    {
        this.original = original;
        this.patch = patch.deepCopy();
        uniqueShortName = id;
        originalShortName = original == null ? id : original.getOriginalShortName();
        contentPack = original == null ? EditorCatalog.CUSTOM_PACK : original.getContentPack();
        fileName = original == null ? id : original.getFileName();
        type = EnumType.TEAM;
        name = text("name", original == null ? id : original.getName());
    }

    @Override
    public String getNameTranslationKey()
    {
        return original == null || !name.equals(original.getName()) ? "" : original.getNameTranslationKey();
    }

    private String text(String field, String fallback)
    {
        return patch.has(field) ? patch.get(field).getAsString() : fallback;
    }

    @Override
    public int getTeamColour()
    {
        int fallback = original == null ? 0xFFFFFF : original.getTeamColour();
        return patch.has("colour") ? Integer.parseInt(patch.get("colour").getAsString().substring(1), 16) : fallback;
    }

    @Override
    public ChatFormatting getTextColour()
    {
        ChatFormatting fallback = original == null ? ChatFormatting.WHITE : original.getTextColour();
        return patch.has("textColour") ? ChatFormatting.getByName(patch.get("textColour").getAsString()) : fallback;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayComponent()
    {
        return getNameComponent().withStyle(getTextColour());
    }

    @Override
    public boolean isAllowedForRoundsGenerator()
    {
        return patch.has("roundsGenerator") ? patch.get("roundsGenerator").getAsBoolean() : original != null && original.isAllowedForRoundsGenerator();
    }

    @Override
    public boolean isVictorySoundEnabled()
    {
        return patch.has("victorySoundEnabled") ? patch.get("victorySoundEnabled").getAsBoolean() : original == null || original.isVictorySoundEnabled();
    }

    @Override
    public String getVictorySound()
    {
        return text("victorySound", original == null ? DEFAULT_VICTORY_SOUND : original.getVictorySound());
    }

    @Override
    public String getDefeatSound()
    {
        return text("defeatSound", original == null ? DEFAULT_DEFEAT_SOUND : original.getDefeatSound());
    }

    @Override
    public String getVictoryFlagTexture()
    {
        return text(VICTORY_FLAG, original == null ? "" : original.getVictoryFlagTexture());
    }

    @Override
    @Nullable
    public ResourceLocation getVictoryFlagTextureLocation()
    {
        if (!patch.has(VICTORY_FLAG))
            return original == null ? null : original.getVictoryFlagTextureLocation();
        String flag = patch.get(VICTORY_FLAG).getAsString();
        return flag.isBlank() || TeamFlagUrl.isUrl(flag) ? null : ResourceLocation.tryParse(flag);
    }

    @Override
    @Nullable
    public String getVictoryFlagUrl()
    {
        if (!patch.has(VICTORY_FLAG))
            return original == null ? null : original.getVictoryFlagUrl();
        String flag = patch.get(VICTORY_FLAG).getAsString();
        return TeamFlagUrl.isUrl(flag) ? flag.trim() : null;
    }

    @Override
    public ItemStack getArmour(EquipmentSlot slot)
    {
        return EditorStacks.armour(patch, slot, () -> original == null ? ItemStack.EMPTY : original.getArmour(slot));
    }

    @Override
    public List<PlayerClass> getClasses()
    {
        if (!patch.has("classes"))
            return original == null ? List.of() : original.getClasses();
        List<PlayerClass> classes = new ArrayList<>();
        for (JsonElement id : patch.getAsJsonArray("classes"))
        {
            PlayerClass playerClass = EditorCatalog.resolveClass(id.getAsString());
            if (playerClass != null && !classes.contains(playerClass))
                classes.add(playerClass);
        }
        return List.copyOf(classes);
    }
}
