package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.names.FactionDisplayName;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Draws faction rows by their translated team names while the list keeps the stored names the page looks factions up by. */
public class FactionNameScroll extends GuiCustomScrollNop
{
    public FactionNameScroll(Screen parent, int id)
    {
        super(parent, id);
    }

    public FactionNameScroll(Screen parent, int id, boolean multipleSelection)
    {
        super(parent, id, multipleSelection);
    }

    @Override
    protected void drawItems(GuiGraphics graphics)
    {
        List<String> names = new ArrayList<>(list);
        list.replaceAll(FactionNameScroll::label);
        try
        {
            super.drawItems(graphics);
        }
        finally
        {
            list.clear();
            list.addAll(names);
        }
    }

    public static String label(String name)
    {
        Faction faction = FactionController.instance.getFactionFromName(name);
        return faction == null ? name : FactionDisplayName.of(faction).getString();
    }
}
