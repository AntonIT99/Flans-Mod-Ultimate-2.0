package com.wolffsarmormod.common.types;

import com.wolffsarmormod.common.paintjob.Paintjob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PaintableType extends InfoType
{
    //Paintjobs
    /** The list of all available paintjobs for this gun */
    @Getter
    protected ArrayList<Paintjob> paintjobs = new ArrayList<>();
    protected ArrayList<Paintjob> nonlegendarypaintjobs = new ArrayList<>();
    /** The default paintjob for this gun. This is created automatically in the load process from existing info */
    @Getter
    protected Paintjob defaultPaintjob;
    /** Whether to add this paintjob to the paintjob table, gunmode table e.t.c. */
    protected Boolean addAnyPaintjobToTables = true;
    /** Assigns IDs to paintjobs */
    private int nextPaintjobId = 1;

    protected void postRead()
    {
        defaultPaintjob = new Paintjob(this, 0, getIcon(), getTexture(), new ItemStack[0]);
        paintjobs.add(defaultPaintjob);

        super.postRead();
    }
}
