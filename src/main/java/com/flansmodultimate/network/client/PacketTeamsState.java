package com.flansmodultimate.network.client;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.GameType;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketIO;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable, per-player view of the Teams runtime used by all legacy-style screens. */
@NoArgsConstructor
public final class PacketTeamsState implements IClientPacket
{
    public enum OpenScreen { NONE, TEAM_SELECT, CLASS_SELECT, SCOREBOARD, VOTING, CLOSE }

    public record TeamChoice(String id, String name, int colour) {}
    public record ClassChoice(String id, String name, int unlockLevel, List<ItemStack> loadout) {}
    public record PlayerScore(String name, int score, int kills, int deaths, int zombieScore, String playerClass) {}
    public record TeamScore(String id, String name, int colour, int score, List<PlayerScore> players) {}
    public record VoteOption(String mapName, String gameType, String teams, int votes) {}

    private OpenScreen openScreen = OpenScreen.NONE;
    private boolean enabled;
    private boolean roundRunning;
    private boolean sortedByTeam;
    private boolean showZombieScore;
    private String mapName = "";
    private String gameType = "";
    private int timeLeftTicks;
    private int intermissionTicks;
    private int scoreLimit;
    private int playerRank;
    private int playerVote;
    private String selectedTeam = "";
    private String selectedClass = "";
    private List<TeamChoice> teamChoices = List.of();
    private List<ClassChoice> classChoices = List.of();
    private List<TeamScore> teamScores = List.of();
    private List<VoteOption> voteOptions = List.of();

    public static PacketTeamsState create(TeamsManager manager, ServerPlayer viewer, OpenScreen openScreen)
    {
        PacketTeamsState packet = new PacketTeamsState();
        packet.openScreen = openScreen;
        packet.enabled = manager.isEnabled();
        packet.roundRunning = manager.isRoundRunning();
        packet.timeLeftTicks = manager.getRoundTimeLeftTicks();
        packet.intermissionTicks = manager.getIntermissionTicks();

        PlayerData viewerData = PlayerData.getInstance(viewer);
        packet.playerRank = manager.getStats(viewer).getRank();
        packet.playerVote = viewerData.getVote();
        Team selectedTeam = viewerData.getNewTeam();
        PlayerClass selectedClass = viewerData.getNewPlayerClass();
        packet.selectedTeam = selectedTeam == null ? "" : selectedTeam.getOriginalShortName();
        packet.selectedClass = selectedClass == null ? "" : selectedClass.getOriginalShortName();

        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null)
            return packet;

        GameType gameType = round.getGametype();
        packet.gameType = gameType == null ? round.getGameTypeId() : gameType.getName();
        packet.mapName = manager.getMap(round.getMapId()).map(map -> map.getName()).orElse(round.getMapId());
        packet.scoreLimit = round.getScoreLimit();
        packet.sortedByTeam = gameType == null || gameType.isScoreboardSortedByTeam();
        packet.showZombieScore = gameType != null && gameType.showZombieScore();

