package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketSetServerOption;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's own options screen, reached from the button added to the vanilla options screen or pause menu,
 * and from the Config button of the mod list.
 *
 * <p>{@link Mode#QUICK} shows the settings worth changing mid-game; {@link Mode#FULL} shows every client
 * setting the config file has, grouped by its sections. Client settings take effect as soon as they are
 * changed. The server settings at the bottom are sent to the server, which applies them for operators only
 * and answers with the values that are actually in force.</p>
 */
public class FlansOptionsScreen extends Screen
{
    private static final String SECTION_KEY_PREFIX = "gui.flansmodultimate.options.section.";
    private static final int TITLE_TOP = 16;
    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM_MARGIN = 36;
    private static final int LIST_ITEM_HEIGHT = 25;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_GAP = 10;
    private static final int WIDE_BUTTON_WIDTH = 200;
    private static final int BUTTON_BOTTOM_MARGIN = 27;
    private static final int OPERATOR_PERMISSION_LEVEL = 2;

    public enum Mode
    {
        /** The settings worth changing while playing. */
        QUICK,
        /** Every client setting, section by section. */
        FULL
    }

    /** The settings the quick screen shows, in the order and grouping it shows them. */
    private static final List<QuickSection> QUICK_SECTIONS = List.of(
        new QuickSection("hud", List.of(
            ModClientConfig.SHOW_FLANS_HUD,
            ModClientConfig.SHOW_AMMO_HUD,
            ModClientConfig.AMMO_HUD_LAYOUT,
            ModClientConfig.SHOW_ARMOR_DAMAGE_ABSORPTION_BAR,
            ModClientConfig.SHOW_SHOOTABLE_DURABILITY_BARS,
            ModClientConfig.SHOW_FLASHES_WHEN_WOUNDED)),
        new QuickSection("crosshair", List.of(
            ModClientConfig.HIDE_CROSSHAIR_FOR_GUNS,
            ModClientConfig.HIT_MARKER_STYLE,
            ModClientConfig.FANCY_HIT_MARKER)),
        new QuickSection("gameplay", List.of(
            ModClientConfig.AIM_TYPE,
            ModClientConfig.DRIVEABLE_SPEED_UNIT,
            ModClientConfig.ENABLE_UNCENSORED_CONTENT,
            ModClientConfig.OPTIONS_BUTTON_PLACEMENT)));

    private record QuickSection(String name, List<ForgeConfigSpec.ConfigValue<?>> values)
    {
    }

    @Nullable
    private final Screen lastScreen;
    private final Mode mode;
    private final List<OptionInstance<?>> serverOptions = new ArrayList<>();
    private FlansOptionsList list;
    private double scrollAmount;

    public FlansOptionsScreen(@Nullable Screen lastScreen)
    {
        this(lastScreen, Mode.QUICK);
    }

    public FlansOptionsScreen(@Nullable Screen lastScreen, Mode mode)
    {
        super(Component.translatable(mode == Mode.FULL
            ? "gui.flansmodultimate.options.title_all"
            : "gui.flansmodultimate.options.title"));
        this.lastScreen = lastScreen;
        this.mode = mode;
    }

    @Override
    protected void init()
    {
        serverOptions.clear();
        list = new FlansOptionsList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN, LIST_ITEM_HEIGHT);

        if (mode == Mode.FULL)
            addAllClientSections();
        else
            addQuickSections();

        addServerSection();

        list.setScrollAmount(scrollAmount);
        addWidget(list);
        addFooterButtons();
    }

    private void addQuickSections()
    {
        for (QuickSection section : QUICK_SECTIONS)
        {
            List<OptionInstance<?>> options = new ArrayList<>();
            for (ForgeConfigSpec.ConfigValue<?> value : section.values())
            {
                OptionInstance<?> option = ClientConfigOptions.option(value);
                if (option != null)
                    options.add(option);
            }

            if (options.isEmpty())
                continue;

            list.addHeader(Component.translatable(SECTION_KEY_PREFIX + section.name()));
            list.addOptions(options);
        }
    }

    private void addAllClientSections()
    {
        for (ClientConfigOptions.Section section : ClientConfigOptions.sections())
        {
            list.addHeader(section.title());
            list.addOptions(section.options());
        }
    }

    private void addServerSection()
    {
        // Outside a world there is no server to ask, and the common config belongs to whichever one is joined
        if (minecraft == null || minecraft.level == null)
            return;

        for (ModCommonConfig.RuntimeOption option : ModCommonConfig.RuntimeOption.values())
            serverOptions.add(serverOption(option));

        list.addHeader(Component.translatable(SECTION_KEY_PREFIX + "server"));
        list.addOptions(serverOptions);

        if (!mayEditServerOptions())
            lockServerOptions();
    }

    private static OptionInstance<Boolean> serverOption(ModCommonConfig.RuntimeOption option)
    {
        String key = "options.flansmodultimate.server." + option.configPath();
        return OptionInstance.createBoolean(key,
            OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
            ModCommonConfig.getRuntimeOption(option),
            newValue -> PacketHandler.sendToServer(new PacketSetServerOption(option, newValue)));
    }

    private void addFooterButtons()
    {
        if (mode == Mode.FULL)
        {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - WIDE_BUTTON_WIDTH) / 2, height - BUTTON_BOTTOM_MARGIN, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
            return;
        }

        int left = width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.options.all_settings"),
                button -> minecraft.setScreen(new FlansOptionsScreen(this, Mode.FULL)))
            .bounds(left, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(left + BUTTON_WIDTH + BUTTON_GAP, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    /**
     * Mirrors the permission check the server makes, so that a player who cannot change these settings is
     * shown why instead of being silently refused.
     */
    private boolean mayEditServerOptions()
    {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null)
            return false;

        return player.hasPermissions(OPERATOR_PERMISSION_LEVEL) || minecraft.hasSingleplayerServer();
    }

    private void lockServerOptions()
    {
        Tooltip locked = Tooltip.create(Component.translatable("gui.flansmodultimate.options.server_option_locked"));
        for (OptionInstance<?> option : serverOptions)
        {
            AbstractWidget widget = list.findWidget(option);
            if (widget == null)
                continue;

            widget.active = false;
            widget.setTooltip(locked);
        }
    }

    /**
     * Rebuilds the screen with the values the server just sent, so a change it refused, or one another
     * operator made, snaps the buttons back to what is really in force.
     */
    public static void onServerConfigSynced()
    {
        if (Minecraft.getInstance().screen instanceof FlansOptionsScreen screen)
            screen.refresh();
    }

    private void refresh()
    {
        if (serverOptions.isEmpty())
            return;

        scrollAmount = list.getScrollAmount();
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_TOP, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed()
    {
        // Sliders only change the values in memory while they are dragged, to avoid rewriting the config
        // file on every step
        ModClientConfig.flushPendingChanges();
    }

    @Override
    public void onClose()
    {
        if (minecraft != null)
            minecraft.setScreen(lastScreen);
    }

    /** Opens the quick screen on top of whatever is currently shown, keeping it as the screen to return to. */
    public static void open()
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new FlansOptionsScreen(minecraft.screen));
    }
}
