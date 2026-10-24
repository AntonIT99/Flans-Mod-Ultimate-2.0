package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcTacticChoices;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom NPCs' AI page. While a tactic of the mod's own moves the NPC, the tactical variant button shows that tactic and
 * cannot be changed; the variant stays saved for when the tactic goes back to Custom NPCs'. Restores the Can Sprint
 * switch, using the AI page's existing save path.
 */
// The mixin extends the target's superclass to reach its buttons, so it inherits Custom NPCs' screen hierarchy.
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcAI.class, remap = false)
public abstract class GuiNpcAIMixin extends GuiNPCInterface2
{
    /** The tactical variant button of the page. */
    @Unique
    private static final int WOLFFSMODNPCS_TACTIC_BUTTON = 17;
    /** The unused row below Mount Control, alongside Must See Target. */
    @Unique
    private static final int WOLFFSMODNPCS_SPRINT_BUTTON = 24;

    protected GuiNpcAIMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsInitControls(CallbackInfo callback)
    {
        addLabel(new GuiLabel(WOLFFSMODNPCS_SPRINT_BUTTON, "ai.sprint", guiLeft + 150, guiTop + 115));
        addButton(new GuiButtonYesNo(this, WOLFFSMODNPCS_SPRINT_BUTTON, guiLeft + 230, guiTop + 110, 60, 20, npc.ais.canSprint));
        GuiButtonNop button = getButton(WOLFFSMODNPCS_TACTIC_BUTTON);
        if (button == null)
            return;
        NpcTacticChoices.replacing(npc).ifPresent(tactic ->
        {
            button.setDisplayText(NpcTacticChoices.key(tactic));
            button.setEnabled(false);
        });
    }

    @Inject(method = "buttonEvent", at = @At("TAIL"))
    private void wolffsmodnpcsSetSprint(GuiButtonNop button, CallbackInfo callback)
    {
        if (button.id == WOLFFSMODNPCS_SPRINT_BUTTON)
            npc.ais.canSprint = ((GuiButtonYesNo) button).getBoolean();
    }
}
