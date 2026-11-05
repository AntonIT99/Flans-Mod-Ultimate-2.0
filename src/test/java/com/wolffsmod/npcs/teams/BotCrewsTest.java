package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotCrewsTest
{
    @Test
    void eachSelectedAvailableClassCanCrewASeat()
    {
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(0, 1);
        List<String> offered = List.of("infantry", "driver", "engineer");
        String wanted = "driver|missing|engineer|driver";
        assertEquals(Optional.of("driver"), BotCrews.selectedClass(offered, wanted, random));
        assertEquals(Optional.of("engineer"), BotCrews.selectedClass(offered, wanted, random));
        verify(random, times(2)).nextInt(2);
    }

    @Test
    void legacySingleClassesAndExclusiveModesRetainTheirMeaning()
    {
        Random random = new Random(1);
        List<String> offered = List.of("driver", "infantry");
        assertEquals(Optional.of("driver"), BotCrews.selectedClass(offered, "driver", random));
        assertEquals(Optional.of(""), BotCrews.selectedClass(offered, "NONE", random));
        assertEquals(Optional.empty(), BotCrews.selectedClass(offered, "", random));
        assertEquals(Optional.empty(), BotCrews.selectedClass(offered, "missing|removed", random));
        assertEquals(Optional.of("driver"), BotCrews.selectedClass(offered, "missing|driver", random));
    }
}
