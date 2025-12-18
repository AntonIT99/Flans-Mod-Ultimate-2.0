package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelGun;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.flansmodultimate.util.TypeReaderUtils.readFloat;
import static com.flansmodultimate.util.TypeReaderUtils.readInteger;

@NoArgsConstructor
public class GunAnim
{
    private Float numBulletsInReloadAnimation = null;
    private Integer pumpDelay = null;
    private Integer pumpDelayAfterReload = null;
    private Integer pumpTime = null;
    private Integer hammerDelay = null;

    @OnlyIn(Dist.CLIENT)
    public void read(TypeFile file)
    {
        numBulletsInReloadAnimation = readFloat("animNumBulletsInReloadAnimation", file);
        pumpDelay = readInteger("animPumpDelay", file);
        pumpDelayAfterReload = readInteger("animPumpDelayAfterReload", file);
        pumpTime = readInteger("animPumpTime", file);
        hammerDelay = readInteger("animHammerDelay", file);

        //TODO: continue (WIP)
    }

    @OnlyIn(Dist.CLIENT)
    public void write(ModelGun modelGun)
    {
        if (numBulletsInReloadAnimation != null)
            modelGun.setNumBulletsInReloadAnimation(numBulletsInReloadAnimation);
        if (pumpDelay != null)
            modelGun.setPumpDelay(pumpDelay);
        if (pumpDelayAfterReload != null)
            modelGun.setPumpDelayAfterReload(pumpDelayAfterReload);
        if (pumpTime != null)
            modelGun.setPumpTime(pumpTime);
        if (hammerDelay != null)
            modelGun.setHammerDelay(hammerDelay);
    }
}
