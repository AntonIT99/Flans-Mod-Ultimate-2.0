package com.wolffsarmormod.common.types;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.paintjob.LegacyDyeMapper;
import com.wolffsarmormod.common.paintjob.Paintjob;
import com.wolffsarmormod.util.ResourceUtils;
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

import static com.wolffsarmormod.util.TypeReaderUtils.logError;

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
    protected Boolean addAnyPaintjobToTables = true;
    /** Assigns IDs to paintjobs */
    private int nextPaintjobId = 0;

    public Paintjob getPaintjob(int id)
    {
        return paintjobs.getOrDefault(id, defaultPaintjob);
    }

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        if (split[0].equalsIgnoreCase("Paintjob") && split.length > 2)
        {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 3) / 2);
            for (int i = 0; i < (split.length - 3) / 2; i++)
            {
                dyeStacks.add(dyeNameToItemStack(split[i * 2 + 3], split[i * 2 + 4], line, file));
            }
            nextPaintjobId++;
            paintjobs.put(nextPaintjobId, new Paintjob(this, nextPaintjobId, StringUtils.EMPTY, ResourceUtils.sanitize(split[1]), ResourceUtils.sanitize(split[2]), dyeStacks));
        }

        if (split[0].equalsIgnoreCase("AdvPaintJob") && split.length > 3)
        {
            List<Supplier<ItemStack>> dyeStacks = new ArrayList<>((split.length - 4) / 2);
            for (int i = 0; i < (split.length - 4) / 2; i++)
            {
                dyeStacks.add(dyeNameToItemStack(split[i * 2 + 4], split[i * 2 + 5], line, file));
            }
            nextPaintjobId++;
            paintjobs.put(nextPaintjobId, new Paintjob(this, nextPaintjobId, split[1], ResourceUtils.sanitize(split[2]), ResourceUtils.sanitize(split[3]), dyeStacks));
        }

        if (split[0].equalsIgnoreCase("AddPaintableToTables"))
        {
            if (split.length == 2)
            {
                addAnyPaintjobToTables = Boolean.parseBoolean(split[1]);
            }
            else if (split.length >= 3)
            {
                String paintjobName = split[1];

                for (Paintjob paintjob : paintjobs.values())
                {
                    if (paintjob.getTextureName().equals(paintjobName))
                        paintjob.setAddToTables(Boolean.parseBoolean(split[2]));
                }
            }
        }
    }

    @Override
    protected void postRead()
    {
        super.postRead();
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER)
        {
            defaultPaintjob = new Paintjob(this, 0, StringUtils.EMPTY, icon, textureName, texture, Collections.emptyList());
            paintjobs.put(0, defaultPaintjob);
        }

        //TODO: fix this (is legendary can not be invoked at this point)
        // Add all custom paintjobs to dungeon loot. Equal chance for each
        //InfoType.setTotalDungeonChance(InfoType.getTotalDungeonChance() + dungeonChance * (nonlegendarypaintjobs.size() - 1));
    }

    @Override
    protected void postReadClient()
    {
        super.postReadClient();
        defaultPaintjob = new Paintjob(this, 0, StringUtils.EMPTY, icon, textureName, texture, Collections.emptyList());
        paintjobs.put(0, defaultPaintjob);
    }

    private static Supplier<ItemStack> dyeNameToItemStack(String dyeName, String stackSize, String line, TypeFile file)
    {
        if (dyeName.equalsIgnoreCase("rainbow"))
            return () -> new ItemStack(ArmorMod.rainbowPaintcan.get(), Integer.parseInt(stackSize));
        else
        {
            try
            {
                return () -> LegacyDyeMapper.toDyeStack(dyeName, Integer.parseInt(stackSize));
            }
            catch (Exception e)
            {
                logError(String.format("%s in '%s'", e.getMessage(), line), file);
            }
        }
        return () -> ItemStack.EMPTY;
    }
}
