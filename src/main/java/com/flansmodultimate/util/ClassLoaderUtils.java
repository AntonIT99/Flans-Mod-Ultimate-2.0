package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassLoaderUtils
{
    @Getter
    private static final Map<String, List<TransformOp>> transforms = new HashMap<>();

    private static final CustomClassLoader classLoader = new CustomClassLoader();

    private static final Map<String, String> minecraftMethodMappings = Map.ofEntries(
        Map.entry("func_78084_a", "getTextureOffset"),
        Map.entry("func_78085_a", "setTextureOffset"),
        Map.entry("func_78086_a", "setLivingAnimations"),
        Map.entry("func_78087_a", "setRotationAngles"),
        Map.entry("func_78088_a", "render"),
        Map.entry("func_78784_a", "setTextureOffset"),
        Map.entry("func_78785_a", "render"),
        Map.entry("func_78786_a", "addBox"),
        Map.entry("func_78787_b", "setTextureSize"),
        Map.entry("func_78788_d", "compileDisplayList"),
        Map.entry("func_78789_a", "addBox"),
        Map.entry("func_78790_a", "addBox"),
        Map.entry("func_78791_b", "renderWithRotation"),
        Map.entry("func_78792_a", "addChild"),
        Map.entry("func_78793_a", "setRotationPoint"),
        Map.entry("func_78794_c", "postRender"),
        Map.entry("func_85181_a", "getRandomModelBox"),
        Map.entry("func_178685_a", "copyModelAngles"),
        Map.entry("func_178686_a", "setModelAttributes")
    );

    private static final Map<String, String> minecraftFieldMappings = Map.ofEntries(
        Map.entry("field_78782_b", "textureOffsetY"),
        Map.entry("field_78783_a", "textureOffsetX"),
        Map.entry("field_78795_f", "rotateAngleX"),
        Map.entry("field_78796_g", "rotateAngleY"),
        Map.entry("field_78797_d", "rotationPointY"),
        Map.entry("field_78798_e", "rotationPointZ"),
        Map.entry("field_78799_b", "textureHeight"),
        Map.entry("field_78800_c", "rotationPointX"),
        Map.entry("field_78801_a", "textureWidth"),
        Map.entry("field_78802_n", "boxName"),
        Map.entry("field_78803_o", "textureOffsetX"),
        Map.entry("field_78804_l", "cubeList"),
        Map.entry("field_78805_m", "childModels"),
        Map.entry("field_78806_j", "showModel"),
        Map.entry("field_78807_k", "isHidden"),
        Map.entry("field_78808_h", "rotateAngleZ"),
        Map.entry("field_78809_i", "mirror"),
        Map.entry("field_78810_s", "baseModel"),
        Map.entry("field_78811_r", "displayList"),
        Map.entry("field_78812_q", "compiled"),
        Map.entry("field_78813_p", "textureOffsetY"),
        Map.entry("field_82906_o", "offsetX,0"),
        Map.entry("field_82907_q", "offsetZ,0"),
        Map.entry("field_82908_p", "offsetY,0")
    );

    private static final Map<String, String> classMappings = Map.ofEntries(
        Map.entry("net/minecraft/client/model/ModelRenderer", "com/wolffsmod/api/client/model/ModelRenderer"),
        Map.entry("net/minecraft/entity/Entity", "net/minecraft/world/entity/Entity"),
        Map.entry("net/minecraft/entity/EntityLivingBase", "net/minecraft/world/entity/LivingEntity")
    );

    private static final String LEGACY_MODELBASE = "net/minecraft/client/model/ModelBase";
    private static final String NEW_MODELBASE = "com/wolffsmod/api/client/model/ModelBase";
    private static final String INTERFACE_MODELBASE = "com/wolffsmod/api/client/model/IModelBase";

    /**
     * Loads a compiled Java class (.class file) from a given file path.
     * <p>
     * Examples:
     * <p>
     * - In File System "C:/parent/com/example/MyClass.class": <p>
     * ClassLoaderUtil.loadClass(Path.of("C://parent"), "com.example.MyClass");
     * <p>
     * - In JAR Archive "C:/archive.jar/com/example/MyClass.class": <p>
     * ClassLoaderUtil.loadClass(Path.of("C://archive.jar"), "com.example.MyClass");
     * <p>
     * - In ZIP Archive "C:/archive.zip/com/example/MyClass.class": <p>
     * ClassLoaderUtil.loadClass(Path.of("C://archive.zip"), "com.example.MyClass");
     *
     * @param parentPath    The parent path to the package root of the .class file.
     * @param className     The fully qualified name of the class (e.g., "com.example.MyClass").
     * @return The loaded {@link Class} object.
     * @throws IOException            If an I/O error occurs while accessing the file.
     * @throws ClassNotFoundException If the class cannot be found or loaded.
     */
    public static Class<?> loadClass(Path parentPath, String className) throws IOException, ClassNotFoundException
    {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { parentPath.toUri().toURL() }))
        {
            return classLoader.loadClass(className);
        }
    }

    public static Class<?> loadAndModifyClass(IContentProvider contentProvider, String fileClassName, String actualClassName) throws IOException, NoClassDefFoundError, ClassFormatError
    {
        Class<?> loadedClass = classLoader.findClass(actualClassName);
        if (loadedClass != null)
            return loadedClass;

        String relativeClassPath = fileClassName.replace('.', '/') + ".class";

        byte[] classData;

        if (contentProvider.isDirectory())
        {
            classData = Files.readAllBytes(contentProvider.getPath().resolve(relativeClassPath));
        }
        else if (contentProvider.isArchive())
        {
            classData = readFileBytesFromArchive(contentProvider.getPath(), relativeClassPath);
        }
        else
        {
            throw new IllegalArgumentException(contentProvider.getPath() + " is not an existing directory or JAR/ZIP file.");
        }

        byte[] newClassData = getModifiedClassData(classData, fileClassName.equals(actualClassName) ? null : actualClassName);

        //In case there is a dependency to a super class which has not been loaded yet
        ClassReader cr = new ClassReader(newClassData);
        String superClassName = cr.getSuperName().replace('/', '.');
        try
        {
            Class.forName(superClassName);
        }
        catch (Exception ex)
        {
            try
            {
                loadAndModifyClass(contentProvider, superClassName, superClassName);
            }
            catch (Exception | NoClassDefFoundError | ClassFormatError e)
            {
                FlansMod.log.error("Could not load super class {} for {}", superClassName, fileClassName);
                LogUtils.logWithoutStacktrace(e);
            }
        }

        return classLoader.defineClass(actualClassName, newClassData);
    }

    private static byte[] getModifiedClassData(byte[] classData, @Nullable String newClassName)
    {
        ClassReader cr = new ClassReader(classData);

        Map<String,String> map = new HashMap<>();
        if (newClassName != null)
            map.put(cr.getClassName(), newClassName.replace('.', '/'));
        map.put(LEGACY_MODELBASE, INTERFACE_MODELBASE);
        map.putAll(classMappings);

        ClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor deobfClassVisitor = new DeobfClassVisitor(cw, minecraftMethodMappings, minecraftFieldMappings);
        ClassVisitor remapper = new ClassRemapper(deobfClassVisitor, new SimpleRemapper(map));
        ClassVisitor superAndOwnerFixVisitor = new SuperAndOwnerFixVisitor(Opcodes.ASM9, remapper, LEGACY_MODELBASE, NEW_MODELBASE);
        ClassVisitor transformVisitor = new TransformClassVisitor(Opcodes.ASM9, superAndOwnerFixVisitor);

        cr.accept(transformVisitor, 0);
        return cw.toByteArray();
    }

    /** ClassWriter that resolves common super classes using the current loader (helps COMPUTE_FRAMES). */
    private static final class SafeClassWriter extends ClassWriter
    {
        SafeClassWriter(ClassReader cr, int flags)
        {
            super(cr, flags);
        }

        @Override
        protected String getCommonSuperClass(String t1, String t2)
        {
            final String objectClassName = "java/lang/Object";
            try
            {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> c1 = Class.forName(t1.replace('/', '.'), false, cl);
                Class<?> c2 = Class.forName(t2.replace('/', '.'), false, cl);

                if (c1.isAssignableFrom(c2))
                    return t1;
                if (c2.isAssignableFrom(c1))
                    return t2;

                while (!c1.isInterface())
                {
                    c1 = c1.getSuperclass();
                    if (c1 == null)
                        return objectClassName;
                    if (c1.isAssignableFrom(c2))
                        return c1.getName().replace('.', '/');
                }
                return objectClassName;
            }
            catch (Throwable ex)
            {
                return objectClassName;
            }
        }
    }

    // Custom ClassLoader to define classes
    private static final class CustomClassLoader extends ClassLoader
    {
        public CustomClassLoader()
        {
            super(IModelBase.class.getClassLoader());
        }

        public Class<?> defineClass(String name, byte[] b) throws ClassFormatError, NoClassDefFoundError
        {
            return super.defineClass(name, b, 0, b.length);
        }

        @Override
        public Class<?> findClass(String name)
        {
            return super.findLoadedClass(name);
        }
    }

    private static byte[] readFileBytesFromArchive(Path archivePath, String filePath) throws IOException
    {
        try (FileSystem fs = FileSystems.newFileSystem(archivePath))
        {
            Path fileInArchive = fs.getPath(filePath);

            try (InputStream inputStream = Files.newInputStream(fileInArchive, StandardOpenOption.READ))
            {
                return inputStream.readAllBytes();
            }
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not read {} in {}", filePath, archivePath);
            throw new IOException(e);
        }
    }
}
