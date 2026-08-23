package com.flansmodultimate;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreativeTabs
{
    public static final String TAB_GENERAL = "general";
    public static final String TAB_ARMORS = "armors";
    public static final String TAB_ATTACHMENTS = "attachments";
    public static final String TAB_GUNS = "guns";
    public static final String TAB_GRENADES = "grenades";
    public static final String TAB_TOOLS = "tools";
    public static final String TAB_BOMBS_AND_SHELLS = "bombs_and_shells";
    public static final String TAB_AA_GUNS = "aaguns";
    public static final String TAB_MECHAS = "mechas";
    public static final String TAB_PLANES = "planes";
    public static final String TAB_VEHICLES = "vehicles";
    public static final String TAB_PARTS = "parts";

    @SafeVarargs
    public static void registerCreativeTab(DeferredRegister<CreativeModeTab> creativeTabRegistry, String tabName, List<DeferredHolder<Item, ? extends Item>> itemsForTab, List<EnumType> typesForIcon, ResourceKey<CreativeModeTab> beforeTab, ResourceKey<CreativeModeTab>... afterTab)
    {
        creativeTabRegistry.register(tabName, () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab." + FlansMod.MOD_ID + "." + tabName))
            .icon(createIcon(tabName, itemsForTab, typesForIcon))
            .withSearchBar()
            .withTabsBefore(beforeTab)
            .withTabsAfter(afterTab)
            .displayItems(displayItemsWithPaintjobsGenerator(tabName, itemsForTab))
            .build());
    }

    private static Supplier<ItemStack> createIcon(String tabName, List<DeferredHolder<Item, ? extends Item>> itemsForTab, List<EnumType> typesForIcons)
    {
        return () -> {
            if (tabName.equals(TAB_GENERAL))
                return new ItemStack(FlansMod.gunWorkbenchItem.get());

            List<DeferredHolder<Item, ? extends Item>> itemsForIcon = itemsForTab.stream()
                .filter(ro -> {
                    for (EnumType type: typesForIcons) {
                        Class<?> itemClass = type.getItemClass();
                        if (itemClass != null && itemClass.isInstance(ro.get()))
                            return true;
                    }
                    return false;
                }).toList();

            if (itemsForIcon.isEmpty())
                return new ItemStack(Items.WHITE_WOOL);

            return new ItemStack(itemsForIcon.get(ThreadLocalRandom.current().nextInt(0, itemsForIcon.size())).get());
        };
    }

    private static CreativeModeTab.DisplayItemsGenerator displayItemsWithPaintjobsGenerator(String tabName, List<DeferredHolder<Item, ? extends Item>> itemsForTab)
    {
        boolean onlyGunAmmo = tabName.equals(TAB_GUNS);
        boolean onlyVehicleAmmo = tabName.equals(TAB_BOMBS_AND_SHELLS);

        return (parameters, output) -> {
            for (DeferredHolder<Item, ? extends Item> ro : sortForCreativeTab(itemsForTab))
            {
                Item item = ro.get();

                if (item instanceof BulletItem bi &&
                    (onlyGunAmmo && !EnumWeaponType.TAB_GUNS_TYPES.contains(bi.getConfigType().getWeaponType())
                    || onlyVehicleAmmo && !EnumWeaponType.TAB_DRIVEABLES_TYPES.contains(bi.getConfigType().getWeaponType())))
                        continue;

                output.accept(item);

                if (ModCommonConfig.get().addAllPaintjobsToCreative() && item instanceof IPaintableItem<?> paintableItem)
                {
                    for (Paintjob pj : paintableItem.getPaintableType().getPaintjobs().values())
                        if (!pj.isDefault())
                            output.accept(paintableItem.makePaintjobStack(pj));
                }
            }
        };
    }

    private static List<DeferredHolder<Item, ? extends Item>> sortForCreativeTab(List<DeferredHolder<Item, ? extends Item>> itemsForTab)
    {
        List<DeferredHolder<Item, ? extends Item>> sorted = new ArrayList<>(itemsForTab);
        Comparator<DeferredHolder<Item, ? extends Item>> cmp = Comparator
            // 1) content pack name (case-insensitive)
            .comparing((DeferredHolder<Item, ? extends Item> ro) -> getPackName(ro.get()), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            // 2) item type (EnumType)
            .thenComparing(ro -> getPackType(ro.get()), Comparator.nullsFirst(Comparator.naturalOrder()))
            // 3) registry name (alphabetical)
            .thenComparing(ro -> getRegistryName(ro.get()), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        sorted.sort(cmp);
        return sorted;
    }

    private static String getPackName(Item item)
    {
        if (item instanceof IFlanItem<?> flanItem)
        {
            return flanItem.getConfigType().getContentPack().getName();
        }
        return null;
    }

    private static EnumType getPackType(Item item)
    {
        if (item instanceof IFlanItem<?> flanItem)
        {
            return flanItem.getConfigType().getType();
        }
        return null;
    }

    private static String getRegistryName(Item item)
    {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : null;
    }
}
