package com.wolffsmod.npcs.soldier;

import com.wolffsmod.npcs.names.SoldierNameAssociations;
import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.WeakHashMap;

/** Applies the server's naming association on an explicit class-picker request, independently of the loadout toggle. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanSoldierNames
{
    private static final Map<ServerPlayer, Pending> PENDING = new WeakHashMap<>();

    private record Pending(WeakReference<EntityNPCInterface> npc, SoldierNamePacket packet, long expires)
    {}

    public static Optional<SoldierNamePacket> request(ServerPlayer player, int entityId, String team, String playerClass)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        boolean allowed = npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_DISPLAY);
        if (!allowed)
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.soldier.name.denied"));
            return Optional.empty();
        }
        PENDING.remove(player);
        Optional<SoldierNamePacket> result = FlanSoldierSelection.resolve(team, playerClass).flatMap(selection -> apply(npc, selection));
        result.ifPresent(packet -> PENDING.put(player, new Pending(new WeakReference<>(npc), packet, npc.level().getGameTime() + 200)));
        return result;
    }

    /**
     * Finishes only a name this server just issued. A picker close may have saved the old display while the reply was in
     * flight; this acknowledgement follows those saves and restores the generated name without trusting client data.
     */
    public static void confirm(ServerPlayer player, SoldierNamePacket packet)
    {
        Pending pending = PENDING.get(player);
        if (pending == null || !pending.packet().equals(packet))
            return;
        PENDING.remove(player);
        EntityNPCInterface npc = pending.npc().get();
        if (npc == null || !npc.isAlive() || npc.level() != player.level() || npc.level().getGameTime() > pending.expires())
            return;
        npc.display.setMarkovGeneratorId(SoldierNames.generatorId(pending.packet().nameList()));
        npc.display.setName(pending.packet().name());
        npc.updateClient = true;
    }

    static Optional<SoldierNamePacket> apply(EntityNPCInterface npc, FlanSoldierSelection selection)
    {
        String team = selection.team() == null ? "" : selection.team().getShortName();
        String playerClass = selection.playerClass().getShortName();
        SoldierNameAssociations associations = SoldierNameAssociations.current();
        OptionalInt generator = associations.find(team, playerClass);
        if (generator.isEmpty())
            return Optional.empty();
        int id = generator.getAsInt();
        String name = SoldierNames.generate(id, associations.gender(team, playerClass));
        npc.display.setMarkovGeneratorId(id);
        npc.display.setName(name);
        npc.updateClient = true;
        return Optional.of(new SoldierNamePacket(npc.getId(), team, playerClass, true, SoldierNames.associationId(id), name));
    }
}
