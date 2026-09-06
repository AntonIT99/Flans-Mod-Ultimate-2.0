package com.flansmodultimate.client.gui;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Blueprint browser of the Vehicle Crafting Table, on the legacy Flan's Mod panel. */
public final class DriveableCraftingScreen extends AbstractContainerScreen<DriveableCraftingMenu>
{
    private static final int SHEET_SIZE = 256;
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 234;

    private static final int SLOT_SIZE = 18;
    private static final int BLUEPRINT_COLUMNS = 8;
    private static final int BLUEPRINT_ROWS = 4;
    private static final int BLUEPRINTS_LEFT = 8;
    private static final int BLUEPRINTS_TOP = 18;

    private static final int RECIPE_COLUMNS = 4;
    private static final int RECIPE_ROWS = 3;
    private static final int RECIPE_LEFT = 8;
    private static final int RECIPE_TOP = 174;

    private static final int ENGINE_SLOT_LEFT = 152;
    private static final int ENGINE_SLOT_TOP = 174;
    private static final int STATS_LEFT = 82;
    private static final int STATS_TOP = 100;
    private static final int STATS_LINE_HEIGHT = 10;
    private static final int MODEL_CENTRE_X = 42;
    private static final int MODEL_CENTRE_Y = 125;
    private static final float PREVIEW_SCALE = 50F;
    private static final float PREVIEW_TILT = 30F;
    private static final float PREVIEW_SPIN_SPEED = 1F;

    /** Red "missing item" frame and the selected-blueprint highlight live on the sheet. */
    private static final int MISSING_U = 195;
    private static final int MISSING_V = 11;
    private static final int SELECTED_U = 213;
    private static final int SELECTED_V = 11;
    private static final int ICON_SIZE = 16;

    private static final int WHITE = 0xFFFFFF;
    private static final int MAX_NAME_WIDTH = 88;

    private static int selectedBlueprint;
    private static int blueprintScrollRow;
    private int recipeScrollRow;

    private ArrowButton blueprintsUpButton;
    private ArrowButton blueprintsDownButton;
    private ArrowButton recipeUpButton;
    private ArrowButton recipeDownButton;
    private Button craftButton;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public DriveableCraftingScreen(DriveableCraftingMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init()
    {
        super.init();
        craftButton = addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.driveable.craft"), button -> craftSelected())
            .bounds(leftPos + 110, topPos + 198, 40, 20).build());
        blueprintsUpButton = addRenderableWidget(new ArrowButton(leftPos + 157, topPos + 21, true, button -> scrollBlueprints(-1)));
        blueprintsDownButton = addRenderableWidget(new ArrowButton(leftPos + 157, topPos + 75, false, button -> scrollBlueprints(1)));
        recipeUpButton = addRenderableWidget(new ArrowButton(leftPos + 83, topPos + 177, true, button -> scrollRecipe(-1)));
        recipeDownButton = addRenderableWidget(new ArrowButton(leftPos + 83, topPos + 213, false, button -> scrollRecipe(1)));
        clampSelection();
        updateButtons();
    }

    private void scrollBlueprints(int direction)
    {
        blueprintScrollRow = Mth.clamp(blueprintScrollRow + direction, 0, maxBlueprintScrollRow());
        updateButtons();
    }

