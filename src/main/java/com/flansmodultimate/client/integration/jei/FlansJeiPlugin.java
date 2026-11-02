package com.flansmodultimate.client.integration.jei;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.FlansModItems;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.client.gui.DriveableCraftingScreen;
import com.flansmodultimate.client.gui.GunBoxScreen;
import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

/** Optional JEI entry point; common initialization never references this class. */
@JeiPlugin
public final class FlansJeiPlugin implements IModPlugin
{
    public static final RecipeType<WorkbenchRecipe> DRIVEABLE = RecipeType.create(FlansMod.MOD_ID, "vehicle_assembly", WorkbenchRecipe.class);
    public static final RecipeType<WorkbenchRecipe> GUN_BOX = RecipeType.create(FlansMod.MOD_ID, "gun_box", WorkbenchRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid()
    {
        return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "workbenches");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new WorkbenchRecipeCategory(DRIVEABLE, Component.translatable("jei.flansmod.vehicle_assembly"), new ItemStack(FlansModItems.vehicleCraftingTableItem.get()), gui),
            new WorkbenchRecipeCategory(GUN_BOX, Component.translatable("jei.flansmod.gun_box"), new ItemStack(FlansModItems.gunWorkbenchItem.get()), gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        List<PartType> parts = InfoType.getInfoTypes().values().stream().filter(PartType.class::isInstance).map(PartType.class::cast).distinct().toList();
        ItemStack table = new ItemStack(FlansModItems.vehicleCraftingTableItem.get());
        registration.addRecipes(DRIVEABLE,
            DriveableCraftingMenu.getBlueprints().stream().map(type -> WorkbenchRecipe.driveable(type, table, parts)).filter(recipe -> !recipe.output().isEmpty()).toList());

        List<WorkbenchRecipe> purchases = new ArrayList<>();
        for (GunBoxType box : gunBoxes())
        {
            ItemStack station = ModUtils.getItemStack(box).orElse(ItemStack.EMPTY);
            if (station.isEmpty())
                continue;
            for (GunBoxType.GunBoxPage page : box.getGunPages())
                for (GunBoxType.GunBoxEntry entry : page.getEntries())
                {
                    purchases.add(WorkbenchRecipe.gunBox(entry, station));
                    for (GunBoxType.GunBoxEntry ammo : entry.getAmmoEntryList())
                        purchases.add(WorkbenchRecipe.gunBox(ammo, station));
                }
        }
        registration.addRecipes(GUN_BOX, purchases.stream().filter(recipe -> !recipe.output().isEmpty()).toList());
        registerCoreItemInformation(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        registration.addRecipeCatalyst(new ItemStack(FlansModItems.vehicleCraftingTableItem.get()), DRIVEABLE);
        registration.addRecipeCatalyst(new ItemStack(FlansModItems.gunWorkbenchItem.get()), DRIVEABLE);
        for (GunBoxType box : gunBoxes())
            ModUtils.getItemStack(box).ifPresent(stack -> registration.addRecipeCatalyst(stack, GUN_BOX));
    }

    private static List<GunBoxType> gunBoxes()
    {
        return InfoType.getInfoTypes().values().stream().filter(GunBoxType.class::isInstance).map(GunBoxType.class::cast).distinct().sorted(Comparator.comparing(GunBoxType::getShortName)).toList();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addRecipeClickArea(DriveableCraftingScreen.class, 7, 5, 160, 10, DRIVEABLE);
        registration.addRecipeClickArea(GunBoxScreen.class, 7, 5, 259, 10, GUN_BOX);
    }

    private static void registerCoreItemInformation(IRecipeRegistration registration)
    {
        for (var item : List.of(FlansModItems.opStick, FlansModItems.flagpoleItem, FlansModItems.capturePointItem, FlansModItems.teamBannerItem, FlansModItems.playerSpawnerItem,
            FlansModItems.itemSpawnerItem, FlansModItems.vehicleSpawnerItem))
            registration.addIngredientInfo(item.get(), Component.translatable("jei.flansmod.teams_items"));
        registration.addIngredientInfo(FlansModItems.rainbowPaintcan.get(), Component.translatable("jei.flansmod.rainbow_paint"));
        registration.addIngredientInfo(ApocalypseContent.SULPHUR.get(), Component.translatable("jei.flansmod.sulphur"));
        registration.addIngredientInfo(ApocalypseContent.BLOCK_SULPHUR_ITEM.get(), Component.translatable("jei.flansmod.sulphur_block"));
        registration.addIngredientInfo(ApocalypseContent.BLOCK_LAB_STONE_ITEM.get(), Component.translatable("jei.flansmod.lab_stone"));
        registration.addIngredientInfo(ApocalypseContent.SULPHURIC_ACID_BUCKET.get(), Component.translatable("jei.flansmod.acid_bucket"));
    }
}
