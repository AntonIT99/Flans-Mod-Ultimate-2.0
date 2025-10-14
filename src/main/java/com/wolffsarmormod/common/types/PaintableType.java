package com.wolffsarmormod.common.types;

import com.wolffsarmormod.common.paintjob.Paintjob;

import java.util.ArrayList;

public abstract class PaintableType extends InfoType
{
    //Paintjobs
    /** The list of all available paintjobs for this gun */
    public ArrayList<Paintjob> paintjobs = new ArrayList<>();
    public ArrayList<Paintjob> nonlegendarypaintjobs = new ArrayList<>();
    /** The default paintjob for this gun. This is created automatically in the load process from existing info */
    public Paintjob defaultPaintjob;
    /** Whether to add this paintjob to the paintjob table, gunmode table e.t.c. */
    public Boolean addAnyPaintjobToTables = true;
    /** Assigns IDs to paintjobs */
    private int nextPaintjobId = 1;
}
