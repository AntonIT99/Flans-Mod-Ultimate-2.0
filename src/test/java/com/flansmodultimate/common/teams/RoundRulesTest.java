package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRulesTest
{
    @Test
    void deployedGunDestructionDefaultsOffAndPersists()
    {
        RoundRules defaults = new RoundRules();
        assertFalse(defaults.enabled(RoundRule.DESTROYABLE_DEPLOYED_GUNS));
        defaults.set(RoundRule.DESTROYABLE_DEPLOYED_GUNS, "true");
        RoundRules round = new RoundRules();
        assertTrue(round.resolve(RoundRules.load(defaults.save())).enabled(RoundRule.DESTROYABLE_DEPLOYED_GUNS));
        round.set(RoundRule.DESTROYABLE_DEPLOYED_GUNS, "false");
        assertFalse(RoundRules.load(round.save()).resolve(defaults).enabled(RoundRule.DESTROYABLE_DEPLOYED_GUNS));
    }

    @Test
    void legacyRoundsHaveNoEnabledRestrictionsOrTimingChanges()
    {
        TeamsRound old = new TeamsRound("map", "tdm", List.of("red", "blue"), 15, 50);
        CompoundTag tag = old.save();
        tag.remove("rules");
        RoundRules rules = TeamsRound.load(tag).getRules();
        for (RoundRule rule : RoundRule.values())
        {
            assertFalse(rules.overrides(rule));
            if (rule.getKind() == RoundRule.Kind.BOOLEAN)
                assertFalse(rules.enabled(rule));
            else if (rule.name().endsWith("_LIMIT"))
                assertEquals(-1, rules.number(rule));
            else if (rule.getKind() == RoundRule.Kind.INTEGER && rule != RoundRule.HOME_BASE_RADIUS)
                assertEquals(0, rules.number(rule));
        }
    }

    @Test
    void explicitOffOverridesServerDefaultAndInheritanceRestoresIt()
    {
        RoundRules defaults = new RoundRules();
        defaults.set(RoundRule.FLAG_NO_VEHICLES, "true");
        RoundRules round = new RoundRules();
        assertTrue(round.resolve(defaults).enabled(RoundRule.FLAG_NO_VEHICLES));
        round.set(RoundRule.FLAG_NO_VEHICLES, "false");
        assertFalse(round.resolve(defaults).enabled(RoundRule.FLAG_NO_VEHICLES));
        round.inherit(RoundRule.FLAG_NO_VEHICLES);
        assertTrue(round.resolve(defaults).enabled(RoundRule.FLAG_NO_VEHICLES));
    }

    @Test
    void runningSnapshotDoesNotChangeWhenDefaultsOrOverridesAreEdited()
    {
        RoundRules defaults = new RoundRules();
        defaults.set(RoundRule.PLAYER_LIVES, "3");
        RoundRules round = new RoundRules();
        round.set(RoundRule.RESPAWN_WAVE_SECONDS, "15");
        RoundRules active = round.resolve(defaults);
        defaults.set(RoundRule.PLAYER_LIVES, "1");
        round.set(RoundRule.RESPAWN_WAVE_SECONDS, "0");
        assertEquals(3, active.number(RoundRule.PLAYER_LIVES));
        assertEquals(15, active.number(RoundRule.RESPAWN_WAVE_SECONDS));
    }

    @Test
    void roundSerializationPreservesExplicitOverridesAndUnsetValues()
    {
        TeamsRound round = new TeamsRound("map", "ctf", List.of("red", "blue"), 15, 50);
        round.getRules().set(RoundRule.TANK_LIMIT, "0");
        round.getRules().set(RoundRule.FLAG_BANNED_ITEMS, "minecraft:stone, minecraft:bow,minecraft:stone");
        RoundRules loaded = TeamsRound.load(round.save()).getRules();
        assertTrue(loaded.overrides(RoundRule.TANK_LIMIT));
        assertEquals(0, loaded.number(RoundRule.TANK_LIMIT));
        assertFalse(loaded.overrides(RoundRule.AIRCRAFT_LIMIT));
        assertEquals("minecraft:bow,minecraft:stone", loaded.get(RoundRule.FLAG_BANNED_ITEMS));
    }

    @Test
    void malformedAndOutOfBoundsInputIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> RoundRule.REQUIRE_READY.validate("yes"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.PLAYER_LIVES.validate("-1"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.RESPAWN_WAVE_SECONDS.validate("301"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.FLAG_BANNED_ITEMS.validate("minecraft:bow,"));
        assertThrows(IllegalArgumentException.class, () -> RoundRule.FLAG_BANNED_ITEMS.validate("bad item"));
        CompoundTag damaged = new CompoundTag();
        damaged.putString("playerLives", "-1");
        damaged.putString("requireReady", "yes");
        damaged.putString("overtimeSeconds", "45");
        RoundRules recovered = RoundRules.load(damaged);
        assertEquals(0, recovered.number(RoundRule.PLAYER_LIVES));
        assertFalse(recovered.enabled(RoundRule.REQUIRE_READY));
        assertEquals(45, recovered.number(RoundRule.OVERTIME_SECONDS));
    }
}
