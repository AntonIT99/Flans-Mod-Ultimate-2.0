package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.teams.ObjectiveSnapshot;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The capture points of the running round on their own, sent whenever one of them visibly
 * changes so the objective HUD does not wait for the next full {@link PacketTeamsState}.
 */
@NoArgsConstructor
public final class PacketTeamsObjectives implements IClientPacket
{
    private List<ObjectiveSnapshot> objectives = List.of();

    public PacketTeamsObjectives(List<ObjectiveSnapshot> objectives)
    {
        this.objectives = List.copyOf(objectives);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeCollection(objectives, (buf, objective) -> objective.write(buf));
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        objectives = data.readList(ObjectiveSnapshot::read);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        TeamsClientState.acceptObjectives(objectives);
    }
}
