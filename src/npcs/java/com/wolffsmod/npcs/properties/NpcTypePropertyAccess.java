package com.wolffsmod.npcs.properties;

import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Custom NPCs boundary for the reversible inheritance overlay. */
public final class NpcTypePropertyAccess
{
    private NpcTypePropertyAccess()
    {}

    public static Tag read(EntityNPCInterface npc, NpcTypeProperty property)
    {
        return switch (property)
        {
            case HEALTH -> IntTag.valueOf(npc.stats.maxHealth);
            case AGGRO_RANGE -> IntTag.valueOf(npc.stats.aggroRange);
            case CAN_DROWN -> ByteTag.valueOf(npc.stats.canDrown);
            case NO_FALL_DAMAGE -> ByteTag.valueOf(npc.stats.noFallDamage);
            case DAMAGE -> IntTag.valueOf(npc.stats.ranged.getStrength());
            case ACCURACY -> IntTag.valueOf(npc.stats.ranged.getAccuracy());
            case SPEED -> IntTag.valueOf(npc.stats.ranged.getSpeed());
            case RANGE -> IntTag.valueOf(npc.stats.ranged.getRange());
            case DELAY_MIN -> IntTag.valueOf(npc.stats.ranged.getDelayMin());
            case DELAY_MAX -> IntTag.valueOf(npc.stats.ranged.getDelayMax());
            case BURST_DELAY -> IntTag.valueOf(npc.stats.ranged.getBurstDelay());
            case BURST -> IntTag.valueOf(npc.stats.ranged.getBurst());
            case SHOT_COUNT -> IntTag.valueOf(npc.stats.ranged.getShotCount());
            case FIRING_SOUND -> string(npc.stats.ranged.getSound(0));
            case MELEE_REACH -> FloatTag.valueOf(npc.stats.melee.getRange());
            case WALKING_SPEED -> IntTag.valueOf(npc.ais.getWalkingSpeed());
            case IDLE_SOUND -> string(npc.advanced.getSound(0));
            case STEP_SOUND -> string(npc.advanced.getSound(4));
        };
    }

    public static void write(EntityNPCInterface npc, NpcTypeProperty property, Tag value)
    {
        switch (property)
        {
            case HEALTH -> setHealth(npc, integer(value));
            case AGGRO_RANGE -> npc.stats.setAggroRange(integer(value));
            case CAN_DROWN -> npc.stats.canDrown = bool(value);
            case NO_FALL_DAMAGE -> npc.stats.noFallDamage = bool(value);
            case DAMAGE -> npc.stats.ranged.setStrength(integer(value));
            case ACCURACY -> npc.stats.ranged.setAccuracy(integer(value));
            case SPEED -> npc.stats.ranged.setSpeed(integer(value));
            case RANGE -> npc.stats.ranged.setRange(integer(value));
            case DELAY_MIN -> npc.stats.ranged.setDelay(integer(value), Math.max(integer(value), npc.stats.ranged.getDelayMax()));
            case DELAY_MAX -> npc.stats.ranged.setDelay(Math.min(integer(value), npc.stats.ranged.getDelayMin()), integer(value));
            case BURST_DELAY -> npc.stats.ranged.setBurstDelay(integer(value));
            case BURST -> npc.stats.ranged.setBurst(integer(value));
            case SHOT_COUNT -> npc.stats.ranged.setShotCount(integer(value));
            case FIRING_SOUND -> npc.stats.ranged.setSound(0, value.getAsString());
            case MELEE_REACH -> npc.stats.melee.setRange(((NumericTag) value).getAsFloat());
            case WALKING_SPEED -> npc.ais.setWalkingSpeed(integer(value));
            case IDLE_SOUND -> npc.advanced.setSound(0, value.getAsString());
            case STEP_SOUND -> npc.advanced.setSound(4, value.getAsString());
        }
    }

    private static StringTag string(String value)
    {
        return StringTag.valueOf(value == null ? "" : value);
    }

    private static int integer(Tag value)
    {
        return ((NumericTag) value).getAsInt();
    }

    private static boolean bool(Tag value)
    {
        return ((NumericTag) value).getAsByte() != 0;
    }

    private static void setHealth(EntityNPCInterface npc, int value)
    {
        float proportion = NpcTypeHealth.fraction(npc.getHealth(), npc.getMaxHealth());
        npc.stats.setMaxHealth(value);
        // Changing models preserves the damage fraction rather than healing on every tick.
        npc.setHealth(proportion * npc.getMaxHealth());
    }
}
