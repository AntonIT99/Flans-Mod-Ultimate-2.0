package com.flansmodultimate;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.config.ModCommonConfigs;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.BooleanUtils;

import net.minecraft.network.chat.Component;
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
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreativeTabs
{
    public static void registerCreativeModeTabs()
    {
        registerCreativeTab("flansmod", List.of(FlansMod.rainbowPaintcan));
        registerCreativeTab("armors", FlansMod.items.get(EnumType.ARMOR));
        registerCreativeTab("attachments", FlansMod.items.get(EnumType.ATTACHMENT));
        registerCreativeTab("guns", Stream.of(FlansMod.items.get(EnumType.GUN), FlansMod.items.get(EnumType.BULLET), FlansMod.items.get(EnumType.GRENADE)).flatMap(List::stream).toList());
        registerCreativeTab("driveables", FlansMod.items.get(EnumType.BULLET));
    }

    private static void registerCreativeTab(String tabName, List<RegistryObject<Item>> itemsForTab)
    {
        if (itemsForTab.isEmpty())
            return;

        FlansMod.creativeModeTabRegistry.register(tabName, () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab." + FlansMod.MOD_ID + "." + tabName))
            .icon(createIcon(tabName, itemsForTab))
            .withSearchBar()
            .displayItems(displayItemsWithPaintjobsGenerator(tabName, itemsForTab))
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

    private static CreativeModeTab.DisplayItemsGenerator displayItemsWithPaintjobsGenerator(String tabName, List<RegistryObject<Item>> itemsForTab)
    {
        return (parameters, output) -> {
            for (RegistryObject<Item> ro : sortForCreativeTab(itemsForTab))
            {
                Item item = ro.get();

                if (item instanceof BulletItem bi)
                {
                    if (tabName.equals("guns") && !EnumWeaponType.TAB_GUNS_TYPES.contains(bi.getConfigType().getWeaponType()))
                        continue;
                    if (tabName.equals("driveables") && !EnumWeaponType.TAB_DRIVEABLES_TYPES.contains(bi.getConfigType().getWeaponType()))
                        continue;
                }

                if (item instanceof IPaintableItem<?> paintableItem)
                {
                    PaintableType type = paintableItem.getPaintableType();
                    if (BooleanUtils.isTrue(ModCommonConfigs.addAllPaintjobsToCreative.get()))
                    {
                        for (Paintjob pj : type.getPaintjobs().values())
                            output.accept(paintableItem.makePaintjobStack(pj));
                    }
                    else
                    {
                        output.accept(paintableItem.makeDefaultPaintjobStack());
                    }
                }
                else
                {
                    output.accept(item);
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
