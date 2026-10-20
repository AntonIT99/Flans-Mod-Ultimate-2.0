package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcTacticChoices;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom NPCs' AI page. While a tactic of the mod's own moves the NPC, the tactical variant button shows that tactic and
 * cannot be changed; the variant stays saved for when the tactic goes back to Custom NPCs'.
 */
// The mixin extends the target's superclass to reach its buttons, so it inherits Custom NPCs' screen hierarchy.
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcAI.class, remap = false)
public abstract class GuiNpcAIMixin extends GuiNPCInterface2
{
    /** The tactical variant button of the page. */
    @Unique
    private static final int WOLFFSMODNPCS_TACTIC_BUTTON = 17;

    protected GuiNpcAIMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsShowTactic(CallbackInfo callback)
    {
        GuiButtonNop button = getButton(WOLFFSMODNPCS_TACTIC_BUTTON);
        if (button == null)
            return;
        NpcTacticChoices.replacing(npc).ifPresent(tactic ->
        {
            button.setDisplayText(NpcTacticChoices.key(tactic));
            button.setEnabled(false);
        });
    }
}
