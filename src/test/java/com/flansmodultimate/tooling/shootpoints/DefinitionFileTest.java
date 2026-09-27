package com.flansmodultimate.tooling.shootpoints;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefinitionFileTest
{
    private static DefinitionFile parse(String text)
    {
        return DefinitionFile.parse(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String text(DefinitionFile file)
    {
        return new String(file.toBytes(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void anUntouchedFileRoundTripsByteForByte()
    {
        byte[] bytes = ("ï»¿Name Tiger\r\n// comment\r\n\tModel W44.Tiger  \r\nDescription PzÃ¤\n"
            + "BarrelPosition 1 2 3").getBytes(StandardCharsets.ISO_8859_1);

        DefinitionFile file = DefinitionFile.parse(bytes);

        assertArrayEquals(bytes, file.toBytes());
        assertFalse(file.isModified());
        assertEquals("Tiger", file.lastValue("name"));
    }

    @Test
    void keysMatchAnyCaseAndCommentsEndTheValues()
    {
        DefinitionFile file = parse("shootpointprimary 1 2 3 turret // main gun\n//ShootPointPrimary 9 9 9\n"
            + "  // ShootPointPrimary 8 8 8\n");

        List<DefinitionFile.Line> lines = file.lines("ShootPointPrimary");

        assertEquals(1, lines.size());
        assertEquals(4, lines.get(0).size());
        assertEquals("turret", lines.get(0).value(3));
    }

    @Test
    void theLastLineOfASingleValueKeyWins()
    {
        DefinitionFile file = parse("ModelScale 1\nModelScale 0.5\n");

        assertEquals("0.5", file.lastValue("ModelScale"));
        assertNull(file.lastValue("VehicleGunModelScale"));
    }

    @Test
    void replacingValuesKeepsSpacingAndComments()
    {
        DefinitionFile file = parse("BarrelPosition  10\t-2 0 5 5 5   // tip\r\nNext 1\r\n");

        file.replaceValues(0, 3, List.of("62.5", "3", "-1"));

        assertEquals("BarrelPosition  10\t-2 0 62.5 3 -1   // tip\r\nNext 1\r\n", text(file));
        assertTrue(file.isModified());
    }

    @Test
    void insertedLinesTakeTheFileLineEndingAndTheAnchorIndent()
    {
        DefinitionFile file = parse("  Passenger 1 0 0 0 turret\r\nOther 1\r\n");

        file.insertAfter(0, "GunOrigin 1 4 5 6");

        assertEquals("  Passenger 1 0 0 0 turret\r\n  GunOrigin 1 4 5 6\r\nOther 1\r\n", text(file));
    }

    @Test
    void appendingKeepsWhetherTheFileEndedWithALineBreak()
    {
        DefinitionFile ended = parse("A 1\n");
        ended.append("B 2");
        DefinitionFile open = parse("A 1");
        open.append("B 2");

        assertEquals("A 1\nB 2\n", text(ended));
        assertEquals("A 1\nB 2", text(open));
    }

    @Test
    void bracketedValuesAreNotPlain()
    {
        DefinitionFile file = parse("GunOrigin 1 [1,2,3]\nGunOrigin 2 1 2 3\n");

        List<DefinitionFile.Line> lines = file.lines("GunOrigin");

        assertFalse(lines.get(0).plain());
        assertTrue(lines.get(1).plain());
    }

    @Test
    void numbersAreWrittenWithoutTrailingZerosOrNegativeZero()
    {
        assertEquals("12", DefinitionFile.formatNumber(12.0004F, 1));
        assertEquals("12.5", DefinitionFile.formatNumber(12.5F, 2));
        assertEquals("-0.13", DefinitionFile.formatNumber(-0.125D, 2));
        assertEquals("0", DefinitionFile.formatNumber(-0.01F, 1));
        assertEquals(1.5F, DefinitionFile.parseFloat("1,5"));
    }
}
