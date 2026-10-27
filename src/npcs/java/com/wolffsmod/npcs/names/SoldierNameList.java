package com.wolffsmod.npcs.names;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Whole-name pools, or generated designations and fixed names. National name components stay within one selected group. */
public record SoldierNameList(String id, List<Group> groups, List<String> designations, List<Serial> serials, List<String> fixedNames)
{
    public SoldierNameList(String id, List<Group> groups)
    {
        this(id, groups, null, null, null);
    }

    public SoldierNameList(String id, List<Group> groups, List<String> designations)
    {
        this(id, groups, designations, null, null);
    }

    public SoldierNameList(String id, List<Group> groups, List<String> designations, List<Serial> serials)
    {
        this(id, groups, designations, serials, null);
    }

    public SoldierNameList
    {
        if (id == null || !id.matches("[a-z][a-z0-9_]*"))
            throw new IllegalArgumentException("Invalid soldier name list ID");
        groups = groups == null ? List.of() : List.copyOf(groups);
        designations = designations == null ? List.of() : List.copyOf(designations);
        serials = serials == null ? List.of() : List.copyOf(serials);
        fixedNames = fixedNames == null ? List.of() : Group.names(fixedNames);
        if (groups.isEmpty() == (designations.isEmpty() && serials.isEmpty() && fixedNames.isEmpty()) || groups.stream().mapToLong(Group::weight).sum() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid soldier name group weights: " + id);
        if (designations.stream().anyMatch(pattern -> pattern.length() > 40 || !pattern.matches("([A-Za-z0-9]+-){0,8}#+(-#+){0,19}"))
            || designations.stream().distinct().count() != designations.size())
            throw new IllegalArgumentException("Invalid designation patterns: " + id);
    }

    public String generate(int gender, RandomGenerator random)
    {
        if (!designations.isEmpty() || !serials.isEmpty() || !fixedNames.isEmpty())
        {
            int choice = random.nextInt(designations.size() + serials.size() + fixedNames.size());
            if (choice >= designations.size() + serials.size())
                return fixedNames.get(choice - designations.size() - serials.size());
            if (choice >= designations.size())
                return serials.get(choice - designations.size()).generate(random);
            String pattern = designations.get(choice);
            StringBuilder name = new StringBuilder(pattern.length());
            for (char character : pattern.toCharArray())
                name.append(character == '#' ? (char) ('0' + random.nextInt(10)) : character);
            return name.toString();
        }
        int ticket = random.nextInt(groups.stream().mapToInt(Group::weight).sum());
        for (Group group : groups)
        {
            ticket -= group.weight();
            if (ticket < 0)
                return group.generate(gender, random);
        }
        throw new IllegalStateException("No soldier name group selected");
    }

    /** A bounded numeric pool, for small designation series such as the Alpha and Null clones. */
    public record Serial(String prefix, int min, int max)
    {
        public Serial
        {
            if (prefix == null || prefix.length() > 24 || !prefix.matches("([A-Za-z0-9]+-){1,8}") || min < 0 || max < min || max > 999999999)
                throw new IllegalArgumentException("Invalid designation serial range");
        }

        private String generate(RandomGenerator random)
        {
            return prefix + random.nextInt(min, max + 1);
        }
    }

    /**
     * Female surnames are explicit where the language has gendered forms, rather than inferred by suffix. Each optional weight map raises the chance of the named
     * entries of one pool; entries it does not list weigh 1, so common names can outweigh rare ones without duplicating them.
     */
    public record Group(int weight, List<String> male, List<String> female, List<String> surnames, List<String> femaleSurnames, boolean surnameFirst, Map<String, Integer> maleWeights,
        Map<String, Integer> femaleWeights, Map<String, Integer> surnameWeights, Map<String, Integer> femaleSurnameWeights)
    {
        private static final int MAX_NAME_WEIGHT = 1000;

        public Group(int weight, List<String> male, List<String> female, List<String> surnames, List<String> femaleSurnames)
        {
            this(weight, male, female, surnames, femaleSurnames, false);
        }

        public Group(int weight, List<String> male, List<String> female, List<String> surnames, List<String> femaleSurnames, boolean surnameFirst)
        {
            this(weight, male, female, surnames, femaleSurnames, surnameFirst, null, null, null, null);
        }

        public Group
        {
            if (weight <= 0)
                throw new IllegalArgumentException("Soldier name group weight must be positive");
            male = names(male);
            female = names(female);
            surnames = names(surnames);
            boolean sharedSurnames = femaleSurnames == null;
            femaleSurnames = sharedSurnames ? surnames : names(femaleSurnames);
            maleWeights = nameWeights(maleWeights, male);
            femaleWeights = nameWeights(femaleWeights, female);
            surnameWeights = nameWeights(surnameWeights, surnames);
            femaleSurnameWeights = sharedSurnames && femaleSurnameWeights == null ? surnameWeights : nameWeights(femaleSurnameWeights, femaleSurnames);
        }

        private static List<String> names(List<String> values)
        {
            List<String> result = List.copyOf(values);
            if (result.isEmpty() || result.stream()
                .anyMatch(name -> name.isBlank() || !name.equals(name.strip()) || name.length() > 40 || name.codePoints().anyMatch(character -> Character.isISOControl(character) || character == '§')))
                throw new IllegalArgumentException("Invalid or empty soldier name pool");
            if (result.stream().distinct().count() != result.size())
                throw new IllegalArgumentException("Duplicate soldier name; use name weights instead");
            return result;
        }

        private static Map<String, Integer> nameWeights(Map<String, Integer> weights, List<String> pool)
        {
            if (weights == null)
                return Map.of();
            if (weights.entrySet().stream().anyMatch(entry -> !pool.contains(entry.getKey()) || entry.getValue() == null || entry.getValue() < 1 || entry.getValue() > MAX_NAME_WEIGHT))
                throw new IllegalArgumentException("Soldier name weights must name pool entries and be between 1 and " + MAX_NAME_WEIGHT);
            return Map.copyOf(weights);
        }

        private static String pick(List<String> pool, Map<String, Integer> weights, RandomGenerator random)
        {
            if (weights.isEmpty())
                return pool.get(random.nextInt(pool.size()));
            long ticket = random.nextLong(pool.stream().mapToLong(name -> weights.getOrDefault(name, 1)).sum());
            for (String name : pool)
            {
                ticket -= weights.getOrDefault(name, 1);
                if (ticket < 0)
                    return name;
            }
            throw new IllegalStateException("No soldier name selected");
        }

        private String generate(int gender, RandomGenerator random)
        {
            boolean woman = gender == 2 || (gender != 1 && random.nextBoolean());
            String first = woman ? pick(female, femaleWeights, random) : pick(male, maleWeights, random);
            String last = woman ? pick(femaleSurnames, femaleSurnameWeights, random) : pick(surnames, surnameWeights, random);
            return surnameFirst ? last + " " + first : first + " " + last;
        }
    }
}
