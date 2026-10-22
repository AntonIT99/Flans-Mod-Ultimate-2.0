package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsRound;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import com.wolffsmod.npcs.network.BotEditorActionPacket;
import com.wolffsmod.npcs.network.BotEditorStatePacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;

/** The server side of the bot editor screen, for operators. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotEditor
{
    /** Permission level an operator needs, as for the {@code /teams ai} settings commands. */
    private static final int PERMISSION = 2;
    private static final String PREFIX = "wolffsmodnpcs.editor.";
    /** Keys of the data of preset and copy actions. */
    public static final String NAME = "name";
    public static final String SETTINGS = "settings";
    public static final String FROM = "from";

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
            case SAVE_SETTINGS -> saveSettings(player, round, packet);
            case SAVE_TEMPLATE -> SquadTemplate.fromTag(packet.data()).ifPresentOrElse(SquadTemplates.get()::put,
                () -> player.sendSystemMessage(Component.translatable(PREFIX + "bad_template_id").withStyle(ChatFormatting.RED)));
            case REMOVE_TEMPLATE -> SquadTemplates.get().remove(packet.data().getString("id"));
            case SAVE_PRESET -> savePreset(player, round, packet);
            case APPLY_PRESET -> applyPreset(player, round, packet);
            case REMOVE_PRESET -> BotPresets.get().remove(packet.data().getString(NAME));
            case COPY_ROUND -> copyRound(player, rounds, round, packet);
        }
        NpcNetwork.sendTo(player, state(packet.round()));
    }

    private static void savePreset(ServerPlayer player, @Nullable ITeamsRound round, BotEditorActionPacket packet)
    {
        String name = packet.data().getString(NAME).toLowerCase(Locale.ROOT);
        if (BotPresets.invalidName(name))
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.preset.bad_name").withStyle(ChatFormatting.RED));
            return;
        }
        if (outdated(player, round, packet))
            return;
        BotPresets.get().put(new BotPresets.Preset(name, teams(Objects.requireNonNull(round)), packet.data().getCompound(SETTINGS)));
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.preset.saved", name));
    }

    private static void applyPreset(ServerPlayer player, @Nullable ITeamsRound round, BotEditorActionPacket packet)
    {
        Optional<BotPresets.Preset> preset = BotPresets.get().find(packet.data().getString(NAME));
        if (preset.isEmpty() || outdated(player, round, packet))
            return;
        FlansTeams.setRoundExtensionData(Objects.requireNonNull(round), BotSettings.KEY, BotPresets.carryOver(preset.get().settings(), preset.get().teams(), teams(round)).save());
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.preset.applied", preset.get().name(), 1));
    }

    private static void copyRound(ServerPlayer player, List<ITeamsRound> rounds, @Nullable ITeamsRound round, BotEditorActionPacket packet)
    {
        int from = packet.data().getInt(FROM);
        if (from < 0 || from >= rounds.size() || outdated(player, round, packet))
            return;
        ITeamsRound source = rounds.get(from);
        FlansTeams.setRoundExtensionData(Objects.requireNonNull(round), BotSettings.KEY, BotPresets.carryOver(source.getExtensionData(BotSettings.KEY), teams(source), teams(round)).save());
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.copied", label(from, source)));
    }

    /** Whether the round the operator edits is no longer the one at its place in the rotation; tells them so. */
    private static boolean outdated(ServerPlayer player, @Nullable ITeamsRound round, BotEditorActionPacket packet)
    {
        if (round != null && round.getId().equals(packet.roundId()))
            return false;
        player.sendSystemMessage(Component.translatable(PREFIX + "rotation_changed").withStyle(ChatFormatting.RED));
        return true;
    }

    private static List<String> teams(ITeamsRound round)
    {
        return round.getTeams().stream().map(ITeam::getShortName).toList();
    }

    private static void saveSettings(ServerPlayer player, @Nullable ITeamsRound round, BotEditorActionPacket packet)
    {
        if (round == null || !round.getId().equals(packet.roundId()))
        {
            player.sendSystemMessage(Component.translatable(PREFIX + "rotation_changed").withStyle(ChatFormatting.RED));
            return;
        }
        BotSettings settings = BotSettings.load(packet.data());
        FlansTeams.setRoundExtensionData(round, BotSettings.KEY, settings.save());
        player.sendSystemMessage(Component.translatable(PREFIX + "saved"));
        problems(round, settings).forEach(problem -> player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.YELLOW)));
    }

    /**
     * What in a round's settings will not work as written: classes, squad templates and vehicles that do not exist or that
     * the team does not offer. Such entries are kept, since content packs or templates may still be added, but skipped.
     */
    public static List<Component> problems(ITeamsRound round, BotSettings settings)
    {
        List<Component> problems = new ArrayList<>();
        for (ITeam team : round.getTeams())
        {
            BotSettings.TeamForce force = settings.getTeams().get(team.getShortName());
            if (force == null)
                continue;
            String name = team.getShortName();
            Set<String> classes = team.getClasses().stream().map(IContentType::getShortName).collect(Collectors.toSet());
            force.getClassWeights().keySet().stream().filter(playerClass -> !classes.contains(playerClass))
                .forEach(playerClass -> problems.add(Component.translatable(PREFIX + "unknown_class", name, playerClass)));
            force.getClassRoles().keySet().stream().filter(playerClass -> !classes.contains(playerClass))
                .forEach(playerClass -> problems.add(Component.translatable(PREFIX + "unknown_role_class", name, playerClass)));
            for (BotSettings.SquadEntry entry : force.getSquads())
                templateProblems(name, classes, entry.template(), problems);
            for (BotSettings.VehicleEntry entry : force.getVehicles())
            {
                if (FlanModelNpcs.type(entry.type()).isEmpty())
                    problems.add(Component.translatable(PREFIX + "unknown_vehicle", name, entry.type()));
                if (!entry.carry().isEmpty())
                    templateProblems(name, classes, entry.carry(), problems);
            }
            crewProblems(name, classes, force, problems);
        }
        return problems;
    }

    /** Crew classes the team does not offer, for which a class is picked by weight instead. */
    private static void crewProblems(String team, Set<String> classes, BotSettings.TeamForce force, List<Component> problems)
    {
        for (CrewPost post : CrewPost.values())
        {
            String crew = force.crewClass(post);
            if (!crew.isEmpty() && !BotSettings.TeamForce.NO_CREW.equalsIgnoreCase(crew) && !classes.contains(crew))
                problems.add(Component.translatable(PREFIX + "unknown_crew_class", team, crew, post.id()));
        }
    }

    private static void templateProblems(String team, Set<String> classes, String id, List<Component> problems)
    {
        Optional<SquadTemplate> template = SquadTemplates.get().find(id);
        if (template.isEmpty())
        {
            problems.add(Component.translatable(PREFIX + "unknown_template", team, id));
            return;
        }
        template.get().members().stream().map(SquadTemplate.Member::playerClass).filter(playerClass -> !classes.contains(playerClass)).distinct()
            .forEach(playerClass -> problems.add(Component.translatable(PREFIX + "template_class", team, playerClass, id)));
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
        return new BotEditorStatePacket(entries, shown, settings.save(), SquadTemplates.get().all().stream().map(SquadTemplate::toTag).toList(), List.copyOf(BotPresets.get().names()));
    }

    private static String label(int index, ITeamsRound round)
    {
        return index + ": " + round.getGameTypeId() + " @ " + round.getMapId();
    }
}
