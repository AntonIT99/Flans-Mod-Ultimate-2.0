package com.flansmodultimate.client.integration.jei;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/** Scrollable costs preserve large legacy recipes and their full ingredient counts. */
public final class WorkbenchRecipeCategory implements IRecipeCategory<WorkbenchRecipe>
{
    @Getter
    private final RecipeType<WorkbenchRecipe> recipeType;
    @Getter
    private final Component title;
    @Getter
    private final IDrawable icon;

    public WorkbenchRecipeCategory(RecipeType<WorkbenchRecipe> recipeType, Component title, ItemStack icon, IGuiHelper guiHelper)
    {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(icon);
    }

    @Override
    public int getWidth()
    {
        return 170;
    }

    @Override
    public int getHeight()
    {
        return 126;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull WorkbenchRecipe recipe, @NotNull IFocusGroup focuses)
    {
        builder.addSlot(RecipeIngredientRole.CATALYST, 5, 3).addItemStack(recipe.station());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 145, 3).addItemStack(recipe.output());
        for (ItemStack material : recipe.materials())
            builder.addSlot(RecipeIngredientRole.INPUT).addItemStack(material);
        if (recipe.engineCount() > 0)
            builder.addSlot(RecipeIngredientRole.INPUT).addItemStacks(recipe.engines()).addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.flansmod.engine_hint")));
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull WorkbenchRecipe recipe, @NotNull IFocusGroup focuses)
    {
        builder.addScrollGridWidget(builder.getRecipeSlots().getSlots(RecipeIngredientRole.INPUT), 8, 4).setPosition(4, 27);
    }

    @Override
    public void draw(@NotNull WorkbenchRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics graphics, double mouseX, double mouseY)
    {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.flansmod.materials"), 27, 7, 0x404040, false);
        if (recipe.engineCount() > 0)
        {
            Component engines = recipe.engines().isEmpty() ? Component.translatable("jei.flansmod.no_engine") : Component.translatable("jei.flansmod.engines", recipe.engineCount());
            graphics.drawWordWrap(font, engines, 4, 108, 162, 0x404040);
        }
    }
}
