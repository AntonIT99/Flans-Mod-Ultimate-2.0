package com.flansmodultimate.apocalyse;

import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.apocalyse.common.util.ApocalypseGunHelper;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApocalypseMobParityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void peacefulDifficultyDoesNotDiscardTheBossOrItsDrones()
    {
        Level level = mock(Level.class);
        when(level.getDifficulty()).thenReturn(Difficulty.PEACEFUL);
        for (Mob mob : List.of(mock(SkullBossEntity.class, CALLS_REAL_METHODS), mock(SkullDroneEntity.class, CALLS_REAL_METHODS)))
        {
            doReturn(level).when(mob).level();
            doReturn(true).when(mob).isPersistenceRequired();
            doNothing().when(mob).discard();

            mob.checkDespawn();

            verify(mob, never()).discard();
        }
    }

    @Test
    void dronesCanReloadSmokeGrenadesWhileSurvivorsCanAlsoCarryExplosiveAmmunition()
    {
        GunType gun = mock(GunType.class);
        BulletType explosive = mock(BulletType.class);
        GrenadeType smoke = mock(GrenadeType.class);
        when(explosive.getExplosiveMass()).thenReturn(1F);
        when(gun.getAmmoTypes()).thenReturn(List.of(explosive, smoke));
        RandomSource random = mock(RandomSource.class);
        try (var utils = mockStatic(ModUtils.class))
        {
            utils.when(() -> ModUtils.getItemStack(explosive)).thenReturn(Optional.of(new ItemStack(Items.FIRE_CHARGE)));
            utils.when(() -> ModUtils.getItemStack(explosive, 1)).thenReturn(Optional.of(new ItemStack(Items.FIRE_CHARGE)));
            utils.when(() -> ModUtils.getItemStack(smoke)).thenReturn(Optional.of(new ItemStack(Items.SNOWBALL)));
            utils.when(() -> ModUtils.getItemStack(smoke, 1)).thenReturn(Optional.of(new ItemStack(Items.SNOWBALL)));

            assertTrue(ApocalypseGunHelper.spareAmmoFor(gun, random).orElseThrow().is(Items.SNOWBALL));
            assertTrue(ApocalypseGunHelper.spareAmmoFor(gun, random, true).orElseThrow().is(Items.FIRE_CHARGE));
        }
    }

    @Test
    void survivorJournalsKeepAllEightLegacyEntriesAndTheTwoPagePortalWarning()
    {
        String[] titles = {"Help me!", "The Endtimes", "Day 5", "Day 7", "Day 10", "The Wasteland", "", "Time Portal: Instruction Manual"};
        RandomSource random = mock(RandomSource.class);
        for (int i = 0; i < titles.length; i++)
        {
            when(random.nextInt(8)).thenReturn(i);
            ItemStack journal = ApocalypseLoot.survivorJournal(random);
            var tag = journal.getOrCreateTag();
            assertEquals(titles[i], tag.getString("title"));
            assertEquals("Flan", tag.getString("author"));
            var pages = tag.getList("pages", Tag.TAG_STRING);
            assertEquals(i == 7 ? 2 : 1, pages.size());
            if (i == 7)
            {
                assertEquals(3, tag.getInt("generation"));
                Component warning = Component.Serializer.fromJson(pages.getString(1));
                assertNotNull(warning);
                assertTrue(warning.getString().startsWith("Beware! The mechas are coming!"));
            }
        }
    }

    @Test
    void survivorDeathLootRollsFoodCountOnceAndIncludesBooksAndShovels()
    {
        Mob mob = mock(Mob.class);
        Level level = mock(Level.class);
        RandomSource random = mock(RandomSource.class);
        when(mob.level()).thenReturn(level);
        when(mob.getRandom()).thenReturn(random);
        when(random.nextInt(5)).thenReturn(4, 0, 1, 2, 3, 1);
        when(random.nextInt(12)).thenReturn(0, 1);
        when(random.nextInt(40)).thenReturn(1, 1, 0);
        when(random.nextInt(4)).thenReturn(1);
        List<ItemStack> loot = new ArrayList<>();
        try (var drops = mockStatic(Containers.class); var guns = mockStatic(ApocalypseGunHelper.class))
        {
            guns.when(() -> ApocalypseGunHelper.randomAmmoStack(random)).thenReturn(Optional.empty());
            drops.when(() -> Containers.dropItemStack(eq(level), anyDouble(), anyDouble(), anyDouble(), any(ItemStack.class))).thenAnswer(call ->
            {
                loot.add(call.getArgument(4));
                return null;
            });

            ApocalypseLoot.dropSurvivorLoot(mob);

            assertEquals(List.of(Items.COOKED_BEEF, Items.BREAD, Items.MUSHROOM_STEW, Items.COOKED_RABBIT, Items.BOOK, Items.IRON_SHOVEL), loot.stream().map(ItemStack::getItem).toList());
        }
    }
}
