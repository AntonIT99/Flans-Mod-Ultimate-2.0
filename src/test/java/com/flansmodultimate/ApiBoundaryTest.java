package com.flansmodultimate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the rules in {@code src/main/java/com/flansmodultimate/api/AGENTS.md}: public API
 * signatures expose no main-mod internals, every API file carries its MIT header, and every API
 * type declares its stability.
 */
class ApiBoundaryTest
{
    private static final String API_PACKAGE = "com.flansmodultimate.api";
    private static final Path API_SOURCES = Path.of("src/main/java/com/flansmodultimate/api");

    @Test
    void apiSignaturesExposeNoInternalTypes() throws Exception
    {
        List<Class<?>> apiClasses = findApiClasses();
        assertFalse(apiClasses.isEmpty(), "No API classes found");

        List<String> violations = new ArrayList<>();
        for (Class<?> apiClass : apiClasses)
        {
            if (!isExposed(apiClass.getModifiers()))
                continue;
            String owner = apiClass.getName();
            check(apiClass.getGenericSuperclass(), owner + " supertype", violations);
            for (Type superInterface : apiClass.getGenericInterfaces())
                check(superInterface, owner + " supertype", violations);
            for (Field field : apiClass.getDeclaredFields())
            {
                if (isExposed(field))
                    check(field.getGenericType(), owner + "." + field.getName(), violations);
            }
            for (Constructor<?> constructor : apiClass.getDeclaredConstructors())
            {
                if (isExposed(constructor))
                    checkAll(constructor.getGenericParameterTypes(), owner + " constructor", violations);
            }
            for (Method method : apiClass.getDeclaredMethods())
            {
                if (!isExposed(method) || method.isSynthetic() || method.isBridge())
                    continue;
                String where = owner + "." + method.getName() + "()";
                check(method.getGenericReturnType(), where, violations);
                checkAll(method.getGenericParameterTypes(), where, violations);
                checkAll(method.getGenericExceptionTypes(), where, violations);
            }
        }
        assertTrue(violations.isEmpty(), "API signatures expose internal types:\n" + String.join("\n", violations));
    }

    @Test
    void everyApiFileCarriesTheMitHeader() throws IOException
    {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(API_SOURCES))
        {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList())
            {
                if (!Files.readString(file).contains("SPDX-License-Identifier: MIT"))
                    missing.add(API_SOURCES.relativize(file).toString());
            }
        }
        assertTrue(missing.isEmpty(), "API files without the MIT header (see LICENSE-API): " + missing);
    }

    @Test
    void everyApiTypeDeclaresItsStability() throws IOException
    {
        List<String> unmarked = new ArrayList<>();
        try (Stream<Path> files = Files.walk(API_SOURCES))
        {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList())
            {
                if (file.getFileName().toString().equals("package-info.java"))
                    continue;
                String source = Files.readString(file);
                if (!source.contains("@ApiStatus.Experimental") && !source.contains("@ApiStatus.AvailableSince"))
                    unmarked.add(API_SOURCES.relativize(file).toString());
            }
        }
        assertTrue(unmarked.isEmpty(), "API types without an @ApiStatus stability marker: " + unmarked);
    }

    private static boolean isExposed(int modifiers)
    {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static boolean isExposed(Member member)
    {
        return isExposed(member.getModifiers()) && isExposed(member.getDeclaringClass().getModifiers());
    }

    private static void checkAll(Type[] types, String where, List<String> violations)
    {
        for (Type type : types)
            check(type, where, violations);
    }

    private static void check(Type type, String where, List<String> violations)
    {
        check(type, where, violations, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void check(Type type, String where, List<String> violations, Set<Type> visited)
    {
        if (type == null || !visited.add(type))
            return;
        if (type instanceof Class<?> typeClass)
        {
            Class<?> element = typeClass;
            while (element.isArray())
                element = element.getComponentType();
            if (isInternal(element.getName()))
                violations.add(where + " -> " + element.getName());
        }
        else if (type instanceof ParameterizedType parameterized)
        {
            check(parameterized.getRawType(), where, violations, visited);
            for (Type argument : parameterized.getActualTypeArguments())
                check(argument, where, violations, visited);
        }
        else if (type instanceof WildcardType wildcard)
        {
            for (Type bound : wildcard.getUpperBounds())
                check(bound, where, violations, visited);
            for (Type bound : wildcard.getLowerBounds())
                check(bound, where, violations, visited);
        }
        else if (type instanceof GenericArrayType array)
            check(array.getGenericComponentType(), where, violations, visited);
        else if (type instanceof TypeVariable<?> variable)
        {
            for (Type bound : variable.getBounds())
                check(bound, where, violations, visited);
        }
    }

    /** Main-mod code outside the API, legacy model classes and the shared model library. */
    private static boolean isInternal(String className)
    {
        if (className.startsWith(API_PACKAGE + "."))
            return false;
        return className.startsWith("com.flansmodultimate.") || className.startsWith("com.flansmod.") || className.startsWith("com.wolffsmod.");
    }

    /** Loads without initialising, so no game bootstrap is needed. */
    private static List<Class<?>> findApiClasses() throws IOException, URISyntaxException, ClassNotFoundException
    {
        ClassLoader loader = ApiBoundaryTest.class.getClassLoader();
        Set<String> names = new HashSet<>();
        var roots = loader.getResources(API_PACKAGE.replace('.', '/'));
        while (roots.hasMoreElements())
        {
            URL root = roots.nextElement();
            if (!"file".equals(root.getProtocol()))
                continue;
            Path directory = Path.of(root.toURI());
            try (Stream<Path> files = Files.walk(directory))
            {
                files.map(directory::relativize).map(Path::toString).filter(name -> name.endsWith(".class") && !name.endsWith("package-info.class"))
                    .map(name -> API_PACKAGE + "." + name.substring(0, name.length() - 6).replace('\\', '.').replace('/', '.')).forEach(names::add);
            }
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String name : names)
            classes.add(Class.forName(name, false, loader));
        return classes;
    }
}
