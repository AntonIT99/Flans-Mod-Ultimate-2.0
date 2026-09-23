package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TeamsRewardBoxScreen extends Screen
{
    private static final int WIDTH = 196;
    private static final int HEIGHT = 200;
    private static final int ROWS = 5;

    public TeamsRewardBoxScreen()
    {
        super(Component.literal("Reward Boxes"));
    }

    @Override
    protected void init()
    {
        PacketLoadoutState state = LoadoutClientState.get();
        if (state == null)
            return;

        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        int row = 0;

        // One row per box type the pool offers, whether or not the player holds
        // any, so the kinds still to be earned stay visible. A type with nothing
        // unopened shows a disabled button rather than disappearing.
        for (PacketLoadoutState.BoxTypeView type : state.getBoxTypes())
        {
            if (row >= ROWS)
                break;
            Button button = Button.builder(Component.literal("Open " + type.name() + " x" + type.unopened()),
                    ignored -> openFirstUnopened(state, type.boxId()))
                .bounds(left + 28, top + 30 + row * 25, 140, 20).build();
            button.active = type.unopened() > 0;
            addRenderableWidget(button);
            row++;
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.openHub()))
            .bounds(left + 68, top + 171, 60, 20).build());
    }

    /** Opens the oldest box the player still holds of this kind. */
    private static void openFirstUnopened(PacketLoadoutState state, String boxId)
    {
        state.getBoxes().stream()
            .filter(box -> !box.opened() && boxId.equals(box.boxId()))
            .findFirst()
            .ifPresent(box -> PacketHandler.sendToServer(PacketLoadoutAction.openBox(box.id())));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        PacketLoadoutState state = LoadoutClientState.get();

        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        graphics.blit(FlansMod.TEXTURE_GUI_TEAMSOPENCREATES, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);

        if (state != null)
        {
            int row = 0;
            for (PacketLoadoutState.BoxTypeView type : state.getBoxTypes())
            {
                if (row >= ROWS)
                    break;
                if (!type.preview().isEmpty())
                    graphics.renderItem(type.preview(), left + 6, top + 30 + row * 25 + 2);
                row++;
            }
        }

        if (state != null && !state.getRevealedReward().isBlank())
        {
            String name = state.getRewards().stream().filter(reward -> reward.key().equals(state.getRevealedReward()))
                .map(PacketLoadoutState.RewardView::name)
                .findFirst()
                .orElse(state.getRevealedReward());
            graphics.drawCenteredString(font, "Unlocked: " + name, width / 2, top + 151, 0xFFE06B);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
