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
    /** The landing page has room for three advertised box types, as in 1.12.2. */
    private static final int BOX_SLOTS = 3;
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

        // A slot per box type the pool advertises rather than per box held, so
        // the kinds still to be earned keep their panel with a disabled button.
        for (int slot = 0; slot < Math.min(BOX_SLOTS, state.getBoxTypes().size()); slot++)
        {
            PacketLoadoutState.BoxTypeView type = state.getBoxTypes().get(slot);
            Button button = Button.builder(Component.literal("Open"), ignored -> openFirstUnopened(state, type.boxId()))
                .bounds(left + 9 + 65 * slot, top + 187, 59, 20).build();
            button.active = type.unopened() > 0;
            addRenderableWidget(button);
        }
    }

    /** Opens the oldest box the player still holds of this kind. */
    private static void openFirstUnopened(PacketLoadoutState state, String boxId)
    {
        state.getBoxes().stream()
            .filter(box -> !box.opened() && boxId.equals(box.boxId()))
            .findFirst()
            .ifPresent(box -> PacketHandler.sendToServer(PacketLoadoutAction.openBox(box.id())));
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        PacketLoadoutState state = LoadoutClientState.get();

        if (state == null)
            return;

        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        graphics.blit(FlansMod.TEXTURE_GUI_TEAMSLANDINGPAGE, left, top, 0, 0, WIDTH, HEIGHT, 512, 256);
        // The server's greeting takes the header, as it did in 1.12.2, with the pool named
        // underneath it so neither is lost.
        graphics.drawCenteredString(font, state.getMotd(), width / 2, top + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, state.getPoolName(), width / 2, top + 16, 0xA0A0A0);
        graphics.drawString(font, "Rank " + state.getRank(), left + 113, top + 150, 0xFFFFFF, false);
        TeamsRankIcon.draw(graphics, state.getRank(), 0, left + 9, top + 146, false);

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

        // Legacy landing page drew each advertised box with its icon and how
        // many of it are waiting to be opened, zero included.
        for (int slot = 0; slot < Math.min(BOX_SLOTS, state.getBoxTypes().size()); slot++)
        {
            PacketLoadoutState.BoxTypeView type = state.getBoxTypes().get(slot);
            int x = left + 7 + 65 * slot;
            if (!type.preview().isEmpty())
                graphics.renderItem(type.preview(), x + 3, top + 169);
            graphics.drawCenteredString(font, "x " + type.unopened(), x + 33, top + 173, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
