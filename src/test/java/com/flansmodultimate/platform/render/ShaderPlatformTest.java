package com.flansmodultimate.platform.render;

import net.irisshaders.iris.api.v0.IrisApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.jupiter.api.Assertions.*;

class ShaderPlatformTest
{
    @AfterEach
    void resetStub()
    {
        IrisApi.Stub.INSTANCE.shaderPackInUse = false;
        IrisApi.Stub.INSTANCE.renderingShadowPass = false;
    }

    @Test
    void queriesBindToTheLiveApiInstanceWithAnExactBooleanType() throws Throwable
    {
        MethodHandle inUse = ShaderPlatform.irisApiQuery("isShaderPackInUse");
        MethodHandle shadowPass = ShaderPlatform.irisApiQuery("isRenderingShadowPass");

        // invokeExact is what the hot path uses; any other handle type would throw here.
        assertFalse((boolean)inUse.invokeExact());
        assertFalse((boolean)shadowPass.invokeExact());

        IrisApi.Stub.INSTANCE.shaderPackInUse = true;
        assertTrue((boolean)inUse.invokeExact());
        assertFalse((boolean)shadowPass.invokeExact());

        IrisApi.Stub.INSTANCE.renderingShadowPass = true;
        assertTrue((boolean)shadowPass.invokeExact());
    }

    @Test
    void absentQueryFailsToResolve()
    {
        assertThrows(NoSuchMethodException.class, () -> ShaderPlatform.irisApiQuery("getSunPathRotation"));
    }
}
