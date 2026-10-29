package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.FactionNameScroll;
import com.wolffsmod.npcs.names.FactionDisplayName;
import com.wolffsmod.npcs.teams.FactionMobSettings;
import noppes.npcs.client.gui.global.GuiNPCManageFactions;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;

/**
 * Custom NPCs' Global Factions page.
 *
 * <ul>
 * <li>Adds the faction's saved Attack Mobs toggle below Attacked by mobs.</li>
 * <li>Shows team factions by their translated names in the faction and hostile lists and the Name field; an untouched
 * translation in the Name field keeps the stored name, so the faction is not renamed.</li>
 * </ul>
 */
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"}) // Uses the target's screen hierarchy and controls.
@Mixin(value = GuiNPCManageFactions.class, remap = false)
public abstract class GuiNPCManageFactionsMixin extends GuiNPCInterface2
{
    @Unique
    private static final int WOLFFSMODNPCS_NAME_FIELD = 0;

    @Shadow
    private Faction faction;

    protected GuiNPCManageFactionsMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    // --- Attack Mobs ---

    @Inject(method = "init", at = @At("TAIL"))
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

    // --- Translated team names ---

    // Mixin matches a NEW by its type and then looks forward for the first matching constructor, so without the ordinals the
    // hostile list's three-argument descriptor also matches the faction list's NEW and one of the two redirects is dropped.
    @Redirect(method = "init", at = @At(value = "NEW", target = "(Lnet/minecraft/client/gui/screens/Screen;I)Lnoppes/npcs/shared/client/gui/components/GuiCustomScrollNop;", ordinal = 0))
    private GuiCustomScrollNop wolffsmodnpcsFactionScroll(Screen parent, int id)
    {
        return new FactionNameScroll(parent, id);
    }

    // IntelliJ filters the candidates by descriptor before the ordinal and so sees only one; Mixin counts the ordinal over
    // every NEW of the type first, where 1 is the hostile list.
    @SuppressWarnings("MixinAnnotationTarget")
    @Redirect(method = "init", at = @At(value = "NEW", target = "(Lnet/minecraft/client/gui/screens/Screen;IZ)Lnoppes/npcs/shared/client/gui/components/GuiCustomScrollNop;", ordinal = 1))
    private GuiCustomScrollNop wolffsmodnpcsHostileScroll(Screen parent, int id, boolean multipleSelection)
    {
        return new FactionNameScroll(parent, id, multipleSelection);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void wolffsmodnpcsShowTranslatedName(CallbackInfo callback)
    {
        wolffsmodnpcsShowTranslated();
    }

    @Inject(method = "unFocused", at = @At("HEAD"))
    private void wolffsmodnpcsKeepStoredName(GuiTextFieldNop textfield, CallbackInfo callback)
    {
        if (textfield.id == WOLFFSMODNPCS_NAME_FIELD && faction.id != -1 && textfield.getValue().equals(FactionDisplayName.of(faction).getString()))
            textfield.setValue(faction.name);
    }

    @Inject(method = "unFocused", at = @At("TAIL"))
    private void wolffsmodnpcsReshowTranslatedName(GuiTextFieldNop textfield, CallbackInfo callback)
    {
        if (textfield.id == WOLFFSMODNPCS_NAME_FIELD)
            wolffsmodnpcsShowTranslated();
    }

    @Unique
    private void wolffsmodnpcsShowTranslated()
    {
        GuiTextFieldNop field = getTextField(WOLFFSMODNPCS_NAME_FIELD);
        if (field != null && faction.id != -1)
            field.setValue(FactionDisplayName.of(faction).getString());
    }
}
