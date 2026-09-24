package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.client.CommonConfigMirror;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The chooser between the two config files the options screens can edit: this client's own settings, and
 * the common settings, which belong to the server while one is joined.
 */
public class FlansSettingsHubScreen extends Screen
{
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int FIRST_ROW_TOP = 70;
    private static final int TITLE_TOP = 40;
    private static final int DONE_BOTTOM_MARGIN = 38;

    @Nullable
    private final Screen lastScreen;

    public FlansSettingsHubScreen(@Nullable Screen lastScreen)
    {
        super(Component.translatable("gui.flansmodultimate.options.hub_title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init()
    {
        // The common config of a joined server takes a round trip to arrive; ask for it now so that the
        // screen behind this button is already filled in when it opens
        if (minecraft != null && minecraft.level != null)
            CommonConfigMirror.request();

        int left = (width - BUTTON_WIDTH) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.options.client_settings"),
                button -> minecraft.setScreen(new FlansOptionsScreen(this, FlansOptionsScreen.Mode.CLIENT)))
            .bounds(left, FIRST_ROW_TOP, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        Button common = addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.options.common_settings"),
                button -> minecraft.setScreen(new FlansOptionsScreen(this, FlansOptionsScreen.Mode.COMMON)))
            .bounds(left, FIRST_ROW_TOP + ROW_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        common.setTooltip(Tooltip.create(Component.translatable(minecraft != null && minecraft.level != null
            ? "gui.flansmodultimate.options.common_settings.server_tooltip"
            : "gui.flansmodultimate.options.common_settings.local_tooltip")));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(left, height - DONE_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_TOP, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose()
    {
        if (minecraft != null)
            minecraft.setScreen(lastScreen);
    }
}
