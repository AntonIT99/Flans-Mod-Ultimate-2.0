package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.entity.HumanoidBody;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.ModelData;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcHumanoidBodyTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void anNpcWithThePlayerModelIsDrawnAsAPlayer()
    {
        EntityCustomNpc npc = npc(new NpcWeaponOptions());
        HumanoidBody body = body(npc);
        assertEquals(HumanoidBody.PLAYER_SCALE, body.scaleY(), 0.0001F);
        assertEquals(0.25F, body.offsetY(), 0.0001F);
        assertEquals(HumanoidBody.Part.STANDARD, body.head());
        assertEquals(HumanoidBody.Part.STANDARD, body.leftLeg());
        assertEquals(HumanoidBody.Part.STANDARD, body.rightArm());

        when(npc.display.getSize()).thenReturn(10F);
        assertEquals(HumanoidBody.PLAYER_SCALE * 2F, body(npc).scaleX(), 0.0001F);
    }

    @Test
    void shorterLegsLowerTheBodyAsCustomNpcsDrawsIt()
    {
        EntityCustomNpc npc = npc(new NpcWeaponOptions());
        npc.modelData.leg1.setScale(1F, 0.5F, 1F);
        npc.modelData.arm1.setScale(0.5F, 1F, 1F);
        HumanoidBody body = body(npc);

        // Legs half as long leave 6 pixels of the model empty below the body, which moves every part down by as much
        assertEquals(6F, body.body().offsetY(), 0.0001F);
        assertEquals(6F, body.head().offsetY(), 0.0001F);
        assertEquals(6F, body.leftLeg().offsetY(), 0.0001F);
        assertEquals(0.5F, body.rightLeg().scaleY(), 0.0001F);
        // A shared arm setting applies to both arms, each moved in towards the body
        assertEquals(0.5F, body.rightArm().scaleX(), 0.0001F);
        assertEquals(0.5F, body.rightArm().offsetX(), 0.0001F);
        assertEquals(-0.5F, body.leftArm().offsetX(), 0.0001F);
    }

    @Test
    void otherModelsPosesAndTheSwitchKeepTheNpcsOwnHitbox()
    {
        NpcWeaponOptions off = new NpcWeaponOptions();
        off.set(Feature.LIMB_HITBOXES, false);
        assertTrue(of(npc(off)).isEmpty());

        EntityCustomNpc mob = npc(new NpcWeaponOptions());
        mob.modelData.setEntity(ResourceLocation.tryParse("minecraft:zombie"));
        assertTrue(of(mob).isEmpty());

        EntityCustomNpc sitting = npc(new NpcWeaponOptions());
        sitting.currentAnimation = AnimationType.SIT;
        assertTrue(of(sitting).isEmpty());
        when(sitting.isPassenger()).thenReturn(true);
        assertTrue(of(sitting).isPresent());

        EntityCustomNpc crawling = npc(new NpcWeaponOptions());
        crawling.currentAnimation = AnimationType.CRAWL;
        assertTrue(of(crawling).isEmpty());
    }

    private static EntityCustomNpc npc(NpcWeaponOptions options)
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.modelData = new ModelData(npc);
        npc.display = mock(DataDisplay.class);
        when(npc.display.getSize()).thenReturn(5F);
        npc.scaleX = HumanoidBody.PLAYER_SCALE;
        npc.scaleY = HumanoidBody.PLAYER_SCALE;
        npc.scaleZ = HumanoidBody.PLAYER_SCALE;
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        return npc;
    }

    private static Optional<HumanoidBody> of(EntityCustomNpc npc)
    {
        try (var models = mockStatic(NpcModels.class))
        {
            models.when(() -> NpcModels.bodyOffset(npc)).thenReturn(0.25D);
            return NpcHumanoidBody.of(npc);
        }
    }

    private static HumanoidBody body(EntityCustomNpc npc)
    {
        return of(npc).orElseThrow();
    }
}
