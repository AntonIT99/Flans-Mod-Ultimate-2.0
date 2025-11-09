package com.wolffsarmormod.network;

import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.client.render.InstantBulletRenderer;
import com.wolffsarmormod.client.render.InstantShotTrail;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor
public class PacketBulletTrail extends PacketBase
{
    private Vector3f origin;
    private Vector3f hitPos;
    private float width;
    private float length;
    private float bulletSpeed;
    private String trailTexture;

    public PacketBulletTrail(Vector3f origin, Vector3f hitPos, Float width, Float length, Float bulletSpeed, String trailTexture)
    {
        this.origin = origin;
        this.hitPos = hitPos;
        this.width = width;
        this.length = length;
        this.bulletSpeed = bulletSpeed;
        this.trailTexture = trailTexture;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(origin.x);
        data.writeFloat(origin.y);
        data.writeFloat(origin.z);

        data.writeFloat(hitPos.x);
        data.writeFloat(hitPos.y);
        data.writeFloat(hitPos.z);

        data.writeFloat(width);
        data.writeFloat(length);
        data.writeFloat(bulletSpeed);

        data.writeUtf(trailTexture);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        origin = new Vector3f(data.readFloat(), data.readFloat(), data.readFloat());

        hitPos = new Vector3f(data.readFloat(), data.readFloat(), data.readFloat());

        width = data.readFloat();
        length = data.readFloat();
        bulletSpeed = data.readFloat();

        trailTexture = data.readUtf();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        //TODO trails not visible when trail origin position and player camera position are to close. the can only be seen with an slight angle
        ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "textures/skins/" + trailTexture + ".png");
        InstantBulletRenderer.addTrail(new InstantShotTrail(origin, hitPos, width, length, bulletSpeed, resLoc));
    }
}
