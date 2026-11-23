package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcSpawnerRange;
import com.wolffsmod.npcs.sim.SpawnerRanges;
import noppes.npcs.client.gui.roles.GuiNpcSpawner;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobSpawner;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

/**
 * Custom NPCs' Spawner job page. Adds the spawner's own roam range for spawned creatures, vehicles and aircraft beside the
 * first slots, each left at 0 to take the server default the hint below them names.
 */
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"}) // Uses the target's screen hierarchy and controls.
@Mixin(value = GuiNpcSpawner.class, remap = false)
public abstract class GuiNpcSpawnerMixin extends GuiNPCInterface2
{
    /** First text field and label id of the ranges, one per kind, clear of the page's own ids up to 26. */
    @Unique
    private static final int WOLFFSMODNPCS_RANGE_ID = 40;
    @Unique
    private static final int WOLFFSMODNPCS_HINT_ID = 50;

    @Shadow
    private JobSpawner job;

    protected GuiNpcSpawnerMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    // --- Roam range ---

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsAddRanges(CallbackInfo callback)
    {
        SpawnerRanges ranges = (SpawnerRanges) job;
        int y = guiTop + 6;
        for (NpcSpawnerRange.Kind kind : NpcSpawnerRange.Kind.values())
        {
            int id = WOLFFSMODNPCS_RANGE_ID + kind.ordinal();
            addLabel(new GuiLabel(id, "wolffsmodnpcs.spawner.range." + kind.name().toLowerCase(Locale.ROOT), guiLeft + 258, y + 5));
            GuiTextFieldNop field = new GuiTextFieldNop(id, this, guiLeft + 360, y, 50, 20, String.valueOf(ranges.wolffsmodnpcsSpawnerRange(kind)));
            field.numbersOnly = true;
            field.setMinMaxDefault(0, NpcSpawnerRange.MAX, 0);
            addTextField(field);
            y += 23;
        }
        addLabel(new GuiLabel(WOLFFSMODNPCS_HINT_ID, I18n.get("wolffsmodnpcs.spawner.range.hint", NpcSpawnerRange.Kind.GENERAL.serverDefault(),
            NpcSpawnerRange.Kind.VEHICLE.serverDefault(), NpcSpawnerRange.Kind.AIRCRAFT.serverDefault()), guiLeft + 258, y + 5));
    }

    @Inject(method = "unFocused", at = @At("TAIL"))
    private void wolffsmodnpcsSetRange(GuiTextFieldNop textfield, CallbackInfo callback)
    {
        int index = textfield.id - WOLFFSMODNPCS_RANGE_ID;
        NpcSpawnerRange.Kind[] kinds = NpcSpawnerRange.Kind.values();
        if (index >= 0 && index < kinds.length)
            ((SpawnerRanges) job).wolffsmodnpcsSetSpawnerRange(kinds[index], textfield.getInteger());
    }
}
