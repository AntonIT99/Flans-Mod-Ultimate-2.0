package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.client.CommonConfigMirror;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's options screen, reached from the button added to the vanilla options screen or pause menu, and
 * from the Config button of the mod list.
 *
 * <p>{@link Mode#QUICK} shows the settings worth changing mid-game, client ones plus a few server toggles.
 * {@link Mode#CLIENT} and {@link Mode#COMMON} show everything one of the two config files has, section by
 * section. Client settings take effect as soon as they are changed; common settings are requests the server
 * validates, applies and reports back, so the screen always ends up showing what is really in force.</p>
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
    private static final int NOTE_COLOUR = 0xA0A0A0;

    public enum Mode
    {
        /** The settings worth changing while playing, from both config files. */
        QUICK,
        /** Every client setting, section by section. */
        CLIENT,
        /** Every common setting, section by section. */
        COMMON
    }

    /** The client settings the quick screen shows, in the order and grouping it shows them. */
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
            ModClientConfig.GUN_BLOCK_INTERACTION,
            ModClientConfig.DRIVEABLE_SPEED_UNIT,
            ModClientConfig.ENABLE_UNCENSORED_CONTENT,
            ModClientConfig.OPTIONS_BUTTON_PLACEMENT)));

    private record QuickSection(String name, List<ForgeConfigSpec.ConfigValue<?>> values)
    {
    }

    @Nullable
    private final Screen lastScreen;
    private final Mode mode;
    private final ConfigTarget clientTarget = new ClientConfigTarget();
    private final CommonConfigTarget commonTarget = new CommonConfigTarget();
    /** The rows the server owns, which are disabled for players who may not change them. */
    private final List<OptionInstance<?>> commonOptions = new ArrayList<>();
    private FlansOptionsList list;
    private double scrollAmount;

    public FlansOptionsScreen(@Nullable Screen lastScreen)
    {
        this(lastScreen, Mode.QUICK);
    }

    public FlansOptionsScreen(@Nullable Screen lastScreen, Mode mode)
    {
        super(Component.translatable(switch (mode)
        {
            case QUICK -> "gui.flansmodultimate.options.title";
            case CLIENT -> "gui.flansmodultimate.options.title_client";
            case COMMON -> "gui.flansmodultimate.options.title_common";
        }));
        this.lastScreen = lastScreen;
        this.mode = mode;
    }

    @Override
    protected void init()
    {
        commonOptions.clear();
        // The values of a joined server take a round trip; the screen fills in when the answer arrives
        if (!commonTarget.ready())
            CommonConfigMirror.request();

        list = new FlansOptionsList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN, LIST_ITEM_HEIGHT);

        switch (mode)
        {
            case QUICK -> {
                addQuickClientSections();
                addQuickCommonSection();
            }
            case CLIENT -> addSections(ConfigOptionFactory.sections(clientTarget));
            case COMMON -> addCommonSections();
        }

        if (!commonOptions.isEmpty() && !commonTarget.editable())
            lockCommonOptions();

        list.setScrollAmount(scrollAmount);
        addWidget(list);
        addFooterButtons();
    }

    private void addQuickClientSections()
    {
        for (QuickSection section : QUICK_SECTIONS)
        {
            List<OptionInstance<?>> options = new ArrayList<>();
            for (ForgeConfigSpec.ConfigValue<?> value : section.values())
            {
                OptionInstance<?> option = ConfigOptionFactory.option(clientTarget, value);
                if (option != null)
                    options.add(option);
            }

            if (options.isEmpty())
                continue;

            list.addHeader(Component.translatable(SECTION_KEY_PREFIX + section.name()));
            list.addOptions(options);
        }
    }

    /** The handful of server settings worth reaching without leaving the quick screen. */
    private void addQuickCommonSection()
    {
        // Outside a world the common config is edited whole, from the hub, rather than a few settings of it
        if (minecraft == null || minecraft.level == null)
            return;

        for (ModCommonConfig.RuntimeOption runtimeOption : ModCommonConfig.RuntimeOption.values())
        {
            OptionInstance<?> option = ConfigOptionFactory.option(commonTarget, runtimeOption.getConfigValue().get());
            if (option != null)
                commonOptions.add(option);
        }

        if (commonOptions.isEmpty())
            return;

        list.addHeader(Component.translatable(SECTION_KEY_PREFIX + "server"));
        list.addOptions(commonOptions);
    }

    private void addCommonSections()
    {
        List<ConfigOptionFactory.Section> sections = ConfigOptionFactory.sections(commonTarget);
        addSections(sections);
        for (ConfigOptionFactory.Section section : sections)
            commonOptions.addAll(section.options());
    }

    private void addSections(List<ConfigOptionFactory.Section> sections)
    {
        for (ConfigOptionFactory.Section section : sections)
        {
            list.addHeader(section.title());
            list.addOptions(section.options());
        }
    }

    private void lockCommonOptions()
    {
        Tooltip locked = Tooltip.create(Component.translatable("gui.flansmodultimate.options.server_option_locked"));
        for (OptionInstance<?> option : commonOptions)
        {
            AbstractWidget widget = list.findWidget(option);
            if (widget == null)
                continue;

            widget.active = false;
            widget.setTooltip(locked);
        }
    }

    private void addFooterButtons()
    {
        if (mode != Mode.QUICK)
        {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - WIDE_BUTTON_WIDTH) / 2, height - BUTTON_BOTTOM_MARGIN, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
            return;
        }

        int left = width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.options.all_settings"),
                button -> minecraft.setScreen(new FlansSettingsHubScreen(this)))
            .bounds(left, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(left + BUTTON_WIDTH + BUTTON_GAP, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    /**
     * Rebuilds the screen with the values the server just sent, so a change it refused, or one another
     * operator made, snaps the rows back to what is really in force.
     */
    public static void onServerConfigSynced()
    {
        if (Minecraft.getInstance().screen instanceof FlansOptionsScreen screen)
            screen.refresh();
    }

    private void refresh()
    {
        if (commonOptions.isEmpty() && mode != Mode.COMMON)
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

        if (mode == Mode.COMMON)
            graphics.drawCenteredString(font, footerNote(), width / 2, height - BUTTON_BOTTOM_MARGIN - 12, NOTE_COLOUR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Says whose settings these are, because it decides whether they can be changed at all. */
    private Component footerNote()
    {
        if (!commonTarget.ready())
            return Component.translatable("gui.flansmodultimate.options.common_waiting");

        if (minecraft != null && minecraft.level == null)
            return Component.translatable("gui.flansmodultimate.options.common_local");

        return Component.translatable(commonTarget.editable()
            ? "gui.flansmodultimate.options.common_server"
            : "gui.flansmodultimate.options.common_server_read_only");
    }

    @Override
    public void removed()
    {
        // Sliders only record their value while they are dragged, to avoid rewriting the config, or asking
        // the server, on every step
        clientTarget.flush();
        commonTarget.flush();
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
