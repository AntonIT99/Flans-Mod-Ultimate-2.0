package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.client.CommonConfigMirror;
import com.flansmodultimate.client.distant.DistantHorizonsClient;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.platform.client.ClientPlatform;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's options screen, reached from the button added to the vanilla options screen or pause menu, and
 * from the Config button of the mod list.
 *
 * <p>
 * {@link Mode#QUICK} shows the settings worth changing mid-game, client ones plus a few server toggles.
 * {@link Mode#CLIENT} and {@link Mode#COMMON} show everything one of the two config files has, section by
 * section. Client settings take effect as soon as they are changed; common settings are requests the server
 * validates, applies and reports back, so the screen always ends up showing what is really in force.
 * </p>
 */
public class FlansOptionsScreen extends Screen
{
    private static final String SECTION_KEY_PREFIX = "gui.flansmodultimate.options.section.";
    private static final int TITLE_TOP = 16;
    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM_MARGIN = 66;
    private static final int LIST_ITEM_HEIGHT = 25;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_GAP = 10;
    private static final int WIDE_BUTTON_WIDTH = 200;
    private static final int BUTTON_BOTTOM_MARGIN = 27;
    private static final int RESTORE_BUTTON_BOTTOM_MARGIN = 52;
    private static final int NOTE_COLOUR = 0xA0A0A0;
    /** Only shown while Distant Horizons is installed, since its settings do nothing otherwise. */
    private static final String DISTANT_HORIZONS_SECTION = "distant_horizons";

    public enum Mode
    {
        /** The settings worth changing while playing, from both config files. */
        QUICK,
        /** The quick menu's HUD settings. */
        HUD,
        /** The quick menu's server settings. */
        SERVER,
        /** The quick menu's server sound ranges. */
        SERVER_SOUNDS,
        /** Every client setting, section by section. */
        CLIENT,
        /** Every common setting, section by section. */
        COMMON
    }

    /** The client settings the quick screen shows, in the order and grouping it shows them. */
    private static final List<QuickSection> QUICK_SECTIONS = List.of(
        new QuickSection("hud",
            List.of(ModClientConfig.SHOW_FLANS_HUD, ModClientConfig.SHOW_AMMO_HUD, ModClientConfig.AMMO_HUD_LAYOUT, ModClientConfig.SHOW_ARMOR_DAMAGE_ABSORPTION_BAR,
                ModClientConfig.SHOW_SHOOTABLE_DURABILITY_BARS, ModClientConfig.SHOW_FLASHES_WHEN_WOUNDED, ModClientConfig.VEHICLE_SCREEN_SHAKE, ModClientConfig.GUN_SCREEN_SHAKE,
                ModClientConfig.EXPLOSION_SCREEN_SHAKE, ModClientConfig.EXPLOSION_SHAKE_INTENSITY, ModClientConfig.EXPLOSION_SHAKE_DURATION, ModClientConfig.NEAR_MISS_SUPPRESSION,
                ModClientConfig.SUPPRESSION_INTENSITY, ModClientConfig.SUPPRESSION_DURATION, ModClientConfig.SUPPRESSION_CAMERA_ROLL, ModClientConfig.SUPPRESSION_VIGNETTE,
                ModClientConfig.BULLET_FLYBY_SOUNDS, ModClientConfig.BULLET_FLYBY_VOLUME, ModClientConfig.VEHICLE_HUD_LEFT_X, ModClientConfig.VEHICLE_HUD_LEFT_Y, ModClientConfig.VEHICLE_HUD_RIGHT_X,
                ModClientConfig.VEHICLE_HUD_RIGHT_Y)),
        new QuickSection("crosshair",
            List.of(ModClientConfig.HIDE_CROSSHAIR_FOR_GUNS, ModClientConfig.SHOW_CROSSHAIR_WHILE_SCOPED, ModClientConfig.HIT_MARKER_STYLE, ModClientConfig.FANCY_HIT_MARKER,
                ModClientConfig.MUZZLE_FLASH_STYLE)),
        new QuickSection("rendering", List.of(ModClientConfig.ENABLE_GPU_MODEL_CACHE, ModClientConfig.DRIVEABLE_RENDER_DISTANCE_MULTIPLIER, ModClientConfig.FLAN_NPC_RENDER_DISTANCE_MULTIPLIER)),
        new QuickSection("gameplay",
            List.of(ModClientConfig.AIM_TYPE, ModClientConfig.AIM_POSE, ModClientConfig.GUN_BLOCK_INTERACTION, ModClientConfig.DRIVEABLE_SPEED_UNIT, ModClientConfig.DRIVEABLE_VERTICAL_SPEED_UNIT,
                ModClientConfig.ENABLE_UNCENSORED_CONTENT, ModClientConfig.OPTIONS_BUTTON_PLACEMENT)),
        new QuickSection(DISTANT_HORIZONS_SECTION, List.of(ModClientConfig.DISTANT_HORIZONS_INTEGRATION, ModClientConfig.DISTANT_HORIZONS_RANGEFINDER, ModClientConfig.DISTANT_HORIZONS_CONTACTS,
            ModClientConfig.DISTANT_HORIZONS_EXPLOSIONS, ModClientConfig.DISTANT_HORIZONS_DRIVEABLE_RENDERING)));

    private record QuickSection(String name, List<ConfigValue<?>> values)
    {}

    @Nullable
    private final Screen lastScreen;
    private final Mode mode;
    private final ConfigTarget clientTarget = new ClientConfigTarget();
    private final CommonConfigTarget commonTarget = new CommonConfigTarget();
    /** The rows the server owns, which are disabled for players who may not change them. */
    private final List<ConfigOptionFactory.Option> commonOptions = new ArrayList<>();
    private FlansOptionsList list;
    private double scrollAmount;
    private final SettingsTooltip settingsTooltip = new SettingsTooltip();
    private final ConfigDefaults defaults = new ConfigDefaults();
    @Nullable
    private Button restoreDefaultsButton;

    public FlansOptionsScreen(@Nullable Screen lastScreen)
    {
        this(lastScreen, Mode.QUICK);
    }

    public FlansOptionsScreen(@Nullable Screen lastScreen, Mode mode)
    {
        super(Component.translatable(switch (mode)
        {
            case QUICK -> "gui.flansmodultimate.options.title";
            case HUD -> SECTION_KEY_PREFIX + "hud";
            case SERVER -> SECTION_KEY_PREFIX + "server";
            case SERVER_SOUNDS -> SECTION_KEY_PREFIX + "server_sounds";
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
        defaults.clear();
        // The values of a joined server take a round trip; the screen fills in when the answer arrives
        if (!commonTarget.ready())
            CommonConfigMirror.request();

        list = new FlansOptionsList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN, LIST_ITEM_HEIGHT);

        switch (mode)
        {
            case QUICK -> {
                addQuickClientSections();
                if (minecraft != null && minecraft.level != null)
                {
                    addSubmenu("server", Mode.SERVER);
                    addSubmenu("server_sounds", Mode.SERVER_SOUNDS);
                }
            }
            case HUD -> addQuickClientSections();
            case SERVER -> addQuickCommonSection(ModCommonConfig.QuickSection.SERVER);
            case SERVER_SOUNDS -> addQuickCommonSection(ModCommonConfig.QuickSection.SOUNDS);
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
            if (mode == Mode.HUD && !section.name().equals("hud"))
                continue;
            addQuickClientSection(section);
        }
    }

    private void addQuickClientSection(QuickSection section)
    {
        if (mode == Mode.QUICK && section.name().equals("hud"))
        {
            addSubmenu(section.name(), Mode.HUD);
            return;
        }
        if (section.name().equals(DISTANT_HORIZONS_SECTION) && !PlatformEnvironment.isModLoaded(DistantHorizonsClient.MOD_ID))
            return;

        List<ConfigOptionFactory.Option> options = new ArrayList<>();
        for (ConfigValue<?> value : section.values())
        {
            ConfigOptionFactory.Option option = ConfigOptionFactory.option(clientTarget, value);
            if (option != null)
                options.add(option);
        }

        if (options.isEmpty())
            return;

        if (mode == Mode.QUICK)
            list.addHeader(Component.translatable(SECTION_KEY_PREFIX + section.name()));
        if (section.name().equals("rendering"))
        {
            addOptions(options.subList(0, 1));
            list.addWidgetRow(new RenderingPresetSlider(150, RenderingPresetSlider.Kind.LOD), new RenderingPresetSlider(150, RenderingPresetSlider.Kind.IMPOSTOR));
            for (ConfigValue<?> value : ModClientConfig.renderPresetConfigValues())
                defaults.add(clientTarget, value);
            addOptions(options.subList(1, options.size()));
        }
        else
            addOptions(options);
    }

    private void addSubmenu(String section, Mode submenuMode)
    {
        list.addWidgetRow(Button.builder(Component.translatable(SECTION_KEY_PREFIX + section).append("..."), button ->
        {
            if (minecraft != null)
                minecraft.setScreen(new FlansOptionsScreen(this, submenuMode));
        }).width(BUTTON_WIDTH * 2 + BUTTON_GAP).build(), null);
    }

    /** The server settings selected for one of the quick menu's submenus. */
    private void addQuickCommonSection(ModCommonConfig.QuickSection section)
    {
        // Outside a world the common config is edited whole, from the hub, rather than a few settings of it
        if (minecraft == null || minecraft.level == null)
            return;

        List<ConfigOptionFactory.Option> options = new ArrayList<>();
        for (ModCommonConfig.RuntimeOption runtimeOption : ModCommonConfig.RuntimeOption.values())
        {
            if (runtimeOption.getSection() != section)
                continue;
            ConfigOptionFactory.Option option = ConfigOptionFactory.option(commonTarget, runtimeOption.getConfigValue().get());
            if (option != null)
                options.add(option);
        }

        addOptions(options);
        commonOptions.addAll(options);
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
            addOptions(section.options());
        }
    }

    private void addOptions(List<ConfigOptionFactory.Option> options)
    {
        list.addOptions(options);
        for (ConfigOptionFactory.Option option : options)
            defaults.add(option.target(), option.value());
    }

    private void lockCommonOptions()
    {
        // Their tooltips already carry the locked note, next to the name and value they still show
        for (ConfigOptionFactory.Option option : commonOptions)
        {
            AbstractWidget widget = list.findWidget(option);
            if (widget != null)
                widget.active = false;
        }
    }

    private void addFooterButtons()
    {
        int restoreWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        restoreDefaultsButton = addRenderableWidget(Button.builder(restoreDefaultsCaption(defaults.changedCount()), button -> confirmRestoreDefaults())
            .bounds((width - restoreWidth) / 2, height - RESTORE_BUTTON_BOTTOM_MARGIN, restoreWidth, BUTTON_HEIGHT).build());
        updateRestoreDefaultsButton();

        if (mode != Mode.QUICK)
        {
            addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds((width - WIDE_BUTTON_WIDTH) / 2, height - BUTTON_BOTTOM_MARGIN, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT).build());
            return;
        }

        int left = width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.options.all_settings"), button -> minecraft.setScreen(new FlansSettingsHubScreen(this)))
            .bounds(left, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(left + BUTTON_WIDTH + BUTTON_GAP, height - BUTTON_BOTTOM_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void confirmRestoreDefaults()
    {
        if (minecraft == null || !defaults.canRestore())
            return;

        Component heading = restoreDefaultsCaption(defaults.changedCount());
        var warning = Component.translatable("gui.flansmodultimate.options.restore_defaults.warning", title);
        if (!commonOptions.isEmpty() && minecraft.level != null)
            warning.append("\n\n").append(Component.translatable("gui.flansmodultimate.options.restore_defaults.server_warning"));

        Minecraft client = minecraft;
        client.setScreen(new ConfirmScreen(confirmed ->
        {
            if (confirmed)
                defaults.restore();
            client.setScreen(this);
        }, heading, warning, heading, CommonComponents.GUI_CANCEL));
    }

    private Component restoreDefaultsCaption(int count)
    {
        return Component.translatable("gui.flansmodultimate.options.restore_defaults").append(" (" + count + ")");
    }

    private void updateRestoreDefaultsButton()
    {
        if (restoreDefaultsButton != null)
        {
            int count = defaults.changedCount();
            restoreDefaultsButton.setMessage(restoreDefaultsCaption(count));
            restoreDefaultsButton.active = count > 0;
        }
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
    public void tick()
    {
        list.tick();
        updateRestoreDefaultsButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        settingsTooltip.beginFrame();
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_TOP, 0xFFFFFF);

        if (mode == Mode.COMMON || mode == Mode.SERVER || mode == Mode.SERVER_SOUNDS)
            graphics.drawCenteredString(font, footerNote(), width / 2, height - RESTORE_BUTTON_BOTTOM_MARGIN - 12, NOTE_COLOUR);

        super.render(graphics, mouseX, mouseY, partialTick);
        settingsTooltip.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public void setTooltipForNextRenderPass(@NotNull List<FormattedCharSequence> tooltip, @NotNull ClientTooltipPositioner positioner, boolean override)
    {
        settingsTooltip.queue(tooltip, positioner, override);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (hasShiftDown() && settingsTooltip.scroll(delta))
            return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** Says whose settings these are, because it decides whether they can be changed at all. */
    private Component footerNote()
    {
        if (!commonTarget.ready())
            return Component.translatable("gui.flansmodultimate.options.common_waiting");

        if (minecraft != null && minecraft.level == null)
            return Component.translatable("gui.flansmodultimate.options.common_local");

        return Component.translatable(commonTarget.editable() ? "gui.flansmodultimate.options.common_server" : "gui.flansmodultimate.options.common_server_read_only");
    }

    @Override
    public void removed()
    {
        if (list != null)
            scrollAmount = list.getScrollAmount();
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
