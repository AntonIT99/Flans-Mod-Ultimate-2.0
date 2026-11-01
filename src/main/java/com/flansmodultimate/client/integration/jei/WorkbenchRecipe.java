package com.flansmodultimate.client.integration.jei;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.util.ModUtils;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A read-only view of the same costs the server's custom workbenches consume. */
public record WorkbenchRecipe(ItemStack output, List<ItemStack> materials, List<ItemStack> engines, int engineCount, ItemStack station)
{
    public static WorkbenchRecipe driveable(DriveableType type, ItemStack station, List<PartType> parts)
    {
        int count = Math.max(0, type.numEngines());
        List<ItemStack> engines = count == 0
            ? List.of()
            : parts.stream().filter(part -> part.getCategory() == PartType.Category.ENGINE && part.worksWith(type.getType())).sorted(Comparator.comparing(PartType::getShortName))
                .map(part -> ModUtils.getItemStack(part, count).orElse(ItemStack.EMPTY)).filter(stack -> !stack.isEmpty()).toList();
        return new WorkbenchRecipe(ModUtils.getItemStack(type).orElse(ItemStack.EMPTY), combineMaterials(type.getDriveableRecipe()), engines, count, station);
    }

    public static WorkbenchRecipe gunBox(GunBoxType.GunBoxEntry entry, ItemStack station)
    {
        return new WorkbenchRecipe(ModUtils.getItemStack(entry.getType()).orElse(ItemStack.EMPTY), combineMaterials(entry.getRequiredParts()), List.of(), 0, station);
    }

    public static List<ItemStack> combineMaterials(List<ItemStack> materials)
    {
        List<ItemStack> combined = new ArrayList<>();
        for (ItemStack material : materials)
        {
            if (material.isEmpty())
                continue;
            ItemStack existing = combined.stream().filter(stack -> ItemStack.isSameItemSameTags(stack, material)).findFirst().orElse(null);
            if (existing == null)
                combined.add(material.copy());
            else
                existing.grow(material.getCount());
        }
        return List.copyOf(combined);
    }
}
