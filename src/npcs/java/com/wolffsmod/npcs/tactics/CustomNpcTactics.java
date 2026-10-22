package com.wolffsmod.npcs.tactics;

import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Restored Custom NPCs tactical settings, carried by DataAI and its existing save/menu transport. */
@Getter
public final class CustomNpcTactics
{
    public static final int RUSH = 0;
    public static final int STAGGER = 1;
    public static final int SURROUND = 2;
    public static final int HIT_AND_RUN = 3;
    public static final int AMBUSH = 4;
    public static final int STALK = 5;
    public static final int NONE = 6;
    private static final String RADIUS_KEY = "TacticalRadius";
    private int variant;
    private int range = 4;

    public void setVariant(int value)
    {
        variant = Mth.clamp(value, RUSH, NONE);
    }

    public void setRange(int value)
    {
        range = Mth.clamp(value, 1, 4096);
    }

    public void load(CompoundTag tag)
    {
        setVariant(tag.getInt("TacticalVariant"));
        setRange(tag.contains(RADIUS_KEY) ? tag.getInt(RADIUS_KEY) : 4);
    }

    public void save(CompoundTag tag)
    {
        tag.putInt("TacticalVariant", variant);
        tag.putInt(RADIUS_KEY, range);
    }

    public static CustomNpcTactics of(EntityNPCInterface npc)
    {
        return ((Holder) npc.ais).wolffsmodnpcsCustomTactics();
    }

    /** Implemented by the existing data mixin; only DataAI consumes these settings. */
    public interface Holder
    {
        CustomNpcTactics wolffsmodnpcsCustomTactics();
    }
}
