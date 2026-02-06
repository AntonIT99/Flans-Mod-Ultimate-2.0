package com.flansmodultimate.common.types;

import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.*;

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
    protected static Map<EnumType, PartType> defaultEngines = new EnumMap<>(EnumType.class);

    /** Category */
    @Getter
    protected Category category = Category.COCKPIT;
    /** Max stack size of item */
    protected int stackSize = 0;
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
    protected List<EnumType> worksWith = new ArrayList<>();
    //TODO: replace by uncommented version
    //protected List<EnumType> worksWith = Arrays.asList(EnumType.mecha, EnumType.plane, EnumType.vehicle);
    protected List<ItemStack> partBoxRecipe = new ArrayList<>();
    /** If true, then this engine will draw from RedstoneFlux power source items such as power cubes. Otherwise it will draw from Flan's Mod fuel items */
    protected boolean useRFPower = false;
    /** The power draw rate for RF (per tick) */
    protected int rfDrawRate = 1;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        // Generic
        category = readValue("Category", category, Category.class, file);
        stackSize = readValue("StackSize", stackSize, file);

        // Engine
        fuelConsumption = readValue("FuelConsumption", engineSpeed, file);
        engineSpeed = readValue("EngineSpeed", engineSpeed, file);
        enginePower = readValue("EnginePower", enginePower, file);

        //RedstoneFlux, for engines
        useRFPower = readValue("UseRF", useRFPower, file);
        useRFPower = readValue("UseRFPower", useRFPower, file);
        rfDrawRate = readValue("RFDrawRate", rfDrawRate, file);

        // Engine compatibility
        //TODO: Uncomment when Driveables are added
        /*readValuesInLines("WorksWith", file).ifPresentOrElse(lines -> lines.forEach(split -> {
            for (String rawType : split)
                EnumType.getType(rawType).ifPresentOrElse(type -> worksWith.add(type), () -> logError("type not found for part WorksWith", file));
        }), () -> worksWith = new ArrayList<>());*/

        // Fuel cans
        fuel = readValue("Fuel", fuel, file);

        //Recipe
        partBoxRecipe.addAll(getPartBoxRecipeStacks(readValues("PartBoxRecipe", file), file));

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

    protected List<ItemStack> getPartBoxRecipeStacks(String[] split, TypeFile file)
    {
        int pairs = Math.max(0, (split.length - 2) / 2);
        List<ItemStack> stacks = new ArrayList<>(pairs);

        for (int i = 0; i < pairs; i++)
        {
            int amountIndex = 2 * i + 2;
            int itemIndex   = 2 * i + 3;

            if (amountIndex >= split.length || itemIndex >= split.length)
                break;

            int amount;
            try
            {
                amount = Integer.parseInt(split[amountIndex].trim());
            }
            catch (Exception e)
            {
                logError("Invalid amount '" + split[amountIndex] + "' in PartBoxRecipe", file);
                continue;
            }

            String token = split[itemIndex] == null ? "" : split[itemIndex].trim();
            if (token.isEmpty())
            {
                logError("Missing item token in PartBoxRecipe", file);
                continue;
            }

            String itemName = token;
            int damage = 0;

            int dot = token.indexOf('.');
            if (dot >= 0)
            {
                itemName = token.substring(0, dot).trim();
                String dmgStr = (dot + 1 < token.length()) ? token.substring(dot + 1).trim() : "";
                if (!dmgStr.isEmpty())
                {
                    try
                    {
                        damage = Integer.parseInt(dmgStr);
                    }
                    catch (Exception e)
                    {
                        logError("Invalid damage '" + dmgStr + "' in PartBoxRecipe", file);
                    }
                }
            }

            ItemStack recipeElement = getRecipeElement(itemName, amount, damage, contentPack);
            if (recipeElement == null)
                logError("Could not find item for PartBoxRecipe: '" + itemName + "'", file);
            else
                stacks.add(recipeElement);
        }

        return stacks;
    }
}
