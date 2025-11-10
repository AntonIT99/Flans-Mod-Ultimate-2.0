package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.types.GunType;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Setter;

public class ModelMG extends ModelBase implements IFlanTypeModel<GunType>
{
    @Setter
    protected GunType type;

    protected ModelRendererTurbo[] bipodModel;
    protected ModelRendererTurbo[] gunModel;
    protected ModelRendererTurbo[] ammoModel;
    protected ModelRendererTurbo[] ammoBoxModel = new ModelRendererTurbo[0];

    @Override
    public Class<GunType> typeClass()
    {
        return GunType.class;
    }

    public void renderBipod(float f, float f1, float f2, float f3, float f4, float f5, DeployedGun mg)
    {
        for(ModelRendererTurbo bipodPart : bipodModel)
        {
            bipodPart.render(f5);
        }
        if (mg.getReloadTimer() > 0 || mg.getAmmo().isEmpty())
            return;

        for(ModelRendererTurbo ammoBoxPart : ammoBoxModel)
        {
            ammoBoxPart.render(f5);
        }
    }

    public void renderGun(float f, float f1, float f2, float f3, float f4, float f5, float f6, DeployedGun mg)
    {
        for (ModelRendererTurbo gunPart : gunModel)
        {
            gunPart.rotateAngleX = -(mg.xRotO + (mg.getXRot() - mg.xRotO) * f6) / 180F * (float) Math.PI;
            gunPart.render(f5);
        }

        if (mg.getReloadTimer() > 0 || mg.getAmmo().isEmpty())
            return;

        for (ModelRendererTurbo ammoPart : ammoModel)
        {
            ammoPart.rotateAngleX = -(mg.xRotO + (mg.getXRot() - mg.xRotO) * f6) / 180F * (float) Math.PI;
            ammoPart.render(f5);
        }
    }

    public void flipAll()
    {
        for (ModelRendererTurbo aBipodModel : bipodModel)
        {
            aBipodModel.doMirror(false, true, true);
            aBipodModel.setRotationPoint(aBipodModel.rotationPointX, -aBipodModel.rotationPointY, -aBipodModel.rotationPointZ);
        }
        for (ModelRendererTurbo aGunModel : gunModel)
        {
            aGunModel.doMirror(false, true, true);
            aGunModel.setRotationPoint(aGunModel.rotationPointX, -aGunModel.rotationPointY, -aGunModel.rotationPointZ);
        }
        for (ModelRendererTurbo anAmmoModel : ammoModel)
        {
            anAmmoModel.doMirror(false, true, true);
            anAmmoModel.setRotationPoint(anAmmoModel.rotationPointX, -anAmmoModel.rotationPointY, -anAmmoModel.rotationPointZ);
        }
        for (ModelRendererTurbo anAmmoBoxModel : ammoBoxModel)
        {
            anAmmoBoxModel.doMirror(false, true, true);
            anAmmoBoxModel.setRotationPoint(anAmmoBoxModel.rotationPointX, -anAmmoBoxModel.rotationPointY, -anAmmoBoxModel.rotationPointZ);
        }
    }
}
