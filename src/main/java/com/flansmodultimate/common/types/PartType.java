package com.flansmodultimate.common.types;

import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
public class PartType extends InfoType
{
    public enum Category
    {
        COCKPIT,
        WING,
        ENGINE,
        PROPELLER,
        BAY,
        TAIL,
        WHEEL,
        CHASSIS,
        TURRET,
        FUEL,
        MISC
    }

    /** The default engine (normally the first one read by the type loader) for driveables with corrupt nbt or those spawned in creative */
    protected static final Map<EnumType, PartType> defaultEngines = new EnumMap<>(EnumType.class);

    /** Category */
    @Getter
    protected Category category = Category.COCKPIT;
    /** Max stack size of item */
    protected int stackSize = 1;
    /** (Engine) Multiplier applied to the thrust of the driveable */
    protected float engineSpeed = 1.0F;
    /** (Engine) Rate at which this engine consumes fuel */
    protected float fuelConsumption = 1.0F;
    /** (Engine) Power output of the engine - if using realistic acceleration. */
    protected float enginePower = 10F;
    /** (Fuel) The amount of fuel this fuel tank gives */
    @Getter
    protected int fuel = 0;
    /** The types of driveables that this engine works with. Used to designate some engines as mecha CPUs and what not*/
    protected Set<EnumType> worksWith = EnumSet.of(EnumType.MECHA, EnumType.PLANE, EnumType.VEHICLE);
    protected List<RecipeIngredient> partBoxRecipeRefs = new ArrayList<>();
    protected boolean partBoxRecipeResolved;
    protected TypeFile partBoxRecipeSourceFile;
    /** If true, then this engine will draw from RedstoneFlux power source items such as power cubes. Otherwise it will draw from Flan's Mod fuel items */
    protected boolean useRFPower = false;
    /** The power draw rate for RF (per tick) */
    protected int rfDrawRate = 1;
    /** Legacy apocalypse AI chip marker. Used by apocalypse integration and content compatibility. */
    @Getter
    protected boolean aiChip = false;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        // Generic
        category = readValue("Category", category, Category.class, file);
        stackSize = Math.max(1, Math.min(64, readValue("StackSize", stackSize, file)));

        // Engine
        fuelConsumption = Math.max(0F, readValue("FuelConsumption", fuelConsumption, file));
        engineSpeed = readValue("EngineSpeed", engineSpeed, file);
        enginePower = Math.max(0F, readValue("EnginePower", enginePower, file));

        //RedstoneFlux, for engines
        useRFPower = readValue("UseRF", useRFPower, file);
        useRFPower = readValue("UseRFPower", useRFPower, file);
        rfDrawRate = Math.max(1, readValue("RFDrawRate", rfDrawRate, file));
        aiChip = readValue("IsAIChip", aiChip, file);

        // Engine compatibility
        if (file.hasConfigLine("WorksWith"))
        {
            EnumSet<EnumType> configuredTypes = EnumSet.noneOf(EnumType.class);
            readValuesInLines("WorksWith", file).ifPresent(lines -> lines.forEach(split -> {
                for (String rawType : split)
                    EnumType.getType(rawType).ifPresentOrElse(configuredTypes::add, () -> logError("Unknown type '" + rawType + "' in WorksWith", file));
            }));
            worksWith = configuredTypes;
        }

        // Fuel cans
        fuel = readValue("Fuel", fuel, file);

        //Recipe
        if (hasValueForConfigField("PartBoxRecipe", file))
        {
            partBoxRecipeSourceFile = file;
            partBoxRecipeRefs.addAll(RecipeParser.parseAmountThenItemReferences(readValues("PartBoxRecipe", file), 2, contentPack, file, "PartBoxRecipe"));
        }

        if (category == Category.ENGINE && !useRFPower)
        {
            for (EnumType type : worksWith)
            {
                // If there is already a default engine for this type, compare and see if this one is better
                if (defaultEngines.containsKey(type) && isInferiorEngine(defaultEngines.get(type)))
                    defaultEngines.put(type, this);

                defaultEngines.putIfAbsent(type, this);
            }
        }
    }

    public boolean isInferiorEngine(PartType quitePossiblyAnInferiorEngine)
    {
        return engineSpeed > quitePossiblyAnInferiorEngine.engineSpeed;
    }

    public boolean worksWith(EnumType type)
    {
        return type != null && worksWith.contains(type);
    }

    public static PartType getDefaultEngine(EnumType type)
    {
        return defaultEngines.get(type);
    }

    public void validateRecipeIngredients()
    {
        getPartBoxRecipe();
    }

    public List<ItemStack> getPartBoxRecipe()
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (RecipeIngredient recipeItem : partBoxRecipeRefs)
        {
            ItemStack stack = recipeItem.resolve();
            if (!stack.isEmpty())
            {
                stacks.add(stack);
                continue;
            }

            if (!partBoxRecipeResolved)
                logMissingPartBoxRecipeIngredient(recipeItem);
        }
        partBoxRecipeResolved = true;
        return stacks;
    }

    private void logMissingPartBoxRecipeIngredient(RecipeIngredient recipeItem)
    {
        if (partBoxRecipeSourceFile == null)
            return;

        logError("Could not resolve PartBoxRecipe ingredient '" + recipeItem.getItemName()
            + "' (amount " + recipeItem.getAmount()
            + ") for part '" + getShortName()
            + "', skipping ingredient.", partBoxRecipeSourceFile);
    }

}
