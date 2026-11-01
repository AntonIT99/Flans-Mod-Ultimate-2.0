package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.combat.NpcSkill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * What a bot difficulty trains bots to: factors on the aim and the range within which they notice enemies that their
 * loadout or clone gives them, and the human limits on their fighting. The preset levels have fixed values; the custom
 * level takes those an operator chose. No value makes a bot better than its equipment.
 *
 * @param accuracy
 *            factor, above 0 up to 1, on the ranged accuracy
 * @param aggro
 *            factor, above 0 up to 1, on the range within which bots notice enemies
 * @param skill
 *            reaction, aim settling, lead and damage to players
 */
public record BotTraining(float accuracy, float aggro, NpcSkill skill)
{
    // The bounds the editor and the commands keep the custom values within.
    public static final int LEAST_PERCENT = 1;
    public static final int MOST_PERCENT = 100;
    public static final int MOST_REACTION_TICKS = 200;
    public static final int MOST_SETTLE_TICKS = 400;
    public static final float MOST_ERROR = 45F;

    private static final String NBT_ACCURACY = "accuracy";
    private static final String NBT_AGGRO = "aggro";
    private static final String NBT_REACTION = "reaction";
    private static final String NBT_SETTLE = "settle";
    private static final String NBT_ERROR = "error";
    private static final String NBT_LEAD = "lead";
    private static final String NBT_PLAYER_DAMAGE = "player_damage";
    private static final String NBT_NON_LETHAL = "non_lethal";
    private static final float PERCENT = 100F;
    /** Steps of the aim error per degree, as the NPC settings keep it. */
    private static final float ERROR_STEPS = 10F;

    public BotTraining
    {
        accuracy = bounded(accuracy);
        aggro = bounded(aggro);
        skill = new NpcSkill(Mth.clamp(skill.reactionTicks(), 0, MOST_REACTION_TICKS), Mth.clamp(skill.settleTicks(), 0, MOST_SETTLE_TICKS),
            Math.round(Mth.clamp(finite(skill.startError(), 0F), 0F, MOST_ERROR) * ERROR_STEPS) / ERROR_STEPS, Mth.clamp(finite(skill.lead(), 1F), 0F, 1F),
            Mth.clamp(finite(skill.playerDamage(), 1F), 0F, 1F), skill.nonLethal());
    }

    /**
     * The training from values as the editor and the commands give them.
     *
     * @param accuracy
     *            percent of the ranged accuracy
     * @param aggro
     *            percent of the range within which bots notice enemies
     * @param reactionTicks
     *            ticks between first seeing a target and the first shot at it
     * @param settleTicks
     *            ticks over which the extra aim error dies away
     * @param startError
     *            greatest extra aim error, in degrees, of the first shot
     * @param lead
     *            percent of a moving target's run allowed for
     * @param playerDamage
     *            percent of the damage dealt to players
     * @param nonLethal
     *            whether hits leave a player at least half a heart
     */
    @SuppressWarnings("java:S107") // Every value a difficulty sets, as the editor and the command list them.
    public static BotTraining of(int accuracy, int aggro, int reactionTicks, int settleTicks, float startError, int lead, int playerDamage, boolean nonLethal)
    {
        return new BotTraining(accuracy / PERCENT, aggro / PERCENT, new NpcSkill(reactionTicks, settleTicks, startError, lead / PERCENT, playerDamage / PERCENT, nonLethal));
    }

    public int accuracyPercent()
    {
        return Math.round(accuracy * PERCENT);
    }

    public int aggroPercent()
    {
        return Math.round(aggro * PERCENT);
    }

    public int leadPercent()
    {
        return Math.round(skill.lead() * PERCENT);
    }

    public int playerDamagePercent()
    {
        return Math.round(skill.playerDamage() * PERCENT);
    }

    /** Whether the training changes the stats of a bot's loadout or clone at all. */
    public boolean scalesStats()
    {
        return Float.compare(accuracy, 1F) != 0 || Float.compare(aggro, 1F) != 0;
    }

    /** @return the values, separated by spaces, in the order {@code difficulty custom} takes them */
    public String commandArguments()
    {
        return String.join(" ", Integer.toString(accuracyPercent()), Integer.toString(aggroPercent()), Integer.toString(skill.reactionTicks()), Integer.toString(skill.settleTicks()),
            Float.toString(skill.startError()), Integer.toString(leadPercent()), Integer.toString(playerDamagePercent()), Boolean.toString(skill.nonLethal()));
    }

    /** @return a name that differs between trainings of different values, so a bot trained to other values is trained again */
    String id()
    {
        return "custom " + commandArguments().toLowerCase(Locale.ROOT);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(NBT_ACCURACY, accuracy);
        tag.putFloat(NBT_AGGRO, aggro);
        tag.putInt(NBT_REACTION, skill.reactionTicks());
        tag.putInt(NBT_SETTLE, skill.settleTicks());
        tag.putFloat(NBT_ERROR, skill.startError());
        tag.putFloat(NBT_LEAD, skill.lead());
        tag.putFloat(NBT_PLAYER_DAMAGE, skill.playerDamage());
        tag.putBoolean(NBT_NON_LETHAL, skill.nonLethal());
        return tag;
    }

    /** @return the saved training, or the fallback when none was saved */
    public static BotTraining load(CompoundTag tag, BotTraining fallback)
    {
        if (!tag.contains(NBT_ACCURACY))
            return fallback;
        return new BotTraining(tag.getFloat(NBT_ACCURACY), tag.getFloat(NBT_AGGRO),
            new NpcSkill(tag.getInt(NBT_REACTION), tag.getInt(NBT_SETTLE), tag.getFloat(NBT_ERROR), tag.getFloat(NBT_LEAD), tag.getFloat(NBT_PLAYER_DAMAGE), tag.getBoolean(NBT_NON_LETHAL)));
    }

    private static float bounded(float factor)
    {
        return Mth.clamp(finite(factor, 1F), LEAST_PERCENT / PERCENT, 1F);
    }

    private static float finite(float value, float fallback)
    {
        return Float.isFinite(value) ? value : fallback;
    }
}
