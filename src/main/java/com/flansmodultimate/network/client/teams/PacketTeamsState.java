package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.PacketIO;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

/** Immutable, per-player view of the Teams runtime used by all legacy-style screens. */
@NoArgsConstructor
public final class PacketTeamsState implements IClientPacket
{
    public enum OpenScreen
    {
        NONE, TEAM_SELECT, CLASS_SELECT, SCOREBOARD, VOTING, CLOSE, DEPLOY
    }

    public record TeamChoice(String id, String name, int colour)
    {}
    /**
     * A class of the selected team. {@code capacity} is the number of players of that team who may
     * use it at once, or {@link #UNLIMITED_CLASS} for a class without a limit, and {@code used} how
     * many of those slots other players have taken.
     */
    public record ClassChoice(String id, String name, int unlockLevel, int requiredPrestige, int used, int capacity, List<ItemStack> loadout)
    {
        public boolean isFull()
        {
            return capacity != UNLIMITED_CLASS && used >= capacity;
        }
    }
    public record PlayerScore(String name, int score, int kills, int deaths, int zombieScore, String playerClass)
    {}
    /**
     * A team of the round. Allied teams carry the same {@code sideId}, the round team id of the first
     * team of their side, and the same {@code score}, which belongs to the side as a whole.
     */
    public record TeamScore(String id, String name, int colour, int score, int sideId, List<PlayerScore> players)
    {}
    public record VoteOption(String mapName, String gameType, String teams, int votes)
    {}
    /** Viewer-independent data, built once for each broadcast rather than once per player. */
    public record SharedScoreboard(List<TeamScore> teamScores, List<VoteOption> voteOptions, List<ObjectiveSnapshot> objectives)
    {}

    /** Written for {@link ClassChoice#capacity()} when a class has no limit. */
    public static final int UNLIMITED_CLASS = -1;

    private OpenScreen openScreen = OpenScreen.NONE;
    private boolean enabled;
    private boolean vehiclesCanZoom;
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
    private int playerPrestige;
    /** Round team id the viewer plays or is about to play for; 0 when they have none. */
    private int playerTeamId;
    /** Side id of that team: the id capture points are owned under and allies have in common. */
    private int playerSideId;
    private ObjectiveMode objectiveMode = ObjectiveMode.NONE;
    private int attackingTeamId;
    private int defendingTeamId;
    private int deployDelaySeconds;
    private boolean canEditDeployLayout;
    private String mapId = "";
    private String deployImage = "";
    private TeamsMap.ImageCrop deployImageCrop = TeamsMap.ImageCrop.FULL;
    private List<DeployLayoutEntry> deployLayout = List.of();
    private List<ObjectiveSnapshot> objectives = List.of();
    /** Whether a round has just ended, which is what makes a blank {@link #winnerId} a draw. */
    private boolean roundDecided;
    private String winnerId = "";

    public static PacketTeamsState create(TeamsManager manager, ServerPlayer viewer, OpenScreen openScreen)
    {
        return create(manager, viewer, openScreen, createSharedScoreboard(manager));
    }

    public static PacketTeamsState create(TeamsManager manager, ServerPlayer viewer, OpenScreen openScreen, SharedScoreboard shared)
    {
        PacketTeamsState packet = new PacketTeamsState();
        packet.openScreen = openScreen;
        packet.enabled = manager.isEnabled();
        packet.vehiclesCanZoom = manager.isVehiclesCanZoom();
        packet.roundRunning = manager.isRoundRunning();
        packet.timeLeftTicks = manager.getRoundTimeLeftTicks();
        packet.intermissionTicks = manager.getIntermissionTicks();

        PlayerData viewerData = PlayerData.getInstance(viewer);
        packet.playerRank = manager.getStats(viewer).getRank();
        packet.playerPrestige = manager.getStats(viewer).getPrestige();
        packet.roundDecided = manager.isLastRoundDecided();
        packet.winnerId = manager.getLastWinnerId();
        packet.playerVote = viewerData.getVote();
        Team selectedTeam = viewerData.getNewTeam();
        PlayerClass selectedClass = viewerData.getNewPlayerClass();
        packet.selectedTeam = selectedTeam == null ? "" : selectedTeam.getShortName();
        packet.selectedClass = selectedClass == null ? "" : selectedClass.getShortName();

        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null)
            return packet;

