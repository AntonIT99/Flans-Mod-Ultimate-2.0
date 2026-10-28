package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentType;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.*;

/**
 * Registers one {@link FlanModelEntityType} for every AA gun and driveable of the loaded content packs.
 *
 * <p>
 * Custom NPCs lists every registered living entity type in its model selection, so each
 * definition becomes a selectable NPC model without any class of its own. Flan's Mod Ultimate reads
 * the content packs while it is constructed, which is complete before any registry event fires.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanModelEntities
{
    /** Keeps these ids apart from any entity the module registers under its own name. */
    private static final String ID_PREFIX = "model/";

    private static final List<FlanModelEntityType> entityTypes = new ArrayList<>();

    @Unmodifiable
    public static List<FlanModelEntityType> getEntityTypes()
    {
        return Collections.unmodifiableList(entityTypes);
    }

    public static void registerEntityTypes(RegisterEvent event)
    {
        event.register(Registries.ENTITY_TYPE, helper ->
        {
            // Shortname order keeps the registration independent of hash iteration order.
            Map<String, IContentType> definitions = new TreeMap<>();
            for (IContentType type : FlansModApi.getTypes())
            {
                if (FlanModelKind.of(type) != null)
                    definitions.put(type.getShortName(), type);
            }

            definitions.forEach((shortName, type) ->
            {
                FlanModelKind kind = FlanModelKind.of(type);
                ResourceLocation id = ResourceLocation.tryBuild(NpcsMod.MOD_ID, ID_PREFIX + shortName);
                if (id == null)
                {
                    NpcsMod.log.warn("Skipping NPC model for '{}': not a valid entity id", shortName);
                    return;
                }
                FlanModelShape shape = FlanModelShape.of(type, kind);
                NpcsMod.log.debug("NPC model {}: {} x {} hitbox, model origin {} above the feet", id, NpcPlatform.width(shape.dimensions()), NpcPlatform.height(shape.dimensions()),
                    shape.modelHeight());
                FlanModelEntityType entityType = new FlanModelEntityType(shortName, kind, shape);
                helper.register(id, entityType);
                entityTypes.add(entityType);
            });
            NpcsMod.log.info("Registered {} content-pack models for Custom NPCs", entityTypes.size());
        });
    }

    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        AttributeSupplier attributes = FlanModelEntity.createAttributes().build();
        for (FlanModelEntityType entityType : entityTypes)
            event.put(entityType, attributes);
    }
}
