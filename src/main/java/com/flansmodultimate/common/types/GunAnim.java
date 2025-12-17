package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelGun;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class GunAnim
{
    private boolean initialized;

    private float numBulletsInReloadAnimation;
    private int pumpDelay;
    private int pumpDelayAfterReload;
    private int pumpTime;
    private int hammerDelay;

    @OnlyIn(Dist.CLIENT)
    public void read(GunType type, TypeFile file)
    {
        IModelBase model = InfoType.loadModel(type.getModelClassName(), type, type.getDefaultModel());
        if (model instanceof ModelGun modelGun)
        {
            numBulletsInReloadAnimation = readValue("animNumBulletsInReloadAnimation", modelGun.getNumBulletsInReloadAnimation(), file);
            pumpDelay = readValue("animPumpDelay", modelGun.getPumpDelay(), file);
            pumpDelayAfterReload = readValue("animPumpDelayAfterReload", modelGun.getPumpDelayAfterReload(), file);
            pumpTime = readValue("animPumpTime", modelGun.getPumpTime(), file);
            hammerDelay = readValue("animHammerDelay", modelGun.getHammerDelay(), file);

            //TODO: continue (WIP)
        }
        initialized = true;
    }

    @OnlyIn(Dist.CLIENT)
    public void write(ModelGun modelGun)
    {
        if (!initialized)
            return;

        modelGun.setNumBulletsInReloadAnimation(numBulletsInReloadAnimation);
        modelGun.setPumpDelay(pumpDelay);
        modelGun.setPumpDelayAfterReload(pumpDelayAfterReload);
        modelGun.setPumpTime(pumpTime);
        modelGun.setHammerDelay(hammerDelay);
    }
}
