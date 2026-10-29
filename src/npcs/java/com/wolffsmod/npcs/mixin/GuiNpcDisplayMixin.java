package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.ContentNameBinding;
import com.wolffsmod.npcs.names.NpcDisplayNames;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom NPCs' Display page. The Name and Title fields show the translated team or class name of a bound name, and an
 * untouched translation is saved back as the bound literal so the binding survives the edit.
 */
// The mixin extends the target's superclass to reach its text fields, so it inherits Custom NPCs' screen hierarchy.
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcDisplay.class, remap = false)
public abstract class GuiNpcDisplayMixin extends GuiNPCInterface2
{
    @Unique
    private static final int WOLFFSMODNPCS_NAME_FIELD = 0;
    @Unique
    private static final int WOLFFSMODNPCS_TITLE_FIELD = 11;

    protected GuiNpcDisplayMixin(EntityNPCInterface npc)
    {
        super(npc);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void wolffsmodnpcsShowTranslatedNames(CallbackInfo callback)
    {
        wolffsmodnpcsShowTranslated(WOLFFSMODNPCS_NAME_FIELD);
        wolffsmodnpcsShowTranslated(WOLFFSMODNPCS_TITLE_FIELD);
    }

    @Inject(method = "unFocused", at = @At("HEAD"))
    private void wolffsmodnpcsRestoreBoundLiteral(GuiTextFieldNop textfield, CallbackInfo callback)
    {
        ContentNameBinding binding = wolffsmodnpcsBinding(textfield.id);
        if (binding != null && textfield.getValue().equals(wolffsmodnpcsTranslated(textfield.id)))
            textfield.setValue(binding.literal());
    }

    @Inject(method = "unFocused", at = @At("TAIL"))
    private void wolffsmodnpcsReshowTranslated(GuiTextFieldNop textfield, CallbackInfo callback)
    {
        wolffsmodnpcsShowTranslated(textfield.id);
    }

    @Unique
    private void wolffsmodnpcsShowTranslated(int id)
    {
        GuiTextFieldNop field = getTextField(id);
        if (field != null && wolffsmodnpcsBinding(id) != null)
            field.setValue(wolffsmodnpcsTranslated(id));
    }

    /** The active binding of the Name or Title field; null for other fields or literal text. */
    @Unique
    @Nullable
    private ContentNameBinding wolffsmodnpcsBinding(int id)
    {
        NpcDisplayNames names = NpcDisplayNames.of(npc.display);
        if (id == WOLFFSMODNPCS_NAME_FIELD)
            return names.getName();
        return id == WOLFFSMODNPCS_TITLE_FIELD ? names.getTitle() : null;
    }

    @Unique
    private String wolffsmodnpcsTranslated(int id)
    {
        return (id == WOLFFSMODNPCS_NAME_FIELD ? NpcDisplayNames.name(npc.display) : NpcDisplayNames.title(npc.display)).getString();
    }
}
