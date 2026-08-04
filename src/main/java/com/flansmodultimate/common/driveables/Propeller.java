package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/** Plane propeller definition with a load-order-safe part-item reference. */
@Getter
public final class Propeller extends DriveablePosition
{
    private final int id;
    private final String itemTypeShortName;
    @Nullable private final IContentProvider contentPack;

    public Propeller(int id, Vector3f position, EnumDriveablePart planePart, String itemTypeShortName)
    {
        this(id, position, planePart, itemTypeShortName, null);
    }

    public Propeller(int id, Vector3f position, EnumDriveablePart planePart, String itemTypeShortName, @Nullable IContentProvider contentPack)
    {
        super(position, planePart);
        this.id = Math.max(0, id);
        this.itemTypeShortName = StringUtils.defaultString(itemTypeShortName);
        this.contentPack = contentPack;
    }

    public EnumDriveablePart getPlanePart()
    {
        return getPart();
    }

    @Nullable
    public PartType getItemType()
    {
        InfoType resolved = InfoType.getInfoType(itemTypeShortName, contentPack);
        return resolved instanceof PartType partType ? partType : null;
    }
}
