package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.api.entity.HumanoidBody;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HumanoidBodySnapshotTest
{
    private static final float EPSILON = 0.0001F;
    private static final HumanoidBody PLAYER = HumanoidBody.standard(HumanoidBody.PLAYER_SCALE, 0F);

    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries initialize first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    @Test
    void playersKeepTheirBoxes()
    {
        Player player = humanoid(Player.class);
        PlayerSnapshot own = new PlayerSnapshot(player);
        PlayerSnapshot standard = new PlayerSnapshot(player, 1F, PLAYER);
        for (EnumHitboxType type : List.of(EnumHitboxType.HEAD, EnumHitboxType.BODY, EnumHitboxType.LEFTARM, EnumHitboxType.RIGHTARM))
            assertTrue(box(own, type).transform.equals(box(standard, type).transform, EPSILON));
        assertEquals(1.876F, top(own, EnumHitboxType.HEAD), 0.001F);
    }

    @Test
    void anNpcIsHitLimbByLimb()
    {
        LivingEntity npc = humanoid(LivingEntity.class);
        PlayerSnapshot snapshot = new PlayerSnapshot(npc, 1F, PLAYER);

        List<BulletHit> head = snapshot.raytrace(new Vector3f(0F, 1.7F, -2F), new Vector3f(0F, 0F, 4F));
        assertEquals(EnumHitboxType.HEAD, ((PlayerBulletHit) head.get(0)).getHitbox().type);
        assertSame(npc, head.get(0).getEntity());
        // Inside the entity's box, which is 0.3 blocks either side, but beside the head
        assertTrue(snapshot.raytrace(new Vector3f(0.27F, 1.7F, -2F), new Vector3f(0F, 0F, 4F)).isEmpty());
    }

    @Test
    void theWholeModelScalesAndRises()
    {
        LivingEntity npc = humanoid(LivingEntity.class);
        float standing = top(new PlayerSnapshot(npc, 1F, PLAYER), EnumHitboxType.HEAD);

        assertEquals(standing * 2F, top(new PlayerSnapshot(npc, 1F, HumanoidBody.standard(HumanoidBody.PLAYER_SCALE * 2F, 0F)), EnumHitboxType.HEAD), 0.001F);
        assertEquals(standing - 0.5F, top(new PlayerSnapshot(npc, 1F, HumanoidBody.standard(HumanoidBody.PLAYER_SCALE, -0.5F)), EnumHitboxType.HEAD), 0.001F);
    }

    @Test
    void aPartMovesAndScalesAboutItsPivot()
    {
        LivingEntity npc = humanoid(LivingEntity.class);
        PlayerSnapshot standard = new PlayerSnapshot(npc, 1F, PLAYER);
        HumanoidBody.Part lowered = new HumanoidBody.Part(0F, 4F, 0F, 1F, 1F, 1F);
        HumanoidBody.Part halved = new HumanoidBody.Part(0F, 0F, 0F, 0.5F, 0.5F, 0.5F);
        PlayerSnapshot moved = new PlayerSnapshot(npc, 1F,
            new HumanoidBody(HumanoidBody.PLAYER_SCALE, HumanoidBody.PLAYER_SCALE, HumanoidBody.PLAYER_SCALE, 0F, lowered, null, null, null, null, null));
        PlayerSnapshot shrunk = new PlayerSnapshot(npc, 1F,
            new HumanoidBody(HumanoidBody.PLAYER_SCALE, HumanoidBody.PLAYER_SCALE, HumanoidBody.PLAYER_SCALE, 0F, halved, null, null, null, null, null));

        // Model pixels down are world blocks down, scaled by the model
        assertEquals(top(standard, EnumHitboxType.HEAD) - 4F / 16F * HumanoidBody.PLAYER_SCALE, top(moved, EnumHitboxType.HEAD), 0.001F);
        assertEquals(top(standard, EnumHitboxType.BODY), top(moved, EnumHitboxType.BODY), EPSILON);
        // The head's pivot is at its bottom, which stays where it is
        float neck = top(standard, EnumHitboxType.BODY);
        assertEquals((top(standard, EnumHitboxType.HEAD) - neck) / 2F, top(shrunk, EnumHitboxType.HEAD) - neck, 0.001F);
    }

    @Test
    void proportionsAreKeptValid()
    {
        HumanoidBody body = new HumanoidBody(Float.NaN, 0F, -1F, Float.POSITIVE_INFINITY, null, null, null, null, null, null);
        assertEquals(1F, body.scaleX());
        assertEquals(1F, body.scaleY());
        assertEquals(1F, body.scaleZ());
        assertEquals(0F, body.offsetY());
        assertSame(HumanoidBody.Part.STANDARD, body.head());
        assertEquals(HumanoidBody.Part.STANDARD, new HumanoidBody.Part(Float.NaN, 0F, 0F, 0F, Float.NaN, 1F));
    }

    private static <T extends LivingEntity> T humanoid(Class<T> type)
    {
        T entity = mock(type);
        Level level = mock(Level.class);
        when(entity.level()).thenReturn(level);
        when(entity.getMainArm()).thenReturn(HumanoidArm.RIGHT);
        when(entity.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(entity.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(entity.getItemInHand(any())).thenReturn(ItemStack.EMPTY);
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        return entity;
    }

    private static PlayerHitbox box(PlayerSnapshot snapshot, EnumHitboxType type)
    {
        return snapshot.hitboxes.stream().filter(hitbox -> hitbox.type == type).findFirst().orElseThrow();
    }

    /** @return the highest corner of the box, in blocks above the entity's feet */
    private static float top(PlayerSnapshot snapshot, EnumHitboxType type)
    {
        PlayerHitbox box = box(snapshot, type);
        float top = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++)
        {
            Vector3f point = new Vector3f(box.o).add((corner & 1) != 0 ? box.d.x : 0F, (corner & 2) != 0 ? box.d.y : 0F, (corner & 4) != 0 ? box.d.z : 0F);
            top = Math.max(top, box.transform.transformPosition(point).y);
        }
        return top;
    }
}
