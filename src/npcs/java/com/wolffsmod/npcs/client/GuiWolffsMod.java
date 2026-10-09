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
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

/** Per-NPC weapon switches, grouped into submenus, using the existing Custom NPCs stats menu transport. */
// Custom NPCs' editor and tab base classes require the external GUI inheritance chain.
@SuppressWarnings("java:S110")
public class GuiWolffsMod extends GuiNPCInterface2 implements IGuiData
{
    public static final int MENU_ID = 100;
    private static final int CATEGORY_BUTTON_ID = 200;
    private static final int BACK_BUTTON_ID = 300;

    /** Submenus of the Wolff's Mod tab; every feature belongs to exactly one. */
    private enum Category
    {
        EQUIPMENT(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.TYPE_PROPERTIES, Feature.WEAPON_STATS), FIRING(Feature.PROJECTILES, Feature.GRENADES, Feature.MODEL_MUZZLES,
            Feature.ALTERNATE_BARRELS, Feature.SECONDARY_BANK), AIMING(Feature.LEAD_TARGET,
                Feature.BALLISTIC_AIM), ANIMATIONS(Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS), EFFECTS(Feature.FLAN_SOUNDS, Feature.SHOOT_PARTICLES);

        private final Feature[] features;

        Category(Feature... features)
        {
            this.features = features;
        }

        private String key()
        {
            return "wolffsmodnpcs.category." + name().toLowerCase(Locale.ROOT);
        }
    }

    private boolean loaded;
    private boolean changed;
    private Category category;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
    }

    @Override
    public void init()
    {
        super.init();
        if (category == null)
            initOverview();
        else
            initCategory(category);
        addLabel(new GuiLabel(101, loaded ? "wolffsmodnpcs.weapons.ammo_hint" : "wolffsmodnpcs.weapons.loading", guiLeft + 8, guiTop + 203));
    }

    private void initOverview()
    {
        addLabel(new GuiLabel(100, "wolffsmodnpcs.weapons.title", guiLeft + 8, guiTop + 10));
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++)
        {
            int y = guiTop + 29 + i * 24;
            String key = categories[i].key();
            GuiButtonNop button = new GuiButtonNop(this, CATEGORY_BUTTON_ID + i, guiLeft + 8, y, 120, 20, new String[]{key}, 0);
            button.setEnabled(loaded);
            addButton(button);
            addLabel(new GuiLabel(CATEGORY_BUTTON_ID + i, key + ".help", guiLeft + 136, y + 6));
        }
    }

    private void initCategory(Category shown)
    {
        addLabel(new GuiLabel(100, shown.key(), guiLeft + 8, guiTop + 10));
        addButton(new GuiButtonNop(this, BACK_BUTTON_ID, guiLeft + 354, guiTop + 4, 50, 20, new String[]{"gui.back"}, 0));
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        for (int i = 0; i < shown.features.length; i++)
        {
            Feature feature = shown.features[i];
            int x = guiLeft + 8;
            int y = guiTop + 29 + i * 21;
            String key = "wolffsmodnpcs.weapons." + feature.name().toLowerCase(Locale.ROOT);
            addLabel(new GuiLabel(feature.ordinal(), key, x, y + 5, key + ".help"));
            GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), x + 148, y, 48, 20, new String[]{"gui.no", "gui.yes"}, options.enabled(feature) ? 1 : 0);
            button.setEnabled(loaded);
            addButton(button);
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (!loaded)
            return;
        if (button.id == BACK_BUTTON_ID)
        {
            category = null;
            init();
        }
        else if (button.id >= CATEGORY_BUTTON_ID && button.id < CATEGORY_BUTTON_ID + Category.values().length)
        {
            category = Category.values()[button.id - CATEGORY_BUTTON_ID];
            init();
        }
        else if (button.id >= 0 && button.id < Feature.values().length)
        {
            ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.values()[button.id], button.getValue() == 1);
            changed = true;
        }
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
