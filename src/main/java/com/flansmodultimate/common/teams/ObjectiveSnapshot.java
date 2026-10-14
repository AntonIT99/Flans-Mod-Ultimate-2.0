package com.flansmodultimate.common.teams;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/** Client-safe view of one capture point, sent for the objective HUD and the deployment map. */
public record ObjectiveSnapshot(UUID id, char symbol, String name, int ownerTeamId, int capturingTeamId, float progress, boolean contested, boolean spawnPoint, int radius, double x, double y,
    double z)
{
    private static final int MAX_NAME_LENGTH = 60;

    public static ObjectiveSnapshot of(ITeamCapturePoint point, boolean spawnPoint)
    {
        CaptureState state = point.getCaptureState();
        return new ObjectiveSnapshot(point.getObjectId(), point.getSymbol(), point.getBaseName(), state.ownerTeamId(), state.capturingTeamId(), state.progress(), state.contested(), spawnPoint,
            point.getCaptureRadius(), point.getTeamObjectPosition().x, point.getTeamObjectPosition().y, point.getTeamObjectPosition().z);
    }

    public CaptureState state()
    {
        return new CaptureState(ownerTeamId, capturingTeamId, progress, contested);
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeUUID(id);
        buf.writeChar(symbol);
        buf.writeUtf(name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name, MAX_NAME_LENGTH);
        buf.writeVarInt(ownerTeamId);
        buf.writeVarInt(capturingTeamId);
        buf.writeFloat(progress);
        buf.writeBoolean(contested);
        buf.writeBoolean(spawnPoint);
        buf.writeVarInt(radius);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static ObjectiveSnapshot read(FriendlyByteBuf buf)
    {
        return new ObjectiveSnapshot(buf.readUUID(), buf.readChar(), buf.readUtf(MAX_NAME_LENGTH), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
            buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
