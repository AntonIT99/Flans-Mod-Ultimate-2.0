package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.soldier.SoldierCloneNames;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.*;

/** Explicit naming actions and per-NPC gender and clone preferences in the class model picker. */
final class SoldierNameMenu extends GuiBasic
{
    private static final String PREFIX = "wolffsmodnpcs.soldier.name.";
    private final GuiCreationScreenInterface picker;
    private final FlanSoldierSelection selection;

    SoldierNameMenu(GuiCreationScreenInterface picker, FlanSoldierSelection selection)
    {
        this.picker = picker;
        this.selection = selection;
        imageWidth = 360;
        imageHeight = 180;
        setBackground("menubg.png");
    }

    @Override
    public void init()
    {
        super.init();
        addLabel(new GuiLabel(0, PREFIX + "menu", guiLeft + 10, guiTop + 10));
        String[] labels = {"class_name", "class_title", "real_name"};
        for (int row = 0; row < labels.length; row++)
        {
            int y = guiTop + 34 + row * 26;
            addLabel(new GuiLabel(row + 1, PREFIX + labels[row], guiLeft + 10, y + 5));
            addButton(new GuiButtonNop(this, row, guiLeft + 284, y, 66, 20, "wolffsmodnpcs.preset.apply"));
        }
        addButton(new GuiButtonNop(this, 3, guiLeft + 166, guiTop + 86, 112, 20, button ->
        {
            picker.playerdata.extra.putInt(SoldierCloneNames.GENDER, ((GuiButtonNop) button).getValue() + 1);
            picker.save();
        }, SoldierCloneNames.selectedGender(picker.playerdata.extra) - 1, PREFIX + "male", PREFIX + "female"));
        addButton(new GuiButtonNop(this, 4, guiLeft + 10, guiTop + 116, 340, 20, button ->
        {
            picker.playerdata.extra.putBoolean(SoldierCloneNames.REGENERATE, ((GuiButtonNop) button).getValue() == 1);
            picker.save();
        }, picker.playerdata.extra.getBoolean(SoldierCloneNames.REGENERATE) ? 1 : 0, PREFIX + "keep_clone", PREFIX + "regenerate_clone"));
        addButton(new GuiButtonNop(this, 5, guiLeft + 250, guiTop + 150, 100, 20, "gui.done"));
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        switch (button.id)
        {
            case 0 -> {
                picker.npc.display.setName(selection.playerClass().getName());
                picker.save();
            }
            case 1 -> {
                picker.npc.display.setTitle(selection.playerClass().getName());
                picker.save();
            }
            case 2 -> {
                picker.save();
                NpcNetwork.sendToServer(SoldierNamePacket.request(picker.npc.getId(), selection, SoldierCloneNames.selectedGender(picker.playerdata.extra)));
            }
            case 5 -> close();
            default -> {
                // Selector buttons persist their values through their own callbacks.
            }
        }
    }
}
