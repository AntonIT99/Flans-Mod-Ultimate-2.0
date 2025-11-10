package com.flansmod.client.tmt;

import com.flansmodultimate.FlansMod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModelPool
{
    private static final Map<String, ModelPoolEntry> modelMap = new HashMap<>();
    private static final String[] resourceDir = new String[] {
            "/resources/models/",
            "/resources/mod/models/",
            "/Flan/"
    };
    public static final Class<ModelPoolObjEntry> OBJ = ModelPoolObjEntry.class;

    private ModelPool() {}

    @Nullable
    public static ModelPoolEntry addFile(String file, Class<?> modelClass, Map<String, TransformGroup> group, Map<String, TextureGroup> textureGroup)
    {
        ModelPoolEntry entry;

        if (modelMap.containsKey(file))
        {
            entry = modelMap.get(file);
            entry.applyGroups(group, textureGroup);
            return entry;
        }

        try
        {
            entry = (ModelPoolEntry) modelClass.getConstructor().newInstance();
        }
        catch(Exception e)
        {
            FlansMod.log.error("A new {} could not be initialized.", modelClass.getName());
            FlansMod.log.error(e.getMessage());
            return null;
        }

        File modelFile = null;

        for (int i = 0; i < resourceDir.length && (modelFile == null || !modelFile.exists()); i++)
        {
            String absPath = new File(FMLPaths.CONFIGDIR.get().getParent().toFile(), resourceDir[i]).getAbsolutePath();
            if(!absPath.endsWith("/") || !absPath.endsWith("\\"))
                absPath += "/";
            modelFile = entry.checkValidPath(absPath + file);
        }

        if(modelFile == null || !modelFile.exists())
        {
            FlansMod.log.warn("The model with the name {} does not exist.", file);
            return null;
        }

        entry.groups = new HashMap<>();
        entry.textures = new HashMap<>();
        entry.name = file;
        entry.setGroup("0");
        entry.setTextureGroup("0");
        entry.getModel(modelFile);
        entry.applyGroups(group, textureGroup);
        modelMap.put(file, entry);
        return entry;
    }
}
