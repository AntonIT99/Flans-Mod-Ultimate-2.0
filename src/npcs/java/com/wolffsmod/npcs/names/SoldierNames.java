package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import nikedemos.markovnames.generators.MarkovGenerator;

/** Bundled modern and world-war-era lists, with append-only numeric slots for Custom NPCs' existing packets and scripts. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoldierNames
{
    public static final int FIRST_ID = 10;
    public static final String SAVE_KEY = "WolffsModNameList";
    public static final String ROMAN_ID = "customnpcs:roman";
    private static final List<String> IDS = List.of("german_21st", "british_21st", "american_21st", "russian_21st", "german_20th", "british_20th", "american_20th", "russian_20th", "canadian_20th",
        "italian_20th", "french_20th", "dutch_20th", "chinese_20th", "polish_20th", "hungarian_20th", "finnish_20th", "romanian_20th", "jewish_20th", "estonian_20th", "latvian_20th", "croatian_20th",
        "spanish_20th", "serbian_20th", "bulgarian_20th", "swedish_20th", "greek_20th", "ukrainian_20th", "czech_20th", "slovak_20th", "lithuanian_20th", "norwegian_20th", "danish_20th",
        "belgian_20th", "portuguese_20th", "soviet_20th", "japanese_20th", "clone_ct", "clone_cc", "clone_arc", "clone_rc", "clone_cl", "clone_cs", "clone_alpha", "clone_null", "clone_rex",
        "clone_cody", "imperial_tk", "imperial_td", "imperial_ts", "imperial_tb", "imperial_rs", "imperial_tx", "imperial_tk_legends", "droid_b1", "droid_oom", "droid_b2", "droid_bx",
        "droid_tactical", "droid_magnaguard", "droid_droideka", "french_21st", "jedi_knight", "jedi_master", "jedi_temple_guard", "jedi_wayfarer", "sith_apprentice", "sith_warrior", "sith_assassin",
        "sith_marauder", "sith_lord");
    private static final List<SoldierNameList> LISTS = IDS.stream().map(SoldierNames::load).toList();

    public static List<String> ids()
    {
        return IDS;
    }

    private static SoldierNameList load(String id)
    {
        String path = "/data/wolffsmodnpcs/names/" + id + ".json";
        try (var stream = SoldierNames.class.getResourceAsStream(path))
        {
            if (stream == null)
                throw new IllegalStateException("Missing soldier name list: " + path);
            SoldierNameList list = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), SoldierNameList.class);
            if (!id.equals(list.id()))
                throw new IllegalStateException("Soldier name list ID differs from its file: " + path);
            return list;
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Cannot read soldier name list: " + path, exception);
        }
    }

    public static boolean contains(int generator)
    {
        return generator >= FIRST_ID && generator < FIRST_ID + LISTS.size();
    }

    public static String generate(int generator, int gender)
    {
        if (generator == 0)
            return MarkovGenerator.fetch(0, gender);
        return LISTS.get(generator - FIRST_ID).generate(gender, ThreadLocalRandom.current());
    }

    /** Stable IDs accepted by class-picker associations, including the original Roman generator in slot zero. */
    public static boolean acceptsAssociation(String id)
    {
        return ROMAN_ID.equals(id) || contains(generatorId(id));
    }

    public static String associationId(int generator)
    {
        return generator == 0 ? ROMAN_ID : stableId(generator);
    }

    public static String stableId(int generator)
    {
        return contains(generator) ? "wolffsmodnpcs:" + LISTS.get(generator - FIRST_ID).id() : "";
    }

    /** Unknown saved lists fall back to the original Roman generator, never a different national list. */
    public static int generatorId(String stableId)
    {
        for (int index = 0; index < LISTS.size(); index++)
            if (stableId(FIRST_ID + index).equals(stableId))
                return FIRST_ID + index;
        return 0;
    }

    public static void saveSelection(int generator, CompoundTag tag)
    {
        String id = stableId(generator);
        if (id.isEmpty())
            tag.remove(SAVE_KEY);
        else
            tag.putString(SAVE_KEY, id);
    }

    public static int loadSelection(int legacyGenerator, CompoundTag tag)
    {
        return tag.contains(SAVE_KEY, Tag.TAG_STRING) ? generatorId(tag.getString(SAVE_KEY)) : legacyGenerator;
    }

    public static String[] appendLabels(String[] original)
    {
        if (original.length != FIRST_ID)
            throw new IllegalStateException("Custom NPCs name generator slots changed");
        String[] labels = Arrays.copyOf(original, FIRST_ID + LISTS.size());
        for (int index = 0; index < LISTS.size(); index++)
            labels[FIRST_ID + index] = "wolffsmodnpcs.names." + LISTS.get(index).id();
        return labels;
    }
}
