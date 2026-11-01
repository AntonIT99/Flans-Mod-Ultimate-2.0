package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpawnerNpcPolicyTest
{
    @Test
    void categoriesAllowSeveralKindsAndShipsRemainSeparate()
    {
        IAAGunType aa = mock(IAAGunType.class);
        IDriveableType ship = mock(IDriveableType.class);
        when(ship.isShip()).thenReturn(true);
        SpawnerNpcPolicy policy = new SpawnerNpcPolicy(SpawnerNpcPolicy.Mode.CATEGORIES, Set.of("aa_guns", "ships"));
        assertTrue(policy.allows(aa));
        assertTrue(policy.allows(ship));
        assertFalse(policy.allows(mock(IContentType.class)));
        assertFalse(new SpawnerNpcPolicy(SpawnerNpcPolicy.Mode.CATEGORIES, Set.of("vehicles")).allows(ship));
        assertEquals(policy, SpawnerNpcPolicy.load(policy.save()));
    }

    @Test
    void exactDefinitionsAndLegacyDefaults()
    {
        IAAGunType aa = mock(IAAGunType.class);
        when(aa.getShortName()).thenReturn("testAA");
        assertTrue(SpawnerNpcPolicy.load(new CompoundTag()).allows(aa));
        assertFalse(new SpawnerNpcPolicy(SpawnerNpcPolicy.Mode.NONE, Set.of()).allows(aa));
        assertTrue(new SpawnerNpcPolicy(SpawnerNpcPolicy.Mode.DEFINITIONS, Set.of("testAA", "tigerII")).allows(aa));
        assertFalse(new SpawnerNpcPolicy(SpawnerNpcPolicy.Mode.DEFINITIONS, Set.of("otherAA")).allows(aa));
        assertFalse(SpawnerNpcPolicy.ALL.allows(null));
    }
}
