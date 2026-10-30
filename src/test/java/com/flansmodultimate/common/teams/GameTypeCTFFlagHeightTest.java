package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GameTypeCTFFlagHeightTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void tallHomeFlagsRemainTouchableFromTheGround()
    {
        TeamsManager manager = mock(TeamsManager.class);
        ServerLevel level = mock(ServerLevel.class);
        Flag flag = mock(Flag.class);
        Flagpole pole = mock(Flagpole.class);
        when(manager.getCurrentLevel()).thenReturn(level);
        when(manager.getFlags()).thenReturn(List.of(flag));
        when(flag.getBase()).thenReturn(pole);
        when(flag.isHome()).thenReturn(true);
        when(flag.getY()).thenReturn(79D);
        when(pole.getY()).thenReturn(64D);
        when(flag.getBoundingBox()).thenReturn(new AABB(-0.375D, 79D, -0.375D, 0.375D, 79.75D, 0.375D));
        // Run the real touch check without registering a second global CTF game type.
        GameTypeCTF game = mock(GameTypeCTF.class, CALLS_REAL_METHODS);
        for (int tick = 0; tick < 5; tick++)
            game.tick(manager);
        ArgumentCaptor<AABB> reach = ArgumentCaptor.forClass(AABB.class);
        verify(level).getEntitiesOfClass(eq(LivingEntity.class), reach.capture(), any());
        assertEquals(63.5D, reach.getValue().minY);
        assertEquals(69.25D, reach.getValue().maxY);
    }
}
