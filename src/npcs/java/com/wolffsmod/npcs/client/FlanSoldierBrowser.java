package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentPack;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.soldier.SoldierCloneNames;
import noppes.npcs.CustomEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * The "Flan's Soldier" folder of the Custom NPCs model picker: content packs, their teams, then a team's classes.
 * Choosing a class gives the NPC the human model and, unless the builder turned the class skin off, the class skin, and
 * remembers the choice. A toggle applies the class loadout; the naming controls generate a name on explicit request.
 */
@SuppressWarnings("unused") // Reached only from the model picker mixin, which IntelliJ does not follow.
public final class FlanSoldierBrowser
{
    public static final String LABEL = "Flan's Soldier";
    private static final int LOADOUT_TOGGLE_ID = 9120;
    private static final int SKIN_TOGGLE_ID = 9121;
    private static final int NAME_APPLY_ID = 9123;
    private static final int FACTION_APPLY_ID = 9124;
    private static final int SKIN_WIDTH = 64;
    /** Whether choosing a class also gives the NPC its loadout; the builder's choice lasts for the session. */
    private static boolean applyClassLoadout = true;
    /** Whether choosing a class or applying its loadout also gives the NPC the class skin; the builder's choice lasts for the session. */
    private static boolean applyClassSkin = true;

    @Nullable
    private IContentPack pack;
    @Nullable
    private ITeam team;
    private List<IContentPack> packs = List.of();
    private List<ITeam> teams = List.of();
    private List<? extends IPlayerClass> classes = List.of();
    @Nullable
    private IPlayerClass variantClass;
    private List<SkinOverrideVariant> skins = List.of();
    private boolean unassigned;

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
        if (variantClass != null)
            return skins.stream().map(SkinOverrideVariant::name).toList();
        if (unassigned)
        {
            classes = FlansModApi.getPlayerClasses().stream().filter(candidate -> Objects.equals(candidate.getContentPack(), pack))
                .filter(candidate -> FlansModApi.getTeams().stream().noneMatch(owner -> owner.getClasses().contains(candidate)))
                .sorted(Comparator.comparing(IPlayerClass::getName, String.CASE_INSENSITIVE_ORDER)).toList();
            return classes.stream().map(type -> type.getNameComponent().getString()).toList();
        }
        if (team != null)
        {
            classes = team.getClasses();
            return classes.stream().map(type -> type.getNameComponent().getString()).toList();
        }
        if (pack != null)
        {
            teams = FlansModApi.getTeams().stream().filter(candidate -> Objects.equals(candidate.getContentPack(), pack) && !candidate.getClasses().isEmpty())
                .sorted(Comparator.comparing(ITeam::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ITeam::getShortName)).toList();
            List<String> labels = new ArrayList<>(teams.stream().map(type -> type.getNameComponent().getString()).toList());
            if (hasUnassigned(pack))
                labels.add(net.minecraft.network.chat.Component.translatable("wolffsmodnpcs.soldier.unassigned_classes").getString());
            return List.copyOf(labels);
        }
        packs = java.util.stream.Stream
            .concat(FlansModApi.getTeams().stream().filter(candidate -> !candidate.getClasses().isEmpty()).map(IContentType::getContentPack),
                FlansModApi.getPlayerClasses().stream().map(IContentType::getContentPack))
            .filter(Objects::nonNull).distinct().sorted(Comparator.comparing(IContentPack::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(candidate -> candidate.getPath().toString())).toList();
        return packs.stream().map(IContentPack::getName).toList();
    }

    /** @return whether the browser is still inside the folder; false when the back row left it */
    public boolean back()
    {
        if (variantClass != null)
            variantClass = null;
        else if (unassigned)
            unassigned = false;
        else if (team != null)
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
        if (row < 0)
            return Optional.empty();
        if (variantClass != null)
            return row < skins.size() ? Optional.of(new FlanSoldierSelection(team, variantClass, row)) : Optional.empty();
        if (team != null || unassigned)
        {
            if (row >= classes.size())
                return Optional.empty();
            IPlayerClass selected = classes.get(row);
            if (selected.getSkinOverrideVariants().size() > 1)
            {
                variantClass = selected;
                skins = selected.getSkinOverrideVariants();
            }
            return Optional.of(new FlanSoldierSelection(team, selected));
        }
        if (pack != null)
        {
            if (row < teams.size())
                team = teams.get(row);
            else if (row == teams.size() && hasUnassigned(pack))
                unassigned = true;
        }
        else if (row < packs.size())
            pack = packs.get(row);
        return Optional.empty();
    }

    /** @return the row of the selected class at the level listing it, or -1 */
    public int selectedRow(@Nullable FlanSoldierSelection selection)
    {
        if ((!unassigned && team == null) || selection == null || selection.team() != team)
            return -1;
        if (variantClass == null)
            return classes.indexOf(selection.playerClass());
        return selection.playerClass() == variantClass ? selection.skin() : -1;
    }

    /** Whether this level lists the skins of one class, as the vehicle picker lists paintjobs. */
    public boolean browsingSkins()
    {
        return variantClass != null;
    }

    private static boolean hasUnassigned(IContentPack pack)
    {
        return FlansModApi.getPlayerClasses().stream().filter(candidate -> Objects.equals(candidate.getContentPack(), pack))
            .anyMatch(candidate -> FlansModApi.getTeams().stream().noneMatch(owner -> owner.getClasses().contains(candidate)));
    }