        if (openScreen == OpenScreen.TEAM_SELECT)
        {
            List<TeamChoice> teamChoices = new ArrayList<>();
            for (String id : round.getTeamIds())
            {
                Team team = Team.getTeam(id);
                if (team != null)
                    teamChoices.add(new TeamChoice(id, team.getName(), team.getTeamColour()));
            }
            teamChoices.add(new TeamChoice(Team.SPECTATORS_ID, Team.SPECTATORS.getName(), Team.SPECTATORS.getTeamColour()));
            if (viewer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                teamChoices.add(new TeamChoice("$builder", "No Team / Builder", 0xB0B0B0));
            packet.teamChoices = List.copyOf(teamChoices);
        }

        if (openScreen == OpenScreen.CLASS_SELECT && selectedTeam != null && selectedTeam != Team.SPECTATORS)
        {
            packet.classChoices = selectedTeam.getClasses().stream().map(playerClass ->
                new ClassChoice(playerClass.getOriginalShortName(), playerClass.getName(), playerClass.getUnlockLevel(),
                    playerClass.createStartingItemPreviews())).toList();
        }

        List<TeamScore> scores = new ArrayList<>();
        for (String id : round.getTeamIds())
        {
            Team team = Team.getTeam(id);
            if (team == null)
                continue;
            List<PlayerScore> players = manager.getPlayersOnTeam(team).stream()
                .map(player -> playerScore(player))
                .sorted(Comparator.comparingInt(PlayerScore::score).reversed().thenComparing(PlayerScore::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
            scores.add(new TeamScore(id, team.getName(), team.getTeamColour(), manager.getTeamScore(team), players));
        }
        packet.teamScores = List.copyOf(scores);

        int[] votes = new int[manager.getVoteOptions().size()];
        for (ServerPlayer player : manager.getServer().getPlayerList().getPlayers())
        {
            int vote = PlayerData.getInstance(player).getVote();
            if (vote > 0 && vote <= votes.length)
                votes[vote - 1]++;
        }
        List<VoteOption> options = new ArrayList<>();
        for (int i = 0; i < manager.getVoteOptions().size(); i++)
        {
            TeamsRound option = manager.getVoteOptions().get(i);
            String map = manager.getMap(option.getMapId()).map(value -> value.getName()).orElse(option.getMapId());
            GameType type = option.getGametype();
            String teams = option.getTeamIds().stream().map(Team::getTeam).filter(java.util.Objects::nonNull)
                .map(Team::getName).reduce((left, right) -> left + " vs " + right).orElse("");
            options.add(new VoteOption(map, type == null ? option.getGameTypeId() : type.getName(), teams, votes[i]));
        }
        packet.voteOptions = List.copyOf(options);
        return packet;
    }

    private static PlayerScore playerScore(ServerPlayer player)
    {
        PlayerData data = PlayerData.getInstance(player);
        return new PlayerScore(player.getScoreboardName(), data.getScore(), data.getKills(), data.getDeaths(), data.getZombieScore(),
            data.getPlayerClass() == null ? "" : data.getPlayerClass().getName());
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeByte(openScreen.ordinal());
        data.writeBoolean(enabled);
        data.writeBoolean(roundRunning);
        data.writeBoolean(sortedByTeam);
        data.writeBoolean(showZombieScore);
        data.writeUtf(mapName);
        data.writeUtf(gameType);
        data.writeVarInt(timeLeftTicks);
        data.writeVarInt(intermissionTicks);
        data.writeVarInt(scoreLimit);
        data.writeVarInt(playerRank);
        data.writeVarInt(playerVote);
        data.writeUtf(selectedTeam);
        data.writeUtf(selectedClass);

        data.writeCollection(teamChoices, (buf, choice) -> {
            buf.writeUtf(choice.id()); buf.writeUtf(choice.name()); buf.writeInt(choice.colour());
        });
        data.writeVarInt(classChoices.size());
        for (ClassChoice choice : classChoices)
        {
            data.writeUtf(choice.id()); data.writeUtf(choice.name()); data.writeVarInt(choice.unlockLevel());
            PacketIO.writeItems(data, choice.loadout());
        }
        data.writeCollection(teamScores, (buf, team) -> {
            buf.writeUtf(team.id()); buf.writeUtf(team.name()); buf.writeInt(team.colour()); buf.writeVarInt(team.score());
            buf.writeCollection(team.players(), PacketTeamsState::writePlayer);
        });
        data.writeCollection(voteOptions, (buf, option) -> {
            buf.writeUtf(option.mapName()); buf.writeUtf(option.gameType()); buf.writeUtf(option.teams()); buf.writeVarInt(option.votes());
        });
    }

    private static void writePlayer(FriendlyByteBuf data, PlayerScore player)
    {
        data.writeUtf(player.name()); data.writeInt(player.score()); data.writeVarInt(player.kills());
        data.writeVarInt(player.deaths()); data.writeVarInt(player.zombieScore()); data.writeUtf(player.playerClass());
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        int screen = data.readUnsignedByte();
        openScreen = screen < OpenScreen.values().length ? OpenScreen.values()[screen] : OpenScreen.NONE;
        enabled = data.readBoolean();
        roundRunning = data.readBoolean();
        sortedByTeam = data.readBoolean();
        showZombieScore = data.readBoolean();
        mapName = data.readUtf();
        gameType = data.readUtf();
        timeLeftTicks = data.readVarInt();
        intermissionTicks = data.readVarInt();
        scoreLimit = data.readVarInt();
        playerRank = data.readVarInt();
        playerVote = data.readVarInt();
        selectedTeam = data.readUtf();
        selectedClass = data.readUtf();
        teamChoices = data.readList(buf -> new TeamChoice(buf.readUtf(), buf.readUtf(), buf.readInt()));
        int classCount = data.readVarInt();
        classChoices = new ArrayList<>(classCount);
        for (int i = 0; i < classCount; i++)
            classChoices.add(new ClassChoice(data.readUtf(), data.readUtf(), data.readVarInt(), PacketIO.readItems(data)));
        teamScores = data.readList(buf -> new TeamScore(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readVarInt(), buf.readList(PacketTeamsState::readPlayer)));
        voteOptions = data.readList(buf -> new VoteOption(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
    }

    private static PlayerScore readPlayer(FriendlyByteBuf data)
    {
        return new PlayerScore(data.readUtf(), data.readInt(), data.readVarInt(), data.readVarInt(), data.readVarInt(), data.readUtf());
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        TeamsClientState.accept(this);
    }

    public OpenScreen getOpenScreen() { return openScreen; }
    public boolean isEnabled() { return enabled; }
    public boolean isRoundRunning() { return roundRunning; }
    public boolean isSortedByTeam() { return sortedByTeam; }
    public boolean isShowZombieScore() { return showZombieScore; }
    public String getMapName() { return mapName; }
    public String getGameType() { return gameType; }
    public int getTimeLeftTicks() { return timeLeftTicks; }
    public int getIntermissionTicks() { return intermissionTicks; }
    public int getScoreLimit() { return scoreLimit; }
    public int getPlayerRank() { return playerRank; }
    public int getPlayerVote() { return playerVote; }
    public String getSelectedTeam() { return selectedTeam; }
    public String getSelectedClass() { return selectedClass; }
    public List<TeamChoice> getTeamChoices() { return teamChoices; }
    public List<ClassChoice> getClassChoices() { return classChoices; }
    public List<TeamScore> getTeamScores() { return teamScores; }
    public List<VoteOption> getVoteOptions() { return voteOptions; }
}
