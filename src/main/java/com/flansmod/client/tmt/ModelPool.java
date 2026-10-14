package com.flansmod.client.tmt;

import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ModelResourceResolver;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModelPool
{
    private record ModelKey(String resource, Class<?> format)
    {}

    private static final Map<ModelKey, ModelPoolEntry> modelMap = new HashMap<>();
    public static final Class<ModelPoolObjEntry> OBJ = ModelPoolObjEntry.class;

    private ModelPool()
    {}

    @Nullable
    public static synchronized ModelPoolEntry addFile(String file, Class<?> modelClass, Map<String, TransformGroup> group, Map<String, TextureGroup> textureGroup)
    {
        ModelPoolEntry entry;

        try
        {
            entry = (ModelPoolEntry) modelClass.getConstructor().newInstance();
        }
        catch (Exception e)
        {
            FlansLog.log.error("A new {} could not be initialized.", modelClass.getName());
            FlansLog.log.error(e.getMessage());
            return null;
        }

        try (ModelResourceResolver.Resource resource = ModelResourceResolver.open(file, entry.fileExtensions))
        {
            if (resource == null)
            {
                FlansLog.log.warn("The model with the name {} does not exist.", file);
                return null;
            }

            // Authored strings can be identical in unrelated packs. Cache the resolved source instead.
            ModelKey key = new ModelKey(resource.path().toUri().toString(), modelClass);
            ModelPoolEntry cached = modelMap.get(key);
            if (cached != null)
            {
                cached.applyGroups(group, textureGroup);
                return cached;
            }

            entry.groups = new HashMap<>();
            entry.textures = new HashMap<>();
            entry.name = file;
            entry.setGroup("0");
            entry.setTextureGroup("0");
            entry.getModel(resource.path());
            if (entry.vertices == null || entry.faces == null)
                return null;
            entry.applyGroups(group, textureGroup);
            modelMap.put(key, entry);
            return entry;
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Could not load model {}", file, e);
            return null;
        }
    }

    /** Discards parsed geometry together with the client model cache on resource reload. */
    public static synchronized void clear()
    {
        modelMap.clear();
    }
}
