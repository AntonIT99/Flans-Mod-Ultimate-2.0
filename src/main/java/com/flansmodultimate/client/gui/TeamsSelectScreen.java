package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.teams.TeamFlagTextures;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.network.server.teams.PacketTeamsAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Modern recreation of the legacy team and class selection window. */
public final class TeamsSelectScreen extends Screen
{
    /** Box of the team flag drawn inside a team button. */
    private static final int TEAM_FLAG_WIDTH = 24;
    private static final int TEAM_FLAG_HEIGHT = 16;

    private final boolean classMenu;
    /**
     * Choices of the update that opened this screen. Only that update carries them; the periodic syncs that follow send
     * empty lists, so the screen keeps its own copy.
     */
    private final List<PacketTeamsState.TeamChoice> teamChoices;
    private final List<PacketTeamsState.ClassChoice> classChoices;
    private int panelHeight;
    private int page;
    private int pageCount;
    private int firstRow;
    private int visibleRows;

    public TeamsSelectScreen(boolean classMenu)
    {
        super(Component.translatable(classMenu ? "gui.flansmod.teams.choose_class" : "gui.flansmod.teams.choose_team"));
        this.classMenu = classMenu;
        PacketTeamsState state = TeamsClientState.get();
        teamChoices = state == null ? List.of() : state.getTeamChoices();
        classChoices = state == null ? List.of() : state.getClassChoices();
    }

    @Override
    protected void init()
    {
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
        {
            onClose();
            return;
        }
        int count = classMenu ? classChoices.size() : teamChoices.size();
        // Leave eight pixels above and below the panel, reserving one row for navigation when needed.
        int availableRows = Math.max(1, (height - 45) / 24);
        int rowsPerPage = count > availableRows ? Math.max(1, (height - 69) / 24) : availableRows;
        pageCount = Math.max(1, (count + rowsPerPage - 1) / rowsPerPage);
        page = Math.min(page, pageCount - 1);
        firstRow = page * rowsPerPage;
        visibleRows = Math.min(rowsPerPage, count - firstRow);
        panelHeight = 29 + 24 * visibleRows + (pageCount > 1 ? 24 : 0);
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;

        if (classMenu)
        {
            for (int i = 0; i < visibleRows; i++)
            {
                PacketTeamsState.ClassChoice choice = classChoices.get(firstRow + i);
                Button button = Button.builder(Component.literal(choice.name()), ignored -> chooseClass(choice.id())).bounds(left + 9, top + 24 + 24 * i, 73, 20).build();
                button.active = state.getPlayerRank() >= choice.unlockLevel() && state.getPlayerPrestige() >= choice.requiredPrestige() && !choice.isFull();
                addRenderableWidget(button);
            }
        }
        else
        {
            for (int i = 0; i < visibleRows; i++)
            {
                PacketTeamsState.TeamChoice choice = teamChoices.get(firstRow + i);
                Component label = Component.literal(choice.name()).withStyle(style -> style.withColor(choice.colour()));
                addRenderableWidget(Button.builder(label, ignored -> chooseTeam(choice.id())).bounds(left + 10, top + 24 + 24 * i, 236, 20).build());
            }
        }
        if (pageCount > 1)
        {
            int navigationY = top + 24 + 24 * visibleRows;
            Button previous = Button.builder(Component.literal("<"), ignored -> changePage(-1)).bounds(left + 9, navigationY, 30, 20).build();
            previous.active = page > 0;
            addRenderableWidget(previous);
            Button next = Button.builder(Component.literal(">"), ignored -> changePage(1)).bounds(left + 217, navigationY, 30, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }
    }

    private void changePage(int offset)
    {
        page = Math.max(0, Math.min(pageCount - 1, page + offset));
        rebuildWidgets();
    }

    private void chooseTeam(String id)
    {
        PacketHandler.sendToServer(PacketTeamsAction.selectTeam(id));
        minecraft.setScreen(null);
    }

    private void chooseClass(String id)
    {
        PacketHandler.sendToServer(PacketTeamsAction.selectClass(id));
        minecraft.setScreen(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        if (TeamsClientState.get() == null)
            return;
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top, 0, 0, 256, 22, 256, 256);
        int count = visibleRows + (pageCount > 1 ? 1 : 0);
        for (int row = 0; row < count; row++)
            graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top + 22 + 24 * row, 0, classMenu && row < visibleRows ? 23 : 48, 256, 24, 256, 256);
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top + 22 + 24 * count, 0, 73, 256, 7, 256, 256);
        graphics.drawString(font, title, left + 8, top + 7, 0xFFFFFF, true);
        if (pageCount > 1)
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, top + 30 + 24 * visibleRows, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (classMenu)
            renderLoadouts(graphics, left, top, mouseX, mouseY);
        else
            renderTeamFlags(graphics, left, top);
    }

    /** The flag of each team at the left end of its button. */
    private void renderTeamFlags(GuiGraphics graphics, int left, int top)
    {
        for (int i = 0; i < visibleRows; i++)
        {
            TeamFlagTextures.FlagTexture flag = TeamFlagTextures.get(Team.getTeam(teamChoices.get(firstRow + i).id()));
            if (flag != null)
                TeamFlagTextures.draw(graphics, flag, left + 14, top + 26 + 24 * i, TEAM_FLAG_WIDTH, TEAM_FLAG_HEIGHT);
        }
    }

    private void renderLoadouts(GuiGraphics graphics, int left, int top, int mouseX, int mouseY)
    {
        for (int row = 0; row < visibleRows; row++)
        {
            PacketTeamsState.ClassChoice choice = classChoices.get(firstRow + row);
            List<ItemStack> loadout = choice.loadout();
            for (int slot = 0; slot < Math.min(9, loadout.size()); slot++)
            {
                int x = left + 85 + 18 * slot;
                int y = top + 26 + 24 * row;
                ItemStack stack = loadout.get(slot);
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(font, stack, x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                    graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
            int y = top + 24 + 24 * row;
            if (mouseX >= left + 9 && mouseX < left + 82 && mouseY >= y && mouseY < y + 20)
                graphics.renderComponentTooltip(font, classRequirements(choice), mouseX, mouseY);
        }
    }

    /** What a class asks of the player, and how many of its limited slots are taken. */
    private static List<Component> classRequirements(PacketTeamsState.ClassChoice choice)
    {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.flansmod.teams.required_rank", choice.unlockLevel()));
        if (choice.requiredPrestige() > 0)
            lines.add(Component.translatable("gui.flansmod.teams.required_prestige", choice.requiredPrestige()));
        if (choice.capacity() != PacketTeamsState.UNLIMITED_CLASS)
            lines.add(Component.translatable("gui.flansmod.teams.class_slots", choice.used(), choice.capacity()));
        return lines;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
