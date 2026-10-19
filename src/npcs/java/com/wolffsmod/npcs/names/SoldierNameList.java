package com.wolffsmod.npcs.names;

import java.util.List;
import java.util.random.RandomGenerator;

/** Whole-name pools; a group is selected before its components so naming traditions remain coherent. */
public record SoldierNameList(String id, List<Group> groups)
{
    public SoldierNameList
    {
        if (id == null || !id.matches("[a-z][a-z0-9_]*"))
            throw new IllegalArgumentException("Invalid soldier name list ID");
        groups = List.copyOf(groups);
        if (groups.isEmpty() || groups.stream().mapToLong(Group::weight).sum() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid soldier name group weights: " + id);
    }

    public String generate(int gender, RandomGenerator random)
    {
        int ticket = random.nextInt(groups.stream().mapToInt(Group::weight).sum());
        for (Group group : groups)
        {
            ticket -= group.weight();
            if (ticket < 0)
                return group.generate(gender, random);
        }
        throw new IllegalStateException("No soldier name group selected");
    }

    /** Female surnames are explicit where the language has gendered forms, rather than inferred by suffix. */
    public record Group(int weight, List<String> male, List<String> female, List<String> surnames, List<String> femaleSurnames, boolean surnameFirst)
    {
        public Group(int weight, List<String> male, List<String> female, List<String> surnames, List<String> femaleSurnames)
        {
            this(weight, male, female, surnames, femaleSurnames, false);
        }

        public Group
        {
            if (weight <= 0)
                throw new IllegalArgumentException("Soldier name group weight must be positive");
            male = names(male);
            female = names(female);
            surnames = names(surnames);
            femaleSurnames = femaleSurnames == null ? surnames : names(femaleSurnames);
        }

        private static List<String> names(List<String> values)
        {
            List<String> result = List.copyOf(values);
            if (result.isEmpty() || result.stream()
                .anyMatch(name -> name.isBlank() || !name.equals(name.strip()) || name.length() > 40 || name.codePoints().anyMatch(character -> Character.isISOControl(character) || character == '§')))
                throw new IllegalArgumentException("Invalid or empty soldier name pool");
            if (result.stream().distinct().count() != result.size())
                throw new IllegalArgumentException("Duplicate soldier name; use group weights instead");
            return result;
        }

        private String generate(int gender, RandomGenerator random)
        {
            boolean woman = gender == 2 || (gender != 1 && random.nextBoolean());
            List<String> given = woman ? female : male;
            List<String> family = woman ? femaleSurnames : surnames;
            String first = given.get(random.nextInt(given.size()));
            String last = family.get(random.nextInt(family.size()));
            return surnameFirst ? last + " " + first : first + " " + last;
        }
    }
}
