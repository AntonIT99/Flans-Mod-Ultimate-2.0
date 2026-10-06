package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

/** Per-NPC weapon switches using the existing Custom NPCs stats menu transport. */
// Custom NPCs' editor and tab base classes require the external GUI inheritance chain.
@SuppressWarnings("java:S110")
public class GuiWolffsMod extends GuiNPCInterface2 implements IGuiData
{
    public static final int MENU_ID = 100;
    private boolean loaded;
    private boolean changed;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
    }

    @Override
    public void init()
    {
        super.init();
        addLabel(new GuiLabel(100, "wolffsmodnpcs.weapons.title", guiLeft + 8, guiTop + 10));
        Feature[] features = Feature.values();
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        for (int i = 0; i < features.length; i++)
        {
            int x = guiLeft + 8 + i / 8 * 206;
            int y = guiTop + 29 + i % 8 * 21;
            String key = "wolffsmodnpcs.weapons." + features[i].name().toLowerCase(Locale.ROOT);
            addLabel(new GuiLabel(i, key, x, y + 5, key + ".help"));
            GuiButtonNop button = new GuiButtonNop(this, i, x + 148, y, 48, 20, new String[]{"gui.no", "gui.yes"}, options.enabled(features[i]) ? 1 : 0);
            button.setEnabled(loaded);
            addButton(button);
        }
        addLabel(new GuiLabel(101, loaded ? "wolffsmodnpcs.weapons.ammo_hint" : "wolffsmodnpcs.weapons.loading", guiLeft + 8, guiTop + 203));
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (!loaded || button.id < 0 || button.id >= Feature.values().length)
            return;
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.values()[button.id], button.getValue() == 1);
        changed = true;
    }

    @Override
    public void setGuiData(CompoundTag tag)
    {
        npc.stats.readToNBT(tag);
        loaded = true;
        init();
    }

    public static GuiMenuTopButton createTab(IGuiInterface parent, EntityNPCInterface npc, int x, int y)
    {
        return new GuiMenuTopButton(parent, MENU_ID, x, y, "menu.wolffsmodnpcs")
        {
            @Override
            public void onClick(double mouseX, double mouseY)
            {
                GuiTextFieldNop.unfocus();
                parent.save();
                NoppesUtil.openGUI(Minecraft.getInstance().player, new GuiWolffsMod(npc));
            }
        };
    }

    @Override
    public void save()
    {
        if (loaded && changed)
        {
            Packets.sendServer(new SPacketMenuSave(EnumMenuType.STATS, npc.stats.save(new CompoundTag())));
            changed = false;
        }
    }
}
