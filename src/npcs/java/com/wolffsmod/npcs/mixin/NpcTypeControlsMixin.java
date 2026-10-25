package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcTypeControls;
import com.wolffsmod.npcs.client.RangedControlLocks;
import noppes.npcs.client.gui.SubGuiNpcMeleeProperties;
import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.client.gui.advanced.GuiNPCSoundsMenu;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Locks mapped editor controls and removes the Stats screen's obsolete Peaceful combat warnings. */
// The mixin becomes each target at runtime, so the Object cast and target check are required.
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference", "java:S1905", "ConstantValue"})
@Mixin(value = {GuiNpcStats.class, GuiNPCSoundsMenu.class, SubGuiNpcMeleeProperties.class, SubGuiNpcMovement.class}, remap = false)
public abstract class NpcTypeControlsMixin extends GuiBasic
{
    @Unique
    private RangedControlLocks wolffsmodnpcsInheritedLocks;

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsDisplayType(CallbackInfo callback)
    {
        if ((Object) this instanceof GuiNpcStats)
        {
            wolffsmodnpcsRestoreCombatLabel(5, "stats.meleeproperties", "guihint.npcmeleeprops");
            wolffsmodnpcsRestoreCombatLabel(6, "stats.rangedproperties", "guihint.npcrangedprops");
        }
        wolffsmodnpcsInheritedLocks = NpcTypeControls.apply(this);
    }

    @Unique
    private void wolffsmodnpcsRestoreCombatLabel(int id, String title, String hint)
    {
        GuiLabel label = getLabel(id);
        label.setMessage(Component.translatable(title));
        label.setTooltip(Tooltip.create(Component.translatable(hint).withStyle(style -> style.withColor(16762460))));
        label.setWidth(font.width(label.getMessage()));
    }

    @Inject(method = "unFocused", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsRetainDefault(GuiTextFieldNop field, CallbackInfo callback)
    {
        if (wolffsmodnpcsInheritedLocks != null && wolffsmodnpcsInheritedLocks.textFields().containsKey(field.id))
            callback.cancel();
    }

    @Inject(method = "buttonEvent", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsRetainSelection(GuiButtonNop button, CallbackInfo callback)
    {
        if (wolffsmodnpcsInheritedLocks != null && wolffsmodnpcsInheritedLocks.buttons().containsKey(button.id))
            callback.cancel();
    }
}
