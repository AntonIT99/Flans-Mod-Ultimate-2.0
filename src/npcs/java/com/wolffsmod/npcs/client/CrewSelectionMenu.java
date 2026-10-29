package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.SkinOverrideVariant;
import com.wolffsmod.npcs.crew.CrewSource;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.network.CrewClonePreviewPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneList;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Browses server clones or loaded teams and classes without changing the crew source until Done is pressed. */
final class CrewSelectionMenu extends GuiBasic implements IGuiData, ICustomScrollListener
{
    private static final String PREFIX = "wolffsmodnpcs.crew.select.";
    private static final int DONE = 0;
    private static final int CANCEL = 1;
    private static final int TAB = 2;
    private static final int SKIN = 3;
    private static final int LIST_TOP = 32;
    private static final int LIST_HEIGHT = 136;
    private static final int PREVIEW_LEFT = 280;
    private final int entityId;
    private final CrewSource original;
    private final List<ITeam> teams;
    private final GuiCustomScrollNop choices;
    private final GuiCustomScrollNop classes;
    private List<? extends IPlayerClass> playerClasses = List.of();
    private int cloneTab;
    private int teamIndex;
    private boolean loading;
    private boolean clonesRequested;
    private String chosenClass;
    private int chosenSkin;
    private CrewSource selected;
    @Nullable
    private EntityNPCInterface preview;
    @Nullable
    private UUID previewToken;

    CrewSelectionMenu(int entityId, CrewSource source)
    {
        this.entityId = entityId;
        original = source;
        chosenClass = source.playerClass();
        chosenSkin = source.skin();
        cloneTab = source.cloneTab();
        imageWidth = 400;
        imageHeight = 222;
        setBackground("menubg.png");
        choices = new GuiCustomScrollNop(this, 0);
        classes = new GuiCustomScrollNop(this, 1);
        teams = FlansModApi.getTeams().stream().filter(team -> !team.getClasses().isEmpty())
            .sorted(Comparator.comparing(ITeam::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ITeam::getShortName)).toList();
        teamIndex = -1;
        for (int i = 0; i < teams.size(); i++)
            if (teams.get(i).getShortName().equals(source.team()))
                teamIndex = i;
    }

    @Override
    public void init()
    {
        super.init();
        boolean clones = original.kind() == CrewSource.Kind.CLONE;
        addLabel(new GuiLabel(0, PREFIX + (clones ? "clone" : "class"), guiLeft + 8, guiTop + 8));
        choices.setSize(clones ? 264 : 130, LIST_HEIGHT);
        choices.guiLeft = guiLeft + 8;
        choices.guiTop = guiTop + LIST_TOP;
        addScroll(choices);
        if (clones)
        {
            String[] tabs = new String[CrewSource.MAX_CLONE_TAB];
            for (int i = 0; i < tabs.length; i++)
                tabs[i] = Component.translatable(PREFIX + "tab", i + 1).getString();
            addButton(new GuiButtonNop(this, TAB, guiLeft + 280, guiTop + 4, 112, 20, tabs, cloneTab - 1));
            if (!clonesRequested)
                requestClones();
        }
        else
        {
            List<String> labels = new ArrayList<>();
            labels.add(Component.translatable(PREFIX + "all_classes").getString());
            labels.addAll(teams.stream().map(team -> team.getNameComponent().getString() + " [" + team.getShortName() + "]").toList());
            choices.setUnsortedList(labels);
            choices.setSelectedIndex(teamIndex + 1);
            classes.setSize(130, LIST_HEIGHT);
            classes.guiLeft = guiLeft + 142;
            classes.guiTop = guiTop + LIST_TOP;
            addScroll(classes);
            addButton(new GuiButtonNop(this, SKIN, guiLeft + 8, guiTop + 172, 264, 20, ""));
            showClasses();
            showSkins();
        }
        addButton(new GuiButtonNop(this, DONE, guiLeft + 202, guiTop + 196, 92, 20, "gui.done"));
        addButton(new GuiButtonNop(this, CANCEL, guiLeft + 300, guiTop + 196, 92, 20, "gui.cancel"));
        updateButtons();
        if (!clones)
            updatePreview();
    }

