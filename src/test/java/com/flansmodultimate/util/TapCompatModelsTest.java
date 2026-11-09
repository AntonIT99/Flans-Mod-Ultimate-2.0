package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TapCompatModelsTest
{
    @Test
    void redirectsOnlyLegacyModelClasses()
    {
        assertEquals("com.flansmodultimate.compat.tap.model.monolith.ModelDroyd", TapCompatModels.compatClassName("com.flansmod.client.model.monolith.ModelDroyd"));
        assertNull(TapCompatModels.compatClassName("com.example.ModelDroyd"));
    }

    @Test
    void knowsModelsThePackShipsElsewhere()
    {
        assertEquals("com.flansmod.client.model.W44.ModelZF41Scope", TapCompatModels.movedClassName("com.flansmod.client.model.NewAge.ModelZF41Scope"));
        assertNull(TapCompatModels.movedClassName("com.flansmod.client.model.NewAge.ModelAK12"));
    }

    @Test
    void recognizesTapReleasesWhateverTheirSuffix()
    {
        assertTrue(TapCompatModels.isTapDescription("Tyrants And Plebeians Exorcism"));
        assertTrue(TapCompatModels.isTapDescription("tyrants and plebeians redemption"));
        assertFalse(TapCompatModels.isTapDescription("Modern Warfare Content Pack"));
    }
}
