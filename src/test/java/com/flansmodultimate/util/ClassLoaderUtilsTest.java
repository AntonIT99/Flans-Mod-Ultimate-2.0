package com.flansmodultimate.util;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.wolffsmod.api.client.model.IModelBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ClassLoaderUtilsTest
{
    private static final String COMPILED_CLASS = CompiledTestModel.class.getName();
    private static final String PACK_CLASS = "com.flansmod.client.model.test.ModelPackTestModel";

    @TempDir
    Path tempDir;

    @Test
    void contentPackClassFileTakesPrecedenceOverTheCompiledClass() throws Exception
    {
        IContentProvider pack = createContentPack("OverridingPack", COMPILED_CLASS);

        Class<?> loaded = ClassLoaderUtils.loadModelClass(pack, COMPILED_CLASS, true);

        assertEquals(COMPILED_CLASS, loaded.getName());
        assertNotSame(CompiledTestModel.class, loaded);
        assertSame(ClassLoaderUtils.getClassLoader(pack), loaded.getClassLoader());
        assertSame(loaded, ClassLoaderUtils.loadModelClass(pack, COMPILED_CLASS, true));
    }

    @Test
    void compiledClassAlwaysWinsWhenContentPackClassesAreNotPreferred() throws Exception
    {
        IContentProvider pack = createContentPack("OverriddenPack", COMPILED_CLASS);

        assertSame(CompiledTestModel.class, ClassLoaderUtils.loadModelClass(pack, COMPILED_CLASS, false));
    }

    @Test
    void contentPackClassesAreLoadedForClassesTheModDoesNotProvide() throws Exception
    {
        IContentProvider pack = createContentPack("PackOnlyModelPack", PACK_CLASS);

        assertEquals(PACK_CLASS, ClassLoaderUtils.loadModelClass(pack, PACK_CLASS, false).getName());
    }

    @Test
    void missingClassFilesReportTheMissingFile() throws Exception
    {
        IContentProvider pack = new ContentPack("EmptyPack", Files.createDirectories(tempDir.resolve("EmptyPack")));

        assertThrows(NoSuchFileException.class, () -> ClassLoaderUtils.loadModelClass(pack, PACK_CLASS, true));
        assertThrows(NoSuchFileException.class, () -> ClassLoaderUtils.loadModelClass(pack, PACK_CLASS, false));
    }

    @Test
    void contentPacksShippingTheSameClassNameKeepTheirOwnClass() throws Exception
    {
        IContentProvider firstPack = createContentPack("FirstPack", PACK_CLASS);
        IContentProvider secondPack = createContentPack("SecondPack", PACK_CLASS);

        Class<?> fromFirstPack = ClassLoaderUtils.loadModelClass(firstPack, PACK_CLASS, true);
        Class<?> fromSecondPack = ClassLoaderUtils.loadModelClass(secondPack, PACK_CLASS, true);

        assertEquals(PACK_CLASS, fromFirstPack.getName());
        assertEquals(PACK_CLASS, fromSecondPack.getName());
        assertNotSame(fromFirstPack, fromSecondPack);
        assertSame(fromFirstPack, ClassLoaderUtils.loadModelClass(firstPack, PACK_CLASS, true));
        assertSame(fromSecondPack, ClassLoaderUtils.loadModelClass(secondPack, PACK_CLASS, true));
    }

    @Test
    void packagedContentPacksAreIsolatedPerModelsRoot() throws Exception
    {
        Path firstModelsRoot = writeClassFile(Files.createDirectories(tempDir.resolve("first_mod/flans_models")), PACK_CLASS, 1F);
        Path secondModelsRoot = writeClassFile(Files.createDirectories(tempDir.resolve("second_mod/flans_models")), PACK_CLASS, 4F);

        IContentProvider firstModelsPack = new PackagedTestProvider("First Pack", firstModelsRoot);
        IContentProvider firstModelsOtherPack = new PackagedTestProvider("Other Pack of First Mod", firstModelsRoot);
        IContentProvider secondModelsPack = new PackagedTestProvider("Second Pack", secondModelsRoot);

        Class<?> fromFirstMod = ClassLoaderUtils.loadModelClass(firstModelsPack, PACK_CLASS, true);
        Class<?> fromSecondMod = ClassLoaderUtils.loadModelClass(secondModelsPack, PACK_CLASS, true);

        // Packs packaged in different mods keep their own class, packs sharing one models root share it
        assertNotSame(fromFirstMod, fromSecondMod);
        assertSame(fromFirstMod, ClassLoaderUtils.loadModelClass(firstModelsOtherPack, PACK_CLASS, true));
        assertTranslation(fromFirstMod, 1F);
        assertTranslation(fromSecondMod, 4F);
    }

    @Test
    void legacyTransformsAreStoredPerLoadedClass() throws Exception
    {
        IContentProvider firstPack = createContentPack("FirstTransformPack", PACK_CLASS, 1F);
        IContentProvider secondPack = createContentPack("SecondTransformPack", PACK_CLASS, 4F);

        Class<?> fromFirstPack = ClassLoaderUtils.loadModelClass(firstPack, PACK_CLASS, true);
        Class<?> fromSecondPack = ClassLoaderUtils.loadModelClass(secondPack, PACK_CLASS, true);

        // LegacyTransformApplier resolves the captured transforms by the loaded model class, so equally
        // named classes of two content packs must keep their own transforms.
        assertTranslation(fromFirstPack, 1F);
        assertTranslation(fromSecondPack, 4F);
    }

    @Test
    void transformedLegacyModelUsesNonGenericModelContract() throws Exception
    {
        String className = "com.flansmod.client.model.test.LegacyModelContractTest";
        Path packRoot = Files.createDirectories(tempDir.resolve("LegacyModelContractPack"));
        Path classFile = packRoot.resolve(className.replace('.', '/') + FileUtils.CLASS_EXTENSION);
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, createLegacyModelData(className));

        IContentProvider pack = new ContentPack("LegacyModelContractPack", packRoot);
        Object loadedModel = ClassLoaderUtils.loadModelClass(pack, className, true).getConstructor().newInstance();

        IModelBase model = assertInstanceOf(IModelBase.class, loadedModel);
        AtomicInteger modelBoxCount = new AtomicInteger();
        model.forEachModelBox(modelRenderer -> modelBoxCount.incrementAndGet());
        assertEquals(1, modelBoxCount.get());
    }

    private static void assertTranslation(Class<?> modelClass, float expectedX)
    {
        List<TransformOp> ops = ClassLoaderUtils.getTransforms(modelClass);
        assertNotNull(ops, "No transforms stored for " + modelClass);
        assertEquals(1, ops.size());
        assertEquals(TransformOp.EnumKind.TRANSLATE, ops.get(0).kind);
        assertArrayEquals(new float[] { expectedX, 2F, 0F }, ops.get(0).args);
    }

    private IContentProvider createContentPack(String packName, String className) throws IOException
    {
        return createContentPack(packName, className, 1F);
    }

    private IContentProvider createContentPack(String packName, String className, float translationX) throws IOException
    {
        Path packRoot = Files.createDirectories(tempDir.resolve(packName));
        return new ContentPack(packName, writeClassFile(packRoot, className, translationX));
    }

    private static Path writeClassFile(Path classRoot, String className, float translationX) throws IOException
    {
        Path classFile = classRoot.resolve(className.replace('.', '/') + FileUtils.CLASS_EXTENSION);
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, createClassData(className, translationX));
        return classRoot;
    }

    /** Two of these stand for two logical packs packaged below one shared models root. */
    private record PackagedTestProvider(String packName, Path modelsRoot) implements IContentProvider
    {
        @Override
        public String getName()
        {
            return packName;
        }

        @Override
        public Path getPath()
        {
            return modelsRoot;
        }

        @Override
        public String getRunId()
        {
            return packName;
        }

        @Override
        public void update(String name, Path path)
        {
            throw new UnsupportedOperationException();
        }
    }

    private static byte[] createClassData(String className, float translationX)
    {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className.replace('.', '/'), null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        // A constructor-time OpenGL transform, the way legacy Techne models position their model
        constructor.visitLdcInsn(translationX);
        constructor.visitLdcInsn(2F);
        constructor.visitInsn(Opcodes.FCONST_0);
        constructor.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL11", "glTranslatef", "(FFF)V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(3, 1);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] createLegacyModelData(String className)
    {
        String legacyModelBase = "net/minecraft/client/model/ModelBase";
        String legacyModelRenderer = "net/minecraft/client/model/ModelRenderer";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className.replace('.', '/'), null, legacyModelBase, null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, legacyModelBase, "<init>", "()V", false);
        constructor.visitTypeInsn(Opcodes.NEW, legacyModelRenderer);
        constructor.visitInsn(Opcodes.DUP);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, legacyModelRenderer, "<init>", "(Lnet/minecraft/client/model/ModelBase;)V", false);
        constructor.visitInsn(Opcodes.POP);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(3, 1);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
