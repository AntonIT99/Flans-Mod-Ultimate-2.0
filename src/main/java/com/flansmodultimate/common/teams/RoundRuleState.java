package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent, server-owned state of one playing of a round, including players who temporarily disconnect. */
@Getter
public final class RoundRuleState
{
    @Getter
    @Setter
    public static final class Participant
    {
        private int deaths;
        private boolean awaitingRespawn;
        private int deathTick = -1000000;
        private int side;
        private int switchTick = -1000000;
        private int pendingTick = -1;
        private ITeamsRespawnRule.Reason pendingReason = ITeamsRespawnRule.Reason.DIED;
        private boolean ready;

        private CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt("deaths", deaths);
            tag.putBoolean("awaiting_respawn", awaitingRespawn);
            tag.putInt("death_tick", deathTick);
            tag.putInt("side", side);
            tag.putInt("switch_tick", switchTick);
            tag.putInt(PENDING_TICK, pendingTick);
            tag.putString("pending_reason", pendingReason.name());
            tag.putBoolean("ready", ready);
            return tag;
        }

        private static Participant load(CompoundTag tag)
        {
            Participant player = new Participant();
            player.deaths = Math.max(0, tag.getInt("deaths"));
            player.awaitingRespawn = tag.getBoolean("awaiting_respawn");
            player.deathTick = tag.getInt("death_tick");
            player.side = tag.getInt("side");
            player.switchTick = tag.getInt("switch_tick");
            player.pendingTick = tag.contains(PENDING_TICK) ? tag.getInt(PENDING_TICK) : -1;
            player.ready = tag.getBoolean("ready");
            try
            {
                player.pendingReason = ITeamsRespawnRule.Reason.valueOf(tag.getString("pending_reason"));
            }
            catch (IllegalArgumentException ignored)
            {
                player.pendingReason = ITeamsRespawnRule.Reason.DIED;
            }
            return player;
        }
    }

    private static final String PENDING_TICK = "pending_tick";
    private final RoundRules rules;
    private final Map<UUID, Participant> participants = new HashMap<>();
    @Setter
    private boolean staging;
    @Setter
    private int warmupTicks;
    @Setter
    private int overtimeTicks;
    @Setter
    private boolean overtime;

    public RoundRuleState(RoundRules rules)
    {
        this.rules = rules;
        warmupTicks = rules.number(RoundRule.WARMUP_SECONDS) * 20;
        staging = warmupTicks > 0 || rules.number(RoundRule.MINIMUM_PLAYERS) > 0 || rules.enabled(RoundRule.REQUIRE_READY);
    }

    public Participant player(UUID id)
    {
        return participants.computeIfAbsent(id, ignored -> new Participant());
    }

    public boolean exhausted(UUID id)
    {
        int lives = rules.number(RoundRule.PLAYER_LIVES);
        return lives > 0 && player(id).deaths >= lives;
    }

    /** First shared wave at or after the minimum legal spawn tick; never moves a request into the past. */
    public static int nextWaveTick(int earliest, int waveSeconds)
    {
        int interval = waveSeconds * 20;
        return interval <= 0 ? earliest : ((earliest + interval - 1) / interval) * interval;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.put("rules", rules.save());
        tag.putBoolean("staging", staging);
        tag.putInt("warmup", warmupTicks);
        tag.putInt("overtime_ticks", overtimeTicks);
        tag.putBoolean("overtime", overtime);
        CompoundTag players = new CompoundTag();
        participants.forEach((id, player) -> players.put(id.toString(), player.save()));
        tag.put("players", players);
        return tag;
    }

    public static RoundRuleState load(CompoundTag tag)
    {
        RoundRuleState state = new RoundRuleState(RoundRules.load(tag.getCompound("rules")));
        state.staging = tag.getBoolean("staging");
        state.warmupTicks = Math.max(0, tag.getInt("warmup"));
        state.overtimeTicks = Math.max(0, tag.getInt("overtime_ticks"));
        state.overtime = tag.getBoolean("overtime");
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys())
        {
            try
            {
                if (players.contains(key, Tag.TAG_COMPOUND))
                    state.participants.put(UUID.fromString(key), Participant.load(players.getCompound(key)));
            }
            catch (IllegalArgumentException ignored)
            {
                // Ignore an invalid player UUID in a damaged save.
            }
        }
        return state;
    }
}
