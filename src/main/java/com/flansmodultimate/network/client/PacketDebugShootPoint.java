package com.flansmodultimate.network.client;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.IClientPacket;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Mirrors a shoot-point debug override onto the client's own copy of a type.
 *
 * <p>Both sides hold their own {@link DriveableType} instances, and the in-world
 * muzzle markers are drawn from the client's. In singleplayer the two are the
 * same object and this changes nothing; on a server it is what keeps the markers
 * showing where the shot actually comes from.</p>
 *
 * <p>Purely a developer diagnostic: it carries no gameplay state, and the command
 * that sends it is permission gated.</p>
 */
public class PacketDebugShootPoint implements IClientPacket
{
    /** Widest offset a marker may be moved to, in model pixels. */
    private static final float MAX_MODEL_PIXELS = 1024F;

    public enum Operation { SET_PRIMARY, SET_SECONDARY, ADD_PRIMARY, ADD_SECONDARY, GUN_ORIGIN, RESET }

    private static final Operation[] OPERATIONS = Operation.values();

    private String shortName = StringUtils.EMPTY;
    private Operation operation = Operation.RESET;
    private int index;
    private float x;
    private float y;
    private float z;
    private String partName = StringUtils.EMPTY;

    public PacketDebugShootPoint()
    {
    }

    public PacketDebugShootPoint(String shortName, Operation operation, int index, Vector3f modelPixels,
                                 @NotNull String partName)
    {
        this.shortName = StringUtils.defaultString(shortName);
        this.operation = operation;
        this.index = index;
        this.x = modelPixels.x;
        this.y = modelPixels.y;
        this.z = modelPixels.z;
        this.partName = partName;
    }

    public static PacketDebugShootPoint reset(String shortName)
    {
        return new PacketDebugShootPoint(shortName, Operation.RESET, 0, new Vector3f(), StringUtils.EMPTY);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(shortName);
        data.writeByte(operation.ordinal());
        data.writeVarInt(index);
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);
        data.writeUtf(partName);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        shortName = data.readUtf();
        int ordinal = data.readByte();
        operation = ordinal >= 0 && ordinal < OPERATIONS.length ? OPERATIONS[ordinal] : Operation.RESET;
        index = data.readVarInt();
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();
        partName = data.readUtf();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!(InfoType.getInfoType(shortName) instanceof DriveableType type) || !isSaneOffset())
            return;

        Vector3f position = new Vector3f(x, y, z);
        switch (operation)
        {
            case SET_PRIMARY -> type.setDebugShootPoint(false, index, position);
            case SET_SECONDARY -> type.setDebugShootPoint(true, index, position);
            case ADD_PRIMARY -> type.addDebugShootPoint(false, position, EnumDriveablePart.getPart(partName));
            case ADD_SECONDARY -> type.addDebugShootPoint(true, position, EnumDriveablePart.getPart(partName));
            case GUN_ORIGIN -> type.setDebugGunOrigin(index, position);
            case RESET -> type.resetDebugOverrides();
        }
    }

    private boolean isSaneOffset()
    {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
            && Math.abs(x) <= MAX_MODEL_PIXELS && Math.abs(y) <= MAX_MODEL_PIXELS && Math.abs(z) <= MAX_MODEL_PIXELS;
    }
}
