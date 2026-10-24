package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.content.IContentType;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.*;

/**
 * What the NPC being edited is, as far as the Wolff's Mod tab is concerned: a standard NPC, or the kind of Flan model it
 * shows. The tab lists the pages, tabs and settings meant for that kind and greys out the settings that do nothing for it.
 */
enum NpcKind
{
    STANDARD, AA_GUN, VEHICLE, PLANE, MECHA;

    /** The kinds of NPC a page, a tab or a setting is meant for. */
    @RequiredArgsConstructor
    enum Scope
    {
        ALL(EnumSet.allOf(NpcKind.class)), STANDARD(EnumSet.of(NpcKind.STANDARD)), MODELS(EnumSet.of(NpcKind.AA_GUN, NpcKind.VEHICLE, NpcKind.PLANE, NpcKind.MECHA)),
        /** The models with part hitboxes: an AA gun has none. */
        DRIVEABLES(EnumSet.of(NpcKind.VEHICLE, NpcKind.PLANE, NpcKind.MECHA)), VEHICLES(EnumSet.of(NpcKind.VEHICLE)),
        /** The models whose hull or base keeps its own facing. */
        HULLS(EnumSet.of(NpcKind.VEHICLE, NpcKind.AA_GUN)),
        /** Aircraft, and watercraft that dive. */
        PLANES_AND_VEHICLES(EnumSet.of(NpcKind.PLANE, NpcKind.VEHICLE)),
        /** The models that drive over the ground, and may run over what is in their way. */
        GROUND_MACHINES(EnumSet.of(NpcKind.VEHICLE, NpcKind.MECHA));

        private final Set<NpcKind> kinds;

        boolean includes(NpcKind kind)
        {
            return kinds.contains(kind);
        }

        /** @return the key of the name of the NPCs the scope is meant for, which completes "Only for ..." */
        String key()
        {
            return "wolffsmodnpcs.scope." + name().toLowerCase(Locale.ROOT);
        }
    }

    static NpcKind of(EntityNPCInterface npc)
    {
        return model(npc).map(FlanModelKind::of).map(kind -> switch (kind)
        {
            case AA_GUN -> AA_GUN;
            case VEHICLE -> VEHICLE;
            case PLANE -> PLANE;
            case MECHA -> MECHA;
        }).orElse(STANDARD);
    }

    /** @return the Flan model type the NPC shows, or nothing for a standard NPC */
    static Optional<IContentType> model(EntityNPCInterface npc)
    {
        if (npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model)
            return Optional.ofNullable(model.getInfoType());
        return Optional.empty();
    }

    String key()
    {
        return "wolffsmodnpcs.kind." + name().toLowerCase(Locale.ROOT);
    }
}
