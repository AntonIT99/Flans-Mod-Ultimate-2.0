package com.flansmodultimate;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
    public static void registerCreativeTab(DeferredRegister<CreativeModeTab> creativeTabRegistry, String tabName, List<RegistryObject<Item>> itemsForTab, boolean onlyGunAmmo, boolean onlyVehicleAmmo, ResourceKey<CreativeModeTab> beforeTab, ResourceKey<CreativeModeTab>... afterTab)
    {
        creativeTabRegistry.register(tabName, () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab." + FlansMod.MOD_ID + "." + tabName))
            .icon(createIcon(tabName, itemsForTab))
            .withSearchBar()
            .withTabsBefore(beforeTab)
            .withTabsAfter(afterTab)
            .displayItems(displayItemsWithPaintjobsGenerator(itemsForTab, onlyGunAmmo, onlyVehicleAmmo))
            .build());
    }

    private static Supplier<ItemStack> createIcon(String tabName, List<RegistryObject<Item>> itemsForTab)
    {
        return () -> {
            //TODO: only display planes and vehicles for the icon: replace instanceof Item by corresponding Item class
            List<RegistryObject<Item>> itemsForIcon = itemsForTab;
            if (tabName.equals("guns"))
                itemsForIcon = itemsForTab.stream().filter(ro -> ro.get() instanceof GunItem).toList();
            if (tabName.equals("driveables"))
                itemsForIcon = itemsForTab.stream().filter(ro -> ro.get() instanceof Item).toList();

            if (itemsForIcon.isEmpty())
                return new ItemStack(Items.WHITE_WOOL);

            return new ItemStack(itemsForIcon.get(ThreadLocalRandom.current().nextInt(0, itemsForIcon.size())).get());
        };
    }

    private static CreativeModeTab.DisplayItemsGenerator displayItemsWithPaintjobsGenerator(List<RegistryObject<Item>> itemsForTab, boolean onlyGunAmmo, boolean onlyVehicleAmmo)
    {
        return (parameters, output) -> {
            for (RegistryObject<Item> ro : sortForCreativeTab(itemsForTab))
            {
                Item item = ro.get();

                if (item instanceof BulletItem bi)
                {
                    if (onlyGunAmmo && !EnumWeaponType.TAB_GUNS_TYPES.contains(bi.getConfigType().getWeaponType()))
                        continue;
                    if (onlyVehicleAmmo && !EnumWeaponType.TAB_DRIVEABLES_TYPES.contains(bi.getConfigType().getWeaponType()))
                        continue;
                }

                output.accept(item);

                if (ModCommonConfig.get().addAllPaintjobsToCreative && item instanceof IPaintableItem<?> paintableItem)
                {
                    for (Paintjob pj : paintableItem.getPaintableType().getPaintjobs().values())
                        if (!pj.isDefault())
                            output.accept(paintableItem.makePaintjobStack(pj));
                }
            }
        };
    }

    private static List<RegistryObject<Item>> sortForCreativeTab(List<RegistryObject<Item>> itemsForTab)
    {
        List<RegistryObject<Item>> sorted = new ArrayList<>(itemsForTab);
        Comparator<RegistryObject<Item>> cmp = Comparator
            // 1) content pack name (case-insensitive), Flan items first
            .comparing(
                    (RegistryObject<Item> ro) -> getPackName(ro.get()),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            )
            // 2) content pack type (EnumType)
            .thenComparing(
                    ro -> getPackType(ro.get()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            )
            // 3) registry name (alphabetical)
            .thenComparing(
                    ro -> getRegistryName(ro.get()),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null ? key.toString() : null;
    }
}