        GameType gameType = round.getGametype();
        packet.gameType = gameType == null ? round.getGameTypeId() : gameType.getName();
        packet.mapName = manager.getMap(round.getMapId()).map(TeamsMap::getName).orElse(round.getMapId());
        packet.scoreLimit = round.getScoreLimit();
        packet.sortedByTeam = gameType == null || gameType.isScoreboardSortedByTeam();
        packet.showZombieScore = gameType != null && gameType.showZombieScore();
        packet.playerTeamId = round.getTeamId(selectedTeam);
        packet.playerSideId = round.getSideId(selectedTeam);
        if (gameType != null && gameType.getObjectiveMode() != ObjectiveMode.NONE)
            packet.describeObjectives(manager, viewer, round, gameType, shared);

        if (openScreen == OpenScreen.TEAM_SELECT)
            packet.teamChoices = teamChoices(manager, viewer, round);
        if (openScreen == OpenScreen.CLASS_SELECT && selectedTeam != null && selectedTeam != Team.SPECTATORS)
            packet.classChoices = selectedTeam.getClasses().stream().map(playerClass -> classChoice(manager, viewer, selectedTeam, playerClass)).toList();

        packet.teamScores = shared.teamScores();
        packet.voteOptions = shared.voteOptions();
        return packet;
    }

    private static List<TeamChoice> teamChoices(TeamsManager manager, ServerPlayer viewer, TeamsRound round)
    {
        List<TeamChoice> choices = new ArrayList<>();
        for (String id : round.getTeamIds())
        {
            Team team = Team.getTeam(id);
            if (team != null && manager.canChooseTeam(viewer, team))
                choices.add(new TeamChoice(id, team.getName(), team.getTeamColour()));
        }
        choices.add(new TeamChoice(Team.SPECTATORS_ID, Team.SPECTATORS.getName(), Team.SPECTATORS.getTeamColour()));
        if (viewer.hasPermissions(2))
            choices.add(new TeamChoice("$builder", "No Team / Builder", 0xB0B0B0));
        return List.copyOf(choices);
    }

    private static ClassChoice classChoice(TeamsManager manager, ServerPlayer viewer, Team team, PlayerClass playerClass)
    {
        int capacity = manager.getClassCapacity(team, playerClass);
        return new ClassChoice(playerClass.getShortName(), playerClass.getName(), playerClass.getUnlockLevel(), playerClass.getRequiredPrestigeLevel(),
            manager.countPlayersUsingClass(team, playerClass, viewer), capacity == ClassLimits.UNLIMITED ? UNLIMITED_CLASS : capacity, playerClass.createStartingItemPreviews());
    }

    /** Adds what the objective HUD and the deployment map need to know about a round fought over capture points. */
    private void describeObjectives(TeamsManager manager, ServerPlayer viewer, TeamsRound round, GameType type, SharedScoreboard shared)
    {
        objectiveMode = type.getObjectiveMode();
        attackingTeamId = type.getAttackingTeamId(round);
        defendingTeamId = type.getDefendingTeamId(round);
        deployDelaySeconds = type.getDeployDelaySeconds(manager, playerTeamId);
        canEditDeployLayout = viewer.hasPermissions(2);
        objectives = shared.objectives();
        TeamsMap map = manager.getMap(round.getMapId()).orElse(null);
        if (map == null)
            return;
        mapId = map.getShortName();
        deployImage = map.getDeployImage();
        deployImageCrop = map.getDeployImageCrop();
        deployLayout = map.getDeployLayout();
    }

    public static SharedScoreboard createSharedScoreboard(TeamsManager manager)
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null)
            return new SharedScoreboard(List.of(), List.of(), List.of());

        List<TeamScore> scores = new ArrayList<>();
        for (String id : round.getTeamIds())
        {
            Team team = Team.getTeam(id);
            if (team == null)
                continue;
            List<PlayerScore> players = manager.getPlayersOnTeam(team).stream().map(PacketTeamsState::playerScore)
                .sorted(Comparator.comparingInt(PlayerScore::score).reversed().thenComparing(PlayerScore::name, String.CASE_INSENSITIVE_ORDER)).toList();
            scores.add(new TeamScore(id, team.getName(), team.getTeamColour(), manager.getTeamScore(team), round.getSideId(team), players));
        }

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
            String map = manager.getMap(option.getMapId()).map(TeamsMap::getName).orElse(option.getMapId());
            GameType type = option.getGametype();
            String teams = option.getSideIds().stream().map(sideId -> option.getSideDisplayComponent(sideId).getString()).filter(side -> !side.isEmpty()).reduce((left, right) -> left + " vs " + right)
                .orElse("");
            options.add(new VoteOption(map, type == null ? option.getGameTypeId() : type.getName(), teams, votes[i]));
        }
        GameType gameType = round.getGametype();
        return new SharedScoreboard(List.copyOf(scores), List.copyOf(options), gameType == null ? List.of() : gameType.createObjectives(manager));
    }

    private static PlayerScore playerScore(ServerPlayer player)
    {
        PlayerData data = PlayerData.getInstance(player);
        return new PlayerScore(player.getScoreboardName(), data.getScore(), data.getKills(), data.getDeaths(), data.getZombieScore(),
            data.getPlayerClass() == null ? "" : data.getPlayerClass().getName());
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeByte(openScreen.ordinal());
        data.writeBoolean(enabled);
        data.writeBoolean(vehiclesCanZoom);
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

        data.writeCollection(teamChoices, (buf, choice) ->
        {
            buf.writeUtf(choice.id());
            buf.writeUtf(choice.name());
            buf.writeInt(choice.colour());
        });
        data.writeVarInt(classChoices.size());
        for (ClassChoice choice : classChoices)
        {
            data.writeUtf(choice.id());
            data.writeUtf(choice.name());
            data.writeVarInt(choice.unlockLevel());
            data.writeVarInt(choice.requiredPrestige());
            data.writeVarInt(choice.used());
            data.writeVarInt(choice.capacity() + 1);
            PacketIO.writeItems(data, choice.loadout());
        }
        data.writeCollection(teamScores, (buf, team) ->
        {
            buf.writeUtf(team.id());
            buf.writeUtf(team.name());
            buf.writeInt(team.colour());
            buf.writeVarInt(team.score());
            buf.writeVarInt(team.sideId());
            buf.writeCollection(team.players(), PacketTeamsState::writePlayer);
        });
        data.writeCollection(voteOptions, (buf, option) ->
        {
            buf.writeUtf(option.mapName());
            buf.writeUtf(option.gameType());
            buf.writeUtf(option.teams());
            buf.writeVarInt(option.votes());
        });
        data.writeVarInt(playerPrestige);
        data.writeVarInt(playerTeamId);
        data.writeVarInt(playerSideId);
        data.writeBoolean(roundDecided);
        data.writeUtf(winnerId);
        data.writeByte(objectiveMode.ordinal());
        if (objectiveMode == ObjectiveMode.NONE)
            return;
        data.writeVarInt(attackingTeamId);
        data.writeVarInt(defendingTeamId);
        data.writeVarInt(deployDelaySeconds);
        data.writeBoolean(canEditDeployLayout);
        data.writeUtf(mapId);
        data.writeUtf(deployImage);
        data.writeFloat(deployImageCrop.minU());
        data.writeFloat(deployImageCrop.minV());
        data.writeFloat(deployImageCrop.maxU());
        data.writeFloat(deployImageCrop.maxV());
        data.writeCollection(deployLayout, (buf, entry) ->
        {
            buf.writeUtf(entry.key(), DeployLayoutEntry.MAX_KEY_LENGTH);
            buf.writeFloat(entry.x());
            buf.writeFloat(entry.y());
        });
        data.writeCollection(objectives, (buf, objective) -> objective.write(buf));
    }

    private static void writePlayer(FriendlyByteBuf data, PlayerScore player)
    {
        data.writeUtf(player.name());
        data.writeInt(player.score());
        data.writeVarInt(player.kills());
        data.writeVarInt(player.deaths());
        data.writeVarInt(player.zombieScore());
        data.writeUtf(player.playerClass());
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        int screen = data.readUnsignedByte();
        openScreen = screen < OpenScreen.values().length ? OpenScreen.values()[screen] : OpenScreen.NONE;
        enabled = data.readBoolean();
        vehiclesCanZoom = data.readBoolean();
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
            classChoices.add(new ClassChoice(data.readUtf(), data.readUtf(), data.readVarInt(), data.readVarInt(), data.readVarInt(), data.readVarInt() - 1, PacketIO.readItems(data)));
        teamScores = data.readList(buf -> new TeamScore(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readList(PacketTeamsState::readPlayer)));
        voteOptions = data.readList(buf -> new VoteOption(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
        playerPrestige = data.readVarInt();
        playerTeamId = data.readVarInt();
        playerSideId = data.readVarInt();
        roundDecided = data.readBoolean();
        winnerId = data.readUtf();
        objectiveMode = ObjectiveMode.byOrdinal(data.readUnsignedByte());
        if (objectiveMode == ObjectiveMode.NONE)
            return;
        attackingTeamId = data.readVarInt();
        defendingTeamId = data.readVarInt();
        deployDelaySeconds = data.readVarInt();
        canEditDeployLayout = data.readBoolean();
        mapId = data.readUtf();
        deployImage = data.readUtf();
        deployImageCrop = TeamsMap.ImageCrop.normalised(data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat());
        deployLayout = data.readList(buf -> new DeployLayoutEntry(buf.readUtf(DeployLayoutEntry.MAX_KEY_LENGTH), buf.readFloat(), buf.readFloat()));
        objectives = data.readList(ObjectiveSnapshot::read);
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

    public OpenScreen getOpenScreen()
    {
        return openScreen;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isVehiclesCanZoom()
    {
        return vehiclesCanZoom;
    }

    public boolean isRoundRunning()
    {
        return roundRunning;
    }

    public boolean isSortedByTeam()
    {
        return sortedByTeam;
    }

    public boolean isShowZombieScore()
    {
        return showZombieScore;
    }

    public String getMapName()
    {
        return mapName;
    }

    public String getGameType()
    {
        return gameType;
    }

    public int getTimeLeftTicks()
    {
        return timeLeftTicks;
    }

    public int getIntermissionTicks()
    {
        return intermissionTicks;
    }

    public int getScoreLimit()
    {
        return scoreLimit;
    }

    public int getPlayerRank()
    {
        return playerRank;
    }

    public int getPlayerVote()
    {
        return playerVote;
    }

    public String getSelectedTeam()
    {
        return selectedTeam;
    }

    public String getSelectedClass()
    {
        return selectedClass;
    }

    public List<TeamChoice> getTeamChoices()
    {
        return teamChoices;
    }

    public List<ClassChoice> getClassChoices()
    {
        return classChoices;
    }

    public List<TeamScore> getTeamScores()
    {
        return teamScores;
    }

    public List<VoteOption> getVoteOptions()
    {
        return voteOptions;
    }

    public int getPlayerPrestige()
    {
        return playerPrestige;
    }

    public int getPlayerTeamId()
    {
        return playerTeamId;
    }

    public ObjectiveMode getObjectiveMode()
    {
        return objectiveMode;
    }

    public int getAttackingTeamId()
    {
        return attackingTeamId;
    }

    public int getDefendingTeamId()
    {
        return defendingTeamId;
    }

    public int getDeployDelaySeconds()
    {
        return deployDelaySeconds;
    }

    public boolean canEditDeployLayout()
    {
        return canEditDeployLayout;
    }

    public String getMapId()
    {
        return mapId;
    }

    public String getDeployImage()
    {
        return deployImage;
    }

    public TeamsMap.ImageCrop getDeployImageCrop()
    {
        return deployImageCrop;
    }

    public List<DeployLayoutEntry> getDeployLayout()
    {
        return deployLayout;
    }

    public List<ObjectiveSnapshot> getObjectives()
    {
        return objectives;
    }

    public boolean isRoundDecided()
    {
        return roundDecided;
    }

    public String getWinnerId()
    {
        return winnerId;
    }

    public int getPlayerSideId()
    {
        return playerSideId;
    }

    /**
     * The team score of a round team id, or null when the round has no such team. Given a side id
     * it is the first team of that side, whose colour and score stand for the whole side.
     */
    @Nullable
    public TeamScore getTeamScore(int teamId)
    {
        int index = teamId - TeamsRound.FIRST_TEAM_ID;
        return index >= 0 && index < teamScores.size() ? teamScores.get(index) : null;
    }

    /** The side ids of the round in order, one for each group of allied teams. */
    public List<Integer> getSideIds()
    {
        return teamScores.stream().map(TeamScore::sideId).distinct().toList();
    }

    /** The teams fighting on a side, in round order. */
    public List<TeamScore> getTeamsOnSide(int sideId)
    {
        return teamScores.stream().filter(team -> team.sideId() == sideId).toList();
    }

    /** The names of the teams of a side, joined with ampersands. */
    public String getSideName(int sideId)
    {
        return getTeamsOnSide(sideId).stream().map(TeamScore::name).reduce((left, right) -> left + " & " + right).orElse("");
    }

    /** The side id of a team by short name, or 0 for one that is not playing this round. */
    public int getSideId(String teamId)
    {
        return teamScores.stream().filter(team -> team.id().equals(teamId)).mapToInt(TeamScore::sideId).findFirst().orElse(0);
    }

    /** Whether two teams, by short name, fight on the same side of the round. */
    public boolean areAllied(String firstTeamId, String secondTeamId)
    {
        int side = getSideId(firstTeamId);
        return side >= TeamsRound.FIRST_TEAM_ID && side == getSideId(secondTeamId);
    }
}
