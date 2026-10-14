package com.flansmod.client.tmt;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.util.ModelClassResolver;
import com.flansmodultimate.util.ModelResourceResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModelPoolObjEntryTest
{
    private static final String AUTHORED_PATH = "/Tyrants and Plebeians/assets/flansmod/models/Body.obj";
    private static final String OBJ = "v 1 2 3\nv 4 5 6\nv 7 8 9\nvt 0.25 0.75\nvt 0.5 0.5\nvt 1 0\nvn 1 0 0\ng hull\nf 1/1/1 2/2/1 3/3/1\n";

    @TempDir
    Path tempDir;

    @AfterEach
    void clearPool()
    {
        ModelPool.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {".zip", ".jar"})
    void renamedArchivesResolveOriginalPackPathsAndMixedCaseWithoutExtraction(String extension) throws Exception
    {
        Path archive = tempDir.resolve("Tyrants And Plebeians OHIO" + extension);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)))
        {
            zip.putNextEntry(new ZipEntry("assets/flansmod/models/body.obj"));
            zip.write(OBJ.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        IContentProvider pack = new ContentPack(archive.getFileName().toString(), archive);
        ModelPoolEntry entry = load(pack, AUTHORED_PATH);

        assertGeometry(entry);
        assertFalse(Files.exists(pack.getExtractedPath()));
        // Cached geometry stays usable after its archive filesystem has been closed.
        assertSame(entry, load(pack, AUTHORED_PATH));
    }

    @Test
    void directoryModelsAcceptRelativeReferencesAndAnOmittedExtension() throws Exception
    {
        IContentProvider pack = directoryPack("DirectoryPack", OBJ);
        ModelPoolEntry entry = load(pack, "models/body");

        assertGeometry(entry);
        assertSame(entry, load(pack, "body.obj"));
    }

    @Test
    void identicalAuthoredPathsStayIsolatedAcrossPacksAndReloadReadsChangedGeometry() throws Exception
    {
        IContentProvider first = directoryPack("First", OBJ);
        IContentProvider second = directoryPack("Second", OBJ.replace("v 1 2 3", "v 10 2 3"));
        ModelPoolEntry firstEntry = load(first, AUTHORED_PATH);
        ModelPoolEntry secondEntry = load(second, AUTHORED_PATH);

        assertNotSame(firstEntry, secondEntry);
        assertEquals(1D, firstEntry.vertices[0].vector3D.x());
        assertEquals(10D, secondEntry.vertices[0].vector3D.x());
        Files.writeString(first.getAssetsPath().resolve("models/body.obj"), OBJ.replace("v 1 2 3", "v 20 2 3"));
        ModelPool.clear();
        assertEquals(20D, load(first, AUTHORED_PATH).vertices[0].vector3D.x());
    }

    @Test
    void malformedModelsDoNotProduceNullGeometryOrPoisonTheCache() throws Exception
    {
        IContentProvider pack = directoryPack("Invalid", "v broken 2 3\n");
        assertNull(load(pack, AUTHORED_PATH));
        Files.writeString(pack.getAssetsPath().resolve("models/body.obj"), OBJ);
        assertGeometry(load(pack, AUTHORED_PATH));
    }

    @Test
    void modelInstantiationBindsTheSupplyingPackAndRestoresTheOuterScope() throws Exception
    {
        IContentProvider outer = directoryPack("Outer", OBJ);
        IContentProvider inner = directoryPack("Inner", OBJ.replace("v 1 2 3", "v 10 2 3"));
        try (ModelResourceResolver.Scope ignored = ModelResourceResolver.useContentPack(outer))
        {
            ObjModel model = (ObjModel) ModelClassResolver.instantiate(new ModelClassResolver.ModelClassLocation(inner, ObjModel.class.getName(), false), false);
            assertEquals(10D, model.entry.vertices[0].vector3D.x());
            assertEquals(1D, ModelPool.addFile(AUTHORED_PATH, ModelPool.OBJ, new HashMap<>(), new HashMap<>()).vertices[0].vector3D.x());
        }
    }

    private IContentProvider directoryPack(String name, String geometry) throws Exception
    {
        Path packRoot = tempDir.resolve(name);
        Path file = packRoot.resolve("assets/flansmod/models/body.obj");
        Files.createDirectories(file.getParent());
        Files.writeString(file, geometry);
        return new ContentPack(name, packRoot);
    }

    private static ModelPoolEntry load(IContentProvider pack, String path)
    {
        try (ModelResourceResolver.Scope ignored = ModelResourceResolver.useContentPack(pack))
        {
            return ModelPool.addFile(path, ModelPool.OBJ, new HashMap<>(), new HashMap<>());
        }
    }

    private static void assertGeometry(ModelPoolEntry entry)
    {
        assertNotNull(entry);
        assertEquals(3, entry.vertices.length);
        assertEquals(1, entry.faces.length);
        assertEquals(3, entry.faces[0].nVertices);
        assertEquals(3D, entry.vertices[0].vector3D.y());
        assertEquals(-2D, entry.vertices[0].vector3D.z());
        assertEquals(0.25F, entry.faces[0].vertexPositions[0].texturePositionX);
        assertEquals(0.25F, entry.faces[0].vertexPositions[0].texturePositionY);
        assertEquals(1, entry.textures.get("hull").poly.size());
    }

    public static class ObjModel
    {
        final ModelPoolEntry entry;

        public ObjModel()
        {
            entry = ModelPool.addFile(AUTHORED_PATH, ModelPool.OBJ, new HashMap<>(), new HashMap<>());
        }
    }
}
