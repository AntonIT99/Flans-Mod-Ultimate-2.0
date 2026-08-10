package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Modern, texture-independent blueprint browser for the legacy vehicle table. */
public final class DriveableCraftingScreen extends AbstractContainerScreen<DriveableCraftingMenu>
{
    private static final int BLUEPRINTS_PER_PAGE = 16;
    private static final int RECIPE_ITEMS_PER_PAGE = 12;

    private static int selectedBlueprint;
    private static int blueprintPage;
    private int recipeOffset;
    private Button previousButton;
    private Button nextButton;
    private Button craftButton;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public DriveableCraftingScreen(DriveableCraftingMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 212;
    }

    @Override
    protected void init()
    {
        super.init();
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changeBlueprintPage(-1))
            .bounds(leftPos + 8, topPos + 181, 24, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changeBlueprintPage(1))
            .bounds(leftPos + 36, topPos + 181, 24, 20).build());
        craftButton = addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.driveable.craft"), button -> craftSelected())
            .bounds(leftPos + 178, topPos + 181, 62, 20).build());
        clampSelection();
        updateButtons();
    }

    private void changeBlueprintPage(int direction)
    {
        int maxPage = Math.max(0, (DriveableCraftingMenu.getBlueprints().size() - 1) / BLUEPRINTS_PER_PAGE);
        blueprintPage = Mth.clamp(blueprintPage + direction, 0, maxPage);
        updateButtons();
    }

    private void craftSelected()
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null
            || selectedBlueprint < 0 || selectedBlueprint >= blueprints.size())
            return;
        int id = DriveableCraftingMenu.CRAFT_BUTTON_BASE + selectedBlueprint;
        if (menu.clickMenuButton(minecraft.player, id))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        hoveredStack = ItemStack.EMPTY;
        clampSelection();
        updateButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderBlueprints(graphics, mouseX, mouseY);
        renderRecipe(graphics, mouseX, mouseY);
        if (!hoveredStack.isEmpty())
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        else
            renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0181C22);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF313842);
        graphics.fill(leftPos + 7, topPos + 16, leftPos + 153, topPos + 55, 0xFF20262E);
        graphics.fill(leftPos + 7, topPos + 60, leftPos + imageWidth - 7, topPos + 116, 0xFF20262E);
        graphics.fill(leftPos + 7, topPos + 121, leftPos + imageWidth - 7, topPos + 177, 0xFF20262E);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.crafting"), 8, 6, 0xFFFFFF, false);
        DriveableType selected = getSelectedBlueprint();
        if (selected == null)
        {
            graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.none"), 12, 73, 0xB0B0B0, false);
            return;
        }

        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(selected.getName(), 150)), 38, 65, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.passengers", selected.getNumPassengers()), 38, 77, 0xC9D2DC, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.cargo", selected.getNumCargoSlots()), 38, 88, 0xC9D2DC, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.ammo", selected.getNumAmmoSlots()), 38, 99, 0xC9D2DC, false);
        PartType engine = menu.getBestEngine(selected);
        boolean needsEngine = selected.numEngines() > 0;
        Component engineDescription = !needsEngine
            ? Component.translatable("gui.flansmodultimate.driveable.no_engine")
            : Component.translatable("gui.flansmodultimate.driveable.engine", selected.numEngines(), engine == null
                ? Component.translatable("gui.flansmodultimate.driveable.missing_engine") : Component.literal(engine.getName()));
        graphics.drawString(font, engineDescription, 116, 99, needsEngine && engine == null ? 0xFF7777 : 0xC9D2DC, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.requires"), 10, 123, 0xFFFFFF, false);
    }

    private void renderBlueprints(GuiGraphics graphics, int mouseX, int mouseY)
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        int first = blueprintPage * BLUEPRINTS_PER_PAGE;
        for (int visible = 0; visible < BLUEPRINTS_PER_PAGE; visible++)
        {
            int blueprint = first + visible;
            if (blueprint >= blueprints.size())
                break;
            int x = leftPos + 8 + visible % 8 * 18;
            int y = topPos + 18 + visible / 8 * 18;
            int background = blueprint == selectedBlueprint ? 0xFF4B9360 : 0xFF464F5B;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, background);
            ItemStack stack = ModUtils.getItemStack(blueprints.get(blueprint)).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty())
                graphics.renderItem(stack, x, y);
            if (isInside(mouseX, mouseY, x - 1, y - 1, 18, 18))
                hoveredStack = stack;
        }
    }

    private void renderRecipe(GuiGraphics graphics, int mouseX, int mouseY)
    {
        DriveableType selected = getSelectedBlueprint();
        if (selected == null)
            return;
        List<ItemStack> recipe = menu.getDisplayRecipe(selected);
        int maxOffset = Math.max(0, recipe.size() - RECIPE_ITEMS_PER_PAGE);
        recipeOffset = Mth.clamp(recipeOffset, 0, maxOffset);
        for (int visible = 0; visible < RECIPE_ITEMS_PER_PAGE; visible++)
        {
            int recipeIndex = recipeOffset + visible;
            if (recipeIndex >= recipe.size())
                break;
            ItemStack required = recipe.get(recipeIndex);
            int x = leftPos + 9 + visible % 12 * 18;
            int y = topPos + 140;
            boolean enough = minecraft != null && minecraft.player != null
                && (minecraft.player.getAbilities().instabuild
                    || InventoryHelper.countInInventory(minecraft.player.getInventory(), required) >= required.getCount());
            graphics.fill(x - 1, y - 1, x + 17, y + 17, enough ? 0xFF465349 : 0xFF713F43);
            graphics.renderItem(required, x, y);
            graphics.renderItemDecorations(font, required, x, y);
            if (isInside(mouseX, mouseY, x - 1, y - 1, 18, 18))
                hoveredStack = required;
        }
        if (recipe.size() > RECIPE_ITEMS_PER_PAGE)
        {
            graphics.drawString(font, Component.literal((recipeOffset + 1) + "-" + Math.min(recipe.size(), recipeOffset + RECIPE_ITEMS_PER_PAGE)
                + " / " + recipe.size()), leftPos + 89, topPos + 163, 0xAAB4C0, false);
        }

        ItemStack output = ModUtils.getItemStack(selected).orElse(ItemStack.EMPTY);
        if (!output.isEmpty())
            graphics.renderItem(output, leftPos + 16, topPos + 76);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            int localX = (int) mouseX - leftPos - 8;
            int localY = (int) mouseY - topPos - 18;
            if (localX >= 0 && localX < 8 * 18 && localY >= 0 && localY < 2 * 18)
            {
                int visible = localX / 18 + localY / 18 * 8;
                int index = blueprintPage * BLUEPRINTS_PER_PAGE + visible;
                if (index < DriveableCraftingMenu.getBlueprints().size())
                {
                    selectedBlueprint = index;
                    recipeOffset = 0;
                    updateButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        DriveableType selected = getSelectedBlueprint();
        if (selected != null && isInside(mouseX, mouseY, leftPos + 7, topPos + 121, imageWidth - 14, 56))
        {
            int maxOffset = Math.max(0, menu.getDisplayRecipe(selected).size() - RECIPE_ITEMS_PER_PAGE);
            recipeOffset = Mth.clamp(recipeOffset + (scrollY < 0D ? 1 : -1), 0, maxOffset);
            return true;
        }
        changeBlueprintPage(scrollY < 0D ? 1 : -1);
        return true;
    }

    private void clampSelection()
    {
        int size = DriveableCraftingMenu.getBlueprints().size();
        selectedBlueprint = size == 0 ? -1 : Mth.clamp(selectedBlueprint, 0, size - 1);
        int maxPage = Math.max(0, (size - 1) / BLUEPRINTS_PER_PAGE);
        blueprintPage = Mth.clamp(blueprintPage, 0, maxPage);
        if (selectedBlueprint >= 0 && selectedBlueprint / BLUEPRINTS_PER_PAGE != blueprintPage)
            selectedBlueprint = Math.min(size - 1, blueprintPage * BLUEPRINTS_PER_PAGE);
    }

    private void updateButtons()
    {
        if (previousButton == null || nextButton == null || craftButton == null)
            return;
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        previousButton.active = blueprintPage > 0;
        nextButton.active = (blueprintPage + 1) * BLUEPRINTS_PER_PAGE < blueprints.size();
        DriveableType selected = getSelectedBlueprint();
        craftButton.active = selected != null && menu.canCraft(selected);
    }

    private DriveableType getSelectedBlueprint()
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        return selectedBlueprint >= 0 && selectedBlueprint < blueprints.size() ? blueprints.get(selectedBlueprint) : null;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
