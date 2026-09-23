package com.flansmodultimate.util;

import com.flansmodultimate.IContentProvider;
import com.wolffsmod.api.client.model.IModelBase;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;

/**
 * Loads legacy model classes from the class files of one content pack.
 * <p>
 * Each class file tree gets its own loader, so content packs may ship different classes under the
 * same canonical name without conflicting with each other or with the classes compiled into the mod.
 * Content packs packaged below a shared models root share one loader, because they share the files.
 * <p>
 * Classes a content pack does not ship are delegated to the mod's own class loader, which keeps the
 * model framework classes shared between all content packs: a content pack cannot replace
 * {@code ModelGun} or {@code ModelDriveable} for its models, because the renderers identify models
 * by those types.
 */
public final class ContentPackClassLoader extends ClassLoader
{
    private final IContentProvider contentProvider;

    ContentPackClassLoader(IContentProvider contentProvider)
    {
        super("flansmod-content-pack/" + contentProvider.getName(), IModelBase.class.getClassLoader());
        this.contentProvider = contentProvider;
    }

    /**
     * Loads the content pack's own version of a class, even when the mod provides a class with that
     * name as well.
     *
     * @throws IOException if the content pack does not contain that class file or it cannot be read
     */
    public Class<?> loadContentPackClass(String className) throws IOException
    {
        synchronized (getClassLoadingLock(className))
        {
            Class<?> loadedClass = findLoadedClass(className);
            return loadedClass != null ? loadedClass : defineContentPackClass(className);
        }
    }

    /** Called for the classes a loaded model depends on which the mod itself does not provide. */
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException
    {
        try
        {
            return defineContentPackClass(className);
        }
        catch (IOException | RuntimeException e)
        {
            throw new ClassNotFoundException(className, e);
        }
    }

    @Override
    public String toString()
    {
        return getName() + " [" + contentProvider.getPath() + "]";
    }

    private Class<?> defineContentPackClass(String className) throws IOException
    {
        ClassLoaderUtils.ModifiedClass modifiedClass = ClassLoaderUtils.transformClass(readClassData(className), this);
        byte[] classData = modifiedClass.classData();
        Class<?> definedClass = defineClass(className, classData, 0, classData.length);
        ClassLoaderUtils.registerTransforms(definedClass, modifiedClass.transforms());
        return definedClass;
    }

    private byte[] readClassData(String className) throws IOException
    {
        if (!contentProvider.isDirectory() && !contentProvider.isArchive())
            throw new IOException(contentProvider.getPath() + " is not an existing directory or JAR/ZIP file.");

        FileSystem fs = FileUtils.createFileSystem(contentProvider);
        try
        {
            return Files.readAllBytes(contentProvider.getModelPath(className, fs));
        }
        finally
        {
            FileUtils.closeFileSystem(fs, contentProvider);
        }
    }
}
