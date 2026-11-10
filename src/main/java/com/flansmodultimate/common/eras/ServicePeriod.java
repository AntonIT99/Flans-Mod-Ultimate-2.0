package com.flansmodultimate.common.eras;

import org.jetbrains.annotations.Nullable;

/** When an item entered service and, if known, when it left it. */
public record ServicePeriod(ServiceDate introduction, @Nullable ServiceDate retirement)
{
    /**
     * Reads the {@link #toString} form: an introduction, optionally followed by a retirement.
     *
     * @throws IllegalArgumentException when it is not one or two dates
     */
    public static ServicePeriod parse(String text)
    {
        String[] dates = text.strip().split("\\s+");
        if (dates.length > 2)
            throw new IllegalArgumentException("Expected an introduction date and at most one retirement date");
        return new ServicePeriod(ServiceDate.parse(dates[0]), dates.length == 2 ? ServiceDate.parse(dates[1]) : null);
    }

    public boolean isResolvable(EraCatalog eras)
    {
        return introduction.isResolvable(eras) && (retirement == null || retirement.isResolvable(eras));
    }

    /** Whether the retirement, where given, can end no earlier than the introduction starts. */
    public boolean isOrdered(EraCatalog eras)
    {
        return retirement == null || !isResolvable(eras) || retirement.last(eras) >= introduction.first(eras);
    }

    @Override
    public String toString()
    {
        return retirement == null ? introduction.toString() : introduction + " " + retirement;
    }
}
