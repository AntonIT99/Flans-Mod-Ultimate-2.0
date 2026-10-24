package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.FactionMobSettings;
import noppes.npcs.client.gui.global.GuiNPCManageFactions;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the faction's saved Attack Mobs toggle below Attacked by mobs. */
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"}) // Uses the target's screen hierarchy and controls.
@Mixin(value = GuiNPCManageFactions.class, remap = false)
public abstract class GuiNPCManageFactionsMixin extends GuiNPCInterface2
{
    @Shadow
    private Faction faction;

    protected GuiNPCManageFactionsMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsAddAttackMobs(CallbackInfo callback)
    {
        if (faction.id == -1)
            return;
        addLabel(new GuiLabel(5, "wolffsmodnpcs.faction.attack_mobs", guiLeft + 8, guiTop + 119));
        addButton(new GuiButtonYesNo(this, 5, guiLeft + 100, guiTop + 114, 45, 20, ((FactionMobSettings) faction).wolffsmodnpcsAttackMobs()));
    }

    @Inject(method = "buttonEvent", at = @At("TAIL"))
    private void wolffsmodnpcsSetAttackMobs(GuiButtonNop button, CallbackInfo callback)
    {
        if (button.id == 5)
            ((FactionMobSettings) faction).wolffsmodnpcsSetAttackMobs(button.getValue() == 1);
    }
}
