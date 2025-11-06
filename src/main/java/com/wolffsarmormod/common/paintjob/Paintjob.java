package com.wolffsarmormod.common.paintjob;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.PaintableType;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

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

    private final PaintableType type;
    @Getter
    private final int id;
    @Getter
    private final String displayName;
    @Getter
    private final String icon;
    @Getter
    private String textureName = StringUtils.EMPTY;
    @Getter
    private final List<Supplier<ItemStack>> dyesNeeded;
    @Getter @Setter
    private EnumPaintjobRarity rarity;
    @Getter @Setter
    private boolean addToTables;
    @Getter
    private ResourceLocation texture;

    /**
     * Constructor for the default Paintjob
     */
    public Paintjob(PaintableType type, int id, String icon, ResourceLocation texture, List<Supplier<ItemStack>> dyesNeeded)
    {
        this(type, id, StringUtils.EMPTY, icon, dyesNeeded);
        this.texture = texture;
    }

    public Paintjob(PaintableType type, int id, String displayName, String icon, String textureName, List<Supplier<ItemStack>> dyesNeeded)
    {
        this(type, id, displayName, icon, dyesNeeded);
        this.textureName = textureName;
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            this.texture = InfoType.loadTexture(textureName, type);
        }
    }

    private Paintjob(PaintableType type, int id, String displayName, String icon, List<Supplier<ItemStack>> dyesNeeded)
    {
        this.type = type;
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.dyesNeeded = dyesNeeded;
        this.rarity = EnumPaintjobRarity.UNKNOWN;
    }

    public boolean isLegendary()
    {
        for (Supplier<ItemStack> stack : dyesNeeded)
        {
            if (stack.get().getItem() == ArmorMod.rainbowPaintcan.get())
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Paintjob otherPaintjob))
            return false;
        return type.equals(otherPaintjob.type) && id == otherPaintjob.id;
    }

    @Override
    public int hashCode()
    {
        return type.hashCode() ^ id;
    }
}