    private void scrollRecipe(int direction)
    {
        recipeScrollRow = Mth.clamp(recipeScrollRow + direction, 0, maxRecipeScrollRow());
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        hoveredStack = ItemStack.EMPTY;
        clampSelection();
        updateButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredStack.isEmpty())
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        else
            renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLECRAFTING, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, SHEET_SIZE, SHEET_SIZE);
        renderBlueprints(graphics, mouseX, mouseY);
        renderRecipe(graphics, mouseX, mouseY);
        renderEngineSlot(graphics, mouseX, mouseY);
        renderPreview(graphics, partialTick);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.crafting"), 6, 6, WHITE, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.requires"), 6, 161, WHITE, false);

        DriveableType selected = getSelectedBlueprint();
        if (selected == null)
        {
            graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.none"), STATS_LEFT, STATS_TOP, WHITE, false);
            return;
        }

        graphics.drawString(font, font.plainSubstrByWidth(selected.getName(), MAX_NAME_WIDTH), STATS_LEFT, STATS_TOP, WHITE, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.cargo", selected.getNumCargoSlots()),
            STATS_LEFT, STATS_TOP + STATS_LINE_HEIGHT, WHITE, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.passengers", selected.getNumPassengers()),
            STATS_LEFT, STATS_TOP + STATS_LINE_HEIGHT * 2, WHITE, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.ammo", selected.getNumAmmoSlots()),
            STATS_LEFT, STATS_TOP + STATS_LINE_HEIGHT * 3, WHITE, false);

        // Engine requirement, beside its slot
        graphics.drawString(font, selected.numEngines() + "x", 100, 177, WHITE, false);
        graphics.drawString(font, Component.translatable("gui.flansmodultimate.driveable.engine_label"), 114, 177, WHITE, false);
    }

    private void renderBlueprints(GuiGraphics graphics, int mouseX, int mouseY)
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        for (int row = 0; row < BLUEPRINT_ROWS; row++)
        {
            for (int column = 0; column < BLUEPRINT_COLUMNS; column++)
            {
                int index = (blueprintScrollRow + row) * BLUEPRINT_COLUMNS + column;
                if (index >= blueprints.size())
                    return;
                int x = leftPos + BLUEPRINTS_LEFT + column * SLOT_SIZE;
                int y = topPos + BLUEPRINTS_TOP + row * SLOT_SIZE;
                if (index == selectedBlueprint)
                    graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLECRAFTING, x, y, SELECTED_U, SELECTED_V, ICON_SIZE, ICON_SIZE, SHEET_SIZE, SHEET_SIZE);

                ItemStack stack = ModUtils.getItemStack(blueprints.get(index)).orElse(ItemStack.EMPTY);
                if (stack.isEmpty())
                    continue;
                graphics.renderItem(stack, x, y);
                if (isInside(mouseX, mouseY, x, y, ICON_SIZE, ICON_SIZE))
                    hoveredStack = stack;
            }
        }
    }

    private void renderRecipe(GuiGraphics graphics, int mouseX, int mouseY)
    {
        DriveableType selected = getSelectedBlueprint();
        if (selected == null)
            return;

        // Only the parts list here: the engine has its own slot, as in the legacy table
        List<ItemStack> recipe = selected.getDriveableRecipe();
        for (int row = 0; row < RECIPE_ROWS; row++)
        {
            for (int column = 0; column < RECIPE_COLUMNS; column++)
            {
                int index = (recipeScrollRow + row) * RECIPE_COLUMNS + column;
                if (index >= recipe.size())
                    return;
                ItemStack required = recipe.get(index);
                int x = leftPos + RECIPE_LEFT + column * SLOT_SIZE;
                int y = topPos + RECIPE_TOP + row * SLOT_SIZE;

                if (!hasEnough(required))
                    graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLECRAFTING, x, y, MISSING_U, MISSING_V, ICON_SIZE, ICON_SIZE, SHEET_SIZE, SHEET_SIZE);
                graphics.renderItem(required, x, y);
                graphics.renderItemDecorations(font, required, x, y);
                if (isInside(mouseX, mouseY, x, y, ICON_SIZE, ICON_SIZE))
                    hoveredStack = required;
            }
        }
    }

    /** The engine is not part of the recipe list; the table picks the best one you carry. */
    private void renderEngineSlot(GuiGraphics graphics, int mouseX, int mouseY)
    {
        DriveableType selected = getSelectedBlueprint();
        int x = leftPos + ENGINE_SLOT_LEFT;
        int y = topPos + ENGINE_SLOT_TOP;
        if (selected == null || selected.numEngines() <= 0)
            return;

        PartType engine = menu.getBestEngine(selected);
        ItemStack engineStack = engine == null ? ItemStack.EMPTY : ModUtils.getItemStack(engine, selected.numEngines()).orElse(ItemStack.EMPTY);
        if (engineStack.isEmpty())
        {
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLECRAFTING, x, y, MISSING_U, MISSING_V, ICON_SIZE, ICON_SIZE, SHEET_SIZE, SHEET_SIZE);
            return;
        }
        graphics.renderItem(engineStack, x, y);
        graphics.renderItemDecorations(font, engineStack, x, y);
        if (isInside(mouseX, mouseY, x, y, ICON_SIZE, ICON_SIZE))
            hoveredStack = engineStack;
    }

    /** The selected driveable turns slowly in the preview window, as in the legacy table. */
    private void renderPreview(GuiGraphics graphics, float partialTick)
    {
        DriveableType selected = getSelectedBlueprint();
        if (selected == null || minecraft == null)
            return;
        if (!(ModelCache.getOrLoadTypeModel(selected) instanceof ModelDriveable model))
        {
            renderPreviewIcon(graphics, selected);
            return;
        }

        ResourceLocation texture = selected.getTexture();
        boolean translucent = ModClientConfig.get().useTranslucentRendering(selected);
        boolean cull = ModClientConfig.get().useCullingRendering(selected);
        int colour = selected.getColour();
        float red = (colour >> 16 & 255) / 255F;
        float green = (colour >> 8 & 255) / 255F;
        float blue = (colour & 255) / 255F;
        float spin = (minecraft.level == null ? 0F : minecraft.level.getGameTime() + partialTick) * PREVIEW_SPIN_SPEED;
        float scale = PREVIEW_SCALE * selected.getModelScale() / Math.max(1F, selected.getCameraDistance());

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(leftPos + MODEL_CENTRE_X, topPos + MODEL_CENTRE_Y, 100D);
        if (selected instanceof MechaType)
            pose.translate(0D, 15D, 0D);
        pose.scale(-scale, scale, scale);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.XP.rotationDegrees(PREVIEW_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        Lighting.setupForFlatItems();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            model.render(selected, pose, buffers.getBuffer(renderPass.getRenderType(texture, translucent, cull)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, 1F, renderPass);
        }
        buffers.endBatch();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    /** Content packs without a loaded model still get a recognisable picture. */
    private void renderPreviewIcon(GuiGraphics graphics, DriveableType selected)
    {
        ItemStack output = ModUtils.getItemStack(selected).orElse(ItemStack.EMPTY);
        if (output.isEmpty())
            return;
        graphics.renderItem(output, leftPos + MODEL_CENTRE_X - ICON_SIZE / 2, topPos + MODEL_CENTRE_Y - ICON_SIZE / 2);
    }

    private boolean hasEnough(ItemStack required)
    {
        if (minecraft == null || minecraft.player == null)
            return false;
        return minecraft.player.getAbilities().instabuild
            || InventoryHelper.countInInventory(minecraft.player.getInventory(), required) >= required.getCount();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        int localX = (int) mouseX - leftPos - BLUEPRINTS_LEFT;
        int localY = (int) mouseY - topPos - BLUEPRINTS_TOP;
        if (localX >= 0 && localX < BLUEPRINT_COLUMNS * SLOT_SIZE && localY >= 0 && localY < BLUEPRINT_ROWS * SLOT_SIZE)
        {
            int index = (blueprintScrollRow + localY / SLOT_SIZE) * BLUEPRINT_COLUMNS + localX / SLOT_SIZE;
            if (index < blueprints.size())
            {
                selectedBlueprint = index;
                recipeScrollRow = 0;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int direction = delta < 0D ? 1 : -1;
        if (isInside(mouseX, mouseY, leftPos + RECIPE_LEFT, topPos + RECIPE_TOP, RECIPE_COLUMNS * SLOT_SIZE, RECIPE_ROWS * SLOT_SIZE))
            scrollRecipe(direction);
        else
            scrollBlueprints(direction);
        return true;
    }

    private int maxBlueprintScrollRow()
    {
        int rows = (DriveableCraftingMenu.getBlueprints().size() + BLUEPRINT_COLUMNS - 1) / BLUEPRINT_COLUMNS;
        return Math.max(0, rows - BLUEPRINT_ROWS);
    }

    private int maxRecipeScrollRow()
    {
        DriveableType selected = getSelectedBlueprint();
        if (selected == null)
            return 0;
        int rows = (selected.getDriveableRecipe().size() + RECIPE_COLUMNS - 1) / RECIPE_COLUMNS;
        return Math.max(0, rows - RECIPE_ROWS);
    }

    private void clampSelection()
    {
        int size = DriveableCraftingMenu.getBlueprints().size();
        selectedBlueprint = size == 0 ? -1 : Mth.clamp(selectedBlueprint, 0, size - 1);
        blueprintScrollRow = Mth.clamp(blueprintScrollRow, 0, maxBlueprintScrollRow());
        recipeScrollRow = Mth.clamp(recipeScrollRow, 0, maxRecipeScrollRow());

        // Keep the selection on screen when the list is scrolled with the wheel
        if (selectedBlueprint < 0)
            return;
        int selectedRow = selectedBlueprint / BLUEPRINT_COLUMNS;
        if (selectedRow < blueprintScrollRow || selectedRow >= blueprintScrollRow + BLUEPRINT_ROWS)
            selectedBlueprint = Math.min(size - 1, blueprintScrollRow * BLUEPRINT_COLUMNS);
    }

    private void updateButtons()
    {
        if (craftButton == null || blueprintsUpButton == null)
            return;
        blueprintsUpButton.active = blueprintScrollRow > 0;
        blueprintsDownButton.active = blueprintScrollRow < maxBlueprintScrollRow();
        recipeUpButton.active = recipeScrollRow > 0;
        recipeDownButton.active = recipeScrollRow < maxRecipeScrollRow();
        DriveableType selected = getSelectedBlueprint();
        craftButton.active = selected != null && menu.canCraft(selected);
    }

    @Nullable
    private DriveableType getSelectedBlueprint()
    {
        List<DriveableType> blueprints = DriveableCraftingMenu.getBlueprints();
        return selectedBlueprint >= 0 && selectedBlueprint < blueprints.size() ? blueprints.get(selectedBlueprint) : null;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** The small scroll arrows drawn straight from the panel sheet, as in the legacy GUI. */
    private static final class ArrowButton extends Button
    {
        private static final int SIZE = 10;
        private static final int UP_ENABLED_U = 216;
        private static final int UP_DISABLED_U = 196;
        private static final int DOWN_ENABLED_U = 226;
        private static final int DOWN_DISABLED_U = 206;

        private final boolean up;

        private ArrowButton(int x, int y, boolean up, OnPress onPress)
        {
            super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
            this.up = up;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            int u = up
                ? (active ? UP_ENABLED_U : UP_DISABLED_U)
                : (active ? DOWN_ENABLED_U : DOWN_DISABLED_U);
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLECRAFTING, getX(), getY(), u, 0, SIZE, SIZE, SHEET_SIZE, SHEET_SIZE);
        }
    }
}