    private void requestClones()
    {
        clonesRequested = true;
        loading = true;
        choices.clear();
        preview = null;
        previewToken = null;
        Packets.sendServer(new SPacketCloneList(cloneTab));
    }

    private void showClasses()
    {
        playerClasses = (teamIndex < 0 ? FlansModApi.getPlayerClasses() : teams.get(teamIndex).getClasses()).stream()
            .sorted(Comparator.comparing(IPlayerClass::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(IPlayerClass::getShortName)).toList();
        classes.clear();
        classes.setUnsortedList(playerClasses.stream().map(playerClass -> playerClass.getNameComponent().getString() + " [" + playerClass.getShortName() + "]").toList());
        for (int i = 0; i < playerClasses.size(); i++)
            if (playerClasses.get(i).getShortName().equals(chosenClass))
                classes.setSelectedIndex(i);
    }

    private void showSkins()
    {
        int index = classes.getSelectedIndex();
        List<String> labels = new ArrayList<>();
        labels.add(Component.translatable(PREFIX + "random_skin").getString());
        if (index >= 0 && index < playerClasses.size())
            labels.addAll(playerClasses.get(index).getSkinOverrideVariants().stream().map(SkinOverrideVariant::name).toList());
        if (labels.size() == 1)
            labels.add(Component.translatable(PREFIX + "default_skin").getString());
        if (chosenSkin >= labels.size() - 1)
            chosenSkin = CrewSource.RANDOM_SKIN;
        // addButton replaces the ID lookup but leaves the old widget in Screen's render and input lists.
        // Update the one button made by init, including when the selected class changes.
        GuiButtonNop skin = getButton(SKIN);
        Component name = Component.translatable(PREFIX + "skin", labels.get(chosenSkin + 1));
        skin.setMessage(name);
        skin.setTooltip(Tooltip.create(name.copy().append("\n").append(Component.translatable(PREFIX + "skin.help"))));
        skin.setEnabled(index >= 0 && index < playerClasses.size() && playerClasses.get(index).getSkinOverrideVariants().size() > 1);
    }

    private void updateButtons()
    {
        if (getButton(DONE) != null)
            getButton(DONE).setEnabled(original.kind() == CrewSource.Kind.CLONE ? !loading && choices.getSelected() != null : classes.getSelected() != null);
        if (getButton(TAB) != null)
            getButton(TAB).setEnabled(!loading);
        if (loading || choices.getList().isEmpty() || original.kind() == CrewSource.Kind.CLASS && playerClasses.isEmpty())
            addLabel(new GuiLabel(1, PREFIX + (loading ? "loading" : "empty"), guiLeft + 8, guiTop + 197));
        else
            addLabel(new GuiLabel(1, "", guiLeft + 8, guiTop + 197));
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        switch (button.id)
        {
            case CANCEL -> close();
            case TAB -> {
                if (!loading)
                {
                    cloneTab = button.getValue() + 1;
                    requestClones();
                    updateButtons();
                }
            }
            case SKIN -> cycleSkin();
            case DONE -> confirmSelection();
            default -> {
                // The scrolls have their own listeners.
            }
        }
    }

    private void cycleSkin()
    {
        int index = classes.getSelectedIndex();
        if (index < 0 || index >= playerClasses.size())
            return;
        int variants = playerClasses.get(index).getSkinOverrideVariants().size();
        chosenSkin = (chosenSkin + 2) % (variants + 1) - 1;
        showSkins();
        updatePreview();
    }

    private void confirmSelection()
    {
        if (original.kind() == CrewSource.Kind.CLONE)
        {
            if (loading || choices.getSelected() == null)
                return;
            selected = original.withClone(cloneTab, choices.getSelected());
        }
        else
        {
            int index = classes.getSelectedIndex();
            if (index < 0 || index >= playerClasses.size())
                return;
            selected = original.withClass(teamIndex < 0 ? "" : teams.get(teamIndex).getShortName(), playerClasses.get(index).getShortName()).withSkin(chosenSkin);
        }
        close();
    }

    @Override
    public void setGuiData(CompoundTag tag)
    {
        if (original.kind() != CrewSource.Kind.CLONE || !loading || !tag.contains("List"))
            return;
        ListTag list = tag.getList("List", 8);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
            names.add(list.getString(i));
        choices.setList(names);
        if (cloneTab == original.cloneTab())
            choices.setSelected(original.cloneName());
        loading = false;
        updateButtons();
        updatePreview();
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll)
    {
        if (original.kind() == CrewSource.Kind.CLASS && scroll == choices)
        {
            teamIndex = choices.getSelectedIndex() - 1;
            showClasses();
            showSkins();
        }
        else if (original.kind() == CrewSource.Kind.CLASS && scroll == classes)
        {
            int index = classes.getSelectedIndex();
            if (index >= 0 && index < playerClasses.size())
            {
                String name = playerClasses.get(index).getShortName();
                if (!name.equals(chosenClass))
                    chosenSkin = CrewSource.RANDOM_SKIN;
                chosenClass = name;
            }
            showSkins();
        }
        updateButtons();
        updatePreview();
    }

    /** Builds an isolated client entity. Browsing never equips or spawns the NPC being edited. */
    private void updatePreview()
    {
        preview = null;
        previewToken = null;
        if (minecraft == null || minecraft.level == null)
            return;
        if (original.kind() == CrewSource.Kind.CLONE)
        {
            String name = choices.getSelected();
            if (!loading && name != null)
            {
                previewToken = UUID.randomUUID();
                NpcNetwork.sendToServer(CrewClonePreviewPacket.request(entityId, cloneTab, name, previewToken));
            }
            return;
        }
        int index = classes.getSelectedIndex();
        if (index < 0 || index >= playerClasses.size())
            return;
        // Random previews the first variant, while the setting still randomizes each server-side spawn.
        FlanSoldierSelection selection = new FlanSoldierSelection(teamIndex < 0 ? null : teams.get(teamIndex), playerClasses.get(index), chosenSkin);
        EntityCustomNpc model = new EntityCustomNpc(CustomEntities.entityCustomNpc, minecraft.level);
        FlanSoldierLoadout.Equipment equipment = FlanSoldierLoadout.equipment(selection);
        model.inventory.armor = equipment.armour();
        model.inventory.weapons = equipment.weapons();
        selection.write(model.modelData.extra);
        NpcSkins.markClassSkin(model);
        NpcSkinResolver.apply(model);
        preview = model;
    }

    void receive(CrewClonePreviewPacket packet)
    {
        if (packet.request() || packet.entityId() != entityId || !packet.token().equals(previewToken) || packet.tab() != cloneTab || !packet.name().equals(choices.getSelected()) || minecraft == null
            || minecraft.level == null)
            return;
        previewToken = null;
        if (packet.data().isEmpty())
            return;
        preview = EntityType.create(packet.data(), minecraft.level).filter(EntityNPCInterface.class::isInstance).map(EntityNPCInterface.class::cast).orElse(null);
        if (preview != null)
            NpcSkins.resolveOnClient(preview);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        int left = guiLeft + PREVIEW_LEFT;
        int top = guiTop + LIST_TOP;
        graphics.fill(left, top, guiLeft + 392, top + LIST_HEIGHT, 0x40303030);
        if (preview == null)
        {
            graphics.drawCenteredString(font, Component.translatable(PREFIX + "preview"), left + 56, top + 58, 0xFFFFFF);
            return;
        }
        graphics.enableScissor(left, top, guiLeft + 392, top + LIST_HEIGHT);
        try
        {
            NpcClientPlatform.drawNpc(this, graphics, preview, PREVIEW_LEFT + 56, LIST_TOP + LIST_HEIGHT - 8, 1.6F, 0, false);
        }
        finally
        {
            graphics.disableScissor();
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll)
    {
        if (original.kind() == CrewSource.Kind.CLONE || scroll == classes)
            buttonEvent(getButton(DONE));
    }

    Optional<CrewSource> selection()
    {
        return Optional.ofNullable(selected);
    }
}
