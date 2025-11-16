package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.types.InfoType;

public interface IFlanEntity<T extends InfoType>
{
    String getShortName();

    T getConfigType();
}
