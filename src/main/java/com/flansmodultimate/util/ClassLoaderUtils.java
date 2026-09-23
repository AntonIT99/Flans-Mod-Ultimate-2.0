package com.flansmodultimate.util;

import com.flansmodultimate.IContentProvider;
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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassLoaderUtils
{
    /** A transformed legacy class file together with the OpenGL transforms found inside it. */
    public record ModifiedClass(byte[] classData, List<TransformOp> transforms) {}

    /**
     * One loader per class file tree, keyed by {@link IContentProvider#getModelSourceId()}, so equally named
     * model classes of different content packs stay separate from each other and from the mod's own classes.
     */
    private static final Map<String, ContentPackClassLoader> classLoaders = new ConcurrentHashMap<>();

    /** OpenGL transforms of the legacy model classes, keyed by the loaded class itself. */
    private static final Map<Class<?>, List<TransformOp>> transforms = new ConcurrentHashMap<>();

    @Getter
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

    @Getter
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
        Map.entry("net/minecraft/client/model/ModelRenderer", "com/flansmodultimate/client/model/ModelRenderer"),
        Map.entry("net/minecraft/entity/Entity", "net/minecraft/world/entity/Entity"),
        Map.entry("net/minecraft/entity/EntityLivingBase", "net/minecraft/world/entity/LivingEntity")
    );

    private static final String LEGACY_MODELBASE = "net/minecraft/client/model/ModelBase";
    private static final String NEW_MODELBASE = "com/flansmodultimate/client/model/ModelBase";
    private static final String INTERFACE_MODELBASE = "com/wolffsmod/api/client/model/IModelBase";

    public static Map<String, String> getSourceClassMappings()
    {
        Map<String, String> map = new HashMap<>();
        map.put(LEGACY_MODELBASE.replace('/', '.'), NEW_MODELBASE.replace('/', '.'));
        classMappings.forEach((from, to) -> map.put(from.replace('/', '.'), to.replace('/', '.')));
        return Map.copyOf(map);
    }

    /** Loader of the class files shipped inside one content pack. */
    public static ContentPackClassLoader getClassLoader(IContentProvider contentProvider)
    {
        return classLoaders.computeIfAbsent(contentProvider.getModelSourceId(), modelSourceId -> new ContentPackClassLoader(contentProvider));
    }

    /**
     * Loads the model class {@code className} for one content pack.
     *
     * @param preferContentPackClass load the class file the content pack ships instead of the class of that
     *                               name compiled into the mod. Pass {@code false} to let the mod's own class
     *                               win, which falls back to the content pack for classes the mod lacks.
     * @throws IOException if the class has to be read from the content pack but its class file cannot be read
     */
    public static Class<?> loadModelClass(IContentProvider contentProvider, String className, boolean preferContentPackClass) throws IOException
    {
        if (preferContentPackClass)
            return getClassLoader(contentProvider).loadContentPackClass(className);

        Class<?> modClass = findModClass(className);
        return modClass != null ? modClass : getClassLoader(contentProvider).loadContentPackClass(className);
    }

    public static boolean hasClassFile(IContentProvider contentProvider, String className)
    {
        FileSystem fs = FileUtils.createFileSystem(contentProvider);
        try
        {
            return (contentProvider.isDirectory() || contentProvider.isArchive())
                && Files.isRegularFile(contentProvider.getModelPath(className, fs));
        }
        finally
        {
            FileUtils.closeFileSystem(fs, contentProvider);
        }
    }

    /** The OpenGL transforms a legacy model class applies to itself, or {@code null} if it has none. */
    @Nullable
    public static List<TransformOp> getTransforms(Class<?> modelClass)
    {
        return transforms.get(modelClass);
    }

    static void registerTransforms(Class<?> modelClass, List<TransformOp> modelTransforms)
    {
        if (!modelTransforms.isEmpty())
            transforms.put(modelClass, modelTransforms);
    }

    /** Rewrites a legacy model class that is only stored as a class file instead of being loaded. */
    public static ModifiedClass transformClass(byte[] classData)
    {
        return transformClass(classData, null);
    }

    /**
     * Rewrites a legacy 1.7.10 / 1.12.2 model class so it can run against the current model framework.
     *
     * @param classLoader the loader the class is defined with, used to resolve the types the rewritten
     *                    class refers to. Pass {@code null} to resolve them with the context loader.
     */
    public static ModifiedClass transformClass(byte[] classData, @Nullable ClassLoader classLoader)
    {
        ClassReader cr = new ClassReader(classData);

        Map<String, String> map = new HashMap<>(classMappings);
        map.put(LEGACY_MODELBASE, INTERFACE_MODELBASE);

        List<TransformOp> modelTransforms = new ArrayList<>();
        ClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, classLoader);
        ClassVisitor deobfClassVisitor = new DeobfClassVisitor(cw, minecraftMethodMappings, minecraftFieldMappings);
        ClassVisitor remapper = new ClassRemapper(deobfClassVisitor, new SimpleRemapper(Opcodes.ASM9, map));
        ClassVisitor superAndOwnerFixVisitor = new SuperAndOwnerFixVisitor(Opcodes.ASM9, remapper, LEGACY_MODELBASE, NEW_MODELBASE);
        ClassVisitor transformVisitor = new TransformClassVisitor(Opcodes.ASM9, superAndOwnerFixVisitor, modelTransforms);

        cr.accept(transformVisitor, 0);
        return new ModifiedClass(cw.toByteArray(), List.copyOf(modelTransforms));
    }

    @Nullable
    private static Class<?> findModClass(String className)
    {
        try
        {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        catch (Exception | LinkageError ignored)
        {
            return null;
        }
    }

    /** ClassWriter that resolves common super classes using the given loader (helps COMPUTE_FRAMES). */
    private static final class SafeClassWriter extends ClassWriter
    {
        private final ClassLoader classLoader;

        SafeClassWriter(ClassReader cr, int flags, @Nullable ClassLoader classLoader)
        {
            super(cr, flags);
            this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        }

        @Override
        protected String getCommonSuperClass(String t1, String t2)
        {
            final String objectClassName = "java/lang/Object";
            try
            {
                Class<?> c1 = Class.forName(t1.replace('/', '.'), false, classLoader);
                Class<?> c2 = Class.forName(t2.replace('/', '.'), false, classLoader);

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
}
