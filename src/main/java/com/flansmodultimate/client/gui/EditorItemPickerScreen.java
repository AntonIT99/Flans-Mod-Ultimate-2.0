package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.teams.editor.EditorStacks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.util.*;
import java.util.function.Consumer;

/** A copy-only inventory/catalogue picker; it never touches the player's carried stack. */
@SuppressWarnings({"DataFlowIssue", "deprecation"}) // Screen lifecycle initializes minecraft; built-in registry IDs match the ItemStackData boundary.
public final class EditorItemPickerScreen extends Screen
{
    private final Screen parent;
    private final Consumer<ItemStack> selected;
    private boolean inventory;
    private String query = "";
    private int page;
    private List<ItemStack> choices = List.of();
    private int left;
    private int top;
    private int rows;

    public EditorItemPickerScreen(Screen parent, boolean inventory, Consumer<ItemStack> selected)
    {
        super(TeamsEditorScreen.tr("items"));
        this.parent = parent;
        this.inventory = inventory;
        this.selected = selected;
    }

    @Override
    protected void init()
    {
        left = width / 2 - 108;
        top = 12;
        rows = Math.max(1, Math.min(7, (height - 115) / 24));
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("inventory"), button -> changeSource(true)).bounds(left, top, 105, 20).build());
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("catalogue"), button -> changeSource(false)).bounds(left + 110, top, 105, 20).build());
        EditBox search = new EditBox(font, left, top + 27, 215, 20, TeamsEditorScreen.tr("search"));
        search.setHint(TeamsEditorScreen.tr("search"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            page = 0;
            refreshChoices();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1)).bounds(left, height - 45, 25, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
        {
            if ((page + 1) * rows * 9 < choices.size())
                page++;
        }).bounds(left + 30, height - 45, 25, 20).build());
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("back"), button -> onClose()).bounds(left + 110, height - 45, 105, 20).build());
        refreshChoices();
    }

    private void changeSource(boolean inventory)
    {
        this.inventory = inventory;
        page = 0;
        refreshChoices();
    }

    private void refreshChoices()
    {
        if (minecraft == null || minecraft.player == null)
            return;
        List<ItemStack> stacks = new ArrayList<>();
        if (inventory)
        {
            for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++)
            {
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                if (!stack.isEmpty())
                    stacks.add(stack.copy());
            }
        }
        else
            for (Item item : BuiltInRegistries.ITEM)
                if (item != Items.AIR)
                    stacks.add(item.getDefaultInstance());
        String needle = query.toLowerCase(Locale.ROOT);
        choices = stacks.stream()
            .filter(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle) || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(needle)).toList();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int column = ((int) mouseX - left) / 24;
        int row = ((int) mouseY - top - 55) / 24;
        if (mouseX >= left && mouseX < left + 216 && mouseY >= top + 55 && mouseY < top + 55 + rows * 24)
        {
            int index = page * rows * 9 + row * 9 + column;
            if (index < choices.size())
            {
                ItemStack stack = choices.get(index).copy();
                if (button == 1)
                    stack.setCount(stack.getMaxStackSize());
                // Serialization is also checked here, before the draft is handed back to the editor.
                EditorStacks.save(stack);
                selected.accept(stack);
                minecraft.setScreen(parent);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int cell = 0; cell < rows * 9; cell++)
        {
            int x = left + cell % 9 * 24;
            int y = top + 55 + cell / 9 * 24;
            graphics.fill(x, y, x + 22, y + 22, 0xFF424955);
            int index = page * rows * 9 + cell;
            if (index >= choices.size())
                continue;
            ItemStack stack = choices.get(index);
            graphics.renderItem(stack, x + 3, y + 3);
            graphics.renderItemDecorations(font, stack, x + 3, y + 3);
            if (mouseX >= x && mouseX < x + 22 && mouseY >= y && mouseY < y + 22)
                graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
        graphics.drawString(font, (page + 1) + " / " + Math.max(1, (choices.size() + rows * 9 - 1) / (rows * 9)), left + 62, height - 39, 0xFFFFFF);
        graphics.drawCenteredString(font, TeamsEditorScreen.tr("copy_hint"), width / 2, height - 17, 0xBBBBBB);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
