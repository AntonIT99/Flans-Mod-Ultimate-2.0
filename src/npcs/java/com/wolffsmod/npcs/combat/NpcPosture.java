package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.entity.FlansPostures;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;

/**
 * Custom NPCs lays a soldier down with its own crawling animation rather than the vanilla crawling pose. Flan's Mod Ultimate
 * learns of it here, so a crawling soldier fires with its gun's prone spread and holds the gun forward along its body.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPosture
{
    /** Registers the rule on both sides, from the module's constructor. */
    public static void register()
    {
        FlansPostures.addProneRule(NpcPosture::isProne);
    }

    /** @return whether the NPC crawls on its own feet; a crew member keeps the animation but is drawn seated */
    static boolean isProne(LivingEntity entity)
    {
        return entity instanceof EntityNPCInterface npc && npc.currentAnimation == AnimationType.CRAWL && !npc.isPassenger();
    }
}
