package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.FactionDisplayName;
import noppes.npcs.client.gui.advanced.GuiNPCFactionSetup;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Custom NPCs' faction setup page. Lists factions by their translated team names, keeping each label mapped to the
 * faction id the page sends when one is picked.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = GuiNPCFactionSetup.class, remap = false)
public abstract class GuiNPCFactionSetupMixin
{
    @Inject(method = "setData", at = @At("HEAD"))
    private void wolffsmodnpcsTranslateFactions(Vector<String> list, Map<String, Integer> data, CallbackInfo callback)
    {
        Map<String, Integer> labelled = new HashMap<>();
        list.clear();
        data.forEach((name, id) ->
        {
            String label = wolffsmodnpcsLabel(name, id, labelled);
            list.add(label);
            labelled.put(label, id);
        });
        list.sort(String.CASE_INSENSITIVE_ORDER);
        data.clear();
        data.putAll(labelled);
    }

    /** Selects the NPC's own faction by the same label the list shows. */
    @ModifyVariable(method = "setData", at = @At("STORE"), ordinal = 0)
    private String wolffsmodnpcsTranslateSelected(String name)
    {
        Faction faction = name == null ? null : FactionController.instance.getFactionFromName(name);
        return faction == null ? name : FactionDisplayName.of(faction).getString();
    }

    /** The translated name of a faction, or its stored name when that would collide with another faction's label. */
    @Unique
    private static String wolffsmodnpcsLabel(String name, int id, Map<String, Integer> taken)
    {
        Faction faction = FactionController.instance.getFaction(id);
        String label = faction == null ? name : FactionDisplayName.of(faction).getString();
        return taken.containsKey(label) ? name : label;
    }
}
