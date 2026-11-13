package com.flansmodultimate.common.eras;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.eras.EraScopeRestriction;
import com.flansmodultimate.api.eras.IEraContext;
import com.flansmodultimate.api.eras.IEraScope;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.common.teams.TeamsEraScope;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** What the public era API delegates to: addon scopes, contexts and checks of API types. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraApiSupport
{
    /** Restrictions parsed from addon scopes, bounded since battles come and go. */
    private static final Map<String, EraRestriction> parsed = new ConcurrentHashMap<>();
    private static final int MAX_PARSED = 256;

    public static void addScope(IEraScope scope)
    {
        EraRestrictions.addProvider(new ApiScope(scope));
    }

    public static boolean isAllowed(Entity subject, IContentType type)
    {
        return !(type instanceof InfoType infoType) || EraRestrictions.isAllowed(subject, infoType);
    }

    public static boolean isClassAllowed(Entity subject, IPlayerClass playerClass)
    {
        return !(playerClass instanceof PlayerClass flansClass) || flansClass.getGrantedTypes().stream().allMatch(type -> EraRestrictions.isAllowed(subject, type));
    }

    /** The level's server and dimension restrictions and, for a round combatant, the running round's. */
    public static IEraContext contextAt(ServerLevel level, boolean roundCombatant)
    {
        List<ScopedRestriction> restrictions = new ArrayList<>(EraRestrictions.worldRestrictionsAt(level));
        if (roundCombatant)
            TeamsEraScope.currentRestriction().ifPresent(restrictions::add);
        return new Context(List.copyOf(restrictions));
    }

    /** The canonical one-line form of a restriction an addon scope may use; despawning is for server and dimension ones. */
    public static String validate(String text)
    {
        if (text.isBlank())
            return "";
        EraRestriction restriction = EraRestriction.parse(text);
        restriction.validate(IssueDates.eras()).ifPresent(problem ->
        {
            throw new IllegalArgumentException(problem);
        });
        if (restriction.policies().despawn())
            throw new IllegalArgumentException("despawn=on is only for server and dimension restrictions");
        return restriction.toString();
    }

    @Nullable
    static EraRestriction parse(String text)
    {
        EraRestriction known = parsed.get(text);
        if (known != null)
            return known;
        try
        {
            EraRestriction restriction = EraRestriction.parse(text);
            if (parsed.size() >= MAX_PARSED)
                parsed.clear();
            parsed.put(text, restriction);
            return restriction;
        }
        catch (IllegalArgumentException exception)
        {
            FlansLog.log.warn("Ignoring the unreadable era restriction '{}': {}", text, exception.getMessage());
            return null;
        }
    }

    private static ScopedRestriction battle(String key, EraRestriction restriction)
    {
        return new ScopedRestriction(new EraScope(EraScope.Kind.BATTLE, key), restriction);
    }

    /** An addon's scope, whose restrictions count as battle restrictions. */
    private record ApiScope(IEraScope scope) implements EraScopeProvider
    {
        @Override
        public void collect(Entity subject, Consumer<ScopedRestriction> restrictions)
        {
            for (EraScopeRestriction entry : scope.restrictionsFor(subject))
            {
                EraRestriction restriction = parse(entry.restriction());
                if (restriction != null)
                    restrictions.accept(battle(entry.key(), restriction));
            }
        }
    }

    private record Context(List<ScopedRestriction> restrictions) implements IEraContext
    {
        @Override
        public boolean allows(IContentType type)
        {
            return !(type instanceof InfoType infoType) || EraRestrictions.verdicts().check(restrictions, infoType).allowed();
        }

        @Override
        public boolean allowsClass(IPlayerClass playerClass)
        {
            return !(playerClass instanceof PlayerClass flansClass) || flansClass.getGrantedTypes().stream().allMatch(this::allows);
        }

        @Override
        public IEraContext with(String key, String restriction)
        {
            EraRestriction added = parse(restriction);
            if (added == null || !EraRestrictions.verdicts().isUsable(added))
                return this;
            List<ScopedRestriction> extended = new ArrayList<>(restrictions);
            extended.add(battle(key, added));
            return new Context(List.copyOf(extended));
        }
    }
}
