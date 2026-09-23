package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/** A slider whose handle and keyboard movement stop exactly at the named rendering presets. */
public final class RenderingPresetSlider extends AbstractSliderButton
{
    private static final int LAST_STOP = ModClientConfig.RenderPreset.CUSTOM.ordinal();
    private static final int TICK_COLOR = 0xFFB0B0B0;
    private static final int SELECTED_TICK_COLOR = 0xFFFFFF80;

    public enum Kind
    {
        LOD("lodPreset"),
        IMPOSTOR("impostorPreset");

        private final String key;

        Kind(String name)
        {
            key = "options.flansmodultimate." + name;
        }
    }

    private final Kind kind;
    private ModClientConfig.RenderPreset selected;

    public RenderingPresetSlider(int width, Kind kind)
    {
        super(0, 0, width, 20, CommonComponents.EMPTY, position(currentPreset(kind).ordinal()));
        this.kind = kind;
        selected = currentPreset(kind);
        updateMessage();
    }

    private static ModClientConfig.RenderPreset currentPreset(Kind kind)
    {
        return kind == Kind.LOD ? ModClientConfig.currentLodPreset() : ModClientConfig.currentImpostorPreset();
    }

    private static double position(int stop)
    {
        return (double) stop / LAST_STOP;
    }

    @Override
    protected void applyValue()
    {
        select(Mth.clamp((int) Math.round(value * LAST_STOP), 0, LAST_STOP));
    }

    private void select(int stop)
    {
        ModClientConfig.RenderPreset next = ModClientConfig.RenderPreset.at(stop);
        if (next == ModClientConfig.RenderPreset.CUSTOM && selected != next)
        {
            // Custom reports manual settings; dragging to it cannot manufacture them.
            value = position(selected.ordinal());
            return;
        }

        value = position(stop);
        if (selected == next)
            return;

        selected = next;
        if (next != ModClientConfig.RenderPreset.CUSTOM)
        {
            if (kind == Kind.LOD)
                ModClientConfig.applyLodPreset(next);
            else
                ModClientConfig.applyImpostorPreset(next);
        }
        updateMessage();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (active && isFocused() && (keyCode == 263 || keyCode == 262))
        {
            select(Mth.clamp(selected.ordinal() + (keyCode == 262 ? 1 : -1), 0, LAST_STOP));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void updateMessage()
    {
        Component caption = Component.translatable(kind.key);
        Component preset = Component.translatable("options.flansmodultimate.value.renderpreset."
            + selected.name().toLowerCase(Locale.ROOT));
        setMessage(Options.genericValueLabel(caption, preset));
        Component details = kind == Kind.LOD ? lodDetails() : impostorDetails();
        setTooltip(Tooltip.create(Component.translatable(kind.key + ".tooltip").append("\n\n").append(details)));
    }

    private Component lodDetails()
    {
        ModClientConfig.LodValues values = ModClientConfig.currentLodValues();
        return Component.translatable(kind.key + ".values",
            values.nearPixels(), values.farPixels(), values.detailMultiplier(),
            values.trackPixels(), values.groupedTrackPixels());
    }

    private Component impostorDetails()
    {
        ModClientConfig.ImpostorValues values = ModClientConfig.currentImpostorValues();
        return Component.translatable(kind.key + ".values",
            values.pixels(), values.minimumDistance(), values.maximumDistance(),
            values.qualityMultiplier(), values.resolution(), values.yawAngles());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        for (int stop = 0; stop <= LAST_STOP; stop++)
        {
            int x = getX() + 4 + (int) Math.round(position(stop) * (getWidth() - 8));
            graphics.fill(x, getY() + getHeight() - 3, x + 1, getY() + getHeight() - 1,
                stop == selected.ordinal() ? SELECTED_TICK_COLOR : TICK_COLOR);
        }
    }
}
