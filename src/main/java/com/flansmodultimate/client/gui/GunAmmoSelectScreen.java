package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketGunPreferredAmmo;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Selects which compatible magazine the held gun should prefer on its next reload. */
public final class GunAmmoSelectScreen extends Screen
{
    private static final int CHOICES_PER_PAGE = 8;
    private static final int ROW_HEIGHT = 24;
    private final InteractionHand hand;
    private final List<AmmoChoice> choices = new ArrayList<>();
    private int page;

    public GunAmmoSelectScreen(InteractionHand hand)
    {
        super(Component.translatable("gui.flansmodultimate.ammo_select.title"));
        this.hand = hand;
    }

    @Override
    protected void init()
    {
        choices.clear();
        ItemStack gunStack = minecraft.player != null ? minecraft.player.getItemInHand(hand) : ItemStack.EMPTY;
        if (!(gunStack.getItem() instanceof GunItem gunItem))
        {
            onClose();
            return;
        }

        Map<String, ShootableType> uniqueTypes = new LinkedHashMap<>();
        for (ShootableType type : gunItem.getConfigType().getAmmoTypes())
            uniqueTypes.putIfAbsent(type.getOriginalShortName(), type);
        for (ShootableType type : uniqueTypes.values())
            choices.add(new AmmoChoice(type, ModUtils.getItemStack(type).orElse(ItemStack.EMPTY)));

        if (choices.isEmpty())
        {
            onClose();
            return;
        }
        page = Math.min(page, maxPage());
        rebuildWidgets(gunItem.getPreferredAmmo(gunStack));
    }

    private void rebuildWidgets(String selectedAmmo)
    {
        clearWidgets();
        int first = page * CHOICES_PER_PAGE;
        int count = Math.min(CHOICES_PER_PAGE, choices.size() - first);
        int top = height / 2 - (count * ROW_HEIGHT + 45) / 2;
        for (int row = 0; row < count; row++)
        {
            AmmoChoice choice = choices.get(first + row);
            String name = choice.type().getName();
            Component label = Component.literal(name.isBlank() ? choice.type().getOriginalShortName() : name);
            Button button = Button.builder(label, ignored -> select(choice.type().getOriginalShortName()))
                .bounds(width / 2 - 70, top + 24 + row * ROW_HEIGHT, 166, 20).build();
            button.active = !choice.type().getOriginalShortName().equals(selectedAmmo);
            addRenderableWidget(button);
        }

        if (maxPage() > 0)
        {
            Button previous = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                .bounds(width / 2 - 70, top + 28 + count * ROW_HEIGHT, 80, 20).build();
            previous.active = page > 0;
            addRenderableWidget(previous);
            Button next = Button.builder(Component.literal(">"), ignored -> changePage(1))
                .bounds(width / 2 + 16, top + 28 + count * ROW_HEIGHT, 80, 20).build();
            next.active = page < maxPage();
            addRenderableWidget(next);
        }
    }

    private void changePage(int delta)
    {
        page = Math.max(0, Math.min(maxPage(), page + delta));
        ItemStack stack = minecraft.player != null ? minecraft.player.getItemInHand(hand) : ItemStack.EMPTY;
        String selected = stack.getItem() instanceof GunItem gunItem ? gunItem.getPreferredAmmo(stack) : "";
        rebuildWidgets(selected);
    }

    private int maxPage()
    {
        return Math.max(0, (choices.size() - 1) / CHOICES_PER_PAGE);
    }

    private void select(String ammoName)
    {
        PacketHandler.sendToServer(new PacketGunPreferredAmmo(hand, ammoName));
        minecraft.setScreen(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        int first = page * CHOICES_PER_PAGE;
        int count = Math.min(CHOICES_PER_PAGE, choices.size() - first);
        int top = height / 2 - (count * ROW_HEIGHT + 45) / 2;
        graphics.fill(width / 2 - 104, top - 4, width / 2 + 104,
            top + 52 + count * ROW_HEIGHT, 0xCC101010);
        graphics.drawCenteredString(font, title, width / 2, top + 4, 0xFFFFFF);
        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int row = 0; row < count; row++)
        {
            AmmoChoice choice = choices.get(first + row);
            int x = width / 2 - 96;
            int y = top + 26 + row * ROW_HEIGHT;
            if (!choice.stack().isEmpty())
            {
                graphics.renderItem(choice.stack(), x, y);
                graphics.renderItemDecorations(font, choice.stack(), x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                    hoveredStack = choice.stack();
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredStack.isEmpty())
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private record AmmoChoice(ShootableType type, ItemStack stack) {}
}
