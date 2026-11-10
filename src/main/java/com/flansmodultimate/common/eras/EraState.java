package com.flansmodultimate.common.eras;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * What a client is told so it can hide, grey out and explain items: the server's eras and date overrides, the
 * restrictions on that player and whether they bypass them. The server stays the authority; this only drives display.
 */
public record EraState(EraCatalog eras, Map<String, ServicePeriod> overrides, List<ScopedRestriction> restrictions, boolean bypass)
{
    private static final String KEY_ERAS = "eras";
    private static final String KEY_OVERRIDES = "overrides";
    private static final String KEY_RESTRICTIONS = "restrictions";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_KEY = "key";
    private static final String KEY_VALUE = "value";
    private static final String KEY_BYPASS = "bypass";

    public static final EraState EMPTY = new EraState(EraCatalog.defaults(), Map.of(), List.of(), false);

    public EraState
    {
        overrides = Map.copyOf(overrides);
        restrictions = List.copyOf(restrictions);
    }

    public JsonObject toJson()
    {
        JsonObject root = new JsonObject();
        root.add(KEY_ERAS, eras.toJson());
        JsonObject overrideObject = new JsonObject();
        new TreeMap<>(overrides).forEach((shortname, period) -> overrideObject.addProperty(shortname, period.toString()));
        root.add(KEY_OVERRIDES, overrideObject);
        JsonArray restrictionArray = new JsonArray();
        for (ScopedRestriction scoped : restrictions)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty(KEY_SCOPE, scoped.scope().kind().getId());
            entry.addProperty(KEY_KEY, scoped.scope().key());
            entry.addProperty(KEY_VALUE, scoped.restriction().toString());
            restrictionArray.add(entry);
        }
        root.add(KEY_RESTRICTIONS, restrictionArray);
        root.addProperty(KEY_BYPASS, bypass);
        return root;
    }

    /** Reads what {@link #toJson} wrote, skipping entries it cannot read rather than refusing the whole state. */
    public static EraState fromJson(JsonObject root)
    {
        EraCatalog eras = root.has(KEY_ERAS) && root.get(KEY_ERAS).isJsonObject() ? EraCatalog.fromJson(root.getAsJsonObject(KEY_ERAS)).catalog() : EraCatalog.defaults();
        Map<String, ServicePeriod> overrides = new TreeMap<>();
        if (root.has(KEY_OVERRIDES) && root.get(KEY_OVERRIDES).isJsonObject())
        {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(KEY_OVERRIDES).entrySet())
                readOverride(entry.getValue()).ifPresent(period -> overrides.put(entry.getKey(), period));
        }
        List<ScopedRestriction> restrictions = new ArrayList<>();
        if (root.has(KEY_RESTRICTIONS) && root.get(KEY_RESTRICTIONS).isJsonArray())
        {
            for (JsonElement element : root.getAsJsonArray(KEY_RESTRICTIONS))
                readRestriction(element).ifPresent(restrictions::add);
        }
        boolean bypass = root.has(KEY_BYPASS) && root.get(KEY_BYPASS).isJsonPrimitive() && root.get(KEY_BYPASS).getAsBoolean();
        return new EraState(eras, overrides, restrictions, bypass);
    }

    /** An entry the client cannot read leaves that item to its own dates. */
    private static Optional<ServicePeriod> readOverride(JsonElement value)
    {
        try
        {
            return Optional.of(ServicePeriod.parse(value.getAsString()));
        }
        catch (RuntimeException exception)
        {
            return Optional.empty();
        }
    }

    /** The server checks every restriction itself, so one the client cannot read only goes unshown. */
    private static Optional<ScopedRestriction> readRestriction(JsonElement element)
    {
        try
        {
            JsonObject entry = element.getAsJsonObject();
            EraScope scope = new EraScope(EraScope.Kind.byId(entry.get(KEY_SCOPE).getAsString()), entry.get(KEY_KEY).getAsString());
            return Optional.of(new ScopedRestriction(scope, EraRestriction.parse(entry.get(KEY_VALUE).getAsString())));
        }
        catch (RuntimeException exception)
        {
            return Optional.empty();
        }
    }
}
