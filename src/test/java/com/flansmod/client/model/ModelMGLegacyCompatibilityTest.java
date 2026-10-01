package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ModelMGLegacyCompatibilityTest
{
    @Test
    void flipAllAcceptsLegacyModelsWithOnlyBipodParts()
    {
        ModelMG model = new ModelMG();
        model.bipodModel = new ModelRendererTurbo[] {
            new ModelRendererTurbo(model, 0, 0, 16, 16)
        };

        assertDoesNotThrow(model::flipAll);
    }
}
