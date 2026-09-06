package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.inventory.MechaInventoryMenu;
import com.flansmodultimate.common.types.MechaType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Flan's Mod mecha window: equipment slots around a live mecha preview, with its cargo alongside. */
public final class MechaInventoryScreen extends AbstractContainerScreen<MechaInventoryMenu>
{
    private static final int SHEET_WIDTH = 512;
    private static final int SHEET_HEIGHT = 256;
    private static final int GUI_WIDTH = 350;
    private static final int GUI_HEIGHT = 180;

    private static final int CARGO_ROW_LEFT = 185;
    private static final int CARGO_ROW_TOP = 24;
    private static final int CARGO_ROW_HEIGHT = 19;
    private static final int CARGO_ROW_U = 181;
    private static final int CARGO_ROW_V = 97;
    private static final int SLOT_SIZE = 18;

    private static final int SCROLL_BUTTON_LEFT = 336;
    private static final int SCROLL_UP_TOP = 41;
    private static final int SCROLL_DOWN_TOP = 53;
    private static final int SCROLL_BUTTON_SIZE = 10;
    private static final int SCROLL_UP_DISABLED_V = 0;
    private static final int SCROLL_DOWN_DISABLED_V = 10;
    private static final int SCROLL_DISABLED_U = 350;

    private static final int LOW_FUEL_LEFT = 161;
    private static final int LOW_FUEL_TOP = 31;
    private static final int LOW_FUEL_SIZE = 6;
    private static final int LOW_FUEL_U = 360;
    private static final int FUEL_BAR_LEFT = 157;
    private static final int FUEL_BAR_BOTTOM = 135;
    private static final int FUEL_BAR_WIDTH = 15;
    private static final int FUEL_BAR_HEIGHT = 94;
    private static final int FUEL_BAR_U = 350;
    private static final int FUEL_BAR_V = 20;
    private static final int LOW_FUEL_FRACTION = 8;

    private static final int MODEL_LEFT = 92;
    private static final int MODEL_TOP = 105;
    private static final float MODEL_SCALE_FACTOR = 50F;
    private static final float MODEL_TILT = 30F;
    private static final float SPIN_DEGREES_PER_TICK = 1F;

    public MechaInventoryScreen(MechaInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        titleLabelX = 9;
        titleLabelY = 9;
        inventoryLabelX = 181;
        inventoryLabelY = GUI_HEIGHT - 96 + 2;
    }

