package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.network.server.PacketTeamsAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Modern recreation of the legacy team and class selection window. */
public final class TeamsSelectScreen extends Screen
{
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams.png");
    private final boolean classMenu;
    private int panelHeight;

    public TeamsSelectScreen(boolean classMenu)
    {
        super(Component.translatable(classMenu ? "gui.flansmod.teams.choose_class" : "gui.flansmod.teams.choose_team"));
        this.classMenu = classMenu;
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
        int count = classMenu ? state.getClassChoices().size() : state.getTeamChoices().size();
        panelHeight = 29 + 24 * count;
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;

        if (classMenu)
        {
            List<PacketTeamsState.ClassChoice> choices = state.getClassChoices();
            for (int i = 0; i < choices.size(); i++)
            {
                PacketTeamsState.ClassChoice choice = choices.get(i);
                Button button = Button.builder(Component.literal(choice.name()), ignored -> chooseClass(choice.id()))
                    .bounds(left + 9, top + 24 + 24 * i, 73, 20).build();
                button.active = state.getPlayerRank() >= choice.unlockLevel();
                addRenderableWidget(button);
            }
        }
        else
        {
            List<PacketTeamsState.TeamChoice> choices = state.getTeamChoices();
            for (int i = 0; i < choices.size(); i++)
            {
                PacketTeamsState.TeamChoice choice = choices.get(i);
                Component label = Component.literal(choice.name()).withStyle(style -> style.withColor(choice.colour()));
                addRenderableWidget(Button.builder(label, ignored -> chooseTeam(choice.id()))
                    .bounds(left + 10, top + 24 + 24 * i, 236, 20).build());
            }
        }
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
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
            return;
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, 256, 22, 256, 256);
        int count = classMenu ? state.getClassChoices().size() : state.getTeamChoices().size();
        for (int row = 0; row < count; row++)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top + 22 + 24 * row, 0, classMenu ? 23 : 48, 256, 24, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top + 22 + 24 * count, 0, 73, 256, 7, 256, 256);
        graphics.text(font, title, left + 8, top + 7, 0xFFFFFF, true);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (classMenu)
            renderLoadouts(graphics, state, left, top, mouseX, mouseY);
    }

    private void renderLoadouts(GuiGraphicsExtractor graphics, PacketTeamsState state, int left, int top, int mouseX, int mouseY)
    {
        for (int row = 0; row < state.getClassChoices().size(); row++)
        {
            PacketTeamsState.ClassChoice choice = state.getClassChoices().get(row);
            List<ItemStack> loadout = choice.loadout();
            for (int slot = 0; slot < Math.min(9, loadout.size()); slot++)
            {
                int x = left + 85 + 18 * slot;
                int y = top + 26 + 24 * row;
                ItemStack stack = loadout.get(slot);
                graphics.item(stack, x, y);
                graphics.itemDecorations(font, stack, x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                    graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
            int y = top + 24 + 24 * row;
            if (mouseX >= left + 9 && mouseX < left + 82 && mouseY >= y && mouseY < y + 20)
                graphics.setTooltipForNextFrame(font, Component.translatable("gui.flansmod.teams.required_rank", choice.unlockLevel()), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
