package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsRound;
import com.wolffsmod.npcs.network.BotEditorActionPacket;
import com.wolffsmod.npcs.network.BotEditorStatePacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** The server side of the bot editor screen, for operators. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotEditor
{
    /** Permission level an operator needs, as for the {@code /teams ai} settings commands. */
    private static final int PERMISSION = 2;

    /** Opens the editor on a round of the rotation, the round being played when the index is out of range. */
    public static void open(ServerPlayer player, int round)
    {
        if (player.hasPermissions(PERMISSION))
            NpcNetwork.sendTo(player, state(round));
    }

    public static void handle(ServerPlayer player, BotEditorActionPacket packet)
    {
        if (!player.hasPermissions(PERMISSION))
            return;
        List<ITeamsRound> rounds = FlansTeams.getRounds();
        ITeamsRound round = packet.round() >= 0 && packet.round() < rounds.size() ? rounds.get(packet.round()) : null;
        switch (packet.action())
        {
            case OPEN -> {
                // The state is sent below.
            }
            case SAVE_SETTINGS -> {
                if (round == null || !round.getId().equals(packet.roundId()))
                    player.sendSystemMessage(Component.literal("The rotation changed; the bot settings were not saved"));
                else
                    FlansTeams.setRoundExtensionData(round, BotSettings.KEY, BotSettings.load(packet.data()).save());
            }
            case SAVE_TEMPLATE -> SquadTemplate.fromTag(packet.data()).ifPresentOrElse(SquadTemplates.get()::put,
                () -> player.sendSystemMessage(Component.literal("A squad template needs an id of lower-case letters, digits, - and _")));
            case REMOVE_TEMPLATE -> SquadTemplates.get().remove(packet.data().getString("id"));
        }
        NpcNetwork.sendTo(player, state(packet.round()));
    }

    /** The editor's view of a round, the round being played when the index is out of range. */
    static BotEditorStatePacket state(int round)
    {
        List<ITeamsRound> rounds = FlansTeams.getRounds();
        int shown = round;
        if (shown < 0 || shown >= rounds.size())
        {
            shown = FlansTeams.getRunningRound().map(live -> rounds.indexOf(live.getRound())).orElse(0);
            shown = rounds.isEmpty() ? -1 : Math.max(0, shown);
        }
        List<BotEditorStatePacket.Round> entries = rounds.stream()
            .map(entry -> new BotEditorStatePacket.Round(entry.getId(), label(rounds.indexOf(entry), entry), entry.getTeams().stream().map(ITeam::getShortName).toList())).toList();
        BotSettings settings = shown < 0 ? new BotSettings() : BotSettings.load(rounds.get(shown).getExtensionData(BotSettings.KEY));
        return new BotEditorStatePacket(entries, shown, settings.save(), SquadTemplates.get().all().stream().map(SquadTemplate::toTag).toList());
    }

    private static String label(int index, ITeamsRound round)
    {
        return index + ": " + round.getGameTypeId() + " @ " + round.getMapId();
    }
}
