package com.flansmodultimate.client.model;

import com.flansmodultimate.common.types.InfoType;
import com.wolffsmod.api.client.model.IModelBase;

public interface IFlanTypeModel<T extends InfoType> extends IModelBase
{
    void setType(T type);

    Class<T> typeClass();
}
