package com.flansmodultimate.common.recipe;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/** Hidden vanilla recipe-book unlocks: finding any ingredient reveals the recipe. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeBookAdvancement
{
    public static JsonObject create(String recipeId, Collection<JsonObject> ingredients)
    {
        JsonObject criteria = new JsonObject();
        JsonObject unlocked = new JsonObject();
        unlocked.addProperty("trigger", "minecraft:recipe_unlocked");
        JsonObject unlockedConditions = new JsonObject();
        unlockedConditions.addProperty("recipe", recipeId);
        unlocked.add("conditions", unlockedConditions);
        criteria.add("has_the_recipe", unlocked);

        int index = 0;
        for (JsonObject ingredient : ingredients)
        {
            JsonObject predicate = new JsonObject();
            if (ingredient.has("item"))
            {
                JsonArray items = new JsonArray();
                items.add(ingredient.get("item"));
                predicate.add("items", items);
            }
            else if (ingredient.has("tag"))
                predicate.add("tag", ingredient.get("tag"));
            else
                continue;
            JsonArray items = new JsonArray();
            items.add(predicate);
            JsonObject conditions = new JsonObject();
            conditions.add("items", items);
            JsonObject criterion = new JsonObject();
            criterion.addProperty("trigger", "minecraft:inventory_changed");
            criterion.add("conditions", conditions);
            criteria.add("has_ingredient_" + index++, criterion);
        }

        JsonArray alternatives = new JsonArray();
        criteria.keySet().forEach(alternatives::add);
        JsonArray requirements = new JsonArray();
        requirements.add(alternatives);
        JsonArray recipes = new JsonArray();
        recipes.add(recipeId);
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);
        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");
        advancement.add("criteria", criteria);
        advancement.add("requirements", requirements);
        advancement.add("rewards", rewards);
        return advancement;
    }
}
