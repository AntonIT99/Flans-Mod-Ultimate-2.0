package com.flansmodultimate.util;

import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.content.IContentProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

/**
 * Finds the content pack a model class is loaded from and instantiates it.
 *
 * <p>Holds no client state, so the client model cache and the server-side muzzle
 * measurement resolve a {@code Model} entry to the same class.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelClassResolver
{
    /**
     * @param contentPack  the content pack the class is loaded for
     * @param className    name of the model class inside {@code contentPack}, which is the declared model
     *                     class name unless a legacy pack ships it below its own package
     * @param ownClassFile true when the class file is shipped by the content pack the type belongs to, which
     *                     makes it take precedence over a model class of the same name compiled into the mod
     */
    public record ModelClassLocation(IContentProvider contentPack, String className, boolean ownClassFile) {}

    public static ModelClassLocation find(IContentProvider preferredContentPack, String modelClassName, boolean searchOtherContentPacks)
    {
        if (ClassLoaderUtils.hasClassFile(preferredContentPack, modelClassName))
            return new ModelClassLocation(preferredContentPack, modelClassName, true);

        if (!searchOtherContentPacks)
            return new ModelClassLocation(preferredContentPack, modelClassName, false);

        String legacyClassName = getLegacyClassName(modelClassName);

        return ContentManager.getContentPacks().stream()
            .filter(contentPack -> !contentPack.equals(preferredContentPack))
            .sorted(Comparator.comparing(IContentProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .map(contentPack -> findClassFile(contentPack, modelClassName, legacyClassName))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(new ModelClassLocation(preferredContentPack, modelClassName, false));
    }

    /**
     * Creates the model a location points to.
     *
     * @param preferBuiltInModelClasses let a model class compiled into the mod win over a class file of the
     *                                  same name that the type's own content pack ships
     */
    public static Object instantiate(ModelClassLocation location, boolean preferBuiltInModelClasses) throws ReflectiveOperationException, IOException
    {
        boolean preferContentPackClass = location.ownClassFile() && !preferBuiltInModelClasses;
        return ClassLoaderUtils.loadModelClass(location.contentPack(), location.className(), preferContentPackClass)
            .getConstructor().newInstance();
    }

    /** Legacy content packs may ship the same model below their own package instead of the common one. */
    @Nullable
    private static ModelClassLocation findClassFile(IContentProvider contentPack, String modelClassName, @Nullable String legacyClassName)
    {
        if (ClassLoaderUtils.hasClassFile(contentPack, modelClassName))
            return new ModelClassLocation(contentPack, modelClassName, false);

        if (legacyClassName != null && ClassLoaderUtils.hasClassFile(contentPack, legacyClassName))
            return new ModelClassLocation(contentPack, legacyClassName, false);

        return null;
    }

    @Nullable
    private static String getLegacyClassName(String modelClassName)
    {
        String prefix = "com.flansmod.client.model.";
        if (!modelClassName.startsWith(prefix))
            return null;

        int packageEnd = modelClassName.indexOf('.', prefix.length());
        if (packageEnd < 0)
            return null;

        String packPackage = modelClassName.substring(prefix.length(), packageEnd);
        String simpleClassName = modelClassName.substring(packageEnd + 1);
        return "com.flansmod." + packPackage + ".client.model." + simpleClassName;
    }
}