    @Override
    protected void init()
    {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.mecha.driveable_menu"),
                ignored -> sendMenuButton(MechaInventoryMenu.DRIVEABLE_MENU_BUTTON))
            .bounds(leftPos + 9, topPos + 153, 93, 20).build());
    }

    private void sendMenuButton(int id)
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
            return;
        if (menu.clickMenuButton(minecraft.player, id))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);

        renderCargoRows(graphics);
        renderScrollButtons(graphics);
        renderFuelGauge(graphics);
        renderMecha(graphics, partialTick);
    }

    /** Draws one slot-background strip per visible cargo row, cut short on the last partial row. */
    private void renderCargoRows(GuiGraphics graphics)
    {
        int cargoSlots = menu.getCargoSlotCount();
        for (int row = 0; row < MechaInventoryMenu.VISIBLE_ROWS; row++)
        {
            int firstSlot = (menu.getScrollRow() + row) * MechaInventoryMenu.COLUMN_COUNT;
            int columns = Math.min(MechaInventoryMenu.COLUMN_COUNT, cargoSlots - firstSlot);
            if (columns <= 0)
                break;
            graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos + CARGO_ROW_LEFT, topPos + CARGO_ROW_TOP + CARGO_ROW_HEIGHT * row,
                CARGO_ROW_U, CARGO_ROW_V, SLOT_SIZE * columns, SLOT_SIZE, SHEET_WIDTH, SHEET_HEIGHT);
        }
    }

    /** Greys out each scroll arrow once the cargo list cannot move further that way. */
    private void renderScrollButtons(GuiGraphics graphics)
    {
        if (menu.getScrollRow() <= 0)
            graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos + SCROLL_BUTTON_LEFT, topPos + SCROLL_UP_TOP,
                SCROLL_DISABLED_U, SCROLL_UP_DISABLED_V, SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE, SHEET_WIDTH, SHEET_HEIGHT);
        if (menu.getScrollRow() >= menu.getMaxScrollRow())
            graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos + SCROLL_BUTTON_LEFT, topPos + SCROLL_DOWN_TOP,
                SCROLL_DISABLED_U, SCROLL_DOWN_DISABLED_V, SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE, SHEET_WIDTH, SHEET_HEIGHT);
    }

    private void renderFuelGauge(GuiGraphics graphics)
    {
        Mecha mecha = menu.getMecha();
        if (mecha == null || mecha.getDriveableData() == null || mecha.getConfigType() == null)
            return;

        int fuelTankSize = mecha.getConfigType().getFuelTankSize();
        if (fuelTankSize <= 0)
            return;
        float fuelInTank = mecha.getDriveableData().getFuelInTank();

        if (fuelInTank < (float)fuelTankSize / LOW_FUEL_FRACTION && (mecha.tickCount / 5) % 4 > 1)
            graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos + LOW_FUEL_LEFT, topPos + LOW_FUEL_TOP,
                LOW_FUEL_U, 0, LOW_FUEL_SIZE, LOW_FUEL_SIZE, SHEET_WIDTH, SHEET_HEIGHT);

        int barHeight = (int)(FUEL_BAR_HEIGHT * Math.min(fuelInTank / fuelTankSize, 1F));
        if (barHeight > 0)
            graphics.blit(FlansMod.TEXTURE_GUI_MECHAINVENTORY, leftPos + FUEL_BAR_LEFT, topPos + FUEL_BAR_BOTTOM - barHeight,
                FUEL_BAR_U, FUEL_BAR_V, FUEL_BAR_WIDTH, barHeight, SHEET_WIDTH, SHEET_HEIGHT);
    }

    /** Renders the mecha itself, slowly turning, the way the legacy window did. */
    private void renderMecha(GuiGraphics graphics, float partialTick)
    {
        Mecha mecha = menu.getMecha();
        if (mecha == null || !(mecha.getConfigType() instanceof MechaType type) || minecraft == null || minecraft.level == null)
            return;

        float spin = (minecraft.level.getGameTime() + partialTick) * SPIN_DEGREES_PER_TICK;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(leftPos + MODEL_LEFT, topPos + MODEL_TOP, 100D);
        float scale = MODEL_SCALE_FACTOR / Math.max(1F, type.getCameraDistance());
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.XP.rotationDegrees(MODEL_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        dispatcher.render(mecha, 0D, 0D, 0D, 0F, partialTick, pose, buffers, 0xF000F0);
        buffers.endBatch();
        dispatcher.setRenderShadow(true);
        Lighting.setupFor3DItems();

        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x = (int)mouseX - leftPos;
        int y = (int)mouseY - topPos;
        if (x >= SCROLL_BUTTON_LEFT && x < SCROLL_BUTTON_LEFT + SCROLL_BUTTON_SIZE)
        {
            if (y >= SCROLL_UP_TOP && y < SCROLL_UP_TOP + SCROLL_BUTTON_SIZE && menu.getScrollRow() > 0)
            {
                sendMenuButton(MechaInventoryMenu.SCROLL_UP_BUTTON);
                return true;
            }
            if (y >= SCROLL_DOWN_TOP && y < SCROLL_DOWN_TOP + SCROLL_BUTTON_SIZE && menu.getScrollRow() < menu.getMaxScrollRow())
            {
                sendMenuButton(MechaInventoryMenu.SCROLL_DOWN_BUTTON);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (menu.getMaxScrollRow() > 0)
        {
            int id = delta < 0D ? MechaInventoryMenu.SCROLL_DOWN_BUTTON : MechaInventoryMenu.SCROLL_UP_BUTTON;
            if (delta < 0D ? menu.getScrollRow() < menu.getMaxScrollRow() : menu.getScrollRow() > 0)
            {
                sendMenuButton(id);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
