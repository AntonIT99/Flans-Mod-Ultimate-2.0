package com.flansmodultimate.common.eras;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The eras a server recognises, as read from {@code eras.json}. Immutable; a reload builds a new catalog. */
public final class EraCatalog
{
    public static final int FORMAT_VERSION = 1;
    private static final String KEY_VERSION = "version";
    private static final String KEY_ERAS = "eras";
    private static final String KEY_ID = "id";
    private static final String KEY_FROM = "from";
    private static final String KEY_TO = "to";
    private static final String KEY_ALIASES = "aliases";

    private static final EraCatalog DEFAULTS = new EraCatalog(List.of(
        era("Prehistory", null, "3001BC"),
        era("Antiquity", "3000BC", "476"),
        era("Medieval", "476", "1492", "MiddleAges"),
        era("EarlyModern", "1492", "1800"),
        era("Napoleonic", "1792-04", "1815-11"),
        era("WW1", "1914-07", "1918-11", "GreatWar"),
        era("Interwar", "1918-11", "1939-08"),
        era("WW2", "1939-09", "1945-09"),
        era("ColdWar", "1947-03", "1991-12"),
        era("DigitalAge", "1990", null, "Modern")));

    private final List<Era> eras;

    public record Loaded(EraCatalog catalog, List<String> problems)
    {}

    private EraCatalog(List<Era> eras)
    {
        this.eras = List.copyOf(eras);
    }

    public static EraCatalog defaults()
    {
        return DEFAULTS;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof EraCatalog catalog && eras.equals(catalog.eras);
    }

    @Override
    public int hashCode()
    {
        return eras.hashCode();
    }

    public List<Era> getEras()
    {
        return eras;
    }

    /** The era with this id or alias, compared as {@link Era#normalize} does, or {@code null}. */
    @Nullable
    public Era find(@Nullable String name)
    {
        if (name == null)
            return null;
        String normalized = Era.normalize(name);
        for (Era era : eras)
            if (era.isNamed(normalized))
                return era;
        return null;
    }

    /**
     * The month a service starting in {@code month} ends when no retirement is known: the end of the latest-ending era
     * containing it, or {@link ServiceDate#OPEN_HIGH} when an era containing it is open or none does.
     */
    public int eraEnd(int month)
    {
        int end = Integer.MIN_VALUE;
        for (Era era : eras)
            if (era.contains(month))
                end = Math.max(end, era.last());
        return end == Integer.MIN_VALUE ? ServiceDate.OPEN_HIGH : end;
    }

    /** Reads a catalog, skipping and reporting entries that cannot be used rather than failing the whole file. */
    public static Loaded fromJson(JsonObject root)
    {
        List<String> problems = new ArrayList<>();
        if (root.has(KEY_VERSION) && !isFormatVersion(root.get(KEY_VERSION)))
            problems.add("Unsupported version " + root.get(KEY_VERSION) + "; reading it as version " + FORMAT_VERSION);
        if (!root.has(KEY_ERAS) || !root.get(KEY_ERAS).isJsonArray())
        {
            problems.add("Missing \"" + KEY_ERAS + "\" array; using the default eras");
            return new Loaded(DEFAULTS, problems);
        }
        List<Era> eras = new ArrayList<>();
        Set<String> names = new HashSet<>();
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(KEY_ERAS))
        {
            index++;
            try
            {
                Era era = readEra(element);
                for (String name : allNames(era))
                    if (Era.isFutureName(name) || !names.add(Era.normalize(name)))
                        throw new IllegalArgumentException("the name '" + name + "' is reserved or already used");
                eras.add(era);
            }
            catch (RuntimeException exception)
            {
                problems.add("Era #" + index + " skipped: " + exception.getMessage());
            }
        }
        return new Loaded(new EraCatalog(eras), problems);
    }

    public JsonObject toJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty(KEY_VERSION, FORMAT_VERSION);
        JsonArray array = new JsonArray();
        for (Era era : eras)
        {
            JsonObject object = new JsonObject();
            object.addProperty(KEY_ID, era.id());
            if (era.first() != ServiceDate.OPEN_LOW)
                object.addProperty(KEY_FROM, ServiceDate.formatMonth(era.first()));
            if (era.last() != ServiceDate.OPEN_HIGH)
                object.addProperty(KEY_TO, ServiceDate.formatMonth(era.last()));
            if (!era.aliases().isEmpty())
            {
                JsonArray aliases = new JsonArray();
                era.aliases().forEach(aliases::add);
                object.add(KEY_ALIASES, aliases);
            }
            array.add(object);
        }
        root.add(KEY_ERAS, array);
        return root;
    }

    private static boolean isFormatVersion(JsonElement version)
    {
        return version instanceof JsonPrimitive primitive && primitive.isNumber() && primitive.getAsDouble() == FORMAT_VERSION;
    }

    private static Era readEra(JsonElement element)
    {
        if (!element.isJsonObject())
            throw new IllegalArgumentException("expected an object");
        JsonObject object = element.getAsJsonObject();
        String id = string(object, KEY_ID);
        if (id == null || !Era.normalize(id).matches("[a-z][a-z0-9]*"))
            throw new IllegalArgumentException("\"id\" must start with a letter and use letters and digits");
        List<String> aliases = new ArrayList<>();
        if (object.has(KEY_ALIASES))
        {
            if (!object.get(KEY_ALIASES).isJsonArray())
                throw new IllegalArgumentException("\"aliases\" must be an array of names");
            for (JsonElement alias : object.getAsJsonArray(KEY_ALIASES))
                aliases.add(alias.getAsString());
        }
        String from = string(object, KEY_FROM);
        String to = string(object, KEY_TO);
        return new Era(id, aliases, from == null ? ServiceDate.OPEN_LOW : bound(from).first(empty()), to == null ? ServiceDate.OPEN_HIGH : bound(to).last(empty()));
    }

    @Nullable
    private static String string(JsonObject object, String key)
    {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull())
            return null;
        if (!(value instanceof JsonPrimitive primitive) || primitive.isBoolean())
            throw new IllegalArgumentException("\"" + key + "\" must be text");
        return primitive.getAsString();
    }

    /** An era bound must be a month, year or century: an era defined by another era or the future could not be ordered. */
    private static ServiceDate bound(String text)
    {
        ServiceDate date = ServiceDate.parse(text);
        if (date.precision() == ServiceDate.Precision.ERA || date.precision() == ServiceDate.Precision.FUTURE)
            throw new IllegalArgumentException("bound '" + text + "' must be a month, year or century");
        return date;
    }

    private static List<String> allNames(Era era)
    {
        List<String> names = new ArrayList<>(era.aliases());
        names.add(0, era.id());
        return names;
    }

    private static Era era(String id, @Nullable String from, @Nullable String to, String... aliases)
    {
        int first = from == null ? ServiceDate.OPEN_LOW : ServiceDate.parse(from).first(EraCatalog.empty());
        int last = to == null ? ServiceDate.OPEN_HIGH : ServiceDate.parse(to).last(EraCatalog.empty());
        return new Era(id, List.of(aliases), first, last);
    }

    private static EraCatalog empty()
    {
        return new EraCatalog(List.of());
    }
}
