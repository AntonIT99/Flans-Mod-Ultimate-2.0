package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What a wreck remembers of its load, how parts lost together come away, and how hot a wreck shows. */
class WreckStateTest
{
    @Test
    @DisplayName("A wreck's load survives being synchronised and saved")
    void loadRoundTrips()
    {
        WreckLoad load = new WreckLoad(0.37F, 0.82F);
        WreckLoad synced = WreckLoad.unpack(load.pack());
        assertEquals(0.37F, synced.ammunition(), 0.006F);
        assertEquals(0.82F, synced.fuel(), 0.006F);
        CompoundTag tag = new CompoundTag();
        load.save(tag);
        assertEquals(load, WreckLoad.load(tag));
        assertEquals(WreckLoad.UNKNOWN, WreckLoad.load(new CompoundTag()), "a wreck saved before loads were gets the assumed load");
    }

    @Test
    @DisplayName("Rounds and fuel are shares of a full load, kept in range")
    void loadIsClamped()
    {
        assertEquals(1F, WreckLoad.of(500, 2F).ammunition());
        assertEquals(1F, WreckLoad.of(500, 2F).fuel());
        assertEquals(0.5F, WreckLoad.of(WreckLoad.FULL_ROUNDS / 2, 0F).ammunition());
        assertEquals(0F, new WreckLoad(Float.NaN, -1F).ammunition());
        assertFalse(WreckLoad.of(0, 1F).cooksOff(), "an empty rack does not cook off");
        assertTrue(WreckLoad.of(3, 0F).cooksOff());
        assertTrue(WreckLoad.of(0, 0F).fireStrength() < WreckLoad.of(0, 1F).fireStrength(), "dry tanks smoulder, full ones blaze");
    }

    @Test
    @DisplayName("A wing comes off with what hangs from it, as one piece")
    void piecesCarryTheirChildren()
    {
        List<Set<EnumDriveablePart>> pieces = PartPieces.group(List.of(EnumDriveablePart.LEFT_WING_WHEEL, EnumDriveablePart.LEFT_WING, EnumDriveablePart.TOP_WING, EnumDriveablePart.TURRET));
        assertEquals(2, pieces.size());
        assertEquals(EnumSet.of(EnumDriveablePart.LEFT_WING, EnumDriveablePart.LEFT_WING_WHEEL, EnumDriveablePart.TOP_WING), pieces.get(0));
        assertEquals(EnumSet.of(EnumDriveablePart.TURRET), pieces.get(1));
    }

    @Test
    @DisplayName("A part whose parent stays on comes away alone, and the core is never a piece")
    void lonePartsAndTheCore()
    {
        List<Set<EnumDriveablePart>> pieces = PartPieces.group(List.of(EnumDriveablePart.CORE, EnumDriveablePart.TAIL_WHEEL));
        assertEquals(List.of(EnumSet.of(EnumDriveablePart.TAIL_WHEEL)), pieces);
        assertTrue(PartPieces.group(List.of()).isEmpty());
    }

    @Test
    @DisplayName("A wreck shows white-hot while it burns and cools as it burns out")
    void wreckCools()
    {
        int lifetime = 2400;
        assertEquals(1F, DamageVisualCurves.wreckHeat(-1, lifetime), "a working machine is hot");
        assertEquals(1F, DamageVisualCurves.wreckHeat(0, lifetime));
        float late = DamageVisualCurves.wreckHeat(lifetime - 1, lifetime);
        assertTrue(late < DamageVisualCurves.wreckHeat(DamageVisualCurves.fireTicks(lifetime) + 20, lifetime));
        assertTrue(late >= DamageVisualCurves.MIN_WRECK_HEAT);
    }
}
