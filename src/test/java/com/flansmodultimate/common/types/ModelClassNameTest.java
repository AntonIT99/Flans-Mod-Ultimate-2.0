package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModelClassNameTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "models"));

    @Test
    void acceptsModelNamesContentPacksUse()
    {
        assertTrue(InfoType.isValidModelName("Ak47"));
        assertTrue(InfoType.isValidModelName("cvts.155mm_3rdYear"), "leading digits are legal, the class name prefixes Model");
        assertTrue(InfoType.isValidModelName("modernweapons.sub.M4"));
    }

    @Test
    void rejectsNamesNoClassCanHave()
    {
        assertFalse(InfoType.isValidModelName("cvts.155mm_3rdYear:Type"), "a colon is no legal path character on Windows");
        assertFalse(InfoType.isValidModelName("cvts/Model"));
        assertFalse(InfoType.isValidModelName("cvts\\Model"));
        assertFalse(InfoType.isValidModelName("Model Gun"));
        assertFalse(InfoType.isValidModelName(".Ak47"));
        assertFalse(InfoType.isValidModelName("Ak47."));
        assertFalse(InfoType.isValidModelName("cvts..Ak47"));
        assertFalse(InfoType.isValidModelName(""));
    }

    @Test
    void resolvingDropsCharactersNoClassNameHolds()
    {
        // Tyrants and Plebeians Redemption ships Model155mm_3rdYearType and asks for '155mm_3rdYear:Type'.
        assertEquals("com.flansmod.client.model.cvts.Model155mm_3rdYearType", InfoType.findModelClass("cvts.155mm_3rdYear:Type", PACK));
        assertEquals("com.flansmod.client.model.ModelAk47", InfoType.findModelClass("Ak 47", PACK));
    }

    @Test
    void resolvingAnUnrepairableModelNameYieldsNoClassInsteadOfFailing()
    {
        assertEquals("", InfoType.findModelClass(":::", PACK));
        assertEquals("", InfoType.findModelClass("..", PACK));
    }

    @Test
    void resolvingAMissingModelStillFallsBackToTheDefaultPackage()
    {
        assertEquals("com.flansmod.client.model.cvts.Model155mm", InfoType.findModelClass("cvts.155mm", PACK));
    }
}
