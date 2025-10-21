package com.wolffsarmormod.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.network.PacketHandler;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FlansExplosion extends Explosion
{
    private final boolean causesFire;
    private final boolean breaksBlocks;
    private final Random random;
    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Player player;
    private final Entity explosive;
    private final float size;
    private final List<BlockPos> affectedBlockPositions;
    private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();
    private final Vec3 position;
    private final InfoType type; // type of Flan's Mod weapon causing explosion

    public FlansExplosion(Level level, Entity entity, @Nullable Player player, InfoType type, double x, double y, double z, float size, boolean causesFire, boolean smoking, boolean breaksBlocks)
    {
        super(level, entity, x, y, z, size, causesFire, breaksBlocks ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);
        this.random = new Random();
        this.affectedBlockPositions = Lists.newArrayList();
        this.level = level;
        this.player = player;
        this.size = size;
        this.x = x;
        this.y = y;
        this.z = z;
        this.causesFire = causesFire;
        //TODO Teams
        this.breaksBlocks = breaksBlocks; /*&& TeamsManager.explosions;*/
        this.position = new Vec3(this.x, this.y, this.z);
        this.type = type;
        this.explosive = entity;

        if(!ForgeEventFactory.onExplosionStart(level, this))
        {
            this.doExplosionA();
            this.doExplosionB(smoking);

            for (Player p : level.players())
            {
                PacketHandler.sendTo(new SPacketExplosion(x, y, z, size, affectedBlockPositions, getHitPlayers().get(p)), (ServerPlayer) p);
            }
        }
    }

    @Override
    @NotNull
    public Map<Player, Vec3> getHitPlayers()
    {
        return this.hitPlayers;
    }
}
