package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TypeReaderUtilsTest
{
    @Test
    void keepsApostrophesInsideLegacyProjectileNames()
    {
        assertArrayEquals(new String[]{"Pzgr.L'Spur", "1", "72", "0", "850", "30"}, TypeReaderUtils.splitValues("Pzgr.L'Spur 1 72 0 850 30"));
    }

    @Test
    void stillSupportsQuotedValues()
    {
        assertArrayEquals(new String[]{"High Explosive", "1", "72"}, TypeReaderUtils.splitValues("\"High Explosive\" 1 72"));
    }

    @Test
    void separatesAdjacentVectorsAndDropsDoubledBrackets()
    {
        int[] repairs = {0};
        Runnable onRepair = () -> repairs[0]++;
        assertArrayEquals(new String[]{"[12,1,12]", "[0,1,0]", "-1"}, TypeReaderUtils.tokenize("[12,1,12][0,1,0] -1", onRepair));
        assertArrayEquals(new String[]{"[12,1,12]", "[0,1,0]", "-1"}, TypeReaderUtils.tokenize("[12,1,12] [[0,1,0] -1", onRepair));
        assertEquals(2, repairs[0]);
    }

    @Test
    void leavesWellFormedVectorsUnreported()
    {
        assertArrayEquals(new String[]{"[1,2,3]", "[4,5,6]"}, TypeReaderUtils.tokenize("[1,2,3] [4,5,6]", () -> fail("Nothing to repair")));
    }

    @Test
    void separatesOnlyANumberFollowedByAName()
    {
        assertArrayEquals(new String[]{"-0", "leftWing"}, TypeReaderUtils.splitNumberThenName("-0leftWing"));
        assertNull(TypeReaderUtils.splitNumberThenName("leftWing"));
        assertNull(TypeReaderUtils.splitNumberThenName("46"));
    }

    @Test
    void silencesOnlyFilesTheModReadsDirectly()
    {
        assertTrue(TypeReaderUtils.isDirectlyReadPackFileMessage("Invalid path in pack: flansmod:animations/AUGReload.json, ignoring"));
        assertTrue(TypeReaderUtils.isDirectlyReadPackFileMessage("Invalid path in pack: flansmod:models/GustavBodyFix.obj, ignoring"));
        assertFalse(TypeReaderUtils.isDirectlyReadPackFileMessage("Invalid path in pack: flansmod:models/item/Broken.json, ignoring"));
        assertFalse(TypeReaderUtils.isDirectlyReadPackFileMessage("Invalid path in pack: othermod:animations/Broken.json, ignoring"));
    }

    @Test
    void recoversNumbersWithTrailingTypos()
    {
        assertEquals(80, TypeReaderUtils.recoverInteger("80s"));
        assertEquals(0, TypeReaderUtils.recoverInteger("0k"));
        assertEquals(0.533D, TypeReaderUtils.recoverDecimal("0.533s"));
        assertEquals(1.75D, TypeReaderUtils.recoverDecimal("1.75.2"));
        assertEquals(4.5D, TypeReaderUtils.recoverDecimal("4.5."));
        assertEquals(0.2D, TypeReaderUtils.recoverDecimal("0,2"));
        assertEquals(0D, TypeReaderUtils.recoverDecimal("0-"));
    }

    @Test
    void doesNotInventNumbersFromWords()
    {
        assertNull(TypeReaderUtils.recoverInteger("Burst3"));
        assertNull(TypeReaderUtils.recoverDecimal("ModelScale"));
    }

    @Test
    void recognizesOnlyValuesThatAreClearlyNumbers()
    {
        assertTrue(TypeReaderUtils.isPlainNumber("30"));
        assertTrue(TypeReaderUtils.isPlainNumber("2.5"));
        assertFalse(TypeReaderUtils.isPlainNumber("flame"));
        assertFalse(TypeReaderUtils.isPlainNumber("Burst3"));
        assertFalse(TypeReaderUtils.isPlainNumber("30s"));
        assertFalse(TypeReaderUtils.isPlainNumber("NaN"));
        assertFalse(TypeReaderUtils.isPlainNumber(null));
    }

    @Test
    void recoversOnlyRecognizableBooleans()
    {
        assertEquals(Boolean.TRUE, TypeReaderUtils.recoverBoolean("Trues"));
        assertEquals(Boolean.FALSE, TypeReaderUtils.recoverBoolean("Flase"));
        assertNull(TypeReaderUtils.recoverBoolean("5"));
    }
}
