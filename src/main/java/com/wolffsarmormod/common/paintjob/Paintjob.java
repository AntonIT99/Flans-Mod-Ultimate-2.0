package com.wolffsarmormod.common.paintjob;

import com.wolffsarmormod.common.types.PaintableType;

import net.minecraft.world.item.ItemStack;

public class Paintjob
{
    public enum EnumPaintjobRarity
    {
        UNKNOWN,
        COMMON,
        UNCOMMON,
        RARE,
        LEGENDARY,
    }

    public int id;
    public PaintableType parent;
    public String displayName;
    public String iconName;
    public String textureName;
    public ItemStack[] dyesNeeded;
    public Boolean addToTables;
    public EnumPaintjobRarity rarity;

    public Paintjob(PaintableType parent, int id, String iconName, String textureName, ItemStack[] dyesNeeded, boolean addToTables)
    {
        this(parent, id, "", iconName, textureName, dyesNeeded, addToTables);
    }

    public Paintjob(PaintableType parent, int id, String displayName, String iconName, String textureName, ItemStack[] dyesNeeded, boolean addToTables)
    {
        this.parent = parent;
        this.id = id;
        this.displayName = displayName;
        this.iconName = iconName;
        this.textureName = textureName;
        this.dyesNeeded = dyesNeeded;
        this.addToTables = addToTables;
        this.rarity = EnumPaintjobRarity.UNKNOWN;
    }

    public boolean isLegendary()
    {
        //TODO: uncomment this
        /*for (ItemStack stack : dyesNeeded)
        {
            if (stack.getItem() == FlansMod.rainbowPaintcan)
                return true;
        }*/
        return false;
    }

    @Override
    public int hashCode()
    {
        return parent.hashCode() ^ id;
    }
}
