package com.flansmodultimate.common.driveables;

import com.flansmodultimate.api.parts.FlansRepairs;
import com.flansmodultimate.api.parts.IFlanRepairTarget;
import com.flansmodultimate.common.entity.Driveable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows a player the progress of a hand-tool repair: sparks and a hammer strike where the tool met the hull, and the repaired
 * part's health bar with the overall health on the action bar. Server side, after the repair has been applied.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepairToolFeedback
{
    private static final int BAR_SEGMENTS = 20;

    /** One part's hit points as the server holds them. */
    public record PartHealth(String name, String displayName, float health, float maximum)
    {}

    /** Hit points of every part of a driveable or part-body wearer that holds any; read before a repair to find the part it raised. */
    public static List<PartHealth> parts(Entity target)
    {
        List<PartHealth> parts = new ArrayList<>();
        if (target instanceof Driveable driveable && driveable.getDriveableData() != null)
        {
            for (DriveablePart part : driveable.getDriveableData().getParts().values())
            {
                if (part.getMaxHealth() > 0F)
                    parts.add(new PartHealth(part.getType().getShortName(), part.getType().getName(), Math.max(0F, part.getHealth()), part.getMaxHealth()));
            }
        }
        else if (target instanceof IFlanRepairTarget repairable)
        {
            repairable.flansmodultimatePartBody().ifPresent(body -> body.getParts().stream().filter(part -> part.enabled() && part.box().health() > 0F)
                .forEach(part -> parts.add(new PartHealth(part.name(), part.displayName(), part.health(), part.box().health()))));
        }
        return parts;
    }

    /**
     * @param before
     *            part health read with {@link #parts} before the repair
     * @param start
     *            start of the tool ray in world space
     * @param end
     *            end of the tool ray in world space
     */
    public static void show(ServerPlayer player, Entity target, List<PartHealth> before, Vec3 start, Vec3 end)
    {
        Map<String, Float> previous = new HashMap<>();
        for (PartHealth part : before)
            previous.put(part.name(), part.health());
        List<PartHealth> after = parts(target);
        PartHealth repaired = null;
        float bestGain = 0F;
        for (PartHealth part : after)
        {
            float gain = part.health() - previous.getOrDefault(part.name(), part.health());
            if (gain > bestGain)
            {
                repaired = part;
                bestGain = gain;
            }
        }

        boolean complete = repaired != null && repaired.health() >= repaired.maximum();
        Vec3 point = FlansRepairs.toolHitPoint(target, start, end).orElseGet(() -> closestPoint(target.getBoundingBox(), start));
        effects(player.serverLevel(), point, complete);
        if (repaired != null)
            player.displayClientMessage(message(repaired, after, complete), true);
    }

    private static void effects(ServerLevel level, Vec3 point, boolean complete)
    {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 12, 0.2D, 0.2D, 0.2D, 0.2D);
        level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 3, 0.1D, 0.1D, 0.1D, 0.01D);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.3F, 1.4F + level.random.nextFloat() * 0.3F);
        if (!complete)
            return;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 14, 0.5D, 0.5D, 0.5D, 0D);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1F);
    }

    private static Component message(PartHealth part, List<PartHealth> parts, boolean complete)
    {
        float health = 0F;
        float maximum = 0F;
        for (PartHealth each : parts)
        {
            health += each.health();
            maximum += each.maximum();
        }
        Component name = Component.literal(part.displayName()).withStyle(ChatFormatting.YELLOW);
        Component overall = percent(maximum <= 0F ? 1F : health / maximum);
        if (complete)
            return Component.translatable("gui.flansmodultimate.driveable.tool_repaired", name, overall).withStyle(ChatFormatting.GRAY);
        float share = part.health() / part.maximum();
        return Component.translatable("gui.flansmodultimate.driveable.tool_repair", name, bar(share), percent(share), overall).withStyle(ChatFormatting.GRAY);
    }

    private static Component bar(float share)
    {
        int filled = Mth.clamp(Mth.floor(share * BAR_SEGMENTS), 0, BAR_SEGMENTS);
        return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal("|".repeat(filled)).withStyle(color(share)))
            .append(Component.literal("|".repeat(BAR_SEGMENTS - filled))).append("]");
    }

    /** Rounded down, so a part reads 100% only once it is whole. */
    private static MutableComponent percent(float share)
    {
        return Component.literal(Integer.toString(Mth.clamp(Mth.floor(share * 100F), 0, 100))).withStyle(color(share));
    }

    private static ChatFormatting color(float share)
    {
        if (share < 1F / 3F)
            return ChatFormatting.RED;
        return share < 2F / 3F ? ChatFormatting.GOLD : ChatFormatting.GREEN;
    }

    private static Vec3 closestPoint(AABB box, Vec3 origin)
    {
        return new Vec3(Mth.clamp(origin.x, box.minX, box.maxX), Mth.clamp(origin.y, box.minY, box.maxY), Mth.clamp(origin.z, box.minZ, box.maxZ));
    }
}
