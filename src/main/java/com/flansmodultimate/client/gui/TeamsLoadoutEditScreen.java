package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TeamsLoadoutEditScreen extends Screen
{
    private static final int WIDTH = 326;
    private static final int HEIGHT = 198;
    private static final int PAGE_SIZE = 15;
    private final int loadoutIndex;
    @Getter
    private LoadoutSlot selectedSlot;
    private int page;

    public TeamsLoadoutEditScreen(int loadoutIndex, LoadoutSlot selectedSlot)
    {
        super(Component.literal("Edit Loadout " + (loadoutIndex + 1)));
        this.loadoutIndex = loadoutIndex;
        this.selectedSlot = selectedSlot;
    }

    @Override
    protected void init()
    {
        PacketLoadoutState state = LoadoutClientState.get();
        if (state == null || loadoutIndex >= state.getLoadouts().size()) return;
        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        for (LoadoutSlot slot : LoadoutSlot.values())
            addRenderableWidget(Button.builder(Component.literal(slot.getDisplayName()), ignored -> { selectedSlot = slot; page = 0; rebuild(); })
                .bounds(left + 7, top + 28 + slot.ordinal() * 25, 75, 20).build());

        List<PacketLoadoutState.Entry> choices = choices(state);
        int start = page * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, choices.size()); i++)
        {
            PacketLoadoutState.Entry choice = choices.get(i);
            int local = i - start;
            Button button = Button.builder(Component.literal(choice.name()), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.setEntry(loadoutIndex, selectedSlot, choice.typeId())))
                .bounds(left + 91 + (local % 3) * 75, top + 28 + (local / 3) * 25, 70, 20).build();
            button.active = state.getRank() >= choice.unlockRank();
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.literal("Clear"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.setEntry(loadoutIndex, selectedSlot, "$clear")))
            .bounds(left + 7, top + 159, 75, 20)
            .build());

        if (page > 0)
            addRenderableWidget(Button.builder(Component.literal("<"), ignored -> { page--; rebuild(); })
                .bounds(left + 91, top + 159, 25, 20)
                .build());

        if ((page + 1) * PAGE_SIZE < choices.size())
            addRenderableWidget(Button.builder(Component.literal(">"), ignored -> { page++; rebuild(); })
                .bounds(left + 286, top + 159, 25, 20)
                .build());

        String selectedType = "";
        var selected = state.getLoadouts().get(loadoutIndex).get(selectedSlot);
        if (selected.getItem() instanceof IFlanItem<?> item) selectedType = item.getConfigType().getOriginalShortName();
        int paint = 0;

        if (!selectedType.isBlank())
        {
            addRenderableWidget(Button.builder(Component.literal("Default"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.setPaint(loadoutIndex, selectedSlot, "$default")))
                .bounds(left + 121, top + 159, 43, 20).build());
            paint = 1;
        }

        for (PacketLoadoutState.RewardView reward : state.getRewards())
        {
            if (!selectedType.equalsIgnoreCase(reward.typeId()) || paint >= 4)
                continue;
            int x = left + 121 + paint * 47;
            addRenderableWidget(Button.builder(Component.literal(reward.name()), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.setPaint(loadoutIndex, selectedSlot, reward.key())))
                .bounds(x, top + 159, 43, 20).build());
            paint++;
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.openHub()))
            .bounds(left + 246, top + 181, 65, 16).build());
    }

    private List<PacketLoadoutState.Entry> choices(PacketLoadoutState state)
    {
        return state.getEntries().stream().filter(entry -> entry.slot() == selectedSlot).toList();
    }

    private void rebuild()
    {
        clearWidgets();
        init();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        PacketLoadoutState state = LoadoutClientState.get();
        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - HEIGHT / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FlansMod.teamsLoadoutEditorGuiTexture, left, top, 0, 0, WIDTH, HEIGHT, 512, 256);
        graphics.text(font, title, left + 8, top + 8, 0xFFFFFF, true);
        graphics.text(font, "Rank " + (state == null ? 0 : state.getRank()) + " · " + selectedSlot.getDisplayName(), left + 110, top + 9, 0xFFFFFF, false);
        if (state != null && loadoutIndex < state.getLoadouts().size())
        {
            for (LoadoutSlot slot : LoadoutSlot.values())
            {
                var stack = state.getLoadouts().get(loadoutIndex).get(slot);
                graphics.item(stack, left + 62, top + 30 + slot.ordinal() * 25);
            }
            List<PacketLoadoutState.Entry> choices = choices(state);
            int start = page * PAGE_SIZE;
            for (int i = start; i < Math.min(start + PAGE_SIZE, choices.size()); i++)
            {
                int local = i - start;
                var stack = choices.get(i).preview();
                int x = left + 93 + (local % 3) * 75;
                int y = top + 30 + (local / 3) * 25;
                graphics.item(stack, x, y);
                if (!stack.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
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
