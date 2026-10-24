package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.TeamFactionPacket;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searchable team picker; creating a faction is explicit and never changes the edited NPC's faction. */
final class TeamFactionMenu extends GuiBasic implements ICustomScrollListener
{
    private static final String PREFIX = "wolffsmodnpcs.team_faction.";
    private static final int CREATE = 0;
    private static final int CANCEL = 1;
    private final int entityId;
    private final List<ITeam> teams;
    private final GuiCustomScrollNop choices;
    private List<ITeam> filtered = List.of();
    private String query = "";

    TeamFactionMenu(int entityId)
    {
        this.entityId = entityId;
        imageWidth = 400;
        imageHeight = 242;
        setBackground("menubg.png");
        teams = FlansModApi.getTeams().stream().sorted(Comparator.comparing(ITeam::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ITeam::getShortName)).toList();
        choices = new GuiCustomScrollNop(this, 0);
    }

    @Override
    public void init()
    {
        super.init();
        addLabel(new GuiLabel(0, PREFIX + "select", guiLeft + 8, guiTop + 8));
        GuiTextFieldNop search = new GuiTextFieldNop(0, this, guiLeft + 8, guiTop + 26, 384, 20, query);
        search.setHint(Component.translatable(PREFIX + "search"));
        search.setResponder(value ->
        {
            query = value;
            filter();
        });
        addTextField(search);
        choices.setSize(384, 154);
        choices.guiLeft = guiLeft + 8;
        choices.guiTop = guiTop + 52;
        addScroll(choices);
        addButton(new GuiButtonNop(this, CREATE, guiLeft + 106, guiTop + 216, 188, 20, PREFIX + "create"));
        addButton(new GuiButtonNop(this, CANCEL, guiLeft + 300, guiTop + 216, 92, 20, "gui.cancel"));
        filter();
    }

    private void filter()
    {
        String selected = choices.getSelected();
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = teams.stream()
            .filter(team -> (team.getName() + " " + team.getShortName() + " " + (team.getContentPack() == null ? "" : team.getContentPack().getName())).toLowerCase(Locale.ROOT).contains(needle))
            .toList();
        choices.clear();
        choices.setUnsortedList(filtered.stream().map(team -> team.getName() + " [" + team.getShortName() + "]").toList());
        if (selected != null)
            choices.setSelected(selected);
        addLabel(new GuiLabel(1, filtered.isEmpty() ? PREFIX + "empty" : "", guiLeft + 8, guiTop + 207));
        updateButton();
    }

    private void updateButton()
    {
        int index = choices.getSelectedIndex();
        getButton(CREATE).setEnabled(index >= 0 && index < filtered.size());
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (button.id == CANCEL)
            close();
        else if (button.id == CREATE)
        {
            int index = choices.getSelectedIndex();
            if (index < 0 || index >= filtered.size())
                return;
            NpcNetwork.sendToServer(new TeamFactionPacket(entityId, filtered.get(index).getShortName(), "", false));
            close();
        }
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll)
    {
        updateButton();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll)
    {
        buttonEvent(getButton(CREATE));
    }
}
