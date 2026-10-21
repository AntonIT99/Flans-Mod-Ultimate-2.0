package com.wolffsmod.npcs.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Skins that only clients can resolve. A content pack's texture locations, and the size of a skin image, are known to
 * clients alone: a dedicated server reads no client data. The server therefore marks an NPC whose skin follows its Flan
 * class or its model, in the model's extra data that Custom NPCs synchronizes, and each client puts the skin on its own
 * copy of the NPC whenever the server sends it.
 *
 * <p>
 * A mark keeps the skin the server left on the NPC. Once a builder gives the NPC another skin, the two differ and the
 * mark is ignored, so a skin chosen by hand always wins.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSkins
{
    /** The NPC wears the skin of the Flan class recorded in its extra data, on the model that skin is laid out for. */
    public static final String CLASS_SKIN_KEY = "WolffsModClassSkin";
    /** The NPC wears its content-pack model's texture, in the paintjob recorded in its extra data. */
    public static final String MODEL_SKIN_KEY = "WolffsModModelSkin";

    private static Consumer<EntityCustomNpc> clientResolver = npc ->
    {
        // Replaced on clients; a dedicated server resolves nothing.
    };

    /** Lets the client put marked skins on its NPCs; called once during client setup. */
    public static void setClientResolver(Consumer<EntityCustomNpc> resolver)
    {
        clientResolver = Objects.requireNonNull(resolver);
    }

    /** Marks a server-side NPC as wearing the skin of the class recorded in its extra data. */
    public static void markClassSkin(EntityCustomNpc npc)
    {
        npc.modelData.extra.putString(CLASS_SKIN_KEY, npc.display.getSkinTexture());
    }

    /** Marks a server-side NPC as wearing its model's own texture. */
    public static void markModelSkin(EntityCustomNpc npc)
    {
        npc.modelData.extra.putString(MODEL_SKIN_KEY, npc.display.getSkinTexture());
    }

    /** Whether a mark still applies: the NPC wears the skin the server left on it. */
    public static boolean applies(EntityCustomNpc npc, String key)
    {
        return npc.modelData.extra.contains(key) && npc.modelData.extra.getString(key).equals(npc.display.getSkinTexture());
    }

    /** Puts a marked skin on a client's copy of an NPC, after the server sent the NPC. Client side. */
    public static void resolveOnClient(EntityNPCInterface npc)
    {
        if (npc.level().isClientSide && npc instanceof EntityCustomNpc custom)
            clientResolver.accept(custom);
    }
}
