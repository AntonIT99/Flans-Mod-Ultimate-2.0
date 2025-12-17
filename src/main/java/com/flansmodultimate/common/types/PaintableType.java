package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.LegacyDyeMapper;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.util.ResourceUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
    //Paintjobs
    /** The list of all available paintjobs for this gun */
    @Getter
    protected Map<Integer, Paintjob> paintjobs = new HashMap<>();
    /** The default paintjob for this gun. This is created automatically in the load process from existing info */
    @Getter
    protected Paintjob defaultPaintjob;
    /** Whether to add this paintjob to the paintjob table, gunmode table e.t.c. */
    protected boolean addAnyPaintjobToTables = true;
    /** Assigns IDs to paintjobs */
    private int nextPaintjobId = 0;

    public Paintjob getPaintjob(int id)
    {
        return paintjobs.getOrDefault(id, defaultPaintjob);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        readValuesInLines("Paintjob", file, 2).ifPresent(lines -> lines.forEach(split -> {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 2) / 2);
            for (int i = 0; i < (split.length - 2) / 2; i++)
            {
                dyeStacks.add(dyeNameToItemStack(split[i * 2 + 2], split[i * 2 + 3], "Paintjob", split, file));
            }
            nextPaintjobId++;
            paintjobs.put(nextPaintjobId, new Paintjob(this, nextPaintjobId, StringUtils.EMPTY, ResourceUtils.sanitize(split[0]), ResourceUtils.sanitize(split[1]), dyeStacks));
        }));

        readValuesInLines("AdvPaintJob", file, 3).ifPresent(lines -> lines.forEach(split -> {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 3) / 2);
            for (int i = 0; i < (split.length - 3) / 2; i++)
            {
                dyeStacks.add(dyeNameToItemStack(split[i * 2 + 3], split[i * 2 + 4], "AdvPaintJob", split, file));
            }
            nextPaintjobId++;
            paintjobs.put(nextPaintjobId, new Paintjob(this, nextPaintjobId, split[0], ResourceUtils.sanitize(split[1]), ResourceUtils.sanitize(split[2]), dyeStacks));
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

        //TODO: fix this (is legendary can not be invoked at this point)
        // Add all custom paintjobs to dungeon loot. Equal chance for each
        //InfoType.setTotalDungeonChance(InfoType.getTotalDungeonChance() + dungeonChance * (nonlegendarypaintjobs.size() - 1));

        // For servers, do not wait for resource initialization to create the default paintjob
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER)
        {
            defaultPaintjob = new Paintjob(this, 0, StringUtils.EMPTY, icon, textureName, texture, Collections.emptyList());
            paintjobs.put(0, defaultPaintjob);
        }
    }

    protected void readClient(TypeFile file)
    {
        super.readClient(file);
        defaultPaintjob = new Paintjob(this, 0, StringUtils.EMPTY, icon, textureName, texture, Collections.emptyList());
        paintjobs.put(0, defaultPaintjob);
    }

    private static Supplier<ItemStack> dyeNameToItemStack(String dyeName, String stackSize, String key, String[] values, TypeFile file)
    {
        if (dyeName.equalsIgnoreCase("rainbow"))
            return () -> new ItemStack(FlansMod.rainbowPaintcan.get(), Integer.parseInt(stackSize));
        else
        {
            try
            {
                return () -> LegacyDyeMapper.toDyeStack(dyeName, Integer.parseInt(stackSize));
            }
            catch (Exception e)
            {
                logError(String.format("%s in '%s %s'", e.getMessage(), key, String.join(StringUtils.SPACE, values)), file);
            }
        }
        return () -> ItemStack.EMPTY;
    }
}
