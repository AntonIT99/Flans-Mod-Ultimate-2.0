package com.wolffsmod.npcs.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Places vehicle, plane, mecha and AA gun NPCs from code, as summoning their model does. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanModelNpcs
{
    /** @return the model type registered for a definition, or empty when the definition has no NPC model */
    public static Optional<FlanModelEntityType> type(String shortName)
    {
        return FlanModelEntities.getEntityTypes().stream().filter(type -> type.getShortName().equals(shortName)).findFirst();
    }

    /**
     * Adds an NPC wearing a definition's model to the level, named and textured as a summoned one.
     *
     * @return the new NPC, or empty when the definition has no NPC model
     */
    public static Optional<EntityCustomNpc> spawn(ServerLevel level, String shortName, Vec3 position, float yaw)
    {
        return type(shortName).map(type ->
        {
            FlanModelEntity model = new FlanModelEntity(type, level);
            model.moveTo(position.x, position.y, position.z, yaw, 0F);
            return FlanModelSummons.replace(level, model);
        });
    }
}
