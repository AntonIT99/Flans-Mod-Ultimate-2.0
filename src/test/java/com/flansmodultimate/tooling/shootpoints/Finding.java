package com.flansmodultimate.tooling.shootpoints;

import java.util.ArrayList;
import java.util.List;

/**
 * One value the shoot-point sync looked at, what it did with it, and how much
 * reason there is to check it by hand.
 */
final class Finding
{
    enum Kind
    {
        /** The whole definition: its model could not be measured. */
        MODEL,
        /** The single primary shoot point, from {@code ShootPointPrimary}, {@code BombPosition} or {@code BarrelPosition}. */
        PRIMARY,
        GUN_ORIGIN,
        AA_BARREL
    }

    enum Action
    {
        UPDATE,
        ADD,
        UNCHANGED,
        SKIPPED,
        FAILED
    }

    final String definition;
    final Kind kind;
    final String target;
    final float[] authored;
    final float[] measured;
    /** Distance between the authored and measured muzzle, in model pixels, or NaN. */
    final double deltaPx;
    final Action action;
    final int decimals;
    final List<String> reasons = new ArrayList<>();
    String modelClass = "";
    int score;

    Finding(String definition, Kind kind, String target, float[] authored, float[] measured, double deltaPx,
            Action action, int decimals)
    {
        this.definition = definition;
        this.kind = kind;
        this.target = target;
        this.authored = authored;
        this.measured = measured;
        this.deltaPx = deltaPx;
        this.action = action;
        this.decimals = decimals;
    }

    static Finding problem(String definition, Action action, int score, String reason)
    {
        Finding finding = new Finding(definition, Kind.MODEL, "model", null, null, Double.NaN, action, 1);
        finding.flag(score, reason);
        return finding;
    }

    void flag(int points, String reason)
    {
        score += points;
        reasons.add(reason);
    }

    boolean changesFile()
    {
        return action == Action.UPDATE || action == Action.ADD;
    }

    String authoredText()
    {
        return vector(authored);
    }

    String measuredText()
    {
        return vector(measured);
    }

    private String vector(float[] values)
    {
        if (values == null)
            return "";
        StringBuilder text = new StringBuilder();
        for (float value : values)
        {
            if (!text.isEmpty())
                text.append(' ');
            text.append(DefinitionFile.formatNumber(value, decimals));
        }
        return text.toString();
    }
}
