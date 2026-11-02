package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One successful mounted shot, expanded into budgeted particles locally instead of networking every sprite. */
@NoArgsConstructor
public class PacketWeaponShotEffects implements IClientPacket
{
    private String weapon;
    private Vec3 origin;
    private Vec3 direction;
    private float scale;
    private boolean heavy;

    public PacketWeaponShotEffects(String weapon, Vec3 origin, Vec3 direction, float scale, boolean heavy)
    {
        this.weapon = weapon;
        this.origin = origin;
        this.direction = direction;
        this.scale = scale;
        this.heavy = heavy;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(weapon);
        data.writeDouble(origin.x);
        data.writeDouble(origin.y);
        data.writeDouble(origin.z);
        data.writeFloat((float) direction.x);
        data.writeFloat((float) direction.y);
        data.writeFloat((float) direction.z);
        data.writeFloat(scale);
        data.writeBoolean(heavy);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        weapon = data.readUtf();
        origin = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        direction = new Vec3(data.readFloat(), data.readFloat(), data.readFloat());
        scale = data.readFloat();
        heavy = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.weaponShotEffects(weapon, origin, direction, scale, heavy);
    }
}
