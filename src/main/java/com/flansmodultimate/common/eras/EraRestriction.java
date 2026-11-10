package com.flansmodultimate.common.eras;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * A time window equipment must fit in, with the policies that say how an item's service is compared with it. Written as
 * one line, for example {@code between WW2 1946 match=introduction undated=block}: a boundary mode, its one or two
 * dates, then optional {@code key=value} policies. Every date resolves against an {@link EraCatalog}; a restriction
 * naming an era the catalog lacks is reported by {@link #validate} and must not be checked against items.
 */
public record EraRestriction(BoundaryMode mode, @Nullable ServiceDate lower, @Nullable ServiceDate upper, Policies policies)
{
    private static final String MATCH = "match";
    private static final String RETIREMENT = "retirement";
    private static final String UNDATED = "undated";
    private static final String AMMO = "ammo";
    private static final String DESPAWN = "despawn";

    @Getter
    @RequiredArgsConstructor
    public enum BoundaryMode
    {
        /** Every month the one value covers. */
        EQUAL("equal", 2),
        /** From the first month of the lower value to the last month of the upper, inclusive. */
        BETWEEN("between", 3),
        /** Up to the last month of the value, with no lower bound. */
        UNTIL("until", 2),
        /** From the first month of the value, with no upper bound. */
        NOT_BEFORE("notbefore", 2);

        private final String key;
        /** Tokens the mode takes, its own included. */
        private final int tokens;
    }

    @Getter
    @RequiredArgsConstructor
    public enum MatchMode
    {
        /** The item passes when its service overlaps the window. */
        SERVICE("service"),
        /** The item passes when it was introduced inside the window. */
        INTRODUCTION("introduction");

        private final String key;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RetirementPolicy
    {
        /** An item with no retirement date leaves service when the latest-ending era containing its introduction ends. */
        ERA_END("eraend"),
        /** An item with no retirement date is still in service. */
        OPEN("open");

        private final String key;
    }

    @Getter
    @RequiredArgsConstructor
    public enum UndatedPolicy
    {
        ALLOW("allow"), BLOCK("block");

        private final String key;
    }

    /** How items are compared with the window; {@link #DEFAULT} unless a policy is written out. */
    public record Policies(MatchMode match, RetirementPolicy retirement, UndatedPolicy undated, boolean ammo, boolean despawn)
    {
        public static final Policies DEFAULT = new Policies(MatchMode.SERVICE, RetirementPolicy.ERA_END, UndatedPolicy.ALLOW, false, false);
    }

    public EraRestriction
    {
        if ((mode == BoundaryMode.EQUAL || mode == BoundaryMode.BETWEEN) && (lower == null || upper == null))
            throw new IllegalArgumentException(mode.getKey() + " needs both bounds");
        if (mode == BoundaryMode.UNTIL && (lower != null || upper == null))
            throw new IllegalArgumentException("until takes only an upper bound");
        if (mode == BoundaryMode.NOT_BEFORE && (lower == null || upper != null))
            throw new IllegalArgumentException("notbefore takes only a lower bound");
    }

    /** A window with the default policies: whole service, era-end retirement, undated items allowed, ammunition exempt. */
    public static EraRestriction of(BoundaryMode mode, @Nullable ServiceDate lower, @Nullable ServiceDate upper)
    {
        return new EraRestriction(mode, lower, upper, Policies.DEFAULT);
    }

    /**
     * Reads the one-line form. Dates are single words, so an era with a space in its name is written without it
     * ({@code ColdWar}).
     *
     * @throws IllegalArgumentException with a message fit to show the user
     */
    public static EraRestriction parse(String text)
    {
        String[] tokens = text == null ? new String[0] : text.strip().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty())
            throw new IllegalArgumentException("Expected equal, between, until or notbefore followed by dates");
        BoundaryMode mode = byKey(BoundaryMode.values(), tokens[0], BoundaryMode::getKey, "boundary mode");
        if (tokens.length < mode.getTokens() || tokens[mode.getTokens() - 1].contains("="))
            throw new IllegalArgumentException(mode.getKey() + " takes " + (mode.getTokens() - 1) + (mode.getTokens() == 2 ? " date" : " dates"));
        ServiceDate first = ServiceDate.parse(tokens[1]);
        ServiceDate second = mode == BoundaryMode.BETWEEN ? ServiceDate.parse(tokens[2]) : null;
        ServiceDate lower = switch (mode)
        {
            case EQUAL, NOT_BEFORE, BETWEEN -> first;
            case UNTIL -> null;
        };
        ServiceDate upper = switch (mode)
        {
            case EQUAL, UNTIL -> first;
            case BETWEEN -> second;
            case NOT_BEFORE -> null;
        };
        Policies defaults = Policies.DEFAULT;
        MatchMode match = defaults.match();
        RetirementPolicy retirement = defaults.retirement();
        UndatedPolicy undated = defaults.undated();
        boolean ammo = defaults.ammo();
        boolean despawn = defaults.despawn();
        List<String> seen = new ArrayList<>();
        for (String token : Arrays.copyOfRange(tokens, mode.getTokens(), tokens.length))
        {
            int equals = token.indexOf('=');
            if (equals <= 0 || equals == token.length() - 1)
                throw new IllegalArgumentException("Expected a policy as key=value but found '" + token + "'");
            String key = token.substring(0, equals).toLowerCase(Locale.ROOT);
            String value = token.substring(equals + 1);
            if (seen.contains(key))
                throw new IllegalArgumentException("Policy '" + key + "' is given twice");
            seen.add(key);
            switch (key)
            {
                case MATCH -> match = byKey(MatchMode.values(), value, MatchMode::getKey, MATCH);
                case RETIREMENT -> retirement = byKey(RetirementPolicy.values(), value, RetirementPolicy::getKey, RETIREMENT);
                case UNDATED -> undated = byKey(UndatedPolicy.values(), value, UndatedPolicy::getKey, UNDATED);
                case AMMO -> ammo = toggle(AMMO, value);
                case DESPAWN -> despawn = toggle(DESPAWN, value);
                default -> throw new IllegalArgumentException("Unknown policy '" + key + "'; expected match, retirement, undated, ammo or despawn");
            }
        }
        return new EraRestriction(mode, lower, upper, new Policies(match, retirement, undated, ammo, despawn));
    }

    /** A reason this restriction cannot be checked against those eras, such as an unknown era or reversed bounds. */
    public Optional<String> validate(EraCatalog eras)
    {
        for (ServiceDate bound : new ServiceDate[]{lower, upper})
            if (bound != null && !bound.isResolvable(eras))
                return Optional.of("Unknown era '" + bound + "'");
        if (lowerMonth(eras) > upperMonth(eras))
            return Optional.of("The lower bound is later than the upper bound");
        return Optional.empty();
    }

    /** First month of the window, or {@link ServiceDate#OPEN_LOW}. */
    public int lowerMonth(EraCatalog eras)
    {
        return lower == null ? ServiceDate.OPEN_LOW : lower.first(eras);
    }

    /** Last month of the window, or {@link ServiceDate#OPEN_HIGH}. */
    public int upperMonth(EraCatalog eras)
    {
        return upper == null ? ServiceDate.OPEN_HIGH : upper.last(eras);
    }

    /**
     * Whether an item with this service may be used. An undated item, or one whose dates name an era the catalog lacks,
     * follows the {@link #undated} policy. Imprecise dates are read leniently: an introduction counts from the first
     * month it covers and a retirement until the last.
     */
    public boolean allows(@Nullable ServicePeriod period, EraCatalog eras)
    {
        if (period == null || !period.isResolvable(eras))
            return policies.undated() == UndatedPolicy.ALLOW;
        int introduced = period.introduction().first(eras);
        int lowest = lowerMonth(eras);
        int highest = upperMonth(eras);
        if (policies.match() == MatchMode.INTRODUCTION)
            return lowest <= introduced && introduced <= highest;
        return introduced <= highest && retiredMonth(period, introduced, eras) >= lowest;
    }

    /** The last month the item counts as in service under this restriction's retirement policy, for drawing it. */
    public int serviceEnd(ServicePeriod period, EraCatalog eras)
    {
        return retiredMonth(period, period.introduction().first(eras), eras);
    }

    /**
     * The last month of service. A dated item never serves into the future, however open its service, so a window that
     * reaches only the future admits science fiction alone.
     */
    private int retiredMonth(ServicePeriod period, int introduced, EraCatalog eras)
    {
        if (introduced == ServiceDate.FUTURE)
            return ServiceDate.OPEN_HIGH;
        int retired;
        if (period.retirement() != null)
            retired = period.retirement().last(eras);
        else
            retired = policies.retirement() == RetirementPolicy.OPEN ? ServiceDate.OPEN_HIGH : eras.eraEnd(introduced);
        return Math.min(ServiceDate.FUTURE - 1, Math.max(introduced, retired));
    }

    /** The one-line form, with only the policies that differ from their defaults. */
    @Override
    public String toString()
    {
        StringBuilder text = new StringBuilder(mode.getKey());
        switch (mode)
        {
            case EQUAL, NOT_BEFORE -> text.append(' ').append(lower);
            case UNTIL -> text.append(' ').append(upper);
            case BETWEEN -> text.append(' ').append(lower).append(' ').append(upper);
        }
        Policies defaults = Policies.DEFAULT;
        if (policies.match() != defaults.match())
            text.append(' ').append(MATCH).append('=').append(policies.match().getKey());
        if (policies.retirement() != defaults.retirement())
            text.append(' ').append(RETIREMENT).append('=').append(policies.retirement().getKey());
        if (policies.undated() != defaults.undated())
            text.append(' ').append(UNDATED).append('=').append(policies.undated().getKey());
        if (policies.ammo())
            text.append(' ').append(AMMO).append("=on");
        if (policies.despawn())
            text.append(' ').append(DESPAWN).append("=on");
        return text.toString();
    }

    private static boolean toggle(String key, String value)
    {
        return switch (value.toLowerCase(Locale.ROOT))
        {
            case "on", "true" -> true;
            case "off", "false" -> false;
            default -> throw new IllegalArgumentException("Policy '" + key + "' expects on or off");
        };
    }

    private static <E> E byKey(E[] values, String key, Function<E, String> keyOf, String what)
    {
        for (E value : values)
            if (keyOf.apply(value).equalsIgnoreCase(key))
                return value;
        throw new IllegalArgumentException("Unknown " + what + " '" + key + "'; expected one of " + String.join(", ", Arrays.stream(values).map(keyOf).toList()));
    }
}
