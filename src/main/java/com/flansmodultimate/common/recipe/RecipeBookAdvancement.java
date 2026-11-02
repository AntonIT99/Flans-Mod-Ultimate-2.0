package com.flansmodultimate.common.recipe;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/** Hidden vanilla recipe-book unlocks: finding any ingredient reveals the recipe. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeBookAdvancement
{
    private static final String KEY_ITEMS = "items";

    /**
     * Creates the unlock in the item-predicate format of the loader that reads the given layout. 1.20.1 names a tag in a
     * separate {@code tag} field; 1.21 ignores that field, which would make the criterion match any item, and expects
     * {@code #tag} in {@code items} instead.
     */
    public static JsonObject create(String recipeId, Collection<JsonObject> ingredients, RecipeDataCompatibility.Format format)
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
                predicate.add(KEY_ITEMS, items);
            }
            else if (ingredient.has("tag") && format == RecipeDataCompatibility.Format.MODERN)
                predicate.addProperty(KEY_ITEMS, "#" + ingredient.get("tag").getAsString());
            else if (ingredient.has("tag"))
                predicate.add("tag", ingredient.get("tag"));
            else
                continue;
            JsonArray items = new JsonArray();
            items.add(predicate);
            JsonObject conditions = new JsonObject();
            conditions.add(KEY_ITEMS, items);
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
