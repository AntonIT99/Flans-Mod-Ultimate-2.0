package com.flansmodultimate.client.integration.jei;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.eras.EraClientState;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.platform.registry.RegistryEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;

/** Hides from JEI what an era restriction blocks for the local player, as the creative tabs do. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EraJeiHiding
{
    private static final AtomicBoolean listening = new AtomicBoolean();
    private static final List<ItemStack> hidden = new ArrayList<>();
    @Nullable
    private static IIngredientManager ingredients;

    static void attach(IJeiRuntime runtime)
    {
        ingredients = runtime.getIngredientManager();
        hidden.clear();
        if (listening.compareAndSet(false, true))
            EraClientState.addListener(EraJeiHiding::refresh);
        refresh();
    }

    static void detach()
    {
        ingredients = null;
        hidden.clear();
    }

    private static void refresh()
    {
        IIngredientManager manager = ingredients;
        if (manager == null)
            return;
        if (!hidden.isEmpty())
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(hidden));
        hidden.clear();
        for (RegistryEntry<Item> entry : FlansMod.getItems())
        {
            if (entry.get() instanceof IFlanItem<?> item && !EraClientState.check(item.getConfigType()).allowed())
                hidden.add(new ItemStack(entry.get()));
        }
        if (!hidden.isEmpty())
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(hidden));
    }
}
