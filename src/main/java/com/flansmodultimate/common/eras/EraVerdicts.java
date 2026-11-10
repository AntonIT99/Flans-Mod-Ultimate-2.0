package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Decides whether types pass restrictions, for one set of eras and dates. Results are cached per restriction and type,
 * so a use-path check is a map lookup; a change to the eras or dates builds a new instance instead of invalidating this
 * one. Not thread-safe: the server keeps one for its thread and the client one for its own.
 */
public final class EraVerdicts
{
    /** Equipment an {@code undated=block} restriction refuses; other undated types, such as parts, stay usable. */
    private static final Set<EnumType> UNDATED_BLOCKABLE = EnumSet.of(EnumType.AA_GUN, EnumType.ARMOR, EnumType.ATTACHMENT, EnumType.GRENADE, EnumType.GUN, EnumType.MECHA,
        EnumType.MECHA_ITEM, EnumType.PLANE, EnumType.VEHICLE);
    /** Restrictions held in the cache before it is emptied, which bounds it when restrictions change often. */
    private static final int MAX_CACHED_RESTRICTIONS = 64;

    private final EraCatalog eras;
    private final Function<InfoType, ServicePeriod> periods;
    private final Map<EraRestriction, Map<InfoType, Boolean>> cache = new HashMap<>();

    /** The outcome of a check: allowed, or refused by the first restriction that blocks the type. */
    public record Verdict(boolean allowed, @Nullable ScopedRestriction blockedBy, @Nullable ServicePeriod period)
    {
        public static final Verdict ALLOWED = new Verdict(true, null, null);
    }

    /** {@code periods} answers {@code null} for an undated type. */
    public EraVerdicts(EraCatalog eras, Function<InfoType, ServicePeriod> periods)
    {
        this.eras = eras;
        this.periods = periods;
    }

    public EraCatalog getEras()
    {
        return eras;
    }

    @Nullable
    public ServicePeriod periodOf(InfoType type)
    {
        return periods.apply(type);
    }

    /** Whether the restriction can be checked at all; one naming an era these eras lack is skipped. */
    public boolean isUsable(EraRestriction restriction)
    {
        return restriction.validate(eras).isEmpty();
    }

    public boolean allows(EraRestriction restriction, InfoType type)
    {
        if (cache.size() >= MAX_CACHED_RESTRICTIONS && !cache.containsKey(restriction))
            cache.clear();
        return cache.computeIfAbsent(restriction, ignored -> new IdentityHashMap<>()).computeIfAbsent(type, ignored -> compute(restriction, type));
    }

    /** The first of the restrictions that refuses the type, in their order, or {@link Verdict#ALLOWED}. */
    public Verdict check(List<ScopedRestriction> restrictions, InfoType type)
    {
        for (ScopedRestriction scoped : restrictions)
            if (isUsable(scoped.restriction()) && !allows(scoped.restriction(), type))
                return new Verdict(false, scoped, periodOf(type));
        return Verdict.ALLOWED;
    }

    private boolean compute(EraRestriction restriction, InfoType type)
    {
        return decide(restriction, type.getType(), periodOf(type), eras);
    }

    /**
     * The rule behind every verdict. Ammunition passes unless the restriction covers it ({@code ammo=on}); an undated
     * type other than equipment or covered ammunition passes even under {@code undated=block}.
     */
    public static boolean decide(EraRestriction restriction, EnumType kind, @Nullable ServicePeriod period, EraCatalog eras)
    {
        boolean ammunition = kind == EnumType.BULLET;
        if (ammunition && !restriction.policies().ammo())
            return true;
        boolean undated = period == null || !period.isResolvable(eras);
        if (undated && !ammunition && !UNDATED_BLOCKABLE.contains(kind))
            return true;
        return restriction.allows(period, eras);
    }
}
