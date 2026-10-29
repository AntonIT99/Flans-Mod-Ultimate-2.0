package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcActivity;
import com.wolffsmod.npcs.sim.NpcDormancy;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Counts the loaded NPCs of every level by kind and by what they are doing, for the Server page. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCensus
{
    private static final int MAX_DIMENSION = 256;

    /** The loaded NPCs of one kind of model, or of none. */
    public enum Kind
    {
        STANDARD, AA_GUN, VEHICLE, PLANE, MECHA
    }

    /**
     * The loaded NPCs of one level.
     *
     * @param kinds
     *            how many there are of each {@link Kind}, in its order
     * @param fighting
     *            those with a target
     * @param stayActive
     *            those with Stay active on
     * @param held
     *            those Stay active keeps ticking now
     * @param refused
     *            those the server's limit keeps Stay active from holding
     * @param dormant
     *            those beyond their dormant range from every player
     */
    public record Row(String dimension, List<Integer> kinds, int fighting, int stayActive, int held, int refused, int dormant)
    {
        public int total()
        {
            return kinds.stream().mapToInt(Integer::intValue).sum();
        }

        public void write(FriendlyByteBuf buffer)
        {
            buffer.writeUtf(dimension, MAX_DIMENSION);
            buffer.writeVarInt(kinds.size());
            kinds.forEach(buffer::writeVarInt);
            buffer.writeVarInt(fighting);
            buffer.writeVarInt(stayActive);
            buffer.writeVarInt(held);
            buffer.writeVarInt(refused);
            buffer.writeVarInt(dormant);
        }

        public static Row read(FriendlyByteBuf buffer)
        {
            String dimension = buffer.readUtf(MAX_DIMENSION);
            int count = buffer.readVarInt();
            if (count < 0 || count > Kind.values().length * 4)
                throw new IllegalArgumentException("Too many NPC kinds: " + count);
            List<Integer> kinds = new ArrayList<>(count);
            for (int index = 0; index < count; index++)
                kinds.add(buffer.readVarInt());
            return new Row(dimension, List.copyOf(kinds), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        public int count(Kind kind)
        {
            return kind.ordinal() < kinds.size() ? kinds.get(kind.ordinal()) : 0;
        }
    }

    /** @return a row for every level with loaded NPCs */
    public static List<Row> take(MinecraftServer server)
    {
        List<Row> rows = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels())
        {
            Row row = row(level);
            if (row.total() > 0)
                rows.add(row);
        }
        return rows;
    }

    private static Row row(ServerLevel level)
    {
        int[] kinds = new int[Kind.values().length];
        int fighting = 0;
        int stayActive = 0;
        int dormant = 0;
        for (Entity entity : level.getAllEntities())
        {
            if (!(entity instanceof EntityNPCInterface npc) || npc.isRemoved())
                continue;
            kinds[kind(npc).ordinal()]++;
            fighting += npc.getTarget() != null ? 1 : 0;
            stayActive += NpcEquipment.enabled(npc, Feature.STAY_ACTIVE) ? 1 : 0;
            dormant += NpcDormancy.dormant(npc) ? 1 : 0;
        }
        NpcActivity.Held held = NpcActivity.held(level);
        return new Row(level.dimension().location().toString(), Arrays.stream(kinds).boxed().toList(), fighting, stayActive, held.ids().size(), held.refused(), dormant);
    }

    static Kind kind(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        FlanModelKind kind = model == null ? null : FlanModelKind.of(model.getInfoType());
        if (kind == null)
            return Kind.STANDARD;
        return switch (kind)
        {
            case AA_GUN -> Kind.AA_GUN;
            case VEHICLE -> Kind.VEHICLE;
            case PLANE -> Kind.PLANE;
            case MECHA -> Kind.MECHA;
        };
    }
}
