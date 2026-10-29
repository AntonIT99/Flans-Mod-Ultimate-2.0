package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcTacticChoices;
import com.wolffsmod.npcs.tactics.CustomNpcTactics;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom NPCs' AI page. Restores the tactical variant and range controls the installed NeoForge build omits; while a
 * tactic of the mod's own moves the NPC, the variant button shows that tactic and cannot be changed, and the variant
 * stays saved for when the tactic goes back to Custom NPCs'. Restores the Can Sprint switch, using the AI page's
 * existing save path.
 */
// The mixin extends the target's superclass to reach its buttons, so it inherits Custom NPCs' screen hierarchy.
@SuppressWarnings({"java:S110", "DataFlowIssue", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcAI.class, remap = false)
public abstract class GuiNpcAIMixin extends GuiNPCInterface2
{
    @Unique
    private static final int WOLFFSMODNPCS_TACTIC_BUTTON = 17;
    @Unique
    private static final int WOLFFSMODNPCS_RANGE_FIELD = 3;
    @Unique
    private static final String[] WOLFFSMODNPCS_TACTICS = {"aitactics.rush", "aitactics.stagger", "aitactics.orbit", "aitactics.hitandrun", "aitactics.ambush", "aitactics.stalk", "gui.none"};
    /** The unused row below Mount Control, alongside Must See Target. */
    @Unique
    private static final int WOLFFSMODNPCS_SPRINT_BUTTON = 24;

    protected GuiNpcAIMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void wolffsmodnpcsInitControls(CallbackInfo callback)
    {
        addLabel(new GuiLabel(WOLFFSMODNPCS_SPRINT_BUTTON, "ai.sprint", guiLeft + 150, guiTop + 115));
        addButton(new GuiButtonYesNo(this, WOLFFSMODNPCS_SPRINT_BUTTON, guiLeft + 230, guiTop + 110, 60, 20, npc.ais.canSprint));
        CustomNpcTactics settings = CustomNpcTactics.of(npc);
        int variant = settings.getVariant();
        addLabel(new GuiLabel(19, "ai.tacticalvariant", guiLeft + 150, guiTop + 140));
        GuiButtonNop button = new GuiButtonNop(this, WOLFFSMODNPCS_TACTIC_BUTTON, guiLeft + 230, guiTop + 135, 60, 20, WOLFFSMODNPCS_TACTICS, variant);
        addButton(button);
        button.setEnabled(npc.ais.onAttack == 0);
        getButton(15).setEnabled(npc.ais.onAttack == 0);
        NpcTacticChoices.replacing(npc).ifPresent(tactic ->
        {
            button.setDisplayText(NpcTacticChoices.key(tactic));
            button.setEnabled(false);
        });
        if (variant == CustomNpcTactics.RUSH || variant == CustomNpcTactics.NONE)
            return;
        String label = switch (variant)
        {
            case CustomNpcTactics.SURROUND -> "gui.orbitdistance";
            case CustomNpcTactics.HIT_AND_RUN -> "gui.fightifthisclose";
            case CustomNpcTactics.AMBUSH, CustomNpcTactics.STALK -> "gui.ambushdistance";
            default -> "gui.engagedistance";
        };
        addLabel(new GuiLabel(21, label, guiLeft + 300, guiTop + 140));
        GuiTextFieldNop field = new GuiTextFieldNop(WOLFFSMODNPCS_RANGE_FIELD, (GuiNpcAI) (Object) this, guiLeft + 380, guiTop + 135, 30, 20, Integer.toString(settings.getRange()));
        field.numbersOnly = true;
        field.setMinMaxDefault(1, Math.max(1, npc.stats.aggroRange), 5);
        addTextField(field);
    }

    @Inject(method = "buttonEvent", at = @At("HEAD"))
    private void wolffsmodnpcsChooseTactic(GuiButtonNop button, CallbackInfo callback)
    {
        if (button.id != WOLFFSMODNPCS_TACTIC_BUTTON || npc.ais.onAttack != 0 || NpcTacticChoices.replacing(npc).isPresent())
            return;
        CustomNpcTactics settings = CustomNpcTactics.of(npc);
        settings.setVariant(button.getValue());
        if (settings.getVariant() == CustomNpcTactics.STALK)
            npc.ais.directLOS = false;
        init();
    }

    @Inject(method = "unFocused", at = @At("HEAD"))
    private void wolffsmodnpcsTacticRange(GuiTextFieldNop field, CallbackInfo callback)
    {
        if (field.id == WOLFFSMODNPCS_RANGE_FIELD)
            CustomNpcTactics.of(npc).setRange(field.getInteger());
    }

    @Inject(method = "buttonEvent", at = @At("TAIL"))
    private void wolffsmodnpcsSetSprint(GuiButtonNop button, CallbackInfo callback)
    {
        if (button.id == WOLFFSMODNPCS_SPRINT_BUTTON)
            npc.ais.canSprint = ((GuiButtonYesNo) button).getBoolean();
    }
}
