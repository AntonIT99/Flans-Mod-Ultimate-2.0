package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout.Equipment;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.entity.data.DataInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Shows the model screens' preview of a Flan soldier in its class loadout, so a builder sees what the loadout button
 * gives before pressing it. The loadout is only worn while the preview is drawn: the NPC keeps its own equipment, on
 * this client as on the server. Render thread.
 */
@SuppressWarnings("unused") // Reached only from the model preview mixin, which IntelliJ does not follow.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanSoldierPreview
{
    /** The loadout last drawn, kept so that its items are not rebuilt every frame. */
    @Nullable
    private static FlanSoldierSelection shown;
    @Nullable
    private static Equipment equipment;

    /**
     * Draws the preview wearing the loadout of the class selected for the NPC, or as it is without one.
     *
     * @param draw
     *            draws the screen's NPC
     */
    public static void draw(GuiCreationScreenInterface screen, Runnable draw)
    {
        Optional<FlanSoldierSelection> selection = FlanSoldierSelection.read(screen.playerdata.extra);
        if (selection.isEmpty())
        {
            draw.run();
            return;
        }
        Equipment loadout = equipment(selection.get());
        // A human model that stands in for the NPC shares this inventory, so both wear the loadout.
        DataInventory inventory = screen.npc.inventory;
        Map<Integer, IItemStack> armour = inventory.armor;
        Map<Integer, IItemStack> weapons = inventory.weapons;
        inventory.armor = loadout.armour();
        inventory.weapons = loadout.weapons();
        try
        {
            draw.run();
        }
        finally
        {
            inventory.armor = armour;
            inventory.weapons = weapons;
        }
    }

    private static Equipment equipment(FlanSoldierSelection selection)
    {
        Equipment cached = equipment;
        if (cached == null || !selection.equals(shown))
        {
            cached = FlanSoldierLoadout.equipment(selection);
            shown = selection;
            equipment = cached;
        }
        return cached;
    }
}
