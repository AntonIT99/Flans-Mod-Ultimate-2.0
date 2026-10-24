package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.crew.CrewSource;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneList;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Browses server clones or loaded teams and classes without changing the crew source until Done is pressed. */
final class CrewSelectionMenu extends GuiBasic implements IGuiData, ICustomScrollListener
{
    private static final String PREFIX = "wolffsmodnpcs.crew.select.";
    private static final int DONE = 0;
    private static final int CANCEL = 1;
    private static final int TAB = 2;
    private static final int LIST_TOP = 32;
    private static final int LIST_HEIGHT = 158;
    private final CrewSource original;
    private final List<ITeam> teams;
    private final GuiCustomScrollNop choices;
    private final GuiCustomScrollNop classes;
    private List<? extends IPlayerClass> playerClasses = List.of();
    private int cloneTab;
    private int teamIndex;
    private boolean loading;
    private CrewSource selected;

    CrewSelectionMenu(CrewSource source)
    {
        original = source;
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
        choices.setSize(clones ? 384 : 190, LIST_HEIGHT);
        choices.guiLeft = guiLeft + 8;
        choices.guiTop = guiTop + LIST_TOP;
        addScroll(choices);
        if (clones)
        {
            String[] tabs = new String[CrewSource.MAX_CLONE_TAB];
            for (int i = 0; i < tabs.length; i++)
                tabs[i] = Component.translatable(PREFIX + "tab", i + 1).getString();
            addButton(new GuiButtonNop(this, TAB, guiLeft + 280, guiTop + 4, 112, 20, tabs, cloneTab - 1));
            if (!loading)
                requestClones();
        }
        else
        {
            List<String> labels = new ArrayList<>();
            labels.add(Component.translatable(PREFIX + "all_classes").getString());
            labels.addAll(teams.stream().map(team -> team.getName() + " [" + team.getShortName() + "]").toList());
            choices.setUnsortedList(labels);
            choices.setSelectedIndex(teamIndex + 1);
            classes.setSize(190, LIST_HEIGHT);
            classes.guiLeft = guiLeft + 202;
            classes.guiTop = guiTop + LIST_TOP;
            addScroll(classes);
            showClasses();
        }
        addButton(new GuiButtonNop(this, DONE, guiLeft + 202, guiTop + 196, 92, 20, "gui.done"));
        addButton(new GuiButtonNop(this, CANCEL, guiLeft + 300, guiTop + 196, 92, 20, "gui.cancel"));
        updateButtons();
    }

    private void requestClones()
    {
        loading = true;
        choices.clear();
        Packets.sendServer(new SPacketCloneList(cloneTab));
    }

    private void showClasses()
    {
        playerClasses = (teamIndex < 0 ? FlansModApi.getPlayerClasses() : teams.get(teamIndex).getClasses()).stream()
            .sorted(Comparator.comparing(IPlayerClass::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(IPlayerClass::getShortName)).toList();
        classes.clear();
        classes.setUnsortedList(playerClasses.stream().map(playerClass -> playerClass.getName() + " [" + playerClass.getShortName() + "]").toList());
        for (int i = 0; i < playerClasses.size(); i++)
            if (playerClasses.get(i).getShortName().equals(original.playerClass()))
                classes.setSelectedIndex(i);
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
        if (button.id == CANCEL)
            close();
        else if (button.id == TAB && !loading)
        {
            cloneTab = button.getValue() + 1;
            requestClones();
            updateButtons();
        }
        else if (button.id == DONE)
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
                selected = original.withClass(teamIndex < 0 ? "" : teams.get(teamIndex).getShortName(), playerClasses.get(index).getShortName());
            }
            close();
        }
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
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll)
    {
        if (original.kind() == CrewSource.Kind.CLASS && scroll == choices)
        {
            teamIndex = choices.getSelectedIndex() - 1;
            showClasses();
        }
        updateButtons();
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
