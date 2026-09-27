package com.wolffsmod.npcs.client;

import noppes.npcs.client.layer.LayerHeadwear;
import noppes.npcs.client.model.ModelPlayer64x32;
import noppes.npcs.client.renderer.RenderCustomNpc;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import java.util.List;

/** Custom NPCs renderer with both the base mesh and pixel headwear mapped to 64x32. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class LegacyNpc64x32Renderer extends RenderCustomNpc
{
    public LegacyNpc64x32Renderer(EntityRendererProvider.Context context)
    {
        super(context, new ModelPlayer64x32(LegacyPlayer64x32Mesh.bake()));

        // Custom NPCs keeps a second copy of the layer list when it borrows another entity model.
        LegacyHeadwearLayer64x32 headwear = new LegacyHeadwearLayer64x32(this);
        replaceHeadwear(this.layers, headwear);
        replaceHeadwear(this.npclayers, headwear);
    }

    private static void replaceHeadwear(List layers, LegacyHeadwearLayer64x32 replacement)
    {
        for (int i = 0; i < layers.size(); i++)
        {
            if (layers.get(i) instanceof LayerHeadwear)
            {
                layers.set(i, replacement);
                return;
            }
        }
        layers.add(replacement);
    }
}