    /**
     * Gives the NPC the looks of a class. A class without a usable skin, or any class while the class skin is turned off,
     * keeps the current skin, and with it the human model that skin was laid out for.
     */
    public static void select(GuiCreationScreenInterface screen, FlanSoldierSelection selection)
    {
        List<SkinOverrideVariant> variants = selection.playerClass().getSkinOverrideVariants();
        ResourceLocation skin = applyClassSkin && selection.skin() < variants.size() ? variants.get(selection.skin()).texture() : null;
        SkinLayout layout = layout(skin);
        ResourceLocation legacyModel = BuiltInRegistries.ENTITY_TYPE.getKey(CustomEntities.entityNPC64x32);
        boolean legacy = layout == SkinLayout.LEGACY || layout == SkinLayout.NONE && Objects.equals(screen.playerdata.getEntityName(), legacyModel);
        // Selecting a model also empties its extra data, so the choice is recorded afterwards.
        boolean regenerate = screen.playerdata.extra.getBoolean(SoldierCloneNames.REGENERATE);
        int gender = SoldierCloneNames.selectedGender(screen.playerdata.extra);
        screen.playerdata.setEntity(legacy ? legacyModel : null);
        screen.playerdata.extra.putBoolean(SoldierCloneNames.REGENERATE, regenerate);
        screen.playerdata.extra.putInt(SoldierCloneNames.GENDER, gender);
        selection.write(screen.playerdata.extra);
        if (skin != null && layout != SkinLayout.NONE)
            screen.npc.display.setSkinTexture(skin.toString());
    }

    /**
     * Gives the NPC the looks of a chosen class and optionally its loadout. Naming requires the Apply button.
     */
    public static void choose(GuiCreationScreenInterface screen, FlanSoldierSelection selection)
    {
        select(screen, selection);
        if (applyClassLoadout)
            screen.save();
        if (applyClassLoadout)
            NpcNetwork.sendToServer(ApplyLoadoutPacket.of(screen.npc.getId(), selection));
    }

    /**
     * Adds loadout and skin toggles, plus the NPC naming menu.
     *
     * @param browsing
     *            whether the picker is inside the soldier folder
     */
    public static void addLoadoutControls(GuiCreationScreenInterface screen, boolean browsing)
    {
        Optional<FlanSoldierSelection> selection = FlanSoldierSelection.read(screen.playerdata.extra);
        if (!browsing && selection.isEmpty())
            return;
        screen.addLabel(new GuiLabel(LOADOUT_TOGGLE_ID, "wolffsmodnpcs.soldier.apply_loadout", screen.guiLeft + 124, screen.guiTop + 28, 0xFF0000));
        screen.addButton(new GuiButtonYesNo(screen, LOADOUT_TOGGLE_ID, screen.guiLeft + 260, screen.guiTop + 23, applyClassLoadout,
            button -> setApplyClassLoadout(screen, selection.orElse(null), ((GuiButtonYesNo) button).getBoolean())));
        screen.addLabel(new GuiLabel(SKIN_TOGGLE_ID, "wolffsmodnpcs.soldier.apply_skin", screen.guiLeft + 124, screen.guiTop + 51, 0xFF0000));
        screen.addButton(new GuiButtonYesNo(screen, SKIN_TOGGLE_ID, screen.guiLeft + 260, screen.guiTop + 46, applyClassSkin, button -> setApplyClassSkin(((GuiButtonYesNo) button).getBoolean())));
        GuiButtonNop naming = new GuiButtonNop(screen, NAME_APPLY_ID, screen.guiLeft + 292, screen.guiTop + 69, 96, 20, "wolffsmodnpcs.soldier.name.menu",
            button -> selection.ifPresent(chosen -> screen.setSubGui(new SoldierNameMenu(screen, chosen))));
        naming.setEnabled(selection.isPresent());
        screen.addButton(naming);
        GuiButtonNop faction = new GuiButtonNop(screen, FACTION_APPLY_ID, screen.guiLeft + 292, screen.guiTop + 92, 96, 20, "wolffsmodnpcs.team_faction.apply",
            button -> selection.filter(chosen -> chosen.team() != null).ifPresent(chosen ->
            {
                screen.save();
                NpcNetwork.sendToServer(new TeamFactionPacket(screen.npc.getId(), chosen.team().getShortName(), chosen.playerClass().getShortName(), true));
            }));
        faction.setEnabled(selection.filter(chosen -> chosen.team() != null).isPresent());
        screen.addButton(faction);
    }

    private static void setApplyClassSkin(boolean apply)
    {
        applyClassSkin = apply;
    }

    /** Turning the toggle on also equips the NPC with the class already chosen. */
    private static void setApplyClassLoadout(GuiCreationScreenInterface screen, @Nullable FlanSoldierSelection selection, boolean apply)
    {
        applyClassLoadout = apply;
        if (apply && selection != null)
        {
            applyLoadout(screen, selection);
            screen.init();
        }
    }

    /** The class skin is applied again with the loadout, in case the skin was changed since the class was chosen. */
    private static void applyLoadout(GuiCreationScreenInterface screen, FlanSoldierSelection selection)
    {
        select(screen, selection);
        // The server takes the skin and its model from the screen's own save, then equips the NPC.
        screen.save();
        NpcNetwork.sendToServer(ApplyLoadoutPacket.of(screen.npc.getId(), selection));
    }

    /** The layout of a skin image, read from the client's resources. */
    static SkinLayout layout(@Nullable ResourceLocation skin)
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
