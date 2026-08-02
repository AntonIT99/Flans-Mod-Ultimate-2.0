package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TeamsLoadoutHubScreen extends Screen
{
    private static final int WIDTH = 256;
    private static final int HEIGHT = 215;

    public TeamsLoadoutHubScreen()
    {
        super(Component.literal("Teams Loadouts"));
    }

    @Override protected void init()
    {
        PacketLoadoutState state = LoadoutClientState.get();
        if (state == null || state.getPoolId().isBlank())
        {
            onClose();
            return;
        }

        int left = width / 2 - WIDTH / 2, top = height / 2 - HEIGHT / 2;
        for (int i = 0; i < Math.min(5, state.getLoadouts().size()); i++)
        {
            int index = i;
            Button button = Button.builder(Component.literal("Edit"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.edit(index)))
                .bounds(left + 12 + 49 * i, top + 117, 36, 20).build();
            button.active = i >= state.getLoadoutUnlockRanks().size() || state.getRank() >= state.getLoadoutUnlockRanks().get(i);
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.literal("Play >>"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.play()))
            .bounds(left + 190, top + 162, 59, 20).build());

        int unopened = 0;

        for (PacketLoadoutState.BoxView box : state.getBoxes())
        {
            if (box.opened())
                continue;
            int x = left + 9 + 65 * Math.min(unopened, 2);

            addRenderableWidget(Button.builder(Component.literal("Open"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.openBox(box.id())))
                .bounds(x, top + 187, 59, 20).build());

            if (++unopened >= 3)
                break;
        }
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        PacketLoadoutState state = LoadoutClientState.get();

        if (state == null)
            return;

        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        graphics.blit(FlansMod.teamsLandingPageGuiTexture, left, top, 0, 0, WIDTH, HEIGHT, 512, 256);
        graphics.drawCenteredString(font, state.getPoolName(), width / 2, top + 9, 0xFFFFFF);
        graphics.drawString(font, "Rank " + state.getRank(), left + 113, top + 150, 0xFFFFFF, false);

        int next = state.getExperienceForNextRank();
        String xp = next == Integer.MAX_VALUE ? "MAX" : state.getExperience() + " / " + next + " XP";
        graphics.drawCenteredString(font, xp, left + 154, top + 174, 0xFFFFFF);

        for (int loadout = 0; loadout < Math.min(5, state.getLoadouts().size()); loadout++) {
            for (LoadoutSlot slot : LoadoutSlot.values())
            {
                var stack = state.getLoadouts().get(loadout).get(slot);
                if (!stack.isEmpty())
                    graphics.renderItem(stack, left + 13 + loadout * 49 + slot.ordinal() * 6, top + 72 + slot.ordinal() * 7);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
