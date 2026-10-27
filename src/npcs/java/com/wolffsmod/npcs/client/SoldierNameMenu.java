package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.names.NpcDisplayNames;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.soldier.SoldierCloneNames;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.*;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

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
        imageHeight = 236;
        setBackground("menubg.png");
    }

    @Override
    public void init()
    {
        super.init();
        addLabel(new GuiLabel(0, PREFIX + "menu", guiLeft + 10, guiTop + 10));
        addMirror(6, "display_name", 30, NpcDisplayNames.name(picker.npc.display).getString());
        addMirror(7, "display_title", 56, NpcDisplayNames.title(picker.npc.display).getString());
        String[] labels = {"class_name", "class_title", "real_name"};
        for (int row = 0; row < labels.length; row++)
        {
            int y = guiTop + 90 + row * 26;
            Tooltip help = Tooltip.create(Component.translatable(PREFIX + labels[row] + ".help"));
            GuiLabel label = new GuiLabel(row + 1, PREFIX + labels[row], guiLeft + 10, y + 5);
            label.setTooltip(help);
            addLabel(label);
            GuiButtonNop apply = new GuiButtonNop(this, row, guiLeft + 284, y, 66, 20, "wolffsmodnpcs.preset.apply");
            apply.setTooltip(help);
            addButton(apply);
        }
        addButton(new GuiButtonNop(this, 3, guiLeft + 166, guiTop + 142, 112, 20, button ->
        {
            picker.playerdata.extra.putInt(SoldierCloneNames.GENDER, ((GuiButtonNop) button).getValue() + 1);
            picker.save();
        }, SoldierCloneNames.selectedGender(picker.playerdata.extra) - 1, PREFIX + "male", PREFIX + "female"));
        addButton(new GuiButtonNop(this, 4, guiLeft + 10, guiTop + 172, 340, 20, button ->
        {
            picker.playerdata.extra.putBoolean(SoldierCloneNames.REGENERATE, ((GuiButtonNop) button).getValue() == 1);
            picker.save();
        }, picker.playerdata.extra.getBoolean(SoldierCloneNames.REGENERATE) ? 1 : 0, PREFIX + "keep_clone", PREFIX + "regenerate_clone"));
        addButton(new GuiButtonNop(this, 5, guiLeft + 250, guiTop + 206, 100, 20, "gui.done"));
        getButton(3).setTooltip(Tooltip.create(Component.translatable(PREFIX + "gender.help")));
        getButton(4).setTooltip(Tooltip.create(Component.translatable(PREFIX + "clone.help")));
        getButton(5).setTooltip(Tooltip.create(Component.translatable(PREFIX + "done.help")));
    }

    private void addMirror(int id, String key, int top, String value)
    {
        Tooltip help = Tooltip.create(Component.translatable(PREFIX + key + ".help"));
        GuiLabel label = new GuiLabel(id, PREFIX + key, guiLeft + 10, guiTop + top + 5);
        label.setTooltip(help);
        addLabel(label);
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, guiLeft + 72, guiTop + top, 278, 20, "");
        field.setMaxLength(32767);
        field.setEditable(false);
        field.setTextColorUneditable(0xE0E0E0);
        field.setTooltip(help);
        addTextField(field);
        updateMirror(id, value);
    }

    @Override
    public void tick()
    {
        super.tick();
        updateMirrors();
    }

    private void updateMirrors()
    {
        updateMirror(6, NpcDisplayNames.name(picker.npc.display).getString());
        updateMirror(7, NpcDisplayNames.title(picker.npc.display).getString());
    }

    private void updateMirror(int id, String value)
    {
        GuiTextFieldNop field = getTextField(id);
        if (field != null && !field.getValue().equals(value))
        {
            field.setValue(value);
            field.moveCursorToStart();
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        switch (button.id)
        {
            case 0 -> {
                NpcDisplayNames.bindName(picker.npc.display, selection.playerClass());
                picker.save();
            }
            case 1 -> {
                NpcDisplayNames.bindTitle(picker.npc.display, selection.playerClass());
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
        updateMirrors();
    }
}
