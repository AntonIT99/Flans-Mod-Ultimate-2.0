package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.LegacyDyeMapper;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.util.ResourceUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PaintableType extends InfoType
{
    public static final String RAINBOW_DYE = "rainbow";

    //Paintjobs
    /** The list of all available paintjobs for this gun */
    @Getter
    protected Map<Integer, Paintjob> paintjobs = new HashMap<>();
    @Getter
    protected Map<Integer, Paintjob> nonLegendaryPaintjobs = new HashMap<>();
    /** The default paintjob for this gun. This is created automatically in the load process from existing info */
    @Getter
    protected Paintjob defaultPaintjob;
    /** Whether to add this paintjob to the paintjob table, gunmode table e.t.c. */
    protected boolean addAnyPaintjobToTables = true;
    /** Assigns IDs to paintjobs */
    private int nextPaintjobId = 0;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        readValuesInLines("Paintjob", file, 2).ifPresent(lines -> lines.forEach(split -> {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 2) / 2);
            boolean isLegendary = false;
            for (int i = 0; i < (split.length - 2) / 2; i++)
            {
                String dyeName = split[i * 2 + 2];
                String dyeStackSize = split[i * 2 + 3];
                if (dyeName.equalsIgnoreCase(RAINBOW_DYE))
                    isLegendary = true;
                dyeStacks.add(dyeNameToItemStack(dyeName, dyeStackSize, "Paintjob", split, file));
            }
            nextPaintjobId++;
            Paintjob paintjob = new Paintjob(this, nextPaintjobId, StringUtils.EMPTY, ResourceUtils.sanitize(split[0]), ResourceUtils.sanitize(split[1]), dyeStacks);
            paintjobs.put(nextPaintjobId, paintjob);
            if (!isLegendary)
                nonLegendaryPaintjobs.put(nextPaintjobId, paintjob);
        }));

        readValuesInLines("AdvPaintJob", file, 3).ifPresent(lines -> lines.forEach(split -> {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 3) / 2);
            boolean isLegendary = false;
            for (int i = 0; i < (split.length - 3) / 2; i++)
            {
                String dyeName = split[i * 2 + 3];
                String dyeStackSize = split[i * 2 + 4];
                if (dyeName.equalsIgnoreCase(RAINBOW_DYE))
                    isLegendary = true;
                dyeStacks.add(dyeNameToItemStack(dyeName, dyeStackSize, "AdvPaintJob", split, file));
            }
            nextPaintjobId++;
            Paintjob paintjob = new Paintjob(this, nextPaintjobId, split[0], ResourceUtils.sanitize(split[1]), ResourceUtils.sanitize(split[2]), dyeStacks);
            paintjobs.put(nextPaintjobId, paintjob);
            if (!isLegendary)
                nonLegendaryPaintjobs.put(nextPaintjobId, paintjob);
        }));

        readValues("AddPaintableToTables", file, 2).ifPresent(values -> {
            if (values.length > 1)
            {
                String paintjobName = values[0];

                for (Paintjob paintjob : paintjobs.values())
                {
                    if (paintjob.getTextureName().equals(paintjobName))
                        paintjob.setAddToTables(Boolean.parseBoolean(values[1]));
                }
            }
            else
            {
                addAnyPaintjobToTables = Boolean.parseBoolean(values[0]);
            }
        });

        defaultPaintjob = new Paintjob(this, 0, StringUtils.EMPTY, icon, textureName, Collections.emptyList());
        paintjobs.put(0, defaultPaintjob);
        nonLegendaryPaintjobs.put(0, defaultPaintjob);

        // Add all custom paintjobs to dungeon loot. Equal chance for each.
        InfoType.setTotalDungeonChance(InfoType.getTotalDungeonChance() + dungeonChance * (nonLegendaryPaintjobs.size() - 1));
    }

    private static Supplier<ItemStack> dyeNameToItemStack(String dyeName, String stackSize, String key, String[] values, TypeFile file)
    {
        try
        {
            if (dyeName.equalsIgnoreCase(RAINBOW_DYE))
                return () -> new ItemStack(FlansMod.rainbowPaintcan.get(), Integer.parseInt(stackSize));
            else
                return () -> LegacyDyeMapper.toDyeStack(dyeName, Integer.parseInt(stackSize));
        }
        catch (Exception e)
        {
            logError(String.format("%s in '%s %s'", e.getMessage(), key, String.join(StringUtils.SPACE, values)), file);
        }
        return () -> ItemStack.EMPTY;
    }

    public Paintjob getPaintjob(int id)
    {
        return paintjobs.getOrDefault(id, defaultPaintjob);
    }

    public void applyPaintjobToStack(ItemStack stack, Paintjob paintjob)
    {
        stack.getOrCreateTag().putInt(IPaintableItem.NBT_PAINTJOB_ID, paintjob.getId());
    }

    public int getPaintjobId(ItemStack stack)
    {
        return stack.getOrCreateTag().getInt(IPaintableItem.NBT_PAINTJOB_ID);
    }

    public Paintjob getPaintjob(ItemStack stack)
    {
        return getPaintjob(getPaintjobId(stack));
    }
}
