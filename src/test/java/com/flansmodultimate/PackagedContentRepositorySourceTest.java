package com.flansmodultimate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagedContentRepositorySourceTest
{
    @Test
    void encryptedPackSortsBeforeNormalAssetsForTopPositionInsertion()
    {
        String modId = "test_content_module";

        assertTrue(PackagedContentRepositorySource.encryptedPackId(modId)
            .compareTo(modId + ":assets") < 0);
    }
}
