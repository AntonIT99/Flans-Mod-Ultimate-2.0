package com.wolffsmod.npcs.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.Set;

/** A cancellable choice of seats to copy to; the parent applies the choice only after this window closes. */
final class CrewCopyMenu extends GuiBasic
{
    private static final String PREFIX = "wolffsmodnpcs.crew.";
    private static final int CLOSE = 4;
    private final Set<Target> enabled;
    @Nullable
    private Target selected;

    @Getter
    @RequiredArgsConstructor
    enum Target
    {
        NEXT("copy_next"), DRIVER("copy_driver"), GUNNERS("copy_gunner"), PASSENGERS("copy_passenger");

        private final String key;
    }

    CrewCopyMenu(Set<Target> enabled)
    {
        this.enabled = Set.copyOf(enabled);
        imageWidth = 256;
        imageHeight = 150;
        setBackground("menubg.png");
    }

    @Override
    public void init()
    {
        super.init();
        addLabel(new GuiLabel(0, PREFIX + "copy_to", guiLeft + 8, guiTop + 8));
        for (Target target : Target.values())
        {
            GuiButtonNop button = new GuiButtonNop(this, target.ordinal(), guiLeft + 8, guiTop + 26 + target.ordinal() * 23, 240, 20, PREFIX + target.getKey());
            button.setEnabled(enabled.contains(target));
            button.setTooltip(Tooltip.create(Component.translatable(PREFIX + target.getKey() + ".help")));
            addButton(button);
        }
        addButton(new GuiButtonNop(this, CLOSE, guiLeft + 168, guiTop + 122, 80, 20, "gui.close"));
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (button.id == CLOSE)
            close();
        else if (button.id >= 0 && button.id < Target.values().length && enabled.contains(Target.values()[button.id]))
        {
            selected = Target.values()[button.id];
            close();
        }
    }

    @Nullable
    Target selection()
    {
        return selected;
    }
}
