package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.network.ApplyLoadoutPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * The "Flan Soldier" folder of the Custom NPCs model picker: content packs, their teams, then a team's classes.
 * Choosing a class gives the NPC the human model and the class skin and remembers the choice; the NPC's equipment is
 * left alone until the builder presses the loadout button.
 */
@SuppressWarnings("unused") // Reached only from the model picker mixin, which IntelliJ does not follow.
public final class FlanSoldierBrowser
{
    public static final String LABEL = "Flan Soldier";
    private static final int LOADOUT_BUTTON_ID = 9120;
    private static final int SKIN_WIDTH = 64;

    @Nullable
    private IContentPack pack;
    @Nullable
    private ITeam team;
    private List<IContentPack> packs = List.of();
    private List<ITeam> teams = List.of();
    private List<? extends IPlayerClass> classes = List.of();

    /** How a class skin is laid out, which decides the human model it fits. */
    enum SkinLayout
    {
        /** A square sheet, as the default human model samples it. */
        MODERN,
        /** The old 2:1 sheet, which Custom NPCs' 64x32 human model samples. */
        LEGACY,
        /** No usable skin. */
        NONE
    }

    /** @return the rows of the current level, below the back row */
    public List<String> labels()
    {
        if (team != null)
        {
            classes = team.getClasses();
            return classes.stream().map(IContentType::getName).toList();
        }
        if (pack != null)
        {
            teams = FlansModApi.getTeams().stream().filter(candidate -> Objects.equals(candidate.getContentPack(), pack) && !candidate.getClasses().isEmpty())
                .sorted(Comparator.comparing(ITeam::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ITeam::getShortName)).toList();
            return teams.stream().map(IContentType::getName).toList();
        }
        packs = FlansModApi.getTeams().stream().filter(candidate -> !candidate.getClasses().isEmpty()).map(IContentType::getContentPack).filter(Objects::nonNull).distinct()
            .sorted(Comparator.comparing(IContentPack::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(candidate -> candidate.getPath().toString())).toList();
        return packs.stream().map(IContentPack::getName).toList();
    }

    /** @return whether the browser is still inside the folder; false when the back row left it */
    public boolean back()
    {
        if (team != null)
            team = null;
        else if (pack != null)
            pack = null;
        else
            return false;
        return true;
    }

    /**
     * Opens a row of the current level.
     *
     * @param row
     *            index below the back row
     * @return the class a class row stands for, or empty after entering a pack or a team
     */
    public Optional<FlanSoldierSelection> open(int row)
    {
        if (team != null)
            return row < classes.size() ? Optional.of(new FlanSoldierSelection(team, classes.get(row))) : Optional.empty();
        if (pack != null && row < teams.size())
            team = teams.get(row);
        else if (pack == null && row < packs.size())
            pack = packs.get(row);
        return Optional.empty();
    }

    /** @return the row of the selected class at the level listing it, or -1 */
    public int selectedRow(@Nullable FlanSoldierSelection selection)
    {
        return team == null || selection == null || selection.team() != team ? -1 : classes.indexOf(selection.playerClass());
    }

    /**
     * Gives the NPC the looks of a class. A class without a usable skin keeps the current one, and with it the human
     * model that skin was laid out for.
     */
    public static void select(GuiCreationScreenInterface screen, FlanSoldierSelection selection)
    {
        ResourceLocation skin = selection.playerClass().getSkinOverrideTexture();
        SkinLayout layout = layout(skin);
        ResourceLocation legacyModel = ForgeRegistries.ENTITY_TYPES.getKey(CustomEntities.entityNPC64x32);
        boolean legacy = layout == SkinLayout.LEGACY || layout == SkinLayout.NONE && Objects.equals(screen.playerdata.getEntityName(), legacyModel);
        // Selecting a model also empties its extra data, so the choice is recorded afterwards.
        screen.playerdata.setEntity(legacy ? legacyModel : null);
        selection.write(screen.playerdata.extra);
        if (skin != null && layout != SkinLayout.NONE)
            screen.npc.display.setSkinTexture(skin.toString());
    }

    /**
     * Adds the button that gives the NPC the selected class's loadout; without a selected class there is none. The class
     * skin is applied again with it, in case the skin was changed since the class was chosen.
     */
    public static void addLoadoutButton(GuiCreationScreenInterface screen)
    {
        Optional<FlanSoldierSelection> selection = FlanSoldierSelection.read(screen.playerdata.extra);
        if (selection.isEmpty())
            return;
        screen.addButton(
            new GuiButtonNop(screen, LOADOUT_BUTTON_ID, screen.guiLeft + 124, screen.guiTop + 23, 130, 20, "wolffsmodnpcs.soldier.apply_loadout", button -> applyLoadout(screen, selection.get())));
    }

    private static void applyLoadout(GuiCreationScreenInterface screen, FlanSoldierSelection selection)
    {
        select(screen, selection);
        // The server takes the skin and its model from the screen's own save, then equips the NPC.
        screen.save();
        NpcNetwork.sendToServer(ApplyLoadoutPacket.of(screen.npc.getId(), selection));
        screen.init();
    }

    private static SkinLayout layout(@Nullable ResourceLocation skin)
    {
        if (skin == null || skin.getPath().isEmpty())
            return SkinLayout.NONE;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(skin);
        if (resource.isEmpty())
            return SkinLayout.NONE;
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream))
        {
            return layout(image.getWidth(), image.getHeight());
        }
        catch (IOException | IllegalArgumentException exception)
        {
            NpcsMod.log.warn("Ignoring class skin {}: {}", skin, exception.getMessage());
            return SkinLayout.NONE;
        }
    }

    /** Player skins are sized in multiples of 64 pixels: square, or twice as wide as high in the old format. */
    static SkinLayout layout(int width, int height)
    {
        if (width < SKIN_WIDTH || width % SKIN_WIDTH != 0)
            return SkinLayout.NONE;
        if (height == width)
            return SkinLayout.MODERN;
        return height * 2 == width ? SkinLayout.LEGACY : SkinLayout.NONE;
    }
}
