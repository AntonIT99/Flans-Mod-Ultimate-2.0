package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.FlanModelMenuPreview;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.client.gui.mainmenu.GuiNPCInv;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.GuiGraphics;

/** Fits the inventory tab's preview of an NPC wearing a Flan model between the equipment slots, which it otherwise covers. */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = GuiNPCInv.class, remap = false)
public abstract class GuiNPCInvMixin
{
    /** The free area between the armour slots (x 9) and the weapon slots (x 81), relative to the screen's corner. */
    private static final int WOLFFSMODNPCS_PREVIEW_LEFT = 27;
    private static final int WOLFFSMODNPCS_PREVIEW_TOP = 14;
    private static final int WOLFFSMODNPCS_PREVIEW_RIGHT = 80;
    private static final int WOLFFSMODNPCS_PREVIEW_BOTTOM = 96;

    @Redirect(method = {"render", "m_88315_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/client/gui/mainmenu/GuiNPCInv;drawNpc(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void wolffsmodnpcsFitPreview(GuiNPCInv screen, GuiGraphics graphics, int x, int y)
    {
        if (screen.npc instanceof EntityCustomNpc npc && npc.modelData.getEntity(npc) instanceof FlanModelEntity model)
            FlanModelMenuPreview.render(graphics, model, npc.display.getSkinTexture(), 0, screen.guiLeft + WOLFFSMODNPCS_PREVIEW_LEFT, screen.guiTop + WOLFFSMODNPCS_PREVIEW_TOP,
                screen.guiLeft + WOLFFSMODNPCS_PREVIEW_RIGHT, screen.guiTop + WOLFFSMODNPCS_PREVIEW_BOTTOM);
        else
            screen.drawNpc(graphics, x, y);
    }
}
