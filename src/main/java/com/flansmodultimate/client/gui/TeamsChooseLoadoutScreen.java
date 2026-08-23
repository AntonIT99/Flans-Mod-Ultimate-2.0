package com.flansmodultimate.client.gui;

import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TeamsChooseLoadoutScreen extends Screen
{
    public TeamsChooseLoadoutScreen()
    {
        super(Component.literal("Choose Loadout"));
    }

    @Override protected void init()
    {
        PacketLoadoutState state = LoadoutClientState.get();
        if (state == null)
            return;
        int top = height / 2 - 70;
        for (int i = 0; i < state.getLoadouts().size(); i++)
        {
            int index = i;
            Button button = Button.builder(Component.literal("Loadout " + (i + 1)), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.select(index)))
                .bounds(width / 2 - 110, top + i * 26, 90, 20).build();
            button.active = i >= state.getLoadoutUnlockRanks().size() || state.getRank() >= state.getLoadoutUnlockRanks().get(i);
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("Change Team"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.play()))
            .bounds(width / 2 - 45, top + 140, 90, 20).build());
    }
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        PacketLoadoutState state = LoadoutClientState.get();
        graphics.centeredText(font, title, width / 2, height / 2 - 92, 0xFFFFFF);
        if (state != null)
        {
            for (int i = 0; i < state.getLoadouts().size(); i++)
            {
                for (LoadoutSlot slot : LoadoutSlot.values())
                {
                    var stack = state.getLoadouts().get(i).get(slot);
                    int x = width / 2 - 8 + slot.ordinal() * 20;
                    int y = height / 2 - 68 + i * 26;
                    graphics.item(stack, x, y);
                    if (!stack.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                        graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
                }
            }

        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
