package com.flansmodultimate;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModCreativeTabs
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

    @SuppressWarnings("unchecked")
    static void registerCreativeModeTabs()
    {
        ResourceKey<CreativeModeTab> creativeTabMainKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, TAB_GENERAL));
        ResourceKey<CreativeModeTab>[] creativeTabsFlansModReloadedKey = new ResourceKey[]
            {
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "creative_tab_guns")),
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "creative_tab_modifiers")),
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "creative_tab_parts")),
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "creative_tab_bullets"))
            };

        List<Supplier<? extends Item>> generalItemList = new ArrayList<>();
        generalItemList.add(FlansModItems.gunWorkbenchItem);
        generalItemList.add(FlansModItems.vehicleCraftingTableItem);
        generalItemList.add(FlansModItems.paintjobTableItem);
        generalItemList.add(FlansModItems.rainbowPaintcan);
        generalItemList.add(FlansModItems.flagpoleItem);
        generalItemList.add(FlansModItems.playerSpawnerItem);
        generalItemList.add(FlansModItems.itemSpawnerItem);
        generalItemList.add(FlansModItems.vehicleSpawnerItem);
        generalItemList.add(FlansModItems.opStick);
        if (ModApocalypseConfig.apocalypseEnabled()) {
            generalItemList.add(ApocalypseContent.SULPHUR);
            generalItemList.add(ApocalypseContent.BLOCK_SULPHUR_ITEM);
            generalItemList.add(ApocalypseContent.BLOCK_LAB_STONE_ITEM);
            generalItemList.add(ApocalypseContent.BLOCK_POWER_CUBE_ITEM);
            generalItemList.add(ApocalypseContent.SULPHURIC_ACID_BUCKET);
        }
        generalItemList.addAll(FlansMod.getItems(EnumSet.of(
            EnumType.ITEM_HOLDER,
            EnumType.ARMOR_BOX,
            EnumType.GUN_BOX
        )));

        BiConsumer<String, Supplier<CreativeModeTab>> registerTab = FlansModRegistries.creativeModeTabRegistry::register;
        registerCreativeTab(registerTab, TAB_GENERAL, generalItemList, Collections.emptyList(), CreativeModeTabs.SPAWN_EGGS, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_ARMORS, FlansMod.getItems(EnumType.ARMOR), List.of(EnumType.ARMOR), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_ATTACHMENTS, FlansMod.getItems(EnumType.ATTACHMENT), List.of(EnumType.ATTACHMENT), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_GUNS, FlansMod.getItems(EnumSet.of(EnumType.GUN, EnumType.BULLET)), List.of(EnumType.GUN), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_GRENADES, FlansMod.getItems(EnumType.GRENADE), List.of(EnumType.GRENADE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_TOOLS, FlansMod.getItems(EnumSet.of(EnumType.TOOL, EnumType.GLOVE)), List.of(EnumType.TOOL, EnumType.GLOVE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_BOMBS_AND_SHELLS, FlansMod.getItems(EnumSet.of(EnumType.BULLET)), List.of(EnumType.BULLET), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_AA_GUNS, FlansMod.getItems(EnumType.AA_GUN), List.of(EnumType.AA_GUN), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_MECHAS, FlansMod.getItems(EnumSet.of(EnumType.MECHA, EnumType.MECHA_ITEM)), List.of(EnumType.MECHA), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_PLANES, FlansMod.getItems(EnumSet.of(EnumType.PLANE)), List.of(EnumType.PLANE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_VEHICLES, FlansMod.getItems(EnumSet.of(EnumType.VEHICLE)), List.of(EnumType.VEHICLE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        registerCreativeTab(registerTab, TAB_PARTS, FlansMod.getItems(EnumSet.of(EnumType.PART)), List.of(EnumType.PART), creativeTabMainKey, creativeTabsFlansModReloadedKey);
    }

    @SafeVarargs
    private static void registerCreativeTab(BiConsumer<String, Supplier<CreativeModeTab>> tabRegistrar, String tabName, List<? extends Supplier<? extends Item>> itemsForTab, List<EnumType> typesForIcon, ResourceKey<CreativeModeTab> beforeTab, ResourceKey<CreativeModeTab>... afterTab)
    {
        tabRegistrar.accept(tabName, () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab." + FlansMod.MOD_ID + "." + tabName))
            .icon(createIcon(tabName, itemsForTab, typesForIcon))
            .withSearchBar()
            .withTabsBefore(beforeTab)
            .withTabsAfter(afterTab)
            .displayItems(displayItemsWithPaintjobsGenerator(tabName, itemsForTab))
            .build());
    }

    private static Supplier<ItemStack> createIcon(String tabName, List<? extends Supplier<? extends Item>> itemsForTab, List<EnumType> typesForIcons)
    {
        return () -> {
            if (tabName.equals(TAB_GENERAL))
                return new ItemStack(FlansModItems.gunWorkbenchItem.get());

            List<? extends Supplier<? extends Item>> itemsForIcon = itemsForTab.stream()
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

    private static CreativeModeTab.DisplayItemsGenerator displayItemsWithPaintjobsGenerator(String tabName, List<? extends Supplier<? extends Item>> itemsForTab)
    {
        boolean onlyGunAmmo = tabName.equals(TAB_GUNS);
        boolean onlyVehicleAmmo = tabName.equals(TAB_BOMBS_AND_SHELLS);

        return (parameters, output) -> {
            for (Supplier<? extends Item> ro : sortForCreativeTab(itemsForTab))
            {
                Item item = ro.get();

                if (item instanceof BulletItem bi &&
                    (onlyGunAmmo && !EnumWeaponType.TAB_GUNS_TYPES.contains(bi.getConfigType().getWeaponType())
                    || onlyVehicleAmmo && !EnumWeaponType.TAB_DRIVEABLES_TYPES.contains(bi.getConfigType().getWeaponType())))
                        continue;

                output.accept(createCreativeStack(new ItemStack(item), parameters));

                if (ModCommonConfig.get().addAllPaintjobsToCreative() && item instanceof IPaintableItem<?> paintableItem)
                {
                    for (Paintjob pj : paintableItem.getPaintableType().getPaintjobs().values())
                        if (!pj.isDefault())
                            output.accept(createCreativeStack(paintableItem.makePaintjobStack(pj), parameters));
                }
            }
        };
    }

    private static ItemStack createCreativeStack(ItemStack stack, CreativeModeTab.ItemDisplayParameters parameters)
    {
        if (!(stack.getItem() instanceof DriveableItem<?, ?> driveableItem))
            return stack;

        DriveableData data = DriveableData.fromStack(driveableItem.getConfigType(), stack, parameters.holders());
        data.setFuelInTank(driveableItem.getConfigType().getFuelTankSize());
        return data.copyToStack(stack);
    }

    private static List<Supplier<? extends Item>> sortForCreativeTab(List<? extends Supplier<? extends Item>> itemsForTab)
    {
        List<Supplier<? extends Item>> sorted = new ArrayList<>(itemsForTab);
        Comparator<Supplier<? extends Item>> cmp = Comparator
            // 1) content pack name (case-insensitive)
            .comparing((Supplier<? extends Item> ro) -> getPackName(ro.get()), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
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
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key.toString();
    }
}
